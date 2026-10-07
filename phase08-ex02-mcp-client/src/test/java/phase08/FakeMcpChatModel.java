package phase08;

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
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 08 Exercise 02.
 * Captures outgoing prompts and returns deterministic responses.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeMcpChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Function<Prompt, String> dynamicResponder;
    private int promptTokens = 120;
    private int completionTokens = 40;

    public FakeMcpChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response));
        return this;
    }

    public FakeMcpChatModel setDynamicResponder(Function<Prompt, String> responder) {
        this.dynamicResponder = responder;
        return this;
    }

    public FakeMcpChatModel setTokenUsage(int promptTokens, int completionTokens) {
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
        dynamicResponder = null;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        capturedPrompts.add(prompt);

        String text;
        if (dynamicResponder != null) {
            text = dynamicResponder.apply(prompt);
        } else {
            text = scriptedResponses.poll();
            if (text == null) {
                text = "Autonomous agent resolution with MCP tools completed.";
            }
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming not required for Phase 08 Exercise 02");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
