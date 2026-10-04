package phase05;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import phase05.HarnessContracts.*;
import phase05.OfflineHarnessUnderTest.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 05 Exercise 01:
 * Deterministic Offline Test Harness & LLM Boundary Seams (10 Scenarios).
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
        String name = "ScriptedChatPort (FIFO Response Queue Test Double)";
        try {
            ScriptedChatPort port = new ScriptedChatPort();
            port.enqueue("Initial investigation proposal", 50, 30);
            port.enqueue("Follow-up mitigation plan", 40, 20);

            ChatResult res1 = port.chat("sys", "user 1");
            if (!"Initial investigation proposal".equals(res1.text()) || res1.tokensIn() != 50 || res1.tokensOut() != 30) {
                return new ScenarioResult(1, name, false, "First response mismatch: " + res1);
            }
            if (port.callCount() != 1) {
                return new ScenarioResult(1, name, false, "callCount expected 1, got: " + port.callCount());
            }

            ChatResult res2 = port.chat("sys", "user 2");
            if (!"Follow-up mitigation plan".equals(res2.text()) || res2.tokensIn() != 40 || res2.tokensOut() != 20) {
                return new ScenarioResult(1, name, false, "Second response mismatch: " + res2);
            }
            if (port.callCount() != 2) {
                return new ScenarioResult(1, name, false, "callCount expected 2, got: " + port.callCount());
            }

            // Third call should fail with exhausted queue exception
            boolean exhausted = false;
            try {
                port.chat("sys", "user 3");
            } catch (IllegalStateException ise) {
                exhausted = true;
            }
            if (!exhausted) {
                return new ScenarioResult(1, name, false, "Calling chat on exhausted queue must throw IllegalStateException");
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "PredicateRoutingChatPort (Dynamic Pattern Matching Test Double)";
        try {
            PredicateRoutingChatPort port = new PredicateRoutingChatPort();
            port.when(user -> user.contains("metrics"), new ChatResult("Metrics: CPU 94%, Heap 88%", 30, 20));
            port.when(user -> user.contains("threads"), new ChatResult("ThreadDump: 42 threads blocked", 35, 25));
            port.setDefaultResult(new ChatResult("Generic diagnostic response", 10, 10));

            ChatResult resMetrics = port.chat("sys", "Check system metrics now");
            if (!resMetrics.text().contains("CPU 94%")) {
                return new ScenarioResult(2, name, false, "Expected metrics response, got: " + resMetrics.text());
            }

            ChatResult resThreads = port.chat("sys", "Analyze threads state");
            if (!resThreads.text().contains("42 threads blocked")) {
                return new ScenarioResult(2, name, false, "Expected threads response, got: " + resThreads.text());
            }

            ChatResult resFallback = port.chat("sys", "Arbitrary unknown prompt");
            if (!resFallback.text().contains("Generic diagnostic response")) {
                return new ScenarioResult(2, name, false, "Expected default response, got: " + resFallback.text());
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "FaultInjectingChatPort (Simulated Transient Fault & Retry Double)";
        try {
            FaultInjectingChatPort port = new FaultInjectingChatPort();
            port.enqueueFault(new SimulatedUpstreamException("503 Service Unavailable"));
            port.enqueueResult("Recovery response after retry", 40, 25);

            // Turn 1: Throws 503
            boolean threwFault = false;
            try {
                port.chat("sys", "query upstream");
            } catch (SimulatedUpstreamException sue) {
                threwFault = true;
            }
            if (!threwFault) {
                return new ScenarioResult(3, name, false, "Expected SimulatedUpstreamException on first call");
            }

            // Turn 2: Recovers
            ChatResult res = port.chat("sys", "retry upstream");
            if (!"Recovery response after retry".equals(res.text())) {
                return new ScenarioResult(3, name, false, "Expected recovered result on second call, got: " + res.text());
            }
            if (port.callCount() != 2) {
                return new ScenarioResult(3, name, false, "Expected callCount 2, got: " + port.callCount());
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "PromptCapturingChatPort (Outgoing Prompt Spy & Inspection Seam)";
        try {
            ScriptedChatPort target = new ScriptedChatPort();
            target.enqueue("Response 1", 10, 10);
            target.enqueue("Response 2", 10, 10);

            PromptCapturingChatPort spy = new PromptCapturingChatPort(target);

            Object sampleTool = new Object() {
                @Tool(name = "testTool")
                public String run() { return "ok"; }
            };

            spy.chat("SYSTEM: SRE agent", "USER: triage incident #42", List.of(sampleTool));
            spy.chat("SYSTEM: SRE agent", "USER: verify mitigation", List.of());

            if (spy.getCallCount() != 2) {
                return new ScenarioResult(4, name, false, "getCallCount expected 2, got: " + spy.getCallCount());
            }

            List<PromptCapture> captures = spy.getCapturedPrompts();
            if (captures.size() != 2) {
                return new ScenarioResult(4, name, false, "getCapturedPrompts size expected 2, got: " + captures.size());
            }

            PromptCapture p1 = captures.get(0);
            if (!"SYSTEM: SRE agent".equals(p1.system()) || !"USER: triage incident #42".equals(p1.user())) {
                return new ScenarioResult(4, name, false, "Prompt 1 capture mismatch: " + p1);
            }
            if (p1.toolBeans().size() != 1 || p1.toolBeans().get(0) != sampleTool) {
                return new ScenarioResult(4, name, false, "Prompt 1 toolBeans capture mismatch");
            }

            PromptCapture last = spy.getLastPrompt();
            if (!"USER: verify mitigation".equals(last.user())) {
                return new ScenarioResult(4, name, false, "getLastPrompt mismatch: " + last);
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "BudgetEnforcingChatPort (Test Spend Ceiling Guardrail)";
        try {
            ScriptedChatPort target = new ScriptedChatPort();
            target.enqueue("Call 1", 100, 100); // 200 total
            target.enqueue("Call 2", 150, 100); // 250 total (cumulative 450)
            target.enqueue("Call 3", 50, 50);   // 100 total (cumulative 550 -> exceeds 500)

            BudgetEnforcingChatPort budgeted = new BudgetEnforcingChatPort(target, 500);

            budgeted.chat("sys", "Call 1");
            budgeted.chat("sys", "Call 2");

            if (budgeted.getTotalTokensUsed() != 450) {
                return new ScenarioResult(5, name, false, "Expected 450 tokens used, got: " + budgeted.getTotalTokensUsed());
            }

            boolean exceeded = false;
            try {
                budgeted.chat("sys", "Call 3");
            } catch (HarnessBudgetExceededException hbe) {
                String msg = hbe.getMessage() != null ? hbe.getMessage().toLowerCase() : "";
                if (!msg.contains("offline token budget exceeded")) {
                    return new ScenarioResult(5, name, false,
                            "Exception message must contain 'Offline token budget exceeded', got: " + hbe.getMessage());
                }
                exceeded = true;
            }

            if (!exceeded) {
                return new ScenarioResult(5, name, false, "Expected HarnessBudgetExceededException when exceeding 500 tokens");
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "ToolRegistrySpy (Tool Bean Reflection & Discovery Seam)";
        try {
            class SampleDiagnosticTools {
                @Tool(name = "inspectK8sPods")
                public String inspectK8sPods() { return "pods"; }

                @Tool(name = "queryPrometheus")
                public String queryPrometheus() { return "prom"; }

                public String helperMethodNotATool() { return "helper"; }
            }

            class SampleStorageTools {
                @Tool(name = "readDatabaseMetrics")
                public String readDatabaseMetrics() { return "db"; }
            }

            List<Object> tools = List.of(new SampleDiagnosticTools(), new SampleStorageTools());
            List<String> discovered = ToolRegistrySpy.extractToolNames(tools);

            if (discovered.size() != 3) {
                return new ScenarioResult(6, name, false, "Expected 3 tool names, got: " + discovered);
            }
            if (!discovered.contains("inspectK8sPods") || !discovered.contains("queryPrometheus") || !discovered.contains("readDatabaseMetrics")) {
                return new ScenarioResult(6, name, false, "Discovered tools mismatch: " + discovered);
            }
            if (discovered.contains("helperMethodNotATool")) {
                return new ScenarioResult(6, name, false, "Non-annotated methods must not be discovered as tools");
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "InMemoryStreamingChatPort (Deterministic Reactive Flux Test Double)";
        try {
            InMemoryStreamingChatPort streamPort = new InMemoryStreamingChatPort();
            streamPort.enqueueStream(List.of("Streaming ", "diagnostic ", "telemetry ", "chunks."));

            Flux<String> flux = streamPort.stream("sys", "stream query");
            List<String> received = flux.collectList().block();

            if (received == null || received.size() != 4) {
                return new ScenarioResult(7, name, false, "Expected 4 chunks, got: " + received);
            }
            String joined = String.join("", received);
            if (!"Streaming diagnostic telemetry chunks.".equals(joined)) {
                return new ScenarioResult(7, name, false, "Joined stream mismatch: " + joined);
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "VirtualTimeSimulator (Deterministic Time Advancement Seam)";
        try {
            TestClock clock = new TestClock(1000L);
            AtomicInteger stepCount = new AtomicInteger(0);

            long elapsed = VirtualTimeSimulator.measureSimulatedExecution(clock, () -> {
                stepCount.incrementAndGet();
            }, 5000L);

            if (elapsed != 5000L) {
                return new ScenarioResult(8, name, false, "Expected simulated elapsed 5000ms, got: " + elapsed);
            }
            if (clock.now() != 6000L) {
                return new ScenarioResult(8, name, false, "Expected clock now 6000L, got: " + clock.now());
            }
            if (stepCount.get() != 1) {
                return new ScenarioResult(8, name, false, "Expected stepCount 1, got: " + stepCount.get());
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "SpringAiChatPort (Spring AI ChatClient Framework Adapter)";
        try {
            FakeTestDoubleChatModel model = new FakeTestDoubleChatModel();
            model.enqueue("Production adapter response");
            model.setTokens(120, 60);

            ChatClient chatClient = ChatClient.builder(model).build();
            SpringAiChatPort adapter = new SpringAiChatPort(chatClient);

            ChatResult result = adapter.chat("SYSTEM: Cloud SRE", "USER: check ingress logs");
            if (!"Production adapter response".equals(result.text())) {
                return new ScenarioResult(9, name, false, "Adapter text mismatch: " + result.text());
            }
            if (result.tokensIn() != 120 || result.tokensOut() != 60) {
                return new ScenarioResult(9, name, false, "Adapter token mapping mismatch: " + result);
            }

            if (model.getCallCount() != 1) {
                return new ScenarioResult(9, name, false, "Expected 1 underlying model call, got: " + model.getCallCount());
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "DeterministicDiagnosticLoop (End-to-End Autonomous Offline Triage Harness)";
        try {
            ScriptedChatPort scripted = new ScriptedChatPort();
            // Turn 1: Agent decides to inspect logs
            scripted.enqueue("{\"action\": \"INSPECT_LOGS\", \"target\": \"order-service\"}", 50, 25);
            // Turn 2: Agent analyzes logs and decides mitigation
            scripted.enqueue("{\"action\": \"RESTART_POD\", \"target\": \"order-service-7cf9\"}", 60, 30);
            // Turn 3: Agent completes diagnosis
            scripted.enqueue("{\"action\": \"DIAGNOSIS_COMPLETE\", \"rootCause\": \"OOMKilled memory leak in payment buffer\", \"recommendation\": \"Increase pod memory limit to 2Gi\"}", 70, 35);

            PromptCapturingChatPort spy = new PromptCapturingChatPort(scripted);

            DiagnosticTriageReport report = DeterministicDiagnosticLoop.runTriage(
                    spy,
                    "High 500 error rate in checkout flow"
            );

            if (report == null) {
                return new ScenarioResult(10, name, false, "Triage report must not be null");
            }
            if (report.turnsTaken() != 3) {
                return new ScenarioResult(10, name, false, "Expected turnsTaken 3, got: " + report.turnsTaken());
            }
            if (!report.rootCause().contains("OOMKilled")) {
                return new ScenarioResult(10, name, false, "Expected rootCause to mention OOMKilled, got: " + report.rootCause());
            }
            if (!report.recommendation().contains("2Gi")) {
                return new ScenarioResult(10, name, false, "Expected recommendation to mention 2Gi, got: " + report.recommendation());
            }

            // Total tokens = (50+25) + (60+30) + (70+35) = 75 + 90 + 105 = 270
            if (report.totalTokens() != 270) {
                return new ScenarioResult(10, name, false, "Expected totalTokens 270, got: " + report.totalTokens());
            }

            if (spy.getCallCount() != 3) {
                return new ScenarioResult(10, name, false, "Expected spy to capture 3 calls, got: " + spy.getCallCount());
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("PHASE 05 EXERCISE 01: DETERMINISTIC OFFLINE TEST HARNESS REPORT");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String status = r.passed() ? "PASS" : "FAIL";
            if (r.passed()) passed++;
            System.out.printf("[%s] Scenario %02d: %s%n", status, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("       Detail: %s%n", r.errorDetail());
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Total: %d | Passed: %d | Failed: %d%n", results.size(), passed, results.size() - passed);
        System.out.println("===============================================================================");
    }
}
