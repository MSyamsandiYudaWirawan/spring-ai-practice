# Phase 02 Exercise 02 — Golden Reference Solution
## Performance Diagnostic Decision Gateway & Resilient Schema Repair (15 Drills)

```java
package phase02;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import phase02.DecisionModelContracts.Change;
import phase02.DecisionModelContracts.DecisionEnvelope;
import phase02.DecisionModelContracts.Hypothesis;
import phase02.DecisionModelContracts.LedgerUpdate;
import phase02.DecisionModelContracts.OptimizationDecision;
import phase02.DecisionModelContracts.Prediction;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service under test for Phase 02 Exercise 02:
 * Performance Diagnostic Decision Gateway & Resilient Schema Repair (15 Drills).
 */
public class DecisionEngineUnderTest {

    private final BeanOutputConverter<OptimizationDecision> decisionConverter;

    public DecisionEngineUnderTest() {
        this.decisionConverter = new BeanOutputConverter<>(OptimizationDecision.class);
    }

    /**
     * Drill 1: Generate schema format instructions for prompt injection.
     */
    public String generateFormatInstructions() {
        return decisionConverter.getFormat();
    }

    /**
     * Drills 2 & 3: Direct record deserialization from clean JSON with fail-fast boundaries.
     */
    public OptimizationDecision parseDecision(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson must not be null or blank");
        }
        return decisionConverter.convert(rawJson);
    }

    /**
     * Drills 4, 5 & 6: Defensive markdown fence stripping & conversational preamble sanitization.
     */
    public OptimizationDecision extractFromConversationalText(String markdownPayload) {
        if (markdownPayload == null || markdownPayload.isBlank()) {
            throw new IllegalArgumentException("markdownPayload must not be null or blank");
        }
        int firstBrace = markdownPayload.indexOf('{');
        int lastBrace = markdownPayload.lastIndexOf('}');
        if (firstBrace == -1 || lastBrace == -1 || firstBrace > lastBrace) {
            throw new IllegalArgumentException("No JSON object found in conversational text");
        }
        String json = markdownPayload.substring(firstBrace, lastBrace + 1);
        return parseDecision(json);
    }

    /**
     * Drills 7, 8 & 9: TigerStyle domain invariants and cross-field rules.
     */
    public void validateCrossFieldRules(OptimizationDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(decision.hypothesis(), "hypothesis must not be null");
        Objects.requireNonNull(decision.prediction(), "prediction must not be null");
        Objects.requireNonNull(decision.ledger(), "ledger must not be null");
        Objects.requireNonNull(decision.change(), "change must not be null");

        Hypothesis h = decision.hypothesis();
        if (h.category() == null || !DecisionModelContracts.HYPOTHESIS_PATTERN.matcher(h.category()).matches()) {
            throw new IllegalArgumentException("Invalid hypothesis category: " + h.category());
        }
        if (h.confidence() < 0.0 || h.confidence() > 1.0) {
            throw new IllegalArgumentException("Confidence must be in [0.0, 1.0]: " + h.confidence());
        }
        if (h.rationale() == null || h.rationale().isBlank()) {
            throw new IllegalArgumentException("Rationale must not be blank");
        }

        Prediction p = decision.prediction();
        Set<String> allowedMetrics = Set.of("p95", "median", "rps", "error_rate");
        if (p.metric() == null || !allowedMetrics.contains(p.metric())) {
            throw new IllegalArgumentException("Invalid prediction metric: " + p.metric());
        }
        Set<String> allowedDirections = Set.of("improve", "regress", "neutral");
        if (p.direction() == null || !allowedDirections.contains(p.direction())) {
            throw new IllegalArgumentException("Invalid prediction direction: " + p.direction());
        }
        if (p.signalToEliminate() == null || p.signalToEliminate().isBlank()) {
            throw new IllegalArgumentException("signalToEliminate must not be blank");
        }

        LedgerUpdate l = decision.ledger();
        if (l.hypothesisCategory() == null || !DecisionModelContracts.HYPOTHESIS_PATTERN.matcher(l.hypothesisCategory()).matches()) {
            throw new IllegalArgumentException("Invalid ledger hypothesisCategory: " + l.hypothesisCategory());
        }
        Set<String> allowedLedgerDirs = Set.of("strengthen", "weaken");
        if (l.direction() == null || !allowedLedgerDirs.contains(l.direction())) {
            throw new IllegalArgumentException("Invalid ledger direction: " + l.direction());
        }
        if (l.reason() == null || l.reason().isBlank()) {
            throw new IllegalArgumentException("Ledger reason must not be blank");
        }

        Change c = decision.change();
        if ("edits".equalsIgnoreCase(c.kind())) {
            if (c.edits() == null || c.edits().isEmpty()) {
                throw new IllegalArgumentException("kind=edits requires non-empty edits list");
            }
            if (c.template() != null && !c.template().isBlank()) {
                throw new IllegalArgumentException("kind=edits must not specify template");
            }
        } else if ("template".equalsIgnoreCase(c.kind())) {
            Set<String> allowedTemplates = Set.of("jar-unpack", "hikari-pool", "virtual-threads");
            if (c.template() == null || !allowedTemplates.contains(c.template())) {
                throw new IllegalArgumentException("kind=template requires allowed template: " + c.template());
            }
            if (c.edits() != null && !c.edits().isEmpty()) {
                throw new IllegalArgumentException("kind=template must have null or empty edits");
            }
        } else {
            throw new IllegalArgumentException("Invalid change kind: " + c.kind());
        }
    }

    /**
     * Drills 10 & 11: Generic collections extraction via ParameterizedTypeReference.
     */
    public List<OptimizationDecision> parseDecisionBatch(String jsonArrayPayload) {
        if (jsonArrayPayload == null || jsonArrayPayload.isBlank()) {
            throw new IllegalArgumentException("jsonArrayPayload must not be null or blank");
        }
        BeanOutputConverter<List<OptimizationDecision>> batchConverter =
                new BeanOutputConverter<>(new ParameterizedTypeReference<List<OptimizationDecision>>() {});
        return batchConverter.convert(jsonArrayPayload);
    }

    /**
     * Drill 12: Fallback semi-structured diagnostic prose extraction.
     */
    public static final Pattern PROSE_HYPOTHESIS_PATTERN = Pattern.compile("(?i)\\b(H[1-7])\\b");
    public static final Pattern PROSE_CONFIDENCE_PATTERN = Pattern.compile("(?i)confidence\\s+([0-9]*\\.?[0-9]+)");
    public static final Pattern PROSE_TEMPLATE_PATTERN = Pattern.compile("(?i)template\\s+([a-z0-9-]+)");
    public static final Pattern PROSE_SIGNAL_PATTERN = Pattern.compile("(?i)(JavaMonitorEnter|SocketRead|ObjectAllocationSample)");
    public static final Pattern PROSE_METRIC_PATTERN = Pattern.compile("(?i)\\b(p95|median|rps|error_rate)\\b");
    public static final Pattern PROSE_DIRECTION_PATTERN = Pattern.compile("(?i)\\b(strengthen|weaken)\\b");

    public OptimizationDecision fallbackExtractFromProse(String proseText) {
        if (proseText == null || proseText.isBlank()) {
            return null;
        }
        Matcher hypoMatcher = PROSE_HYPOTHESIS_PATTERN.matcher(proseText);
        if (!hypoMatcher.find()) {
            return null;
        }
        String category = hypoMatcher.group(1).toUpperCase();

        Matcher templateMatcher = PROSE_TEMPLATE_PATTERN.matcher(proseText);
        if (!templateMatcher.find()) {
            return null;
        }
        String template = templateMatcher.group(1).toLowerCase();

        double confidence = 0.80;
        Matcher confMatcher = PROSE_CONFIDENCE_PATTERN.matcher(proseText);
        if (confMatcher.find()) {
            try {
                confidence = Double.parseDouble(confMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

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

        Hypothesis hypothesis = new Hypothesis(category, confidence, "Extracted from prose diagnostics");
        Prediction prediction = new Prediction(metric, "improve", signal);
        LedgerUpdate ledger = new LedgerUpdate(category, direction, "Prose reasoning evidence");
        Change change = new Change("template", null, template, Map.of());

        return new OptimizationDecision(hypothesis, prediction, ledger, change);
    }

    /**
     * Drills 13 & 14: One-shot feedback schema repair loop (Saga DECIDE pattern).
     */
    public DecisionEnvelope<OptimizationDecision> repairDecisionWithFeedback(ChatClient client, String promptText) {
        if (client == null || promptText == null || promptText.isBlank()) {
            throw new IllegalArgumentException("client and promptText must not be null or blank");
        }
        int attemptsUsed = 0;
        attemptsUsed++;
        String response1 = client.prompt(promptText).call().content();
        try {
            OptimizationDecision decision1 = extractFromConversationalText(response1);
            validateCrossFieldRules(decision1);
            return DecisionEnvelope.accepted(decision1, attemptsUsed);
        } catch (Exception e1) {
            attemptsUsed++;
            String feedbackPrompt = "Previous proposal failed validation: " + e1.getMessage() + ". Please fix the error and return strictly valid JSON matching the schema.";
            String response2 = client.prompt(feedbackPrompt).call().content();
            OptimizationDecision decision2 = extractFromConversationalText(response2);
            validateCrossFieldRules(decision2);
            return DecisionEnvelope.accepted(decision2, attemptsUsed);
        }
    }

    /**
     * Drill 15: Enforce strict retry limit (prevent runaway infinite repair loops).
     */
    public DecisionEnvelope<OptimizationDecision> enforceStrictRepairCircuitBreaker(ChatClient client, String promptText) {
        if (client == null || promptText == null || promptText.isBlank()) {
            throw new IllegalArgumentException("client and promptText must not be null or blank");
        }
        int attemptsUsed = 0;
        Exception lastError = null;

        // Attempt 1
        attemptsUsed++;
        try {
            String response1 = client.prompt(promptText).call().content();
            OptimizationDecision decision1 = extractFromConversationalText(response1);
            validateCrossFieldRules(decision1);
            return DecisionEnvelope.accepted(decision1, attemptsUsed);
        } catch (Exception e) {
            lastError = e;
        }

        // Attempt 2 (Retry 1)
        attemptsUsed++;
        try {
            String feedbackPrompt = "Previous proposal failed validation: " + lastError.getMessage() + ". Please fix the error and return strictly valid JSON matching the schema.";
            String response2 = client.prompt(feedbackPrompt).call().content();
            OptimizationDecision decision2 = extractFromConversationalText(response2);
            validateCrossFieldRules(decision2);
            return DecisionEnvelope.accepted(decision2, attemptsUsed);
        } catch (Exception e) {
            throw new IllegalStateException("Decision schema repair exceeded maximum attempts (1)", e);
        }
    }
}
```
