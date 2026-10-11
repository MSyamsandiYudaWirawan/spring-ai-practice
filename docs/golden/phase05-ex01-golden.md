# Golden Solution: Phase 05 Exercise 01 (Deterministic Offline Test Harness & LLM Boundary Seams)

## Overview
This golden solution implements all 15 repetitive drills of `phase05-ex01-offline-harness`, organized into 5 core topics (3 drills each) following the Foundation $\to$ Variation $\to$ Real-World Composition pattern:

1. **Topic 1: Scripted FIFO & Inspection Doubles**
   - Scenario 01: `ScriptedChatPort` (FIFO Response Queue Test Double)
   - Scenario 02: `IntrospectableScriptedChatPort` (Queue Introspection, Peek & Reset)
   - Scenario 03: `SequentialDialogueReplayer` (Multi-Turn Scripted Replay Seam with Prompt Substring Validation)
2. **Topic 2: Dynamic Pattern & Predicate Routing Doubles**
   - Scenario 04: `PredicateRoutingChatPort` (Dynamic Pattern Matching Test Double)
   - Scenario 05: `BiPredicateRoutingChatPort` (System & User Dual-Predicate Matcher)
   - Scenario 06: `RegexTemplateRoutingChatPort` (Parameterized Dynamic Response Generator with Variable Substitution)
3. **Topic 3: Fault Injection & Resilience Testing**
   - Scenario 07: `FaultInjectingChatPort` (Simulated Transient Fault & Retry Double)
   - Scenario 08: `ConsecutiveFaultChatPort` (K-Failure Fault Burst Generator)
   - Scenario 09: `ExponentialBackoffRetrySimulator` (Virtual Time Retry Loop with Exponential Delay)
4. **Topic 4: Observation, Auditing & Budget Ceilings**
   - Scenario 10: `PromptCapturingChatPort` (Outgoing Prompt Spy & Inspection Seam)
   - Scenario 11: `BudgetEnforcingChatPort` (Test Spend Ceiling Guardrail throwing `HarnessBudgetExceededException`)
   - Scenario 12: `AuditingCompositeChatPort` (Full Audit Trail Decorator with Delta and Peak Accounting)
5. **Topic 5: Framework Adapters, Streaming & Autonomous Loops**
   - Scenario 13: `ToolRegistrySpy` (Tool Bean Reflection & Discovery Seam)
   - Scenario 14: `InMemoryStreamingChatPort` (Deterministic Reactive Flux Test Double)
   - Scenario 15: `SpringAiChatPort & DeterministicDiagnosticLoop` (Multi-Turn Autonomous Diagnostic Loop Harness)

Verified: `15 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase05-ex01-offline-harness/src/main/java/phase05/OfflineHarnessUnderTest.java`

```java
package phase05;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import phase05.HarnessContracts.*;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service under test — Phase 05 Exercise 01:
 * Deterministic Offline Test Harness & LLM Boundary Seams (15 Scenarios).
 */
public class OfflineHarnessUnderTest {

    // =========================================================================
    // Topic 1: Scripted FIFO & Inspection Doubles (Scenarios 1 - 3)
    // =========================================================================

    public static class ScriptedChatPort implements ChatPort {
        private final Deque<ChatResult> responses = new ArrayDeque<>();
        private int callCount = 0;

        public ScriptedChatPort enqueue(String text, long tokensIn, long tokensOut) {
            Objects.requireNonNull(text, "text must not be null");
            responses.add(new ChatResult(text, tokensIn, tokensOut));
            return this;
        }

        public int callCount() {
            return callCount;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            callCount++;
            if (responses.isEmpty()) {
                throw new IllegalStateException("FakeChatPort: no more scripted responses (call " + callCount + ")");
            }
            return responses.poll();
        }
    }

    public static class IntrospectableScriptedChatPort implements ChatPort {
        private final Deque<ChatResult> queue = new ArrayDeque<>();
        private int callCount = 0;

        public IntrospectableScriptedChatPort enqueue(String text, long tokensIn, long tokensOut) {
            Objects.requireNonNull(text, "text must not be null");
            queue.add(new ChatResult(text, tokensIn, tokensOut));
            return this;
        }

        public int remaining() {
            return queue.size();
        }

        public boolean hasMore() {
            return !queue.isEmpty();
        }

        public ChatResult peek() {
            return queue.peek();
        }

        public int callCount() {
            return callCount;
        }

        public void clear() {
            queue.clear();
            callCount = 0;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            callCount++;
            if (queue.isEmpty()) {
                throw new IllegalStateException("IntrospectableScriptedChatPort: queue empty");
            }
            return queue.poll();
        }
    }

    public static class SequentialDialogueReplayer implements ChatPort {
        private final List<DialogueTurn> script;
        private int currentTurnIndex = 0;
        private String lastSummary = "none";

        public SequentialDialogueReplayer(List<DialogueTurn> script) {
            this.script = script != null ? List.copyOf(script) : List.of();
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            if (currentTurnIndex >= script.size()) {
                throw new IllegalStateException("Dialogue exhausted at turn " + currentTurnIndex);
            }
            DialogueTurn turn = script.get(currentTurnIndex);
            if (!user.contains(turn.expectedPromptSubstring())) {
                throw new IllegalStateException("Prompt mismatch at turn " + currentTurnIndex +
                        ": expected substring [" + turn.expectedPromptSubstring() + "] but was [" + user + "]");
            }
            lastSummary = turn.expectedPromptSubstring();
            currentTurnIndex++;
            return turn.reply();
        }

        public ReplayValidation validateCompletion() {
            boolean matched = currentTurnIndex == script.size();
            return new ReplayValidation(matched, currentTurnIndex, lastSummary);
        }
    }

    // =========================================================================
    // Topic 2: Dynamic Pattern & Predicate Routing Doubles (Scenarios 4 - 6)
    // =========================================================================

    public static class PredicateRoutingChatPort implements ChatPort {
        public record Route(Predicate<String> matcher, ChatResult result) {}

        private final List<Route> routes = new ArrayList<>();
        private ChatResult defaultResult = new ChatResult("Default fallback response", 10, 10);

        public PredicateRoutingChatPort when(Predicate<String> matcher, ChatResult result) {
            Objects.requireNonNull(matcher, "matcher must not be null");
            Objects.requireNonNull(result, "result must not be null");
            routes.add(new Route(matcher, result));
            return this;
        }

        public PredicateRoutingChatPort setDefaultResult(ChatResult result) {
            this.defaultResult = Objects.requireNonNull(result, "defaultResult must not be null");
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

    public static class BiPredicateRoutingChatPort implements ChatPort {
        public record BiRoute(BiPredicate<String, String> matcher, ChatResult result) {}

        private final List<BiRoute> routes = new ArrayList<>();
        private ChatResult defaultResult = new ChatResult("Default fallback response", 10, 10);

        public BiPredicateRoutingChatPort when(BiPredicate<String, String> matcher, ChatResult result) {
            Objects.requireNonNull(matcher, "matcher must not be null");
            Objects.requireNonNull(result, "result must not be null");
            routes.add(new BiRoute(matcher, result));
            return this;
        }

        public BiPredicateRoutingChatPort setDefaultResult(ChatResult result) {
            this.defaultResult = Objects.requireNonNull(result, "defaultResult must not be null");
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            for (BiRoute r : routes) {
                if (r.matcher().test(system, user)) {
                    return r.result();
                }
            }
            return defaultResult;
        }
    }

    public static class RegexTemplateRoutingChatPort implements ChatPort {
        public record RegexRoute(Pattern pattern, String template) {}

        private final List<RegexRoute> routes = new ArrayList<>();
        private String defaultTemplate = "Default: ${0}";

        public RegexTemplateRoutingChatPort whenRegex(String regex, String template) {
            Objects.requireNonNull(regex, "regex must not be null");
            Objects.requireNonNull(template, "template must not be null");
            routes.add(new RegexRoute(Pattern.compile(regex), template));
            return this;
        }

        public RegexTemplateRoutingChatPort setDefaultTemplate(String template) {
            this.defaultTemplate = Objects.requireNonNull(template, "defaultTemplate must not be null");
            return this;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            for (RegexRoute route : routes) {
                Matcher m = route.pattern().matcher(user);
                if (m.find()) {
                    String text = route.template();
                    for (int g = 1; g <= m.groupCount(); g++) {
                        text = text.replace("${" + g + "}", m.group(g));
                    }
                    return new ChatResult(text, user.length(), text.length());
                }
            }
            String fallback = defaultTemplate.replace("${0}", user);
            return new ChatResult(fallback, user.length(), fallback.length());
        }
    }

    // =========================================================================
    // Topic 3: Fault Injection & Resilience Testing (Scenarios 7 - 9)
    // =========================================================================

    public static class FaultInjectingChatPort implements ChatPort {
        private final Deque<Object> actions = new ArrayDeque<>();
        private int callCount = 0;

        public FaultInjectingChatPort enqueueResult(String text, long tokensIn, long tokensOut) {
            actions.add(new ChatResult(text, tokensIn, tokensOut));
            return this;
        }

        public FaultInjectingChatPort enqueueFault(RuntimeException ex) {
            actions.add(Objects.requireNonNull(ex, "fault must not be null"));
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
            Object action = actions.poll();
            if (action instanceof RuntimeException re) {
                throw re;
            }
            return (ChatResult) action;
        }
    }

    public static class ConsecutiveFaultChatPort implements ChatPort {
        private int failuresRemaining = 0;
        private RuntimeException faultToThrow;
        private ChatResult recoveryResult;
        private int totalAttempts = 0;

        public ConsecutiveFaultChatPort failNext(int failureCount, RuntimeException faultToThrow, ChatResult recoveryResult) {
            this.failuresRemaining = failureCount;
            this.faultToThrow = Objects.requireNonNull(faultToThrow, "faultToThrow must not be null");
            this.recoveryResult = Objects.requireNonNull(recoveryResult, "recoveryResult must not be null");
            return this;
        }

        public int consecutiveFailuresRemaining() {
            return failuresRemaining;
        }

        public int totalAttempts() {
            return totalAttempts;
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            totalAttempts++;
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw faultToThrow;
            }
            return recoveryResult;
        }
    }

    public static class ExponentialBackoffRetrySimulator {
        public static RetrySimulationReport simulateRetry(ChatPort port, String userPrompt, int maxRetries, long initialDelayMs, TestClock clock) {
            int attempts = 0;
            long totalDelay = 0;
            long currentDelay = initialDelayMs;

            while (attempts <= maxRetries) {
                attempts++;
                try {
                    ChatResult res = port.chat("SYSTEM", userPrompt);
                    return new RetrySimulationReport(attempts, totalDelay, true, res);
                } catch (SimulatedUpstreamException sue) {
                    if (attempts > maxRetries) {
                        return new RetrySimulationReport(attempts, totalDelay, false, null);
                    }
                    clock.advance(currentDelay);
                    totalDelay += currentDelay;
                    currentDelay *= 2;
                }
            }
            return new RetrySimulationReport(attempts, totalDelay, false, null);
        }
    }

    // =========================================================================
    // Topic 4: Observation, Auditing & Budget Ceilings (Scenarios 10 - 12)
    // =========================================================================

    public static class PromptCapturingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final List<PromptCapture> captured = new CopyOnWriteArrayList<>();

        public PromptCapturingChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public List<PromptCapture> getCapturedPrompts() {
            return Collections.unmodifiableList(captured);
        }

        public PromptCapture getLastPrompt() {
            if (captured.isEmpty()) return null;
            return captured.get(captured.size() - 1);
        }

        public int getCallCount() {
            return captured.size();
        }

        public void clear() {
            captured.clear();
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            captured.add(new PromptCapture(system, user, toolBeans, System.currentTimeMillis()));
            return delegate.chat(system, user, toolBeans);
        }
    }

    public static class BudgetEnforcingChatPort implements ChatPort {
        private final ChatPort delegate;
        private final long maxTokensAllowed;
        private long totalTokensUsed = 0;

        public BudgetEnforcingChatPort(ChatPort delegate, long maxTokensAllowed) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.maxTokensAllowed = maxTokensAllowed;
        }

        public synchronized long getTotalTokensUsed() {
            return totalTokensUsed;
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            ChatResult result = delegate.chat(system, user, toolBeans);
            totalTokensUsed += result.totalTokens();
            if (totalTokensUsed > maxTokensAllowed) {
                throw new HarnessBudgetExceededException("Offline token budget exceeded: used " + totalTokensUsed + " > max " + maxTokensAllowed);
            }
            return result;
        }
    }

    public static class AuditingCompositeChatPort implements ChatPort {
        private final ChatPort delegate;
        private final List<AuditLogEntry> auditLog = new ArrayList<>();
        private long cumulativeTokens = 0;
        private int callIndex = 0;

        public AuditingCompositeChatPort(ChatPort delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public synchronized List<AuditLogEntry> getAuditLog() {
            return Collections.unmodifiableList(new ArrayList<>(auditLog));
        }

        public synchronized AuditLogEntry peakTokenCall() {
            return auditLog.stream()
                    .max(Comparator.comparingLong(AuditLogEntry::tokensThisCall))
                    .orElse(null);
        }

        @Override
        public synchronized ChatResult chat(String system, String user, List<Object> toolBeans) {
            callIndex++;
            ChatResult result = delegate.chat(system, user, toolBeans);
            long tokensThisCall = result.totalTokens();
            cumulativeTokens += tokensThisCall;
            AuditLogEntry entry = new AuditLogEntry(callIndex, system, user, tokensThisCall, cumulativeTokens, System.currentTimeMillis());
            auditLog.add(entry);
            return result;
        }
    }

    // =========================================================================
    // Topic 5: Framework Adapters, Streaming & Autonomous Loops (Scenarios 13 - 15)
    // =========================================================================

    public static class ToolRegistrySpy {
        public static List<String> extractToolNames(List<Object> toolBeans) {
            if (toolBeans == null) return List.of();
            List<String> names = new ArrayList<>();
            for (Object bean : toolBeans) {
                if (bean == null) continue;
                for (Method method : bean.getClass().getMethods()) {
                    if (method.isAnnotationPresent(Tool.class)) {
                        Tool tool = method.getAnnotation(Tool.class);
                        String name = (tool.name() != null && !tool.name().isBlank()) ? tool.name() : method.getName();
                        names.add(name);
                    }
                }
            }
            return names;
        }
    }

    public static class InMemoryStreamingChatPort implements StreamingChatPort {
        private final Deque<List<String>> streamQueue = new ArrayDeque<>();
        private int streamCallCount = 0;

        public InMemoryStreamingChatPort enqueueStream(List<String> chunks) {
            Objects.requireNonNull(chunks, "chunks must not be null");
            streamQueue.add(new ArrayList<>(chunks));
            return this;
        }

        public int streamCallCount() {
            return streamCallCount;
        }

        @Override
        public Flux<String> stream(String system, String user) {
            streamCallCount++;
            List<String> chunks = streamQueue.poll();
            if (chunks == null) {
                return Flux.empty();
            }
            return Flux.fromIterable(chunks);
        }
    }

    public static class SpringAiChatPort implements ChatPort {
        private final ChatClient chatClient;

        public SpringAiChatPort(ChatClient chatClient) {
            this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        }

        @Override
        public ChatResult chat(String system, String user, List<Object> toolBeans) {
            var promptSpec = chatClient.prompt().system(system).user(user);
            if (toolBeans != null && !toolBeans.isEmpty()) {
                promptSpec = promptSpec.tools(toolBeans.toArray());
            }
            var response = promptSpec.call().chatResponse();
            String text = "";
            long in = 0;
            long out = 0;
            if (response != null) {
                if (response.getResult() != null && response.getResult().getOutput() != null) {
                    text = response.getResult().getOutput().getText();
                }
                if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                    var usage = response.getMetadata().getUsage();
                    in = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    out = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                }
            }
            return new ChatResult(text != null ? text : "", in, out);
        }
    }

    public static class DeterministicDiagnosticLoop {
        public static DiagnosticTriageReport runTriage(ChatPort chatPort, String incident) {
            Objects.requireNonNull(chatPort, "chatPort must not be null");
            Objects.requireNonNull(incident, "incident must not be null");

            ChatResult t1 = chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "INCIDENT: " + incident);
            ChatResult t2 = chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "EVIDENCE: Log query for target completed. Found 13 OOM errors.");
            ChatResult t3 = chatPort.chat("SYSTEM: Autonomous Diagnostic Copilot", "MITIGATION_STATUS: Pod restarted successfully. Please summarize root cause and recommendation.");

            long totalTokens = t1.totalTokens() + t2.totalTokens() + t3.totalTokens();
            String text = t3.text();

            String rootCause = "Unknown root cause";
            String recommendation = "Manual inspection recommended";

            Matcher rcMatcher = Pattern.compile("\"rootCause\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (rcMatcher.find()) {
                rootCause = rcMatcher.group(1);
            }

            Matcher recMatcher = Pattern.compile("\"recommendation\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (recMatcher.find()) {
                recommendation = recMatcher.group(1);
            }

            return new DiagnosticTriageReport(incident, rootCause, recommendation, 3, totalTokens);
        }
    }
}
```
