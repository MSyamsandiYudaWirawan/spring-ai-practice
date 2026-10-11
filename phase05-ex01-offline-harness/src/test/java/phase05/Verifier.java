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
 * Deterministic Offline Test Harness & LLM Boundary Seams (15 Repetitive Scenarios).
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
        // Topic 1: Scripted FIFO & Inspection Doubles
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Dynamic Pattern & Predicate Routing Doubles
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Fault Injection & Resilience Testing
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Prompt Observation, Auditing & Token Budget Ceilings
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Framework Adapters, Streaming & Autonomous Loops
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Scripted FIFO & Inspection Doubles (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "ScriptedChatPort (FIFO Response Queue Test Double)";
        List<String> hints = List.of(
                "Maintain an internal FIFO Deque<ChatResult> to queue responses.",
                "Increment callCount on every call before polling the deque.",
                "Throw IllegalStateException if the queue is empty when chat() is invoked."
        );
        try {
            ScriptedChatPort port = new ScriptedChatPort();
            port.enqueue("Initial investigation proposal", 50, 30);
            port.enqueue("Follow-up mitigation plan", 40, 20);

            ChatResult res1 = port.chat("sys", "user 1");
            if (!"Initial investigation proposal".equals(res1.text()) || res1.tokensIn() != 50 || res1.tokensOut() != 30) {
                return new ScenarioResult(1, name, false, "First response mismatch: " + res1, hints);
            }
            if (port.callCount() != 1) {
                return new ScenarioResult(1, name, false, "callCount expected 1, got: " + port.callCount(), hints);
            }

            ChatResult res2 = port.chat("sys", "user 2");
            if (!"Follow-up mitigation plan".equals(res2.text()) || res2.tokensIn() != 40 || res2.tokensOut() != 20) {
                return new ScenarioResult(1, name, false, "Second response mismatch: " + res2, hints);
            }
            if (port.callCount() != 2) {
                return new ScenarioResult(1, name, false, "callCount expected 2, got: " + port.callCount(), hints);
            }

            boolean exhausted = false;
            try {
                port.chat("sys", "user 3");
            } catch (IllegalStateException ise) {
                exhausted = true;
            }
            if (!exhausted) {
                return new ScenarioResult(1, name, false, "Calling chat on exhausted queue must throw IllegalStateException", hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "IntrospectableScriptedChatPort (Queue Introspection & Reset)";
        List<String> hints = List.of(
                "peek() inspects queue.peek() without removing elements.",
                "clear() should empty the deque and reset callCount to 0.",
                "remaining() returns queue.size() and hasMore() returns !queue.isEmpty()."
        );
        try {
            IntrospectableScriptedChatPort port = new IntrospectableScriptedChatPort();
            port.enqueue("Resp 1", 10, 10);
            port.enqueue("Resp 2", 20, 20);
            port.enqueue("Resp 3", 30, 30);

            if (port.remaining() != 3 || !port.hasMore()) {
                return new ScenarioResult(2, name, false, "Expected remaining=3 and hasMore=true, got: " + port.remaining(), hints);
            }
            ChatResult peeked = port.peek();
            if (peeked == null || !"Resp 1".equals(peeked.text()) || port.remaining() != 3) {
                return new ScenarioResult(2, name, false, "peek() must return next element without dequeuing it", hints);
            }

            ChatResult res = port.chat("sys", "first");
            if (!"Resp 1".equals(res.text()) || port.remaining() != 2 || port.callCount() != 1) {
                return new ScenarioResult(2, name, false, "After one call, remaining must be 2 and callCount 1", hints);
            }

            port.clear();
            if (port.remaining() != 0 || port.hasMore() || port.callCount() != 0) {
                return new ScenarioResult(2, name, false, "clear() must reset remaining to 0, hasMore to false, and callCount to 0", hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "SequentialDialogueReplayer (Multi-Turn Scripted Replay Seam)";
        List<String> hints = List.of(
                "Maintain a turn index pointer starting at 0.",
                "In chat(), verify that user prompt contains turn.expectedPromptSubstring().",
                "Throw IllegalStateException if prompt substring does not match."
        );
        try {
            List<DialogueTurn> script = List.of(
                    new DialogueTurn("USER", "diagnose incident #101", new ChatResult("Checking logs for #101", 30, 20)),
                    new DialogueTurn("USER", "query metrics", new ChatResult("CPU utilization 92%", 25, 15)),
                    new DialogueTurn("USER", "apply mitigation", new ChatResult("Scaled pods to 5", 20, 10))
            );

            SequentialDialogueReplayer replayer = new SequentialDialogueReplayer(script);

            ChatResult r1 = replayer.chat("sys", "Please diagnose incident #101 now");
            if (!r1.text().contains("Checking logs")) {
                return new ScenarioResult(3, name, false, "Turn 1 reply mismatch: " + r1.text(), hints);
            }

            ChatResult r2 = replayer.chat("sys", "Now query metrics from Prometheus");
            if (!r2.text().contains("CPU utilization")) {
                return new ScenarioResult(3, name, false, "Turn 2 reply mismatch: " + r2.text(), hints);
            }

            // Test turn mismatch
            boolean mismatched = false;
            try {
                replayer.chat("sys", "unexpected prompt without substring");
            } catch (IllegalStateException ex) {
                mismatched = true;
            }
            if (!mismatched) {
                return new ScenarioResult(3, name, false, "Expected IllegalStateException on turn prompt mismatch", hints);
            }

            ChatResult r3 = replayer.chat("sys", "Please apply mitigation to service");
            if (!r3.text().contains("Scaled pods")) {
                return new ScenarioResult(3, name, false, "Turn 3 reply mismatch: " + r3.text(), hints);
            }

            ReplayValidation validation = replayer.validateCompletion();
            if (!validation.allTurnsMatched() || validation.turnsExecuted() != 3) {
                return new ScenarioResult(3, name, false, "Expected allTurnsMatched=true and turnsExecuted=3, got: " + validation, hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Dynamic Pattern & Predicate Routing Doubles (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "PredicateRoutingChatPort (Dynamic Pattern Matching Test Double)";
        List<String> hints = List.of(
                "Store registered routes in a List<Route> where Route contains Predicate<String> and ChatResult.",
                "In chat(), test each route's predicate against the user prompt in insertion order.",
                "If no route matches, return defaultResult."
        );
        try {
            PredicateRoutingChatPort port = new PredicateRoutingChatPort();
            port.when(user -> user.contains("metrics"), new ChatResult("Metrics: CPU 94%, Heap 88%", 30, 20));
            port.when(user -> user.contains("threads"), new ChatResult("ThreadDump: 42 threads blocked", 35, 25));
            port.setDefaultResult(new ChatResult("Generic diagnostic response", 10, 10));

            ChatResult resMetrics = port.chat("sys", "Check system metrics now");
            if (!resMetrics.text().contains("CPU 94%")) {
                return new ScenarioResult(4, name, false, "Expected metrics response, got: " + resMetrics.text(), hints);
            }

            ChatResult resThreads = port.chat("sys", "Analyze threads state");
            if (!resThreads.text().contains("42 threads blocked")) {
                return new ScenarioResult(4, name, false, "Expected threads response, got: " + resThreads.text(), hints);
            }

            ChatResult resFallback = port.chat("sys", "Arbitrary unknown prompt");
            if (!resFallback.text().contains("Generic diagnostic response")) {
                return new ScenarioResult(4, name, false, "Expected default response, got: " + resFallback.text(), hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "BiPredicateRoutingChatPort (System & User Dual-Predicate Matcher)";
        List<String> hints = List.of(
                "Use BiPredicate<String, String> to evaluate both system and user prompts.",
                "Iterate registered routes in insertion order.",
                "Return defaultResult when no route matches."
        );
        try {
            BiPredicateRoutingChatPort port = new BiPredicateRoutingChatPort();
            port.when((sys, user) -> sys.contains("SRE") && user.contains("restart"),
                    new ChatResult("SRE Action: Pod restart initiated", 40, 20));
            port.when((sys, user) -> sys.contains("DBA") && user.contains("lock"),
                    new ChatResult("DBA Action: Deadlock table kill", 50, 25));
            port.setDefaultResult(new ChatResult("Fallback Action", 10, 10));

            ChatResult r1 = port.chat("ROLE: SRE Copilot", "Please restart order-pod");
            if (!r1.text().contains("Pod restart initiated")) {
                return new ScenarioResult(5, name, false, "Expected SRE action match, got: " + r1.text(), hints);
            }

            ChatResult r2 = port.chat("ROLE: DBA Copilot", "Investigate lock contention");
            if (!r2.text().contains("Deadlock table kill")) {
                return new ScenarioResult(5, name, false, "Expected DBA action match, got: " + r2.text(), hints);
            }

            ChatResult r3 = port.chat("ROLE: Unknown", "Check status");
            if (!r3.text().contains("Fallback Action")) {
                return new ScenarioResult(5, name, false, "Expected fallback match, got: " + r3.text(), hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "RegexTemplateRoutingChatPort (Parameterized Dynamic Response Generator)";
        List<String> hints = List.of(
                "Compile regex Pattern and test with matcher.find().",
                "Replace ${1}, ${2}, etc. with matcher.group(1), matcher.group(2).",
                "Compute tokensIn as user.length() and tokensOut as text.length()."
        );
        try {
            RegexTemplateRoutingChatPort port = new RegexTemplateRoutingChatPort();
            port.whenRegex(
                    "(?i)investigate pod ([a-z0-9-]+) on namespace ([a-z0-9-]+)",
                    "Inspecting pod ${1} in namespace ${2}: status OK"
            );
            port.setDefaultTemplate("Default diagnosis for prompt: ${0}");

            String query = "investigate pod order-service-7cf9 on namespace prod";
            ChatResult r = port.chat("sys", query);

            if (!r.text().contains("Inspecting pod order-service-7cf9 in namespace prod: status OK")) {
                return new ScenarioResult(6, name, false, "Parameterized template replacement failed: " + r.text(), hints);
            }
            if (r.tokensIn() != query.length() || r.tokensOut() != r.text().length()) {
                return new ScenarioResult(6, name, false, "Dynamic token mapping mismatch: " + r, hints);
            }

            ChatResult rFallback = port.chat("sys", "unmatched query");
            if (!rFallback.text().contains("Default diagnosis for prompt: unmatched query")) {
                return new ScenarioResult(6, name, false, "Fallback template replacement failed: " + rFallback.text(), hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Fault Injection & Resilience Testing (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "FaultInjectingChatPort (Simulated Transient Fault & Retry Double)";
        List<String> hints = List.of(
                "Maintain a queue of actions (either a ChatResult or a RuntimeException).",
                "If the polled action is a RuntimeException, throw it; if ChatResult, return it.",
                "Increment callCount on every chat() call."
        );
        try {
            FaultInjectingChatPort port = new FaultInjectingChatPort();
            port.enqueueFault(new SimulatedUpstreamException("503 Service Unavailable"));
            port.enqueueResult("Recovery response after retry", 40, 25);

            boolean threwFault = false;
            try {
                port.chat("sys", "query upstream");
            } catch (SimulatedUpstreamException sue) {
                threwFault = true;
            }
            if (!threwFault) {
                return new ScenarioResult(7, name, false, "Expected SimulatedUpstreamException on first call", hints);
            }

            ChatResult res = port.chat("sys", "retry upstream");
            if (!"Recovery response after retry".equals(res.text())) {
                return new ScenarioResult(7, name, false, "Expected recovered result on second call, got: " + res.text(), hints);
            }
            if (port.callCount() != 2) {
                return new ScenarioResult(7, name, false, "Expected callCount 2, got: " + port.callCount(), hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "ConsecutiveFaultChatPort (K-Failure Fault Burst Generator)";
        List<String> hints = List.of(
                "Decrement consecutiveFailuresRemaining when throwing each fault.",
                "Once remaining reaches 0, return the recovery ChatResult.",
                "Track totalAttempts across all calls."
        );
        try {
            ConsecutiveFaultChatPort port = new ConsecutiveFaultChatPort();
            port.failNext(3, new SimulatedUpstreamException("503 Server Error"), new ChatResult("Recovered after 3 faults", 30, 20));

            for (int i = 1; i <= 3; i++) {
                boolean threw = false;
                try {
                    port.chat("sys", "attempt " + i);
                } catch (SimulatedUpstreamException ex) {
                    threw = true;
                }
                if (!threw) {
                    return new ScenarioResult(8, name, false, "Expected fault on attempt " + i, hints);
                }
            }

            if (port.consecutiveFailuresRemaining() != 0) {
                return new ScenarioResult(8, name, false, "Expected 0 remaining failures, got: " + port.consecutiveFailuresRemaining(), hints);
            }

            ChatResult recovered = port.chat("sys", "attempt 4");
            if (!"Recovered after 3 faults".equals(recovered.text()) || port.totalAttempts() != 4) {
                return new ScenarioResult(8, name, false, "Expected recovered result on attempt 4 with totalAttempts=4, got: " + recovered, hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "ExponentialBackoffRetrySimulator (Virtual Time Retry Loop)";
        List<String> hints = List.of(
                "Loop while attempts <= maxRetries.",
                "In catch block of SimulatedUpstreamException, advance clock.advance(currentDelay) and double currentDelay *= 2.",
                "Return RetrySimulationReport with total delay accumulated and final success flag."
        );
        try {
            ConsecutiveFaultChatPort port = new ConsecutiveFaultChatPort();
            port.failNext(2, new SimulatedUpstreamException("429 Too Many Requests"), new ChatResult("Recovered on attempt 3", 40, 20));

            TestClock clock = new TestClock(1000L);
            RetrySimulationReport report = ExponentialBackoffRetrySimulator.simulateRetry(port, "incident check", 3, 1000L, clock);

            if (!report.succeeded() || report.attempts() != 3) {
                return new ScenarioResult(9, name, false, "Expected succeeded=true with 3 attempts, got: " + report, hints);
            }
            // Delays: attempt 1 failed (delay 1000ms), attempt 2 failed (delay 2000ms) -> total 3000ms
            if (report.totalDelayMillis() != 3000L || clock.now() != 4000L) {
                return new ScenarioResult(9, name, false, "Expected totalDelay 3000ms and clock at 4000L, got: " + report.totalDelayMillis() + ", clock=" + clock.now(), hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Prompt Observation, Auditing & Token Budget Ceilings (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "PromptCapturingChatPort (Outgoing Prompt Spy & Inspection Seam)";
        List<String> hints = List.of(
                "In chat(), record new PromptCapture(system, user, toolBeans, System.currentTimeMillis()).",
                "Delegate call execution to delegate.chat(system, user, toolBeans).",
                "Return unmodifiable list from getCapturedPrompts()."
        );
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
                return new ScenarioResult(10, name, false, "getCallCount expected 2, got: " + spy.getCallCount(), hints);
            }

            List<PromptCapture> captures = spy.getCapturedPrompts();
            if (captures.size() != 2) {
                return new ScenarioResult(10, name, false, "getCapturedPrompts size expected 2, got: " + captures.size(), hints);
            }

            PromptCapture p1 = captures.get(0);
            if (!"SYSTEM: SRE agent".equals(p1.system()) || !"USER: triage incident #42".equals(p1.user())) {
                return new ScenarioResult(10, name, false, "Prompt 1 capture mismatch: " + p1, hints);
            }
            if (p1.toolBeans().size() != 1 || p1.toolBeans().get(0) != sampleTool) {
                return new ScenarioResult(10, name, false, "Prompt 1 toolBeans capture mismatch", hints);
            }

            PromptCapture last = spy.getLastPrompt();
            if (!"USER: verify mitigation".equals(last.user())) {
                return new ScenarioResult(10, name, false, "getLastPrompt mismatch: " + last, hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "BudgetEnforcingChatPort (Test Spend Ceiling Guardrail)";
        List<String> hints = List.of(
                "Accumulate totalTokensUsed += result.totalTokens().",
                "Check if totalTokensUsed > maxTokensAllowed.",
                "Throw HarnessBudgetExceededException with message containing 'Offline token budget exceeded'."
        );
        try {
            ScriptedChatPort target = new ScriptedChatPort();
            target.enqueue("Call 1", 100, 100); // 200 total
            target.enqueue("Call 2", 150, 100); // 250 total (cumulative 450)
            target.enqueue("Call 3", 50, 50);   // 100 total (cumulative 550 -> exceeds 500)

            BudgetEnforcingChatPort budgeted = new BudgetEnforcingChatPort(target, 500);

            budgeted.chat("sys", "Call 1");
            budgeted.chat("sys", "Call 2");

            if (budgeted.getTotalTokensUsed() != 450) {
                return new ScenarioResult(11, name, false, "Expected 450 tokens used, got: " + budgeted.getTotalTokensUsed(), hints);
            }

            boolean exceeded = false;
            try {
                budgeted.chat("sys", "Call 3");
            } catch (HarnessBudgetExceededException hbe) {
                String msg = hbe.getMessage() != null ? hbe.getMessage().toLowerCase() : "";
                if (!msg.contains("offline token budget exceeded")) {
                    return new ScenarioResult(11, name, false,
                            "Exception message must contain 'Offline token budget exceeded', got: " + hbe.getMessage(), hints);
                }
                exceeded = true;
            }

            if (!exceeded) {
                return new ScenarioResult(11, name, false, "Expected HarnessBudgetExceededException when exceeding 500 tokens", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "AuditingCompositeChatPort (Full Audit Trail Decorator)";
        List<String> hints = List.of(
                "Maintain a List<AuditLogEntry> auditLog.",
                "Accumulate cumulativeTokens across invocations.",
                "peakTokenCall() returns the AuditLogEntry with max tokensThisCall."
        );
        try {
            ScriptedChatPort target = new ScriptedChatPort();
            target.enqueue("First response", 50, 50);   // 100 tokens
            target.enqueue("Second large response", 200, 150); // 350 tokens (cumulative 450)
            target.enqueue("Third response", 30, 20);   // 50 tokens (cumulative 500)

            AuditingCompositeChatPort auditPort = new AuditingCompositeChatPort(target);
            auditPort.chat("SYSTEM: auditor", "call 1");
            auditPort.chat("SYSTEM: auditor", "call 2");
            auditPort.chat("SYSTEM: auditor", "call 3");

            List<AuditLogEntry> log = auditPort.getAuditLog();
            if (log.size() != 3) {
                return new ScenarioResult(12, name, false, "Expected 3 audit log entries, got: " + log.size(), hints);
            }
            if (log.get(1).tokensThisCall() != 350 || log.get(2).cumulativeTokens() != 500) {
                return new ScenarioResult(12, name, false, "Token accounting mismatch in audit log: " + log, hints);
            }

            AuditLogEntry peak = auditPort.peakTokenCall();
            if (peak == null || peak.callIndex() != 2 || peak.tokensThisCall() != 350) {
                return new ScenarioResult(12, name, false, "Expected peak call index 2 with 350 tokens, got: " + peak, hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Framework Adapters, Streaming & Autonomous Loops (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "ToolRegistrySpy (Tool Bean Reflection & Discovery Seam)";
        List<String> hints = List.of(
                "Iterate methods via bean.getClass().getMethods().",
                "Check method.isAnnotationPresent(Tool.class).",
                "If @Tool(name = '...') is non-blank use it, else fallback to method.getName()."
        );
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
                return new ScenarioResult(13, name, false, "Expected 3 tool names, got: " + discovered, hints);
            }
            if (!discovered.contains("inspectK8sPods") || !discovered.contains("queryPrometheus") || !discovered.contains("readDatabaseMetrics")) {
                return new ScenarioResult(13, name, false, "Discovered tools mismatch: " + discovered, hints);
            }
            if (discovered.contains("helperMethodNotATool")) {
                return new ScenarioResult(13, name, false, "Non-annotated methods must not be discovered as tools", hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "InMemoryStreamingChatPort (Deterministic Reactive Flux Test Double)";
        List<String> hints = List.of(
                "Maintain a queue of chunk lists (Deque<List<String>>).",
                "In stream(), poll the next chunk list and return Flux.fromIterable(chunks).",
                "Increment streamCallCount on each stream() call."
        );
        try {
            InMemoryStreamingChatPort streamPort = new InMemoryStreamingChatPort();
            streamPort.enqueueStream(List.of("Streaming ", "diagnostic ", "telemetry ", "chunks."));

            Flux<String> flux = streamPort.stream("sys", "stream query");
            List<String> received = flux.collectList().block();

            if (received == null || received.size() != 4) {
                return new ScenarioResult(14, name, false, "Expected 4 chunks, got: " + received, hints);
            }
            String joined = String.join("", received);
            if (!"Streaming diagnostic telemetry chunks.".equals(joined)) {
                return new ScenarioResult(14, name, false, "Joined stream mismatch: " + joined, hints);
            }
            if (streamPort.streamCallCount() != 1) {
                return new ScenarioResult(14, name, false, "Expected streamCallCount 1, got: " + streamPort.streamCallCount(), hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "SpringAiChatPort & DeterministicDiagnosticLoop (Multi-Turn Triage Harness)";
        List<String> hints = List.of(
                "Execute 3 sequential chatPort.chat() turns.",
                "Accumulate totalTokens across all 3 turns.",
                "Extract rootCause and recommendation from Turn 3 output and return DiagnosticTriageReport."
        );
        try {
            FakeTestDoubleChatModel model = new FakeTestDoubleChatModel();
            model.enqueue("Production adapter response");
            model.setTokens(120, 60);

            ChatClient chatClient = ChatClient.builder(model).build();
            SpringAiChatPort adapter = new SpringAiChatPort(chatClient);

            ChatResult adapterRes = adapter.chat("SYSTEM: Cloud SRE", "USER: check ingress logs");
            if (!"Production adapter response".equals(adapterRes.text()) || adapterRes.tokensIn() != 120 || adapterRes.tokensOut() != 60) {
                return new ScenarioResult(15, name, false, "SpringAiChatPort adapter mismatch: " + adapterRes, hints);
            }

            ScriptedChatPort scripted = new ScriptedChatPort();
            scripted.enqueue("{\"action\": \"INSPECT_LOGS\", \"target\": \"order-service\"}", 50, 25);
            scripted.enqueue("{\"action\": \"RESTART_POD\", \"target\": \"order-service-7cf9\"}", 60, 30);
            scripted.enqueue("{\"action\": \"DIAGNOSIS_COMPLETE\", \"rootCause\": \"OOMKilled memory leak in payment buffer\", \"recommendation\": \"Increase pod memory limit to 2Gi\"}", 70, 35);

            PromptCapturingChatPort spy = new PromptCapturingChatPort(scripted);
            DiagnosticTriageReport report = DeterministicDiagnosticLoop.runTriage(spy, "High 500 error rate in checkout flow");

            if (report == null || report.turnsTaken() != 3) {
                return new ScenarioResult(15, name, false, "Triage report must take 3 turns, got: " + report, hints);
            }
            if (!report.rootCause().contains("OOMKilled") || !report.recommendation().contains("2Gi")) {
                return new ScenarioResult(15, name, false, "RootCause or recommendation missing key terms: " + report, hints);
            }
            if (report.totalTokens() != 270) {
                return new ScenarioResult(15, name, false, "Expected totalTokens 270, got: " + report.totalTokens(), hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 05 EXERCISE 01: DETERMINISTIC OFFLINE TEST HARNESS REPORT");
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
