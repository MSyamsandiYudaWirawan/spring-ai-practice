# Phase 02 Exercise 01 — Golden Reference Solution
## Structured Outputs & Resilient Schema Extraction (15 Drills)

```java
package phase02;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import phase02.SecurityModelContracts.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service under test — Phase 02 Exercise 01:
 * Structured Outputs & Resilient Schema Extraction (15 Scenarios).
 */
public class SecurityTriageServiceUnderTest {

    private final BeanOutputConverter<VulnerabilityAssessment> assessmentConverter = new BeanOutputConverter<>(VulnerabilityAssessment.class);
    private final BeanOutputConverter<PatchPlan> patchPlanConverter = new BeanOutputConverter<>(PatchPlan.class);
    private final BeanOutputConverter<List<PatchPlan>> patchPlanListConverter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<PatchPlan>>() {});
    private final BeanOutputConverter<List<VulnerabilityAssessment>> vulnerabilityListConverter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<VulnerabilityAssessment>>() {});
    private final BeanOutputConverter<Map<String, Severity>> severityMapConverter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<Map<String, Severity>>() {});

    private String cleanPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload must not be null or blank");
        }
        String trimmed = payload.trim();
        int firstBrace = trimmed.indexOf('{');
        int firstBracket = trimmed.indexOf('[');
        int start = -1;
        int end = -1;

        if (firstBrace >= 0 && (firstBracket < 0 || firstBrace < firstBracket)) {
            start = firstBrace;
            end = trimmed.lastIndexOf('}') + 1;
        } else if (firstBracket >= 0) {
            start = firstBracket;
            end = trimmed.lastIndexOf(']') + 1;
        }

        if (start >= 0 && end > start) {
            return trimmed.substring(start, end);
        }
        return trimmed;
    }

    /**
     * Drill 1: Direct JSON record deserialization via BeanOutputConverter.
     */
    public VulnerabilityAssessment parseVulnerabilityAssessment(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson must not be null or blank");
        }
        return assessmentConverter.convert(rawJson);
    }

    /**
     * Drill 2: Defensive markdown fence stripping & conversational preamble sanitization.
     */
    public VulnerabilityAssessment extractFromMarkdownFences(String markdownPayload) {
        String cleaned = cleanPayload(markdownPayload);
        return assessmentConverter.convert(cleaned);
    }

    /**
     * Drill 3: Direct single record deserialization for PatchPlan.
     */
    public PatchPlan parsePatchPlan(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson must not be null or blank");
        }
        return patchPlanConverter.convert(rawJson);
    }

    /**
     * Drill 4: Generic collections extraction via ParameterizedTypeReference.
     */
    public List<PatchPlan> extractPatchPlanList(String jsonArrayPayload) {
        if (jsonArrayPayload == null || jsonArrayPayload.isBlank()) {
            throw new IllegalArgumentException("jsonArrayPayload must not be null or blank");
        }
        return patchPlanListConverter.convert(cleanPayload(jsonArrayPayload));
    }

    /**
     * Drill 5: Generic VulnerabilityAssessment list extraction.
     */
    public List<VulnerabilityAssessment> extractVulnerabilityList(String jsonArrayPayload) {
        if (jsonArrayPayload == null || jsonArrayPayload.isBlank()) {
            throw new IllegalArgumentException("jsonArrayPayload must not be null or blank");
        }
        return vulnerabilityListConverter.convert(cleanPayload(jsonArrayPayload));
    }

    /**
     * Drill 6: Generic Map extraction via ParameterizedTypeReference.
     */
    public Map<String, Severity> extractSeverityMap(String jsonMapPayload) {
        if (jsonMapPayload == null || jsonMapPayload.isBlank()) {
            throw new IllegalArgumentException("jsonMapPayload must not be null or blank");
        }
        return severityMapConverter.convert(cleanPayload(jsonMapPayload));
    }

    /**
     * Drill 7: Type-safe ChatClient execution via .entity(Class<T>).
     */
    public VulnerabilityAssessment generateAssessmentWithEntity(ChatClient client, String advisoryText) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        if (advisoryText == null || advisoryText.isBlank()) throw new IllegalArgumentException("advisoryText must not be null or blank");
        return client.prompt().user(advisoryText).call().entity(VulnerabilityAssessment.class);
    }

    /**
     * Drill 8: Type-safe ChatClient execution via .entity(ParameterizedTypeReference).
     */
    public List<PatchPlan> generatePatchPlansWithEntity(ChatClient client, String advisoryText) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        if (advisoryText == null || advisoryText.isBlank()) throw new IllegalArgumentException("advisoryText must not be null or blank");
        return client.prompt().user(advisoryText).call().entity(new ParameterizedTypeReference<List<PatchPlan>>() {});
    }

    /**
     * Drill 9: Generic parameterized class entity extraction.
     */
    public <T> T generateCustomEntity(ChatClient client, String advisoryText, Class<T> clazz) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        if (advisoryText == null || advisoryText.isBlank()) throw new IllegalArgumentException("advisoryText must not be null or blank");
        if (clazz == null) throw new IllegalArgumentException("clazz must not be null");
        return client.prompt().user(advisoryText).call().entity(clazz);
    }

    /**
     * Drill 10: TigerStyle domain invariant validation.
     */
    public void validateRecordInvariants(VulnerabilityAssessment assessment) {
        if (assessment == null) throw new IllegalArgumentException("assessment must not be null");
        // Compact constructor of VulnerabilityAssessment validates invariants
    }

    /**
     * Drill 11: Safe deserializer with diagnostic envelope (TriageEnvelope).
     */
    public TriageEnvelope<VulnerabilityAssessment> safeDeserializeWithFallback(String jsonPayload) {
        try {
            String cleaned = cleanPayload(jsonPayload);
            VulnerabilityAssessment va = assessmentConverter.convert(cleaned);
            validateRecordInvariants(va);
            return TriageEnvelope.accepted(va);
        } catch (Exception ex) {
            return TriageEnvelope.rejected(ex.getMessage() != null ? ex.getMessage() : "Unknown deserialization failure");
        }
    }

    /**
     * Drill 12: Safe batch deserializer with diagnostic envelopes.
     */
    public List<TriageEnvelope<VulnerabilityAssessment>> safeBatchDeserializeWithFallback(List<String> jsonList) {
        if (jsonList == null) throw new IllegalArgumentException("jsonList must not be null");
        List<TriageEnvelope<VulnerabilityAssessment>> result = new ArrayList<>();
        for (String json : jsonList) {
            result.add(safeDeserializeWithFallback(json));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Drill 13: One-shot feedback schema repair loop.
     */
    public VulnerabilityAssessment repairWithOneShotRetry(ChatClient client, String cveIdentifier) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        if (cveIdentifier == null || cveIdentifier.isBlank()) throw new IllegalArgumentException("cveIdentifier must not be null or blank");

        String firstResponse = client.prompt().user("Analyze vulnerability for: " + cveIdentifier).call().content();
        try {
            return assessmentConverter.convert(cleanPayload(firstResponse));
        } catch (Exception e1) {
            String retryPrompt = "Previous output failed schema validation: " + e1.getMessage() + ". Please output strictly valid JSON conforming to schema:";
            String secondResponse = client.prompt().user(retryPrompt).call().content();
            return assessmentConverter.convert(cleanPayload(secondResponse));
        }
    }

    /**
     * Drill 14: Enforce strict retry limit (prevent runaway infinite repair loops).
     */
    public VulnerabilityAssessment enforceRetryLimit(ChatClient client, String cveIdentifier) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        if (cveIdentifier == null || cveIdentifier.isBlank()) throw new IllegalArgumentException("cveIdentifier must not be null or blank");

        String firstResponse = client.prompt().user("Analyze vulnerability for: " + cveIdentifier).call().content();
        try {
            return assessmentConverter.convert(cleanPayload(firstResponse));
        } catch (Exception e1) {
            String retryPrompt = "Previous output failed schema validation: " + e1.getMessage() + ". Please output strictly valid JSON conforming to schema:";
            String secondResponse = client.prompt().user(retryPrompt).call().content();
            try {
                return assessmentConverter.convert(cleanPayload(secondResponse));
            } catch (Exception e2) {
                throw new IllegalStateException("Schema repair exceeded maximum attempts (1)", e2);
            }
        }
    }

    /**
     * Drill 15: Autonomous resilient triage pipeline.
     */
    public TriageEnvelope<VulnerabilityAssessment> autonomousResilientTriagePipeline(ChatClient client, String cveIdentifier) {
        try {
            VulnerabilityAssessment va = enforceRetryLimit(client, cveIdentifier);
            validateRecordInvariants(va);
            return TriageEnvelope.accepted(va);
        } catch (Exception ex) {
            return TriageEnvelope.rejected(ex.getMessage() != null ? ex.getMessage() : "Pipeline failed");
        }
    }
}
```
