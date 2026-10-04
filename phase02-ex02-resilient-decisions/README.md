# Phase 02 Exercise 02 â€” Performance Diagnostic Decision Gateway & Resilient Schema Repair

**Theme:** Autonomous Performance Optimization Decision Engine & Polymorphic Schema Validation (8 Production Scenarios).  
**Diagnostician Mapping:** Directly mirrors the core guarantees of `DecisionValidator`, `DecisionDto`, `FallbackDecisionExtractor`, and `SpringAiDecideTurn` in the Saga's DECIDE phaseâ€”injecting format blueprints, handling discriminated change unions (`kind=edits` vs `kind=template`), extracting unstructured reasoning prose, and executing closed-loop one-shot repair with strict circuit-breakers.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase02/DecisionEngineUnderTest.java`](src/main/java/phase02/DecisionEngineUnderTest.java) | The service under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase02/DecisionModelContracts.java`](src/main/java/phase02/DecisionModelContracts.java) | Immutable domain records, JSON samples, and contracts (DO NOT MODIFY) |
| [`src/test/java/phase02/FakeDecisionChatModel.java`](src/test/java/phase02/FakeDecisionChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase02/Verifier.java`](src/test/java/phase02/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase02-ex02-golden.md`](golden/phase02-ex02-golden.md) | **SEALED** golden solution (open only after completion) |

**Time-box:** **55 minutes** for all 8 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or isolate an individual scenario):

```powershell
# Run all 8 scenarios:
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions

# Or via Maven test runner:
mvn test -pl phase02-ex02-resilient-decisions

# Run a specific scenario (1 through 8):
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="1"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="2"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="3"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="4"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="5"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="6"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="7"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="8"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. The 8 Scenarios & Gates

### Scenario 1: `generateFormatInstructions()`
- **Gate:** Uses `BeanOutputConverter<OptimizationDecision>.getFormat()` to generate dynamic schema format directives for prompt injection. Must be non-blank and contain `hypothesis`, `prediction`, `ledger`, and `change`.

### Scenario 2: `parseDecision(rawJson)`
- **Invariants:** `rawJson` must not be null or blank.
- **Gate:** Deserializes raw JSON into `OptimizationDecision` record using `BeanOutputConverter`.

### Scenario 3: `extractFromConversationalText(markdownPayload)`
- **Invariants:** `markdownPayload` must not be null or blank.
- **Gate:** Strips conversational preamble and markdown code fences (```` ```json ... ``` ````) before delegating to `decisionConverter`.

### Scenario 4: `validateCrossFieldRules(decision)`
- **Invariants:** `decision` must not be null.
- **Gate:** Enforces TigerStyle domain boundaries and discriminated union rules:
  - `hypothesis.category` must match `^H[1-7]$`; confidence in `[0.0, 1.0]`; non-blank rationale.
  - `prediction.metric` must be one of `{"p95", "median", "rps", "error_rate"}`; direction in `{"improve", "regress", "neutral"}`.
  - `ledger.hypothesisCategory` must match `^H[1-7]$`; direction in `{"strengthen", "weaken"}`.
  - Cross-field: if `change.kind` is `"edits"`, `edits` must not be null/empty, and `template` must be null/blank. If `change.kind` is `"template"`, `template` must be one of `{"jar-unpack", "hikari-pool", "virtual-threads"}` and `edits` must be null/empty. Any other `kind` throws `IllegalArgumentException`.

### Scenario 5: `parseDecisionBatch(jsonArrayPayload)`
- **Invariants:** `jsonArrayPayload` must not be null or blank.
- **Gate:** Deserializes JSON array into typed `List<OptimizationDecision>` using `new ParameterizedTypeReference<List<OptimizationDecision>>() {}`.

### Scenario 6: `fallbackExtractFromProse(proseText)`
- **Gate:** When model articulates reasoning in prose without JSON fences (mirrors `FallbackDecisionExtractor`), uses regex patterns to reconstruct an `OptimizationDecision`:
  - `hypothesis`: category `H1-H7`, confidence (default 0.8), rationale.
  - `prediction`: metric (default `"p95"`), direction fixed to `"improve"`, signal to eliminate (default `"JavaMonitorEnter"`).
  - `ledger`: category, direction (strengthen/weaken, default `"strengthen"`), reason.
  - `change`: `kind = "template"`, template name.
  Returns `null` on blank or unparseable input.

### Scenario 7: `repairDecisionWithFeedback(client, promptText)` (Saga DECIDE One-Shot Loop)
- **Invariants:** `client` and `promptText` must not be null or blank.
- **Gate:** When attempt 1 produces malformed JSON or invalid schema, catches the exception, quotes the failure message in a repair feedback prompt:
  `"Previous proposal failed validation: " + e.getMessage() + ". Please fix the error and return strictly valid JSON matching the schema."`
  Calls client a second time, parses, validates, and returns `DecisionEnvelope.accepted(repaired, 2)`.

### Scenario 8: `enforceStrictRepairCircuitBreaker(client, promptText)` (Retry Budget Circuit Breaker)
- **Invariants:** When both attempt 1 and attempt 2 fail, the service must NOT loop infinitely.
- **Gate:** Caps calls at exactly 2 (initial + 1 retry) and throws `IllegalStateException("Decision schema repair exceeded maximum attempts (1)")`.

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase02-ex02-resilient-decisions` $\to$ `0 PASSED, 8 FAILED (exit code 99)`.
2. **Fix** [`DecisionEngineUnderTest.java`](src/main/java/phase02/DecisionEngineUnderTest.java).
3. **Verify** all 8 pass:
   `mvn test-compile exec:java -pl phase02-ex02-resilient-decisions` $\to$ `8 PASSED, 0 FAILED (exit code 0)`.
