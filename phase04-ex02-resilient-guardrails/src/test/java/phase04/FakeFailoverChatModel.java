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
import java.util.function.Function;

/**
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 04 Exercise 02.
 * Supports scripted responses, dynamic responders, configurable failure simulation (503/timeouts),
 * token usage tracking, and prompt inspection.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeFailoverChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private RuntimeException failureToThrow = null;
    private Function<Prompt, String> dynamicResponder = null;
    private Integer promptTokens = 50;
    private Integer completionTokens = 30;

    public FakeFailoverChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
        return this;
    }

    public FakeFailoverChatModel setDynamicResponder(Function<Prompt, String> responder) {
        this.dynamicResponder = responder;
        return this;
    }

    public FakeFailoverChatModel failWith(RuntimeException failure) {
        this.failureToThrow = failure;
        return this;
    }

    public FakeFailoverChatModel setTokenUsage(Integer promptTokens, Integer completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
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
        failureToThrow = null;
        dynamicResponder = null;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);

        if (failureToThrow != null) {
            RuntimeException ex = failureToThrow;
            failureToThrow = null; // one-shot failure unless re-set
            throw ex;
        }

        String text;
        if (dynamicResponder != null) {
            text = dynamicResponder.apply(prompt);
        } else {
            text = scriptedResponses.poll();
            if (text == null) {
                text = "{\"status\": \"SUCCESS\", \"message\": \"Default resilient model response\"}";
            }
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not required for Phase 04 Exercise 02");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
