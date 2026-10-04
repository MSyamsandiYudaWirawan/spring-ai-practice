# Golden Solution: Phase 07 Exercise 01 (In-Memory Saga Agent Loop & State Machine)

## Overview
This golden solution implements all 10 scenarios of `phase07-ex01-saga-loop`, providing complete in-memory implementations of the Saga Agent Loop lifecycle, virtual workspace with atomic git-like commit/rollback, schema parsing with one-shot repair, build failure compensation, noise floor threshold evaluation, iteration and token guardrails, trajectory auditing, checkpoint recovery, and end-to-end loop orchestration.

Verified: `10 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase07-ex01-saga-loop/src/main/java/phase07/SagaLoopUnderTest.java`

```java
package phase07;

import org.springframework.ai.chat.client.ChatClient;
import phase07.SagaContracts.*;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Golden implementation for Phase 07 Exercise 01.
 */
public class SagaLoopUnderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Scenario 1: State Machine Transition Engine.
     */
    public static class StateTransitionEngine {
        private SagaState state = SagaState.IDLE;

        public SagaState getState() {
            return state;
        }

        public void transitionTo(SagaState newState) {
            if (newState == SagaState.ABORTED || newState == SagaState.FINISH) {
                this.state = newState;
                return;
            }

            boolean valid = switch (state) {
                case IDLE -> newState == SagaState.DECIDE;
                case DECIDE -> newState == SagaState.APPLY || newState == SagaState.DECIDE;
                case APPLY -> newState == SagaState.MEASURE || newState == SagaState.DECIDE;
                case MEASURE -> newState == SagaState.JUDGE;
                case JUDGE -> newState == SagaState.DECIDE || newState == SagaState.FINISH;
                default -> false;
            };

            if (!valid) {
                throw new IllegalStateException("Invalid transition from " + state + " to " + newState);
            }
            this.state = newState;
        }
    }

    /**
     * Scenario 2: In-Memory Virtual Workspace (Git-like Commit and Rollback).
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
            this.headSha = Objects.requireNonNull(sha, "sha must not be null");
            if (files != null) {
                this.currentFiles.clear();
                this.currentFiles.putAll(files);
            }
            this.commitHistory.put(sha, new HashMap<>(this.currentFiles));
        }

        @Override
        public void revertTo(String sha) {
            Map<String, String> snapshot = commitHistory.get(sha);
            if (snapshot == null) {
                throw new IllegalArgumentException("Commit sha not found: " + sha);
            }
            this.currentFiles.clear();
            this.currentFiles.putAll(snapshot);
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
     */
    public static class DecideProposalExtractor {
        public static Optional<DecisionProposal> extractProposal(ChatClient chatClient, String promptText) {
            String raw = chatClient.prompt().user(promptText).call().content();
            Optional<DecisionProposal> parsed = parseJson(raw);
            if (parsed.isPresent()) {
                return parsed;
            }

            // Exactly ONE retry with feedback
            String repairPrompt = "Previous proposal was invalid JSON. Please output strictly valid JSON matching schema.";
            String retryRaw = chatClient.prompt().user(repairPrompt).call().content();
            return parseJson(retryRaw);
        }

        private static Optional<DecisionProposal> parseJson(String text) {
            if (text == null || text.isBlank()) return Optional.empty();
            String clean = sanitizeFences(text);
            try {
                DecisionProposal p = MAPPER.readValue(clean, DecisionProposal.class);
                return Optional.of(p);
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        private static String sanitizeFences(String text) {
            int firstBrace = text.indexOf('{');
            int lastBrace = text.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return text.substring(firstBrace, lastBrace + 1);
            }
            return text.trim();
        }
    }

    /**
     * Scenario 4: APPLY Phase with Compensatory Rollback on Failure.
     */
    public static class ApplyAndCompensateStep {
        public static IterationOutcome applyChange(
                VirtualWorkspace workspace,
                DecisionProposal proposal,
                String lastKeptSha,
                Predicate<Map<String, String>> buildCheck
        ) {
            workspace.writeFile(proposal.fileToModify(), proposal.patchContent());
            boolean buildOk = buildCheck.test(workspace.getFiles());
            if (!buildOk) {
                workspace.revertTo(lastKeptSha);
                return IterationOutcome.REVERTED;
            }
            return IterationOutcome.KEPT;
        }
    }

    /**
     * Scenario 5: MEASURE & JUDGE Telemetry Keep Gate.
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
            double p95Improve = baseline.p95Ms() - candidate.p95Ms();
            double rpsImprove = candidate.rps() - baseline.rps();

            boolean rpsImproved = rpsImprove > floors.rpsFloor();
            boolean p95Improved = p95Improve > floors.p95FloorMs();
            boolean failGuard = candidate.failRate() <= baseline.failRate();

            if ((rpsImproved || p95Improved) && failGuard) {
                workspace.commit(newSha, workspace.getFiles());
                return IterationOutcome.KEPT;
            } else {
                workspace.revertTo(lastKeptSha);
                return IterationOutcome.REVERTED;
            }
        }
    }

    /**
     * Scenario 6: Iteration Cap Circuit Breaker.
     */
    public static class IterationCapCircuitBreaker {
        public static void checkTurnLimit(int currentTurn, int maxIterations) {
            if (currentTurn >= maxIterations) {
                throw new SagaLoopBreachException("Saga guardrail breached: max iterations reached (" + maxIterations + ")");
            }
        }
    }

    /**
     * Scenario 7: Token Budget Ceiling Guardrail.
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
            this.cumulativeTokens += tokens;
            if (cumulativeTokens > maxTokens) {
                throw new SagaLoopBreachException("Saga guardrail breached: token ceiling exceeded ("
                        + cumulativeTokens + " > " + maxTokens + ")");
            }
        }
    }

    /**
     * Scenario 8: Trajectory Event Audit Recorder.
     */
    public static class TrajectoryAuditRecorder {
        private final List<TrajectoryEvent> events = new CopyOnWriteArrayList<>();

        public void record(int turn, SagaState state, String eventType, String detail, long tokens) {
            events.add(new TrajectoryEvent(turn, state, eventType, detail, tokens));
        }

        public List<TrajectoryEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }
    }

    /**
     * Scenario 9: Checkpoint Save & Resume Recovery.
     */
    public static class CheckpointRecoveryManager {
        private LoopCheckpoint savedCheckpoint;

        public void saveCheckpoint(LoopCheckpoint checkpoint) {
            this.savedCheckpoint = Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        }

        public LoopCheckpoint resume(VirtualWorkspace workspace) {
            if (savedCheckpoint != null) {
                workspace.revertTo(savedCheckpoint.lastKeptSha());
            }
            return savedCheckpoint;
        }
    }

    /**
     * Scenario 10: Autonomous Saga Agent Loop Runner.
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
            int kept = 0;
            int reverted = 0;
            int wasted = 0;
            long totalTokens = 0;
            String lastKeptSha = workspace.getHeadSha();
            TelemetryMeasurement currentBaseline = initialBaseline;
            StateTransitionEngine state = new StateTransitionEngine();

            int turns = Math.min(config.maxIterations(), simulatedTurnTelemetry.size());
            for (int t = 0; t < turns; t++) {
                int turnNum = t + 1;
                state.transitionTo(SagaState.DECIDE);

                // DECIDE
                Optional<DecisionProposal> proposalOpt = DecideProposalExtractor.extractProposal(chatClient, "propose turn " + turnNum);
                totalTokens += 200; // Simulated turn tokens

                if (proposalOpt.isEmpty()) {
                    wasted++;
                    continue;
                }

                DecisionProposal proposal = proposalOpt.get();
                state.transitionTo(SagaState.APPLY);

                // APPLY
                IterationOutcome applyOutcome = ApplyAndCompensateStep.applyChange(
                        workspace, proposal, lastKeptSha, buildCheck
                );

                if (applyOutcome == IterationOutcome.REVERTED) {
                    reverted++;
                    continue;
                }

                state.transitionTo(SagaState.MEASURE);
                state.transitionTo(SagaState.JUDGE);

                // MEASURE & JUDGE
                TelemetryMeasurement turnCandidate = simulatedTurnTelemetry.get(t);
                String candidateSha = "sha-" + turnNum;
                IterationOutcome judgeOutcome = NoiseFloorKeepGate.judgeTelemetry(
                        workspace, turnCandidate, currentBaseline, config.noiseFloors(), lastKeptSha, candidateSha
                );

                if (judgeOutcome == IterationOutcome.KEPT) {
                    kept++;
                    lastKeptSha = candidateSha;
                    currentBaseline = turnCandidate;
                } else {
                    reverted++;
                }
            }

            state.transitionTo(SagaState.FINISH);
            return new LoopSummary(turns, kept, reverted, wasted, totalTokens, lastKeptSha, state.getState());
        }
    }
}
```
