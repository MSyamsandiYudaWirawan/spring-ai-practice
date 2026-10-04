package phase08.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VirtualWorkspaceTest {

    @Test
    @DisplayName("Should commit new snapshot and revert cleanly to baseline on failure")
    void testCommitAndRevert() {
        VirtualWorkspace ws = new VirtualWorkspace("sha-0", Map.of("App.java", "int count = 0;"));

        // Mutate and commit sha-1
        ws.writeFile("App.java", "int count = 10;");
        ws.commit("sha-1", ws.getFiles());

        assertThat(ws.getHeadSha()).isEqualTo("sha-1");
        assertThat(ws.readFile("App.java")).isEqualTo("int count = 10;");

        // Mutate uncommitted dirty change
        ws.writeFile("App.java", "DIRTY_BROKEN_CODE");

        // Roll back to sha-1
        ws.revertTo("sha-1");
        assertThat(ws.readFile("App.java")).isEqualTo("int count = 10;");

        // Roll back to baseline sha-0
        ws.revertTo("sha-0");
        assertThat(ws.getHeadSha()).isEqualTo("sha-0");
        assertThat(ws.readFile("App.java")).isEqualTo("int count = 0;");
    }

    @Test
    @DisplayName("Reverting to non-existent sha should throw IllegalArgumentException")
    void testRevertUnknownShaThrows() {
        VirtualWorkspace ws = new VirtualWorkspace("sha-0", Map.of());
        assertThatThrownBy(() -> ws.revertTo("sha-nonexistent"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
