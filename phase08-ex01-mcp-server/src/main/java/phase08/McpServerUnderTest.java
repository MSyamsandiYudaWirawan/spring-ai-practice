package phase08;

import io.modelcontextprotocol.server.McpServerFeatures.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import phase08.McpServerContracts.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Phase 08 Exercise 01: Model Context Protocol (MCP) Server Architecture & Protocol Specifications.
 * <p>
 * Practice Drills:
 * 1. Server Capabilities Declaration & Protocol Negotiation
 * 2. Tool Definition with JSON Input Schema
 * 3. SyncToolSpecification Execution Handler & Output Wrapping
 * 4. Resilient Tool Execution & Error Signal Wrapping
 * 5. Spring AI ToolCallback to MCP Tool Adapter
 * 6. MCP Static & Dynamic Resource Specification
 * 7. MCP Parameterized Prompt Template Specification
 * 8. Security Allowlist & Tool Filter
 * 9. MCP Server Telemetry & Audit Recorder
 * 10. Composite Enterprise MCP Server Registry & Router
 */
public class McpServerUnderTest {

    /**
     * Scenario 01: Server Capabilities Declaration.
     * <p>
     * Instructions:
     * - Use ServerCapabilities.builder().
     * - If enableTools is true, configure tools(true).
     * - If enableResources is true, configure resources(true, true).
     * - If enablePrompts is true, configure prompts(true).
     * - If enableLogging is true, configure logging().
     * - Build and return ServerCapabilities.
     */
    public ServerCapabilities createServerCapabilities(
            boolean enableTools,
            boolean enableResources,
            boolean enablePrompts,
            boolean enableLogging
    ) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 02: Tool Definition with JSON Input Schema.
     * <p>
     * Instructions:
     * - Construct a JSON schema map for inputs:
     *   "type" -> "object"
     *   "properties" -> properties (or empty map if null)
     *   "required" -> requiredFields (or empty list if null)
     * - Construct Tool.builder(name) (or new Tool(name, null, description, schema, null, null, null, null))
     * - Set name, description, and inputSchema.
     * - Build and return Tool.
     */
    public Tool createToolDefinition(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> requiredFields
    ) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 03: SyncToolSpecification Execution Handler.
     * <p>
     * Instructions:
     * - Return a new SyncToolSpecification(tool, (exchange, request) -> {
     *     Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *     ServerContext ctx = new ServerContext(
     *         exchange != null ? exchange.sessionId() : "session-0",
     *         "role-standard",
     *         "trace-0"
     *     );
     *     String output = logicHandler.apply(args, ctx);
     *     return new CallToolResult(List.of(new TextContent(output)), false, null, null);
     *   });
     */
    public SyncToolSpecification createToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 04: Resilient Tool Execution & Error Signal Wrapping.
     * <p>
     * Instructions:
     * - Return a new SyncToolSpecification(tool, (exchange, request) -> {
     *     try {
     *         Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *         ServerContext ctx = new ServerContext(
     *             exchange != null ? exchange.sessionId() : "session-0",
     *             "role-standard",
     *             "trace-0"
     *         );
     *         String output = logicHandler.apply(args, ctx);
     *         return new CallToolResult(List.of(new TextContent(output)), false, null, null);
     *     } catch (Throwable t) {
     *         return new CallToolResult(List.of(new TextContent("Error: " + t.getMessage())), true, null, null);
     *     }
     *   });
     */
    public SyncToolSpecification createResilientToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 05: Spring AI ToolCallback to MCP Tool Adapter.
     * <p>
     * Instructions:
     * - Use McpToolUtils.toSyncToolSpecification(springAiTool).
     * - Return the resulting SyncToolSpecification with null-safe exchange fallback:
     *   if exchange is null, call springAiTool.call(jsonInput).
     */
    public SyncToolSpecification adaptSpringAiTool(ToolCallback springAiTool) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 06: MCP Static & Dynamic Resource Specification.
     * <p>
     * Instructions:
     * - Construct Resource.builder(uri, name).description("resource").mimeType(mimeType).build().
     * - Return new SyncResourceSpecification(resource, (exchange, request) -> {
     *     String content = contentProvider.apply(request.uri());
     *     return new ReadResourceResult(List.of(new TextResourceContents(request.uri(), mimeType, content)));
     *   });
     */
    public SyncResourceSpecification createResourceSpecification(
            String uri,
            String name,
            String mimeType,
            Function<String, String> contentProvider
    ) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 07: MCP Parameterized Prompt Template Specification.
     * <p>
     * Instructions:
     * - Construct Prompt(promptName, description, arguments).
     * - Return new SyncPromptSpecification(prompt, (exchange, request) -> {
     *     Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *     String formatted = promptFormatter.apply(request.name(), args);
     *     return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(formatted))));
     *   });
     */
    public SyncPromptSpecification createPromptSpecification(
            String promptName,
            String description,
            List<PromptArgument> arguments,
            BiFunction<String, Map<String, Object>, String> promptFormatter
    ) {
        // DEFECT: Returns null
        return null;
    }

    /**
     * Scenario 08: Security Allowlist & Tool Filter.
     * <p>
     * Instructions:
     * - Filter availableTools so only tools whose tool().name() is contained in allowlistedNames are kept.
     * - Return the filtered List<SyncToolSpecification>.
     */
    public List<SyncToolSpecification> filterToolsByAllowlist(
            List<SyncToolSpecification> availableTools,
            Set<String> allowlistedNames
    ) {
        // DEFECT: Returns empty list
        return List.of();
    }

    /**
     * Scenario 09: MCP Server Telemetry & Audit Recorder.
     */
    public static class McpServerAuditRecorder {
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);
        private final Map<String, AtomicInteger> toolCounts = new ConcurrentHashMap<>();

        public void recordSuccess(String toolName) {
            // DEFECT: No-op
        }

        public void recordError(String toolName) {
            // DEFECT: No-op
        }

        public McpAuditReport getReport() {
            // DEFECT: Returns empty report
            return new McpAuditReport(0, 0, 0, Map.of());
        }
    }

    /**
     * Scenario 10: Composite Enterprise MCP Server Registry & Router.
     */
    public static class EnterpriseMcpRegistry {
        private final Map<String, SyncToolSpecification> tools = new ConcurrentHashMap<>();
        private final Map<String, SyncResourceSpecification> resources = new ConcurrentHashMap<>();
        private final Map<String, SyncPromptSpecification> prompts = new ConcurrentHashMap<>();
        private final Set<String> allowlistedTools = ConcurrentHashMap.newKeySet();
        private final McpServerAuditRecorder auditRecorder = new McpServerAuditRecorder();

        public void registerTool(SyncToolSpecification spec) {
            // DEFECT: No-op
        }

        public void registerResource(SyncResourceSpecification spec) {
            // DEFECT: No-op
        }

        public void registerPrompt(SyncPromptSpecification spec) {
            // DEFECT: No-op
        }

        public void setAllowlist(Set<String> allowedTools) {
            // DEFECT: No-op
        }

        public CallToolResult handleCallTool(String toolName, Map<String, Object> arguments, ServerContext context) {
            // DEFECT: Returns null
            return null;
        }

        public ReadResourceResult handleReadResource(String uri) {
            // DEFECT: Returns null
            return null;
        }

        public GetPromptResult handleGetPrompt(String promptName, Map<String, Object> arguments) {
            // DEFECT: Returns null
            return null;
        }

        public McpAuditReport getAuditReport() {
            return auditRecorder.getReport();
        }
    }
}
