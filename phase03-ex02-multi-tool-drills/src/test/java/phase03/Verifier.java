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
 * Repetitive Spring AI @Tool Calling Drills (10 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

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
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "restartPod (Primitive Parameters & Validation)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            // 1. Boundary checks: null / blank throws IllegalArgumentException
            try {
                tools.restartPod(null, "api-pod-1");
                return new ScenarioResult(1, name, false, "null namespace must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.restartPod("prod", "  ");
                return new ScenarioResult(1, name, false, "blank podName must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            // 2. Valid invocation
            boolean restarted = tools.restartPod("prod", "auth-service-7d8b-xyz");
            if (!restarted) {
                return new ScenarioResult(1, name, false, "restartPod returned false on valid parameters.");
            }

            if (tools.getCallCount() != 3) {
                return new ScenarioResult(1, name, false, "Expected callCount == 3, got: " + tools.getCallCount());
            }

            return new ScenarioResult(1, name, true, "restartPod validated boundaries and executed cleanly.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception during restartPod: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "scaleReplicas (Numeric Range & Record Return)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            // 1. Boundary checks
            try {
                tools.scaleReplicas("", 5);
                return new ScenarioResult(2, name, false, "blank deployment must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.scaleReplicas("checkout", 0);
                return new ScenarioResult(2, name, false, "replicas < 1 must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            try {
                tools.scaleReplicas("checkout", 100);
                return new ScenarioResult(2, name, false, "replicas > 50 must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            // 2. Valid call
            DeploymentStatus status = tools.scaleReplicas("checkout-service", 8);
            if (status == null || !"checkout-service".equals(status.deployment()) || status.replicas() != 8 || !"SCALED".equals(status.state())) {
                return new ScenarioResult(2, name, false, "scaleReplicas returned unexpected status: " + status);
            }

            return new ScenarioResult(2, name, true, "scaleReplicas validated bounds [1..50] and returned typed DeploymentStatus record.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception during scaleReplicas: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "updateLogLevel (Enum Parameter Dispatch)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.updateLogLevel("order-api", null);
                return new ScenarioResult(3, name, false, "null level must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            ConfigUpdateResult res = tools.updateLogLevel("order-api", LogLevel.DEBUG);
            if (res == null || !res.applied() || res.level() != LogLevel.DEBUG || !"order-api".equals(res.service())) {
                return new ScenarioResult(3, name, false, "updateLogLevel returned unexpected result: " + res);
            }

            return new ScenarioResult(3, name, true, "updateLogLevel cleanly processed LogLevel enum parameter.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception during updateLogLevel: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "queryAuditLogs (Structured Record Input Schema & List Return)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            try {
                tools.queryAuditLogs(null);
                return new ScenarioResult(4, name, false, "null filter must throw IllegalArgumentException.");
            } catch (IllegalArgumentException ignored) {}

            LogQueryFilter filter = new LogQueryFilter("payment-gateway", LogLevel.WARN, 5, "timeout");
            List<LogEntry> entries = tools.queryAuditLogs(filter);

            if (entries == null || entries.isEmpty()) {
                return new ScenarioResult(4, name, false, "queryAuditLogs returned null or empty list for valid filter.");
            }

            if (!"payment-gateway".equals(entries.get(0).service())) {
                return new ScenarioResult(4, name, false, "LogEntry service mismatch: " + entries.get(0));
            }

            return new ScenarioResult(4, name, true, "queryAuditLogs accepted record input schema and returned strongly typed List<LogEntry>.");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception during queryAuditLogs: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "executeHealthProbe (Envelope Wrapping & Exception Isolation)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            // 1. Valid component
            ToolEnvelope<HealthReport> okRes = tools.executeHealthProbe("database");
            if (okRes == null || !okRes.ok() || okRes.data() == null) {
                return new ScenarioResult(5, name, false, "executeHealthProbe failed on valid component: " + okRes);
            }
            if (!"database".equals(okRes.data().component()) || !"HEALTHY".equals(okRes.data().status())) {
                return new ScenarioResult(5, name, false, "HealthReport data mismatch: " + okRes.data());
            }

            // 2. Unknown component returns fail envelope (does not throw)
            ToolEnvelope<HealthReport> failRes = tools.executeHealthProbe("unknown-queue");
            if (failRes == null || failRes.ok() || failRes.error() == null || failRes.error().isBlank()) {
                return new ScenarioResult(5, name, false, "Unknown component must return ToolEnvelope.fail with descriptive error.");
            }
            if (!failRes.error().toLowerCase().contains("unknown component") && !failRes.error().toLowerCase().contains("unknown")) {
                return new ScenarioResult(5, name, false, "Error message must mention 'Unknown component'. Got: " + failRes.error());
            }

            return new ScenarioResult(5, name, true, "executeHealthProbe safely isolated errors into ToolEnvelope.");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception escaped executeHealthProbe: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "triggerDatabaseSnapshot (Regex Validation & Envelope Return)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            // Invalid cluster regex
            ToolEnvelope<String> badCluster = tools.triggerDatabaseSnapshot("INVALID_CLUSTER_NAME!!!", 14);
            if (badCluster == null || badCluster.ok() || badCluster.error() == null || badCluster.error().isBlank()) {
                return new ScenarioResult(6, name, false, "Invalid clusterId must return fail envelope with format warning.");
            }
            if (!badCluster.error().toLowerCase().contains("invalid clusterid") && !badCluster.error().toLowerCase().contains("cluster")) {
                return new ScenarioResult(6, name, false, "Error message must mention 'Invalid clusterId'. Got: " + badCluster.error());
            }

            // Invalid retention range
            ToolEnvelope<String> badDays = tools.triggerDatabaseSnapshot("pg-master", 365);
            if (badDays == null || badDays.ok() || badDays.error() == null || badDays.error().isBlank()) {
                return new ScenarioResult(6, name, false, "retentionDays > 90 must return fail envelope.");
            }
            if (!badDays.error().toLowerCase().contains("retentiondays") && !badDays.error().toLowerCase().contains("retention")) {
                return new ScenarioResult(6, name, false, "Error message must mention 'retentionDays'. Got: " + badDays.error());
            }

            // Valid snapshot
            ToolEnvelope<String> valid = tools.triggerDatabaseSnapshot("pg-orders", 30);
            if (valid == null || !valid.ok() || valid.data() == null || !valid.data().contains("pg-orders")) {
                return new ScenarioResult(6, name, false, "Valid snapshot failed: " + valid);
            }

            return new ScenarioResult(6, name, true, "triggerDatabaseSnapshot validated regex pattern and retention bounds.");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception during triggerDatabaseSnapshot: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "purgeCacheKeys (Stateful Quota & Rate-Limiting Guard)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            // Calls 1, 2, 3 succeed
            ToolEnvelope<Integer> p1 = tools.purgeCacheKeys("user:*");
            ToolEnvelope<Integer> p2 = tools.purgeCacheKeys("session:*");
            ToolEnvelope<Integer> p3 = tools.purgeCacheKeys("cart:*");

            if (p1 == null || !p1.ok() || p2 == null || !p2.ok() || p3 == null || !p3.ok()) {
                return new ScenarioResult(7, name, false, "Initial 3 purges within quota failed.");
            }

            // Call 4 must be rate-limited
            ToolEnvelope<Integer> p4 = tools.purgeCacheKeys("token:*");
            if (p4 == null || p4.ok() || p4.error() == null || p4.error().isBlank()) {
                return new ScenarioResult(7, name, false, "Call 4 succeeded, but quota is 3 purges per session!");
            }
            if (!p4.error().toLowerCase().contains("rate limit")) {
                return new ScenarioResult(7, name, false, "Call 4 error message must mention 'Rate limit exceeded'. Got: " + p4.error());
            }

            return new ScenarioResult(7, name, true, "purgeCacheKeys enforced strict 3-call session rate limit.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception during purgeCacheKeys: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "springAi_ReflectionDiscovery (ToolCallbacks.from Metadata Extraction)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();
            ToolCallback[] callbacks = ToolCallbacks.from(tools);

            if (callbacks == null || callbacks.length < 7) {
                return new ScenarioResult(8, name, false,
                        "Expected at least 7 @Tool callbacks resolved, got: " + (callbacks != null ? callbacks.length : "null"));
            }

            Set<String> toolNames = new HashSet<>();
            for (ToolCallback cb : callbacks) {
                toolNames.add(cb.getToolDefinition().name());
            }

            List<String> expectedTools = List.of(
                    "restartPod",
                    "scaleReplicas",
                    "updateLogLevel",
                    "queryAuditLogs",
                    "executeHealthProbe",
                    "triggerDatabaseSnapshot",
                    "purgeCacheKeys"
            );

            for (String expected : expectedTools) {
                if (!toolNames.contains(expected)) {
                    return new ScenarioResult(8, name, false, "Missing @Tool definition: " + expected + ". Resolved: " + toolNames);
                }
            }

            // Direct execution of scaleReplicas via Spring AI callback dispatcher
            ToolCallback scaleCb = Arrays.stream(callbacks)
                    .filter(c -> "scaleReplicas".equals(c.getToolDefinition().name()))
                    .findFirst()
                    .orElseThrow();

            String jsonResult = scaleCb.call("{\"deployment\": \"payment-service\", \"replicas\": 6}");
            if (jsonResult == null || !jsonResult.contains("payment-service") || !jsonResult.contains("6")) {
                return new ScenarioResult(8, name, false, "Spring AI callback execution failed. Got:\n" + jsonResult);
            }

            return new ScenarioResult(8, name, true, "All 7 @Tool definitions discovered and callable via Spring AI reflection.");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception during ToolCallbacks discovery: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "chatClient_ToolRegistration (ChatClient Tool Configuration)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();
            ToolCallback[] callbacks = ToolCallbacks.from(tools);
            if (callbacks == null || callbacks.length < 7) {
                return new ScenarioResult(9, name, false,
                        "CloudOpsToolsUnderTest must have at least 7 @Tool methods before registering with ChatClient. Found: "
                                + (callbacks != null ? callbacks.length : 0));
            }

            FakeOpsChatModel fakeModel = new FakeOpsChatModel();
            ChatClient client = ChatClient.builder(fakeModel)
                    .defaultTools(tools)
                    .build();

            String response = client.prompt()
                    .user("Check database cluster health and scale billing if needed")
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return new ScenarioResult(9, name, false, "ChatClient call returned null or blank response.");
            }

            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(9, name, false, "Expected 1 prompt captured by fake model, got: " + fakeModel.getCallCount());
            }

            return new ScenarioResult(9, name, true, "ChatClient successfully configured with defaultTools and dispatched prompt.");
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception during ChatClient tool registration: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "trajectoryAudit_ComprehensiveLogging (Audit Trail Across All Tools)";
        try {
            CloudOpsToolsUnderTest tools = new CloudOpsToolsUnderTest();

            tools.restartPod("prod", "web-1");
            tools.scaleReplicas("web-app", 3);
            tools.updateLogLevel("web-app", LogLevel.WARN);
            tools.executeHealthProbe("api-gateway");
            tools.triggerDatabaseSnapshot("pg-main", 7);
            tools.purgeCacheKeys("cache:*");

            List<ToolExecutionAudit> audits = tools.getExecutionAudits();
            if (audits == null || audits.size() != 6) {
                return new ScenarioResult(10, name, false, "Expected 6 execution audits, got: " + (audits != null ? audits.size() : "null"));
            }

            for (int i = 0; i < audits.size(); i++) {
                ToolExecutionAudit audit = audits.get(i);
                if (audit.callIndex() != (i + 1)) {
                    return new ScenarioResult(10, name, false, "Audit sequence mismatch at index " + i + ": " + audit);
                }
                if (!audit.success()) {
                    return new ScenarioResult(10, name, false, "Audit reported failure for valid call: " + audit);
                }
            }

            return new ScenarioResult(10, name, true, "Complete audit trail recorded across all 6 tool executions.");
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception during trajectoryAudit verification: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("                 PHASE 03 EXERCISE 02: VERIFICATION REPORT             ");
        System.out.println("======================================================================");
        int passed = 0;
        int failed = 0;

        for (ScenarioResult r : results) {
            System.out.printf("SCENARIO %2d: %s%n", r.scenarioNumber(), r.name());
            if (r.passed()) {
                System.out.println("  [PASS] " + r.errorDetail());
                passed++;
            } else {
                System.out.println("  [FAIL] " + r.errorDetail());
                failed++;
            }
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.printf("SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
        if (failed == 0) {
            System.out.println("VERDICT: ALL SCENARIOS PASSED (exit code 0)");
        } else {
            System.out.println("VERDICT: FINDINGS DETECTED (exit code 99)");
        }
        System.out.println("======================================================================");
        System.out.println();
    }
}
