# Phase 08 Exercise 02: Golden Reference Solution

## Overview
This golden solution implements **Model Context Protocol (MCP) Client Integration, Tool Providers, and Autonomous Agentic Orchestration** using the official Java MCP SDK (`io.modelcontextprotocol.sdk:mcp-core:2.0.0`) and Spring AI MCP integration (`org.springframework.ai:spring-ai-mcp:2.0.1`).

## Verified Solution Code

```java
package phase08;

import io.modelcontextprotocol.spec.McpSchema.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.JsonHelper;
import phase08.McpClientContracts.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 08 Exercise 02: Model Context Protocol (MCP) Client Integration, Tool Providers & Agentic Orchestration.
 */
public class McpClientUnderTest {

    /**
     * Scenario 01: Client Capability Negotiation & Specs.
     */
    public ClientCapabilities createClientCapabilities(boolean enableRoots, boolean enableSampling) {
        ClientCapabilities.Builder builder = ClientCapabilities.builder();
        if (enableRoots) builder.roots(true);
        if (enableSampling) builder.sampling();
        return builder.build();
    }

    /**
     * Scenario 02: Tool Definition Adaptation from MCP Schemas.
     */
    public ToolDefinition adaptMcpToolToDefinition(Tool mcpTool) {
        return McpToolUtils.createToolDefinition(mcpTool.name(), mcpTool);
    }

    /**
     * Scenario 03: Adapting MCP Sync Tools to ToolCallbacks.
     */
    public ToolCallback adaptMcpToolToCallback(McpClientAdapter client, Tool mcpTool) {
        ToolDefinition def = McpToolUtils.createToolDefinition(mcpTool.name(), mcpTool);
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return def;
            }

            @Override
            public String call(String toolInput) {
                Map<String, Object> args = Map.of();
                if (toolInput != null && !toolInput.isBlank()) {
                    try {
                        args = new JsonHelper().fromJsonToMap(toolInput);
                    } catch (Exception ignored) {}
                }
                CallToolResult result = client.callTool(mcpTool.name(), args);
                if (result != null && result.content() != null && !result.content().isEmpty()) {
                    if (result.content().get(0) instanceof TextContent tc) {
                        return tc.text();
                    }
                }
                return "";
            }
        };
    }

    /**
     * Scenario 04: Multi-Tool Discovery & Callback Provider Generation.
     */
    public List<ToolCallback> createToolCallbacksForClient(McpClientAdapter client) {
        List<Tool> tools = client.listTools();
        if (tools == null) return List.of();
        return tools.stream()
                .map(t -> adaptMcpToolToCallback(client, t))
                .toList();
    }

    /**
     * Scenario 05: Multi-Server Tool Name Prefixing (Collision Prevention).
     */
    public String buildPrefixedToolName(String serverPrefix, String toolName) {
        return McpToolUtils.prefixedToolName(serverPrefix, toolName);
    }

    /**
     * Scenario 06: MCP Resource Reading into Prompt Context.
     */
    public String formatResourceAsContext(McpClientAdapter client, String uri) {
        ReadResourceResult result = client.readResource(uri);
        if (result != null && result.contents() != null && !result.contents().isEmpty()) {
            if (result.contents().get(0) instanceof TextResourceContents trc) {
                return "[Resource: " + uri + "]\n" + trc.text();
            }
        }
        return "[Resource: " + uri + "]\n";
    }

    /**
     * Scenario 07: MCP Parameterized Prompt Retrieval.
     */
    public String fetchPromptMessageText(McpClientAdapter client, String promptName, Map<String, Object> arguments) {
        GetPromptResult result = client.getPrompt(promptName, arguments != null ? arguments : Map.of());
        if (result != null && result.messages() != null && !result.messages().isEmpty()) {
            PromptMessage msg = result.messages().get(0);
            if (msg.content() instanceof TextContent tc) {
                return tc.text();
            }
        }
        return "";
    }

    /**
     * Scenario 08: Tool Execution Resilience & Error Boundary.
     */
    public String executeToolSafely(McpClientAdapter client, String toolName, Map<String, Object> arguments) {
        try {
            CallToolResult result = client.callTool(toolName, arguments != null ? arguments : Map.of());
            if (result != null && Boolean.TRUE.equals(result.isError())) {
                String msg = "";
                if (result.content() != null && !result.content().isEmpty()) {
                    if (result.content().get(0) instanceof TextContent tc) {
                        msg = tc.text();
                    }
                }
                return "TOOL_ERROR: " + msg;
            }
            if (result != null && result.content() != null && !result.content().isEmpty()) {
                if (result.content().get(0) instanceof TextContent tc) {
                    return tc.text();
                }
            }
            return "";
        } catch (Throwable t) {
            return "TOOL_ERROR: " + t.getMessage();
        }
    }

    /**
     * Scenario 09: Multi-Server Client Registry & Unified Tool Dispatcher.
     */
    public static class MultiServerClientRegistry {
        private final Map<String, McpClientAdapter> clients = new ConcurrentHashMap<>();
        private final Map<String, McpClientAdapter> toolToClient = new ConcurrentHashMap<>();
        private final Map<String, String> toolToOriginalName = new ConcurrentHashMap<>();

        public void registerClient(McpClientAdapter client) {
            if (client != null && client.serverName() != null) {
                clients.put(client.serverName(), client);
                List<Tool> tools = client.listTools();
                if (tools != null) {
                    for (Tool t : tools) {
                        String prefixed = McpToolUtils.prefixedToolName(client.serverName(), t.name());
                        toolToClient.put(prefixed, client);
                        toolToOriginalName.put(prefixed, t.name());
                    }
                }
            }
        }

        public List<String> listAllPrefixedTools() {
            return new ArrayList<>(toolToClient.keySet());
        }

        public String executePrefixedTool(String prefixedName, Map<String, Object> arguments) {
            McpClientAdapter client = toolToClient.get(prefixedName);
            String originalName = toolToOriginalName.get(prefixedName);
            if (client == null || originalName == null) {
                throw new McpClientBreachException("No registered client for tool: " + prefixedName);
            }
            CallToolResult res = client.callTool(originalName, arguments != null ? arguments : Map.of());
            if (res != null && res.content() != null && !res.content().isEmpty()) {
                if (res.content().get(0) instanceof TextContent tc) {
                    return tc.text();
                }
            }
            return "";
        }

        public Map<String, McpClientAdapter> getClients() {
            return Collections.unmodifiableMap(clients);
        }
    }

    /**
     * Scenario 10: End-to-End Enterprise Agentic Gateway with MCP Tool Calling.
     */
    public AgentExecutionSummary executeAgentWorkflow(
            MultiServerClientRegistry registry,
            ChatClient.Builder chatClientBuilder,
            AgentExecutionRequest request
    ) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new McpClientBreachException("Blank query");
        }

        List<String> executedTools = new ArrayList<>();
        List<ToolCallback> callbacks = new ArrayList<>();

        for (String prefixedTool : registry.listAllPrefixedTools()) {
            ToolDefinition def = ToolDefinition.builder()
                    .name(prefixedTool)
                    .description("Executes tool " + prefixedTool)
                    .inputSchema("{\"type\":\"object\"}")
                    .build();

            callbacks.add(new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return def;
                }

                @Override
                public String call(String toolInput) {
                    executedTools.add(prefixedTool);
                    if (executedTools.size() > request.maxToolCalls()) {
                        throw new McpClientBreachException("Max tool call ceiling exceeded: " + executedTools.size());
                    }
                    Map<String, Object> args = Map.of();
                    if (toolInput != null && !toolInput.isBlank()) {
                        try {
                            args = new JsonHelper().fromJsonToMap(toolInput);
                        } catch (Exception ignored) {}
                    }
                    return registry.executePrefixedTool(prefixedTool, args);
                }
            });
        }

        ChatClient client = chatClientBuilder
                .defaultToolCallbacks(callbacks)
                .build();

        ChatResponse response = client.prompt()
                .user(request.query())
                .call()
                .chatResponse();

        int totalTokens = (response != null && response.getMetadata().getUsage() != null)
                ? response.getMetadata().getUsage().getTotalTokens()
                : 0;

        String answer = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                ? response.getResult().getOutput().getText()
                : "";

        return new AgentExecutionSummary(answer, executedTools, totalTokens, true);
    }
}
```

## Verification Command
```powershell
mvn clean test-compile exec:java -pl phase08-ex02-mcp-client
```
Output:
```
VERIFICATION SUMMARY: 10 / 10 PASSED, 0 FAILED
```
