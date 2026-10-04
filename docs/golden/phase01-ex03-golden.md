# Phase 01 Exercise 03 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `analyzePodCrash` (Boundary Validation & Role Separation)

### Root Cause
Concatenating cluster metadata (`clusterId`, `namespace`) directly into the user message fails to set `prompt.getSystemMessage()`, mixing operational control plane metadata with untrusted pod log text. Furthermore, missing fail-fast parameter validation allows `null` or blank parameters to propagate into LLM requests.

### TigerStyle Fix
```java
public String analyzePodCrash(String clusterId, String namespace, String podLogs) {
    Objects.requireNonNull(clusterId, "clusterId must not be null");
    if (clusterId.isBlank()) throw new IllegalArgumentException("clusterId cannot be blank");
    Objects.requireNonNull(namespace, "namespace must not be null");
    if (namespace.isBlank()) throw new IllegalArgumentException("namespace cannot be blank");
    Objects.requireNonNull(podLogs, "podLogs must not be null");
    if (podLogs.isBlank()) throw new IllegalArgumentException("podLogs cannot be blank");

    return chatClient.prompt()
            .system("Cluster: " + clusterId + " | Namespace: " + namespace)
            .user(podLogs)
            .call()
            .content();
}
```

---

## 2. Scenario 2: `renderResourceExhaustionAlert` (Template Parameter Binding)

### Root Cause
Passing `SreModelContracts.RESOURCE_ALERT_TEMPLATE` directly as raw string text leaves placeholders like `{serviceName}`, `{cpuPercent}`, `{memoryBytes}`, `{p99LatencySec}`, and `{errorCodes}` completely un-substituted.

### TigerStyle Fix
```java
public String renderResourceExhaustionAlert(SreModelContracts.AlertDetails alert) {
    Objects.requireNonNull(alert, "alert must not be null");

    return chatClient.prompt()
            .user(u -> u.text(SreModelContracts.RESOURCE_ALERT_TEMPLATE)
                    .params(Map.of(
                            "serviceName", alert.serviceName(),
                            "cpuPercent", alert.cpuPercent(),
                            "memoryBytes", alert.memoryBytes(),
                            "p99LatencySec", alert.p99LatencySec(),
                            "errorCodes", alert.errorCodes()
                    )))
            .call()
            .content();
}
```

---

## 3. Scenario 3: `triageWithEnvironmentPolicy` (Conditional System Policy Injection)

### Root Cause
Calling `.user(anomalyDescription)` without setting `.system(...)` causes the prompt to fall back to the generic `BASE_SRE_SYSTEM_PROMPT` configured on the client builder, ignoring environment governance policies (production vs non-production).

### TigerStyle Fix
```java
public String triageWithEnvironmentPolicy(SreModelContracts.Environment env, String anomalyDescription) {
    Objects.requireNonNull(env, "env must not be null");
    Objects.requireNonNull(anomalyDescription, "anomalyDescription must not be null");
    if (anomalyDescription.isBlank()) throw new IllegalArgumentException("anomalyDescription cannot be blank");

    String policy = (env == SreModelContracts.Environment.PRODUCTION)
            ? SreModelContracts.PROD_POLICY
            : SreModelContracts.NON_PROD_POLICY;

    return chatClient.prompt()
            .system(policy)
            .user(anomalyDescription)
            .call()
            .content();
}
```

---

## 4. Scenario 4: `executeIncidentDiagnosisWithAudit` (TigerStyle Defensive Token Auditing)

### Root Cause
Calling `.call().content()` discards all metadata, making token accounting impossible. In addition, attempting to unbox `usage.getPromptTokens()` directly without checking for null causes `NullPointerException` if a mock model or provider returns null metadata.

### TigerStyle Fix
```java
public SreModelContracts.SreAudit executeIncidentDiagnosisWithAudit(String incidentReport) {
    Objects.requireNonNull(incidentReport, "incidentReport must not be null");
    if (incidentReport.isBlank()) throw new IllegalArgumentException("incidentReport cannot be blank");

    org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt()
            .user(incidentReport)
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

    return new SreModelContracts.SreAudit(content, promptTokens, completionTokens);
}
```

---

## 5. Scenario 5: `deriveCanaryAnalysisClient` (Client Mutation with Parameterized Template)

### Root Cause
Returning `this.chatClient` without calling `.mutate()` produces no isolated client specialization. Furthermore, missing boundary validation allows negative traffic percentages.

### TigerStyle Fix
```java
public ChatClient deriveCanaryAnalysisClient(double canaryTrafficPercent) {
    if (canaryTrafficPercent < 0.0 || canaryTrafficPercent > 100.0) {
        throw new IllegalArgumentException("canaryTrafficPercent must be between 0.0 and 100.0");
    }

    return this.chatClient.mutate()
            .defaultSystem(s -> s.text(SreModelContracts.CANARY_SYSTEM_TEMPLATE)
                    .param("trafficPercent", canaryTrafficPercent))
            .defaultOptions(ChatOptions.builder()
                    .temperature(0.1))
            .build();
}
```

---

## 6. Scenario 6: `generateDeterministicRunbook` (Fine-Grained ChatOptions)

### Root Cause
Dispatches requests without call-level options, inheriting the base client's temperature (`0.4`) and unbounded generation length.

### TigerStyle Fix
```java
public String generateDeterministicRunbook(String alertName) {
    Objects.requireNonNull(alertName, "alertName must not be null");
    if (alertName.isBlank()) throw new IllegalArgumentException("alertName cannot be blank");

    return chatClient.prompt()
            .options(ChatOptions.builder()
                    .temperature(SreModelContracts.DETERMINISTIC_TEMPERATURE)
                    .maxTokens(SreModelContracts.RUNBOOK_MAX_TOKENS)
                    .stopSequences(SreModelContracts.RUNBOOK_STOP_SEQUENCES)
                    .frequencyPenalty(SreModelContracts.RUNBOOK_FREQUENCY_PENALTY))
            .user(alertName)
            .call()
            .content();
}
```

---

## 7. Scenario 7: `classifyRemediationAction` (Few-Shot In-Context Priming)

### Root Cause
Dispatches `failureSymptom` directly without priming the prompt with few-shot demonstration pairs, leaving the model without in-context grounding.

### TigerStyle Fix
```java
public String classifyRemediationAction(String failureSymptom) {
    Objects.requireNonNull(failureSymptom, "failureSymptom must not be null");
    if (failureSymptom.isBlank()) throw new IllegalArgumentException("failureSymptom cannot be blank");

    List<Message> fewShots = new java.util.ArrayList<>();
    for (SreModelContracts.FewShotPair pair : SreModelContracts.REMEDIATION_FEW_SHOTS) {
        fewShots.add(new org.springframework.ai.chat.messages.UserMessage(pair.symptom()));
        fewShots.add(new org.springframework.ai.chat.messages.AssistantMessage(pair.action()));
    }

    return chatClient.prompt()
            .messages(fewShots)
            .user(failureSymptom)
            .call()
            .content();
}
```

---

## 8. Scenario 8: `continueTroubleshootingSession` (History Replay)

### Root Cause
Omits `.messages(history)` and only passes `userReply`, discarding the multi-turn diagnostic conversation history and causing state loss.

### TigerStyle Fix
```java
public String continueTroubleshootingSession(List<Message> history, String userReply) {
    Objects.requireNonNull(history, "history must not be null");
    Objects.requireNonNull(userReply, "userReply must not be null");
    if (userReply.isBlank()) throw new IllegalArgumentException("userReply cannot be blank");

    return chatClient.prompt()
            .messages(history)
            .user(userReply)
            .call()
            .content();
}
```

---

## 9. Scenario 9: `applyIncidentPagingEnvelope` (System Envelope Augmentation)

### Root Cause
Concatenating the paging envelope into the user query leaks paging dispatcher instructions into the user role and prevents proper templating of the system prompt.

### TigerStyle Fix
```java
public String applyIncidentPagingEnvelope(String onCallEngineer, String incidentQuery) {
    Objects.requireNonNull(onCallEngineer, "onCallEngineer must not be null");
    if (onCallEngineer.isBlank()) throw new IllegalArgumentException("onCallEngineer cannot be blank");
    Objects.requireNonNull(incidentQuery, "incidentQuery must not be null");
    if (incidentQuery.isBlank()) throw new IllegalArgumentException("incidentQuery cannot be blank");

    String systemTemplate = "ON-CALL DISPATCH TO: {engineer}\n" + SreModelContracts.BASE_SRE_SYSTEM_PROMPT;

    return chatClient.prompt()
            .system(s -> s.text(systemTemplate).param("engineer", onCallEngineer))
            .user(incidentQuery)
            .call()
            .content();
}
```

---

## 10. Scenario 10: `safeExecuteDiagnosis` (Fail-Fast Structural Invariant Enforcement)

### Root Cause
Calling `.call().content()` blindly without validating the response structure returns raw un-trimmed content and silently permits empty or malformed model responses, violating production diagnostic guarantees.

### TigerStyle Fix
```java
public String safeExecuteDiagnosis(String query) {
    Objects.requireNonNull(query, "query must not be null");
    if (query.isBlank()) throw new IllegalArgumentException("query cannot be blank");

    org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt()
            .user(query)
            .call()
            .chatResponse();

    if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
        throw new IllegalStateException("SRE diagnosis failed: empty or malformed LLM response");
    }

    String text = response.getResult().getOutput().getText();
    if (text == null || text.isBlank()) {
        throw new IllegalStateException("SRE diagnosis failed: empty or malformed LLM response");
    }

    return text.trim();
}
```
