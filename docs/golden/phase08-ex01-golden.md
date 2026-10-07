# Phase 08 Exercise 01: Golden Reference Solution

## Overview
This golden solution implements **Model Context Protocol (MCP) Server Architecture, Protocol Specifications, and Composite Registry** using the official Java MCP SDK (`io.modelcontextprotocol.sdk:mcp-core:2.0.0`) and Spring AI MCP integration (`org.springframework.ai:spring-ai-mcp:2.0.1`).

## Verified Solution Code

```java
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
 */
public class McpServerUnderTest {

    /**
     * Scenario 01: Server Capabilities Declaration.
     */
    public ServerCapabilities createServerCapabilities(
            boolean enableTools,
            boolean enableResources,
            boolean enablePrompts,
            boolean enableLogging
    ) {
        ServerCapabilities.Builder builder = ServerCapabilities.builder();
        if (enableTools) builder.tools(true);
        if (enableResources) builder.resources(true, true);
        if (enablePrompts) builder.prompts(true);
        if (enableLogging) builder.logging();
        return builder.build();
    }

    /**
     * Scenario 02: Tool Definition with JSON Input Schema.
     */
    public Tool createToolDefinition(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> requiredFields
    ) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties != null ? properties : Map.of());
        schema.put("required", requiredFields != null ? requiredFields : List.of());

        return Tool.builder(name)
                .description(description)
                .inputSchema(schema)
                .build();
    }

    /**
     * Scenario 03: SyncToolSpecification Execution Handler.
     */
    public SyncToolSpecification createToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        return new SyncToolSpecification(tool, (exchange, request) -> {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            ServerContext ctx = new ServerContext(
                    exchange != null ? exchange.sessionId() : "session-0",
                    "role-standard",
                    "trace-0"
            );
            String output = logicHandler.apply(args, ctx);
            return new CallToolResult(List.of(new TextContent(output)), false, null, null);
        });
    }

    /**
     * Scenario 04: Resilient Tool Execution & Error Signal Wrapping.
     */
    public SyncToolSpecification createResilientToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        return new SyncToolSpecification(tool, (exchange, request) -> {
            try {
                Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
                ServerContext ctx = new ServerContext(
                        exchange != null ? exchange.sessionId() : "session-0",
                        "role-standard",
                        "trace-0"
                );
                String output = logicHandler.apply(args, ctx);
                return new CallToolResult(List.of(new TextContent(output)), false, null, null);
            } catch (Throwable t) {
                return new CallToolResult(List.of(new TextContent("Error: " + t.getMessage())), true, null, null);
            }
        });
    }

    /**
     * Scenario 05: Spring AI ToolCallback to MCP Tool Adapter.
     */
    public SyncToolSpecification adaptSpringAiTool(ToolCallback springAiTool) {
        SyncToolSpecification original = McpToolUtils.toSyncToolSpecification(springAiTool);
        return new SyncToolSpecification(original.tool(), (exchange, request) -> {
            if (exchange == null) {
                String input = request.arguments() != null && !request.arguments().isEmpty()
                        ? new org.springframework.ai.util.JsonHelper().toJson(request.arguments())
                        : "{}";
                String output = springAiTool.call(input);
                return new CallToolResult(List.of(new TextContent(output)), false, null, null);
            }
            return original.callHandler().apply(exchange, request);
        });
    }

    /**
     * Scenario 06: MCP Static & Dynamic Resource Specification.
     */
    public SyncResourceSpecification createResourceSpecification(
            String uri,
            String name,
            String mimeType,
            Function<String, String> contentProvider
    ) {
        Resource resource = Resource.builder(uri, name)
                .description("resource")
                .mimeType(mimeType)
                .build();
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            String content = contentProvider.apply(request.uri());
            return new ReadResourceResult(List.of(new TextResourceContents(request.uri(), mimeType, content)));
        });
    }

    /**
     * Scenario 07: MCP Parameterized Prompt Template Specification.
     */
    public SyncPromptSpecification createPromptSpecification(
            String promptName,
            String description,
            List<PromptArgument> arguments,
            BiFunction<String, Map<String, Object>, String> promptFormatter
    ) {
        Prompt prompt = new Prompt(promptName, description, arguments);
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            String formatted = promptFormatter.apply(request.name(), args);
            return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(formatted))));
        });
    }

    /**
     * Scenario 08: Security Allowlist & Tool Filter.
     */
    public List<SyncToolSpecification> filterToolsByAllowlist(
            List<SyncToolSpecification> availableTools,
            Set<String> allowlistedNames
    ) {
        if (availableTools == null) return List.of();
        if (allowlistedNames == null || allowlistedNames.isEmpty()) return List.of();
        return availableTools.stream()
                .filter(t -> t.tool() != null && allowlistedNames.contains(t.tool().name()))
                .toList();
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
            totalCalls.incrementAndGet();
            successCount.incrementAndGet();
            toolCounts.computeIfAbsent(toolName, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public void recordError(String toolName) {
            totalCalls.incrementAndGet();
            errorCount.incrementAndGet();
            toolCounts.computeIfAbsent(toolName, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public McpAuditReport getReport() {
            Map<String, Integer> map = new LinkedHashMap<>();
            toolCounts.forEach((k, v) -> map.put(k, v.get()));
            return new McpAuditReport(totalCalls.get(), successCount.get(), errorCount.get(), map);
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
            if (spec != null && spec.tool() != null && spec.tool().name() != null) {
                tools.put(spec.tool().name(), spec);
            }
        }

        public void registerResource(SyncResourceSpecification spec) {
            if (spec != null && spec.resource() != null && spec.resource().uri() != null) {
                resources.put(spec.resource().uri(), spec);
            }
        }

        public void registerPrompt(SyncPromptSpecification spec) {
            if (spec != null && spec.prompt() != null && spec.prompt().name() != null) {
                prompts.put(spec.prompt().name(), spec);
            }
        }

        public void setAllowlist(Set<String> allowedTools) {
            allowlistedTools.clear();
            if (allowedTools != null) {
                allowlistedTools.addAll(allowedTools);
            }
        }

        public CallToolResult handleCallTool(String toolName, Map<String, Object> arguments, ServerContext context) {
            if (!tools.containsKey(toolName)) {
                throw new McpRegistryBreachException("Tool not registered: " + toolName);
            }
            if (!allowlistedTools.isEmpty() && !allowlistedTools.contains(toolName)) {
                throw new McpSecurityBreachException("Access denied by allowlist for tool: " + toolName);
            }

            SyncToolSpecification spec = tools.get(toolName);
            try {
                CallToolRequest req = new CallToolRequest(toolName, arguments != null ? arguments : Map.of());
                CallToolResult res = spec.callHandler().apply(null, req);
                if (res != null && Boolean.TRUE.equals(res.isError())) {
                    auditRecorder.recordError(toolName);
                } else {
                    auditRecorder.recordSuccess(toolName);
                }
                return res;
            } catch (Throwable t) {
                auditRecorder.recordError(toolName);
                throw t;
            }
        }

        public ReadResourceResult handleReadResource(String uri) {
            if (!resources.containsKey(uri)) {
                throw new McpRegistryBreachException("Resource not registered: " + uri);
            }
            SyncResourceSpecification spec = resources.get(uri);
            return spec.readHandler().apply(null, new ReadResourceRequest(uri));
        }

        public GetPromptResult handleGetPrompt(String promptName, Map<String, Object> arguments) {
            if (!prompts.containsKey(promptName)) {
                throw new McpRegistryBreachException("Prompt not registered: " + promptName);
            }
            SyncPromptSpecification spec = prompts.get(promptName);
            return spec.promptHandler().apply(null, new GetPromptRequest(promptName, arguments != null ? arguments : Map.of()));
        }

        public McpAuditReport getAuditReport() {
            return auditRecorder.getReport();
        }
    }
}
```

## Verification Command
```powershell
mvn clean test-compile exec:java -pl phase08-ex01-mcp-server
```
Output:
```
VERIFICATION SUMMARY: 10 / 10 PASSED, 0 FAILED
```
