package phase04;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import phase04.ResilientAdvisorContracts.*;
import phase04.ResilientGuardrailAdvisorsUnderTest.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 04 Exercise 02:
 * Resilient Guardrails, Failover Routing, and Transactional Token Quotas (15 Repetitive Drills).
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
        // Topic 1: Context Augmentation & History Truncation
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Rate Limiting & Distributed Tracing
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Failover Routing & Circuit Breaking
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Schema Healing & Hallucination Grounding
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Transactional Quotas & Production Stacks
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Context Augmentation & History Truncation (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "RuntimeContextAdvisor (Dynamic Execution Mode Prompt & Context Injection)";
        List<String> hints = List.of(
                "getOrder() must return 10.",
                "In DRY_RUN mode, prefix user message text with '[DRY_RUN] ' and set context 'audit.dryRun' = true.",
                "In LIVE mode, set context 'audit.dryRun' = false; set 'processedBy' = 'RuntimeContextAdvisor'."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            RuntimeContextAdvisor advisor = new RuntimeContextAdvisor();
            if (advisor.getOrder() != 10) {
                return new ScenarioResult(1, name, false, "advisor.getOrder() must return 10, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            ChatClientResponse dryRunResponse = client.prompt()
                    .user("Drain worker node k8s-worker-04")
                    .advisors(a -> a.param(ContextKeys.EXECUTION_MODE, "DRY_RUN"))
                    .call()
                    .chatClientResponse();

            Prompt captured1 = model.getLastPrompt();
            if (captured1 == null || captured1.getUserMessage() == null ||
                    !captured1.getUserMessage().getText().startsWith("[DRY_RUN] ")) {
                return new ScenarioResult(1, name, false, "In DRY_RUN mode, user prompt must be prefixed with '[DRY_RUN] '", hints);
            }
            if (!Boolean.TRUE.equals(dryRunResponse.context().get(ContextKeys.AUDIT_DRY_RUN))) {
                return new ScenarioResult(1, name, false, "Response context must contain 'audit.dryRun' = true in DRY_RUN mode", hints);
            }
            if (!"RuntimeContextAdvisor".equals(dryRunResponse.context().get(ContextKeys.PROCESSED_BY))) {
                return new ScenarioResult(1, name, false, "Response context must contain 'processedBy' = 'RuntimeContextAdvisor'", hints);
            }

            ChatClientResponse liveResponse = client.prompt().user("Restart ingress controller").call().chatClientResponse();
            Prompt captured2 = model.getLastPrompt();
            if (captured2 == null || captured2.getUserMessage() == null ||
                    captured2.getUserMessage().getText().startsWith("[DRY_RUN] ")) {
                return new ScenarioResult(1, name, false, "In LIVE mode, user prompt must not be prefixed with '[DRY_RUN] '", hints);
            }
            if (!Boolean.FALSE.equals(liveResponse.context().get(ContextKeys.AUDIT_DRY_RUN))) {
                return new ScenarioResult(1, name, false, "Response context must contain 'audit.dryRun' = false in LIVE mode", hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "SlidingWindowTruncationAdvisor (Pinned SystemMessage with History Pruning)";
        List<String> hints = List.of(
                "getOrder() must return 20.",
                "Separate instructions into SystemMessages and non-system messages.",
                "Retain only the last maxHistoryMessages non-system messages, keeping SystemMessage at index 0.",
                "Record count of removed messages into response context 'truncatedMessageCount'."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            SlidingWindowTruncationAdvisor advisor = new SlidingWindowTruncationAdvisor(2);
            if (advisor.getOrder() != 20) {
                return new ScenarioResult(2, name, false, "advisor.getOrder() must return 20, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            List<Message> history = List.of(
                    new SystemMessage("SYSTEM: You are an autonomous SRE agent."),
                    new UserMessage("Message 1: Service degraded"),
                    new AssistantMessage("Response 1: Checking logs"),
                    new UserMessage("Message 2: DB pool exhausted"),
                    new AssistantMessage("Response 2: Increasing connections"),
                    new UserMessage("Message 3: Pool saturated again")
            );

            ChatClientResponse response = client.prompt(new Prompt(history)).call().chatClientResponse();

            Prompt captured = model.getLastPrompt();
            if (captured == null || captured.getInstructions() == null) {
                return new ScenarioResult(2, name, false, "Captured prompt instructions must not be null", hints);
            }
            List<Message> instructions = captured.getInstructions();
            if (instructions.size() != 3) {
                return new ScenarioResult(2, name, false, "Expected 3 messages (1 system + 2 retained), got: " + instructions.size(), hints);
            }
            if (!(instructions.get(0) instanceof SystemMessage sys) || !sys.getText().contains("autonomous SRE agent")) {
                return new ScenarioResult(2, name, false, "SystemMessage must be preserved at index 0", hints);
            }
            if (!Integer.valueOf(3).equals(response.context().get(ContextKeys.TRUNCATED_MESSAGE_COUNT))) {
                return new ScenarioResult(2, name, false, "Response context 'truncatedMessageCount' expected 3, got: " + response.context().get(ContextKeys.TRUNCATED_MESSAGE_COUNT), hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "SystemPromptDynamicAugmentorAdvisor (Runtime Metadata Tag Augmenter)";
        List<String> hints = List.of(
                "getOrder() must return 25.",
                "Extract tenantId and env from request context if present.",
                "Append dynamic tags '[TENANT=' + tenantId + '] [ENV=' + env + ']' to the existing SystemMessage.",
                "Store concatenated tags in response context 'dynamicSystemTags'."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            SystemPromptDynamicAugmentorAdvisor advisor = new SystemPromptDynamicAugmentorAdvisor();
            if (advisor.getOrder() != 25) {
                return new ScenarioResult(3, name, false, "getOrder() must return 25, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            List<Message> msgs = List.of(
                    new SystemMessage("SYSTEM: SRE copilot."),
                    new UserMessage("Investigate disk spill")
            );

            ChatClientResponse res = client.prompt(new Prompt(msgs))
                    .advisors(a -> a.param("tenantId", "prod-42").param("env", "production"))
                    .call()
                    .chatClientResponse();

            Prompt captured = model.getLastPrompt();
            if (captured == null || !(captured.getInstructions().get(0) instanceof SystemMessage sys)) {
                return new ScenarioResult(3, name, false, "Expected SystemMessage at index 0", hints);
            }
            if (!sys.getText().contains("[TENANT=prod-42]") || !sys.getText().contains("[ENV=production]")) {
                return new ScenarioResult(3, name, false, "SystemMessage did not contain augmented metadata tags: " + sys.getText(), hints);
            }
            if (!res.context().containsKey(ContextKeys.DYNAMIC_SYSTEM_TAGS)) {
                return new ScenarioResult(3, name, false, "Response context missing 'dynamicSystemTags'", hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Rate Limiting & Distributed Tracing (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "SlidingWindowRateLimiterAdvisor (Rolling Timestamp Deque Rate Limiter)";
        List<String> hints = List.of(
                "getOrder() must return 30.",
                "Prune timestamps older than (clock.getAsLong() - windowMs).",
                "If queue.size() >= maxRequests, throw RateLimitExceededException containing 'Rate limit exceeded'.",
                "Set context 'remainingRequests' = maxRequests - queue.size()."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            AtomicLong simulatedClock = new AtomicLong(1000L);
            SlidingWindowRateLimiterAdvisor advisor = new SlidingWindowRateLimiterAdvisor(2, 5000L, simulatedClock::get);
            if (advisor.getOrder() != 30) {
                return new ScenarioResult(4, name, false, "advisor.getOrder() must return 30, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            ChatClientResponse res1 = client.prompt().user("Request 1").call().chatClientResponse();
            if (!Integer.valueOf(1).equals(res1.context().get(ContextKeys.REMAINING_REQUESTS))) {
                return new ScenarioResult(4, name, false, "Expected remainingRequests 1, got: " + res1.context().get(ContextKeys.REMAINING_REQUESTS), hints);
            }

            simulatedClock.set(2000L);
            ChatClientResponse res2 = client.prompt().user("Request 2").call().chatClientResponse();
            if (!Integer.valueOf(0).equals(res2.context().get(ContextKeys.REMAINING_REQUESTS))) {
                return new ScenarioResult(4, name, false, "Expected remainingRequests 0, got: " + res2.context().get(ContextKeys.REMAINING_REQUESTS), hints);
            }

            simulatedClock.set(3000L);
            boolean rejected = false;
            try {
                client.prompt().user("Request 3").call().content();
            } catch (RateLimitExceededException rle) {
                if (rle.getMessage() != null && rle.getMessage().toLowerCase().contains("rate limit exceeded")) {
                    rejected = true;
                }
            }
            if (!rejected) {
                return new ScenarioResult(4, name, false, "Request 3 must throw RateLimitExceededException", hints);
            }

            simulatedClock.set(6500L);
            ChatClientResponse res4 = client.prompt().user("Request 4").call().chatClientResponse();
            if (!Integer.valueOf(0).equals(res4.context().get(ContextKeys.REMAINING_REQUESTS))) {
                return new ScenarioResult(4, name, false, "Request 4 should succeed after window slide", hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "TraceContextPropagationAdvisor (W3C Distributed Traceparent Propagation)";
        List<String> hints = List.of(
                "getOrder() must return 40.",
                "Generate W3C traceparent '00-<32hex>-<16hex>-01' if missing; preserve if provided.",
                "Prefix user prompt with '[traceparent=' + trace + '] ' and set context 'propagationSuccess' = true."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            TraceContextPropagationAdvisor advisor = new TraceContextPropagationAdvisor();
            if (advisor.getOrder() != 40) {
                return new ScenarioResult(5, name, false, "advisor.getOrder() must return 40, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();
            Pattern traceparentPattern = Pattern.compile("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");

            ChatClientResponse res1 = client.prompt().user("Trace payment flow").call().chatClientResponse();
            String trace1 = String.valueOf(res1.context().get(ContextKeys.TRACEPARENT));
            if (!traceparentPattern.matcher(trace1).matches()) {
                return new ScenarioResult(5, name, false, "Traceparent mismatch: " + trace1, hints);
            }
            Prompt captured1 = model.getLastPrompt();
            if (captured1 == null || captured1.getUserMessage() == null ||
                    !captured1.getUserMessage().getText().startsWith("[traceparent=" + trace1 + "] ")) {
                return new ScenarioResult(5, name, false, "Prompt missing trace prefix", hints);
            }
            if (!Boolean.TRUE.equals(res1.context().get(ContextKeys.PROPAGATION_SUCCESS))) {
                return new ScenarioResult(5, name, false, "propagationSuccess should be true", hints);
            }

            String existingTrace = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
            ChatClientResponse res2 = client.prompt().user("Trace downstream").advisors(a -> a.param(ContextKeys.TRACEPARENT, existingTrace)).call().chatClientResponse();
            if (!existingTrace.equals(res2.context().get(ContextKeys.TRACEPARENT))) {
                return new ScenarioResult(5, name, false, "Existing traceparent must be preserved", hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "CompositeRateAndTraceAdvisor (Unified Ingress Rate & Trace Guardrail)";
        List<String> hints = List.of(
                "getOrder() must return 35.",
                "Enforce rate limiting; if within limits, generate/propagate W3C traceparent.",
                "Ensure both 'remainingRequests' and 'traceparent' are set in response context."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            AtomicLong clock = new AtomicLong(1000L);
            CompositeRateAndTraceAdvisor advisor = new CompositeRateAndTraceAdvisor(2, 5000L, clock::get);
            if (advisor.getOrder() != 35) {
                return new ScenarioResult(6, name, false, "getOrder() must return 35, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();
            ChatClientResponse res = client.prompt().user("Ingress call").call().chatClientResponse();

            if (!res.context().containsKey(ContextKeys.REMAINING_REQUESTS)) {
                return new ScenarioResult(6, name, false, "Response context missing 'remainingRequests'", hints);
            }
            if (!res.context().containsKey(ContextKeys.TRACEPARENT)) {
                return new ScenarioResult(6, name, false, "Response context missing 'traceparent'", hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Failover Routing & Circuit Breaking (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "ModelFailoverAdvisor (Downstream Model Resilient Failover Routing)";
        List<String> hints = List.of(
                "getOrder() must return 80.",
                "Try chain.nextCall(request).",
                "On failure, execute fallbackModel.call(request.prompt()) and set context 'failoverTriggered' = true, 'primaryError' = ex.getMessage()."
        );
        try {
            FakeFailoverChatModel primaryModel = new FakeFailoverChatModel();
            FakeFailoverChatModel fallbackModel = new FakeFailoverChatModel();
            fallbackModel.enqueue("{\"status\": \"SUCCESS\", \"source\": \"FALLBACK_MODEL\"}");

            ModelFailoverAdvisor advisor = new ModelFailoverAdvisor(fallbackModel);
            if (advisor.getOrder() != 80) {
                return new ScenarioResult(7, name, false, "advisor.getOrder() must return 80, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(primaryModel).defaultAdvisors(advisor).build();

            primaryModel.failWith(new DownstreamModelFailureException("503 Gateway Timeout"));
            ChatClientResponse failoverResponse = client.prompt().user("Diagnose payment latency").call().chatClientResponse();

            if (!Boolean.TRUE.equals(failoverResponse.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(7, name, false, "failoverTriggered must be true on primary failure", hints);
            }
            if (!failoverResponse.chatResponse().getResult().getOutput().getText().contains("FALLBACK_MODEL")) {
                return new ScenarioResult(7, name, false, "Response output should come from fallback model", hints);
            }

            primaryModel.clear();
            fallbackModel.clear();
            primaryModel.enqueue("{\"status\": \"SUCCESS\", \"source\": \"PRIMARY_MODEL\"}");
            ChatClientResponse normalResponse = client.prompt().user("Check database status").call().chatClientResponse();

            if (!Boolean.FALSE.equals(normalResponse.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(7, name, false, "failoverTriggered must be false on primary success", hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "CircuitBreakerFailoverAdvisor (Circuit-Tripping Failover Advisor)";
        List<String> hints = List.of(
                "getOrder() must return 85.",
                "Track primary consecutive failure count; trip to OPEN after threshold failures.",
                "When tripped, skip primary and route directly to fallbackModel, setting context 'circuitTripped' = true."
        );
        try {
            FakeFailoverChatModel primaryModel = new FakeFailoverChatModel();
            FakeFailoverChatModel fallbackModel = new FakeFailoverChatModel();
            fallbackModel.enqueue("Fallback 1");
            fallbackModel.enqueue("Fallback 2");

            CircuitBreakerFailoverAdvisor advisor = new CircuitBreakerFailoverAdvisor(fallbackModel, 2);
            if (advisor.getOrder() != 85) {
                return new ScenarioResult(8, name, false, "getOrder() must return 85, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(primaryModel).defaultAdvisors(advisor).build();

            primaryModel.failWith(new DownstreamModelFailureException("500 Internal Server Error"));
            ChatClientResponse r1 = client.prompt().user("Call 1").call().chatClientResponse();
            primaryModel.failWith(new DownstreamModelFailureException("500 Internal Server Error"));
            ChatClientResponse r2 = client.prompt().user("Call 2").call().chatClientResponse();

            if (advisor.isTripped() != true) {
                return new ScenarioResult(8, name, false, "Circuit should be tripped after 2 consecutive failures", hints);
            }

            ChatClientResponse r3 = client.prompt().user("Call 3 - circuit open").call().chatClientResponse();
            if (!Boolean.TRUE.equals(r3.context().get(ContextKeys.CIRCUIT_TRIPPED))) {
                return new ScenarioResult(8, name, false, "Response context should contain 'circuitTripped' = true", hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "AdaptiveMultiModelRouterAdvisor (Tiered 3-Model Failover Cascade)";
        List<String> hints = List.of(
                "getOrder() must return 88.",
                "Cascade: primary -> secondary -> offlineStub.",
                "Record cascadeFailoverLevel (0 for primary, 1 for secondary, 2 for offline stub) in response context."
        );
        try {
            FakeFailoverChatModel primary = new FakeFailoverChatModel();
            FakeFailoverChatModel secondary = new FakeFailoverChatModel();
            FakeFailoverChatModel offlineStub = new FakeFailoverChatModel();
            offlineStub.enqueue("Offline emergency response");

            AdaptiveMultiModelRouterAdvisor advisor = new AdaptiveMultiModelRouterAdvisor(secondary, offlineStub);
            if (advisor.getOrder() != 88) {
                return new ScenarioResult(9, name, false, "getOrder() must return 88, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(primary).defaultAdvisors(advisor).build();

            primary.failWith(new RuntimeException("Primary down"));
            secondary.failWith(new RuntimeException("Secondary down"));

            ChatClientResponse res = client.prompt().user("Emergency query").call().chatClientResponse();
            if (!Integer.valueOf(2).equals(res.context().get(ContextKeys.CASCADE_FAILOVER_LEVEL))) {
                return new ScenarioResult(9, name, false, "Expected cascadeFailoverLevel 2 for offline stub, got: " + res.context().get(ContextKeys.CASCADE_FAILOVER_LEVEL), hints);
            }
            if (!res.chatResponse().getResult().getOutput().getText().contains("Offline emergency response")) {
                return new ScenarioResult(9, name, false, "Expected output from offline stub", hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Schema Healing & Hallucination Grounding (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "SchemaSelfHealingAdvisor (One-Shot Output Repair Retry Loop)";
        List<String> hints = List.of(
                "getOrder() must return 70.",
                "Verify if output contains valid JSON with 'status' field.",
                "If invalid, execute one-shot retry with 'SCHEMA_REPAIR_NOTICE' prepended to instructions.",
                "If repaired, set context 'schemaHealed' = true; if retry also fails, throw SchemaValidationException containing 'Schema validation failed'."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            SchemaSelfHealingAdvisor advisor = new SchemaSelfHealingAdvisor();
            if (advisor.getOrder() != 70) {
                return new ScenarioResult(10, name, false, "advisor.getOrder() must return 70, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            model.clear();
            model.enqueue("{\"status\": \"SUCCESS\", \"action\": \"NO_ACTION_NEEDED\"}");
            ChatClientResponse res1 = client.prompt().user("Triage cluster").call().chatClientResponse();
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.SCHEMA_HEALED))) {
                return new ScenarioResult(10, name, false, "schemaHealed should be false on first attempt valid schema", hints);
            }

            model.clear();
            model.enqueue("Invalid non-json output");
            model.enqueue("{\"status\": \"SUCCESS\", \"action\": \"SCALED_OUT\"}");
            ChatClientResponse res2 = client.prompt().user("Scale pods").call().chatClientResponse();
            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.SCHEMA_HEALED))) {
                return new ScenarioResult(10, name, false, "schemaHealed should be true after repair retry", hints);
            }

            model.clear();
            model.enqueue("Corrupted output 1");
            model.enqueue("Still corrupted output 2");
            boolean failed = false;
            try {
                client.prompt().user("Restart node").call().content();
            } catch (SchemaValidationException sve) {
                if (sve.getMessage() != null && sve.getMessage().toLowerCase().contains("schema validation failed")) {
                    failed = true;
                }
            }
            if (!failed) {
                return new ScenarioResult(10, name, false, "Double invalid output must throw SchemaValidationException", hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "GroundingValidationAdvisor (Entity Hallucination & Ungrounded Reference Detector)";
        List<String> hints = List.of(
                "getOrder() must return 60.",
                "Extract referenced entities matching regex: (cluster-[a-z0-9-]+|host-[a-z0-9-]+).",
                "If entity is not in allowedEntities, set context 'groundingViolation' = true, 'ungroundedEntities' = Set.",
                "Prefix output text with '[UNGROUNDED_ENTITY_DETECTED] '."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            Set<String> allowedEntities = Set.of("cluster-alpha", "cluster-beta", "host-web-01", "host-web-02");
            GroundingValidationAdvisor advisor = new GroundingValidationAdvisor(allowedEntities);
            if (advisor.getOrder() != 60) {
                return new ScenarioResult(11, name, false, "advisor.getOrder() must return 60, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            model.clear();
            model.enqueue("Healthy node host-web-01 running on cluster-alpha.");
            ChatClientResponse res1 = client.prompt().user("Audit node").call().chatClientResponse();
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(11, name, false, "groundingViolation should be false for allowed entities", hints);
            }

            model.clear();
            model.enqueue("Compromised node host-web-99 detected on cluster-rogue.");
            ChatClientResponse res2 = client.prompt().user("Audit unknown node").call().chatClientResponse();
            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(11, name, false, "groundingViolation should be true when unknown entities referenced", hints);
            }
            String outputText = res2.chatResponse().getResult().getOutput().getText();
            if (!outputText.startsWith("[UNGROUNDED_ENTITY_DETECTED] ")) {
                return new ScenarioResult(11, name, false, "Ungrounded response must start with '[UNGROUNDED_ENTITY_DETECTED] '", hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "FactualityStrictGroundingAdvisor (Strict Grounding Enforcement Double)";
        List<String> hints = List.of(
                "getOrder() must return 65.",
                "If any entity in completion is ungrounded, completely overwrite output with '[REDACTED_UNGROUNDED_FACTS]'.",
                "Set context 'groundingViolation' = true."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            Set<String> allowed = Set.of("prod-cluster");
            FactualityStrictGroundingAdvisor advisor = new FactualityStrictGroundingAdvisor(allowed);
            if (advisor.getOrder() != 65) {
                return new ScenarioResult(12, name, false, "getOrder() must return 65, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            model.enqueue("Status on rogue-cluster is bad");
            ChatClientResponse res = client.prompt().user("Query status").call().chatClientResponse();

            if (!res.chatResponse().getResult().getOutput().getText().contains("[REDACTED_UNGROUNDED_FACTS]")) {
                return new ScenarioResult(12, name, false, "Expected output to be replaced with '[REDACTED_UNGROUNDED_FACTS]'", hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Transactional Quotas & Production Stacks (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "DynamicTokenQuotaAdvisor (Pre-Allocation Reservation with Transactional Rollback)";
        List<String> hints = List.of(
                "getOrder() must return 50.",
                "Estimate required tokens; if remainingQuota < estimate, throw BudgetExceededException containing 'Insufficient token quota'.",
                "Deduct estimate before call; on exception rollback estimate; on success reconcile actual token consumption."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            DynamicTokenQuotaAdvisor advisor = new DynamicTokenQuotaAdvisor(100, text -> 50);
            if (advisor.getOrder() != 50) {
                return new ScenarioResult(13, name, false, "advisor.getOrder() must return 50, got: " + advisor.getOrder(), hints);
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            model.setTokenUsage(30, 20);
            ChatClientResponse res1 = client.prompt().user("Triage load balancer").call().chatClientResponse();
            if (advisor.getRemainingQuota() != 50) {
                return new ScenarioResult(13, name, false, "Remaining quota should be 50, got: " + advisor.getRemainingQuota(), hints);
            }

            model.failWith(new RuntimeException("Simulated network timeout"));
            boolean threw = false;
            try {
                client.prompt().user("Query telemetry").call().content();
            } catch (RuntimeException re) {
                threw = true;
            }
            if (!threw || advisor.getRemainingQuota() != 50) {
                return new ScenarioResult(13, name, false, "Failed call must rollback reservation! Remaining quota should be 50, got: " + advisor.getRemainingQuota(), hints);
            }

            DynamicTokenQuotaAdvisor smallAdvisor = new DynamicTokenQuotaAdvisor(20, text -> 50);
            ChatClient smallClient = ChatClient.builder(model).defaultAdvisors(smallAdvisor).build();
            boolean budgetExceeded = false;
            try {
                smallClient.prompt().user("Query large report").call().content();
            } catch (BudgetExceededException bee) {
                if (bee.getMessage() != null && bee.getMessage().toLowerCase().contains("insufficient token quota")) {
                    budgetExceeded = true;
                }
            }
            if (!budgetExceeded) {
                return new ScenarioResult(13, name, false, "Should throw BudgetExceededException when remaining quota is insufficient", hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "CompositeAdvisorChainShortCircuit (Order-Based Short-Circuiting Pipeline)";
        List<String> hints = List.of(
                "Ensure RateLimiter (order 30) runs BEFORE QuotaAdvisor (order 50).",
                "When RateLimiter short-circuits call 2, verify QuotaAdvisor remaining quota is NOT deducted.",
                "Verify downstream ChatModel is NOT invoked on short-circuited call."
        );
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            AtomicLong clock = new AtomicLong(1000L);
            SlidingWindowRateLimiterAdvisor rateLimiter = new SlidingWindowRateLimiterAdvisor(1, 10000L, clock::get);
            DynamicTokenQuotaAdvisor quotaAdvisor = new DynamicTokenQuotaAdvisor(200, text -> 50);

            ChatClient client = ResilientGuardrailAdvisorsUnderTest.createShortCircuitingClient(model, rateLimiter, quotaAdvisor);

            client.prompt().user("First allowed call").call().content();
            if (model.getCallCount() != 1) {
                return new ScenarioResult(14, name, false, "First call should invoke model once, got: " + model.getCallCount(), hints);
            }
            int quotaAfterCall1 = quotaAdvisor.getRemainingQuota();

            boolean shortCircuited = false;
            try {
                client.prompt().user("Second blocked call").call().content();
            } catch (RateLimitExceededException rle) {
                shortCircuited = true;
            }
            if (!shortCircuited) {
                return new ScenarioResult(14, name, false, "Second call should be short-circuited by RateLimiter", hints);
            }
            if (model.getCallCount() != 1) {
                return new ScenarioResult(14, name, false, "Downstream model must not be called when short-circuited", hints);
            }
            if (quotaAdvisor.getRemainingQuota() != quotaAfterCall1) {
                return new ScenarioResult(14, name, false, "Downstream quota advisor must not deduct quota when short-circuited", hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "AutonomousResilientRemediationStack (Production Multi-Guardrail Integration)";
        List<String> hints = List.of(
                "Build full ChatClient stack combining RuntimeContext, RateLimiter, TokenQuota, TraceContext, GroundingValidation, and Failover.",
                "Under normal execution, verify audit.dryRun, traceparent, and clean outputs.",
                "Under primary model failure, verify failover to secondary model and grounding validation of secondary output."
        );
        try {
            FakeFailoverChatModel primaryModel = new FakeFailoverChatModel();
            FakeFailoverChatModel fallbackModel = new FakeFailoverChatModel();
            Set<String> allowedEntities = Set.of("cluster-alpha", "cluster-beta", "host-web-01");

            ChatClient stackClient = ResilientGuardrailAdvisorsUnderTest.createResilientProductionStack(
                    primaryModel,
                    fallbackModel,
                    5,
                    60000L,
                    500,
                    allowedEntities
            );

            primaryModel.clear();
            primaryModel.enqueue("Remediation complete on host-web-01");
            ChatClientResponse res1 = stackClient.prompt()
                    .user("Remediate web tier")
                    .advisors(a -> a.param(ContextKeys.EXECUTION_MODE, "DRY_RUN"))
                    .call()
                    .chatClientResponse();

            if (!Boolean.TRUE.equals(res1.context().get(ContextKeys.AUDIT_DRY_RUN))) {
                return new ScenarioResult(15, name, false, "Stack must propagate audit.dryRun", hints);
            }
            if (res1.context().get(ContextKeys.TRACEPARENT) == null) {
                return new ScenarioResult(15, name, false, "Stack must generate and propagate traceparent", hints);
            }
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(15, name, false, "failoverTriggered should be false under normal execution", hints);
            }
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(15, name, false, "groundingViolation should be false for valid host", hints);
            }

            primaryModel.clear();
            fallbackModel.clear();
            primaryModel.failWith(new DownstreamModelFailureException("Primary LLM connection reset"));
            fallbackModel.enqueue("Fallback recommendation targeting host-rogue-66 on cluster-delta");

            ChatClientResponse res2 = stackClient.prompt()
                    .user("Execute emergency failover action")
                    .call()
                    .chatClientResponse();

            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(15, name, false, "Stack must trigger failover when primary fails", hints);
            }
            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(15, name, false, "Stack grounding validator must inspect fallback output", hints);
            }
            String res2Text = res2.chatResponse().getResult().getOutput().getText();
            if (!res2Text.startsWith("[UNGROUNDED_ENTITY_DETECTED] ")) {
                return new ScenarioResult(15, name, false, "Ungrounded fallback response must be prefixed with '[UNGROUNDED_ENTITY_DETECTED] '", hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 04 EXERCISE 02: RESILIENT GUARDRAILS & FAILOVER ROUTING REPORT");
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
