# Phase 01 Exercise 01 — Golden Reference Solution
## ChatClient Fluent API & Foundations (15 Drills)

```java
package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service under test for Phase 01 Exercise 01: ChatClient Fluent API & Foundations (15 Drills).
 */
public class ChatServiceUnderTest {

    private final ChatClient chatClient;

    public ChatServiceUnderTest(ChatClient.Builder clientBuilder) {
        Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
        this.chatClient = clientBuilder
                .defaultSystem(ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT)
                .defaultOptions(ChatOptions.builder()
                        .temperature(ModelContracts.EXPECTED_DEFAULT_TEMPERATURE)
                        .topP(ModelContracts.EXPECTED_DEFAULT_TOP_P))
                .build();
    }

    /**
     * Drills 1 & 2: Role separation and fail-fast boundary validation.
     */
    public String askWithSystemInstructions(String systemRole, String userQuery) {
        if (systemRole == null || systemRole.isBlank()) {
            throw new IllegalArgumentException("systemRole must not be null or blank");
        }
        if (userQuery == null || userQuery.isBlank()) {
            throw new IllegalArgumentException("userQuery must not be null or blank");
        }
        return chatClient.prompt()
                .system(systemRole)
                .user(userQuery)
                .call()
                .content();
    }

    /**
     * Drill 3: Parameterized system prompt specification.
     */
    public String askWithSystemAndContext(String systemRole, String env, String userQuery) {
        if (systemRole == null || systemRole.isBlank() || env == null || env.isBlank() || userQuery == null || userQuery.isBlank()) {
            throw new IllegalArgumentException("Parameters must not be null or blank");
        }
        return chatClient.prompt()
                .system(s -> s.text("Role: {role} | Env: {env}").param("role", systemRole).param("env", env))
                .user(userQuery)
                .call()
                .content();
    }

    /**
     * Drills 4 & 5: Diagnosis template parameter binding with fail-fast invariants.
     */
    public String renderDiagnosisPrompt(ModelContracts.DiagnosisContext ctx) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        return chatClient.prompt()
                .user(u -> u.text(ModelContracts.DIAGNOSIS_PROMPT_TEMPLATE)
                        .param("targetApp", ctx.targetApp())
                        .param("baselineP95", ctx.baselineP95Ms())
                        .param("topFrame", ctx.jfrTopFrame()))
                .call()
                .content();
    }

    /**
     * Drill 6: Dynamic parameter map binding.
     */
    public String renderCustomTemplate(String template, Map<String, Object> params) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("template must not be null or blank");
        }
        if (params == null) {
            throw new IllegalArgumentException("params must not be null");
        }
        return chatClient.prompt()
                .user(u -> u.text(template).params(params))
                .call()
                .content();
    }

    /**
     * Drills 7 & 8: Inherit builder defaults (defaultSystem and defaultOptions).
     */
    public String generateReportWithDefaults(String analysisSnippet) {
        if (analysisSnippet == null || analysisSnippet.isBlank()) {
            throw new IllegalArgumentException("analysisSnippet must not be null or blank");
        }
        return chatClient.prompt()
                .user(analysisSnippet)
                .call()
                .content();
    }

    /**
     * Drills 10 & 11: Call-level temperature override with range boundary check.
     */
    public String askWithCustomTemperature(String userQuery, double temperature) {
        if (userQuery == null || userQuery.isBlank()) {
            throw new IllegalArgumentException("userQuery must not be null or blank");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
        return chatClient.prompt()
                .user(userQuery)
                .options(ChatOptions.builder().temperature(temperature))
                .call()
                .content();
    }

    /**
     * Drill 12: Call-level maxTokens override with positive bounds validation.
     */
    public String askWithMaxTokens(String userQuery, int maxTokens) {
        if (userQuery == null || userQuery.isBlank()) {
            throw new IllegalArgumentException("userQuery must not be null or blank");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be > 0");
        }
        return chatClient.prompt()
                .user(userQuery)
                .options(ChatOptions.builder().maxTokens(maxTokens))
                .call()
                .content();
    }

    /**
     * Drills 13, 14 & 15: Multi-turn message history replay with boundary invariants.
     */
    public String askMultiTurnConversation(List<Message> history, String newUserQuery) {
        if (history == null) {
            throw new IllegalArgumentException("history must not be null");
        }
        if (newUserQuery == null || newUserQuery.isBlank()) {
            throw new IllegalArgumentException("newUserQuery must not be null or blank");
        }
        return chatClient.prompt()
                .messages(history)
                .user(newUserQuery)
                .call()
                .content();
    }
}
```
