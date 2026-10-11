# Phase 01 Exercise 01 — `ChatClient` Fluent API & Prompt Templating

**Theme:** Core `ChatClient` request building, system/user role separation, template parameter binding, and builder defaults (15 Muscle-Memory Drills).  
**Diagnostician Mapping:** `DecideTurn` prompt building, baseline injection, and system context isolation (`SystemPrompts.buildUserContext`).

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase01/ChatServiceUnderTest.java`](src/main/java/phase01/ChatServiceUnderTest.java) | The code under test — **FIX THIS FILE ONLY** |
| [`src/main/java/phase01/ModelContracts.java`](src/main/java/phase01/ModelContracts.java) | Data records, prompt templates, and constants (DO NOT MODIFY) |
| [`src/test/java/phase01/FakeChatModel.java`](src/test/java/phase01/FakeChatModel.java) | Deterministic offline `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase01/Verifier.java`](src/test/java/phase01/Verifier.java) | The evaluation gate (15 Drills, exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase01-ex01-golden.md`](../docs/golden/phase01-ex01-golden.md) | **SEALED** golden reference solution |

**Time-box:** **45 minutes** for all 15 drills + write-up.

---

## 2. Reproduction Commands

Run the verifier across all drills (or isolate an individual drill):

```powershell
# Run all 15 drills:
mvn test-compile exec:java -pl phase01-ex01-chat-basics

# Or via Maven test runner:
mvn test -pl phase01-ex01-chat-basics

# Run a specific drill (1 through 15):
mvn test-compile exec:java -pl phase01-ex01-chat-basics -Dexec.args="1"
mvn test-compile exec:java -pl phase01-ex01-chat-basics -Dexec.args="7"
mvn test-compile exec:java -pl phase01-ex01-chat-basics -Dexec.args="13"
```

Exit `99` = measured FAIL (findings detected with tiered progressive hints). Read the report.  
Exit `0` = PASS.

---

## 3. The 15 Drills (5 Topics x 3 Repetitions)

### Topic 1: Role Separation & System Instructions (Drills 1–3)
- **Drill 1:** `askWithSystemInstructions` (Role Separation) — Explicit `.system(systemRole).user(userQuery)` semantic role separation.
- **Drill 2:** `askWithSystemInstructions` (Boundaries) — Fail-fast boundary checks throwing `IllegalArgumentException` on null/blank roles or queries.
- **Drill 3:** `askWithSystemAndContext` (Parameterized SystemSpec) — Dynamic system prompt configuration via `.system(s -> s.text(...).param(...))`.

### Topic 2: Prompt Templates & Fluent Parameter Binding (Drills 4–6)
- **Drill 4:** `renderDiagnosisPrompt` (Diagnosis Binding) — Parameterized prompt template binding `{targetApp}`, `{baselineP95}`, `{topFrame}`.
- **Drill 5:** `renderDiagnosisPrompt` (Boundaries) — Fail-fast validation verifying null `DiagnosisContext` throws `NullPointerException` or `IllegalArgumentException`.
- **Drill 6:** `renderCustomTemplate` (Map Binding) — Dynamic map binding using `.user(u -> u.text(template).params(map))`.

### Topic 3: ChatClient Builder Defaults & Configuration (Drills 7–9)
- **Drill 7:** `generateReportWithDefaults` (Default System) — ChatClient automatically attaches `EXPECTED_DEFAULT_SYSTEM_PROMPT`.
- **Drill 8:** `generateReportWithDefaults` (Default Options) — ChatClient attaches default options (`temperature = 0.2`, `topP = 0.9`).
- **Drill 9:** `ChatServiceUnderTest` Constructor (Builder Null-Check) — `Objects.requireNonNull(clientBuilder)` fail-fast invariant.

### Topic 4: Dynamic Option Overrides at Call-Time (Drills 10–12)
- **Drill 10:** `askWithCustomTemperature` (Call-Level Override) — Per-call temperature override (`.options(ChatOptions.builder().temperature(...))`).
- **Drill 11:** `askWithCustomTemperature` (Range Validation) — Temperature must be within `[0.0, 2.0]`; throws `IllegalArgumentException` otherwise.
- **Drill 12:** `askWithMaxTokens` (Max Tokens Override & Bounds) — Call-level `maxTokens` override; non-positive values throw `IllegalArgumentException`.

### Topic 5: Multi-Message Turn History & Conversational Invariants (Drills 13–15)
- **Drill 13:** `askMultiTurnConversation` (History Replay) — Injects prior conversation history via `.messages(history).user(newUserQuery)`.
- **Drill 14:** `askMultiTurnConversation` (Empty History) — Seamless execution with empty history list `List.of()`.
- **Drill 15:** `askMultiTurnConversation` (Boundaries) — Fail-fast validation on null history or null/blank user queries.
