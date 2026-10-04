package phase03;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import phase03.ToolModelContracts.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 03 Exercise 01:
 * Autonomous Diagnostic Tool Sandboxing & Execution Bounding.
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
        return list;
    }

    private static Path createTempSandbox() throws IOException {
        Path temp = Files.createTempDirectory("phase03-sandbox-");
        ToolModelContracts.setupSandboxFixture(temp);
        return temp;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "readSource (Normal In-Sandbox File Inspection)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Iteration 1 context");

            // 1. Boundary check: null / blank path returns fail envelope (does not throw)
            ToolEnvelope<String> nullRes = tools.readSource(null);
            if (nullRes == null || nullRes.ok() || nullRes.error() == null) {
                return new ScenarioResult(1, name, false, "null path must return fail envelope with error message.");
            }

            ToolEnvelope<String> blankRes = tools.readSource("   ");
            if (blankRes == null || blankRes.ok() || blankRes.error() == null) {
                return new ScenarioResult(1, name, false, "blank path must return fail envelope with error message.");
            }

            // 2. Valid file read
            ToolEnvelope<String> validRes = tools.readSource("src/main/resources/application.properties");
            if (validRes == null || !validRes.ok() || validRes.data() == null) {
                return new ScenarioResult(1, name, false, "readSource failed on valid file: " + (validRes != null ? validRes.error() : "null"));
            }

            if (!validRes.data().contains("spring.datasource.hikari.maximum-pool-size=10")) {
                return new ScenarioResult(1, name, false, "File content mismatch. Expected hikari pool config, got:\n" + validRes.data());
            }

            if (tools.getCallCount() != 3) {
                return new ScenarioResult(1, name, false, "Expected callCount == 3 (2 invalid + 1 valid), got: " + tools.getCallCount());
            }

            return new ScenarioResult(1, name, true, "readSource cleanly read target configuration inside sandbox.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception during readSource: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "readSource (Path Traversal Sandboxing)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Iteration 1 context");

            List<String> escapeAttempts = List.of(
                    "../../etc/passwd",
                    "..\\..\\secret.env",
                    "/etc/shadow",
                    "src/../../.env",
                    "../outside.txt"
            );

            for (String escapePath : escapeAttempts) {
                ToolEnvelope<String> res = tools.readSource(escapePath);
                if (res == null || res.ok()) {
                    return new ScenarioResult(2, name, false, "Path traversal exploit succeeded! Path: " + escapePath);
                }
                if (!res.error().toLowerCase().contains("access denied")) {
                    return new ScenarioResult(2, name, false,
                            "Path traversal rejected with unexpected message (expected 'Access denied...'). Got: " + res.error());
                }
            }

            return new ScenarioResult(2, name, true, "Path traversal attacks cleanly blocked by sandbox root boundary validation.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Uncaught exception escaped during path traversal attempt: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "readSource (Missing File & Exception Isolation)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Iteration 1 context");

            ToolEnvelope<String> res = tools.readSource("src/missing/NonExistentService.java");
            if (res == null) {
                return new ScenarioResult(3, name, false, "readSource returned null for missing file.");
            }
            if (res.ok()) {
                return new ScenarioResult(3, name, false, "readSource returned ok=true for missing file.");
            }
            if (!res.error().toLowerCase().contains("file not found")) {
                return new ScenarioResult(3, name, false,
                        "Missing file error did not indicate 'File not found'. Got: " + res.error());
            }

            return new ScenarioResult(3, name, true, "Missing file safely caught and returned as ToolEnvelope.fail without crashing.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Uncaught exception escaped from missing file check: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "readSource (Maximum File Size Guard - 100 KB Cap)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Iteration 1 context");

            // logs/large_heap_dump.bin is ~110 KB (> 100 KB cap)
            ToolEnvelope<String> res = tools.readSource("logs/large_heap_dump.bin");
            if (res == null) {
                return new ScenarioResult(4, name, false, "readSource returned null for oversized file.");
            }
            if (res.ok()) {
                return new ScenarioResult(4, name, false, "Oversized file (110 KB) was accepted but must be blocked!");
            }
            if (!res.error().toLowerCase().contains("exceeds maximum allowable limit")) {
                return new ScenarioResult(4, name, false,
                        "Oversized file error message did not contain 'exceeds maximum allowable limit'. Got: " + res.error());
            }

            return new ScenarioResult(4, name, true, "Oversized binary/log file blocked by 100 KB safety cap.");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception during file size check: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "listRepositoryStructure (Safe Sandboxed Repository Listing)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Iteration 1 context");

            ToolEnvelope<List<String>> res = tools.listRepositoryStructure();
            if (res == null || !res.ok() || res.data() == null) {
                return new ScenarioResult(5, name, false, "listRepositoryStructure failed: " + (res != null ? res.error() : "null"));
            }

            List<String> files = res.data();

            // Must include safe project files:
            boolean hasProps = files.stream().anyMatch(f -> f.replace('\\', '/').contains("application.properties"));
            boolean hasJava = files.stream().anyMatch(f -> f.replace('\\', '/').contains("TargetApplication.java"));
            boolean hasPom = files.stream().anyMatch(f -> f.replace('\\', '/').contains("pom.xml"));

            if (!hasProps || !hasJava || !hasPom) {
                return new ScenarioResult(5, name, false, "Repository listing missing standard project files: " + files);
            }

            // Must NOT include forbidden files:
            boolean hasGit = files.stream().anyMatch(f -> f.contains(".git"));
            boolean hasEnv = files.stream().anyMatch(f -> f.contains(".env"));
            boolean hasBin = files.stream().anyMatch(f -> f.endsWith(".bin") || f.endsWith(".class"));

            if (hasGit || hasEnv || hasBin) {
                return new ScenarioResult(5, name, false,
                        "Repository listing leaked sensitive/forbidden files (.git, .env, or .bin): " + files);
            }

            return new ScenarioResult(5, name, true, "Repository structure listed safely without leaking sensitive or oversized files.");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception during listRepositoryStructure: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "BoundedToolExecution (Hard Turn Bound Cut-Off)";
        try {
            Path sandbox = createTempSandbox();
            // Bound set to 3 calls
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 3, "Iteration 1 context");

            // Calls 1, 2, 3 must succeed
            ToolEnvelope<String> c1 = tools.readSource("pom.xml");
            ToolEnvelope<String> c2 = tools.readSource("src/main/resources/application.properties");
            ToolEnvelope<String> c3 = tools.recallContext();

            if (!c1.ok() || !c2.ok() || !c3.ok()) {
                return new ScenarioResult(6, name, false, "Initial calls within bound (calls 1-3) failed.");
            }

            // Call 4 must be CUT OFF
            ToolEnvelope<String> c4 = tools.readSource("pom.xml");
            if (c4 == null || c4.ok()) {
                return new ScenarioResult(6, name, false, "Call 4 succeeded but bound is 3! Model was not cut off.");
            }
            if (!c4.error().contains("tool-call bound (3) exceeded — decide now")) {
                return new ScenarioResult(6, name, false,
                        "Call 4 cut-off message mismatch. Expected 'tool-call bound (3) exceeded — decide now', got: " + c4.error());
            }

            // Call 5 must also be cut off
            ToolEnvelope<List<String>> c5 = tools.listRepositoryStructure();
            if (c5 == null || c5.ok() || !c5.error().contains("tool-call bound (3) exceeded — decide now")) {
                return new ScenarioResult(6, name, false, "Call 5 was not cut off properly.");
            }

            return new ScenarioResult(6, name, true, "Tool-call bound strictly enforced (model cut off after bound exceeded).");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception during bound cut-off check: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "trajectoryAudit (Tool Call Event Logging)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 2, "Test context");

            tools.readSource("pom.xml");              // Event 1 (ok)
            tools.readSource("../../etc/shadow");     // Event 2 (fail: security)
            tools.readSource("pom.xml");              // Event 3 (fail: bound exceeded)

            List<ToolCallEvent> audit = tools.getTrajectoryAudit();
            if (audit == null || audit.size() != 3) {
                return new ScenarioResult(7, name, false, "Expected exactly 3 audit events, got: " + (audit != null ? audit.size() : "null"));
            }

            ToolCallEvent e1 = audit.get(0);
            ToolCallEvent e2 = audit.get(1);
            ToolCallEvent e3 = audit.get(2);

            if (e1.sequence() != 1 || !e1.ok() || !"readSource".equals(e1.action())) {
                return new ScenarioResult(7, name, false, "Event 1 mismatch: " + e1);
            }
            if (e2.sequence() != 2 || e2.ok() || !"readSource".equals(e2.action())) {
                return new ScenarioResult(7, name, false, "Event 2 mismatch: " + e2);
            }
            if (e3.sequence() != 3 || e3.ok() || !e3.detail().contains("bound")) {
                return new ScenarioResult(7, name, false, "Event 3 mismatch: " + e3);
            }

            return new ScenarioResult(7, name, true, "Every tool call logged into trajectory audit with sequence, parameters, and outcome.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception during trajectory audit check: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "springAi_ToolCallbackIntegration (Spring AI @Tool Resolution)";
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test diagnostic context");

            ToolCallback[] callbacks = ToolCallbacks.from(tools);
            if (callbacks == null || callbacks.length < 3) {
                return new ScenarioResult(8, name, false,
                        "Spring AI failed to resolve @Tool methods. Expected at least 3 callbacks, got: " + (callbacks != null ? callbacks.length : "null"));
            }

            Map<String, ToolCallback> callbackMap = new HashMap<>();
            for (ToolCallback cb : callbacks) {
                callbackMap.put(cb.getToolDefinition().name(), cb);
            }

            if (!callbackMap.containsKey("readSource") ||
                !callbackMap.containsKey("listRepositoryStructure") ||
                !callbackMap.containsKey("recallContext")) {
                return new ScenarioResult(8, name, false,
                        "Missing expected tool definitions in callbacks. Resolved: " + callbackMap.keySet());
            }

            // Execute readSource via Spring AI ToolCallback dispatcher
            ToolCallback readCallback = callbackMap.get("readSource");
            String resultJson = readCallback.call("{\"path\": \"pom.xml\"}");

            if (resultJson == null || !resultJson.contains("\"ok\":true") || !resultJson.contains("target-app")) {
                return new ScenarioResult(8, name, false,
                        "Spring AI ToolCallback dispatch failed or produced invalid envelope JSON:\n" + resultJson);
            }

            return new ScenarioResult(8, name, true, "Spring AI @Tool metadata resolved and callback executed successfully.");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception during Spring AI tool callback integration: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("                 PHASE 03 EXERCISE 01: VERIFICATION REPORT             ");
        System.out.println("======================================================================");
        int passed = 0;
        int failed = 0;

        for (ScenarioResult r : results) {
            System.out.printf("SCENARIO %d: %s%n", r.scenarioNumber(), r.name());
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
