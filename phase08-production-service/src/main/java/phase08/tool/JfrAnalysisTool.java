package phase08.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Phase 03: Java Flight Recorder (JFR) Profiling Tool.
 * <p>
 * Exposes Spring AI {@code @Tool} methods for lock contention and allocation diagnostics.
 */
@Component
public class JfrAnalysisTool {

    /**
     * Queries lock contention events from JFR recording.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If serviceName is null or blank, throw IllegalArgumentException("serviceName cannot be blank").
     * - Output:
     *   - Return formatted report:
     *     "Service " + serviceName + " lock contention: HikariCP connection pool acquisition lock duration 4200ms"
     */
    @Tool(description = "Query lock contention events from JFR recording")
    public String queryLockContention(@ToolParam(description = "Target microservice name") String serviceName) {
        throw new UnsupportedOperationException("TODO: Implement queryLockContention");
    }

    /**
     * Queries allocation and GC events from JFR recording.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If serviceName is null or blank, throw IllegalArgumentException("serviceName cannot be blank").
     * - Output:
     *   - Return formatted report:
     *     "Service " + serviceName + " memory allocations: High allocation rate in Jackson ObjectMapper deserialization"
     */
    @Tool(description = "Query allocation and GC events from JFR recording")
    public String queryMemoryAllocations(@ToolParam(description = "Target microservice name") String serviceName) {
        throw new UnsupportedOperationException("TODO: Implement queryMemoryAllocations");
    }
}
