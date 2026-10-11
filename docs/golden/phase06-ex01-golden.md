# Phase 06 Exercise 01 — Deterministic Evaluators & Benchmark Gates (Golden Solution)

**Path:** `phase06-ex01-evaluators/src/main/java/phase06/EvaluatorUnderTest.java`  
**All 15 Scenarios Verified Passing.**

```java
package phase06;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.util.JsonHelper;
import phase06.EvaluatorContracts.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exercise implementation under test for Phase 06 Exercise 01:
 * Deterministic Evaluators, Spring AI Evaluator Interface & Foundational LLM-as-a-Judge.
 * <p>
 * 15 repetitive muscle-memory drills across 5 core evaluation topics (3 repetitions each).
 */
public class EvaluatorUnderTest {

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    // =========================================================================
    // TOPIC 1: Spring AI Evaluator Interface & Request/Response Basics (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 01: Direct EvaluationRequest & Threshold Evaluation.
     */
    public static EvaluationResponse evaluateThreshold(
            String userQuery,
            String modelResponse,
            float score,
            float threshold
    ) {
        if (userQuery == null || userQuery.isBlank() || modelResponse == null || modelResponse.isBlank()) {
            throw new IllegalArgumentException("userQuery and modelResponse must not be null/blank");
        }
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        boolean isPass = score >= threshold;
        return new EvaluationResponse(isPass, score, isPass ? "PASSED" : "FAILED", Map.of("threshold", threshold));
    }

    /**
     * Scenario 02: Context-Enriched EvaluationRequest (RAG Triad Triplet).
     */
    public static EvaluationResponse evaluateContextGrounded(
            String userQuery,
            List<Document> contextDocs,
            String modelResponse,
            float score,
            float threshold
    ) {
        if (userQuery == null || userQuery.isBlank() || contextDocs == null || modelResponse == null || modelResponse.isBlank()) {
            throw new IllegalArgumentException("userQuery, contextDocs, and modelResponse must not be null/blank");
        }
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        boolean isPass = score >= threshold;
        return new EvaluationResponse(
                isPass,
                score,
                isPass ? "GROUNDED" : "UNGROUNDED",
                Map.of("contextDocCount", contextDocs.size(), "threshold", threshold)
        );
    }

    /**
     * Scenario 03: Spring AI Built-in Evaluator Adapter with Fallback Guard.
     */
    public static EvaluationResponse evaluateRelevancy(
            ChatClient.Builder chatClientBuilder,
            String query,
            String response,
            List<Document> contextDocs
    ) {
        if (chatClientBuilder == null) {
            throw new IllegalArgumentException("chatClientBuilder must not be null");
        }
        RelevancyEvaluator relevancyEvaluator = RelevancyEvaluator.builder().chatClientBuilder(chatClientBuilder).build();
        EvaluationRequest request = new EvaluationRequest(query, contextDocs != null ? contextDocs : List.of(), response);
        try {
            return relevancyEvaluator.evaluate(request);
        } catch (Throwable t) {
            return new EvaluationResponse(false, 0.0f, "Evaluator error: " + t.getMessage(), Map.of());
        }
    }

    // =========================================================================
    // TOPIC 2: Deterministic Metric & Accuracy Evaluators (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 04: Exact Match Ground Truth Evaluator.
     */
    public static class GroundTruthAccuracyEvaluator implements Evaluator {
        public EvaluationResponse evaluateGroundTruth(String predictedAnswer, String expectedGroundTruth) {
            if (predictedAnswer == null || expectedGroundTruth == null) {
                throw new IllegalArgumentException("predictedAnswer and expectedGroundTruth must not be null");
            }
            boolean match = predictedAnswer.trim().equalsIgnoreCase(expectedGroundTruth.trim());
            return new EvaluationResponse(
                    match,
                    match ? 1.0f : 0.0f,
                    match ? "MATCH" : "MISMATCH: expected '" + expectedGroundTruth.trim() + "', got '" + predictedAnswer.trim() + "'",
                    Map.of()
            );
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            return evaluateGroundTruth(request.getResponseContent(), request.getUserText());
        }
    }

    /**
     * Scenario 05: Categorical Convergence Evaluator.
     */
    public static class CategoricalConvergenceEvaluator implements Evaluator {
        public EvaluationResponse evaluateCategory(String predictedCategory, Set<String> allowedCategories) {
            if (predictedCategory == null || allowedCategories == null || allowedCategories.isEmpty()) {
                throw new IllegalArgumentException("predictedCategory and allowedCategories must not be null/empty");
            }
            boolean valid = allowedCategories.stream().anyMatch(c -> c.equalsIgnoreCase(predictedCategory.trim()));
            return new EvaluationResponse(
                    valid,
                    valid ? 1.0f : 0.0f,
                    valid ? "VALID_CATEGORY: " + predictedCategory.trim() : "INVALID_CATEGORY: " + predictedCategory.trim(),
                    Map.of()
            );
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            return evaluateCategory(request.getResponseContent(), Set.of(request.getUserText().split(",")));
        }
    }

    /**
     * Scenario 06: Bounded Numeric Score Evaluator.
     */
    public static class BoundedScoreEvaluator implements Evaluator {
        public EvaluationResponse evaluateBounds(float actualScore, float minScore, float maxScore) {
            if (minScore < 0.0f || maxScore > 1.0f || minScore > maxScore) {
                throw new IllegalArgumentException("Invalid bounds: [" + minScore + ", " + maxScore + "]");
            }
            boolean within = actualScore >= minScore && actualScore <= maxScore;
            return new EvaluationResponse(
                    within,
                    actualScore,
                    within ? "WITHIN_BOUNDS" : "OUT_OF_BOUNDS",
                    Map.of("min", minScore, "max", maxScore)
            );
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            float val = Float.parseFloat(request.getResponseContent());
            return evaluateBounds(val, 0.0f, 1.0f);
        }
    }

    // =========================================================================
    // TOPIC 3: LLM Rubric Parsing & Score Extraction (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 07: Single Numeric Rubric Score Parser.
     */
    public static class NumericRubricParser {
        public static final Pattern SCORE_PATTERN =
                Pattern.compile("(?i)score:\\s*([0-9]+(?:\\.[0-9]+)?)(?:\\s*/\\s*([0-9]+(?:\\.[0-9]+)?))?");
        public static final Pattern REASON_PATTERN =
                Pattern.compile("(?i)reason:\\s*(.*)", Pattern.DOTALL);

        public static RubricScore parseScore(String llmOutput) {
            if (llmOutput == null || llmOutput.isBlank()) {
                throw new IllegalArgumentException("llmOutput must not be blank");
            }
            Matcher scoreMatcher = SCORE_PATTERN.matcher(llmOutput);
            if (!scoreMatcher.find()) {
                throw new IllegalArgumentException("Missing SCORE in output: " + llmOutput);
            }
            float numerator = Float.parseFloat(scoreMatcher.group(1));
            float score;
            if (scoreMatcher.group(2) != null) {
                float denominator = Float.parseFloat(scoreMatcher.group(2));
                score = numerator / denominator;
            } else if (numerator > 1.0f) {
                score = numerator / 5.0f;
            } else {
                score = numerator;
            }

            String reason = "Unspecified";
            Matcher reasonMatcher = REASON_PATTERN.matcher(llmOutput);
            if (reasonMatcher.find()) {
                reason = reasonMatcher.group(1).trim();
            }
            return new RubricScore(score, reason);
        }
    }

    /**
     * Scenario 08: Multi-Field Verdict & Reasoning Rubric Parser.
     */
    public static class VerdictRubricParser {
        public static final Pattern VERDICT_PATTERN =
                Pattern.compile("(?i)verdict:\\s*(PASS|FAIL)");
        public static final Pattern REASON_PATTERN =
                Pattern.compile("(?i)reason:\\s*(.*)", Pattern.DOTALL);

        public static RubricVerdict parseVerdict(String llmOutput) {
            if (llmOutput == null || llmOutput.isBlank()) {
                throw new IllegalArgumentException("llmOutput must not be blank");
            }
            Matcher verdictMatcher = VERDICT_PATTERN.matcher(llmOutput);
            if (!verdictMatcher.find()) {
                throw new IllegalArgumentException("Missing VERDICT (PASS/FAIL) in output: " + llmOutput);
            }
            boolean pass = "PASS".equalsIgnoreCase(verdictMatcher.group(1));

            String reason = "Unspecified";
            Matcher reasonMatcher = REASON_PATTERN.matcher(llmOutput);
            if (reasonMatcher.find()) {
                reason = reasonMatcher.group(1).trim();
            }
            return new RubricVerdict(pass, reason);
        }
    }

    /**
     * Scenario 09: Structured JSON Rubric Parser.
     */
    public static class JsonRubricParser {
        public static StructuredRubricResult parseJsonRubric(String llmOutput) {
            if (llmOutput == null || llmOutput.isBlank()) {
                throw new IllegalArgumentException("llmOutput must not be blank");
            }
            int start = llmOutput.indexOf('{');
            int end = llmOutput.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalArgumentException("No JSON object found in output: " + llmOutput);
            }
            String clean = llmOutput.substring(start, end + 1).trim();

            Map<String, Object> map = JSON_HELPER.fromJsonToMap(clean);
            if (map == null || !map.containsKey("score") || !map.containsKey("verdict")) {
                throw new IllegalArgumentException("JSON must contain 'score' and 'verdict': " + llmOutput);
            }
            float score = ((Number) map.get("score")).floatValue();
            boolean verdict = (Boolean) map.get("verdict");
            String reason = (String) map.getOrDefault("reason", "Unspecified");
            return new StructuredRubricResult(score, verdict, reason);
        }
    }

    // =========================================================================
    // TOPIC 4: Composite & Chained Evaluators (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Weighted Multi-Criteria Evaluator.
     */
    public static class WeightedMultiCriteriaEvaluator {
        private final List<EvaluationCriterion> criteria;
        private final float passingThreshold;

        public WeightedMultiCriteriaEvaluator(List<EvaluationCriterion> criteria, float passingThreshold) {
            if (criteria == null || criteria.isEmpty()) {
                throw new IllegalArgumentException("criteria must not be null/empty");
            }
            float sum = 0.0f;
            for (EvaluationCriterion c : criteria) {
                sum += c.weight();
            }
            if (Math.abs(sum - 1.0f) > 0.001f) {
                throw new IllegalArgumentException("Criteria weights must sum to 1.0, current sum: " + sum);
            }
            if (passingThreshold < 0.0f || passingThreshold > 1.0f) {
                throw new IllegalArgumentException("passingThreshold must be between 0.0 and 1.0");
            }
            this.criteria = List.copyOf(criteria);
            this.passingThreshold = passingThreshold;
        }

        public MultiCriteriaResult evaluate(Map<String, Float> criterionScores) {
            float composite = 0.0f;
            List<CriterionScore> detailed = new ArrayList<>();
            for (EvaluationCriterion c : criteria) {
                float s = criterionScores != null ? criterionScores.getOrDefault(c.name(), 0.0f) : 0.0f;
                composite += s * c.weight();
                detailed.add(new CriterionScore(c.name(), s, "weight=" + c.weight()));
            }
            boolean passed = composite >= passingThreshold;
            return new MultiCriteriaResult(composite, passed, detailed, passed ? "PASS" : "FAIL");
        }
    }

    /**
     * Scenario 11: Fail-Fast Short-Circuit Composite Evaluator.
     */
    public static class ShortCircuitCompositeEvaluator implements Evaluator {
        private final List<Evaluator> evaluators;

        public ShortCircuitCompositeEvaluator(List<Evaluator> evaluators) {
            if (evaluators == null || evaluators.isEmpty()) {
                throw new IllegalArgumentException("evaluators must not be null or empty");
            }
            this.evaluators = List.copyOf(evaluators);
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            for (Evaluator eval : evaluators) {
                EvaluationResponse res = eval.evaluate(request);
                if (res == null || !res.isPass()) {
                    return res;
                }
            }
            return new EvaluationResponse(true, 1.0f, "All " + evaluators.size() + " evaluators passed", Map.of("stepsExecuted", evaluators.size()));
        }
    }

    /**
     * Scenario 12: All-Must-Pass Strict Composite Evaluator.
     */
    public static class AllMustPassCompositeEvaluator implements Evaluator {
        private final List<Evaluator> evaluators;

        public AllMustPassCompositeEvaluator(List<Evaluator> evaluators) {
            if (evaluators == null || evaluators.isEmpty()) {
                throw new IllegalArgumentException("evaluators must not be null or empty");
            }
            this.evaluators = List.copyOf(evaluators);
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            boolean allPassed = true;
            float totalScore = 0.0f;
            int failedCount = 0;
            List<String> failedFeedbacks = new ArrayList<>();

            for (Evaluator eval : evaluators) {
                EvaluationResponse res = eval.evaluate(request);
                totalScore += res != null ? res.getScore() : 0.0f;
                if (res == null || !res.isPass()) {
                    allPassed = false;
                    failedCount++;
                    if (res != null && res.getFeedback() != null) {
                        failedFeedbacks.add(res.getFeedback());
                    }
                }
            }
            float avgScore = totalScore / evaluators.size();
            String feedback = allPassed ? "All passed" : String.join("; ", failedFeedbacks);
            return new EvaluationResponse(allPassed, avgScore, feedback, Map.of("totalEvaluators", evaluators.size(), "failedCount", failedCount));
        }
    }

    // =========================================================================
    // TOPIC 5: Benchmark Runner & Suite Aggregation (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Simple Batch Evaluation Runner.
     */
    public static class BatchEvaluationRunner {
        public static BenchmarkSummary runBatch(Evaluator evaluator, List<EvaluationRequest> requests) {
            if (evaluator == null || requests == null || requests.isEmpty()) {
                throw new IllegalArgumentException("evaluator and non-empty requests required");
            }
            int total = requests.size();
            int passCount = 0;
            float totalScore = 0.0f;
            for (EvaluationRequest req : requests) {
                EvaluationResponse res = evaluator.evaluate(req);
                if (res != null && res.isPass()) {
                    passCount++;
                }
                totalScore += res != null ? res.getScore() : 0.0f;
            }
            int failCount = total - passCount;
            float passRate = passCount / (float) total;
            float averageScore = totalScore / (float) total;
            return new BenchmarkSummary(total, passCount, failCount, passRate, averageScore);
        }
    }

    /**
     * Scenario 14: Multi-Metric Benchmark Suite Runner.
     */
    public static class BenchmarkSuiteRunner {
        public static Map<String, BenchmarkSummary> runSuite(
                Map<String, Evaluator> evaluators,
                List<EvaluationRequest> requests
        ) {
            if (evaluators == null || evaluators.isEmpty() || requests == null || requests.isEmpty()) {
                throw new IllegalArgumentException("evaluators and requests must not be null/empty");
            }
            Map<String, BenchmarkSummary> result = new LinkedHashMap<>();
            for (Map.Entry<String, Evaluator> entry : evaluators.entrySet()) {
                result.put(entry.getKey(), BatchEvaluationRunner.runBatch(entry.getValue(), requests));
            }
            return result;
        }
    }

    /**
     * Scenario 15: Quality Gate Threshold Enforcer.
     */
    public static class QualityGateEnforcer {
        public static void enforceGate(BenchmarkSummary summary, float minPassRate, float minAvgScore) {
            if (summary == null) {
                throw new IllegalArgumentException("summary must not be null");
            }
            if (minPassRate < 0.0f || minPassRate > 1.0f || minAvgScore < 0.0f || minAvgScore > 1.0f) {
                throw new IllegalArgumentException("Thresholds must be between 0.0 and 1.0");
            }
            if (summary.passRate() < minPassRate) {
                throw new EvaluationGateException("Evaluation threshold breach: passRate " + summary.passRate() + " < required " + minPassRate);
            }
            if (summary.averageScore() < minAvgScore) {
                throw new EvaluationGateException("Evaluation threshold breach: averageScore " + summary.averageScore() + " < required " + minAvgScore);
            }
        }
    }
}
```
