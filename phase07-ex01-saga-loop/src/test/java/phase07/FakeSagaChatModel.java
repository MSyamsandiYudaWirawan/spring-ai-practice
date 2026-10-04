package phase07;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 07.
 * Provides scripted JSON proposals and records outgoing prompts.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeSagaChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Integer promptTokens = 150;
    private Integer completionTokens = 50;

    public FakeSagaChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response, "response must not be null"));
        return this;
    }

    public FakeSagaChatModel setTokens(Integer in, Integer out) {
        this.promptTokens = in;
        this.completionTokens = out;
        return this;
    }

    public List<Prompt> getCapturedPrompts() {
        return Collections.unmodifiableList(capturedPrompts);
    }

    public Prompt getLastPrompt() {
        if (capturedPrompts.isEmpty()) return null;
        return capturedPrompts.get(capturedPrompts.size() - 1);
    }

    public int getCallCount() {
        return capturedPrompts.size();
    }

    public void clear() {
        capturedPrompts.clear();
        scriptedResponses.clear();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);

        String text = scriptedResponses.poll();
        if (text == null) {
            text = """
                   {
                     "category": "INDEX_OPTIMIZATION",
                     "fileToModify": "src/main/resources/db/schema.sql",
                     "patchContent": "CREATE INDEX idx_orders_user ON orders(user_id);",
                     "rationale": "High query latency on user_id filter."
                   }
                   """;
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not used in Phase 07");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
