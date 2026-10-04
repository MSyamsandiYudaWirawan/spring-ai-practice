package phase05;

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
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 05.
 * Used exclusively for testing the SpringAiChatPort adapter seam.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeTestDoubleChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Integer promptTokens = 45;
    private Integer completionTokens = 25;

    public FakeTestDoubleChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
        return this;
    }

    public FakeTestDoubleChatModel setTokens(Integer in, Integer out) {
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
            text = "Default adapter response";
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not tested on ChatModel level in Phase 05");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
