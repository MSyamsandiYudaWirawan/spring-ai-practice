package phase07;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import phase07.SagaContracts.*;
import phase07.SagaLoopUnderTest.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 07 Exercise 01:
 * In-Memory Saga Agent Loop, State Machine, Compensation & Trajectory Auditing (10 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

    public static void main(String[] args) {
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selectedScenario);
        printReport(results);

        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            fail("Verifier detected scenario failures. See printed report above.");
        }
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        List<ScenarioResult> list = new ArrayList<>();
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "StateTransitionEngine (State Machine Validation)";
        try {
            StateTransitionEngine engine = new StateTransitionEngine();
            if (engine.getState() != SagaState.IDLE) {
                return new ScenarioResult(1, name, false, "Initial state should be IDLE, got: " + engine.getState());
            }

            engine.transitionTo(SagaState.DECIDE);
            engine.transitionTo(SagaState.APPLY);
            engine.transitionTo(SagaState.MEASURE);
            engine.transitionTo(SagaState.JUDGE);
            engine.transitionTo(SagaState.DECIDE);
            engine.transitionTo(SagaState.ABORTED);

            // Re-test invalid transition: IDLE directly to JUDGE
            StateTransitionEngine invalidEngine = new StateTransitionEngine();
            try {
                invalidEngine.transitionTo(SagaState.JUDGE);
                return new ScenarioResult(1, name, false, "Expected IllegalStateException for invalid transition IDLE -> JUDGE");
            } catch (IllegalStateException expected) {}

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "VirtualWorkspaceMemory (In-Memory Atomic Git-like Tree)";
        try {
            Map<String, String> initFiles = Map.of("src/App.java", "public class App { int v = 1; }");
            VirtualWorkspace workspace = new VirtualWorkspaceMemory("sha-0", initFiles);

            if (!"sha-0".equals(workspace.getHeadSha())) {
                return new ScenarioResult(2, name, false, "Expected initial sha-0, got: " + workspace.getHeadSha());
            }

            workspace.writeFile("src/App.java", "public class App { int v = 2; }");
            workspace.commit("sha-1", workspace.getFiles());

            if (!"sha-1".equals(workspace.getHeadSha()) || !workspace.readFile("src/App.java").contains("v = 2")) {
                return new ScenarioResult(2, name, false, "Failed to commit new sha-1");
            }

            workspace.revertTo("sha-0");
            if (!"sha-0".equals(workspace.getHeadSha()) || !workspace.readFile("src/App.java").contains("v = 1")) {
                return new ScenarioResult(2, name, false, "Failed to revert to sha-0 baseline");
            }

            try {
                workspace.revertTo("sha-nonexistent");
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException for unknown commit sha");
            } catch (IllegalArgumentException expected) {}

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "DecideProposalExtractor (DECIDE Schema Extraction with 1-Shot Repair)";
        try {
            FakeSagaChatModel model = new FakeSagaChatModel();
            ChatClient client = ChatClient.create(model);

            // Valid proposal
            String validJson = "{\"category\":\"INDEX\",\"fileToModify\":\"schema.sql\",\"patchContent\":\"INDEX ON orders(user_id);\",\"rationale\":\"slow queries\"}";
            model.enqueue(validJson);
            Optional<DecisionProposal> p1 = DecideProposalExtractor.extractProposal(client, "propose");
            if (p1.isEmpty() || !"INDEX".equals(p1.get().category())) {
                return new ScenarioResult(3, name, false, "Failed to extract valid proposal: " + p1);
            }

            // Malformed first attempt, valid on retry
            model.enqueue("Not valid json at all");
            model.enqueue("```json\n" + validJson + "\n```");
            Optional<DecisionProposal> p2 = DecideProposalExtractor.extractProposal(client, "propose");
            if (p2.isEmpty()) {
                return new ScenarioResult(3, name, false, "Expected 1-shot repair to succeed on second attempt");
            }

            // Malformed on both attempts -> Optional.empty()
            model.enqueue("Malformed 1");
            model.enqueue("Malformed 2");
            Optional<DecisionProposal> p3 = DecideProposalExtractor.extractProposal(client, "propose");
            if (p3.isPresent()) {
                return new ScenarioResult(3, name, false, "Expected Optional.empty() after retry exhaustion");
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "ApplyAndCompensateStep (APPLY Phase with Compensation Rollback)";
        try {
            VirtualWorkspace workspace = new VirtualWorkspaceMemory("sha-0", Map.of("App.java", "int x = 1;"));
            DecisionProposal badProposal = new DecisionProposal("BUG", "App.java", "SYNTAX_ERROR!", "bad patch");

            // Build check fails on "SYNTAX_ERROR"
            IterationOutcome outcome = ApplyAndCompensateStep.applyChange(
                    workspace, badProposal, "sha-0", files -> !files.get("App.java").contains("SYNTAX_ERROR")
            );

            if (outcome != IterationOutcome.REVERTED) {
                return new ScenarioResult(4, name, false, "Expected outcome REVERTED on build failure, got: " + outcome);
            }
            if (!workspace.readFile("App.java").contains("int x = 1;")) {
                return new ScenarioResult(4, name, false, "Workspace was not rolled back to sha-0 after build failure");
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "NoiseFloorKeepGate (MEASURE & JUDGE Telemetry Gate)";
        try {
            VirtualWorkspace workspace = new VirtualWorkspaceMemory("sha-0", Map.of("App.java", "int x = 1;"));
            workspace.writeFile("App.java", "int x = 2;"); // candidate modification
            NoiseFloors floors = new NoiseFloors(15.0, 50.0);
            TelemetryMeasurement baseline = new TelemetryMeasurement(300.0, 100.0, 0.01);

            // Test 1: RPS +70 cleared floor -> KEPT
            TelemetryMeasurement candidateGood = new TelemetryMeasurement(295.0, 170.0, 0.01);
            IterationOutcome o1 = NoiseFloorKeepGate.judgeTelemetry(workspace, candidateGood, baseline, floors, "sha-0", "sha-1");
            if (o1 != IterationOutcome.KEPT || !"sha-1".equals(workspace.getHeadSha())) {
                return new ScenarioResult(5, name, false, "Expected KEPT with commit to sha-1, got: " + o1);
            }

            // Test 2: Regression (deltas within noise floor) -> REVERTED to sha-0
            workspace.writeFile("App.java", "int x = 3;");
            TelemetryMeasurement candidateSmall = new TelemetryMeasurement(295.0, 110.0, 0.01);
            IterationOutcome o2 = NoiseFloorKeepGate.judgeTelemetry(workspace, candidateSmall, baseline, floors, "sha-1", "sha-2");
            if (o2 != IterationOutcome.REVERTED || !"sha-1".equals(workspace.getHeadSha())) {
                return new ScenarioResult(5, name, false, "Expected REVERTED back to sha-1, got: " + o2);
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "IterationCapCircuitBreaker (Turn Counter Safety Cut-off)";
        try {
            IterationCapCircuitBreaker.checkTurnLimit(1, 3); // Under cap -> OK
            IterationCapCircuitBreaker.checkTurnLimit(2, 3); // Under cap -> OK

            boolean breachCaught = false;
            try {
                IterationCapCircuitBreaker.checkTurnLimit(3, 3);
            } catch (SagaLoopBreachException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("saga guardrail breached")) {
                    breachCaught = true;
                } else {
                    return new ScenarioResult(6, name, false, "Exception message missing 'Saga guardrail breached': " + ex.getMessage());
                }
            }

            if (!breachCaught) {
                return new ScenarioResult(6, name, false, "Expected SagaLoopBreachException when maxIterations reached");
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "TokenBudgetGuardrail (Token Ceiling Circuit Breaker)";
        try {
            TokenBudgetGuardrail guardrail = new TokenBudgetGuardrail(500L);
            guardrail.recordUsage(200L);
            guardrail.recordUsage(250L); // 450 total -> OK

            boolean breachCaught = false;
            try {
                guardrail.recordUsage(100L); // 550 total -> breach
            } catch (SagaLoopBreachException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("saga guardrail breached")) {
                    breachCaught = true;
                } else {
                    return new ScenarioResult(7, name, false, "Exception message missing 'Saga guardrail breached': " + ex.getMessage());
                }
            }

            if (!breachCaught) {
                return new ScenarioResult(7, name, false, "Expected SagaLoopBreachException when tokens exceed 500");
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "TrajectoryAuditRecorder (Trajectory Event Auditing)";
        try {
            TrajectoryAuditRecorder recorder = new TrajectoryAuditRecorder();
            recorder.record(1, SagaState.DECIDE, "LLM_REQ", "Prompt sent", 120L);
            recorder.record(1, SagaState.APPLY, "PATCH_APPLIED", "Patched schema.sql", 0L);

            List<TrajectoryEvent> events = recorder.getEvents();
            if (events.size() != 2) {
                return new ScenarioResult(8, name, false, "Expected 2 events recorded, got: " + events.size());
            }
            if (events.get(0).state() != SagaState.DECIDE || events.get(1).tokensUsed() != 0L) {
                return new ScenarioResult(8, name, false, "Event fields mismatch: " + events);
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "CheckpointRecoveryManager (Save & Resume State Recovery)";
        try {
            VirtualWorkspace workspace = new VirtualWorkspaceMemory("sha-0", Map.of("App.java", "int v = 0;"));
            workspace.commit("sha-1", Map.of("App.java", "int v = 1;"));

            CheckpointRecoveryManager checkpointMgr = new CheckpointRecoveryManager();
            LoopCheckpoint cp = new LoopCheckpoint(1, "sha-1", new TelemetryMeasurement(200.0, 150.0, 0.0), 300L, SagaState.JUDGE);
            checkpointMgr.saveCheckpoint(cp);

            // Dirty workspace state (uncommitted edits)
            workspace.writeFile("App.java", "int v = DIRTY_CRASH;");

            // Resume should revert workspace back to lastKeptSha
            LoopCheckpoint resumed = checkpointMgr.resume(workspace);
            if (!"sha-1".equals(workspace.getHeadSha()) || !workspace.readFile("App.java").contains("int v = 1;")) {
                return new ScenarioResult(9, name, false, "Failed to restore workspace cleanly to checkpoint sha-1");
            }
            if (resumed.iteration() != 1 || resumed.totalTokensUsed() != 300L) {
                return new ScenarioResult(9, name, false, "Resumed checkpoint metadata mismatch: " + resumed);
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "AutonomousSagaLoopRunner (End-to-End Saga Orchestrator)";
        try {
            VirtualWorkspace workspace = new VirtualWorkspaceMemory("sha-0", Map.of("App.java", "class App {}"));
            FakeSagaChatModel model = new FakeSagaChatModel();
            ChatClient client = ChatClient.create(model);
            LoopConfig config = new LoopConfig(3, 5000L, 0.50, new NoiseFloors(10.0, 40.0));
            TelemetryMeasurement baseline = new TelemetryMeasurement(250.0, 100.0, 0.01);

            // Script 3 turns:
            // Turn 1: Valid proposal, but buildCheck fails -> REVERTED
            // Turn 2: Valid proposal, buildCheck passes, telemetry improves -> KEPT
            // Turn 3: Malformed proposal twice -> WASTED
            String prop1 = "{\"category\":\"C1\",\"fileToModify\":\"App.java\",\"patchContent\":\"syntax error\",\"rationale\":\"r1\"}";
            String prop2 = "{\"category\":\"C2\",\"fileToModify\":\"App.java\",\"patchContent\":\"class App { int x = 2; }\",\"rationale\":\"r2\"}";
            model.enqueue(prop1);
            model.enqueue(prop2);
            model.enqueue("bad json 1");
            model.enqueue("bad json 2");

            List<TelemetryMeasurement> turnTelemetry = List.of(
                    new TelemetryMeasurement(245.0, 105.0, 0.01), // Turn 1 (reverted anyway)
                    new TelemetryMeasurement(210.0, 160.0, 0.01), // Turn 2: P95 +40, RPS +60 -> KEPT
                    new TelemetryMeasurement(210.0, 160.0, 0.01)  // Turn 3 (wasted)
            );

            LoopSummary summary = AutonomousSagaLoopRunner.executeLoop(
                    workspace, client, config, baseline, turnTelemetry,
                    files -> !files.getOrDefault("App.java", "").contains("syntax error")
            );

            if (summary.totalIterations() != 3) {
                return new ScenarioResult(10, name, false, "Expected 3 total iterations, got: " + summary.totalIterations());
            }
            if (summary.keptCount() != 1 || summary.revertedCount() != 1 || summary.wastedCount() != 1) {
                return new ScenarioResult(10, name, false, "Expected (kept=1, reverted=1, wasted=1), got: " + summary);
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 07 EXERCISE 01: SAGA AGENT LOOP & STATE MACHINE VERIFIER");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String mark = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", mark, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        System.out.println("===============================================================================");
    }
}
