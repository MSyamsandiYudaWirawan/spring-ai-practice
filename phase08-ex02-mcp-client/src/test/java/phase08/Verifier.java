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
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selectedScenario);
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        System.exit(anyFailed ? 99 : 0);
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        Assertions.assertFalse(anyFailed, "Verifier detected scenario failures. See printed report above.");
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        McpClientUnderTest underTest = new McpClientUnderTest();
        List<ScenarioResult> results = new ArrayList<>();

        // Topic 1: Client Capabilities & Specs (Scenarios 1 - 3)
        if (selected == 0 || selected == 1) results.add(verifyScenario01(underTest));
        if (selected == 0 || selected == 2) results.add(verifyScenario02(underTest));
        if (selected == 0 || selected == 3) results.add(verifyScenario03(underTest));

        // Topic 2: Adapting MCP Tools to Spring AI ToolCallbacks (Scenarios 4 - 6)
        if (selected == 0 || selected == 4) results.add(verifyScenario04(underTest));
        if (selected == 0 || selected == 5) results.add(verifyScenario05(underTest));
        if (selected == 0 || selected == 6) results.add(verifyScenario06(underTest));

        // Topic 3: Tool Discovery, Batch Adaptation & Namespacing (Scenarios 7 - 9)
        if (selected == 0 || selected == 7) results.add(verifyScenario07(underTest));
        if (selected == 0 || selected == 8) results.add(verifyScenario08(underTest));
        if (selected == 0 || selected == 9) results.add(verifyScenario09(underTest));

        // Topic 4: MCP Resources & Prompts Retrieval (Scenarios 10 - 12)
        if (selected == 0 || selected == 10) results.add(verifyScenario10(underTest));
        if (selected == 0 || selected == 11) results.add(verifyScenario11(underTest));
        if (selected == 0 || selected == 12) results.add(verifyScenario12(underTest));

        // Topic 5: Multi-Server Routing & Fault Resilience (Scenarios 13 - 15)
        if (selected == 0 || selected == 13) results.add(verifyScenario13(underTest));
        if (selected == 0 || selected == 14) results.add(verifyScenario14(underTest));
        if (selected == 0 || selected == 15) results.add(verifyScenario15(underTest));

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
        List<String> hints = List.of(
                "ClientCapabilities informs an MCP server which features the client supports (e.g. roots, sampling).",
                "Configure roots capability with RootCapabilities(listChanged) if enabled; null if disabled.",
                "ClientCapabilities.builder().roots(enableRoots ? new ClientCapabilities.RootCapabilities(true) : null).sampling(enableSampling ? new ClientCapabilities.SamplingCapabilities(...) : null).build();"
        );
        try {
            ClientCapabilities caps1 = underTest.createClientCapabilities(true, false);
            if (caps1 == null || caps1.roots() == null || !Boolean.TRUE.equals(caps1.roots().listChanged())) {
                return new ScenarioResult(1, name, false, "Expected roots capability with listChanged=true", hints);
            }
            if (caps1.sampling() != null) {
                return new ScenarioResult(1, name, false, "Expected sampling to be null when disabled", hints);
            }

            ClientCapabilities caps2 = underTest.createClientCapabilities(false, true);
            if (caps2 == null || caps2.sampling() == null || caps2.roots() != null) {
                return new ScenarioResult(1, name, false, "Expected sampling enabled and roots null", hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario02(McpClientUnderTest underTest) {
        String name = "ClientRoots (URI Validation & Construction)";
        List<String> hints = List.of(
                "Client roots define local directory boundaries (file:// URIs) exposed to the MCP server.",
                "Throw IllegalArgumentException if any root URI does not start with 'file://'.",
                "uriMap.entrySet().stream().map(e -> { if (!e.getKey().startsWith(\"file://\")) throw new IllegalArgumentException(...); return new Root(e.getKey(), e.getValue()); }).toList();"
        );
        try {
            List<Root> roots = underTest.buildClientRoots(Map.of(
                    "file:///workspace/project", "workspace",
                    "file:///logs/agent", "logs"
            ));

            if (roots == null || roots.size() != 2) {
                return new ScenarioResult(2, name, false, "Expected 2 roots, got: " + (roots != null ? roots.size() : "null"), hints);
            }

            // Exception check for invalid URI
            try {
                underTest.buildClientRoots(Map.of("http://remote-server", "remote"));
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException for non-file URI", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario03(McpClientUnderTest underTest) {
        String name = "ImplementationSpec (Client Identity & Version)";
        List<String> hints = List.of(
                "Implementation records identify the client software name, version, and descriptive title.",
                "Throw IllegalArgumentException on null or blank name or version.",
                "new Implementation(name.trim(), title, version.trim());"
        );
        try {
            Implementation impl = underTest.createClientImplementation("enterprise-agent", "2.1.0", "Enterprise Agent Client");
            if (impl == null) {
                return new ScenarioResult(3, name, false, "createClientImplementation returned null", hints);
            }
            if (!"enterprise-agent".equals(impl.name()) || !"2.1.0".equals(impl.version())) {
                return new ScenarioResult(3, name, false, "Implementation name or version mismatch", hints);
            }
            if (!"Enterprise Agent Client".equals(impl.description())) {
                return new ScenarioResult(3, name, false, "Implementation description mismatch", hints);
            }

            try {
                underTest.createClientImplementation("   ", "1.0", "desc");
                return new ScenarioResult(3, name, false, "Expected IllegalArgumentException on blank name", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 2: Adapting MCP Tools to Spring AI ToolCallbacks (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario04(McpClientUnderTest underTest) {
        String name = "ToolDefinition (Schema Adaptation from MCP Tool)";
        List<String> hints = List.of(
                "Adapt an MCP Tool schema to a Spring AI ToolDefinition.",
                "Serialize inputSchema to JSON string or pass through directly.",
                "ToolDefinition.builder().name(tool.name()).description(tool.description()).inputSchema(JsonHelper.toJson(tool.inputSchema())).build();"
        );
        try {
            Tool tool = Tool.builder("query_metrics")
                    .description("queries cluster metrics")
                    .inputSchema(Map.of("type", "object", "properties", Map.of("timeRange", Map.of("type", "string"))))
                    .build();

            ToolDefinition def = underTest.adaptMcpToolToDefinition(tool);
            if (def == null) {
                return new ScenarioResult(4, name, false, "adaptMcpToolToDefinition returned null", hints);
            }
            if (!"query_metrics".equals(def.name()) || !"queries cluster metrics".equals(def.description())) {
                return new ScenarioResult(4, name, false, "ToolDefinition name or description mismatch", hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario05(McpClientUnderTest underTest) {
        String name = "ToolCallback (Standard Synchronous MCP Invocation Bridge)";
        List<String> hints = List.of(
                "Wrap an MCP client tool call in Spring AI's ToolCallback interface.",
                "Deserialize JSON input into Map<String, Object> arguments and invoke client.callTool(toolName, args).",
                "ToolCallback.builder().toolDefinition(def).call((toolInput) -> extractText(client.callTool(tool.name(), parseArgs(toolInput)))).build();"
        );
        try {
            McpClientAdapter client = createMockClient("cluster-mgr", null);
            Tool tool = client.listTools().get(0);

            ToolCallback callback = underTest.adaptMcpToolToCallback(client, tool);
            if (callback == null || callback.getToolDefinition() == null) {
                return new ScenarioResult(5, name, false, "adaptMcpToolToCallback returned null callback or definition", hints);
            }

            String output = callback.call("{\"param\": 1}");
            if (output == null || !output.contains("status-ok from cluster-mgr")) {
                return new ScenarioResult(5, name, false, "Expected output containing 'status-ok', got: " + output, hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario06(McpClientUnderTest underTest) {
        String name = "ToolCallback (Resilient Error Trapping & Shielding)";
        List<String> hints = List.of(
                "Resilient tool callbacks shield LLMs from runtime crashes by translating MCP errors into descriptive error strings.",
                "If CallToolResult.isError() is true or an exception occurs, return a formatted string starting with 'ERROR: '.",
                "try { CallToolResult res = client.callTool(...); if (Boolean.TRUE.equals(res.isError())) return \"ERROR: \" + extract(res); return extract(res); } catch (Exception e) { return \"ERROR: \" + e.getMessage(); }"
        );
        try {
            McpClientAdapter client = createMockClient("resilience-svc", null);

            // Subtest A: Tool returns isError = true
            Tool failingTool = Tool.builder("failing_tool").description("failing").inputSchema(Map.of("type", "object")).build();
            ToolCallback cbFailing = underTest.adaptResilientMcpToolToCallback(client, failingTool);
            String resFailing = cbFailing.call("{}");
            if (resFailing == null || !resFailing.startsWith("ERROR:") || !resFailing.contains("Internal DB error")) {
                return new ScenarioResult(6, name, false, "Expected ERROR prefix for error result, got: " + resFailing, hints);
            }

            // Subtest B: Tool throws runtime exception
            Tool explodingTool = Tool.builder("exploding_tool").description("exploding").inputSchema(Map.of("type", "object")).build();
            ToolCallback cbExploding = underTest.adaptResilientMcpToolToCallback(client, explodingTool);
            String resExploding = cbExploding.call("{}");
            if (resExploding == null || !resExploding.startsWith("ERROR:") || !resExploding.contains("Network connection reset")) {
                return new ScenarioResult(6, name, false, "Expected caught exception trapped into ERROR, got: " + resExploding, hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 3: Tool Discovery, Batch Adaptation & Namespacing (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario07(McpClientUnderTest underTest) {
        String name = "ToolDiscovery (Batch Callback Provider Generation)";
        List<String> hints = List.of(
                "Batch discover and adapt all tools hosted by an MCP client into a List<ToolCallback>.",
                "Call client.listTools() and adapt each Tool into a ToolCallback.",
                "client.listTools().stream().map(tool -> adaptMcpToolToCallback(client, tool)).toList();"
        );
        try {
            Tool t1 = Tool.builder("t1").description("Tool 1").inputSchema(Map.of("type", "object")).build();
            Tool t2 = Tool.builder("t2").description("Tool 2").inputSchema(Map.of("type", "object")).build();
            McpClientAdapter client = createMockClient("prod-infra", List.of(t1, t2));

            List<ToolCallback> callbacks = underTest.createToolCallbacksForClient(client);
            if (callbacks == null || callbacks.size() != 2) {
                return new ScenarioResult(7, name, false, "Expected 2 callbacks, got: " + (callbacks != null ? callbacks.size() : "null"), hints);
            }
            if (!"t1".equals(callbacks.get(0).getToolDefinition().name())) {
                return new ScenarioResult(7, name, false, "Expected first tool name 't1', got: " + callbacks.get(0).getToolDefinition().name(), hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario08(McpClientUnderTest underTest) {
        String name = "ToolNaming (Multi-Server Prefix Formatting)";
        List<String> hints = List.of(
                "Multi-server setups prevent tool name collisions by namespacing tool names (e.g., serverId_toolName).",
                "Throw IllegalArgumentException if serverId or toolName is blank.",
                "serverId.trim() + \"_\" + toolName.trim();"
        );
        try {
            String prefixed = underTest.buildPrefixedToolName("github", "create_issue");
            if (prefixed == null || !prefixed.contains("create_issue")) {
                return new ScenarioResult(8, name, false, "Expected prefixed name containing 'create_issue', got: " + prefixed, hints);
            }

            try {
                underTest.buildPrefixedToolName("   ", "tool");
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException on blank prefix", hints);
            } catch (IllegalArgumentException expected) {
                // pass
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario09(McpClientUnderTest underTest) {
        String name = "FederatedDiscovery (Prefixed Tool Discovery with Collision Prevention)";
        List<String> hints = List.of(
                "Federated discovery scans multiple MCP clients and registers their tools under unique prefixed keys.",
                "For each client, iterate client.listTools(), prefix each name, and store in Map<String, ToolCallback>.",
                "map.put(buildPrefixedToolName(client.serverName(), tool.name()), adaptMcpToolToCallback(client, tool));"
        );
        try {
            Tool k8sLogs = Tool.builder("getLogs").description("k8s logs").inputSchema(Map.of("type", "object")).build();
            Tool awsLogs = Tool.builder("getLogs").description("aws logs").inputSchema(Map.of("type", "object")).build();
            McpClientAdapter clientK8s = createMockClient("k8s", List.of(k8sLogs));
            McpClientAdapter clientAws = createMockClient("aws", List.of(awsLogs));

            Map<String, ToolCallback> map = underTest.discoverPrefixedToolCallbacks(List.of(clientK8s, clientAws));
            if (map == null || map.size() != 2) {
                return new ScenarioResult(9, name, false, "Expected 2 prefixed callbacks, got: " + (map != null ? map.size() : "null"), hints);
            }

            String k8sPrefixed = underTest.buildPrefixedToolName("k8s", "getLogs");
            String awsPrefixed = underTest.buildPrefixedToolName("aws", "getLogs");

            if (!map.containsKey(k8sPrefixed) || !map.containsKey(awsPrefixed)) {
                return new ScenarioResult(9, name, false, "Expected keys [" + k8sPrefixed + ", " + awsPrefixed + "], got: " + map.keySet(), hints);
            }

            String resK8s = map.get(k8sPrefixed).call("{}");
            if (!resK8s.contains("from k8s")) {
                return new ScenarioResult(9, name, false, "Expected routing to k8s, got: " + resK8s, hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 4: MCP Resources & Prompts Retrieval (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10(McpClientUnderTest underTest) {
        String name = "ResourceReader (Single Resource Prompt Context Attachment)";
        List<String> hints = List.of(
                "Fetch an MCP resource and format it as structured context for LLM prompt augmentation.",
                "Call client.readResource(uri); extract TextResourceContents.",
                "String.format(\"[Resource: %s]\\n%s\", uri, textContent);"
        );
        try {
            McpClientAdapter client = createMockClient("telemetry-svc", null);
            String context = underTest.formatResourceAsContext(client, "telemetry://nodes");

            if (context == null || !context.contains("[Resource: telemetry://nodes]") || !context.contains("status\": \"healthy")) {
                return new ScenarioResult(10, name, false, "Expected resource header and text, got: " + context, hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11(McpClientUnderTest underTest) {
        String name = "ResourceReader (Multi-Resource Composite Context Assembly)";
        List<String> hints = List.of(
                "Read multiple MCP resources and assemble them into a composite prompt context block.",
                "Delimit adjacent resource blocks with '\\n---\\n'.",
                "uris.stream().map(u -> formatResourceAsContext(client, u)).collect(Collectors.joining(\"\\n---\\n\"));"
        );
        try {
            McpClientAdapter client = createMockClient("telemetry-svc", null);
            String assembled = underTest.assembleMultiResourceContext(client, List.of("telemetry://nodes", "config://cluster"));

            if (assembled == null || !assembled.contains("telemetry://nodes") || !assembled.contains("config://cluster")) {
                return new ScenarioResult(11, name, false, "Missing resource headers in assembled context: " + assembled, hints);
            }
            if (!assembled.contains("\n---\n")) {
                return new ScenarioResult(11, name, false, "Expected delimiter '\\n---\\n' between resource blocks", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12(McpClientUnderTest underTest) {
        String name = "PromptReader (Parameterized Prompt Retrieval & Variable Binding)";
        List<String> hints = List.of(
                "Retrieve a prompt template from an MCP server with bound argument parameters.",
                "Call client.getPrompt(name, args); extract text from the first PromptMessage content.",
                "GetPromptResult res = client.getPrompt(promptName, arguments); return ((TextContent) res.messages().get(0).content()).text();"
        );
        try {
            McpClientAdapter client = createMockClient("prompt-svc", null);
            String promptText = underTest.fetchPromptMessageText(client, "remediate", Map.of("key", "val-99"));

            if (promptText == null || !promptText.contains("arg=val-99")) {
                return new ScenarioResult(12, name, false, "Expected prompt text containing 'arg=val-99', got: " + promptText, hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, t.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 5: Multi-Server Routing & Fault Resilience (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13(McpClientUnderTest underTest) {
        String name = "FederatedRegistry (Multi-Server Tool Registration & Dispatch)";
        List<String> hints = List.of(
                "MultiServerClientRegistry maintains a catalog of connected MCP servers and routes tool calls by prefix.",
                "Parse serverId and toolName by splitting on the first underscore ('_').",
                "Find client by serverId; invoke client.callTool(originalToolName, arguments) and return text."
        );
        try {
            MultiServerClientRegistry registry = new MultiServerClientRegistry();
            registry.registerClient(createMockClient("k8s", null));
            registry.registerClient(createMockClient("vault", null));

            List<String> prefixedTools = registry.listAllPrefixedTools();
            if (prefixedTools == null || prefixedTools.size() != 2) {
                return new ScenarioResult(13, name, false, "Expected 2 prefixed tools, got: " + (prefixedTools != null ? prefixedTools.size() : "null"), hints);
            }

            String k8sPrefixed = underTest.buildPrefixedToolName("k8s", "fetch_status");
            String vaultPrefixed = underTest.buildPrefixedToolName("vault", "fetch_status");

            Set<String> set = Set.copyOf(prefixedTools);
            if (!set.contains(k8sPrefixed) || !set.contains(vaultPrefixed)) {
                return new ScenarioResult(13, name, false, "Expected [" + k8sPrefixed + ", " + vaultPrefixed + "], got: " + set, hints);
            }

            String executed = registry.executePrefixedTool(k8sPrefixed, Map.of());
            if (executed == null || !executed.contains("status-ok from k8s")) {
                return new ScenarioResult(13, name, false, "Expected routing to k8s client, got: " + executed, hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14(McpClientUnderTest underTest) {
        String name = "CatalogReload (Dynamic Tool List Reloading & Re-indexing)";
        List<String> hints = List.of(
                "Dynamic tool list reloading queries an MCP server for newly added or removed tools and updates the registry.",
                "Throw McpClientBreachException if serverId is not found in registry.",
                "Count previous tools, fetch new tools from client, rebuild registry mappings, and return ToolListReloadReport(serverId, prevCount, newCount, timestamp);"
        );
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
                return new ScenarioResult(14, name, false, "Expected 1 initial tool, got: " + toolsBefore, hints);
            }

            // Server dynamically adds 2 new tools
            dynamicTools.add(Tool.builder("restart_pod").description("restart pod").inputSchema(Map.of("type", "object")).build());
            dynamicTools.add(Tool.builder("get_logs").description("get logs").inputSchema(Map.of("type", "object")).build());

            ToolListReloadReport report = underTest.reloadServerTools(registry, "k8s");
            if (report == null) {
                return new ScenarioResult(14, name, false, "reloadServerTools returned null report", hints);
            }

            if (!"k8s".equals(report.serverId()) || report.previousToolCount() != 1 || report.updatedToolCount() != 3) {
                return new ScenarioResult(14, name, false, "Report counts mismatched: " + report, hints);
            }

            List<String> toolsAfter = registry.listAllPrefixedTools();
            if (toolsAfter.size() != 3) {
                return new ScenarioResult(14, name, false, "Expected registry to have 3 tools after reload, got: " + toolsAfter.size(), hints);
            }

            // Test execution of newly reloaded tool
            String restartPrefixed = underTest.buildPrefixedToolName("k8s", "restart_pod");
            String res = registry.executePrefixedTool(restartPrefixed, Map.of());
            if (!res.contains("Executed restart_pod")) {
                return new ScenarioResult(14, name, false, "Expected execution output 'Executed restart_pod', got: " + res, hints);
            }

            // Unknown server breach check
            try {
                underTest.reloadServerTools(registry, "unknown_server");
                return new ScenarioResult(14, name, false, "Expected McpClientBreachException for unknown server", hints);
            } catch (McpClientBreachException expected) {
                // pass
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15(McpClientUnderTest underTest) {
        String name = "SamplingProtocol (Server-to-Client LLM Delegation & Quotas)";
        List<String> hints = List.of(
                "Sampling protocol delegates LLM generation requests from MCP servers back to the host client.",
                "Throw McpClientBreachException if request.maxTokens() exceeds the configured maxTokenCeiling.",
                "ChatResponse resp = chatClientBuilder.build().prompt().user(request.prompt()).call().chatResponse(); return new SamplingResponse(resp.getResult().getOutput().getContent(), totalTokens);"
        );
        try {
            FakeMcpChatModel chatModel = new FakeMcpChatModel();
            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

            // Subtest A: Successful sampling within token ceiling
            chatModel.enqueue("Generated optimized query: SELECT id, name FROM accounts WHERE status = 'ACTIVE'");
            chatModel.setTokenUsage(70, 25);

            SamplingRequest request = new SamplingRequest("Optimize query for accounts", 100, 0.7);
            SamplingResponse response = underTest.handleSamplingRequest(chatClientBuilder, request, 200);

            if (response == null) {
                return new ScenarioResult(15, name, false, "handleSamplingRequest returned null response", hints);
            }
            if (!response.content().contains("SELECT id, name FROM accounts")) {
                return new ScenarioResult(15, name, false, "Expected completion content from chat model, got: " + response.content(), hints);
            }
            if (response.tokensUsed() != 95) {
                return new ScenarioResult(15, name, false, "Expected 95 tokens used (70+25), got: " + response.tokensUsed(), hints);
            }

            // Subtest B: Token ceiling breach
            try {
                SamplingRequest breachRequest = new SamplingRequest("Deep reasoning task", 350, 0.2);
                underTest.handleSamplingRequest(chatClientBuilder, breachRequest, 200);
                return new ScenarioResult(15, name, false, "Expected McpClientBreachException when request.maxTokens exceeds ceiling", hints);
            } catch (McpClientBreachException expected) {
                // pass
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, t.getMessage(), hints);
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
