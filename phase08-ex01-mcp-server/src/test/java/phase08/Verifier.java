package phase08;

import io.modelcontextprotocol.server.McpServerFeatures.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import phase08.McpServerContracts.*;
import phase08.McpServerUnderTest.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verification harness for Phase 08 Exercise 01 (MCP Server).
 * Executes 10 deterministic offline tests against {@link McpServerUnderTest}.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class Verifier {

    private static final AtomicInteger passed = new AtomicInteger(0);
    private static final AtomicInteger failed = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  PHASE 08 EXERCISE 01: MCP SERVER ARCHITECTURE & SPEC VERIFIER  ");
        System.out.println("=================================================================\n");

        McpServerUnderTest underTest = new McpServerUnderTest();

        testScenario01(underTest);
        testScenario02(underTest);
        testScenario03(underTest);
        testScenario04(underTest);
        testScenario05(underTest);
        testScenario06(underTest);
        testScenario07(underTest);
        testScenario08(underTest);
        testScenario09(underTest);
        testScenario10(underTest);
        testScenario11(underTest);
        testScenario12(underTest);

        System.out.println("\n-----------------------------------------------------------------");
        System.out.printf("VERIFICATION SUMMARY: %d / 12 PASSED, %d FAILED%n", passed.get(), failed.get());
        System.out.println("-----------------------------------------------------------------");

        if (failed.get() > 0) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        McpServerUnderTest underTest = new McpServerUnderTest();
        passed.set(0);
        failed.set(0);

        testScenario01(underTest);
        testScenario02(underTest);
        testScenario03(underTest);
        testScenario04(underTest);
        testScenario05(underTest);
        testScenario06(underTest);
        testScenario07(underTest);
        testScenario08(underTest);
        testScenario09(underTest);
        testScenario10(underTest);
        testScenario11(underTest);
        testScenario12(underTest);

        Assertions.assertEquals(0, failed.get(), "Verifier detected scenario failures.");
    }

    private static void pass(String name) {
        passed.incrementAndGet();
        System.out.println("  [PASS] " + name);
    }

    private static void fail(String name, String detail) {
        failed.incrementAndGet();
        System.out.println("  [FAIL] " + name);
        if (detail != null && !detail.isBlank()) {
            System.out.println("         --> DETAIL: " + detail);
        }
    }

    private static void testScenario01(McpServerUnderTest underTest) {
        String name = "Scenario 01: Server Capabilities Declaration";
        try {
            ServerCapabilities caps = underTest.createServerCapabilities(true, true, false, true);
            if (caps == null) {
                fail(name, "createServerCapabilities returned null");
                return;
            }

            if (caps.tools() == null || !Boolean.TRUE.equals(caps.tools().listChanged())) {
                fail(name, "Expected tools capability with listChanged=true");
                return;
            }

            if (caps.resources() == null || !Boolean.TRUE.equals(caps.resources().subscribe())) {
                fail(name, "Expected resources capability with subscribe=true");
                return;
            }

            if (caps.prompts() != null) {
                fail(name, "Expected prompts capability to be null when disabled");
                return;
            }

            if (caps.logging() == null) {
                fail(name, "Expected logging capability to be non-null when enabled");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario02(McpServerUnderTest underTest) {
        String name = "Scenario 02: Tool Definition with JSON Input Schema";
        try {
            Map<String, Object> props = Map.of("query", Map.of("type", "string", "description", "SQL query"));
            List<String> required = List.of("query");

            Tool tool = underTest.createToolDefinition("sql_runner", "Executes SQL query", props, required);
            if (tool == null) {
                fail(name, "createToolDefinition returned null");
                return;
            }

            if (!"sql_runner".equals(tool.name())) {
                fail(name, "Expected name 'sql_runner', got: " + tool.name());
                return;
            }

            if (!"Executes SQL query".equals(tool.description())) {
                fail(name, "Expected description 'Executes SQL query', got: " + tool.description());
                return;
            }

            Map<String, Object> schema = tool.inputSchema();
            if (schema == null || !"object".equals(schema.get("type"))) {
                fail(name, "Expected inputSchema type 'object'");
                return;
            }

            if (!schema.containsKey("properties") || !schema.containsKey("required")) {
                fail(name, "Expected inputSchema to contain 'properties' and 'required'");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario03(McpServerUnderTest underTest) {
        String name = "Scenario 03: SyncToolSpecification Execution Handler";
        try {
            Tool tool = Tool.builder("echo_tool").description("echo").build();
            SyncToolSpecification spec = underTest.createToolSpecification(tool, (args, ctx) -> "ECHO: " + args.get("msg") + " from " + ctx.tenantId());

            if (spec == null || spec.callHandler() == null) {
                fail(name, "createToolSpecification returned null or null callHandler");
                return;
            }

            CallToolResult result = spec.callHandler().apply(null, new CallToolRequest("echo_tool", Map.of("msg", "hello")));
            if (result == null || Boolean.TRUE.equals(result.isError())) {
                fail(name, "Expected successful CallToolResult, got: " + result);
                return;
            }

            if (result.content().isEmpty() || !(result.content().get(0) instanceof TextContent tc) || !tc.text().contains("ECHO: hello")) {
                fail(name, "Expected TextContent containing 'ECHO: hello', got: " + result.content());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario04(McpServerUnderTest underTest) {
        String name = "Scenario 04: Resilient Tool Execution & Error Signal Wrapping";
        try {
            Tool tool = Tool.builder("fail_tool").description("fails").build();
            SyncToolSpecification spec = underTest.createResilientToolSpecification(tool, (args, ctx) -> {
                throw new IllegalArgumentException("Invalid partition ID: -1");
            });

            if (spec == null || spec.callHandler() == null) {
                fail(name, "createResilientToolSpecification returned null");
                return;
            }

            CallToolResult result = spec.callHandler().apply(null, new CallToolRequest("fail_tool", Map.of()));
            if (result == null || !Boolean.TRUE.equals(result.isError())) {
                fail(name, "Expected isError=true on exception, got: " + result);
                return;
            }

            if (result.content().isEmpty() || !(result.content().get(0) instanceof TextContent tc) || !tc.text().contains("Invalid partition ID: -1")) {
                fail(name, "Expected error message in TextContent, got: " + result.content());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario05(McpServerUnderTest underTest) {
        String name = "Scenario 05: Spring AI ToolCallback to MCP Tool Adapter";
        try {
            ToolCallback springTool = new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name("spring_calc")
                            .description("Spring calculation tool")
                            .inputSchema("{\"type\":\"object\"}")
                            .build();
                }

                @Override
                public String call(String toolInput) {
                    return "Calculated: 42";
                }
            };

            SyncToolSpecification spec = underTest.adaptSpringAiTool(springTool);
            if (spec == null || spec.tool() == null) {
                fail(name, "adaptSpringAiTool returned null spec or tool");
                return;
            }

            if (!"spring_calc".equals(spec.tool().name())) {
                fail(name, "Expected adapted tool name 'spring_calc', got: " + spec.tool().name());
                return;
            }

            CallToolResult result = spec.callHandler().apply(null, new CallToolRequest("spring_calc", Map.of()));
            if (result == null || result.content().isEmpty() || !(result.content().get(0) instanceof TextContent tc) || !tc.text().contains("Calculated: 42")) {
                fail(name, "Expected execution output 'Calculated: 42', got: " + result);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario06(McpServerUnderTest underTest) {
        String name = "Scenario 06: MCP Static & Dynamic Resource Specification";
        try {
            SyncResourceSpecification spec = underTest.createResourceSpecification(
                    "config://cluster/kafka.json",
                    "kafka-config",
                    "application/json",
                    uri -> "{\"brokerId\": 101, \"rack\": \"us-east-1a\"}"
            );

            if (spec == null || spec.resource() == null || spec.readHandler() == null) {
                fail(name, "createResourceSpecification returned null");
                return;
            }

            if (!"config://cluster/kafka.json".equals(spec.resource().uri())) {
                fail(name, "Expected URI 'config://cluster/kafka.json', got: " + spec.resource().uri());
                return;
            }

            ReadResourceResult result = spec.readHandler().apply(null, new ReadResourceRequest("config://cluster/kafka.json"));
            if (result == null || result.contents().isEmpty()) {
                fail(name, "Expected non-empty ReadResourceResult contents");
                return;
            }

            if (!(result.contents().get(0) instanceof TextResourceContents trc) || !trc.text().contains("brokerId\": 101")) {
                fail(name, "Expected TextResourceContents with JSON content, got: " + result.contents());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario07(McpServerUnderTest underTest) {
        String name = "Scenario 07: MCP Parameterized Prompt Template Specification";
        try {
            List<PromptArgument> args = List.of(new PromptArgument("system_name", "target system", true));
            SyncPromptSpecification spec = underTest.createPromptSpecification(
                    "diagnose_service",
                    "Root cause analysis prompt",
                    args,
                    (pName, argMap) -> "Diagnose the incident on system: " + argMap.get("system_name")
            );

            if (spec == null || spec.prompt() == null || spec.promptHandler() == null) {
                fail(name, "createPromptSpecification returned null");
                return;
            }

            GetPromptResult result = spec.promptHandler().apply(null, new GetPromptRequest("diagnose_service", Map.of("system_name", "PaymentGateway")));
            if (result == null || result.messages().isEmpty()) {
                fail(name, "Expected non-empty GetPromptResult messages");
                return;
            }

            PromptMessage msg = result.messages().get(0);
            if (msg.role() != Role.USER || !(msg.content() instanceof TextContent tc) || !tc.text().contains("PaymentGateway")) {
                fail(name, "Expected USER message containing 'PaymentGateway', got: " + msg);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario08(McpServerUnderTest underTest) {
        String name = "Scenario 08: Security Allowlist & Tool Filter";
        try {
            SyncToolSpecification t1 = underTest.createToolSpecification(Tool.builder("read_users").description("read").build(), (a, c) -> "users");
            SyncToolSpecification t2 = underTest.createToolSpecification(Tool.builder("write_users").description("write").build(), (a, c) -> "ok");
            SyncToolSpecification t3 = underTest.createToolSpecification(Tool.builder("delete_database").description("delete").build(), (a, c) -> "dropped");

            List<SyncToolSpecification> filtered = underTest.filterToolsByAllowlist(List.of(t1, t2, t3), Set.of("read_users", "write_users"));
            if (filtered == null || filtered.size() != 2) {
                fail(name, "Expected 2 allowlisted tools, got: " + (filtered != null ? filtered.size() : "null"));
                return;
            }

            Set<String> names = Set.of(filtered.get(0).tool().name(), filtered.get(1).tool().name());
            if (!names.contains("read_users") || !names.contains("write_users") || names.contains("delete_database")) {
                fail(name, "Unexpected tools in allowlist result: " + names);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario09(McpServerUnderTest underTest) {
        String name = "Scenario 09: MCP Server Telemetry & Audit Recorder";
        try {
            McpServerAuditRecorder recorder = new McpServerAuditRecorder();
            recorder.recordSuccess("tool_ping");
            recorder.recordSuccess("tool_ping");
            recorder.recordError("tool_ping");
            recorder.recordSuccess("tool_query");

            McpAuditReport report = recorder.getReport();
            if (report == null) {
                fail(name, "getReport returned null");
                return;
            }

            if (report.totalCalls() != 4 || report.successCount() != 3 || report.errorCount() != 1) {
                fail(name, "Expected (total=4, success=3, error=1), got: " + report);
                return;
            }

            if (report.toolCallCounts().getOrDefault("tool_ping", 0) != 3) {
                fail(name, "Expected 3 calls for tool_ping, got: " + report.toolCallCounts().get("tool_ping"));
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario10(McpServerUnderTest underTest) {
        String name = "Scenario 10: Composite Enterprise MCP Server Registry & Router";
        try {
            EnterpriseMcpRegistry registry = new EnterpriseMcpRegistry();

            SyncToolSpecification t1 = underTest.createToolSpecification(
                    Tool.builder("query_orders").description("query").build(),
                    (a, c) -> "orders-found"
            );
            SyncToolSpecification t2 = underTest.createToolSpecification(
                    Tool.builder("drop_tables").description("drop").build(),
                    (a, c) -> "dropped"
            );

            registry.registerTool(t1);
            registry.registerTool(t2);
            registry.setAllowlist(Set.of("query_orders"));

            SyncResourceSpecification r1 = underTest.createResourceSpecification(
                    "metrics://latency",
                    "latency-metrics",
                    "text/plain",
                    u -> "p99=45ms"
            );
            registry.registerResource(r1);

            SyncPromptSpecification p1 = underTest.createPromptSpecification(
                    "review_code",
                    "Code review prompt",
                    List.of(),
                    (n, a) -> "Review code template"
            );
            registry.registerPrompt(p1);

            // Subtest A: Unregistered tool
            try {
                registry.handleCallTool("unknown_tool", Map.of(), new ServerContext("t1", "user", "tr1"));
                fail(name, "Expected McpRegistryBreachException for unregistered tool");
                return;
            } catch (McpRegistryBreachException expected) {
                // pass
            }

            // Subtest B: Disallowed tool
            try {
                registry.handleCallTool("drop_tables", Map.of(), new ServerContext("t1", "user", "tr1"));
                fail(name, "Expected McpSecurityBreachException for tool not in allowlist");
                return;
            } catch (McpSecurityBreachException expected) {
                // pass
            }

            // Subtest C: Allowed tool execution
            CallToolResult toolResult = registry.handleCallTool("query_orders", Map.of(), new ServerContext("fintech", "admin", "tr2"));
            if (toolResult == null || Boolean.TRUE.equals(toolResult.isError())) {
                fail(name, "Expected successful execution of query_orders, got: " + toolResult);
                return;
            }

            // Subtest D: Read resource
            ReadResourceResult resResult = registry.handleReadResource("metrics://latency");
            if (resResult == null || resResult.contents().isEmpty()) {
                fail(name, "Expected successful read of metrics://latency");
                return;
            }

            // Subtest E: Get prompt
            GetPromptResult promptResult = registry.handleGetPrompt("review_code", Map.of());
            if (promptResult == null || promptResult.messages().isEmpty()) {
                fail(name, "Expected successful prompt retrieval for review_code");
                return;
            }

            // Subtest F: Audit report check
            McpAuditReport report = registry.getAuditReport();
            if (report.totalCalls() != 1 || report.successCount() != 1) {
                fail(name, "Expected exactly 1 successful tool call in audit report, got: " + report);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario11(McpServerUnderTest underTest) {
        String name = "Scenario 11: Parameterized Resource Template URI Matcher";
        try {
            List<String> templates = List.of(
                    "metrics://{cluster}/{service}/{metric}",
                    "logs://{cluster}/{service}"
            );

            // Subtest A: Exact match with 3 path variables
            MatchedResourceRoute match1 = ParameterizedResourceTemplateMatcher.matchTemplate(
                    templates,
                    "metrics://prod-east/payment-svc/p99"
            );

            if (match1 == null) {
                fail(name, "matchTemplate returned null for valid URI");
                return;
            }
            if (!"metrics://{cluster}/{service}/{metric}".equals(match1.uriTemplate())) {
                fail(name, "Expected template 'metrics://{cluster}/{service}/{metric}', got: " + match1.uriTemplate());
                return;
            }
            if (!"prod-east".equals(match1.pathVariables().get("cluster")) ||
                    !"payment-svc".equals(match1.pathVariables().get("service")) ||
                    !"p99".equals(match1.pathVariables().get("metric"))) {
                fail(name, "Unexpected pathVariables: " + match1.pathVariables());
                return;
            }

            // Subtest B: Match second template with 2 path variables
            MatchedResourceRoute match2 = ParameterizedResourceTemplateMatcher.matchTemplate(
                    templates,
                    "logs://staging-west/auth-svc"
            );
            if (match2 == null || !"logs://{cluster}/{service}".equals(match2.uriTemplate()) ||
                    !"staging-west".equals(match2.pathVariables().get("cluster")) ||
                    !"auth-svc".equals(match2.pathVariables().get("service"))) {
                fail(name, "Failed matching second template: " + match2);
                return;
            }

            // Subtest C: Unmatched URI throws McpRegistryBreachException
            try {
                ParameterizedResourceTemplateMatcher.matchTemplate(templates, "config://prod/database");
                fail(name, "Expected McpRegistryBreachException for unmatched URI");
                return;
            } catch (McpRegistryBreachException expected) {
                // Expected
            }

            // Subtest D: Argument validation throws IllegalArgumentException
            try {
                ParameterizedResourceTemplateMatcher.matchTemplate(null, "metrics://a/b/c");
                fail(name, "Expected IllegalArgumentException for null template list");
                return;
            } catch (IllegalArgumentException expected) {
                // Expected
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario12(McpServerUnderTest underTest) {
        String name = "Scenario 12: MCP Protocol Log Notification Dispatcher & Level Filter";
        try {
            McpProtocolLogNotificationDispatcher dispatcher = new McpProtocolLogNotificationDispatcher();

            // Default minLevel is INFO
            if (dispatcher.getMinLevel() != McpLogLevel.INFO) {
                fail(name, "Expected default minLevel to be INFO, got: " + dispatcher.getMinLevel());
                return;
            }

            // Subtest A: DEBUG filtered out under INFO
            boolean d1 = dispatcher.dispatchLog(new McpLogMessage(
                    McpLogLevel.DEBUG, "engine.core", "Detailed tick trace", Map.of("tick", 100)
            ));
            if (d1 || !dispatcher.getEmittedLogs().isEmpty()) {
                fail(name, "Expected DEBUG message to be filtered out under default INFO level");
                return;
            }

            // Subtest B: INFO and ERROR accepted under INFO
            boolean d2 = dispatcher.dispatchLog(new McpLogMessage(
                    McpLogLevel.INFO, "engine.core", "Service started", Map.of("port", 8080)
            ));
            boolean d3 = dispatcher.dispatchLog(new McpLogMessage(
                    McpLogLevel.ERROR, "engine.db", "Connection timeout", Map.of("db", "primary")
            ));
            if (!d2 || !d3 || dispatcher.getEmittedLogs().size() != 2) {
                fail(name, "Expected INFO and ERROR logs to be dispatched, count: " + dispatcher.getEmittedLogs().size());
                return;
            }

            // Subtest C: Lower threshold to DEBUG allows DEBUG
            dispatcher.setMinimumLevel(McpLogLevel.DEBUG);
            boolean d4 = dispatcher.dispatchLog(new McpLogMessage(
                    McpLogLevel.DEBUG, "engine.core", "Cache hit: key_123", Map.of()
            ));
            if (!d4 || dispatcher.getEmittedLogs().size() != 3) {
                fail(name, "Expected DEBUG log to be dispatched when minLevel=DEBUG");
                return;
            }

            // Subtest D: Raise threshold to ERROR filters WARNING
            dispatcher.setMinimumLevel(McpLogLevel.ERROR);
            boolean d5 = dispatcher.dispatchLog(new McpLogMessage(
                    McpLogLevel.WARNING, "engine.disk", "Disk usage at 85%", Map.of()
            ));
            if (d5 || dispatcher.getEmittedLogs().size() != 3) {
                fail(name, "Expected WARNING log to be filtered out when minLevel=ERROR");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }
}
