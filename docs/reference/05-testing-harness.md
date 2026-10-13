# Spring AI 2.0.1 — Deterministic Testing & Offline Harness Patterns

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 05 (`phase05-testing-evaluations` Part 1) & CI Determinism

---

## 1. The Architectural Seam Problem: Direct Mocking vs Port Interfaces

Testing LLM-backed applications presents unique architectural challenges: live model calls are slow, nondeterministic, cost money, and fail unexpectedly in CI.

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

### The Solution: The `ChatPort` Hexagonal Seam
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

---

## 2. Scripted FIFO & Pattern-Matching Test Doubles

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

---

## 3. Outgoing Prompt Spy (`PromptCaptor`)

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

---

## 4. Controllable Virtual Time (`TestClock`)

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

---

## 5. Deterministic Reactive Flux Testing

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
