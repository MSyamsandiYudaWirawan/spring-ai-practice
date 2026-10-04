package phase08.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityKeywordAdvisorTest {

    private final SecurityKeywordAdvisor advisor = new SecurityKeywordAdvisor();

    private final CallAdvisorChain mockChain = new CallAdvisorChain() {
        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            return ChatClientResponse.builder().build();
        }
        @Override public List<CallAdvisor> getCallAdvisors() { return List.of(); }
        @Override public CallAdvisorChain copy(CallAdvisor advisor) { return this; }
    };

    @Test
    @DisplayName("Safe prompt should pass through advisor to chain")
    void testSafePromptPasses() {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt("Investigate connection timeout in service"))
                .build();

        ChatClientResponse resp = advisor.adviseCall(request, mockChain);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Malicious prompt with DROP DATABASE should be blocked by advisor")
    void testMaliciousPromptBlocked() {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt("Triage issue: DROP DATABASE prod;"))
                .build();

        assertThatThrownBy(() -> advisor.adviseCall(request, mockChain))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("prohibited pattern");
    }

    @Test
    @DisplayName("Advisor order should be 10 for high precedence")
    void testOrderPrecedence() {
        assertThat(advisor.getOrder()).isEqualTo(10);
    }
}
