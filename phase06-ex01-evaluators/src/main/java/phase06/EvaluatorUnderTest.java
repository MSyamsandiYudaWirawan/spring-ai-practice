package phase06;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import phase06.EvaluatorContracts.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service under test — THE ONLY FILE YOU MODIFY for Phase 06 Exercise 01.
 * <p>
 * Practice implementing deterministic evaluators, Spring AI Evaluator interfaces,
 * noise floor gates, rubric parsers, and benchmark runners:
 * 1. RelevancyEvaluatorAdapter (Spring AI built-in RelevancyEvaluator)
 * 2. FactCheckingEvaluatorAdapter (Spring AI built-in FactCheckingEvaluator)
 * 3. NoiseFloorThresholdEvaluator (Deterministic telemetry keep gate)
 * 4. GroundTruthAccuracyEvaluator (Exact ground-truth category convergence)
 * 5. SingleMetricThresholdEvaluator (Configurable numeric threshold gate)
 * 6. RubricScoreParser (Structured LLM judge rubric extraction)
 * 7. BinaryJudgeEvaluator (Prompt-driven LLM-as-a-judge with binary verdict)
 * 8. WeightedMultiCriteriaEvaluator (Multi-dimension score aggregator)
 * 9. ShortCircuitCompositeEvaluator (Chained evaluators with fast fail)
 * 10. BatchEvaluationRunner (Benchmark suite runner with aggregate metrics)
 */
public class EvaluatorUnderTest {

    /**
     * Scenario 1: Spring AI RelevancyEvaluator Adapter.
     * <p>
     * Instructions:
     * - Build a RelevancyEvaluator using RelevancyEvaluator.builder().chatClientBuilder(chatClientBuilder).build()
     * (or new RelevancyEvaluator(chatClientBuilder)).
     * - Construct an EvaluationRequest using query, contextDocs, and response.
     * - Call relevancyEvaluator.evaluate(request) and return the EvaluationResponse.
     */
    public static EvaluationResponse evaluateRelevancy(
            ChatClient.Builder chatClientBuilder,
            String query,
            String response,
            List<Document> contextDocs
    ) {
        // DEFECT (Scenario 1): Returns dummy failing response without invoking Spring AI RelevancyEvaluator
        return new EvaluationResponse(false, 0.0f, "Unimplemented", Map.of());
    }

    /**
     * Scenario 2: Spring AI FactCheckingEvaluator Adapter.
     * <p>
     * Instructions:
     * - Build a FactCheckingEvaluator using FactCheckingEvaluator.builder(chatClientBuilder).build().
     * - Construct an EvaluationRequest using contextDocs (dataList) and claim (responseContent).
     * - Call factCheckingEvaluator.evaluate(request) and return the EvaluationResponse.
     */
    public static EvaluationResponse evaluateFactuality(
            ChatClient.Builder chatClientBuilder,
            String claim,
            List<Document> contextDocs
    ) {
        // DEFECT (Scenario 2): Returns dummy failing response without invoking Spring AI FactCheckingEvaluator
        return new EvaluationResponse(false, 0.0f, "Unimplemented", Map.of());
    }

    /**
     * Scenario 3: Deterministic Noise Floor Threshold Evaluator.
     * <p>
     * Instructions:
     * - Evaluate TelemetryDelta against NoiseFloors (from Diagnostician KeepRule v2).
     * - Guard check: if failRateAfter > failRateBefore, return EvaluationResponse(false, 0.0f,
     * "REJECTED: failRate worsened from " + delta.failRateBefore() + " to " + delta.failRateAfter(), metadata).
     * - Improvement check:
     * rpsImproved = delta.rpsDelta() > floors.rpsFloor()
     * p95Improved = delta.p95DeltaMs() > floors.p95FloorMs()
     * - If (rpsImproved || p95Improved):
     * return EvaluationResponse(true, 1.0f, "KEPT: " + (rpsImproved ? "RPS" : "P95") + " improved beyond noise floor", metadata)
     * - Else:
     * return EvaluationResponse(false, 0.0f, "REJECTED: deltas did not clear noise floors", metadata)
     */
    public static class NoiseFloorThresholdEvaluator implements Evaluator {
        private final NoiseFloors floors;

        public NoiseFloorThresholdEvaluator(NoiseFloors floors) {
            this.floors = Objects.requireNonNull(floors, "floors must not be null");
        }

        public EvaluationResponse evaluateTelemetry(TelemetryDelta delta) {
            // DEFECT (Scenario 3): Always rejects without evaluating noise floors
            return new EvaluationResponse(false, 0.0f, "REJECTED: noise floor not cleared", Map.of());
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            return evaluateTelemetry(new TelemetryDelta(0, 0, 0, 0, 0));
        }
    }

    /**
     * Scenario 4: Ground-Truth Category Accuracy Evaluator.
     * <p>
     * Instructions:
     * - Compare predictedCategory against groundTruthCategory (case-insensitive after trim).
     * - If match:
     * return EvaluationResponse(true, 1.0f, "ACCURATE: predicted '" + predicted.trim() + "' matched ground truth '" + truth.trim() + "'", metadata)
     * - If mismatch:
     * return EvaluationResponse(false, 0.0f, "MISMATCH: predicted '" + predicted.trim() + "' != ground truth '" + truth.trim() + "'", metadata)
     */
    public static class GroundTruthAccuracyEvaluator implements Evaluator {

        public EvaluationResponse evaluateGroundTruth(String predictedCategory, String groundTruthCategory) {
            // DEFECT (Scenario 4): Hardcoded mismatch
            return new EvaluationResponse(false, 0.0f, "MISMATCH", Map.of());
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            return evaluateGroundTruth(request.getResponseContent(), request.getUserText());
        }
    }

    /**
     * Scenario 5: Configurable Numeric Single Metric Threshold Evaluator.
     * <p>
     * Instructions:
     * - Constructor validates 0.0f <= passingThreshold && passingThreshold <= 1.0f, else throws IllegalArgumentException.
     * - evaluateScore(actualScore):
     * - if actualScore >= passingThreshold:
     * return EvaluationResponse(true, actualScore, "PASSED: score " + actualScore + " >= threshold " + passingThreshold, metadata)
     * - else:
     * return EvaluationResponse(false, actualScore, "FAILED: score " + actualScore + " < threshold " + passingThreshold, metadata)
     */
    public static class SingleMetricThresholdEvaluator implements Evaluator {
        private final float passingThreshold;

        public SingleMetricThresholdEvaluator(float passingThreshold) {
            // DEFECT (Scenario 5): Missing bounds validation
            this.passingThreshold = passingThreshold;
        }

        public EvaluationResponse evaluateScore(float actualScore) {
            // DEFECT (Scenario 5): Always passes
            return new EvaluationResponse(true, actualScore, "PASSED", Map.of());
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            float score = 0.0f;
            try {
                score = Float.parseFloat(request.getResponseContent());
            } catch (Exception ignored) {
            }
            return evaluateScore(score);
        }
    }

    /**
     * Scenario 6: Structured LLM Judge Rubric Score Parser.
     * <p>
     * Instructions:
     * - Parse text formatted as "SCORE: <value>" and "REASON: <text>" (case-insensitive).
     * - Supported value formats:
     *   - Fraction: "4/5" -> 4.0 / 5.0 = 0.80f
     *   - Fraction: "8/10" -> 8.0 / 10.0 = 0.80f
     *   - Decimal: "0.85" -> 0.85f
     *   - Integer: "4" (assumed out of 5 if > 1.0) -> 4.0 / 5.0 = 0.80f
     * - Extract reason text following "REASON:" (trimmed, default to "Unspecified" if missing).
     * - Return new RubricScore(normalizedScore, reason).
     * - If llmOutput is null or blank, or if "SCORE:" is missing or unparseable:
     *   throw IllegalArgumentException.
     * <p>
     * Note: Pre-compiled {@link #SCORE_PATTERN} and {@link #REASON_PATTERN} constants are provided below.
     */
    public static class RubricScoreParser {

        /**
         * Pre-compiled Regex Pattern for parsing score lines:
         * Group 1: Numerator or decimal value. Group 2: Optional denominator after slash.
         */
        public static final Pattern SCORE_PATTERN =
                Pattern.compile("(?i)score:\\s*([0-9]+(?:\\.[0-9]+)?)(?:\\s*/\\s*([0-9]+(?:\\.[0-9]+)?))?");

        /**
         * Pre-compiled Regex Pattern for parsing reason lines:
         * Group 1: Everything following "REASON:".
         */
        public static final Pattern REASON_PATTERN =
                Pattern.compile("(?i)reason:\\s*(.*)", Pattern.DOTALL);

        public static RubricScore parse(String llmOutput) {
            // DEFECT (Scenario 6): Always returns 0.0 without parsing
            if (llmOutput == null || llmOutput.isBlank()) {
                throw new IllegalArgumentException("llmOutput must not be blank");
            }
            return new RubricScore(0.0f, "Unparsed");
        }
    }

    /**
     * Scenario 7: Prompt-Driven Binary LLM Judge Evaluator.
     * <p>
     * Instructions:
     * - Prompt the injected ChatClient:
     * "Evaluate if the following response correctly answers the user query.\n" +
     * "Query: " + request.getUserText() + "\n" +
     * "Response: " + request.getResponseContent() + "\n" +
     * "Instructions: Respond with PASS or FAIL on the first line, followed by reasoning."
     * - Call chatClient.prompt().user(prompt).call().content().
     * - If output stripped starts with or contains "PASS" (case-insensitive):
     * isPass = true, score = 1.0f
     * - Else:
     * isPass = false, score = 0.0f
     * - Return EvaluationResponse(isPass, score, rawOutput, Map.of()).
     */
    public static class BinaryJudgeEvaluator implements Evaluator {
        private final ChatClient chatClient;

        public BinaryJudgeEvaluator(ChatClient chatClient) {
            this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            // DEFECT (Scenario 7): Doesn't call chatClient
            return new EvaluationResponse(false, 0.0f, "Unimplemented", Map.of());
        }
    }

    /**
     * Scenario 8: Weighted Multi-Criteria Evaluator.
     * <p>
     * Instructions:
     * - Constructor:
     * - Validates criteria is not null and not empty.
     * - Validates that sum of weights == 1.0f (Math.abs(sum - 1.0f) < 0.001f).
     * Else throws IllegalArgumentException("Criteria weights must sum to 1.0, current sum: " + sum).
     * - Validates 0.0f <= passingThreshold && passingThreshold <= 1.0f.
     * - evaluate(criterionScores):
     * - Computes compositeScore = sum(criterionScores.getOrDefault(c.name(), 0.0f) * c.weight()).
     * - passed = compositeScore >= passingThreshold.
     * - Returns MultiCriteriaResult(compositeScore, passed, detailedList, passed ? "PASS" : "FAIL").
     */
    public static class WeightedMultiCriteriaEvaluator {
        private final List<EvaluationCriterion> criteria;
        private final float passingThreshold;

        public WeightedMultiCriteriaEvaluator(List<EvaluationCriterion> criteria, float passingThreshold) {
            // DEFECT (Scenario 8): Missing weight validation
            this.criteria = criteria != null ? List.copyOf(criteria) : List.of();
            this.passingThreshold = passingThreshold;
        }

        public MultiCriteriaResult evaluate(Map<String, Float> criterionScores) {
            // DEFECT (Scenario 8): Always returns 0.0f composite score
            return new MultiCriteriaResult(0.0f, false, List.of(), "FAIL");
        }
    }

    /**
     * Scenario 9: Short-Circuiting Composite Evaluator Chain.
     * <p>
     * Instructions:
     * - Constructor takes List<Evaluator> evaluators.
     * - evaluate(request):
     * - Iterates through evaluators in list order.
     * - Evaluates each evaluator.evaluate(request).
     * - If any evaluator returns !response.isPass():
     * IMMEDIATELY returns that failing response (short-circuit, do NOT execute remaining evaluators).
     * - If all pass:
     * returns new EvaluationResponse(true, 1.0f, "All " + evaluators.size() + " evaluators passed",
     * Map.of("stepsExecuted", evaluators.size()));
     */
    public static class ShortCircuitCompositeEvaluator implements Evaluator {
        private final List<Evaluator> evaluators;

        public ShortCircuitCompositeEvaluator(List<Evaluator> evaluators) {
            this.evaluators = evaluators != null ? List.copyOf(evaluators) : List.of();
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            // DEFECT (Scenario 9): Does not evaluate chain or short-circuit
            return new EvaluationResponse(false, 0.0f, "Unimplemented", Map.of());
        }
    }

    /**
     * Scenario 10: Batch Evaluation Suite Runner with Threshold Gate.
     * <p>
     * Instructions:
     * - runBatch(evaluator, requests):
     * - Validates evaluator != null and requests != null && !requests.isEmpty(), else throws IllegalArgumentException.
     * - Evaluates each request with evaluator.evaluate(req).
     * - Calculates:
     * totalRequests = requests.size()
     * passCount = count of resp.isPass()
     * failCount = totalRequests - passCount
     * passRate = passCount / (float) totalRequests
     * averageScore = sum(scores) / totalRequests
     * - Returns new BenchmarkSummary(totalRequests, passCount, failCount, passRate, averageScore).
     * - enforceThreshold(summary, minPassRate):
     * - If summary.passRate() < minPassRate:
     * throws EvaluationGateException("Evaluation threshold breach: passRate " + summary.passRate() + " < required " + minPassRate).
     */
    public static class BatchEvaluationRunner {
        public static BenchmarkSummary runBatch(Evaluator evaluator, List<EvaluationRequest> requests) {
            // DEFECT (Scenario 10): Returns empty summary without evaluating requests
            if (evaluator == null || requests == null || requests.isEmpty()) {
                throw new IllegalArgumentException("evaluator and non-empty requests required");
            }
            return new BenchmarkSummary(0, 0, 0, 0.0f, 0.0f);
        }

        public static void enforceThreshold(BenchmarkSummary summary, float minPassRate) {
            // DEFECT (Scenario 10): Does not enforce threshold or throw EvaluationGateException
        }
    }
}
