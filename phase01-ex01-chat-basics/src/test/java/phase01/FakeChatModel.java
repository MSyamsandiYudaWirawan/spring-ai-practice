package phase01;

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
 * Deterministic offline mock implementing Spring AI's ChatModel interface.
 * Implements the external boundary seam (D2 pattern from step9-agent-loop-spec).
 * Captures all incoming Prompts for assertion by Verifier without making network calls.
 *
 * DO NOT MODIFY THIS FILE.
 */
public class FakeChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();

    public FakeChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
        return this;
    }

    public List<Prompt> getCapturedPrompts() {
        return Collections.unmodifiableList(capturedPrompts);
    }

    public Prompt getLastPrompt() {
        if (capturedPrompts.isEmpty()) {
            return null;
        }
        return capturedPrompts.get(capturedPrompts.size() - 1);
    }

    public void clear() {
        capturedPrompts.clear();
        scriptedResponses.clear();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);
        String text = scriptedResponses.isEmpty() ? "STUB_HYPOTHESIS_OK" : scriptedResponses.poll();
        DefaultUsage usage = new DefaultUsage(42, 18);
        ChatResponseMetadata meta = ChatResponseMetadata.builder().usage(usage).build();
        Generation gen = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(gen), meta);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not used in this exercise");
    }
}
