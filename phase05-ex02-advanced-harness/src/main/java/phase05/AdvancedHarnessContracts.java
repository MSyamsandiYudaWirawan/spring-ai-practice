package phase05;

import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Domain contracts, exceptions, and records for Phase 05 Exercise 02:
 * Advanced Offline Test Harness, Chaos Matrix, Circuit Breakers, Multi-Tenancy, and HTTP Stubs.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class AdvancedHarnessContracts {

    private AdvancedHarnessContracts() {}

    /**
     * Standard immutable result record across ChatPort boundaries.
     */
    public record ChatResult(String text, long tokensIn, long tokensOut) {
        public ChatResult {
            Objects.requireNonNull(text, "text must not be null");
        }

        public long totalTokens() {
            return tokensIn + tokensOut;
        }
    }

    /**
     * Boundary interface for synchronous LLM interaction.
     */
    public interface ChatPort {
        ChatResult chat(String system, String user, List<Object> toolBeans);

        default ChatResult chat(String system, String user) {
            return chat(system, user, List.of());
        }
    }

    /**
     * Boundary interface for multi-tenant LLM interaction.
     */
    public interface MultiTenantChatPort {
        ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans);

        default ChatResult chat(String tenantId, String system, String user) {
            return chat(tenantId, system, user, List.of());
        }
    }

    /**
     * Boundary interface for reactive token streaming.
     */
    public interface StreamingChatPort {
        Flux<String> stream(String system, String user);
    }

    /**
     * Configuration for chaos testing matrix.
     */
    public record ChaosMatrix(
            double failureRate,
            long latencyMs,
            boolean corruptPayload,
            Random random
    ) {
        public ChaosMatrix {
            Objects.requireNonNull(random, "random must not be null");
        }
    }

    /**
     * State of the circuit breaker test double.
     */
    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    /**
     * Thrown when circuit breaker is in OPEN state.
     * Contract requirement: Message MUST contain "Circuit breaker is OPEN".
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when a tenant exceeds its allocated token budget.
     * Contract requirement: Message MUST contain "Quota exceeded for tenant".
     */
    public static class TenantQuotaExceededException extends RuntimeException {
        public TenantQuotaExceededException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when rate limit retries are exhausted.
     * Contract requirement: Message MUST contain "Rate limit retries exhausted".
     */
    public static class RateLimitExhaustedException extends RuntimeException {
        public RateLimitExhaustedException(String message) {
            super(message);
        }
    }

    /**
     * A single recorded conversational turn in a replay trajectory.
     */
    public record ConversationTurn(
            String userPrompt,
            String expectedResponse,
            long expectedTokens
    ) {
        public ConversationTurn {
            Objects.requireNonNull(userPrompt, "userPrompt must not be null");
            Objects.requireNonNull(expectedResponse, "expectedResponse must not be null");
        }
    }

    /**
     * A full recorded conversational trajectory for deterministic replay.
     */
    public record ConversationTrajectory(
            String trajectoryId,
            List<ConversationTurn> turns
    ) {
        public ConversationTrajectory {
            Objects.requireNonNull(trajectoryId, "trajectoryId must not be null");
            turns = turns != null ? List.copyOf(turns) : List.of();
        }
    }

    /**
     * Deterministic virtual clock fixture for testing timeouts and circuit breaker cooldowns.
     */
    public static class AdvancedTestClock {
        private long currentTime;

        public AdvancedTestClock(long initialTime) {
            this.currentTime = initialTime;
        }

        public AdvancedTestClock() {
            this(10_000L);
        }

        public synchronized long now() {
            return currentTime;
        }

        public synchronized void advance(long millis) {
            this.currentTime += millis;
        }
    }

    /**
     * Strongly-typed diagnostic decision schema for structured output validation.
     */
    public record TriageDecision(
            String action,
            String rootCause,
            String recommendation
    ) {
        public TriageDecision {
            Objects.requireNonNull(action, "action must not be null");
            Objects.requireNonNull(rootCause, "rootCause must not be null");
            Objects.requireNonNull(recommendation, "recommendation must not be null");
        }
    }

    /**
     * Final report emitted by the integrated autonomous chaos triage harness in Scenario 10.
     */
    public record AdvancedDiagnosticReport(
            String incidentId,
            String tenantId,
            String rootCause,
            int turnsCompleted,
            long totalTokensUsed,
            boolean circuitTripped,
            boolean failoverUsed
    ) {}
}
