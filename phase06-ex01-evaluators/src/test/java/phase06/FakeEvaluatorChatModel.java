package phase06;

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
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 06.
 * Provides scripted responses and captures evaluation prompts without external API dependencies.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeEvaluatorChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Integer promptTokens = 50;
    private Integer completionTokens = 20;

    public FakeEvaluatorChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response, "response must not be null"));
        return this;
    }

    public FakeEvaluatorChatModel setTokens(Integer in, Integer out) {
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
            String contents = prompt.getContents();
            if (contents.toLowerCase().contains("relevan") || contents.toLowerCase().contains("either yes or no")) {
                text = "YES";
            } else if (contents.toLowerCase().contains("claim is supported")) {
                text = "YES";
            } else if (contents.toLowerCase().contains("pass or fail")) {
                text = "PASS\nReason: The response meets all acceptance criteria.";
            } else {
                text = "SCORE: 4/5\nREASON: Good answer with solid factual backing.";
            }
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming evaluation not used in Phase 06");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
