package phase03;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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
 * Autonomous Diagnostic Tool Sandboxing & Execution Bounding (15 Repetitive Drills).
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
        // Topic 1: Safe File System Reading & Sandbox Isolation
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: File Size, Sensitive Artifacts & Tree Listing
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Execution Bounding & Turn Scoping
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Subtree Filtering & Trajectory Auditing
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Spring AI @Tool Resolution & ChatClient Dispatch
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    private static Path createTempSandbox() throws IOException {
        Path temp = Files.createTempDirectory("phase03-sandbox-");
        ToolModelContracts.setupSandboxFixture(temp);
        return temp;
    }

    // =========================================================================
    // Topic 1: Safe File System Reading & Sandbox Isolation (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "readSource (Normal In-Sandbox File Inspection)";
        List<String> hints = List.of(
                "Resolve relative path against sandboxRoot and normalize it.",
                "Verify file is within sandboxRoot, exists, and is readable.",
                "Return ToolEnvelope.ok(content) and increment callCount."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<String> validRes = tools.readSource("src/main/resources/application.properties");
            if (validRes == null) {
                return new ScenarioResult(1, name, false, "readSource returned null", hints);
            }
            if (!validRes.ok() || validRes.data() == null) {
                return new ScenarioResult(1, name, false, "readSource failed on valid file: " + validRes.error(), hints);
            }
            if (!validRes.data().contains("spring.datasource.hikari.maximum-pool-size=10")) {
                return new ScenarioResult(1, name, false, "File content mismatch: " + validRes.data(), hints);
            }
            if (tools.getCallCount() != 1) {
                return new ScenarioResult(1, name, false, "Expected callCount == 1, got: " + tools.getCallCount(), hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "readSource (Path Traversal Sandboxing)";
        List<String> hints = List.of(
                "Sandbox root boundary check: normalized path must start with sandboxRoot.",
                "Block paths like '../../etc/passwd', '..\\\\secret.env', and absolute paths.",
                "Return ToolEnvelope.fail(\"Access denied: path escapes sandbox root\") without throwing."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

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
                    return new ScenarioResult(2, name, false, "Path traversal exploit succeeded! Path: " + escapePath, hints);
                }
                if (!res.error().toLowerCase().contains("access denied")) {
                    return new ScenarioResult(2, name, false, "Expected 'Access denied...' but got: " + res.error(), hints);
                }
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "readSource (Missing File & Blank Path Guards)";
        List<String> hints = List.of(
                "Check for null or blank path: return ToolEnvelope.fail(\"path must not be null or blank\").",
                "If file does not exist, return ToolEnvelope.fail(\"File not found: \" + path).",
                "Catch all IOExceptions and isolate them inside ToolEnvelope.fail without letting them escape."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<String> nullRes = tools.readSource(null);
            if (nullRes == null || nullRes.ok() || nullRes.error() == null || !nullRes.error().contains("null or blank")) {
                return new ScenarioResult(3, name, false, "null path must return fail envelope containing 'null or blank'", hints);
            }

            ToolEnvelope<String> blankRes = tools.readSource("   ");
            if (blankRes == null || blankRes.ok() || blankRes.error() == null || !blankRes.error().contains("null or blank")) {
                return new ScenarioResult(3, name, false, "blank path must return fail envelope containing 'null or blank'", hints);
            }

            ToolEnvelope<String> missingRes = tools.readSource("src/missing/NonExistentService.java");
            if (missingRes == null || missingRes.ok() || !missingRes.error().toLowerCase().contains("file not found")) {
                return new ScenarioResult(3, name, false, "Missing file error did not indicate 'File not found'", hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: File Size, Sensitive Artifacts & Tree Listing (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "readSource (Maximum File Size Guard - 100 KB Cap)";
        List<String> hints = List.of(
                "Before reading content, check Files.size(resolved) > maxFileSizeBytes.",
                "If exceeded, return ToolEnvelope.fail(...) containing 'exceeds maximum allowable limit'.",
                "Do not load oversized byte contents into memory."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<String> res = tools.readSource("logs/large_heap_dump.bin");
            if (res == null || res.ok()) {
                return new ScenarioResult(4, name, false, "Oversized file (110 KB) was accepted but must be blocked!", hints);
            }
            if (!res.error().toLowerCase().contains("exceeds maximum allowable limit")) {
                return new ScenarioResult(4, name, false, "Error message must contain 'exceeds maximum allowable limit', got: " + res.error(), hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "readSource (Sensitive Configuration Quarantine - .git & .env)";
        List<String> hints = List.of(
                "Check if requested relative path contains '.git' or '.env'.",
                "Quarantine access by returning ToolEnvelope.fail(\"Access denied: access to sensitive configuration is prohibited\").",
                "Ensure sensitive environment and version control files are never read."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<String> envRes = tools.readSource(".env");
            if (envRes == null || envRes.ok() || !envRes.error().toLowerCase().contains("sensitive configuration")) {
                return new ScenarioResult(5, name, false, "Access to .env must be rejected with sensitive configuration denial", hints);
            }

            ToolEnvelope<String> gitRes = tools.readSource(".git/config");
            if (gitRes == null || gitRes.ok() || !gitRes.error().toLowerCase().contains("sensitive configuration")) {
                return new ScenarioResult(5, name, false, "Access to .git/config must be rejected with sensitive configuration denial", hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "listRepositoryStructure (Safe Sandboxed Repository File Tree Listing)";
        List<String> hints = List.of(
                "Walk sandboxRoot recursively with Files.walk, filtering for regular files.",
                "Format relative paths using forward slashes '/' and sort alphabetically.",
                "Filter out .git, .env, .bin, .class, and .jar files."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<List<String>> res = tools.listRepositoryStructure();
            if (res == null || !res.ok() || res.data() == null) {
                return new ScenarioResult(6, name, false, "listRepositoryStructure failed: " + (res != null ? res.error() : "null"), hints);
            }

            List<String> files = res.data();
            boolean hasProps = files.stream().anyMatch(f -> f.contains("application.properties"));
            boolean hasJava = files.stream().anyMatch(f -> f.contains("TargetApplication.java"));
            boolean hasPom = files.stream().anyMatch(f -> f.contains("pom.xml"));

            if (!hasProps || !hasJava || !hasPom) {
                return new ScenarioResult(6, name, false, "Repository listing missing standard project files: " + files, hints);
            }

            boolean hasGit = files.stream().anyMatch(f -> f.contains(".git"));
            boolean hasEnv = files.stream().anyMatch(f -> f.contains(".env"));
            boolean hasBin = files.stream().anyMatch(f -> f.endsWith(".bin") || f.endsWith(".class"));

            if (hasGit || hasEnv || hasBin) {
                return new ScenarioResult(6, name, false, "Repository listing leaked sensitive/forbidden files: " + files, hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Execution Bounding & Turn Scoping (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "BoundedToolExecution (Hard Turn Bound Cut-Off)";
        List<String> hints = List.of(
                "Increment callCount on every tool invocation.",
                "If callCount > bound: cut off with ToolEnvelope.fail(\"tool-call bound (\" + bound + \") exceeded — decide now\").",
                "Ensure all subsequent calls continue to be cut off."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 3, "Test context");

            ToolEnvelope<String> c1 = tools.readSource("pom.xml");
            ToolEnvelope<String> c2 = tools.readSource("src/main/resources/application.properties");
            ToolEnvelope<String> c3 = tools.recallContext();

            if (c1 == null || !c1.ok() || c2 == null || !c2.ok() || c3 == null || !c3.ok()) {
                return new ScenarioResult(7, name, false, "Initial calls within bound (calls 1-3) failed", hints);
            }

            ToolEnvelope<String> c4 = tools.readSource("pom.xml");
            if (c4 == null || c4.ok()) {
                return new ScenarioResult(7, name, false, "Call 4 succeeded but bound is 3! Cut-off failed.", hints);
            }
            if (!c4.error().contains("tool-call bound (3) exceeded — decide now")) {
                return new ScenarioResult(7, name, false, "Expected 'tool-call bound (3) exceeded — decide now', got: " + c4.error(), hints);
            }

            ToolEnvelope<List<String>> c5 = tools.listRepositoryStructure();
            if (c5 == null || c5.ok() || !c5.error().contains("tool-call bound (3) exceeded — decide now")) {
                return new ScenarioResult(7, name, false, "Call 5 cut-off message mismatch: " + (c5 != null ? c5.error() : "null"), hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "recallContext (Context Recall with Turn Enforcement)";
        List<String> hints = List.of(
                "Check bound inside recallContext().",
                "Increment callCount and return ToolEnvelope.ok(fullContext).",
                "Cut off with bound exceeded message if callCount > bound."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 1, "Diagnostic Context JFR 42");

            ToolEnvelope<String> c1 = tools.recallContext();
            if (c1 == null || !c1.ok() || !"Diagnostic Context JFR 42".equals(c1.data())) {
                return new ScenarioResult(8, name, false, "recallContext failed to return expected context string", hints);
            }

            ToolEnvelope<String> c2 = tools.recallContext();
            if (c2 == null || c2.ok() || !c2.error().contains("tool-call bound (1) exceeded — decide now")) {
                return new ScenarioResult(8, name, false, "Second recallContext call must be cut off by bound of 1", hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "resetTurn (Turn Counter Reset & Multi-Phase Bounding)";
        List<String> hints = List.of(
                "resetTurn() must reset callCount to 0.",
                "Verify remaining turns resets to initial bound.",
                "Ensure next call after reset succeeds cleanly."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 2, "Test context");

            tools.readSource("pom.xml");
            tools.readSource("src/main/resources/application.properties");
            ToolEnvelope<String> over = tools.readSource("pom.xml");
            if (over == null || over.ok()) {
                return new ScenarioResult(9, name, false, "Turn bound should have been exceeded on call 3", hints);
            }

            tools.resetTurn();
            if (tools.getCallCount() != 0) {
                return new ScenarioResult(9, name, false, "resetTurn() must reset callCount to 0, got: " + tools.getCallCount(), hints);
            }

            ToolEnvelope<String> fresh = tools.readSource("pom.xml");
            if (fresh == null || !fresh.ok()) {
                return new ScenarioResult(9, name, false, "Call after resetTurn() must succeed", hints);
            }
            if (tools.getCallCount() != 1) {
                return new ScenarioResult(9, name, false, "callCount must be 1 after single call following reset", hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Subtree Filtering & Trajectory Auditing (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "searchSource (Sandboxed Text Query Search)";
        List<String> hints = List.of(
                "Enforce turn bound; reject null or blank query.",
                "Search non-binary files inside sandbox matching query string.",
                "Return ToolEnvelope.ok(List<String>) of relative paths with forward slashes."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<List<String>> nullRes = tools.searchSource(null);
            if (nullRes == null || nullRes.ok() || !nullRes.error().contains("null or blank")) {
                return new ScenarioResult(10, name, false, "searchSource must reject null query", hints);
            }

            ToolEnvelope<List<String>> hitRes = tools.searchSource("hikari");
            if (hitRes == null || !hitRes.ok() || hitRes.data() == null) {
                return new ScenarioResult(10, name, false, "searchSource failed on valid query 'hikari'", hints);
            }

            boolean foundProps = hitRes.data().stream().anyMatch(p -> p.contains("application.properties"));
            if (!foundProps) {
                return new ScenarioResult(10, name, false, "searchSource did not locate application.properties containing 'hikari'", hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "recordDiagnosticFinding (Diagnostic Finding Persistence)";
        List<String> hints = List.of(
                "Enforce turn bound; validate finding is not null or blank.",
                "Store finding in internal list exposed via getFindings().",
                "Return ToolEnvelope.ok(\"Recorded: \" + finding)."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test context");

            ToolEnvelope<String> blankRes = tools.recordDiagnosticFinding("  ");
            if (blankRes == null || blankRes.ok() || !blankRes.error().contains("null or blank")) {
                return new ScenarioResult(11, name, false, "recordDiagnosticFinding must reject blank string", hints);
            }

            ToolEnvelope<String> recRes = tools.recordDiagnosticFinding("HikariCP pool exhaustion detected at 10 conns");
            if (recRes == null || !recRes.ok() || !recRes.data().contains("Recorded:")) {
                return new ScenarioResult(11, name, false, "recordDiagnosticFinding failed to record valid hypothesis", hints);
            }

            if (tools.getFindings().size() != 1 || !tools.getFindings().get(0).contains("pool exhaustion")) {
                return new ScenarioResult(11, name, false, "tools.getFindings() does not contain recorded finding", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "trajectoryAudit (Tool Call Event Logging)";
        List<String> hints = List.of(
                "Record ToolCallEvent on EVERY tool call (successful, rejected, or cut-off).",
                "Capture sequence number (callCount), action name, parameters map, ok flag, detail, and timestamp.",
                "Expose immutable list via getTrajectoryAudit()."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 2, "Test context");

            tools.readSource("pom.xml");              // Event 1 (ok)
            tools.readSource("../../etc/shadow");     // Event 2 (fail: security)
            tools.readSource("pom.xml");              // Event 3 (fail: bound exceeded)

            List<ToolCallEvent> audit = tools.getTrajectoryAudit();
            if (audit == null || audit.size() != 3) {
                return new ScenarioResult(12, name, false, "Expected exactly 3 audit events, got: " + (audit != null ? audit.size() : "null"), hints);
            }

            ToolCallEvent e1 = audit.get(0);
            ToolCallEvent e2 = audit.get(1);
            ToolCallEvent e3 = audit.get(2);

            if (e1.sequence() != 1 || !e1.ok() || !"readSource".equals(e1.action())) {
                return new ScenarioResult(12, name, false, "Event 1 mismatch: " + e1, hints);
            }
            if (e2.sequence() != 2 || e2.ok() || !"readSource".equals(e2.action())) {
                return new ScenarioResult(12, name, false, "Event 2 mismatch: " + e2, hints);
            }
            if (e3.sequence() != 3 || e3.ok() || !e3.detail().contains("bound")) {
                return new ScenarioResult(12, name, false, "Event 3 mismatch: " + e3, hints);
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
        String name = "springAi_ToolCallbackDiscovery (Spring AI @Tool Resolution via ToolCallbacks)";
        List<String> hints = List.of(
                "Annotate tool methods with @Tool(description = \"...\").",
                "Annotate parameters with @ToolParam(description = \"...\").",
                "ToolCallbacks.from(tools) must resolve at least 5 tools: readSource, listRepositoryStructure, recallContext, searchSource, recordDiagnosticFinding."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test diagnostic context");

            ToolCallback[] callbacks = ToolCallbacks.from(tools);
            if (callbacks == null || callbacks.length < 5) {
                return new ScenarioResult(13, name, false,
                        "Expected at least 5 resolved tool callbacks, got: " + (callbacks != null ? callbacks.length : "null"), hints);
            }

            Map<String, ToolCallback> callbackMap = new HashMap<>();
            for (ToolCallback cb : callbacks) {
                callbackMap.put(cb.getToolDefinition().name(), cb);
            }

            List<String> expectedTools = List.of("readSource", "listRepositoryStructure", "recallContext", "searchSource", "recordDiagnosticFinding");
            for (String expected : expectedTools) {
                if (!callbackMap.containsKey(expected)) {
                    return new ScenarioResult(13, name, false, "Missing expected tool callback: " + expected + ". Found: " + callbackMap.keySet(), hints);
                }
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "springAi_ToolCallbackExecution (Dynamic ToolCallback Dispatch via JSON)";
        List<String> hints = List.of(
                "Spring AI ToolCallback.call(json) dispatches method call dynamically.",
                "Pass JSON string arguments '{\"path\": \"pom.xml\"}'.",
                "Verify return envelope serializes to valid JSON containing '\"ok\":true' and expected payload."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test diagnostic context");

            ToolCallback[] callbacks = ToolCallbacks.from(tools);
            ToolCallback readCallback = Arrays.stream(callbacks)
                    .filter(cb -> "readSource".equals(cb.getToolDefinition().name()))
                    .findFirst()
                    .orElse(null);

            if (readCallback == null) {
                return new ScenarioResult(14, name, false, "readSource callback not found in ToolCallbacks.from(tools)", hints);
            }

            String resultJson = readCallback.call("{\"path\": \"pom.xml\"}");
            if (resultJson == null || !resultJson.contains("\"ok\":true") || !resultJson.contains("target-app")) {
                return new ScenarioResult(14, name, false, "Dispatch failed or produced invalid envelope JSON: " + resultJson, hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "springAi_ChatClientIntegration (End-to-End ChatClient Tool Registration)";
        List<String> hints = List.of(
                "ChatClient.builder(chatModel).defaultTools(tools).build() registers all tools.",
                "Execute chat prompt with registered tools.",
                "Verify ChatClient executes successfully without throwing."
        );
        try {
            Path sandbox = createTempSandbox();
            BoundedDiagnosticToolsUnderTest tools = new BoundedDiagnosticToolsUnderTest(sandbox, 10, "Test diagnostic context");
            FakeToolChatModel model = new FakeToolChatModel();

            ToolEnvelope<String> toolCheck = tools.readSource("pom.xml");
            if (toolCheck == null || !toolCheck.ok()) {
                return new ScenarioResult(15, name, false, "readSource must be operational before registering with ChatClient", hints);
            }

            ChatClient client = ChatClient.builder(model)
                    .defaultTools(tools)
                    .build();

            String response = client.prompt()
                    .user("Diagnose target repository configuration")
                    .call()
                    .content();

            if (response == null || !response.contains("Investigation complete")) {
                return new ScenarioResult(15, name, false, "ChatClient response mismatch: " + response, hints);
            }
            if (model.getCallCount() != 1) {
                return new ScenarioResult(15, name, false, "ChatModel should have been called once, got: " + model.getCallCount(), hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 03 EXERCISE 01: AUTONOMOUS TOOL SANDBOXING & BOUNDING REPORT");
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
