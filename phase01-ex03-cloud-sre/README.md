# Phase 01 Exercise 03 â€” Cloud SRE & Kubernetes Remediation Gateway

**Theme:** Cloud SRE & Kubernetes Operations Incident Gateway (10 Production Scenarios).  
**Core Competency:** Complete TigerStyle defensive programming and Spring AI `ChatClient` muscle memory: fail-fast boundary validation, role separation, template binding with complex types, conditional environment policy injection, defensive token usage unboxing, parameterized client mutation, fine-grained LLM runtime options, few-shot demonstration priming, multi-turn history preservation, system paging envelopes, and structural LLM output invariant enforcement.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase01/SreOpsServiceUnderTest.java`](src/main/java/phase01/SreOpsServiceUnderTest.java) | The service under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase01/SreModelContracts.java`](src/main/java/phase01/SreModelContracts.java) | Immutable domain records, templates, policies, and contracts (DO NOT MODIFY) |
| [`src/test/java/phase01/FakeChatModel.java`](src/test/java/phase01/FakeChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase01/Verifier.java`](src/test/java/phase01/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase01-ex03-golden.md`](golden/phase01-ex03-golden.md) | Sealed golden solution (open only after completion) |

**Time-box:** **60 minutes** for all 10 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or isolate an individual scenario):

```powershell
# Run all 10 scenarios:
mvn test-compile exec:java -pl phase01-ex03-cloud-sre

# Or via Maven test runner:
mvn test -pl phase01-ex03-cloud-sre

# Run a specific scenario (1 through 10):
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="1"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="2"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="3"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="4"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="5"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="6"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="7"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="8"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="9"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="10"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. The 10 Scenarios & TigerStyle Gates

### Scenario 1: `analyzePodCrash(clusterId, namespace, podLogs)`
- **Invariants:** `clusterId`, `namespace`, and `podLogs` must not be null or blank (throw `IllegalArgumentException` / `NullPointerException`).
- **Gate:** `SystemMessage` must equal `"Cluster: " + clusterId + " | Namespace: " + namespace`; `UserMessage` must be pure `podLogs`.

### Scenario 2: `renderResourceExhaustionAlert(alert)`
- **Invariants:** `alert` must not be null.
- **Gate:** Binds `SreModelContracts.RESOURCE_ALERT_TEMPLATE` via `.user(u -> u.text(...).params(Map.of(...)))` with parameters `serviceName`, `cpuPercent`, `memoryBytes`, `p99LatencySec`, `errorCodes`. Placeholders must not remain un-substituted.

### Scenario 3: `triageWithEnvironmentPolicy(env, anomalyDescription)`
- **Invariants:** `env` and `anomalyDescription` must not be null or blank.
- **Gate:** If `env == PRODUCTION`, system prompt must be `SreModelContracts.PROD_POLICY`. Otherwise, system prompt must be `SreModelContracts.NON_PROD_POLICY`. User message must be `anomalyDescription`.

### Scenario 4: `executeIncidentDiagnosisWithAudit(incidentReport)`
- **Invariants:** `incidentReport` must not be null or blank.
- **Gate:** Extracts response text via `.call().chatResponse()`. Defensively extracts `promptTokens` and `completionTokens` without NPE when usage metadata is null (fallback to `0L`).

### Scenario 5: `deriveCanaryAnalysisClient(canaryTrafficPercent)`
- **Invariants:** `canaryTrafficPercent` must be between `0.0` and `100.0` inclusive (throw `IllegalArgumentException`).
- **Gate:** Uses `chatClient.mutate()` to derive an isolated client with parameterized `CANARY_SYSTEM_TEMPLATE` (binding `trafficPercent`) and `temperature = 0.1`.

### Scenario 6: `generateDeterministicRunbook(alertName)`
- **Invariants:** `alertName` must not be null or blank.
- **Gate:** Attaches call-level `ChatOptions`:
  - `temperature = DETERMINISTIC_TEMPERATURE` (0.0)
  - `maxTokens = RUNBOOK_MAX_TOKENS` (300)
  - `stopSequences = RUNBOOK_STOP_SEQUENCES` (`["END_RUNBOOK", "---"]`)
  - `frequencyPenalty = RUNBOOK_FREQUENCY_PENALTY` (0.5)

### Scenario 7: `classifyRemediationAction(failureSymptom)`
- **Invariants:** `failureSymptom` must not be null or blank.
- **Gate:** Primes `REMEDIATION_FEW_SHOTS` as alternating `UserMessage` and `AssistantMessage` instances in `.messages(...)`, followed by `failureSymptom` as the final user query.

### Scenario 8: `continueTroubleshootingSession(history, userReply)`
- **Invariants:** `history` and `userReply` must not be null or blank.
- **Gate:** Preserves full multi-turn conversational context by passing `.messages(history)` and appending `userReply` via `.user(userReply)`.

### Scenario 9: `applyIncidentPagingEnvelope(onCallEngineer, incidentQuery)`
- **Invariants:** `onCallEngineer` and `incidentQuery` must not be null or blank.
- **Gate:** System prompt is augmented with paging header: `"ON-CALL DISPATCH TO: {engineer}\n" + BASE_SRE_SYSTEM_PROMPT` using parameter binding for `{engineer}`. User message is pure `incidentQuery`.

### Scenario 10: `safeExecuteDiagnosis(query)`
- **Invariants:** `query` must not be null or blank.
- **Gate:** TigerStyle fail-fast on LLM output: if the response, result, output, or text is null/blank, throw `IllegalStateException("SRE diagnosis failed: empty or malformed LLM response")`. Otherwise return `text.trim()`.

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase01-ex03-cloud-sre` $\to$ `0 PASSED, 10 FAILED (exit code 99)`.
2. **Fix** [`SreOpsServiceUnderTest.java`](src/main/java/phase01/SreOpsServiceUnderTest.java).
3. **Verify** all 10 pass:
   `mvn test-compile exec:java -pl phase01-ex03-cloud-sre` $\to$ `10 PASSED, 0 FAILED (exit code 0)`.
