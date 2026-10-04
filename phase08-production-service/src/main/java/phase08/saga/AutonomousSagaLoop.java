package phase08.saga;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import phase08.config.AgentProperties;
import phase08.evaluator.NoiseFloorGate;
import phase08.model.*;
import phase08.parser.TriageProposalParser;
import phase08.prompt.TriagePromptBuilder;
import phase08.repository.TrajectoryRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

/**
 * Phase 07: Autonomous Saga Agent Loop & State Machine.
 * <p>
 * Orchestrates the 6-phase Saga FSM:
 * IDLE -> DECIDE -> APPLY -> MEASURE -> JUDGE -> COMPENSATE/FINISH.
 */
public class AutonomousSagaLoop {

    private final ChatClient chatClient;
    private final ToolCallback[] tools;
    private final TriagePromptBuilder promptBuilder;
    private final TriageProposalParser parser;
    private final NoiseFloorGate noiseFloorGate;
    private final TrajectoryRepository repository;
    private final AgentProperties properties;

    public AutonomousSagaLoop(
            ChatClient chatClient,
            ToolCallback[] tools,
            TriagePromptBuilder promptBuilder,
            TriageProposalParser parser,
            NoiseFloorGate noiseFloorGate,
            TrajectoryRepository repository,
            AgentProperties properties
    ) {
        this.chatClient = chatClient;
        this.tools = tools != null ? tools : new ToolCallback[0];
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.noiseFloorGate = noiseFloorGate;
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Executes the autonomous multi-turn Saga loop until resolution or iteration exhaustion.
     * <p>
     * Instructions:
     * - Initialization:
     *   - String lastKeptSha = workspace.getHeadSha().
     *   - DiagnosticTelemetry currentBaseline = DiagnosticTelemetry.baseline().
     *   - int iteration = 0.
     *   - String finalStatus = "UNRESOLVED".
     *   - String rootCause = "UNKNOWN".
     *   - Record event: recordEvent(incidentId, 0, SagaState.IDLE, "Loop initialized at sha=" + lastKeptSha).
     * - Loop Condition: while (iteration < properties.maxIterations()):
     *   - iteration++.
     *   - Record event: recordEvent(incidentId, iteration, SagaState.DECIDE, "Starting decision turn " + iteration).
     *   - 1. DECIDE:
     *     - Build prompt using promptBuilder.buildUserPrompt(incidentId, serviceName, description, currentBaseline).
     *     - Call chatClient.prompt().user(prompt).tools((Object[]) tools).call().content().
     *     - Parse response using parser.parseWithOneShotRepair(rawResponse, repairPrompt -> chatClient.prompt().user(repairPrompt).call().content()).
     *     - If parsing returns empty Optional:
     *       - recordEvent(incidentId, iteration, SagaState.DECIDE, "Schema parse failed after 1-shot repair; skipping").
     *       - continue to next iteration.
     *     - Extract TriageProposal proposal. Set rootCause = proposal.hypothesis().
     *   - 2. APPLY:
     *     - recordEvent(incidentId, iteration, SagaState.APPLY, "Applying proposal: " + proposal.action()).
     *     - If proposal.targetFile() and proposal.patchContent() are not null, call workspace.writeFile(...).
     *   - 3. MEASURE:
     *     - recordEvent(incidentId, iteration, SagaState.MEASURE, "Harvesting telemetry for proposal").
     *     - DiagnosticTelemetry candidateTelemetry = telemetrySupplier != null ? telemetrySupplier.apply(proposal) : currentBaseline.
     *   - 4. JUDGE:
     *     - recordEvent(incidentId, iteration, SagaState.JUDGE, "Evaluating telemetry delta").
     *     - NoiseFloorGate.KeepDecision verdict = noiseFloorGate.evaluate(currentBaseline, candidateTelemetry, properties.p95FloorMs(), properties.rpsFloor()).
     *     - If verdict.keep():
     *       - String newSha = "sha-" + iteration.
     *       - workspace.commit(newSha, workspace.getFiles()).
     *       - lastKeptSha = newSha.
     *       - currentBaseline = candidateTelemetry.
     *       - finalStatus = "RESOLVED".
     *       - recordEvent(incidentId, iteration, SagaState.FINISH, "Decision KEPT, committed " + newSha + ": " + verdict.reason()).
     *       - break loop!
     *     - Else (verdict rejected):
     *       - 5. COMPENSATE:
     *       - recordEvent(incidentId, iteration, SagaState.COMPENSATE, "Decision REJECTED, rolling back to " + lastKeptSha).
     *       - workspace.revertTo(lastKeptSha).
     * - Termination:
     *   - recordEvent(incidentId, iteration, SagaState.FINISH, "Loop terminated with status " + finalStatus).
     *   - Return new TriageResponseDto(incidentId, rootCause, finalStatus, iteration, 0, workspace.getHeadSha()).
     */
    public TriageResponseDto runLoop(
            String incidentId,
            String serviceName,
            String description,
            VirtualWorkspace workspace,
            Function<TriageProposal, DiagnosticTelemetry> telemetrySupplier
    ) {
        throw new UnsupportedOperationException("TODO: Implement runLoop");
    }

    private void recordEvent(String incidentId, int iteration, SagaState state, String summary) {
        if (repository != null) {
            repository.save(incidentId, new TrajectoryEvent(incidentId, iteration, state, summary, Instant.now()));
        }
    }
}
