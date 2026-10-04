package phase08.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import phase08.testdouble.FakeProductionChatModel;

import static org.assertj.core.api.Assertions.assertThat;

class AgentHealthIndicatorTest {

    @Test
    @DisplayName("HealthIndicator should return UP when model is responsive")
    void testHealthUp() {
        FakeProductionChatModel model = new FakeProductionChatModel();
        AgentHealthIndicator indicator = new AgentHealthIndicator(model, "production-llm");

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("model", "production-llm");
        assertThat(health.getDetails()).containsEntry("status", "AVAILABLE");
    }

    @Test
    @DisplayName("HealthIndicator should return DOWN when upstream model fails")
    void testHealthDownOnFailure() {
        FakeProductionChatModel model = new FakeProductionChatModel();
        model.setFailOnCall(true);
        AgentHealthIndicator indicator = new AgentHealthIndicator(model, "production-llm");

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("model", "production-llm");
        assertThat(health.getDetails()).containsEntry("status", "UNAVAILABLE");
    }
}
