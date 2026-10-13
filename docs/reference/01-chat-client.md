# Spring AI 2.0.1 — ChatClient & Core Accessors

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Java Version:** Java 21+ (`maven.compiler.parameters=true`)  
**Curriculum Alignment:** Phase 01 (`phase01-chat-client`) & Core Foundations

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

### Response Unpacking Chain
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
