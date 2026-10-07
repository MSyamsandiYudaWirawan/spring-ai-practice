# Phase 08 Exercise 01: Model Context Protocol (MCP) Server Architecture & Protocol Specifications

## Focus & Learning Objectives
This exercise teaches the official Model Context Protocol (MCP) Java SDK (`io.modelcontextprotocol.sdk:mcp-core:2.0.0`) and Spring AI's native MCP integration (`org.springframework.ai:spring-ai-mcp:2.0.1`):
- **Server Protocol Capabilities:** Negotiating server capabilities (`tools`, `resources`, `prompts`, `logging`).
- **Tool Schema Generation:** Defining tools with JSON Schema input validation (`type: object`, `properties`, `required`).
- **Execution Handlers:** Wrapping tool execution into `CallToolResult` with structured `TextContent`.
- **Resilient Tool Execution:** Shielding the server runtime by catching tool errors and returning `isError: true` signals.
- **Spring AI Tool Adapter:** Exporting native Spring AI `@Tool` and `ToolCallback` beans to MCP `SyncToolSpecification` via `McpToolUtils`.
- **MCP Resources:** Exposing static and dynamic resources (URIs, mime-types, and read handlers).
- **MCP Prompts:** Exposing parameterized prompt templates with arguments and variable substitution.
- **Security Allowlists:** Enforcing tool execution allowlists and role gates.
- **Server Telemetry & Auditing:** Tracking total tool executions, success rates, errors, and per-tool frequency.
- **Enterprise MCP Server Registry:** Managing composite tool, resource, and prompt catalogs with unified dispatch and error boundaries.

---

## Scenarios
1. **Scenario 01: Server Capabilities Declaration**
   - Configure and build `ServerCapabilities` declaring protocol features.
2. **Scenario 02: Tool Definition with JSON Input Schema**
   - Construct a `Tool` with JSON Schema input specifications.
3. **Scenario 03: SyncToolSpecification Execution Handler**
   - Implement execution handler logic wrapped in `CallToolResult`.
4. **Scenario 04: Resilient Tool Execution & Error Signal Wrapping**
   - Catch internal tool exceptions and return error results safely.
5. **Scenario 05: Spring AI ToolCallback to MCP Tool Adapter**
   - Bridge Spring AI `ToolCallback` into MCP `SyncToolSpecification`.
6. **Scenario 06: MCP Static & Dynamic Resource Specification**
   - Register MCP resources and read handlers (`ReadResourceResult`).
7. **Scenario 07: MCP Parameterized Prompt Template Specification**
   - Register MCP prompts with arguments and formatting logic (`GetPromptResult`).
8. **Scenario 08: Security Allowlist & Tool Filter**
   - Filter available tools against security allowlists.
9. **Scenario 09: MCP Server Telemetry & Audit Recorder**
   - Audit tool invocations, failures, and per-tool call metrics.
10. **Scenario 10: Composite Enterprise MCP Server Registry & Router**
    - Orchestrate tool, resource, and prompt dispatch with security and auditing.

---

## Verification
Run the offline verification harness:
```powershell
mvn test-compile exec:java -pl phase08-ex01-mcp-server
```

Or run standard Maven tests:
```powershell
mvn test -pl phase08-ex01-mcp-server
```

A verified reference implementation is available in `docs/golden/phase08-ex01-golden.md`.
