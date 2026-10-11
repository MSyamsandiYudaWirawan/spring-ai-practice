package phase03;

import phase03.ToolModelContracts.ToolCallEvent;
import phase03.ToolModelContracts.ToolEnvelope;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service under test — Phase 03 Exercise 01:
 * Autonomous Diagnostic Tool Sandboxing & Execution Bounding (15 Scenarios).
 * <p>
 * PRACTICE MODE: All drills stubbed. Implement each scenario to pass Verifier.
 * Remember to annotate diagnostic methods with @Tool and @ToolParam!
 * Golden reference solution is sealed in: docs/golden/phase03-ex01-golden.md
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

    /**
     * Drills 1 - 5: Read a file from the target repository sandbox.
     * TODO: Add @Tool and @ToolParam annotations.
     */
    public ToolEnvelope<String> readSource(String path) {
        // TODO: Enforce turn bound, validate path, sandbox root containment, sensitive files, file size, and return content
        return null;
    }

    /**
     * Drill 6: List all source and configuration files in the target repository structure.
     * TODO: Add @Tool annotation.
     */
    public ToolEnvelope<List<String>> listRepositoryStructure() {
        // TODO: Enforce turn bound, walk sandboxRoot, filter out .git, .env, and binary files, sort alphabetically
        return null;
    }

    /**
     * Drill 8: Recall diagnostic context.
     * TODO: Add @Tool annotation.
     */
    public ToolEnvelope<String> recallContext() {
        // TODO: Enforce turn bound, return ToolEnvelope.ok(fullContext)
        return null;
    }

    /**
     * Drill 10: Search source files for occurrences of a given query string.
     * TODO: Add @Tool and @ToolParam annotations.
     */
    public ToolEnvelope<List<String>> searchSource(String query) {
        // TODO: Enforce turn bound, search non-binary files inside sandbox matching query
        return null;
    }

    /**
     * Drill 11: Record a diagnostic finding or hypothesis for the current investigation.
     * TODO: Add @Tool and @ToolParam annotations.
     */
    public ToolEnvelope<String> recordDiagnosticFinding(String finding) {
        // TODO: Enforce turn bound, validate non-blank finding, record in list, return confirmation
        return null;
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

    public void resetTurn() {
        // TODO: Reset callCount to 0
    }
}
