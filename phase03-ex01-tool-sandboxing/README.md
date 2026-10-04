# Phase 03 Exercise 01 â€” Autonomous Diagnostic Tool Sandboxing & Execution Bounding

**Theme:** Multi-Turn Tool Calling (`@Tool`), Path Traversal Sandboxing, Exception Isolation, and Turn-Scoped Tool Bounding (8 Production Scenarios).  
**Diagnostician Mapping:** Directly implements [`BoundedReadSource`](https://github.com/syamsandi/agentic-performance-diagnostician/agent-core/src/main/java/io/diag/agent/loop/BoundedReadSource.java) from [`agentic-performance-diagnostician`](https://github.com/syamsandi/agentic-performance-diagnostician) (Step 8/9). Enforces: *"AI proposes, pipeline disposes. Model only reads; loop applies."*

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase03/BoundedDiagnosticToolsUnderTest.java`](src/main/java/phase03/BoundedDiagnosticToolsUnderTest.java) | The service under test â€” **FIX THIS FILE ONLY** |
| [`src/main/java/phase03/ToolModelContracts.java`](src/main/java/phase03/ToolModelContracts.java) | Immutable domain records, fixtures, and contracts (DO NOT MODIFY) |
| [`src/test/java/phase03/FakeToolChatModel.java`](src/test/java/phase03/FakeToolChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase03/Verifier.java`](src/test/java/phase03/Verifier.java) | The evaluation gate (exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase03-ex01-golden.md`](golden/phase03-ex01-golden.md) | **SEALED** golden reference solution |

**Time-box:** **55 minutes** for all 8 scenarios + write-up.

---

## 2. Reproduction Commands

Run the verifier across all scenarios (or isolate an individual scenario):

```powershell
# Run all 8 scenarios:
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing

# Or via Maven test runner:
mvn test -pl phase03-ex01-tool-sandboxing

# Run a specific scenario (1 through 8):
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=1"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=2"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=3"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=4"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=5"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=6"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=7"
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing "-Dexec.args=8"
```

Exit `99` = measured FAIL (findings detected). Read the report.  
Exit `0` = PASS.

---

## 3. The 8 Scenarios & Gates

### Scenario 1: `readSource(path)` â€” Normal File Reading
- **Gate:** Reads a valid configuration or source file relative to `sandboxRoot` (e.g. `src/main/resources/application.properties`). Returns `ToolEnvelope.ok(content)`. Increments `callCount`.

### Scenario 2: `readSource(path)` â€” Path Traversal Sandboxing
- **Gate:** Detects attempts to break out of the repository sandbox (`../../etc/passwd`, absolute paths, or escaping traversal). Rejects with `ToolEnvelope.fail("Access denied: path escapes sandbox root")`. Zero exceptions escape to the caller.

### Scenario 3: `readSource(path)` â€” Missing File & Exception Isolation
- **Gate:** When reading a non-existent file, catches `IOException`/`NoSuchFileException` and returns `ToolEnvelope.fail("File not found: " + path)`. Protects the LLM conversation loop from terminating.

### Scenario 4: `readSource(path)` â€” Maximum File Size Guard (100 KB Cap)
- **Gate:** Checks file size before reading. Rejects files $> 100\text{ KB}$ (such as `logs/large_heap_dump.bin`) with `ToolEnvelope.fail("File size ... exceeds maximum allowable limit (102400 bytes)")`.

### Scenario 5: `listRepositoryStructure()` â€” Safe Repository Listing
- **Gate:** Recursively walks the sandbox, returning relative file paths (`ToolEnvelope.ok(List<String>)`). Filters out internal `.git/` directories, `.env` credentials, and binary `.class`/`.bin` artifacts.

### Scenario 6: `BoundedToolExecution` â€” Hard Turn Bound Cut-Off
- **Gate:** Wraps a per-turn counter. When `callCount > bound` (e.g. bound = 3), subsequent tool calls are cut off immediately with `ToolEnvelope.fail("tool-call bound (3) exceeded â€” decide now")`. The model is cut off, not paid for.

### Scenario 7: `trajectoryAudit` â€” Tool Call Event Logging
- **Gate:** Logs every tool execution (both successful and failed/blocked calls) into `trajectoryAudit` as a `ToolCallEvent` (sequence number, tool name, parameters, status, and detail).

### Scenario 8: `springAi_ToolCallbackIntegration` â€” Spring AI `@Tool` Discovery
- **Gate:** Ensures method annotations (`@Tool`, `@ToolParam`) and signatures integrate seamlessly with Spring AI's `ToolCallbacks.from(service)` dynamic reflection dispatcher.

---

## 4. Your Task

1. **Reproduce** the initial failure:
   `mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing` $\to$ `0 PASSED, 8 FAILED (exit code 99)`.
2. **Fix** [`BoundedDiagnosticToolsUnderTest.java`](src/main/java/phase03/BoundedDiagnosticToolsUnderTest.java).
3. **Verify** all 8 pass:
   `mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing` $\to$ `8 PASSED, 0 FAILED (exit code 0)`.
