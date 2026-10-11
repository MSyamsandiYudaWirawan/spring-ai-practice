package phase01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 01 Exercise 01: ChatClient Fluent API & Foundations.
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
        // Topic 1: Role Separation & System Instructions (Drills 1 - 3)
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Prompt Templates & Fluent Parameter Binding (Drills 4 - 6)
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: ChatClient Builder Defaults & Configuration (Drills 7 - 9)
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Dynamic Option Overrides at Call-Time (Drills 10 - 12)
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Multi-Message Turn History & Conversational Invariants (Drills 13 - 15)
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Role Separation & System Instructions (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "askWithSystemInstructions (Role Separation Direct Match)";
        List<String> hints = List.of(
                "In LLM chat protocols, System and User messages have distinct semantic roles.",
                "Do NOT concatenate system instructions into the user query string.",
                "Use .system(systemRole).user(userQuery) on chatClient.prompt()."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            ChatServiceUnderTest service = new ChatServiceUnderTest(builder);

            String systemRole = "You are a database diagnosis specialist.";
            String userQuery = "Explain why connection pool exhaustion causes high p99 latency.";
            service.askWithSystemInstructions(systemRole, userQuery);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) {
                return new ScenarioResult(1, name, false, "No prompt was dispatched to ChatModel.", hints);
            }
            if (prompt.getSystemMessage() == null) {
                return new ScenarioResult(1, name, false, "SystemMessage is null! Use .system(systemRole).", hints);
            }
            if (!systemRole.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "SystemMessage mismatch: expected '" + systemRole + "', got '" + prompt.getSystemMessage().getText() + "'", hints);
            }
            if (prompt.getUserMessage() == null || !userQuery.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "UserMessage mismatch: expected '" + userQuery + "', got '" + (prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "null") + "'", hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "askWithSystemInstructions (Fail-Fast Boundary Invariants)";
        List<String> hints = List.of(
                "Enforce fail-fast input validation before dispatching to the LLM.",
                "Throw IllegalArgumentException when systemRole or userQuery is null or blank.",
                "Check `if (systemRole == null || systemRole.isBlank() || userQuery == null || userQuery.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.askWithSystemInstructions(null, "Valid query");
                return new ScenarioResult(2, name, false, "null systemRole must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.askWithSystemInstructions("Valid role", null);
                return new ScenarioResult(2, name, false, "null userQuery must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.askWithSystemInstructions("   ", "Valid query");
                return new ScenarioResult(2, name, false, "blank systemRole must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.askWithSystemInstructions("Valid role", "   ");
                return new ScenarioResult(2, name, false, "blank userQuery must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "askWithSystemAndContext (Parameterized System Role Specification)";
        List<String> hints = List.of(
                "System prompts can also be parameterized dynamically using the fluent SystemSpec.",
                "Use `.system(s -> s.text(\"Role: {role} | Env: {env}\").param(\"role\", systemRole).param(\"env\", env))`.",
                "Enforce non-blank checks on systemRole, env, and userQuery."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            service.askWithSystemAndContext("SRE Lead", "production-us-east-1", "Inspect failing pods");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false, "Prompt or SystemMessage is null.", hints);
            }
            String sysText = prompt.getSystemMessage().getText();
            if (!sysText.contains("SRE Lead") || !sysText.contains("production-us-east-1")) {
                return new ScenarioResult(3, name, false,
                        "System message missing parameterized values. Got: '" + sysText + "'", hints);
            }
            if (prompt.getUserMessage() == null || !"Inspect failing pods".equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(3, name, false, "UserMessage mismatch: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Prompt Templates & Fluent Parameter Binding (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "renderDiagnosisPrompt (Standard Diagnosis Template Binding)";
        List<String> hints = List.of(
                "Spring AI provides template substitution via .user(u -> u.text(...).param(...)).",
                "Do NOT send the raw un-substituted template string {targetApp}, {baselineP95}, {topFrame}.",
                "Bind 'targetApp' -> ctx.targetApp(), 'baselineP95' -> ctx.baselineP95Ms(), 'topFrame' -> ctx.jfrTopFrame()."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            ModelContracts.DiagnosisContext ctx = new ModelContracts.DiagnosisContext(
                    "payment-gateway", 450, "org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun");
            service.renderDiagnosisPrompt(ctx);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(4, name, false, "UserMessage is null.", hints);
            }
            String userText = prompt.getUserMessage().getText();
            if (userText.contains("{targetApp}") || userText.contains("{baselineP95}") || userText.contains("{topFrame}")) {
                return new ScenarioResult(4, name, false, "Prompt contains un-substituted template placeholders: " + userText, hints);
            }
            if (!userText.contains("payment-gateway") || !userText.contains("450") || !userText.contains("SocketProcessor.doRun")) {
                return new ScenarioResult(4, name, false, "Prompt missing bound parameter values: " + userText, hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "renderDiagnosisPrompt (Fail-Fast Boundary Invariants)";
        List<String> hints = List.of(
                "Fail fast when input domain record is null.",
                "Throw IllegalArgumentException or NullPointerException when ctx is null.",
                "Objects.requireNonNull(ctx, 'ctx must not be null');"
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.renderDiagnosisPrompt(null);
                return new ScenarioResult(5, name, false, "null ctx must throw IllegalArgumentException or NullPointerException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "renderCustomTemplate (Arbitrary Parameter Map Binding)";
        List<String> hints = List.of(
                "Support dynamic parameter maps using `.user(u -> u.text(template).params(paramsMap))`.",
                "Enforce fail-fast boundary checks: template must not be null/blank, params must not be null.",
                "Return client call content with all parameters bound cleanly."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            String template = "Service {service} breached latency threshold of {threshold}ms";
            Map<String, Object> params = Map.of("service", "order-api", "threshold", 500);
            service.renderCustomTemplate(template, params);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(6, name, false, "UserMessage is null.", hints);
            }
            String userText = prompt.getUserMessage().getText();
            if (userText.contains("{service}") || userText.contains("{threshold}")) {
                return new ScenarioResult(6, name, false, "Prompt contains un-substituted template placeholders: " + userText, hints);
            }
            if (!userText.contains("order-api") || !userText.contains("500")) {
                return new ScenarioResult(6, name, false, "Prompt missing substituted map values: " + userText, hints);
            }

            // Boundary validation
            try {
                service.renderCustomTemplate(null, params);
                return new ScenarioResult(6, name, false, "null template must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.renderCustomTemplate(template, null);
                return new ScenarioResult(6, name, false, "null params must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: ChatClient Builder Defaults & Configuration (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "generateReportWithDefaults (Default System Prompt Attachment)";
        List<String> hints = List.of(
                "Configure default system prompt on ChatClient.Builder in constructor.",
                "Use `builder.defaultSystem(ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT)`.",
                "All prompt calls automatically inherit the default system prompt."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            service.generateReportWithDefaults("Thread dump indicates lock contention on synchronized block");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(7, name, false, "Prompt is missing inherited default system message.", hints);
            }
            if (!ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(7, name, false,
                        "System message does not match EXPECTED_DEFAULT_SYSTEM_PROMPT: " + prompt.getSystemMessage().getText(), hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "generateReportWithDefaults (Default Options Temperature & TopP Attachment)";
        List<String> hints = List.of(
                "Pass ChatOptions.Builder to `builder.defaultOptions(ChatOptions.builder().temperature(...).topP(...))` in constructor.",
                "In Spring AI 2.0.1, defaultOptions takes `ChatOptions.Builder<?>` (do not call .build()).",
                "Verify prompt options contain temperature 0.2 and topP 0.9."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            service.generateReportWithDefaults("Heap memory usage at 92%");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(8, name, false, "ChatOptions missing from dispatched prompt.", hints);
            }
            ChatOptions options = prompt.getOptions();
            if (options.getTemperature() == null || Math.abs(options.getTemperature() - ModelContracts.EXPECTED_DEFAULT_TEMPERATURE) > 0.001) {
                return new ScenarioResult(8, name, false,
                        "Default temperature mismatch. Expected: " + ModelContracts.EXPECTED_DEFAULT_TEMPERATURE + ", got: " + options.getTemperature(), hints);
            }
            if (options.getTopP() == null || Math.abs(options.getTopP() - ModelContracts.EXPECTED_DEFAULT_TOP_P) > 0.001) {
                return new ScenarioResult(8, name, false,
                        "Default topP mismatch. Expected: " + ModelContracts.EXPECTED_DEFAULT_TOP_P + ", got: " + options.getTopP(), hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "ChatServiceUnderTest Constructor (Null Builder Fail-Fast Validation)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation in constructor.",
                "Throw NullPointerException or IllegalArgumentException if clientBuilder is null.",
                "Use `Objects.requireNonNull(clientBuilder, 'clientBuilder must not be null');`."
        );
        try {
            try {
                new ChatServiceUnderTest(null);
                return new ScenarioResult(9, name, false, "new ChatServiceUnderTest(null) must throw NullPointerException", hints);
            } catch (NullPointerException | IllegalArgumentException ignored) {}

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Dynamic Option Overrides at Call-Time (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "askWithCustomTemperature (Call-Level Temperature Override)";
        List<String> hints = List.of(
                "Override default options for a specific turn using `.options(ChatOptions.builder().temperature(temperature))`.",
                "Ensure call-level options take precedence over builder defaults.",
                "Verify prompt options contain the requested custom temperature."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            service.askWithCustomTemperature("Propose creative optimization hypotheses", 0.7);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(10, name, false, "No options attached to prompt.", hints);
            }
            Double temp = prompt.getOptions().getTemperature();
            if (temp == null || Math.abs(temp - 0.7) > 0.001) {
                return new ScenarioResult(10, name, false, "Temperature override mismatch. Expected 0.7, got: " + temp, hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "askWithCustomTemperature (Temperature Range Boundary Validation)";
        List<String> hints = List.of(
                "Validate temperature range: LLM temperature must be between 0.0 and 2.0 inclusive.",
                "Throw IllegalArgumentException if temperature < 0.0 or temperature > 2.0.",
                "Check `if (temperature < 0.0 || temperature > 2.0) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.askWithCustomTemperature("Query", -0.1);
                return new ScenarioResult(11, name, false, "Negative temperature must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                service.askWithCustomTemperature("Query", 2.1);
                return new ScenarioResult(11, name, false, "Temperature > 2.0 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "askWithMaxTokens (Call-Level MaxTokens Override & Validation)";
        List<String> hints = List.of(
                "Set max tokens limit using `.options(ChatOptions.builder().maxTokens(maxTokens))`.",
                "Validate that maxTokens > 0; throw IllegalArgumentException if non-positive.",
                "Verify prompt options reflect maxTokens == 250."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            service.askWithMaxTokens("Generate concise 2-sentence summary", 250);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(12, name, false, "No options attached to prompt.", hints);
            }
            Integer tokens = prompt.getOptions().getMaxTokens();
            if (tokens == null || tokens != 250) {
                return new ScenarioResult(12, name, false, "maxTokens mismatch. Expected 250, got: " + tokens, hints);
            }

            // Boundary validation
            try {
                service.askWithMaxTokens("Query", 0);
                return new ScenarioResult(12, name, false, "maxTokens <= 0 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                service.askWithMaxTokens("Query", -50);
                return new ScenarioResult(12, name, false, "negative maxTokens must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Multi-Message Turn History & Conversational Invariants (Drills 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "askMultiTurnConversation (History Replay Sequencing)";
        List<String> hints = List.of(
                "Spring AI allows conversational history injection via `.messages(history).user(newUserQuery)`.",
                "Ensure prior conversation messages (UserMessage, AssistantMessage) precede the new user prompt.",
                "Verify prompt instructions contains all history messages in order followed by newUserQuery."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            List<Message> history = List.of(
                    new UserMessage("What is the top contention lock?"),
                    new AssistantMessage("The top lock is JavaMonitorEnter on URLClassLoader.")
            );
            String nextQuery = "What is the recommended remediation?";
            service.askMultiTurnConversation(history, nextQuery);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) {
                return new ScenarioResult(13, name, false, "No prompt dispatched.", hints);
            }
            List<Message> instructions = prompt.getInstructions();
            if (instructions.size() < 3) {
                return new ScenarioResult(13, name, false, "Prompt expected at least 3 messages (2 history + 1 user), got: " + instructions.size(), hints);
            }
            if (!"What is the recommended remediation?".equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(13, name, false, "User query not found in last user message: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "askMultiTurnConversation (Empty History Graceful Handling)";
        List<String> hints = List.of(
                "When history is empty (`List.of()`), the request spec should process the new user query seamlessly.",
                "Ensure no NullPointerException or out-of-bounds error occurs when history is empty.",
                "Verify user message in prompt matches the newUserQuery."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            String query = "Single turn query without history";
            service.askMultiTurnConversation(List.of(), query);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(14, name, false, "Prompt or UserMessage is null.", hints);
            }
            if (!query.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(14, name, false, "UserMessage mismatch: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "askMultiTurnConversation (Boundary Checks on History & Query)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation on conversational history inputs.",
                "Throw IllegalArgumentException if history is null, or if newUserQuery is null or blank.",
                "Check `if (history == null || newUserQuery == null || newUserQuery.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatServiceUnderTest service = new ChatServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.askMultiTurnConversation(null, "Valid query");
                return new ScenarioResult(15, name, false, "null history must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.askMultiTurnConversation(List.of(), null);
                return new ScenarioResult(15, name, false, "null newUserQuery must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.askMultiTurnConversation(List.of(), "   ");
                return new ScenarioResult(15, name, false, "blank newUserQuery must throw IllegalArgumentException", hints);
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
        System.out.println("  PHASE 01 EXERCISE 01: CHAT CLIENT BASICS REPORT");
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
