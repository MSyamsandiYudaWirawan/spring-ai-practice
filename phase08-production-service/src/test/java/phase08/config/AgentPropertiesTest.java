package phase08.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentPropertiesTest {

    @Test
    @DisplayName("Defaults should satisfy all validation rules")
    void testDefaultsValid() {
        AgentProperties props = AgentProperties.defaults();
        assertThatCode(props::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Blank model name should throw IllegalArgumentException")
    void testBlankModelThrows() {
        AgentProperties props = new AgentProperties(true, "  ", 5, 1000, 0.2, 20.0, 10.0);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modelName");
    }

    @Test
    @DisplayName("Iterations outside [1, 20] should throw IllegalArgumentException")
    void testInvalidIterationsThrows() {
        AgentProperties low = new AgentProperties(true, "gpt-4o", 0, 1000, 0.2, 20.0, 10.0);
        assertThatThrownBy(low::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxIterations");

        AgentProperties high = new AgentProperties(true, "gpt-4o", 25, 1000, 0.2, 20.0, 10.0);
        assertThatThrownBy(high::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxIterations");
    }

    @Test
    @DisplayName("Token budget < 100 should throw IllegalArgumentException")
    void testInvalidTokenBudgetThrows() {
        AgentProperties props = new AgentProperties(true, "gpt-4o", 5, 50, 0.2, 20.0, 10.0);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenBudget");
    }
}
