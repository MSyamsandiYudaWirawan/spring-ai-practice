package phase07;

import java.util.*;

/**
 * Immutable domain records, exceptions, and contracts for Phase 07 Exercise 02:
 * Advanced Saga Agent Resilience, Speculative Branches, Triage Escalation & HITL Seams.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class AdvancedSagaContracts {

    private AdvancedSagaContracts() {}

    /**
     * Advanced Saga lifecycle states including human-in-the-loop pauses.
     */
    public enum AdvancedSagaState {
        IDLE,
        DECIDE,
        APPLY,
        MEASURE,
        JUDGE,
        PAUSED_FOR_APPROVAL,
        FINISH,
        ABORTED
    }

    /**
     * 4-Stage Diagnostic Failure Triage hierarchy (from Diagnostician DiagnosticTriageService).
     */
    public enum TriageStage {
        STAGE_1_TOOL_RETRY,
        STAGE_2_SCHEMA_REPAIR,
        STAGE_3_COMPENSATION_REVERT,
        STAGE_4_GUARDRAIL_TERMINATE
    }

    /**
     * Result of triage failure evaluation.
     */
    public record TriageResult(
            TriageStage stage,
            boolean escalate,
            String action,
            String feedback
    ) {
        public TriageResult {
            Objects.requireNonNull(stage, "stage must not be null");
            Objects.requireNonNull(action, "action must not be null");
            Objects.requireNonNull(feedback, "feedback must not be null");
        }
    }

    /**
     * Decision proposal from the DECIDE turn.
     */
    public record DecisionProposal(
            String id,
            String category,
            String fileToModify,
            String patchContent,
            double riskScore,
            String rationale
    ) {
        public DecisionProposal {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(category, "category must not be null");
            Objects.requireNonNull(fileToModify, "fileToModify must not be null");
            Objects.requireNonNull(patchContent, "patchContent must not be null");
            if (riskScore < 0.0 || riskScore > 1.0) {
                throw new IllegalArgumentException("riskScore must be between 0.0 and 1.0, got: " + riskScore);
            }
        }
    }

    /**
     * Speculative execution result comparing 2 branch candidates.
     */
    public record SpeculativeBranchResult(
            String winningBranch,
            String committedSha,
            boolean bothRolledBack,
            String reason
    ) {
        public SpeculativeBranchResult {
            Objects.requireNonNull(winningBranch, "winningBranch must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Telemetry metrics.
     */
    public record Telemetry(double p95Ms, double rps, double failRate) {}

    /**
     * Noise floors.
     */
    public record NoiseFloors(double p95FloorMs, double rpsFloor) {}

    /**
     * JFR Harvest signal map (signal name -> count).
     */
    public record JfrSignals(Map<String, Long> counts) {
        public JfrSignals {
            counts = counts != null ? Map.copyOf(counts) : Map.of();
        }
    }

    /**
     * Mechanism reduction evaluation result.
     */
    public record MechanismVerdict(
            boolean keep,
            double reductionRate,
            boolean tailBounded,
            String reason
    ) {
        public MechanismVerdict {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Human-in-the-loop approval request for risky proposals.
     */
    public record ApprovalRequest(DecisionProposal proposal, String reason) {
        public ApprovalRequest {
            Objects.requireNonNull(proposal, "proposal must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Human-in-the-loop approval verdict.
     */
    public record ApprovalResponse(boolean approved, String reviewerComment) {
        public ApprovalResponse {
            Objects.requireNonNull(reviewerComment, "reviewerComment must not be null");
        }
    }

    /**
     * Workspace interface for atomic commit, branch sandboxing, and rollback.
     */
    public interface SandboxedWorkspace {
        String getHeadSha();
        void commit(String sha, Map<String, String> files);
        void revertTo(String sha);
        Map<String, String> getFiles();
        void writeFile(String path, String content);
        String readFile(String path);
        SandboxedWorkspace forkBranch(String branchName);
        void mergeBranch(SandboxedWorkspace branch, String newSha);
    }

    /**
     * Thrown when an advanced Saga guardrail, token partition, or cycle trap trips.
     * <p>
     * Contract requirement: Message MUST contain "Advanced Saga breach".
     */
    public static class AdvancedSagaBreachException extends RuntimeException {
        public AdvancedSagaBreachException(String message) {
            super(message);
        }
    }
}
