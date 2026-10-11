package phase05;

import org.springframework.ai.converter.BeanOutputConverter;
import phase05.AdvancedHarnessContracts.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Predicate;

/**
 * Service under test — THE ONLY FILE YOU MODIFY for Phase 05 Exercise 02.
 * <p>
 * Practice advanced deterministic offline test doubles across 15 repetitive drills:
 * 1. ChaosInjectingChatPort (Deterministic chaos matrix)
 * 2. CorruptingChaosChatPort (Payload truncation & field mangling)
 * 3. CompositeChaosPipeline (Multi-stage chaos pipeline)
 * 4. StreamingChunkLatencySimulator (Reactive chunk delay simulator)
 * 5. IntermittentDropStreamingChatPort (Simulated token drops)
 * 6. DeterministicConversationReplayer (Trajectory replay)
 * 7. RateLimitBackoffChatPort (Rate limit & virtual backoff)
 * 8. CircuitBreakingChatPort (State machine double)
 * 9. AdaptiveThrottlingChatPort (Sliding window rate limiter)
 * 10. MultiTenantBudgetPartitionPort (Tenant quota manager)
 * 11. TenantTierThrottlingChatPort (Subscription tier quotas)
 * 12. ConcurrencyRaceDetectorChatPort (Thread-safe stress double)
 * 13. ResilientFailoverChatPort (Endpoint failover)
 * 14. SchemaValidatingChatPort (Structured output conversion & fallback)
 * 15. AutonomousChaosTriageHarness (Full chaos triage lifecycle)
 */
public class AdvancedOfflineHarnessUnderTest {

    // =========================================================================
    // Topic 1: Chaos Injection & Payload Corruption (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 1: Deterministic Seeded Chaos Matrix.
     */
    public static class ChaosInjectingChatPort implements ChatPort {
        public ChaosInjectingChatPort(ChatPort delegate, ChaosMatrix matrix) {
            // DEFECT (Scenario 1): Ignores configuration
        }

        public int getTotalCalls() {
            // DEFECT (Scenario 1): Returns 0
            return 0;
        }

        public int getFailureCount() {
            // DEFECT (Scenario 1): Returns 0
            return 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 1): Bypasses chaos injection
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 2: Payload Truncation & Field Mangle Double.
     */
    public static class CorruptingChaosChatPort implements ChatPort {
        public CorruptingChaosChatPort(ChatPort delegate) {
            // DEFECT (Scenario 2): Ignores delegate
        }

        public CorruptingChaosChatPort truncateAfter(int length) {
            // DEFECT (Scenario 2): No-op
            return this;
        }

        public CorruptingChaosChatPort mangleField(String fieldName, String badValue) {
            // DEFECT (Scenario 2): No-op
            return this;
        }

        public int getCorruptedCallsCount() {
            // DEFECT (Scenario 2): Returns 0
            return 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 2): Returns unmodified dummy result
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 3: Multi-Stage Chaos Pipeline with Auditing.
     */
    public static class CompositeChaosPipeline implements ChatPort {
        public CompositeChaosPipeline(ChatPort delegate, ChaosMatrix matrix) {
            // DEFECT (Scenario 3): Ignores configuration
        }

        public List<ChaosAuditEntry> getAuditTrail() {
            // DEFECT (Scenario 3): Returns empty list
            return Collections.emptyList();
        }

        public int getSevereFaultCount() {
            // DEFECT (Scenario 3): Returns 0
            return 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 3): Bypasses pipeline
            return new ChatResult("", 0, 0);
        }
    }

    // =========================================================================
    // Topic 2: Reactive Streaming Delays & Backpressure (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 4: Reactive Chunk Delay & Backpressure.
     */
    public static class StreamingChunkLatencySimulator implements StreamingChatPort {
        public StreamingChunkLatencySimulator enqueueStream(List<String> chunks, long simulatedLatencyMsPerChunk) {
            // DEFECT (Scenario 4): Doesn't queue streams
            return this;
        }

        public int getTotalChunksEmitted() {
            // DEFECT (Scenario 4): Returns 0
            return 0;
        }

        public long getCumulativeSimulatedLatencyMs() {
            // DEFECT (Scenario 4): Returns 0
            return 0;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            // DEFECT (Scenario 4): Returns empty flux
            return Flux.empty();
        }
    }

    /**
     * Scenario 5: Simulated Token Drops & Resumption Double.
     */
    public static class IntermittentDropStreamingChatPort implements StreamingChatPort {
        public IntermittentDropStreamingChatPort enqueueStreamWithDrops(List<String> chunks, Predicate<String> dropPredicate) {
            // DEFECT (Scenario 5): Doesn't queue stream
            return this;
        }

        public int getDroppedChunksCount() {
            // DEFECT (Scenario 5): Returns 0
            return 0;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            // DEFECT (Scenario 5): Returns empty flux
            return Flux.empty();
        }
    }

    /**
     * Scenario 6: Trajectory Replay & State Verification.
     */
    public static class DeterministicConversationReplayer {
        public static long replay(ConversationTrajectory trajectory, ChatPort agent) {
            // DEFECT (Scenario 6): Returns 0 without replaying trajectory
            return 0L;
        }
    }

    // =========================================================================
    // Topic 3: Rate Limiting & Circuit Breakers (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 7: Rate-Limit Backoff & Virtual Clock Simulator.
     */
    public static class RateLimitBackoffChatPort implements ChatPort {
        public RateLimitBackoffChatPort(ChatPort delegate, int maxRetries, long initialDelayMs, AdvancedTestClock clock) {
            // DEFECT (Scenario 7): Ignores parameters
        }

        public int getRetryAttempts() {
            // DEFECT (Scenario 7): Returns 0
            return 0;
        }

        public int getSuccessfulCalls() {
            // DEFECT (Scenario 7): Returns 0
            return 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 7): Returns empty result without retrying
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 8: Resilient Circuit Breaker State Machine.
     */
    public static class CircuitBreakingChatPort implements ChatPort {
        public CircuitBreakingChatPort(ChatPort delegate, int failureThreshold, long cooldownMs, AdvancedTestClock clock) {
            // DEFECT (Scenario 8): Ignores parameters
        }

        public synchronized CircuitState getState() {
            // DEFECT (Scenario 8): Always returns CLOSED
            return CircuitState.CLOSED;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 8): Bypasses circuit breaker logic
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 9: Sliding Window Adaptive Rate Limiter.
     */
    public static class AdaptiveThrottlingChatPort implements ChatPort {
        public AdaptiveThrottlingChatPort(ChatPort delegate, int maxCallsPerWindow, long windowMs, AdvancedTestClock clock) {
            // DEFECT (Scenario 9): Ignores parameters
        }

        public int getThrottledCallsCount() {
            // DEFECT (Scenario 9): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 9): Bypasses throttling
            return new ChatResult("", 0, 0);
        }
    }

    // =========================================================================
    // Topic 4: Multi-Tenant Quotas & Thread-Safe Concurrency (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Multi-Tenant Token Budget Partitioning.
     */
    public static class MultiTenantBudgetPartitionPort implements MultiTenantChatPort {
        public MultiTenantBudgetPartitionPort(ChatPort delegate) {
            // DEFECT (Scenario 10): Ignores delegate
        }

        public void allocateQuota(String tenantId, long quotaTokens) {
            // DEFECT (Scenario 10): No-op
        }

        public long getRemainingQuota(String tenantId) {
            // DEFECT (Scenario 10): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 10): Returns empty result without quota tracking
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 11: Subscription Tier Quotas & Policies.
     */
    public static class TenantTierThrottlingChatPort implements MultiTenantChatPort {
        public TenantTierThrottlingChatPort(ChatPort delegate) {
            // DEFECT (Scenario 11): Ignores delegate
        }

        public void assignTier(String tenantId, TenantTier tier) {
            // DEFECT (Scenario 11): No-op
        }

        public TenantUsage getTenantUsage(String tenantId) {
            // DEFECT (Scenario 11): Returns empty usage
            return new TenantUsage(0, 0);
        }

        @Override
        public synchronized ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 11): Returns dummy result without tier checking
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 12: Concurrency Race Detector Stress Double.
     */
    public static class ConcurrencyRaceDetectorChatPort implements ChatPort {
        public ConcurrencyRaceDetectorChatPort(ChatPort delegate) {
            // DEFECT (Scenario 12): Ignores delegate
        }

        public int getCallCount() {
            // DEFECT (Scenario 12): Returns 0
            return 0;
        }

        public long getTotalTokens() {
            // DEFECT (Scenario 12): Returns 0
            return 0;
        }

        public List<ChatResult> getAuditHistory() {
            // DEFECT (Scenario 12): Returns empty list
            return Collections.emptyList();
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 12): Returns empty result
            return new ChatResult("", 0, 0);
        }
    }

    // =========================================================================
    // Topic 5: Resilient Failover, Schema Conversion & Autonomous Loops (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Primary Endpoint Failure to Secondary Fallback.
     */
    public static class ResilientFailoverChatPort implements ChatPort {
        public ResilientFailoverChatPort(ChatPort primary, ChatPort secondary) {
            // DEFECT (Scenario 13): Ignores delegates
        }

        public boolean isFailoverTriggered() {
            // DEFECT (Scenario 13): Returns false
            return false;
        }

        public int getFailoverCount() {
            // DEFECT (Scenario 13): Returns 0
            return 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 13): Returns empty result without failover
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 14: Structured Output Validation with Fallback.
     */
    public static class SchemaValidatingChatPort {
        public SchemaValidatingChatPort(ChatPort delegate) {
            // DEFECT (Scenario 14): Ignores delegate
        }

        public int getValidationSuccessCount() {
            // DEFECT (Scenario 14): Returns 0
            return 0;
        }

        public int getValidationFailureCount() {
            // DEFECT (Scenario 14): Returns 0
            return 0;
        }

        public <T> T chatAndValidate(String system, String user, BeanOutputConverter<T> converter, T fallback) {
            // DEFECT (Scenario 14): Always returns null
            return null;
        }
    }

    /**
     * Scenario 15: Autonomous Multi-Turn Chaos Triage Lifecycle.
     */
    public static class AutonomousChaosTriageHarness {
        public static AdvancedDiagnosticReport runChaosTriage(
                ChatPort primary,
                ChatPort secondary,
                String tenantId,
                String incidentId,
                AdvancedTestClock clock
        ) {
            // DEFECT (Scenario 15): Returns null without running triage loop
            return null;
        }
    }
}
