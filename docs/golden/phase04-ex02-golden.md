# Phase 04 Exercise 02 — Golden Reference Solution
## Resilient Guardrail Advisors & Failover Routing (15 Drills)

```java
package phase04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import phase04.ResilientAdvisorContracts.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service under test — Phase 04 Exercise 02:
 * Resilient Guardrails, Failover Routing, and Transactional Token Quotas (15 Scenarios).
 */
public class ResilientGuardrailAdvisorsUnderTest {

    // =========================================================================
    // Topic 1: Context Augmentation & History Truncation (Scenarios 1 - 3)
    // =========================================================================

    public static class RuntimeContextAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            String mode = String.valueOf(request.context().getOrDefault(ContextKeys.EXECUTION_MODE, "LIVE"));
            boolean isDryRun = "DRY_RUN".equalsIgnoreCase(mode);

            ChatClientRequest effectiveRequest = request;
            if (isDryRun && request.prompt().getUserMessage() != null) {
                String original = request.prompt().getUserMessage().getText();
                String prefixed = "[DRY_RUN] " + (original != null ? original : "");
                List<Message> updated = new ArrayList<>(request.prompt().getInstructions());
                for (int i = 0; i < updated.size(); i++) {
                    if (updated.get(i) instanceof UserMessage) {
                        updated.set(i, new UserMessage(prefixed));
                        break;
                    }
                }
                effectiveRequest = request.mutate()
                        .prompt(new Prompt(updated, request.prompt().getOptions()))
                        .build();
            }

            ChatClientResponse response = chain.nextCall(effectiveRequest);
            return response.mutate()
                    .context(ContextKeys.AUDIT_DRY_RUN, isDryRun)
                    .context(ContextKeys.PROCESSED_BY, "RuntimeContextAdvisor")
                    .build();
        }

        @Override
        public int getOrder() {
            return 10;
        }

        @Override
        public String getName() {
            return "RuntimeContextAdvisor";
        }
    }

    public static class SlidingWindowTruncationAdvisor implements CallAdvisor {
        private final int maxHistoryMessages;

        public SlidingWindowTruncationAdvisor(int maxHistoryMessages) {
            this.maxHistoryMessages = Math.max(0, maxHistoryMessages);
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            List<Message> instructions = request.prompt().getInstructions();
            SystemMessage systemMessage = null;
            List<Message> nonSystemMessages = new ArrayList<>();

            for (Message msg : instructions) {
                if (msg instanceof SystemMessage sm && systemMessage == null) {
                    systemMessage = sm;
                } else {
                    nonSystemMessages.add(msg);
                }
            }

            int truncatedCount = 0;
            List<Message> retained = new ArrayList<>();
            if (nonSystemMessages.size() > maxHistoryMessages) {
                truncatedCount = nonSystemMessages.size() - maxHistoryMessages;
                retained.addAll(nonSystemMessages.subList(truncatedCount, nonSystemMessages.size()));
            } else {
                retained.addAll(nonSystemMessages);
            }

            List<Message> finalInstructions = new ArrayList<>();
            if (systemMessage != null) {
                finalInstructions.add(systemMessage);
            }
            finalInstructions.addAll(retained);

            Prompt newPrompt = new Prompt(finalInstructions, request.prompt().getOptions());
            ChatClientRequest mutated = request.mutate().prompt(newPrompt).build();

            ChatClientResponse response = chain.nextCall(mutated);
            return response.mutate()
                    .context(ContextKeys.TRUNCATED_MESSAGE_COUNT, truncatedCount)
                    .build();
        }

        @Override
        public int getOrder() {
            return 20;
        }

        @Override
        public String getName() {
            return "SlidingWindowTruncationAdvisor";
        }
    }

    public static class SystemPromptDynamicAugmentorAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Object tenant = request.context().get("tenantId");
            Object env = request.context().get("env");
            String tags = "";
            if (tenant != null) tags += "[TENANT=" + tenant + "] ";
            if (env != null) tags += "[ENV=" + env + "] ";
            tags = tags.trim();

            ChatClientRequest effective = request;
            if (!tags.isEmpty()) {
                List<Message> instructions = new ArrayList<>(request.prompt().getInstructions());
                if (!instructions.isEmpty() && instructions.get(0) instanceof SystemMessage sm) {
                    String updatedText = sm.getText() + " " + tags;
                    instructions.set(0, new SystemMessage(updatedText));
                    effective = request.mutate().prompt(new Prompt(instructions, request.prompt().getOptions())).build();
                }
            }

            ChatClientResponse response = chain.nextCall(effective);
            return response.mutate().context(ContextKeys.DYNAMIC_SYSTEM_TAGS, tags).build();
        }

        @Override
        public int getOrder() {
            return 25;
        }

        @Override
        public String getName() {
            return "SystemPromptDynamicAugmentorAdvisor";
        }
    }

    // =========================================================================
    // Topic 2: Rate Limiting & Distributed Tracing (Scenarios 4 - 6)
    // =========================================================================

    public static class SlidingWindowRateLimiterAdvisor implements CallAdvisor {
        private final int maxRequests;
        private final long windowMs;
        private final LongSupplier clock;
        private final Deque<Long> requestTimestamps = new ConcurrentLinkedDeque<>();

        public SlidingWindowRateLimiterAdvisor(int maxRequests, long windowMs, LongSupplier clock) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
        }

        @Override
        public synchronized ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            long now = clock.getAsLong();
            while (!requestTimestamps.isEmpty() && (now - requestTimestamps.peekFirst()) >= windowMs) {
                requestTimestamps.pollFirst();
            }

            if (requestTimestamps.size() >= maxRequests) {
                throw new RateLimitExceededException("Rate limit exceeded: " + requestTimestamps.size() + " requests in rolling " + windowMs + "ms window");
            }

            requestTimestamps.addLast(now);
            int remaining = Math.max(0, maxRequests - requestTimestamps.size());

            ChatClientResponse response = chain.nextCall(request);
            return response.mutate()
                    .context(ContextKeys.REMAINING_REQUESTS, remaining)
                    .build();
        }

        @Override
        public int getOrder() {
            return 30;
        }

        @Override
        public String getName() {
            return "SlidingWindowRateLimiterAdvisor";
        }
    }

    public static class TraceContextPropagationAdvisor implements CallAdvisor {
        private final Random random = new Random();

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            String traceparent = (String) request.context().get(ContextKeys.TRACEPARENT);
            if (traceparent == null || traceparent.isBlank()) {
                byte[] traceId = new byte[16];
                byte[] spanId = new byte[8];
                random.nextBytes(traceId);
                random.nextBytes(spanId);
                traceparent = String.format("00-%032x-%016x-01", new java.math.BigInteger(1, traceId), new java.math.BigInteger(1, spanId));
            }

            String tracePrefix = "[traceparent=" + traceparent + "] ";
            ChatClientRequest effectiveRequest = request;
            if (request.prompt().getUserMessage() != null) {
                String original = request.prompt().getUserMessage().getText();
                String prefixed = tracePrefix + (original != null ? original : "");
                List<Message> updated = new ArrayList<>(request.prompt().getInstructions());
                for (int i = 0; i < updated.size(); i++) {
                    if (updated.get(i) instanceof UserMessage) {
                        updated.set(i, new UserMessage(prefixed));
                        break;
                    }
                }
                effectiveRequest = request.mutate()
                        .prompt(new Prompt(updated, request.prompt().getOptions()))
                        .context(ContextKeys.TRACEPARENT, traceparent)
                        .build();
            }

            ChatClientResponse response = chain.nextCall(effectiveRequest);
            return response.mutate()
                    .context(ContextKeys.TRACEPARENT, traceparent)
                    .context(ContextKeys.PROPAGATION_SUCCESS, true)
                    .build();
        }

        @Override
        public int getOrder() {
            return 40;
        }

        @Override
        public String getName() {
            return "TraceContextPropagationAdvisor";
        }
    }

    public static class CompositeRateAndTraceAdvisor implements CallAdvisor {
        private final SlidingWindowRateLimiterAdvisor rateLimiter;
        private final TraceContextPropagationAdvisor tracer;

        public CompositeRateAndTraceAdvisor(int maxRequests, long windowMs, LongSupplier clock) {
            this.rateLimiter = new SlidingWindowRateLimiterAdvisor(maxRequests, windowMs, clock);
            this.tracer = new TraceContextPropagationAdvisor();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            return rateLimiter.adviseCall(request, new CallAdvisorChain() {
                @Override
                public ChatClientResponse nextCall(ChatClientRequest req) {
                    return tracer.adviseCall(req, chain);
                }

                @Override
                public List<CallAdvisor> getCallAdvisors() {
                    return chain.getCallAdvisors();
                }

                @Override
                public CallAdvisorChain copy(CallAdvisor advisor) {
                    return chain.copy(advisor);
                }
            });
        }

        @Override
        public int getOrder() {
            return 35;
        }

        @Override
        public String getName() {
            return "CompositeRateAndTraceAdvisor";
        }
    }

    // =========================================================================
    // Topic 3: Failover Routing & Circuit Breaking (Scenarios 7 - 9)
    // =========================================================================

    public static class ModelFailoverAdvisor implements CallAdvisor {
        private final ChatModel fallbackModel;

        public ModelFailoverAdvisor(ChatModel fallbackModel) {
            this.fallbackModel = Objects.requireNonNull(fallbackModel, "fallbackModel must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            try {
                ChatClientResponse response = chain.nextCall(request);
                return response.mutate().context(ContextKeys.FAILOVER_TRIGGERED, false).build();
            } catch (Exception ex) {
                ChatResponse fallbackChatResponse = fallbackModel.call(request.prompt());
                return ChatClientResponse.builder()
                        .chatResponse(fallbackChatResponse)
                        .context(request.context())
                        .context(ContextKeys.FAILOVER_TRIGGERED, true)
                        .context(ContextKeys.PRIMARY_ERROR, ex.getMessage())
                        .build();
            }
        }

        @Override
        public int getOrder() {
            return 80;
        }

        @Override
        public String getName() {
            return "ModelFailoverAdvisor";
        }
    }

    public static class CircuitBreakerFailoverAdvisor implements CallAdvisor {
        private final ChatModel fallbackModel;
        private final int failureThreshold;
        private int failureCount = 0;
        private boolean tripped = false;

        public CircuitBreakerFailoverAdvisor(ChatModel fallbackModel, int failureThreshold) {
            this.fallbackModel = Objects.requireNonNull(fallbackModel, "fallbackModel must not be null");
            this.failureThreshold = failureThreshold;
        }

        public boolean isTripped() {
            return tripped;
        }

        @Override
        public synchronized ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            if (tripped) {
                ChatResponse fb = fallbackModel.call(request.prompt());
                return ChatClientResponse.builder()
                        .chatResponse(fb)
                        .context(request.context())
                        .context(ContextKeys.CIRCUIT_TRIPPED, true)
                        .context(ContextKeys.FAILOVER_TRIGGERED, true)
                        .build();
            }

            try {
                ChatClientResponse res = chain.nextCall(request);
                failureCount = 0;
                return res.mutate().context(ContextKeys.CIRCUIT_TRIPPED, false).build();
            } catch (Exception ex) {
                failureCount++;
                if (failureCount >= failureThreshold) {
                    tripped = true;
                }
                ChatResponse fb = fallbackModel.call(request.prompt());
                return ChatClientResponse.builder()
                        .chatResponse(fb)
                        .context(request.context())
                        .context(ContextKeys.CIRCUIT_TRIPPED, tripped)
                        .context(ContextKeys.FAILOVER_TRIGGERED, true)
                        .build();
            }
        }

        @Override
        public int getOrder() {
            return 85;
        }

        @Override
        public String getName() {
            return "CircuitBreakerFailoverAdvisor";
        }
    }

    public static class AdaptiveMultiModelRouterAdvisor implements CallAdvisor {
        private final ChatModel secondaryModel;
        private final ChatModel offlineStubModel;

        public AdaptiveMultiModelRouterAdvisor(ChatModel secondaryModel, ChatModel offlineStubModel) {
            this.secondaryModel = Objects.requireNonNull(secondaryModel, "secondaryModel must not be null");
            this.offlineStubModel = Objects.requireNonNull(offlineStubModel, "offlineStubModel must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            try {
                ChatClientResponse res = chain.nextCall(request);
                return res.mutate().context(ContextKeys.CASCADE_FAILOVER_LEVEL, 0).build();
            } catch (Exception e1) {
                try {
                    ChatResponse sec = secondaryModel.call(request.prompt());
                    return ChatClientResponse.builder()
                            .chatResponse(sec)
                            .context(request.context())
                            .context(ContextKeys.CASCADE_FAILOVER_LEVEL, 1)
                            .build();
                } catch (Exception e2) {
                    ChatResponse off = offlineStubModel.call(request.prompt());
                    return ChatClientResponse.builder()
                            .chatResponse(off)
                            .context(request.context())
                            .context(ContextKeys.CASCADE_FAILOVER_LEVEL, 2)
                            .build();
                }
            }
        }

        @Override
        public int getOrder() {
            return 88;
        }

        @Override
        public String getName() {
            return "AdaptiveMultiModelRouterAdvisor";
        }
    }

    // =========================================================================
    // Topic 4: Schema Healing & Hallucination Grounding (Scenarios 10 - 12)
    // =========================================================================

    public static class SchemaSelfHealingAdvisor implements CallAdvisor {
        private boolean isSchemaValid(String text) {
            if (text == null || text.isBlank()) return false;
            String trimmed = text.trim();
            return trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.contains("\"status\"");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);
            String text = "";
            if (response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null) {
                text = response.chatResponse().getResult().getOutput().getText();
            }

            if (isSchemaValid(text)) {
                return response.mutate().context(ContextKeys.SCHEMA_HEALED, false).build();
            }

            List<Message> repairInstructions = new ArrayList<>(request.prompt().getInstructions());
            repairInstructions.add(new SystemMessage("SCHEMA_REPAIR_NOTICE: Previous output failed schema. Respond strictly with JSON containing a 'status' field."));
            Prompt retryPrompt = new Prompt(repairInstructions, request.prompt().getOptions());
            ChatClientRequest retryRequest = request.mutate().prompt(retryPrompt).build();
            ChatClientResponse retryResponse = chain.copy(this).nextCall(retryRequest);
            String retryText = "";
            if (retryResponse.chatResponse() != null && retryResponse.chatResponse().getResult() != null && retryResponse.chatResponse().getResult().getOutput() != null) {
                retryText = retryResponse.chatResponse().getResult().getOutput().getText();
            }

            if (isSchemaValid(retryText)) {
                return retryResponse.mutate().context(ContextKeys.SCHEMA_HEALED, true).build();
            }

            throw new SchemaValidationException("Schema validation failed after automated repair retry");
        }

        @Override
        public int getOrder() {
            return 70;
        }

        @Override
        public String getName() {
            return "SchemaSelfHealingAdvisor";
        }
    }

    public static class GroundingValidationAdvisor implements CallAdvisor {
        private static final Pattern ENTITY_PATTERN = Pattern.compile("\\b(cluster-[a-z0-9-]+|host-[a-z0-9-]+)\\b");
        private final Set<String> allowedEntities;

        public GroundingValidationAdvisor(Set<String> allowedEntities) {
            this.allowedEntities = allowedEntities != null ? Set.copyOf(allowedEntities) : Collections.emptySet();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);
            if (response.chatResponse() == null || response.chatResponse().getResult() == null || response.chatResponse().getResult().getOutput() == null) {
                return response;
            }

            String originalText = response.chatResponse().getResult().getOutput().getText();
            Matcher matcher = ENTITY_PATTERN.matcher(originalText != null ? originalText : "");
            Set<String> ungrounded = new HashSet<>();

            while (matcher.find()) {
                String entity = matcher.group(1);
                if (!allowedEntities.contains(entity)) {
                    ungrounded.add(entity);
                }
            }

            if (!ungrounded.isEmpty()) {
                String modifiedText = "[UNGROUNDED_ENTITY_DETECTED] " + originalText;
                Generation newGen = new Generation(new AssistantMessage(modifiedText));
                ChatResponse modifiedChatResponse = new ChatResponse(List.of(newGen), response.chatResponse().getMetadata());
                return response.mutate()
                        .chatResponse(modifiedChatResponse)
                        .context(ContextKeys.GROUNDING_VIOLATION, true)
                        .context(ContextKeys.UNGROUNDED_ENTITIES, Collections.unmodifiableSet(ungrounded))
                        .build();
            }

            return response.mutate()
                    .context(ContextKeys.GROUNDING_VIOLATION, false)
                    .build();
        }

        @Override
        public int getOrder() {
            return 60;
        }

        @Override
        public String getName() {
            return "GroundingValidationAdvisor";
        }
    }

    public static class FactualityStrictGroundingAdvisor implements CallAdvisor {
        private static final Pattern ENTITY_PATTERN = Pattern.compile("\\b([a-z0-9-]+-cluster|[a-z0-9-]+-host|rogue-[a-z0-9-]+)\\b");
        private final Set<String> allowedEntities;

        public FactualityStrictGroundingAdvisor(Set<String> allowedEntities) {
            this.allowedEntities = allowedEntities != null ? Set.copyOf(allowedEntities) : Collections.emptySet();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);
            String text = response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null ?
                    response.chatResponse().getResult().getOutput().getText() : "";

            Matcher m = ENTITY_PATTERN.matcher(text);
            boolean ungrounded = false;
            while (m.find()) {
                if (!allowedEntities.contains(m.group(1))) {
                    ungrounded = true;
                    break;
                }
            }

            if (ungrounded) {
                Generation gen = new Generation(new AssistantMessage("[REDACTED_UNGROUNDED_FACTS]"));
                ChatResponse modified = new ChatResponse(List.of(gen), response.chatResponse().getMetadata());
                return response.mutate()
                        .chatResponse(modified)
                        .context(ContextKeys.GROUNDING_VIOLATION, true)
                        .build();
            }
            return response;
        }

        @Override
        public int getOrder() {
            return 65;
        }

        @Override
        public String getName() {
            return "FactualityStrictGroundingAdvisor";
        }
    }

    // =========================================================================
    // Topic 5: Transactional Quotas & Production Stacks (Scenarios 13 - 15)
    // =========================================================================

    public static class DynamicTokenQuotaAdvisor implements CallAdvisor {
        private final AtomicInteger remainingQuota;
        private final ToIntFunction<String> tokenEstimator;

        public DynamicTokenQuotaAdvisor(int initialQuota, ToIntFunction<String> tokenEstimator) {
            this.remainingQuota = new AtomicInteger(initialQuota);
            this.tokenEstimator = Objects.requireNonNull(tokenEstimator, "tokenEstimator must not be null");
        }

        public int getRemainingQuota() {
            return remainingQuota.get();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            String userText = request.prompt().getUserMessage() != null ? request.prompt().getUserMessage().getText() : "";
            int estimated = tokenEstimator.applyAsInt(userText);

            if (remainingQuota.get() < estimated) {
                throw new BudgetExceededException("Insufficient token quota: remaining " + remainingQuota.get() + " < estimated " + estimated);
            }

            remainingQuota.addAndGet(-estimated);

            ChatClientResponse response;
            try {
                response = chain.nextCall(request);
            } catch (Exception ex) {
                remainingQuota.addAndGet(estimated);
                throw ex;
            }

            int actualUsed = 0;
            if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
                Usage usage = response.chatResponse().getMetadata().getUsage();
                if (usage != null) {
                    int pt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    int ct = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                    actualUsed = pt + ct;
                }
            }

            int refund = estimated - actualUsed;
            int newQuota = remainingQuota.addAndGet(refund);

            return response.mutate()
                    .context(ContextKeys.REMAINING_QUOTA, newQuota)
                    .build();
        }

        @Override
        public int getOrder() {
            return 50;
        }

        @Override
        public String getName() {
            return "DynamicTokenQuotaAdvisor";
        }
    }

    public static ChatClient createShortCircuitingClient(
            ChatModel chatModel,
            SlidingWindowRateLimiterAdvisor rateLimiter,
            DynamicTokenQuotaAdvisor quotaAdvisor
    ) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(rateLimiter, quotaAdvisor)
                .build();
    }

    public static ChatClient createResilientProductionStack(
            ChatModel primaryModel,
            ChatModel fallbackModel,
            int rateLimitMax,
            long windowMs,
            int initialTokenQuota,
            Set<String> allowedEntities
    ) {
        return ChatClient.builder(primaryModel)
                .defaultAdvisors(
                        new RuntimeContextAdvisor(),
                        new SlidingWindowTruncationAdvisor(10),
                        new SlidingWindowRateLimiterAdvisor(rateLimitMax, windowMs, System::currentTimeMillis),
                        new TraceContextPropagationAdvisor(),
                        new DynamicTokenQuotaAdvisor(initialTokenQuota, text -> 50),
                        new GroundingValidationAdvisor(allowedEntities),
                        new ModelFailoverAdvisor(fallbackModel)
                )
                .build();
    }
}
```
