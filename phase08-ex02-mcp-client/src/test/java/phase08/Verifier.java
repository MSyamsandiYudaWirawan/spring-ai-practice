package phase08;

import io.modelcontextprotocol.spec.McpSchema.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import phase08.McpClientContracts.*;
import phase08.McpClientUnderTest.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verification harness for Phase 08 Exercise 02 (MCP Client).
 * Executes 10 deterministic offline tests against {@link McpClientUnderTest}.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class Verifier {

    private static final AtomicInteger passed = new AtomicInteger(0);
    private static final AtomicInteger failed = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  PHASE 08 EXERCISE 02: MCP CLIENT INTEGRATION & AGENT VERIFIER  ");
        System.out.println("=================================================================\n");

        McpClientUnderTest underTest = new McpClientUnderTest();

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

        System.out.println("\n-----------------------------------------------------------------");
        System.out.printf("VERIFICATION SUMMARY: %d / 10 PASSED, %d FAILED%n", passed.get(), failed.get());
        System.out.println("-----------------------------------------------------------------");

        if (failed.get() > 0) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        McpClientUnderTest underTest = new McpClientUnderTest();
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

    private static McpClientAdapter createMockClient(String name) {
        return new McpClientAdapter() {
            @Override
            public String serverName() {
                return name;
            }

            @Override
            public List<Tool> listTools() {
                Tool t1 = Tool.builder("fetch_status")
                        .description("fetches system status")
                        .inputSchema(Map.of("type", "object"))
                        .build();
                return List.of(t1);
            }

            @Override
            public CallToolResult callTool(String toolName, Map<String, Object> arguments) {
                if ("failing_tool".equals(toolName)) {
                    return new CallToolResult(List.of(new TextContent("Internal DB error")), true, null, null);
                }
                return new CallToolResult(List.of(new TextContent("status-ok from " + name)), false, null, null);
            }

            @Override
            public ReadResourceResult readResource(String uri) {
                return new ReadResourceResult(List.of(new TextResourceContents(uri, "application/json", "{\"status\": \"healthy\"}")));
            }

            @Override
            public GetPromptResult getPrompt(String promptName, Map<String, Object> arguments) {
                String text = "Execute prompt " + promptName + " with arg=" + (arguments != null ? arguments.get("key") : "none");
                return new GetPromptResult("desc", List.of(new PromptMessage(Role.USER, new TextContent(text))));
            }
        };
    }

    private static void testScenario01(McpClientUnderTest underTest) {
        String name = "Scenario 01: Client Capability Negotiation & Specs";
        try {
            ClientCapabilities caps = underTest.createClientCapabilities(true, false);
            if (caps == null) {
                fail(name, "createClientCapabilities returned null");
                return;
            }

            if (caps.roots() == null || !Boolean.TRUE.equals(caps.roots().listChanged())) {
                fail(name, "Expected roots capability with listChanged=true");
                return;
            }

            if (caps.sampling() != null) {
                fail(name, "Expected sampling capability to be null when disabled");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario02(McpClientUnderTest underTest) {
        String name = "Scenario 02: Tool Definition Adaptation from MCP Schemas";
        try {
            Tool tool = Tool.builder("query_metrics")
                    .description("queries cluster metrics")
                    .inputSchema(Map.of("type", "object", "properties", Map.of("timeRange", Map.of("type", "string"))))
                    .build();

            ToolDefinition def = underTest.adaptMcpToolToDefinition(tool);
            if (def == null) {
                fail(name, "adaptMcpToolToDefinition returned null");
                return;
            }

            if (!"query_metrics".equals(def.name()) || !"queries cluster metrics".equals(def.description())) {
                fail(name, "ToolDefinition name or description mismatched: " + def.name() + " / " + def.description());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario03(McpClientUnderTest underTest) {
        String name = "Scenario 03: Adapting MCP Sync Tools to ToolCallbacks";
        try {
            McpClientAdapter client = createMockClient("cluster-mgr");
            Tool tool = client.listTools().get(0);

            ToolCallback callback = underTest.adaptMcpToolToCallback(client, tool);
            if (callback == null || callback.getToolDefinition() == null) {
                fail(name, "adaptMcpToolToCallback returned null callback or definition");
                return;
            }

            String output = callback.call("{\"param\": 1}");
            if (output == null || !output.contains("status-ok from cluster-mgr")) {
                fail(name, "Expected callback output containing 'status-ok', got: " + output);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario04(McpClientUnderTest underTest) {
        String name = "Scenario 04: Multi-Tool Discovery & Callback Provider Generation";
        try {
            McpClientAdapter client = createMockClient("prod-infra");
            List<ToolCallback> callbacks = underTest.createToolCallbacksForClient(client);

            if (callbacks == null || callbacks.isEmpty()) {
                fail(name, "createToolCallbacksForClient returned null or empty list");
                return;
            }

            if (!"fetch_status".equals(callbacks.get(0).getToolDefinition().name())) {
                fail(name, "Expected first tool name 'fetch_status', got: " + callbacks.get(0).getToolDefinition().name());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario05(McpClientUnderTest underTest) {
        String name = "Scenario 05: Multi-Server Tool Name Prefixing (Collision Prevention)";
        try {
            String prefixed = underTest.buildPrefixedToolName("github", "create_issue");
            if (prefixed == null || !prefixed.contains("create_issue")) {
                fail(name, "Expected prefixed tool name containing 'create_issue', got: '" + prefixed + "'");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario06(McpClientUnderTest underTest) {
        String name = "Scenario 06: MCP Resource Reading into Prompt Context";
        try {
            McpClientAdapter client = createMockClient("telemetry-svc");
            String context = underTest.formatResourceAsContext(client, "telemetry://nodes");

            if (context == null || !context.contains("[Resource: telemetry://nodes]") || !context.contains("status\": \"healthy")) {
                fail(name, "Expected resource context header and text, got: " + context);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario07(McpClientUnderTest underTest) {
        String name = "Scenario 07: MCP Parameterized Prompt Retrieval";
        try {
            McpClientAdapter client = createMockClient("prompt-svc");
            String promptText = underTest.fetchPromptMessageText(client, "remediate", Map.of("key", "val-99"));

            if (promptText == null || !promptText.contains("arg=val-99")) {
                fail(name, "Expected prompt message text containing 'arg=val-99', got: " + promptText);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario08(McpClientUnderTest underTest) {
        String name = "Scenario 08: Tool Execution Resilience & Error Boundary";
        try {
            McpClientAdapter client = createMockClient("resilience-svc");

            // Subtest A: Tool returns error signal
            String errResult = underTest.executeToolSafely(client, "failing_tool", Map.of());
            if (errResult == null || !errResult.startsWith("TOOL_ERROR") || !errResult.contains("Internal DB error")) {
                fail(name, "Expected TOOL_ERROR prefix for error result, got: " + errResult);
                return;
            }

            // Subtest B: Normal execution
            String okResult = underTest.executeToolSafely(client, "fetch_status", Map.of());
            if (okResult == null || !okResult.contains("status-ok")) {
                fail(name, "Expected success output, got: " + okResult);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario09(McpClientUnderTest underTest) {
        String name = "Scenario 09: Multi-Server Client Registry & Unified Tool Dispatcher";
        try {
            MultiServerClientRegistry registry = new MultiServerClientRegistry();
            registry.registerClient(createMockClient("k8s"));
            registry.registerClient(createMockClient("vault"));

            List<String> prefixedTools = registry.listAllPrefixedTools();
            if (prefixedTools == null || prefixedTools.size() != 2) {
                fail(name, "Expected 2 prefixed tools, got: " + (prefixedTools != null ? prefixedTools.size() : "null"));
                return;
            }

            String k8sPrefixed = underTest.buildPrefixedToolName("k8s", "fetch_status");
            String vaultPrefixed = underTest.buildPrefixedToolName("vault", "fetch_status");

            Set<String> set = Set.copyOf(prefixedTools);
            if (!set.contains(k8sPrefixed) || !set.contains(vaultPrefixed)) {
                fail(name, "Expected [" + k8sPrefixed + ", " + vaultPrefixed + "], got: " + set);
                return;
            }

            String executed = registry.executePrefixedTool(k8sPrefixed, Map.of());
            if (executed == null || !executed.contains("status-ok from k8s")) {
                fail(name, "Expected routing to k8s client, got: " + executed);
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario10(McpClientUnderTest underTest) {
        String name = "Scenario 10: End-to-End Enterprise Agentic Gateway with MCP Tool Calling";
        try {
            MultiServerClientRegistry registry = new MultiServerClientRegistry();
            registry.registerClient(createMockClient("aws"));
            registry.registerClient(createMockClient("pagerduty"));

            FakeMcpChatModel chatModel = new FakeMcpChatModel();
            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

            // Subtest A: Blank query rejection
            try {
                underTest.executeAgentWorkflow(registry, chatClientBuilder, new AgentExecutionRequest("   ", 5));
                fail(name, "Expected IllegalArgumentException or McpClientBreachException on blank query");
                return;
            } catch (IllegalArgumentException | McpClientBreachException expected) {
                // pass
            }

            // Subtest B: Normal execution
            chatModel.enqueue("The issue on aws was successfully mitigated and pagerduty incident acknowledged.");
            chatModel.setTokenUsage(180, 45);

            AgentExecutionSummary summary = underTest.executeAgentWorkflow(
                    registry,
                    chatClientBuilder,
                    new AgentExecutionRequest("Mitigate outage on aws and resolve pagerduty alert", 3)
            );

            if (summary == null || !summary.success()) {
                fail(name, "Expected successful agent summary, got: " + summary);
                return;
            }

            if (!summary.finalAnswer().contains("The issue on aws was successfully mitigated")) {
                fail(name, "Expected answer text from ChatModel, got: " + summary.finalAnswer());
                return;
            }

            if (summary.totalTokens() != 225) {
                fail(name, "Expected 225 total tokens, got: " + summary.totalTokens());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }
}
