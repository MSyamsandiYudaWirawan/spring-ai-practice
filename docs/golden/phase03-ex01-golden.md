# Phase 03 Exercise 01 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `readSource` (Normal In-Sandbox File Inspection)

### Root Cause
Tools exposed to LLMs must be strictly scoped to the repository sandbox. Missing boundary checks or failing to normalize paths causes improper file reads or throws unexpected IO exceptions.

### TigerStyle Fix
```java
@Tool(description = "Read a file from the target repository sandbox. Path must be relative to the sandbox root.")
public ToolEnvelope<String> readSource(@ToolParam(description = "Relative path to the file inside the target sandbox") String path) {
    callCount++;

    if (callCount > bound) {
        String msg = "tool-call bound (" + bound + ") exceeded — decide now";
        recordAudit("readSource", Map.of("path", path == null ? "" : path), false, msg);
        return ToolEnvelope.fail(msg);
    }

    if (path == null || path.isBlank()) {
        recordAudit("readSource", Map.of(), false, "path must not be null or blank");
        return ToolEnvelope.fail("path must not be null or blank");
    }

    if (path.contains("..") || path.startsWith("/") || path.startsWith("\\") || Path.of(path).isAbsolute()) {
        String msg = "Access denied: path escapes sandbox root";
        recordAudit("readSource", Map.of("path", path), false, msg);
        return ToolEnvelope.fail(msg);
    }

    Path resolved = sandboxRoot.resolve(path).normalize();
    if (!resolved.startsWith(sandboxRoot)) {
        String msg = "Access denied: path escapes sandbox root";
        recordAudit("readSource", Map.of("path", path), false, msg);
        return ToolEnvelope.fail(msg);
    }

    if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
        String msg = "File not found: " + path;
        recordAudit("readSource", Map.of("path", path), false, msg);
        return ToolEnvelope.fail(msg);
    }

    try {
        long size = Files.size(resolved);
        if (size > maxFileSizeBytes) {
            String msg = "File size (" + size + " bytes) exceeds maximum allowable limit (" + maxFileSizeBytes + " bytes)";
            recordAudit("readSource", Map.of("path", path), false, msg);
            return ToolEnvelope.fail(msg);
        }

        String content = Files.readString(resolved);
        recordAudit("readSource", Map.of("path", path), true, "Read " + content.length() + " chars");
        return ToolEnvelope.ok(content);
    } catch (Exception e) {
        String msg = "Error reading source: " + e.getMessage();
        recordAudit("readSource", Map.of("path", path), false, msg);
        return ToolEnvelope.fail(msg);
    }
}
```

---

## 2. Scenario 2: `readSource` (Path Traversal Sandboxing)

### Root Cause
LLMs can be prompted (or accidentally hallucinate) to access files outside the sandbox using directory traversal patterns like `../../etc/passwd` or `..\..\secret.env`. Rejecting paths with `..`, leading slashes, or resolved paths that do not start with `sandboxRoot` enforces complete filesystem containment.

### TigerStyle Fix
```java
if (path.contains("..") || path.startsWith("/") || path.startsWith("\\") || Path.of(path).isAbsolute()) {
    String msg = "Access denied: path escapes sandbox root";
    recordAudit("readSource", Map.of("path", path), false, msg);
    return ToolEnvelope.fail(msg);
}

Path resolved = sandboxRoot.resolve(path).normalize();
if (!resolved.startsWith(sandboxRoot)) {
    String msg = "Access denied: path escapes sandbox root";
    recordAudit("readSource", Map.of("path", path), false, msg);
    return ToolEnvelope.fail(msg);
}
```

---

## 3. Scenario 3: `readSource` (Missing File & Exception Isolation)

### Root Cause
In agentic loops, file-not-found errors must never crash the thread. Instead of throwing `NoSuchFileException`, wrapping the error into `ToolEnvelope.fail("File not found: " + path)` allows the model to inspect another file or adjust its hypothesis cleanly.

### TigerStyle Fix
```java
if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
    String msg = "File not found: " + path;
    recordAudit("readSource", Map.of("path", path), false, msg);
    return ToolEnvelope.fail(msg);
}
```

---

## 4. Scenario 4: `readSource` (Maximum File Size Guard - 100 KB Cap)

### Root Cause
Loading multi-megabyte heap dumps or log files directly into prompt context triggers token blowup or out-of-memory errors. The 100 KB cap (`ToolModelContracts.MAX_FILE_SIZE_BYTES`) protects against runaway context consumption.

### TigerStyle Fix
```java
long size = Files.size(resolved);
if (size > maxFileSizeBytes) {
    String msg = "File size (" + size + " bytes) exceeds maximum allowable limit (" + maxFileSizeBytes + " bytes)";
    recordAudit("readSource", Map.of("path", path), false, msg);
    return ToolEnvelope.fail(msg);
}
```

---

## 5. Scenario 5: `listRepositoryStructure` (Safe Sandboxed Repository Listing)

### Root Cause
Listing repository structure should give the AI visibility into source and config files while filtering out internal build artifacts (`.class`, `.jar`), binary files, and sensitive directories (`.git/`, `.env`).

### TigerStyle Fix
```java
@Tool(description = "List all source and configuration files in the target repository structure.")
public ToolEnvelope<List<String>> listRepositoryStructure() {
    callCount++;

    if (callCount > bound) {
        String msg = "tool-call bound (" + bound + ") exceeded — decide now";
        recordAudit("listRepositoryStructure", Map.of(), false, msg);
        return ToolEnvelope.fail(msg);
    }

    try (Stream<Path> stream = Files.walk(sandboxRoot)) {
        List<String> files = stream
                .filter(Files::isRegularFile)
                .map(sandboxRoot::relativize)
                .map(p -> p.toString().replace('\\', '/'))
                .filter(p -> !p.contains(".git") && !p.contains(".env") && !p.endsWith(".bin") && !p.endsWith(".class") && !p.endsWith(".jar"))
                .sorted()
                .toList();

        recordAudit("listRepositoryStructure", Map.of(), true, "Listed " + files.size() + " files");
        return ToolEnvelope.ok(files);
    } catch (Exception e) {
        String msg = "Error listing repository structure: " + e.getMessage();
        recordAudit("listRepositoryStructure", Map.of(), false, msg);
        return ToolEnvelope.fail(msg);
    }
}
```

---

## 6. Scenario 6: `BoundedToolExecution` (Hard Turn Bound Cut-Off)

### Root Cause
Without a hard turn bound, a looping model will call tools indefinitely. Once `callCount > bound`, subsequent tool calls are cut off immediately with `"tool-call bound (" + bound + ") exceeded — decide now"`.

### TigerStyle Fix
```java
callCount++;

if (callCount > bound) {
    String msg = "tool-call bound (" + bound + ") exceeded — decide now";
    recordAudit(actionName, params, false, msg);
    return ToolEnvelope.fail(msg);
}
```

---

## 7. Scenario 7: `trajectoryAudit` (Tool Call Event Logging)

### Root Cause
For agent evaluation and benchmark reproducibility, all tool executions (including failed or bounded calls) must be logged with sequence index, tool name, parameters, outcome status, and timestamps.

### TigerStyle Fix
```java
private void recordAudit(String action, Map<String, Object> params, boolean ok, String detail) {
    trajectoryAudit.add(new ToolCallEvent(callCount, action, params, ok, detail, System.currentTimeMillis()));
}
```

---

## 8. Scenario 8: `springAi_ToolCallbackIntegration` (Spring AI `@Tool` Resolution)

### Root Cause
Spring AI uses `ToolCallbacks.from(service)` to discover `@Tool` methods via reflection, building `ToolDefinition` metadata and callable proxies. Ensuring parameter types and return types match Spring AI converter expectations guarantees seamless LLM execution.
