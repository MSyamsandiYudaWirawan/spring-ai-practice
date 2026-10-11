# Phase 01 Exercise 02 — Golden Reference Solution
## Advanced ChatClient & FinTech Operations (15 Drills)

```java
package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service under test for Phase 01 Exercise 02: Advanced ChatClient & FinTech Operations (15 Drills).
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
        if (complianceRole == null || complianceRole.isBlank()) {
            throw new IllegalArgumentException("complianceRole must not be null or blank");
        }
        Objects.requireNonNull(tx, "tx must not be null");
        return chatClient.prompt()
                .system(complianceRole)
                .user(tx.toString())
                .call()
                .content();
    }

    /**
     * Drill 3: Parameterized system prompt specification.
     */
    public String configureRegionalAuditor(String jurisdiction, String standard, String auditQuery) {
        if (jurisdiction == null || jurisdiction.isBlank() || standard == null || standard.isBlank() || auditQuery == null || auditQuery.isBlank()) {
            throw new IllegalArgumentException("Parameters must not be null or blank");
        }
        return chatClient.prompt()
                .system(s -> s.text(FraudModelContracts.REGIONAL_AUDITOR_SYSTEM_TEMPLATE)
                        .param("jurisdiction", jurisdiction)
                        .param("standard", standard))
                .user(auditQuery)
                .call()
                .content();
    }

    /**
     * Drills 4, 5 & 6: Parameterized template using a Map with special character preservation.
     */
    public String draftDisputeNotice(FraudModelContracts.DisputeNotice dispute) {
        Objects.requireNonNull(dispute, "dispute must not be null");
        return chatClient.prompt()
                .user(u -> u.text(FraudModelContracts.DISPUTE_TEMPLATE)
                        .params(Map.of(
                                "customerName", dispute.customerName(),
                                "disputeId", dispute.disputeId(),
                                "amount", dispute.amount(),
                                "currency", dispute.currency(),
                                "reason", dispute.reason())))
                .call()
                .content();
    }

    /**
     * Drills 7, 8 & 9: Call-level options override with fail-fast validation.
     */
    public String auditHighRiskTransfer(String transferDetails) {
        if (transferDetails == null || transferDetails.isBlank()) {
            throw new IllegalArgumentException("transferDetails must not be null or blank");
        }
        return chatClient.prompt()
                .user(transferDetails)
                .options(ChatOptions.builder()
                        .temperature(FraudModelContracts.OVERRIDE_TEMPERATURE)
                        .maxTokens(FraudModelContracts.OVERRIDE_MAX_TOKENS))
                .call()
                .content();
    }

    /**
     * Drills 10 & 11: Multi-turn history replay with boundary validation.
     */
    public String continueDisputeDialogue(List<Message> history, String latestCustomerMessage) {
        if (history == null) {
            throw new IllegalArgumentException("history must not be null");
        }
        if (latestCustomerMessage == null || latestCustomerMessage.isBlank()) {
            throw new IllegalArgumentException("latestCustomerMessage must not be null or blank");
        }
        return chatClient.prompt()
                .messages(history)
                .user(latestCustomerMessage)
                .call()
                .content();
    }

    /**
     * Drill 12: Few-shot in-context learning priming.
     */
    public String classifyTransactionIntent(String customerInquiry) {
        if (customerInquiry == null || customerInquiry.isBlank()) {
            throw new IllegalArgumentException("customerInquiry must not be null or blank");
        }
        List<Message> messages = new ArrayList<>();
        for (FraudModelContracts.FewShotExample ex : FraudModelContracts.CLASSIFICATION_FEW_SHOTS) {
            messages.add(new UserMessage(ex.userText()));
            messages.add(new AssistantMessage(ex.assistantReply()));
        }
        return chatClient.prompt()
                .messages(messages)
                .user(customerInquiry)
                .call()
                .content();
    }

    /**
     * Drill 13: ChatClient mutation via .mutate().
     */
    public ChatClient deriveExpeditedClient() {
        return this.chatClient.mutate()
                .defaultSystem(FraudModelContracts.EXPEDITED_SYSTEM_PROMPT)
                .defaultOptions(ChatOptions.builder().temperature(0.1))
                .build();
    }

    /**
     * Drills 14 & 15: Token usage audit extraction from ChatResponse metadata.
     */
    public FraudModelContracts.AuditResult executeWithTokenAudit(String operationalPrompt) {
        if (operationalPrompt == null || operationalPrompt.isBlank()) {
            throw new IllegalArgumentException("operationalPrompt must not be null or blank");
        }
        var response = chatClient.prompt()
                .user(operationalPrompt)
                .call()
                .chatResponse();
        String content = response != null && response.getResult() != null && response.getResult().getOutput() != null
                ? response.getResult().getOutput().getText() : "";
        long promptTokens = 0L;
        long completionTokens = 0L;
        if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            promptTokens = response.getMetadata().getUsage().getPromptTokens();
            completionTokens = response.getMetadata().getUsage().getCompletionTokens();
        }
        return new FraudModelContracts.AuditResult(content, promptTokens, completionTokens);
    }
}
```
