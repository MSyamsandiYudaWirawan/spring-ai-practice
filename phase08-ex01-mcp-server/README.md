# Phase 08 Exercise 01: Model Context Protocol (MCP) Server Architecture & Protocol Specifications

## Focus & Learning Objectives
This exercise drills the official Model Context Protocol (MCP) Java SDK (`io.modelcontextprotocol.sdk:mcp-core:2.0.0`) and Spring AI's native MCP integration (`org.springframework.ai:spring-ai-mcp:2.0.1`):
- **Tool Definitions & JSON Schema:** Building standard `Tool` specifications with JSON Schema properties, required fields, and schema titles.
- **SyncToolSpecification Handlers:** Implementing execution handlers, returning `CallToolResult` with `TextContent`, handling exceptions gracefully with error flags, and adapting Spring AI `ToolCallback` via `McpToolUtils`.
- **MCP Resource Specifications:** Exposing static text, dynamic text, and binary/blob resources with `Resource.builder()` and `ReadResourceResult`.
- **MCP Prompt Specifications:** Defining prompts with zero arguments, multi-argument templates, and role enforcement (`GetPromptResult`, `PromptMessage`).
- **Server Capabilities & Filtering:** Declaring protocol capabilities (`ServerCapabilities.builder()`) and filtering tools by allowlists or naming prefixes.

---

## Repetitive Muscle Memory Structure (15 Scenarios)

### Topic 1: MCP Tool Definition & JSON Input Schema (Scenarios 1 – 3)
1. **Scenario 01: Primitive-Typed Tool Definition** — Build a `Tool` with primitive property types (`string`, `integer`, etc.) mapped to JSON schema.
2. **Scenario 02: Structured Tool Definition with Pre-built Schema Maps** — Build a `Tool` with arbitrary structured schema property definitions.
3. **Scenario 03: Title-Enriched Tool Definition** — Build a `Tool` schema enriched with title and audit metadata.

### Topic 2: SyncToolSpecification Handlers (Scenarios 4 – 6)
4. **Scenario 04: Standard SyncToolSpecification Handler** — Wrap tool execution into `CallToolResult` with `TextContent`.
5. **Scenario 05: Resilient SyncToolSpecification with Error Trapping** — Trap execution errors into safe error results (`isError: true`).
6. **Scenario 06: Spring AI ToolCallback to MCP SyncToolSpecification Adapter** — Adapt Spring AI `ToolCallback` into MCP `SyncToolSpecification` using `McpToolUtils`.

### Topic 3: MCP Resource Specifications (Scenarios 7 – 9)
7. **Scenario 07: Static Text Resource Specification** — Serve static text resources with MIME types.
8. **Scenario 08: Dynamic Text Resource Specification** — Serve dynamic runtime text content via functional providers.
9. **Scenario 09: Binary / Blob Resource Specification** — Serve base64 binary content via `BlobResourceContents`.

### Topic 4: MCP Prompt Template Specifications (Scenarios 10 – 12)
10. **Scenario 10: Simple Zero-Argument Prompt Specification** — Build a static zero-argument prompt specification.
11. **Scenario 11: Parameterized Multi-Argument Prompt Specification** — Build a parameterized prompt template accepting argument maps.
12. **Scenario 12: Role-Enforced Prompt Specification** — Build a prompt specification with explicit role assignment (`Role.ASSISTANT` or `Role.USER`).

### Topic 5: Server Capabilities & Protocol Filtering (Scenarios 13 – 15)
13. **Scenario 13: Server Capabilities Declaration** — Declare negotiated protocol features (`tools`, `resources`, `prompts`, `logging`) via `ServerCapabilities.builder()`.
14. **Scenario 14: Tool Filtering by Allowlist** — Filter server tool collections against an allowlist of tool names.
15. **Scenario 15: Tool Filtering by Name Prefix** — Filter and route server tools based on name prefixes.

---

## Verification
Run standard Maven tests:
```powershell
mvn test -pl phase08-ex01-mcp-server
```

A verified reference implementation is available in `docs/golden/phase08-ex01-golden.md`.
