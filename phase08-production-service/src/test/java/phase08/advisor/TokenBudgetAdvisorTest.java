package phase08.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenBudgetAdvisorTest {

    private final TokenBudgetAdvisor budgetAdvisor = new TokenBudgetAdvisor(300);

    private CallAdvisorChain mockChainWithUsage(int promptTokens, int genTokens) {
        Usage usage = new org.springframework.ai.chat.metadata.DefaultUsage(promptTokens, genTokens, promptTokens + genTokens);
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("Response"))),
                ChatResponseMetadata.builder().usage(usage).build()
        );
        return new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest request) {
                return ChatClientResponse.builder().chatResponse(chatResponse).build();
            }
            @Override public List<CallAdvisor> getCallAdvisors() { return List.of(); }
            @Override public CallAdvisorChain copy(CallAdvisor advisor) { return this; }
        };
    }

    @Test
    @DisplayName("Should accumulate tokens and pass when within budget")
    void testWithinBudget() {
        ChatClientRequest request = ChatClientRequest.builder().prompt(new Prompt("ping")).build();
        budgetAdvisor.adviseCall(request, mockChainWithUsage(100, 50)); // 150 used

        assertThat(budgetAdvisor.getAccumulatedTokens()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should trip circuit-breaker when accumulated tokens exceed budget ceiling")
    void testExceedBudgetTrips() {
        ChatClientRequest request = ChatClientRequest.builder().prompt(new Prompt("ping")).build();
        budgetAdvisor.adviseCall(request, mockChainWithUsage(150, 100)); // 250 used

        // Next call brings total to 250 + 100 = 350 > 300
        assertThatThrownBy(() -> budgetAdvisor.adviseCall(request, mockChainWithUsage(60, 40)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token budget circuit breaker tripped");
    }
}
