package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Objects;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 *
 * Seeded with defects across 8 scenarios in FinTech Fraud & Risk Operations:
 * - Scenario 1: Role separation in transaction analysis.
 * - Scenario 2: Parameterized dispute template with Map parameters.
 * - Scenario 3: Parameterized system prompt specification.
 * - Scenario 4: Call-level ChatOptions overrides for high-risk transfers.
 * - Scenario 5: Multi-turn history replay preserving Message types.
 * - Scenario 6: Few-shot in-context learning priming with AssistantMessage.
 * - Scenario 7: ChatClient mutation via .mutate() for expedited operations.
 * - Scenario 8: Token usage audit extraction from ChatResponse metadata.
 */
public class FraudOpsServiceUnderTest {

    private final ChatClient chatClient;

    public FraudOpsServiceUnderTest(ChatClient.Builder clientBuilder) {
        Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
        this.chatClient = clientBuilder
                .defaultSystem(FraudModelContracts.BASE_SYSTEM_PROMPT)
                .defaultOptions(ChatOptions.builder()
                        .temperature(FraudModelContracts.BASE_TEMPERATURE)
                        .topP(FraudModelContracts.BASE_TOP_P))
                .build();
    }

    /**
     * Scenario 1: Role separation.
     * System instructions must be set via .system(complianceRole), and transaction details via .user(...).
     */
    public String analyzeTransaction(String complianceRole, FraudModelContracts.Transaction tx) {
        // DEFECT (Scenario 1): Concatenates compliance role into user message.
        return chatClient.prompt()
                .user("Compliance: " + complianceRole + "\nTransaction: " + tx.toString())
                .call()
                .content();
    }

    /**
     * Scenario 2: Parameterized template using a Map.
     * Must use FraudModelContracts.DISPUTE_TEMPLATE with .user(u -> u.text(...).params(Map.of(...)))
     * Placeholders: "customerName", "disputeId", "amount", "currency", "reason"
     */
    public String draftDisputeNotice(FraudModelContracts.DisputeNotice dispute) {
        // DEFECT (Scenario 2): Sends raw template without parameter bindings.
        return chatClient.prompt()
                .user(FraudModelContracts.DISPUTE_TEMPLATE)
                .call()
                .content();
    }

    /**
     * Scenario 3: Parameterized system prompt specification.
     * Must use .system(s -> s.text(REGIONAL_AUDITOR_SYSTEM_TEMPLATE).param("jurisdiction", ...).param("standard", ...))
     * and .user(auditQuery).
     */
    public String configureRegionalAuditor(String jurisdiction, String standard, String auditQuery) {
        // DEFECT (Scenario 3): Uses static base system prompt and puts jurisdiction into user query.
        return chatClient.prompt()
                .user("Jurisdiction: " + jurisdiction + ", Standard: " + standard + "\nQuery: " + auditQuery)
                .call()
                .content();
    }

    /**
     * Scenario 4: Call-level options override.
     * Must override options on this specific prompt call:
     * - temperature: FraudModelContracts.OVERRIDE_TEMPERATURE (0.0)
     * - maxTokens:   FraudModelContracts.OVERRIDE_MAX_TOKENS (500)
     */
    public String auditHighRiskTransfer(String transferDetails) {
        // DEFECT (Scenario 4): Dispatches without overriding options, inheriting client's higher temperature (0.7).
        return chatClient.prompt()
                .user(transferDetails)
                .call()
                .content();
    }

    /**
     * Scenario 5: Multi-turn history replay.
     * Must supply previous conversation messages via .messages(history) and append latestCustomerMessage via .user(...).
     */
    public String continueDisputeDialogue(List<Message> history, String latestCustomerMessage) {
        // DEFECT (Scenario 5): Ignores history and only sends the latest message.
        return chatClient.prompt()
                .user(latestCustomerMessage)
                .call()
                .content();
    }

    /**
     * Scenario 6: Few-shot in-context learning priming.
     * Must prepend CLASSIFICATION_FEW_SHOTS as alternating UserMessage and AssistantMessage pairs,
     * followed by customerInquiry as the final user message.
     */
    public String classifyTransactionIntent(String customerInquiry) {
        // DEFECT (Scenario 6): Dispatches customer inquiry with zero examples.
        return chatClient.prompt()
                .user(customerInquiry)
                .call()
                .content();
    }

    /**
     * Scenario 7: ChatClient mutation via .mutate().
     * Must derive a new ChatClient by calling this.chatClient.mutate():
     * - defaultSystem: EXPEDITED_SYSTEM_PROMPT
     * - defaultOptions: temperature = 0.1
     * The original this.chatClient must remain unmutated.
     */
    public ChatClient deriveExpeditedClient() {
        // DEFECT (Scenario 7): Returns base chatClient instead of mutated instance.
        return this.chatClient;
    }

    /**
     * Scenario 8: Token usage audit extraction.
     * Must dispatch operationalPrompt and return AuditResult containing:
     * - output text content
     * - promptTokens from ChatResponse metadata Usage
     * - completionTokens from ChatResponse metadata Usage
     */
    public FraudModelContracts.AuditResult executeWithTokenAudit(String operationalPrompt) {
        // DEFECT (Scenario 8): Calls .content() directly, discarding metadata and returning 0 tokens.
        String content = chatClient.prompt()
                .user(operationalPrompt)
                .call()
                .content();
        return new FraudModelContracts.AuditResult(content, 0L, 0L);
    }
}
