package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Objects;

/**
 * Service under test — Phase 01 Exercise 02: Advanced ChatClient & FinTech Operations.
 * <p>
 * Practice mode: Contains intentional defects across all 15 drills (0/15 passing).
 * Follow the tiered progressive hints in Verifier output to implement each gate.
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
     * Drills 1 & 2: Role separation and fail-fast boundary invariants.
     */
    public String analyzeTransaction(String complianceRole, FraudModelContracts.Transaction tx) {
        // DEFECT (Drills 1 & 2): Concatenates role into user prompt without fail-fast validation.
        return chatClient.prompt()
                .user("Compliance: " + complianceRole + "\nTransaction: " + (tx != null ? tx.toString() : "null"))
                .call()
                .content();
    }

    /**
     * Drill 3: Parameterized system prompt specification.
     */
    public String configureRegionalAuditor(String jurisdiction, String standard, String auditQuery) {
        // DEFECT (Drill 3): Returns null without configuring parameterized SystemSpec.
        return null;
    }

    /**
     * Drills 4, 5 & 6: Parameterized template using a Map with special character preservation.
     */
    public String draftDisputeNotice(FraudModelContracts.DisputeNotice dispute) {
        // DEFECT (Drills 4, 5 & 6): Dispatches raw un-substituted template and lacks null-check.
        return chatClient.prompt()
                .user(FraudModelContracts.DISPUTE_TEMPLATE)
                .call()
                .content();
    }

    /**
     * Drills 7, 8 & 9: Call-level options override with fail-fast validation.
     */
    public String auditHighRiskTransfer(String transferDetails) {
        // DEFECT (Drills 7, 8 & 9): Returns null without configuring call-level options override.
        return null;
    }

    /**
     * Drills 10 & 11: Multi-turn history replay with boundary validation.
     */
    public String continueDisputeDialogue(List<Message> history, String latestCustomerMessage) {
        // DEFECT (Drills 10 & 11): Returns null without chaining message history.
        return null;
    }

    /**
     * Drill 12: Few-shot in-context learning priming.
     */
    public String classifyTransactionIntent(String customerInquiry) {
        // DEFECT (Drill 12): Returns null without prepending few-shot example pairs.
        return null;
    }

    /**
     * Drill 13: ChatClient mutation via .mutate().
     */
    public ChatClient deriveExpeditedClient() {
        // DEFECT (Drill 13): Returns null without deriving mutated ChatClient.
        return null;
    }

    /**
     * Drills 14 & 15: Token usage audit extraction from ChatResponse metadata.
     */
    public FraudModelContracts.AuditResult executeWithTokenAudit(String operationalPrompt) {
        // DEFECT (Drills 14 & 15): Returns null without querying chatResponse metadata.
        return null;
    }
}
