# Spring AI 2.0.1 — CallAdvisors & Guardrail Pipelines

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 04 (`phase04-guardrail-advisor`) & Runtime Interception

---

## 1. `CallAdvisor` & Guardrail Pipeline

Spring AI 2.0.1 introduces the `CallAdvisor` interface for intercepting chat calls in an onion-model pipeline:

```java
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

public class SecurityKeywordAdvisor implements CallAdvisor {
    private final List<String> blockedKeywords;

    public SecurityKeywordAdvisor(List<String> blockedKeywords) {
        this.blockedKeywords = blockedKeywords;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. Pre-call inspection / mutation:
        for (Message msg : request.prompt().getInstructions()) {
            if (containsBlocked(msg.getText())) {
                throw new PromptSecurityException("Blocked keyword detected: " + msg.getText());
            }
        }

        // 2. Context enrichment (e.g. correlationId):
        ChatClientRequest mutated = request.mutate()
                .context("correlationId", UUID.randomUUID().toString())
                .build();

        // 3. Chain dispatch:
        ChatClientResponse response = chain.nextCall(mutated);

        // 4. Post-call inspection / response mutation (e.g. PII masking, usage counting):
        Usage usage = response.chatResponse().getMetadata().getUsage();
        accumulateTokens(usage);

        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // runs earliest in pre-call, latest in post-call
    }
}
```

### Advisor Ordering (The Onion Model)
Advisors execute based on `getOrder()`:
- **Lower order values** (e.g. `Ordered.HIGHEST_PRECEDENCE` or `10`) execute **first** on the way in (`PRE_CALL`), wrapping around subsequent advisors.
- On the way out (`POST_CALL`), they execute in **reverse order** (onion unpeeling: higher order advisors complete post-call before lower order advisors).
- Example production chain ordering:
  1. `RuntimeContextAdvisor` (`order = 10`) — tags execution mode (`DRY_RUN` vs `LIVE`), enriches prompt.
  2. `SlidingWindowTruncationAdvisor` (`order = 20`) — keeps pinned `SystemMessage` at index 0, prunes old conversational history.
  3. `SlidingWindowRateLimiterAdvisor` (`order = 30`) — short-circuits runaway request frequency before downstream token expenditure.
  4. `TraceContextPropagationAdvisor` (`order = 40`) — injects and propagates W3C `traceparent` headers.
  5. `DynamicTokenQuotaAdvisor` (`order = 50`) — pre-allocates token estimate, refunds/rolls back on downstream failures.
  6. `GroundingValidationAdvisor` (`order = 60`) — verifies extracted entity tokens against an authorized catalog.
  7. `SchemaSelfHealingAdvisor` (`order = 70`) — validates structured output schema, triggers closed-loop one-shot repair retries.
  8. `ModelFailoverAdvisor` (`order = 80`) — closest to model; catches LLM timeouts/503s and routes to secondary fallback `ChatModel`.

---

## 2. Advanced `CallAdvisor` Patterns & Engineering Discoveries

### 2.1 Client-Side Advisor Parameter Injection
On `ChatClientRequestSpec`, there is **no** `.context(Map)` method. Instead, callers pass context parameters to advisors using the `.advisors(Consumer<AdvisorSpec>)` hook:

```java
// Client invocation:
ChatClientResponse response = client.prompt()
        .user("Restart container")
        .advisors(a -> a.param("executionMode", "DRY_RUN")
                        .param("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"))
        .call()
        .chatClientResponse();

// How it works internally:
// Spring AI's DefaultChatClientUtils.toChatClientRequest(...) takes spec.getAdvisorParams() 
// and copies them into ChatClientRequest.builder().context(advisorParams).

// Inside CallAdvisor.adviseCall:
Object mode = request.context().get("executionMode");
```

To read response-level context after execution:
```java
ChatClientResponse response = client.prompt().call().chatClientResponse();
Object dryRun = response.context().get("audit.dryRun");
```

---

### 2.2 The Single-Pass Chain Rule & Closed-Loop Retries via `chain.copy(this)`
In Spring AI 2.0.1, `CallAdvisorChain` (implemented by `DefaultAroundAdvisorChain`) manages an internal `Deque<CallAdvisor>`. Each call to `chain.nextCall(...)` pops an advisor from the deque.

> [!WARNING]
> Calling `chain.nextCall(...)` a second time on the **same** chain instance will encounter an empty deque and throw:
> `IllegalStateException: No CallAdvisors available to execute`

To perform an automated retry loop from within an advisor (e.g. schema repair, self-healing, retry on transient failure), use **`chain.copy(this)`**:

```java
public class SchemaSelfHealingAdvisor implements CallAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse initial = chain.nextCall(request);
        String text = extractText(initial);

        if (isValidSchema(text)) {
            return initial.mutate().context("schemaHealed", false).build();
        }

        // Output invalid: construct repair prompt with feedback
        List<Message> repairMessages = new ArrayList<>(request.prompt().getInstructions());
        repairMessages.add(new AssistantMessage(text));
        repairMessages.add(new UserMessage("SCHEMA_REPAIR_NOTICE: Missing 'status' field. Re-generate valid JSON."));

        ChatClientRequest retryReq = request.mutate().prompt(new Prompt(repairMessages)).build();

        // KEY DISCOVERY: chain.copy(this) clones the chain for advisors following 'this'
        ChatClientResponse retryResponse = chain.copy(this).nextCall(retryReq);
        String retryText = extractText(retryResponse);

        if (isValidSchema(retryText)) {
            return retryResponse.mutate().context("schemaHealed", true).build();
        }

        throw new SchemaValidationException("Schema validation failed: Assistant output failed repair retry");
    }

    @Override
    public int getOrder() { return 70; }
}
```

`chain.copy(this)` looks up `this` in the advisor list, takes all advisors after `this`, and returns a fresh `CallAdvisorChain` instance ready for the retry call.

---

### 2.3 Transactional Pre-Allocation & Failure Rollback
When managing strictly budgeted resources (token quotas, rate limits, credit pools):
1. **Pre-call reservation:** Estimate tokens before dispatch. If remaining budget is insufficient, throw `BudgetExceededException` immediately before invoking downstream advisors or the model.
2. **Post-call reconciliation:** Deduct actual token usage reported by `response.chatResponse().getMetadata().getUsage()`.
3. **Rollback on failure:** If `chain.nextCall(request)` throws any `RuntimeException`, catch it, refund the estimated tokens back to the quota, and rethrow!

```java
public class DynamicTokenQuotaAdvisor implements CallAdvisor {
    private int remainingQuota;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int estimated = estimateTokens(request.prompt());

        synchronized (this) {
            if (remainingQuota < estimated) {
                throw new BudgetExceededException("Insufficient token quota: required " + estimated + ", remaining " + remainingQuota);
            }
            remainingQuota -= estimated; // Step 1: Pre-allocate
        }

        try {
            ChatClientResponse response = chain.nextCall(request);
            int actual = response.chatResponse().getMetadata().getUsage().getTotalTokens();
            synchronized (this) {
                remainingQuota = remainingQuota + estimated - actual; // Step 2: Reconcile
            }
            return response.mutate().context("remainingQuota", remainingQuota).build();
        } catch (RuntimeException ex) {
            synchronized (this) {
                remainingQuota += estimated; // Step 3: Transactional Rollback
            }
            throw ex;
        }
    }

    @Override
    public int getOrder() { return 50; }
}
```

---

### 2.4 Resilient Failover Routing in the Onion Architecture
To achieve high availability across LLM providers, place `ModelFailoverAdvisor` **closest to the model** (high order value, e.g. `80`):

```java
public class ModelFailoverAdvisor implements CallAdvisor {
    private final ChatModel fallbackModel;

    public ModelFailoverAdvisor(ChatModel fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        try {
            ChatClientResponse response = chain.nextCall(request);
            return response.mutate().context("failoverTriggered", false).build();
        } catch (Exception ex) {
            // Primary model failed (e.g. 503, timeout, connection reset)
            ChatResponse fallbackResponse = fallbackModel.call(request.prompt());
            return ChatClientResponse.builder()
                    .chatResponse(fallbackResponse)
                    .context(request.context())
                    .context("failoverTriggered", true)
                    .context("primaryError", ex.getMessage())
                    .build();
        }
    }

    @Override
    public int getOrder() { return 80; } // Innermost advisor
}
```

**Why Order Matters:**
Because `ModelFailoverAdvisor` (Order 80) is wrapped inside `GroundingValidationAdvisor` (Order 60) and `DynamicTokenQuotaAdvisor` (Order 50):
1. When the primary model fails, Order 80 catches the exception and calls `fallbackModel.call(...)`.
2. Order 80 returns the fallback `ChatClientResponse` up the call stack.
3. Order 60 (`GroundingValidationAdvisor`) and Order 50 (`DynamicTokenQuotaAdvisor`) execute their post-call logic on the **fallback response**, ensuring grounding verification and token accounting apply equally to failover traffic!
