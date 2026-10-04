package phase08.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import phase08.config.AgentProperties;
import phase08.evaluator.NoiseFloorGate;
import phase08.model.IncidentSeverity;
import phase08.model.TriageRequestDto;
import phase08.model.TriageResponseDto;
import phase08.parser.TriageProposalParser;
import phase08.prompt.TriagePromptBuilder;
import phase08.repository.InMemoryTrajectoryRepository;
import phase08.saga.AutonomousSagaLoop;
import phase08.service.IncidentTriageService;
import phase08.testdouble.FakeProductionChatModel;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentTriageControllerTest {

    private final FakeProductionChatModel model = new FakeProductionChatModel();
    private final ChatClient chatClient = ChatClient.builder(model).build();
    private final InMemoryTrajectoryRepository repo = new InMemoryTrajectoryRepository();
    private final AgentProperties props = AgentProperties.defaults();

    private final AutonomousSagaLoop sagaLoop = new AutonomousSagaLoop(
            chatClient, null, new TriagePromptBuilder(), new TriageProposalParser(),
            new NoiseFloorGate(), repo, props
    );

    private final IncidentTriageService service = new IncidentTriageService(sagaLoop, repo, props);
    private final IncidentTriageController controller = new IncidentTriageController(service);

    @Test
    @DisplayName("Should return 400 Bad Request when incidentId is blank")
    void testBlankIncidentIdReturnsBadRequest() {
        TriageRequestDto bad = new TriageRequestDto("", "billing-svc", "Crash", IncidentSeverity.HIGH);
        ResponseEntity<TriageResponseDto> resp = controller.triageIncident(bad);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should return 200 OK with TriageResponseDto for valid incident request")
    void testValidRequestReturnsOk() {
        TriageRequestDto req = new TriageRequestDto("INC-WEB-1", "billing-svc", "Hikari lock contention", IncidentSeverity.CRITICAL);
        ResponseEntity<TriageResponseDto> resp = controller.triageIncident(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().incidentId()).isEqualTo("INC-WEB-1");
        assertThat(resp.getBody().status()).isEqualTo("RESOLVED");
    }
}
