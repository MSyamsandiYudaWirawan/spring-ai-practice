package phase08.evaluator;

import phase08.model.DiagnosticTelemetry;

/**
 * Phase 06: Deterministic Telemetry Noise Floor & Keep Evaluator.
 * <p>
 * Evaluates candidate telemetry improvements against baseline noise floors
 * while strictly enforcing error rate non-regression.
 */
public class NoiseFloorGate {

    public record KeepDecision(boolean keep, String reason) {}

    /**
     * Evaluates candidate telemetry against baseline and noise floor thresholds.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If baseline is null or candidate is null, throw IllegalArgumentException("Telemetry cannot be null").
     * - Calculate Deltas:
     *   - p95Delta = baseline.p95Ms() - candidate.p95Ms() (positive means faster)
     *   - rpsDelta = candidate.rps() - baseline.rps() (positive means higher throughput)
     *   - failRateSafe = candidate.errorRate() <= baseline.errorRate()
     * - Evaluate Noise Floors:
     *   - p95Cleared = p95Delta > p95FloorMs
     *   - rpsCleared = rpsDelta > rpsFloor
     * - Decision Matrix:
     *   1. If (p95Cleared || rpsCleared) && failRateSafe:
     *      - String type = p95Cleared ? "P95 latency" : "Throughput RPS"
     *      - Return new KeepDecision(true, "Keep confirmed: " + type + " cleared floor without error regression")
     *   2. If (p95Cleared || rpsCleared) && !failRateSafe:
     *      - Return new KeepDecision(false, "Rejected: Metric improved but error rate regressed (" + candidate.errorRate() + " > " + baseline.errorRate() + ")")
     *   3. If neither cleared:
     *      - Return new KeepDecision(false, "Rejected: Metrics delta within noise floor (p95Delta=" + p95Delta + ", rpsDelta=" + rpsDelta + ")")
     */
    public KeepDecision evaluate(
            DiagnosticTelemetry baseline,
            DiagnosticTelemetry candidate,
            double p95FloorMs,
            double rpsFloor
    ) {
        throw new UnsupportedOperationException("TODO: Implement evaluate");
    }
}
