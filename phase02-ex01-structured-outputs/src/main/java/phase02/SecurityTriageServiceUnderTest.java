package phase02;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import phase02.SecurityModelContracts.PatchPlan;
import phase02.SecurityModelContracts.TriageEnvelope;
import phase02.SecurityModelContracts.VulnerabilityAssessment;

import java.util.List;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Seeded with defects across 8 scenarios in Structured Outputs & Resilient Schema Extraction:
 * - Scenario 1: Direct JSON record deserialization via BeanOutputConverter.
 * - Scenario 2: Markdown fence stripping & conversational preamble sanitization.
 * - Scenario 3: Generic collections extraction via ParameterizedTypeReference.
 * - Scenario 4: Schema generation & type-safe ChatClient .entity(Class<T>) dispatch.
 * - Scenario 5: TigerStyle domain invariant validation in record constructors.
 * - Scenario 6: Exception wrapping into diagnostic result envelopes (TriageEnvelope).
 * - Scenario 7: One-shot feedback schema repair loop on malformed JSON.
 * - Scenario 8: Strict retry bound enforcement (prevent infinite repair loops -> IllegalStateException).
 */
public class SecurityTriageServiceUnderTest {

    private final BeanOutputConverter<VulnerabilityAssessment> assessmentConverter;

    public SecurityTriageServiceUnderTest() {
        this.assessmentConverter = new BeanOutputConverter<>(VulnerabilityAssessment.class);
    }

    /**
     * Scenario 1: Direct record extraction from raw JSON.
     * Invariants: rawJson must not be null or blank.
     * Must deserialize raw JSON cleanly into VulnerabilityAssessment.
     */
    public VulnerabilityAssessment parseVulnerabilityAssessment(String rawJson) {
        // DEFECT (Scenario 1): Missing boundary checks and returns null.
        return null;
    }

    /**
     * Scenario 2: Defensive markdown fence stripping & conversational preamble sanitization.
     * Real LLMs wrap JSON inside conversational text and markdown fences:
     * ```json
     * { ... }
     * ```
     * Must sanitize input by extracting the pure JSON substring from first '{' to last '}'
     * (or stripping fences) before delegating to assessmentConverter.
     */
    public VulnerabilityAssessment extractFromMarkdownFences(String markdownPayload) {
        // DEFECT (Scenario 2): Passes raw markdown to parser without stripping fences, causing parse failure.
        return assessmentConverter.convert(markdownPayload);
    }

    /**
     * Scenario 3: Generic collections extraction via ParameterizedTypeReference.
     * Invariants: jsonArrayPayload must not be null or blank.
     * Must use new BeanOutputConverter<>(new ParameterizedTypeReference<List<PatchPlan>>() {})
     * to deserialize JSON array into a strongly-typed List<PatchPlan>.
     */
    public List<PatchPlan> extractPatchPlanList(String jsonArrayPayload) {
        // DEFECT (Scenario 3): Returns empty list instead of parsing with ParameterizedTypeReference.
        return List.of();
    }

    /**
     * Scenario 4: Type-safe ChatClient execution via .entity(VulnerabilityAssessment.class).
     * Invariants: client and advisoryText must not be null or blank.
     * Calls client.prompt().user(advisoryText).call().entity(VulnerabilityAssessment.class).
     */
    public VulnerabilityAssessment generateAssessmentWithSchema(ChatClient client, String advisoryText) {
        // DEFECT (Scenario 4): Calls .content() as raw string and returns null.
        return null;
    }

    /**
     * Scenario 5: TigerStyle domain invariant validation.
     * Enforces boundaries on VulnerabilityAssessment:
     * - cveId must match ^CVE-\d{4}-\d{4,7}$
     * - cvssScore must be between 0.0 and 10.0 inclusive
     * - packageCoordinate must contain ':'
     * - rootCauseAnalysis must not be blank
     * - mitigationSteps must not be null or empty
     * Throws IllegalArgumentException on any violation.
     */
    public void validateRecordInvariants(VulnerabilityAssessment assessment) {
        // DEFECT (Scenario 5): No-op; fails to validate invariants.
    }

    /**
     * Scenario 6: Safe deserializer with diagnostic envelope (TriageEnvelope).
     * Invariants: jsonPayload must not be null or blank.
     * Attempts to parse JSON into VulnerabilityAssessment.
     * If valid: returns TriageEnvelope.accepted(assessment).
     * If parsing fails or fields are invalid: catches exception and returns
     * TriageEnvelope.rejected(exception.getMessage()).
     * MUST NOT let parsing or validation exceptions escape to the caller!
     */
    public TriageEnvelope<VulnerabilityAssessment> safeDeserializeWithFallback(String jsonPayload) {
        // DEFECT (Scenario 6): Directly calls converter without try-catch, letting exceptions crash caller.
        return TriageEnvelope.accepted(assessmentConverter.convert(jsonPayload));
    }

    /**
     * Scenario 7: One-shot feedback schema repair loop (Saga DECIDE pattern).
     * Invariants: client and cveIdentifier must not be null or blank.
     * <p>
     * Protocol:
     * 1. Attempt 1: Call client with "Analyze vulnerability for: " + cveIdentifier.
     * Extract and clean/parse JSON response with assessmentConverter.
     * 2. If attempt 1 fails due to parsing or schema exception:
     * Call client a second time with feedback prompt:
     * "Previous output failed schema validation: " + e.getMessage() + ". Please output strictly valid JSON conforming to schema:"
     * Clean and parse second response with assessmentConverter.
     * 3. Return the parsed VulnerabilityAssessment.
     */
    public VulnerabilityAssessment repairWithOneShotRetry(ChatClient client, String cveIdentifier) {
        // DEFECT (Scenario 7): Calls client once and throws without attempting feedback repair.
        client.prompt().user("Analyze vulnerability for: " + cveIdentifier).call().content();
        throw new IllegalStateException("Simulated initial failure");
    }

    /**
     * Scenario 8: Enforce strict retry limit (prevent runaway infinite repair loops).
     * When both attempt 1 and attempt 2 fail:
     * MUST NOT retry a 3rd time!
     * Must throw IllegalStateException("Schema repair exceeded maximum attempts (1)").
     */
    public VulnerabilityAssessment enforceRetryLimit(ChatClient client, String cveIdentifier) {
        // DEFECT (Scenario 8): Returns null without enforcing retry limit.
        return null;
    }
}
