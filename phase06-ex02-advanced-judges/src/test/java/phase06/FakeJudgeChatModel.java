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
 * Deterministic offline test double implementing Spring AI's ChatModel interface for Phase 06 Exercise 02.
 * Supports scripted FIFO responses, dynamic prompt routing, and prompt inspection.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeJudgeChatModel implements ChatModel {

    private final Deque<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new CopyOnWriteArrayList<>();
    private Integer promptTokens = 120;
    private Integer completionTokens = 45;

    public FakeJudgeChatModel enqueue(String response) {
        scriptedResponses.add(Objects.requireNonNull(response, "response must not be null"));
        return this;
    }

    public FakeJudgeChatModel setTokens(Integer in, Integer out) {
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
            String contents = prompt.getContents().toLowerCase();
            if (contents.contains("which option is better") || contents.contains("compare option 1")) {
                text = "OPTION_1\nReasoning: Option 1 is more comprehensive and safe.";
            } else if (contents.contains("consensus") || contents.contains("majority")) {
                text = "PASS\nScore: 0.90\nReason: High quality response.";
            } else if (contents.contains("extract atomic claims") || contents.contains("atomic factual claims")) {
                text = "1. Service p95 is 250ms.\n2. Connection pool size was doubled to 40.";
            } else if (contents.contains("entailed") || contents.contains("claim supported") || contents.contains("is this claim supported")) {
                text = "YES";
            } else if (contents.contains("which document supports")) {
                text = "DOC_0";
            } else if (contents.contains("confidential instructions") || contents.contains("system prompt")) {
                text = "LEAK_DETECTED: NO\nEXPLANATION: No confidential system instructions leaked.";
            } else if (contents.contains("adversarial") || contents.contains("harmful request")) {
                text = "REFUSED: YES\nLEAK: NO\nPREACHY: NO\nREASON: Standard polite security refusal.";
            } else if (contents.contains("rubric") || contents.contains("anchors")) {
                text = "Correctness: 4.5\nCompleteness: 4.0\nConciseness: 4.5\nFeedback: Strong grounding with minor brevity opportunities.";
            } else {
                text = "PASS\nScore: 1.0\nFeedback: Verified successfully.";
            }
        }

        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new UnsupportedOperationException("Streaming evaluation not used in Phase 06 Exercise 02");
    }

    @Override
    @Deprecated
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
