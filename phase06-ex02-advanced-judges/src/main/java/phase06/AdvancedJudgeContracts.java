package phase06;

import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

import java.util.*;

/**
 * Immutable domain records, exceptions, and contracts for Phase 06 Exercise 02:
 * Advanced LLM-as-a-Judge, Bias Mitigation, Calibration & Trajectory Evaluation (15 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class AdvancedJudgeContracts {

    private AdvancedJudgeContracts() {}

    // -------------------------------------------------------------------------
    // Topic 1: Tournament & Voting Judges (Bias Mitigation)
    // -------------------------------------------------------------------------

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
     * Individual candidate ranking in a tournament round-robin.
     */
    public record TournamentRanking(
            String candidateId,
            int wins,
            int ties,
            int losses,
            float winRate
    ) {
        public TournamentRanking {
            Objects.requireNonNull(candidateId, "candidateId must not be null");
        }
    }

    /**
     * Complete tournament leaderboard aggregating round-robin matches.
     */
    public record TournamentLeaderboard(
            List<TournamentRanking> rankings,
            String topWinner
    ) {
        public TournamentLeaderboard {
            rankings = rankings != null ? List.copyOf(rankings) : List.of();
            Objects.requireNonNull(topWinner, "topWinner must not be null");
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

    // -------------------------------------------------------------------------
    // Topic 2: RAG Triad Grounding & Attribution
    // -------------------------------------------------------------------------

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
     * Single atomic claim mapped to supporting document index.
     */
    public record ClaimAttribution(
            String claim,
            int docIndex,
            boolean supported
    ) {
        public ClaimAttribution {
            Objects.requireNonNull(claim, "claim must not be null");
        }
    }

    /**
     * Result of claim-to-document citation mapping and attribution verification.
     */
    public record CitationAttributionResult(
            boolean passed,
            float attributionScore,
            List<ClaimAttribution> attributions,
            String feedback
    ) {
        public CitationAttributionResult {
            attributions = attributions != null ? List.copyOf(attributions) : List.of();
            Objects.requireNonNull(feedback, "feedback must not be null");
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

    // -------------------------------------------------------------------------
    // Topic 3: Calibrated Rubrics & Safety Gates
    // -------------------------------------------------------------------------

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
     * System prompt leakage and sensitive token exfiltration audit result.
     */
    public record LeakAuditResult(
            boolean passed,
            boolean leakDetected,
            List<String> leakedTokens,
            String explanation
    ) {
        public LeakAuditResult {
            leakedTokens = leakedTokens != null ? List.copyOf(leakedTokens) : List.of();
            Objects.requireNonNull(explanation, "explanation must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // Topic 4: Agent Trajectory & Reasoning Audits
    // -------------------------------------------------------------------------

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
     * Trajectory loop audit result detecting cyclical tool call recursion.
     */
    public record LoopAuditResult(
            boolean passed,
            boolean loopDetected,
            int cycleCount,
            List<String> repeatedSequence,
            String feedback
    ) {
        public LoopAuditResult {
            repeatedSequence = repeatedSequence != null ? List.copyOf(repeatedSequence) : List.of();
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Trajectory goal convergence gate result.
     */
    public record TrajectoryGateResult(
            boolean passed,
            boolean goalCompleted,
            int totalSteps,
            boolean convergedWithinBudget,
            float overallScore,
            String reason
    ) {
        public TrajectoryGateResult {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // Topic 5: Inter-Judge Calibration & Production Release Gates
    // -------------------------------------------------------------------------

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
     * Individual evaluation metric scores for a benchmark item.
     */
    public record EvaluationMetricItem(
            String id,
            float faithfulness,
            float relevance,
            float accuracy,
            float efficiency,
            boolean safetyViolation
    ) {
        public EvaluationMetricItem {
            Objects.requireNonNull(id, "id must not be null");
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
