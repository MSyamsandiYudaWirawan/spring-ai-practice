package phase08.health;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Phase 08: Spring Boot Actuator Health Probe.
 * <p>
 * Probes the underlying ChatModel connectivity and availability.
 * Location in Spring Boot 4.1.1: {@code org.springframework.boot.health.contributor.HealthIndicator}
 */
@Component
public class AgentHealthIndicator implements HealthIndicator {

    private final ChatModel chatModel;
    private final String modelName;

    @org.springframework.beans.factory.annotation.Autowired
    public AgentHealthIndicator(ChatModel chatModel) {
        this(chatModel, "default-llm");
    }

    public AgentHealthIndicator(ChatModel chatModel, String modelName) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.modelName = modelName != null ? modelName : "unknown";
    }

    /**
     * Executes a liveness check against the ChatModel.
     * <p>
     * Instructions:
     * - Try:
     *   - Call chatModel.call(new Prompt("health-check-ping")).
     *   - If response != null && response.getResult() != null:
     *     - Return Health.up()
     *         .withDetail("model", modelName)
     *         .withDetail("status", "AVAILABLE")
     *         .build().
     *   - Otherwise (empty response):
     *     - Return Health.down()
     *         .withDetail("model", modelName)
     *         .withDetail("status", "EMPTY_RESPONSE")
     *         .build().
     * - Catch (Exception ex):
     *   - Return Health.down(ex)
     *       .withDetail("model", modelName)
     *       .withDetail("status", "UNAVAILABLE")
     *       .build().
     */
    @Override
    public Health health() {
        throw new UnsupportedOperationException("TODO: Implement health");
    }
}
