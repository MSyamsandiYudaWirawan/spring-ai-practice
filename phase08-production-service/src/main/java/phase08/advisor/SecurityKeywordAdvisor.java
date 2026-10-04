package phase08.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 04: Security Keyword Pre-Call Guardrail Advisor.
 * <p>
 * Inspects outgoing prompts before they reach the model and blocks prompt injection attacks.
 */
@Component
public class SecurityKeywordAdvisor implements CallAdvisor, Ordered {

    public static final List<String> BLOCKED_KEYWORDS = List.of(
            "drop database",
            "rm -rf /",
            "delete from users",
            "exfiltrate",
            "system-prompt-leak"
    );

    /**
     * Intercepts ChatClient call to enforce prompt security policies.
     * <p>
     * Instructions:
     * - Extract prompt text from request.prompt().getContents() (safe null check, lowercase).
     * - Check for prohibited keywords:
     *   - Iterate through BLOCKED_KEYWORDS.
     *   - If prompt contains any blocked keyword, throw SecurityException:
     *     "Security guardrail violation: Prompt contains prohibited pattern: " + blocked
     * - If clean, proceed through chain:
     *   - return chain.nextCall(request).
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        throw new UnsupportedOperationException("TODO: Implement adviseCall");
    }

    /**
     * Advisor precedence in the onion chain.
     * <p>
     * Instructions:
     * - Return 10 (high precedence / outer layer).
     */
    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public String getName() {
        return "SecurityKeywordAdvisor";
    }
}
