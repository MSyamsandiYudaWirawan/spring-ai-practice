package phase08.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import phase08.Application;
import phase08.config.AgentProperties;
import phase08.model.IncidentSeverity;
import phase08.model.SagaState;
import phase08.model.TrajectoryEvent;
import phase08.model.TriageRequestDto;
import phase08.model.TriageResponseDto;
import phase08.repository.TrajectoryRepository;
import phase08.testdouble.FakeProductionChatModel;
import phase08.web.IncidentTriageController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Spring Boot Integration Test.
 * Tests full DI assembly, Advisors, Tools, Saga State Machine, and Controller dispatch.
 */
@SpringBootTest(classes = {Application.class, IncidentTriageEndToEndIntegrationTest.TestConfig.class})
class IncidentTriageEndToEndIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ChatModel fakeChatModel() {
            return new FakeProductionChatModel();
        }
    }

    @Autowired
    private IncidentTriageController controller;

    @Autowired
    private TrajectoryRepository repository;

    @Autowired
    private AgentProperties properties;

    @Test
    @DisplayName("Full End-to-End Pipeline: REST Controller -> Advisors -> Tools -> Saga Loop -> Repository")
    void testEndToEndPipeline() {
        TriageRequestDto request = new TriageRequestDto(
                "INC-E2E-999",
                "payment-service",
                "Deadlock and high Hikari pool acquisition wait times",
                IncidentSeverity.CRITICAL
        );

        ResponseEntity<TriageResponseDto> response = controller.triageIncident(request);

        // 1. Verify HTTP Response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().incidentId()).isEqualTo("INC-E2E-999");
        assertThat(response.getBody().status()).isEqualTo("RESOLVED");
        assertThat(response.getBody().headSha()).isEqualTo("sha-1");

        // 2. Verify Trajectory Audit in Repository
        List<TrajectoryEvent> events = repository.findByIncidentId("INC-E2E-999");
        assertThat(events).isNotEmpty();

        List<SagaState> states = events.stream().map(TrajectoryEvent::state).toList();
        assertThat(states)
                .contains(SagaState.IDLE)
                .contains(SagaState.DECIDE)
                .contains(SagaState.APPLY)
                .contains(SagaState.MEASURE)
                .contains(SagaState.JUDGE)
                .contains(SagaState.FINISH);
    }
}
