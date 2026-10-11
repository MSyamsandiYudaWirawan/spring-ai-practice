package phase05;

import org.springframework.ai.chat.client.ChatClient;
import phase05.HarnessContracts.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice implementing deterministic offline test harness seams, scripted test doubles,
 * outgoing prompt captors, and virtual time simulators across 15 repetitive drills:
 * 1. ScriptedChatPort (FIFO scripted double)
 * 2. IntrospectableScriptedChatPort (Queue introspection & reset)
 * 3. SequentialDialogueReplayer (Multi-turn scripted replay seam)
 * 4. PredicateRoutingChatPort (Dynamic pattern matcher)
 * 5. BiPredicateRoutingChatPort (Dual-predicate system + user matcher)
 * 6. RegexTemplateRoutingChatPort (Parameterized dynamic response generator)
 * 7. FaultInjectingChatPort (Simulated faults & retries)
 * 8. ConsecutiveFaultChatPort (K-failure fault burst generator)
 * 9. ExponentialBackoffRetrySimulator (Virtual time retry loop)
 * 10. PromptCapturingChatPort (Outgoing prompt spy)
 * 11. BudgetEnforcingChatPort (Token ceiling guardrail)
 * 12. AuditingCompositeChatPort (Full audit trail decorator)
 * 13. ToolRegistrySpy (Reflection discovery)
 * 14. InMemoryStreamingChatPort (Reactive Flux double)
 * 15. SpringAiChatPort & DeterministicDiagnosticLoop (Multi-turn triage loop)
 */
public class OfflineHarnessUnderTest {

    // =========================================================================
    // Topic 1: Scripted FIFO & Inspection Doubles (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 1: FIFO Scripted Response Test Double.
     */
    public static class ScriptedChatPort implements ChatPort {
        private int callCount = 0;

        public ScriptedChatPort enqueue(String text, long tokensIn, long tokensOut) {
            // DEFECT (Scenario 1): Doesn't enqueue responses
            return this;
        }

        public int callCount() {
            return callCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 1): Always returns dummy result without incrementing callCount
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 2: Introspectable Scripted Chat Port with Reset.
     */
    public static class IntrospectableScriptedChatPort implements ChatPort {
        public IntrospectableScriptedChatPort enqueue(String text, long tokensIn, long tokensOut) {
            // DEFECT (Scenario 2): Doesn't enqueue
            return this;
        }

        public int remaining() {
            // DEFECT (Scenario 2): Returns 0
            return 0;
        }

        public boolean hasMore() {
            // DEFECT (Scenario 2): Returns false
            return false;
        }

        public ChatResult peek() {
            // DEFECT (Scenario 2): Returns null
            return null;
        }

        public int callCount() {
            // DEFECT (Scenario 2): Returns 0
            return 0;
        }

        public void clear() {
            // DEFECT (Scenario 2): No-op
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 2): Returns dummy result
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 3: Sequential Multi-Turn Dialogue Replayer.
     */
    public static class SequentialDialogueReplayer implements ChatPort {
        public SequentialDialogueReplayer(List<DialogueTurn> script) {
            // DEFECT (Scenario 3): Ignores script
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 3): Doesn't validate prompt substring or advance turn
            return new ChatResult("", 0, 0);
        }

        public ReplayValidation validateCompletion() {
            // DEFECT (Scenario 3): Always reports false
            return new ReplayValidation(false, 0, "none");
        }
    }

    // =========================================================================
    // Topic 2: Dynamic Pattern & Predicate Routing Doubles (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 4: Dynamic Predicate Routing Double.
     */
    public static class PredicateRoutingChatPort implements ChatPort {
        public record Route(Predicate<String> matcher, ChatResult result) {}

        public PredicateRoutingChatPort when(Predicate<String> matcher, ChatResult result) {
            // DEFECT (Scenario 4): Ignores routes
            return this;
        }

        public PredicateRoutingChatPort setDefaultResult(ChatResult result) {
            // DEFECT (Scenario 4): Ignores default result
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 4): Returns empty stub
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 5: System & User Dual-Predicate Matcher.
     */
    public static class BiPredicateRoutingChatPort implements ChatPort {
        public record BiRoute(BiPredicate<String, String> matcher, ChatResult result) {}

        public BiPredicateRoutingChatPort when(BiPredicate<String, String> matcher, ChatResult result) {
            // DEFECT (Scenario 5): Ignores routes
            return this;
        }

        public BiPredicateRoutingChatPort setDefaultResult(ChatResult result) {
            // DEFECT (Scenario 5): Ignores default result
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 5): Returns empty stub
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 6: Parameterized Dynamic Regex Template Double.
     */
    public static class RegexTemplateRoutingChatPort implements ChatPort {
        public RegexTemplateRoutingChatPort whenRegex(String regex, String template) {
            // DEFECT (Scenario 6): Ignores regex patterns
            return this;
        }

        public RegexTemplateRoutingChatPort setDefaultTemplate(String template) {
            // DEFECT (Scenario 6): Ignores default template
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 6): Returns empty stub without template interpolation
            return new ChatResult("", 0, 0);
        }
    }

    // =========================================================================
    // Topic 3: Fault Injection & Resilience Testing (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 7: Simulated Transient Fault & Retry Double.
     */
    public static class FaultInjectingChatPort implements ChatPort {
        private int callCount = 0;

        public FaultInjectingChatPort enqueueResult(String text, long tokensIn, long tokensOut) {
            // DEFECT (Scenario 7): Doesn't queue results
            return this;
        }

        public FaultInjectingChatPort enqueueFault(RuntimeException ex) {
            // DEFECT (Scenario 7): Doesn't queue faults
            return this;
        }

        public int callCount() {
            return callCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 7): Bypasses fault injection
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 8: Consecutive Fault Burst Generator.
     */
    public static class ConsecutiveFaultChatPort implements ChatPort {
        public ConsecutiveFaultChatPort failNext(int failureCount, RuntimeException faultToThrow, ChatResult recoveryResult) {
            // DEFECT (Scenario 8): Ignores fault configuration
            return this;
        }

        public int consecutiveFailuresRemaining() {
            // DEFECT (Scenario 8): Returns 0
            return 0;
        }

        public int totalAttempts() {
            // DEFECT (Scenario 8): Returns 0
            return 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 8): Returns empty result instead of throwing configured faults
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 9: Deterministic Exponential Backoff Retry Simulator.
     */
    public static class ExponentialBackoffRetrySimulator {
        public static RetrySimulationReport simulateRetry(ChatPort port, String userPrompt, int maxRetries, long initialDelayMs, TestClock clock) {
            // DEFECT (Scenario 9): Returns dummy failed report without retrying or advancing clock
            return new RetrySimulationReport(0, 0, false, null);
        }
    }

    // =========================================================================
    // Topic 4: Observation, Auditing & Budget Ceilings (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Outgoing Prompt Spy & Inspection Seam.
     */
    public static class PromptCapturingChatPort implements ChatPort {
        private final ChatPort delegate;

        public PromptCapturingChatPort(ChatPort delegate) {
            this.delegate = delegate;
        }

        public List<PromptCapture> getCapturedPrompts() {
            // DEFECT (Scenario 10): Returns empty list
            return Collections.emptyList();
        }

        public PromptCapture getLastPrompt() {
            // DEFECT (Scenario 10): Returns null
            return null;
        }

        public int getCallCount() {
            // DEFECT (Scenario 10): Returns 0
            return 0;
        }

        public void clear() {
            // no-op
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 10): Doesn't capture prompts
            return delegate.chat(system, user, toolBeans);
        }
    }

    /**
     * Scenario 11: Test Spend Ceiling Guardrail.
     */
    public static class BudgetEnforcingChatPort implements ChatPort {
        private final ChatPort delegate;

        public BudgetEnforcingChatPort(ChatPort delegate, long maxTokensAllowed) {
            this.delegate = delegate;
        }

        public synchronized long getTotalTokensUsed() {
            // DEFECT (Scenario 11): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 11): Bypasses budget enforcement
            return delegate.chat(system, user, toolBeans);
        }
    }

    /**
     * Scenario 12: Full Audit Trail Decorator.
     */
    public static class AuditingCompositeChatPort implements ChatPort {
        private final ChatPort delegate;

        public AuditingCompositeChatPort(ChatPort delegate) {
            this.delegate = delegate;
        }

        public synchronized List<AuditLogEntry> getAuditLog() {
            // DEFECT (Scenario 12): Returns empty log
            return Collections.emptyList();
        }

        public synchronized AuditLogEntry peakTokenCall() {
            // DEFECT (Scenario 12): Returns null
            return null;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 12): Delegates without auditing
            return delegate.chat(system, user, toolBeans);
        }
    }

    // =========================================================================
    // Topic 5: Framework Adapters, Streaming & Autonomous Loops (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Tool Bean Reflection & Discovery Seam.
     */
    public static class ToolRegistrySpy {
        public static List<String> extractToolNames(List<Object> toolBeans) {
            // DEFECT (Scenario 13): Returns empty list without reflection
            return Collections.emptyList();
        }
    }

    /**
     * Scenario 14: Deterministic Reactive Streaming Test Double.
     */
    public static class InMemoryStreamingChatPort implements StreamingChatPort {
        public InMemoryStreamingChatPort enqueueStream(List<String> chunks) {
            // DEFECT (Scenario 14): Doesn't queue stream chunks
            return this;
        }

        public int streamCallCount() {
            // DEFECT (Scenario 14): Returns 0
            return 0;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            // DEFECT (Scenario 14): Returns empty flux
            return Flux.empty();
        }
    }

    /**
     * Scenario 15a: Spring AI ChatClient Framework Adapter.
     */
    public static class SpringAiChatPort implements ChatPort {
        public SpringAiChatPort(ChatClient chatClient) {
            // DEFECT (Scenario 15): Ignores chatClient
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 15): Returns dummy response without invoking chatClient
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 15b: Multi-Turn Autonomous Diagnostic Triage Loop.
     */
    public static class DeterministicDiagnosticLoop {
        public static DiagnosticTriageReport runTriage(ChatPort chatPort, String incident) {
            // DEFECT (Scenario 15): Returns null without running triage loop
            return null;
        }
    }
}
