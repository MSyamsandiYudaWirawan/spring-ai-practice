package phase08.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Production Agent Configuration Properties.
 * <p>
 * Binds properties prefixed with {@code agent.triage} from application.yml or environment.
 */
@ConfigurationProperties(prefix = "agent.triage")
public record AgentProperties(
        boolean enabled,
        String modelName,
        int maxIterations,
        int tokenBudget,
        double temperature,
        double p95FloorMs,
        double rpsFloor
) {
    /**
     * Factory for default production baseline configuration.
     * <p>
     * Instructions:
     * - Returns an AgentProperties instance with standard baseline values:
     *   - enabled: true
     *   - modelName: "gpt-4o"
     *   - maxIterations: 5
     *   - tokenBudget: 4000
     *   - temperature: 0.2
     *   - p95FloorMs: 25.0
     *   - rpsFloor: 15.0
     */
    public static AgentProperties defaults() {
        return new AgentProperties(true, "gpt-4o", 5, 4000, 0.2, 25.0, 15.0);
    }

    /**
     * Fail-Fast Boundary Validation.
     * <p>
     * Instructions:
     * - Validate each configuration property defensively:
     *   - If modelName is null or blank, throw IllegalArgumentException("modelName must not be blank").
     *   - If maxIterations is < 1 or > 20, throw IllegalArgumentException("maxIterations must be between 1 and 20").
     *   - If tokenBudget is < 100, throw IllegalArgumentException("tokenBudget must be at least 100").
     *   - If temperature is < 0.0 or > 2.0, throw IllegalArgumentException("temperature must be between 0.0 and 2.0").
     *   - If p95FloorMs is < 0.0, throw IllegalArgumentException("p95FloorMs must be non-negative").
     *   - If rpsFloor is < 0.0, throw IllegalArgumentException("rpsFloor must be non-negative").
     */
    public void validate() {
        throw new UnsupportedOperationException("TODO: Implement validate()");
    }
}
