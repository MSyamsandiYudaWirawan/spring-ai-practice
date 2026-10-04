package phase07;

import java.util.*;

/**
 * Immutable domain records, exceptions, and contracts for Phase 07 Exercise 01:
 * In-Memory Saga Agent Loop, State Machine, Compensation & Trajectory Auditing.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class SagaContracts {

    private SagaContracts() {}

    /**
     * Discrete lifecycle states of the Saga Agent Loop.
     */
    public enum SagaState {
        IDLE,
        DECIDE,
        APPLY,
        MEASURE,
        JUDGE,
        FINISH,
        ABORTED
    }

    /**
     * Outcome of an individual agent iteration.
     */
    public enum IterationOutcome {
        KEPT,
        REVERTED,
        WASTED,
        ABORTED
    }

    /**
     * Telemetry measurements of system performance.
     */
    public record TelemetryMeasurement(double p95Ms, double rps, double failRate) {
        public TelemetryMeasurement {
            if (failRate < 0.0 || failRate > 1.0) {
                throw new IllegalArgumentException("failRate must be between 0.0 and 1.0, got: " + failRate);
            }
        }
    }

    /**
     * Baseline noise floors: deltas must strictly exceed these to count as improvement.
     */
    public record NoiseFloors(double p95FloorMs, double rpsFloor) {}

    /**
     * Structured optimization proposal from the LLM DECIDE phase.
     */
    public record DecisionProposal(
            String category,
            String fileToModify,
            String patchContent,
            String rationale
    ) {
        public DecisionProposal {
            Objects.requireNonNull(category, "category must not be null");
            Objects.requireNonNull(fileToModify, "fileToModify must not be null");
            Objects.requireNonNull(patchContent, "patchContent must not be null");
            Objects.requireNonNull(rationale, "rationale must not be null");
        }
    }

    /**
     * Recorded audit event capturing state machine trajectory.
     */
    public record TrajectoryEvent(
            int turn,
            SagaState state,
            String eventType,
            String detail,
            long tokensUsed
    ) {
        public TrajectoryEvent {
            Objects.requireNonNull(state, "state must not be null");
            Objects.requireNonNull(eventType, "eventType must not be null");
            Objects.requireNonNull(detail, "detail must not be null");
        }
    }

    /**
     * Guardrail and budget configuration for the Saga loop.
     */
    public record LoopConfig(
            int maxIterations,
            long maxTokens,
            double costUsdCap,
            NoiseFloors noiseFloors
    ) {
        public LoopConfig {
            if (maxIterations < 1) throw new IllegalArgumentException("maxIterations must be >= 1");
            if (maxTokens < 1) throw new IllegalArgumentException("maxTokens must be >= 1");
            Objects.requireNonNull(noiseFloors, "noiseFloors must not be null");
        }
    }

    /**
     * Checkpoint snapshot for pause, kill, and resume operations.
     */
    public record LoopCheckpoint(
            int iteration,
            String lastKeptSha,
            TelemetryMeasurement baselineTelemetry,
            long totalTokensUsed,
            SagaState state
    ) {
        public LoopCheckpoint {
            Objects.requireNonNull(lastKeptSha, "lastKeptSha must not be null");
            Objects.requireNonNull(baselineTelemetry, "baselineTelemetry must not be null");
            Objects.requireNonNull(state, "state must not be null");
        }
    }

    /**
     * Final summary report emitted upon Saga termination.
     */
    public record LoopSummary(
            int totalIterations,
            int keptCount,
            int revertedCount,
            int wastedCount,
            long totalTokens,
            String finalSha,
            SagaState finalState
    ) {
        public LoopSummary {
            Objects.requireNonNull(finalSha, "finalSha must not be null");
            Objects.requireNonNull(finalState, "finalState must not be null");
        }
    }

    /**
     * In-memory virtual code tree interface for git-like atomic commit and rollback.
     */
    public interface VirtualWorkspace {
        String getHeadSha();
        void commit(String sha, Map<String, String> files);
        void revertTo(String sha);
        Map<String, String> getFiles();
        String readFile(String path);
        void writeFile(String path, String content);
    }

    /**
     * Thrown when an execution guardrail or budget ceiling is breached.
     * <p>
     * Contract requirement: Message MUST contain "Saga guardrail breached".
     */
    public static class SagaLoopBreachException extends RuntimeException {
        public SagaLoopBreachException(String message) {
            super(message);
        }
    }
}
