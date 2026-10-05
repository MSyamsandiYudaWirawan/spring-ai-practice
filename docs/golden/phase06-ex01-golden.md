# Golden Solution: Phase 06 Exercise 01 (Deterministic Evaluators & LLM-as-a-Judge)

## Overview
This golden solution implements all 10 scenarios of `phase06-ex01-evaluators`, providing complete implementations of Spring AI built-in evaluators (`RelevancyEvaluator`, `FactCheckingEvaluator`), deterministic telemetry noise floor gates (`NoiseFloorThresholdEvaluator`), ground-truth convergence verification (`GroundTruthAccuracyEvaluator`), numeric threshold gates, structured LLM judge rubric parsing, prompt-driven binary judges, weighted multi-criteria evaluators, short-circuiting chains, and benchmark batch suite runners.

Verified: `10 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase06-ex01-evaluators/src/main/java/phase06/EvaluatorUnderTest.java`

```java
package phase06;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import phase06.EvaluatorContracts.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Golden implementation for Phase 06 Exercise 01.
 */
public class EvaluatorUnderTest {

    /**
     * Scenario 1: Spring AI RelevancyEvaluator Adapter.
     */
    public static EvaluationResponse evaluateRelevancy(
            ChatClient.Builder chatClientBuilder,
            String query,
            String response,
            List<Document> contextDocs
    ) {
        RelevancyEvaluator evaluator = RelevancyEvaluator.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        EvaluationRequest request = new EvaluationRequest(query, contextDocs, response);
        return evaluator.evaluate(request);
    }

    /**
     * Scenario 2: Spring AI FactCheckingEvaluator Adapter.
     */
    public static EvaluationResponse evaluateFactuality(
            ChatClient.Builder chatClientBuilder,
            String claim,
            List<Document> contextDocs
    ) {
        FactCheckingEvaluator evaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();
        EvaluationRequest request = new EvaluationRequest(contextDocs, claim);
        return evaluator.evaluate(request);
    }

    /**
     * Scenario 3: Deterministic Noise Floor Threshold Evaluator.
     */
    public static class NoiseFloorThresholdEvaluator implements Evaluator {
        private final NoiseFloors floors;

        public NoiseFloorThresholdEvaluator(NoiseFloors floors) {
            this.floors = Objects.requireNonNull(floors, "floors must not be null");
        }

        public EvaluationResponse evaluateTelemetry(TelemetryDelta delta) {
            Objects.requireNonNull(delta, "delta must not be null");
            if (delta.failRateAfter() > delta.failRateBefore()) {
                return new EvaluationResponse(false, 0.0f,
                        "REJECTED: failRate worsened from " + delta.failRateBefore() + " to " + delta.failRateAfter(),
                        Map.of("reason", "failRate worsened"));
            }

            double p95Improve = delta.p95DeltaMs();
            double rpsImprove = delta.rpsDelta();
            boolean rpsImproved = rpsImprove > floors.rpsFloor();
            boolean p95Improved = p95Improve > floors.p95FloorMs();

            if (rpsImproved || p95Improved) {
                String type = rpsImproved ? "RPS" : "P95";
                return new EvaluationResponse(true, 1.0f,
                        "KEPT: " + type + " improved beyond noise floor",
                        Map.of("keepType", type, "rpsDelta", rpsImprove, "p95Delta", p95Improve));
            }

            return new EvaluationResponse(false, 0.0f,
                    "REJECTED: deltas did not clear noise floors",
                    Map.of("rpsDelta", rpsImprove, "p95Delta", p95Improve));
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            return evaluateTelemetry(new TelemetryDelta(0, 0, 0, 0, 0));
        }
    }

    /**
     * Scenario 4: Ground-Truth Category Accuracy Evaluator.
     */
    public static class GroundTruthAccuracyEvaluator implements Evaluator {

        public EvaluationResponse evaluateGroundTruth(String predictedCategory, String groundTruthCategory) {
            Objects.requireNonNull(predictedCategory, "predictedCategory must not be null");
            Objects.requireNonNull(groundTruthCategory, "groundTruthCategory must not be null");

            boolean matches = predictedCategory.trim().equalsIgnoreCase(groundTruthCategory.trim());
            if (matches) {
                return new EvaluationResponse(true, 1.0f,
                        "ACCURATE: predicted '" + predictedCategory.trim() + "' matched ground truth '" + groundTruthCategory.trim() + "'",
                        Map.of("accurate", true));
            } else {
                return new EvaluationResponse(false, 0.0f,
                        "MISMATCH: predicted '" + predictedCategory.trim() + "' != ground truth '" + groundTruthCategory.trim() + "'",
                        Map.of("accurate", false));
            }
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            return evaluateGroundTruth(request.getResponseContent(), request.getUserText());
        }
    }

    /**
     * Scenario 5: Configurable Numeric Single Metric Threshold Evaluator.
     */
    public static class SingleMetricThresholdEvaluator implements Evaluator {
        private final float passingThreshold;

        public SingleMetricThresholdEvaluator(float passingThreshold) {
            if (passingThreshold < 0.0f || passingThreshold > 1.0f) {
                throw new IllegalArgumentException("passingThreshold must be between 0.0 and 1.0, got: " + passingThreshold);
            }
            this.passingThreshold = passingThreshold;
        }

        public EvaluationResponse evaluateScore(float actualScore) {
            boolean passed = actualScore >= passingThreshold;
            String feedback = passed
                    ? "PASSED: score " + actualScore + " >= threshold " + passingThreshold
                    : "FAILED: score " + actualScore + " < threshold " + passingThreshold;
            return new EvaluationResponse(passed, actualScore, feedback, Map.of("threshold", passingThreshold));
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            float score = 0.0f;
            try {
                score = Float.parseFloat(request.getResponseContent());
            } catch (Exception ignored) {}
            return evaluateScore(score);
        }
    }

    /**
     * Scenario 6: Structured LLM Judge Rubric Score Parser.
     * <p>
     * Pattern Breakdown:
     * - (?i)score:\s*                  -> Case-insensitive prefix "score:" with optional trailing spaces
     * - ([0-9]+(?:\.[0-9]+)?)          -> Group 1: Captures integer or decimal numerator/score (e.g. "4", "0.95")
     * - (?:\s*/\s*([0-9]+(?:\.[0-9]+)?))? -> Optional non-capturing group for "/ denom", Group 2 = denominator ("5")
     * <p>
     * Alternative Non-Regex Approach:
     * You can also implement this without regex by iterating {@code llmOutput.lines()}, checking
     * {@code line.toLowerCase().startsWith("score:")}, splitting by ':', and checking for '/' in the value.
     */
    public static class RubricScoreParser {
        public static final Pattern SCORE_PATTERN =
                Pattern.compile("(?i)score:\\s*([0-9]+(?:\\.[0-9]+)?)(?:\\s*/\\s*([0-9]+(?:\\.[0-9]+)?))?");
        public static final Pattern REASON_PATTERN =
                Pattern.compile("(?i)reason:\\s*(.*)", Pattern.DOTALL);

        public static RubricScore parse(String llmOutput) {
            if (llmOutput == null || llmOutput.isBlank()) {
                throw new IllegalArgumentException("llmOutput must not be blank");
            }

            Matcher scoreMatcher = SCORE_PATTERN.matcher(llmOutput);
            if (!scoreMatcher.find()) {
                throw new IllegalArgumentException("Unable to parse rubric score from: " + llmOutput);
            }

            float num = Float.parseFloat(scoreMatcher.group(1));
            float normalized;
            if (scoreMatcher.group(2) != null) {
                float denom = Float.parseFloat(scoreMatcher.group(2));
                normalized = num / denom;
            } else if (num > 1.0f) {
                normalized = num / 5.0f;
            } else {
                normalized = num;
            }
            normalized = Math.clamp(normalized, 0.0f, 1.0f);

            String reason = "Unspecified";
            Matcher reasonMatcher = REASON_PATTERN.matcher(llmOutput);
            if (reasonMatcher.find()) {
                reason = reasonMatcher.group(1).trim();
            }

            return new RubricScore(normalized, reason);
        }
    }

    /**
     * Scenario 7: Prompt-Driven Binary LLM Judge Evaluator.
     */
    public static class BinaryJudgeEvaluator implements Evaluator {
        private final ChatClient chatClient;

        public BinaryJudgeEvaluator(ChatClient chatClient) {
            this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            String prompt = "Evaluate if the following response correctly answers the user query.\n" +
                    "Query: " + request.getUserText() + "\n" +
                    "Response: " + request.getResponseContent() + "\n" +
                    "Instructions: Respond with PASS or FAIL on the first line, followed by reasoning.";

            String output = chatClient.prompt().user(prompt).call().content();
            if (output == null) {
                return new EvaluationResponse(false, 0.0f, "Empty judge response", Map.of());
            }

            boolean isPass = output.strip().toUpperCase().startsWith("PASS") || output.toUpperCase().contains("PASS");
            float score = isPass ? 1.0f : 0.0f;
            return new EvaluationResponse(isPass, score, output.strip(), Map.of());
        }
    }

    /**
     * Scenario 8: Weighted Multi-Criteria Evaluator.
     */
    public static class WeightedMultiCriteriaEvaluator {
        private final List<EvaluationCriterion> criteria;
        private final float passingThreshold;

        public WeightedMultiCriteriaEvaluator(List<EvaluationCriterion> criteria, float passingThreshold) {
            if (criteria == null || criteria.isEmpty()) {
                throw new IllegalArgumentException("criteria must not be empty");
            }
            float sum = 0.0f;
            for (EvaluationCriterion c : criteria) {
                sum += c.weight();
            }
            if (Math.abs(sum - 1.0f) > 0.001f) {
                throw new IllegalArgumentException("Criteria weights must sum to 1.0, current sum: " + sum);
            }
            if (passingThreshold < 0.0f || passingThreshold > 1.0f) {
                throw new IllegalArgumentException("passingThreshold must be between 0.0 and 1.0, got: " + passingThreshold);
            }
            this.criteria = List.copyOf(criteria);
            this.passingThreshold = passingThreshold;
        }

        public MultiCriteriaResult evaluate(Map<String, Float> criterionScores) {
            float composite = 0.0f;
            List<CriterionScore> list = new ArrayList<>();
            for (EvaluationCriterion c : criteria) {
                float s = criterionScores.getOrDefault(c.name(), 0.0f);
                composite += s * c.weight();
                list.add(new CriterionScore(c.name(), s, "Weight: " + c.weight()));
            }

            boolean passed = composite >= passingThreshold;
            return new MultiCriteriaResult(composite, passed, list, passed ? "PASS" : "FAIL");
        }
    }

    /**
     * Scenario 9: Short-Circuiting Composite Evaluator Chain.
     */
    public static class ShortCircuitCompositeEvaluator implements Evaluator {
        private final List<Evaluator> evaluators;

        public ShortCircuitCompositeEvaluator(List<Evaluator> evaluators) {
            this.evaluators = evaluators != null ? List.copyOf(evaluators) : List.of();
        }

        @Override
        public EvaluationResponse evaluate(EvaluationRequest request) {
            int executed = 0;
            for (Evaluator eval : evaluators) {
                executed++;
                EvaluationResponse resp = eval.evaluate(request);
                if (!resp.isPass()) {
                    return resp;
                }
            }
            return new EvaluationResponse(true, 1.0f, "All " + evaluators.size() + " evaluators passed",
                    Map.of("stepsExecuted", executed));
        }
    }

    /**
     * Scenario 10: Batch Evaluation Suite Runner with Threshold Gate.
     */
    public static class BatchEvaluationRunner {
        public static BenchmarkSummary runBatch(Evaluator evaluator, List<EvaluationRequest> requests) {
            if (evaluator == null || requests == null || requests.isEmpty()) {
                throw new IllegalArgumentException("evaluator and non-empty requests required");
            }

            int total = requests.size();
            int pass = 0;
            float sumScore = 0.0f;
            for (EvaluationRequest req : requests) {
                EvaluationResponse resp = evaluator.evaluate(req);
                if (resp.isPass()) {
                    pass++;
                }
                sumScore += resp.getScore();
            }

            float passRate = pass / (float) total;
            float avg = sumScore / total;
            return new BenchmarkSummary(total, pass, total - pass, passRate, avg);
        }

        public static void enforceThreshold(BenchmarkSummary summary, float minPassRate) {
            if (summary.passRate() < minPassRate) {
                throw new EvaluationGateException("Evaluation threshold breach: passRate " + summary.passRate() + " < required " + minPassRate);
            }
        }
    }
}
```
