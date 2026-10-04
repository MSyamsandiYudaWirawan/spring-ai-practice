package phase08.model;

/**
 * Inbound REST API request payload for incident triage.
 */
public record TriageRequestDto(
        String incidentId,
        String serviceName,
        String description,
        IncidentSeverity severity
) {}
