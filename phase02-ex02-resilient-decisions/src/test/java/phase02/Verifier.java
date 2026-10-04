package phase02;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import phase02.DecisionModelContracts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 02 Exercise 02:
 * Performance Diagnostic Decision Gateway & Resilient Schema Repair.
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
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "generateFormatInstructions (Schema Directives Injection)";
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            String format = engine.generateFormatInstructions();
            if (format == null || format.isBlank()) {
                return new ScenarioResult(1, name, false, "generateFormatInstructions returned null or blank string.");
            }
            if (!format.contains("hypothesis") || !format.contains("prediction") ||
                !format.contains("ledger") || !format.contains("change")) {
                return new ScenarioResult(1, name, false,
                        "Format instructions missing core schema properties (expected hypothesis, prediction, ledger, change). Got:\n" + format);
            }
            return new ScenarioResult(1, name, true, "Format instructions generated from BeanOutputConverter matching schema specification.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception during generateFormatInstructions: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "parseDecision (Direct BeanOutputConverter Deserialization)";
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            // 1. Boundary check: null / blank throws exception
            try {
                engine.parseDecision(null);
                return new ScenarioResult(2, name, false, "Fail-fast invariant failed: null rawJson must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                engine.parseDecision("   ");
                return new ScenarioResult(2, name, false, "Fail-fast invariant failed: blank rawJson must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 2. Normal deserialization check
            OptimizationDecision decision = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);
            if (decision == null) {
                return new ScenarioResult(2, name, false, "parseDecision returned null.");
            }

            if (!"H3".equals(decision.hypothesis().category()) ||
                Math.abs(decision.hypothesis().confidence() - 0.95) > 0.001 ||
                !"p95".equals(decision.prediction().metric()) ||
                !"jar-unpack".equals(decision.change().template())) {
                return new ScenarioResult(2, name, false,
                        "Deserialized record field values mismatch:\n  Got: " + decision);
            }

            return new ScenarioResult(2, name, true, "Direct JSON parsing cleanly deserialized into OptimizationDecision record.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception during parse: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "extractFromConversationalText (Markdown Fence & Preamble Stripping)";
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            // Boundary checks
            try {
                engine.extractFromConversationalText(null);
                return new ScenarioResult(3, name, false, "Fail-fast invariant failed: null payload must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            OptimizationDecision decision = engine.extractFromConversationalText(DecisionModelContracts.SAMPLE_MARKDOWN_WRAPPED_DECISION_JSON);
            if (decision == null) {
                return new ScenarioResult(3, name, false, "extractFromConversationalText returned null.");
            }

            if (!"H2".equals(decision.hypothesis().category()) ||
                !"GCPhasePause".equals(decision.prediction().signalToEliminate()) ||
                !"virtual-threads".equals(decision.change().template())) {
                return new ScenarioResult(3, name, false,
                        "Sanitized and extracted record values mismatch:\n  Got: " + decision);
            }

            return new ScenarioResult(3, name, true, "Conversational preamble and markdown fences cleanly sanitized before deserialization.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception during extractFromConversationalText: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "validateCrossFieldRules (Cross-Field Invariants & Domain Rules)";
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            // 1. Valid template decision passes
            OptimizationDecision validTemplate = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);
            engine.validateCrossFieldRules(validTemplate);

            // 2. Valid edits decision passes
            OptimizationDecision validEdits = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_EDITS_DECISION_JSON);
            engine.validateCrossFieldRules(validEdits);

            // 3. Null decision throws
            try {
                engine.validateCrossFieldRules(null);
                return new ScenarioResult(4, name, false, "Null decision must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 4. kind=template with blank template throws
            OptimizationDecision invalidTemplate = engine.parseDecision(DecisionModelContracts.SAMPLE_INVALID_CROSS_FIELD_JSON);
            try {
                engine.validateCrossFieldRules(invalidTemplate);
                return new ScenarioResult(4, name, false, "kind=template with blank template must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            // 5. kind=edits with empty edits list throws
            OptimizationDecision emptyEdits = new OptimizationDecision(
                    validEdits.hypothesis(),
                    validEdits.prediction(),
                    validEdits.ledger(),
                    new Change("edits", List.of(), null, Map.of())
            );
            try {
                engine.validateCrossFieldRules(emptyEdits);
                return new ScenarioResult(4, name, false, "kind=edits with empty edits list must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            // 6. Invalid hypothesis category (H9) throws
            OptimizationDecision badHypo = new OptimizationDecision(
                    new Hypothesis("H9", 0.9, "Invalid category"),
                    validTemplate.prediction(),
                    validTemplate.ledger(),
                    validTemplate.change()
            );
            try {
                engine.validateCrossFieldRules(badHypo);
                return new ScenarioResult(4, name, false, "Hypothesis category H9 must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            // 7. Invalid prediction metric throws
            OptimizationDecision badMetric = new OptimizationDecision(
                    validTemplate.hypothesis(),
                    new Prediction("invalid_metric", "improve", "JavaMonitorEnter"),
                    validTemplate.ledger(),
                    validTemplate.change()
            );
            try {
                engine.validateCrossFieldRules(badMetric);
                return new ScenarioResult(4, name, false, "Invalid prediction metric must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(4, name, true, "TigerStyle domain invariants and cross-field rules (kind=edits vs kind=template) validated.");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception during validateCrossFieldRules: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "parseDecisionBatch (Generic Collection List<OptimizationDecision>)";
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            try {
                engine.parseDecisionBatch(null);
                return new ScenarioResult(5, name, false, "Null batch payload must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            List<OptimizationDecision> batch = engine.parseDecisionBatch(DecisionModelContracts.SAMPLE_DECISION_BATCH_JSON);
            if (batch == null || batch.size() != 2) {
                return new ScenarioResult(5, name, false, "Expected batch of size 2, got: " + (batch == null ? "null" : batch.size()));
            }

            OptimizationDecision d1 = batch.get(0);
            OptimizationDecision d2 = batch.get(1);

            if (!"H3".equals(d1.hypothesis().category()) || !"jar-unpack".equals(d1.change().template()) ||
                !"H4".equals(d2.hypothesis().category()) || !"hikari-pool".equals(d2.change().template())) {
                return new ScenarioResult(5, name, false, "Batch element values do not match expected fixtures:\n" + batch);
            }

            return new ScenarioResult(5, name, true, "Generic collection List<OptimizationDecision> deserialized via ParameterizedTypeReference.");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception during parseDecisionBatch: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "fallbackExtractFromProse (Fallback Semi-Structured Diagnosis Reconstruction)";
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            // 1. Null / blank / non-diagnostic text returns null
            if (engine.fallbackExtractFromProse(null) != null) {
                return new ScenarioResult(6, name, false, "Null prose must return null.");
            }
            if (engine.fallbackExtractFromProse("   ") != null) {
                return new ScenarioResult(6, name, false, "Blank prose must return null.");
            }
            if (engine.fallbackExtractFromProse("System is running normally without errors.") != null) {
                return new ScenarioResult(6, name, false, "Unrelated prose text without hypothesis/template must return null.");
            }

            // 2. Prose extraction
            OptimizationDecision decision = engine.fallbackExtractFromProse(DecisionModelContracts.SAMPLE_UNSTRUCTURED_PROSE_DECISION);
            if (decision == null) {
                return new ScenarioResult(6, name, false, "fallbackExtractFromProse returned null for valid diagnostic prose.");
            }

            if (!"H3".equals(decision.hypothesis().category()) ||
                Math.abs(decision.hypothesis().confidence() - 0.88) > 0.001 ||
                !"p95".equals(decision.prediction().metric()) ||
                !"JavaMonitorEnter".equals(decision.prediction().signalToEliminate()) ||
                !"jar-unpack".equals(decision.change().template())) {
                return new ScenarioResult(6, name, false,
                        "Extracted decision fields mismatch from prose:\n  Got: " + decision);
            }

            // 3. Must satisfy cross-field validation rules
            engine.validateCrossFieldRules(decision);

            return new ScenarioResult(6, name, true, "Fallback extractor reconstructed valid OptimizationDecision from semi-structured prose.");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception during fallbackExtractFromProse: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "repairDecisionWithFeedback (Saga DECIDE One-Shot Feedback Loop)";
        try {
            FakeDecisionChatModel fakeModel = new FakeDecisionChatModel();
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_CORRUPTED_SYNTAX_JSON); // Call 1 fails
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON); // Call 2 succeeds

            ChatClient client = ChatClient.builder(fakeModel).build();
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            DecisionEnvelope<OptimizationDecision> envelope = engine.repairDecisionWithFeedback(client, "Analyze iteration 3 JFR telemetry");

            if (envelope == null) {
                return new ScenarioResult(7, name, false, "repairDecisionWithFeedback returned null envelope.");
            }
            if (!envelope.accepted()) {
                return new ScenarioResult(7, name, false, "Expected accepted envelope, but got rejected: " + envelope.rejectionReason());
            }
            if (envelope.attemptsUsed() != 2) {
                return new ScenarioResult(7, name, false, "Expected attemptsUsed == 2 after repair retry, got: " + envelope.attemptsUsed());
            }
            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(7, name, false, "Expected exactly 2 ChatModel calls, got: " + fakeModel.getCallCount());
            }

            // Verify feedback prompt contains error quote
            Prompt secondPrompt = fakeModel.getCapturedPrompts().get(1);
            String secondUserPrompt = secondPrompt.getInstructions().get(0).getText();
            if (!secondUserPrompt.contains("Previous proposal failed validation:")) {
                return new ScenarioResult(7, name, false,
                        "Feedback prompt did not include the required diagnostic feedback prefix. Got: " + secondUserPrompt);
            }

            return new ScenarioResult(7, name, true, "Saga DECIDE closed-loop one-shot feedback repair succeeded on 2nd attempt.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception during repairDecisionWithFeedback: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "enforceStrictRepairCircuitBreaker (Strict Retry Limit Enforcement)";
        try {
            FakeDecisionChatModel fakeModel = new FakeDecisionChatModel();
            // Both attempts return corrupt JSON
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_CORRUPTED_SYNTAX_JSON);
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_CORRUPTED_SYNTAX_JSON);
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON); // Must NEVER be called!

            ChatClient client = ChatClient.builder(fakeModel).build();
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            try {
                engine.enforceStrictRepairCircuitBreaker(client, "Propose decision for iteration 4");
                return new ScenarioResult(8, name, false,
                        "Expected IllegalStateException when both attempts fail, but no exception was thrown.");
            } catch (IllegalStateException e) {
                if (!e.getMessage().contains("Decision schema repair exceeded maximum attempts (1)")) {
                    return new ScenarioResult(8, name, false,
                            "IllegalStateException message mismatch. Expected 'Decision schema repair exceeded maximum attempts (1)', got: " + e.getMessage());
                }
            }

            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(8, name, false,
                        "Strict limit breached: expected exactly 2 calls (1 initial + 1 retry), but model was called " + fakeModel.getCallCount() + " times.");
            }

            return new ScenarioResult(8, name, true, "Strict retry limit enforced (capped at exactly 1 retry / 2 attempts, throws IllegalStateException).");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception during enforceStrictRepairCircuitBreaker: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("                 PHASE 02 EXERCISE 02: VERIFICATION REPORT             ");
        System.out.println("======================================================================");
        int passed = 0;
        int failed = 0;

        for (ScenarioResult r : results) {
            System.out.printf("SCENARIO %d: %s%n", r.scenarioNumber(), r.name());
            if (r.passed()) {
                System.out.println("  [PASS] " + r.errorDetail());
                passed++;
            } else {
                System.out.println("  [FAIL] " + r.errorDetail());
                failed++;
            }
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.printf("SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
        if (failed == 0) {
            System.out.println("VERDICT: ALL SCENARIOS PASSED (exit code 0)");
        } else {
            System.out.println("VERDICT: FINDINGS DETECTED (exit code 99)");
        }
        System.out.println("======================================================================");
        System.out.println();
    }
}
