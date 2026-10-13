# Spring AI 2.0.1 — Enterprise Architecture & Saga Loops

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 09 (`phase09-enterprise-resilience-mesh`) & Autonomous Agentic Loops

---

## 1. In-Memory Saga Loops, State Machines & Compensations

### 1.1 Autonomous Agent State Machine (6-Phase Saga Loop)
Autonomous agents running iterative code optimizations or diagnostic fixes must follow strict finite state machine (FSM) semantics:

```
        +--------------+
        |     IDLE     |
        +-------+------+
                | (start)
                v
        +-------+------+ <---------------------------------+
+-----> |    DECIDE    |                                   |
|       +-------+------+                                   |
|               |                                          |
|               v                                          |
|       +-------+------+                                   |
|       |    APPLY     |                                   |
|       +---+------+---+                                   |
|           |      |                                       |
|  (fail)   |      | (success)                             |
|           |      v                                       |
|           |   +--+-----------+                           |
|           |   |   MEASURE    |                           |
|           |   +--+-----------+                           |
|           |      |                                       |
|           |      v                                       |
|           |   +--+-----------+                           |
|           |   |    JUDGE     |                           |
|           |   +--+-----+-----+                           |
|           |      |     |                                 |
|           | (rej)|     | (keep)                          |
|           v      v     |                                 |
|       +---+------+---+ |                                 |
|       |  COMPENSATE  | | (commit sha)                    |
|       +-------+------+ |                                 |
|               |        +---------------------------------+
+---------------+
                | (target met or breach)
                v
        +-------+------+
        |    FINISH    |
        +--------------+
```

### 1.2 Guaranteed Finally Compensations
When applying risky workspace mutations (e.g. refactoring code, altering pool sizes), unhandled exceptions, JVM OOMs, or timeouts must NEVER leave the workspace dirty. Wrap executions in deterministic finally blocks:

```java
public static void executeWithGuaranteedCompensation(
        SandboxedWorkspace workspace,
        String lastKeptSha,
        Runnable riskyMutation
) {
    boolean completedCleanly = false;
    try {
        riskyMutation.run();
        completedCleanly = true;
    } finally {
        if (!completedCleanly) {
            // Roll back all modified files and head pointer to last known good SHA
            workspace.revertTo(lastKeptSha);
        }
    }
}
```

### 1.3 Multi-Stage Diagnostic Failure Triage
Failure handling in agent loops must follow an escalating hierarchy:
1. **Stage 1 (`STAGE_1_TOOL_RETRY`)**: Ephemeral tool timeout or connection reset $\rightarrow$ retry with exponential backoff (no workspace mutation).
2. **Stage 2 (`STAGE_2_SCHEMA_REPAIR`)**: JSON parse error or malformed schema $\rightarrow$ 1-shot repair prompt sending parser error back to LLM.
3. **Stage 3 (`STAGE_3_COMPENSATION_REVERT`)**: Compilation error or test suite regression $\rightarrow$ roll back workspace to `lastKeptSha` and prompt LLM with build failure trace.
4. **Stage 4 (`STAGE_4_GUARDRAIL_TERMINATE`)**: Token budget ceiling, iteration cap, or cyclical oscillation detected $\rightarrow$ trip circuit breaker, terminate loop, escalate to human.

---

## 2. Spring Boot Assembly, Dynamic Tools & Enterprise Resilience

### 2.1 Package Relocations in Spring Boot 4.1.1
- **Actuator Health & Contributors**:
  Moved from `org.springframework.boot.actuate.health.*` to:
  ```java
  import org.springframework.boot.health.contributor.Health;
  import org.springframework.boot.health.contributor.HealthIndicator;
  import org.springframework.boot.health.contributor.Status;
  ```
- **Spring AI Tool Discovery**:
  Located in `org.springframework.ai.support.ToolCallbacks`:
  ```java
  import org.springframework.ai.support.ToolCallbacks;
  import org.springframework.ai.tool.ToolCallback;
  import org.springframework.ai.tool.annotation.Tool;

  // Discovers all @Tool methods on Spring bean:
  ToolCallback[] callbacks = ToolCallbacks.from(myServiceBean);
  ```

### 2.2 Spring AI 2.0.1 `CallAdvisor` Contracts
`CallAdvisor` directly extends `org.springframework.core.Ordered`. All advisors participate in standard Spring ordering:

```java
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public class TracingCorrelationAdvisor implements CallAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String traceId = (String) request.context().getOrDefault("traceId", "trace-" + UUID.randomUUID());

        // Context enrichment via immutable mutate():
        ChatClientRequest mutated = request.mutate()
                .context("X-Trace-Id", traceId)
                .build();

        return chain.nextCall(mutated);
    }

    @Override
    public int getOrder() {
        return 0; // Highest precedence
    }

    @Override
    public String getName() {
        return "TracingCorrelationAdvisor";
    }
}
```

### 2.3 Enterprise Resilience Mesh Patterns

#### 1. Dynamic Tenant Routing
Partitioning ChatClient instances by tenant enables distinct rate limits, API keys, and model parameters:

```java
public class TenantRoutingChatClientFactory {
    public static ChatClient resolveChatClient(Map<String, ChatClient> tenantClients) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || !tenantClients.containsKey(tenantId)) {
            throw new TenantNotFoundException("No client configured for tenant: " + tenantId);
        }
        return tenantClients.get(tenantId);
    }
}
```

#### 2. Circuit Breaker Fallback Model Switching
Protect against upstream LLM provider 503/429 outages:
- Track consecutive failures on primary high-capability model (e.g. Claude 3.7 Sonnet).
- When failures $\ge 3$, trip circuit state from `CLOSED` to `OPEN`.
- While `OPEN`, immediately dispatch to secondary fallback model (e.g. Claude 3.5 Haiku) without incurring primary latency penalties.

#### 3. Idempotency Key Deduplication
Avoid duplicate billing and redundant reasoning loops on webhook retries:
- Hash: $\text{SHA256}(\text{tenantId} + ":" + \text{incidentId} + ":" + \text{payload})$.
- Concurrency Lock: Reject in-flight duplicate requests with `ConcurrentDuplicateRequestException`.
- Completed Cache: Return pre-computed cached `EnterpriseTriageResponse` instantly on replay.

#### 4. SmartLifecycle Request Draining
Kubernetes rolling restarts must not terminate active agent loops mid-flight:
- Implement `SmartLifecycle`.
- On `stop(Runnable callback)`: set `isRunning = false` (reject new inbound requests with `ServiceDrainingException`).
- Poll active request counter until $0$ (up to `maxWaitMs`), then execute callback to allow clean JVM shutdown.
