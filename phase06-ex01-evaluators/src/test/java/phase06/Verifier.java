package phase06;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import phase06.EvaluatorContracts.*;
import phase06.EvaluatorUnderTest.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 06 Exercise 01:
 * Deterministic Evaluators, Spring AI Evaluator Interface & Foundational LLM-as-a-Judge (10 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

    public static void main(String[] args) {
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selectedScenario);
        printReport(results);

        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            fail("Verifier detected scenario failures. See printed report above.");
        }
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        List<ScenarioResult> list = new ArrayList<>();
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "RelevancyEvaluatorAdapter (Spring AI Built-In Relevancy)";
        try {
            FakeEvaluatorChatModel fakeModel = new FakeEvaluatorChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);

            List<Document> docs = List.of(new Document("Order service p95 is 850ms due to database locks."));

            fakeModel.enqueue("YES");
            EvaluationResponse resp1 = EvaluatorUnderTest.evaluateRelevancy(
                    builder, "What is the bottleneck?", "The bottleneck is database locking in order service.", docs
            );
            if (!resp1.isPass()) {
                return new ScenarioResult(1, name, false, "Expected pass=true when model outputs YES, got: " + resp1);
            }

            fakeModel.enqueue("NO");
            EvaluationResponse resp2 = EvaluatorUnderTest.evaluateRelevancy(
                    builder, "What is the bottleneck?", "The weather in Seattle is rainy.", docs
            );
            if (resp2.isPass()) {
                return new ScenarioResult(1, name, false, "Expected pass=false when model outputs NO, got: " + resp2);
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "FactCheckingEvaluatorAdapter (Spring AI Built-In FactChecking)";
        try {
            FakeEvaluatorChatModel fakeModel = new FakeEvaluatorChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);

            List<Document> docs = List.of(new Document("Memory usage spiked to 92% after commit a1b2c3d."));

            fakeModel.enqueue("YES");
            EvaluationResponse resp1 = EvaluatorUnderTest.evaluateFactuality(
                    builder, "Memory reached 92% following commit a1b2c3d.", docs
            );
            if (!resp1.isPass()) {
                return new ScenarioResult(2, name, false, "Expected pass=true for supported claim, got: " + resp1);
            }

            fakeModel.enqueue("NO");
            EvaluationResponse resp2 = EvaluatorUnderTest.evaluateFactuality(
                    builder, "CPU remained under 10% the entire day.", docs
            );
            if (resp2.isPass()) {
                return new ScenarioResult(2, name, false, "Expected pass=false for unsupported claim, got: " + resp2);
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "NoiseFloorThresholdEvaluator (Deterministic Telemetry Keep Gate)";
        try {
            NoiseFloors floors = new NoiseFloors(15.0, 5.0, 50.0);
            NoiseFloorThresholdEvaluator evaluator = new NoiseFloorThresholdEvaluator(floors);

            // Test 1: RPS improvement cleared floor
            TelemetryDelta d1 = new TelemetryDelta(5.0, 2.0, 65.0, 0.01, 0.01);
            EvaluationResponse r1 = evaluator.evaluateTelemetry(d1);
            if (!r1.isPass() || !r1.getFeedback().toLowerCase().contains("rps")) {
                return new ScenarioResult(3, name, false, "Expected RPS keep, got: " + r1.getFeedback());
            }

            // Test 2: P95 improvement cleared floor
            TelemetryDelta d2 = new TelemetryDelta(25.0, 2.0, 10.0, 0.01, 0.01);
            EvaluationResponse r2 = evaluator.evaluateTelemetry(d2);
            if (!r2.isPass() || !r2.getFeedback().toLowerCase().contains("p95")) {
                return new ScenarioResult(3, name, false, "Expected P95 keep, got: " + r2.getFeedback());
            }

            // Test 3: Neither cleared floor
            TelemetryDelta d3 = new TelemetryDelta(8.0, 2.0, 20.0, 0.01, 0.01);
            EvaluationResponse r3 = evaluator.evaluateTelemetry(d3);
            if (r3.isPass()) {
                return new ScenarioResult(3, name, false, "Expected rejection when deltas below floors, got: " + r3);
            }

            // Test 4: Guard failRate worsened
            TelemetryDelta d4 = new TelemetryDelta(40.0, 20.0, 100.0, 0.01, 0.04);
            EvaluationResponse r4 = evaluator.evaluateTelemetry(d4);
            if (r4.isPass() || (!r4.getFeedback().toLowerCase().contains("failrate") && !r4.getFeedback().toLowerCase().contains("worsened"))) {
                return new ScenarioResult(3, name, false, "Expected rejection on worsened failRate, got: " + r4.getFeedback());
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "GroundTruthAccuracyEvaluator (Exact Category Convergence)";
        try {
            GroundTruthAccuracyEvaluator evaluator = new GroundTruthAccuracyEvaluator();

            EvaluationResponse r1 = evaluator.evaluateGroundTruth("LockContention", "lockcontention");
            if (!r1.isPass() || r1.getScore() != 1.0f || !r1.getFeedback().toLowerCase().contains("accurate")) {
                return new ScenarioResult(4, name, false, "Expected accurate match, got: " + r1);
            }

            EvaluationResponse r2 = evaluator.evaluateGroundTruth("ConnectionPoolLeak", "GcThrashing");
            if (r2.isPass() || r2.getScore() != 0.0f || !r2.getFeedback().toLowerCase().contains("mismatch")) {
                return new ScenarioResult(4, name, false, "Expected mismatch failure, got: " + r2);
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "SingleMetricThresholdEvaluator (Configurable Numeric Gate)";
        try {
            SingleMetricThresholdEvaluator evaluator = new SingleMetricThresholdEvaluator(0.80f);

            EvaluationResponse r1 = evaluator.evaluateScore(0.85f);
            if (!r1.isPass() || !r1.getFeedback().toLowerCase().contains("passed")) {
                return new ScenarioResult(5, name, false, "Expected pass for 0.85 >= 0.80, got: " + r1);
            }

            EvaluationResponse r2 = evaluator.evaluateScore(0.72f);
            if (r2.isPass() || !r2.getFeedback().toLowerCase().contains("failed")) {
                return new ScenarioResult(5, name, false, "Expected fail for 0.72 < 0.80, got: " + r2);
            }

            // Invariant check: invalid threshold out of range
            try {
                new SingleMetricThresholdEvaluator(1.5f);
                return new ScenarioResult(5, name, false, "Expected IllegalArgumentException for threshold 1.5");
            } catch (IllegalArgumentException expected) {}

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "RubricScoreParser (Structured LLM Judge Rubric Parsing)";
        try {
            String output1 = "SCORE: 4/5\nREASON: The diagnosis pinpointed JDBC connection leak.";
            RubricScore s1 = RubricScoreParser.parse(output1);
            if (Math.abs(s1.score() - 0.80f) > 0.01f || !s1.reason().toLowerCase().contains("jdbc")) {
                return new ScenarioResult(6, name, false, "Failed to parse fraction score: " + s1);
            }

            String output2 = "Score: 0.95\nReason: High clarity and actionable instructions.";
            RubricScore s2 = RubricScoreParser.parse(output2);
            if (Math.abs(s2.score() - 0.95f) > 0.01f) {
                return new ScenarioResult(6, name, false, "Failed to parse decimal score: " + s2);
            }

            try {
                RubricScoreParser.parse("Just a conversational response without a score header.");
                return new ScenarioResult(6, name, false, "Expected IllegalArgumentException for missing score");
            } catch (IllegalArgumentException expected) {}

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "BinaryJudgeEvaluator (Prompt-Driven LLM Judge)";
        try {
            FakeEvaluatorChatModel fakeModel = new FakeEvaluatorChatModel();
            ChatClient chatClient = ChatClient.create(fakeModel);
            BinaryJudgeEvaluator judge = new BinaryJudgeEvaluator(chatClient);

            fakeModel.enqueue("PASS\nReason: Comprehensive root-cause explanation provided.");
            EvaluationResponse r1 = judge.evaluate(new EvaluationRequest("Analyze log", "Log indicates deadlocks."));
            if (!r1.isPass() || r1.getScore() != 1.0f) {
                return new ScenarioResult(7, name, false, "Expected pass=true on PASS verdict, got: " + r1);
            }

            fakeModel.enqueue("FAIL\nReason: Ignored the high memory pressure.");
            EvaluationResponse r2 = judge.evaluate(new EvaluationRequest("Analyze log", "Log has no problems."));
            if (r2.isPass() || r2.getScore() != 0.0f) {
                return new ScenarioResult(7, name, false, "Expected pass=false on FAIL verdict, got: " + r2);
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "WeightedMultiCriteriaEvaluator (Multi-Dimension Aggregator)";
        try {
            List<EvaluationCriterion> criteria = List.of(
                    new EvaluationCriterion("Correctness", 0.50f),
                    new EvaluationCriterion("Completeness", 0.30f),
                    new EvaluationCriterion("Safety", 0.20f)
            );
            WeightedMultiCriteriaEvaluator evaluator = new WeightedMultiCriteriaEvaluator(criteria, 0.80f);

            // 0.5 * 0.9 + 0.3 * 0.8 + 0.2 * 1.0 = 0.45 + 0.24 + 0.20 = 0.89 (>= 0.80 -> PASS)
            Map<String, Float> scores1 = Map.of("Correctness", 0.90f, "Completeness", 0.80f, "Safety", 1.0f);
            MultiCriteriaResult res1 = evaluator.evaluate(scores1);
            if (!res1.passed() || Math.abs(res1.compositeScore() - 0.89f) > 0.01f) {
                return new ScenarioResult(8, name, false, "Expected pass with ~0.89 composite score, got: " + res1);
            }

            // 0.5 * 0.4 + 0.3 * 0.5 + 0.2 * 0.6 = 0.20 + 0.15 + 0.12 = 0.47 (< 0.80 -> FAIL)
            Map<String, Float> scores2 = Map.of("Correctness", 0.40f, "Completeness", 0.50f, "Safety", 0.60f);
            MultiCriteriaResult res2 = evaluator.evaluate(scores2);
            if (res2.passed() || Math.abs(res2.compositeScore() - 0.47f) > 0.01f) {
                return new ScenarioResult(8, name, false, "Expected fail with ~0.47 composite score, got: " + res2);
            }

            // Weight validation check: invalid sum != 1.0
            try {
                new WeightedMultiCriteriaEvaluator(List.of(
                        new EvaluationCriterion("A", 0.3f),
                        new EvaluationCriterion("B", 0.3f)
                ), 0.5f);
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException for weight sum 0.6 != 1.0");
            } catch (IllegalArgumentException expected) {}

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "ShortCircuitCompositeEvaluator (Chained Evaluators with Fast Fail)";
        try {
            AtomicInteger callCount = new AtomicInteger(0);

            Evaluator passEvaluator = req -> {
                callCount.incrementAndGet();
                return new EvaluationResponse(true, 1.0f, "Step passed", Map.of());
            };

            Evaluator failEvaluator = req -> {
                callCount.incrementAndGet();
                return new EvaluationResponse(false, 0.0f, "Step failed at gate 2", Map.of());
            };

            Evaluator neverCalledEvaluator = req -> {
                callCount.incrementAndGet();
                return new EvaluationResponse(true, 1.0f, "Should not be reached", Map.of());
            };

            ShortCircuitCompositeEvaluator chain = new ShortCircuitCompositeEvaluator(
                    List.of(passEvaluator, failEvaluator, neverCalledEvaluator)
            );

            EvaluationResponse response = chain.evaluate(new EvaluationRequest("test", "test"));
            if (response.isPass()) {
                return new ScenarioResult(9, name, false, "Expected failure when second evaluator fails");
            }
            if (callCount.get() != 2) {
                return new ScenarioResult(9, name, false, "Expected short-circuit after 2 calls, but called: " + callCount.get());
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "BatchEvaluationRunner (Benchmark Suite Runner & Gate)";
        try {
            // Evaluator that passes requests whose userText starts with "PASS"
            Evaluator testEvaluator = req -> {
                boolean p = req.getUserText().startsWith("PASS");
                return new EvaluationResponse(p, p ? 1.0f : 0.0f, p ? "Good" : "Bad", Map.of());
            };

            List<EvaluationRequest> requests = List.of(
                    new EvaluationRequest("PASS 1", "resp 1"),
                    new EvaluationRequest("PASS 2", "resp 2"),
                    new EvaluationRequest("PASS 3", "resp 3"),
                    new EvaluationRequest("PASS 4", "resp 4"),
                    new EvaluationRequest("FAIL 5", "resp 5")
            );

            BenchmarkSummary summary = BatchEvaluationRunner.runBatch(testEvaluator, requests);
            if (summary.totalRequests() != 5 || summary.passCount() != 4 || summary.failCount() != 1) {
                return new ScenarioResult(10, name, false, "Unexpected summary counts: " + summary);
            }
            if (Math.abs(summary.passRate() - 0.80f) > 0.01f) {
                return new ScenarioResult(10, name, false, "Expected 0.80 pass rate, got: " + summary.passRate());
            }

            // Should pass threshold of 0.75
            BatchEvaluationRunner.enforceThreshold(summary, 0.75f);

            // Should fail threshold of 0.85 and throw EvaluationGateException containing "Evaluation threshold breach"
            boolean breachThrown = false;
            try {
                BatchEvaluationRunner.enforceThreshold(summary, 0.85f);
            } catch (EvaluationGateException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("evaluation threshold breach")) {
                    breachThrown = true;
                } else {
                    return new ScenarioResult(10, name, false, "EvaluationGateException message missing 'Evaluation threshold breach': " + ex.getMessage());
                }
            }

            if (!breachThrown) {
                return new ScenarioResult(10, name, false, "Expected EvaluationGateException for passRate 0.80 < required 0.85");
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 06 EXERCISE 01: DETERMINISTIC EVALUATORS & LLM-AS-A-JUDGE VERIFIER");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String mark = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", mark, r.scenarioNumber(), r.name());
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
