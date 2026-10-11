# Phase 03 Exercise 02 — Repetitive Spring AI @Tool Calling Drills

**Theme:** Cloud Operations & Autonomous Incident Remediation Toolset (**15 High-Repetition Muscle-Memory Drills**).  
**Goal:** Build cold muscle memory writing Spring AI `@Tool` and `@ToolParam` annotations, parameter shapes (primitives, numbers, enums, records, collections), return types, validation boundaries, stateful rate-limiting, and ChatClient multi-tool registration.

---

## The 15 Drills & Exact Contracts

| # | Topic | Drill / Component | Parameter / Return Signature | Validation & Contract Invariants |
|---|---|---|---|---|
| 01 | T1 | `restartPod` | `(String namespace, String podName) -> boolean` | Non-null, non-blank strings (`IllegalArgumentException`). Returns `true`. Increments `callCount`. |
| 02 | T1 | `scaleReplicas` | `(String deployment, int replicas) -> DeploymentStatus` | Validates `replicas` in `[1, 50]` (`IllegalArgumentException`). Returns typed `DeploymentStatus`. |
| 03 | T1 | `drainNode` | `(String nodeName, int gracePeriodSeconds) -> NodeDrainResult` | Validates non-blank `nodeName` and `gracePeriodSeconds` in `[0, 3600]` (`IllegalArgumentException`). Returns typed `NodeDrainResult`. |
| 04 | T2 | `updateLogLevel` | `(String service, LogLevel level) -> ConfigUpdateResult` | Service non-blank and level non-null (`IllegalArgumentException`). Returns `ConfigUpdateResult`. |
| 05 | T2 | `queryAuditLogs` | `(LogQueryFilter filter) -> List<LogEntry>` | Validates non-null filter, non-blank service, `maxEntries >= 1`. Returns `List<LogEntry>`. |
| 06 | T2 | `batchRestartPods` | `(String namespace, List<String> podNames) -> Map<String, Boolean>` | Validates non-blank namespace and non-empty podNames. Returns mapping `podName -> true`. |
| 07 | T3 | `executeHealthProbe` | `(String component) -> ToolEnvelope<HealthReport>` | Allowed components: `"api-gateway"`, `"auth-service"`, `"database"`, `"cache"`. Unknown returns `ToolEnvelope.fail(...)` containing `"Unknown component"`. |
| 08 | T3 | `triggerDatabaseSnapshot` | `(String clusterId, int retentionDays) -> ToolEnvelope<String>` | Regex `^[a-z0-9-]+$` (`"Invalid clusterId"`) and retention `[1..90]` (`"retentionDays"`). Returns `ToolEnvelope<String>`. |
| 09 | T3 | `rollbackDeployment` | `(String deployment, int targetRevision) -> ToolEnvelope<String>` | Validates non-blank deployment and `targetRevision >= 1` (`"Invalid revision"`). Returns `ToolEnvelope<String>`. |
| 10 | T4 | `purgeCacheKeys` | `(String pattern) -> ToolEnvelope<Integer>` | Stateful rate limiter. Max 3 purges per session. 4th returns `ToolEnvelope.fail(...)` with `"Rate limit exceeded"`. |
| 11 | T4 | `resetSession` | `() -> void` | Resets `callCount = 0`, `purgeCount = 0`, and clears audits, restoring session state for subsequent operations. |
| 12 | T4 | `getExecutionAudits` | `() -> List<ToolExecutionAudit>` | Forensic trajectory audit log capturing sequence index, tool name, argument map, success flag, and timestamps for all attempts. |
| 13 | T5 | `springAi_ReflectionDiscovery` | `ToolCallbacks.from(tools)` | Discovers at least 8 `@Tool` methods with parameter metadata and tool definitions. |
| 14 | T5 | `springAi_ToolCallbackExecution` | `ToolCallback.call(json)` | Dispatches JSON to tool callback and verifies typed record serialization in JSON output. |
| 15 | T5 | `springAi_ChatClientMultiToolIntegration` | `ChatClient.defaultTools(tools)` | Builds ChatClient with multi-tool suite and executes conversation prompt. |

---

## Tiered Progressive Hints in Verifier
Every failing scenario displays clear tiered hints:
- `DETAIL`: Exact mismatch or exception message.
- `[HINT 1 (Concept)]`: The core Spring AI concept or tool signature design.
- `[HINT 2 (Edge Case)]`: Specific input validation, boundaries, or enum/envelope requirements.
- `[HINT 3 (Code Pattern)]`: Annotation syntax (`@Tool`, `@ToolParam`), record creation, regex matching.

---

## Verification Commands

To run all 15 scenario gates:
```powershell
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills
```

To run a single scenario (e.g. Scenario 10):
```powershell
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills "-Dexec.args=10"
```

To run via Maven test runner:
```powershell
mvn test -pl phase03-ex02-multi-tool-drills
```
