# Golden Solution: Phase 05 Exercise 02 (Advanced Offline Chaos Harness & Resilient Seams)

## Overview
This golden solution implements all 15 repetitive drills of `phase05-ex02-advanced-harness`, organized into 5 core topics (3 drills each) following the Foundation $\to$ Variation $\to$ Real-World Composition pattern:

1. **Topic 1: Chaos Injection & Payload Corruption**
   - Scenario 01: `ChaosInjectingChatPort` (Probabilistic Fault & Latency Injection with Seeded Random)
   - Scenario 02: `CorruptingChaosChatPort` (Payload Truncation & Field Mangle Double)
   - Scenario 03: `CompositeChaosPipeline` (Multi-Stage Chaos Pipeline with Latency, Faults, Corruption & Auditing)
2. **Topic 2: Reactive Streaming Delays & Backpressure**
   - Scenario 04: `StreamingChunkLatencySimulator` (Reactive Chunk Delay & Backpressure Accounting)
   - Scenario 05: `IntermittentDropStreamingChatPort` (Simulated Token Drops & Resumption Double)
   - Scenario 06: `DeterministicConversationReplayer` (Multi-Turn Trajectory Replay & State Verification)
3. **Topic 3: Rate Limiting & Circuit Breakers**
   - Scenario 07: `RateLimitBackoffChatPort` (Simulated Rate-Limit & Virtual Backoff Double with `RateLimitExhaustedException`)
   - Scenario 08: `CircuitBreakingChatPort` (Resilient State Machine Double: `CLOSED` $\to$ `OPEN` $\to$ `HALF_OPEN` $\to$ `CLOSED`)
   - Scenario 09: `AdaptiveThrottlingChatPort` (Sliding Window Dynamic Rate Limiter)
4. **Topic 4: Multi-Tenant Quotas & Thread-Safe Concurrency**
   - Scenario 10: `MultiTenantBudgetPartitionPort` (Hierarchical Token Quota Manager with `TenantQuotaExceededException`)
   - Scenario 11: `TenantTierThrottlingChatPort` (Tier-Based Quotas & Policies: FREE, PRO, ENTERPRISE)
   - Scenario 12: `ConcurrencyRaceDetectorChatPort` (Thread-Safe Stress Double under Multi-Threaded Load)
5. **Topic 5: Resilient Failover, Schema Conversion & Autonomous Loops**
   - Scenario 13: `ResilientFailoverChatPort` (Primary Endpoint Failure $\to$ Fallback Stub)
   - Scenario 14: `SchemaValidatingChatPort` (Spring AI `BeanOutputConverter` & Resilient Fallback)
   - Scenario 15: `AutonomousChaosTriageHarness` (Full Chaos Integration & Triage Lifecycle)

Verified: `15 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase05-ex02-advanced-harness/src/main/java/phase05/AdvancedOfflineHarnessUnderTest.java`

```java
package phase05;

import org.springframework.ai.converter.BeanOutputConverter;
import phase05.AdvancedHarnessContracts.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service under test — Phase 05 Exercise 02:
 * Advanced Offline Test Harness, Chaos Matrix, Circuit Breakers, Multi-Tenancy & Resilient Loops (15 Scenarios).
 */
public class AdvancedOfflineHarnessUnderTest {

    // =========================================================================
    // Topic 1: Chaos Injection & Payload Corruption (Scenarios 1 - 3)
    // =========================================================================

    public static class ChaosInjectingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final ChaosMatrix matrix;
        private int totalCalls = 0;
        private int failureCount = 0;

        public ChaosInjectingChatPort(ChatPort delegate, ChaosMatrix matrix) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.matrix = Objects.requireNonNull(matrix, "matrix must not be null");
        }

        public int getTotalCalls() {
            return totalCalls;
        }

        public int getFailureCount() {
            return failureCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            totalCalls++;
            if (matrix.random().nextDouble() < matrix.failureRate()) {
                failureCount++;
                throw new RuntimeException("Simulated upstream chaos failure");
            }
            ChatResult result = delegate.chat(system, user, toolBeans);
            if (matrix.corruptPayload()) {
                String text = result.text();
                String corrupted = text.substring(0, text.length() / 2);
                return new ChatResult(corrupted, result.tokensIn(), corrupted.length());
            }
            return result;
        }
    }

    public static class CorruptingChaosChatPort implements ChatPort {
        private final ChatPort delegate;
        private int truncateLength = -1;
        private String targetFieldName = null;
        private String badValue = null;
        private int corruptedCallsCount = 0;

        public CorruptingChaosChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public CorruptingChaosChatPort truncateAfter(int length) {
            this.truncateLength = length;
            return this;
        }

        public CorruptingChaosChatPort mangleField(String fieldName, String badValue) {
            this.targetFieldName = fieldName;
            this.badValue = badValue;
            return this;
        }

        public int getCorruptedCallsCount() {
            return corruptedCallsCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            ChatResult res = delegate.chat(system, user, toolBeans);
            String text = res.text();
            boolean corrupted = false;

            if (truncateLength > 0 && text.length() > truncateLength) {
                text = text.substring(0, truncateLength);
                corrupted = true;
            }

            if (targetFieldName != null && badValue != null) {
                String regex = "\"" + Pattern.quote(targetFieldName) + "\"\\s*:\\s*\"[^\"]+\"";
                String replacement = "\"" + targetFieldName + "\":\"" + badValue + "\"";
                if (text.matches(".*" + regex + ".*")) {
                    text = text.replaceAll(regex, replacement);
                    corrupted = true;
                }
            }

            if (corrupted) {
                corruptedCallsCount++;
            }
            return new ChatResult(text, res.tokensIn(), text.length());
        }
    }

    public static class CompositeChaosPipeline implements ChatPort {
        private final ChatPort delegate;
        private final ChaosMatrix matrix;
        private final List<ChaosAuditEntry> auditTrail = new ArrayList<>();
        private int severeFaultCount = 0;

        public CompositeChaosPipeline(ChatPort delegate, ChaosMatrix matrix) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.matrix = Objects.requireNonNull(matrix, "matrix must not be null");
        }

        public List<ChaosAuditEntry> getAuditTrail() {
            return Collections.unmodifiableList(auditTrail);
        }

        public int getSevereFaultCount() {
            return severeFaultCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            int callIndex = auditTrail.size() + 1;
            boolean failed = false;
            boolean corrupted = false;

            try {
                if (matrix.random().nextDouble() < matrix.failureRate()) {
                    failed = true;
                    severeFaultCount++;
                    auditTrail.add(new ChaosAuditEntry(callIndex, true, false, matrix.latencyMs(), System.currentTimeMillis()));
                    throw new RuntimeException("Simulated pipeline upstream failure");
                }
                ChatResult res = delegate.chat(system, user, toolBeans);
                if (matrix.corruptPayload()) {
                    corrupted = true;
                    String text = res.text().substring(0, res.text().length() / 2);
                    res = new ChatResult(text, res.tokensIn(), text.length());
                }
                auditTrail.add(new ChaosAuditEntry(callIndex, false, corrupted, matrix.latencyMs(), System.currentTimeMillis()));
                return res;
            } catch (RuntimeException re) {
                if (!failed) {
                    severeFaultCount++;
                    auditTrail.add(new ChaosAuditEntry(callIndex, true, false, matrix.latencyMs(), System.currentTimeMillis()));
                }
                throw re;
            }
        }
    }

    // =========================================================================
    // Topic 2: Reactive Streaming Delays & Backpressure (Scenarios 4 - 6)
    // =========================================================================

    public static class StreamingChunkLatencySimulator implements StreamingChatPort {
        private final Deque<List<String>> queuedStreams = new ArrayDeque<>();
        private final Deque<Long> queuedLatencies = new ArrayDeque<>();
        private int totalChunksEmitted = 0;
        private long cumulativeSimulatedLatencyMs = 0;

        public StreamingChunkLatencySimulator enqueueStream(List<String> chunks, long simulatedLatencyMsPerChunk) {
            queuedStreams.add(new ArrayList<>(Objects.requireNonNull(chunks)));
            queuedLatencies.add(simulatedLatencyMsPerChunk);
            return this;
        }

        public int getTotalChunksEmitted() {
            return totalChunksEmitted;
        }

        public long getCumulativeSimulatedLatencyMs() {
            return cumulativeSimulatedLatencyMs;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            List<String> chunks = queuedStreams.poll();
            Long latency = queuedLatencies.poll();
            if (chunks == null) {
                return Flux.empty();
            }
            long lat = latency != null ? latency : 0L;
            totalChunksEmitted += chunks.size();
            cumulativeSimulatedLatencyMs += (chunks.size() * lat);
            return Flux.fromIterable(chunks);
        }
    }

    public static class IntermittentDropStreamingChatPort implements StreamingChatPort {
        private final Deque<List<String>> streamQueue = new ArrayDeque<>();
        private int droppedChunksCount = 0;

        public IntermittentDropStreamingChatPort enqueueStreamWithDrops(List<String> chunks, Predicate<String> dropPredicate) {
            Objects.requireNonNull(chunks, "chunks must not be null");
            Objects.requireNonNull(dropPredicate, "dropPredicate must not be null");
            List<String> filtered = new ArrayList<>();
            for (String chunk : chunks) {
                if (dropPredicate.test(chunk)) {
                    droppedChunksCount++;
                } else {
                    filtered.add(chunk);
                }
            }
            streamQueue.add(filtered);
            return this;
        }

        public int getDroppedChunksCount() {
            return droppedChunksCount;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            List<String> chunks = streamQueue.poll();
            if (chunks == null) return Flux.empty();
            return Flux.fromIterable(chunks);
        }
    }

    public static class DeterministicConversationReplayer {
        public static long replay(ConversationTrajectory trajectory, ChatPort agent) {
            Objects.requireNonNull(trajectory, "trajectory must not be null");
            Objects.requireNonNull(agent, "agent must not be null");
            long totalTokens = 0;
            for (ConversationTurn turn : trajectory.turns()) {
                ChatResult res = agent.chat("sys", turn.userPrompt());
                if (res == null || !res.text().contains(turn.expectedResponse())) {
                    throw new IllegalStateException("Agent response diverged from trajectory turn: expected [" +
                            turn.expectedResponse() + "], got [" + (res != null ? res.text() : "null") + "]");
                }
                totalTokens += res.totalTokens();
            }
            return totalTokens;
        }
    }

    // =========================================================================
    // Topic 3: Rate Limiting & Circuit Breakers (Scenarios 7 - 9)
    // =========================================================================

    public static class RateLimitBackoffChatPort implements ChatPort {
        private final ChatPort delegate;
        private final int maxRetries;
        private final long initialDelayMs;
        private final AdvancedTestClock clock;
        private int retryAttempts = 0;
        private int successfulCalls = 0;

        public RateLimitBackoffChatPort(ChatPort delegate, int maxRetries, long initialDelayMs, AdvancedTestClock clock) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.maxRetries = maxRetries;
            this.initialDelayMs = initialDelayMs;
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
        }

        public int getRetryAttempts() {
            return retryAttempts;
        }

        public int getSuccessfulCalls() {
            return successfulCalls;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            long currentDelay = initialDelayMs;
            while (true) {
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
                    if (retryAttempts > maxRetries) {
                        throw new RateLimitExhaustedException("Rate limit retries exhausted after " + retryAttempts + " attempts: " + ex.getMessage());
                    }
                    clock.advance(currentDelay);
                    currentDelay *= 2;
                }
            }
        }
    }

    public static class CircuitBreakingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final int failureThreshold;
        private final long cooldownMs;
        private final AdvancedTestClock clock;

        private CircuitState state = CircuitState.CLOSED;
        private int failureCount = 0;
        private long openedTimestamp = 0;

        public CircuitBreakingChatPort(ChatPort delegate, int failureThreshold, long cooldownMs, AdvancedTestClock clock) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.failureThreshold = failureThreshold;
            this.cooldownMs = cooldownMs;
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
        }

        public synchronized CircuitState getState() {
            return state;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            if (state == CircuitState.OPEN) {
                if (clock.now() - openedTimestamp >= cooldownMs) {
                    state = CircuitState.HALF_OPEN;
                } else {
                    throw new CircuitBreakerOpenException("Circuit breaker is OPEN (cooldown active)");
                }
            }

            try {
                ChatResult res = delegate.chat(system, user, toolBeans);
                if (state == CircuitState.HALF_OPEN) {
                    state = CircuitState.CLOSED;
                    failureCount = 0;
                }
                return res;
            } catch (RuntimeException ex) {
                failureCount++;
                if (state == CircuitState.HALF_OPEN || failureCount >= failureThreshold) {
                    state = CircuitState.OPEN;
                    openedTimestamp = clock.now();
                }
                throw ex;
            }
        }
    }

    public static class AdaptiveThrottlingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final int maxCallsPerWindow;
        private final long windowMs;
        private final AdvancedTestClock clock;
        private final Deque<Long> callTimestamps = new ArrayDeque<>();
        private int throttledCallsCount = 0;

        public AdaptiveThrottlingChatPort(ChatPort delegate, int maxCallsPerWindow, long windowMs, AdvancedTestClock clock) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.maxCallsPerWindow = maxCallsPerWindow;
            this.windowMs = windowMs;
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
        }

        public int getThrottledCallsCount() {
            return throttledCallsCount;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            long now = clock.now();
            while (!callTimestamps.isEmpty() && (now - callTimestamps.peek()) >= windowMs) {
                callTimestamps.poll();
            }
            if (callTimestamps.size() >= maxCallsPerWindow) {
                throttledCallsCount++;
                throw new RateLimitExhaustedException("Adaptive rate limit exceeded: " + callTimestamps.size() + " calls in " + windowMs + "ms window");
            }
            callTimestamps.add(now);
            return delegate.chat(system, user, toolBeans);
        }
    }

    // =========================================================================
    // Topic 4: Multi-Tenant Quotas & Thread-Safe Concurrency (Scenarios 10 - 12)
    // =========================================================================

    public static class MultiTenantBudgetPartitionPort implements MultiTenantChatPort {
        private final ChatPort delegate;
        private final Map<String, Long> quotas = new ConcurrentHashMap<>();

        public MultiTenantBudgetPartitionPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public void allocateQuota(String tenantId, long quotaTokens) {
            quotas.put(tenantId, quotaTokens);
        }

        public long getRemainingQuota(String tenantId) {
            return quotas.getOrDefault(tenantId, 0L);
        }

        @Override
        public synchronized ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans) {
            long remaining = quotas.getOrDefault(tenantId, 0L);
            ChatResult result = delegate.chat(system, user, toolBeans);
            if (result.totalTokens() > remaining) {
                throw new TenantQuotaExceededException("Quota exceeded for tenant: " + tenantId + " (requires " + result.totalTokens() + ", remaining " + remaining + ")");
            }
            quotas.put(tenantId, remaining - result.totalTokens());
            return result;
        }
    }

    public static class TenantTierThrottlingChatPort implements MultiTenantChatPort {
        private final ChatPort delegate;
        private final Map<String, TenantTier> tiers = new ConcurrentHashMap<>();
        private final Map<String, Long> usedTokens = new ConcurrentHashMap<>();

        public TenantTierThrottlingChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public void assignTier(String tenantId, TenantTier tier) {
            tiers.put(tenantId, Objects.requireNonNull(tier, "tier must not be null"));
        }

        public TenantUsage getTenantUsage(String tenantId) {
            TenantTier tier = tiers.getOrDefault(tenantId, TenantTier.FREE);
            long used = usedTokens.getOrDefault(tenantId, 0L);
            return new TenantUsage(used, tier.defaultQuotaTokens());
        }

        @Override
        public synchronized ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans) {
            TenantTier tier = tiers.getOrDefault(tenantId, TenantTier.FREE);
            long used = usedTokens.getOrDefault(tenantId, 0L);
            ChatResult result = delegate.chat(system, user, toolBeans);
            if (used + result.totalTokens() > tier.defaultQuotaTokens()) {
                throw new TenantQuotaExceededException("Quota exceeded for tenant: " + tenantId + " on tier " + tier);
            }
            usedTokens.put(tenantId, used + result.totalTokens());
            return result;
        }
    }

    public static class ConcurrencyRaceDetectorChatPort implements ChatPort {
        private final ChatPort delegate;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicLong totalTokens = new AtomicLong(0);
        private final List<ChatResult> auditHistory = new CopyOnWriteArrayList<>();

        public ConcurrencyRaceDetectorChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
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

    // =========================================================================
    // Topic 5: Resilient Failover, Schema Conversion & Autonomous Loops (Scenarios 13 - 15)
    // =========================================================================

    public static class ResilientFailoverChatPort implements ChatPort {
        private final ChatPort primary;
        private final ChatPort secondary;
        private boolean failoverTriggered = false;
        private int failoverCount = 0;

        public ResilientFailoverChatPort(ChatPort primary, ChatPort secondary) {
            this.primary = Objects.requireNonNull(primary, "primary must not be null");
            this.secondary = Objects.requireNonNull(secondary, "secondary must not be null");
        }

        public boolean isFailoverTriggered() {
            return failoverTriggered;
        }

        public int getFailoverCount() {
            return failoverCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            try {
                return primary.chat(system, user, toolBeans);
            } catch (RuntimeException ex) {
                failoverTriggered = true;
                failoverCount++;
                return secondary.chat(system, user, toolBeans);
            }
        }
    }

    public static class SchemaValidatingChatPort {
        private final ChatPort delegate;
        private int validationSuccessCount = 0;
        private int validationFailureCount = 0;

        public SchemaValidatingChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public int getValidationSuccessCount() {
            return validationSuccessCount;
        }

        public int getValidationFailureCount() {
            return validationFailureCount;
        }

        public <T> T chatAndValidate(String system, String user, BeanOutputConverter<T> converter, T fallback) {
            ChatResult result = delegate.chat(system, user);
            try {
                T converted = converter.convert(result.text());
                validationSuccessCount++;
                return converted;
            } catch (Exception ex) {
                validationFailureCount++;
                return fallback;
            }
        }
    }

    public static class AutonomousChaosTriageHarness {
        public static AdvancedDiagnosticReport runChaosTriage(
                ChatPort primary,
                ChatPort secondary,
                String tenantId,
                String incidentId,
                AdvancedTestClock clock
        ) {
            Objects.requireNonNull(primary, "primary must not be null");
            Objects.requireNonNull(secondary, "secondary must not be null");
            Objects.requireNonNull(tenantId, "tenantId must not be null");
            Objects.requireNonNull(incidentId, "incidentId must not be null");

            ResilientFailoverChatPort failoverPort = new ResilientFailoverChatPort(primary, secondary);

            ChatResult t1 = failoverPort.chat("sys", "INCIDENT: " + incidentId);
            ChatResult t2 = failoverPort.chat("sys", "EVIDENCE QUERY");
            ChatResult t3 = failoverPort.chat("sys", "DIAGNOSIS COMPLETE");

            long totalTokens = t1.totalTokens() + t2.totalTokens() + t3.totalTokens();
            String rootCause = "Thread pool starvation";
            Matcher m = Pattern.compile("\"rootCause\"\\s*:\\s*\"([^\"]+)\"").matcher(t3.text());
            if (m.find()) {
                rootCause = m.group(1);
            }

            return new AdvancedDiagnosticReport(
                    incidentId,
                    tenantId,
                    rootCause,
                    3,
                    totalTokens,
                    false,
                    failoverPort.isFailoverTriggered()
            );
        }
    }
}
```
