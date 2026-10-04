package phase08.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import phase08.config.AgentProperties;
import phase08.evaluator.NoiseFloorGate;
import phase08.model.DiagnosticTelemetry;
import phase08.model.TriageResponseDto;
import phase08.parser.TriageProposalParser;
import phase08.prompt.TriagePromptBuilder;
import phase08.repository.InMemoryTrajectoryRepository;
import phase08.testdouble.FakeProductionChatModel;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AutonomousSagaLoopTest {

    @Test
    @DisplayName("Should run Saga loop, apply proposal, evaluate telemetry, and commit resolution")
    void testAutonomousSagaResolution() {
        FakeProductionChatModel model = new FakeProductionChatModel();
        ChatClient client = ChatClient.builder(model).build();
        InMemoryTrajectoryRepository repo = new InMemoryTrajectoryRepository();
        AgentProperties props = AgentProperties.defaults();

        AutonomousSagaLoop saga = new AutonomousSagaLoop(
                client,
                null,
                new TriagePromptBuilder(),
                new TriageProposalParser(),
                new NoiseFloorGate(),
                repo,
                props
        );

        VirtualWorkspace workspace = new VirtualWorkspace("sha-0", Map.of("application.properties", "hikari.max=10"));

        TriageResponseDto response = saga.runLoop(
                "INC-TEST-1",
                "payment-service",
                "High latency on db calls",
                workspace,
                proposal -> new DiagnosticTelemetry(200.0, 180.0, 0.01) // Clears noise floor
        );

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(response.headSha()).isEqualTo("sha-1");
        assertThat(workspace.getHeadSha()).isEqualTo("sha-1");

        // Verify trajectory audit events were recorded
        var events = repo.findByIncidentId("INC-TEST-1");
        assertThat(events).isNotEmpty();
    }
}
