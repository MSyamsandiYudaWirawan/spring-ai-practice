package phase06;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import phase06.EvaluatorContracts.*;
import phase06.EvaluatorUnderTest.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gate Verifier for Phase 06 Exercise 01:
 * Deterministic Evaluators, Spring AI Evaluator Interface & Foundational LLM-as-a-Judge (15 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

    public static void main(String[] args) {
        Verifier verifier = new Verifier();
        List<ScenarioResult> results = verifier.runAll();
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        System.exit(anyFailed ? 1 : 0);
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runAll();
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        Assertions.assertFalse(anyFailed, "Verifier detected scenario failures. See printed report above.");
    }

    public List<ScenarioResult> runAll() {
        List<ScenarioResult> results = new ArrayList<>();

        // Topic 1: Spring AI Evaluator Interface & Request/Response Basics (Scenarios 1 - 3)
        results.add(verifyScenario01());
        results.add(verifyScenario02());
        results.add(verifyScenario03());

        // Topic 2: Deterministic Metric & Accuracy Evaluators (Scenarios 4 - 6)
        results.add(verifyScenario04());
        results.add(verifyScenario05());
        results.add(verifyScenario06());

        // Topic 3: LLM Rubric Parsing & Score Extraction (Scenarios 7 - 9)
        results.add(verifyScenario07());
        results.add(verifyScenario08());
        results.add(verifyScenario09());

        // Topic 4: Composite & Chained Evaluators (Scenarios 10 - 12)
        results.add(verifyScenario10());
        results.add(verifyScenario11());
        results.add(verifyScenario12());

        // Topic 5: Benchmark Runner & Suite Aggregation (Scenarios 13 - 15)
        results.add(verifyScenario13());
        results.add(verifyScenario14());
        results.add(verifyScenario15());

        return results;
    }

    // =========================================================================
    // TOPIC 1: Spring AI Evaluator Interface & Request/Response Basics (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario01() {
        String name = "EvaluationRequest (Direct Request & Threshold Gating)";
        try {
            EvaluationResponse passResp = EvaluatorUnderTest.evaluateThreshold("What is lock wait timeout?", "50 seconds", 0.90f, 0.80f);
            if (passResp == null || !passResp.isPass() || passResp.getScore() != 0.90f || !"PASSED".equals(passResp.getFeedback())) {
                return new ScenarioResult(1, name, false, "Expected pass=true, score=0.90, feedback=PASSED, got: " + passResp);
            }

            EvaluationResponse failResp = EvaluatorUnderTest.evaluateThreshold("What is lock wait timeout?", "10 ms", 0.40f, 0.80f);
            if (failResp == null || failResp.isPass() || !"FAILED".equals(failResp.getFeedback())) {
                return new ScenarioResult(1, name, false, "Expected pass=false on score < threshold, got: " + failResp);
            }

            try {
                EvaluatorUnderTest.evaluateThreshold("", "response", 0.5f, 0.5f);
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on blank query");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario02() {
        String name = "EvaluationRequest (Context-Enriched RAG Triplet)";
        try {
            List<Document> docs = List.of(
                    new Document("Doc 1: Database timeout is 50s"),
                    new Document("Doc 2: Service pool max is 20")
            );

            EvaluationResponse resp = EvaluatorUnderTest.evaluateContextGrounded(
                    "What is DB timeout?", docs, "Database timeout is 50s", 0.85f, 0.70f
            );

            if (resp == null || !resp.isPass() || !"GROUNDED".equals(resp.getFeedback())) {
                return new ScenarioResult(2, name, false, "Expected grounded pass response, got: " + resp);
            }

            if (!resp.getMetadata().containsKey("contextDocCount") || !Integer.valueOf(2).equals(resp.getMetadata().get("contextDocCount"))) {
                return new ScenarioResult(2, name, false, "Expected metadata contextDocCount=2, got: " + resp.getMetadata());
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario03() {
        String name = "RelevancyEvaluatorAdapter (Spring AI Built-in Adapter)";
        try {
            FakeEvaluatorChatModel fakeModel = new FakeEvaluatorChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            List<Document> docs = List.of(new Document("Cluster memory limit is 64GB"));

            fakeModel.enqueue("YES");
            EvaluationResponse passResp = EvaluatorUnderTest.evaluateRelevancy(
                    builder, "What is cluster memory limit?", "Cluster memory limit is 64GB", docs
            );
            if (passResp == null || !passResp.isPass()) {
                return new ScenarioResult(3, name, false, "Expected pass=true when model outputs YES, got: " + passResp);
            }

            fakeModel.enqueue("NO");
            EvaluationResponse failResp = EvaluatorUnderTest.evaluateRelevancy(
                    builder, "What is cluster memory limit?", "Today is sunny.", docs
            );
            if (failResp == null || failResp.isPass()) {
                return new ScenarioResult(3, name, false, "Expected pass=false when model outputs NO, got: " + failResp);
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 2: Deterministic Metric & Accuracy Evaluators (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario04() {
        String name = "GroundTruthAccuracy (Exact Match Comparison)";
        try {
            GroundTruthAccuracyEvaluator eval = new GroundTruthAccuracyEvaluator();

            EvaluationResponse match = eval.evaluateGroundTruth("DATABASE_LOCK", "database_lock  ");
            if (match == null || !match.isPass() || match.getScore() != 1.0f || !"MATCH".equals(match.getFeedback())) {
                return new ScenarioResult(4, name, false, "Expected MATCH with score 1.0, got: " + match);
            }

            EvaluationResponse mismatch = eval.evaluateGroundTruth("CPU_BURST", "database_lock");
            if (mismatch == null || mismatch.isPass() || mismatch.getScore() != 0.0f || !mismatch.getFeedback().startsWith("MISMATCH")) {
                return new ScenarioResult(4, name, false, "Expected MISMATCH with score 0.0, got: " + mismatch);
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario05() {
        String name = "CategoricalConvergence (Allowed Category Set Membership)";
        try {
            CategoricalConvergenceEvaluator eval = new CategoricalConvergenceEvaluator();
            Set<String> categories = Set.of("network", "database", "disk");

            EvaluationResponse valid = eval.evaluateCategory("Database", categories);
            if (valid == null || !valid.isPass() || valid.getScore() != 1.0f) {
                return new ScenarioResult(5, name, false, "Expected valid category pass, got: " + valid);
            }

            EvaluationResponse invalid = eval.evaluateCategory("memory", categories);
            if (invalid == null || invalid.isPass() || invalid.getScore() != 0.0f) {
                return new ScenarioResult(5, name, false, "Expected invalid category fail, got: " + invalid);
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario06() {
        String name = "BoundedScore (Numeric Threshold Window Verification)";
        try {
            BoundedScoreEvaluator eval = new BoundedScoreEvaluator();

            EvaluationResponse within = eval.evaluateBounds(0.75f, 0.70f, 0.90f);
            if (within == null || !within.isPass() || !"WITHIN_BOUNDS".equals(within.getFeedback())) {
                return new ScenarioResult(6, name, false, "Expected WITHIN_BOUNDS, got: " + within);
            }

            EvaluationResponse out = eval.evaluateBounds(0.65f, 0.70f, 0.90f);
            if (out == null || out.isPass() || !"OUT_OF_BOUNDS".equals(out.getFeedback())) {
                return new ScenarioResult(6, name, false, "Expected OUT_OF_BOUNDS, got: " + out);
            }

            try {
                eval.evaluateBounds(0.5f, 0.8f, 0.6f);
                return new ScenarioResult(6, name, false, "Expected IllegalArgumentException for min > max");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 3: LLM Rubric Parsing & Score Extraction (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario07() {
        String name = "NumericRubricParser (Regex Score Extraction)";
        try {
            RubricScore s1 = NumericRubricParser.parseScore("SCORE: 4/5\nREASON: Mostly accurate response.");
            if (s1 == null || Math.abs(s1.score() - 0.80f) > 0.001f || !s1.reason().contains("Mostly accurate")) {
                return new ScenarioResult(7, name, false, "Failed fraction 4/5 parsing: " + s1);
            }

            RubricScore s2 = NumericRubricParser.parseScore("score: 0.92\nreason: Highly detailed.");
            if (s2 == null || Math.abs(s2.score() - 0.92f) > 0.001f) {
                return new ScenarioResult(7, name, false, "Failed decimal 0.92 parsing: " + s2);
            }

            try {
                NumericRubricParser.parseScore("No score provided here");
                return new ScenarioResult(7, name, false, "Expected IllegalArgumentException when SCORE missing");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario08() {
        String name = "VerdictRubricParser (Multi-Field Verdict & Reasoning)";
        try {
            RubricVerdict v1 = VerdictRubricParser.parseVerdict("VERDICT: PASS\nREASON: Response matches ground truth.");
            if (v1 == null || !v1.pass() || !v1.reason().contains("matches ground truth")) {
                return new ScenarioResult(8, name, false, "Failed PASS verdict parsing: " + v1);
            }

            RubricVerdict v2 = VerdictRubricParser.parseVerdict("verdict: FAIL\nreason: Hallucinated metrics.");
            if (v2 == null || v2.pass() || !v2.reason().contains("Hallucinated")) {
                return new ScenarioResult(8, name, false, "Failed FAIL verdict parsing: " + v2);
            }

            try {
                VerdictRubricParser.parseVerdict("verdict: MAYBE\nreason: uncertain");
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException on non-PASS/FAIL verdict");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario09() {
        String name = "JsonRubricParser (Markdown Fence Stripping & JSON Extraction)";
        try {
            String jsonInput = "```json\n{\"score\": 0.88, \"verdict\": true, \"reason\": \"Comprehensive coverage\"}\n```";
            StructuredRubricResult r = JsonRubricParser.parseJsonRubric(jsonInput);

            if (r == null || Math.abs(r.score() - 0.88f) > 0.001f || !r.verdict() || !r.reason().contains("Comprehensive")) {
                return new ScenarioResult(9, name, false, "Failed JSON parsing with markdown fences: " + r);
            }

            try {
                JsonRubricParser.parseJsonRubric("not json");
                return new ScenarioResult(9, name, false, "Expected IllegalArgumentException on invalid json");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 4: Composite & Chained Evaluators (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "WeightedMultiCriteria (Multi-Dimension Score Aggregator)";
        try {
            List<EvaluationCriterion> criteria = List.of(
                    new EvaluationCriterion("accuracy", 0.5f),
                    new EvaluationCriterion("relevance", 0.3f),
                    new EvaluationCriterion("clarity", 0.2f)
            );

            WeightedMultiCriteriaEvaluator eval = new WeightedMultiCriteriaEvaluator(criteria, 0.80f);
            MultiCriteriaResult result = eval.evaluate(Map.of("accuracy", 0.90f, "relevance", 0.80f, "clarity", 0.80f));

            // composite = 0.5*0.9 + 0.3*0.8 + 0.2*0.8 = 0.45 + 0.24 + 0.16 = 0.85
            if (result == null || !result.passed() || Math.abs(result.compositeScore() - 0.85f) > 0.001f) {
                return new ScenarioResult(10, name, false, "Expected composite 0.85 and pass=true, got: " + result);
            }

            try {
                new WeightedMultiCriteriaEvaluator(List.of(new EvaluationCriterion("a", 0.4f)), 0.5f);
                return new ScenarioResult(10, name, false, "Expected IllegalArgumentException when weights != 1.0");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "ShortCircuitComposite (Fast-Fail Evaluator Chain)";
        try {
            AtomicBoolean thirdCalled = new AtomicBoolean(false);

            Evaluator eval1 = req -> new EvaluationResponse(true, 1.0f, "Step 1 Pass", Map.of());
            Evaluator eval2 = req -> new EvaluationResponse(false, 0.0f, "Step 2 Fail", Map.of());
            Evaluator eval3 = req -> {
                thirdCalled.set(true);
                return new EvaluationResponse(true, 1.0f, "Step 3", Map.of());
            };

            ShortCircuitCompositeEvaluator chain = new ShortCircuitCompositeEvaluator(List.of(eval1, eval2, eval3));
            EvaluationResponse res = chain.evaluate(new EvaluationRequest("q", "a"));

            if (res == null || res.isPass()) {
                return new ScenarioResult(11, name, false, "Expected short-circuit failure response, got: " + res);
            }
            if (thirdCalled.get()) {
                return new ScenarioResult(11, name, false, "Short-circuit failed: evaluator 3 was called after evaluator 2 failed");
            }

            return new ScenarioResult(11, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "AllMustPassComposite (Strict Consensus Aggregation)";
        try {
            Evaluator eval1 = req -> new EvaluationResponse(true, 1.0f, "P1", Map.of());
            Evaluator eval2 = req -> new EvaluationResponse(false, 0.4f, "Failing due to latency", Map.of());
            Evaluator eval3 = req -> new EvaluationResponse(true, 0.7f, "P3", Map.of());

            AllMustPassCompositeEvaluator composite = new AllMustPassCompositeEvaluator(List.of(eval1, eval2, eval3));
            EvaluationResponse res = composite.evaluate(new EvaluationRequest("q", "a"));

            if (res == null || res.isPass()) {
                return new ScenarioResult(12, name, false, "Expected composite fail when one evaluator fails");
            }
            if (!res.getFeedback().contains("latency")) {
                return new ScenarioResult(12, name, false, "Expected feedback to contain failing feedback, got: " + res.getFeedback());
            }

            // avgScore = (1.0 + 0.4 + 0.7) / 3 = 0.70f
            if (Math.abs(res.getScore() - 0.70f) > 0.01f) {
                return new ScenarioResult(12, name, false, "Expected average score 0.70, got: " + res.getScore());
            }

            return new ScenarioResult(12, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 5: Benchmark Runner & Suite Aggregation (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "BatchEvaluationRunner (Dataset Test Execution & Pass Rates)";
        try {
            Evaluator eval = req -> {
                boolean p = req.getUserText().contains("pass");
                return new EvaluationResponse(p, p ? 1.0f : 0.0f, p ? "P" : "F", Map.of());
            };

            List<EvaluationRequest> requests = List.of(
                    new EvaluationRequest("pass-1", "a"),
                    new EvaluationRequest("pass-2", "a"),
                    new EvaluationRequest("pass-3", "a"),
                    new EvaluationRequest("fail-1", "a")
            );

            BenchmarkSummary summary = BatchEvaluationRunner.runBatch(eval, requests);
            if (summary == null || summary.totalRequests() != 4 || summary.passCount() != 3 || summary.failCount() != 1) {
                return new ScenarioResult(13, name, false, "Expected 4 requests, 3 passes, 1 fail, got: " + summary);
            }
            if (Math.abs(summary.passRate() - 0.75f) > 0.001f || Math.abs(summary.averageScore() - 0.75f) > 0.001f) {
                return new ScenarioResult(13, name, false, "Expected passRate=0.75, avgScore=0.75, got: " + summary);
            }

            return new ScenarioResult(13, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "BenchmarkSuiteRunner (Multi-Metric Suite Execution)";
        try {
            Evaluator evalA = req -> new EvaluationResponse(true, 1.0f, "P", Map.of());
            Evaluator evalB = req -> new EvaluationResponse(req.getUserText().contains("pass"), 0.5f, "M", Map.of());

            List<EvaluationRequest> requests = List.of(
                    new EvaluationRequest("pass-1", "a"),
                    new EvaluationRequest("fail-1", "a")
            );

            Map<String, BenchmarkSummary> suite = BenchmarkSuiteRunner.runSuite(
                    Map.of("evalA", evalA, "evalB", evalB), requests
            );

            if (suite == null || suite.size() != 2) {
                return new ScenarioResult(14, name, false, "Expected 2 evaluators in suite, got: " + suite);
            }
            if (suite.get("evalA").passCount() != 2 || suite.get("evalB").passCount() != 1) {
                return new ScenarioResult(14, name, false, "Suite pass counts mismatched: " + suite);
            }

            return new ScenarioResult(14, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "QualityGateEnforcer (Threshold Gate Enforcement & Breach Triggers)";
        try {
            BenchmarkSummary passingSummary = new BenchmarkSummary(10, 9, 1, 0.90f, 0.88f);
            QualityGateEnforcer.enforceGate(passingSummary, 0.85f, 0.80f); // Should not throw

            // Pass rate breach
            try {
                BenchmarkSummary failingPassRate = new BenchmarkSummary(10, 7, 3, 0.70f, 0.85f);
                QualityGateEnforcer.enforceGate(failingPassRate, 0.80f, 0.80f);
                return new ScenarioResult(15, name, false, "Expected EvaluationGateException on pass rate breach");
            } catch (EvaluationGateException expected) {
                if (!expected.getMessage().contains("Evaluation threshold breach")) {
                    return new ScenarioResult(15, name, false, "Exception message must contain 'Evaluation threshold breach'");
                }
            }

            // Average score breach
            try {
                BenchmarkSummary failingAvgScore = new BenchmarkSummary(10, 9, 1, 0.90f, 0.70f);
                QualityGateEnforcer.enforceGate(failingAvgScore, 0.80f, 0.80f);
                return new ScenarioResult(15, name, false, "Expected EvaluationGateException on average score breach");
            } catch (EvaluationGateException expected) {
                if (!expected.getMessage().contains("Evaluation threshold breach")) {
                    return new ScenarioResult(15, name, false, "Exception message must contain 'Evaluation threshold breach'");
                }
            }

            return new ScenarioResult(15, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // REPORT FORMATTER
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 06 EXERCISE 01: DETERMINISTIC EVALUATORS & BENCHMARK GATES");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String tag = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", tag, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        System.out.println("===============================================================================");
    }
}
