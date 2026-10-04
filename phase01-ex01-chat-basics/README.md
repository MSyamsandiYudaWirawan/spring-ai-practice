# Phase 01 Exercise 01 â€” `ChatClient` Fluent API & Prompt Templating

**Theme:** Core `ChatClient` request building, system/user role separation, template parameter binding, and builder defaults.  
**Diagnostician Mapping:** `DecideTurn` prompt building, baseline injection, and system context isolation (`SystemPrompts.buildUserContext`).

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase01/ChatServiceUnderTest.java`](src/main/java/phase01/ChatServiceUnderTest.java) | The code under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase01/ModelContracts.java`](src/main/java/phase01/ModelContracts.java) | Data records, prompt templates, and constants (DO NOT MODIFY) |
| [`src/test/java/phase01/FakeChatModel.java`](src/test/java/phase01/FakeChatModel.java) | Deterministic offline `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase01/Verifier.java`](src/test/java/phase01/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |

**Time-box:** **35 minutes** for all 3 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or single scenario):

```powershell
# From repo root:
mvn compile exec:java -pl phase01-ex01-chat-basics               # Run all 3 scenarios
mvn compile exec:java -pl phase01-ex01-chat-basics -Dexec.args="1" # Scenario 1 only
mvn compile exec:java -pl phase01-ex01-chat-basics -Dexec.args="2" # Scenario 2 only
mvn compile exec:java -pl phase01-ex01-chat-basics -Dexec.args="3" # Scenario 3 only
```

Or via Maven test:
```powershell
mvn test -pl phase01-ex01-chat-basics
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. What the Verifier Measures

### Scenario 1: `askWithSystemInstructions(systemRole, userQuery)`
- **Gate:** `prompt.getSystemMessage()` must NOT be null and must equal `systemRole`.
- `prompt.getUserMessage()` must equal `userQuery` without leaked `System:` prefixes.
- Total instructions must equal 2 (`SYSTEM`, `USER`).

### Scenario 2: `renderDiagnosisPrompt(DiagnosisContext ctx)`
- **Gate:** Template placeholders `{targetApp}`, `{baselineP95}`, `{topFrame}` must be substituted via `ChatClient`'s parameter binding (`.user(u -> u.text(...).param(...))`).
- Must handle normal method signatures as well as signatures with lambdas and braces (e.g. `OwnerRepo$$Lambda$842.apply({arg0})`) without corruption.
- Output must not contain un-substituted `{targetApp}` placeholders.

### Scenario 3: `generateReportWithDefaults(analysisSnippet)`
- **Gate:** `ChatClient.Builder` in constructor must configure:
  1. Default system prompt: `ModelContracts.EXPECTED_DEFAULT_SYSTEM_PROMPT`
  2. Default options: `temperature = 0.2` and `topP = 0.9` (via `ChatOptions.builder().temperature(...).topP(...)`)
- Calls to `generateReportWithDefaults` must inherit these defaults automatically.

---

## 4. Your Task

1. **Reproduce** the FAIL before touching code:
   `mvn -q exec:java -pl phase01-ex01-chat-basics`
2. **Diagnose** the mechanism for each scenario.
3. **Fix** `ChatServiceUnderTest.java` (only).
4. **Verify**:
   `mvn -q exec:java -pl phase01-ex01-chat-basics` $\to$ `ALL SCENARIOS PASSED (exit code 0)`.
6. Check your reasoning against the rubric, then check `docs/golden/phase01-ex01-golden.md`.
