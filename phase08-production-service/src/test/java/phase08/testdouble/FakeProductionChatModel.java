package phase08.testdouble;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 05: Deterministic Offline Test Double Seam.
 * 100% offline, zero-token-cost ChatModel simulation.
 */
public class FakeProductionChatModel implements ChatModel {

    private final List<Prompt> recordedPrompts = new ArrayList<>();
    private final AtomicInteger invocationCount = new AtomicInteger(0);
    private volatile boolean failOnCall = false;
    private volatile boolean emitMalformedJsonOnce = false;

    public void setFailOnCall(boolean fail) {
        this.failOnCall = fail;
    }

    public void setEmitMalformedJsonOnce(boolean malformed) {
        this.emitMalformedJsonOnce = malformed;
    }

    public List<Prompt> getRecordedPrompts() {
        return new ArrayList<>(recordedPrompts);
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        invocationCount.incrementAndGet();
        recordedPrompts.add(prompt);

        if (failOnCall) {
            throw new RuntimeException("Upstream LLM Provider Outage: 503 Service Unavailable");
        }

        String contents = prompt.getContents() != null ? prompt.getContents() : "";

        if (contents.contains("health-check-ping")) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("pong"))));
        }

        if (emitMalformedJsonOnce) {
            emitMalformedJsonOnce = false;
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Thinking: Here is the fix: { action: unquoted_val, bad_json }"))));
        }

        // Return realistic structured JSON triage proposal
        String jsonPayload = """
                ```json
                {
                  "action": "SCALE_HIKARI_POOL",
                  "hypothesis": "HikariCP pool exhaustion caused thread starvation and HTTP 503 errors",
                  "targetFile": "application.properties",
                  "patchContent": "spring.datasource.hikari.maximum-pool-size=50",
                  "confidence": 0.95,
                  "rationale": "JFR logs confirm HikariCP acquisition wait was >4000ms"
                }
                ```
                """;

        Usage testUsage = new org.springframework.ai.chat.metadata.DefaultUsage(120, 65, 185);

        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .usage(testUsage)
                .build();

        return new ChatResponse(List.of(new Generation(new AssistantMessage(jsonPayload))), metadata);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
