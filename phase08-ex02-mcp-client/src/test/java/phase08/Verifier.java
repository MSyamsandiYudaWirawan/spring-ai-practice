package phase08;

import io.modelcontextprotocol.spec.McpSchema.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import phase08.McpClientContracts.*;
import phase08.McpClientUnderTest.MultiServerClientRegistry;

import java.util.*;

/**
 * Verification harness for Phase 08 Exercise 02 (MCP Client).
 * Executes 15 deterministic offline tests across 5 core topics (3 repetitions each).
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

    public static void main(String[] args) {
        Verifier verifier = new Verifier();
        List<ScenarioResult> results = verifier.runAll();
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        System.exit(anyFailed ? 1 : 0);
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runAll();
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        Assertions.assertFalse(anyFailed, "Verifier detected scenario failures. See printed report above.");
    }

    public List<ScenarioResult> runAll() {
        McpClientUnderTest underTest = new McpClientUnderTest();
        List<ScenarioResult> results = new ArrayList<>();

        // Topic 1: Client Capabilities & Specs (Scenarios 1 - 3)
        results.add(verifyScenario01(underTest));
        results.add(verifyScenario02(underTest));
        results.add(verifyScenario03(underTest));

        // Topic 2: Adapting MCP Tools to Spring AI ToolCallbacks (Scenarios 4 - 6)
        results.add(verifyScenario04(underTest));
        results.add(verifyScenario05(underTest));
        results.add(verifyScenario06(underTest));

        // Topic 3: Tool Discovery, Batch Adaptation & Namespacing (Scenarios 7 - 9)
        results.add(verifyScenario07(underTest));
        results.add(verifyScenario08(underTest));
        results.add(verifyScenario09(underTest));

        // Topic 4: MCP Resources & Prompts Retrieval (Scenarios 10 - 12)
        results.add(verifyScenario10(underTest));
        results.add(verifyScenario11(underTest));
        results.add(verifyScenario12(underTest));

        // Topic 5: Multi-Server Routing & Fault Resilience (Scenarios 13 - 15)
        results.add(verifyScenario13(underTest));
        results.add(verifyScenario14(underTest));
        results.add(verifyScenario15(underTest));

        return results;
    }

    // =========================================================================
    // MOCK CLIENT FACTORY
    // =========================================================================

    private static McpClientAdapter createMockClient(String name, List<Tool> tools) {
        return new McpClientAdapter() {
            @Override
            public String serverName() {
                return name;
            }

            @Override
            public List<Tool> listTools() {
                return tools != null ? tools : List.of(
                        Tool.builder("fetch_status")
                                .description("fetches system status")
                                .inputSchema(Map.of("type", "object"))
                                .build()
                );
            }

            @Override
            public CallToolResult callTool(String toolName, Map<String, Object> arguments) {
                if ("failing_tool".equals(toolName)) {
                    return new CallToolResult(List.of(new TextContent("Internal DB error")), true, null, null);
                }
                if ("exploding_tool".equals(toolName)) {
                    throw new RuntimeException("Network connection reset");
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

    // =========================================================================
    // TOPIC 1: Client Capabilities & Specs (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario01(McpClientUnderTest underTest) {
        String name = "ClientCapabilities (Feature Flags Negotiation)";
        try {
            ClientCapabilities caps1 = underTest.createClientCapabilities(true, false);
            if (caps1 == null || caps1.roots() == null || !Boolean.TRUE.equals(caps1.roots().listChanged())) {
                return new ScenarioResult(1, name, false, "Expected roots capability with listChanged=true");
            }
            if (caps1.sampling() != null) {
                return new ScenarioResult(1, name, false, "Expected sampling to be null when disabled");
            }

            ClientCapabilities caps2 = underTest.createClientCapabilities(false, true);
            if (caps2 == null || caps2.sampling() == null || caps2.roots() != null) {
                return new ScenarioResult(1, name, false, "Expected sampling enabled and roots null");
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario02(McpClientUnderTest underTest) {
        String name = "ClientRoots (URI Validation & Construction)";
        try {
            List<Root> roots = underTest.buildClientRoots(Map.of(
                    "file:///workspace/project", "workspace",
                    "file:///logs/agent", "logs"
            ));

            if (roots == null || roots.size() != 2) {
                return new ScenarioResult(2, name, false, "Expected 2 roots, got: " + (roots != null ? roots.size() : "null"));
            }

            // Exception check for invalid URI
            try {
                underTest.buildClientRoots(Map.of("http://remote-server", "remote"));
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException for non-file URI");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario03(McpClientUnderTest underTest) {
        String name = "ImplementationSpec (Client Identity & Version)";
        try {
            Implementation impl = underTest.createClientImplementation("enterprise-agent", "2.1.0", "Enterprise Agent Client");
            if (impl == null) {
                return new ScenarioResult(3, name, false, "createClientImplementation returned null");
            }
            if (!"enterprise-agent".equals(impl.name()) || !"2.1.0".equals(impl.version())) {
                return new ScenarioResult(3, name, false, "Implementation name or version mismatch");
            }
            if (!"Enterprise Agent Client".equals(impl.description())) {
                return new ScenarioResult(3, name, false, "Implementation description mismatch");
            }

            try {
                underTest.createClientImplementation("   ", "1.0", "desc");
                return new ScenarioResult(3, name, false, "Expected IllegalArgumentException on blank name");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 2: Adapting MCP Tools to Spring AI ToolCallbacks (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario04(McpClientUnderTest underTest) {
        String name = "ToolDefinition (Schema Adaptation from MCP Tool)";
        try {
            Tool tool = Tool.builder("query_metrics")
                    .description("queries cluster metrics")
                    .inputSchema(Map.of("type", "object", "properties", Map.of("timeRange", Map.of("type", "string"))))
                    .build();

            ToolDefinition def = underTest.adaptMcpToolToDefinition(tool);
            if (def == null) {
                return new ScenarioResult(4, name, false, "adaptMcpToolToDefinition returned null");
            }
            if (!"query_metrics".equals(def.name()) || !"queries cluster metrics".equals(def.description())) {
                return new ScenarioResult(4, name, false, "ToolDefinition name or description mismatch");
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario05(McpClientUnderTest underTest) {
        String name = "ToolCallback (Standard Synchronous MCP Invocation Bridge)";
        try {
            McpClientAdapter client = createMockClient("cluster-mgr", null);
            Tool tool = client.listTools().get(0);

            ToolCallback callback = underTest.adaptMcpToolToCallback(client, tool);
            if (callback == null || callback.getToolDefinition() == null) {
                return new ScenarioResult(5, name, false, "adaptMcpToolToCallback returned null callback or definition");
            }

            String output = callback.call("{\"param\": 1}");
            if (output == null || !output.contains("status-ok from cluster-mgr")) {
                return new ScenarioResult(5, name, false, "Expected output containing 'status-ok', got: " + output);
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario06(McpClientUnderTest underTest) {
        String name = "ToolCallback (Resilient Error Trapping & Shielding)";
        try {
            McpClientAdapter client = createMockClient("resilience-svc", null);

            // Subtest A: Tool returns isError = true
            Tool failingTool = Tool.builder("failing_tool").description("failing").inputSchema(Map.of("type", "object")).build();
            ToolCallback cbFailing = underTest.adaptResilientMcpToolToCallback(client, failingTool);
            String resFailing = cbFailing.call("{}");
            if (resFailing == null || !resFailing.startsWith("ERROR:") || !resFailing.contains("Internal DB error")) {
                return new ScenarioResult(6, name, false, "Expected ERROR prefix for error result, got: " + resFailing);
            }

            // Subtest B: Tool throws runtime exception
            Tool explodingTool = Tool.builder("exploding_tool").description("exploding").inputSchema(Map.of("type", "object")).build();
            ToolCallback cbExploding = underTest.adaptResilientMcpToolToCallback(client, explodingTool);
            String resExploding = cbExploding.call("{}");
            if (resExploding == null || !resExploding.startsWith("ERROR:") || !resExploding.contains("Network connection reset")) {
                return new ScenarioResult(6, name, false, "Expected caught exception trapped into ERROR, got: " + resExploding);
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 3: Tool Discovery, Batch Adaptation & Namespacing (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario07(McpClientUnderTest underTest) {
        String name = "ToolDiscovery (Batch Callback Provider Generation)";
        try {
            Tool t1 = Tool.builder("t1").description("Tool 1").inputSchema(Map.of("type", "object")).build();
            Tool t2 = Tool.builder("t2").description("Tool 2").inputSchema(Map.of("type", "object")).build();
            McpClientAdapter client = createMockClient("prod-infra", List.of(t1, t2));

            List<ToolCallback> callbacks = underTest.createToolCallbacksForClient(client);
            if (callbacks == null || callbacks.size() != 2) {
                return new ScenarioResult(7, name, false, "Expected 2 callbacks, got: " + (callbacks != null ? callbacks.size() : "null"));
            }
            if (!"t1".equals(callbacks.get(0).getToolDefinition().name())) {
                return new ScenarioResult(7, name, false, "Expected first tool name 't1', got: " + callbacks.get(0).getToolDefinition().name());
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario08(McpClientUnderTest underTest) {
        String name = "ToolNaming (Multi-Server Prefix Formatting)";
        try {
            String prefixed = underTest.buildPrefixedToolName("github", "create_issue");
            if (prefixed == null || !prefixed.contains("create_issue")) {
                return new ScenarioResult(8, name, false, "Expected prefixed name containing 'create_issue', got: " + prefixed);
            }

            try {
                underTest.buildPrefixedToolName("   ", "tool");
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException on blank prefix");
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario09(McpClientUnderTest underTest) {
        String name = "FederatedDiscovery (Prefixed Tool Discovery with Collision Prevention)";
        try {
            Tool k8sLogs = Tool.builder("getLogs").description("k8s logs").inputSchema(Map.of("type", "object")).build();
            Tool awsLogs = Tool.builder("getLogs").description("aws logs").inputSchema(Map.of("type", "object")).build();
            McpClientAdapter clientK8s = createMockClient("k8s", List.of(k8sLogs));
            McpClientAdapter clientAws = createMockClient("aws", List.of(awsLogs));

            Map<String, ToolCallback> map = underTest.discoverPrefixedToolCallbacks(List.of(clientK8s, clientAws));
            if (map == null || map.size() != 2) {
                return new ScenarioResult(9, name, false, "Expected 2 prefixed callbacks, got: " + (map != null ? map.size() : "null"));
            }

            String k8sPrefixed = underTest.buildPrefixedToolName("k8s", "getLogs");
            String awsPrefixed = underTest.buildPrefixedToolName("aws", "getLogs");

            if (!map.containsKey(k8sPrefixed) || !map.containsKey(awsPrefixed)) {
                return new ScenarioResult(9, name, false, "Expected keys [" + k8sPrefixed + ", " + awsPrefixed + "], got: " + map.keySet());
            }

            String resK8s = map.get(k8sPrefixed).call("{}");
            if (!resK8s.contains("from k8s")) {
                return new ScenarioResult(9, name, false, "Expected routing to k8s, got: " + resK8s);
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 4: MCP Resources & Prompts Retrieval (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10(McpClientUnderTest underTest) {
        String name = "ResourceReader (Single Resource Prompt Context Attachment)";
        try {
            McpClientAdapter client = createMockClient("telemetry-svc", null);
            String context = underTest.formatResourceAsContext(client, "telemetry://nodes");

            if (context == null || !context.contains("[Resource: telemetry://nodes]") || !context.contains("status\": \"healthy")) {
                return new ScenarioResult(10, name, false, "Expected resource header and text, got: " + context);
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario11(McpClientUnderTest underTest) {
        String name = "ResourceReader (Multi-Resource Composite Context Assembly)";
        try {
            McpClientAdapter client = createMockClient("telemetry-svc", null);
            String assembled = underTest.assembleMultiResourceContext(client, List.of("telemetry://nodes", "config://cluster"));

            if (assembled == null || !assembled.contains("telemetry://nodes") || !assembled.contains("config://cluster")) {
                return new ScenarioResult(11, name, false, "Missing resource headers in assembled context: " + assembled);
            }
            if (!assembled.contains("\n---\n")) {
                return new ScenarioResult(11, name, false, "Expected delimiter '\\n---\\n' between resource blocks");
            }

            return new ScenarioResult(11, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario12(McpClientUnderTest underTest) {
        String name = "PromptReader (Parameterized Prompt Retrieval & Variable Binding)";
        try {
            McpClientAdapter client = createMockClient("prompt-svc", null);
            String promptText = underTest.fetchPromptMessageText(client, "remediate", Map.of("key", "val-99"));

            if (promptText == null || !promptText.contains("arg=val-99")) {
                return new ScenarioResult(12, name, false, "Expected prompt text containing 'arg=val-99', got: " + promptText);
            }

            return new ScenarioResult(12, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 5: Multi-Server Routing & Fault Resilience (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13(McpClientUnderTest underTest) {
        String name = "FederatedRegistry (Multi-Server Tool Registration & Dispatch)";
        try {
            MultiServerClientRegistry registry = new MultiServerClientRegistry();
            registry.registerClient(createMockClient("k8s", null));
            registry.registerClient(createMockClient("vault", null));

            List<String> prefixedTools = registry.listAllPrefixedTools();
            if (prefixedTools == null || prefixedTools.size() != 2) {
                return new ScenarioResult(13, name, false, "Expected 2 prefixed tools, got: " + (prefixedTools != null ? prefixedTools.size() : "null"));
            }

            String k8sPrefixed = underTest.buildPrefixedToolName("k8s", "fetch_status");
            String vaultPrefixed = underTest.buildPrefixedToolName("vault", "fetch_status");

            Set<String> set = Set.copyOf(prefixedTools);
            if (!set.contains(k8sPrefixed) || !set.contains(vaultPrefixed)) {
                return new ScenarioResult(13, name, false, "Expected [" + k8sPrefixed + ", " + vaultPrefixed + "], got: " + set);
            }

            String executed = registry.executePrefixedTool(k8sPrefixed, Map.of());
            if (executed == null || !executed.contains("status-ok from k8s")) {
                return new ScenarioResult(13, name, false, "Expected routing to k8s client, got: " + executed);
            }

            return new ScenarioResult(13, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario14(McpClientUnderTest underTest) {
        String name = "CatalogReload (Dynamic Tool List Reloading & Re-indexing)";
        try {
            MultiServerClientRegistry registry = new MultiServerClientRegistry();

            List<Tool> dynamicTools = new ArrayList<>();
            dynamicTools.add(Tool.builder("list_pods").description("list pods").inputSchema(Map.of("type", "object")).build());

            McpClientAdapter dynamicClient = new McpClientAdapter() {
                @Override
                public String serverName() {
                    return "k8s";
                }

                @Override
                public List<Tool> listTools() {
                    return Collections.unmodifiableList(dynamicTools);
                }

                @Override
                public CallToolResult callTool(String toolName, Map<String, Object> arguments) {
                    return new CallToolResult(List.of(new TextContent("Executed " + toolName)), false, null, null);
                }

                @Override
                public ReadResourceResult readResource(String uri) {
                    return new ReadResourceResult(List.of());
                }

                @Override
                public GetPromptResult getPrompt(String promptName, Map<String, Object> arguments) {
                    return new GetPromptResult("desc", List.of());
                }
            };

            registry.registerClient(dynamicClient);

            // Initially 1 tool registered
            List<String> toolsBefore = registry.listAllPrefixedTools();
            String listPodsPrefixed = underTest.buildPrefixedToolName("k8s", "list_pods");
            if (toolsBefore.size() != 1 || !toolsBefore.contains(listPodsPrefixed)) {
                return new ScenarioResult(14, name, false, "Expected 1 initial tool, got: " + toolsBefore);
            }

            // Server dynamically adds 2 new tools
            dynamicTools.add(Tool.builder("restart_pod").description("restart pod").inputSchema(Map.of("type", "object")).build());
            dynamicTools.add(Tool.builder("get_logs").description("get logs").inputSchema(Map.of("type", "object")).build());

            ToolListReloadReport report = underTest.reloadServerTools(registry, "k8s");
            if (report == null) {
                return new ScenarioResult(14, name, false, "reloadServerTools returned null report");
            }

            if (!"k8s".equals(report.serverId()) || report.previousToolCount() != 1 || report.updatedToolCount() != 3) {
                return new ScenarioResult(14, name, false, "Report counts mismatched: " + report);
            }

            List<String> toolsAfter = registry.listAllPrefixedTools();
            if (toolsAfter.size() != 3) {
                return new ScenarioResult(14, name, false, "Expected registry to have 3 tools after reload, got: " + toolsAfter.size());
            }

            // Test execution of newly reloaded tool
            String restartPrefixed = underTest.buildPrefixedToolName("k8s", "restart_pod");
            String res = registry.executePrefixedTool(restartPrefixed, Map.of());
            if (!res.contains("Executed restart_pod")) {
                return new ScenarioResult(14, name, false, "Expected execution output 'Executed restart_pod', got: " + res);
            }

            // Unknown server breach check
            try {
                underTest.reloadServerTools(registry, "unknown_server");
                return new ScenarioResult(14, name, false, "Expected McpClientBreachException for unknown server");
            } catch (McpClientBreachException expected) {
                // pass
            }

            return new ScenarioResult(14, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario15(McpClientUnderTest underTest) {
        String name = "SamplingProtocol (Server-to-Client LLM Delegation & Quotas)";
        try {
            FakeMcpChatModel chatModel = new FakeMcpChatModel();
            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

            // Subtest A: Successful sampling within token ceiling
            chatModel.enqueue("Generated optimized query: SELECT id, name FROM accounts WHERE status = 'ACTIVE'");
            chatModel.setTokenUsage(70, 25);

            SamplingRequest request = new SamplingRequest("Optimize query for accounts", 100, 0.7);
            SamplingResponse response = underTest.handleSamplingRequest(chatClientBuilder, request, 200);

            if (response == null) {
                return new ScenarioResult(15, name, false, "handleSamplingRequest returned null response");
            }
            if (!response.content().contains("SELECT id, name FROM accounts")) {
                return new ScenarioResult(15, name, false, "Expected completion content from chat model, got: " + response.content());
            }
            if (response.tokensUsed() != 95) {
                return new ScenarioResult(15, name, false, "Expected 95 tokens used (70+25), got: " + response.tokensUsed());
            }

            // Subtest B: Token ceiling breach
            try {
                SamplingRequest breachRequest = new SamplingRequest("Deep reasoning task", 350, 0.2);
                underTest.handleSamplingRequest(chatClientBuilder, breachRequest, 200);
                return new ScenarioResult(15, name, false, "Expected McpClientBreachException when request.maxTokens exceeds ceiling");
            } catch (McpClientBreachException expected) {
                // pass
            }

            return new ScenarioResult(15, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, t.getMessage());
        }
    }

    // =========================================================================
    // REPORT FORMATTER
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 08 EXERCISE 02: MCP CLIENT INTEGRATION & AGENTIC ORCHESTRATION");
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
