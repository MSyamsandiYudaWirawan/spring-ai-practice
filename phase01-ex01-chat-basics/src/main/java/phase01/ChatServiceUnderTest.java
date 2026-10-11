package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * Service under test — Phase 01 Exercise 01: ChatClient Fluent API & Foundations.
 * <p>
 * Practice mode: Contains intentional defects across all 15 drills (0/15 passing).
 * Follow the tiered progressive hints in Verifier output to implement each gate.
 */
public class ChatServiceUnderTest {

    private final ChatClient chatClient;

    public ChatServiceUnderTest(ChatClient.Builder clientBuilder) {
        // DEFECT (Drills 7, 8 & 9): Missing null-check on clientBuilder, and does not configure defaultSystem or defaultOptions.
        this.chatClient = clientBuilder != null ? clientBuilder.build() : null;
    }

    /**
     * Drills 1 & 2: Role separation and fail-fast boundary validation.
     */
    public String askWithSystemInstructions(String systemRole, String userQuery) {
        // DEFECT (Drills 1 & 2): Concatenates system into user query and lacks boundary validation.
        return chatClient.prompt()
                .user("System: " + systemRole + "\nUser: " + userQuery)
                .call()
                .content();
    }

    /**
     * Drill 3: Parameterized system prompt specification.
     */
    public String askWithSystemAndContext(String systemRole, String env, String userQuery) {
        // DEFECT (Drill 3): Returns null without configuring parameterized SystemSpec.
        return null;
    }

    /**
     * Drills 4 & 5: Diagnosis template parameter binding with fail-fast invariants.
     */
    public String renderDiagnosisPrompt(ModelContracts.DiagnosisContext ctx) {
        // DEFECT (Drills 4 & 5): Dispatches raw un-substituted template and lacks null-check on ctx.
        return chatClient.prompt()
                .user(ModelContracts.DIAGNOSIS_PROMPT_TEMPLATE)
                .call()
                .content();
    }

    /**
     * Drill 6: Dynamic parameter map binding.
     */
    public String renderCustomTemplate(String template, Map<String, Object> params) {
        // DEFECT (Drill 6): Returns null without binding map parameters.
        return null;
    }

    /**
     * Drills 7 & 8: Inherit builder defaults (defaultSystem and defaultOptions).
     */
    public String generateReportWithDefaults(String analysisSnippet) {
        return chatClient.prompt()
                .user(analysisSnippet)
                .call()
                .content();
    }

    /**
     * Drills 10 & 11: Call-level temperature override with range boundary check.
     */
    public String askWithCustomTemperature(String userQuery, double temperature) {
        // DEFECT (Drills 10 & 11): Returns null without setting call-level options or checking temperature bounds.
        return null;
    }

    /**
     * Drill 12: Call-level maxTokens override with positive bounds validation.
     */
    public String askWithMaxTokens(String userQuery, int maxTokens) {
        // DEFECT (Drill 12): Returns null without setting maxTokens or validating bounds.
        return null;
    }

    /**
     * Drills 13, 14 & 15: Multi-turn message history replay with boundary invariants.
     */
    public String askMultiTurnConversation(List<Message> history, String newUserQuery) {
        // DEFECT (Drills 13, 14 & 15): Returns null without chaining message history.
        return null;
    }
}
