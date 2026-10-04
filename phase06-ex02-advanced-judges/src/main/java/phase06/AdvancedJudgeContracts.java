package phase06;

import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

import java.util.*;

/**
 * Immutable domain records, exceptions, and contracts for Phase 06 Exercise 02:
 * Advanced LLM-as-a-Judge, Bias Mitigation, Calibration & Trajectory Evaluation.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class AdvancedJudgeContracts {

    private AdvancedJudgeContracts() {}

    /**
     * Pairwise tournament winner outcome.
     */
    public enum TournamentWinner {
        CANDIDATE_A,
        CANDIDATE_B,
        INCONCLUSIVE_OR_TIE
    }

    /**
     * Result of pairwise A/B tournament judging with position swap mitigation.
     */
    public record PairwiseResult(
            TournamentWinner winner,
            boolean positionBiasDetected,
            String run1Choice,
            String run2Choice,
            String reasoning
    ) {
        public PairwiseResult {
            Objects.requireNonNull(winner, "winner must not be null");
            Objects.requireNonNull(run1Choice, "run1Choice must not be null");
            Objects.requireNonNull(run2Choice, "run2Choice must not be null");
            Objects.requireNonNull(reasoning, "reasoning must not be null");
        }
    }

    /**
     * Consensus result of self-consistency majority voting.
     */
    public record ConsensusResult(
            boolean majorityPass,
            boolean consensusReached,
            float confidence,
            float averageScore,
            int passVotes,
            int failVotes
    ) {}

    /**
     * RAG Triad claim-level attribution and hallucination evaluation result.
     */
    public record FaithfulnessResult(
            boolean passed,
            float faithfulnessScore,
            int totalClaims,
            List<String> supportedClaims,
            List<String> unsupportedClaims
    ) {
        public FaithfulnessResult {
            supportedClaims = supportedClaims != null ? List.copyOf(supportedClaims) : List.of();
            unsupportedClaims = unsupportedClaims != null ? List.copyOf(unsupportedClaims) : List.of();
        }
    }

    /**
     * Answer relevance and query drift evaluation result.
     */
    public record RelevanceResult(
            boolean passed,
            float relevanceScore,
            boolean directAnswer,
            boolean queryDriftDetected,
            String feedback
    ) {
        public RelevanceResult {
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Calibrated dimension score matched against explicit anchor examples.
     */
    public record CalibratedDimensionScore(
            String dimension,
            float score,
            String anchorMatch
    ) {
        public CalibratedDimensionScore {
            Objects.requireNonNull(dimension, "dimension must not be null");
            Objects.requireNonNull(anchorMatch, "anchorMatch must not be null");
        }
    }

    /**
     * Result of calibrated multi-criteria rubric evaluation.
     */
    public record CalibratedRubricResult(
            boolean passed,
            float normalizedScore,
            List<CalibratedDimensionScore> dimensions,
            String feedback
    ) {
        public CalibratedRubricResult {
            dimensions = dimensions != null ? List.copyOf(dimensions) : List.of();
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Adversarial safety and jailbreak refusal evaluation result.
     */
    public record SafetyResult(
            boolean passed,
            float score,
            boolean refused,
            boolean leakDetected,
            boolean preachinessDetected,
            String feedback
    ) {
        public SafetyResult {
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Latency distribution measurements (P95, P50 in ms).
     */
    public record LatencyDto(double p95, double p50) {}

    /**
     * Load test metrics report (from Diagnostician step 9/11).
     */
    public record LoadReportDto(double rps, LatencyDto latency, double failRate, double checkPassRate) {}

    /**
     * Profiler signal summary count (from Diagnostician JFR harvests).
     */
    public record SignalSummaryDto(long count) {}

    /**
     * Java Flight Recorder profile summary containing identified signals.
     */
    public record JfrReportDto(Map<String, SignalSummaryDto> signals) {
        public JfrReportDto {
            signals = signals != null ? Map.copyOf(signals) : Map.of();
        }
    }

    /**
     * Telemetry noise floors.
     */
    public record NoiseFloors(double p95FloorMs, double p50FloorMs, double rpsFloor) {}

    /**
     * Saddle-Safe Keep decision result (from Diagnostician KeepRule v2).
     */
    public record KeepDecision(boolean keep, String keepType, String reason) {
        public KeepDecision {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Agent trajectory step type.
     */
    public enum StepType {
        DECIDE,
        TOOL_CALL,
        TOOL_RESULT,
        FINISH
    }

    /**
     * Single step in an agent's reasoning trajectory.
     */
    public record TrajectoryStep(int turn, StepType type, String name, String payload) {
        public TrajectoryStep {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(payload, "payload must not be null");
        }
    }

    /**
     * Full multi-turn agent execution trajectory.
     */
    public record AgentTrajectory(String goal, List<TrajectoryStep> steps, boolean goalCompleted) {
        public AgentTrajectory {
            Objects.requireNonNull(goal, "goal must not be null");
            steps = steps != null ? List.copyOf(steps) : List.of();
        }
    }

    /**
     * Trajectory evaluation result measuring step efficiency and tool loops.
     */
    public record TrajectoryEfficiencyResult(
            boolean passed,
            float efficiencyScore,
            int redundantToolCalls,
            boolean cycleDetected,
            String feedback
    ) {
        public TrajectoryEfficiencyResult {
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Statistical inter-judge agreement result (Cohen's Kappa).
     */
    public record AgreementResult(
            boolean passed,
            float kappa,
            float observedAgreement,
            float chanceAgreement,
            String feedback
    ) {
        public AgreementResult {
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Single benchmark test dataset item.
     */
    public record BenchmarkItem(
            String id,
            String query,
            List<String> contextDocs,
            String candidateResponse,
            String groundTruth,
            boolean isAdversarial,
            AgentTrajectory trajectory
    ) {
        public BenchmarkItem {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(query, "query must not be null");
            contextDocs = contextDocs != null ? List.copyOf(contextDocs) : List.of();
            Objects.requireNonNull(candidateResponse, "candidateResponse must not be null");
        }
    }

    /**
     * Release benchmark summary across all dimensions.
     */
    public record ReleaseBenchmarkReport(
            int totalEvaluated,
            float meanFaithfulness,
            float meanRelevance,
            int safetyViolations,
            float meanAccuracy,
            float meanEfficiency,
            boolean releaseApproved,
            String summary
    ) {
        public ReleaseBenchmarkReport {
            Objects.requireNonNull(summary, "summary must not be null");
        }
    }

    /**
     * Exception thrown when release benchmark suite fails release gates.
     * <p>
     * Contract Requirement: Message MUST contain "Release benchmark gate breach".
     */
    public static class BenchmarkGateBreachException extends RuntimeException {
        public BenchmarkGateBreachException(String message) {
            super(message);
        }
    }
}
