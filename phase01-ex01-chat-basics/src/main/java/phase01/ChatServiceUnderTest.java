package phase01;

import org.springframework.ai.chat.client.ChatClient;

import java.util.Objects;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 *
 * Seeded with defects across 3 scenarios:
 * - Scenario 1: Jams system instructions into user prompt instead of using .system(...)
 * - Scenario 2: Leaves template placeholders un-substituted instead of binding via .user(u -> u.text(...).param(...))
 * - Scenario 3: Fails to configure ChatClient.Builder with defaultSystem and defaultOptions
 */
public class ChatServiceUnderTest {

    private final ChatClient chatClient;

    public ChatServiceUnderTest(ChatClient.Builder clientBuilder) {
        // DEFECT (Scenario 3): Does not configure defaultSystem or defaultOptions.
        Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
        this.chatClient = clientBuilder.build();
    }

    /**
     * Scenario 1: Role separation.
     * The system role must be passed via .system(...) and user query via .user(...).
     */
    public String askWithSystemInstructions(String systemRole, String userQuery) {
        // DEFECT (Scenario 1): Raw string concatenation into user message.
        // getSystemMessage() will be null, and prompt injection can override instructions.
        return chatClient.prompt()
                .user("System: " + systemRole + "\nUser: " + userQuery)
                .call()
                .content();
    }

    /**
     * Scenario 2: Template parameter binding.
     * Must use ModelContracts.DIAGNOSIS_PROMPT_TEMPLATE and bind:
     * - "targetApp"   -> ctx.targetApp()
     * - "baselineP95" -> ctx.baselineP95Ms()
     * - "topFrame"    -> ctx.jfrTopFrame()
     * using the fluent .user(u -> u.text(...).param(...)) API.
     */
    public String renderDiagnosisPrompt(ModelContracts.DiagnosisContext ctx) {
        // DEFECT (Scenario 2): Sends the raw template string without binding parameters.
        // The LLM receives un-substituted placeholders: {targetApp}, {baselineP95}, {topFrame}.
        return chatClient.prompt()
                .user(ModelContracts.DIAGNOSIS_PROMPT_TEMPLATE)
                .call()
                .content();
    }

    /**
     * Scenario 3: Builder default inheritance.
     * Sends the analysis snippet as the user query. The ChatClient must automatically
     * attach the defaultSystem prompt and defaultOptions configured during construction.
     */
    public String generateReportWithDefaults(String analysisSnippet) {
        return chatClient.prompt()
                .user(analysisSnippet)
                .call()
                .content();
    }
}
