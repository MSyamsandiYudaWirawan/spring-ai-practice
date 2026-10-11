package phase08;

import java.util.Objects;

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
}
