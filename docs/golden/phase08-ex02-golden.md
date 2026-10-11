# Phase 08 Exercise 02 — MCP Client Integration & Agentic Orchestration (Golden Solution)

**Path:** `phase08-ex02-mcp-client/src/main/java/phase08/McpClientUnderTest.java`  
**All 15 Scenarios Verified Passing.**

```java
package phase08;

import io.modelcontextprotocol.spec.McpSchema.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.JsonHelper;
import phase08.McpClientContracts.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exercise implementation under test for Phase 08 Exercise 02:
 * Model Context Protocol (MCP) Client Integration, Tool Providers & Agentic Orchestration.
 * <p>
 * 15 repetitive muscle-memory drills across 5 core topics (3 repetitions each).
 */
public class McpClientUnderTest {

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    // =========================================================================
    // TOPIC 1: Client Capabilities & Specs (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 01: Client Capabilities Declaration.
     */
    public ClientCapabilities createClientCapabilities(boolean enableRoots, boolean enableSampling) {
        ClientCapabilities.Builder builder = ClientCapabilities.builder();
        if (enableRoots) {
            builder.roots(true);
        }
        if (enableSampling) {
            builder.sampling();
        }
        return builder.build();
    }

    /**
     * Scenario 02: Client Roots List Construction & URI Validation.
     */
    public List<Root> buildClientRoots(Map<String, String> rootUriToNameMap) {
        if (rootUriToNameMap == null || rootUriToNameMap.isEmpty()) {
            throw new IllegalArgumentException("rootUriToNameMap cannot be null or empty");
        }
        List<Root> roots = new ArrayList<>();
        for (Map.Entry<String, String> entry : rootUriToNameMap.entrySet()) {
            String uri = entry.getKey();
            String name = entry.getValue();
            if (uri == null || uri.isBlank() || name == null || name.isBlank()) {
                throw new IllegalArgumentException("uri and name must not be null/blank");
            }
            if (!uri.startsWith("file://")) {
                throw new IllegalArgumentException("uri must start with file://: " + uri);
            }
            roots.add(new Root(uri, name));
        }
        return roots;
    }

    /**
     * Scenario 03: Client Implementation & Metadata Spec.
     */
    public Implementation createClientImplementation(String name, String version, String description) {
        if (name == null || name.isBlank() || version == null || version.isBlank()) {
            throw new IllegalArgumentException("name and version must not be null/blank");
        }
        return Implementation.builder(name, version)
                .description(description != null ? description : "")
                .build();
    }

    // =========================================================================
    // TOPIC 2: Adapting MCP Tools to Spring AI ToolCallbacks (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 04: ToolDefinition Adaptation from MCP Schemas.
     */
    public ToolDefinition adaptMcpToolToDefinition(Tool mcpTool) {
        if (mcpTool == null) {
            throw new IllegalArgumentException("mcpTool must not be null");
        }
        return McpToolUtils.createToolDefinition(mcpTool.name(), mcpTool);
    }

    /**
     * Scenario 05: Standard MCP Tool to ToolCallback Adaptation.
     */
    public ToolCallback adaptMcpToolToCallback(McpClientAdapter client, Tool mcpTool) {
        if (client == null || mcpTool == null) {
            throw new IllegalArgumentException("client and mcpTool must not be null");
        }
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return McpToolUtils.createToolDefinition(mcpTool.name(), mcpTool);
            }

            @Override
            public String call(String toolInput) {
                Map<String, Object> args = (toolInput != null && !toolInput.isBlank())
                        ? JSON_HELPER.fromJsonToMap(toolInput)
                        : Map.of();
                CallToolResult result = client.callTool(mcpTool.name(), args);
                if (result != null && result.content() != null && !result.content().isEmpty()
                        && result.content().get(0) instanceof TextContent tc) {
                    return tc.text();
                }
                return "";
            }
        };
    }

    /**
     * Scenario 06: Resilient ToolCallback with Fault Shielding.
     */
    public ToolCallback adaptResilientMcpToolToCallback(McpClientAdapter client, Tool mcpTool) {
        if (client == null || mcpTool == null) {
            throw new IllegalArgumentException("client and mcpTool must not be null");
        }
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return McpToolUtils.createToolDefinition(mcpTool.name(), mcpTool);
            }

            @Override
            public String call(String toolInput) {
                try {
                    Map<String, Object> args = (toolInput != null && !toolInput.isBlank())
                            ? JSON_HELPER.fromJsonToMap(toolInput)
                            : Map.of();
                    CallToolResult result = client.callTool(mcpTool.name(), args);
                    if (result == null) {
                        return "ERROR: null result";
                    }
                    if (Boolean.TRUE.equals(result.isError())) {
                        String errorText = result.content() != null && !result.content().isEmpty()
                                && result.content().get(0) instanceof TextContent tc
                                ? tc.text() : "unknown error";
                        return "ERROR: " + errorText;
                    }
                    if (result.content() != null && !result.content().isEmpty()
                            && result.content().get(0) instanceof TextContent tc) {
                        return tc.text();
                    }
                    return "";
                } catch (Throwable t) {
                    return "ERROR: " + t.getMessage();
                }
            }
        };
    }

    // =========================================================================
    // TOPIC 3: Tool Discovery, Batch Adaptation & Namespacing (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 07: Multi-Tool Discovery & Batch Callback Generation.
     */
    public List<ToolCallback> createToolCallbacksForClient(McpClientAdapter client) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        return client.listTools().stream()
                .map(tool -> adaptMcpToolToCallback(client, tool))
                .toList();
    }

    /**
     * Scenario 08: Multi-Server Tool Name Prefixing.
     */
    public String buildPrefixedToolName(String serverPrefix, String toolName) {
        if (serverPrefix == null || serverPrefix.isBlank() || toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("serverPrefix and toolName must not be null/blank");
        }
        return McpToolUtils.prefixedToolName(serverPrefix, toolName);
    }

    /**
     * Scenario 09: Prefixed Tool Discovery with Collision Prevention.
     */
    public Map<String, ToolCallback> discoverPrefixedToolCallbacks(List<McpClientAdapter> clients) {
        if (clients == null) {
            throw new IllegalArgumentException("clients must not be null");
        }
        Map<String, ToolCallback> map = new LinkedHashMap<>();
        for (McpClientAdapter client : clients) {
            for (Tool tool : client.listTools()) {
                String prefixedName = buildPrefixedToolName(client.serverName(), tool.name());
                ToolDefinition prefixedDef = ToolDefinition.builder()
                        .name(prefixedName)
                        .description(tool.description())
                        .inputSchema(tool.inputSchema() != null ? JSON_HELPER.toJson(tool.inputSchema()) : "{}")
                        .build();

                ToolCallback cb = new ToolCallback() {
                    @Override
                    public ToolDefinition getToolDefinition() {
                        return prefixedDef;
                    }

                    @Override
                    public String call(String toolInput) {
                        Map<String, Object> args = (toolInput != null && !toolInput.isBlank())
                                ? JSON_HELPER.fromJsonToMap(toolInput)
                                : Map.of();
                        CallToolResult res = client.callTool(tool.name(), args);
                        if (res != null && res.content() != null && !res.content().isEmpty()
                                && res.content().get(0) instanceof TextContent tc) {
                            return tc.text();
                        }
                        return "";
                    }
                };
                map.put(prefixedName, cb);
            }
        }
        return map;
    }

    // =========================================================================
    // TOPIC 4: MCP Resources & Prompts Retrieval (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Single Resource Reading into Prompt Context.
     */
    public String formatResourceAsContext(McpClientAdapter client, String uri) {
        if (client == null || uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("client and uri must not be null/blank");
        }
        ReadResourceResult result = client.readResource(uri);
        String text = "";
        if (result != null && result.contents() != null && !result.contents().isEmpty()
                && result.contents().get(0) instanceof TextResourceContents trc) {
            text = trc.text();
        }
        return "[Resource: " + uri + "]\n" + text;
    }

    /**
     * Scenario 11: Multi-Resource Context Assembly.
     */
    public String assembleMultiResourceContext(McpClientAdapter client, List<String> uris) {
        if (client == null || uris == null) {
            throw new IllegalArgumentException("client and uris must not be null");
        }
        List<String> formatted = new ArrayList<>();
        for (String uri : uris) {
            formatted.add(formatResourceAsContext(client, uri));
        }
        return String.join("\n---\n", formatted);
    }

    /**
     * Scenario 12: Parameterized Prompt Retrieval & Argument Binding.
     */
    public String fetchPromptMessageText(McpClientAdapter client, String promptName, Map<String, Object> arguments) {
        if (client == null || promptName == null || promptName.isBlank()) {
            throw new IllegalArgumentException("client and promptName must not be null/blank");
        }
        GetPromptResult result = client.getPrompt(promptName, arguments != null ? arguments : Map.of());
        if (result != null && result.messages() != null && !result.messages().isEmpty()) {
            PromptMessage msg = result.messages().get(0);
            if (msg.content() instanceof TextContent tc) {
                return tc.text();
            }
        }
        return "";
    }

    // =========================================================================
    // TOPIC 5: Multi-Server Routing & Fault Resilience (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Multi-Server Tool Routing Registry.
     */
    public static class MultiServerClientRegistry {
        private final Map<String, McpClientAdapter> clients = new ConcurrentHashMap<>();
        private final Map<String, McpClientAdapter> toolToClient = new ConcurrentHashMap<>();
        private final Map<String, String> toolToOriginalName = new ConcurrentHashMap<>();

        public void registerClient(McpClientAdapter client) {
            if (client == null) {
                throw new IllegalArgumentException("client must not be null");
            }
            clients.put(client.serverName(), client);
            for (Tool tool : client.listTools()) {
                String prefixed = McpToolUtils.prefixedToolName(client.serverName(), tool.name());
                toolToClient.put(prefixed, client);
                toolToOriginalName.put(prefixed, tool.name());
            }
        }

        public List<String> listAllPrefixedTools() {
            List<String> list = new ArrayList<>(toolToClient.keySet());
            Collections.sort(list);
            return list;
        }

        public String executePrefixedTool(String prefixedName, Map<String, Object> arguments) {
            McpClientAdapter client = toolToClient.get(prefixedName);
            String originalName = toolToOriginalName.get(prefixedName);
            if (client == null || originalName == null) {
                throw new McpClientBreachException("Tool not found: " + prefixedName);
            }
            CallToolResult res = client.callTool(originalName, arguments != null ? arguments : Map.of());
            if (res != null && res.content() != null && !res.content().isEmpty()
                    && res.content().get(0) instanceof TextContent tc) {
                return tc.text();
            }
            return "";
        }

        public ToolListReloadReport reloadTools(String serverId) {
            if (serverId == null || !clients.containsKey(serverId)) {
                throw new McpClientBreachException("Unknown server: " + serverId);
            }
            McpClientAdapter client = clients.get(serverId);

            int previousCount = 0;
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, McpClientAdapter> entry : toolToClient.entrySet()) {
                if (entry.getValue().serverName().equals(serverId)) {
                    previousCount++;
                    toRemove.add(entry.getKey());
                }
            }
            for (String key : toRemove) {
                toolToClient.remove(key);
                toolToOriginalName.remove(key);
            }

            List<Tool> newTools = client.listTools();
            List<String> activeOriginalNames = new ArrayList<>();
            for (Tool tool : newTools) {
                String prefixed = McpToolUtils.prefixedToolName(serverId, tool.name());
                toolToClient.put(prefixed, client);
                toolToOriginalName.put(prefixed, tool.name());
                activeOriginalNames.add(tool.name());
            }

            return new ToolListReloadReport(serverId, previousCount, newTools.size(), activeOriginalNames);
        }

        public Map<String, McpClientAdapter> getClients() {
            return Collections.unmodifiableMap(clients);
        }
    }

    /**
     * Scenario 14: Dynamic Tool List Reloading & Registry Re-indexing.
     */
    public ToolListReloadReport reloadServerTools(MultiServerClientRegistry registry, String serverId) {
        if (registry == null || serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("registry and serverId must not be null/blank");
        }
        return registry.reloadTools(serverId);
    }

    /**
     * Scenario 15: MCP Protocol Sampling Handler (Server-to-Client LLM Delegation).
     */
    public SamplingResponse handleSamplingRequest(
            ChatClient.Builder chatClientBuilder,
            SamplingRequest request,
            int tokenCeiling
    ) {
        if (chatClientBuilder == null || request == null) {
            throw new IllegalArgumentException("chatClientBuilder and request must not be null");
        }
        if (request.maxTokens() > tokenCeiling) {
            throw new McpClientBreachException("Requested tokens " + request.maxTokens() + " exceeds ceiling " + tokenCeiling);
        }
        ChatClient chatClient = chatClientBuilder.build();
        var response = chatClient.prompt(request.prompt()).call().chatResponse();
        String content = response.getResult() != null && response.getResult().getOutput() != null
                ? response.getResult().getOutput().getText() : "";
        int tokensUsed = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getTotalTokens() : 0;
        return new SamplingResponse(content, tokensUsed);
    }
}
```
