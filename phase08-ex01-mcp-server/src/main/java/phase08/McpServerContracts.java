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
     * Matched resource URI route extracting path variables.
     */
    public record MatchedResourceRoute(
            String uriTemplate,
            String requestedUri,
            Map<String, String> pathVariables
    ) {
        public MatchedResourceRoute {
            Objects.requireNonNull(uriTemplate, "uriTemplate cannot be null");
            Objects.requireNonNull(requestedUri, "requestedUri cannot be null");
            pathVariables = pathVariables == null ? Map.of() : Map.copyOf(pathVariables);
        }
    }

    /**
     * Standard MCP logging severity levels (RFC 5424 aligned).
     */
    public enum McpLogLevel {
        DEBUG(0),
        INFO(1),
        WARNING(2),
        ERROR(3);

        private final int priority;

        McpLogLevel(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return priority;
        }

        public boolean isEnabledFor(McpLogLevel configuredThreshold) {
            return this.priority >= configuredThreshold.priority;
        }
    }

    /**
     * Structured MCP protocol log notification event.
     */
    public record McpLogMessage(
            McpLogLevel level,
            String loggerName,
            String message,
            Map<String, Object> data
    ) {
        public McpLogMessage {
            Objects.requireNonNull(level, "level cannot be null");
            loggerName = (loggerName == null || loggerName.isBlank()) ? "root" : loggerName;
            Objects.requireNonNull(message, "message cannot be null");
            data = data == null ? Map.of() : Map.copyOf(data);
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
