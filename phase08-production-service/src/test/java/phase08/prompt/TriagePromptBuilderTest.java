package phase08.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import phase08.model.DiagnosticTelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriagePromptBuilderTest {

    private final TriagePromptBuilder builder = new TriagePromptBuilder();

    @Test
    @DisplayName("Should format dynamic variables into user prompt")
    void testPromptFormatting() {
        DiagnosticTelemetry tel = new DiagnosticTelemetry(450.0, 80.0, 0.12);
        String prompt = builder.buildUserPrompt("INC-101", "payment-service", "Deadlock on checkout", tel);

        assertThat(prompt)
                .contains("INC-101")
                .contains("payment-service")
                .contains("Deadlock on checkout")
                .contains("450.0")
                .contains("80.0")
                .contains("0.12");
    }

    @Test
    @DisplayName("Blank incidentId should throw IllegalArgumentException")
    void testBlankIncidentThrows() {
        DiagnosticTelemetry tel = DiagnosticTelemetry.baseline();
        assertThatThrownBy(() -> builder.buildUserPrompt("", "order-svc", "Crash", tel))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
