package phase04;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable domain records, exceptions, and contracts for Phase 04 Exercise 02:
 * Resilient Guardrails, Failover Routing, and Transactional Token Quotas.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class ResilientAdvisorContracts {

    private ResilientAdvisorContracts() {}

    /**
     * Thrown by SlidingWindowRateLimiterAdvisor when the maximum request limit
     * in the rolling time window has been exhausted.
     * <p>
     * Contract requirement: The exception message MUST contain "Rate limit exceeded".
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Thrown by SchemaSelfHealingAdvisor when an assistant output fails schema verification
     * and automated one-shot repair retry also fails.
     * <p>
     * Contract requirement: The exception message MUST contain "Schema validation failed".
     */
    public static class SchemaValidationException extends RuntimeException {
        public SchemaValidationException(String message) {
            super(message);
        }
    }

    /**
     * Thrown by DynamicTokenQuotaAdvisor when the remaining token budget is insufficient
     * to fulfill the pre-allocated token estimate for a prompt.
     * <p>
     * Contract requirement: The exception message MUST contain "Insufficient token quota".
     */
    public static class BudgetExceededException extends RuntimeException {
        public BudgetExceededException(String message) {
            super(message);
        }
    }

    /**
     * Simulated downstream failure thrown by FakeFailoverChatModel to test failover routing.
     */
    public static class DownstreamModelFailureException extends RuntimeException {
        public DownstreamModelFailureException(String message) {
            super(message);
        }
    }

    /**
     * Context keys used across resilient advisors.
     */
    public static final class ContextKeys {
        public static final String EXECUTION_MODE = "executionMode";
        public static final String AUDIT_DRY_RUN = "audit.dryRun";
        public static final String PROCESSED_BY = "processedBy";
        public static final String TRUNCATED_MESSAGE_COUNT = "truncatedMessageCount";
        public static final String REMAINING_REQUESTS = "remainingRequests";
        public static final String FAILOVER_TRIGGERED = "failoverTriggered";
        public static final String PRIMARY_ERROR = "primaryError";
        public static final String SCHEMA_HEALED = "schemaHealed";
        public static final String GROUNDING_VIOLATION = "groundingViolation";
        public static final String UNGROUNDED_ENTITIES = "ungroundedEntities";
        public static final String TRACEPARENT = "traceparent";
        public static final String PROPAGATION_SUCCESS = "propagationSuccess";
        public static final String REMAINING_QUOTA = "remainingQuota";

        private ContextKeys() {}
    }
}
