# Phase 03 Exercise 01 — Autonomous Diagnostic Tool Sandboxing & Execution Bounding

**Theme:** Multi-Turn Tool Calling (`@Tool`), Path Traversal Sandboxing, Turn Quotas, Trajectory Auditing, and Spring AI Dynamic Dispatch (**15 Muscle-Memory Drills**).  
**Architectural Principle:** Enforces: *"AI proposes, pipeline disposes. Model only reads; loop applies."*

---

## The 15 Drills & Exact Contracts

| # | Topic | Drill / Component | Core Contract & Boundary Invariants |
|---|---|---|---|
| 01 | T1 | `readSource(path)` Normal File Read | Reads configuration/code file inside `sandboxRoot`. Returns `ToolEnvelope.ok(content)`. Increments `callCount`. |
| 02 | T1 | `readSource(path)` Path Traversal Isolation | Rejects `../../etc/passwd`, absolute paths, escaping traversal with `ToolEnvelope.fail("Access denied: path escapes sandbox root")`. No uncaught exceptions. |
| 03 | T1 | `readSource(path)` Missing File & Blank Path | Returns `ToolEnvelope.fail("path must not be null or blank")` on null/blank; returns `ToolEnvelope.fail("File not found: " + path)` on missing file. |
| 04 | T2 | `readSource(path)` Oversized File Guard | Files exceeding `maxFileSizeBytes` (100 KB) rejected with `ToolEnvelope.fail(...)` containing `"exceeds maximum allowable limit"`. |
| 05 | T2 | `readSource(path)` Sensitive File Quarantine | Rejects paths containing `.git` or `.env` with `ToolEnvelope.fail("Access denied: access to sensitive configuration is prohibited")`. |
| 06 | T2 | `listRepositoryStructure()` Safe Tree Listing | Recursively walks `sandboxRoot`. Returns sorted relative paths separated by `/`. Filters out `.git`, `.env`, `.bin`, `.class`, `.jar`. |
| 07 | T3 | `BoundedToolExecution` Turn Bound Cut-Off | When `callCount > bound`, immediately cuts off model with `ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now")`. |
| 08 | T3 | `recallContext()` Turn-Enforced Context Recall | Recalls full diagnostic context string, incrementing `callCount` and respecting turn bound. |
| 09 | T3 | `resetTurn()` Multi-Phase Turn Reset | Resets `callCount` to 0, enabling fresh bounded turns across diagnostic phases. |
| 10 | T4 | `searchSource(query)` Sandboxed Text Search | Searches non-binary files inside sandbox matching query string. Returns `ToolEnvelope.ok(List<String>)`. Rejects blank queries. |
| 11 | T4 | `recordDiagnosticFinding(finding)` | Validates non-blank hypothesis; persists to internal findings list; returns `ToolEnvelope.ok("Recorded: " + finding)`. |
| 12 | T4 | `trajectoryAudit` Event Logging | Every tool call (successful, rejected, or cut-off) recorded in `ToolCallEvent` audit trail with sequence, action, parameters, and outcome. |
| 13 | T5 | `springAi_ToolCallbackDiscovery` | Verifies `ToolCallbacks.from(tools)` discovers all 5 `@Tool` methods with descriptions and `@ToolParam` metadata. |
| 14 | T5 | `springAi_ToolCallbackExecution` | Verifies dynamic JSON dispatch via `ToolCallback.call("{\"path\":\"pom.xml\"}")` producing valid serialized `ToolEnvelope` JSON. |
| 15 | T5 | `springAi_ChatClientIntegration` | Configures `ChatClient.builder(chatModel).defaultTools(tools).build()` and executes call verifying seamless tool integration. |

---

## Tiered Progressive Hints in Verifier
Every failing scenario displays clear tiered hints:
- `DETAIL`: Exact mismatch or exception message.
- `[HINT 1 (Concept)]`: The core Spring AI concept or sandboxing principle.
- `[HINT 2 (Edge Case)]`: Specific edge conditions, normalization, or envelope error keys.
- `[HINT 3 (Code Pattern)]`: Structural code patterns (`Files.walk`, `startsWith(sandboxRoot)`, `@ToolParam`).

---

## Verification Commands

To run all 15 scenario gates:
```powershell
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing
```

To run a single scenario (e.g. Scenario 10):
```powershell
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=10"
```

To run via Maven test runner:
```powershell
mvn test -pl phase03-ex01-tool-sandboxing
```
