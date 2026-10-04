package phase05;

import org.springframework.ai.converter.BeanOutputConverter;
import phase05.AdvancedHarnessContracts.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic test verifier for Phase 05 Exercise 02:
 * Advanced Offline Harness: Chaos Matrix, Streaming Latency, HTTP Stubs, and Conversation Replay.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class Verifier {

    public static void main(String[] args) {
        List<ScenarioResult> results = new ArrayList<>();

        if (args.length > 0) {
            int scenario = Integer.parseInt(args[0]);
            results.add(runScenario(scenario));
        } else {
            for (int i = 1; i <= 10; i++) {
                results.add(runScenario(i));
            }
        }

        printReport(results);

        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            System.exit(99);
        }
    }

    private record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

    private static ScenarioResult runScenario(int scenario) {
        return switch (scenario) {
            case 1 -> verifyScenario1();
            case 2 -> verifyScenario2();
            case 3 -> verifyScenario3();
            case 4 -> verifyScenario4();
            case 5 -> verifyScenario5();
            case 6 -> verifyScenario6();
            case 7 -> verifyScenario7();
            case 8 -> verifyScenario8();
            case 9 -> verifyScenario9();
            case 10 -> verifyScenario10();
            default -> new ScenarioResult(scenario, "Unknown Scenario", false, "Invalid scenario index: " + scenario);
        };
    }

    private static ScenarioResult verifyScenario1() {
        String name = "ChaosInjectingChatPort (Deterministic Seeded Chaos Matrix)";
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Healthy response payload from cluster", 100, 50);

            // Chaos matrix: 50% failure rate, deterministic seed
            Random seededRandom = new Random(42);
            ChaosMatrix matrix = new ChaosMatrix(0.5, 50, false, seededRandom);
            AdvancedOfflineHarnessUnderTest.ChaosInjectingChatPort chaosPort =
                    new AdvancedOfflineHarnessUnderTest.ChaosInjectingChatPort(underlying, matrix);

            int totalCalls = 20;
            int exceptionsCaught = 0;
            for (int i = 0; i < totalCalls; i++) {
                try {
                    chaosPort.chat("sys", "call-" + i);
                } catch (RuntimeException ex) {
                    exceptionsCaught++;
                }
            }

            if (chaosPort.getTotalCalls() != totalCalls) {
                return new ScenarioResult(1, name, false,
                        "Expected totalCalls 20, got: " + chaosPort.getTotalCalls());
            }
            if (exceptionsCaught == 0 || exceptionsCaught == totalCalls) {
                return new ScenarioResult(1, name, false,
                        "Chaos failure rate must inject probabilistic faults, got " + exceptionsCaught + " faults out of 20");
            }
            if (chaosPort.getFailureCount() != exceptionsCaught) {
                return new ScenarioResult(1, name, false,
                        "failureCount mismatch: expected " + exceptionsCaught + ", got " + chaosPort.getFailureCount());
            }

            // Test payload corruption mode
            ChaosMatrix corruptMatrix = new ChaosMatrix(0.0, 0, true, new Random(1));
            AdvancedOfflineHarnessUnderTest.ChaosInjectingChatPort corruptPort =
                    new AdvancedOfflineHarnessUnderTest.ChaosInjectingChatPort(underlying, corruptMatrix);
            ChatResult corrupted = corruptPort.chat("sys", "test");
            if (corrupted.text().length() >= "Healthy response payload from cluster".length()) {
                return new ScenarioResult(1, name, false,
                        "Corrupted payload must be truncated, got length: " + corrupted.text().length());
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "StreamingChunkLatencySimulator (Reactive Chunk Delay & Backpressure)";
        try {
            AdvancedOfflineHarnessUnderTest.StreamingChunkLatencySimulator simulator =
                    new AdvancedOfflineHarnessUnderTest.StreamingChunkLatencySimulator();

            List<String> chunks = List.of("Analyzing", " heap", " dump", " for", " leak");
            simulator.enqueueStream(chunks, 20L); // 20ms simulated latency per chunk

            Flux<String> stream = simulator.stream("sys", "stream test");
            List<String> received = stream.collectList().block();

            if (received == null || received.size() != 5) {
                return new ScenarioResult(2, name, false, "Expected 5 chunks, got: " + received);
            }
            if (!"Analyzing heap dump for leak".equals(String.join("", received))) {
                return new ScenarioResult(2, name, false, "Received stream mismatch: " + String.join("", received));
            }
            if (simulator.getTotalChunksEmitted() != 5) {
                return new ScenarioResult(2, name, false, "Expected 5 totalChunksEmitted, got: " + simulator.getTotalChunksEmitted());
            }
            if (simulator.getCumulativeSimulatedLatencyMs() != 100L) { // 5 chunks * 20ms = 100ms
                return new ScenarioResult(2, name, false, "Expected 100ms cumulative latency, got: " + simulator.getCumulativeSimulatedLatencyMs());
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "RateLimitBackoffChatPort (Simulated Rate-Limit & Virtual Backoff Double)";
        try {
            AdvancedTestClock clock = new AdvancedTestClock(1000L);
            AtomicInteger attempts = new AtomicInteger(0);

            // Flaky delegate that fails with HTTP 429 on first 2 calls, then succeeds on 3rd call
            ChatPort rateLimitedDelegate = (sys, user, tools) -> {
                int count = attempts.incrementAndGet();
                if (count <= 2) {
                    throw new RuntimeException("HTTP 429 Too Many Requests: Rate limit exceeded");
                }
                return new ChatResult("Recovered after backoff", 100, 50);
            };

            // Max 3 retries, initial backoff 1000ms
            // Retry 1: 1000ms, Retry 2: 2000ms -> total advanced = 3000ms
            AdvancedOfflineHarnessUnderTest.RateLimitBackoffChatPort backoffPort =
                    new AdvancedOfflineHarnessUnderTest.RateLimitBackoffChatPort(rateLimitedDelegate, 3, 1000L, clock);

            ChatResult res = backoffPort.chat("sys", "user query");

            if (!"Recovered after backoff".equals(res.text())) {
                return new ScenarioResult(3, name, false, "Response text mismatch: " + res.text());
            }
            if (backoffPort.getRetryAttempts() != 2) {
                return new ScenarioResult(3, name, false, "Expected 2 retryAttempts, got: " + backoffPort.getRetryAttempts());
            }
            if (backoffPort.getSuccessfulCalls() != 1) {
                return new ScenarioResult(3, name, false, "Expected 1 successfulCalls, got: " + backoffPort.getSuccessfulCalls());
            }
            // Clock was 1000L, advanced by 1000L (attempt 1) + 2000L (attempt 2) = 4000L
            if (clock.now() != 4000L) {
                return new ScenarioResult(3, name, false, "Expected clock now 4000L, got: " + clock.now());
            }

            // Test 2: Retries exhausted
            ChatPort alwaysFails = (sys, user, tools) -> {
                throw new RuntimeException("429 Rate limit quota exceeded");
            };
            AdvancedOfflineHarnessUnderTest.RateLimitBackoffChatPort exhaustedPort =
                    new AdvancedOfflineHarnessUnderTest.RateLimitBackoffChatPort(alwaysFails, 2, 500L, clock);

            boolean exhaustedCaught = false;
            try {
                exhaustedPort.chat("sys", "user query 2");
            } catch (RateLimitExhaustedException rle) {
                exhaustedCaught = true;
                String msg = rle.getMessage() != null ? rle.getMessage().toLowerCase() : "";
                if (!msg.contains("rate limit retries exhausted")) {
                    return new ScenarioResult(3, name, false,
                            "Exception message must contain 'Rate limit retries exhausted', got: " + rle.getMessage());
                }
            }
            if (!exhaustedCaught) {
                return new ScenarioResult(3, name, false, "Expected RateLimitExhaustedException when retries are exhausted");
            }

            // Test 3: Non-rate-limit error fails fast without retrying
            ChatPort nonRetryable = (sys, user, tools) -> { throw new IllegalArgumentException("Bad input"); };
            AdvancedOfflineHarnessUnderTest.RateLimitBackoffChatPort fastFailPort =
                    new AdvancedOfflineHarnessUnderTest.RateLimitBackoffChatPort(nonRetryable, 3, 1000L, clock);
            boolean nonRetryableCaught = false;
            try {
                fastFailPort.chat("sys", "bad");
            } catch (IllegalArgumentException ex) {
                nonRetryableCaught = true;
            }
            if (!nonRetryableCaught) {
                return new ScenarioResult(3, name, false, "Expected non-rate-limit exception to fail fast without retrying");
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "DeterministicConversationReplayer (Trajectory Replay & State Verification)";
        try {
            ConversationTurn t1 = new ConversationTurn("INSPECT_PODS", "Found 3 pods CrashLoopBackOff", 50);
            ConversationTurn t2 = new ConversationTurn("GET_POD_LOGS", "Fatal error: Database unreachable", 80);
            ConversationTrajectory trajectory = new ConversationTrajectory("traj-42", List.of(t1, t2));

            // Agent that complies with trajectory
            Deque<ChatResult> answers = new ArrayDeque<>();
            answers.add(new ChatResult("Found 3 pods CrashLoopBackOff", 30, 20));
            answers.add(new ChatResult("Fatal error: Database unreachable", 50, 30));

            ChatPort agent = (sys, user, tools) -> answers.poll();

            long totalTokens = AdvancedOfflineHarnessUnderTest.DeterministicConversationReplayer.replay(trajectory, agent);
            if (totalTokens != 130) { // (30+20) + (50+30) = 50 + 80 = 130
                return new ScenarioResult(4, name, false, "Expected total tokens 130, got: " + totalTokens);
            }

            // Failing agent test (emits unexpected response)
            ChatPort divergentAgent = (sys, user, tools) -> new ChatResult("Unexpected divergent response", 10, 10);
            boolean divergentCaught = false;
            try {
                AdvancedOfflineHarnessUnderTest.DeterministicConversationReplayer.replay(trajectory, divergentAgent);
            } catch (IllegalStateException | AssertionError ex) {
                divergentCaught = true;
            }
            if (!divergentCaught) {
                return new ScenarioResult(4, name, false, "Expected failure when agent diverges from golden trajectory");
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "CircuitBreakingChatPort (Resilient State Machine Test Double)";
        try {
            AdvancedTestClock clock = new AdvancedTestClock(10_000L);
            AtomicInteger attempts = new AtomicInteger(0);
            AtomicInteger failures = new AtomicInteger(3); // First 3 calls fail

            ChatPort flapper = (sys, user, tools) -> {
                attempts.incrementAndGet();
                if (failures.getAndDecrement() > 0) {
                    throw new RuntimeException("Upstream LLM 503 Service Unavailable");
                }
                return new ChatResult("Recovered response", 50, 25);
            };

            // Threshold: 3 failures, Cooldown: 5000ms
            AdvancedOfflineHarnessUnderTest.CircuitBreakingChatPort cb =
                    new AdvancedOfflineHarnessUnderTest.CircuitBreakingChatPort(flapper, 3, 5000L, clock);

            if (cb.getState() != CircuitState.CLOSED) {
                return new ScenarioResult(5, name, false, "Initial state must be CLOSED, got: " + cb.getState());
            }

            // Calls 1, 2, 3 fail
            for (int i = 0; i < 3; i++) {
                try {
                    cb.chat("sys", "query");
                } catch (RuntimeException ignored) {}
            }

            if (cb.getState() != CircuitState.OPEN) {
                return new ScenarioResult(5, name, false, "State after 3 consecutive failures must be OPEN, got: " + cb.getState());
            }

            // 4th call during OPEN must be rejected without calling delegate
            boolean openCaught = false;
            try {
                cb.chat("sys", "query during open");
            } catch (CircuitBreakerOpenException cbe) {
                openCaught = true;
                String msg = cbe.getMessage() != null ? cbe.getMessage().toLowerCase() : "";
                if (!msg.contains("circuit breaker is open")) {
                    return new ScenarioResult(5, name, false,
                            "Exception message must contain 'Circuit breaker is OPEN', got: " + cbe.getMessage());
                }
            }
            if (!openCaught) {
                return new ScenarioResult(5, name, false, "Expected CircuitBreakerOpenException while circuit is OPEN");
            }

            if (attempts.get() != 3) {
                return new ScenarioResult(5, name, false, "Underlying delegate must not be called while OPEN. Attempts: " + attempts.get());
            }

            // Advance clock past cooldown (5000ms)
            clock.advance(5001L);

            // Probe call should transition to HALF_OPEN and succeed -> back to CLOSED
            ChatResult probeResult = cb.chat("sys", "probe call");
            if (!"Recovered response".equals(probeResult.text())) {
                return new ScenarioResult(5, name, false, "Probe call response mismatch: " + probeResult.text());
            }
            if (cb.getState() != CircuitState.CLOSED) {
                return new ScenarioResult(5, name, false, "Circuit must recover to CLOSED after successful probe, got: " + cb.getState());
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "MultiTenantBudgetPartitionPort (Hierarchical Token Quota Manager)";
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Tenant response", 60, 40); // 100 tokens per call

            AdvancedOfflineHarnessUnderTest.MultiTenantBudgetPartitionPort mtPort =
                    new AdvancedOfflineHarnessUnderTest.MultiTenantBudgetPartitionPort(underlying);

            mtPort.allocateQuota("tenant-alpha", 250);
            mtPort.allocateQuota("tenant-beta", 150);

            // Tenant Alpha calls twice (100 + 100 = 200 tokens used, 50 remaining)
            mtPort.chat("tenant-alpha", "sys", "call 1");
            mtPort.chat("tenant-alpha", "sys", "call 2");

            if (mtPort.getRemainingQuota("tenant-alpha") != 50) {
                return new ScenarioResult(6, name, false, "Expected 50 tokens remaining for alpha, got: " + mtPort.getRemainingQuota("tenant-alpha"));
            }

            // Beta calls once (100 used, 50 remaining)
            mtPort.chat("tenant-beta", "sys", "beta call 1");
            if (mtPort.getRemainingQuota("tenant-beta") != 50) {
                return new ScenarioResult(6, name, false, "Expected 50 tokens remaining for beta, got: " + mtPort.getRemainingQuota("tenant-beta"));
            }

            // Beta calls again (requires 100, only 50 remaining -> must throw TenantQuotaExceededException)
            boolean betaExceeded = false;
            try {
                mtPort.chat("tenant-beta", "sys", "beta call 2");
            } catch (TenantQuotaExceededException tqe) {
                betaExceeded = true;
                String msg = tqe.getMessage() != null ? tqe.getMessage().toLowerCase() : "";
                if (!msg.contains("quota exceeded for tenant")) {
                    return new ScenarioResult(6, name, false,
                            "Exception message must contain 'Quota exceeded for tenant', got: " + tqe.getMessage());
                }
            }
            if (!betaExceeded) {
                return new ScenarioResult(6, name, false, "Expected TenantQuotaExceededException when tenant exceeds quota");
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "ConcurrencyRaceDetectorChatPort (Thread-Safe Stress Double)";
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Concurrent response", 10, 5); // 15 tokens per call

            AdvancedOfflineHarnessUnderTest.ConcurrencyRaceDetectorChatPort concurrentPort =
                    new AdvancedOfflineHarnessUnderTest.ConcurrencyRaceDetectorChatPort(underlying);

            int threads = 20;
            int callsPerThread = 10;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                final int threadId = i;
                pool.submit(() -> {
                    try {
                        for (int j = 0; j < callsPerThread; j++) {
                            concurrentPort.chat("sys", "thread-" + threadId + "-call-" + j);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            if (!completed) {
                return new ScenarioResult(7, name, false, "Concurrent test timed out");
            }

            int expectedCalls = threads * callsPerThread; // 200 calls
            long expectedTokens = expectedCalls * 15L;    // 3000 tokens

            if (concurrentPort.getCallCount() != expectedCalls) {
                return new ScenarioResult(7, name, false, "Expected callCount " + expectedCalls + ", got: " + concurrentPort.getCallCount());
            }
            if (concurrentPort.getTotalTokens() != expectedTokens) {
                return new ScenarioResult(7, name, false, "Expected totalTokens " + expectedTokens + ", got: " + concurrentPort.getTotalTokens());
            }
            if (concurrentPort.getAuditHistory().size() != expectedCalls) {
                return new ScenarioResult(7, name, false, "Expected auditHistory size " + expectedCalls + ", got: " + concurrentPort.getAuditHistory().size());
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "SchemaValidatingChatPort (Spring AI BeanOutputConverter & Resilient Fallback)";
        try {
            BeanOutputConverter<TriageDecision> converter = new BeanOutputConverter<>(TriageDecision.class);
            TriageDecision fallback = new TriageDecision("FALLBACK", "UNKNOWN_CAUSE", "MANUAL_INVESTIGATION");

            // 1. Success case: Model returns valid structured JSON
            ChatPort validModel = (sys, user, tools) -> new ChatResult(
                    """
                    {"action":"RESTART_POD","rootCause":"OOMKilled memory exhaustion","recommendation":"Scale memory"}
                    """, 100, 50);

            AdvancedOfflineHarnessUnderTest.SchemaValidatingChatPort port1 =
                    new AdvancedOfflineHarnessUnderTest.SchemaValidatingChatPort(validModel);

            TriageDecision validDecision = port1.chatAndValidate("sys", "diagnose pod", converter, fallback);

            if (!"RESTART_POD".equals(validDecision.action()) || !"OOMKilled memory exhaustion".equals(validDecision.rootCause())) {
                return new ScenarioResult(8, name, false, "Parsed decision mismatch: " + validDecision);
            }
            if (port1.getValidationSuccessCount() != 1 || port1.getValidationFailureCount() != 0) {
                return new ScenarioResult(8, name, false, "Success count expected 1, got: " + port1.getValidationSuccessCount());
            }

            // 2. Corrupted / Truncated JSON payload case: Model returns partial broken JSON
            ChatPort brokenModel = (sys, user, tools) -> new ChatResult(
                    "{\"action\":\"RESTART_POD\",\"rootCause\":\"OOMKilled", 50, 20); // cut off midway

            AdvancedOfflineHarnessUnderTest.SchemaValidatingChatPort port2 =
                    new AdvancedOfflineHarnessUnderTest.SchemaValidatingChatPort(brokenModel);

            TriageDecision fallbackDecision = port2.chatAndValidate("sys", "diagnose pod 2", converter, fallback);

            if (!fallback.equals(fallbackDecision)) {
                return new ScenarioResult(8, name, false, "Expected fallback decision on broken JSON, got: " + fallbackDecision);
            }
            if (port2.getValidationFailureCount() != 1 || port2.getValidationSuccessCount() != 0) {
                return new ScenarioResult(8, name, false, "Failure count expected 1, got: " + port2.getValidationFailureCount());
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "ResilientFailoverChatPort (Primary Endpoint Failure -> Fallback Stub)";
        try {
            ChatPort primary = (sys, user, tools) -> {
                throw new RuntimeException("Primary endpoint 503 Service Unavailable");
            };
            ChatPort secondary = (sys, user, tools) -> new ChatResult("Secondary fallback response", 80, 40);

            AdvancedOfflineHarnessUnderTest.ResilientFailoverChatPort stack =
                    new AdvancedOfflineHarnessUnderTest.ResilientFailoverChatPort(primary, secondary);

            ChatResult res = stack.chat("sys", "diagnose cluster");

            if (!"Secondary fallback response".equals(res.text())) {
                return new ScenarioResult(9, name, false, "Expected secondary response, got: " + res.text());
            }
            if (!stack.isFailoverTriggered()) {
                return new ScenarioResult(9, name, false, "Expected failoverTriggered to be true");
            }
            if (stack.getFailoverCount() != 1) {
                return new ScenarioResult(9, name, false, "Expected failoverCount 1, got: " + stack.getFailoverCount());
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "AutonomousChaosTriageHarness (Full Chaos Integration & Triage Lifecycle)";
        try {
            AdvancedTestClock clock = new AdvancedTestClock(10_000L);

            // Primary port: Turn 1 succeeds, Turn 2 fails with 503, Turn 3 succeeds
            Deque<ChatResult> primaryTurns = new ArrayDeque<>();
            primaryTurns.add(new ChatResult("{\"action\":\"INSPECT_METRICS\",\"status\":\"CPU at 98%\"}", 50, 25));
            primaryTurns.add(new ChatResult("{\"action\":\"COMPLETE\",\"rootCause\":\"Thread pool starvation\",\"recommendation\":\"Increase worker threads\"}", 70, 35));

            AtomicInteger primaryCalls = new AtomicInteger(0);
            ChatPort primary = (sys, user, tools) -> {
                int call = primaryCalls.incrementAndGet();
                if (call == 2) {
                    throw new RuntimeException("Primary HTTP 503 Outage during Turn 2");
                }
                return primaryTurns.poll();
            };

            // Secondary port for failover
            ChatPort secondary = (sys, user, tools) ->
                    new ChatResult("{\"action\":\"QUERY_LOGS_FALLBACK\",\"status\":\"Found deadlocked threads\"}", 60, 30);

            AdvancedDiagnosticReport report = AdvancedOfflineHarnessUnderTest.AutonomousChaosTriageHarness.runChaosTriage(
                    primary,
                    secondary,
                    "tenant-sre-prod",
                    "INC-8891",
                    clock
            );

            if (report == null) {
                return new ScenarioResult(10, name, false, "Report must not be null");
            }
            if (!"INC-8891".equals(report.incidentId())) {
                return new ScenarioResult(10, name, false, "Incident ID mismatch: " + report.incidentId());
            }
            if (!"tenant-sre-prod".equals(report.tenantId())) {
                return new ScenarioResult(10, name, false, "Tenant ID mismatch: " + report.tenantId());
            }
            if (report.turnsCompleted() != 3) {
                return new ScenarioResult(10, name, false, "Expected 3 turns completed, got: " + report.turnsCompleted());
            }
            if (!report.failoverUsed()) {
                return new ScenarioResult(10, name, false, "Expected failoverUsed to be true");
            }
            if (!report.rootCause().contains("Thread pool starvation")) {
                return new ScenarioResult(10, name, false, "Expected rootCause to mention Thread pool starvation, got: " + report.rootCause());
            }

            // Total tokens = Turn 1 (50+25=75) + Turn 2 Failover (60+30=90) + Turn 3 (70+35=105) = 270 tokens
            if (report.totalTokensUsed() != 270L) {
                return new ScenarioResult(10, name, false, "Expected totalTokensUsed 270, got: " + report.totalTokensUsed());
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("PHASE 05 EXERCISE 02: ADVANCED OFFLINE CHAOS HARNESS REPORT");
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
