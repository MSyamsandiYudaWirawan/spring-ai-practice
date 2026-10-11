# Phase 08 Exercise 02: Model Context Protocol (MCP) Client Integration, Tool Providers & Agentic Orchestration

## Focus & Learning Objectives
This exercise drills the client side of the official Model Context Protocol (MCP) Java SDK (`io.modelcontextprotocol.sdk:mcp-core:2.0.0`) and Spring AI's native MCP integration (`org.springframework.ai:spring-ai-mcp:2.0.1`):
- **Client Protocol Capabilities & Roots:** Declaring client capabilities (`roots`, `sampling`) and constructing root URI lists with validation.
- **Adapting MCP Tools to Spring AI ToolCallbacks:** Converting MCP schemas to `ToolDefinition`, bridging synchronous tool execution into `ToolCallback`, and implementing resilient exception shielding.
- **Tool Discovery, Batch Adaptation & Namespacing:** Discovering tools from remote servers, formatting multi-server prefixes (`McpToolUtils.prefixedToolName`), and generating prefixed tool callback maps.
- **MCP Resources & Prompts Retrieval:** Reading single and multiple resources into LLM prompt contexts, and querying parameterized prompt templates with argument bindings.
- **Multi-Server Routing & Fault Resilience:** Managing federated multi-server registries, dynamic tool catalog reloading upon server updates, and executing reverse LLM sampling requests under token ceilings.

---

## Repetitive Muscle Memory Structure (15 Scenarios)

### Topic 1: Client Capabilities & Specs (Scenarios 1 – 3)
1. **Scenario 01: Client Capabilities Declaration** — Build `ClientCapabilities` with `roots` and `sampling` flags.
2. **Scenario 02: Client Roots List Construction & URI Validation** — Validate `file://` URIs and construct `Root` descriptors.
3. **Scenario 03: Client Implementation & Metadata Spec** — Construct `Implementation` client descriptors with name, version, and description.

### Topic 2: Adapting MCP Tools to Spring AI ToolCallbacks (Scenarios 4 – 6)
4. **Scenario 04: ToolDefinition Adaptation from MCP Schemas** — Convert MCP `Tool` schemas to Spring AI `ToolDefinition` via `McpToolUtils`.
5. **Scenario 05: Standard MCP Tool to ToolCallback Adaptation** — Bridge synchronous tool calls into executable Spring AI `ToolCallback` instances.
6. **Scenario 06: Resilient ToolCallback with Fault Shielding** — Intercept execution faults and error flags, formatting resilient `ERROR: ` feedback without crashing.

### Topic 3: Tool Discovery, Batch Adaptation & Namespacing (Scenarios 7 – 9)
7. **Scenario 07: Multi-Tool Discovery & Batch Callback Generation** — Discover all tools from a client and batch-adapt to `List<ToolCallback>`.
8. **Scenario 08: Multi-Server Tool Name Prefixing** — Construct collision-free namespaced tool identifiers via `McpToolUtils.prefixedToolName`.
9. **Scenario 09: Prefixed Tool Discovery with Collision Prevention** — Discover tools across federated servers, namespacing tool definitions while maintaining execution routing.

### Topic 4: MCP Resources & Prompts Retrieval (Scenarios 10 – 12)
10. **Scenario 10: Single Resource Reading into Prompt Context** — Fetch a server resource and format it as context header and text.
11. **Scenario 11: Multi-Resource Context Assembly** — Batch-fetch multiple resources and join them into a single delimited context block.
12. **Scenario 12: Parameterized Prompt Retrieval & Argument Binding** — Query prompt templates with arguments and extract rendered prompt message text.

### Topic 5: Multi-Server Routing & Fault Resilience (Scenarios 13 – 15)
13. **Scenario 13: Multi-Server Tool Routing Registry** — Maintain federated servers, index prefixed tools, and route tool invocations.
14. **Scenario 14: Dynamic Tool List Reloading & Registry Re-indexing** — Invalidate cached tools, query updated catalogs, and re-index active tools.
15. **Scenario 15: MCP Protocol Sampling Handler (Server-to-Client LLM Delegation)** — Execute reverse LLM sampling requests while enforcing token ceilings.

---

## Verification
Run standard Maven tests:
```powershell
mvn test -pl phase08-ex02-mcp-client
```

A verified reference implementation is available in `docs/golden/phase08-ex02-golden.md`.
