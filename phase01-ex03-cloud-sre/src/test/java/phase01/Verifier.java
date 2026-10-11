package phase01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 01 Exercise 03: Cloud SRE & Kubernetes Operations.
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
        // Topic 1: Role Separation, Cluster Invariants & Paging Envelopes (Drills 1 - 3)
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Parameterized Templates & Conditional Environment Policies (Drills 4 - 6)
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Fine-Grained Options & Deterministic Runbooks (Drills 7 - 9)
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Conversational Troubleshooting & Few-Shot Remediations (Drills 10 - 12)
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Client Mutation, Token Auditing & Structural Invariants (Drills 13 - 15)
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Role Separation, Cluster Invariants & Paging Envelopes (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "analyzePodCrash (Boundary Validation and Semantic Role Separation)";
        List<String> hints = List.of(
                "Set system prompt with cluster context: 'Cluster: ' + clusterId + ' | Namespace: ' + namespace.",
                "Set user prompt with pod logs: `.user(podLogs)`.",
                "Do NOT concatenate cluster context into the user prompt."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            String cluster = "us-east-k8s-prod-1";
            String namespace = "payments";
            String logs = "FATAL 2026-09-30 08:10:12 connection pool exhausted";
            service.analyzePodCrash(cluster, namespace, logs);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(1, name, false, "No prompt dispatched.", hints);

            String expectedSystem = "Cluster: " + cluster + " | Namespace: " + namespace;
            if (prompt.getSystemMessage() == null || !expectedSystem.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "SystemMessage mismatch: expected '" + expectedSystem + "', got '" + (prompt.getSystemMessage() != null ? prompt.getSystemMessage().getText() : "null") + "'", hints);
            }
            if (prompt.getUserMessage() == null || !logs.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "UserMessage mismatch: expected '" + logs + "', got '" + (prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "null") + "'", hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "analyzePodCrash (Fail-Fast Cluster Boundaries)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation: clusterId, namespace, podLogs must not be null or blank.",
                "Throw IllegalArgumentException when any parameter is null or blank.",
                "Check `if (clusterId == null || clusterId.isBlank() || namespace == null || namespace.isBlank() || podLogs == null || podLogs.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.analyzePodCrash(null, "payments", "logs");
                return new ScenarioResult(2, name, false, "null clusterId must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.analyzePodCrash("cluster-1", "   ", "logs");
                return new ScenarioResult(2, name, false, "blank namespace must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.analyzePodCrash("cluster-1", "payments", null);
                return new ScenarioResult(2, name, false, "null podLogs must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "applyIncidentPagingEnvelope (Incident Paging Dynamic System Envelope)";
        List<String> hints = List.of(
                "Augment system prompt with on-call paging dispatcher info: 'ON-CALL DISPATCH TO: ' + onCallEngineer + '\\n' + BASE_SRE_SYSTEM_PROMPT.",
                "User message must equal incidentQuery without dispatcher prefix leakage.",
                "Throw IllegalArgumentException if onCallEngineer or incidentQuery is null or blank."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            String engineer = "devops-oncall-primary";
            String query = "P1 Alert: Cassandra latency degraded";
            service.applyIncidentPagingEnvelope(engineer, query);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false, "Prompt or SystemMessage is null.", hints);
            }
            String sys = prompt.getSystemMessage().getText();
            if (!sys.contains("ON-CALL DISPATCH TO: " + engineer) || !sys.contains(SreModelContracts.BASE_SRE_SYSTEM_PROMPT)) {
                return new ScenarioResult(3, name, false, "System envelope missing dispatcher info or base prompt: " + sys, hints);
            }
            if (prompt.getUserMessage() == null || !query.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(3, name, false, "User query corrupted: " + prompt.getUserMessage(), hints);
            }

            // Boundary validation
            try {
                service.applyIncidentPagingEnvelope(null, query);
                return new ScenarioResult(3, name, false, "null onCallEngineer must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Parameterized Templates & Conditional Environment Policies (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "renderResourceExhaustionAlert (Resource Alert Parameter Map Binding)";
        List<String> hints = List.of(
                "Use `.user(u -> u.text(RESOURCE_ALERT_TEMPLATE).params(Map.of(...)))` to bind alert parameters.",
                "Placeholders required: serviceName, cpuPercent, memoryBytes, p99LatencySec, errorCodes.",
                "Verify prompt does not contain `{serviceName}` or other placeholder tokens."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            SreModelContracts.AlertDetails alert = new SreModelContracts.AlertDetails(
                    "checkout-service", 92, 4294967296L, 2.45, List.of("503_BACKEND", "504_TIMEOUT"));
            service.renderResourceExhaustionAlert(alert);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(4, name, false, "No UserMessage generated.", hints);
            }
            String userText = prompt.getUserMessage().getText();
            if (userText.contains("{serviceName}") || userText.contains("{cpuPercent}") ||
                userText.contains("{memoryBytes}") || userText.contains("{p99LatencySec}") || userText.contains("{errorCodes}")) {
                return new ScenarioResult(4, name, false, "Un-substituted template placeholders detected: " + userText, hints);
            }
            if (!userText.contains("checkout-service") || !userText.contains("92%") ||
                !userText.contains("4294967296 bytes") || !userText.contains("2.45s") || !userText.contains("503_BACKEND")) {
                return new ScenarioResult(4, name, false, "Alert parameter values missing or corrupted: " + userText, hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "renderResourceExhaustionAlert (Fail-Fast Null Alert Invariant)";
        List<String> hints = List.of(
                "Fail fast when input domain record is null.",
                "Throw NullPointerException or IllegalArgumentException if alert is null.",
                "Use `Objects.requireNonNull(alert, 'alert must not be null');`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.renderResourceExhaustionAlert(null);
                return new ScenarioResult(5, name, false, "null alert must throw NullPointerException or IllegalArgumentException", hints);
            } catch (NullPointerException | IllegalArgumentException ignored) {}

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "triageWithEnvironmentPolicy (Conditional Environment Policy Injection)";
        List<String> hints = List.of(
                "If env == PRODUCTION: system prompt must equal PROD_POLICY.",
                "Else (STAGING, DEVELOPMENT): system prompt must equal NON_PROD_POLICY.",
                "Throw IllegalArgumentException or NullPointerException if env or anomalyDescription is null or blank."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            // Production check
            service.triageWithEnvironmentPolicy(SreModelContracts.Environment.PRODUCTION, "Database connection timeout");
            Prompt prodPrompt = fakeModel.getLastPrompt();
            if (prodPrompt == null || prodPrompt.getSystemMessage() == null ||
                !SreModelContracts.PROD_POLICY.equals(prodPrompt.getSystemMessage().getText())) {
                return new ScenarioResult(6, name, false, "Production environment policy not injected into SystemMessage.", hints);
            }

            // Non-Production check
            service.triageWithEnvironmentPolicy(SreModelContracts.Environment.STAGING, "Database connection timeout");
            Prompt stagPrompt = fakeModel.getLastPrompt();
            if (stagPrompt == null || stagPrompt.getSystemMessage() == null ||
                !SreModelContracts.NON_PROD_POLICY.equals(stagPrompt.getSystemMessage().getText())) {
                return new ScenarioResult(6, name, false, "Non-production environment policy not injected into SystemMessage.", hints);
            }

            // Boundary validation
            try {
                service.triageWithEnvironmentPolicy(null, "Query");
                return new ScenarioResult(6, name, false, "null env must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Fine-Grained Options & Deterministic Runbooks (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "generateDeterministicRunbook (Fine-Grained ChatOptions Attachment)";
        List<String> hints = List.of(
                "Configure fine-grained call-level options using ChatOptions.builder().",
                "Set temperature(0.0), maxTokens(300), stopSequences(['END_RUNBOOK', '---']), and frequencyPenalty(0.5).",
                "Attach to prompt via `.options(ChatOptions.builder().temperature(...)...)`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            service.generateDeterministicRunbook("HighMemoryUsageAlert");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(7, name, false, "No ChatOptions attached to runbook generation call.", hints);
            }
            ChatOptions options = prompt.getOptions();
            if (options.getTemperature() == null || Math.abs(options.getTemperature() - SreModelContracts.DETERMINISTIC_TEMPERATURE) > 0.001) {
                return new ScenarioResult(7, name, false, "Temperature mismatch: expected 0.0, got: " + options.getTemperature(), hints);
            }
            if (options.getMaxTokens() == null || options.getMaxTokens() != SreModelContracts.RUNBOOK_MAX_TOKENS) {
                return new ScenarioResult(7, name, false, "maxTokens mismatch: expected 300, got: " + options.getMaxTokens(), hints);
            }
            if (options.getStopSequences() == null || !options.getStopSequences().equals(SreModelContracts.RUNBOOK_STOP_SEQUENCES)) {
                return new ScenarioResult(7, name, false, "stopSequences mismatch: " + options.getStopSequences(), hints);
            }
            if (options.getFrequencyPenalty() == null || Math.abs(options.getFrequencyPenalty() - SreModelContracts.RUNBOOK_FREQUENCY_PENALTY) > 0.001) {
                return new ScenarioResult(7, name, false, "frequencyPenalty mismatch: expected 0.5, got: " + options.getFrequencyPenalty(), hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "generateDeterministicRunbook (Base Client Immutability Check)";
        List<String> hints = List.of(
                "Call-level option overrides must not mutate the shared ChatClient instance.",
                "Subsequent calls using safeExecuteDiagnosis must inherit the base temperature (0.4) and topP (0.9).",
                "Verify immutability is preserved."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            // Call with override
            service.generateDeterministicRunbook("Alert");

            // Subsequent call
            service.safeExecuteDiagnosis("General diagnosis");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(8, name, false, "Options missing from subsequent call.", hints);
            }
            Double temp = prompt.getOptions().getTemperature();
            if (temp == null || Math.abs(temp - SreModelContracts.BASE_TEMPERATURE) > 0.001) {
                return new ScenarioResult(8, name, false, "Subsequent call did not retain base temperature 0.4: got " + temp, hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "generateDeterministicRunbook (Fail-Fast Boundary Validation)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation on alertName.",
                "Throw IllegalArgumentException if alertName is null or blank.",
                "Check `if (alertName == null || alertName.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.generateDeterministicRunbook(null);
                return new ScenarioResult(9, name, false, "null alertName must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.generateDeterministicRunbook("   ");
                return new ScenarioResult(9, name, false, "blank alertName must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Conversational Troubleshooting & Few-Shot Remediations (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "continueTroubleshootingSession (Multi-Turn History Replay)";
        List<String> hints = List.of(
                "Supply conversation history via `.messages(history).user(userReply)`.",
                "Preserve order of previous messages in prompt instructions.",
                "Append userReply as trailing turn."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            List<Message> history = List.of(
                    new UserMessage("Worker node ip-10-0-12-88 is reporting Ready=False"),
                    new AssistantMessage("Kubelet stopped posting status. Checking journalctl logs."),
                    new UserMessage("Logs show OOM-killer killed dockerd daemon")
            );
            String reply = "Should we cordon and restart docker daemon?";
            service.continueTroubleshootingSession(history, reply);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(10, name, false, "No prompt dispatched.", hints);

            List<Message> instructions = prompt.getInstructions();
            if (instructions.size() < 4) {
                return new ScenarioResult(10, name, false, "Prompt expected at least 4 messages (3 history + 1 reply), got: " + instructions.size(), hints);
            }
            if (!reply.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(10, name, false, "User reply not found at end of session: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "continueTroubleshootingSession (History Boundary Validation)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation on troubleshooting session inputs.",
                "Throw IllegalArgumentException if history is null, or if userReply is null or blank.",
                "Check `if (history == null || userReply == null || userReply.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.continueTroubleshootingSession(null, "Valid reply");
                return new ScenarioResult(11, name, false, "null history must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.continueTroubleshootingSession(List.of(), null);
                return new ScenarioResult(11, name, false, "null userReply must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.continueTroubleshootingSession(List.of(), "   ");
                return new ScenarioResult(11, name, false, "blank userReply must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "classifyRemediationAction (Few-Shot Remediations Priming)";
        List<String> hints = List.of(
                "Prepend REMEDIATION_FEW_SHOTS as alternating UserMessage(pair.symptom()) and AssistantMessage(pair.action()) pairs.",
                "Append failureSymptom as the trailing user message.",
                "Verify prompt instructions contains few-shot pairs."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            String symptom = "Worker node disk space critically low at 98%";
            service.classifyRemediationAction(symptom);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(12, name, false, "No prompt dispatched.", hints);

            List<Message> instructions = prompt.getInstructions();
            if (instructions.size() < 7) {
                return new ScenarioResult(12, name, false,
                        "Few-shot priming missing! Expected at least 7 messages (6 few-shot pairs + 1 symptom), got: " + instructions.size(), hints);
            }
            boolean hasAssistant = instructions.stream().anyMatch(m -> m.getMessageType() == MessageType.ASSISTANT);
            if (!hasAssistant) {
                return new ScenarioResult(12, name, false, "Few-shot priming missing AssistantMessage replies.", hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Client Mutation, Token Auditing & Structural Invariants (Drills 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "deriveCanaryAnalysisClient (Canary Client Mutation via .mutate())";
        List<String> hints = List.of(
                "Derive canary client using `chatClient.mutate()` with CANARY_SYSTEM_TEMPLATE.",
                "Enforce bounds: canaryTrafficPercent must be in [0.0, 100.0] or throw IllegalArgumentException.",
                "Configure `.defaultSystem(s -> s.text(...).param(\"trafficPercent\", canaryTrafficPercent)).defaultOptions(...)`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            ChatClient canaryClient = service.deriveCanaryAnalysisClient(10.0);
            if (canaryClient == null) {
                return new ScenarioResult(13, name, false, "deriveCanaryAnalysisClient returned null.", hints);
            }

            canaryClient.prompt().user("Canary monitoring check").call().content();
            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(13, name, false, "Mutated canary client missing SystemMessage.", hints);
            }
            if (!prompt.getSystemMessage().getText().contains("traffic share of 10.0%")) {
                return new ScenarioResult(13, name, false, "Canary system template missing bound traffic percent: " + prompt.getSystemMessage().getText(), hints);
            }
            if (prompt.getOptions() == null || Math.abs(prompt.getOptions().getTemperature() - 0.1) > 0.001) {
                return new ScenarioResult(13, name, false, "Canary client temperature mismatch: expected 0.1, got: " + (prompt.getOptions() != null ? prompt.getOptions().getTemperature() : "null"), hints);
            }

            // Boundary checks
            try {
                service.deriveCanaryAnalysisClient(-5.0);
                return new ScenarioResult(13, name, false, "Negative canary traffic percent must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                service.deriveCanaryAnalysisClient(105.0);
                return new ScenarioResult(13, name, false, "Canary traffic percent > 100 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "executeIncidentDiagnosisWithAudit (Token Usage Audit Metadata Extraction)";
        List<String> hints = List.of(
                "Call `.chatResponse()` on the request spec to retrieve ChatResponse metadata.",
                "Defensively handle null usage metadata by defaulting token counts to 0L.",
                "Return `new SreAudit(content, promptTokens, completionTokens)`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            fakeModel.setUsage(240, 95);
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            SreModelContracts.SreAudit audit = service.executeIncidentDiagnosisWithAudit("Investigate pod OOMKilled events");
            if (audit == null) {
                return new ScenarioResult(14, name, false, "executeIncidentDiagnosisWithAudit returned null.", hints);
            }
            if (audit.promptTokens() != 240L || audit.completionTokens() != 95L) {
                return new ScenarioResult(14, name, false, "Token counts mismatch: expected 240/95, got: " + audit, hints);
            }

            // Test defensive null usage handling
            fakeModel.setNullUsage(true);
            SreModelContracts.SreAudit nullAudit = service.executeIncidentDiagnosisWithAudit("Investigate pod OOMKilled events");
            if (nullAudit.promptTokens() != 0L || nullAudit.completionTokens() != 0L) {
                return new ScenarioResult(14, name, false, "Defensive null usage must default to 0 tokens: got " + nullAudit, hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "safeExecuteDiagnosis (Fail-Fast Structural Response Validation)";
        List<String> hints = List.of(
                "Call `.chatResponse()`, inspect output text for non-blank content.",
                "Throw IllegalStateException if response has no generations, or output text is null or blank.",
                "Throw IllegalArgumentException if query is null or blank."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            SreOpsServiceUnderTest service = new SreOpsServiceUnderTest(ChatClient.builder(fakeModel));

            // Normal success
            String result = service.safeExecuteDiagnosis("Query pod status");
            if (result == null || result.isBlank()) {
                return new ScenarioResult(15, name, false, "safeExecuteDiagnosis returned empty result on valid call.", hints);
            }

            // Simulating empty response from model
            fakeModel.setEmptyResponse(true);
            try {
                service.safeExecuteDiagnosis("Query pod status");
                return new ScenarioResult(15, name, false, "Empty response from ChatModel must throw IllegalStateException", hints);
            } catch (IllegalStateException ignored) {}

            // Input boundary validation
            fakeModel.setEmptyResponse(false);
            try {
                service.safeExecuteDiagnosis(null);
                return new ScenarioResult(15, name, false, "null query must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.safeExecuteDiagnosis("   ");
                return new ScenarioResult(15, name, false, "blank query must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

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
        System.out.println("  PHASE 01 EXERCISE 03: CLOUD SRE & KUBERNETES OPERATIONS REPORT");
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
