# Spring AI 2.0.1 — Structured Outputs & Schema Repair

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 02 (`phase02-structured-output`) & Production Ingestion

---

## 1. Structured Outputs & `BeanOutputConverter`

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

## 2. Defensive Markdown Fence Stripping & Sanitization

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

## 3. The Saga DECIDE One-Shot Schema Repair Protocol

From agentic loop specifications: LLMs are permitted at most **1 retry** on schema failure, quoting the parsing error back to the model before terminating with `IllegalStateException` or marking `WASTED`:

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
