package phase08.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticToolsTest {

    private final K8sDiagnosticTool k8sTool = new K8sDiagnosticTool();
    private final JfrAnalysisTool jfrTool = new JfrAnalysisTool();

    @Test
    @DisplayName("K8s tool should return pod status for valid namespace")
    void testK8sPodStatus() {
        String res = k8sTool.queryPodStatus("prod-payment");
        assertThat(res).contains("prod-payment").contains("3 pods running");
    }

    @Test
    @DisplayName("K8s tool should reject blank namespace")
    void testK8sBlankNamespaceThrows() {
        assertThatThrownBy(() -> k8sTool.queryPodStatus("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("JFR tool should return lock contention details")
    void testJfrLockContention() {
        String res = jfrTool.queryLockContention("billing-service");
        assertThat(res).contains("billing-service").contains("HikariCP");
    }

    @Test
    @DisplayName("ToolCallbacks.from should dynamically discover @Tool methods on both beans")
    void testDynamicToolDiscovery() {
        ToolCallback[] callbacks = ToolCallbacks.from(k8sTool, jfrTool);
        assertThat(callbacks).isNotNull();
        // 2 methods on K8sDiagnosticTool, 2 on JfrAnalysisTool = 4 total callbacks
        assertThat(callbacks.length).isEqualTo(4);
    }
}
