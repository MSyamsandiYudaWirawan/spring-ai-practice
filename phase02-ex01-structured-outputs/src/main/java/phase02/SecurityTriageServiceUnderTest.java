package phase02;

import org.springframework.ai.chat.client.ChatClient;
import phase02.SecurityModelContracts.*;

import java.util.List;
import java.util.Map;

/**
 * Service under test — Phase 02 Exercise 01:
 * Structured Outputs & Resilient Schema Extraction (15 Scenarios).
 * <p>
 * PRACTICE MODE: All drills stubbed. Implement each scenario to pass Verifier.
 * Golden reference solution is sealed in: docs/golden/phase02-ex01-golden.md
 */
public class SecurityTriageServiceUnderTest {

    /**
     * Drill 1: Direct JSON record deserialization via BeanOutputConverter.
     */
    public VulnerabilityAssessment parseVulnerabilityAssessment(String rawJson) {
        // TODO: Drill 1 - Validate not null/blank, convert using BeanOutputConverter
        return null;
    }

    /**
     * Drill 2: Defensive markdown fence stripping & conversational preamble sanitization.
     */
    public VulnerabilityAssessment extractFromMarkdownFences(String markdownPayload) {
        // TODO: Drill 2 - Clean fences/preamble, convert using BeanOutputConverter
        return null;
    }

    /**
     * Drill 3: Direct single record deserialization for PatchPlan.
     */
    public PatchPlan parsePatchPlan(String rawJson) {
        // TODO: Drill 3 - Validate not null/blank, convert using BeanOutputConverter<PatchPlan>
        return null;
    }

    /**
     * Drill 4: Generic collections extraction via ParameterizedTypeReference.
     */
    public List<PatchPlan> extractPatchPlanList(String jsonArrayPayload) {
        // TODO: Drill 4 - Convert using BeanOutputConverter<List<PatchPlan>>
        return null;
    }

    /**
     * Drill 5: Generic VulnerabilityAssessment list extraction.
     */
    public List<VulnerabilityAssessment> extractVulnerabilityList(String jsonArrayPayload) {
        // TODO: Drill 5 - Convert using BeanOutputConverter<List<VulnerabilityAssessment>>
        return null;
    }

    /**
     * Drill 6: Generic Map extraction via ParameterizedTypeReference.
     */
    public Map<String, Severity> extractSeverityMap(String jsonMapPayload) {
        // TODO: Drill 6 - Convert using BeanOutputConverter<Map<String, Severity>>
        return null;
    }

    /**
     * Drill 7: Type-safe ChatClient execution via .entity(Class<T>).
     */
    public VulnerabilityAssessment generateAssessmentWithEntity(ChatClient client, String advisoryText) {
        // TODO: Drill 7 - Call client.prompt().user(advisoryText).call().entity(VulnerabilityAssessment.class)
        return null;
    }

    /**
     * Drill 8: Type-safe ChatClient execution via .entity(ParameterizedTypeReference).
     */
    public List<PatchPlan> generatePatchPlansWithEntity(ChatClient client, String advisoryText) {
        // TODO: Drill 8 - Call client with entity(new ParameterizedTypeReference<List<PatchPlan>>() {})
        return null;
    }

    /**
     * Drill 9: Generic parameterized class entity extraction.
     */
    public <T> T generateCustomEntity(ChatClient client, String advisoryText, Class<T> clazz) {
        // TODO: Drill 9 - Call client.prompt().user(advisoryText).call().entity(clazz)
        return null;
    }

    /**
     * Drill 10: TigerStyle domain invariant validation.
     */
    public void validateRecordInvariants(VulnerabilityAssessment assessment) {
        // TODO: Drill 10 - Check null assessment and compact constructor invariants
        throw new UnsupportedOperationException("TODO: validateRecordInvariants");
    }

    /**
     * Drill 11: Safe deserializer with diagnostic envelope (TriageEnvelope).
     */
    public TriageEnvelope<VulnerabilityAssessment> safeDeserializeWithFallback(String jsonPayload) {
        // TODO: Drill 11 - Try deserialization; return accepted on success or rejected on exception
        return null;
    }

    /**
     * Drill 12: Safe batch deserializer with diagnostic envelopes.
     */
    public List<TriageEnvelope<VulnerabilityAssessment>> safeBatchDeserializeWithFallback(List<String> jsonList) {
        // TODO: Drill 12 - Batch convert each item into TriageEnvelope
        return null;
    }

    /**
     * Drill 13: One-shot feedback schema repair loop.
     */
    public VulnerabilityAssessment repairWithOneShotRetry(ChatClient client, String cveIdentifier) {
        // TODO: Drill 13 - Call client; if initial output fails, retry with error message quoted
        return null;
    }

    /**
     * Drill 14: Enforce strict retry limit (prevent runaway infinite repair loops).
     */
    public VulnerabilityAssessment enforceRetryLimit(ChatClient client, String cveIdentifier) {
        // TODO: Drill 14 - Cap at 2 calls; throw IllegalStateException("Schema repair exceeded maximum attempts (1)") if retry also fails
        return null;
    }

    /**
     * Drill 15: Autonomous resilient triage pipeline.
     */
    public TriageEnvelope<VulnerabilityAssessment> autonomousResilientTriagePipeline(ChatClient client, String cveIdentifier) {
        // TODO: Drill 15 - Execute triage pipeline and wrap in TriageEnvelope
        return null;
    }
}
