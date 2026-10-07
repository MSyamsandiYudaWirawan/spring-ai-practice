package phase08;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Domain contracts and data transfer objects for Phase 08 Exercise 01 (MCP Server).
 * <p>
 * DO NOT MODIFY THIS FILE. All implementations must reside in {@link McpServerUnderTest}.
 */
public final class McpServerContracts {

    private McpServerContracts() {}

    /**
     * Context passed during tool execution, carrying tenant and security claims.
     */
    public record ServerContext(String tenantId, String userRole, String traceId) {
        public ServerContext {
            tenantId = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
            userRole = (userRole == null || userRole.isBlank()) ? "anonymous" : userRole;
            traceId = (traceId == null || traceId.isBlank()) ? "trace-0" : traceId;
        }
    }

    /**
     * Audit metrics aggregated by the MCP Server runtime.
     */
    public record McpAuditReport(
            int totalCalls,
            int successCount,
            int errorCount,
            Map<String, Integer> toolCallCounts
    ) {
        public McpAuditReport {
            toolCallCounts = toolCallCounts == null ? Map.of() : Map.copyOf(toolCallCounts);
        }
    }

    /**
     * Exception thrown when an unregistered tool, resource, or prompt is requested.
     */
    public static class McpRegistryBreachException extends RuntimeException {
        public McpRegistryBreachException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when tool execution is denied by security allowlists or permission gates.
     */
    public static class McpSecurityBreachException extends RuntimeException {
        public McpSecurityBreachException(String message) {
            super(message);
        }
    }
}
