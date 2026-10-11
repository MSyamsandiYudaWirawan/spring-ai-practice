package phase02;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.ParameterizedTypeReference;
import phase02.SecurityModelContracts.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 02 Exercise 01:
 * Structured Outputs & Resilient Schema Extraction (15 Repetitive Drills).
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
        // Topic 1: BeanOutputConverter & Markdown Fence Stripping
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Collections & Generic ParameterizedTypeReference
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: ChatClient High-Level Type-Safe Entity Extraction
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Domain Invariant Enforcement & Diagnostic Result Envelopes
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: One-Shot Schema Repair Loops & Bounded Retries
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: BeanOutputConverter & Markdown Fence Stripping (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "parseVulnerabilityAssessment (Direct BeanOutputConverter Deserialization)";
        List<String> hints = List.of(
                "Throw IllegalArgumentException if rawJson is null or blank.",
                "Use BeanOutputConverter<VulnerabilityAssessment> to convert JSON.",
                "Verify returned record matches CVE-2026-38491 and CRITICAL severity."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.parseVulnerabilityAssessment(null);
                return new ScenarioResult(1, name, false, "null rawJson must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.parseVulnerabilityAssessment("   ");
                return new ScenarioResult(1, name, false, "blank rawJson must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            VulnerabilityAssessment va = service.parseVulnerabilityAssessment(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);
            if (va == null) {
                return new ScenarioResult(1, name, false, "parseVulnerabilityAssessment returned null", hints);
            }
            if (!"CVE-2026-38491".equals(va.cveId()) || va.severity() != Severity.CRITICAL || Math.abs(va.cvssScore() - 9.8) > 0.001) {
                return new ScenarioResult(1, name, false, "Record field values corrupted: " + va, hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "extractFromMarkdownFences (Fence Stripping & Conversational Sanitization)";
        List<String> hints = List.of(
                "Extract JSON substring from first '{' to last '}' (or strip ```json fences).",
                "Pass sanitized JSON string to BeanOutputConverter.",
                "Verify extracted record contains CVE-2026-11892 and HIGH severity."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            VulnerabilityAssessment va = service.extractFromMarkdownFences(SecurityModelContracts.SAMPLE_MARKDOWN_WRAPPED_CVE_JSON);
            if (va == null) {
                return new ScenarioResult(2, name, false, "extractFromMarkdownFences returned null", hints);
            }
            if (!"CVE-2026-11892".equals(va.cveId()) || va.severity() != Severity.HIGH || !va.packageCoordinate().contains("jackson-databind")) {
                return new ScenarioResult(2, name, false, "Extracted values mismatch: " + va, hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "parsePatchPlan (Direct Single Record Deserialization)";
        List<String> hints = List.of(
                "Throw IllegalArgumentException if rawJson is null or blank.",
                "Use BeanOutputConverter<PatchPlan> to convert JSON.",
                "Verify returned PatchPlan matches PATCH-2026-001 and IMMEDIATE_ROLLOUT."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.parsePatchPlan(null);
                return new ScenarioResult(3, name, false, "null rawJson must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            PatchPlan plan = service.parsePatchPlan(SecurityModelContracts.SAMPLE_SINGLE_PATCH_PLAN_JSON);
            if (plan == null || !"PATCH-2026-001".equals(plan.patchId()) || plan.actionType() != PatchActionType.IMMEDIATE_ROLLOUT) {
                return new ScenarioResult(3, name, false, "parsePatchPlan returned invalid record: " + plan, hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Collections & Generic ParameterizedTypeReference (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "extractPatchPlanList (Generic Collections via ParameterizedTypeReference)";
        List<String> hints = List.of(
                "Throw IllegalArgumentException if jsonArrayPayload is null or blank.",
                "Use new BeanOutputConverter<>(new ParameterizedTypeReference<List<PatchPlan>>() {}).",
                "Verify List contains exactly 2 PatchPlan records."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.extractPatchPlanList(null);
                return new ScenarioResult(4, name, false, "null jsonArrayPayload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            List<PatchPlan> plans = service.extractPatchPlanList(SecurityModelContracts.SAMPLE_PATCH_PLANS_JSON);
            if (plans == null || plans.size() != 2) {
                return new ScenarioResult(4, name, false, "Expected 2 patch plans, got: " + (plans != null ? plans.size() : "null"), hints);
            }
            if (!"PATCH-2026-001".equals(plans.get(0).patchId()) || !"PATCH-2026-002".equals(plans.get(1).patchId())) {
                return new ScenarioResult(4, name, false, "Patch plan values corrupted: " + plans, hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "extractVulnerabilityList (Generic VulnerabilityAssessment List Extraction)";
        List<String> hints = List.of(
                "Throw IllegalArgumentException if jsonArrayPayload is null or blank.",
                "Use new BeanOutputConverter<>(new ParameterizedTypeReference<List<VulnerabilityAssessment>>() {}).",
                "Verify List contains 2 VulnerabilityAssessment records."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.extractVulnerabilityList(null);
                return new ScenarioResult(5, name, false, "null jsonArrayPayload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            List<VulnerabilityAssessment> list = service.extractVulnerabilityList(SecurityModelContracts.SAMPLE_VULNERABILITY_LIST_JSON);
            if (list == null || list.size() != 2) {
                return new ScenarioResult(5, name, false, "Expected 2 vulnerability assessments, got: " + (list != null ? list.size() : "null"), hints);
            }
            if (!"CVE-2026-38491".equals(list.get(0).cveId()) || !"CVE-2026-11892".equals(list.get(1).cveId())) {
                return new ScenarioResult(5, name, false, "List values mismatch: " + list, hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "extractSeverityMap (Generic Map Extraction via ParameterizedTypeReference)";
        List<String> hints = List.of(
                "Throw IllegalArgumentException if jsonMapPayload is null or blank.",
                "Use new BeanOutputConverter<>(new ParameterizedTypeReference<Map<String, Severity>>() {}).",
                "Verify Map contains Severity enum mappings."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.extractSeverityMap(null);
                return new ScenarioResult(6, name, false, "null jsonMapPayload must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            Map<String, Severity> map = service.extractSeverityMap(SecurityModelContracts.SAMPLE_SEVERITY_MAP_JSON);
            if (map == null || map.size() != 2 || map.get("CVE-2026-38491") != Severity.CRITICAL || map.get("CVE-2026-11892") != Severity.HIGH) {
                return new ScenarioResult(6, name, false, "Map extraction failed: " + map, hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: ChatClient High-Level Type-Safe Entity Extraction (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "generateAssessmentWithEntity (ChatClient .entity(Class<T>) Execution)";
        List<String> hints = List.of(
                "Validate client is not null and advisoryText is not null/blank.",
                "Execute client.prompt().user(advisoryText).call().entity(VulnerabilityAssessment.class).",
                "Verify returned record matches model output."
        );
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.generateAssessmentWithEntity(null, "text");
                return new ScenarioResult(7, name, false, "null client must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            VulnerabilityAssessment result = service.generateAssessmentWithEntity(client, "Audit auth service");
            if (result == null || !"CVE-2026-38491".equals(result.cveId())) {
                return new ScenarioResult(7, name, false, "Entity extraction mismatch: " + result, hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "generatePatchPlansWithEntity (ChatClient .entity(ParameterizedTypeReference) Execution)";
        List<String> hints = List.of(
                "Validate client is not null and advisoryText is not null/blank.",
                "Execute client.prompt().user(advisoryText).call().entity(new ParameterizedTypeReference<List<PatchPlan>>() {}).",
                "Verify returned List contains patch plans."
        );
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_PATCH_PLANS_JSON);
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            List<PatchPlan> plans = service.generatePatchPlansWithEntity(client, "Generate patch plans");
            if (plans == null || plans.size() != 2 || !"PATCH-2026-001".equals(plans.get(0).patchId())) {
                return new ScenarioResult(8, name, false, "Entity collection extraction mismatch: " + plans, hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "generateCustomEntity (Generic Parameterized Class Extraction)";
        List<String> hints = List.of(
                "Validate client, advisoryText, and target class are not null.",
                "Execute client.prompt().user(advisoryText).call().entity(clazz).",
                "Return typed instance of T."
        );
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_SINGLE_PATCH_PLAN_JSON);
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            PatchPlan plan = service.generateCustomEntity(client, "Single patch plan", PatchPlan.class);
            if (plan == null || !"PATCH-2026-001".equals(plan.patchId())) {
                return new ScenarioResult(9, name, false, "Custom entity extraction mismatch: " + plan, hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Domain Invariant Enforcement & Diagnostic Result Envelopes (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "validateRecordInvariants (TigerStyle Domain Invariant Validation)";
        List<String> hints = List.of(
                "Throw IllegalArgumentException if assessment is null.",
                "Validate cveId regex ^CVE-\\d{4}-\\d{4,7}$, cvssScore in [0.0..10.0], packageCoordinate contains ':'.",
                "Validate rootCauseAnalysis is not blank, mitigationSteps is not empty."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.validateRecordInvariants(null);
                return new ScenarioResult(10, name, false, "null assessment must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            VulnerabilityAssessment valid = new VulnerabilityAssessment(
                    "CVE-2026-99120",
                    Severity.MEDIUM,
                    5.5,
                    "io.netty:netty-codec-http:4.1.100.Final",
                    "Header delimiter ambiguity",
                    List.of("Upgrade netty")
            );
            service.validateRecordInvariants(valid);

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "safeDeserializeWithFallback (Diagnostic TriageEnvelope Wrapping)";
        List<String> hints = List.of(
                "Wrap deserialization in try-catch without letting exceptions escape.",
                "Return TriageEnvelope.accepted(data) on valid JSON.",
                "Return TriageEnvelope.rejected(error) on syntax or schema errors."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            TriageEnvelope<VulnerabilityAssessment> accepted = service.safeDeserializeWithFallback(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);
            if (accepted == null || !accepted.accepted() || accepted.data() == null) {
                return new ScenarioResult(11, name, false, "Valid JSON must return accepted envelope", hints);
            }

            TriageEnvelope<VulnerabilityAssessment> rejected = service.safeDeserializeWithFallback(SecurityModelContracts.SAMPLE_CORRUPTED_JSON);
            if (rejected == null || rejected.accepted() || rejected.rejectionReason() == null || rejected.rejectionReason().isBlank()) {
                return new ScenarioResult(11, name, false, "Corrupted JSON must return rejected envelope", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "safeBatchDeserializeWithFallback (Batch Triage Envelope Collection)";
        List<String> hints = List.of(
                "Iterate over jsonList and safely parse each entry into TriageEnvelope.",
                "Return List<TriageEnvelope<VulnerabilityAssessment>> preserving order.",
                "Isolate corrupted items without dropping valid items."
        );
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            List<String> batch = List.of(
                    SecurityModelContracts.SAMPLE_VALID_CVE_JSON,
                    SecurityModelContracts.SAMPLE_CORRUPTED_JSON,
                    SecurityModelContracts.SAMPLE_MARKDOWN_WRAPPED_CVE_JSON
            );

            List<TriageEnvelope<VulnerabilityAssessment>> envelopes = service.safeBatchDeserializeWithFallback(batch);
            if (envelopes == null || envelopes.size() != 3) {
                return new ScenarioResult(12, name, false, "Expected 3 envelopes, got: " + (envelopes != null ? envelopes.size() : "null"), hints);
            }
            if (!envelopes.get(0).accepted() || envelopes.get(1).accepted() || !envelopes.get(2).accepted()) {
                return new ScenarioResult(12, name, false, "Envelope outcomes mismatch: [0]=" + envelopes.get(0).accepted() + ", [1]=" + envelopes.get(1).accepted() + ", [2]=" + envelopes.get(2).accepted(), hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: One-Shot Schema Repair Loops & Bounded Retries (Drills 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "repairWithOneShotRetry (Saga DECIDE One-Shot Feedback Loop)";
        List<String> hints = List.of(
                "Attempt 1: Call client with 'Analyze vulnerability for: ' + cveId.",
                "On failure, Call client attempt 2 quoting 'Previous output failed schema validation: ' + ex.getMessage().",
                "Parse attempt 2 output and return repaired VulnerabilityAssessment."
        );
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON); // Call 1 fails
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);     // Call 2 succeeds
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            VulnerabilityAssessment result = service.repairWithOneShotRetry(client, "CVE-2026-38491");
            if (result == null || !"CVE-2026-38491".equals(result.cveId())) {
                return new ScenarioResult(13, name, false, "repairWithOneShotRetry failed to return repaired record", hints);
            }
            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(13, name, false, "Expected exactly 2 model calls, got: " + fakeModel.getCallCount(), hints);
            }

            Prompt retryPrompt = fakeModel.getLastPrompt();
            String userText = retryPrompt != null && retryPrompt.getUserMessage() != null ? retryPrompt.getUserMessage().getText() : "";
            if (!userText.toLowerCase().contains("failed schema validation") && !userText.toLowerCase().contains("previous output failed")) {
                return new ScenarioResult(13, name, false, "Retry prompt did not quote failure message back to model", hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "enforceRetryLimit (Strict Retry Bound Enforcement)";
        List<String> hints = List.of(
                "Strictly limit model calls to at most 2 (1 initial + 1 repair retry).",
                "If attempt 2 also fails, throw IllegalStateException containing 'Schema repair exceeded maximum attempts (1)'.",
                "Do not execute a 3rd call."
        );
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON); // Call 1 fails
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON); // Call 2 fails
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);     // Call 3 should never happen!
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.enforceRetryLimit(client, "CVE-2026-99120");
                return new ScenarioResult(14, name, false, "When both attempts fail, must throw IllegalStateException", hints);
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains("Schema repair exceeded maximum attempts (1)")) {
                    return new ScenarioResult(14, name, false, "Exception message must contain 'Schema repair exceeded maximum attempts (1)', got: " + expected.getMessage(), hints);
                }
            }

            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(14, name, false, "Model calls must be capped at 2, got: " + fakeModel.getCallCount(), hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "autonomousResilientTriagePipeline (Production End-to-End Triage)";
        List<String> hints = List.of(
                "Execute one-shot repair loop and wrap outcome into TriageEnvelope.",
                "If successful, return TriageEnvelope.accepted(assessment).",
                "If both attempts fail, return TriageEnvelope.rejected(error) without throwing uncaught exceptions."
        );
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_MARKDOWN_WRAPPED_CVE_JSON);
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            TriageEnvelope<VulnerabilityAssessment> env1 = service.autonomousResilientTriagePipeline(client, "CVE-2026-11892");
            if (env1 == null || !env1.accepted() || env1.data() == null || !"CVE-2026-11892".equals(env1.data().cveId())) {
                return new ScenarioResult(15, name, false, "Pipeline failed on markdown wrapped CVE output", hints);
            }

            fakeModel.clear();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON);
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON);
            TriageEnvelope<VulnerabilityAssessment> env2 = service.autonomousResilientTriagePipeline(client, "CVE-2026-99999");
            if (env2 == null || env2.accepted() || env2.rejectionReason() == null) {
                return new ScenarioResult(15, name, false, "Pipeline must return rejected envelope when both repair attempts fail", hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 02 EXERCISE 01: STRUCTURED OUTPUTS & SCHEMA EXTRACTION REPORT");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String status = r.passed() ? "[PASS]" : "[FAIL]";
            if (r.passed()) passed++;
            System.out.printf("  %s Scenario %02d: %s%n", status, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        System.out.printf("         --> HINT %d: %s%n", i + 1, r.hints().get(i));
                    }
                }
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        System.out.println("===============================================================================");
    }
}
