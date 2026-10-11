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
 * Exits with status 99 on FAIL.
 * Exits with status 0 on PASS.
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

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
        try {
            try {
                underTest.buildPrimitiveToolDefinition(" ", "desc", Map.of(), List.of());
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on blank name");
            } catch (IllegalArgumentException expected) {}

            Tool tool = underTest.buildPrimitiveToolDefinition(
                    "lookupUser",
                    "Fetches user by email",
                    Map.of("email", "string", "retries", "integer"),
                    List.of("email")
            );

            if (tool == null) {
                return new ScenarioResult(1, name, false, "buildPrimitiveToolDefinition returned null");
            }
            if (!"lookupUser".equals(tool.name()) || !"Fetches user by email".equals(tool.description())) {
                return new ScenarioResult(1, name, false, "Tool name or description mismatch");
            }
            if (tool.inputSchema() == null || !"object".equals(tool.inputSchema().get("type"))) {
                return new ScenarioResult(1, name, false, "Input schema type must be 'object'");
            }
            return new ScenarioResult(1, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2(McpServerUnderTest underTest) {
        String name = "ToolDefinition (Structured Properties Map)";
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
                return new ScenarioResult(2, name, false, "buildStructuredToolDefinition returned null");
            }
            if (!"restartService".equals(tool.name())) {
                return new ScenarioResult(2, name, false, "Tool name mismatch");
            }
            if (tool.inputSchema() == null || !tool.inputSchema().containsKey("properties")) {
                return new ScenarioResult(2, name, false, "Input schema properties missing");
            }
            return new ScenarioResult(2, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3(McpServerUnderTest underTest) {
        String name = "ToolDefinition (Title & Metadata Enriched Schema)";
        try {
            Tool tool = underTest.buildAuditValidatedToolDefinition(
                    "auditCluster",
                    "Audits cluster configuration",
                    Map.of("namespace", Map.of("type", "string")),
                    List.of("namespace"),
                    "ClusterAuditSpec"
            );

            if (tool == null) {
                return new ScenarioResult(3, name, false, "buildAuditValidatedToolDefinition returned null");
            }
            if (!"ClusterAuditSpec".equals(tool.inputSchema().get("title"))) {
                return new ScenarioResult(3, name, false, "Expected title 'ClusterAuditSpec' in input schema");
            }
            return new ScenarioResult(3, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 2: SyncToolSpecification Handlers (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4(McpServerUnderTest underTest) {
        String name = "SyncToolSpecification (Standard Execution Handler)";
        try {
            Tool tool = new Tool("echo", null, "Echo tool", Map.of("type", "object"), null, null, null, null);
            SyncToolSpecification spec = underTest.createSyncToolSpecification(tool, (args, ctx) -> "echoed: " + args.get("msg"));

            if (spec == null || spec.callHandler() == null) {
                return new ScenarioResult(4, name, false, "createSyncToolSpecification returned null spec or callHandler");
            }

            CallToolResult res = spec.callHandler().apply(null, new CallToolRequest("echo", Map.of("msg", "hello")));
            if (res == null || res.content() == null || res.content().isEmpty()) {
                return new ScenarioResult(4, name, false, "Execution result was empty");
            }
            TextContent text = (TextContent) res.content().get(0);
            if (!"echoed: hello".equals(text.text())) {
                return new ScenarioResult(4, name, false, "Expected output 'echoed: hello', got: " + text.text());
            }
            return new ScenarioResult(4, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5(McpServerUnderTest underTest) {
        String name = "SyncToolSpecification (Resilient Error Signal Trapping)";
        try {
            Tool tool = new Tool("failing", null, "Failing tool", Map.of("type", "object"), null, null, null, null);
            SyncToolSpecification spec = underTest.createResilientToolSpecification(tool, (args, ctx) -> {
                throw new RuntimeException("Database unreachable");
            });

            if (spec == null || spec.callHandler() == null) {
                return new ScenarioResult(5, name, false, "createResilientToolSpecification returned null");
            }

            CallToolResult res = spec.callHandler().apply(null, new CallToolRequest("failing", Map.of()));
            if (res == null || !Boolean.TRUE.equals(res.isError())) {
                return new ScenarioResult(5, name, false, "Expected res.isError() to be true on exception");
            }
            TextContent text = (TextContent) res.content().get(0);
            if (!text.text().contains("Database unreachable")) {
                return new ScenarioResult(5, name, false, "Expected exception message in result content");
            }
            return new ScenarioResult(5, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6(McpServerUnderTest underTest) {
        String name = "SyncToolSpecification (Spring AI ToolCallback Adaptation)";
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
                return new ScenarioResult(6, name, false, "adaptSpringAiToolCallback returned null");
            }
            if (!"springAiEcho".equals(spec.tool().name())) {
                return new ScenarioResult(6, name, false, "Adapted tool name mismatch");
            }
            return new ScenarioResult(6, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 3: MCP Resource Specifications (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7(McpServerUnderTest underTest) {
        String name = "ResourceSpecification (Static Text Resource)";
        try {
            SyncResourceSpecification spec = underTest.createStaticResourceSpecification(
                    "config://system/env", "SystemEnvironment", "text/plain", "ACTIVE"
            );

            if (spec == null || spec.resource() == null || spec.readHandler() == null) {
                return new ScenarioResult(7, name, false, "createStaticResourceSpecification returned null spec or handler");
            }

            ReadResourceResult res = spec.readHandler().apply(null, new ReadResourceRequest("config://system/env"));
            if (res == null || res.contents().isEmpty()) {
                return new ScenarioResult(7, name, false, "ReadResourceResult contents was empty");
            }
            TextResourceContents content = (TextResourceContents) res.contents().get(0);
            if (!"ACTIVE".equals(content.text())) {
                return new ScenarioResult(7, name, false, "Expected content 'ACTIVE', got: " + content.text());
            }
            return new ScenarioResult(7, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8(McpServerUnderTest underTest) {
        String name = "ResourceSpecification (Dynamic Text Provider)";
        try {
            SyncResourceSpecification spec = underTest.createDynamicResourceSpecification(
                    "metrics://jvm/memory", "JvmMemory", "application/json", uri -> "{\"heapUsedMb\": 512}"
            );

            if (spec == null || spec.readHandler() == null) {
                return new ScenarioResult(8, name, false, "createDynamicResourceSpecification returned null");
            }

            ReadResourceResult res = spec.readHandler().apply(null, new ReadResourceRequest("metrics://jvm/memory"));
            TextResourceContents content = (TextResourceContents) res.contents().get(0);
            if (!content.text().contains("512")) {
                return new ScenarioResult(8, name, false, "Dynamic content missing expected heap value");
            }
            return new ScenarioResult(8, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9(McpServerUnderTest underTest) {
        String name = "ResourceSpecification (Binary Blob Resource)";
        try {
            SyncResourceSpecification spec = underTest.createBinaryResourceSpecification(
                    "files://icons/app.png", "AppIcon", "image/png", uri -> "iVBORw0KGgoAAAANSUhEUg=="
            );

            if (spec == null || spec.readHandler() == null) {
                return new ScenarioResult(9, name, false, "createBinaryResourceSpecification returned null");
            }

            ReadResourceResult res = spec.readHandler().apply(null, new ReadResourceRequest("files://icons/app.png"));
            BlobResourceContents blob = (BlobResourceContents) res.contents().get(0);
            if (!"iVBORw0KGgoAAAANSUhEUg==".equals(blob.blob())) {
                return new ScenarioResult(9, name, false, "Expected base64 blob content mismatch");
            }
            return new ScenarioResult(9, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 4: MCP Prompt Template Specifications (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10(McpServerUnderTest underTest) {
        String name = "PromptSpecification (Simple Zero-Argument Prompt)";
        try {
            SyncPromptSpecification spec = underTest.createSimplePromptSpecification(
                    "systemDiagnosis", "System diagnostic prompt", args -> "Perform full health check."
            );

            if (spec == null || spec.promptHandler() == null) {
                return new ScenarioResult(10, name, false, "createSimplePromptSpecification returned null");
            }

            GetPromptResult res = spec.promptHandler().apply(null, new GetPromptRequest("systemDiagnosis", Map.of()));
            PromptMessage msg = res.messages().get(0);
            TextContent content = (TextContent) msg.content();
            if (!"Perform full health check.".equals(content.text())) {
                return new ScenarioResult(10, name, false, "Prompt message text mismatch");
            }
            return new ScenarioResult(10, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario11(McpServerUnderTest underTest) {
        String name = "PromptSpecification (Parameterized Multi-Argument Prompt)";
        try {
            List<PromptArgument> args = List.of(new PromptArgument("service", "target service", true));
            SyncPromptSpecification spec = underTest.createParameterizedPromptSpecification(
                    "serviceTriage", "Triage prompt", args, (name1, map) -> "Triage for service: " + map.get("service")
            );

            if (spec == null || spec.promptHandler() == null) {
                return new ScenarioResult(11, name, false, "createParameterizedPromptSpecification returned null");
            }

            GetPromptResult res = spec.promptHandler().apply(null, new GetPromptRequest("serviceTriage", Map.of("service", "billing")));
            TextContent content = (TextContent) res.messages().get(0).content();
            if (!"Triage for service: billing".equals(content.text())) {
                return new ScenarioResult(11, name, false, "Parameterized prompt text mismatch: " + content.text());
            }
            return new ScenarioResult(11, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(11, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario12(McpServerUnderTest underTest) {
        String name = "PromptSpecification (Role-Enforced System/User Prompt)";
        try {
            SyncPromptSpecification spec = underTest.createRoleEnforcedPromptSpecification(
                    "systemInit", "Initialization prompt", List.of(), Role.ASSISTANT, (n, m) -> "You are a cloud architect."
            );

            if (spec == null || spec.promptHandler() == null) {
                return new ScenarioResult(12, name, false, "createRoleEnforcedPromptSpecification returned null");
            }

            GetPromptResult res = spec.promptHandler().apply(null, new GetPromptRequest("systemInit", Map.of()));
            PromptMessage msg = res.messages().get(0);
            if (msg.role() != Role.ASSISTANT) {
                return new ScenarioResult(12, name, false, "Expected Role.ASSISTANT, got: " + msg.role());
            }
            return new ScenarioResult(12, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(12, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 5: Server Capabilities & Protocol Filtering (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13(McpServerUnderTest underTest) {
        String name = "ServerCapabilities (Feature Negotiation Flags)";
        try {
            ServerCapabilities caps = underTest.createServerCapabilities(true, true, false, true);
            if (caps == null) {
                return new ScenarioResult(13, name, false, "createServerCapabilities returned null");
            }
            if (caps.tools() == null || caps.resources() == null || caps.logging() == null) {
                return new ScenarioResult(13, name, false, "Configured capabilities missing");
            }
            if (caps.prompts() != null) {
                return new ScenarioResult(13, name, false, "Prompts should be null when disabled");
            }
            return new ScenarioResult(13, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(13, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario14(McpServerUnderTest underTest) {
        String name = "ProtocolFilter (Allowlist Tool Filtering)";
        try {
            List<Tool> allTools = List.of(
                    new Tool("readLog", null, "Read", Map.of("type", "object"), null, null, null, null),
                    new Tool("dropDatabase", null, "Drop", Map.of("type", "object"), null, null, null, null),
                    new Tool("restartPod", null, "Restart", Map.of("type", "object"), null, null, null, null)
            );

            List<Tool> filtered = underTest.filterToolsByAllowlist(allTools, Set.of("readLog", "restartPod"));
            if (filtered == null || filtered.size() != 2) {
                return new ScenarioResult(14, name, false, "Expected 2 tools in allowlist, got: " + (filtered == null ? "null" : filtered.size()));
            }
            if (filtered.stream().anyMatch(t -> "dropDatabase".equals(t.name()))) {
                return new ScenarioResult(14, name, false, "Forbidden tool 'dropDatabase' passed through filter");
            }
            return new ScenarioResult(14, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(14, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario15(McpServerUnderTest underTest) {
        String name = "ProtocolFilter (Prefix-Based Tool Routing)";
        try {
            List<Tool> allTools = List.of(
                    new Tool("k8s_restartPod", null, "Restart", Map.of("type", "object"), null, null, null, null),
                    new Tool("k8s_getLogs", null, "Logs", Map.of("type", "object"), null, null, null, null),
                    new Tool("db_queryMetrics", null, "Metrics", Map.of("type", "object"), null, null, null, null)
            );

            List<Tool> k8sTools = underTest.filterToolsByPrefix(allTools, "k8s_");
            if (k8sTools == null || k8sTools.size() != 2) {
                return new ScenarioResult(15, name, false, "Expected 2 k8s tools, got: " + (k8sTools == null ? "null" : k8sTools.size()));
            }
            if (k8sTools.stream().anyMatch(t -> "db_queryMetrics".equals(t.name()))) {
                return new ScenarioResult(15, name, false, "db tool passed through k8s prefix filter");
            }
            return new ScenarioResult(15, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(15, name, false, "Exception: " + e.getMessage());
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
        for (ScenarioResult r : results) {
            String tag = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", tag, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        System.out.println("===============================================================================");
    }
}
