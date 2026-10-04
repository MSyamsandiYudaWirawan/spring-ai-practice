package phase04;

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
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 04.
 * Simulates LLM execution, token usage reporting, simulated latency, and response queuing.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeAdvisorChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Integer promptTokens = 120;
    private Integer completionTokens = 80;
    private long artificialDelayMs = 0L;

    public FakeAdvisorChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
        return this;
    }

    public FakeAdvisorChatModel setTokenUsage(Integer promptTokens, Integer completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        return this;
    }

    public FakeAdvisorChatModel setArtificialDelayMs(long delayMs) {
        this.artificialDelayMs = delayMs;
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
        artificialDelayMs = 0L;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);

        if (artificialDelayMs > 0) {
            try {
                Thread.sleep(artificialDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String text = scriptedResponses.poll();
        if (text == null) {
            text = "Advisory guardrail check completed successfully.";
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not required for Phase 04");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
