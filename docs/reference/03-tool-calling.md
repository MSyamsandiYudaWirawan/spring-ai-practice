# Spring AI 2.0.1 — Tool Calling & Function Callbacks

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 03 (`phase03-tool-calling`) & Agentic Operations

---

## 1. Tool Calling (`@Tool`, `@ToolParam`, and `ToolCallbacks`)

In Spring AI 2.0.1, tool calling uses reflection annotations and the `ToolCallbacks` utility:

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

public class CloudOpsTools {

    @Tool(description = "Restart a specific pod in a Kubernetes namespace.")
    public boolean restartPod(
            @ToolParam(description = "Target Kubernetes namespace") String namespace,
            @ToolParam(description = "Target pod name to restart") String podName
    ) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        return true;
    }

    @Tool(description = "Execute a synthetic health probe on a system component.")
    public ToolEnvelope<HealthReport> executeHealthProbe(
            @ToolParam(description = "Component name: api-gateway, auth-service, database, cache") String component
    ) {
        if (!ALLOWED_COMPONENTS.contains(component.toLowerCase())) {
            return ToolEnvelope.fail("Unknown component: " + component);
        }
        return ToolEnvelope.ok(new HealthReport(component, "HEALTHY", 12L));
    }
}
```

---

## 2. Tool Discovery & `ChatClient` Registration

```java
// 1. Reflection extraction of ToolCallback[]:
ToolCallback[] callbacks = ToolCallbacks.from(cloudOpsToolsInstance);

// 2. Direct registration on ChatClient builder:
ChatClient client = ChatClient.builder(chatModel)
        .defaultTools(cloudOpsToolsInstance)
        .build();

// 3. Or per-call tool attachment:
client.prompt()
        .user("Restart the auth service")
        .tools(cloudOpsToolsInstance)
        .call()
        .content();
```

---

## 3. Tool Invariants & Best Practices

- **Exception Isolation:** Tools should wrap unexpected or business rejections into typed envelopes (e.g. `ToolEnvelope.fail("...")`) rather than throwing raw unhandled exceptions that abort the entire agentic loop.
- **Fail-Fast Invariants:** Validate inputs (`@ToolParam`) immediately at the top of the method.
- **Rate-Limiting & Quota Guards:** Track invocation counters per turn/session to prevent runaway infinite tool execution loops.
- **Trajectory Audits:** Always log tool invocations (name, arguments, success/failure, timestamp) for traceability.
