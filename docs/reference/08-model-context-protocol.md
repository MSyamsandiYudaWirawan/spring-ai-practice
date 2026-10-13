# Spring AI 2.0.1 — Model Context Protocol (MCP)

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 08 (`phase08-model-context-protocol`) & Distributed Tooling

---

Model Context Protocol (MCP) is an open standard created by Anthropic that standardizes how AI models connect to tools, data resources, and prompt templates across distributed environments. Spring AI 2.0.1 provides native first-class MCP server and client integration.

---

## 1. Architecture Overview

- **MCP Host**: The coordinating application (e.g. Spring Boot AI service, Claude Desktop).
- **MCP Client**: Connects 1:1 to an MCP server, initiates tool calls, reads resources, and fetches prompts.
- **MCP Server**: Lightweight process exposing domain-specific tools, resource files, and prompt templates over JSON-RPC 2.0.
- **Transports**:
  - `stdio`: Process-based standard input/output (local executables, Docker containers).
  - `sse`: Server-Sent Events over HTTP (remote microservices).

---

## 2. Dependencies

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp</artifactId>
    <version>2.0.1</version>
</dependency>
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## 3. MCP Server Primitives (`phase08-ex01`)

### 3.1 Server Capabilities
Declares what features the server supports during the initialization handshake:

```java
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

ServerCapabilities capabilities = ServerCapabilities.builder()
        .tools(true)       // Enables tool execution
        .resources(false, true) // subscribe=false, listChanged=true
        .prompts(true)     // Enables prompt templates
        .logging()         // Enables server log notifications
        .build();
```

### 3.2 Tool Specification & Schema Generation
Tools expose JSON Schema for arguments:

```java
import io.modelcontextprotocol.spec.McpSchema.Tool;

Tool tool = Tool.builder()
        .name("query_metrics")
        .description("Queries cluster telemetry metrics")
        .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "metric", Map.of("type", "string", "description", "Metric name"),
                        "windowSeconds", Map.of("type", "integer", "description", "Window size")
                ),
                "required", List.of("metric")
        ))
        .build();
```

### 3.3 Tool Execution & Error Handling (`SyncToolSpecification`)
Spring AI wraps synchronous tool handlers into `SyncToolSpecification`:

```java
import org.springframework.ai.mcp.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

SyncToolSpecification spec = SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, arguments) -> {
            try {
                String metric = (String) arguments.get("metric");
                String telemetry = fetchMetric(metric);
                return CallToolResult.builder()
                        .isError(false)
                        .content(List.of(new TextContent(telemetry)))
                        .build();
            } catch (Exception ex) {
                // MCP protocol requires isError: true when a tool fails
                return CallToolResult.builder()
                        .isError(true)
                        .content(List.of(new TextContent("Failed: " + ex.getMessage())))
                        .build();
            }
        })
        .build();
```

### 3.4 Adapting Spring AI `ToolCallback` to MCP
Convert standard Spring AI `@Tool` callbacks into MCP tool specifications:

```java
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;

SyncToolSpecification syncSpec = McpToolUtils.toSyncToolSpecification(toolCallback);
```

### 3.5 Resources & Prompts
- **Resources**: URIs exposing data (`ResourceContents`, `TextResourceContents`, `BlobResourceContents`).
- **Prompts**: Named prompt templates with parameterized arguments (`PromptMessage`, `GetPromptResult`).

### 3.6 Parameterized Resource URI Templates (RFC 6570)
MCP servers can expose dynamic resource templates with named path parameters (e.g. `"metrics://{cluster}/{service}/{metric}"`). When a client reads a concrete URI, the template matcher extracts the variables into a map for handler routing.

### 3.7 MCP Protocol Log Notifications & Level Filtering
Servers can emit structured log notifications (`notifications/message`) to clients with severity levels (`DEBUG`, `INFO`, `WARNING`, `ERROR`). Dispatchers enforce minimum severity thresholds to prevent log flooding.

---

## 4. MCP Client Primitives (`phase08-ex02`)

### 4.1 Remote Tool Discovery & Name Prefixing
When connecting to multiple MCP servers (e.g., GitHub server, Slack server, Database server), tool names may collide. Spring AI namespaces tools using `McpToolUtils.prefixedToolName`:

```java
import org.springframework.ai.mcp.McpToolUtils;

// Prefix convention compresses words: "github" + "create_issue" -> "g_create_issue"
String namespaced = McpToolUtils.prefixedToolName("github", "create_issue");
```

### 4.2 Adapting Remote MCP Tools to Spring AI `ToolCallback`
```java
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;

List<McpSchema.Tool> remoteTools = client.listTools();
List<ToolCallback> callbacks = remoteTools.stream()
        .map(t -> McpToolUtils.toToolCallback(client, t))
        .toList();
```

### 4.3 Federated Multi-Server Tool Routing
Index tools by their prefixed names for $O(1)$ dispatch:

```java
public class MultiServerClientRegistry {
    private final Map<String, McpClientAdapter> clients = new ConcurrentHashMap<>();
    private final Map<String, McpClientAdapter> toolToClient = new ConcurrentHashMap<>();
    private final Map<String, String> prefixedToOriginalTool = new ConcurrentHashMap<>();

    public void register(String serverId, McpClientAdapter client) {
        clients.put(serverId, client);
        for (var tool : client.listTools()) {
            String prefixed = McpToolUtils.prefixedToolName(serverId, tool.name());
            toolToClient.put(prefixed, client);
            prefixedToOriginalTool.put(prefixed, tool.name());
        }
    }

    public CallToolResult execute(String prefixedToolName, Map<String, Object> arguments) {
        McpClientAdapter client = toolToClient.get(prefixedToolName);
        if (client == null) {
            throw new McpClientBreachException("Unknown tool: " + prefixedToolName);
        }
        String originalName = prefixedToOriginalTool.get(prefixedToolName);
        return client.callTool(originalName, arguments);
    }
}
```

### 4.4 Registering MCP Callbacks with `ChatClient`
```java
ChatClient client = ChatClient.builder(chatModel)
        .defaultToolCallbacks(federatedCallbacks)
        .build();
```

### 4.5 Dynamic Tool List Reloading (`notifications/tools/list_changed`)
MCP servers notify clients when their tool registry changes. Clients invalidate cached tool schemas for that specific server, re-query `listTools()`, update prefixed routing entries, and emit a `ToolListReloadReport`.

### 4.6 MCP Sampling Protocol (Server-to-Client Reverse LLM Delegation)
In the MCP sampling protocol (`sampling/createMessage`), servers request LLM completions from the client application. The client executes the prompt using its configured `ChatClient`, enforces token ceilings, and returns the completion to the server.
