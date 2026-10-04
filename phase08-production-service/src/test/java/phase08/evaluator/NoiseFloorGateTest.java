package phase08.evaluator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import phase08.model.DiagnosticTelemetry;

import static org.assertj.core.api.Assertions.assertThat;

class NoiseFloorGateTest {

    private final NoiseFloorGate gate = new NoiseFloorGate();

    @Test
    @DisplayName("Should KEEP when P95 improves significantly without error regression")
    void testKeepOnP95Improvement() {
        DiagnosticTelemetry baseline = new DiagnosticTelemetry(300.0, 100.0, 0.02);
        DiagnosticTelemetry candidate = new DiagnosticTelemetry(240.0, 102.0, 0.02); // 60ms improvement > 25ms floor

        var decision = gate.evaluate(baseline, candidate, 25.0, 15.0);
        assertThat(decision.keep()).isTrue();
        assertThat(decision.reason()).contains("Keep confirmed");
    }

    @Test
    @DisplayName("Should REJECT when metric improves beyond floor but error rate regresses")
    void testRejectOnRegressedErrorRate() {
        DiagnosticTelemetry baseline = new DiagnosticTelemetry(300.0, 100.0, 0.02);
        DiagnosticTelemetry candidate = new DiagnosticTelemetry(200.0, 100.0, 0.08); // 100ms improvement, but 8% > 2% err

        var decision = gate.evaluate(baseline, candidate, 25.0, 15.0);
        assertThat(decision.keep()).isFalse();
        assertThat(decision.reason()).contains("error rate regressed");
    }

    @Test
    @DisplayName("Should REJECT when improvement is within noise floor")
    void testRejectOnWithinFloor() {
        DiagnosticTelemetry baseline = new DiagnosticTelemetry(300.0, 100.0, 0.02);
        DiagnosticTelemetry candidate = new DiagnosticTelemetry(290.0, 105.0, 0.02); // 10ms improve < 25ms floor, 5 rps < 15 rps

        var decision = gate.evaluate(baseline, candidate, 25.0, 15.0);
        assertThat(decision.keep()).isFalse();
        assertThat(decision.reason()).contains("within noise floor");
    }
}
