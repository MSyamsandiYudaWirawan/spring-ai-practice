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
 * 15 Repetitive Muscle-Memory Drills (5 Topics x 3 Repetitions) with Tiered Progressive Hints.
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL.
 * Exits with status 0 on PASS.
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
        // Topic 1: Schema Directives & Direct Deserialization (Drills 1 - 3)
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Markdown & Conversational Sanitization (Drills 4 - 6)
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Cross-Field Invariants & Discriminated Unions (Drills 7 - 9)
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Generic Collections & Fallback Prose Extraction (Drills 10 - 12)
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Closed-Loop Feedback Repair & Circuit Breakers (Drills 13 - 15)
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Schema Directives & Direct Deserialization (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "generateFormatInstructions (Schema Directives Injection)";
        List<String> hints = List.of(
                "Use BeanOutputConverter<OptimizationDecision> to generate JSON schema directives.",
                "Verify return string is non-blank and includes schema property names.",
                "Call decisionConverter.getFormat() and return the result."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            String format = engine.generateFormatInstructions();
            if (format == null || format.isBlank()) {
                return new ScenarioResult(1, name, false, "generateFormatInstructions returned null or blank string.", hints);
            }
            if (!format.contains("hypothesis") || !format.contains("prediction") ||
                !format.contains("ledger") || !format.contains("change")) {
                return new ScenarioResult(1, name, false,
                        "Format instructions missing core schema properties (expected hypothesis, prediction, ledger, change). Got:\n" + format, hints);
            }
            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "parseDecision (Direct Template Optimization JSON Parsing)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation: throw IllegalArgumentException if rawJson is null or blank.",
                "Use decisionConverter.convert(rawJson) to parse clean JSON into OptimizationDecision.",
                "Ensure parsed record fields match fixtures: category H3, confidence 0.95, metric p95, template jar-unpack."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            OptimizationDecision decision = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);
            if (decision == null) {
                return new ScenarioResult(2, name, false, "parseDecision returned null for valid template JSON.", hints);
            }
            if (!"H3".equals(decision.hypothesis().category()) ||
                Math.abs(decision.hypothesis().confidence() - 0.95) > 0.001 ||
                !"p95".equals(decision.prediction().metric()) ||
                !"jar-unpack".equals(decision.change().template())) {
                return new ScenarioResult(2, name, false,
                        "Deserialized record field values mismatch:\n  Got: " + decision, hints);
            }
            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "parseDecision (Direct Edits Optimization JSON Parsing & Fail-Fast Boundaries)";
        List<String> hints = List.of(
                "Enforce fail-fast validation throwing IllegalArgumentException on null and whitespace strings.",
                "Parse valid source-code edits decision payload using decisionConverter.",
                "Verify category H1, confidence 0.85, metric rps, change kind 'edits', and edits count > 0."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            try {
                engine.parseDecision(null);
                return new ScenarioResult(3, name, false, "null rawJson must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                engine.parseDecision("   ");
                return new ScenarioResult(3, name, false, "blank rawJson must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            OptimizationDecision decision = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_EDITS_DECISION_JSON);
            if (decision == null) {
                return new ScenarioResult(3, name, false, "parseDecision returned null for valid edits JSON.", hints);
            }
            if (!"H1".equals(decision.hypothesis().category()) ||
                Math.abs(decision.hypothesis().confidence() - 0.85) > 0.001 ||
                !"rps".equals(decision.prediction().metric()) ||
                !"edits".equalsIgnoreCase(decision.change().kind()) ||
                decision.change().edits() == null || decision.change().edits().isEmpty()) {
                return new ScenarioResult(3, name, false, "Deserialized edits record values mismatch: " + decision, hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Markdown & Conversational Sanitization (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "extractFromConversationalText (Markdown Code Fence Stripping)";
        List<String> hints = List.of(
                "LLMs frequently wrap JSON output in markdown backtick blocks (```json ... ```).",
                "Sanitize by extracting the substring between first '{' and last '}'.",
                "Delegate sanitized JSON substring to parseDecision(json)."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            OptimizationDecision decision = engine.extractFromConversationalText(DecisionModelContracts.SAMPLE_MARKDOWN_WRAPPED_DECISION_JSON);
            if (decision == null) {
                return new ScenarioResult(4, name, false, "extractFromConversationalText returned null.", hints);
            }
            if (!"H2".equals(decision.hypothesis().category()) ||
                !"GCPhasePause".equals(decision.prediction().signalToEliminate()) ||
                !"virtual-threads".equals(decision.change().template())) {
                return new ScenarioResult(4, name, false, "Sanitized and extracted record values mismatch: " + decision, hints);
            }
            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "extractFromConversationalText (Conversational Preamble & Epilogue Stripping)";
        List<String> hints = List.of(
                "Models often prepend chat pleasantries ('Here is the optimization decision:') without markdown backticks.",
                "Slice string from first '{' to last '}' to strip both preamble and conversational epilogue.",
                "Ensure trimmed inner JSON string passes conversion cleanly."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            String conversationalText = """
                    Here is the optimization decision based on JFR metrics:
                    {
                      "hypothesis": {"category": "H1", "confidence": 0.90, "rationale": "Connection pool bottleneck"},
                      "prediction": {"metric": "rps", "direction": "improve", "signalToEliminate": "ThreadPark"},
                      "ledger": {"hypothesisCategory": "H1", "direction": "strengthen", "reason": "Exhausted pool connections"},
                      "change": {"kind": "template", "edits": null, "template": "hikari-pool", "parameters": {}}
                    }
                    Let me know if you would like me to tune additional settings.
                    """;
            OptimizationDecision decision = engine.extractFromConversationalText(conversationalText);
            if (decision == null) {
                return new ScenarioResult(5, name, false, "extractFromConversationalText returned null for conversational prose.", hints);
            }
            if (!"H1".equals(decision.hypothesis().category()) ||
                !"hikari-pool".equals(decision.change().template()) ||
                !"rps".equals(decision.prediction().metric())) {
                return new ScenarioResult(5, name, false, "Extracted decision mismatch: " + decision, hints);
            }
            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "extractFromConversationalText (Fail-Fast Boundaries on Corrupt / Missing JSON)";
        List<String> hints = List.of(
                "Fail-fast on null, blank, or text without any '{' or '}' brackets.",
                "Throw IllegalArgumentException when no valid JSON boundaries exist in input.",
                "Check `if (start == -1 || end == -1 || start > end) throw new IllegalArgumentException(...);`."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            try {
                engine.extractFromConversationalText(null);
                return new ScenarioResult(6, name, false, "null payload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                engine.extractFromConversationalText("   ");
                return new ScenarioResult(6, name, false, "blank payload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                engine.extractFromConversationalText("No JSON brackets in this plain text response");
                return new ScenarioResult(6, name, false, "payload without JSON brackets must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Cross-Field Invariants & Discriminated Unions (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "validateCrossFieldRules (Valid Template and Edits Decisions Cleared)";
        List<String> hints = List.of(
                "Ensure validateCrossFieldRules executes without exception for valid decisions.",
                "Verify both valid template decision and valid edits decision pass all checks.",
                "Validate hypothesis, prediction, ledger, and change fields for presence."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            OptimizationDecision validTemplate = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);
            engine.validateCrossFieldRules(validTemplate);

            OptimizationDecision validEdits = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_EDITS_DECISION_JSON);
            engine.validateCrossFieldRules(validEdits);

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "validateCrossFieldRules (Template Kind Rules & Hypothesis Boundaries)";
        List<String> hints = List.of(
                "For kind='template', template must be non-blank in {jar-unpack, hikari-pool, virtual-threads} and edits must be null or empty.",
                "Hypothesis category must match ^H[1-7]$ and confidence must be between 0.0 and 1.0.",
                "Throw IllegalArgumentException on any boundary or discriminated union violation."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            OptimizationDecision valid = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);

            // 1. Null decision throws
            try {
                engine.validateCrossFieldRules(null);
                return new ScenarioResult(8, name, false, "Null decision must throw NullPointerException or IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 2. Blank template throws
            OptimizationDecision blankTemplate = engine.parseDecision(DecisionModelContracts.SAMPLE_INVALID_CROSS_FIELD_JSON);
            try {
                engine.validateCrossFieldRules(blankTemplate);
                return new ScenarioResult(8, name, false, "kind=template with blank template must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            // 3. Invalid template name throws
            OptimizationDecision unknownTemplate = new OptimizationDecision(
                    valid.hypothesis(), valid.prediction(), valid.ledger(),
                    new Change("template", null, "unknown-template", Map.of())
            );
            try {
                engine.validateCrossFieldRules(unknownTemplate);
                return new ScenarioResult(8, name, false, "kind=template with unknown template must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            // 4. Invalid hypothesis category (H9) throws
            OptimizationDecision badHypo = new OptimizationDecision(
                    new Hypothesis("H9", 0.9, "Invalid category"),
                    valid.prediction(), valid.ledger(), valid.change()
            );
            try {
                engine.validateCrossFieldRules(badHypo);
                return new ScenarioResult(8, name, false, "Hypothesis category H9 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            // 5. Invalid confidence (> 1.0) throws
            OptimizationDecision badConf = new OptimizationDecision(
                    new Hypothesis("H3", 1.5, "Too high"),
                    valid.prediction(), valid.ledger(), valid.change()
            );
            try {
                engine.validateCrossFieldRules(badConf);
                return new ScenarioResult(8, name, false, "Hypothesis confidence 1.5 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "validateCrossFieldRules (Edits Kind Rules & Metric / Ledger Boundaries)";
        List<String> hints = List.of(
                "For kind='edits', edits list must not be null or empty, and template must be null or blank.",
                "Prediction metric must be one of {p95, median, rps, error_rate} and direction in {improve, regress, neutral}.",
                "Ledger hypothesisCategory must match ^H[1-7]$ and direction must be in {strengthen, weaken}."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            OptimizationDecision valid = engine.parseDecision(DecisionModelContracts.SAMPLE_VALID_EDITS_DECISION_JSON);

            // 1. kind=edits with empty edits list throws
            OptimizationDecision emptyEdits = new OptimizationDecision(
                    valid.hypothesis(), valid.prediction(), valid.ledger(),
                    new Change("edits", List.of(), null, Map.of())
            );
            try {
                engine.validateCrossFieldRules(emptyEdits);
                return new ScenarioResult(9, name, false, "kind=edits with empty edits list must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            // 2. kind=edits with non-null template throws
            OptimizationDecision editsWithTemplate = new OptimizationDecision(
                    valid.hypothesis(), valid.prediction(), valid.ledger(),
                    new Change("edits", valid.change().edits(), "jar-unpack", Map.of())
            );
            try {
                engine.validateCrossFieldRules(editsWithTemplate);
                return new ScenarioResult(9, name, false, "kind=edits with non-null template must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            // 3. Invalid prediction metric throws
            OptimizationDecision badMetric = new OptimizationDecision(
                    valid.hypothesis(),
                    new Prediction("cpu_spike", "improve", "JavaMonitorEnter"),
                    valid.ledger(), valid.change()
            );
            try {
                engine.validateCrossFieldRules(badMetric);
                return new ScenarioResult(9, name, false, "Invalid prediction metric must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            // 4. Invalid ledger direction throws
            OptimizationDecision badLedgerDir = new OptimizationDecision(
                    valid.hypothesis(), valid.prediction(),
                    new LedgerUpdate("H1", "ignore", "not a valid direction"),
                    valid.change()
            );
            try {
                engine.validateCrossFieldRules(badLedgerDir);
                return new ScenarioResult(9, name, false, "Invalid ledger direction must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Generic Collections & Fallback Prose Extraction (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "parseDecisionBatch (Generic Collection List<OptimizationDecision> Deserialization)";
        List<String> hints = List.of(
                "Use new BeanOutputConverter<>(new ParameterizedTypeReference<List<OptimizationDecision>>() {}) for generic list conversion.",
                "Verify returned List<OptimizationDecision> contains all elements with strongly typed fields.",
                "Check batch size is 2 and elements correspond to H3 and H4."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();
            List<OptimizationDecision> batch = engine.parseDecisionBatch(DecisionModelContracts.SAMPLE_DECISION_BATCH_JSON);
            if (batch == null || batch.size() != 2) {
                return new ScenarioResult(10, name, false, "Expected batch of size 2, got: " + (batch == null ? "null" : batch.size()), hints);
            }
            OptimizationDecision d1 = batch.get(0);
            OptimizationDecision d2 = batch.get(1);
            if (!"H3".equals(d1.hypothesis().category()) || !"jar-unpack".equals(d1.change().template()) ||
                !"H4".equals(d2.hypothesis().category()) || !"hikari-pool".equals(d2.change().template())) {
                return new ScenarioResult(10, name, false, "Batch element values do not match expected fixtures: " + batch, hints);
            }
            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "parseDecisionBatch (Batch Boundary Fail-Fast Invariants)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary checking on jsonArrayPayload before generic conversion.",
                "Throw IllegalArgumentException if jsonArrayPayload is null or blank.",
                "Check `if (jsonArrayPayload == null || jsonArrayPayload.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            try {
                engine.parseDecisionBatch(null);
                return new ScenarioResult(11, name, false, "null jsonArrayPayload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                engine.parseDecisionBatch("   ");
                return new ScenarioResult(11, name, false, "blank jsonArrayPayload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "fallbackExtractFromProse (Fallback Semi-Structured Diagnosis Reconstruction)";
        List<String> hints = List.of(
                "When model responds in prose without JSON brackets, extract tokens via regex (PROSE_HYPOTHESIS_PATTERN, PROSE_TEMPLATE_PATTERN, etc.).",
                "Return null if input text is null, blank, or missing required hypothesis/template tokens.",
                "Reconstruct and return valid OptimizationDecision that passes validateCrossFieldRules."
        );
        try {
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            // Null / blank / non-diagnostic text returns null
            if (engine.fallbackExtractFromProse(null) != null) {
                return new ScenarioResult(12, name, false, "Null prose must return null.", hints);
            }
            if (engine.fallbackExtractFromProse("   ") != null) {
                return new ScenarioResult(12, name, false, "Blank prose must return null.", hints);
            }
            if (engine.fallbackExtractFromProse("System is running normally without errors.") != null) {
                return new ScenarioResult(12, name, false, "Unrelated prose text without hypothesis/template must return null.", hints);
            }

            OptimizationDecision decision = engine.fallbackExtractFromProse(DecisionModelContracts.SAMPLE_UNSTRUCTURED_PROSE_DECISION);
            if (decision == null) {
                return new ScenarioResult(12, name, false, "fallbackExtractFromProse returned null for valid diagnostic prose.", hints);
            }
            if (!"H3".equals(decision.hypothesis().category()) ||
                Math.abs(decision.hypothesis().confidence() - 0.88) > 0.001 ||
                !"p95".equals(decision.prediction().metric()) ||
                !"JavaMonitorEnter".equals(decision.prediction().signalToEliminate()) ||
                !"jar-unpack".equals(decision.change().template())) {
                return new ScenarioResult(12, name, false, "Extracted decision fields mismatch from prose: " + decision, hints);
            }

            engine.validateCrossFieldRules(decision);
            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Closed-Loop Feedback Repair & Circuit Breakers (Drills 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "repairDecisionWithFeedback (Saga DECIDE One-Shot Feedback Loop Recovery)";
        List<String> hints = List.of(
                "Attempt 1 calls client with promptText. If parsing or validation fails, catch exception.",
                "Attempt 2 sends feedback prompt: 'Previous proposal failed validation: ' + e.getMessage() + '. Please fix the error and return strictly valid JSON matching the schema.'",
                "Return DecisionEnvelope.accepted(decision, attemptsUsed) when attempt 2 succeeds."
        );
        try {
            FakeDecisionChatModel fakeModel = new FakeDecisionChatModel();
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_CORRUPTED_SYNTAX_JSON); // Call 1 fails
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON); // Call 2 succeeds

            ChatClient client = ChatClient.builder(fakeModel).build();
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            DecisionEnvelope<OptimizationDecision> envelope = engine.repairDecisionWithFeedback(client, "Analyze iteration 3 JFR telemetry");

            if (envelope == null) {
                return new ScenarioResult(13, name, false, "repairDecisionWithFeedback returned null envelope.", hints);
            }
            if (!envelope.accepted()) {
                return new ScenarioResult(13, name, false, "Expected accepted envelope, but got rejected: " + envelope.rejectionReason(), hints);
            }
            if (envelope.attemptsUsed() != 2) {
                return new ScenarioResult(13, name, false, "Expected attemptsUsed == 2 after repair retry, got: " + envelope.attemptsUsed(), hints);
            }
            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(13, name, false, "Expected exactly 2 ChatModel calls, got: " + fakeModel.getCallCount(), hints);
            }

            Prompt secondPrompt = fakeModel.getCapturedPrompts().get(1);
            String secondUserPrompt = secondPrompt.getInstructions().get(0).getText();
            if (!secondUserPrompt.contains("Previous proposal failed validation:")) {
                return new ScenarioResult(13, name, false,
                        "Feedback prompt did not include the required diagnostic feedback prefix. Got: " + secondUserPrompt, hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "repairDecisionWithFeedback (Immediate First-Turn Acceptance Without Repair)";
        List<String> hints = List.of(
                "When initial response parses and validates cleanly, do not trigger a second feedback call.",
                "Return DecisionEnvelope.accepted(decision, 1) directly on attempt 1.",
                "Ensure fakeModel.getCallCount() == 1."
        );
        try {
            FakeDecisionChatModel fakeModel = new FakeDecisionChatModel();
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);

            ChatClient client = ChatClient.builder(fakeModel).build();
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            DecisionEnvelope<OptimizationDecision> envelope = engine.repairDecisionWithFeedback(client, "Propose initial baseline decision");

            if (envelope == null) {
                return new ScenarioResult(14, name, false, "repairDecisionWithFeedback returned null envelope.", hints);
            }
            if (!envelope.accepted()) {
                return new ScenarioResult(14, name, false, "Expected accepted envelope on clean first attempt, got rejected: " + envelope.rejectionReason(), hints);
            }
            if (envelope.attemptsUsed() != 1) {
                return new ScenarioResult(14, name, false, "Expected attemptsUsed == 1 on first-turn success, got: " + envelope.attemptsUsed(), hints);
            }
            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(14, name, false, "Expected exactly 1 ChatModel call, got: " + fakeModel.getCallCount(), hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "enforceStrictRepairCircuitBreaker (Strict Retry Limit Enforcement & Circuit Breaker)";
        List<String> hints = List.of(
                "Enforce bounded repair retries: capped at exactly 1 retry (maximum 2 attempts).",
                "If attempt 2 also fails, throw IllegalStateException('Decision schema repair exceeded maximum attempts (1)').",
                "Ensure model is NEVER invoked a 3rd time (circuit breaker trips)."
        );
        try {
            FakeDecisionChatModel fakeModel = new FakeDecisionChatModel();
            // Both attempts return corrupt JSON; 3rd payload must never be touched
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_CORRUPTED_SYNTAX_JSON);
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_CORRUPTED_SYNTAX_JSON);
            fakeModel.enqueue(DecisionModelContracts.SAMPLE_VALID_TEMPLATE_DECISION_JSON);

            ChatClient client = ChatClient.builder(fakeModel).build();
            DecisionEngineUnderTest engine = new DecisionEngineUnderTest();

            try {
                engine.enforceStrictRepairCircuitBreaker(client, "Propose decision for iteration 4");
                return new ScenarioResult(15, name, false,
                        "Expected IllegalStateException when both attempts fail, but no exception was thrown.", hints);
            } catch (IllegalStateException e) {
                if (!e.getMessage().contains("Decision schema repair exceeded maximum attempts (1)")) {
                    return new ScenarioResult(15, name, false,
                            "IllegalStateException message mismatch. Expected 'Decision schema repair exceeded maximum attempts (1)', got: " + e.getMessage(), hints);
                }
            }

            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(15, name, false,
                        "Strict limit breached: expected exactly 2 calls (1 initial + 1 retry), but model was called " + fakeModel.getCallCount() + " times.", hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Report Formatting
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 02 EXERCISE 02: RESILIENT DECISIONS & SCHEMA REPAIR REPORT");
        System.out.println("===============================================================================");
        int passed = 0;
        String[] hintLabels = {"[HINT 1 (Concept)]", "[HINT 2 (Edge Case)]", "[HINT 3 (Code Pattern)]"};

        for (ScenarioResult r : results) {
            String status = r.passed() ? "[PASS]" : "[FAIL]";
            if (r.passed()) passed++;
            System.out.printf("  %s Scenario %02d: %s%n", status, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null && !r.hints().isEmpty()) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        String label = i < hintLabels.length ? hintLabels[i] : String.format("[HINT %d]", i + 1);
                        System.out.printf("         --> %s %s%n", label, r.hints().get(i));
                    }
                }
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
