package phase08.repository;

import org.springframework.stereotype.Repository;
import phase08.model.TrajectoryEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Phase 08: Thread-Safe In-Memory Trajectory Audit Repository.
 * <p>
 * Persists and retrieves chronological trajectory audit events by incidentId.
 */
@Repository
public class InMemoryTrajectoryRepository implements TrajectoryRepository {

    private final Map<String, List<TrajectoryEvent>> store = new ConcurrentHashMap<>();

    /**
     * Appends an audit event to the incident's trajectory history.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If incidentId is null or blank, throw IllegalArgumentException("incidentId cannot be blank").
     *   - If event is null, throw IllegalArgumentException("event cannot be null").
     * - Persistence:
     *   - store.computeIfAbsent(incidentId, k -> new CopyOnWriteArrayList<>()).add(event).
     */
    @Override
    public void save(String incidentId, TrajectoryEvent event) {
        throw new UnsupportedOperationException("TODO: Implement save");
    }

    /**
     * Queries all trajectory audit events recorded for an incident.
     * <p>
     * Instructions:
     * - Parameter Check:
     *   - If incidentId is null, return List.of().
     * - Query:
     *   - Retrieve list for incidentId from store.
     *   - If list is null, return List.of().
     *   - Otherwise return Collections.unmodifiableList(list).
     */
    @Override
    public List<TrajectoryEvent> findByIncidentId(String incidentId) {
        throw new UnsupportedOperationException("TODO: Implement findByIncidentId");
    }
}
