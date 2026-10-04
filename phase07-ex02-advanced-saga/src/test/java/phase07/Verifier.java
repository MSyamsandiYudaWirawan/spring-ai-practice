package phase07;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import phase07.AdvancedSagaContracts.*;
import phase07.AdvancedSagaUnderTest.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 07 Exercise 02:
 * Advanced Saga Agent Resilience, Speculative Branches, Triage Escalation & HITL Seams (10 Scenarios).
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

    private static class TestSandboxedWorkspace implements SandboxedWorkspace {
        private String headSha;
        private final Map<String, String> files = new HashMap<>();
        private final Map<String, Map<String, String>> history = new HashMap<>();

        public TestSandboxedWorkspace(String initSha, Map<String, String> initFiles) {
            this.headSha = initSha;
            if (initFiles != null) {
                this.files.putAll(initFiles);
                this.history.put(initSha, new HashMap<>(initFiles));
            }
        }

        @Override public String getHeadSha() { return headSha; }
        @Override public void commit(String sha, Map<String, String> newFiles) {
            this.headSha = sha;
            if (newFiles != null) { this.files.clear(); this.files.putAll(newFiles); }
            this.history.put(sha, new HashMap<>(this.files));
        }
        @Override public void revertTo(String sha) {
            this.headSha = sha;
            if (this.history.containsKey(sha)) {
                this.files.clear();
                this.files.putAll(this.history.get(sha));
            }
        }
        @Override public Map<String, String> getFiles() { return new HashMap<>(files); }
        @Override public void writeFile(String path, String content) { files.put(path, content); }
        @Override public String readFile(String path) { return files.get(path); }
        @Override public SandboxedWorkspace forkBranch(String branchName) {
            return new TestSandboxedWorkspace(headSha, new HashMap<>(files));
        }
        @Override public void mergeBranch(SandboxedWorkspace branch, String newSha) {
            this.files.clear();
            this.files.putAll(branch.getFiles());
            this.headSha = newSha;
            this.history.put(newSha, new HashMap<>(this.files));
        }
    }

    private static ScenarioResult verifyScenario1() {
        String name = "SpeculativeBranchManager (Isolated Multi-Branch Sandboxing & Merge)";
        try {
            SandboxedWorkspace workspace = new TestSandboxedWorkspace("sha-0", Map.of("App.java", "int v = 0;"));
            DecisionProposal propA = new DecisionProposal("pA", "INDEX", "App.java", "int v = 10;", 0.1, "patch A");
            DecisionProposal propB = new DecisionProposal("pB", "CACHE", "App.java", "int v = 20;", 0.1, "patch B");

            Telemetry baseline = new Telemetry(300.0, 100.0, 0.01);
            Telemetry telA = new Telemetry(250.0, 150.0, 0.01); // P95 +50ms -> Clears floor
            Telemetry telB = new Telemetry(290.0, 110.0, 0.01); // P95 +10ms -> Fails floor
            NoiseFloors floors = new NoiseFloors(20.0, 30.0);

            SpeculativeBranchResult res1 = SpeculativeBranchManager.evaluateBranches(
                    workspace, propA, propB, baseline, telA, telB, floors, "sha-winner-A"
            );

            if (!"branch-A".equals(res1.winningBranch()) || res1.bothRolledBack() || !"sha-winner-A".equals(workspace.getHeadSha())) {
                return new ScenarioResult(1, name, false, "Expected branch-A to win and merge, got: " + res1);
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "MultiStageDiagnosticTriageEngine (4-Stage Escalation Hierarchy)";
        try {
            TriageResult r1 = MultiStageDiagnosticTriageEngine.triageFailure("TOOL_CALL_EXCEPTION");
            if (r1.stage() != TriageStage.STAGE_1_TOOL_RETRY || r1.escalate()) {
                return new ScenarioResult(2, name, false, "Expected Stage 1 without escalation, got: " + r1);
            }

            TriageResult r2 = MultiStageDiagnosticTriageEngine.triageFailure("SCHEMA_PARSE_ERROR");
            if (r2.stage() != TriageStage.STAGE_2_SCHEMA_REPAIR) {
                return new ScenarioResult(2, name, false, "Expected Stage 2 schema repair, got: " + r2);
            }

            TriageResult r3 = MultiStageDiagnosticTriageEngine.triageFailure("BUILD_FAILURE");
            if (r3.stage() != TriageStage.STAGE_3_COMPENSATION_REVERT) {
                return new ScenarioResult(2, name, false, "Expected Stage 3 compensation revert, got: " + r3);
            }

            TriageResult r4 = MultiStageDiagnosticTriageEngine.triageFailure("GUARDRAIL_BREACH");
            if (r4.stage() != TriageStage.STAGE_4_GUARDRAIL_TERMINATE || !r4.escalate()) {
                return new ScenarioResult(2, name, false, "Expected Stage 4 with escalation, got: " + r4);
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "GuaranteedFinallyCompensationEngine (Guaranteed Rollback on Crash)";
        try {
            SandboxedWorkspace workspace = new TestSandboxedWorkspace("sha-0", Map.of("App.java", "int safe = 1;"));

            boolean caught = false;
            try {
                GuaranteedFinallyCompensationEngine.executeWithGuaranteedCompensation(workspace, "sha-0", () -> {
                    workspace.commit("sha-dirty", Map.of("App.java", "DIRTY_MUTATION!"));
                    throw new RuntimeException("Simulated thread timeout / crash");
                });
            } catch (RuntimeException ex) {
                caught = true;
            }

            if (!caught) {
                return new ScenarioResult(3, name, false, "Expected original runtime exception to be re-thrown");
            }
            if (!"sha-0".equals(workspace.getHeadSha()) || "DIRTY_MUTATION!".equals(workspace.readFile("App.java"))) {
                return new ScenarioResult(3, name, false, "Workspace was not reverted to sha-0 in finally block");
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "SaddleSafeMechanismKeepEngine (Full KeepRule v2 Mechanism Reduction)";
        try {
            JfrSignals prev = new JfrSignals(Map.of("LockContention", 100L));
            JfrSignals candGood = new JfrSignals(Map.of("LockContention", 20L)); // 80% reduction
            JfrSignals candBad = new JfrSignals(Map.of("LockContention", 70L));  // 30% reduction

            Telemetry ref = new Telemetry(300.0, 100.0, 0.01);
            Telemetry candTel = new Telemetry(280.0, 120.0, 0.01); // P95 +20ms, RPS +20
            NoiseFloors floors = new NoiseFloors(15.0, 15.0);

            MechanismVerdict vGood = SaddleSafeMechanismKeepEngine.evaluateMechanism(
                    prev, candGood, "LockContention", 0.05, ref, candTel, floors
            );
            if (!vGood.keep() || Math.abs(vGood.reductionRate() - 0.80) > 0.01) {
                return new ScenarioResult(4, name, false, "Expected mechanism keep with 80% reduction, got: " + vGood);
            }

            MechanismVerdict vBad = SaddleSafeMechanismKeepEngine.evaluateMechanism(
                    prev, candBad, "LockContention", 0.05, ref, candTel, floors
            );
            if (vBad.keep()) {
                return new ScenarioResult(4, name, false, "Expected rejection for 30% reduction, got: " + vBad);
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "CyclicalPingPongDetector (Loop Trapping & Cycle Detection)";
        try {
            CyclicalPingPongDetector detector = new CyclicalPingPongDetector();
            DecisionProposal p1 = new DecisionProposal("1", "C", "App.java", "threads=20", 0.1, "r1");
            DecisionProposal p2 = new DecisionProposal("2", "C", "Other.java", "cache=true", 0.1, "r2");
            DecisionProposal p3 = new DecisionProposal("3", "C", "App.java", "threads=20", 0.1, "r3"); // Duplicate of p1!

            detector.inspectProposal(p1);
            detector.inspectProposal(p2);

            boolean cycleCaught = false;
            try {
                detector.inspectProposal(p3);
            } catch (AdvancedSagaBreachException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("advanced saga breach")) {
                    cycleCaught = true;
                } else {
                    return new ScenarioResult(5, name, false, "Exception missing 'Advanced Saga breach': " + ex.getMessage());
                }
            }

            if (!cycleCaught) {
                return new ScenarioResult(5, name, false, "Expected AdvancedSagaBreachException on cyclical proposal");
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "AdaptiveFailureFeedbackEnricher (Failure Trace Injection into Prompts)";
        try {
            String longLog = "Error at line 42: NullPointerException in Handler. " + "A".repeat(800);
            String enriched = AdaptiveFailureFeedbackEnricher.enrichPrompt(2, "COMPILATION_ERROR", longLog);

            if (!enriched.contains("[PREVIOUS_TURN_FAILURE] Turn 2 (COMPILATION_ERROR):")) {
                return new ScenarioResult(6, name, false, "Enriched prompt missing failure header: " + enriched);
            }
            if (enriched.length() > 650) {
                return new ScenarioResult(6, name, false, "Enriched log tail was not properly truncated: len=" + enriched.length());
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "DynamicPhaseTokenBudgeter (Partitioned Phase Budget Ceilings)";
        try {
            DynamicPhaseTokenBudgeter budgeter = new DynamicPhaseTokenBudgeter(1000L);
            // ANALYSIS = 300 tokens
            budgeter.recordSpend("ANALYSIS", 200L); // OK

            boolean breachCaught = false;
            try {
                budgeter.recordSpend("ANALYSIS", 150L); // 350 total -> breach
            } catch (AdvancedSagaBreachException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("advanced saga breach")) {
                    breachCaught = true;
                } else {
                    return new ScenarioResult(7, name, false, "Exception missing 'Advanced Saga breach': " + ex.getMessage());
                }
            }

            if (!breachCaught) {
                return new ScenarioResult(7, name, false, "Expected AdvancedSagaBreachException for phase partition breach");
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "SplitBrainCheckpointValidator (Split-Brain Corruption Detection & Recovery)";
        try {
            Set<String> knownShas = Set.of("sha-0", "sha-1", "sha-2");

            String valid = SplitBrainCheckpointValidator.resolveConsistentSha("sha-1", knownShas, "sha-0");
            if (!"sha-1".equals(valid)) {
                return new ScenarioResult(8, name, false, "Expected valid sha-1, got: " + valid);
            }

            String recovered = SplitBrainCheckpointValidator.resolveConsistentSha("sha-ghost-divergent", knownShas, "sha-0");
            if (!"sha-0".equals(recovered)) {
                return new ScenarioResult(8, name, false, "Expected fallback recovery to sha-0, got: " + recovered);
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "HumanInTheLoopApprovalGate (Risk-Based Operator Pause & Verdict)";
        try {
            DecisionProposal lowRisk = new DecisionProposal("1", "TUNE", "app.yml", "pool=10", 0.30, "safe");
            AdvancedSagaState s1 = HumanInTheLoopApprovalGate.evaluateRisk(lowRisk, 0.70, req -> new ApprovalResponse(false, ""));
            if (s1 != AdvancedSagaState.APPLY) {
                return new ScenarioResult(9, name, false, "Low risk proposal should advance directly to APPLY without approval");
            }

            DecisionProposal highRisk = new DecisionProposal("2", "KERNEL", "sysctl.conf", "drop=1", 0.85, "risky");
            // Approved -> APPLY
            AdvancedSagaState s2 = HumanInTheLoopApprovalGate.evaluateRisk(highRisk, 0.70, req -> new ApprovalResponse(true, "Looks good"));
            if (s2 != AdvancedSagaState.APPLY) {
                return new ScenarioResult(9, name, false, "Approved high-risk proposal should advance to APPLY");
            }

            // Rejected -> DECIDE
            AdvancedSagaState s3 = HumanInTheLoopApprovalGate.evaluateRisk(highRisk, 0.70, req -> new ApprovalResponse(false, "Too dangerous"));
            if (s3 != AdvancedSagaState.DECIDE) {
                return new ScenarioResult(9, name, false, "Rejected high-risk proposal should return to DECIDE");
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "ResilientAutonomousChaosSagaOrchestrator (Multi-Turn Chaos Orchestrator)";
        try {
            SandboxedWorkspace workspace = new TestSandboxedWorkspace("sha-0", Map.of("App.java", "class App {}"));
            FakeAdvancedSagaChatModel model = new FakeAdvancedSagaChatModel();
            ChatClient client = ChatClient.create(model);
            Telemetry baseline = new Telemetry(250.0, 100.0, 0.01);

            AdvancedSagaState finalState = ResilientAutonomousChaosSagaOrchestrator.runChaosLifecycle(
                    workspace, client, baseline
            );

            if (finalState != AdvancedSagaState.FINISH) {
                return new ScenarioResult(10, name, false, "Expected final state FINISH, got: " + finalState);
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 07 EXERCISE 02: ADVANCED SAGA RESILIENCE & HITL VERIFIER");
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
