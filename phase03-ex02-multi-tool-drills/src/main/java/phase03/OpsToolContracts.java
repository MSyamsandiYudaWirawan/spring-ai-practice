package phase03;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable contracts and data records for Phase 03 Exercise 02:
 * Repetitive Spring AI @Tool Calling Drills.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class OpsToolContracts {

    private OpsToolContracts() {}

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public record DeploymentStatus(
            String deployment,
            int replicas,
            String state
    ) {
        public DeploymentStatus {
            Objects.requireNonNull(deployment, "deployment must not be null");
            Objects.requireNonNull(state, "state must not be null");
        }
    }

    public record ConfigUpdateResult(
            String service,
            LogLevel level,
            boolean applied,
            String message
    ) {
        public ConfigUpdateResult {
            Objects.requireNonNull(service, "service must not be null");
            Objects.requireNonNull(level, "level must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    public record LogQueryFilter(
            String service,
            LogLevel minLevel,
            int maxEntries,
            String keyword
    ) {
        public LogQueryFilter {
            Objects.requireNonNull(service, "service must not be null");
            if (minLevel == null) minLevel = LogLevel.INFO;
            if (maxEntries <= 0) maxEntries = 10;
        }
    }

    public record LogEntry(
            String service,
            LogLevel level,
            String message,
            long timestamp
    ) {
        public LogEntry {
            Objects.requireNonNull(service, "service must not be null");
            Objects.requireNonNull(level, "level must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    public record HealthReport(
            String component,
            String status,
            long latencyMs,
            Map<String, Object> details
    ) {
        public HealthReport {
            Objects.requireNonNull(component, "component must not be null");
            Objects.requireNonNull(status, "status must not be null");
            if (details == null) details = Map.of();
        }
    }

    public record ToolEnvelope<T>(
            boolean ok,
            T data,
            String error
    ) {
        public static <T> ToolEnvelope<T> ok(T data) {
            return new ToolEnvelope<>(true, data, null);
        }

        public static <T> ToolEnvelope<T> fail(String error) {
            Objects.requireNonNull(error, "error must not be null");
            return new ToolEnvelope<>(false, null, error);
        }
    }

    public record ToolExecutionAudit(
            int callIndex,
            String toolName,
            Map<String, Object> arguments,
            boolean success,
            String summary,
            long timestamp
    ) {
        public ToolExecutionAudit {
            Objects.requireNonNull(toolName, "toolName must not be null");
            if (arguments == null) arguments = Map.of();
        }
    }
}
