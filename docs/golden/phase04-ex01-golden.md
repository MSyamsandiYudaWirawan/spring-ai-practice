# Golden Solution: Phase 04 Exercise 01 (Guardrail Advisors & Token Budget Circuit-Breakers)

## Overview
This golden solution implements all 15 repetitive drills of `phase04-ex01-guardrail-advisors`, organized into 5 core topics (3 drills each) following the Foundation $\to$ Variation $\to$ Real-World Composition pattern:

1. **Topic 1: Pre-Call Security & Policy Injections**
   - Scenario 01: `KeywordGuardrailAdvisor` (Pre-Call Prompt Security with `PromptSecurityException`)
   - Scenario 02: `RegexPatternGuardrailAdvisor` (Pre-Call Pattern Blocking: SQL Injection & Command Escapes)
   - Scenario 03: `SystemPromptEnforcingAdvisor` (Pre-Call Policy Injection: Prepending mandatory SystemMessage)
2. **Topic 2: Observation, Auditing & Context Propagation**
   - Scenario 04: `AuditLoggingAdvisor` (Pre/Post-Call Context & Audit Trail with Correlation ID and Latency)
   - Scenario 05: `OrderTrackingAdvisor` (Advisor Precedence & Onion-Model Execution Ordering)
   - Scenario 06: `ContextEnrichmentAdvisor` (Metadata Context Propagation: Merging tenant/env into `request.context()`)
3. **Topic 3: Token Accumulation & Budget Circuit Breakers**
   - Scenario 07: `TokenBudgetAdvisor` (Post-Call Usage Accumulator with Defensive Null Unboxing)
   - Scenario 08: `CostCircuitBreakerAdvisor` (Post-Call Dollar Budget Cap throwing `BudgetExceededException`)
   - Scenario 09: `MaxTurnsAdvisor` (Conversation Session Turn Limiter)
4. **Topic 4: Post-Call Sanitization & Latency Bounds**
   - Scenario 10: `LatencyGuardrailAdvisor` (Execution Wall-Clock Timeout throwing `LatencyTimeoutException`)
   - Scenario 11: `PiiMaskingAdvisor` (Post-Call Response Redaction: Email & SSN Sanitization)
   - Scenario 12: `SecretKeyRedactionAdvisor` (API Key & Bearer Token Redaction: Sanitizing JWT / AWS keys)
5. **Topic 5: Multi-Advisor Composition & Production Stacks**
   - Scenario 13: `ChatClientAdvisorRegistration` (Default Advisors vs Call-Level Advisors Precedence)
   - Scenario 14: `DynamicBypassGuardrailAdvisor` (Role-Based Guardrail Bypass via Context inspection)
   - Scenario 15: `ComprehensiveSagaGuardrailSuite` (Full Production Guardrail Stack Assembly)

Verified: `15 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase04-ex01-guardrail-advisors/src/main/java/phase04/GuardrailAdvisorsUnderTest.java`

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
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import phase04.AdvisorContracts.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service under test — Phase 04 Exercise 01:
 * Guardrails, Advisors, and Token Budget Circuit-Breakers (15 Scenarios).
 */
public class GuardrailAdvisorsUnderTest {

    // =========================================================================
    // Topic 1: Pre-Call Security & Policy Injections (Scenarios 1 - 3)
    // =========================================================================

    public static class KeywordGuardrailAdvisor implements CallAdvisor {
        private final List<String> blockedKeywords;

        public KeywordGuardrailAdvisor(List<String> blockedKeywords) {
            this.blockedKeywords = blockedKeywords != null ? List.copyOf(blockedKeywords) : List.of();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            for (Message msg : request.prompt().getInstructions()) {
                String text = msg.getText();
                if (text != null) {
                    for (String blocked : blockedKeywords) {
                        if (text.toUpperCase().contains(blocked.toUpperCase())) {
                            throw new PromptSecurityException("Blocked keyword detected: " + blocked);
                        }
                    }
                }
            }
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE + 50;
        }

        @Override
        public String getName() {
            return "KeywordGuardrailAdvisor";
        }
    }

    public static class RegexPatternGuardrailAdvisor implements CallAdvisor {
        private final List<Pattern> patterns;

        public RegexPatternGuardrailAdvisor(List<String> regexPatterns) {
            this.patterns = regexPatterns != null ?
                    regexPatterns.stream().map(Pattern::compile).toList() : List.of();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            for (Message msg : request.prompt().getInstructions()) {
                String text = msg.getText();
                if (text != null) {
                    for (Pattern p : patterns) {
                        if (p.matcher(text).matches() || p.matcher(text).find()) {
                            throw new PromptSecurityException("Pattern violation detected: " + p.pattern());
                        }
                    }
                }
            }
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE + 60;
        }

        @Override
        public String getName() {
            return "RegexPatternGuardrailAdvisor";
        }
    }

    public static class SystemPromptEnforcingAdvisor implements CallAdvisor {
        private final String enforcedPolicy;

        public SystemPromptEnforcingAdvisor(String enforcedPolicy) {
            this.enforcedPolicy = Objects.requireNonNull(enforcedPolicy, "enforcedPolicy must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            boolean policyPresent = request.prompt().getInstructions().stream()
                    .anyMatch(msg -> msg.getText() != null && msg.getText().contains(enforcedPolicy));

            if (!policyPresent) {
                List<Message> updated = new ArrayList<>();
                updated.add(new SystemMessage(enforcedPolicy));
                updated.addAll(request.prompt().getInstructions());
                Prompt newPrompt = new Prompt(updated, request.prompt().getOptions());
                ChatClientRequest mutated = request.mutate().prompt(newPrompt).build();
                return chain.nextCall(mutated);
            }
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 50;
        }

        @Override
        public String getName() {
            return "SystemPromptEnforcingAdvisor";
        }
    }

    // =========================================================================
    // Topic 2: Observation, Auditing & Context Propagation (Scenarios 4 - 6)
    // =========================================================================

    public static class AuditLoggingAdvisor implements CallAdvisor {
        private final List<AdvisorAuditRecord> auditRecords = new CopyOnWriteArrayList<>();

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            String correlationId = UUID.randomUUID().toString();
            ChatClientRequest mutated = request.mutate()
                    .context("correlationId", correlationId)
                    .build();

            long start = System.currentTimeMillis();
            ChatClientResponse response = chain.nextCall(mutated);
            long latency = Math.max(0, System.currentTimeMillis() - start);

            String promptContent = request.prompt().getInstructions().stream()
                    .map(Message::getText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));

            String responseContent = "";
            if (response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null) {
                responseContent = response.chatResponse().getResult().getOutput().getText();
            }

            auditRecords.add(new AdvisorAuditRecord(getName(), promptContent, responseContent, correlationId, latency, System.currentTimeMillis()));
            return response;
        }

        public List<AdvisorAuditRecord> getAuditRecords() {
            return Collections.unmodifiableList(auditRecords);
        }

        @Override
        public int getOrder() {
            return 200;
        }

        @Override
        public String getName() {
            return "AuditLoggingAdvisor";
        }
    }

    public static class OrderTrackingAdvisor implements CallAdvisor {
        private final String advisorName;
        private final int order;
        private final List<ExecutionOrderTrace> traces;

        public OrderTrackingAdvisor(String advisorName, int order, List<ExecutionOrderTrace> traces) {
            this.advisorName = Objects.requireNonNull(advisorName, "advisorName must not be null");
            this.order = order;
            this.traces = Objects.requireNonNull(traces, "traces must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            traces.add(new ExecutionOrderTrace(advisorName, "PRE_CALL", System.currentTimeMillis()));
            ChatClientResponse response = chain.nextCall(request);
            traces.add(new ExecutionOrderTrace(advisorName, "POST_CALL", System.currentTimeMillis()));
            return response;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public String getName() {
            return advisorName;
        }
    }

    public static class ContextEnrichmentAdvisor implements CallAdvisor {
        private final Map<String, Object> metadata;
        private int injectedCount = 0;
        private Map<String, Object> lastEnrichedContext = Collections.emptyMap();

        public ContextEnrichmentAdvisor(Map<String, Object> metadata) {
            this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        public int getInjectedCount() {
            return injectedCount;
        }

        public Map<String, Object> getLastEnrichedContext() {
            return lastEnrichedContext;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            injectedCount++;
            Map<String, Object> merged = new HashMap<>(request.context());
            merged.putAll(metadata);
            this.lastEnrichedContext = Collections.unmodifiableMap(merged);

            ChatClientRequest mutated = request.mutate().context(merged).build();
            return chain.nextCall(mutated);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public String getName() {
            return "ContextEnrichmentAdvisor";
        }
    }

    // =========================================================================
    // Topic 3: Token Accumulation & Budget Circuit Breakers (Scenarios 7 - 9)
    // =========================================================================

    public static class TokenBudgetAdvisor implements CallAdvisor {
        private final AtomicInteger promptTokens = new AtomicInteger(0);
        private final AtomicInteger completionTokens = new AtomicInteger(0);
        private final AtomicInteger totalTokens = new AtomicInteger(0);
        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);
            callCount.incrementAndGet();

            if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
                Usage usage = response.chatResponse().getMetadata().getUsage();
                if (usage != null) {
                    int pt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    int ct = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                    promptTokens.addAndGet(pt);
                    completionTokens.addAndGet(ct);
                    totalTokens.addAndGet(pt + ct);
                }
            }
            return response;
        }

        public TokenUsageSummary getSummary() {
            return new TokenUsageSummary(promptTokens.get(), completionTokens.get(), totalTokens.get(), callCount.get());
        }

        @Override
        public int getOrder() {
            return 150;
        }

        @Override
        public String getName() {
            return "TokenBudgetAdvisor";
        }
    }

    public static class CostCircuitBreakerAdvisor implements CallAdvisor {
        private final CostBudgetConfig config;
        private double cumulativeCostUsd = 0.0;

        public CostCircuitBreakerAdvisor(CostBudgetConfig config) {
            this.config = Objects.requireNonNull(config, "config must not be null");
        }

        public synchronized double getCumulativeCostUsd() {
            return cumulativeCostUsd;
        }

        @Override
        public synchronized ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);

            if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
                Usage usage = response.chatResponse().getMetadata().getUsage();
                if (usage != null) {
                    int pt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    int ct = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                    double callCost = (pt * config.priceInPerMtok() + ct * config.priceOutPerMtok()) / 1_000_000.0;
                    cumulativeCostUsd += callCost;

                    if (cumulativeCostUsd > config.maxCostUsd()) {
                        throw new BudgetExceededException(String.format(
                                "Budget limit exceeded: cumulative cost $%.6f exceeds maximum cap $%.6f",
                                cumulativeCostUsd, config.maxCostUsd()
                        ));
                    }
                }
            }
            return response;
        }

        @Override
        public int getOrder() {
            return 160;
        }

        @Override
        public String getName() {
            return "CostCircuitBreakerAdvisor";
        }
    }

    public static class MaxTurnsAdvisor implements CallAdvisor {
        private final int maxTurnsAllowed;
        private final AtomicInteger turnCount = new AtomicInteger(0);

        public MaxTurnsAdvisor(int maxTurnsAllowed) {
            this.maxTurnsAllowed = maxTurnsAllowed;
        }

        public int getTurnCount() {
            return turnCount.get();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            int currentTurn = turnCount.incrementAndGet();
            if (currentTurn > maxTurnsAllowed) {
                throw new BudgetExceededException("Maximum conversation turns exceeded: " + currentTurn + " > " + maxTurnsAllowed);
            }
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 170;
        }

        @Override
        public String getName() {
            return "MaxTurnsAdvisor";
        }
    }

    // =========================================================================
    // Topic 4: Post-Call Sanitization & Latency Bounds (Scenarios 10 - 12)
    // =========================================================================

    public static class LatencyGuardrailAdvisor implements CallAdvisor {
        private final long maxLatencyMs;

        public LatencyGuardrailAdvisor(long maxLatencyMs) {
            if (maxLatencyMs <= 0) throw new IllegalArgumentException("maxLatencyMs must be > 0");
            this.maxLatencyMs = maxLatencyMs;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            long start = System.currentTimeMillis();
            ChatClientResponse response = chain.nextCall(request);
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > maxLatencyMs) {
                throw new LatencyTimeoutException(String.format("Call latency %dms exceeded limit %dms", elapsed, maxLatencyMs));
            }
            return response;
        }

        @Override
        public int getOrder() {
            return 180;
        }

        @Override
        public String getName() {
            return "LatencyGuardrailAdvisor";
        }
    }

    public static class PiiMaskingAdvisor implements CallAdvisor {
        private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);

            if (response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null) {
                String originalText = response.chatResponse().getResult().getOutput().getText();
                if (originalText != null) {
                    String masked = EMAIL_PATTERN.matcher(originalText).replaceAll("[REDACTED_EMAIL]");
                    masked = SSN_PATTERN.matcher(masked).replaceAll("[REDACTED_SSN]");

                    if (!masked.equals(originalText)) {
                        Generation newGen = new Generation(new AssistantMessage(masked));
                        ChatResponse newChatResponse = new ChatResponse(List.of(newGen), response.chatResponse().getMetadata());
                        return response.mutate().chatResponse(newChatResponse).build();
                    }
                }
            }
            return response;
        }

        @Override
        public int getOrder() {
            return 190;
        }

        @Override
        public String getName() {
            return "PiiMaskingAdvisor";
        }
    }

    public static class SecretKeyRedactionAdvisor implements CallAdvisor {
        private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9_\\-\\.]{20,}");
        private static final Pattern AWS_KEY_PATTERN = Pattern.compile("AKIA[0-9A-Z]{16}");

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);

            if (response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null) {
                String text = response.chatResponse().getResult().getOutput().getText();
                if (text != null) {
                    String redacted = BEARER_PATTERN.matcher(text).replaceAll("[REDACTED_SECRET]");
                    redacted = AWS_KEY_PATTERN.matcher(redacted).replaceAll("[REDACTED_SECRET]");

                    if (!redacted.equals(text)) {
                        Generation newGen = new Generation(new AssistantMessage(redacted));
                        ChatResponse newChatResponse = new ChatResponse(List.of(newGen), response.chatResponse().getMetadata());
                        return response.mutate().chatResponse(newChatResponse).build();
                    }
                }
            }
            return response;
        }

        @Override
        public int getOrder() {
            return 195;
        }

        @Override
        public String getName() {
            return "SecretKeyRedactionAdvisor";
        }
    }

    // =========================================================================
    // Topic 5: Multi-Advisor Composition & Production Stacks (Scenarios 13 - 15)
    // =========================================================================

    public ChatClient createGuardedChatClient(ChatModel chatModel, CallAdvisor defaultAdvisor) {
        Objects.requireNonNull(chatModel, "chatModel must not be null");
        Objects.requireNonNull(defaultAdvisor, "defaultAdvisor must not be null");
        return ChatClient.builder(chatModel)
                .defaultAdvisors(defaultAdvisor)
                .build();
    }

    public static class DynamicBypassGuardrailAdvisor implements CallAdvisor {
        private final CallAdvisor innerAdvisor;

        public DynamicBypassGuardrailAdvisor(CallAdvisor innerAdvisor) {
            this.innerAdvisor = Objects.requireNonNull(innerAdvisor, "innerAdvisor must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            if ("ADMIN".equals(request.context().get("role"))) {
                return chain.nextCall(request);
            }
            return innerAdvisor.adviseCall(request, chain);
        }

        @Override
        public int getOrder() {
            return innerAdvisor.getOrder();
        }

        @Override
        public String getName() {
            return "DynamicBypassGuardrailAdvisor(" + innerAdvisor.getName() + ")";
        }
    }

    public ChatClient createProductionDiagnosticStack(
            ChatModel chatModel,
            List<String> blockedKeywords,
            String enforcedPolicy,
            CostBudgetConfig budgetConfig,
            TokenBudgetAdvisor tokenAdvisor,
            AuditLoggingAdvisor auditAdvisor
    ) {
        Objects.requireNonNull(chatModel, "chatModel must not be null");
        Objects.requireNonNull(budgetConfig, "budgetConfig must not be null");
        Objects.requireNonNull(tokenAdvisor, "tokenAdvisor must not be null");
        Objects.requireNonNull(auditAdvisor, "auditAdvisor must not be null");

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new KeywordGuardrailAdvisor(blockedKeywords),
                        new SystemPromptEnforcingAdvisor(enforcedPolicy),
                        tokenAdvisor,
                        new CostCircuitBreakerAdvisor(budgetConfig),
                        new PiiMaskingAdvisor(),
                        auditAdvisor
                )
                .build();
    }
}
```
