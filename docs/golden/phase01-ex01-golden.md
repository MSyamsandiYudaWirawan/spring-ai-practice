# Phase 01 Exercise 01 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `askWithSystemInstructions`

### Root Cause
The broken code performs raw string concatenation (`"System: " + systemRole + "\nUser: " + userQuery`) into the user message. In LLM protocols and Spring AI, messages have explicit semantic roles (`SystemMessage`, `UserMessage`, `AssistantMessage`). Stuffing system steering instructions into the user prompt causes:
1. `prompt.getSystemMessage()` to be `null`.
2. The model to receive the system instruction as untrusted user content, making it susceptible to prompt injection.

### Rejected Fixes
- `chatClient.prompt().user("SYSTEM: " + systemRole).user(userQuery)`: Rejected. Calling `.user()` multiple times overrides the user message or causes undefined ordering; it still fails to produce a `SystemMessage`.
- `chatClient.prompt().messages(new SystemMessage(systemRole), new UserMessage(userQuery))`: While technically producing the messages, it bypasses `ChatClient`'s fluent request spec (`.system(...)` and `.user(...)`) which integrates with advisors and prompt templates.

### Fix Applied
```java
public String askWithSystemInstructions(String systemRole, String userQuery) {
    return chatClient.prompt()
            .system(systemRole)
            .user(userQuery)
            .call()
            .content();
}
```

---

## 2. Scenario 2: `renderDiagnosisPrompt`

### Root Cause
The broken code passes the raw template string `ModelContracts.DIAGNOSIS_PROMPT_TEMPLATE` directly to `.user(...)` without parameter bindings. As a result, literal placeholders (`{targetApp}`, `{baselineP95}`, `{topFrame}`) are dispatched directly to the model.

### Rejected Fixes
- Manual `String.format` or string concatenation (`"Diagnosing target application: " + ctx.targetApp() + ...`): Rejected. String concatenation bypasses template parameter sanitization, breaks when inputs contain curly brackets or special characters, and prevents template caching.
- `String.replace("{targetApp}", ctx.targetApp())`: Rejected. Fragile, manual string surgery violating TigerStyle simplicity.

### Fix Applied
```java
public String renderDiagnosisPrompt(ModelContracts.DiagnosisContext ctx) {
    return chatClient.prompt()
            .user(u -> u.text(ModelContracts.DIAGNOSIS_PROMPT_TEMPLATE)
                    .param("targetApp", ctx.targetApp())
                    .param("baselineP95", ctx.baselineP95Ms())
                    .param("topFrame", ctx.jfrTopFrame()))
            .call()
            .content();
}
```

---

## 3. Scenario 3: `generateReportWithDefaults`

### Root Cause
The constructor called `this.chatClient = clientBuilder.build()` without configuring default system prompts or default options on the builder. Calls made by `generateReportWithDefaults` therefore dispatched with `systemMessage == null` and unconfigured options (`temperature == null`, `topP == null`).

### Rejected Fixes
- Hardcoding `.system(...)` and `.options(...)` on every individual call inside `generateReportWithDefaults`: Rejected. Violates the requirement that `ChatClient` instances configured for a subsystem (e.g. Diagnostician) should carry shared defaults (system role, temperature=0.2) so individual call sites stay clean and DRY.
- Calling `clientBuilder.defaultOptions(ChatOptions.builder()...build())`: Compilation error. In Spring AI 2.0.1, `defaultOptions` takes `ChatOptions.Builder<?>`, NOT the built `ChatOptions` instance.

### Fix Applied
```java
public ChatServiceUnderTest(ChatClient.Builder clientBuilder) {
    Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
    this.chatClient = clientBuilder
            .defaultSystem(ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT)
            .defaultOptions(org.springframework.ai.chat.prompt.ChatOptions.builder()
                    .temperature(ModelContracts.EXPECTED_DEFAULT_TEMPERATURE)
                    .topP(ModelContracts.EXPECTED_DEFAULT_TOP_P))
            .build();
}
```
