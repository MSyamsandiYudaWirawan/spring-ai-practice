package phase01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 01 Exercise 03 (Cloud SRE & Kubernetes Operations).
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
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "analyzePodCrash (Boundary Validation & Role Separation)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            // 1. Boundary check: null / blank inputs must throw
            try {
                service.analyzePodCrash(null, "default", "logs");
                return new ScenarioResult(1, name, false, "Failed fail-fast invariant: null clusterId must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.analyzePodCrash("cluster-1", " ", "logs");
                return new ScenarioResult(1, name, false, "Failed fail-fast invariant: blank namespace must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 2. Role separation check
            String cluster = "us-east-k8s-prod-1";
            String namespace = "payments";
            String logs = "FATAL 2026-09-30 08:10:12 connection pool exhausted";
            service.analyzePodCrash(cluster, namespace, logs);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(1, name, false, "No prompt dispatched.");

            String expectedSystem = "Cluster: " + cluster + " | Namespace: " + namespace;
            if (prompt.getSystemMessage() == null || !expectedSystem.equals(prompt.getSystemMessage().getText())) {
                String actual = prompt.getSystemMessage() != null ? prompt.getSystemMessage().getText() : "null";
                return new ScenarioResult(1, name, false,
                        "SystemMessage role context mismatch:\n  Expected: \"" + expectedSystem + "\"\n  Actual:   \"" + actual + "\"");
            }

            if (prompt.getUserMessage() == null || !logs.equals(prompt.getUserMessage().getText())) {
                String actual = prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "null";
                return new ScenarioResult(1, name, false,
                        "UserMessage mismatch (contains leaked cluster context):\n  Expected: \"" + logs + "\"\n  Actual:   \"" + actual + "\"");
            }

            return new ScenarioResult(1, name, true, "Boundary validation and role separation verified.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "renderResourceExhaustionAlert (Template Parameter Binding)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            SreModelContracts.AlertDetails alert = new SreModelContracts.AlertDetails(
                    "checkout-service", 92, 4294967296L, 2.45, List.of("503_BACKEND", "504_TIMEOUT"));
            service.renderResourceExhaustionAlert(alert);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(2, name, false, "No UserMessage generated.");
            }

            String userText = prompt.getUserMessage().getText();
            if (userText.contains("{serviceName}") || userText.contains("{cpuPercent}") ||
                userText.contains("{memoryBytes}") || userText.contains("{p99LatencySec}") || userText.contains("{errorCodes}")) {
                return new ScenarioResult(2, name, false,
                        "Un-substituted template placeholders detected:\n  Actual: \"" + userText + "\"\n" +
                        "  Fix: Use .user(u -> u.text(RESOURCE_ALERT_TEMPLATE).params(Map.of(...)))");
            }

            if (!userText.contains("checkout-service") || !userText.contains("92%") ||
                !userText.contains("4294967296 bytes") || !userText.contains("2.45s") || !userText.contains("503_BACKEND")) {
                return new ScenarioResult(2, name, false,
                        "Alert template parameter values missing or corrupted:\n  Actual: \"" + userText + "\"");
            }

            return new ScenarioResult(2, name, true, "Resource alert template bound cleanly with numeric and collection types.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "triageWithEnvironmentPolicy (Conditional System Policy Injection)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            // Test 3A: Production
            service.triageWithEnvironmentPolicy(SreModelContracts.Environment.PRODUCTION, "CrashLoop on api-gateway");
            Prompt prodPrompt = fakeModel.getLastPrompt();
            if (prodPrompt == null || prodPrompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false, "SystemMessage is null for PRODUCTION tier.");
            }

            if (!SreModelContracts.PROD_POLICY.equals(prodPrompt.getSystemMessage().getText())) {
                return new ScenarioResult(3, name, false,
                        "PRODUCTION tier must inject PROD_POLICY:\n  Expected: \"" + SreModelContracts.PROD_POLICY +
                        "\"\n  Actual:   \"" + prodPrompt.getSystemMessage().getText() + "\"");
            }

            // Test 3B: Staging
            fakeModel.clear();
            service.triageWithEnvironmentPolicy(SreModelContracts.Environment.STAGING, "High latency in integration test");
            Prompt stagePrompt = fakeModel.getLastPrompt();
            if (stagePrompt == null || stagePrompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false, "SystemMessage is null for STAGING tier.");
            }

            if (!SreModelContracts.NON_PROD_POLICY.equals(stagePrompt.getSystemMessage().getText())) {
                return new ScenarioResult(3, name, false,
                        "STAGING tier must inject NON_PROD_POLICY:\n  Expected: \"" + SreModelContracts.NON_PROD_POLICY +
                        "\"\n  Actual:   \"" + stagePrompt.getSystemMessage().getText() + "\"");
            }

            return new ScenarioResult(3, name, true, "Conditional environment policies cleanly injected based on tier.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "executeIncidentDiagnosisWithAudit (TigerStyle Defensive Token Auditing)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            fakeModel.setUsage(220, 85);
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            // 1. Boundary check: null input throws exception
            try {
                service.executeIncidentDiagnosisWithAudit(null);
                return new ScenarioResult(4, name, false, "Fail-fast invariant failed: null incidentReport must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 2. Normal execution check
            SreModelContracts.SreAudit auditNormal = service.executeIncidentDiagnosisWithAudit("Cluster network partition");
            if (auditNormal.promptTokens() != 220L || auditNormal.completionTokens() != 85L) {
                return new ScenarioResult(4, name, false,
                        "Token usage mismatch on normal call: expected 220/85, got " +
                        auditNormal.promptTokens() + "/" + auditNormal.completionTokens());
            }

            // 3. Defensive unboxing test: null usage metadata must NOT throw NullPointerException!
            fakeModel.clear();
            fakeModel.setNullUsage(true);
            SreModelContracts.SreAudit auditNullUsage = service.executeIncidentDiagnosisWithAudit("Node heartbeat missing");
            if (auditNullUsage.promptTokens() != 0L || auditNullUsage.completionTokens() != 0L) {
                return new ScenarioResult(4, name, false,
                        "Defensive usage check failed: when usage is null, tokens must default to 0L.");
            }

            return new ScenarioResult(4, name, true, "Defensive token extraction verified; zero unboxing NPEs on null usage.");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception during token audit: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "deriveCanaryAnalysisClient (Client Mutation with Template)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            // 1. Boundary check: negative or > 100 traffic must throw
            try {
                service.deriveCanaryAnalysisClient(-5.0);
                return new ScenarioResult(5, name, false, "Fail-fast invariant failed: negative traffic percent must throw exception.");
            } catch (IllegalArgumentException ignored) {}

            // 2. Mutation check
            ChatClient canaryClient = service.deriveCanaryAnalysisClient(15.5);
            if (canaryClient == null) return new ScenarioResult(5, name, false, "deriveCanaryAnalysisClient returned null.");

            fakeModel.clear();
            canaryClient.prompt().user("Analyze canary metrics").call().content();
            Prompt canaryPrompt = fakeModel.getLastPrompt();

            if (canaryPrompt == null || canaryPrompt.getSystemMessage() == null) {
                return new ScenarioResult(5, name, false, "Canary client has null system prompt.");
            }

            String sysText = canaryPrompt.getSystemMessage().getText();
            if (!sysText.contains("traffic share of 15.5%")) {
                return new ScenarioResult(5, name, false,
                        "Canary system prompt missing parameterized traffic share:\n  Actual: \"" + sysText + "\"");
            }

            Double temp = canaryPrompt.getOptions() != null ? canaryPrompt.getOptions().getTemperature() : null;
            if (temp == null || Math.abs(temp - 0.1) > 0.001) {
                return new ScenarioResult(5, name, false, "Canary client temperature mismatch (expected 0.1, got " + temp + ")");
            }

            return new ScenarioResult(5, name, true, "Derived canary client mutated cleanly with parameterized template.");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "generateDeterministicRunbook (Fine-Grained ChatOptions)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            service.generateDeterministicRunbook("PostgresDiskPressureThreshold");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(6, name, false, "No ChatOptions attached to runbook generation call.");
            }

            Double temp = prompt.getOptions().getTemperature();
            Integer maxTokens = prompt.getOptions().getMaxTokens();
            Double freqPenalty = prompt.getOptions().getFrequencyPenalty();
            List<String> stopSeqs = prompt.getOptions().getStopSequences();

            if (temp == null || Math.abs(temp - SreModelContracts.DETERMINISTIC_TEMPERATURE) > 0.001) {
                return new ScenarioResult(6, name, false, "Temperature mismatch: expected 0.0, got " + temp);
            }

            if (maxTokens == null || maxTokens != SreModelContracts.RUNBOOK_MAX_TOKENS) {
                return new ScenarioResult(6, name, false, "maxTokens mismatch: expected 300, got " + maxTokens);
            }

            if (freqPenalty == null || Math.abs(freqPenalty - SreModelContracts.RUNBOOK_FREQUENCY_PENALTY) > 0.001) {
                return new ScenarioResult(6, name, false, "frequencyPenalty mismatch: expected 0.5, got " + freqPenalty);
            }

            if (stopSeqs == null || !stopSeqs.containsAll(SreModelContracts.RUNBOOK_STOP_SEQUENCES)) {
                return new ScenarioResult(6, name, false,
                        "stopSequences mismatch: expected [END_RUNBOOK, ---], got " + stopSeqs);
            }

            return new ScenarioResult(6, name, true, "Fine-grained ChatOptions verified (temp=0.0, maxTokens=300, stopSeqs, freqPenalty).");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "classifyRemediationAction (Few-Shot In-Context Priming)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            String symptom = "Node status NotReady due to kubelet network plugin failure";
            service.classifyRemediationAction(symptom);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(7, name, false, "No prompt dispatched.");

            List<Message> nonSystem = prompt.getInstructions().stream()
                    .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                    .toList();

            // 3 pairs (6 messages) + 1 target query = 7 conversation messages
            if (nonSystem.size() < 7) {
                return new ScenarioResult(7, name, false,
                        "Few-shot remediation examples not primed! Found " + nonSystem.size() + " messages, expected 7.\n" +
                        "  Fix: Build alternating UserMessage/AssistantMessage pairs from REMEDIATION_FEW_SHOTS.");
            }

            boolean alternating = true;
            for (int i = 0; i < 6; i += 2) {
                if (nonSystem.get(i).getMessageType() != MessageType.USER ||
                    nonSystem.get(i + 1).getMessageType() != MessageType.ASSISTANT) {
                    alternating = false;
                    break;
                }
            }

            if (!alternating) {
                return new ScenarioResult(7, name, false, "Few-shot examples must alternate between USER and ASSISTANT roles.");
            }

            Message target = nonSystem.get(nonSystem.size() - 1);
            if (target.getMessageType() != MessageType.USER || !symptom.equals(target.getText())) {
                return new ScenarioResult(7, name, false, "Target symptom query not placed as final UserMessage.");
            }

            return new ScenarioResult(7, name, true, "Few-shot remediation action priming correctly structured.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "continueTroubleshootingSession (History Replay)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            // 1. Boundary check: null history must throw
            try {
                service.continueTroubleshootingSession(null, "reply");
                return new ScenarioResult(8, name, false, "Fail-fast invariant failed: null history must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            List<Message> history = List.of(
                    new UserMessage("Why is the Redis primary pod throttling commands?"),
                    new AssistantMessage("Check the maxmemory setting and eviction policy in configmap."),
                    new UserMessage("Eviction policy is volatile-lru, memory is 98% full.")
            );
            String reply = "Running redis-cli memory purge now.";
            service.continueTroubleshootingSession(history, reply);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(8, name, false, "No prompt dispatched.");

            List<Message> nonSystem = prompt.getInstructions().stream()
                    .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                    .toList();

            if (nonSystem.size() < 4) {
                return new ScenarioResult(8, name, false,
                        "History messages were dropped! Found " + nonSystem.size() + " messages, expected at least 4.\n" +
                        "  Fix: Use .messages(history).user(userReply)");
            }

            if (!nonSystem.get(0).getText().contains("Redis primary pod throttling") ||
                !nonSystem.get(1).getText().contains("maxmemory setting") ||
                !nonSystem.get(2).getText().contains("Eviction policy is volatile-lru") ||
                !nonSystem.get(3).getText().contains("redis-cli memory purge now")) {
                return new ScenarioResult(8, name, false, "History message content was corrupted or lost.");
            }

            return new ScenarioResult(8, name, true, "Troubleshooting session history cleanly replayed.");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "applyIncidentPagingEnvelope (System Envelope Augmentation)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            String engineer = "devops-pager-duty-primary@corp";
            String query = "Production cluster ingress returning 502 Bad Gateway";
            service.applyIncidentPagingEnvelope(engineer, query);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(9, name, false, "SystemMessage is null!");
            }

            String sysText = prompt.getSystemMessage().getText();
            if (!sysText.startsWith("ON-CALL DISPATCH TO: devops-pager-duty-primary@corp") ||
                !sysText.contains(SreModelContracts.BASE_SRE_SYSTEM_PROMPT)) {
                return new ScenarioResult(9, name, false,
                        "System prompt does not contain expected paging header and base prompt:\n  Actual: \"" + sysText + "\"");
            }

            String userText = prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "";
            if (!query.equals(userText)) {
                return new ScenarioResult(9, name, false,
                        "User query corrupted by paging header:\n  Expected: \"" + query + "\"\n  Actual:   \"" + userText + "\"");
            }

            return new ScenarioResult(9, name, true, "System paging envelope augmented and parameterized cleanly.");
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "safeExecuteDiagnosis (Fail-Fast Structural Invariant Enforcement)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(builder);

            // 1. Boundary check: null query throws exception
            try {
                service.safeExecuteDiagnosis(null);
                return new ScenarioResult(10, name, false, "Fail-fast invariant failed: null query must throw exception.");
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            // 2. Normal execution returns trimmed string
            fakeModel.clear();
            fakeModel.enqueue("  Root cause: OOM on JVM container heap  ");
            String result = service.safeExecuteDiagnosis("Analyze dump");
            if (!"Root cause: OOM on JVM container heap".equals(result)) {
                return new ScenarioResult(10, name, false,
                        "Normal response was not returned trimmed: expected 'Root cause: OOM on JVM container heap', got '" + result + "'");
            }

            // 3. Structural invariant check: Empty model response MUST throw IllegalStateException!
            fakeModel.clear();
            fakeModel.setEmptyResponse(true);
            try {
                service.safeExecuteDiagnosis("Analyze failure");
                return new ScenarioResult(10, name, false,
                        "Fail-fast invariant failed: Empty LLM response MUST throw IllegalStateException!");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains("SRE diagnosis failed: empty or malformed LLM response")) {
                    return new ScenarioResult(10, name, false,
                            "Exception message mismatch: expected to contain 'SRE diagnosis failed: empty or malformed LLM response', got: " +
                            expected.getMessage());
                }
            }

            return new ScenarioResult(10, name, true, "Fail-fast response invariant verified (empty response throws descriptive exception).");
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("\n======================================================================");
        System.out.println("                 PHASE 01 EXERCISE 03: VERIFICATION REPORT             ");
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
