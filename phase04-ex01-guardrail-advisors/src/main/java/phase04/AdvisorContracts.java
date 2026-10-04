package phase04;

import java.util.*;

/**
 * Immutable domain records, exceptions, and contracts for Phase 04:
 * Guardrails, Advisors, and Token Budget Circuit-Breakers.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class AdvisorContracts {

    private AdvisorContracts() {}

    /**
     * Thrown by security guardrails when a prompt or message violates security policies.
     */
    public static class PromptSecurityException extends RuntimeException {
        public PromptSecurityException(String message) {
            super(message);
        }
    }

    /**
     * Thrown by budget guardrails when token or dollar budgets are exceeded.
     */
    public static class BudgetExceededException extends RuntimeException {
        public BudgetExceededException(String message) {
            super(message);
        }
    }

    /**
     * Thrown by latency guardrails when call execution exceeds the allowable wall-clock limit.
     */
    public static class LatencyTimeoutException extends RuntimeException {
        public LatencyTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * Chronological audit entry captured by AuditLoggingAdvisor.
     */
    public record AdvisorAuditRecord(
            String advisorName,
            String promptContent,
            String responseContent,
            String correlationId,
            long latencyMs,
            long timestamp
    ) {
        public AdvisorAuditRecord {
            Objects.requireNonNull(advisorName, "advisorName must not be null");
            Objects.requireNonNull(correlationId, "correlationId must not be null");
        }
    }

    /**
     * Cumulative token statistics tracked by TokenBudgetAdvisor.
     */
    public record TokenUsageSummary(
            int promptTokens,
            int completionTokens,
            int totalTokens,
            int callCount
    ) {}

    /**
     * Pricing configuration for dollar budget tracking in CostCircuitBreakerAdvisor.
     */
    public record CostBudgetConfig(
            double maxCostUsd,
            double priceInPerMtok,
            double priceOutPerMtok
    ) {
        public CostBudgetConfig {
            if (maxCostUsd < 0.0) throw new IllegalArgumentException("maxCostUsd must be >= 0");
            if (priceInPerMtok < 0.0) throw new IllegalArgumentException("priceInPerMtok must be >= 0");
            if (priceOutPerMtok < 0.0) throw new IllegalArgumentException("priceOutPerMtok must be >= 0");
        }
    }

    /**
     * Execution order trace event for testing onion-model advisor ordering.
     */
    public record ExecutionOrderTrace(
            String advisorName,
            String phase, // "PRE_CALL" or "POST_CALL"
            long timestamp
    ) {}
}
