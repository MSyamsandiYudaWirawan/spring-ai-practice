package phase03;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import phase03.ToolModelContracts.ToolCallEvent;
import phase03.ToolModelContracts.ToolEnvelope;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Seeded with defects across 8 scenarios in Multi-Turn Tool Calling & Execution Sandboxing:
 * - Scenario 1: Normal in-sandbox file reading with path normalization.
 * - Scenario 2: Path traversal containment (preventing ../ escapes).
 * - Scenario 3: Missing file handling & exception isolation (ToolEnvelope.fail).
 * - Scenario 4: File size safety guard (rejecting files > 100 KB).
 * - Scenario 5: Sandboxed repository file tree listing (filtering .git, .env, binary files).
 * - Scenario 6: Hard turn bound enforcement (cutting off model when callCount > bound).
 * - Scenario 7: Intermediate tool call trajectory auditing (ToolCallEvent logging).
 * - Scenario 8: Spring AI @Tool metadata and parameter annotation integration.
 */
public class BoundedDiagnosticToolsUnderTest {

    private final Path sandboxRoot;
    private final int bound;
    private final long maxFileSizeBytes;
    private final String fullContext;
    private final List<ToolCallEvent> trajectoryAudit = new CopyOnWriteArrayList<>();
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

    /**
     * Scenario 1 - 4: Read a file from the target repository sandbox.
     * <p>
     * Requirements:
     * - Must increment callCount on every invocation.
     * - If callCount > bound: cut off immediately with ToolEnvelope.fail("tool-call bound (" + bound + ") exceeded — decide now").
     * - If path is null or blank: return ToolEnvelope.fail("path must not be null or blank").
     * - Path traversal guard: reject paths with ".." or leading slashes, or resolved paths escaping sandboxRoot with ToolEnvelope.fail("Access denied: path escapes sandbox root").
     * - Sensitive files guard: reject paths containing ".git" or ".env" with ToolEnvelope.fail("Access denied: access to sensitive configuration is prohibited").
     * - Missing file guard: if file does not exist or is a directory, return ToolEnvelope.fail("File not found: " + path).
     * - Size guard: if file size > maxFileSizeBytes, return ToolEnvelope.fail("File size (" + size + " bytes) exceeds maximum allowable limit (" + maxFileSizeBytes + " bytes)").
     * - Success: read file string, record audit event, and return ToolEnvelope.ok(content).
     * - Never let IOException or other exceptions escape to caller!
     */
    @Tool(description = "Read a file from the target repository sandbox. Path must be relative to the sandbox root.")
    public ToolEnvelope<String> readSource(@ToolParam(description = "Relative path to the file inside the target sandbox") String path) {
        // DEFECT (Scenario 1-4): Returns null without reading, sandboxing, or recording audits.
        return null;
    }

    /**
     * Scenario 5: List all source and configuration files in the target repository structure.
     * <p>
     * Requirements:
     * - Increments callCount and enforces tool-call bound.
     * - Walks sandboxRoot recursively, returning relative paths separated by '/'.
     * - Filters out .git directories, .env files, and binary files (.bin, .class, .jar).
     * - Sorts paths alphabetically and returns ToolEnvelope.ok(files).
     */
    @Tool(description = "List all source and configuration files in the target repository structure.")
    public ToolEnvelope<List<String>> listRepositoryStructure() {
        // DEFECT (Scenario 5): Returns empty list without walking filesystem.
        return ToolEnvelope.ok(List.of());
    }

    /**
     * Scenario 6: Recall diagnostic context.
     * <p>
     * Requirements:
     * - Increments callCount and enforces tool-call bound.
     * - Returns ToolEnvelope.ok(fullContext).
     */
    @Tool(description = "Recall the diagnostic context (JFR signals, noise floor thresholds, metrics) for the current iteration.")
    public ToolEnvelope<String> recallContext() {
        // DEFECT (Scenario 6): Missing bound check.
        return ToolEnvelope.ok(fullContext);
    }

    public int getCallCount() {
        return callCount;
    }

    public List<ToolCallEvent> getTrajectoryAudit() {
        return Collections.unmodifiableList(trajectoryAudit);
    }

    public void resetTurn() {
        this.callCount = 0;
    }
}
