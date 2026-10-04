package phase08.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 04: Token Budget Post-Call Circuit-Breaker Advisor.
 * <p>
 * Accumulates prompt and completion token usage across all turns, tripping
 * an execution circuit-breaker if the budget ceiling is breached.
 */
public class TokenBudgetAdvisor implements CallAdvisor, Ordered {

    private final int tokenLimit;
    private final AtomicInteger accumulatedTokens = new AtomicInteger(0);

    /**
     * Initializes the token budget advisor with an execution ceiling.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If tokenLimit <= 0, throw IllegalArgumentException("tokenLimit must be positive").
     */
    public TokenBudgetAdvisor(int tokenLimit) {
        if (tokenLimit <= 0) {
            throw new IllegalArgumentException("tokenLimit must be positive");
        }
        this.tokenLimit = tokenLimit;
    }

    /**
     * Advises call, extracts token usage, and asserts budget compliance.
     * <p>
     * Instructions:
     * - Delegate to chain.nextCall(request) to get ChatClientResponse.
     * - Extract Usage metadata from response:
     *   - Check response.chatResponse() and response.chatResponse().getMetadata().
     *   - Extract promptTokens and completionTokens with defensive null checks (default to 0).
     *   - Add sum (promptTokens + completionTokens) to accumulatedTokens.
     * - Check Circuit-Breaker:
     *   - If accumulatedTokens.get() > tokenLimit, throw IllegalStateException:
     *     "Token budget circuit breaker tripped: " + accumulatedTokens.get() + " exceeds limit " + tokenLimit
     * - Return response.
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        throw new UnsupportedOperationException("TODO: Implement adviseCall");
    }

    public int getAccumulatedTokens() {
        return accumulatedTokens.get();
    }

    public void reset() {
        accumulatedTokens.set(0);
    }

    @Override
    public int getOrder() {
        return 20; // Inner guardrail layer
    }

    @Override
    public String getName() {
        return "TokenBudgetAdvisor";
    }
}
