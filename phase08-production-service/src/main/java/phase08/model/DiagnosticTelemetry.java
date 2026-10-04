package phase08.model;

/**
 * Performance and reliability telemetry snapshot from target service.
 */
public record DiagnosticTelemetry(
        double p95Ms,
        double rps,
        double errorRate
) {
    public static DiagnosticTelemetry baseline() {
        return new DiagnosticTelemetry(350.0, 100.0, 0.05);
    }
}
