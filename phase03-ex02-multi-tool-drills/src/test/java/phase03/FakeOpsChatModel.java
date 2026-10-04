package phase03;

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
 * Deterministic offline mock implementing Spring AI's ChatModel interface for Phase 03 Exercise 02.
 * Captures all incoming Prompts and records tool execution interactions.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeOpsChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Integer promptTokens = 140;
    private Integer completionTokens = 95;

    public FakeOpsChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
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
            text = "Automated remediation plan executed successfully.";
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not required for Phase 03 Exercise 02");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
