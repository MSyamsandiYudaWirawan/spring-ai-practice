# Phase 08 Exercise 02: Model Context Protocol (MCP) Client Integration, Tool Providers & Agentic Orchestration

## Focus & Learning Objectives
This exercise teaches the client side of the official Model Context Protocol (MCP) Java SDK (`io.modelcontextprotocol.sdk:mcp-core:2.0.0`) and Spring AI's native MCP integration (`org.springframework.ai:spring-ai-mcp:2.0.1`):
- **Client Protocol Capabilities:** Negotiating client capabilities (`roots`, `sampling`).
- **MCP Tool Adaptation:** Converting MCP schema definitions into Spring AI `ToolDefinition` instances.
- **Spring AI ToolCallback Adapter:** Bridging MCP synchronous tools into executable Spring AI `ToolCallback` instances with JSON parameter parsing.
- **Multi-Tool Discovery:** Querying server tool catalogs and bulk-generating Spring AI callbacks.
- **Namespace Collision Prevention:** Prefixing tool names across federated servers via `McpToolUtils.prefixedToolName`.
- **Resource Reading into Context:** Fetching MCP resources and formatting them into LLM prompt contexts.
- **Parameterized Prompt Retrieval:** Retrieving and formatting prompt templates hosted on MCP servers.
- **Resilient Tool Execution:** Implementing defensive error boundaries that intercept failures and return standardized error signals.
- **Multi-Server Federated Registry:** Aggregating tools across heterogeneous MCP servers into a single dispatchable catalog.
- **End-to-End Enterprise Agentic Gateway:** Orchestrating autonomous workflows using `ChatClient` with dynamic MCP tool binding and token guardrails.
- **Dynamic Tool List Reloading:** Handling server-side tool updates (`notifications/tools/list_changed`) and re-indexing active tools.
- **MCP Sampling Protocol:** Implementing reverse LLM delegation where an MCP server requests completions from the client under token guardrails.

---

## Scenarios
1. **Scenario 01: Client Capability Negotiation & Specs**
   - Declare client protocol features (`roots`, `sampling`).
2. **Scenario 02: Tool Definition Adaptation from MCP Schemas**
   - Convert MCP `Tool` schemas into Spring AI `ToolDefinition`.
3. **Scenario 03: Adapting MCP Sync Tools to ToolCallbacks**
   - Bridge MCP tool invocations to Spring AI `ToolCallback` with JSON argument handling.
4. **Scenario 04: Multi-Tool Discovery & Callback Provider Generation**
   - Discover all tools from an MCP server and construct callback providers.
5. **Scenario 05: Multi-Server Tool Name Prefixing (Collision Prevention)**
   - Generate namespaced tool identifiers to avoid collisions in multi-server environments.
6. **Scenario 06: MCP Resource Reading into Prompt Context**
   - Read server resources and format them as contextual prompt attachments.
7. **Scenario 07: MCP Parameterized Prompt Retrieval**
   - Query parameterized prompt templates from MCP servers with variable substitution.
8. **Scenario 08: Tool Execution Resilience & Error Boundary**
   - Intercept tool runtime errors and format graceful error feedback for LLM recovery.
9. **Scenario 09: Multi-Server Client Registry & Unified Tool Dispatcher**
   - Register multiple MCP servers, index prefixed tools, and route tool calls.
10. **Scenario 10: End-to-End Enterprise Agentic Gateway with MCP Tool Calling**
    - Execute an autonomous multi-tool agentic loop using `ChatClient` with token ceiling validation.
11. **Scenario 11: Dynamic Tool List Reloading & Registry Re-indexing**
    - Invalidate cached tool definitions upon server catalog updates and re-index active tools.
12. **Scenario 12: MCP Protocol Sampling Handler (Server-to-Client LLM Delegation)**
    - Execute client-side LLM sampling requested by an MCP server while enforcing token limits.

---

## Verification
Run the offline verification harness:
```powershell
mvn test-compile exec:java -pl phase08-ex02-mcp-client
```

Or run standard Maven tests:
```powershell
mvn test -pl phase08-ex02-mcp-client
```

A verified reference implementation is available in `docs/golden/phase08-ex02-golden.md`.
