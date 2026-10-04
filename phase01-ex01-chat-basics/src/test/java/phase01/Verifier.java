package phase01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 01 Exercise 01.
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {
    }

    public static void main(String[] args) {
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {
            }
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
        if (selected == 0 || selected == 1) {
            list.add(verifyScenario1());
        }
        if (selected == 0 || selected == 2) {
            list.add(verifyScenario2());
        }
        if (selected == 0 || selected == 3) {
            list.add(verifyScenario3());
        }
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "askWithSystemInstructions (Role Separation)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            ChatServiceUnderTest service = new ChatServiceUnderTest(builder);

            String systemRole = "You are a database diagnosis specialist.";
            String userQuery = "Explain why connection pool exhaustion causes high p99 latency.";
            service.askWithSystemInstructions(systemRole, userQuery);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) {
                return new ScenarioResult(1, name, false, "No prompt was dispatched to ChatModel.");
            }

            if (prompt.getSystemMessage() == null) {
                String userContent = prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "null";
                return new ScenarioResult(1, name, false,
                        "SystemMessage is null! Found system text concatenated inside UserMessage:\n" +
                                "  Actual UserMessage: \"" + userContent + "\"\n" +
                                "  Fix: Use .system(systemRole) and .user(userQuery) on the request spec.");
            }

            if (!systemRole.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "SystemMessage text mismatch:\n" +
                                "  Expected: \"" + systemRole + "\"\n" +
                                "  Actual:   \"" + prompt.getSystemMessage().getText() + "\"");
            }

            if (prompt.getUserMessage() == null || !userQuery.equals(prompt.getUserMessage().getText())) {
                String actualUser = prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "null";
                return new ScenarioResult(1, name, false,
                        "UserMessage text mismatch (may contain leaked 'System:' prefix):\n" +
                                "  Expected: \"" + userQuery + "\"\n" +
                                "  Actual:   \"" + actualUser + "\"");
            }

            List<Message> instructions = prompt.getInstructions();
            if (instructions.size() != 2) {
                return new ScenarioResult(1, name, false,
                        "Expected exactly 2 messages (SYSTEM, USER), but found " + instructions.size());
            }

            return new ScenarioResult(1, name, true, "SystemMessage and UserMessage cleanly separated.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception thrown during execution: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "renderDiagnosisPrompt (Template Parameter Binding)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            ChatServiceUnderTest service = new ChatServiceUnderTest(builder);

            // Test 2A: Benign target
            ModelContracts.DiagnosisContext ctxA = new ModelContracts.DiagnosisContext(
                    "petclinic", 240, "java.net.SocketInputStream.socketRead0");
            service.renderDiagnosisPrompt(ctxA);
            Prompt promptA = fakeModel.getLastPrompt();

            if (promptA == null || promptA.getUserMessage() == null) {
                return new ScenarioResult(2, name, false, "No UserMessage generated for ctxA.");
            }

            String textA = promptA.getUserMessage().getText();
            if (textA.contains("{targetApp}") || textA.contains("{baselineP95}") || textA.contains("{topFrame}")) {
                return new ScenarioResult(2, name, false,
                        "Un-substituted template placeholders detected in UserMessage:\n" +
                                "  Found: \"" + textA + "\"\n" +
                                "  Fix: Use .user(u -> u.text(TEMPLATE).param(\"targetApp\", ...)...)");
            }

            if (!textA.contains("petclinic") || !textA.contains("240ms") || !textA.contains("SocketInputStream")) {
                return new ScenarioResult(2, name, false,
                        "Rendered prompt missing expected parameter values:\n" +
                                "  Actual: \"" + textA + "\"");
            }

            // Test 2B: Signature with lambda and braces
            fakeModel.clear();
            ModelContracts.DiagnosisContext ctxB = new ModelContracts.DiagnosisContext(
                    "order-service", 580, "org.springframework.samples.OwnerRepo$$Lambda$842.apply({arg0})");
            service.renderDiagnosisPrompt(ctxB);
            Prompt promptB = fakeModel.getLastPrompt();

            String textB = promptB.getUserMessage().getText();
            if (!textB.contains("order-service") || !textB.contains("580ms") || !textB.contains("apply({arg0})")) {
                return new ScenarioResult(2, name, false,
                        "Special characters / braces corrupted during template rendering:\n" +
                                "  Actual: \"" + textB + "\"");
            }

            return new ScenarioResult(2, name, true, "Template rendered with parameters; special signatures preserved.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception thrown during execution: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "generateReportWithDefaults (Builder Default Configuration)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);

            // ChatServiceUnderTest constructor must configure defaultSystem and defaultOptions on builder!
            ChatServiceUnderTest service = new ChatServiceUnderTest(builder);

            String snippet = "JFR recording shows 85% CPU burn in HashMap.resize()";
            service.generateReportWithDefaults(snippet);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) {
                return new ScenarioResult(3, name, false, "No prompt was dispatched to ChatModel.");
            }

            // 1. Default system prompt check
            if (prompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false,
                        "Default system prompt was not configured on ChatClient.Builder!\n" +
                                "  Expected: \"" + ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT + "\"\n" +
                                "  Actual:   null\n" +
                                "  Fix: Call clientBuilder.defaultSystem(...) in constructor.");
            }

            if (!ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(3, name, false,
                        "Default system prompt mismatch:\n" +
                                "  Expected: \"" + ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT + "\"\n" +
                                "  Actual:   \"" + prompt.getSystemMessage().getText() + "\"");
            }

            // 2. Default options check (temperature=0.2, topP=0.9)
            if (prompt.getOptions() == null) {
                return new ScenarioResult(3, name, false,
                        "Default options were not configured on ChatClient.Builder!\n" +
                                "  Expected: ChatOptions with temp=" + ModelContracts.EXPECTED_DEFAULT_TEMPERATURE +
                                ", topP=" + ModelContracts.EXPECTED_DEFAULT_TOP_P + "\n" +
                                "  Fix: Call clientBuilder.defaultOptions(ChatOptions.builder()...) in constructor.");
            }

            Double temp = prompt.getOptions().getTemperature();
            if (temp == null || Math.abs(temp - ModelContracts.EXPECTED_DEFAULT_TEMPERATURE) > 0.001) {
                return new ScenarioResult(3, name, false,
                        "Default temperature mismatch:\n" +
                                "  Expected: " + ModelContracts.EXPECTED_DEFAULT_TEMPERATURE + "\n" +
                                "  Actual:   " + temp);
            }

            Double topP = prompt.getOptions().getTopP();
            if (topP == null || Math.abs(topP - ModelContracts.EXPECTED_DEFAULT_TOP_P) > 0.001) {
                return new ScenarioResult(3, name, false,
                        "Default topP mismatch:\n" +
                                "  Expected: " + ModelContracts.EXPECTED_DEFAULT_TOP_P + "\n" +
                                "  Actual:   " + topP);
            }

            return new ScenarioResult(3, name, true, "Builder default system prompt and options applied (temp=0.2, topP=0.9).");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception thrown during execution: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("\n======================================================================");
        System.out.println("                 PHASE 01 EXERCISE 01: VERIFICATION REPORT             ");
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
