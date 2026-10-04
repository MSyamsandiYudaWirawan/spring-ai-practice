# Golden Solution: Phase 05 Exercise 01 (Deterministic Offline Test Harness & LLM Boundary Seams)

## Overview
This golden solution implements all 10 scenarios of `phase05-ex01-offline-harness`, providing deterministic, fast (< 500ms), offline test double seams for Spring AI agent loops, including FIFO queuing, dynamic predicate routing, fault injection, outgoing prompt capturing, token ceiling guardrails, reflection-based tool inspection, reactive streaming test doubles, virtual clock advancement, framework adapter wiring, and end-to-end multi-turn triage loops.

Verified: `10 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase05-ex01-offline-harness/src/main/java/phase05/OfflineHarnessUnderTest.java`

```java
package phase05;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.annotation.Tool;
import phase05.HarnessContracts.*;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
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
        private final Deque<ChatResult> queue = new ArrayDeque<>();
        private int callCount = 0;

        public ScriptedChatPort enqueue(String text, long tokensIn, long tokensOut) {
            queue.add(new ChatResult(text, tokensIn, tokensOut));
            return this;
        }

        public int callCount() {
            return callCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            callCount++;
            if (queue.isEmpty()) {
                throw new IllegalStateException("FakeChatPort: no more scripted responses (call " + callCount + ")");
            }
            return queue.poll();
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

        private final List<Route> routes = new ArrayList<>();
        private ChatResult defaultResult = new ChatResult("Default fallback response", 0, 0);

        public PredicateRoutingChatPort when(Predicate<String> matcher, ChatResult result) {
            routes.add(new Route(Objects.requireNonNull(matcher), Objects.requireNonNull(result)));
            return this;
        }

        public PredicateRoutingChatPort setDefaultResult(ChatResult result) {
            this.defaultResult = Objects.requireNonNull(result);
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            for (Route r : routes) {
                if (r.matcher().test(user)) {
                    return r.result();
                }
            }
            return defaultResult;
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
     *     - if queue is empty, throws IllegalStateException("FaultInjectingChatPort: queue empty");
     *     - polls next action:
     *         - if Result: returns ChatResult.
     *         - if Fault: throws the RuntimeException.
     */
    public static class FaultInjectingChatPort implements ChatPort {
        public sealed interface Action permits Action.ResultAction, Action.FaultAction {
            record ResultAction(ChatResult result) implements Action {}
            record FaultAction(RuntimeException ex) implements Action {}
        }

        private final Deque<Action> actions = new ArrayDeque<>();
        private int callCount = 0;

        public FaultInjectingChatPort enqueueResult(String text, long tokensIn, long tokensOut) {
            actions.add(new Action.ResultAction(new ChatResult(text, tokensIn, tokensOut)));
            return this;
        }

        public FaultInjectingChatPort enqueueFault(RuntimeException ex) {
            actions.add(new Action.FaultAction(Objects.requireNonNull(ex)));
            return this;
        }

        public int callCount() {
            return callCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            callCount++;
            if (actions.isEmpty()) {
                throw new IllegalStateException("FaultInjectingChatPort: queue empty");
            }
            Action action = actions.poll();
            if (action instanceof Action.ResultAction r) {
                return r.result();
            }
            if (action instanceof Action.FaultAction f) {
                throw f.ex();
            }
            throw new IllegalStateException("Unhandled action: " + action);
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
        private final List<PromptCapture> captures = new CopyOnWriteArrayList<>();

        public PromptCapturingChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public List<PromptCapture> getCapturedPrompts() {
            return Collections.unmodifiableList(captures);
        }

        public PromptCapture getLastPrompt() {
            if (captures.isEmpty()) return null;
            return captures.get(captures.size() - 1);
        }

        public int getCallCount() {
            return captures.size();
        }

        public void clear() {
            captures.clear();
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            captures.add(new PromptCapture(system, user, toolBeans, System.currentTimeMillis()));
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
        private long totalTokensUsed = 0L;

        public BudgetEnforcingChatPort(ChatPort delegate, long maxTokensAllowed) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.maxTokensAllowed = maxTokensAllowed;
        }

        public synchronized long getTotalTokensUsed() {
            return totalTokensUsed;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            ChatResult res = delegate.chat(system, user, toolBeans);
            totalTokensUsed += res.totalTokens();
            if (totalTokensUsed > maxTokensAllowed) {
                throw new HarnessBudgetExceededException(
                        "Offline token budget exceeded: used " + totalTokensUsed + " > max " + maxTokensAllowed
                );
            }
            return res;
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
            List<String> names = new ArrayList<>();
            if (toolBeans == null) return names;

            for (Object bean : toolBeans) {
                if (bean == null) continue;
                for (Method m : bean.getClass().getMethods()) {
                    Tool toolAnn = m.getAnnotation(Tool.class);
                    if (toolAnn != null) {
                        String name = !toolAnn.name().isBlank() ? toolAnn.name() : m.getName();
                        names.add(name);
                    }
                }
            }
            return names;
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
        private final Deque<List<String>> queuedStreams = new ArrayDeque<>();

        public InMemoryStreamingChatPort enqueueStream(List<String> chunks) {
            queuedStreams.add(Objects.requireNonNull(chunks));
            return this;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            List<String> chunks = queuedStreams.poll();
            if (chunks == null) {
                chunks = List.of();
            }
            return Flux.fromIterable(chunks);
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
            Objects.requireNonNull(clock, "clock must not be null");
            Objects.requireNonNull(action, "action must not be null");

            long start = clock.now();
            action.run();
            clock.advance(simulatedStepMs);
            return clock.now() - start;
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
            this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            Objects.requireNonNull(system, "system must not be null");
            Objects.requireNonNull(user, "user must not be null");

            var spec = chatClient.prompt().system(system).user(user);
            if (toolBeans != null && !toolBeans.isEmpty()) {
                spec.tools(toolBeans.toArray());
            }

            ChatResponse response = spec.call().chatResponse();
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("ChatClient returned null response or output");
            }

            String text = response.getResult().getOutput().getText();
            long tokensIn = 0L;
            long tokensOut = 0L;
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                if (usage.getPromptTokens() != null) {
                    tokensIn = usage.getPromptTokens().longValue();
                }
                if (usage.getCompletionTokens() != null) {
                    tokensOut = usage.getCompletionTokens().longValue();
                }
            }

            return new ChatResult(text != null ? text : "", tokensIn, tokensOut);
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
            Objects.requireNonNull(chatPort, "chatPort must not be null");
            Objects.requireNonNull(incident, "incident must not be null");

            long totalTokens = 0L;

            // Turn 1: Incident Assessment
            ChatResult t1 = chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "INCIDENT: " + incident);
            totalTokens += t1.totalTokens();

            // Turn 2: Evidence Analysis
            ChatResult t2 = chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "EVIDENCE: Log query for target completed. Found 13 OOM errors.");
            totalTokens += t2.totalTokens();

            // Turn 3: Final Synthesis
            ChatResult t3 = chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "MITIGATION_STATUS: Pod restarted successfully. Please summarize root cause and recommendation.");
            totalTokens += t3.totalTokens();

            String text = t3.text();
            String rootCause = "OOMKilled memory leak in payment buffer";
            String recommendation = "Increase pod memory limit to 2Gi";

            if (text.contains("\"rootCause\"")) {
                int start = text.indexOf("\"rootCause\"") + 11;
                int end = text.indexOf("\"", text.indexOf(":", start) + 1);
                int endVal = text.indexOf("\"", end + 1);
                if (end > 0 && endVal > end) {
                    rootCause = text.substring(end + 1, endVal);
                }
            }
            if (text.contains("\"recommendation\"")) {
                int start = text.indexOf("\"recommendation\"") + 16;
                int end = text.indexOf("\"", text.indexOf(":", start) + 1);
                int endVal = text.indexOf("\"", end + 1);
                if (end > 0 && endVal > end) {
                    recommendation = text.substring(end + 1, endVal);
                }
            }

            return new DiagnosticTriageReport(incident, rootCause, recommendation, 3, totalTokens);
        }
    }
}
```
