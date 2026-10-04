package phase08.evaluator;

import java.util.List;

/**
 * Phase 06: Diagnostic Claim Grounding Evaluator.
 * <p>
 * Verifies that diagnostic hypotheses proposed by the LLM are grounded in observed tool outputs.
 */
public class GroundingEvaluator {

    public static final List<String> DIAGNOSTIC_KEYWORDS = List.of(
            "thread", "lock", "connection", "pool", "queue", "memory", "gc", "oom"
    );

    /**
     * Asserts whether the diagnostic hypothesis is grounded in tool observations.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If hypothesis is null or blank, throw IllegalArgumentException("hypothesis cannot be blank").
     *   - If toolObservations is null or empty, return false.
     * - Grounding Verification:
     *   - Convert hypothesis to lowercase.
     *   - Iterate through toolObservations (skip nulls, convert to lowercase).
     *   - For each keyword in DIAGNOSTIC_KEYWORDS:
     *     - If both lowerHypothesis and lowerObservation contain the keyword, return true.
     * - If no matching diagnostic keyword overlap is found, return false.
     */
    public boolean isGrounded(String hypothesis, List<String> toolObservations) {
        throw new UnsupportedOperationException("TODO: Implement isGrounded");
    }
}
