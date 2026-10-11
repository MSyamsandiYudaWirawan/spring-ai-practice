package phase03;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import phase03.OpsToolContracts.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 03 Exercise 02:
 * Repetitive Spring AI @Tool Calling Drills (15 Repetitive Drills).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail, List<String> hints) {
        public ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {
            this(scenarioNumber, name, passed, errorDetail, List.of());
        }
    }

    public static void main(String[] args) {
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selectedScenario);
        printReport(results);

        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            fail("Verifier detected scenario failures. See printed report above.");
        }
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        List<ScenarioResult> list = new ArrayList<>();
        // Topic 1: Primitive & Bounded Range Parameters
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Enums, Structured Filters & Complex Collections
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Exception Isolation & Structured Tool Envelopes
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Stateful Rate Limiting & Forensic Trajectory Auditing
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Spring AI @Tool Resolution & ChatClient Dispatch
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Primitive & Bounded Range Parameters (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "restartPod (Primitive Parameters & Validation)";
        List<String> hints = List.of(
                "Annotate method with @Tool and parameters with @ToolParam.",
                "Throw IllegalArgumentException if namespace or podName is null or blank.",
                "Increment callCount and return true on success."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.restartPod(null, "api-pod-1");
                return new ScenarioResult(1, name, false, "null namespace must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.restartPod("prod", "  ");
                return new ScenarioResult(1, name, false, "blank podName must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            boolean restarted = tools.restartPod("prod", "auth-service-7d8b-xyz");
            if (!restarted) {
                return new ScenarioResult(1, name, false, "restartPod returned false on valid parameters", hints);
            }
            if (tools.getCallCount() != 3) {
                return new ScenarioResult(1, name, false, "Expected callCount == 3, got: " + tools.getCallCount(), hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "scaleReplicas (String + Integer Range Validation & DeploymentStatus Record)";
        List<String> hints = List.of(
                "Annotate method with @Tool and parameters with @ToolParam.",
                "Throw IllegalArgumentException if deployment is null/blank or replicas < 1 or > 50.",
                "Return new DeploymentStatus(deployment, replicas, \"SCALED\")."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.scaleReplicas("  ", 5);
                return new ScenarioResult(2, name, false, "blank deployment must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.scaleReplicas("order-service", 0);
                return new ScenarioResult(2, name, false, "replicas == 0 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.scaleReplicas("order-service", 51);
                return new ScenarioResult(2, name, false, "replicas == 51 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            DeploymentStatus status = tools.scaleReplicas("order-service", 8);
            if (status == null || !"order-service".equals(status.deployment()) || status.replicas() != 8 || !"SCALED".equals(status.state())) {
                return new ScenarioResult(2, name, false, "scaleReplicas returned unexpected DeploymentStatus: " + status, hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "drainNode (Node Name + Grace Period Bounds & NodeDrainResult Record)";
        List<String> hints = List.of(
                "Annotate method with @Tool and parameters with @ToolParam.",
                "Throw IllegalArgumentException if nodeName is null/blank or gracePeriodSeconds < 0 or > 3600.",
                "Return new NodeDrainResult(nodeName, gracePeriodSeconds, \"DRAINED\")."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.drainNode(null, 60);
                return new ScenarioResult(3, name, false, "null nodeName must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.drainNode("k8s-worker-01", -1);
                return new ScenarioResult(3, name, false, "negative gracePeriodSeconds must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.drainNode("k8s-worker-01", 3601);
                return new ScenarioResult(3, name, false, "gracePeriodSeconds > 3600 must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            NodeDrainResult res = tools.drainNode("k8s-worker-01", 300);
            if (res == null || !"k8s-worker-01".equals(res.nodeName()) || res.gracePeriodSeconds() != 300 || !"DRAINED".equals(res.status())) {
                return new ScenarioResult(3, name, false, "drainNode returned unexpected NodeDrainResult: " + res, hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Enums, Structured Filters & Complex Collections (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "updateLogLevel (String + LogLevel Enum Parameter & ConfigUpdateResult Record)";
        List<String> hints = List.of(
                "Annotate method with @Tool and parameters with @ToolParam.",
                "Throw IllegalArgumentException if service is null/blank or level is null.",
                "Return ConfigUpdateResult(service, level, true, \"Log level updated to \" + level)."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.updateLogLevel(null, LogLevel.DEBUG);
                return new ScenarioResult(4, name, false, "null service must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.updateLogLevel("auth-service", null);
                return new ScenarioResult(4, name, false, "null level must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            ConfigUpdateResult res = tools.updateLogLevel("payment-gateway", LogLevel.WARN);
            if (res == null || !"payment-gateway".equals(res.service()) || res.level() != LogLevel.WARN || !res.applied()) {
                return new ScenarioResult(4, name, false, "updateLogLevel returned invalid ConfigUpdateResult: " + res, hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "queryAuditLogs (Structured LogQueryFilter Record & List<LogEntry> Return)";
        List<String> hints = List.of(
                "Annotate method with @Tool and filter parameter with @ToolParam.",
                "Throw IllegalArgumentException if filter is null, filter.service() is null/blank, or filter.maxEntries() < 1.",
                "Return List of LogEntry matching the requested filter."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.queryAuditLogs(null);
                return new ScenarioResult(5, name, false, "null filter must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.queryAuditLogs(new LogQueryFilter("   ", LogLevel.INFO, 10, "timeout"));
                return new ScenarioResult(5, name, false, "blank filter service must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            LogQueryFilter filter = new LogQueryFilter("billing-service", LogLevel.ERROR, 5, "DB_TIMEOUT");
            List<LogEntry> entries = tools.queryAuditLogs(filter);
            if (entries == null || entries.isEmpty()) {
                return new ScenarioResult(5, name, false, "queryAuditLogs returned empty or null list", hints);
            }

            for (LogEntry e : entries) {
                if (!"billing-service".equals(e.service()) || e.level() == null || e.message() == null) {
                    return new ScenarioResult(5, name, false, "Invalid LogEntry in result: " + e, hints);
                }
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "batchRestartPods (Namespace String + List<String> Pods & Map<String, Boolean> Result)";
        List<String> hints = List.of(
                "Annotate method with @Tool and parameters with @ToolParam.",
                "Throw IllegalArgumentException if namespace is null/blank or podNames is null/empty.",
                "Return Map<String, Boolean> where every requested pod is mapped to true."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.batchRestartPods(null, List.of("pod-1"));
                return new ScenarioResult(6, name, false, "null namespace must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.batchRestartPods("prod", List.of());
                return new ScenarioResult(6, name, false, "empty podNames must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException ignored) {}

            Map<String, Boolean> res = tools.batchRestartPods("prod", List.of("pod-1", "pod-2", "pod-3"));
            if (res == null || res.size() != 3 || !Boolean.TRUE.equals(res.get("pod-1")) || !Boolean.TRUE.equals(res.get("pod-2"))) {
                return new ScenarioResult(6, name, false, "batchRestartPods returned invalid map: " + res, hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Exception Isolation & Structured Tool Envelopes (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "executeHealthProbe (Component Whitelist Validation & ToolEnvelope<HealthReport>)";
        List<String> hints = List.of(
                "Allowed components (case-insensitive): 'api-gateway', 'auth-service', 'database', 'cache'.",
                "If component is unknown or null/blank, return ToolEnvelope.fail(...) containing 'Unknown component'.",
                "On success return ToolEnvelope.ok(HealthReport)."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            ToolEnvelope<HealthReport> unknown = tools.executeHealthProbe("legacy-monolith");
            if (unknown == null || unknown.ok() || unknown.error() == null || !unknown.error().toLowerCase().contains("unknown component")) {
                return new ScenarioResult(7, name, false, "Unknown component must return ToolEnvelope.fail containing 'Unknown component'", hints);
            }

            ToolEnvelope<HealthReport> valid = tools.executeHealthProbe("api-gateway");
            if (valid == null || !valid.ok() || valid.data() == null || !"api-gateway".equalsIgnoreCase(valid.data().component())) {
                return new ScenarioResult(7, name, false, "Valid component health probe failed: " + (valid != null ? valid.error() : "null"), hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "triggerDatabaseSnapshot (Regex & Retention Range Validation & ToolEnvelope<String>)";
        List<String> hints = List.of(
                "Validate clusterId matches regex '^[a-z0-9-]+$'; error message must contain 'Invalid clusterId'.",
                "Validate retentionDays in [1..90]; error message must contain 'retentionDays'.",
                "On success return ToolEnvelope.ok(\"Snapshot snapshot-\" + clusterId + \"-20261001 created with \" + retentionDays + \"d retention\")."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            ToolEnvelope<String> badCluster = tools.triggerDatabaseSnapshot("INVALID_UPPERCASE#1", 30);
            if (badCluster == null || badCluster.ok() || badCluster.error() == null || !badCluster.error().toLowerCase().contains("invalid clusterid")) {
                return new ScenarioResult(8, name, false, "Regex violation must return ToolEnvelope.fail containing 'Invalid clusterId'", hints);
            }

            ToolEnvelope<String> badDays = tools.triggerDatabaseSnapshot("orders-db-replica", 120);
            if (badDays == null || badDays.ok() || badDays.error() == null || !badDays.error().toLowerCase().contains("retentiondays")) {
                return new ScenarioResult(8, name, false, "Retention out-of-range must return ToolEnvelope.fail containing 'retentionDays'", hints);
            }

            ToolEnvelope<String> valid = tools.triggerDatabaseSnapshot("orders-db-replica", 30);
            if (valid == null || !valid.ok() || valid.data() == null || !valid.data().contains("orders-db-replica")) {
                return new ScenarioResult(8, name, false, "Valid snapshot call failed: " + (valid != null ? valid.error() : "null"), hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "rollbackDeployment (Target Revision Validation & ToolEnvelope<String>)";
        List<String> hints = List.of(
                "Validate deployment is not null/blank and targetRevision >= 1.",
                "If targetRevision < 1, return ToolEnvelope.fail(...) containing 'Invalid revision'.",
                "On success return ToolEnvelope.ok(\"Rollback of \" + deployment + \" to revision \" + targetRevision + \" completed\")."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            ToolEnvelope<String> badRev = tools.rollbackDeployment("order-service", 0);
            if (badRev == null || badRev.ok() || badRev.error() == null || !badRev.error().toLowerCase().contains("invalid revision")) {
                return new ScenarioResult(9, name, false, "targetRevision < 1 must return ToolEnvelope.fail containing 'Invalid revision'", hints);
            }

            ToolEnvelope<String> valid = tools.rollbackDeployment("order-service", 3);
            if (valid == null || !valid.ok() || valid.data() == null || !valid.data().contains("revision 3")) {
                return new ScenarioResult(9, name, false, "Valid rollback call failed: " + (valid != null ? valid.error() : "null"), hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Stateful Rate Limiting & Forensic Trajectory Auditing (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "purgeCacheKeys (Stateful Rate Limiting - Max 3 Purges per Session)";
        List<String> hints = List.of(
                "Reject null or blank pattern with ToolEnvelope.fail(\"pattern must not be null or blank\").",
                "Allow at most 3 purges per session; on 4th call return ToolEnvelope.fail(...) containing 'Rate limit exceeded'.",
                "Return ToolEnvelope.ok(42) on success."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            ToolEnvelope<Integer> blank = tools.purgeCacheKeys("  ");
            if (blank == null || blank.ok() || !blank.error().contains("null or blank")) {
                return new ScenarioResult(10, name, false, "blank pattern must return fail envelope containing 'null or blank'", hints);
            }

            ToolEnvelope<Integer> p1 = tools.purgeCacheKeys("users:*");
            ToolEnvelope<Integer> p2 = tools.purgeCacheKeys("sessions:*");
            ToolEnvelope<Integer> p3 = tools.purgeCacheKeys("tokens:*");
            if (p1 == null || !p1.ok() || p2 == null || !p2.ok() || p3 == null || !p3.ok()) {
                return new ScenarioResult(10, name, false, "Purges 1-3 within limit failed", hints);
            }

            ToolEnvelope<Integer> p4 = tools.purgeCacheKeys("orders:*");
            if (p4 == null || p4.ok() || !p4.error().toLowerCase().contains("rate limit exceeded")) {
                return new ScenarioResult(10, name, false, "Purge 4 must be rejected with 'Rate limit exceeded'", hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "resetSession (Session Counter Reset & State Restoration)";
        List<String> hints = List.of(
                "resetSession() must reset callCount to 0, purgeCount to 0, and clear executionAudits.",
                "Verify next purge call succeeds cleanly after reset.",
                "Ensure state is properly restored for multi-turn sessions."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            tools.purgeCacheKeys("k1");
            tools.purgeCacheKeys("k2");
            tools.purgeCacheKeys("k3");
            ToolEnvelope<Integer> blocked = tools.purgeCacheKeys("k4");
            if (blocked == null || blocked.ok()) {
                return new ScenarioResult(11, name, false, "Call 4 should have been rate limited", hints);
            }

            tools.resetSession();
            if (tools.getCallCount() != 0 || tools.getExecutionAudits().size() != 0) {
                return new ScenarioResult(11, name, false, "resetSession() must clear callCount and executionAudits", hints);
            }

            ToolEnvelope<Integer> fresh = tools.purgeCacheKeys("k1");
            if (fresh == null || !fresh.ok()) {
                return new ScenarioResult(11, name, false, "Purge after resetSession() must succeed", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "trajectoryAudit (Forensic Trajectory Audit Trail Integrity)";
        List<String> hints = List.of(
                "Record ToolExecutionAudit on every invocation (both successful and failed).",
                "Log callIndex (callCount), toolName, argument map, success boolean, and summary.",
                "Expose audit trail via getExecutionAudits()."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            tools.restartPod("prod", "pod-1");               // Call 1
            tools.executeHealthProbe("unknown-service");      // Call 2 (fail)
            tools.scaleReplicas("order-service", 5);          // Call 3

            List<ToolExecutionAudit> audits = tools.getExecutionAudits();
            if (audits == null || audits.size() != 3) {
                return new ScenarioResult(12, name, false, "Expected exactly 3 audits, got: " + (audits != null ? audits.size() : "null"), hints);
            }

            ToolExecutionAudit a1 = audits.get(0);
            ToolExecutionAudit a2 = audits.get(1);
            ToolExecutionAudit a3 = audits.get(2);

            if (a1.callIndex() != 1 || !"restartPod".equals(a1.toolName()) || !a1.success()) {
                return new ScenarioResult(12, name, false, "Audit 1 mismatch: " + a1, hints);
            }
            if (a2.callIndex() != 2 || !"executeHealthProbe".equals(a2.toolName()) || a2.success()) {
                return new ScenarioResult(12, name, false, "Audit 2 mismatch: " + a2, hints);
            }
            if (a3.callIndex() != 3 || !"scaleReplicas".equals(a3.toolName()) || !a3.success()) {
                return new ScenarioResult(12, name, false, "Audit 3 mismatch: " + a3, hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Spring AI @Tool Resolution & ChatClient Dispatch (Drills 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "springAi_ReflectionDiscovery (Spring AI @Tool Metadata Resolution via ToolCallbacks)";
        List<String> hints = List.of(
                "Annotate all operational tool methods with @Tool(description = \"...\").",
                "Annotate all parameters with @ToolParam(description = \"...\").",
                "ToolCallbacks.from(tools) must discover at least 8 tools: restartPod, scaleReplicas, drainNode, updateLogLevel, queryAuditLogs, batchRestartPods, executeHealthProbe, triggerDatabaseSnapshot, rollbackDeployment, purgeCacheKeys."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            ToolCallback[] callbacks = ToolCallbacks.from(tools);
            if (callbacks == null || callbacks.length < 8) {
                return new ScenarioResult(13, name, false,
                        "Expected at least 8 discovered tool callbacks, got: " + (callbacks != null ? callbacks.length : "null"), hints);
            }

            Map<String, ToolCallback> callbackMap = new HashMap<>();
            for (ToolCallback cb : callbacks) {
                callbackMap.put(cb.getToolDefinition().name(), cb);
            }

            List<String> requiredTools = List.of("restartPod", "scaleReplicas", "drainNode", "updateLogLevel", "executeHealthProbe", "purgeCacheKeys");
            for (String req : requiredTools) {
                if (!callbackMap.containsKey(req)) {
                    return new ScenarioResult(13, name, false, "Missing expected tool callback: " + req + ". Discovered: " + callbackMap.keySet(), hints);
                }
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "springAi_ToolCallbackExecution (Dynamic ToolCallback Dispatch with Typed Records via JSON)";
        List<String> hints = List.of(
                "Spring AI ToolCallback.call(json) dynamically invokes tool method.",
                "Call scaleReplicas with JSON '{\"deployment\": \"order-service\", \"replicas\": 5}'.",
                "Verify result JSON contains 'order-service', '5', and '\"state\":\"SCALED\"'."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            ToolCallback[] callbacks = ToolCallbacks.from(tools);
            ToolCallback scaleCallback = Arrays.stream(callbacks)
                    .filter(cb -> "scaleReplicas".equals(cb.getToolDefinition().name()))
                    .findFirst()
                    .orElse(null);

            if (scaleCallback == null) {
                return new ScenarioResult(14, name, false, "scaleReplicas callback not found in ToolCallbacks.from(tools)", hints);
            }

            String resultJson = scaleCallback.call("{\"deployment\": \"order-service\", \"replicas\": 5}");
            if (resultJson == null || !resultJson.contains("order-service") || !resultJson.contains("SCALED")) {
                return new ScenarioResult(14, name, false, "scaleReplicas dispatch produced invalid JSON: " + resultJson, hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "springAi_ChatClientMultiToolIntegration (End-to-End ChatClient Multi-Tool Registration)";
        List<String> hints = List.of(
                "Verify restartPod is operational before building ChatClient.",
                "Register tools on ChatClient.builder(model).defaultTools(tools).build().",
                "Verify ChatClient executes multi-tool prompt cleanly."
        );
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();
            boolean check = tools.restartPod("prod", "test-pod");
            if (!check) {
                return new ScenarioResult(15, name, false, "restartPod must be operational before registering with ChatClient", hints);
            }

            FakeOpsChatModel model = new FakeOpsChatModel();
            ChatClient client = ChatClient.builder(model)
                    .defaultTools(tools)
                    .build();

            String response = client.prompt()
                    .user("Execute multi-tool cluster remediation")
                    .call()
                    .content();

            if (response == null || !response.contains("Automated remediation")) {
                return new ScenarioResult(15, name, false, "ChatClient response mismatch: " + response, hints);
            }
            if (model.getCallCount() != 1) {
                return new ScenarioResult(15, name, false, "ChatModel should have been invoked once, got: " + model.getCallCount(), hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 03 EXERCISE 02: REPETITIVE CLOUD OPS TOOL CALLING REPORT");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String status = r.passed() ? "[PASS]" : "[FAIL]";
            if (r.passed()) passed++;
            System.out.printf("  %s Scenario %02d: %s%n", status, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        System.out.printf("         --> HINT %d: %s%n", i + 1, r.hints().get(i));
                    }
                }
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        System.out.println("===============================================================================");
    }
}
