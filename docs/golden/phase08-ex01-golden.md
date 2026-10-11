# Phase 08 Exercise 01 — MCP Server Architecture & Protocol Specifications (Golden Solution)

**Path:** `phase08-ex01-mcp-server/src/main/java/phase08/McpServerUnderTest.java`  
**All 15 Scenarios Verified Passing.**

```java
package phase08;

import io.modelcontextprotocol.server.McpServerFeatures.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import phase08.McpServerContracts.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Exercise implementation under test for Phase 08 Exercise 01:
 * Model Context Protocol (MCP) Server Architecture & Protocol Specifications.
 * <p>
 * 15 repetitive muscle-memory drills across 5 core topics (3 repetitions each).
 */
public class McpServerUnderTest {

    // =========================================================================
    // TOPIC 1: MCP Tool Definition & JSON Input Schema (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 01: Primitive-Typed Tool Definition.
     */
    public Tool buildPrimitiveToolDefinition(
            String name,
            String description,
            Map<String, String> propertyTypes,
            List<String> requiredFields
    ) {
        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("name and description must not be null/blank");
        }
        Map<String, Object> props = new LinkedHashMap<>();
        if (propertyTypes != null) {
            propertyTypes.forEach((k, v) -> props.put(k, Map.of("type", v)));
        }
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", props,
                "required", requiredFields != null ? requiredFields : List.of()
        );
        return new Tool(name, null, description, schema, null, null, null, null);
    }

    /**
     * Scenario 02: Structured Tool Definition with Pre-built Schema Maps.
     */
    public Tool buildStructuredToolDefinition(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> requiredFields
    ) {
        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("name and description must not be null/blank");
        }
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties != null ? properties : Map.of(),
                "required", requiredFields != null ? requiredFields : List.of()
        );
        return new Tool(name, null, description, schema, null, null, null, null);
    }

    /**
     * Scenario 03: Title-Enriched Tool Definition.
     */
    public Tool buildAuditValidatedToolDefinition(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> requiredFields,
            String title
    ) {
        if (name == null || name.isBlank() || description == null || description.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("name, description, and title must not be null/blank");
        }
        Map<String, Object> schema = Map.of(
                "title", title.trim(),
                "type", "object",
                "properties", properties != null ? properties : Map.of(),
                "required", requiredFields != null ? requiredFields : List.of()
        );
        return new Tool(name, null, description, schema, null, null, null, null);
    }

    // =========================================================================
    // TOPIC 2: SyncToolSpecification Handlers (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 04: Standard SyncToolSpecification Handler.
     */
    public SyncToolSpecification createSyncToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        if (tool == null || logicHandler == null) {
            throw new IllegalArgumentException("tool and logicHandler must not be null");
        }
        return new SyncToolSpecification(tool, (exchange, request) -> {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            ServerContext ctx = new ServerContext(
                    exchange != null ? exchange.sessionId() : "default",
                    "standard",
                    "trace-0"
            );
            String output = logicHandler.apply(args, ctx);
            return new CallToolResult(List.of(new TextContent(output)), false, null, null);
        });
    }

    /**
     * Scenario 05: Resilient SyncToolSpecification with Error Trapping.
     */
    public SyncToolSpecification createResilientToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        if (tool == null || logicHandler == null) {
            throw new IllegalArgumentException("tool and logicHandler must not be null");
        }
        return new SyncToolSpecification(tool, (exchange, request) -> {
            try {
                Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
                ServerContext ctx = new ServerContext(
                        exchange != null ? exchange.sessionId() : "default",
                        "standard",
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
     * Scenario 06: Spring AI ToolCallback to MCP SyncToolSpecification Adapter.
     */
    public SyncToolSpecification adaptSpringAiToolCallback(ToolCallback springAiTool) {
        if (springAiTool == null) {
            throw new IllegalArgumentException("springAiTool must not be null");
        }
        return McpToolUtils.toSyncToolSpecification(springAiTool);
    }

    // =========================================================================
    // TOPIC 3: MCP Resource Specifications (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 07: Static Text Resource Specification.
     */
    public SyncResourceSpecification createStaticResourceSpecification(
            String uri,
            String name,
            String mimeType,
            String staticContent
    ) {
        if (uri == null || uri.isBlank() || name == null || name.isBlank() || mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("uri, name, and mimeType must not be null/blank");
        }
        Resource resource = Resource.builder(uri, name).mimeType(mimeType).build();
        return new SyncResourceSpecification(resource, (exchange, request) ->
                new ReadResourceResult(List.of(new TextResourceContents(request.uri(), mimeType, staticContent != null ? staticContent : "")))
        );
    }

    /**
     * Scenario 08: Dynamic Text Resource Specification.
     */
    public SyncResourceSpecification createDynamicResourceSpecification(
            String uri,
            String name,
            String mimeType,
            Function<String, String> contentProvider
    ) {
        if (uri == null || uri.isBlank() || name == null || name.isBlank() || mimeType == null || mimeType.isBlank() || contentProvider == null) {
            throw new IllegalArgumentException("uri, name, mimeType, and contentProvider must not be null/blank");
        }
        Resource resource = Resource.builder(uri, name).mimeType(mimeType).build();
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            String content = contentProvider.apply(request.uri());
            return new ReadResourceResult(List.of(new TextResourceContents(request.uri(), mimeType, content != null ? content : "")));
        });
    }

    /**
     * Scenario 09: Binary / Blob Resource Specification.
     */
    public SyncResourceSpecification createBinaryResourceSpecification(
            String uri,
            String name,
            String mimeType,
            Function<String, String> base64Provider
    ) {
        if (uri == null || uri.isBlank() || name == null || name.isBlank() || mimeType == null || mimeType.isBlank() || base64Provider == null) {
            throw new IllegalArgumentException("uri, name, mimeType, and base64Provider must not be null/blank");
        }
        Resource resource = Resource.builder(uri, name).mimeType(mimeType).build();
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            String b64 = base64Provider.apply(request.uri());
            return new ReadResourceResult(List.of(new BlobResourceContents(request.uri(), mimeType, b64 != null ? b64 : "")));
        });
    }

    // =========================================================================
    // TOPIC 4: MCP Prompt Template Specifications (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Simple Zero-Argument Prompt Specification.
     */
    public SyncPromptSpecification createSimplePromptSpecification(
            String promptName,
            String description,
            Function<Map<String, Object>, String> promptFormatter
    ) {
        if (promptName == null || promptName.isBlank() || promptFormatter == null) {
            throw new IllegalArgumentException("promptName and promptFormatter must not be null/blank");
        }
        Prompt prompt = new Prompt(promptName, null, description, List.of());
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            String text = promptFormatter.apply(args);
            return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
        });
    }

    /**
     * Scenario 11: Parameterized Multi-Argument Prompt Specification.
     */
    public SyncPromptSpecification createParameterizedPromptSpecification(
            String promptName,
            String description,
            List<PromptArgument> arguments,
            BiFunction<String, Map<String, Object>, String> promptFormatter
    ) {
        if (promptName == null || promptName.isBlank() || promptFormatter == null) {
            throw new IllegalArgumentException("promptName and promptFormatter must not be null/blank");
        }
        Prompt prompt = new Prompt(promptName, null, description, arguments != null ? arguments : List.of());
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            String text = promptFormatter.apply(request.name(), args);
            return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
        });
    }

    /**
     * Scenario 12: Role-Enforced Prompt Specification.
     */
    public SyncPromptSpecification createRoleEnforcedPromptSpecification(
            String promptName,
            String description,
            List<PromptArgument> arguments,
            Role messageRole,
            BiFunction<String, Map<String, Object>, String> promptFormatter
    ) {
        if (promptName == null || promptName.isBlank() || messageRole == null || promptFormatter == null) {
            throw new IllegalArgumentException("promptName, messageRole, and promptFormatter must not be null/blank");
        }
        Prompt prompt = new Prompt(promptName, null, description, arguments != null ? arguments : List.of());
        return new SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            String text = promptFormatter.apply(request.name(), args);
            return new GetPromptResult(description, List.of(new PromptMessage(messageRole, new TextContent(text))));
        });
    }

    // =========================================================================
    // TOPIC 5: Server Capabilities & Protocol Filtering (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Server Capabilities Declaration.
     */
    public ServerCapabilities createServerCapabilities(
            boolean enableTools,
            boolean enableResources,
            boolean enablePrompts,
            boolean enableLogging
    ) {
        ServerCapabilities.Builder builder = ServerCapabilities.builder();
        if (enableTools) {
            builder.tools(true);
        }
        if (enableResources) {
            builder.resources(true, true);
        }
        if (enablePrompts) {
            builder.prompts(true);
        }
        if (enableLogging) {
            builder.logging();
        }
        return builder.build();
    }

    /**
     * Scenario 14: Tool Filtering by Allowlist.
     */
    public List<Tool> filterToolsByAllowlist(List<Tool> allTools, Set<String> allowedNames) {
        if (allTools == null || allowedNames == null) {
            throw new IllegalArgumentException("allTools and allowedNames must not be null");
        }
        return allTools.stream()
                .filter(t -> allowedNames.contains(t.name()))
                .toList();
    }

    /**
     * Scenario 15: Tool Filtering by Name Prefix.
     */
    public List<Tool> filterToolsByPrefix(List<Tool> allTools, String requiredPrefix) {
        if (allTools == null || requiredPrefix == null || requiredPrefix.isBlank()) {
            throw new IllegalArgumentException("allTools and requiredPrefix must not be null/blank");
        }
        return allTools.stream()
                .filter(t -> t.name().startsWith(requiredPrefix))
                .toList();
    }
}
```
