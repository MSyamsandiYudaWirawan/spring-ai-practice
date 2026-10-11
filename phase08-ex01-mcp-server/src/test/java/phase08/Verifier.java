package phase08;

import io.modelcontextprotocol.server.McpServerFeatures.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import phase08.McpServerContracts.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 08 Exercise 01:
 * Model Context Protocol (MCP) Server Architecture & Protocol Specifications.
 * <p>
 * 15 repetitive muscle-memory drills across 5 core topics (3 repetitions each).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail, List<String> hints) {
        public ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {
            this(scenarioNumber, name, passed, errorDetail, List.of());
        }
    }

    public static void main(String[] args) {
        int selected = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selected = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selected);
        printReport(results);

        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            fail("Verifier detected scenario failures. See printed report above.");
        }
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        McpServerUnderTest underTest = new McpServerUnderTest();
        List<ScenarioResult> list = new ArrayList<>();
        if (selected == 0 || selected == 1) list.add(verifyScenario1(underTest));
        if (selected == 0 || selected == 2) list.add(verifyScenario2(underTest));
        if (selected == 0 || selected == 3) list.add(verifyScenario3(underTest));
        if (selected == 0 || selected == 4) list.add(verifyScenario4(underTest));
        if (selected == 0 || selected == 5) list.add(verifyScenario5(underTest));
        if (selected == 0 || selected == 6) list.add(verifyScenario6(underTest));
        if (selected == 0 || selected == 7) list.add(verifyScenario7(underTest));
        if (selected == 0 || selected == 8) list.add(verifyScenario8(underTest));
        if (selected == 0 || selected == 9) list.add(verifyScenario9(underTest));
        if (selected == 0 || selected == 10) list.add(verifyScenario10(underTest));
        if (selected == 0 || selected == 11) list.add(verifyScenario11(underTest));
        if (selected == 0 || selected == 12) list.add(verifyScenario12(underTest));
        if (selected == 0 || selected == 13) list.add(verifyScenario13(underTest));
        if (selected == 0 || selected == 14) list.add(verifyScenario14(underTest));
        if (selected == 0 || selected == 15) list.add(verifyScenario15(underTest));
        return list;
    }

    // =========================================================================
    // TOPIC 1: MCP Tool Definition & JSON Input Schema (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1(McpServerUnderTest underTest) {
        String name = "ToolDefinition (Primitive-Typed JSON Schema)";
        List<String> hints = List.of(
                "McpSchema.Tool wraps tool name, description, and an inputSchema JSON Object map.",
                "Throw IllegalArgumentException on blank tool name; validate inputSchema type is 'object'.",
                "new Tool(name, null, description, Map.of(\"type\", \"object\", \"properties\", propMap, \"required\", required), null, null, null, null);"
        );
        try {
            try {
                underTest.buildPrimitiveToolDefinition(" ", "desc", Map.of(), List.of());
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on blank name", hints);
            } catch (IllegalArgumentException expected) {}

            Tool tool = underTest.buildPrimitiveToolDefinition(
                    "lookupUser",
                    "Fetches user by email",
                    Map.of("email", "string", "retries", "integer"),
                    List.of("email")
            );

            if (tool == null) {
                return new ScenarioResult(1, name, false, "buildPrimitiveToolDefinition returned null", hints);
            }
            if (!"lookupUser".equals(tool.name()) || !"Fetches user by email".equals(tool.description())) {
                return new ScenarioResult(1, name, false, "Tool name or description mismatch", hints);
            }
            if (tool.inputSchema() == null || !"object".equals(tool.inputSchema().get("type"))) {
                return new ScenarioResult(1, name, false, "Input schema type must be 'object'", hints);
            }
            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2(McpServerUnderTest underTest) {
        String name = "ToolDefinition (Structured Properties Map)";
        List<String> hints = List.of(
                "Structured tool definitions specify detailed property maps with parameter types and descriptions.",
                "Preserve property descriptions and types in inputSchema properties map.",
                "Map<String, Object> schema = Map.of(\"type\", \"object\", \"properties\", properties, \"required\", required); return new Tool(name, null, desc, schema, null, null, null, null);"
        );
        try {
            Map<String, Object> props = Map.of(
                    "service", Map.of("type", "string", "description", "target microservice"),
                    "timeoutMs", Map.of("type", "number")
            );

            Tool tool = underTest.buildStructuredToolDefinition(
                    "restartService",
                    "Restarts target pod or deployment",
                    props,
                    List.of("service")
            );

            if (tool == null) {
                return new ScenarioResult(2, name, false, "buildStructuredToolDefinition returned null", hints);
            }
            if (!"restartService".equals(tool.name())) {
                return new ScenarioResult(2, name, false, "Tool name mismatch", hints);
            }
            if (tool.inputSchema() == null || !tool.inputSchema().containsKey("properties")) {
                return new ScenarioResult(2, name, false, "Input schema properties missing", hints);
            }
            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3(McpServerUnderTest underTest) {
        String name = "ToolDefinition (Title & Metadata Enriched Schema)";
        List<String> hints = List.of(
                "Tool definitions can include schema titles and audit metadata to conform to API specifications.",
                "Set 'title' key in inputSchema map alongside type, properties, and required.",
                "Map<String, Object> schema = new HashMap<>(Map.of(\"type\", \"object\", \"title\", title, \"properties\", props, \"required\", req)); return new Tool(name, null, desc, schema, null, null, null, null);"
        );
        try {
            Tool tool = underTest.buildAuditValidatedToolDefinition(
                    "auditCluster",
                    "Audits cluster configuration",
                    Map.of("namespace", Map.of("type", "string")),
                    List.of("namespace"),
                    "ClusterAuditSpec"
            );

            if (tool == null) {
                return new ScenarioResult(3, name, false, "buildAuditValidatedToolDefinition returned null", hints);
            }
            if (!"ClusterAuditSpec".equals(tool.inputSchema().get("title"))) {
                return new ScenarioResult(3, name, false, "Expected title 'ClusterAuditSpec' in input schema", hints);
            }
            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 2: SyncToolSpecification Handlers (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4(McpServerUnderTest underTest) {
        String name = "SyncToolSpecification (Standard Execution Handler)";
        List<String> hints = List.of(
                "SyncToolSpecification binds an McpSchema.Tool with a BiFunction execution callHandler.",
                "Call handler receives (exchange, CallToolRequest) and produces CallToolResult wrapping List<Content>.",
                "new SyncToolSpecification(tool, (exchange, req) -> new CallToolResult(List.of(new TextContent(handler.apply(req.arguments(), null))), false));"
        );
        try {
            Tool tool = new Tool("echo", null, "Echo tool", Map.of("type", "object"), null, null, null, null);
            SyncToolSpecification spec = underTest.createSyncToolSpecification(tool, (args, ctx) -> "echoed: " + args.get("msg"));

            if (spec == null || spec.callHandler() == null) {
                return new ScenarioResult(4, name, false, "createSyncToolSpecification returned null spec or callHandler", hints);
            }

            CallToolResult res = spec.callHandler().apply(null, new CallToolRequest("echo", Map.of("msg", "hello")));
            if (res == null || res.content() == null || res.content().isEmpty()) {
                return new ScenarioResult(4, name, false, "Execution result was empty", hints);
            }
            TextContent text = (TextContent) res.content().get(0);
            if (!"echoed: hello".equals(text.text())) {
                return new ScenarioResult(4, name, false, "Expected output 'echoed: hello', got: " + text.text(), hints);
            }
            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5(McpServerUnderTest underTest) {
        String name = "SyncToolSpecification (Resilient Error Signal Trapping)";
        List<String> hints = List.of(
                "Resilient MCP tool specifications catch unhandled exceptions and return an error CallToolResult instead of crashing the server.",
                "Catch Throwable inside the callHandler lambda and return new CallToolResult(List.of(new TextContent(t.getMessage())), true).",
                "try { return new CallToolResult(List.of(new TextContent(handler.apply(...))), false); } catch (Exception e) { return new CallToolResult(List.of(new TextContent(\"Error: \" + e.getMessage())), true); }"
        );
        try {
            Tool tool = new Tool("failing", null, "Failing tool", Map.of("type", "object"), null, null, null, null);
            SyncToolSpecification spec = underTest.createResilientToolSpecification(tool, (args, ctx) -> {
                throw new RuntimeException("Database unreachable");
            });

            if (spec == null || spec.callHandler() == null) {
                return new ScenarioResult(5, name, false, "createResilientToolSpecification returned null", hints);
            }

            CallToolResult res = spec.callHandler().apply(null, new CallToolRequest("failing", Map.of()));
            if (res == null || !Boolean.TRUE.equals(res.isError())) {
                return new ScenarioResult(5, name, false, "Expected res.isError() to be true on exception", hints);
            }
            TextContent text = (TextContent) res.content().get(0);
            if (!text.text().contains("Database unreachable")) {
                return new ScenarioResult(5, name, false, "Expected exception message in result content", hints);
            }
            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6(McpServerUnderTest underTest) {
        String name = "SyncToolSpecification (Spring AI ToolCallback Adaptation)";
        List<String> hints = List.of(
                "Adapt Spring AI's ToolCallback to an MCP SyncToolSpecification.",
                "Extract name, description, and inputSchema from toolCallback.getToolDefinition().",
                "Wrap toolCallback.call(jsonInput) inside SyncToolSpecification callHandler; map String output to TextContent."
        );
        try {
            ToolCallback dummyCallback = new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name("springAiEcho")
                            .description("Spring AI echo tool")
                            .inputSchema("{\"type\":\"object\"}")
                            .build();
                }

                @Override
                public String call(String toolInput) {
                    return "callback output";
                }
            };

            SyncToolSpecification spec = underTest.adaptSpringAiToolCallback(dummyCallback);
            if (spec == null || spec.tool() == null) {
                return new ScenarioResult(6, name, false, "adaptSpringAiToolCallback returned null", hints);
            }
            if (!"springAiEcho".equals(spec.tool().name())) {
                return new ScenarioResult(6, name, false, "Adapted tool name mismatch", hints);
            }
            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 3: MCP Resource Specifications (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7(McpServerUnderTest underTest) {
        String name = "ResourceSpecification (Static Text Resource)";
        List<String> hints = List.of(
                "SyncResourceSpecification exposes fixed static resources via standard MCP resource URIs.",
                "Read handler returns ReadResourceResult containing TextResourceContents with text and mimeType.",
                "new SyncResourceSpecification(resource, (ex, req) -> new ReadResourceResult(List.of(new TextResourceContents(uri, mimeType, staticText))));"
        );
        try {
            SyncResourceSpecification spec = underTest.createStaticResourceSpecification(
                    "config://system/env", "SystemEnvironment", "text/plain", "ACTIVE"
            );

            if (spec == null || spec.resource() == null || spec.readHandler() == null) {
                return new ScenarioResult(7, name, false, "createStaticResourceSpecification returned null spec or handler", hints);
            }

            ReadResourceResult res = spec.readHandler().apply(null, new ReadResourceRequest("config://system/env"));
            if (res == null || res.contents().isEmpty()) {
                return new ScenarioResult(7, name, false, "ReadResourceResult contents was empty", hints);
            }
            TextResourceContents content = (TextResourceContents) res.contents().get(0);
            if (!"ACTIVE".equals(content.text())) {
                return new ScenarioResult(7, name, false, "Expected content 'ACTIVE', got: " + content.text(), hints);
            }
            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8(McpServerUnderTest underTest) {
        String name = "ResourceSpecification (Dynamic Text Provider)";
        List<String> hints = List.of(
                "Dynamic resource specifications generate live resource content on-demand via a provider function.",
                "Invoke the textProvider function with the requested URI inside the readHandler.",
                "String liveText = textProvider.apply(req.uri()); return new ReadResourceResult(List.of(new TextResourceContents(req.uri(), mimeType, liveText)));"
        );
        try {
            SyncResourceSpecification spec = underTest.createDynamicResourceSpecification(
                    "metrics://jvm/memory", "JvmMemory", "application/json", uri -> "{\"heapUsedMb\": 512}"
            );

            if (spec == null || spec.readHandler() == null) {
                return new ScenarioResult(8, name, false, "createDynamicResourceSpecification returned null", hints);
            }

            ReadResourceResult res = spec.readHandler().apply(null, new ReadResourceRequest("metrics://jvm/memory"));
            TextResourceContents content = (TextResourceContents) res.contents().get(0);
            if (!content.text().contains("512")) {
                return new ScenarioResult(8, name, false, "Dynamic content missing expected heap value", hints);
            }
            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9(McpServerUnderTest underTest) {
        String name = "ResourceSpecification (Binary Blob Resource)";
        List<String> hints = List.of(
                "Binary MCP resources return base64-encoded binary content using BlobResourceContents.",
                "Pass base64 data to BlobResourceContents(req.uri(), mimeType, base64Blob).",
                "new SyncResourceSpecification(resource, (ex, req) -> new ReadResourceResult(List.of(new BlobResourceContents(req.uri(), mimeType, blobProvider.apply(req.uri())))));"
        );
        try {
            SyncResourceSpecification spec = underTest.createBinaryResourceSpecification(
                    "files://icons/app.png", "AppIcon", "image/png", uri -> "iVBORw0KGgoAAAANSUhEUg=="
            );

            if (spec == null || spec.readHandler() == null) {
                return new ScenarioResult(9, name, false, "createBinaryResourceSpecification returned null", hints);
            }

            ReadResourceResult res = spec.readHandler().apply(null, new ReadResourceRequest("files://icons/app.png"));
            BlobResourceContents blob = (BlobResourceContents) res.contents().get(0);
            if (!"iVBORw0KGgoAAAANSUhEUg==".equals(blob.blob())) {
                return new ScenarioResult(9, name, false, "Expected base64 blob content mismatch", hints);
            }
            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 4: MCP Prompt Template Specifications (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10(McpServerUnderTest underTest) {
        String name = "PromptSpecification (Simple Zero-Argument Prompt)";
        List<String> hints = List.of(
                "SyncPromptSpecification registers reusable prompt templates with the MCP server.",
                "Prompt handler returns GetPromptResult wrapping PromptMessage with Role.USER and TextContent.",
                "new SyncPromptSpecification(prompt, (ex, req) -> new GetPromptResult(\"\", List.of(new PromptMessage(Role.USER, new TextContent(handler.apply(req.arguments()))))));"
        );
        try {
            SyncPromptSpecification spec = underTest.createSimplePromptSpecification(
                    "systemDiagnosis", "System diagnostic prompt", args -> "Perform full health check."
            );

            if (spec == null || spec.promptHandler() == null) {
                return new ScenarioResult(10, name, false, "createSimplePromptSpecification returned null", hints);
            }

            GetPromptResult res = spec.promptHandler().apply(null, new GetPromptRequest("systemDiagnosis", Map.of()));
            PromptMessage msg = res.messages().get(0);
            TextContent content = (TextContent) msg.content();
            if (!"Perform full health check.".equals(content.text())) {
                return new ScenarioResult(10, name, false, "Prompt message text mismatch", hints);
            }
            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11(McpServerUnderTest underTest) {
        String name = "PromptSpecification (Parameterized Multi-Argument Prompt)";
        List<String> hints = List.of(
                "Parameterized prompts accept named arguments declared in List<PromptArgument>.",
                "Pass arguments map to generator function: generator.apply(req.name(), req.arguments()).",
                "GetPromptResult with PromptMessage(Role.USER, new TextContent(resultText));"
        );
        try {
            List<PromptArgument> args = List.of(new PromptArgument("service", "target service", true));
            SyncPromptSpecification spec = underTest.createParameterizedPromptSpecification(
                    "serviceTriage", "Triage prompt", args, (name1, map) -> "Triage for service: " + map.get("service")
            );

            if (spec == null || spec.promptHandler() == null) {
                return new ScenarioResult(11, name, false, "createParameterizedPromptSpecification returned null", hints);
            }

            GetPromptResult res = spec.promptHandler().apply(null, new GetPromptRequest("serviceTriage", Map.of("service", "billing")));
            TextContent content = (TextContent) res.messages().get(0).content();
            if (!"Triage for service: billing".equals(content.text())) {
                return new ScenarioResult(11, name, false, "Parameterized prompt text mismatch: " + content.text(), hints);
            }
            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(11, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12(McpServerUnderTest underTest) {
        String name = "PromptSpecification (Role-Enforced System/User Prompt)";
        List<String> hints = List.of(
                "Role-enforced prompts explicitly attach a specified Role (e.g. ASSISTANT, USER, SYSTEM) to the generated prompt message.",
                "Use the provided Role parameter when constructing PromptMessage(role, textContent).",
                "new PromptMessage(enforcedRole, new TextContent(generator.apply(req.name(), req.arguments())));"
        );
        try {
            SyncPromptSpecification spec = underTest.createRoleEnforcedPromptSpecification(
                    "systemInit", "Initialization prompt", List.of(), Role.ASSISTANT, (n, m) -> "You are a cloud architect."
            );

            if (spec == null || spec.promptHandler() == null) {
                return new ScenarioResult(12, name, false, "createRoleEnforcedPromptSpecification returned null", hints);
            }

            GetPromptResult res = spec.promptHandler().apply(null, new GetPromptRequest("systemInit", Map.of()));
            PromptMessage msg = res.messages().get(0);
            if (msg.role() != Role.ASSISTANT) {
                return new ScenarioResult(12, name, false, "Expected Role.ASSISTANT, got: " + msg.role(), hints);
            }
            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(12, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 5: Server Capabilities & Protocol Filtering (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13(McpServerUnderTest underTest) {
        String name = "ServerCapabilities (Feature Negotiation Flags)";
        List<String> hints = List.of(
                "ServerCapabilities declares supported MCP protocol features (tools, resources, prompts, logging) to clients during initialization.",
                "Null out capabilities that are disabled; instantiate capability objects for those enabled.",
                "ServerCapabilities.builder().tools(enableTools ? new ServerCapabilities.ToolCapabilities(...) : null).resources(...).prompts(...).logging(...).build();"
        );
        try {
            ServerCapabilities caps = underTest.createServerCapabilities(true, true, false, true);
            if (caps == null) {
                return new ScenarioResult(13, name, false, "createServerCapabilities returned null", hints);
            }
            if (caps.tools() == null || caps.resources() == null || caps.logging() == null) {
                return new ScenarioResult(13, name, false, "Configured capabilities missing", hints);
            }
            if (caps.prompts() != null) {
                return new ScenarioResult(13, name, false, "Prompts should be null when disabled", hints);
            }
            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(13, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14(McpServerUnderTest underTest) {
        String name = "ProtocolFilter (Allowlist Tool Filtering)";
        List<String> hints = List.of(
                "Protocol filters restrict available MCP tools according to an allowlist of permitted tool names.",
                "Check tool names against the allowlist Set: allowlist.contains(tool.name()).",
                "allTools.stream().filter(t -> allowlist.contains(t.name())).toList();"
        );
        try {
            List<Tool> allTools = List.of(
                    new Tool("readLog", null, "Read", Map.of("type", "object"), null, null, null, null),
                    new Tool("dropDatabase", null, "Drop", Map.of("type", "object"), null, null, null, null),
                    new Tool("restartPod", null, "Restart", Map.of("type", "object"), null, null, null, null)
            );

            List<Tool> filtered = underTest.filterToolsByAllowlist(allTools, Set.of("readLog", "restartPod"));
            if (filtered == null || filtered.size() != 2) {
                return new ScenarioResult(14, name, false, "Expected 2 tools in allowlist, got: " + (filtered == null ? "null" : filtered.size()), hints);
            }
            if (filtered.stream().anyMatch(t -> "dropDatabase".equals(t.name()))) {
                return new ScenarioResult(14, name, false, "Forbidden tool 'dropDatabase' passed through filter", hints);
            }
            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(14, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15(McpServerUnderTest underTest) {
        String name = "ProtocolFilter (Prefix-Based Tool Routing)";
        List<String> hints = List.of(
                "Prefix routing filters tools by category or domain prefix (e.g., 'k8s_', 'db_').",
                "Match tool names starting with prefix: tool.name().startsWith(prefix).",
                "allTools.stream().filter(t -> t.name().startsWith(prefix)).toList();"
        );
        try {
            List<Tool> allTools = List.of(
                    new Tool("k8s_restartPod", null, "Restart", Map.of("type", "object"), null, null, null, null),
                    new Tool("k8s_getLogs", null, "Logs", Map.of("type", "object"), null, null, null, null),
                    new Tool("db_queryMetrics", null, "Metrics", Map.of("type", "object"), null, null, null, null)
            );

            List<Tool> k8sTools = underTest.filterToolsByPrefix(allTools, "k8s_");
            if (k8sTools == null || k8sTools.size() != 2) {
                return new ScenarioResult(15, name, false, "Expected 2 k8s tools, got: " + (k8sTools == null ? "null" : k8sTools.size()), hints);
            }
            if (k8sTools.stream().anyMatch(t -> "db_queryMetrics".equals(t.name()))) {
                return new ScenarioResult(15, name, false, "db tool passed through k8s prefix filter", hints);
            }
            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(15, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // REPORT FORMATTER
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 08 EXERCISE 01: MCP SERVER ARCHITECTURE & SPECIFICATIONS");
        System.out.println("===============================================================================");
        int passed = 0;
        String[] hintLabels = {"[HINT 1 (Concept)]", "[HINT 2 (Edge Case)]", "[HINT 3 (Code Pattern)]"};

        for (ScenarioResult r : results) {
            String tag = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", tag, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null && !r.hints().isEmpty()) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        String label = i < hintLabels.length ? hintLabels[i] : String.format("[HINT %d]", i + 1);
                        System.out.printf("         --> %s %s%n", label, r.hints().get(i));
                    }
                }
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        if (passed == results.size()) {
            System.out.println("  VERDICT: ALL 15 GATES PASSED (exit code 0)");
        } else {
            System.out.printf("  VERDICT: %d FINDINGS DETECTED (exit code 99)%n", results.size() - passed);
        }
        System.out.println("===============================================================================");
    }
}
