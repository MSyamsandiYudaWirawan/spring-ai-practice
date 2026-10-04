package phase01;

import java.util.List;
import java.util.Objects;

/**
 * Immutable contracts and data models for Phase 01 Exercise 03 (Cloud SRE & Kubernetes Operations).
 * DO NOT MODIFY THIS FILE.
 */
public final class SreModelContracts {

    private SreModelContracts() {}

    public enum Environment {
        PRODUCTION,
        STAGING,
        DEVELOPMENT
    }

    public record AlertDetails(
            String serviceName,
            int cpuPercent,
            long memoryBytes,
            double p99LatencySec,
            List<String> errorCodes
    ) {
        public AlertDetails {
            Objects.requireNonNull(serviceName, "serviceName must not be null");
            if (serviceName.isBlank()) throw new IllegalArgumentException("serviceName cannot be blank");
            if (cpuPercent < 0 || cpuPercent > 100) throw new IllegalArgumentException("cpuPercent must be between 0 and 100");
            if (memoryBytes < 0) throw new IllegalArgumentException("memoryBytes must be >= 0");
            if (p99LatencySec < 0.0) throw new IllegalArgumentException("p99LatencySec must be >= 0.0");
            Objects.requireNonNull(errorCodes, "errorCodes must not be null");
        }
    }

    public record SreAudit(
            String diagnosis,
            long promptTokens,
            long completionTokens
    ) {
        public SreAudit {
            Objects.requireNonNull(diagnosis, "diagnosis must not be null");
        }
    }

    public record FewShotPair(String symptom, String action) {}

    public static final String BASE_SRE_SYSTEM_PROMPT =
            "You are a Kubernetes Site Reliability Engineering (SRE) assistant. Investigate cluster anomalies with extreme precision.";

    public static final String RESOURCE_ALERT_TEMPLATE =
            "Resource Alert for service {serviceName}: CPU at {cpuPercent}%, Memory at {memoryBytes} bytes, p99 Latency at {p99LatencySec}s. Active error codes: {errorCodes}. Recommend immediate mitigation.";

    public static final String PROD_POLICY =
            "CRITICAL PRODUCTION POLICY: Zero downtime permitted. Require dual-approval runbook step before applying destructive mutating commands.";

    public static final String NON_PROD_POLICY =
            "NON-PRODUCTION POLICY: Fast failover permitted. Debug logging enabled.";

    public static final String CANARY_SYSTEM_TEMPLATE =
            "You are a Canary Release Auditor monitoring traffic share of {trafficPercent}%. Flag performance divergence exceeding 5% threshold.";

    public static final double BASE_TEMPERATURE = 0.4;
    public static final double BASE_TOP_P = 0.9;

    public static final double DETERMINISTIC_TEMPERATURE = 0.0;
    public static final int RUNBOOK_MAX_TOKENS = 300;
    public static final List<String> RUNBOOK_STOP_SEQUENCES = List.of("END_RUNBOOK", "---");
    public static final double RUNBOOK_FREQUENCY_PENALTY = 0.5;

    public static final List<FewShotPair> REMEDIATION_FEW_SHOTS = List.of(
            new FewShotPair("Pod OOMKilled repeatedly in payments namespace", "RESTART_AND_SCALE_MEMORY"),
            new FewShotPair("CrashLoopBackOff due to bad environment variable config", "ROLLBACK_DEPLOYMENT"),
            new FewShotPair("Disk pressure threshold 95% on worker node ip-10-0-12-88", "DRAIN_AND_CORDON_NODE")
    );
}
