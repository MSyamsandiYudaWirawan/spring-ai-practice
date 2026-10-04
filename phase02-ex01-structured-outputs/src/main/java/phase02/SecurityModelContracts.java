package phase02;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable contracts and data records for Phase 02 Exercise 01 (Structured Outputs & Resilient Schema Extraction).
 * DO NOT MODIFY THIS FILE.
 */
public final class SecurityModelContracts {

    private SecurityModelContracts() {}

    private static final Pattern CVE_PATTERN = Pattern.compile("^CVE-\\d{4}-\\d{4,7}$");

    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum PatchActionType {
        IMMEDIATE_ROLLOUT,
        CANARY_DEPLOY,
        ISOLATE_SERVICE,
        MONITOR_ONLY
    }

    /**
     * Primary domain assessment record.
     * Enforces TigerStyle fail-fast validation in compact constructor.
     */
    public record VulnerabilityAssessment(
            String cveId,
            Severity severity,
            double cvssScore,
            String packageCoordinate,
            String rootCauseAnalysis,
            List<String> mitigationSteps
    ) {
        public VulnerabilityAssessment {
            Objects.requireNonNull(cveId, "cveId must not be null");
            if (!CVE_PATTERN.matcher(cveId.trim()).matches()) {
                throw new IllegalArgumentException("Invalid CVE format: " + cveId + " (expected CVE-YYYY-NNNN+)");
            }
            Objects.requireNonNull(severity, "severity must not be null");
            if (cvssScore < 0.0 || cvssScore > 10.0) {
                throw new IllegalArgumentException("cvssScore must be between 0.0 and 10.0 inclusive, got " + cvssScore);
            }
            Objects.requireNonNull(packageCoordinate, "packageCoordinate must not be null");
            if (!packageCoordinate.contains(":")) {
                throw new IllegalArgumentException("packageCoordinate must be formatted as group:artifact:version, got " + packageCoordinate);
            }
            Objects.requireNonNull(rootCauseAnalysis, "rootCauseAnalysis must not be null");
            if (rootCauseAnalysis.isBlank()) {
                throw new IllegalArgumentException("rootCauseAnalysis must not be blank");
            }
            Objects.requireNonNull(mitigationSteps, "mitigationSteps must not be null");
            if (mitigationSteps.isEmpty()) {
                throw new IllegalArgumentException("mitigationSteps must not be empty");
            }
        }
    }

    /**
     * Secondary record for collection extraction drills.
     */
    public record PatchPlan(
            String patchId,
            String targetService,
            String upgradeVersion,
            PatchActionType actionType,
            boolean requiresDowntime,
            List<String> validationCommands
    ) {
        public PatchPlan {
            Objects.requireNonNull(patchId, "patchId must not be null");
            if (patchId.isBlank()) throw new IllegalArgumentException("patchId must not be blank");
            Objects.requireNonNull(targetService, "targetService must not be null");
            if (targetService.isBlank()) throw new IllegalArgumentException("targetService must not be blank");
            Objects.requireNonNull(upgradeVersion, "upgradeVersion must not be null");
            if (upgradeVersion.isBlank()) throw new IllegalArgumentException("upgradeVersion must not be blank");
            Objects.requireNonNull(actionType, "actionType must not be null");
            Objects.requireNonNull(validationCommands, "validationCommands must not be null");
        }
    }

    /**
     * Diagnostic result envelope (accepted vs rejected).
     */
    public record TriageEnvelope<T>(
            boolean accepted,
            T data,
            String rejectionReason
    ) {
        public static <T> TriageEnvelope<T> accepted(T data) {
            Objects.requireNonNull(data, "accepted data must not be null");
            return new TriageEnvelope<>(true, data, null);
        }

        public static <T> TriageEnvelope<T> rejected(String rejectionReason) {
            Objects.requireNonNull(rejectionReason, "rejectionReason must not be null");
            return new TriageEnvelope<>(false, null, rejectionReason);
        }
    }

    public static final String SAMPLE_VALID_CVE_JSON = """
            {
              "cveId": "CVE-2026-38491",
              "severity": "CRITICAL",
              "cvssScore": 9.8,
              "packageCoordinate": "org.springframework.security:spring-security-web:6.2.1",
              "rootCauseAnalysis": "Unauthenticated regex path traversal bypass in filter chain matcher.",
              "mitigationSteps": [
                "Upgrade spring-security-web to 6.2.2+",
                "Deploy WAF filter blocking trailing slash bypass patterns"
              ]
            }
            """;

    public static final String SAMPLE_MARKDOWN_WRAPPED_CVE_JSON = """
            Here is the security assessment report from SecOps automated triage:
            ```json
            {
              "cveId": "CVE-2026-11892",
              "severity": "HIGH",
              "cvssScore": 8.5,
              "packageCoordinate": "com.fasterxml.jackson.core:jackson-databind:2.15.0",
              "rootCauseAnalysis": "Polymorphic typing deserialization gadget in LDAP lookup provider.",
              "mitigationSteps": [
                "Apply Jackson blocklist patch",
                "Disable default polymorphic typing"
              ]
            }
            ```
            Please ensure all staging clusters verify this patch before production rollout.
            """;

    public static final String SAMPLE_PATCH_PLANS_JSON = """
            [
              {
                "patchId": "PATCH-2026-001",
                "targetService": "auth-service",
                "upgradeVersion": "6.2.2",
                "actionType": "IMMEDIATE_ROLLOUT",
                "requiresDowntime": false,
                "validationCommands": ["curl -f http://localhost:8080/actuator/health", "mvn test-compile"]
              },
              {
                "patchId": "PATCH-2026-002",
                "targetService": "billing-gateway",
                "upgradeVersion": "2.16.0",
                "actionType": "CANARY_DEPLOY",
                "requiresDowntime": true,
                "validationCommands": ["kubectl rollout status deployment/billing-gateway"]
              }
            ]
            """;

    public static final String SAMPLE_CORRUPTED_JSON = """
            {
              "cveId": "CVE-2026-99120",
              "severity": "HIGH",
              "cvssScore": 7.4,
              "packageCoordinate": "io.netty:netty-codec-http:4.1.100.Final",
              "rootCauseAnalysis": "Incomplete header line delimiter validation leading to HTTP request smuggling
            """;

    public static final String SAMPLE_INVALID_FIELD_JSON = """
            {
              "cveId": "NOT-A-CVE",
              "severity": "LOW",
              "cvssScore": 15.0,
              "packageCoordinate": "invalid-no-colons",
              "rootCauseAnalysis": "Minor issue",
              "mitigationSteps": []
            }
            """;
}
