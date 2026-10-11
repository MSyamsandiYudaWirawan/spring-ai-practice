package phase04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import phase04.AdvisorContracts.*;

import java.util.*;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice implementing Spring AI CallAdvisor guardrails, budget circuit-breakers, and client integration across 15 repetitive drills:
 * 1. KeywordGuardrailAdvisor
 * 2. RegexPatternGuardrailAdvisor
 * 3. SystemPromptEnforcingAdvisor
 * 4. AuditLoggingAdvisor
 * 5. OrderTrackingAdvisor
 * 6. ContextEnrichmentAdvisor
 * 7. TokenBudgetAdvisor
 * 8. CostCircuitBreakerAdvisor
 * 9. MaxTurnsAdvisor
 * 10. LatencyGuardrailAdvisor
 * 11. PiiMaskingAdvisor
 * 12. SecretKeyRedactionAdvisor
 * 13. createGuardedChatClient
 * 14. DynamicBypassGuardrailAdvisor
 * 15. createProductionDiagnosticStack
 */
public class GuardrailAdvisorsUnderTest {

    // =========================================================================
    // Topic 1: Pre-Call Security & Policy Injections (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 1: Pre-Call Prompt Security Circuit-Breaker Advisor.
     */
    public static class KeywordGuardrailAdvisor implements CallAdvisor {
        public KeywordGuardrailAdvisor(List<String> blockedKeywords) {
            // DEFECT (Scenario 1): Ignores blocked keywords
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 1): Bypasses blocked keyword checking
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
     * Scenario 2: Pre-Call Regex Pattern Blocking Guardrail.
     */
    public static class RegexPatternGuardrailAdvisor implements CallAdvisor {
        public RegexPatternGuardrailAdvisor(List<String> regexPatterns) {
            // DEFECT (Scenario 2): Ignores regex patterns
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 2): Bypasses pattern checks
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "RegexPatternGuardrailAdvisor";
        }
    }

    /**
     * Scenario 3: Pre-Call Mandatory System Policy Injector Advisor.
     */
    public static class SystemPromptEnforcingAdvisor implements CallAdvisor {
        public SystemPromptEnforcingAdvisor(String enforcedPolicy) {
            // DEFECT (Scenario 3): Ignores policy
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 3): Bypasses policy injection
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

    // =========================================================================
    // Topic 2: Observation, Auditing & Context Propagation (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 4: Pre/Post-Call Inspection and Context Enrichment Advisor.
     */
    public static class AuditLoggingAdvisor implements CallAdvisor {
        public List<AdvisorAuditRecord> getAuditRecords() {
            // DEFECT (Scenario 4): Returns empty list
            return Collections.emptyList();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 4): Bypasses correlationId enrichment and audit recording
            return chain.nextCall(request);
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
     * Scenario 5: Onion-Model Execution Order Tracking Advisor.
     */
    public static class OrderTrackingAdvisor implements CallAdvisor {
        public OrderTrackingAdvisor(String advisorName, int order, List<ExecutionOrderTrace> traces) {
            // DEFECT (Scenario 5): Ignores parameters
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 5): Does not record pre/post traces
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "OrderTrackingAdvisor";
        }
    }

    /**
     * Scenario 6: Metadata Context Propagation Advisor.
     */
    public static class ContextEnrichmentAdvisor implements CallAdvisor {
        public ContextEnrichmentAdvisor(Map<String, Object> metadata) {
            // DEFECT (Scenario 6): Ignores metadata
        }

        public int getInjectedCount() {
            // DEFECT (Scenario 6): Returns 0
            return 0;
        }

        public Map<String, Object> getLastEnrichedContext() {
            // DEFECT (Scenario 6): Returns empty map
            return Collections.emptyMap();
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 6): Does not enrich request context
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "ContextEnrichmentAdvisor";
        }
    }

    // =========================================================================
    // Topic 3: Token Accumulation & Budget Circuit Breakers (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 7: Post-Call Cumulative Token Usage Tracking Advisor.
     */
    public static class TokenBudgetAdvisor implements CallAdvisor {
        public TokenUsageSummary getSummary() {
            // DEFECT (Scenario 7): Returns empty summary
            return new TokenUsageSummary(0, 0, 0, 0);
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 7): Does not accumulate tokens
            return chain.nextCall(request);
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
     * Scenario 8: Post-Call Dollar Spend Cap Circuit-Breaker Advisor.
     */
    public static class CostCircuitBreakerAdvisor implements CallAdvisor {
        public CostCircuitBreakerAdvisor(CostBudgetConfig config) {
            // DEFECT (Scenario 8): Ignores config
        }

        public double getCumulativeCostUsd() {
            // DEFECT (Scenario 8): Returns 0.0
            return 0.0;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 8): Bypasses cost checking
            return chain.nextCall(request);
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
     * Scenario 9: Session Conversation Turn Limiter Advisor.
     */
    public static class MaxTurnsAdvisor implements CallAdvisor {
        public MaxTurnsAdvisor(int maxTurnsAllowed) {
            // DEFECT (Scenario 9): Ignores maxTurnsAllowed
        }

        public int getTurnCount() {
            // DEFECT (Scenario 9): Returns 0
            return 0;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 9): Does not count turns or enforce limit
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "MaxTurnsAdvisor";
        }
    }

    // =========================================================================
    // Topic 4: Post-Call Sanitization & Latency Bounds (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Execution Wall-Clock Timeout Guardrail Advisor.
     */
    public static class LatencyGuardrailAdvisor implements CallAdvisor {
        public LatencyGuardrailAdvisor(long maxLatencyMs) {
            // DEFECT (Scenario 10): Ignores maxLatencyMs
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 10): Bypasses latency measurement
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
     * Scenario 11: Post-Call Response PII Sanitization Advisor.
     */
    public static class PiiMaskingAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 11): Returns raw unmasked response
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
     * Scenario 12: Post-Call API Key & Secret Token Redaction Advisor.
     */
    public static class SecretKeyRedactionAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 12): Returns raw unmasked response
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "SecretKeyRedactionAdvisor";
        }
    }

    // =========================================================================
    // Topic 5: Multi-Advisor Composition & Production Stacks (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: ChatClient Default vs Call-Level Advisor Integration.
     */
    public ChatClient createGuardedChatClient(ChatModel chatModel, CallAdvisor defaultAdvisor) {
        // DEFECT (Scenario 13): Omits defaultAdvisor
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Scenario 14: Dynamic Role-Based Guardrail Bypass Advisor.
     */
    public static class DynamicBypassGuardrailAdvisor implements CallAdvisor {
        public DynamicBypassGuardrailAdvisor(CallAdvisor innerAdvisor) {
            // DEFECT (Scenario 14): Ignores innerAdvisor
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 14): Dispatches call without checking role or inner advisor
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "DynamicBypassGuardrailAdvisor";
        }
    }

    /**
     * Scenario 15: Production-Grade Multi-Guardrail Stack Integration.
     */
    public ChatClient createProductionDiagnosticStack(
            ChatModel chatModel,
            List<String> blockedKeywords,
            String enforcedPolicy,
            CostBudgetConfig budgetConfig,
            TokenBudgetAdvisor tokenAdvisor,
            AuditLoggingAdvisor auditAdvisor
    ) {
        // DEFECT (Scenario 15): Returns ungarded ChatClient
        return ChatClient.builder(chatModel).build();
    }
}
