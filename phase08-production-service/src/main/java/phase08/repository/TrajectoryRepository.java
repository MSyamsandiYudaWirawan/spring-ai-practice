package phase08.repository;

import phase08.model.TrajectoryEvent;

import java.util.List;

/**
 * Repository contract for persisting and retrieving agent trajectory audit events.
 */
public interface TrajectoryRepository {
    void save(String incidentId, TrajectoryEvent event);
    List<TrajectoryEvent> findByIncidentId(String incidentId);
}
