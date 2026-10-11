package phase05;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import phase05.AdvancedHarnessContracts.*;
import phase05.AdvancedOfflineHarnessUnderTest.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 05 Exercise 02:
 * Advanced Offline Test Harness, Chaos Matrix, Circuit Breakers, Multi-Tenancy & Resilient Loops (15 Repetitive Drills).
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
        // Topic 1: Chaos Injection & Payload Corruption
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Reactive Streaming Delays & Backpressure
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Rate Limiting & Circuit Breakers
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Multi-Tenant Quotas & Thread-Safe Concurrency
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Resilient Failover, Schema Conversion & Autonomous Loops
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Chaos Injection & Payload Corruption (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "ChaosInjectingChatPort (Probabilistic Fault & Latency Injection)";
        List<String> hints = List.of(
                "In chat(), increment totalCalls and check matrix.random().nextDouble() < matrix.failureRate().",
                "If fault is triggered, increment failureCount and throw RuntimeException('Simulated upstream chaos failure').",
                "If corruptPayload is true, truncate returned ChatResult text to text.substring(0, text.length() / 2)."
        );
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Healthy response payload from cluster", 100, 50);

            Random seededRandom = new Random(42);
            ChaosMatrix matrix = new ChaosMatrix(0.5, 50, false, seededRandom);
            ChaosInjectingChatPort chaosPort = new ChaosInjectingChatPort(underlying, matrix);

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
                return new ScenarioResult(1, name, false, "Expected totalCalls 20, got: " + chaosPort.getTotalCalls(), hints);
            }
            if (exceptionsCaught == 0 || exceptionsCaught == totalCalls) {
                return new ScenarioResult(1, name, false, "Chaos failure rate must inject probabilistic faults, got " + exceptionsCaught + " faults out of 20", hints);
            }
            if (chaosPort.getFailureCount() != exceptionsCaught) {
                return new ScenarioResult(1, name, false, "failureCount mismatch: expected " + exceptionsCaught + ", got " + chaosPort.getFailureCount(), hints);
            }

            ChaosMatrix corruptMatrix = new ChaosMatrix(0.0, 0, true, new Random(1));
            ChaosInjectingChatPort corruptPort = new ChaosInjectingChatPort(underlying, corruptMatrix);
            ChatResult corrupted = corruptPort.chat("sys", "test");
            if (corrupted.text().length() >= "Healthy response payload from cluster".length()) {
                return new ScenarioResult(1, name, false, "Corrupted payload must be truncated, got length: " + corrupted.text().length(), hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "CorruptingChaosChatPort (Payload Truncation & Field Mangle Double)";
        List<String> hints = List.of(
                "Wrap underlying ChatPort and inspect returned ChatResult text.",
                "If truncateLength > 0 and text.length() > truncateLength, slice text.substring(0, truncateLength).",
                "If mangleField is configured, replace '\"' + fieldName + '\":\"[^\"]+\"' with badValue."
        );
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("{\"action\":\"RESTART_POD\",\"target\":\"pod-order-1\",\"status\":\"PENDING\"}", 60, 40);

            CorruptingChaosChatPort truncatingPort = new CorruptingChaosChatPort(underlying);
            truncatingPort.truncateAfter(25);

            ChatResult r1 = truncatingPort.chat("sys", "check pod");
            if (r1.text().length() != 25) {
                return new ScenarioResult(2, name, false, "Expected truncated length 25, got: " + r1.text().length() + " (" + r1.text() + ")", hints);
            }
            if (truncatingPort.getCorruptedCallsCount() != 1) {
                return new ScenarioResult(2, name, false, "Expected corruptedCallsCount 1, got: " + truncatingPort.getCorruptedCallsCount(), hints);
            }

            CorruptingChaosChatPort manglingPort = new CorruptingChaosChatPort(underlying);
            manglingPort.mangleField("target", "CORRUPTED_POD_X");

            ChatResult r2 = manglingPort.chat("sys", "check pod");
            if (!r2.text().contains("CORRUPTED_POD_X") || r2.text().contains("pod-order-1")) {
                return new ScenarioResult(2, name, false, "Expected mangled target field in output, got: " + r2.text(), hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "CompositeChaosPipeline (Multi-Stage Chaos Pipeline with Auditing)";
        List<String> hints = List.of(
                "Maintain a List<ChaosAuditEntry> tracking each call's outcome.",
                "Execute stages in order: latency simulation, probabilistic fault, and corruption.",
                "Count severe faults whenever an exception is raised."
        );
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Standard cluster diagnosis response", 50, 25);

            Random seededRandom = new Random(7);
            ChaosMatrix matrix = new ChaosMatrix(0.4, 30L, true, seededRandom);
            CompositeChaosPipeline pipeline = new CompositeChaosPipeline(underlying, matrix);

            int totalCalls = 10;
            int failed = 0;
            for (int i = 0; i < totalCalls; i++) {
                try {
                    pipeline.chat("sys", "call-" + i);
                } catch (RuntimeException ex) {
                    failed++;
                }
            }

            List<ChaosAuditEntry> audit = pipeline.getAuditTrail();
            if (audit.size() != totalCalls) {
                return new ScenarioResult(3, name, false, "Expected 10 audit entries, got: " + audit.size(), hints);
            }
            if (pipeline.getSevereFaultCount() != failed) {
                return new ScenarioResult(3, name, false, "severeFaultCount mismatch: expected " + failed + ", got " + pipeline.getSevereFaultCount(), hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Reactive Streaming Delays & Backpressure (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "StreamingChunkLatencySimulator (Reactive Chunk Delay & Backpressure)";
        List<String> hints = List.of(
                "Maintain queue of queued chunk streams with their simulated latency per chunk.",
                "In stream(), return Flux.fromIterable(chunks) and track totalChunksEmitted and cumulativeSimulatedLatencyMs.",
                "Formula: cumulativeLatency = chunks.size() * simulatedLatencyMsPerChunk."
        );
        try {
            StreamingChunkLatencySimulator simulator = new StreamingChunkLatencySimulator();

            List<String> chunks = List.of("Analyzing", " heap", " dump", " for", " leak");
            simulator.enqueueStream(chunks, 20L);

            Flux<String> stream = simulator.stream("sys", "stream test");
            List<String> received = stream.collectList().block();

            if (received == null || received.size() != 5) {
                return new ScenarioResult(4, name, false, "Expected 5 chunks, got: " + received, hints);
            }
            if (!"Analyzing heap dump for leak".equals(String.join("", received))) {
                return new ScenarioResult(4, name, false, "Received stream mismatch: " + String.join("", received), hints);
            }
            if (simulator.getTotalChunksEmitted() != 5) {
                return new ScenarioResult(4, name, false, "Expected 5 totalChunksEmitted, got: " + simulator.getTotalChunksEmitted(), hints);
            }
            if (simulator.getCumulativeSimulatedLatencyMs() != 100L) {
                return new ScenarioResult(4, name, false, "Expected 100ms cumulative latency, got: " + simulator.getCumulativeSimulatedLatencyMs(), hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "IntermittentDropStreamingChatPort (Simulated Token Drops & Resumption)";
        List<String> hints = List.of(
                "Filter incoming chunk stream against dropPredicate.",
                "Increment droppedChunksCount for each filtered chunk.",
                "Emit non-matching chunks as reactive Flux.fromIterable(filtered)."
        );
        try {
            IntermittentDropStreamingChatPort streamer = new IntermittentDropStreamingChatPort();
            List<String> rawChunks = List.of("Telemetry:", " CPU 90%", " DROP_CORRUPTED_CHUNK", " Memory 85%", " DROP_TIMEOUT");

            streamer.enqueueStreamWithDrops(rawChunks, chunk -> chunk.startsWith(" DROP_"));

            Flux<String> stream = streamer.stream("sys", "stream query");
            List<String> emitted = stream.collectList().block();

            if (emitted == null || emitted.size() != 3) {
                return new ScenarioResult(5, name, false, "Expected 3 chunks emitted, got: " + emitted, hints);
            }
            if (!"Telemetry: CPU 90% Memory 85%".equals(String.join("", emitted))) {
                return new ScenarioResult(5, name, false, "Filtered stream output mismatch: " + String.join("", emitted), hints);
            }
            if (streamer.getDroppedChunksCount() != 2) {
                return new ScenarioResult(5, name, false, "Expected 2 dropped chunks, got: " + streamer.getDroppedChunksCount(), hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "DeterministicConversationReplayer (Trajectory Replay & State Verification)";
        List<String> hints = List.of(
                "Iterate turns in trajectory and invoke agent.chat('sys', turn.userPrompt()).",
                "Verify res.text().contains(turn.expectedResponse()) or matches expected output.",
                "Accumulate totalTokens across all turns; throw IllegalStateException on divergence."
        );
        try {
            ConversationTurn t1 = new ConversationTurn("INSPECT_PODS", "Found 3 pods CrashLoopBackOff", 50);
            ConversationTurn t2 = new ConversationTurn("GET_POD_LOGS", "Fatal error: Database unreachable", 80);
            ConversationTrajectory trajectory = new ConversationTrajectory("traj-42", List.of(t1, t2));

            Deque<ChatResult> answers = new ArrayDeque<>();
            answers.add(new ChatResult("Found 3 pods CrashLoopBackOff", 30, 20));
            answers.add(new ChatResult("Fatal error: Database unreachable", 50, 30));

            ChatPort agent = (sys, user, tools) -> answers.poll();

            long totalTokens = DeterministicConversationReplayer.replay(trajectory, agent);
            if (totalTokens != 130) {
                return new ScenarioResult(6, name, false, "Expected total tokens 130, got: " + totalTokens, hints);
            }

            ChatPort divergentAgent = (sys, user, tools) -> new ChatResult("Unexpected divergent response", 10, 10);
            boolean divergentCaught = false;
            try {
                DeterministicConversationReplayer.replay(trajectory, divergentAgent);
            } catch (IllegalStateException | AssertionError ex) {
                divergentCaught = true;
            }
            if (!divergentCaught) {
                return new ScenarioResult(6, name, false, "Expected failure when agent diverges from golden trajectory", hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Rate Limiting & Circuit Breakers (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "RateLimitBackoffChatPort (Simulated Rate-Limit & Virtual Backoff Double)";
        List<String> hints = List.of(
                "Detect 429 status or 'rate limit' in exception message.",
                "Advance clock with exponential delay (currentDelay *= 2) on each 429 failure.",
                "Throw RateLimitExhaustedException with message containing 'Rate limit retries exhausted' when attempts > maxRetries."
        );
        try {
            AdvancedTestClock clock = new AdvancedTestClock(1000L);
            AtomicInteger attempts = new AtomicInteger(0);

            ChatPort rateLimitedDelegate = (sys, user, tools) -> {
                int count = attempts.incrementAndGet();
                if (count <= 2) {
                    throw new RuntimeException("HTTP 429 Too Many Requests: Rate limit exceeded");
                }
                return new ChatResult("Recovered after backoff", 100, 50);
            };

            RateLimitBackoffChatPort backoffPort = new RateLimitBackoffChatPort(rateLimitedDelegate, 3, 1000L, clock);
            ChatResult res = backoffPort.chat("sys", "user query");

            if (!"Recovered after backoff".equals(res.text())) {
                return new ScenarioResult(7, name, false, "Response text mismatch: " + res.text(), hints);
            }
            if (backoffPort.getRetryAttempts() != 2) {
                return new ScenarioResult(7, name, false, "Expected 2 retryAttempts, got: " + backoffPort.getRetryAttempts(), hints);
            }
            if (backoffPort.getSuccessfulCalls() != 1) {
                return new ScenarioResult(7, name, false, "Expected 1 successfulCalls, got: " + backoffPort.getSuccessfulCalls(), hints);
            }
            if (clock.now() != 4000L) {
                return new ScenarioResult(7, name, false, "Expected clock now 4000L, got: " + clock.now(), hints);
            }

            ChatPort alwaysFails = (sys, user, tools) -> {
                throw new RuntimeException("429 Rate limit quota exceeded");
            };
            RateLimitBackoffChatPort exhaustedPort = new RateLimitBackoffChatPort(alwaysFails, 2, 500L, clock);

            boolean exhaustedCaught = false;
            try {
                exhaustedPort.chat("sys", "user query 2");
            } catch (RateLimitExhaustedException rle) {
                exhaustedCaught = true;
                String msg = rle.getMessage() != null ? rle.getMessage().toLowerCase() : "";
                if (!msg.contains("rate limit retries exhausted")) {
                    return new ScenarioResult(7, name, false,
                            "Exception message must contain 'Rate limit retries exhausted', got: " + rle.getMessage(), hints);
                }
            }
            if (!exhaustedCaught) {
                return new ScenarioResult(7, name, false, "Expected RateLimitExhaustedException when retries are exhausted", hints);
            }

            ChatPort nonRetryable = (sys, user, tools) -> { throw new IllegalArgumentException("Bad input"); };
            RateLimitBackoffChatPort fastFailPort = new RateLimitBackoffChatPort(nonRetryable, 3, 1000L, clock);
            boolean nonRetryableCaught = false;
            try {
                fastFailPort.chat("sys", "bad");
            } catch (IllegalArgumentException ex) {
                nonRetryableCaught = true;
            }
            if (!nonRetryableCaught) {
                return new ScenarioResult(7, name, false, "Expected non-rate-limit exception to fail fast without retrying", hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "CircuitBreakingChatPort (Resilient State Machine Test Double)";
        List<String> hints = List.of(
                "Track failureCount in CLOSED state; trip to OPEN after threshold failures and record openedTimestamp.",
                "In OPEN state, check if (clock.now() - openedTimestamp >= cooldownMs); if not, throw CircuitBreakerOpenException containing 'Circuit breaker is OPEN'.",
                "If cooldown has elapsed, transition to HALF_OPEN; on probe success reset to CLOSED, on failure re-trip to OPEN."
        );
        try {
            AdvancedTestClock clock = new AdvancedTestClock(10_000L);
            AtomicInteger attempts = new AtomicInteger(0);
            AtomicInteger failures = new AtomicInteger(3);

            ChatPort flapper = (sys, user, tools) -> {
                attempts.incrementAndGet();
                if (failures.getAndDecrement() > 0) {
                    throw new RuntimeException("Upstream LLM 503 Service Unavailable");
                }
                return new ChatResult("Recovered response", 50, 25);
            };

            CircuitBreakingChatPort cb = new CircuitBreakingChatPort(flapper, 3, 5000L, clock);

            if (cb.getState() != CircuitState.CLOSED) {
                return new ScenarioResult(8, name, false, "Initial state must be CLOSED, got: " + cb.getState(), hints);
            }

            for (int i = 0; i < 3; i++) {
                try {
                    cb.chat("sys", "query");
                } catch (RuntimeException ignored) {}
            }

            if (cb.getState() != CircuitState.OPEN) {
                return new ScenarioResult(8, name, false, "State after 3 consecutive failures must be OPEN, got: " + cb.getState(), hints);
            }

            boolean openCaught = false;
            try {
                cb.chat("sys", "query during open");
            } catch (CircuitBreakerOpenException cbe) {
                openCaught = true;
                String msg = cbe.getMessage() != null ? cbe.getMessage().toLowerCase() : "";
                if (!msg.contains("circuit breaker is open")) {
                    return new ScenarioResult(8, name, false,
                            "Exception message must contain 'Circuit breaker is OPEN', got: " + cbe.getMessage(), hints);
                }
            }
            if (!openCaught) {
                return new ScenarioResult(8, name, false, "Expected CircuitBreakerOpenException while circuit is OPEN", hints);
            }

            if (attempts.get() != 3) {
                return new ScenarioResult(8, name, false, "Underlying delegate must not be called while OPEN. Attempts: " + attempts.get(), hints);
            }

            clock.advance(5001L);

            ChatResult probeResult = cb.chat("sys", "probe call");
            if (!"Recovered response".equals(probeResult.text())) {
                return new ScenarioResult(8, name, false, "Probe call response mismatch: " + probeResult.text(), hints);
            }
            if (cb.getState() != CircuitState.CLOSED) {
                return new ScenarioResult(8, name, false, "Circuit must recover to CLOSED after successful probe, got: " + cb.getState(), hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "AdaptiveThrottlingChatPort (Sliding Window Dynamic Rate Limiter)";
        List<String> hints = List.of(
                "Maintain a queue of timestamps of recent calls.",
                "Prune timestamps older than (clock.now() - windowMs).",
                "If queue.size() >= maxCallsPerWindow, throw RateLimitExhaustedException and increment throttledCallsCount."
        );
        try {
            AdvancedTestClock clock = new AdvancedTestClock(1000L);
            ChatPort underlying = (sys, user, tools) -> new ChatResult("OK", 10, 5);

            AdaptiveThrottlingChatPort throttler = new AdaptiveThrottlingChatPort(underlying, 3, 1000L, clock);

            throttler.chat("sys", "call 1");
            throttler.chat("sys", "call 2");
            throttler.chat("sys", "call 3");

            clock.advance(200L); // at 1200L
            boolean throttledCaught = false;
            try {
                throttler.chat("sys", "call 4 - exceeds window");
            } catch (RateLimitExhaustedException rle) {
                throttledCaught = true;
            }
            if (!throttledCaught) {
                return new ScenarioResult(9, name, false, "Expected RateLimitExhaustedException when window capacity is breached", hints);
            }
            if (throttler.getThrottledCallsCount() != 1) {
                return new ScenarioResult(9, name, false, "Expected throttledCallsCount 1, got: " + throttler.getThrottledCallsCount(), hints);
            }

            clock.advance(900L); // at 2100L (old calls at 1000L are now outside 1000ms window)
            ChatResult afterReset = throttler.chat("sys", "call 5 - reset");
            if (!"OK".equals(afterReset.text())) {
                return new ScenarioResult(9, name, false, "Expected call to succeed after window advancement, got: " + afterReset.text(), hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Multi-Tenant Quotas & Thread-Safe Concurrency (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "MultiTenantBudgetPartitionPort (Hierarchical Token Quota Manager)";
        List<String> hints = List.of(
                "Maintain per-tenant quota map: Map<String, Long> remainingQuota.",
                "In chat(), calculate call tokens; if remainingQuota < callTokens, throw TenantQuotaExceededException.",
                "Exception message MUST contain 'Quota exceeded for tenant'."
        );
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Tenant response", 60, 40);

            MultiTenantBudgetPartitionPort mtPort = new MultiTenantBudgetPartitionPort(underlying);
            mtPort.allocateQuota("tenant-alpha", 250);
            mtPort.allocateQuota("tenant-beta", 150);

            mtPort.chat("tenant-alpha", "sys", "call 1");
            mtPort.chat("tenant-alpha", "sys", "call 2");

            if (mtPort.getRemainingQuota("tenant-alpha") != 50) {
                return new ScenarioResult(10, name, false, "Expected 50 tokens remaining for alpha, got: " + mtPort.getRemainingQuota("tenant-alpha"), hints);
            }

            mtPort.chat("tenant-beta", "sys", "beta call 1");
            if (mtPort.getRemainingQuota("tenant-beta") != 50) {
                return new ScenarioResult(10, name, false, "Expected 50 tokens remaining for beta, got: " + mtPort.getRemainingQuota("tenant-beta"), hints);
            }

            boolean betaExceeded = false;
            try {
                mtPort.chat("tenant-beta", "sys", "beta call 2");
            } catch (TenantQuotaExceededException tqe) {
                betaExceeded = true;
                String msg = tqe.getMessage() != null ? tqe.getMessage().toLowerCase() : "";
                if (!msg.contains("quota exceeded for tenant")) {
                    return new ScenarioResult(10, name, false,
                            "Exception message must contain 'Quota exceeded for tenant', got: " + tqe.getMessage(), hints);
                }
            }
            if (!betaExceeded) {
                return new ScenarioResult(10, name, false, "Expected TenantQuotaExceededException when tenant exceeds quota", hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "TenantTierThrottlingChatPort (Tier-Based Quotas & Policies)";
        List<String> hints = List.of(
                "Map tenants to TenantTier (FREE: 200, PRO: 2000, ENTERPRISE: 20000 tokens).",
                "Track cumulative tokens used per tenant; throw TenantQuotaExceededException when usage + tokens > tier quota.",
                "getTenantUsage(tenantId) returns TenantUsage(used, maxQuota)."
        );
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Tier response", 90, 60); // 150 tokens per call

            TenantTierThrottlingChatPort tierPort = new TenantTierThrottlingChatPort(underlying);
            tierPort.assignTier("dev-user", TenantTier.FREE); // 200 quota

            ChatResult r1 = tierPort.chat("dev-user", "sys", "call 1");
            if (tierPort.getTenantUsage("dev-user").usedTokens() != 150L) {
                return new ScenarioResult(11, name, false, "Expected 150 tokens used, got: " + tierPort.getTenantUsage("dev-user"), hints);
            }

            boolean freeExceeded = false;
            try {
                tierPort.chat("dev-user", "sys", "call 2"); // 150 + 150 = 300 > 200
            } catch (TenantQuotaExceededException tqe) {
                freeExceeded = true;
            }
            if (!freeExceeded) {
                return new ScenarioResult(11, name, false, "Expected TenantQuotaExceededException when FREE tier quota is breached", hints);
            }

            tierPort.assignTier("enterprise-client", TenantTier.ENTERPRISE); // 20000 quota
            tierPort.chat("enterprise-client", "sys", "enterprise call");
            if (tierPort.getTenantUsage("enterprise-client").remainingTokens() != 19850L) {
                return new ScenarioResult(11, name, false, "Expected 19850 remaining tokens, got: " + tierPort.getTenantUsage("enterprise-client").remainingTokens(), hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "ConcurrencyRaceDetectorChatPort (Thread-Safe Stress Double)";
        List<String> hints = List.of(
                "Use AtomicInteger or LongAdder for call count and token counters.",
                "Store audit records in CopyOnWriteArrayList or synchronized collection.",
                "Ensure chat() is thread-safe without race conditions."
        );
        try {
            ChatPort underlying = (sys, user, tools) -> new ChatResult("Concurrent response", 10, 5);

            ConcurrencyRaceDetectorChatPort concurrentPort = new ConcurrencyRaceDetectorChatPort(underlying);

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
                return new ScenarioResult(12, name, false, "Concurrent test timed out", hints);
            }

            int expectedCalls = threads * callsPerThread;
            long expectedTokens = expectedCalls * 15L;

            if (concurrentPort.getCallCount() != expectedCalls) {
                return new ScenarioResult(12, name, false, "Expected callCount " + expectedCalls + ", got: " + concurrentPort.getCallCount(), hints);
            }
            if (concurrentPort.getTotalTokens() != expectedTokens) {
                return new ScenarioResult(12, name, false, "Expected totalTokens " + expectedTokens + ", got: " + concurrentPort.getTotalTokens(), hints);
            }
            if (concurrentPort.getAuditHistory().size() != expectedCalls) {
                return new ScenarioResult(12, name, false, "Expected auditHistory size " + expectedCalls + ", got: " + concurrentPort.getAuditHistory().size(), hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Resilient Failover, Schema Conversion & Autonomous Loops (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "ResilientFailoverChatPort (Primary Endpoint Failure -> Fallback Stub)";
        List<String> hints = List.of(
                "Wrap primary and secondary ChatPort delegates.",
                "In chat(), try calling primary; in catch (RuntimeException ex), set failoverTriggered=true, increment failoverCount, and delegate to secondary.",
                "Return secondary's ChatResult on fallback."
        );
        try {
            ChatPort primary = (sys, user, tools) -> {
                throw new RuntimeException("Primary endpoint 503 Service Unavailable");
            };
            ChatPort secondary = (sys, user, tools) -> new ChatResult("Secondary fallback response", 80, 40);

            ResilientFailoverChatPort stack = new ResilientFailoverChatPort(primary, secondary);
            ChatResult res = stack.chat("sys", "diagnose cluster");

            if (!"Secondary fallback response".equals(res.text())) {
                return new ScenarioResult(13, name, false, "Expected secondary response, got: " + res.text(), hints);
            }
            if (!stack.isFailoverTriggered()) {
                return new ScenarioResult(13, name, false, "Expected failoverTriggered to be true", hints);
            }
            if (stack.getFailoverCount() != 1) {
                return new ScenarioResult(13, name, false, "Expected failoverCount 1, got: " + stack.getFailoverCount(), hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "SchemaValidatingChatPort (Spring AI BeanOutputConverter & Resilient Fallback)";
        List<String> hints = List.of(
                "Call delegate.chat() to get raw output text.",
                "Try converter.convert(rawText); on success, increment validationSuccessCount and return decision.",
                "On parse error / exception, increment validationFailureCount and return fallbackDecision."
        );
        try {
            BeanOutputConverter<TriageDecision> converter = new BeanOutputConverter<>(TriageDecision.class);
            TriageDecision fallback = new TriageDecision("FALLBACK", "UNKNOWN_CAUSE", "MANUAL_INVESTIGATION");

            ChatPort validModel = (sys, user, tools) -> new ChatResult(
                    """
                    {"action":"RESTART_POD","rootCause":"OOMKilled memory exhaustion","recommendation":"Scale memory"}
                    """, 100, 50);

            SchemaValidatingChatPort port1 = new SchemaValidatingChatPort(validModel);
            TriageDecision validDecision = port1.chatAndValidate("sys", "diagnose pod", converter, fallback);

            if (!"RESTART_POD".equals(validDecision.action()) || !"OOMKilled memory exhaustion".equals(validDecision.rootCause())) {
                return new ScenarioResult(14, name, false, "Parsed decision mismatch: " + validDecision, hints);
            }
            if (port1.getValidationSuccessCount() != 1 || port1.getValidationFailureCount() != 0) {
                return new ScenarioResult(14, name, false, "Success count expected 1, got: " + port1.getValidationSuccessCount(), hints);
            }

            ChatPort brokenModel = (sys, user, tools) -> new ChatResult(
                    "{\"action\":\"RESTART_POD\",\"rootCause\":\"OOMKilled", 50, 20);

            SchemaValidatingChatPort port2 = new SchemaValidatingChatPort(brokenModel);
            TriageDecision fallbackDecision = port2.chatAndValidate("sys", "diagnose pod 2", converter, fallback);

            if (!fallback.equals(fallbackDecision)) {
                return new ScenarioResult(14, name, false, "Expected fallback decision on broken JSON, got: " + fallbackDecision, hints);
            }
            if (port2.getValidationFailureCount() != 1 || port2.getValidationSuccessCount() != 0) {
                return new ScenarioResult(14, name, false, "Failure count expected 1, got: " + port2.getValidationFailureCount(), hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "AutonomousChaosTriageHarness (Full Chaos Integration & Triage Lifecycle)";
        List<String> hints = List.of(
                "Wrap primary and secondary ports in ResilientFailoverChatPort.",
                "Execute 3 turns: Turn 1 (Metrics), Turn 2 (Logs under outage/failover), Turn 3 (Diagnosis completion).",
                "Extract rootCause and return AdvancedDiagnosticReport with token total and failover status."
        );
        try {
            AdvancedTestClock clock = new AdvancedTestClock(10_000L);

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

            ChatPort secondary = (sys, user, tools) ->
                    new ChatResult("{\"action\":\"QUERY_LOGS_FALLBACK\",\"status\":\"Found deadlocked threads\"}", 60, 30);

            AdvancedDiagnosticReport report = AutonomousChaosTriageHarness.runChaosTriage(
                    primary,
                    secondary,
                    "tenant-sre-prod",
                    "INC-8891",
                    clock
            );

            if (report == null) {
                return new ScenarioResult(15, name, false, "Report must not be null", hints);
            }
            if (!"INC-8891".equals(report.incidentId())) {
                return new ScenarioResult(15, name, false, "Incident ID mismatch: " + report.incidentId(), hints);
            }
            if (!"tenant-sre-prod".equals(report.tenantId())) {
                return new ScenarioResult(15, name, false, "Tenant ID mismatch: " + report.tenantId(), hints);
            }
            if (report.turnsCompleted() != 3) {
                return new ScenarioResult(15, name, false, "Expected 3 turns completed, got: " + report.turnsCompleted(), hints);
            }
            if (!report.failoverUsed()) {
                return new ScenarioResult(15, name, false, "Expected failoverUsed to be true", hints);
            }
            if (!report.rootCause().contains("Thread pool starvation")) {
                return new ScenarioResult(15, name, false, "Expected rootCause to mention Thread pool starvation, got: " + report.rootCause(), hints);
            }
            if (report.totalTokensUsed() != 270L) {
                return new ScenarioResult(15, name, false, "Expected totalTokensUsed 270, got: " + report.totalTokensUsed(), hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 05 EXERCISE 02: ADVANCED OFFLINE CHAOS HARNESS REPORT");
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
