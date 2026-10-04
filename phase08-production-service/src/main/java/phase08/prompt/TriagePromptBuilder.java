package phase08.prompt;

import phase08.model.DiagnosticTelemetry;

/**
 * Phase 01: Dynamic Prompt Construction & SRE Context Templating.
 * <p>
 * Synthesizes incident parameters, target microservice identification,
 * observed symptoms, and baseline telemetry into a structured prompt.
 */
public class TriagePromptBuilder {

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are an automated production incident triage agent. Diagnose root cause and propose atomic fixes.";

    /**
     * Builds structured user prompt incorporating incident details and telemetry.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If incidentId is null or blank, throw IllegalArgumentException("incidentId cannot be blank").
     *   - If serviceName is null or blank, throw IllegalArgumentException("serviceName cannot be blank").
     *   - If description is null or blank, throw IllegalArgumentException("description cannot be blank").
     *   - If telemetry is null, throw NullPointerException("telemetry cannot be null").
     * - Formatting:
     *   - Format using exact multi-line template:
     *     "Incident: %s | Service: %s%nDescription: %s%nCurrent Telemetry: p95=%.1fms, rps=%.1f, err=%.2f%nPropose a concrete JSON triage proposal."
     *   - where parameters are: incidentId, serviceName, description, telemetry.p95Ms(), telemetry.rps(), telemetry.errorRate().
     */
    public String buildUserPrompt(String incidentId, String serviceName, String description, DiagnosticTelemetry telemetry) {
        throw new UnsupportedOperationException("TODO: Implement buildUserPrompt");
    }
}
