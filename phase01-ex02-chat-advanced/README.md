# Phase 01 Exercise 02 â€” Advanced `ChatClient` Fluent API Suite

**Theme:** FinTech Fraud & Risk Operations Platform (8 Comprehensive Scenarios).  
**Core Competency:** Total muscle memory over Spring AI's `ChatClient` request building, parameter maps, parameterized system prompts, options overrides, history replay, few-shot priming, client mutation, and metadata extraction.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase01/FraudOpsServiceUnderTest.java`](src/main/java/phase01/FraudOpsServiceUnderTest.java) | The code under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase01/FraudModelContracts.java`](src/main/java/phase01/FraudModelContracts.java) | Domain records, prompt templates, and constants (DO NOT MODIFY) |
| [`src/test/java/phase01/FakeChatModel.java`](src/test/java/phase01/FakeChatModel.java) | Deterministic offline `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase01/Verifier.java`](src/test/java/phase01/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| `docs/golden/phase01-ex02-golden.md` | Sealed golden solution (open only after completion) |

**Time-box:** **50 minutes** for all 8 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or isolate an individual scenario):

```powershell
# Run all 8 scenarios:
mvn test-compile exec:java -pl phase01-ex02-chat-advanced

# Or via Maven test runner:
mvn test -pl phase01-ex02-chat-advanced

# Run a specific scenario (1 through 8):
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="1"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="2"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="3"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="4"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="5"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="6"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="7"
mvn test-compile exec:java -pl phase01-ex02-chat-advanced -Dexec.args="8"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. The 8 Scenarios & Gates

### Scenario 1: `analyzeTransaction(complianceRole, tx)`
- **Gate:** `SystemMessage` must equal `complianceRole`; `UserMessage` must contain transaction details without leaked `Role:` prefix.

### Scenario 2: `draftDisputeNotice(dispute)`
- **Gate:** Uses `FraudModelContracts.DISPUTE_TEMPLATE` with `.params(Map.of(...))`. Special characters (`$250.00`, `{sub_ref: 99}`) must NOT be corrupted or trigger string format errors.

### Scenario 3: `configureRegionalAuditor(jurisdiction, standard, auditQuery)`
- **Gate:** Parameterizes system prompt dynamically via `.system(s -> s.text(...).param(...).param(...))` without disturbing the user query.

### Scenario 4: `auditHighRiskTransfer(transferDetails)`
- **Gate:** Applies call-level options override (`temperature = 0.0`, `maxTokens = 500`) to override the client's default `0.7` temperature.

### Scenario 5: `continueDisputeDialogue(history, latestCustomerMessage)`
- **Gate:** Replays multi-turn message history via `.messages(history)` while appending `latestCustomerMessage` as the final `USER` message, maintaining message types.

### Scenario 6: `classifyTransactionIntent(customerInquiry)`
- **Gate:** Primes the model using in-context few-shot learning by transforming `CLASSIFICATION_FEW_SHOTS` into alternating `UserMessage` and `AssistantMessage` pairs via `.messages(...)`.

### Scenario 7: `deriveExpeditedClient()`
- **Gate:** Mutates the existing `chatClient` via `.mutate()`, setting `EXPEDITED_SYSTEM_PROMPT` and `temperature = 0.1` on the derived client without altering the base client.

### Scenario 8: `executeWithTokenAudit(operationalPrompt)`
- **Gate:** Calls `.call().chatResponse()`, extracts reply text, and retrieves non-zero `promptTokens` and `completionTokens` from `response.getMetadata().getUsage()`.

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase01-ex02-chat-advanced` $\to$ `0 PASSED, 8 FAILED (exit code 99)`.
2. **Fix** [`FraudOpsServiceUnderTest.java`](src/main/java/phase01/FraudOpsServiceUnderTest.java).
3. **Verify** all 8 pass:
   `mvn test-compile exec:java -pl phase01-ex02-chat-advanced` $\to$ `8 PASSED, 0 FAILED (exit code 0)`.
