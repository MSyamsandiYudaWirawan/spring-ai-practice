package phase08.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import phase08.model.TriageProposal;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriageProposalParserTest {

    private final TriageProposalParser parser = new TriageProposalParser();

    @Test
    @DisplayName("Should strip markdown code fences cleanly")
    void testStripFences() {
        String fenced = "```json\n{\"action\": \"RESTART\"}\n```";
        assertThat(parser.stripMarkdownFences(fenced)).isEqualTo("{\"action\": \"RESTART\"}");
    }

    @Test
    @DisplayName("Should parse valid JSON into TriageProposal")
    void testValidParse() {
        String json = """
                ```json
                {
                  "action": "SCALE_POOL",
                  "hypothesis": "Hikari pool starvation",
                  "targetFile": "application.properties",
                  "patchContent": "max=50",
                  "confidence": 0.90,
                  "rationale": "High acquisition wait"
                }
                ```
                """;

        TriageProposal proposal = parser.parse(json);
        assertThat(proposal.action()).isEqualTo("SCALE_POOL");
        assertThat(proposal.hypothesis()).isEqualTo("Hikari pool starvation");
        assertThat(proposal.confidence()).isEqualTo(0.90);
    }

    @Test
    @DisplayName("Invalid confidence boundary (> 1.0) should throw IllegalArgumentException")
    void testInvalidConfidenceThrows() {
        String json = """
                {
                  "action": "SCALE_POOL",
                  "hypothesis": "Hikari pool starvation",
                  "targetFile": "app.props",
                  "patchContent": "max=50",
                  "confidence": 1.50,
                  "rationale": "test"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1-shot repair should succeed when initial raw JSON is malformed")
    void testOneShotRepairSuccess() {
        String malformed = "I recommend this: { action: unquoted }";
        String validJson = """
                {
                  "action": "RESTART_POD",
                  "hypothesis": "OOM leak",
                  "targetFile": "deploy.yml",
                  "patchContent": "mem=2Gi",
                  "confidence": 0.85,
                  "rationale": "Kernel OOM log"
                }
                """;

        Optional<TriageProposal> result = parser.parseWithOneShotRepair(malformed, repairPrompt -> validJson);
        assertThat(result).isPresent();
        assertThat(result.get().action()).isEqualTo("RESTART_POD");
    }
}
