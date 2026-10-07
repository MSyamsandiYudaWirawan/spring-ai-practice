package phase08;

import io.modelcontextprotocol.spec.McpSchema.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain contracts and data transfer objects for Phase 08 Exercise 02 (MCP Client).
 * <p>
 * DO NOT MODIFY THIS FILE. All implementations must reside in {@link McpClientUnderTest}.
 */
public final class McpClientContracts {

    private McpClientContracts() {}

    /**
     * Client interface abstracting an active synchronous MCP connection.
     */
    public interface McpClientAdapter {
        String serverName();
        List<Tool> listTools();
        CallToolResult callTool(String toolName, Map<String, Object> arguments);
        ReadResourceResult readResource(String uri);
        GetPromptResult getPrompt(String promptName, Map<String, Object> arguments);
    }

    /**
     * User task submitted to the autonomous agentic MCP gateway.
     */
    public record AgentExecutionRequest(String query, int maxToolCalls) {
        public AgentExecutionRequest {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("query cannot be blank");
            }
            if (maxToolCalls <= 0) {
                throw new IllegalArgumentException("maxToolCalls must be > 0");
            }
        }
    }

    /**
     * Summary report returned after executing an autonomous workflow with MCP tools.
     */
    public record AgentExecutionSummary(
            String finalAnswer,
            List<String> toolsExecuted,
            int totalTokens,
            boolean success
    ) {
        public AgentExecutionSummary {
            Objects.requireNonNull(finalAnswer, "finalAnswer cannot be null");
            toolsExecuted = toolsExecuted == null ? List.of() : List.copyOf(toolsExecuted);
        }
    }

    /**
     * Report produced when an MCP server's tool catalog is dynamically reloaded.
     */
    public record ToolListReloadReport(
            String serverId,
            int previousToolCount,
            int updatedToolCount,
            List<String> activeToolNames
    ) {
        public ToolListReloadReport {
            Objects.requireNonNull(serverId, "serverId cannot be null");
            activeToolNames = activeToolNames == null ? List.of() : List.copyOf(activeToolNames);
        }
    }

    /**
     * Server-to-client LLM completion sampling request (MCP protocol reverse delegation).
     */
    public record SamplingRequest(
            String prompt,
            int maxTokens,
            double temperature
    ) {
        public SamplingRequest {
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("prompt cannot be blank");
            }
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens must be > 0");
            }
        }
    }

    /**
     * Response returned to an MCP server after client-side LLM sampling execution.
     */
    public record SamplingResponse(
            String content,
            int tokensUsed
    ) {
        public SamplingResponse {
            Objects.requireNonNull(content, "content cannot be null");
        }
    }

    /**
     * Exception thrown when an MCP client encounter a tool breach, unknown server, or quota violation.
     */
    public static class McpClientBreachException extends RuntimeException {
        public McpClientBreachException(String message) {
            super(message);
        }

        public McpClientBreachException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
