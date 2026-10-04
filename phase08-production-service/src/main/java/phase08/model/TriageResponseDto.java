package phase08.model;

/**
 * REST API response payload after incident triage completion.
 */
public record TriageResponseDto(
        String incidentId,
        String rootCause,
        String status,
        int iterationsUsed,
        int tokensUsed,
        String headSha
) {}
