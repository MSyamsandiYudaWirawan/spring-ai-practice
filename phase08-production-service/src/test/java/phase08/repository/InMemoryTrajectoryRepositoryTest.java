package phase08.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import phase08.model.SagaState;
import phase08.model.TrajectoryEvent;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTrajectoryRepositoryTest {

    private final InMemoryTrajectoryRepository repo = new InMemoryTrajectoryRepository();

    @Test
    @DisplayName("Should save and retrieve trajectory events partitioned by incidentId")
    void testSaveAndFind() {
        repo.save("INC-1", new TrajectoryEvent("INC-1", 1, SagaState.DECIDE, "Decide turn 1", Instant.now()));
        repo.save("INC-1", new TrajectoryEvent("INC-1", 1, SagaState.APPLY, "Apply turn 1", Instant.now()));
        repo.save("INC-2", new TrajectoryEvent("INC-2", 1, SagaState.DECIDE, "Decide turn 1", Instant.now()));

        List<TrajectoryEvent> inc1 = repo.findByIncidentId("INC-1");
        List<TrajectoryEvent> inc2 = repo.findByIncidentId("INC-2");
        List<TrajectoryEvent> missing = repo.findByIncidentId("INC-999");

        assertThat(inc1).hasSize(2);
        assertThat(inc2).hasSize(1);
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("Saving with null or blank incidentId should throw IllegalArgumentException")
    void testInvalidSaveThrows() {
        TrajectoryEvent event = new TrajectoryEvent("INC-X", 1, SagaState.IDLE, "Init", Instant.now());
        assertThatThrownBy(() -> repo.save("", event))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
