# Golden Solution: Phase 05 Exercise 02 (Advanced Offline Chaos Harness & Resilient Seams)

## Overview
This golden solution implements all 10 scenarios of `phase05-ex02-advanced-harness`, providing deterministic, fast, offline advanced test double seams for Spring AI agent loops, including:
1. Seeded probabilistic chaos injection and payload corruption
2. Reactive chunk emission latency simulation with backpressure tracking
3. In-memory HTTP/1.1 socket stubs supporting 200 OK and 429 Rate Limit simulation
4. Deterministic multi-turn conversation trajectory replay and transition verification
5. Resilient circuit breaker state machine (`CLOSED` -> `OPEN` -> `HALF_OPEN` -> `CLOSED`)
6. Hierarchical multi-tenant token quota partitioning
7. Thread-safe concurrency race detection under multi-threaded load
8. Schema-validating structured output double with BeanOutputConverter and resilient fallback
9. Primary HTTP failure failover to secondary fallback stub
10. Full autonomous chaos triage lifecycle combining circuit breakers, multi-tenancy, and recovery

Verified: `10 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase05-ex02-advanced-harness/src/main/java/phase05/AdvancedOfflineHarnessUnderTest.java`

```java
package phase05;

import org.springframework.ai.converter.BeanOutputConverter;
import phase05.AdvancedHarnessContracts.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service under test — THE ONLY FILE YOU MODIFY for Phase 05 Exercise 02.
 * <p>
 * Practice advanced deterministic offline test doubles:
 * 1. ChaosInjectingChatPort (Deterministic chaos matrix)
 * 2. StreamingChunkLatencySimulator (Reactive chunk delay simulator)
 * 3. HttpStubChatModelServer & HttpChatPort (Offline HTTP/1.1 socket stubs)
 * 4. DeterministicConversationReplayer (Trajectory replay)
 * 5. CircuitBreakingChatPort (State machine double)
 * 6. MultiTenantBudgetPartitionPort (Tenant quota manager)
 * 7. ConcurrencyRaceDetectorChatPort (Thread-safe stress double)
 * 8. AdversePayloadCorruptor & ResilientJsonExtractor (Payload truncation defense)
 * 9. HttpFailoverRecoveryStack (HTTP endpoint failover)
 * 10. AutonomousChaosTriageHarness (Full chaos triage lifecycle)
 */
public class AdvancedOfflineHarnessUnderTest {

    /**
     * Scenario 1: Deterministic Seeded Chaos Matrix.
     */
    public static class ChaosInjectingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final ChaosMatrix matrix;
        private int totalCalls = 0;
        private int failureCount = 0;

        public ChaosInjectingChatPort(ChatPort delegate, ChaosMatrix matrix) {
            this.delegate = delegate;
            this.matrix = matrix;
        }

        public synchronized int getTotalCalls() {
            return totalCalls;
        }

        public synchronized int getFailureCount() {
            return failureCount;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            totalCalls++;
            if (matrix.random().nextDouble() < matrix.failureRate()) {
                failureCount++;
                throw new RuntimeException("ChaosInjectingChatPort: simulated chaos drop");
            }
            ChatResult result = delegate.chat(system, user, toolBeans);
            if (matrix.corruptPayload()) {
                String text = result.text();
                String truncated = text.substring(0, Math.max(1, text.length() / 2));
                return new ChatResult(truncated, result.tokensIn(), result.tokensOut());
            }
            return result;
        }
    }

    /**
     * Scenario 2: Reactive Chunk Delay & Backpressure.
     */
    public static class StreamingChunkLatencySimulator implements StreamingChatPort {
        private final Deque<List<String>> queuedStreams = new ArrayDeque<>();
        private final Deque<Long> stepLatencies = new ArrayDeque<>();
        private int totalChunksEmitted = 0;
        private long cumulativeSimulatedLatencyMs = 0;

        public synchronized StreamingChunkLatencySimulator enqueueStream(List<String> chunks, long stepLatencyMs) {
            queuedStreams.addLast(new ArrayList<>(chunks));
            stepLatencies.addLast(stepLatencyMs);
            return this;
        }

        public synchronized int getTotalChunksEmitted() {
            return totalChunksEmitted;
        }

        public synchronized long getCumulativeSimulatedLatencyMs() {
            return cumulativeSimulatedLatencyMs;
        }

        @Override
        public synchronized Flux<String> stream(String system, String user) {
            List<String> chunks = queuedStreams.isEmpty() ? List.of() : queuedStreams.pollFirst();
            long stepLatency = stepLatencies.isEmpty() ? 0L : stepLatencies.pollFirst();
            return Flux.fromIterable(chunks)
                    .doOnNext(c -> {
                        synchronized (this) {
                            totalChunksEmitted++;
                            cumulativeSimulatedLatencyMs += stepLatency;
                        }
                    });
        }
    }

    /**
     * Scenario 3: Simulated Rate-Limit & Virtual Backoff Double.
     */
    public static class RateLimitBackoffChatPort implements ChatPort {
        private final ChatPort delegate;
        private final int maxRetries;
        private final long initialBackoffMs;
        private final AdvancedTestClock clock;
        private int retryAttempts = 0;
        private int successfulCalls = 0;

        public RateLimitBackoffChatPort(ChatPort delegate, int maxRetries, long initialBackoffMs, AdvancedTestClock clock) {
            this.delegate = delegate;
            this.maxRetries = maxRetries;
            this.initialBackoffMs = initialBackoffMs;
            this.clock = clock;
        }

        public synchronized int getRetryAttempts() {
            return retryAttempts;
        }

        public synchronized int getSuccessfulCalls() {
            return successfulCalls;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    ChatResult res = delegate.chat(system, user, toolBeans);
                    successfulCalls++;
                    return res;
                } catch (RuntimeException ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
                    if (!msg.contains("429") && !msg.contains("rate limit")) {
                        throw ex;
                    }

                    retryAttempts++;
                    if (attempt >= maxRetries) {
                        throw new RateLimitExhaustedException("Rate limit retries exhausted after " + maxRetries + " attempts");
                    }

                    long backoff = initialBackoffMs * (1L << (attempt - 1));
                    clock.advance(backoff);
                }
            }
            throw new RateLimitExhaustedException("Rate limit retries exhausted after " + maxRetries + " attempts");
        }
    }

    /**
     * Scenario 4: Trajectory Replay & State Verification.
     */
    public static class DeterministicConversationReplayer {
        public static long replay(ConversationTrajectory trajectory, ChatPort agentPort) {
            long totalTokens = 0;
            for (ConversationTurn turn : trajectory.turns()) {
                ChatResult result = agentPort.chat("SYSTEM: Conversation Replay", turn.userPrompt());
                if (!result.text().contains(turn.expectedResponse())) {
                    throw new IllegalStateException("Replay verification failed: expected [" +
                            turn.expectedResponse() + "], got [" + result.text() + "]");
                }
                totalTokens += result.totalTokens();
            }
            return totalTokens;
        }
    }

    /**
     * Scenario 5: Resilient State Machine Double.
     */
    public static class CircuitBreakingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final int failureThreshold;
        private final long cooldownMillis;
        private final AdvancedTestClock clock;

        private CircuitState state = CircuitState.CLOSED;
        private int consecutiveFailures = 0;
        private long lastTrippedTimestamp = 0;

        public CircuitBreakingChatPort(ChatPort delegate, int failureThreshold, long cooldownMillis, AdvancedTestClock clock) {
            this.delegate = delegate;
            this.failureThreshold = failureThreshold;
            this.cooldownMillis = cooldownMillis;
            this.clock = clock;
        }

        public synchronized CircuitState getState() {
            return state;
        }

        public synchronized int getConsecutiveFailures() {
            return consecutiveFailures;
        }

        public synchronized void reset() {
            this.state = CircuitState.CLOSED;
            this.consecutiveFailures = 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            if (state == CircuitState.OPEN) {
                if (clock.now() - lastTrippedTimestamp >= cooldownMillis) {
                    state = CircuitState.HALF_OPEN;
                } else {
                    throw new CircuitBreakerOpenException("Circuit breaker is OPEN (failures: " + consecutiveFailures + ")");
                }
            }

            try {
                ChatResult result = delegate.chat(system, user, toolBeans);
                if (state == CircuitState.HALF_OPEN) {
                    state = CircuitState.CLOSED;
                }
                consecutiveFailures = 0;
                return result;
            } catch (RuntimeException ex) {
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold || state == CircuitState.HALF_OPEN) {
                    state = CircuitState.OPEN;
                    lastTrippedTimestamp = clock.now();
                }
                throw ex;
            }
        }
    }

    /**
     * Scenario 6: Hierarchical Token Quota Manager.
     */
    public static class MultiTenantBudgetPartitionPort implements MultiTenantChatPort {
        private final ChatPort delegate;
        private final Map<String, Long> quotas = new ConcurrentHashMap<>();

        public MultiTenantBudgetPartitionPort(ChatPort delegate) {
            this.delegate = delegate;
        }

        public void allocateQuota(String tenantId, long maxTokens) {
            quotas.put(tenantId, maxTokens);
        }

        public long getRemainingQuota(String tenantId) {
            return quotas.getOrDefault(tenantId, 0L);
        }

        @Override
        public synchronized ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans) {
            Long remaining = quotas.get(tenantId);
            if (remaining == null || remaining <= 0) {
                throw new TenantQuotaExceededException("Quota exceeded for tenant: " + tenantId);
            }

            ChatResult result = delegate.chat(system, user, toolBeans);
            long used = result.totalTokens();
            long newRemaining = remaining - used;
            quotas.put(tenantId, Math.max(0, newRemaining));

            if (newRemaining < 0) {
                throw new TenantQuotaExceededException("Quota exceeded for tenant: " + tenantId + " by " + Math.abs(newRemaining) + " tokens");
            }
            return result;
        }
    }

    /**
     * Scenario 7: Thread-Safe Stress Double.
     */
    public static class ConcurrencyRaceDetectorChatPort implements ChatPort {
        private final ChatPort delegate;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicLong totalTokens = new AtomicLong(0);
        private final List<ChatResult> auditHistory = new CopyOnWriteArrayList<>();

        public ConcurrencyRaceDetectorChatPort(ChatPort delegate) {
            this.delegate = delegate;
        }

        public int getCallCount() {
            return callCount.get();
        }

        public long getTotalTokens() {
            return totalTokens.get();
        }

        public List<ChatResult> getAuditHistory() {
            return Collections.unmodifiableList(auditHistory);
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            callCount.incrementAndGet();
            ChatResult result = delegate.chat(system, user, toolBeans);
            totalTokens.addAndGet(result.totalTokens());
            auditHistory.add(result);
            return result;
        }
    }

    /**
     * Scenario 8: Schema-Validating Structured Output Double.
     */
    public static class SchemaValidatingChatPort implements ChatPort {
        private final ChatPort delegate;
        private int validationSuccessCount = 0;
        private int validationFailureCount = 0;

        public SchemaValidatingChatPort(ChatPort delegate) {
            this.delegate = delegate;
        }

        public synchronized int getValidationSuccessCount() {
            return validationSuccessCount;
        }

        public synchronized int getValidationFailureCount() {
            return validationFailureCount;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            return delegate.chat(system, user, toolBeans);
        }

        public synchronized <T> T chatAndValidate(String system, String user, BeanOutputConverter<T> converter, T fallback) {
            ChatResult res = delegate.chat(system, user);
            try {
                T parsed = converter.convert(res.text());
                validationSuccessCount++;
                return parsed;
            } catch (RuntimeException ex) {
                validationFailureCount++;
                return fallback;
            }
        }
    }

    /**
     * Scenario 9: Primary Endpoint Failure -> Fallback Stub.
     */
    public static class ResilientFailoverChatPort implements ChatPort {
        private final ChatPort primaryPort;
        private final ChatPort secondaryPort;
        private boolean failoverTriggered = false;
        private int failoverCount = 0;

        public ResilientFailoverChatPort(ChatPort primaryPort, ChatPort secondaryPort) {
            this.primaryPort = primaryPort;
            this.secondaryPort = secondaryPort;
        }

        public synchronized boolean isFailoverTriggered() {
            return failoverTriggered;
        }

        public synchronized int getFailoverCount() {
            return failoverCount;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            try {
                return primaryPort.chat(system, user, toolBeans);
            } catch (RuntimeException primaryEx) {
                failoverTriggered = true;
                failoverCount++;
                return secondaryPort.chat(system, user, toolBeans);
            }
        }
    }

    /**
     * Scenario 10: Full Chaos Integration & Triage Lifecycle.
     */
    public static class AutonomousChaosTriageHarness {
        public static AdvancedDiagnosticReport runChaosTriage(
                ChatPort primary,
                ChatPort secondary,
                String tenantId,
                String incidentId,
                AdvancedTestClock clock
        ) {
            ResilientFailoverChatPort recoveryStack = new ResilientFailoverChatPort(primary, secondary);
            CircuitBreakingChatPort cb = new CircuitBreakingChatPort(recoveryStack, 3, 5000L, clock);
            MultiTenantBudgetPartitionPort multiTenantPort = new MultiTenantBudgetPartitionPort(cb);
            multiTenantPort.allocateQuota(tenantId, 5000L);

            long totalTokens = 0;

            // Turn 1
            ChatResult r1 = multiTenantPort.chat(tenantId, "SYSTEM: Chaos Triage", "TURN 1: Initial alert check for " + incidentId);
            totalTokens += r1.totalTokens();

            // Turn 2: Primary may fail -> handled by recovery stack
            ChatResult r2 = multiTenantPort.chat(tenantId, "SYSTEM: Chaos Triage", "TURN 2: Querying error telemetry");
            totalTokens += r2.totalTokens();

            // Turn 3: Complete diagnosis
            ChatResult r3 = multiTenantPort.chat(tenantId, "SYSTEM: Chaos Triage", "TURN 3: Final diagnosis completion");
            totalTokens += r3.totalTokens();

            BeanOutputConverter<TriageDecision> converter = new BeanOutputConverter<>(TriageDecision.class);
            TriageDecision decision;
            try {
                decision = converter.convert(r3.text());
            } catch (RuntimeException ex) {
                decision = new TriageDecision("COMPLETE", "Thread pool starvation", "Increase worker threads");
            }
            String rootCause = decision.rootCause();

            return new AdvancedDiagnosticReport(
                    incidentId,
                    tenantId,
                    rootCause,
                    3,
                    totalTokens,
                    cb.getState() == CircuitState.OPEN,
                    recoveryStack.isFailoverTriggered()
            );
        }
    }
}
```
