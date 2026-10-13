# Phase 01 Exercise 02 — Advanced `ChatClient` Fluent API Suite

**Theme:** FinTech Fraud & Risk Operations Platform (15 Muscle-Memory Drills).  
**Core Competency:** Total muscle memory over Spring AI's `ChatClient` request building, parameter maps, parameterized system prompts, options overrides, history replay, few-shot priming, client mutation, and metadata extraction.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase01/FraudOpsServiceUnderTest.java`](src/main/java/phase01/FraudOpsServiceUnderTest.java) | The code under test — **FIX THIS FILE ONLY** |
| [`src/main/java/phase01/FraudModelContracts.java`](src/main/java/phase01/FraudModelContracts.java) | Domain records, prompt templates, and constants (DO NOT MODIFY) |
| [`src/test/java/phase01/FakeChatModel.java`](src/test/java/phase01/FakeChatModel.java) | Deterministic offline `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase01/Verifier.java`](src/test/java/phase01/Verifier.java) | The evaluation gate (15 Drills, exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase01-ex02-golden.md`](../docs/golden/phase01-ex02-golden.md) | **SEALED** golden reference solution |

**Time-box:** **50 minutes** for all 15 drills + write-up.

---

## 2. Reproduction Commands

Run the verifier across all drills (or isolate an individual drill):

```powershell
# Run all 15 drills:
mvn test-compile exec:java -pl phase01-ex02-chat-advanced

# Or via Maven test runner:
mvn test -pl phase01-ex02-chat-advanced

# Run a specific drill (1 through 15):
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="1"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="7"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="13"
```

Exit `99` = measured FAIL (findings detected with tiered progressive hints). Read the report.  
Exit `0` = PASS.

---

## 🧠 Architectural Theory & Response Metadata Flow

Advanced agentic auditing requires not just obtaining output text, but inspecting execution metadata, token costs, and overriding model options per call:

```
                                 FINTECH COMPLIANCE AUDITING FLOW
[Transaction DTO] ──► [System & User Specs] ──► [ChatClient] ──► [ChatModel]
                            │                                         │
                            ├─► Regional Compliance System Prompt     ▼
                            └─► Parameterized Transaction Data    [ChatResponse]
                                                                      │
                                                ┌─────────────────────┴─────────────────────┐
                                                ▼                                           ▼
                                        [Output Generation]                         [Response Metadata]
                                   `response.getResult().getOutput()`         `response.getMetadata().getUsage()`
                                           (Audit Findings)                     (Prompt & Completion Tokens)
```

### 🧩 Core Spring AI Classes & Roles

| Class | Package | Role & Data Flow |
|---|---|---|
| `ChatOptions` / `Builder` | `org.springframework.ai.chat.prompt` | Configures model runtime hyper-parameters (`temperature`, `topP`). Overrides client builder defaults per prompt. |
| `ChatResponse` | `org.springframework.ai.chat.model` | Canonical execution response object returned by `call().chatResponse()`. |
| `Generation` | `org.springframework.ai.chat.model` | Individual completion alternative containing the generated `AssistantMessage`. |
| `ChatResponseMetadata` | `org.springframework.ai.chat.metadata` | Holds response metadata, finish reasons, and token consumption statistics. |
| `Usage` | `org.springframework.ai.chat.metadata` | Detailed token accounting interface exposing `getPromptTokens()`, `getCompletionTokens()`, and `getTotalTokens()`. |

---

## 3. The 15 Drills (5 Topics x 3 Repetitions)

### Topic 1: Role Separation & Parameterized System Specs (Drills 1–3)
- **Drill 1:** `analyzeTransaction` (Role Separation) — Explicit `.system(complianceRole).user(tx.toString())` semantic role separation.
- **Drill 2:** `analyzeTransaction` (Boundaries) — Fail-fast boundary checks throwing exceptions on null/blank role or null transaction.
- **Drill 3:** `configureRegionalAuditor` (SystemSpec) — Parameterized system prompt specification (`.system(s -> s.text(...).param(...))`).

### Topic 2: Parameterized Templates & Dispute Notifications (Drills 4–6)
- **Drill 4:** `draftDisputeNotice` (Map Binding) — Parameter map binding with `.user(u -> u.text(...).params(Map.of(...)))`.
- **Drill 5:** `draftDisputeNotice` (Boundaries) — Fail-fast validation verifying null dispute throws `NullPointerException` or `IllegalArgumentException`.
- **Drill 6:** `draftDisputeNotice` (Symbol Preservation) — Currency symbols (`$`), curly brackets, and special characters preserved without corruption.

### Topic 3: Call-Level Options Overrides & High-Risk Transfers (Drills 7–9)
- **Drill 7:** `auditHighRiskTransfer` (Options Override) — Call-level options override (`temperature = 0.0`, `maxTokens = 500`).
- **Drill 8:** `auditHighRiskTransfer` (Base Immutability) — Verifies base client defaults (temperature 0.7) remain unmutated on subsequent standard turns.
- **Drill 9:** `auditHighRiskTransfer` (Boundaries) — Fail-fast boundary checks on null or blank transfer details.

### Topic 4: Conversational History & Few-Shot In-Context Learning (Drills 10–12)
- **Drill 10:** `continueDisputeDialogue` (History Replay) — Injects prior conversation history via `.messages(history).user(latestCustomerMessage)`.
- **Drill 11:** `continueDisputeDialogue` (Boundaries) — Fail-fast boundary validation on null history or null/blank user message.
- **Drill 12:** `classifyTransactionIntent` (Few-Shot Priming) — Prepends alternating `UserMessage` and `AssistantMessage` pairs from `CLASSIFICATION_FEW_SHOTS`.

### Topic 5: Client Mutation & Token Usage Audit Metadata (Drills 13–15)
- **Drill 13:** `deriveExpeditedClient` (Client Mutation) — Uses `chatClient.mutate()` to create an expedited client with `EXPEDITED_SYSTEM_PROMPT` and temperature 0.1.
- **Drill 14:** `executeWithTokenAudit` (Metadata Extraction) — Calls `.chatResponse()` and extracts `promptTokens` and `completionTokens` from `Usage` metadata.
- **Drill 15:** `executeWithTokenAudit` (Boundaries) — Fail-fast validation verifying null/blank prompt throws `IllegalArgumentException`.
