package phase08.saga;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 07: In-Memory Git-like Sandboxed Workspace.
 * <p>
 * Supports atomic commits, rollback to last kept SHA, and file mutations.
 */
public class VirtualWorkspace {

    private String headSha;
    private final Map<String, String> files = new HashMap<>();
    private final Map<String, Map<String, String>> commitHistory = new HashMap<>();

    /**
     * Initializes workspace at initial commit SHA with starting files.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If initialSha is null or blank, throw IllegalArgumentException("initialSha cannot be blank").
     * - State Setup:
     *   - Set headSha = initialSha.
     *   - If initialFiles is not null, copy into files map and record snapshot in commitHistory under initialSha.
     *   - If initialFiles is null, record empty snapshot in commitHistory under initialSha.
     */
    public VirtualWorkspace(String initialSha, Map<String, String> initialFiles) {
        if (initialSha == null || initialSha.isBlank()) {
            throw new IllegalArgumentException("initialSha cannot be blank");
        }
        this.headSha = initialSha;
        if (initialFiles != null) {
            this.files.putAll(initialFiles);
            this.commitHistory.put(initialSha, new HashMap<>(initialFiles));
        } else {
            this.commitHistory.put(initialSha, new HashMap<>());
        }
    }

    /**
     * Commits a new revision and updates HEAD.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If sha is null or blank, throw IllegalArgumentException("Commit sha cannot be blank").
     * - State Update (synchronized):
     *   - Set headSha = sha.
     *   - If newFiles != null, clear current files map and copy all newFiles into files.
     *   - Store a copy of files in commitHistory under sha.
     */
    public synchronized void commit(String sha, Map<String, String> newFiles) {
        throw new UnsupportedOperationException("TODO: Implement commit");
    }

    /**
     * Rolls back workspace files to a specific commit SHA.
     * <p>
     * Instructions:
     * - Validation:
     *   - If commitHistory does not contain targetSha, throw IllegalArgumentException("Cannot revert: Unknown SHA " + targetSha).
     * - State Reversion (synchronized):
     *   - Set headSha = targetSha.
     *   - Clear current files map and restore from commitHistory snapshot for targetSha.
     */
    public synchronized void revertTo(String targetSha) {
        throw new UnsupportedOperationException("TODO: Implement revertTo");
    }

    /**
     * Writes or overwrites a file in the active working tree.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If path is null or blank, throw IllegalArgumentException("File path cannot be blank").
     * - Write (synchronized):
     *   - Put content (or empty string if content is null) into files under path.
     */
    public synchronized void writeFile(String path, String content) {
        throw new UnsupportedOperationException("TODO: Implement writeFile");
    }

    /**
     * Reads file content from active working tree.
     * <p>
     * Instructions:
     * - Return content associated with path from files, or null if not found.
     */
    public synchronized String readFile(String path) {
        throw new UnsupportedOperationException("TODO: Implement readFile");
    }

    public synchronized String getHeadSha() {
        return headSha;
    }

    public synchronized Map<String, String> getFiles() {
        return Collections.unmodifiableMap(new HashMap<>(files));
    }
}
