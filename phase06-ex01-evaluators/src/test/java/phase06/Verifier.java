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
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail, List<String> hints) {
        public ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {
            this(scenarioNumber, name, passed, errorDetail, List.of());
        }
    }

    public static void main(String[] args) {
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selectedScenario);
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        System.exit(anyFailed ? 99 : 0);
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        Assertions.assertFalse(anyFailed, "Verifier detected scenario failures. See printed report above.");
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        List<ScenarioResult> results = new ArrayList<>();

        // Topic 1: Spring AI Evaluator Interface & Request/Response Basics (Scenarios 1 - 3)
        if (selected == 0 || selected == 1) results.add(verifyScenario01());
        if (selected == 0 || selected == 2) results.add(verifyScenario02());
        if (selected == 0 || selected == 3) results.add(verifyScenario03());

        // Topic 2: Deterministic Metric & Accuracy Evaluators (Scenarios 4 - 6)
        if (selected == 0 || selected == 4) results.add(verifyScenario04());
        if (selected == 0 || selected == 5) results.add(verifyScenario05());
        if (selected == 0 || selected == 6) results.add(verifyScenario06());

        // Topic 3: LLM Rubric Parsing & Score Extraction (Scenarios 7 - 9)
        if (selected == 0 || selected == 7) results.add(verifyScenario07());
        if (selected == 0 || selected == 8) results.add(verifyScenario08());
        if (selected == 0 || selected == 9) results.add(verifyScenario09());

        // Topic 4: Composite & Chained Evaluators (Scenarios 10 - 12)
        if (selected == 0 || selected == 10) results.add(verifyScenario10());
        if (selected == 0 || selected == 11) results.add(verifyScenario11());
        if (selected == 0 || selected == 12) results.add(verifyScenario12());

        // Topic 5: Benchmark Runner & Suite Aggregation (Scenarios 13 - 15)
        if (selected == 0 || selected == 13) results.add(verifyScenario13());
        if (selected == 0 || selected == 14) results.add(verifyScenario14());
        if (selected == 0 || selected == 15) results.add(verifyScenario15());

        return results;
    }

    // =========================================================================
    // TOPIC 1: Spring AI Evaluator Interface & Request/Response Basics (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario01() {
        String name = "EvaluationRequest (Direct Request & Threshold Gating)";
        List<String> hints = List.of(
                "Spring AI's EvaluationResponse wraps boolean pass, float score, String feedback, and metadata Map.",
                "Enforce fail-fast boundary checks: throw IllegalArgumentException on null/blank userQuery or modelResponse, or invalid threshold.",
                "Return new EvaluationResponse(score >= threshold, score, score >= threshold ? \"PASSED\" : \"FAILED\", Map.of(\"threshold\", threshold));"
        );
        try {
            EvaluationResponse passResp = EvaluatorUnderTest.evaluateThreshold("What is lock wait timeout?", "50 seconds", 0.90f, 0.80f);
            if (passResp == null || !passResp.isPass() || passResp.getScore() != 0.90f || !"PASSED".equals(passResp.getFeedback())) {
                return new ScenarioResult(1, name, false, "Expected pass=true, score=0.90, feedback=PASSED, got: " + passResp, hints);
            }

            EvaluationResponse failResp = EvaluatorUnderTest.evaluateThreshold("What is lock wait timeout?", "10 ms", 0.40f, 0.80f);
            if (failResp == null || failResp.isPass() || !"FAILED".equals(failResp.getFeedback())) {
                return new ScenarioResult(1, name, false, "Expected pass=false on score < threshold, got: " + failResp, hints);
            }

            try {
                EvaluatorUnderTest.evaluateThreshold("", "response", 0.5f, 0.5f);
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on blank query", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario02() {
        String name = "EvaluationRequest (Context-Enriched RAG Triplet)";
        List<String> hints = List.of(
                "Context-grounded evaluation takes the query, retrieved documents, and the model response to compute groundedness.",
                "Enforce validation on query, response, and documents; include 'contextDocCount' in the metadata Map.",
                "Return new EvaluationResponse(score >= threshold, score, score >= threshold ? \"GROUNDED\" : \"UNGROUNDED\", Map.of(\"contextDocCount\", contextDocs.size(), \"threshold\", threshold));"
        );
        try {
            List<Document> docs = List.of(
                    new Document("Doc 1: Database timeout is 50s"),
                    new Document("Doc 2: Service pool max is 20")
            );

            EvaluationResponse resp = EvaluatorUnderTest.evaluateContextGrounded(
                    "What is DB timeout?", docs, "Database timeout is 50s", 0.85f, 0.70f
            );

            if (resp == null || !resp.isPass() || !"GROUNDED".equals(resp.getFeedback())) {
                return new ScenarioResult(2, name, false, "Expected grounded pass response, got: " + resp, hints);
            }

            if (!resp.getMetadata().containsKey("contextDocCount") || !Integer.valueOf(2).equals(resp.getMetadata().get("contextDocCount"))) {
                return new ScenarioResult(2, name, false, "Expected metadata contextDocCount=2, got: " + resp.getMetadata(), hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario03() {
        String name = "RelevancyEvaluatorAdapter (Spring AI Built-in Adapter)";
        List<String> hints = List.of(
                "Relevancy evaluation asks an LLM judge whether the response directly addresses the user query given context docs.",
                "Build RelevancyEvaluator with RelevancyEvaluator.builder().chatClientBuilder(chatClientBuilder).build().",
                "Create an EvaluationRequest(query, contextDocs, response) and call relevancyEvaluator.evaluate(request) with fallback handling."
        );
        try {
            FakeEvaluatorChatModel fakeModel = new FakeEvaluatorChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            List<Document> docs = List.of(new Document("Cluster memory limit is 64GB"));

            fakeModel.enqueue("YES");
            EvaluationResponse passResp = EvaluatorUnderTest.evaluateRelevancy(
                    builder, "What is cluster memory limit?", "Cluster memory limit is 64GB", docs
            );
            if (passResp == null || !passResp.isPass()) {
                return new ScenarioResult(3, name, false, "Expected pass=true when model outputs YES, got: " + passResp, hints);
            }

            fakeModel.enqueue("NO");
            EvaluationResponse failResp = EvaluatorUnderTest.evaluateRelevancy(
                    builder, "What is cluster memory limit?", "Today is sunny.", docs
            );
            if (failResp == null || failResp.isPass()) {
                return new ScenarioResult(3, name, false, "Expected pass=false when model outputs NO, got: " + failResp, hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 2: Deterministic Metric & Accuracy Evaluators (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario04() {
        String name = "GroundTruthAccuracy (Exact Match Comparison)";
        List<String> hints = List.of(
                "Ground truth exact matching checks whether the candidate response matches the expected ground truth string.",
                "Trim whitespace and ignore case when comparing candidate and expected ground truth strings.",
                "Return new EvaluationResponse(match, match ? 1.0f : 0.0f, match ? \"MATCH\" : \"MISMATCH: expected '\" + expected + \"' but got '\" + candidate + \"'\", Map.of());"
        );
        try {
            GroundTruthAccuracyEvaluator eval = new GroundTruthAccuracyEvaluator();

            EvaluationResponse match = eval.evaluateGroundTruth("DATABASE_LOCK", "database_lock  ");
            if (match == null || !match.isPass() || match.getScore() != 1.0f || !"MATCH".equals(match.getFeedback())) {
                return new ScenarioResult(4, name, false, "Expected MATCH with score 1.0, got: " + match, hints);
            }

            EvaluationResponse mismatch = eval.evaluateGroundTruth("CPU_BURST", "database_lock");
            if (mismatch == null || mismatch.isPass() || mismatch.getScore() != 0.0f || !mismatch.getFeedback().startsWith("MISMATCH")) {
                return new ScenarioResult(4, name, false, "Expected MISMATCH with score 0.0, got: " + mismatch, hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario05() {
        String name = "CategoricalConvergence (Allowed Category Set Membership)";
        List<String> hints = List.of(
                "Categorical convergence checks if the predicted category belongs to a predefined set of permitted labels.",
                "Compare case-insensitively against the allowed category set: allowedCategories.stream().anyMatch(c -> c.equalsIgnoreCase(category.trim())).",
                "Return new EvaluationResponse(valid, valid ? 1.0f : 0.0f, valid ? \"VALID_CATEGORY\" : \"INVALID_CATEGORY\", Map.of());"
        );
        try {
            CategoricalConvergenceEvaluator eval = new CategoricalConvergenceEvaluator();
            Set<String> categories = Set.of("network", "database", "disk");

            EvaluationResponse valid = eval.evaluateCategory("Database", categories);
            if (valid == null || !valid.isPass() || valid.getScore() != 1.0f) {
                return new ScenarioResult(5, name, false, "Expected valid category pass, got: " + valid, hints);
            }

            EvaluationResponse invalid = eval.evaluateCategory("memory", categories);
            if (invalid == null || invalid.isPass() || invalid.getScore() != 0.0f) {
                return new ScenarioResult(5, name, false, "Expected invalid category fail, got: " + invalid, hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario06() {
        String name = "BoundedScore (Numeric Threshold Window Verification)";
        List<String> hints = List.of(
                "Bounded score evaluators ensure numerical metric scores lie within a valid [minScore, maxScore] range.",
                "Validate boundaries first: throw IllegalArgumentException if minScore > maxScore.",
                "Return new EvaluationResponse(score >= minScore && score <= maxScore, score, score >= minScore && score <= maxScore ? \"WITHIN_BOUNDS\" : \"OUT_OF_BOUNDS\", Map.of());"
        );
        try {
            BoundedScoreEvaluator eval = new BoundedScoreEvaluator();

            EvaluationResponse within = eval.evaluateBounds(0.75f, 0.70f, 0.90f);
            if (within == null || !within.isPass() || !"WITHIN_BOUNDS".equals(within.getFeedback())) {
                return new ScenarioResult(6, name, false, "Expected WITHIN_BOUNDS, got: " + within, hints);
            }

            EvaluationResponse out = eval.evaluateBounds(0.65f, 0.70f, 0.90f);
            if (out == null || out.isPass() || !"OUT_OF_BOUNDS".equals(out.getFeedback())) {
                return new ScenarioResult(6, name, false, "Expected OUT_OF_BOUNDS, got: " + out, hints);
            }

            try {
                eval.evaluateBounds(0.5f, 0.8f, 0.6f);
                return new ScenarioResult(6, name, false, "Expected IllegalArgumentException for min > max", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 3: LLM Rubric Parsing & Score Extraction (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario07() {
        String name = "NumericRubricParser (Regex Score Extraction)";
        List<String> hints = List.of(
                "LLM judge outputs often provide scores formatted as fractions (e.g. 4/5) or decimal floats (e.g. 0.92).",
                "Use regex patterns SCORE:\\s*(\\d+)/(\\d+) and SCORE:\\s*([0-9.]+) (case-insensitive); throw IllegalArgumentException if missing.",
                "Extract float score = num / den or Float.parseFloat(m), and extract REASON from trailing text into RubricScore."
        );
        try {
            RubricScore s1 = NumericRubricParser.parseScore("SCORE: 4/5\nREASON: Mostly accurate response.");
            if (s1 == null || Math.abs(s1.score() - 0.80f) > 0.001f || !s1.reason().contains("Mostly accurate")) {
                return new ScenarioResult(7, name, false, "Failed fraction 4/5 parsing: " + s1, hints);
            }

            RubricScore s2 = NumericRubricParser.parseScore("score: 0.92\nreason: Highly detailed.");
            if (s2 == null || Math.abs(s2.score() - 0.92f) > 0.001f) {
                return new ScenarioResult(7, name, false, "Failed decimal 0.92 parsing: " + s2, hints);
            }

            try {
                NumericRubricParser.parseScore("No score provided here");
                return new ScenarioResult(7, name, false, "Expected IllegalArgumentException when SCORE missing", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario08() {
        String name = "VerdictRubricParser (Multi-Field Verdict & Reasoning)";
        List<String> hints = List.of(
                "Binary LLM judges output VERDICT: PASS/FAIL along with a REASON explanation.",
                "Parse VERDICT:\\s*(PASS|FAIL) case-insensitively; throw IllegalArgumentException if verdict is missing or neither PASS nor FAIL.",
                "Check boolean pass = \"PASS\".equalsIgnoreCase(verdict); extract REASON: line and return new RubricVerdict(pass, reason);"
        );
        try {
            RubricVerdict v1 = VerdictRubricParser.parseVerdict("VERDICT: PASS\nREASON: Response matches ground truth.");
            if (v1 == null || !v1.pass() || !v1.reason().contains("matches ground truth")) {
                return new ScenarioResult(8, name, false, "Failed PASS verdict parsing: " + v1, hints);
            }

            RubricVerdict v2 = VerdictRubricParser.parseVerdict("verdict: FAIL\nreason: Hallucinated metrics.");
            if (v2 == null || v2.pass() || !v2.reason().contains("Hallucinated")) {
                return new ScenarioResult(8, name, false, "Failed FAIL verdict parsing: " + v2, hints);
            }

            try {
                VerdictRubricParser.parseVerdict("verdict: MAYBE\nreason: uncertain");
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException on non-PASS/FAIL verdict", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario09() {
        String name = "JsonRubricParser (Markdown Fence Stripping & JSON Extraction)";
        List<String> hints = List.of(
                "LLM judges may enclose JSON in markdown code blocks like ```json ... ```.",
                "Strip markdown code fences before parsing with JsonHelper or ObjectMapper.",
                "Deserialize JSON into StructuredRubricResult containing score, verdict, and reason; throw IllegalArgumentException on bad format."
        );
        try {
            String jsonInput = "```json\n{\"score\": 0.88, \"verdict\": true, \"reason\": \"Comprehensive coverage\"}\n```";
            StructuredRubricResult r = JsonRubricParser.parseJsonRubric(jsonInput);

            if (r == null || Math.abs(r.score() - 0.88f) > 0.001f || !r.verdict() || !r.reason().contains("Comprehensive")) {
                return new ScenarioResult(9, name, false, "Failed JSON parsing with markdown fences: " + r, hints);
            }

            try {
                JsonRubricParser.parseJsonRubric("not json");
                return new ScenarioResult(9, name, false, "Expected IllegalArgumentException on invalid json", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 4: Composite & Chained Evaluators (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "WeightedMultiCriteria (Multi-Dimension Score Aggregator)";
        List<String> hints = List.of(
                "Multi-criteria evaluation aggregates weighted scores across multiple dimensions (e.g., accuracy, relevance).",
                "Validate criteria weights sum to 1.0f (+/- 0.001f); throw IllegalArgumentException if invalid.",
                "Calculate composite = sum(score * weight); return new MultiCriteriaResult(composite >= threshold, composite, scoreMap);"
        );
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
                return new ScenarioResult(10, name, false, "Expected composite 0.85 and pass=true, got: " + result, hints);
            }

            try {
                new WeightedMultiCriteriaEvaluator(List.of(new EvaluationCriterion("a", 0.4f)), 0.5f);
                return new ScenarioResult(10, name, false, "Expected IllegalArgumentException when weights != 1.0", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "ShortCircuitComposite (Fast-Fail Evaluator Chain)";
        List<String> hints = List.of(
                "A short-circuit composite evaluator evaluates components sequentially and halts immediately upon the first failure.",
                "Iterate through the evaluator list; if resp.isPass() is false, return that failing response immediately without calling remaining evaluators.",
                "If all evaluators pass, return the last evaluation response."
        );
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
                return new ScenarioResult(11, name, false, "Expected short-circuit failure response, got: " + res, hints);
            }
            if (thirdCalled.get()) {
                return new ScenarioResult(11, name, false, "Short-circuit failed: evaluator 3 was called after evaluator 2 failed", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "AllMustPassComposite (Strict Consensus Aggregation)";
        List<String> hints = List.of(
                "Strict consensus evaluators execute all evaluators in the chain and require every single one to pass.",
                "Run every evaluator, compute average score = totalScore / evaluators.size(), and collect any failure feedbacks.",
                "Return new EvaluationResponse(allPass, avgScore, feedbackSummary, Map.of());"
        );
        try {
            Evaluator eval1 = req -> new EvaluationResponse(true, 1.0f, "P1", Map.of());
            Evaluator eval2 = req -> new EvaluationResponse(false, 0.4f, "Failing due to latency", Map.of());
            Evaluator eval3 = req -> new EvaluationResponse(true, 0.7f, "P3", Map.of());

            AllMustPassCompositeEvaluator composite = new AllMustPassCompositeEvaluator(List.of(eval1, eval2, eval3));
            EvaluationResponse res = composite.evaluate(new EvaluationRequest("q", "a"));

            if (res == null || res.isPass()) {
                return new ScenarioResult(12, name, false, "Expected composite fail when one evaluator fails", hints);
            }
            if (!res.getFeedback().contains("latency")) {
                return new ScenarioResult(12, name, false, "Expected feedback to contain failing feedback, got: " + res.getFeedback(), hints);
            }

            // avgScore = (1.0 + 0.4 + 0.7) / 3 = 0.70f
            if (Math.abs(res.getScore() - 0.70f) > 0.01f) {
                return new ScenarioResult(12, name, false, "Expected average score 0.70, got: " + res.getScore(), hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 5: Benchmark Runner & Suite Aggregation (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "BatchEvaluationRunner (Dataset Test Execution & Pass Rates)";
        List<String> hints = List.of(
                "Batch evaluation processes a collection of EvaluationRequests through an Evaluator and aggregates metrics.",
                "Count total requests, passes, failures, and calculate passRate = passCount / total and averageScore = totalScore / total.",
                "Return new BenchmarkSummary(total, passCount, failCount, passRate, averageScore);"
        );
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
                return new ScenarioResult(13, name, false, "Expected 4 requests, 3 passes, 1 fail, got: " + summary, hints);
            }
            if (Math.abs(summary.passRate() - 0.75f) > 0.001f || Math.abs(summary.averageScore() - 0.75f) > 0.001f) {
                return new ScenarioResult(13, name, false, "Expected passRate=0.75, avgScore=0.75, got: " + summary, hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "BenchmarkSuiteRunner (Multi-Metric Suite Execution)";
        List<String> hints = List.of(
                "A benchmark suite runs multiple named evaluators across the same evaluation dataset.",
                "For each evaluator entry in evaluators.entrySet(), call BatchEvaluationRunner.runBatch(entry.getValue(), requests).",
                "Collect results into a Map<String, BenchmarkSummary> and return."
        );
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
                return new ScenarioResult(14, name, false, "Expected 2 evaluators in suite, got: " + suite, hints);
            }
            if (suite.get("evalA").passCount() != 2 || suite.get("evalB").passCount() != 1) {
                return new ScenarioResult(14, name, false, "Suite pass counts mismatched: " + suite, hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "QualityGateEnforcer (Threshold Gate Enforcement & Breach Triggers)";
        List<String> hints = List.of(
                "Quality gate enforcers compare benchmark summaries against target thresholds for automated CI/CD release decisions.",
                "If summary.passRate() < minPassRate or summary.averageScore() < minAvgScore, throw EvaluationGateException.",
                "Include descriptive breach message containing 'Evaluation threshold breach' in the thrown EvaluationGateException."
        );
        try {
            BenchmarkSummary passingSummary = new BenchmarkSummary(10, 9, 1, 0.90f, 0.88f);
            QualityGateEnforcer.enforceGate(passingSummary, 0.85f, 0.80f); // Should not throw

            // Pass rate breach
            try {
                BenchmarkSummary failingPassRate = new BenchmarkSummary(10, 7, 3, 0.70f, 0.85f);
                QualityGateEnforcer.enforceGate(failingPassRate, 0.80f, 0.80f);
                return new ScenarioResult(15, name, false, "Expected EvaluationGateException on pass rate breach", hints);
            } catch (EvaluationGateException expected) {
                if (!expected.getMessage().contains("Evaluation threshold breach")) {
                    return new ScenarioResult(15, name, false, "Exception message must contain 'Evaluation threshold breach'", hints);
                }
            }

            // Average score breach
            try {
                BenchmarkSummary failingAvgScore = new BenchmarkSummary(10, 9, 1, 0.90f, 0.70f);
                QualityGateEnforcer.enforceGate(failingAvgScore, 0.80f, 0.80f);
                return new ScenarioResult(15, name, false, "Expected EvaluationGateException on average score breach", hints);
            } catch (EvaluationGateException expected) {
                if (!expected.getMessage().contains("Evaluation threshold breach")) {
                    return new ScenarioResult(15, name, false, "Exception message must contain 'Evaluation threshold breach'", hints);
                }
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, t.getMessage(), hints);
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
        String[] hintLabels = {"[HINT 1 (Concept)]", "[HINT 2 (Edge Case)]", "[HINT 3 (Code Pattern)]"};

        for (ScenarioResult r : results) {
            String tag = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", tag, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null && !r.hints().isEmpty()) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        String label = i < hintLabels.length ? hintLabels[i] : String.format("[HINT %d]", i + 1);
                        System.out.printf("         --> %s %s%n", label, r.hints().get(i));
                    }
                }
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        if (passed == results.size()) {
            System.out.println("  VERDICT: ALL 15 GATES PASSED (exit code 0)");
        } else {
            System.out.printf("  VERDICT: %d FINDINGS DETECTED (exit code 99)%n", results.size() - passed);
        }
        System.out.println("===============================================================================");
    }
}
