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
 * Implement all 15 scenarios in this file.
 */
public class McpServerUnderTest {

    // =========================================================================
    // TOPIC 1: MCP Tool Definition & JSON Input Schema (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 01: Primitive-Typed Tool Definition.
     * <p>
     * Instructions:
     * - Validate name and description are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build properties map where each property name maps to Map.of("type", type).
     * - Build JSON schema:
     *   "type" -> "object"
     *   "properties" -> propertiesMap
     *   "required" -> requiredFields != null ? requiredFields : List.of()
     * - Return new Tool(name, null, description, schema, null, null, null, null).
     */
    public Tool buildPrimitiveToolDefinition(
            String name,
            String description,
            Map<String, String> propertyTypes,
            List<String> requiredFields
    ) {
        // DEFECT (Scenario 1): Returns null
        return null;
    }

    /**
     * Scenario 02: Structured Tool Definition with Pre-built Schema Maps.
     * <p>
     * Instructions:
     * - Validate name and description are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Construct JSON schema:
     *   "type" -> "object"
     *   "properties" -> properties != null ? properties : Map.of()
     *   "required" -> requiredFields != null ? requiredFields : List.of()
     * - Return new Tool(name, null, description, schema, null, null, null, null).
     */
    public Tool buildStructuredToolDefinition(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> requiredFields
    ) {
        // DEFECT (Scenario 2): Returns null
        return null;
    }

    /**
     * Scenario 03: Title-Enriched Tool Definition.
     * <p>
     * Instructions:
     * - Validate name, description, and title are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Construct JSON schema:
     *   "title" -> title.trim()
     *   "type" -> "object"
     *   "properties" -> properties != null ? properties : Map.of()
     *   "required" -> requiredFields != null ? requiredFields : List.of()
     * - Return new Tool(name, null, description, schema, null, null, null, null).
     */
    public Tool buildAuditValidatedToolDefinition(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> requiredFields,
            String title
    ) {
        // DEFECT (Scenario 3): Returns null
        return null;
    }

    // =========================================================================
    // TOPIC 2: SyncToolSpecification Handlers (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 04: Standard SyncToolSpecification Handler.
     * <p>
     * Instructions:
     * - Validate tool != null and logicHandler != null; throw {@link IllegalArgumentException} otherwise.
     * - Return new SyncToolSpecification(tool, (exchange, request) -> {
     *     Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *     ServerContext ctx = new ServerContext(
     *         exchange != null ? exchange.sessionId() : "default",
     *         "standard",
     *         "trace-0"
     *     );
     *     String output = logicHandler.apply(args, ctx);
     *     return new CallToolResult(List.of(new TextContent(output)), false, null, null);
     *   }).
     */
    public SyncToolSpecification createSyncToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        // DEFECT (Scenario 4): Returns null
        return null;
    }

    /**
     * Scenario 05: Resilient SyncToolSpecification with Error Trapping.
     * <p>
     * Instructions:
     * - Validate tool != null and logicHandler != null; throw {@link IllegalArgumentException} otherwise.
     * - Return new SyncToolSpecification(tool, (exchange, request) -> {
     *     try {
     *         Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *         ServerContext ctx = new ServerContext(
     *             exchange != null ? exchange.sessionId() : "default",
     *             "standard",
     *             "trace-0"
     *         );
     *         String output = logicHandler.apply(args, ctx);
     *         return new CallToolResult(List.of(new TextContent(output)), false, null, null);
     *     } catch (Throwable t) {
     *         return new CallToolResult(List.of(new TextContent("Error: " + t.getMessage())), true, null, null);
     *     }
     *   }).
     */
    public SyncToolSpecification createResilientToolSpecification(
            Tool tool,
            BiFunction<Map<String, Object>, ServerContext, String> logicHandler
    ) {
        // DEFECT (Scenario 5): Returns null
        return null;
    }

    /**
     * Scenario 06: Spring AI ToolCallback to MCP SyncToolSpecification Adapter.
     * <p>
     * Instructions:
     * - Validate springAiTool != null; throw {@link IllegalArgumentException} otherwise.
     * - Use McpToolUtils.toSyncToolSpecification(springAiTool).
     * - Return the adapted SyncToolSpecification.
     */
    public SyncToolSpecification adaptSpringAiToolCallback(ToolCallback springAiTool) {
        // DEFECT (Scenario 6): Returns null
        return null;
    }

    // =========================================================================
    // TOPIC 3: MCP Resource Specifications (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 07: Static Text Resource Specification.
     * <p>
     * Instructions:
     * - Validate uri, name, and mimeType are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build Resource: Resource.builder(uri, name).mimeType(mimeType).build().
     * - Return new SyncResourceSpecification(resource, (exchange, request) -> {
     *     return new ReadResourceResult(List.of(new TextResourceContents(request.uri(), mimeType, staticContent != null ? staticContent : "")));
     *   }).
     */
    public SyncResourceSpecification createStaticResourceSpecification(
            String uri,
            String name,
            String mimeType,
            String staticContent
    ) {
        // DEFECT (Scenario 7): Returns null
        return null;
    }

    /**
     * Scenario 08: Dynamic Text Resource Specification.
     * <p>
     * Instructions:
     * - Validate uri, name, mimeType, and contentProvider are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build Resource: Resource.builder(uri, name).mimeType(mimeType).build().
     * - Return new SyncResourceSpecification(resource, (exchange, request) -> {
     *     String content = contentProvider.apply(request.uri());
     *     return new ReadResourceResult(List.of(new TextResourceContents(request.uri(), mimeType, content != null ? content : "")));
     *   }).
     */
    public SyncResourceSpecification createDynamicResourceSpecification(
            String uri,
            String name,
            String mimeType,
            Function<String, String> contentProvider
    ) {
        // DEFECT (Scenario 8): Returns null
        return null;
    }

    /**
     * Scenario 09: Binary / Blob Resource Specification.
     * <p>
     * Instructions:
     * - Validate uri, name, mimeType, and base64Provider are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build Resource: Resource.builder(uri, name).mimeType(mimeType).build().
     * - Return new SyncResourceSpecification(resource, (exchange, request) -> {
     *     String b64 = base64Provider.apply(request.uri());
     *     return new ReadResourceResult(List.of(new BlobResourceContents(request.uri(), mimeType, b64 != null ? b64 : "")));
     *   }).
     */
    public SyncResourceSpecification createBinaryResourceSpecification(
            String uri,
            String name,
            String mimeType,
            Function<String, String> base64Provider
    ) {
        // DEFECT (Scenario 9): Returns null
        return null;
    }

    // =========================================================================
    // TOPIC 4: MCP Prompt Template Specifications (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Simple Zero-Argument Prompt Specification.
     * <p>
     * Instructions:
     * - Validate promptName and promptFormatter are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build Prompt: new Prompt(promptName, null, description, List.of()).
     * - Return new SyncPromptSpecification(prompt, (exchange, request) -> {
     *     Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *     String text = promptFormatter.apply(args);
     *     return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
     *   }).
     */
    public SyncPromptSpecification createSimplePromptSpecification(
            String promptName,
            String description,
            Function<Map<String, Object>, String> promptFormatter
    ) {
        // DEFECT (Scenario 10): Returns null
        return null;
    }

    /**
     * Scenario 11: Parameterized Multi-Argument Prompt Specification.
     * <p>
     * Instructions:
     * - Validate promptName and promptFormatter are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build Prompt: new Prompt(promptName, null, description, arguments != null ? arguments : List.of()).
     * - Return new SyncPromptSpecification(prompt, (exchange, request) -> {
     *     Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *     String text = promptFormatter.apply(request.name(), args);
     *     return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
     *   }).
     */
    public SyncPromptSpecification createParameterizedPromptSpecification(
            String promptName,
            String description,
            List<PromptArgument> arguments,
            BiFunction<String, Map<String, Object>, String> promptFormatter
    ) {
        // DEFECT (Scenario 11): Returns null
        return null;
    }

    /**
     * Scenario 12: Role-Enforced Prompt Specification.
     * <p>
     * Instructions:
     * - Validate promptName, messageRole, and promptFormatter are not null/blank; throw {@link IllegalArgumentException} otherwise.
     * - Build Prompt: new Prompt(promptName, null, description, arguments != null ? arguments : List.of()).
     * - Return new SyncPromptSpecification(prompt, (exchange, request) -> {
     *     Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
     *     String text = promptFormatter.apply(request.name(), args);
     *     return new GetPromptResult(description, List.of(new PromptMessage(messageRole, new TextContent(text))));
     *   }).
     */
    public SyncPromptSpecification createRoleEnforcedPromptSpecification(
            String promptName,
            String description,
            List<PromptArgument> arguments,
            Role messageRole,
            BiFunction<String, Map<String, Object>, String> promptFormatter
    ) {
        // DEFECT (Scenario 12): Returns null
        return null;
    }

    // =========================================================================
    // TOPIC 5: Server Capabilities & Protocol Filtering (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Server Capabilities Declaration.
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
        // DEFECT (Scenario 13): Returns null
        return null;
    }

    /**
     * Scenario 14: Tool Filtering by Allowlist.
     * <p>
     * Instructions:
     * - Validate allTools != null and allowedNames != null; throw {@link IllegalArgumentException} otherwise.
     * - Return list of tools where allowedNames.contains(tool.name()).
     */
    public List<Tool> filterToolsByAllowlist(List<Tool> allTools, Set<String> allowedNames) {
        // DEFECT (Scenario 14): Returns empty list
        return List.of();
    }

    /**
     * Scenario 15: Tool Filtering by Name Prefix.
     * <p>
     * Instructions:
     * - Validate allTools != null and requiredPrefix != null && !requiredPrefix.isBlank(); throw {@link IllegalArgumentException} otherwise.
     * - Return list of tools where tool.name().startsWith(requiredPrefix).
     */
    public List<Tool> filterToolsByPrefix(List<Tool> allTools, String requiredPrefix) {
        // DEFECT (Scenario 15): Returns empty list
        return List.of();
    }
}
