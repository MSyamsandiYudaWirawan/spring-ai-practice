# Phase 04 Exercise 01 â€” Guardrails, Advisors & Token Budget Circuit-Breakers

**Theme:** Production Agentic Guardrail Stack & Execution Budget Circuit-Breakers (10 High-Repetition Scenarios).  
**Goal:** Build cold muscle memory writing Spring AI `CallAdvisor` implementations: pre/post call interception, context enrichment, keyword security, policy injection, token usage tracking, dollar cost limits, wall-clock latency caps, PII masking, onion ordering (`getOrder()`), and `ChatClient` advisor registration.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase04/GuardrailAdvisorsUnderTest.java`](src/main/java/phase04/GuardrailAdvisorsUnderTest.java) | The service under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase04/AdvisorContracts.java`](src/main/java/phase04/AdvisorContracts.java) | Immutable domain records, exceptions, and configs (DO NOT MODIFY) |
| [`src/test/java/phase04/FakeAdvisorChatModel.java`](src/test/java/phase04/FakeAdvisorChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase04/Verifier.java`](src/test/java/phase04/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase04-ex01-golden.md`](golden/phase04-ex01-golden.md) | **SEALED** golden reference solution |

**Time-box:** **50 minutes** for all 10 scenarios + write-up.

---

## 2. Reproduction Commands

```powershell
# Run all 10 scenarios:
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors

# Or via Maven test runner:
mvn test -pl phase04-ex01-guardrail-advisors

# Run a specific scenario (1 through 10):
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=1"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=2"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=3"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=4"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=5"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=6"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=7"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=8"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=9"
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors "-Dexec.args=10"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. Explicit Scenario & Error Message Contracts

In accordance with our zero-flakiness rule, all string contracts, exception types, and behavior invariants are explicitly documented below:

1. **`AuditLoggingAdvisor` (Scenario 1)**:
   - Implements `CallAdvisor`.
   - Pre-call: Enriches request context with `"correlationId"` (UUID string).
   - Post-call: Measures execution elapsed time (`latencyMs >= 0`).
   - Appends an [`AdvisorAuditRecord`](src/main/java/phase04/AdvisorContracts.java#L39) to a thread-safe list.
   - `getOrder()` returns `200`.

2. **`KeywordGuardrailAdvisor` (Scenario 2)**:
   - Implements `CallAdvisor`.
   - Inspects all messages in `request.prompt().getInstructions()`.
   - If any message contains a blocked keyword (case-insensitive): throws [`PromptSecurityException`](src/main/java/phase04/AdvisorContracts.java#L16).
   - **Error Message Contract**: Exception message MUST contain `"Blocked keyword"` (e.g. `"Blocked keyword detected: " + blockedKeyword`).
   - `getOrder()` returns `Ordered.HIGHEST_PRECEDENCE`.

3. **`SystemPromptEnforcingAdvisor` (Scenario 3)**:
   - Implements `CallAdvisor`.
   - Ensures outgoing `request.prompt()` contains the specified compliance `enforcedPolicy`.
   - If not present: prepends a `new SystemMessage(enforcedPolicy)` to the prompt's instructions and mutates request via `request.mutate().prompt(...)`.
   - `getOrder()` returns `50`.

4. **`TokenBudgetAdvisor` (Scenario 4)**:
   - Implements `CallAdvisor`.
   - Dispatches `chain.nextCall(request)`.
   - Reads `Usage` from `response.chatResponse().getMetadata().getUsage()`.
   - Defensively unboxes nullable tokens (null $\to$ 0).
   - Accumulates cumulative prompt, completion, and total tokens.
   - `getSummary()` returns [`TokenUsageSummary`](src/main/java/phase04/AdvisorContracts.java#L55).
   - `getOrder()` returns `150`.

5. **`CostCircuitBreakerAdvisor` (Scenario 5)**:
   - Implements `CallAdvisor`.
   - Configured with [`CostBudgetConfig(maxCostUsd, priceInPerMtok, priceOutPerMtok)`](src/main/java/phase04/AdvisorContracts.java#L64).
   - Computes call cost = `(promptTokens * priceInPerMtok + completionTokens * priceOutPerMtok) / 1_000_000.0`.
   - Tracks cumulative cost.
   - If cumulative cost exceeds `config.maxCostUsd()`: throws [`BudgetExceededException`](src/main/java/phase04/AdvisorContracts.java#L25).
   - **Error Message Contract**: Exception message MUST contain `"Budget limit exceeded"` (e.g. `"Budget limit exceeded: cumulative cost $X exceeds maximum cap $Y"`).
   - `getOrder()` returns `160`.

6. **`LatencyGuardrailAdvisor` (Scenario 6)**:
   - Implements `CallAdvisor`.
   - Configured with `maxLatencyMs`.
   - Measures elapsed time across `chain.nextCall(request)`.
   - If elapsed time exceeds `maxLatencyMs`: throws [`LatencyTimeoutException`](src/main/java/phase04/AdvisorContracts.java#L34).
   - **Error Message Contract**: Exception message MUST contain `"latency"` (e.g. `"Call latency X ms exceeded limit Y ms"`).
   - `getOrder()` returns `170`.

7. **`PiiMaskingAdvisor` (Scenario 7)**:
   - Implements `CallAdvisor`.
   - Inspects assistant output text in `response.chatResponse()`.
   - Redacts email patterns (`[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}`) to `"[REDACTED_EMAIL]"`.
   - Redacts US SSN patterns (`\b\d{3}-\d{2}-\d{4}\b`) to `"[REDACTED_SSN]"`.
   - Mutates `response` with a new `ChatResponse` containing the sanitized `AssistantMessage`.
   - `getOrder()` returns `180`.

8. **`OrderTrackingAdvisor` (Scenario 8)**:
   - Implements `CallAdvisor`.
   - Records an [`ExecutionOrderTrace`](src/main/java/phase04/AdvisorContracts.java#L78) for `"PRE_CALL"` before `chain.nextCall(request)` and for `"POST_CALL"` after.
   - Uses `getOrder()` to prove strict onion-model execution sequence.

9. **`createGuardedChatClient` (Scenario 9)**:
   - Helper factory method: `ChatClient.builder(model).defaultAdvisors(advisors).build()`.
   - Verifies both default and call-level `.advisors(...)` are invoked cleanly.

10. **`createProductionDiagnosticStack` (Scenario 10)**:
    - Assembles the complete multi-guardrail pipeline:
      `keywordAdvisor` $\to$ `policyAdvisor` $\to$ `tokenAdvisor` $\to$ `costAdvisor` $\to$ `piiAdvisor` $\to$ `auditAdvisor`.
    - Tests the full integrated invariant across multi-turn prompts.

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors` $\to$ `0 PASSED, 10 FAILED (exit code 99)`.
2. **Fix** [`GuardrailAdvisorsUnderTest.java`](src/main/java/phase04/GuardrailAdvisorsUnderTest.java).
3. **Verify** all 10 pass:
   `mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors` $\to$ `10 PASSED, 0 FAILED (exit code 0)`.
