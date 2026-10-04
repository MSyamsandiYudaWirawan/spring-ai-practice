# Phase 02 Exercise 01 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `parseVulnerabilityAssessment` (Direct Record Extraction)

### Root Cause
Direct deserialization requires `BeanOutputConverter<VulnerabilityAssessment>` to map JSON fields to the record constructor. Missing boundary validation allows `null` or blank strings to produce runtime errors.

### TigerStyle Fix
```java
public VulnerabilityAssessment parseVulnerabilityAssessment(String rawJson) {
    Objects.requireNonNull(rawJson, "rawJson must not be null");
    if (rawJson.isBlank()) throw new IllegalArgumentException("rawJson cannot be blank");

    return assessmentConverter.convert(rawJson);
}
```

---

## 2. Scenario 2: `extractFromMarkdownFences` (Markdown Fence & Preamble Sanitization)

### Root Cause
Real LLMs frequently wrap JSON in markdown code fences (```` ```json ... ``` ````) accompanied by conversational preamble and closing remarks. Passing conversational prose directly to a JSON parser triggers syntax errors.

### TigerStyle Fix
```java
public VulnerabilityAssessment extractFromMarkdownFences(String markdownPayload) {
    Objects.requireNonNull(markdownPayload, "markdownPayload must not be null");
    if (markdownPayload.isBlank()) throw new IllegalArgumentException("markdownPayload cannot be blank");

    String cleaned = sanitizeJsonPayload(markdownPayload);
    return assessmentConverter.convert(cleaned);
}

private String sanitizeJsonPayload(String text) {
    if (text == null) return null;
    int firstBrace = text.indexOf('{');
    int lastBrace = text.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
        return text.substring(firstBrace, lastBrace + 1).trim();
    }
    return text.trim();
}
```

---

## 3. Scenario 3: `extractPatchPlanList` (Generic Collections via `ParameterizedTypeReference`)

### Root Cause
Type erasure in Java prevents `BeanOutputConverter<>(List.class)` from knowing the generic type argument `PatchPlan`, causing deserialization to produce generic maps or fail. `ParameterizedTypeReference<List<PatchPlan>>` preserves generic type tokens at runtime.

### TigerStyle Fix
```java
public List<PatchPlan> extractPatchPlanList(String jsonArrayPayload) {
    Objects.requireNonNull(jsonArrayPayload, "jsonArrayPayload must not be null");
    if (jsonArrayPayload.isBlank()) throw new IllegalArgumentException("jsonArrayPayload cannot be blank");

    BeanOutputConverter<List<PatchPlan>> listConverter = new BeanOutputConverter<>(
            new ParameterizedTypeReference<List<PatchPlan>>() {}
    );

    int firstBracket = jsonArrayPayload.indexOf('[');
    int lastBracket = jsonArrayPayload.lastIndexOf(']');
    String cleaned = (firstBracket >= 0 && lastBracket > firstBracket)
            ? jsonArrayPayload.substring(firstBracket, lastBracket + 1).trim()
            : jsonArrayPayload.trim();

    return listConverter.convert(cleaned);
}
```

---

## 4. Scenario 4: `generateAssessmentWithSchema` (ChatClient `.entity(Class<T>)` Execution)

### Root Cause
Calling `.call().content()` discards schema guidance and produces raw unparsed text. Calling `.call().entity(VulnerabilityAssessment.class)` automatically injects the schema format and deserializes the response.

### TigerStyle Fix
```java
public VulnerabilityAssessment generateAssessmentWithSchema(ChatClient client, String advisoryText) {
    Objects.requireNonNull(client, "client must not be null");
    Objects.requireNonNull(advisoryText, "advisoryText must not be null");
    if (advisoryText.isBlank()) throw new IllegalArgumentException("advisoryText cannot be blank");

    return client.prompt()
            .user(advisoryText)
            .call()
            .entity(VulnerabilityAssessment.class);
}
```

---

## 5. Scenario 5: `validateRecordInvariants` (TigerStyle Invariant Validation)

### Root Cause
Relying solely on syntactic JSON validity allows semantically corrupt data (e.g. malformed CVE identifiers, negative CVSS scores, missing package group separators) to enter production systems. Domain boundaries must fail fast.

### TigerStyle Fix
```java
public void validateRecordInvariants(VulnerabilityAssessment assessment) {
    Objects.requireNonNull(assessment, "assessment must not be null");

    if (assessment.cvssScore() < 0.0 || assessment.cvssScore() > 10.0) {
        throw new IllegalArgumentException("cvssScore must be between 0.0 and 10.0");
    }
    if (!assessment.packageCoordinate().contains(":")) {
        throw new IllegalArgumentException("packageCoordinate must be formatted as group:artifact:version");
    }
    if (assessment.mitigationSteps().isEmpty()) {
        throw new IllegalArgumentException("mitigationSteps cannot be empty");
    }
}
```

---

## 6. Scenario 6: `safeDeserializeWithFallback` (Diagnostic Result Envelopes)

### Root Cause
Allowing parsing and validation exceptions to bubble up uncaught crashes upstream services. TigerStyle requires wrapping failures into typed diagnostic envelopes (`TriageEnvelope.rejected(...)`) without swallowing failure details.

### TigerStyle Fix
```java
public TriageEnvelope<VulnerabilityAssessment> safeDeserializeWithFallback(String jsonPayload) {
    Objects.requireNonNull(jsonPayload, "jsonPayload must not be null");
    if (jsonPayload.isBlank()) throw new IllegalArgumentException("jsonPayload cannot be blank");

    try {
        String cleaned = sanitizeJsonPayload(jsonPayload);
        VulnerabilityAssessment va = assessmentConverter.convert(cleaned);
        return TriageEnvelope.accepted(va);
    } catch (Exception e) {
        return TriageEnvelope.rejected("Schema parsing/validation error: " + e.getMessage());
    }
}
```

---

## 7. Scenario 7: `repairWithOneShotRetry` (Saga DECIDE One-Shot Feedback Loop)

### Root Cause
LLMs occasionally emit slightly malformed JSON on their first turn. Instead of aborting immediately or entering an infinite retry loop, production systems quote the validation error back to the model once to allow self-correction.

### TigerStyle Fix
```java
public VulnerabilityAssessment repairWithOneShotRetry(ChatClient client, String cveIdentifier) {
    Objects.requireNonNull(client, "client must not be null");
    Objects.requireNonNull(cveIdentifier, "cveIdentifier must not be null");
    if (cveIdentifier.isBlank()) throw new IllegalArgumentException("cveIdentifier cannot be blank");

    String firstResponse = client.prompt()
            .user("Analyze vulnerability for: " + cveIdentifier)
            .call()
            .content();

    try {
        return assessmentConverter.convert(sanitizeJsonPayload(firstResponse));
    } catch (Exception e) {
        String feedback = "Previous output failed schema validation: " + e.getMessage()
                + ". Please output strictly valid JSON conforming to schema:";

        String secondResponse = client.prompt()
                .user(feedback)
                .call()
                .content();

        return assessmentConverter.convert(sanitizeJsonPayload(secondResponse));
    }
}
```

---

## 8. Scenario 8: `enforceRetryLimit` (Strict Retry Bound Enforcement)

### Root Cause
Without a hard upper bound on retries, an uncooperative model causes an infinite loop consuming tokens and thread resources. The Saga DECIDE policy permits at most 1 retry, terminating as `IllegalStateException` / `WASTED`.

### TigerStyle Fix
```java
public VulnerabilityAssessment enforceRetryLimit(ChatClient client, String cveIdentifier) {
    Objects.requireNonNull(client, "client must not be null");
    Objects.requireNonNull(cveIdentifier, "cveIdentifier must not be null");
    if (cveIdentifier.isBlank()) throw new IllegalArgumentException("cveIdentifier cannot be blank");

    int maxRetries = 1;
    int attempts = 0;
    Exception lastException = null;
    String promptText = "Analyze vulnerability for: " + cveIdentifier;

    while (attempts <= maxRetries) {
        attempts++;
        String response = client.prompt()
                .user(promptText)
                .call()
                .content();

        try {
            return assessmentConverter.convert(sanitizeJsonPayload(response));
        } catch (Exception e) {
            lastException = e;
            promptText = "Previous output failed schema validation: " + e.getMessage()
                    + ". Please output strictly valid JSON conforming to schema:";
        }
    }

    throw new IllegalStateException("Schema repair exceeded maximum attempts (" + maxRetries + ")", lastException);
}
```
