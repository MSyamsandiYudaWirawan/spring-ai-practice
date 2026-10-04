package phase07;

import org.springframework.ai.chat.client.ChatClient;
import phase07.SagaContracts.*;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Service under test — THE ONLY FILE YOU MODIFY for Phase 07 Exercise 01.
 * <p>
 * Practice implementing the in-memory Saga agent loop state machine, virtual workspace,
 * compensatory rollback, noise floor gating, budget circuit-breakers, and trajectory logging:
 * 1. StateTransitionEngine (State machine validation)
 * 2. VirtualWorkspaceMemory (In-memory atomic git-like tree)
 * 3. DecideProposalExtractor (DECIDE schema extraction with 1-shot repair)
 * 4. ApplyAndCompensateStep (APPLY phase with compensation rollback)
 * 5. NoiseFloorKeepGate (MEASURE & JUDGE telemetry gate)
 * 6. IterationCapCircuitBreaker (Turn counter safety cut-off)
 * 7. TokenBudgetGuardrail (Token ceiling circuit breaker)
 * 8. TrajectoryAuditRecorder (Trajectory event auditing)
 * 9. CheckpointRecoveryManager (Save & resume state recovery)
 * 10. AutonomousSagaLoopRunner (End-to-end autonomous Saga orchestrator)
 */
public class SagaLoopUnderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Scenario 1: State Machine Transition Engine.
     * <p>
     * Instructions:
     * - Maintain current SagaState (starts at IDLE).
     * - transitionTo(newState):
     *   - Valid transitions:
     *     IDLE -> DECIDE
     *     DECIDE -> APPLY
     *     DECIDE -> WASTED (stay in loop, goes to DECIDE or FINISH/ABORTED)
     *     APPLY -> MEASURE
     *     APPLY -> DECIDE (on compensation revert)
     *     MEASURE -> JUDGE
     *     JUDGE -> DECIDE (next iteration)
     *     JUDGE -> FINISH (goal accomplished)
     *     ANY state -> ABORTED (guardrail breach or kill)
     *     ANY state -> FINISH
     *   - If transition is invalid, throw IllegalStateException("Invalid transition from " + currentState + " to " + newState).
     */
    public static class StateTransitionEngine {
        private SagaState state = SagaState.IDLE;

        public SagaState getState() {
            return state;
        }

        public void transitionTo(SagaState newState) {
            // DEFECT (Scenario 1): Blindly sets state without validating allowed transitions
            this.state = newState;
        }
    }

    /**
     * Scenario 2: In-Memory Virtual Workspace (Git-like Commit and Rollback).
     * <p>
     * Instructions:
     * - Maintain Map<String, String> currentFiles (path -> content).
     * - Maintain Map<String, Map<String, String>> commitHistory (sha -> file snapshot).
     * - headSha starts at initialSha ("sha-0").
     * - commit(sha, files): saves snapshot in commitHistory, updates currentFiles, sets headSha = sha.
     * - revertTo(sha): restores currentFiles from commitHistory.get(sha), sets headSha = sha.
     *   If sha not in commitHistory, throws IllegalArgumentException("Commit sha not found: " + sha).
     * - writeFile(path, content): updates currentFiles.
     * - readFile(path): returns content or null.
     */
    public static class VirtualWorkspaceMemory implements VirtualWorkspace {
        private String headSha;
        private final Map<String, String> currentFiles = new HashMap<>();
        private final Map<String, Map<String, String>> commitHistory = new HashMap<>();

        public VirtualWorkspaceMemory(String initialSha, Map<String, String> initialFiles) {
            this.headSha = Objects.requireNonNull(initialSha, "initialSha must not be null");
            if (initialFiles != null) {
                this.currentFiles.putAll(initialFiles);
            }
            this.commitHistory.put(initialSha, new HashMap<>(this.currentFiles));
        }

        @Override
        public String getHeadSha() {
            return headSha;
        }

        @Override
        public void commit(String sha, Map<String, String> files) {
            // DEFECT (Scenario 2): Doesn't update commitHistory
            this.headSha = sha;
        }

        @Override
        public void revertTo(String sha) {
            // DEFECT (Scenario 2): Revert does not restore files
            this.headSha = sha;
        }

        @Override
        public Map<String, String> getFiles() {
            return Collections.unmodifiableMap(currentFiles);
        }

        @Override
        public String readFile(String path) {
            return currentFiles.get(path);
        }

        @Override
        public void writeFile(String path, String content) {
            currentFiles.put(path, content);
        }
    }

    /**
     * Scenario 3: DECIDE Phase Structured Proposal Extractor with One-Shot Repair.
     * <p>
     * Instructions:
     * - Call chatClient with prompt.
     * - Parse JSON into DecisionProposal.
     * - Strip markdown fences (```json ... ```).
     * - If JSON parsing fails:
     *   - Attempt exactly ONE retry with feedback prompt:
     *     "Previous proposal was invalid JSON: " + error + ". Please output valid JSON matching schema."
     *   - If retry succeeds, return Optional.of(repairedProposal).
     *   - If retry also fails, return Optional.empty() (marks turn as WASTED).
     */
    public static class DecideProposalExtractor {
        public static Optional<DecisionProposal> extractProposal(ChatClient chatClient, String promptText) {
            // DEFECT (Scenario 3): Returns empty optional without calling model or attempting repair
            return Optional.empty();
        }
    }

    /**
     * Scenario 4: APPLY Phase with Compensatory Rollback on Failure.
     * <p>
     * Instructions:
     * - Apply proposal's file modification to workspace:
     *   workspace.writeFile(proposal.fileToModify(), proposal.patchContent()).
     * - Run buildCheck.test(workspace.getFiles()).
     * - If build succeeds:
     *   return IterationOutcome.KEPT.
     * - If build fails (compilation/syntax error):
     *   call workspace.revertTo(lastKeptSha).
     *   return IterationOutcome.REVERTED.
     */
    public static class ApplyAndCompensateStep {
        public static IterationOutcome applyChange(
                VirtualWorkspace workspace,
                DecisionProposal proposal,
                String lastKeptSha,
                Predicate<Map<String, String>> buildCheck
        ) {
            // DEFECT (Scenario 4): Does not revert workspace on build failure
            workspace.writeFile(proposal.fileToModify(), proposal.patchContent());
            return IterationOutcome.KEPT;
        }
    }

    /**
     * Scenario 5: MEASURE & JUDGE Telemetry Keep Gate.
     * <p>
     * Instructions:
     * - rpsImprove = candidate.rps() - baseline.rps()
     * - p95Improve = baseline.p95Ms() - candidate.p95Ms()
     * - rpsImproved = rpsImprove > floors.rpsFloor()
     * - p95Improved = p95Improve > floors.p95FloorMs()
     * - failGuard = candidate.failRate() <= baseline.failRate()
     * - If (rpsImproved || p95Improved) && failGuard:
     *   workspace.commit(newSha, workspace.getFiles());
     *   return IterationOutcome.KEPT;
     * - Else:
     *   workspace.revertTo(lastKeptSha);
     *   return IterationOutcome.REVERTED;
     */
    public static class NoiseFloorKeepGate {
        public static IterationOutcome judgeTelemetry(
                VirtualWorkspace workspace,
                TelemetryMeasurement candidate,
                TelemetryMeasurement baseline,
                NoiseFloors floors,
                String lastKeptSha,
                String newSha
        ) {
            // DEFECT (Scenario 5): Always returns KEPT without comparing against noise floors
            return IterationOutcome.KEPT;
        }
    }

    /**
     * Scenario 6: Iteration Cap Circuit Breaker.
     * <p>
     * Instructions:
     * - checkTurnLimit(currentTurn, maxIterations):
     *   if currentTurn >= maxIterations:
     *     throw new SagaLoopBreachException("Saga guardrail breached: max iterations reached (" + maxIterations + ")");
     */
    public static class IterationCapCircuitBreaker {
        public static void checkTurnLimit(int currentTurn, int maxIterations) {
            // DEFECT (Scenario 6): Missing breach check
        }
    }

    /**
     * Scenario 7: Token Budget Ceiling Guardrail.
     * <p>
     * Instructions:
     * - Maintain cumulativeTokensUsed.
     * - addTokens(tokens):
     *   cumulativeTokensUsed += tokens;
     *   if cumulativeTokensUsed > maxTokens:
     *     throw new SagaLoopBreachException("Saga guardrail breached: token ceiling exceeded ("
     *       + cumulativeTokensUsed + " > " + maxTokens + ")");
     */
    public static class TokenBudgetGuardrail {
        private long cumulativeTokens = 0;
        private final long maxTokens;

        public TokenBudgetGuardrail(long maxTokens) {
            this.maxTokens = maxTokens;
        }

        public long getCumulativeTokens() {
            return cumulativeTokens;
        }

        public void recordUsage(long tokens) {
            // DEFECT (Scenario 7): Does not throw SagaLoopBreachException on ceiling breach
            this.cumulativeTokens += tokens;
        }
    }

    /**
     * Scenario 8: Trajectory Event Audit Recorder.
     * <p>
     * Instructions:
     * - Record TrajectoryEvent(turn, state, eventType, detail, tokensUsed).
     * - Provide unmodifiable List<TrajectoryEvent> getEvents().
     */
    public static class TrajectoryAuditRecorder {
        private final List<TrajectoryEvent> events = new CopyOnWriteArrayList<>();

        public void record(int turn, SagaState state, String eventType, String detail, long tokens) {
            // DEFECT (Scenario 8): Ignores event logging
        }

        public List<TrajectoryEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }
    }

    /**
     * Scenario 9: Checkpoint Save & Resume Recovery.
     * <p>
     * Instructions:
     * - saveCheckpoint(checkpoint): saves the snapshot.
     * - resume(workspace):
     *   workspace.revertTo(checkpoint.lastKeptSha());
     *   returns the saved LoopCheckpoint.
     */
    public static class CheckpointRecoveryManager {
        private LoopCheckpoint savedCheckpoint;

        public void saveCheckpoint(LoopCheckpoint checkpoint) {
            this.savedCheckpoint = Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        }

        public LoopCheckpoint resume(VirtualWorkspace workspace) {
            // DEFECT (Scenario 9): Does not revert workspace to lastKeptSha
            return savedCheckpoint;
        }
    }

    /**
     * Scenario 10: Autonomous Saga Agent Loop Runner.
     * <p>
     * Instructions:
     * - Run loop for up to config.maxIterations turns:
     *   - Check turn limits.
     *   - DECIDE: Extract proposal via DecideProposalExtractor. If empty -> wastedCount++, turn to next.
     *   - APPLY: ApplyChange via ApplyAndCompensateStep. If REVERTED -> revertedCount++, turn to next.
     *   - MEASURE & JUDGE: Measure telemetry via NoiseFloorKeepGate.
     *     - If KEPT: keptCount++, advance baseline telemetry and lastKeptSha.
     *     - If REVERTED: revertedCount++.
     * - On finish or breach: return LoopSummary.
     */
    public static class AutonomousSagaLoopRunner {
        public static LoopSummary executeLoop(
                VirtualWorkspace workspace,
                ChatClient chatClient,
                LoopConfig config,
                TelemetryMeasurement initialBaseline,
                List<TelemetryMeasurement> simulatedTurnTelemetry,
                Predicate<Map<String, String>> buildCheck
        ) {
            // DEFECT (Scenario 10): Returns empty summary without executing loop turns
            return new LoopSummary(0, 0, 0, 0, 0L, workspace.getHeadSha(), SagaState.IDLE);
        }
    }
}
