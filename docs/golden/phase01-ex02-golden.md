# Phase 01 Exercise 02 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `analyzeTransaction` (Role Separation)

### Root Cause
Concatenating the compliance role into the user prompt string leaves `prompt.getSystemMessage()` null and confuses untrusted user data with authoritative system instructions.

### Fix Applied
```java
public String analyzeTransaction(String complianceRole, FraudModelContracts.Transaction tx) {
    return chatClient.prompt()
            .system(complianceRole)
            .user(tx.toString())
            .call()
            .content();
}
```

---

## 2. Scenario 2: `draftDisputeNotice` (Map Template Binding)

### Root Cause
Sending the raw template string leaves `{placeholder}` variables un-rendered. Manual string replacement can corrupt special characters (e.g. `$` and `{}` inside values).

### Fix Applied
```java
public String draftDisputeNotice(FraudModelContracts.DisputeNotice dispute) {
    return chatClient.prompt()
            .user(u -> u.text(FraudModelContracts.DISPUTE_TEMPLATE)
                    .params(Map.of(
                            "customerName", dispute.customerName(),
                            "disputeId", dispute.disputeId(),
                            "amount", dispute.amount(),
                            "currency", dispute.currency(),
                            "reason", dispute.reason()
                    )))
            .call()
            .content();
}
```

---

## 3. Scenario 3: `configureRegionalAuditor` (Parameterized System Prompt)

### Root Cause
System prompts can also be parameterized dynamically using `PromptSystemSpec`. Passing the regional template in the user message or using string concat bypasses Spring AI's system prompt renderer.

### Fix Applied
```java
public String configureRegionalAuditor(String jurisdiction, String standard, String auditQuery) {
    return chatClient.prompt()
            .system(s -> s.text(FraudModelContracts.REGIONAL_AUDITOR_SYSTEM_TEMPLATE)
                    .param("jurisdiction", jurisdiction)
                    .param("standard", standard))
            .user(auditQuery)
            .call()
            .content();
}
```

---

## 4. Scenario 4: `auditHighRiskTransfer` (Call-Level Options Override)

### Root Cause
Without call-level `.options(...)`, the request inherits the base client's higher temperature (0.7), violating zero-temperature determinism required for high-risk audits.

### Fix Applied
```java
public String auditHighRiskTransfer(String transferDetails) {
    return chatClient.prompt()
            .options(ChatOptions.builder()
                    .temperature(FraudModelContracts.OVERRIDE_TEMPERATURE)
                    .maxTokens(FraudModelContracts.OVERRIDE_MAX_TOKENS))
            .user(transferDetails)
            .call()
            .content();
}
```

---

## 5. Scenario 5: `continueDisputeDialogue` (Multi-Turn History Replay)

### Root Cause
Omitting `.messages(history)` drops previous conversational context. Passing flattened strings loses the explicit `MessageType.USER` and `MessageType.ASSISTANT` semantic structure.

### Fix Applied
```java
public String continueDisputeDialogue(List<Message> history, String latestCustomerMessage) {
    return chatClient.prompt()
            .messages(history)
            .user(latestCustomerMessage)
            .call()
            .content();
}
```

---

## 6. Scenario 6: `classifyTransactionIntent` (Few-Shot Priming)

### Root Cause
Few-shot examples must be structured as alternating `UserMessage` and `AssistantMessage` instances before the final user query to establish pattern completion for the model.

### Fix Applied
```java
public String classifyTransactionIntent(String customerInquiry) {
    List<Message> fewShots = new java.util.ArrayList<>();
    for (FraudModelContracts.FewShotExample ex : FraudModelContracts.CLASSIFICATION_FEW_SHOTS) {
        fewShots.add(new org.springframework.ai.chat.messages.UserMessage(ex.userText()));
        fewShots.add(new org.springframework.ai.chat.messages.AssistantMessage(ex.assistantReply()));
    }
    return chatClient.prompt()
            .messages(fewShots)
            .user(customerInquiry)
            .call()
            .content();
}
```

---

## 7. Scenario 7: `deriveExpeditedClient` (ChatClient Mutation)

### Root Cause
`chatClient.mutate()` produces a mutable builder pre-populated with the existing client's configuration (advisors, tools, HTTP settings), allowing isolated derivation of specialized client variants without altering the base client.

### Fix Applied
```java
public ChatClient deriveExpeditedClient() {
    return this.chatClient.mutate()
            .defaultSystem(FraudModelContracts.EXPEDITED_SYSTEM_PROMPT)
            .defaultOptions(ChatOptions.builder()
                    .temperature(0.1))
            .build();
}
```

---

## 8. Scenario 8: `executeWithTokenAudit` (Usage Metadata Extraction)

### Root Cause
Calling `.call().content()` discards metadata. Using `.call().chatResponse()` enables inspection of `response.getMetadata().getUsage()`, providing `getPromptTokens()` and `getCompletionTokens()`.

### Fix Applied
```java
public FraudModelContracts.AuditResult executeWithTokenAudit(String operationalPrompt) {
    org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt()
            .user(operationalPrompt)
            .call()
            .chatResponse();

    String content = "";
    if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
        content = response.getResult().getOutput().getText();
        if (content == null) content = "";
    }

    long promptTokens = 0L;
    long completionTokens = 0L;
    if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
        org.springframework.ai.chat.metadata.Usage usage = response.getMetadata().getUsage();
        if (usage.getPromptTokens() != null) {
            promptTokens = usage.getPromptTokens().longValue();
        }
        if (usage.getCompletionTokens() != null) {
            completionTokens = usage.getCompletionTokens().longValue();
        }
    }

    return new FraudModelContracts.AuditResult(content, promptTokens, completionTokens);
}
```
