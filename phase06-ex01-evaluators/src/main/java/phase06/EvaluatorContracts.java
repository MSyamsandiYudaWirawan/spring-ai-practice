package phase06;

import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

import java.util.*;

/**
 * Immutable domain records, exceptions, and contracts for Phase 06 Exercise 01:
 * Deterministic Evaluators, Spring AI Evaluator Interface and Foundational LLM-as-a-Judge.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class EvaluatorContracts {

    private EvaluatorContracts() {}

    /**
     * Performance telemetry delta comparing candidate run against baseline.
     * Positive deltas indicate improvement for latency (lower is better, so baseline - candidate)
     * and RPS (higher is better, so candidate - baseline).
     */
    public record TelemetryDelta(
            double p95DeltaMs,
            double p50DeltaMs,
            double rpsDelta,
            double failRateBefore,
            double failRateAfter
    ) {}

    /**
     * Baseline noise floors: improvements below these thresholds are discarded as statistical jitter.
     */
    public record NoiseFloors(
            double p95FloorMs,
            double p50FloorMs,
            double rpsFloor
    ) {}

    /**
     * Parsed evaluation rubric score.
     */
    public record RubricScore(
            float score,
            String reason
    ) {
        public RubricScore {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Evaluation criterion definition for multi-attribute evaluation.
     */
    public record EvaluationCriterion(
            String name,
            float weight
    ) {
        public EvaluationCriterion {
            Objects.requireNonNull(name, "name must not be null");
            if (weight < 0.0f || weight > 1.0f) {
                throw new IllegalArgumentException("Weight must be between 0.0 and 1.0, got: " + weight);
            }
        }
    }

    /**
     * Individual criterion evaluation result.
     */
    public record CriterionScore(
            String criterionName,
            float score,
            String feedback
    ) {
        public CriterionScore {
            Objects.requireNonNull(criterionName, "criterionName must not be null");
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Aggregated result of a multi-criteria evaluation.
     */
    public record MultiCriteriaResult(
            float compositeScore,
            boolean passed,
            List<CriterionScore> criterionScores,
            String feedback
    ) {
        public MultiCriteriaResult {
            criterionScores = criterionScores != null ? List.copyOf(criterionScores) : List.of();
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Aggregate benchmark summary across a batch of evaluation requests.
     */
    public record BenchmarkSummary(
            int totalRequests,
            int passCount,
            int failCount,
            float passRate,
            float averageScore
    ) {}

    /**
     * Exception thrown when an evaluation batch breaches required passing threshold.
     * <p>
     * Contract Requirement: Exception message MUST contain "Evaluation threshold breach".
     */
    public static class EvaluationGateException extends RuntimeException {
        public EvaluationGateException(String message) {
            super(message);
        }
    }
}
