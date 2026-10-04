package phase05;

import org.springframework.ai.chat.client.ChatClient;
import phase05.HarnessContracts.ChatPort;
import phase05.HarnessContracts.ChatResult;
import phase05.HarnessContracts.DiagnosticTriageReport;
import phase05.HarnessContracts.PromptCapture;
import phase05.HarnessContracts.StreamingChatPort;
import phase05.HarnessContracts.TestClock;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice implementing deterministic offline test harness seams, scripted test doubles,
 * outgoing prompt captors, and virtual time simulators:
 * 1. ScriptedChatPort (FIFO scripted double)
 * 2. PredicateRoutingChatPort (Dynamic pattern matcher)
 * 3. FaultInjectingChatPort (Simulated faults & retries)
 * 4. PromptCapturingChatPort (Outgoing prompt spy)
 * 5. BudgetEnforcingChatPort (Token ceiling guardrail)
 * 6. ToolRegistrySpy (Reflection discovery)
 * 7. InMemoryStreamingChatPort (Reactive Flux double)
 * 8. VirtualTimeSimulator (Deterministic clock seam)
 * 9. SpringAiChatPort (Spring AI ChatClient adapter)
 * 10. DeterministicDiagnosticLoop (Multi-turn triage loop)
 */
public class OfflineHarnessUnderTest {

    /**
     * Scenario 1: FIFO Scripted Response Test Double.
     * <p>
     * Instructions:
     * - Maintain an internal FIFO Deque<ChatResult> of scripted responses.
     * - enqueue(text, tokensIn, tokensOut): adds a new ChatResult to the queue.
     * - callCount(): returns the total number of chat() calls made.
     * - chat(system, user, toolBeans):
     *     - increments callCount.
     *     - if queue is empty, throws IllegalStateException("FakeChatPort: no more scripted responses (call " + callCount + ")").
     *     - polls and returns the next ChatResult.
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
            // DEFECT (Scenario 1): Always returns empty dummy result without incrementing callCount
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 2: Dynamic Pattern Matching Test Double.
     * <p>
     * Instructions:
     * - Maintain registered routes: list of (Predicate<String> matcher, ChatResult result).
     * - when(matcher, result): registers a route rule.
     * - setDefaultResult(result): sets fallback response if no predicate matches.
     * - chat(system, user, toolBeans):
     *     - iterates over routes in insertion order.
     *     - if matcher.test(user) is true, returns that route's ChatResult.
     *     - if no route matches, returns defaultResult.
     */
    public static class PredicateRoutingChatPort implements ChatPort {
        public record Route(Predicate<String> matcher, ChatResult result) {}

        public PredicateRoutingChatPort when(Predicate<String> matcher, ChatResult result) {
            // DEFECT (Scenario 2): Ignores route registration
            return this;
        }

        public PredicateRoutingChatPort setDefaultResult(ChatResult result) {
            // DEFECT (Scenario 2): Ignores default result setting
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 2): Ignores matching and returns stub
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 3: Simulated Fault & Retry Test Double.
     * <p>
     * Instructions:
     * - Supports queuing both normal results and simulated exceptions.
     * - enqueueResult(text, tokensIn, tokensOut): queues a successful ChatResult.
     * - enqueueFault(RuntimeException ex): queues an exception to throw.
     * - callCount(): returns total calls made.
     * - chat(system, user, toolBeans):
     *     - increments callCount.
     *     - if queue is empty, throws IllegalStateException("FaultInjectingChatPort: queue empty").
     *     - polls next action:
     *         - if Result: returns ChatResult.
     *         - if Fault: throws the RuntimeException.
     */
    public static class FaultInjectingChatPort implements ChatPort {
        private int callCount = 0;

        public FaultInjectingChatPort enqueueResult(String text, long tokensIn, long tokensOut) {
            // DEFECT (Scenario 3): Doesn't queue results
            return this;
        }

        public FaultInjectingChatPort enqueueFault(RuntimeException ex) {
            // DEFECT (Scenario 3): Doesn't queue faults
            return this;
        }

        public int callCount() {
            return callCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 3): Bypasses fault injection
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 4: Outgoing Prompt Spy & Inspection Seam.
     * <p>
     * Instructions:
     * - Wraps an underlying delegate ChatPort.
     * - Intercepts chat(system, user, toolBeans) and records new PromptCapture(system, user, toolBeans, System.currentTimeMillis()).
     * - getCapturedPrompts(): unmodifiable list of captured prompts.
     * - getLastPrompt(): last captured prompt or null if empty.
     * - getCallCount(): number of captured prompts.
     * - clear(): clears recorded prompts.
     */
    public static class PromptCapturingChatPort implements ChatPort {
        private final ChatPort delegate;

        public PromptCapturingChatPort(ChatPort delegate) {
            this.delegate = delegate;
        }

        public List<PromptCapture> getCapturedPrompts() {
            // DEFECT (Scenario 4): Returns empty list
            return Collections.emptyList();
        }

        public PromptCapture getLastPrompt() {
            // DEFECT (Scenario 4): Returns null
            return null;
        }

        public int getCallCount() {
            // DEFECT (Scenario 4): Returns 0
            return 0;
        }

        public void clear() {
            // no-op
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 4): Doesn't capture prompts
            return delegate.chat(system, user, toolBeans);
        }
    }

    /**
     * Scenario 5: Test Spend Ceiling Guardrail.
     * <p>
     * Instructions:
     * - Wraps a delegate ChatPort with a maxTokensAllowed budget.
     * - Tracks cumulative totalTokensUsed.
     * - In chat():
     *     - dispatches to delegate.chat().
     *     - accumulates result.totalTokens() into totalTokensUsed.
     *     - if totalTokensUsed > maxTokensAllowed:
     *         throw new HarnessBudgetExceededException("Offline token budget exceeded: used " + totalTokensUsed + " > max " + maxTokensAllowed);
     *         (Message MUST contain "Offline token budget exceeded").
     *     - returns result.
     * - getTotalTokensUsed(): returns cumulative tokens used.
     */
    public static class BudgetEnforcingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final long maxTokensAllowed;

        public BudgetEnforcingChatPort(ChatPort delegate, long maxTokensAllowed) {
            this.delegate = delegate;
            this.maxTokensAllowed = maxTokensAllowed;
        }

        public synchronized long getTotalTokensUsed() {
            // DEFECT (Scenario 5): Returns 0
            return 0;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 5): Bypasses budget enforcement
            return delegate.chat(system, user, toolBeans);
        }
    }

    /**
     * Scenario 6: Tool Bean Reflection & Discovery Seam.
     * <p>
     * Instructions:
     * - extractToolNames(List<Object> toolBeans):
     *     - inspects each object in toolBeans.
     *     - finds public methods annotated with @Tool.
     *     - if tool.name() is non-blank, uses that; otherwise uses method.getName().
     *     - returns list of discovered tool names.
     */
    public static class ToolRegistrySpy {
        public static List<String> extractToolNames(List<Object> toolBeans) {
            // DEFECT (Scenario 6): Returns empty list without reflection
            return Collections.emptyList();
        }
    }

    /**
     * Scenario 7: Deterministic Reactive Streaming Test Double.
     * <p>
     * Instructions:
     * - Implements StreamingChatPort.
     * - enqueueStream(List<String> chunks): queues a list of token chunks for the next stream.
     * - stream(system, user):
     *     - polls next chunk list (or empty list if none queued).
     *     - returns Flux.fromIterable(chunks).
     */
    public static class InMemoryStreamingChatPort implements StreamingChatPort {
        public InMemoryStreamingChatPort enqueueStream(List<String> chunks) {
            // DEFECT (Scenario 7): Doesn't queue stream chunks
            return this;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            // DEFECT (Scenario 7): Returns empty flux
            return Flux.empty();
        }
    }

    /**
     * Scenario 8: Deterministic Time Advancement Seam.
     * <p>
     * Instructions:
     * - measureSimulatedExecution(TestClock clock, Runnable action, long simulatedStepMs):
     *     - captures start = clock.now().
     *     - runs action.run().
     *     - advances clock.advance(simulatedStepMs).
     *     - returns clock.now() - start.
     */
    public static class VirtualTimeSimulator {
        public static long measureSimulatedExecution(TestClock clock, Runnable action, long simulatedStepMs) {
            // DEFECT (Scenario 8): Doesn't advance clock or measure delta
            return 0L;
        }
    }

    /**
     * Scenario 9: Spring AI ChatClient Framework Adapter.
     * <p>
     * Instructions:
     * - Implements ChatPort by wrapping ChatClient.
     * - In chat(system, user, toolBeans):
     *     - dispatches chatClient.prompt().system(system).user(user)
     *     - attaches tools if toolBeans is non-empty: .tools(toolBeans.toArray())
     *     - executes .call().chatResponse()
     *     - safely extracts response text and usage tokens (defending against null metadata/tokens).
     *     - returns new ChatResult(text, tokensIn, tokensOut).
     */
    public static class SpringAiChatPort implements ChatPort {
        private final ChatClient chatClient;

        public SpringAiChatPort(ChatClient chatClient) {
            this.chatClient = chatClient;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            // DEFECT (Scenario 9): Returns dummy response without invoking chatClient
            return new ChatResult("", 0, 0);
        }
    }

    /**
     * Scenario 10: Multi-Turn Autonomous Diagnostic Triage Loop.
     * <p>
     * Instructions:
     * - Executes a 3-turn diagnostic loop against chatPort:
     *     - Turn 1: chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "INCIDENT: " + incident)
     *     - Turn 2: chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "EVIDENCE: Log query for target completed. Found 13 OOM errors.")
     *     - Turn 3: chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "MITIGATION_STATUS: Pod restarted successfully. Please summarize root cause and recommendation.")
     * - Accumulates total tokens across all 3 turns.
     * - Extracts root cause and recommendation from Turn 3 output text.
     * - Returns DiagnosticTriageReport(incident, rootCause, recommendation, 3, totalTokens).
     */
    public static class DeterministicDiagnosticLoop {
        public static DiagnosticTriageReport runTriage(ChatPort chatPort, String incident) {
            // DEFECT (Scenario 10): Returns null without running triage loop
            return null;
        }
    }
}
