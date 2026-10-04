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
    private Integer promptTokens = 180;
    private Integer completionTokens = 65;
    private boolean simulateNullUsage = false;
    private boolean simulateEmptyResponse = false;

    public FakeChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
        return this;
    }

    public FakeChatModel setUsage(Integer promptTokens, Integer completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.simulateNullUsage = false;
        return this;
    }

    public FakeChatModel setNullUsage(boolean nullUsage) {
        this.simulateNullUsage = nullUsage;
        return this;
    }

    public FakeChatModel setEmptyResponse(boolean emptyResponse) {
        this.simulateEmptyResponse = emptyResponse;
        return this;
    }

    public List<Prompt> getCapturedPrompts() {
        return Collections.unmodifiableList(capturedPrompts);
    }

    public Prompt getLastPrompt() {
        if (capturedPrompts.isEmpty()) return null;
        return capturedPrompts.get(capturedPrompts.size() - 1);
    }

    public void clear() {
        capturedPrompts.clear();
        scriptedResponses.clear();
        simulateNullUsage = false;
        simulateEmptyResponse = false;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);

        if (simulateEmptyResponse) {
            return new ChatResponse(List.of(), null);
        }

        String text = scriptedResponses.isEmpty() ? "STUB_SRE_INVESTIGATION_PASS" : scriptedResponses.poll();
        ChatResponseMetadata meta = null;
        if (!simulateNullUsage) {
            DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
            meta = ChatResponseMetadata.builder().usage(usage).build();
        }

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
