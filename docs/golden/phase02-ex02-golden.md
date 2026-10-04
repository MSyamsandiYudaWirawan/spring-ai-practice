# Phase 02 Exercise 02 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `generateFormatInstructions` (Schema Directives Injection)

### Root Cause
LLMs require explicit schema blueprints in their system context to know what fields and JSON structure to produce. `BeanOutputConverter.getFormat()` dynamically generates JSON schema instructions based on the target Java record class.

### TigerStyle Fix
```java
public String generateFormatInstructions() {
    return decisionConverter.getFormat();
}
```

---

## 2. Scenario 2: `parseDecision` (Direct BeanOutputConverter Deserialization)

### Root Cause
Direct deserialization requires `BeanOutputConverter<OptimizationDecision>` to map JSON fields to nested record constructors. Missing boundary validation allows `null` or blank strings to cause runtime null pointer exceptions.

### TigerStyle Fix
```java
public OptimizationDecision parseDecision(String rawJson) {
    Objects.requireNonNull(rawJson, "rawJson must not be null");
    if (rawJson.isBlank()) {
        throw new IllegalArgumentException("rawJson must not be blank");
    }
    return decisionConverter.convert(rawJson);
}
```

---

## 3. Scenario 3: `extractFromConversationalText` (Markdown Fence & Preamble Stripping)

### Root Cause
LLMs frequently wrap JSON in markdown code fences (` ```json ... ``` `) accompanied by conversational preamble ("Here is the proposed optimization...") and concluding remarks. Passing conversational prose directly to a JSON parser triggers syntax errors. Extracting the substring between the first `{` and last `}` strips away conversational noise.

### TigerStyle Fix
```java
public OptimizationDecision extractFromConversationalText(String markdownPayload) {
    Objects.requireNonNull(markdownPayload, "markdownPayload must not be null");
    if (markdownPayload.isBlank()) {
        throw new IllegalArgumentException("markdownPayload must not be blank");
    }
    String cleaned = sanitizeJsonPayload(markdownPayload);
    return decisionConverter.convert(cleaned);
}

private String sanitizeJsonPayload(String json) {
    if (json == null) return null;
    int firstBrace = json.indexOf('{');
    int lastBrace = json.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
        return json.substring(firstBrace, lastBrace + 1).trim();
    }
    return json.trim();
}
```

---

## 4. Scenario 4: `validateCrossFieldRules` (Cross-Field Invariants & Domain Rules)

### Root Cause
In agentic decision loops, structural JSON validity is not enough. The `Change` payload is a discriminated union: `kind=edits` demands a non-empty list of `FileEdit`s and no template, whereas `kind=template` demands an admissible template ID and no file edits. Validating these cross-field invariants before any code changes are applied guarantees the Saga's atomicity.

### TigerStyle Fix
```java
public void validateCrossFieldRules(OptimizationDecision decision) {
    Objects.requireNonNull(decision, "decision must not be null");

    // 1. Hypothesis validation
    Hypothesis h = decision.hypothesis();
    Objects.requireNonNull(h, "hypothesis must not be null");
    if (h.category() == null || !HYPOTHESIS_PATTERN.matcher(h.category()).matches()) {
        throw new IllegalArgumentException("hypothesis.category must match ^H[1-7]$, got: " + (h.category() == null ? "null" : h.category()));
    }
    if (h.confidence() < 0.0 || h.confidence() > 1.0) {
        throw new IllegalArgumentException("hypothesis.confidence must be between 0.0 and 1.0, got: " + h.confidence());
    }
    if (h.rationale() == null || h.rationale().isBlank()) {
        throw new IllegalArgumentException("hypothesis.rationale must not be blank");
    }

    // 2. Prediction validation
    Prediction p = decision.prediction();
    Objects.requireNonNull(p, "prediction must not be null");
    if (p.metric() == null || !List.of("p95", "median", "rps", "error_rate").contains(p.metric().toLowerCase())) {
        throw new IllegalArgumentException("prediction.metric must be p95, median, rps, or error_rate, got: " + p.metric());
    }
    if (p.direction() == null || !List.of("improve", "regress", "neutral").contains(p.direction().toLowerCase())) {
        throw new IllegalArgumentException("prediction.direction must be improve, regress, or neutral, got: " + p.direction());
    }
    if (p.signalToEliminate() == null || p.signalToEliminate().isBlank()) {
        throw new IllegalArgumentException("prediction.signalToEliminate must not be blank");
    }

    // 3. LedgerUpdate validation
    LedgerUpdate l = decision.ledger();
    Objects.requireNonNull(l, "ledger must not be null");
    if (l.hypothesisCategory() == null || !HYPOTHESIS_PATTERN.matcher(l.hypothesisCategory()).matches()) {
        throw new IllegalArgumentException("ledger.hypothesisCategory must match ^H[1-7]$, got: " + l.hypothesisCategory());
    }
    if (l.direction() == null || !List.of("strengthen", "weaken").contains(l.direction().toLowerCase())) {
        throw new IllegalArgumentException("ledger.direction must be strengthen or weaken, got: " + l.direction());
    }
    if (l.reason() == null || l.reason().isBlank()) {
        throw new IllegalArgumentException("ledger.reason must not be blank");
    }

    // 4. Change validation (cross-field: edits vs template)
    Change c = decision.change();
    Objects.requireNonNull(c, "change must not be null");
    String kind = c.kind();
    if ("edits".equalsIgnoreCase(kind)) {
        if (c.edits() == null || c.edits().isEmpty()) {
            throw new IllegalArgumentException("kind=edits requires a non-empty edits list");
        }
        if (c.template() != null && !c.template().isBlank()) {
            throw new IllegalArgumentException("kind=edits must not specify a template");
        }
    } else if ("template".equalsIgnoreCase(kind)) {
        if (c.template() == null || c.template().isBlank()) {
            throw new IllegalArgumentException("kind=template requires a non-blank template id");
        }
        if (!List.of("jar-unpack", "hikari-pool", "virtual-threads").contains(c.template().toLowerCase())) {
            throw new IllegalArgumentException("Unknown template id: " + c.template() + " (admissible: jar-unpack, hikari-pool, virtual-threads)");
        }
        if (c.edits() != null && !c.edits().isEmpty()) {
            throw new IllegalArgumentException("kind=template must not specify edits");
        }
    } else {
        throw new IllegalArgumentException("change.kind must be 'edits' or 'template', got: " + kind);
    }
}
```

---

## 5. Scenario 5: `parseDecisionBatch` (Generic Collections via `ParameterizedTypeReference`)

### Root Cause
Type erasure prevents `BeanOutputConverter<>(List.class)` from knowing the inner type `OptimizationDecision`. Passing `new ParameterizedTypeReference<List<OptimizationDecision>>() {}` retains full type information for Jackson.

### TigerStyle Fix
```java
public List<OptimizationDecision> parseDecisionBatch(String jsonArrayPayload) {
    Objects.requireNonNull(jsonArrayPayload, "jsonArrayPayload must not be null");
    if (jsonArrayPayload.isBlank()) {
        throw new IllegalArgumentException("jsonArrayPayload must not be blank");
    }
    BeanOutputConverter<List<OptimizationDecision>> listConverter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
    int firstBracket = jsonArrayPayload.indexOf('[');
    int lastBracket = jsonArrayPayload.lastIndexOf(']');
    String payload = (firstBracket >= 0 && lastBracket > firstBracket)
            ? jsonArrayPayload.substring(firstBracket, lastBracket + 1).trim()
            : jsonArrayPayload.trim();
    return listConverter.convert(payload);
}
```

---

## 6. Scenario 6: `fallbackExtractFromProse` (Semi-Structured Text Reconstruction)

### Root Cause
When an LLM provides a clear diagnostic rationale in natural language prose but forgets to emit JSON brackets, a fallback extractor recovers the intent using regex matching against known domain entities (`H1-H7`, template names, signals, and metrics).

### TigerStyle Fix
```java
public OptimizationDecision fallbackExtractFromProse(String proseText) {
    if (proseText == null || proseText.isBlank()) {
        return null;
    }

    Matcher hypMatcher = PROSE_HYPOTHESIS_PATTERN.matcher(proseText);
    if (!hypMatcher.find()) {
        return null;
    }
    String category = hypMatcher.group(1).toUpperCase();

    double confidence = 0.8;
    Matcher confMatcher = PROSE_CONFIDENCE_PATTERN.matcher(proseText);
    if (confMatcher.find()) {
        try {
            confidence = Double.parseDouble(confMatcher.group(1));
        } catch (NumberFormatException ignored) {}
    }

    Matcher tplMatcher = PROSE_TEMPLATE_PATTERN.matcher(proseText);
    if (!tplMatcher.find()) {
        return null;
    }
    String template = tplMatcher.group(1).toLowerCase();

    String signal = "JavaMonitorEnter";
    Matcher sigMatcher = PROSE_SIGNAL_PATTERN.matcher(proseText);
    if (sigMatcher.find()) {
        signal = sigMatcher.group(1);
    }

    String metric = "p95";
    Matcher metricMatcher = PROSE_METRIC_PATTERN.matcher(proseText);
    if (metricMatcher.find()) {
        metric = metricMatcher.group(1).toLowerCase();
    }

    String direction = "strengthen";
    Matcher dirMatcher = PROSE_DIRECTION_PATTERN.matcher(proseText);
    if (dirMatcher.find()) {
        direction = dirMatcher.group(1).toLowerCase();
    }

    Hypothesis h = new Hypothesis(category, confidence, "Extracted from prose diagnosis");
    Prediction p = new Prediction(metric, "improve", signal);
    LedgerUpdate l = new LedgerUpdate(category, direction, "Prose evidence for " + signal);
    Change c = new Change("template", null, template, Map.of());

    return new OptimizationDecision(h, p, l, c);
}
```

---

## 7. Scenario 7: `repairDecisionWithFeedback` (Saga DECIDE One-Shot Feedback Loop)

### Root Cause
In autonomous agent loops, intermittent formatting errors or constraint violations should not crash the pipeline. Quoting the exact validation or parse failure back to the model in a second prompt gives it the context required to correct its output.

### TigerStyle Fix
```java
public DecisionEnvelope<OptimizationDecision> repairDecisionWithFeedback(ChatClient client, String promptText) {
    Objects.requireNonNull(client, "client must not be null");
    Objects.requireNonNull(promptText, "promptText must not be null");
    if (promptText.isBlank()) {
        throw new IllegalArgumentException("promptText must not be blank");
    }

    // Attempt 1
    String firstRaw = client.prompt().user(promptText).call().content();
    try {
        String cleaned = sanitizeJsonPayload(firstRaw);
        OptimizationDecision decision = decisionConverter.convert(cleaned);
        validateCrossFieldRules(decision);
        return DecisionEnvelope.accepted(decision, 1);
    } catch (Exception e) {
        // Attempt 2: feedback repair
        String feedbackPrompt = "Previous proposal failed validation: " + e.getMessage() + ". Please fix the error and return strictly valid JSON matching the schema.";
        String secondRaw = client.prompt().user(feedbackPrompt).call().content();
        String cleanedSecond = sanitizeJsonPayload(secondRaw);
        OptimizationDecision repaired = decisionConverter.convert(cleanedSecond);
        validateCrossFieldRules(repaired);
        return DecisionEnvelope.accepted(repaired, 2);
    }
}
```

---

## 8. Scenario 8: `enforceStrictRepairCircuitBreaker` (Strict Retry Limit Enforcement)

### Root Cause
Without a hard upper bound on repair attempts, an uncorrectable schema bug or hallucinating model causes runaway recursion or infinite loops, rapidly consuming token budgets. Permitting at most 1 retry (max 2 total calls) and throwing `IllegalStateException("Decision schema repair exceeded maximum attempts (1)")` ensures clean failure handling.

### TigerStyle Fix
```java
public DecisionEnvelope<OptimizationDecision> enforceStrictRepairCircuitBreaker(ChatClient client, String promptText) {
    Objects.requireNonNull(client, "client must not be null");
    Objects.requireNonNull(promptText, "promptText must not be null");
    if (promptText.isBlank()) {
        throw new IllegalArgumentException("promptText must not be blank");
    }

    int attempts = 0;
    Exception lastError = null;

    while (attempts < 2) {
        attempts++;
        try {
            String promptToSend = (attempts == 1)
                    ? promptText
                    : "Previous proposal failed validation: " + (lastError != null ? lastError.getMessage() : "unknown") + ". Please fix the error and return strictly valid JSON matching the schema.";
            String raw = client.prompt().user(promptToSend).call().content();
            String cleaned = sanitizeJsonPayload(raw);
            OptimizationDecision decision = decisionConverter.convert(cleaned);
            validateCrossFieldRules(decision);
            return DecisionEnvelope.accepted(decision, attempts);
        } catch (Exception e) {
            lastError = e;
        }
    }

    // Circuit breaker trip
    throw new IllegalStateException("Decision schema repair exceeded maximum attempts (1)");
}
```
