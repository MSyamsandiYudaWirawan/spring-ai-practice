package phase05;

import org.springframework.ai.converter.BeanOutputConverter;
import phase05.AdvancedHarnessContracts.AdvancedDiagnosticReport;
import phase05.AdvancedHarnessContracts.AdvancedTestClock;
import phase05.AdvancedHarnessContracts.ChaosMatrix;
import phase05.AdvancedHarnessContracts.ChatPort;
import phase05.AdvancedHarnessContracts.ChatResult;
import phase05.AdvancedHarnessContracts.CircuitState;
import phase05.AdvancedHarnessContracts.ConversationTrajectory;
import phase05.AdvancedHarnessContracts.MultiTenantChatPort;
import phase05.AdvancedHarnessContracts.StreamingChatPort;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * Service under test — THE ONLY FILE YOU MODIFY for Phase 05 Exercise 02.
 * <p>
 * Practice advanced deterministic offline test doubles:
 * 1. ChaosInjectingChatPort (Deterministic chaos matrix)
 * 2. StreamingChunkLatencySimulator (Reactive chunk delay simulator)
 * 3. RateLimitBackoffChatPort (Simulated rate-limit & backoff)
 * 4. DeterministicConversationReplayer (Trajectory replay)
 * 5. CircuitBreakingChatPort (State machine double)
 * 6. MultiTenantBudgetPartitionPort (Tenant quota manager)
 * 7. ConcurrencyRaceDetectorChatPort (Thread-safe stress double)
 * 8. SchemaValidatingChatPort (Schema-validating structured output double)
 * 9. ResilientFailoverChatPort (HTTP endpoint failover)
 * 10. AutonomousChaosTriageHarness (Full chaos triage lifecycle)
 */
public class AdvancedOfflineHarnessUnderTest {

    /**
     * Scenario 1: Deterministic Seeded Chaos Matrix.
     * <p>
     * Instructions:
     * - Dispatches to delegate.chat().
     * - Probabilistically drops calls: if matrix.random().nextDouble() < matrix.failureRate(),
     *     increments failureCount and throws new RuntimeException("ChaosInjectingChatPort: simulated chaos drop").
     * - If matrix.corruptPayload(), truncates the resulting text in half:
     *     result.text().substring(0, Math.max(1, result.text().length() / 2)).
     * - Tracks totalCalls and failureCount.
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
            // DEFECT (Scenario 1): Returns 0 without tracking calls
            return 0;
        }

        public synchronized int getFailureCount() {
            // DEFECT (Scenario 1): Returns 0 without tracking failures
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 1): Bypasses chaos matrix evaluation and returns delegate result directly
            return delegate.chat(system, user, toolBeans);
        }
    }

    /**
     * Scenario 2: Reactive Chunk Delay & Backpressure.
     * <p>
     * Instructions:
     * - enqueueStream(chunks, stepLatencyMs): queues chunks and the step latency for next stream call.
     * - stream(system, user):
     *     - returns Flux.fromIterable(chunks)
     *     - on each chunk emission (e.g. .doOnNext(...)), increments totalChunksEmitted
     *       and accumulates stepLatency into cumulativeSimulatedLatencyMs.
     */
    public static class StreamingChunkLatencySimulator implements StreamingChatPort {
        public synchronized StreamingChunkLatencySimulator enqueueStream(List<String> chunks, long stepLatencyMs) {
            // DEFECT (Scenario 2): Doesn't queue chunks or latency
            return this;
        }

        public synchronized int getTotalChunksEmitted() {
            // DEFECT (Scenario 2): Returns 0
            return 0;
        }

        public synchronized long getCumulativeSimulatedLatencyMs() {
            // DEFECT (Scenario 2): Returns 0
            return 0;
        }

        @Override
        public synchronized Flux<String> stream(String system, String user) {
            // DEFECT (Scenario 2): Returns empty flux
            return Flux.empty();
        }
    }

    /**
     * Scenario 3: Simulated Rate-Limit & Virtual Backoff Double.
     * <p>
     * Instructions:
     * - Wraps a delegate ChatPort.
     * - Configured with maxRetries, initialBackoffMs, and an AdvancedTestClock.
     * - In chat(system, user, toolBeans):
     *     - Loops from attempt = 1 to maxRetries.
     *     - If delegate succeeds: increments successfulCalls and returns ChatResult.
     *     - If delegate throws an exception:
     *         - Checks if exception message contains "429" or "rate limit" (case-insensitive).
     *         - If NOT rate-limit related: rethrow immediately without retrying (fail-fast).
     *         - If rate-limit related:
     *             - Increments retryAttempts.
     *             - If attempt >= maxRetries: throw new RateLimitExhaustedException("Rate limit retries exhausted after " + maxRetries + " attempts").
     *               (Message MUST contain "Rate limit retries exhausted").
     *             - Otherwise advances clock by backoff:
     *                 long backoff = initialBackoffMs * (1L << (attempt - 1));
     *                 clock.advance(backoff);
     * - getRetryAttempts(): returns total retries across calls.
     * - getSuccessfulCalls(): returns total successful calls completed.
     */
    public static class RateLimitBackoffChatPort implements ChatPort {
        public RateLimitBackoffChatPort(ChatPort delegate, int maxRetries, long initialBackoffMs, AdvancedTestClock clock) {
            // no-op
        }

        public synchronized int getRetryAttempts() {
            // DEFECT (Scenario 3): Returns 0
            return 0;
        }

        public synchronized int getSuccessfulCalls() {
            // DEFECT (Scenario 3): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 4: Trajectory Replay & State Verification.
     * <p>
     * Instructions:
     * - replay(trajectory, agentPort):
     *     - Iterates through trajectory.turns().
     *     - Calls agentPort.chat("SYSTEM: Conversation Replay", turn.userPrompt()).
     *     - Asserts that agent response text contains turn.expectedResponse().
     *       If not, throws IllegalStateException("Replay verification failed: expected [" +
     *       turn.expectedResponse() + "], got [" + result.text() + "]").
     *     - Accumulates total tokens across all turns.
     *     - Returns total tokens.
     */
    public static class DeterministicConversationReplayer {
        public static long replay(ConversationTrajectory trajectory, ChatPort agentPort) {
            // DEFECT (Scenario 4): Returns 0 without replaying turns
            return 0L;
        }
    }

    /**
     * Scenario 5: Resilient State Machine Double.
     * <p>
     * Instructions:
     * - Implements circuit breaker: CLOSED -> OPEN -> HALF_OPEN -> CLOSED.
     * - During OPEN state:
     *     - If clock.now() - lastTrippedTimestamp >= cooldownMillis, transitions to HALF_OPEN.
     *     - Otherwise throws new CircuitBreakerOpenException("Circuit breaker is OPEN (failures: " + consecutiveFailures + ")").
     *       (Message MUST contain "Circuit breaker is OPEN").
     * - On call success:
     *     - If HALF_OPEN, transitions to CLOSED.
     *     - Resets consecutiveFailures = 0.
     * - On call failure:
     *     - Increments consecutiveFailures.
     *     - If consecutiveFailures >= failureThreshold or state was HALF_OPEN,
     *       transitions to OPEN and records lastTrippedTimestamp = clock.now().
     *     - Rethrows the exception.
     */
    public static class CircuitBreakingChatPort implements ChatPort {
        public CircuitBreakingChatPort(ChatPort delegate, int failureThreshold, long cooldownMillis, AdvancedTestClock clock) {
            // no-op
        }

        public synchronized CircuitState getState() {
            // DEFECT (Scenario 5): Always reports OPEN
            return CircuitState.OPEN;
        }

        public synchronized int getConsecutiveFailures() {
            return 0;
        }

        public synchronized void reset() {
            // no-op
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 6: Hierarchical Token Quota Manager.
     * <p>
     * Instructions:
     * - allocateQuota(tenantId, maxTokens): sets token quota for tenant.
     * - getRemainingQuota(tenantId): returns remaining tokens.
     * - chat(tenantId, system, user, toolBeans):
     *     - Checks remaining quota for tenantId. If missing or <= 0,
     *       throws new TenantQuotaExceededException("Quota exceeded for tenant: " + tenantId).
     *       (Message MUST contain "Quota exceeded for tenant").
     *     - Dispatches call to delegate.
     *     - Deducts result.totalTokens() from tenant quota.
     *     - If resulting quota is negative, clamps remaining to 0 and throws TenantQuotaExceededException.
     *     - Returns result.
     */
    public static class MultiTenantBudgetPartitionPort implements MultiTenantChatPort {
        public MultiTenantBudgetPartitionPort(ChatPort delegate) {
            // no-op
        }

        public void allocateQuota(String tenantId, long maxTokens) {
            // DEFECT (Scenario 6): Ignores allocation
        }

        public long getRemainingQuota(String tenantId) {
            // DEFECT (Scenario 6): Always returns 0
            return 0L;
        }

        @Override
        public synchronized ChatResult chat(String tenantId, String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 6): Bypasses multi-tenant quota checks
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 7: Thread-Safe Stress Double.
     * <p>
     * Instructions:
     * - Wraps a delegate ChatPort.
     * - Uses thread-safe primitives (AtomicInteger for callCount, AtomicLong for totalTokens,
     *   CopyOnWriteArrayList for auditHistory).
     * - In chat():
     *     - increments callCount.
     *     - calls delegate.
     *     - adds result.totalTokens() to totalTokens.
     *     - appends result to auditHistory.
     *     - returns result.
     */
    public static class ConcurrencyRaceDetectorChatPort implements ChatPort {
        public ConcurrencyRaceDetectorChatPort(ChatPort delegate) {
            // no-op
        }

        public int getCallCount() {
            // DEFECT (Scenario 7): Returns 0
            return 0;
        }

        public long getTotalTokens() {
            // DEFECT (Scenario 7): Returns 0
            return 0L;
        }

        public List<ChatResult> getAuditHistory() {
            // DEFECT (Scenario 7): Returns empty list
            return Collections.emptyList();
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 7): Doesn't track concurrent calls
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 8: Schema-Validating Structured Output Double.
     * <p>
     * Instructions:
     * - Wraps a delegate ChatPort.
     * - chatAndValidate(system, user, BeanOutputConverter<T> converter, T fallback):
     *     - Calls delegate.chat(system, user).
     *     - Converts response text using converter.convert(result.text()).
     *     - If conversion succeeds: increments validationSuccessCount and returns parsed object.
     *     - If conversion throws RuntimeException (e.g. malformed/truncated payload):
     *       increments validationFailureCount and returns fallback.
     * - getValidationSuccessCount(): returns total successful validations.
     * - getValidationFailureCount(): returns total validation failures handled.
     */
    public static class SchemaValidatingChatPort implements ChatPort {
        public SchemaValidatingChatPort(ChatPort delegate) {
            // no-op
        }

        public synchronized int getValidationSuccessCount() {
            // DEFECT (Scenario 8): Returns 0
            return 0;
        }

        public synchronized int getValidationFailureCount() {
            // DEFECT (Scenario 8): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            return new ChatResult("", 0, 0);
        }

        public synchronized <T> T chatAndValidate(String system, String user, BeanOutputConverter<T> converter, T fallback) {
            // DEFECT (Scenario 8): Returns null without validation
            return null;
        }
    }

    /**
     * Scenario 9: Primary HTTP Failure -> Fallback Stub.
     * <p>
     * Instructions:
     * - Wraps primaryPort and secondaryPort.
     * - In chat():
     *     - attempts primaryPort.chat().
     *     - if primaryPort throws any RuntimeException:
     *         - sets failoverTriggered = true
     *         - increments failoverCount
     *         - returns secondaryPort.chat()
     * - isFailoverTriggered(): returns failover status.
     * - getFailoverCount(): returns count of failovers triggered.
     */
    public static class ResilientFailoverChatPort implements ChatPort {
        public ResilientFailoverChatPort(ChatPort primaryPort, ChatPort secondaryPort) {
            // no-op
        }

        public synchronized boolean isFailoverTriggered() {
            // DEFECT (Scenario 9): Returns false
            return false;
        }

        public synchronized int getFailoverCount() {
            // DEFECT (Scenario 9): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 9): Dispatches to primary without failover catch
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 10: Full Chaos Integration & Triage Lifecycle.
     * <p>
     * Instructions:
     * - Assembles the resilient chaos harness:
     *     - ResilientFailoverChatPort(primary, secondary)
     *     - CircuitBreakingChatPort(recoveryStack, 3, 5000L, clock)
     *     - MultiTenantBudgetPartitionPort(circuitBreaker) with quota 5000 allocated for tenantId.
     * - Executes 3-turn triage loop:
     *     - Turn 1: chat(tenantId, "SYSTEM: Chaos Triage", "TURN 1: Initial alert check for " + incidentId)
     *     - Turn 2: chat(tenantId, "SYSTEM: Chaos Triage", "TURN 2: Querying error telemetry")
     *     - Turn 3: chat(tenantId, "SYSTEM: Chaos Triage", "TURN 3: Final diagnosis completion")
     * - Extracts root cause from Turn 3 output.
     * - Returns AdvancedDiagnosticReport(incidentId, tenantId, rootCause, 3, totalTokens, circuitTripped, failoverUsed).
     */
    public static class AutonomousChaosTriageHarness {
        public static AdvancedDiagnosticReport runChaosTriage(
                ChatPort primary,
                ChatPort secondary,
                String tenantId,
                String incidentId,
                AdvancedTestClock clock
        ) {
            // DEFECT (Scenario 10): Returns null without running triage lifecycle
            return null;
        }
    }
}
