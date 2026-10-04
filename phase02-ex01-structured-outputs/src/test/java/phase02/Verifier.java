package phase02;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import phase02.SecurityModelContracts.PatchActionType;
import phase02.SecurityModelContracts.PatchPlan;
import phase02.SecurityModelContracts.Severity;
import phase02.SecurityModelContracts.TriageEnvelope;
import phase02.SecurityModelContracts.VulnerabilityAssessment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 02 Exercise 01 (Structured Outputs & Resilient Schema Extraction).
 * DO NOT MODIFY THIS FILE.
 *
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
        String name = "parseVulnerabilityAssessment (Direct BeanOutputConverter Deserialization)";
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            // 1. Boundary check: null / blank throws exception
            try {
                service.parseVulnerabilityAssessment(null);
                return new ScenarioResult(1, name, false, "Fail-fast invariant failed: null rawJson must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.parseVulnerabilityAssessment("   ");
                return new ScenarioResult(1, name, false, "Fail-fast invariant failed: blank rawJson must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 2. Normal deserialization check
            VulnerabilityAssessment va = service.parseVulnerabilityAssessment(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);
            if (va == null) {
                return new ScenarioResult(1, name, false, "parseVulnerabilityAssessment returned null.");
            }

            if (!"CVE-2026-38491".equals(va.cveId()) || va.severity() != Severity.CRITICAL ||
                Math.abs(va.cvssScore() - 9.8) > 0.001 || !va.packageCoordinate().contains("spring-security-web")) {
                return new ScenarioResult(1, name, false,
                        "Deserialized record field values mismatch:\n  Got: " + va);
            }

            return new ScenarioResult(1, name, true, "Direct JSON deserialization cleanly parsed into Java record.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception during parse: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "extractFromMarkdownFences (Fence Stripping & Conversational Sanitization)";
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            VulnerabilityAssessment va = service.extractFromMarkdownFences(SecurityModelContracts.SAMPLE_MARKDOWN_WRAPPED_CVE_JSON);
            if (va == null) {
                return new ScenarioResult(2, name, false, "extractFromMarkdownFences returned null.");
            }

            if (!"CVE-2026-11892".equals(va.cveId()) || va.severity() != Severity.HIGH ||
                Math.abs(va.cvssScore() - 8.5) > 0.001 || !va.packageCoordinate().contains("jackson-databind")) {
                return new ScenarioResult(2, name, false,
                        "Sanitized and extracted record values mismatch:\n  Got: " + va);
            }

            return new ScenarioResult(2, name, true, "Markdown fences and conversational preamble stripped cleanly before parsing.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Parser threw exception on markdown fences: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "extractPatchPlanList (Generic Collections via ParameterizedTypeReference)";
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            // 1. Boundary check: null / blank
            try {
                service.extractPatchPlanList(null);
                return new ScenarioResult(3, name, false, "Fail-fast invariant failed: null jsonArrayPayload must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            List<PatchPlan> plans = service.extractPatchPlanList(SecurityModelContracts.SAMPLE_PATCH_PLANS_JSON);
            if (plans == null || plans.isEmpty()) {
                return new ScenarioResult(3, name, false, "extractPatchPlanList returned null or empty list.");
            }

            if (plans.size() != 2) {
                return new ScenarioResult(3, name, false, "Expected 2 patch plans, got: " + plans.size());
            }

            PatchPlan p1 = plans.get(0);
            if (!"PATCH-2026-001".equals(p1.patchId()) || p1.actionType() != PatchActionType.IMMEDIATE_ROLLOUT ||
                p1.requiresDowntime()) {
                return new ScenarioResult(3, name, false, "First patch plan values corrupted: " + p1);
            }

            PatchPlan p2 = plans.get(1);
            if (!"PATCH-2026-002".equals(p2.patchId()) || p2.actionType() != PatchActionType.CANARY_DEPLOY ||
                !p2.requiresDowntime()) {
                return new ScenarioResult(3, name, false, "Second patch plan values corrupted: " + p2);
            }

            return new ScenarioResult(3, name, true, "Generic collection List<PatchPlan> deserialized cleanly via ParameterizedTypeReference.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception during collection extraction: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "generateAssessmentWithSchema (ChatClient .entity(Class<T>) Execution)";
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            // 1. Boundary check: null client or blank text
            try {
                service.generateAssessmentWithSchema(null, "text");
                return new ScenarioResult(4, name, false, "Fail-fast invariant failed: null client must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            VulnerabilityAssessment result = service.generateAssessmentWithSchema(client, "Audit auth service for CVE-2026-38491");
            if (result == null) {
                return new ScenarioResult(4, name, false, "generateAssessmentWithSchema returned null.");
            }

            if (!"CVE-2026-38491".equals(result.cveId()) || result.severity() != Severity.CRITICAL) {
                return new ScenarioResult(4, name, false, "Assessment record values mismatch: " + result);
            }

            return new ScenarioResult(4, name, true, "Type-safe ChatClient execution via .entity(Class<T>) verified.");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception during ChatClient entity call: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "validateRecordInvariants (TigerStyle Domain Invariant Validation)";
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            // 1. Valid record must pass validation
            VulnerabilityAssessment valid = new VulnerabilityAssessment(
                    "CVE-2026-99120",
                    Severity.MEDIUM,
                    5.5,
                    "io.netty:netty-codec-http:4.1.100.Final",
                    "Header delimiter ambiguity",
                    List.of("Upgrade netty to 4.1.101+")
            );
            service.validateRecordInvariants(valid);

            // 2. Invalid inputs must be caught:
            try {
                service.validateRecordInvariants(null);
                return new ScenarioResult(5, name, false, "Null assessment must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(5, name, true, "Domain invariant boundaries verified (CVE regex, CVSS range, package coordinate format).");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception during invariant validation: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "safeDeserializeWithFallback (Diagnostic TriageEnvelope Wrapping)";
        try {
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            // 1. Valid JSON returns accepted envelope
            TriageEnvelope<VulnerabilityAssessment> accepted = service.safeDeserializeWithFallback(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);
            if (accepted == null || !accepted.accepted() || accepted.data() == null) {
                return new ScenarioResult(6, name, false, "Valid JSON must return accepted envelope with non-null data.");
            }

            // 2. Corrupted JSON returns rejected envelope WITHOUT throwing uncaught exception
            TriageEnvelope<VulnerabilityAssessment> rejected = service.safeDeserializeWithFallback(SecurityModelContracts.SAMPLE_CORRUPTED_JSON);
            if (rejected == null || rejected.accepted() || rejected.rejectionReason() == null || rejected.rejectionReason().isBlank()) {
                return new ScenarioResult(6, name, false,
                        "Corrupted JSON must return rejected envelope with descriptive rejectionReason.");
            }

            return new ScenarioResult(6, name, true, "Exceptions safely caught and wrapped into diagnostic TriageEnvelope.");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Uncaught exception escaped from safeDeserializeWithFallback: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "repairWithOneShotRetry (Saga DECIDE One-Shot Feedback Loop)";
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON); // Call 1 fails
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);     // Call 2 succeeds
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            VulnerabilityAssessment result = service.repairWithOneShotRetry(client, "CVE-2026-38491");
            if (result == null) {
                return new ScenarioResult(7, name, false, "repairWithOneShotRetry returned null.");
            }

            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(7, name, false,
                        "Expected exactly 2 model calls (1 initial + 1 feedback retry), got: " + fakeModel.getCallCount());
            }

            Prompt retryPrompt = fakeModel.getLastPrompt();
            String userText = retryPrompt != null && retryPrompt.getUserMessage() != null
                    ? retryPrompt.getUserMessage().getText() : "";

            if (!userText.toLowerCase().contains("failed schema validation") &&
                !userText.toLowerCase().contains("previous output failed")) {
                return new ScenarioResult(7, name, false,
                        "Retry prompt does not contain error feedback quoted back to model:\n  Actual: \"" + userText + "\"");
            }

            if (!"CVE-2026-38491".equals(result.cveId())) {
                return new ScenarioResult(7, name, false, "Repaired assessment record values corrupted: " + result);
            }

            return new ScenarioResult(7, name, true, "One-shot feedback schema repair loop succeeded on 2nd attempt.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception during schema repair retry: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "enforceRetryLimit (Strict Retry Bound Enforcement)";
        try {
            FakeSecurityChatModel fakeModel = new FakeSecurityChatModel();
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON); // Call 1 fails
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_CORRUPTED_JSON); // Call 2 fails
            fakeModel.enqueue(SecurityModelContracts.SAMPLE_VALID_CVE_JSON);     // Call 3 should NEVER happen!
            ChatClient client = ChatClient.create(fakeModel);
            SecurityTriageServiceUnderTest service = new SecurityTriageServiceUnderTest();

            try {
                service.enforceRetryLimit(client, "CVE-2026-99120");
                return new ScenarioResult(8, name, false,
                        "Fail-fast invariant failed: when both attempts fail, must throw IllegalStateException!");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains("Schema repair exceeded maximum attempts (1)")) {
                    return new ScenarioResult(8, name, false,
                            "Exception message mismatch: expected to contain 'Schema repair exceeded maximum attempts (1)', got: " +
                            expected.getMessage());
                }
            }

            if (fakeModel.getCallCount() != 2) {
                return new ScenarioResult(8, name, false,
                        "Runaway retry detected! Model called " + fakeModel.getCallCount() + " times (must cap at exactly 2).");
            }

            return new ScenarioResult(8, name, true, "Strict retry limit enforced (capped at exactly 1 retry, throws IllegalStateException).");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception during retry limit check: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("\n======================================================================");
        System.out.println("                 PHASE 02 EXERCISE 01: VERIFICATION REPORT             ");
        System.out.println("======================================================================");

        int passed = 0;
        int failed = 0;

        for (ScenarioResult res : results) {
            System.out.println("SCENARIO " + res.scenarioNumber() + ": " + res.name());
            if (res.passed()) {
                System.out.println("  [PASS] " + res.errorDetail());
                passed++;
            } else {
                System.out.println("  [FAIL] " + res.errorDetail().replace("\n", "\n         "));
                failed++;
            }
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.println("SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        if (failed > 0) {
            System.out.println("VERDICT: FAIL (exit code 99)");
        } else {
            System.out.println("VERDICT: ALL SCENARIOS PASSED (exit code 0)");
        }
        System.out.println("======================================================================\n");
    }
}
