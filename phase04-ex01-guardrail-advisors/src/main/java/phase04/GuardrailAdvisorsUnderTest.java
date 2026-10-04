package phase04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import phase04.AdvisorContracts.AdvisorAuditRecord;
import phase04.AdvisorContracts.CostBudgetConfig;
import phase04.AdvisorContracts.ExecutionOrderTrace;
import phase04.AdvisorContracts.TokenUsageSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice implementing Spring AI CallAdvisor guardrails, budget circuit-breakers, and client integration:
 * 1. AuditLoggingAdvisor
 * 2. KeywordGuardrailAdvisor
 * 3. SystemPromptEnforcingAdvisor
 * 4. TokenBudgetAdvisor
 * 5. CostCircuitBreakerAdvisor
 * 6. LatencyGuardrailAdvisor
 * 7. PiiMaskingAdvisor
 * 8. OrderTrackingAdvisor
 * 9. createGuardedChatClient
 * 10. createProductionDiagnosticStack
 */
public class GuardrailAdvisorsUnderTest {

    /**
     * Scenario 1: Pre/Post-Call Inspection and Context Enrichment Advisor.
     * <p>
     * Instructions:
     * - Generate a UUID string for correlationId: UUID.randomUUID().toString()
     * - Mutate the request: request.mutate().context("correlationId", correlationId).build()
     * - Measure execution latency around chain.nextCall(mutatedRequest)
     * - Extract prompt text from instructions and response text from response.chatResponse()
     * - Add new AdvisorAuditRecord(getName(), promptText, responseText, correlationId, latencyMs, System.currentTimeMillis())
     * - getOrder() returns 200
     */
    public static class AuditLoggingAdvisor implements CallAdvisor {
        private final List<AdvisorAuditRecord> auditRecords = new ArrayList<>();

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 1): Bypasses correlationId enrichment and audit recording
            return chain.nextCall(request);
        }

        public List<AdvisorAuditRecord> getAuditRecords() {
            return Collections.unmodifiableList(auditRecords);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "AuditLoggingAdvisor";
        }
    }

    /**
     * Scenario 2: Pre-Call Prompt Security Circuit-Breaker Advisor.
     * <p>
     * Instructions:
     * - Inspect every message in request.prompt().getInstructions()
     * - If any message text contains any blockedKeyword (case-insensitive):
     *     throw new PromptSecurityException("Blocked keyword detected: " + blockedKeyword)
     * - Otherwise: return chain.nextCall(request)
     * - getOrder() returns Ordered.HIGHEST_PRECEDENCE
     */
    public static class KeywordGuardrailAdvisor implements CallAdvisor {
        private final List<String> blockedKeywords;

        public KeywordGuardrailAdvisor(List<String> blockedKeywords) {
            this.blockedKeywords = blockedKeywords;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 2): Bypasses blocked keyword checking
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "KeywordGuardrailAdvisor";
        }
    }

    /**
     * Scenario 3: Pre-Call Mandatory System Policy Injector Advisor.
     * <p>
     * Instructions:
     * - Inspect messages in request.prompt().getInstructions()
     * - If no message contains enforcedPolicy:
     *     prepend new SystemMessage(enforcedPolicy) at index 0 of the instructions
     * - Mutate request with the updated Prompt containing the policy
     * - Return chain.nextCall(mutatedRequest)
     * - getOrder() returns 50
     */
    public static class SystemPromptEnforcingAdvisor implements CallAdvisor {
        private final String enforcedPolicy;

        public SystemPromptEnforcingAdvisor(String enforcedPolicy) {
            this.enforcedPolicy = enforcedPolicy;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 3): Dispatches prompt without injecting mandatory system policy
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "SystemPromptEnforcingAdvisor";
        }
    }

    /**
     * Scenario 4: Post-Call Cumulative Token Usage Tracking Advisor.
     * <p>
     * Instructions:
     * - Dispatch chain.nextCall(request)
     * - Extract Usage from response.chatResponse().getMetadata().getUsage()
     * - Defensively handle nullable tokens (treat null as 0)
     * - Accumulate promptTokens, completionTokens, and totalTokens
     * - Increment callCount
     * - getSummary() returns TokenUsageSummary(prompt, completion, total, callCount)
     * - getOrder() returns 150
     */
    public static class TokenBudgetAdvisor implements CallAdvisor {
        private int callCount = 0;
        private long promptTokens = 0;
        private long completionTokens = 0;
        private long totalTokens = 0;

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 4): Bypasses token usage tracking
            return chain.nextCall(request);
        }

        public TokenUsageSummary getSummary() {
            return new TokenUsageSummary((int) promptTokens, (int) completionTokens, (int) totalTokens, callCount);
        }

        public void reset() {
            this.callCount = 0;
            this.promptTokens = 0;
            this.completionTokens = 0;
            this.totalTokens = 0;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "TokenBudgetAdvisor";
        }
    }

    /**
     * Scenario 5: Dollar Budget Accumulator and Circuit-Breaker Advisor.
     * <p>
     * Instructions:
     * - Dispatch chain.nextCall(request)
     * - Calculate call cost: (promptTokens * priceInPerMtok + completionTokens * priceOutPerMtok) / 1_000_000.0
     * - Accumulate into cumulativeCostUsd
     * - If cumulativeCostUsd > config.maxCostUsd():
     *     throw new BudgetExceededException("Budget limit exceeded: cost $" + cumulativeCostUsd + " exceeds cap $" + config.maxCostUsd())
     *     (Message MUST contain "Budget limit exceeded")
     * - getCumulativeCostUsd() returns cumulativeCostUsd
     * - getOrder() returns 160
     */
    public static class CostCircuitBreakerAdvisor implements CallAdvisor {
        private final CostBudgetConfig config;
        private double cumulativeCostUsd = 0.0;

        public CostCircuitBreakerAdvisor(CostBudgetConfig config) {
            this.config = config;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 5): Bypasses dollar cost accumulation and budget circuit-breaker
            return chain.nextCall(request);
        }

        public double getCumulativeCostUsd() {
            return this.cumulativeCostUsd;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "CostCircuitBreakerAdvisor";
        }
    }

    /**
     * Scenario 6: Wall-Clock Execution Latency Guardrail Advisor.
     * <p>
     * Instructions:
     * - Measure elapsed time across chain.nextCall(request)
     * - If elapsed > maxLatencyMs:
     *     throw new LatencyTimeoutException("Call latency " + elapsed + "ms exceeded limit " + maxLatencyMs + "ms")
     *     (Message MUST contain "latency")
     * - getOrder() returns 170
     */
    public static class LatencyGuardrailAdvisor implements CallAdvisor {
        private final long maxLatencyMs;

        public LatencyGuardrailAdvisor(long maxLatencyMs) {
            this.maxLatencyMs = maxLatencyMs;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 6): Bypasses latency measurement and timeout exception
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "LatencyGuardrailAdvisor";
        }
    }

    /**
     * Scenario 7: Post-Call PII Masking & Redaction Advisor.
     * <p>
     * Instructions:
     * - Dispatch chain.nextCall(request)
     * - Extract text from assistant output
     * - Replace email addresses with "[REDACTED_EMAIL]" (Pattern: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})
     * - Replace US SSNs with "[REDACTED_SSN]" (Pattern: \\b\\d{3}-\\d{2}-\\d{4}\\b)
     * - If modified, mutate response with new ChatResponse containing the masked AssistantMessage
     * - getOrder() returns 180
     */
    public static class PiiMaskingAdvisor implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 7): Bypasses PII masking
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "PiiMaskingAdvisor";
        }
    }

    /**
     * Scenario 8: Order Tracking Advisor for Verifying Chain Execution Order.
     * <p>
     * Instructions:
     * - Record ExecutionOrderTrace(name, "PRE_CALL", System.currentTimeMillis()) before chain.nextCall(request)
     * - Record ExecutionOrderTrace(name, "POST_CALL", System.currentTimeMillis()) after chain.nextCall(request)
     * - Return getOrder() as passed in constructor
     */
    public static class OrderTrackingAdvisor implements CallAdvisor {
        private final String name;
        private final int order;
        private final List<ExecutionOrderTrace> traces;

        public OrderTrackingAdvisor(String name, int order, List<ExecutionOrderTrace> traces) {
            this.name = name;
            this.order = order;
            this.traces = traces;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 8): Does not record pre/post traces
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Scenario 9: Helper method to create a guarded ChatClient with default advisors.
     * <p>
     * Instructions:
     * - ChatClient.builder(model).defaultAdvisors(advisors).build()
     */
    public ChatClient createGuardedChatClient(ChatModel model, CallAdvisor... advisors) {
        return null;
    }

    /**
     * Scenario 10: Integrated Production Guardrail Stack Assembly.
     * <p>
     * Instructions:
     * - Create KeywordGuardrailAdvisor(blockedKeywords)
     * - Create SystemPromptEnforcingAdvisor(policy)
     * - Create CostCircuitBreakerAdvisor(budgetConfig)
     * - Create PiiMaskingAdvisor()
     * - Build ChatClient with defaultAdvisors in order:
     *     keywordAdvisor, policyAdvisor, tokenAdvisor, costAdvisor, piiAdvisor, auditAdvisor
     */
    public ChatClient createProductionDiagnosticStack(
            ChatModel model,
            List<String> blockedKeywords,
            String policy,
            CostBudgetConfig budgetConfig,
            TokenBudgetAdvisor tokenAdvisor,
            AuditLoggingAdvisor auditAdvisor
    ) {
        return null;
    }
}
