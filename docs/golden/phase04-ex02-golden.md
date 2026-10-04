# Golden Solution: Phase 04 Exercise 02 (Resilient Guardrails, Failover Routing, and Transactional Token Quotas)

## Overview
This golden solution implements all 10 scenarios of `phase04-ex02-resilient-guardrails`, testing production-grade resiliency, sliding window state management, onion model order propagation, automated schema repair via `chain.copy(this)`, ungrounded hallucination detection, W3C trace context, and transactional token quota reservation with rollback.

Verified: `10 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase04-ex02-resilient-guardrails/src/main/java/phase04/ResilientGuardrailAdvisorsUnderTest.java`

```java
package phase04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import phase04.ResilientAdvisorContracts.*;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice implementing resilient Spring AI CallAdvisor guardrails, failover routing,
 * schema healing, and transactional token quota circuit-breakers:
 * 1. RuntimeContextAdvisor (Order 10)
 * 2. SlidingWindowTruncationAdvisor (Order 20)
 * 3. SlidingWindowRateLimiterAdvisor (Order 30)
 * 4. ModelFailoverAdvisor (Order 80)
 * 5. SchemaSelfHealingAdvisor (Order 70)
 * 6. GroundingValidationAdvisor (Order 60)
 * 7. TraceContextPropagationAdvisor (Order 40)
 * 8. DynamicTokenQuotaAdvisor (Order 50)
 * 9. createShortCircuitingClient
 * 10. createResilientProductionStack
 */
public class ResilientGuardrailAdvisorsUnderTest {

    /**
     * Scenario 1: Pre/Post-Call Dynamic Execution Mode Context Injection.
     * <p>
     * Instructions:
     * - Inspect request context for key "executionMode" (defaults to "LIVE" if absent or null).
     * - If "DRY_RUN" (case-insensitive):
     *     - Prepend "[DRY_RUN] " to the UserMessage text in prompt instructions.
     *     - Mutate request context: set "audit.dryRun" = true, "processedBy" = "RuntimeContextAdvisor".
     *     - Execute chain.nextCall(mutatedRequest).
     *     - Mutate response context: ensure "audit.dryRun" = true and "processedBy" = "RuntimeContextAdvisor".
     * - If not "DRY_RUN" (e.g. "LIVE"):
     *     - Mutate request context: set "audit.dryRun" = false, "processedBy" = "RuntimeContextAdvisor".
     *     - Execute chain.nextCall(mutatedRequest).
     *     - Mutate response context: ensure "audit.dryRun" = false and "processedBy" = "RuntimeContextAdvisor".
     * - getOrder() returns 10.
     * - getName() returns "RuntimeContextAdvisor".
     */
    public static class RuntimeContextAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            Object modeObj = request.context().get(ContextKeys.EXECUTION_MODE);
            String mode = modeObj != null ? modeObj.toString() : "LIVE";
            boolean isDryRun = "DRY_RUN".equalsIgnoreCase(mode);

            ChatClientRequest mutatedRequest;
            if (isDryRun) {
                List<Message> newMessages = new ArrayList<>();
                for (Message m : request.prompt().getInstructions()) {
                    if (m instanceof UserMessage u) {
                        newMessages.add(new UserMessage("[DRY_RUN] " + u.getText()));
                    } else {
                        newMessages.add(m);
                    }
                }
                mutatedRequest = request.mutate()
                        .prompt(new Prompt(newMessages))
                        .context(ContextKeys.AUDIT_DRY_RUN, true)
                        .context(ContextKeys.PROCESSED_BY, "RuntimeContextAdvisor")
                        .build();
            } else {
                mutatedRequest = request.mutate()
                        .context(ContextKeys.AUDIT_DRY_RUN, false)
                        .context(ContextKeys.PROCESSED_BY, "RuntimeContextAdvisor")
                        .build();
            }

            ChatClientResponse response = chain.nextCall(mutatedRequest);
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

    /**
     * Scenario 2: Conversation History Sliding Window Truncation with Pinned SystemMessage.
     * <p>
     * Instructions:
     * - Inspect request.prompt().getInstructions().
     * - Separate messages into SystemMessage (if any) and non-system messages (UserMessage, AssistantMessage, etc.).
     * - If nonSystemMessages.size() > maxHistoryMessages:
     *     - Keep only the last maxHistoryMessages of non-system messages.
     *     - Calculate dropped non-system count: nonSystemMessages.size() - maxHistoryMessages.
     * - Else:
     *     - Keep all non-system messages, dropped count = 0.
     * - Reassemble instructions: SystemMessage(s) pinned at the start (index 0), followed by retained non-system messages.
     * - Mutate request with the reassembled Prompt.
     * - Call chain.nextCall(mutatedRequest).
     * - Mutate response context: set "truncatedMessageCount" = dropped count.
     * - getOrder() returns 20.
     * - getName() returns "SlidingWindowTruncationAdvisor".
     */
    public static class SlidingWindowTruncationAdvisor implements CallAdvisor {
        private final int maxHistoryMessages;

        public SlidingWindowTruncationAdvisor(int maxHistoryMessages) {
            this.maxHistoryMessages = maxHistoryMessages;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            List<Message> original = request.prompt().getInstructions();
            List<Message> systemMessages = new ArrayList<>();
            List<Message> nonSystemMessages = new ArrayList<>();

            for (Message m : original) {
                if (m.getMessageType() == MessageType.SYSTEM) {
                    systemMessages.add(m);
                } else {
                    nonSystemMessages.add(m);
                }
            }

            int truncatedCount = 0;
            List<Message> retained;
            if (nonSystemMessages.size() > maxHistoryMessages) {
                truncatedCount = nonSystemMessages.size() - maxHistoryMessages;
                retained = new ArrayList<>(nonSystemMessages.subList(truncatedCount, nonSystemMessages.size()));
            } else {
                retained = new ArrayList<>(nonSystemMessages);
            }

            List<Message> reassembled = new ArrayList<>(systemMessages);
            reassembled.addAll(retained);

            ChatClientRequest mutated = request.mutate().prompt(new Prompt(reassembled)).build();
            ChatClientResponse response = chain.nextCall(mutated);

            return response.mutate().context(ContextKeys.TRUNCATED_MESSAGE_COUNT, truncatedCount).build();
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

    /**
     * Scenario 3: Rolling Timestamp Deque Sliding Window Rate Limiter.
     * <p>
     * Instructions:
     * - Maintain an internal Deque<Long> of call timestamps.
     * - In adviseCall, get current time: long now = clock.getAsLong().
     * - Synchronized on the timestamps deque:
     *     - Evict all entries where timestamp <= (now - windowMs).
     *     - If deque.size() >= maxRequestsPerWindow:
     *         throw new RateLimitExceededException("Rate limit exceeded: maximum " + maxRequestsPerWindow + " requests allowed in " + windowMs + "ms window");
     *         (Message MUST contain "Rate limit exceeded").
     *     - Add 'now' to the deque.
     *     - Calculate remaining = maxRequestsPerWindow - deque.size().
     * - Call chain.nextCall(request).
     * - Mutate response context: set "remainingRequests" = remaining.
     * - getOrder() returns 30.
     * - getName() returns "SlidingWindowRateLimiterAdvisor".
     */
    public static class SlidingWindowRateLimiterAdvisor implements CallAdvisor {
        private final int maxRequestsPerWindow;
        private final long windowMs;
        private final LongSupplier clock;
        private final Deque<Long> callTimestamps = new ArrayDeque<>();

        public SlidingWindowRateLimiterAdvisor(int maxRequestsPerWindow, long windowMs, LongSupplier clock) {
            this.maxRequestsPerWindow = maxRequestsPerWindow;
            this.windowMs = windowMs;
            this.clock = Objects.requireNonNull(clock);
        }

        public SlidingWindowRateLimiterAdvisor(int maxRequestsPerWindow, long windowMs) {
            this(maxRequestsPerWindow, windowMs, System::currentTimeMillis);
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            int remaining;
            synchronized (callTimestamps) {
                long now = clock.getAsLong();
                long cutoff = now - windowMs;
                while (!callTimestamps.isEmpty() && callTimestamps.peekFirst() <= cutoff) {
                    callTimestamps.pollFirst();
                }
                if (callTimestamps.size() >= maxRequestsPerWindow) {
                    throw new RateLimitExceededException(
                            "Rate limit exceeded: maximum " + maxRequestsPerWindow + " requests allowed in " + windowMs + "ms window"
                    );
                }
                callTimestamps.addLast(now);
                remaining = maxRequestsPerWindow - callTimestamps.size();
            }

            ChatClientResponse response = chain.nextCall(request);
            return response.mutate().context(ContextKeys.REMAINING_REQUESTS, remaining).build();
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

    /**
     * Scenario 4: Downstream Model Resilient Failover Routing.
     * <p>
     * Instructions:
     * - Try executing chain.nextCall(request).
     * - If successful:
     *     - Return response mutated with context "failoverTriggered" = false.
     * - If an Exception is caught:
     *     - Invoke fallbackModel.call(request.prompt()).
     *     - Construct ChatClientResponse with:
     *         .chatResponse(fallbackChatResponse)
     *         .context(request.context())
     *         .context("failoverTriggered", true)
     *         .context("primaryError", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
     *         .build()
     * - getOrder() returns 80.
     * - getName() returns "ModelFailoverAdvisor".
     */
    public static class ModelFailoverAdvisor implements CallAdvisor {
        private final ChatModel fallbackModel;

        public ModelFailoverAdvisor(ChatModel fallbackModel) {
            this.fallbackModel = Objects.requireNonNull(fallbackModel, "fallbackModel must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            try {
                ChatClientResponse response = chain.nextCall(request);
                return response.mutate().context(ContextKeys.FAILOVER_TRIGGERED, false).build();
            } catch (Exception ex) {
                ChatResponse fallbackResponse = fallbackModel.call(request.prompt());
                return ChatClientResponse.builder()
                        .chatResponse(fallbackResponse)
                        .context(request.context())
                        .context(ContextKeys.FAILOVER_TRIGGERED, true)
                        .context(ContextKeys.PRIMARY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
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

    /**
     * Scenario 5: Automated One-Shot Schema Repair Retry Loop.
     * <p>
     * Instructions:
     * - Call chain.nextCall(request).
     * - Extract response text from output.
     * - Check if output contains "\"status\": \"SUCCESS\"":
     *     - If valid: mutate response context with "schemaHealed" = false, return response.
     * - If invalid:
     *     - Attempt at most ONE repair retry using chain.copy(this).nextCall(retryReq).
     *     - Build repair prompt instructions:
     *         - original prompt instructions
     *         - AssistantMessage with the invalid output text
     *         - UserMessage: "SCHEMA_REPAIR_NOTICE: The output was invalid and missing '\"status\": \"SUCCESS\"'. Please return compliant JSON schema."
     *     - Call chain.copy(this).nextCall(retryReq).
     *     - Check retry response text:
     *         - If retry output contains "\"status\": \"SUCCESS\"":
     *             mutate retry response context with "schemaHealed" = true, return response.
     *         - Else:
     *             throw new SchemaValidationException("Schema validation failed: Assistant output does not meet required schema after retry");
     *             (Message MUST contain "Schema validation failed").
     * - getOrder() returns 70.
     * - getName() returns "SchemaSelfHealingAdvisor".
     */
    public static class SchemaSelfHealingAdvisor implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            ChatClientResponse response = chain.nextCall(request);
            String text = extractText(response);

            if (isValidSchema(text)) {
                return response.mutate().context(ContextKeys.SCHEMA_HEALED, false).build();
            }

            // One-shot repair attempt
            List<Message> repairMessages = new ArrayList<>(request.prompt().getInstructions());
            repairMessages.add(new AssistantMessage(text != null ? text : ""));
            repairMessages.add(new UserMessage("SCHEMA_REPAIR_NOTICE: The output was invalid and missing '\"status\": \"SUCCESS\"'. Please return compliant JSON schema."));

            ChatClientRequest retryReq = request.mutate().prompt(new Prompt(repairMessages)).build();
            ChatClientResponse retryResponse = chain.copy(this).nextCall(retryReq);
            String retryText = extractText(retryResponse);

            if (isValidSchema(retryText)) {
                return retryResponse.mutate().context(ContextKeys.SCHEMA_HEALED, true).build();
            }

            throw new SchemaValidationException("Schema validation failed: Assistant output does not meet required schema after retry");
        }

        private boolean isValidSchema(String text) {
            return text != null && text.contains("\"status\": \"SUCCESS\"");
        }

        private String extractText(ChatClientResponse resp) {
            if (resp == null || resp.chatResponse() == null ||
                    resp.chatResponse().getResult() == null ||
                    resp.chatResponse().getResult().getOutput() == null) {
                return null;
            }
            return resp.chatResponse().getResult().getOutput().getText();
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

    /**
     * Scenario 6: Entity Grounding & Hallucination Detector.
     * <p>
     * Instructions:
     * - Call chain.nextCall(request).
     * - Extract assistant output text.
     * - Match entity tokens using entityPattern (default matches \b(host-[a-z0-9-]+|cluster-[a-z0-9-]+)\b).
     * - Any entity found that is NOT in allowedEntities is ungrounded.
     * - If ungrounded entities are detected:
     *     - Prepend "[UNGROUNDED_ENTITY_DETECTED] " to the assistant output text.
     *     - Recreate ChatResponse wrapping the updated Generation with the modified text.
     *     - Mutate response with new ChatResponse and context:
     *         "groundingViolation" = true
     *         "ungroundedEntities" = Set of ungrounded entities
     * - If all entities are grounded (or none matched):
     *     - Mutate response context: "groundingViolation" = false.
     * - getOrder() returns 60.
     * - getName() returns "GroundingValidationAdvisor".
     */
    public static class GroundingValidationAdvisor implements CallAdvisor {
        private final Set<String> allowedEntities;
        private final Pattern entityPattern;

        public GroundingValidationAdvisor(Set<String> allowedEntities, Pattern entityPattern) {
            this.allowedEntities = Objects.requireNonNull(allowedEntities);
            this.entityPattern = Objects.requireNonNull(entityPattern);
        }

        public GroundingValidationAdvisor(Set<String> allowedEntities) {
            this(allowedEntities, Pattern.compile("\\b(host-[a-z0-9-]+|cluster-[a-z0-9-]+)\\b"));
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            ChatClientResponse response = chain.nextCall(request);
            if (response == null || response.chatResponse() == null ||
                    response.chatResponse().getResult() == null ||
                    response.chatResponse().getResult().getOutput() == null) {
                return response;
            }

            String text = response.chatResponse().getResult().getOutput().getText();
            var matcher = entityPattern.matcher(text != null ? text : "");
            Set<String> ungrounded = new LinkedHashSet<>();
            while (matcher.find()) {
                String token = matcher.group();
                if (!allowedEntities.contains(token)) {
                    ungrounded.add(token);
                }
            }

            if (!ungrounded.isEmpty()) {
                String prefixed = "[UNGROUNDED_ENTITY_DETECTED] " + text;
                ChatResponse newChatResponse = new ChatResponse(
                        List.of(new Generation(new AssistantMessage(prefixed))),
                        response.chatResponse().getMetadata()
                );
                return response.mutate()
                        .chatResponse(newChatResponse)
                        .context(ContextKeys.GROUNDING_VIOLATION, true)
                        .context(ContextKeys.UNGROUNDED_ENTITIES, ungrounded)
                        .build();
            }

            return response.mutate().context(ContextKeys.GROUNDING_VIOLATION, false).build();
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

    /**
     * Scenario 7: W3C Distributed Traceparent Propagation Advisor.
     * <p>
     * Instructions:
     * - Check request context for key "traceparent".
     * - If present and non-blank: use it.
     * - If absent or blank: generate valid W3C traceparent ("00-" + 32hex + "-" + 16hex + "-01").
     * - Inject traceparent notice by prepending "[traceparent=" + traceparent + "] " to UserMessage text.
     * - Mutate request with updated Prompt and context "traceparent" = traceparent.
     * - Call chain.nextCall(mutatedRequest).
     * - Mutate response context: set "traceparent" = traceparent, "propagationSuccess" = true.
     * - getOrder() returns 40.
     * - getName() returns "TraceContextPropagationAdvisor".
     */
    public static class TraceContextPropagationAdvisor implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            Object existing = request.context().get(ContextKeys.TRACEPARENT);
            String traceparent;
            if (existing != null && !existing.toString().isBlank()) {
                traceparent = existing.toString();
            } else {
                traceparent = generateTraceparent();
            }

            List<Message> updated = new ArrayList<>();
            for (Message m : request.prompt().getInstructions()) {
                if (m instanceof UserMessage u) {
                    updated.add(new UserMessage("[traceparent=" + traceparent + "] " + u.getText()));
                } else {
                    updated.add(m);
                }
            }

            ChatClientRequest mutated = request.mutate()
                    .prompt(new Prompt(updated))
                    .context(ContextKeys.TRACEPARENT, traceparent)
                    .build();

            ChatClientResponse response = chain.nextCall(mutated);
            return response.mutate()
                    .context(ContextKeys.TRACEPARENT, traceparent)
                    .context(ContextKeys.PROPAGATION_SUCCESS, true)
                    .build();
        }

        private String generateTraceparent() {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            return "00-" + traceId.toLowerCase() + "-" + spanId.toLowerCase() + "-01";
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

    /**
     * Scenario 8: Dynamic Pre-Allocation Token Quota Advisor with Transactional Rollback.
     * <p>
     * Instructions:
     * - Estimate required tokens: tokenEstimator.applyAsInt(userMessageText).
     * - In synchronized block:
     *     - If remainingQuota < estimatedTokens:
     *         throw new BudgetExceededException("Insufficient token quota: required " + estimatedTokens + ", remaining " + remainingQuota);
     *         (Message MUST contain "Insufficient token quota").
     *     - Deduct estimatedTokens: remainingQuota -= estimatedTokens.
     * - In try block:
     *     - Execute chain.nextCall(request).
     *     - Extract actual total tokens from response metadata usage (if available, else fallback to estimatedTokens).
     *     - In synchronized block: reconcile remainingQuota = remainingQuota + estimatedTokens - actualTokens.
     *     - Mutate response context: set "remainingQuota" = remainingQuota.
     * - In catch (RuntimeException ex):
     *     - In synchronized block: rollback pre-allocation: remainingQuota += estimatedTokens.
     *     - Rethrow ex.
     * - getOrder() returns 50.
     * - getName() returns "DynamicTokenQuotaAdvisor".
     */
    public static class DynamicTokenQuotaAdvisor implements CallAdvisor {
        private int remainingQuota;
        private final ToIntFunction<String> tokenEstimator;

        public DynamicTokenQuotaAdvisor(int initialQuota, ToIntFunction<String> tokenEstimator) {
            this.remainingQuota = initialQuota;
            this.tokenEstimator = Objects.requireNonNull(tokenEstimator);
        }

        public DynamicTokenQuotaAdvisor(int initialQuota) {
            this(initialQuota, text -> Math.max(10, text != null ? text.length() / 4 : 10));
        }

        public synchronized int getRemainingQuota() {
            return remainingQuota;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            Objects.requireNonNull(request, "request must not be null");
            Objects.requireNonNull(chain, "chain must not be null");

            String userText = request.prompt().getUserMessage() != null ? request.prompt().getUserMessage().getText() : "";
            int estimated = tokenEstimator.applyAsInt(userText);

            synchronized (this) {
                if (remainingQuota < estimated) {
                    throw new BudgetExceededException(
                            "Insufficient token quota: required " + estimated + ", remaining " + remainingQuota
                    );
                }
                remainingQuota -= estimated;
            }

            try {
                ChatClientResponse response = chain.nextCall(request);
                int actual = estimated;
                if (response.chatResponse() != null &&
                        response.chatResponse().getMetadata() != null &&
                        response.chatResponse().getMetadata().getUsage() != null &&
                        response.chatResponse().getMetadata().getUsage().getTotalTokens() != null) {
                    actual = response.chatResponse().getMetadata().getUsage().getTotalTokens();
                }

                synchronized (this) {
                    remainingQuota = remainingQuota + estimated - actual;
                }
                return response.mutate().context(ContextKeys.REMAINING_QUOTA, getRemainingQuota()).build();
            } catch (RuntimeException ex) {
                synchronized (this) {
                    remainingQuota += estimated;
                }
                throw ex;
            }
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

    /**
     * Scenario 9: Composite Short-Circuiting Client Factory.
     * <p>
     * Assembles a ChatClient with rateLimiter and quotaAdvisor advisors.
     */
    public static ChatClient createShortCircuitingClient(ChatModel model, CallAdvisor rateLimiter, CallAdvisor quotaAdvisor) {
        return ChatClient.builder(model)
                .defaultAdvisors(rateLimiter, quotaAdvisor)
                .build();
    }

    /**
     * Scenario 10: Full Autonomous Resilient Production Stack Factory.
     * <p>
     * Combines all resilient advisors in proper onion order:
     * - Order 10: RuntimeContextAdvisor
     * - Order 30: SlidingWindowRateLimiterAdvisor(rateLimitMax, windowMs)
     * - Order 40: TraceContextPropagationAdvisor
     * - Order 50: DynamicTokenQuotaAdvisor(initialTokenQuota)
     * - Order 60: GroundingValidationAdvisor(allowedEntities)
     * - Order 80: ModelFailoverAdvisor(fallbackModel)
     */
    public static ChatClient createResilientProductionStack(
            ChatModel primaryModel,
            ChatModel fallbackModel,
            int rateLimitMax,
            long windowMs,
            int initialTokenQuota,
            Set<String> allowedEntities
    ) {
        RuntimeContextAdvisor runtimeContext = new RuntimeContextAdvisor();
        SlidingWindowRateLimiterAdvisor rateLimiter = new SlidingWindowRateLimiterAdvisor(rateLimitMax, windowMs);
        TraceContextPropagationAdvisor traceContext = new TraceContextPropagationAdvisor();
        DynamicTokenQuotaAdvisor quotaAdvisor = new DynamicTokenQuotaAdvisor(initialTokenQuota);
        GroundingValidationAdvisor groundingAdvisor = new GroundingValidationAdvisor(allowedEntities);
        ModelFailoverAdvisor failoverAdvisor = new ModelFailoverAdvisor(fallbackModel);

        return ChatClient.builder(primaryModel)
                .defaultAdvisors(
                        runtimeContext,
                        rateLimiter,
                        traceContext,
                        quotaAdvisor,
                        groundingAdvisor,
                        failoverAdvisor
                )
                .build();
    }
}
```
