package phase05;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

/**
 * Immutable domain records, exceptions, and contracts for Phase 05 Exercise 01:
 * Deterministic Offline Test Harness & LLM Boundary Seams (15 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class HarnessContracts {

    private HarnessContracts() {}

    /**
     * Immutable response contract returned across ChatPort boundaries.
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
     * Recorded prompt invocation capturing the boundary call arguments.
     */
    public record PromptCapture(
            String system,
            String user,
            List<Object> toolBeans,
            long timestamp
    ) {
        public PromptCapture {
            Objects.requireNonNull(system, "system must not be null");
            Objects.requireNonNull(user, "user must not be null");
            toolBeans = toolBeans != null ? List.copyOf(toolBeans) : List.of();
        }
    }

    /**
     * Seam between agent loops/business logic and the LLM provider.
     * Decouples application logic from Spring AI's internal builder chains.
     */
    public interface ChatPort {
        ChatResult chat(String system, String user, List<Object> toolBeans);

        default ChatResult chat(String system, String user) {
            return chat(system, user, List.of());
        }
    }

    /**
     * Single turn definition for scripted sequential dialogue replay.
     */
    public record DialogueTurn(String expectedRole, String expectedPromptSubstring, ChatResult reply) {
        public DialogueTurn {
            Objects.requireNonNull(expectedRole, "expectedRole must not be null");
            Objects.requireNonNull(expectedPromptSubstring, "expectedPromptSubstring must not be null");
            Objects.requireNonNull(reply, "reply must not be null");
        }
    }

    /**
     * Verification result of sequential dialogue replaying.
     */
    public record ReplayValidation(boolean allTurnsMatched, int turnsExecuted, String lastTurnSummary) {
        public ReplayValidation {
            Objects.requireNonNull(lastTurnSummary, "lastTurnSummary must not be null");
        }
    }

    /**
     * Reactive streaming chat seam.
     */
    public interface StreamingChatPort {
        Flux<String> stream(String system, String user);
    }

    /**
     * Thrown by offline test harnesses when accumulated token spend exceeds the test ceiling.
     * <p>
     * Contract requirement: The exception message MUST contain "Offline token budget exceeded".
     */
    public static class HarnessBudgetExceededException extends RuntimeException {
        public HarnessBudgetExceededException(String message) {
            super(message);
        }
    }

    /**
     * Simulated network / provider fault for offline testing.
     */
    public static class SimulatedUpstreamException extends RuntimeException {
        public SimulatedUpstreamException(String message) {
            super(message);
        }
    }

    /**
     * Deterministic virtual clock for offline time simulation.
     */
    public static class TestClock {
        private long currentTime;

        public TestClock(long initialTime) {
            this.currentTime = initialTime;
        }

        public TestClock() {
            this(1000L);
        }

        public synchronized long now() {
            return currentTime;
        }

        public synchronized void advance(long millis) {
            this.currentTime += millis;
        }
    }

    /**
     * Summary report for simulated retry and backoff loops.
     */
    public record RetrySimulationReport(
            int attempts,
            long totalDelayMillis,
            boolean succeeded,
            ChatResult finalResult
    ) {}

    /**
     * Audit log record capturing prompt details and token delta for each invocation.
     */
    public record AuditLogEntry(
            int callIndex,
            String system,
            String user,
            long tokensThisCall,
            long cumulativeTokens,
            long timestamp
    ) {
        public AuditLogEntry {
            Objects.requireNonNull(system, "system must not be null");
            Objects.requireNonNull(user, "user must not be null");
        }
    }

    /**
     * Final report emitted by the integrated diagnostic triage loop in Scenario 15.
     */
    public record DiagnosticTriageReport(
            String issue,
            String rootCause,
            String recommendation,
            int turnsTaken,
            long totalTokens
    ) {}
}
