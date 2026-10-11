# Phase 03 Exercise 01 — Golden Reference Solution
## Autonomous Diagnostic Tool Sandboxing & Execution Bounding (15 Drills)

```java
package phase03;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import phase03.ToolModelContracts.ToolCallEvent;
import phase03.ToolModelContracts.ToolEnvelope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Service under test — Phase 03 Exercise 01:
 * Autonomous Diagnostic Tool Sandboxing & Execution Bounding (15 Scenarios).
 */
public class BoundedDiagnosticToolsUnderTest {

    private final Path sandboxRoot;
    private final int bound;
    private final long maxFileSizeBytes;
    private final String fullContext;
    private final List<ToolCallEvent> trajectoryAudit = new CopyOnWriteArrayList<>();
    private final List<String> recordedFindings = new CopyOnWriteArrayList<>();
    private int callCount = 0;

    public BoundedDiagnosticToolsUnderTest(Path sandboxRoot, int bound, String fullContext) {
        this(sandboxRoot, bound, ToolModelContracts.MAX_FILE_SIZE_BYTES, fullContext);
    }

    public BoundedDiagnosticToolsUnderTest(Path sandboxRoot, int bound, long maxFileSizeBytes, String fullContext) {
        this.sandboxRoot = Objects.requireNonNull(sandboxRoot, "sandboxRoot must not be null").toAbsolutePath().normalize();
        if (bound < 1) throw new IllegalArgumentException("bound must be >= 1, got " + bound);
        this.bound = bound;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.fullContext = fullContext;
    }

    private synchronized boolean checkBound(String action, Map<String, Object> params) {
        callCount++;
        if (callCount > bound) {
            String err = "tool-call bound (" + bound + ") exceeded — decide now";
            trajectoryAudit.add(new ToolCallEvent(callCount, action, params, false, err, System.currentTimeMillis()));
            return false;
        }
        return true;
    }

    private void recordAudit(String action, Map<String, Object> params, boolean ok, String detail) {
        trajectoryAudit.add(new ToolCallEvent(callCount, action, params, ok, detail, System.currentTimeMillis()));
    }

    @Tool(description = "Read a file from the target repository sandbox. Path must be relative to the sandbox root.")
    public ToolEnvelope<String> readSource(@ToolParam(description = "Relative path to the file inside the target sandbox") String path) {
        Map<String, Object> params = Map.of("path", path != null ? path : "null");
        if (!checkBound("readSource", params)) {
            return ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now");
        }

        if (path == null || path.isBlank()) {
            String err = "path must not be null or blank";
            recordAudit("readSource", params, false, err);
            return ToolEnvelope.fail(err);
        }

        if (path.contains(".git") || path.contains(".env")) {
            String err = "Access denied: access to sensitive configuration is prohibited";
            recordAudit("readSource", params, false, err);
            return ToolEnvelope.fail(err);
        }

        Path resolved = sandboxRoot.resolve(path).normalize();
        if (!resolved.startsWith(sandboxRoot)) {
            String err = "Access denied: path escapes sandbox root";
            recordAudit("readSource", params, false, err);
            return ToolEnvelope.fail(err);
        }

        try {
            if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
                String err = "File not found: " + path;
                recordAudit("readSource", params, false, err);
                return ToolEnvelope.fail(err);
            }

            long size = Files.size(resolved);
            if (size > maxFileSizeBytes) {
                String err = "File size (" + size + " bytes) exceeds maximum allowable limit (" + maxFileSizeBytes + " bytes)";
                recordAudit("readSource", params, false, err);
                return ToolEnvelope.fail(err);
            }

            String content = Files.readString(resolved);
            recordAudit("readSource", params, true, "Read " + size + " bytes");
            return ToolEnvelope.ok(content);
        } catch (IOException e) {
            String err = "File not found: " + path;
            recordAudit("readSource", params, false, err);
            return ToolEnvelope.fail(err);
        }
    }

    @Tool(description = "List all source and configuration files in the target repository structure.")
    public ToolEnvelope<List<String>> listRepositoryStructure() {
        Map<String, Object> params = Map.of();
        if (!checkBound("listRepositoryStructure", params)) {
            return ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now");
        }

        try (Stream<Path> stream = Files.walk(sandboxRoot)) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(p -> sandboxRoot.relativize(p).toString().replace('\\', '/'))
                    .filter(rel -> !rel.contains(".git") && !rel.contains(".env") &&
                            !rel.endsWith(".bin") && !rel.endsWith(".class") && !rel.endsWith(".jar"))
                    .sorted()
                    .toList();

            recordAudit("listRepositoryStructure", params, true, "Found " + files.size() + " files");
            return ToolEnvelope.ok(files);
        } catch (IOException e) {
            String err = "Failed to list repository structure: " + e.getMessage();
            recordAudit("listRepositoryStructure", params, false, err);
            return ToolEnvelope.fail(err);
        }
    }

    @Tool(description = "Recall the diagnostic context (JFR signals, noise floor thresholds, metrics) for the current iteration.")
    public ToolEnvelope<String> recallContext() {
        Map<String, Object> params = Map.of();
        if (!checkBound("recallContext", params)) {
            return ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now");
        }
        recordAudit("recallContext", params, true, "Recalled context");
        return ToolEnvelope.ok(fullContext);
    }

    @Tool(description = "Search source files for occurrences of a given query string.")
    public ToolEnvelope<List<String>> searchSource(@ToolParam(description = "Text query to search for across text files") String query) {
        Map<String, Object> params = Map.of("query", query != null ? query : "null");
        if (!checkBound("searchSource", params)) {
            return ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now");
        }

        if (query == null || query.isBlank()) {
            String err = "query must not be null or blank";
            recordAudit("searchSource", params, false, err);
            return ToolEnvelope.fail(err);
        }

        try (Stream<Path> stream = Files.walk(sandboxRoot)) {
            List<String> matches = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String rel = sandboxRoot.relativize(p).toString().replace('\\', '/');
                        return !rel.contains(".git") && !rel.contains(".env") &&
                                !rel.endsWith(".bin") && !rel.endsWith(".class") && !rel.endsWith(".jar");
                    })
                    .filter(p -> {
                        try {
                            if (Files.size(p) > maxFileSizeBytes) return false;
                            return Files.readString(p).contains(query);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(p -> sandboxRoot.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();

            recordAudit("searchSource", params, true, "Found " + matches.size() + " matches");
            return ToolEnvelope.ok(matches);
        } catch (IOException e) {
            String err = "Search error: " + e.getMessage();
            recordAudit("searchSource", params, false, err);
            return ToolEnvelope.fail(err);
        }
    }

    @Tool(description = "Record a diagnostic finding or hypothesis for the current investigation.")
    public ToolEnvelope<String> recordDiagnosticFinding(@ToolParam(description = "Diagnostic finding or root cause hypothesis") String finding) {
        Map<String, Object> params = Map.of("finding", finding != null ? finding : "null");
        if (!checkBound("recordDiagnosticFinding", params)) {
            return ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now");
        }

        if (finding == null || finding.isBlank()) {
            String err = "finding must not be null or blank";
            recordAudit("recordDiagnosticFinding", params, false, err);
            return ToolEnvelope.fail(err);
        }

        recordedFindings.add(finding);
        recordAudit("recordDiagnosticFinding", params, true, "Recorded: " + finding);
        return ToolEnvelope.ok("Recorded: " + finding);
    }

    public int getCallCount() {
        return callCount;
    }

    public int getRemainingTurns() {
        return Math.max(0, bound - callCount);
    }

    public List<String> getFindings() {
        return Collections.unmodifiableList(recordedFindings);
    }

    public List<ToolCallEvent> getTrajectoryAudit() {
        return Collections.unmodifiableList(trajectoryAudit);
    }

    public synchronized void resetTurn() {
        this.callCount = 0;
    }
}
```
