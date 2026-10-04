# Spring AI 2.0.1 — API Reference & Accessor Cheatsheet

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Java Version:** Java 21+ (`maven.compiler.parameters=true`)

This document captures the verified signatures, accessors, and contracts in `spring-ai-client-chat-2.0.1.jar` and `spring-ai-model-2.0.1.jar` so we never need to reverse-engineer them again.

---

## 1. Maven Coordinates

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>2.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>4.1.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Key dependencies:
- `org.springframework.ai:spring-ai-client-chat`: `ChatClient`, `PromptUserSpec`, `CallResponseSpec`, Advisors
- `org.springframework.ai:spring-ai-model`: `ChatModel`, `Prompt`, `ChatResponse`, `Generation`, `ChatOptions`, `Usage`
- `org.springframework.boot:spring-boot-starter-test`: Testing stack (JUnit 5, AssertJ, Mockito)

---

## 2. `ChatClient` Creation & Configuration

`ChatClient` is an interface with fluent builders:

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;

// 1. Minimal creation around any ChatModel:
ChatClient client = ChatClient.create(chatModel);

// 2. Full builder with defaults:
ChatClient client = ChatClient.builder(chatModel)
        .defaultSystem("You are a TigerStyle performance diagnostician.")
        // NOTE: defaultOptions accepts ChatOptions.Builder<?> (do NOT call .build())
        .defaultOptions(ChatOptions.builder().temperature(0.2).topP(0.9))
        .defaultAdvisors(...)
        .defaultTools(...)
        .build();
```

### Key `ChatClient.Builder` Methods
| Method | Argument | Notes |
|---|---|---|
| `defaultSystem(String)` | Plain string | Static default system prompt |
| `defaultSystem(Consumer<PromptSystemSpec>)` | Spec lambda | Parameterized system prompt template |
| `defaultOptions(ChatOptions$Builder)` | `ChatOptions.builder()` | Default options passed as a **Builder**, not instance |
| `defaultTools(Object...)` | Tool bean instances | Registers `@Tool` annotated objects |
| `defaultAdvisors(Advisor...)` | Advisor instances | Around-call advisors (token tracking, logging) |
| `build()` | None | Builds the immutable `ChatClient` |

---

## 3. `ChatClient` Prompt & Execution Flow

```java
String text = client.prompt()
        .system("System instructions here")
        .user(u -> u.text("Diagnosing {app} with p95={p95}ms")
                    .param("app", "petclinic")
                    .param("p95", 250))
        .options(ChatOptions.builder().temperature(0.1)) // overrides default
        .call()
        .content();
```

### Prompt Specification (`ChatClientRequestSpec`)
- `.system(String text)`: Sets the system message role.
- `.system(Consumer<PromptSystemSpec> spec)`: Parameterized system template (`spec.text("...").param(...)`).
- `.user(String text)`: Simple user message string.
- `.user(Consumer<PromptUserSpec> spec)`: Parameterized user template.
  - `u.text("Template with {key}")`
  - `u.param("key", value)`
  - `u.params(Map<String, Object>)`
- `.options(ChatOptions$Builder builder)`: Overrides model options for this specific call.
- `.tools(Object... toolBeans)`: Attaches tool beans for this call.
- `.advisors(Advisor... advisors)`: Adds per-call advisors.
- `.call()`: Transitions to `CallResponseSpec` (blocking).
- `.stream()`: Transitions to `StreamResponseSpec` (reactive Flux).

### Execution Outcomes (`CallResponseSpec`)
| Method | Return Type | Purpose |
|---|---|---|
| `.content()` | `String` | Returns raw model output text (throws on empty/error) |
| `.chatResponse()` | `ChatResponse` | Full response entity including tokens, finish reasons, metadata |
| `.entity(Class<T>)` | `T` | Automatically deserializes JSON output to record/POJO class |
| `.entity(ParameterizedTypeReference<T>)` | `T` | Deserializes generic collections (`List<Item>`) |
| `.responseEntity(Class<T>)` | `ResponseEntity<ChatResponse, T>` | Combined `ChatResponse` + parsed DTO |

---

## 4. `ChatResponse`, `Prompt`, and Accessor Chains

### Response Unpacking Chain (Diagnostician §5.4 verified)
```java
ChatResponse response = client.prompt()...call().chatResponse();

// 1. Output Text:
String text = response.getResult().getOutput().getText(); 
// or:
String text = response.getResult().getOutput().getContent();

// 2. Token Usage:
long tokensIn = 0;
long tokensOut = 0;
if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
    Usage usage = response.getMetadata().getUsage();
    if (usage.getPromptTokens() != null) {
        tokensIn = usage.getPromptTokens().longValue();
    }
    if (usage.getCompletionTokens() != null) {
        tokensOut = usage.getCompletionTokens().longValue();
    }
}
```

### Inspecting Incoming `Prompt` inside `ChatModel`
```java
public ChatResponse call(Prompt prompt) {
    // 1. Messages:
    List<Message> instructions = prompt.getInstructions();
    SystemMessage sysMsg = prompt.getSystemMessage(); // null if not sent
    UserMessage userMsg = prompt.getUserMessage();     // null if not sent
    
    // 2. Options:
    ChatOptions options = prompt.getOptions();
    Double temp = options != null ? options.getTemperature() : null;
    Double topP = options != null ? options.getTopP() : null;
    
    // 3. Raw Contents:
    String contents = prompt.getContents();
}
```

---

## 5. Test Double: Constructing Fake `ChatResponse`
When building offline stubs like `FakeChatModel`:
```java
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

// Create metadata with simulated usage
DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();

// Create generation with assistant message
Generation generation = new Generation(new AssistantMessage(responseText));

// Wrap into ChatResponse
ChatResponse response = new ChatResponse(List.of(generation), metadata);
```

---

## 6. Structured Outputs & `BeanOutputConverter`

In Spring AI 2.0.1 / Spring Boot 4.1.1, `BeanOutputConverter` lives in package `org.springframework.ai.converter`:

```java
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

// 1. Single Record / Class conversion:
BeanOutputConverter<VulnerabilityAssessment> converter = 
        new BeanOutputConverter<>(VulnerabilityAssessment.class);

// Schema format string to inject into prompt:
String formatInstructions = converter.getFormat();

// Deserialization:
VulnerabilityAssessment record = converter.convert(rawJson);

// 2. Generic Collection conversion:
BeanOutputConverter<List<PatchPlan>> listConverter = 
        new BeanOutputConverter<>(new ParameterizedTypeReference<List<PatchPlan>>() {});
List<PatchPlan> plans = listConverter.convert(rawJsonArray);

// 3. Direct Fluent ChatClient entity invocation:
VulnerabilityAssessment direct = client.prompt()
        .user(advisoryText)
        .call()
        .entity(VulnerabilityAssessment.class);
```

---

## 7. Defensive Markdown Fence Stripping & Sanitization

Real LLMs frequently return markdown code blocks (```` ```json ... ``` ````) and conversational pleasantries around JSON. A resilient sanitizer extracts the pure JSON substring:

```java
public static String sanitizeJsonPayload(String text) {
    if (text == null) return null;
    int firstBrace = text.indexOf('{');
    int lastBrace = text.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
        return text.substring(firstBrace, lastBrace + 1).trim();
    }
    int firstBracket = text.indexOf('[');
    int lastBracket = text.lastIndexOf(']');
    if (firstBracket >= 0 && lastBracket > firstBracket) {
        return text.substring(firstBracket, lastBracket + 1).trim();
    }
    return text.trim();
}
```

---

## 8. The Saga DECIDE One-Shot Schema Repair Protocol

From `step9-agent-loop-spec.md` (§10.4 and §10.21): LLMs are permitted at most **1 retry** on schema failure, quoting the error back to the model before terminating with `IllegalStateException` / marking `WASTED`:

```java
int maxRetries = 1;
int attempts = 0;
Exception lastException = null;
String promptText = initialPrompt;

while (attempts <= maxRetries) {
    attempts++;
    String raw = client.prompt().user(promptText).call().content();
    try {
        return converter.convert(sanitizeJsonPayload(raw));
    } catch (Exception e) {
        lastException = e;
        promptText = "Previous output failed schema validation: " + e.getMessage()
                + ". Please output strictly valid JSON conforming to schema:";
    }
}
throw new IllegalStateException("Schema repair exceeded maximum attempts (" + maxRetries + ")", lastException);
```

---

## 9. Tool Calling (`@Tool`, `@ToolParam`, and `ToolCallbacks`)

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

### Tool Discovery & `ChatClient` Registration
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

### Tool Invariants & Best Practices
- **Exception Isolation:** Tools should wrap unexpected or business rejections into typed envelopes (e.g. `ToolEnvelope.fail("...")`) rather than throwing raw unhandled exceptions that abort the entire agentic loop.
- **Fail-Fast Invariants:** Validate inputs (`@ToolParam`) immediately at the top of the method.
- **Rate-Limiting & Quota Guards:** Track invocation counters per turn/session to prevent runaway infinite tool execution loops.
- **Trajectory Audits:** Always log tool invocations (name, arguments, success/failure, timestamp) for traceability.

---

## 10. `CallAdvisor` & Guardrail Pipeline

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

## 11. Advanced `CallAdvisor` Patterns & Engineering Discoveries

### 11.1 Client-Side Advisor Parameter Injection
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

### 11.2 The Single-Pass Chain Rule & Closed-Loop Retries via `chain.copy(this)`
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

### 11.3 Transactional Pre-Allocation & Failure Rollback
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

### 11.4 Resilient Failover Routing in the Onion Architecture
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

---

## 12. Deterministic Testing & Offline Harness Patterns

Testing LLM-backed applications presents unique architectural challenges: live model calls are slow, nondeterministic, cost money, and fail unexpectedly in CI.

### 12.1 The Architectural Seam Problem: Direct Mocking vs Port Interfaces
Directly mocking Spring AI's fluent `ChatClient` chain requires mocking 6+ internal framework types:
```java
// Anti-pattern: mocking fluent builder chains in tests
when(chatClient.prompt()).thenReturn(requestSpec);
when(requestSpec.system(anyString())).thenReturn(requestSpec);
when(requestSpec.user(anyString())).thenReturn(requestSpec);
when(requestSpec.tools(any())).thenReturn(requestSpec);
when(requestSpec.call()).thenReturn(responseSpec);
when(responseSpec.chatResponse()).thenReturn(chatResponse);
when(chatResponse.getResult()).thenReturn(generation);
// Brittle, couples tests to internal builder structure, breaks on minor upgrades!
```

**The Solution: The `ChatPort` Hexagonal Seam**
Introduce a simple, domain-focused interface for agentic loops:
```java
public interface ChatPort {
    ChatResult chat(String system, String user, List<Object> toolBeans);
}
```
- In **production**: `SpringAiChatPort` adapts `ChatPort` to Spring AI's `ChatClient`:
  ```java
  public class SpringAiChatPort implements ChatPort {
      private final ChatClient chatClient;
      public SpringAiChatPort(ChatClient chatClient) { this.chatClient = chatClient; }

      @Override
      public ChatResult chat(String system, String user, List<Object> toolBeans) {
          var promptSpec = chatClient.prompt().system(system).user(user);
          if (toolBeans != null && !toolBeans.isEmpty()) {
              promptSpec.tools(toolBeans.toArray());
          }
          ChatResponse resp = promptSpec.call().chatResponse();
          String text = resp.getResult() != null && resp.getResult().getOutput() != null
                  ? resp.getResult().getOutput().getText() : "";
          Usage usage = resp.getMetadata() != null ? resp.getMetadata().getUsage() : null;
          long in = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
          long out = usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
          return new ChatResult(text != null ? text : "", in, out);
      }
  }
  ```
- In **tests**: Use deterministic in-memory implementations (`ScriptedChatPort`, `PredicateRoutingChatPort`, `FaultInjectingChatPort`) executing in 0 milliseconds with 0 live API calls.

### 12.2 Scripted FIFO & Pattern-Matching Test Doubles
```java
// 1. FIFO Scripted Queue:
ScriptedChatPort fakePort = new ScriptedChatPort()
    .enqueue("{\"action\":\"INVESTIGATE\",\"service\":\"cart\"}", 120, 45)
    .enqueue("{\"action\":\"REMEDIATE\",\"action_type\":\"SCALE_UP\"}", 150, 60);

// 2. Dynamic Predicate Routing:
PredicateRoutingChatPort routerPort = new PredicateRoutingChatPort()
    .when(prompt -> prompt.contains("METRICS"), new ChatResult("Metrics healthy", 50, 20))
    .when(prompt -> prompt.contains("LOGS"), new ChatResult("OutOfMemoryError detected", 80, 40))
    .setDefaultResult(new ChatResult("No anomaly", 10, 5));
```

### 12.3 Outgoing Prompt Spy (`PromptCaptor`)
To assert that prompt templates, few-shots, and system rules are correctly composed without invoking a real LLM:
```java
public class PromptCapturingChatPort implements ChatPort {
    private final ChatPort delegate;
    private final List<PromptCapture> captures = new CopyOnWriteArrayList<>();

    @Override
    public ChatResult chat(String system, String user, List<Object> toolBeans) {
        captures.add(new PromptCapture(system, user, toolBeans == null ? List.of() : List.copyOf(toolBeans)));
        return delegate.chat(system, user, toolBeans);
    }

    public List<PromptCapture> getCaptures() { return List.copyOf(captures); }
}
```

### 12.4 Controllable Virtual Time (`TestClock`)
Never use `Thread.sleep(...)` in agentic tests (causes slow, flaky CI runs). Inject a controllable clock fixture:
```java
public class TestClock {
    private Instant now;
    public TestClock(Instant initial) { this.now = initial; }
    public Instant instant() { return now; }
    public void advance(Duration duration) { this.now = this.now.plus(duration); }
    public void advanceMillis(long ms) { this.now = this.now.plusMillis(ms); }
}
```

### 12.5 Deterministic Reactive Flux Testing
For streaming scenarios (`chatClient.prompt().stream()`), simulate token chunks deterministically:
```java
public class InMemoryStreamingChatPort implements StreamingChatPort {
    private final List<String> chunks;
    public InMemoryStreamingChatPort(List<String> chunks) { this.chunks = chunks; }

    @Override
    public Flux<String> stream(String system, String user) {
        return Flux.fromIterable(chunks);
    }
}
// In test:
StepVerifier.create(streamingPort.stream(sys, user))
    .expectNext("Investigating", " CPU", " spike", " on node-4")
    .verifyComplete();
```

---

## 13. Evaluators & LLM-as-a-Judge Architecture

Evaluating LLM applications requires a dual-track strategy:
1. **Deterministic Mathematical Gates:** Evaluating hard metrics (latency, RPS, error rates, token usage) against noise floor baselines.
2. **Qualitative & Semantic Evaluators:** Evaluating relevance, factual groundedness, query drift, safety, and rubric alignment using Spring AI's `Evaluator` interfaces and LLM-as-a-judge techniques.

### 13.1 Maven Coordinates & Module Breakdown
Evaluation types in Spring AI 2.0.1 are split across two core jars:
```xml
<!-- Core evaluation contracts: Evaluator, EvaluationRequest, EvaluationResponse -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-commons</artifactId>
</dependency>

<!-- Chat-specific evaluators: RelevancyEvaluator, FactCheckingEvaluator -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-client-chat</artifactId>
</dependency>
```

### 13.2 Core Evaluation Interfaces & Data Contracts

#### 1. The `Evaluator` Interface
Located in package `org.springframework.ai.evaluation`:
```java
public interface Evaluator {
    EvaluationResponse evaluate(EvaluationRequest evaluationRequest);

    default String doGetSupportingData(EvaluationRequest evaluationRequest) {
        // Concatenates documents in evaluationRequest.getDataList()
    }
}
```

#### 2. `EvaluationRequest` Contract
```java
package org.springframework.ai.evaluation;

public class EvaluationRequest {
    public EvaluationRequest(String userText, String responseContent);
    public EvaluationRequest(List<Document> dataList, String responseContent);
    public EvaluationRequest(String userText, List<Document> dataList, String responseContent);

    public String getUserText();
    public List<Document> getDataList();
    public String getResponseContent();
}
```
> [!IMPORTANT]
> `EvaluationRequest` exposes **only** `.getUserText()`, `.getDataList()`, and `.getResponseContent()`. It does **not** have a `getMetadata()` method. Pass reference context or retrieved chunks via `dataList` (`List<Document>`).

#### 3. `EvaluationResponse` Contract
```java
package org.springframework.ai.evaluation;

public class EvaluationResponse {
    public EvaluationResponse(boolean pass, float score, String feedback, Map<String, Object> metadata);
    public EvaluationResponse(boolean pass, String feedback, Map<String, Object> metadata);

    public boolean isPass();
    public float getScore();
    public String getFeedback();
    public Map<String, Object> getMetadata();
}
```

---

### 13.3 Built-In Spring AI Evaluators

#### 1. `RelevancyEvaluator`
Located in `org.springframework.ai.chat.evaluation.RelevancyEvaluator`. Evaluates whether the generated response directly aligns with the query and provided context.

```java
// Creation via Builder:
RelevancyEvaluator evaluator = RelevancyEvaluator.builder()
        .chatClientBuilder(chatClientBuilder)
        .build();

// Execution:
EvaluationRequest request = new EvaluationRequest(query, contextDocs, response);
EvaluationResponse response = evaluator.evaluate(request);

boolean passed = response.isPass(); // true if model answers "YES"
float score = response.getScore();  // 1.0f on YES, 0.0f on NO
```

**Internal Framework Prompt Template:**
```text
Your task is to evaluate if the response for the query
is in line with the context information provided.

You have two options to answer. Either YES or NO.

Answer YES, if the response for the query
is in line with context information otherwise NO.

Query:
{query}

Response:
{response}

Context:
{context}

Answer:
```
**Parsing Rule:** Spring AI strips the output and checks `.equalsIgnoreCase("yes")`. If true, `isPass = true, score = 1.0f`; otherwise `isPass = false, score = 0.0f`.

#### 2. `FactCheckingEvaluator`
Located in `org.springframework.ai.chat.evaluation.FactCheckingEvaluator`. Evaluates whether a claim (in response) is factually supported by context documents.

```java
// Creation via Builder:
FactCheckingEvaluator evaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();

// Execution:
EvaluationRequest request = new EvaluationRequest(contextDocs, claimText);
EvaluationResponse response = evaluator.evaluate(request);
```

**Internal Framework Prompt Template:**
```text
Evaluate whether or not the following claim is supported by the provided document.
Respond with "yes" if the claim is supported, or "no" if it is not.

Document:
{document}

Claim:
{claim}
```
**Parsing Rule:** Checks `output.strip().equalsIgnoreCase("yes")` to determine pass/fail.

---

### 13.4 Deterministic Telemetry Gating (`KeepRule` v2)
In autonomous agentic architectures (like `agentic-performance-diagnostician`), qualitative LLM evaluation must be paired with mathematical threshold gates to prevent accepting illusory improvements caused by telemetry noise.

```java
public class NoiseFloorEvaluator {
    public static KeepDecision evaluate(LoadReportDto ref, LoadReportDto result, NoiseFloors floors) {
        double p95Improve = ref.latency().p95() - result.latency().p95();
        double rpsImprove = result.rps() - ref.rps();

        boolean rpsImproved = rpsImprove > floors.rpsFloor();
        boolean p95Improved = p95Improve > floors.p95FloorMs();
        boolean failGuard = result.failRate() <= ref.failRate();

        // 1. Classic keep: metric improved beyond noise floor AND error rate did not worsen
        if ((rpsImproved || p95Improved) && failGuard) {
            String keepType = rpsImproved ? "RPS" : "P95";
            return new KeepDecision(true, keepType, "Classic keep: " + keepType + " cleared floor");
        }

        // 2. Regression guard: improved but fail rate worsened -> REJECT
        if ((rpsImproved || p95Improved) && !failGuard) {
            return new KeepDecision(false, null, "improved beyond floor but failRate worsened");
        }

        return new KeepDecision(false, null, "Deltas within noise floor");
    }
}
```

---

### 13.5 Advanced LLM-as-a-Judge Engineering & Bias Mitigation

#### 1. Position Bias Mitigation (Pairwise A/B Tournaments)
LLMs exhibit severe positional bias: when given two candidate answers, they prefer the first candidate ("Option 1") in >70% of evaluations regardless of quality.

**The Mitigation Protocol:**
1. **Run 1:** Present `(Candidate A, Candidate B)`. Judge picks winner.
2. **Run 2:** Swap presentation order to `(Candidate B, Candidate A)`. Judge picks winner.
3. **Verdict Matrix:**
   - Run 1 chose Option 1 (A) and Run 2 chose Option 2 (A) $\rightarrow$ **Winner: Candidate A** (unbiased).
   - Run 1 chose Option 2 (B) and Run 2 chose Option 1 (B) $\rightarrow$ **Winner: Candidate B** (unbiased).
   - Run 1 and Run 2 chose the *same option position* (e.g. Option 1 in both runs) $\rightarrow$ **Position Bias Detected**, outcome is marked `INCONCLUSIVE_OR_TIE`.

#### 2. Self-Consistency Majority Voting (Confidence Sampling)
Single judge calls on borderline decisions are noisy. Sampling $N$ inferences (e.g. $N=3$ or $N=5$) with temperature > 0 creates a voting ensemble:
- **Consensus Rule:** $\text{Majority Pass} = \text{passVotes} > \frac{N}{2}$.
- **Confidence Metric:** $\text{Confidence} = \frac{\max(\text{passVotes}, \text{failVotes})}{N}$.
- If $\text{Confidence} < \text{threshold}$ (e.g. 0.70 for 3-2 split), reject the decision as inconclusive.

#### 3. RAG Triad Claim-Level Attribution (Hallucination Detection)
Evaluating whole paragraphs allows subtle hallucinations to slip through. Decompose responses into atomic factual claims:
1. Extract atomic claims from the candidate response.
2. Verify each claim against context documents individually.
3. Compute Groundedness / Faithfulness:
   $$\text{Faithfulness Score} = \frac{\text{Supported Claims}}{\text{Total Claims}}$$
4. Isolate unsupported claims for regression tracking.

#### 4. Anchor-Based Calibrated Rubrics (Grade Inflation Mitigation)
Zero-shot LLM judges inflate scores (awarding 4/5 or 5/5 to mediocre answers). Calibrate scoring by embedding explicit few-shot anchors in the prompt:
- **Anchor 1 (Level 1 - Poor):** Severe hallucination, missing query intent, or unsafe code.
- **Anchor 3 (Level 3 - Adequate):** Factually correct but incomplete, verbose, or lacking edge-case handling.
- **Anchor 5 (Level 5 - Exemplary):** Grounded, concise, handles edge cases, zero hallucinations.
Evaluate across separate dimensions (`Correctness`, `Completeness`, `Conciseness`) and normalize to $[0.0, 1.0]$.

#### 5. Statistical Inter-Judge Agreement (Cohen's Kappa)
To calibrate an LLM judge against human annotations or ground-truth datasets, compute **Cohen's Kappa ($\kappa$)**:
$$\kappa = \frac{P_o - P_e}{1 - P_e}$$
- $P_o$: Observed proportional agreement between Judge 1 and Judge 2.
- $P_e$: Probability of chance agreement based on marginal totals.
- **Interpretation:** $\kappa \ge 0.70$ indicates substantial agreement; $\kappa < 0.40$ indicates unacceptable judge drift.

---

## 14. In-Memory Saga Loops, State Machines & Compensations

### 14.1 Autonomous Agent State Machine (6-Phase Saga Loop)
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

### 14.2 Guaranteed Finally Compensations
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

### 14.3 Multi-Stage Diagnostic Failure Triage
Failure handling in agent loops must follow an escalating hierarchy:
1. **Stage 1 (`STAGE_1_TOOL_RETRY`)**: Ephemeral tool timeout or connection reset $\rightarrow$ retry with exponential backoff (no workspace mutation).
2. **Stage 2 (`STAGE_2_SCHEMA_REPAIR`)**: JSON parse error or malformed schema $\rightarrow$ 1-shot repair prompt sending parser error back to LLM.
3. **Stage 3 (`STAGE_3_COMPENSATION_REVERT`)**: Compilation error or test suite regression $\rightarrow$ roll back workspace to `lastKeptSha` and prompt LLM with build failure trace.
4. **Stage 4 (`STAGE_4_GUARDRAIL_TERMINATE`)**: Token budget ceiling, iteration cap, or cyclical oscillation detected $\rightarrow$ trip circuit breaker, terminate loop, escalate to human.

---

## 15. Spring Boot Assembly, Dynamic Tools & Enterprise Resilience

### 15.1 Package Relocations in Spring Boot 4.1.1
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

### 15.2 Spring AI 2.0.1 `CallAdvisor` Contracts
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

### 15.3 Enterprise Resilience Mesh Patterns

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
