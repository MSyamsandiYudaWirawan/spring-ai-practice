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
 * Deterministic in-memory Spring AI ChatModel test double for Phase 05 Exercise 02.
 * Records outgoing Prompt objects and serves queued responses without external network access.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeAdvancedChatModel implements ChatModel {

    private final Deque<String> responseQueue = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private long promptTokens = 100;
    private long completionTokens = 50;

    public FakeAdvancedChatModel enqueue(String response) {
        responseQueue.addLast(response);
        return this;
    }

    public FakeAdvancedChatModel setTokens(long promptTokens, long completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        return this;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);
        String text = responseQueue.isEmpty() ? "{\"status\":\"ok\"}" : responseQueue.pollFirst();
        Generation generation = new Generation(new AssistantMessage(text));
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .usage(new DefaultUsage((int) promptTokens, (int) completionTokens))
                .build();
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming tested on StreamingChatPort level in Phase 05");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }

    public int getCallCount() {
        return capturedPrompts.size();
    }

    public Prompt getLastPrompt() {
        return capturedPrompts.isEmpty() ? null : capturedPrompts.get(capturedPrompts.size() - 1);
    }

    public List<Prompt> getCapturedPrompts() {
        return Collections.unmodifiableList(capturedPrompts);
    }

    public void clear() {
        capturedPrompts.clear();
        responseQueue.clear();
    }
}
