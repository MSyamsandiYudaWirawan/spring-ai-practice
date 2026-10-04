# Phase 03 Exercise 02 â€” Repetitive Spring AI @Tool Calling Drills

**Theme:** Cloud Operations & Autonomous Incident Remediation Toolset (10 High-Repetition Scenarios).  
**Goal:** Build cold muscle memory writing Spring AI `@Tool` and `@ToolParam` annotations, parameter shapes (primitives, numbers, enums, records), return types, validation boundaries, stateful rate-limiting, and ChatClient multi-tool registration.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase03/CloudOpsToolsUnderTest.java`](src/main/java/phase03/CloudOpsToolsUnderTest.java) | The service under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase03/OpsToolContracts.java`](src/main/java/phase03/OpsToolContracts.java) | Immutable domain records, enums, and envelopes (DO NOT MODIFY) |
| [`src/test/java/phase03/FakeOpsChatModel.java`](src/test/java/phase03/FakeOpsChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase03/Verifier.java`](src/test/java/phase03/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase03-ex02-golden.md`](golden/phase03-ex02-golden.md) | **SEALED** golden reference solution |

**Time-box:** **50 minutes** for all 10 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or isolate an individual scenario):

```powershell
# Run all 10 scenarios:
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills

# Or via Maven test runner:
mvn test -pl phase03-ex02-multi-tool-drills

# Run a specific scenario (1 through 10):
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=1"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=2"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=3"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=4"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=5"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=6"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=7"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=8"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=9"
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=10"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. The 10 Scenarios & Gates

1. **`restartPod(namespace, podName)`**: Multi-primitive parameters (`String`, `String`). Validates neither is null/blank (throws `IllegalArgumentException`). Returns `boolean`. Increments `callCount`.
2. **`scaleReplicas(deployment, replicas)`**: String + integer range. Validates replicas between `[1, 50]` (throws `IllegalArgumentException`). Returns strongly-typed [`DeploymentStatus`](src/main/java/phase03/OpsToolContracts.java#L23) record.
3. **`updateLogLevel(service, level)`**: Service string + [`LogLevel`](src/main/java/phase03/OpsToolContracts.java#L16) enum parameter. Returns [`ConfigUpdateResult`](src/main/java/phase03/OpsToolContracts.java#L34).
4. **`queryAuditLogs(filter)`**: Complex input schema using [`LogQueryFilter`](src/main/java/phase03/OpsToolContracts.java#L45) record. Returns strongly-typed `List<LogEntry>`.
5. **`executeHealthProbe(component)`**: Isolates unknown component errors into [`ToolEnvelope<HealthReport>`](src/main/java/phase03/OpsToolContracts.java#L69). Allowed components: `"api-gateway"`, `"auth-service"`, `"database"`, `"cache"`. Error envelope message MUST mention `"Unknown component"` (e.g. `"Unknown component: " + component`).
6. **`triggerDatabaseSnapshot(clusterId, retentionDays)`**: Validates regex `^[a-z0-9-]+$` and retention `[1..90]`. Returns `ToolEnvelope<String>`. Error envelope message MUST mention `"Invalid clusterId"` for regex failure (e.g. `"Invalid clusterId format: " + clusterId`), and MUST mention `"retentionDays"` for range failure (e.g. `"retentionDays must be between 1 and 90, got " + retentionDays`).
7. **`purgeCacheKeys(pattern)`**: Stateful rate-limiter. Allows at most 3 purges per session. Rejects 4th call with `ToolEnvelope.fail(...)` where the message MUST mention `"Rate limit exceeded"` (e.g. `"Rate limit exceeded: maximum 3 cache purges allowed per turn"`). Blank pattern fails with `"pattern must not be null or blank"`.
8. **`springAi_ReflectionDiscovery`**: Verifies that Spring AI's `ToolCallbacks.from(tools)` discovers all 7 `@Tool` methods with correct metadata, and executes callback dispatch cleanly.
9. **`chatClient_ToolRegistration`**: Tests configuring `ChatClient.builder().defaultTools(tools)` and verifying prompt dispatch.
10. **`trajectoryAudit_ComprehensiveLogging`**: Verifies that calling tools records a chronological [`ToolExecutionAudit`](src/main/java/phase03/OpsToolContracts.java#L84) trail (call index, tool name, arguments, success, timestamp).

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills` $\to$ `0 PASSED, 10 FAILED (exit code 99)`.
2. **Fix** [`CloudOpsToolsUnderTest.java`](src/main/java/phase03/CloudOpsToolsUnderTest.java).
3. **Verify** all 10 pass:
   `mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills` $\to$ `10 PASSED, 0 FAILED (exit code 0)`.
