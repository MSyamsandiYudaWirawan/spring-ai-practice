package phase08.model;

import java.time.Instant;

/**
 * Audit trajectory record captured during Saga execution.
 */
public record TrajectoryEvent(
        String incidentId,
        int iteration,
        SagaState state,
        String summary,
        Instant timestamp
) {}
