package phase08.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Phase 03: Kubernetes Infrastructure Diagnostic Tool.
 * <p>
 * Exposes Spring AI {@code @Tool} methods for autonomous pod health inspection.
 */
@Component
public class K8sDiagnosticTool {

    /**
     * Queries status of pods running in the specified Kubernetes namespace.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If namespace is null or blank, throw IllegalArgumentException("namespace cannot be blank").
     * - Output:
     *   - Return formatted status: "Namespace " + namespace + ": 3 pods running, 0 failed, 0 restarts".
     */
    @Tool(description = "Query status of pods running in the specified Kubernetes namespace")
    public String queryPodStatus(@ToolParam(description = "Kubernetes namespace") String namespace) {
        throw new UnsupportedOperationException("TODO: Implement queryPodStatus");
    }

    /**
     * Fetches recent tail logs for a specific pod.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If podName is null or blank, throw IllegalArgumentException("podName cannot be blank").
     *   - If tailLines < 1 or > 500, throw IllegalArgumentException("tailLines must be between 1 and 500").
     * - Output:
     *   - Return formatted tail snippet:
     *     "Pod " + podName + " logs [tail " + tailLines + "]: ThreadPoolExecutor rejected execution, queue capacity 50 reached"
     */
    @Tool(description = "Fetch recent tail logs for a specific pod")
    public String fetchPodLogs(
            @ToolParam(description = "Target pod name") String podName,
            @ToolParam(description = "Number of tail lines (1 to 500)") int tailLines
    ) {
        throw new UnsupportedOperationException("TODO: Implement fetchPodLogs");
    }
}
