# Phase 02 Exercise 01 â€” Structured Outputs & Resilient Schema Extraction

**Theme:** Autonomous Security Vulnerability Assessment & Patch Proposal Gateway (8 Production Scenarios).  
**Diagnostician Mapping:** Directly mirrors the core guarantees of `DecisionValidator`, `DecisionDto`, and `SpringAiDecideTurn` in the Saga's DECIDE phaseâ€”stripping conversational markdown fences, handling malformed syntax, enforcing domain record invariants, and executing the one-shot feedback repair loop.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase02/SecurityTriageServiceUnderTest.java`](src/main/java/phase02/SecurityTriageServiceUnderTest.java) | The service under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase02/SecurityModelContracts.java`](src/main/java/phase02/SecurityModelContracts.java) | Immutable domain records, JSON samples, and contracts (DO NOT MODIFY) |
| [`src/test/java/phase02/FakeSecurityChatModel.java`](src/test/java/phase02/FakeSecurityChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase02/Verifier.java`](src/test/java/phase02/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase02-ex01-golden.md`](golden/phase02-ex01-golden.md) | **SEALED** golden solution (open only after completion) |

**Time-box:** **55 minutes** for all 8 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or isolate an individual scenario):

```powershell
# Run all 8 scenarios:
mvn test-compile exec:java -pl phase02-ex01-structured-outputs

# Or via Maven test runner:
mvn test -pl phase02-ex01-structured-outputs

# Run a specific scenario (1 through 8):
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="1"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="2"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="3"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="4"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="5"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="6"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="7"
mvn test-compile exec:java -pl phase02-ex01-structured-outputs -Dexec.args="8"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. The 8 Scenarios & Gates

### Scenario 1: `parseVulnerabilityAssessment(rawJson)`
- **Invariants:** `rawJson` must not be null or blank.
- **Gate:** Direct deserialization of pure JSON string into `VulnerabilityAssessment` record using `BeanOutputConverter`.

### Scenario 2: `extractFromMarkdownFences(markdownPayload)`
- **Invariants:** `markdownPayload` must not be null or blank.
- **Gate:** Strips conversational preamble and markdown code fences (```` ```json ... ``` ````) or extracts JSON substring from `{` to `}` before parsing.

### Scenario 3: `extractPatchPlanList(jsonArrayPayload)`
- **Invariants:** `jsonArrayPayload` must not be null or blank.
- **Gate:** Uses `new ParameterizedTypeReference<List<PatchPlan>>() {}` with `BeanOutputConverter` to deserialize a JSON array into `List<PatchPlan>`.

### Scenario 4: `generateAssessmentWithSchema(client, advisoryText)`
- **Invariants:** `client` and `advisoryText` must not be null or blank.
- **Gate:** Dispatches via `client.prompt().user(advisoryText).call().entity(VulnerabilityAssessment.class)` returning a typed record.

### Scenario 5: `validateRecordInvariants(assessment)`
- **Invariants:** `assessment` must not be null.
- **Gate:** Enforces TigerStyle domain boundaries: `cvssScore` within `[0.0, 10.0]`, `packageCoordinate` containing `:`, and non-empty `mitigationSteps`.

### Scenario 6: `safeDeserializeWithFallback(jsonPayload)`
- **Invariants:** `jsonPayload` must not be null or blank.
- **Gate:** Never lets Jackson or validation exceptions escape. Returns `TriageEnvelope.accepted(data)` on valid JSON, and `TriageEnvelope.rejected(errorReason)` on corrupted JSON.

### Scenario 7: `repairWithOneShotRetry(client, cveIdentifier)` (Saga DECIDE One-Shot Loop)
- **Invariants:** `client` and `cveIdentifier` must not be null or blank.
- **Gate:** When attempt 1 produces malformed JSON, quotes the error back to the model in a feedback prompt:
  `"Previous output failed schema validation: " + e.getMessage() + ". Please output strictly valid JSON conforming to schema:"`
  Then parses the second response and returns the repaired `VulnerabilityAssessment`. Verifies exactly 2 model calls are made.

### Scenario 8: `enforceRetryLimit(client, cveIdentifier)` (Hard Stop on Repair Retries)
- **Invariants:** When both attempt 1 and attempt 2 produce invalid JSON, the service must NOT loop indefinitely.
- **Gate:** Caps retries at exactly 1 (2 calls total), and throws `IllegalStateException("Schema repair exceeded maximum attempts (1)")`.

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase02-ex01-structured-outputs` $\to$ `0 PASSED, 8 FAILED (exit code 99)`.
2. **Fix** [`SecurityTriageServiceUnderTest.java`](src/main/java/phase02/SecurityTriageServiceUnderTest.java).
3. **Verify** all 8 pass:
   `mvn test-compile exec:java -pl phase02-ex01-structured-outputs` $\to$ `8 PASSED, 0 FAILED (exit code 0)`.
