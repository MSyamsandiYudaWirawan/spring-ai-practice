package phase04;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
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
 * Resilient Guardrails, Failover Routing, and Transactional Token Quotas (10 Scenarios).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

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
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "RuntimeContextAdvisor (Dynamic Execution Mode Prompt & Context Injection)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            RuntimeContextAdvisor advisor = new RuntimeContextAdvisor();
            if (advisor.getOrder() != 10) {
                return new ScenarioResult(1, name, false, "advisor.getOrder() must return 10, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            // Test 1: DRY_RUN mode
            ChatClientResponse dryRunResponse = client.prompt()
                    .user("Drain worker node k8s-worker-04")
                    .advisors(a -> a.param(ContextKeys.EXECUTION_MODE, "DRY_RUN"))
                    .call()
                    .chatClientResponse();

            Prompt captured1 = model.getLastPrompt();
            if (captured1 == null || captured1.getUserMessage() == null ||
                    !captured1.getUserMessage().getText().startsWith("[DRY_RUN] ")) {
                return new ScenarioResult(1, name, false,
                        "In DRY_RUN mode, user prompt must be prefixed with '[DRY_RUN] ', got: " +
                                (captured1 != null && captured1.getUserMessage() != null ? captured1.getUserMessage().getText() : "null"));
            }
            if (!Boolean.TRUE.equals(dryRunResponse.context().get(ContextKeys.AUDIT_DRY_RUN))) {
                return new ScenarioResult(1, name, false,
                        "Response context must contain 'audit.dryRun' = true in DRY_RUN mode");
            }
            if (!"RuntimeContextAdvisor".equals(dryRunResponse.context().get(ContextKeys.PROCESSED_BY))) {
                return new ScenarioResult(1, name, false,
                        "Response context must contain 'processedBy' = 'RuntimeContextAdvisor'");
            }

            // Test 2: LIVE mode (or omitted)
            ChatClientResponse liveResponse = client.prompt()
                    .user("Restart ingress controller")
                    .call()
                    .chatClientResponse();

            Prompt captured2 = model.getLastPrompt();
            if (captured2 == null || captured2.getUserMessage() == null ||
                    captured2.getUserMessage().getText().startsWith("[DRY_RUN] ")) {
                return new ScenarioResult(1, name, false,
                        "In LIVE mode, user prompt must not be prefixed with '[DRY_RUN] '");
            }
            if (!Boolean.FALSE.equals(liveResponse.context().get(ContextKeys.AUDIT_DRY_RUN))) {
                return new ScenarioResult(1, name, false,
                        "Response context must contain 'audit.dryRun' = false in LIVE mode");
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "SlidingWindowTruncationAdvisor (Pinned SystemMessage with Message History Pruning)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            SlidingWindowTruncationAdvisor advisor = new SlidingWindowTruncationAdvisor(2);
            if (advisor.getOrder() != 20) {
                return new ScenarioResult(2, name, false, "advisor.getOrder() must return 20, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            // Construct 1 SystemMessage + 5 non-system messages
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
                return new ScenarioResult(2, name, false, "Captured prompt instructions must not be null");
            }
            List<Message> instructions = captured.getInstructions();
            // With maxHistoryMessages = 2, instructions should contain 1 SystemMessage + 2 retained non-system messages = 3 total
            if (instructions.size() != 3) {
                return new ScenarioResult(2, name, false,
                        "Expected 3 messages (1 system + 2 retained history), got: " + instructions.size());
            }
            if (!(instructions.get(0) instanceof SystemMessage sys) || !sys.getText().contains("autonomous SRE agent")) {
                return new ScenarioResult(2, name, false, "SystemMessage must be preserved at index 0");
            }
            if (!(instructions.get(1) instanceof AssistantMessage a) || !a.getText().contains("Increasing connections")) {
                return new ScenarioResult(2, name, false, "Second message should be 'Increasing connections'");
            }
            if (!(instructions.get(2) instanceof UserMessage u) || !u.getText().contains("Pool saturated again")) {
                return new ScenarioResult(2, name, false, "Third message should be 'Pool saturated again'");
            }

            Object truncatedCount = response.context().get(ContextKeys.TRUNCATED_MESSAGE_COUNT);
            if (!Integer.valueOf(3).equals(truncatedCount)) {
                return new ScenarioResult(2, name, false,
                        "Response context 'truncatedMessageCount' expected 3, got: " + truncatedCount);
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "SlidingWindowRateLimiterAdvisor (Rolling Timestamp Deque Rate Limiter)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            AtomicLong simulatedClock = new AtomicLong(1000L);
            // 2 requests per 5000ms window
            SlidingWindowRateLimiterAdvisor advisor = new SlidingWindowRateLimiterAdvisor(2, 5000L, simulatedClock::get);
            if (advisor.getOrder() != 30) {
                return new ScenarioResult(3, name, false, "advisor.getOrder() must return 30, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            // Request 1 at t = 1000
            ChatClientResponse res1 = client.prompt().user("Request 1").call().chatClientResponse();
            if (!Integer.valueOf(1).equals(res1.context().get(ContextKeys.REMAINING_REQUESTS))) {
                return new ScenarioResult(3, name, false,
                        "After Request 1, remainingRequests should be 1, got: " + res1.context().get(ContextKeys.REMAINING_REQUESTS));
            }

            // Request 2 at t = 2000
            simulatedClock.set(2000L);
            ChatClientResponse res2 = client.prompt().user("Request 2").call().chatClientResponse();
            if (!Integer.valueOf(0).equals(res2.context().get(ContextKeys.REMAINING_REQUESTS))) {
                return new ScenarioResult(3, name, false,
                        "After Request 2, remainingRequests should be 0, got: " + res2.context().get(ContextKeys.REMAINING_REQUESTS));
            }

            // Request 3 at t = 3000 (should fail rate limit)
            simulatedClock.set(3000L);
            boolean rejected = false;
            try {
                client.prompt().user("Request 3").call().content();
            } catch (RateLimitExceededException rle) {
                String msg = rle.getMessage() != null ? rle.getMessage().toLowerCase() : "";
                if (!msg.contains("rate limit exceeded")) {
                    return new ScenarioResult(3, name, false,
                            "RateLimitExceededException message must contain 'Rate limit exceeded', got: " + rle.getMessage());
                }
                rejected = true;
            }
            if (!rejected) {
                return new ScenarioResult(3, name, false, "Request 3 at t=3000 should have thrown RateLimitExceededException");
            }

            // Request 4 at t = 6500 (t - 5000 = 1500, so t=1000 is expired; 1 slot open)
            simulatedClock.set(6500L);
            ChatClientResponse res4 = client.prompt().user("Request 4").call().chatClientResponse();
            if (!Integer.valueOf(0).equals(res4.context().get(ContextKeys.REMAINING_REQUESTS))) {
                return new ScenarioResult(3, name, false,
                        "Request 4 after slide should succeed with 0 remaining, got: " + res4.context().get(ContextKeys.REMAINING_REQUESTS));
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "ModelFailoverAdvisor (Downstream Model Resilient Failover Routing)";
        try {
            FakeFailoverChatModel primaryModel = new FakeFailoverChatModel();
            FakeFailoverChatModel fallbackModel = new FakeFailoverChatModel();
            fallbackModel.enqueue("{\"status\": \"SUCCESS\", \"source\": \"FALLBACK_MODEL\"}");

            ModelFailoverAdvisor advisor = new ModelFailoverAdvisor(fallbackModel);
            if (advisor.getOrder() != 80) {
                return new ScenarioResult(4, name, false, "advisor.getOrder() must return 80, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(primaryModel).defaultAdvisors(advisor).build();

            // Test 1: Primary failure triggers failover
            primaryModel.failWith(new DownstreamModelFailureException("503 Gateway Timeout"));
            ChatClientResponse failoverResponse = client.prompt().user("Diagnose payment latency").call().chatClientResponse();

            if (!Boolean.TRUE.equals(failoverResponse.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(4, name, false, "failoverTriggered must be true on primary failure");
            }
            String primaryError = String.valueOf(failoverResponse.context().get(ContextKeys.PRIMARY_ERROR));
            if (!primaryError.toLowerCase().contains("503")) {
                return new ScenarioResult(4, name, false, "primaryError context should contain error message '503', got: " + primaryError);
            }
            if (!failoverResponse.chatResponse().getResult().getOutput().getText().contains("FALLBACK_MODEL")) {
                return new ScenarioResult(4, name, false, "Response output should come from fallback model");
            }

            // Test 2: Primary success does NOT trigger failover
            primaryModel.clear();
            fallbackModel.clear();
            primaryModel.enqueue("{\"status\": \"SUCCESS\", \"source\": \"PRIMARY_MODEL\"}");
            ChatClientResponse normalResponse = client.prompt().user("Check database status").call().chatClientResponse();

            if (!Boolean.FALSE.equals(normalResponse.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(4, name, false, "failoverTriggered must be false on primary success");
            }
            if (fallbackModel.getCallCount() != 0) {
                return new ScenarioResult(4, name, false, "Fallback model should not be called when primary succeeds");
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "SchemaSelfHealingAdvisor (One-Shot Output Repair Retry Loop)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            SchemaSelfHealingAdvisor advisor = new SchemaSelfHealingAdvisor();
            if (advisor.getOrder() != 70) {
                return new ScenarioResult(5, name, false, "advisor.getOrder() must return 70, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            // Test 1: Immediate valid schema
            model.clear();
            model.enqueue("{\"status\": \"SUCCESS\", \"action\": \"NO_ACTION_NEEDED\"}");
            ChatClientResponse res1 = client.prompt().user("Triage cluster").call().chatClientResponse();
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.SCHEMA_HEALED))) {
                return new ScenarioResult(5, name, false, "schemaHealed should be false on first-attempt valid schema");
            }
            if (model.getCallCount() != 1) {
                return new ScenarioResult(5, name, false, "Expected 1 model call for valid schema, got: " + model.getCallCount());
            }

            // Test 2: Invalid first attempt healed on retry
            model.clear();
            model.enqueue("Invalid non-json output missing status field");
            model.enqueue("{\"status\": \"SUCCESS\", \"action\": \"SCALED_OUT\"}");
            ChatClientResponse res2 = client.prompt().user("Scale pods").call().chatClientResponse();
            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.SCHEMA_HEALED))) {
                return new ScenarioResult(5, name, false, "schemaHealed should be true after successful retry");
            }
            if (model.getCallCount() != 2) {
                return new ScenarioResult(5, name, false, "Expected 2 model calls (1 initial + 1 repair), got: " + model.getCallCount());
            }
            Prompt retryPrompt = model.getLastPrompt();
            if (retryPrompt == null || !retryPrompt.getInstructions().stream()
                    .anyMatch(m -> m.getText().toLowerCase().contains("schema_repair_notice"))) {
                return new ScenarioResult(5, name, false, "Retry prompt must contain 'SCHEMA_REPAIR_NOTICE' instruction");
            }

            // Test 3: Invalid first attempt AND invalid retry throws SchemaValidationException
            model.clear();
            model.enqueue("Corrupted output 1");
            model.enqueue("Still corrupted output 2");
            boolean failed = false;
            try {
                client.prompt().user("Restart node").call().content();
            } catch (SchemaValidationException sve) {
                String msg = sve.getMessage() != null ? sve.getMessage().toLowerCase() : "";
                if (!msg.contains("schema validation failed")) {
                    return new ScenarioResult(5, name, false,
                            "SchemaValidationException message must contain 'Schema validation failed', got: " + sve.getMessage());
                }
                failed = true;
            }
            if (!failed) {
                return new ScenarioResult(5, name, false, "Double invalid output should throw SchemaValidationException");
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "GroundingValidationAdvisor (Entity Hallucination & Ungrounded Reference Detector)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            Set<String> allowedEntities = Set.of("cluster-alpha", "cluster-beta", "host-web-01", "host-web-02");
            GroundingValidationAdvisor advisor = new GroundingValidationAdvisor(allowedEntities);
            if (advisor.getOrder() != 60) {
                return new ScenarioResult(6, name, false, "advisor.getOrder() must return 60, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            // Test 1: Grounded response
            model.clear();
            model.enqueue("Healthy node host-web-01 running on cluster-alpha.");
            ChatClientResponse res1 = client.prompt().user("Audit node").call().chatClientResponse();
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(6, name, false, "groundingViolation should be false for allowed entities");
            }
            if (res1.chatResponse().getResult().getOutput().getText().contains("[UNGROUNDED_ENTITY_DETECTED]")) {
                return new ScenarioResult(6, name, false, "Grounded response must not contain violation prefix");
            }

            // Test 2: Ungrounded response
            model.clear();
            model.enqueue("Compromised node host-web-99 detected on cluster-rogue.");
            ChatClientResponse res2 = client.prompt().user("Audit unknown node").call().chatClientResponse();
            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(6, name, false, "groundingViolation should be true when unknown entities referenced");
            }
            String outputText = res2.chatResponse().getResult().getOutput().getText();
            if (!outputText.startsWith("[UNGROUNDED_ENTITY_DETECTED] ")) {
                return new ScenarioResult(6, name, false,
                        "Ungrounded response must start with '[UNGROUNDED_ENTITY_DETECTED] ', got: " + outputText);
            }
            Object ungrounded = res2.context().get(ContextKeys.UNGROUNDED_ENTITIES);
            if (!(ungrounded instanceof Set<?> ungroundedSet) ||
                    !ungroundedSet.contains("host-web-99") || !ungroundedSet.contains("cluster-rogue")) {
                return new ScenarioResult(6, name, false,
                        "Ungrounded entities context should contain 'host-web-99' and 'cluster-rogue', got: " + ungrounded);
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "TraceContextPropagationAdvisor (W3C Distributed Traceparent Propagation)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            TraceContextPropagationAdvisor advisor = new TraceContextPropagationAdvisor();
            if (advisor.getOrder() != 40) {
                return new ScenarioResult(7, name, false, "advisor.getOrder() must return 40, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();
            Pattern traceparentPattern = Pattern.compile("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");

            // Test 1: Generate traceparent when missing
            ChatClientResponse res1 = client.prompt().user("Trace payment flow").call().chatClientResponse();
            String trace1 = String.valueOf(res1.context().get(ContextKeys.TRACEPARENT));
            if (!traceparentPattern.matcher(trace1).matches()) {
                return new ScenarioResult(7, name, false,
                        "Generated traceparent must match W3C format '00-<32hex>-<16hex>-01', got: " + trace1);
            }
            Prompt captured1 = model.getLastPrompt();
            if (captured1 == null || captured1.getUserMessage() == null ||
                    !captured1.getUserMessage().getText().startsWith("[traceparent=" + trace1 + "] ")) {
                return new ScenarioResult(7, name, false,
                        "User prompt must be prefixed with '[traceparent=" + trace1 + "] '");
            }
            if (!Boolean.TRUE.equals(res1.context().get(ContextKeys.PROPAGATION_SUCCESS))) {
                return new ScenarioResult(7, name, false, "propagationSuccess should be true in response context");
            }

            // Test 2: Preserve existing traceparent when provided
            String existingTrace = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
            ChatClientResponse res2 = client.prompt()
                    .user("Trace downstream auth")
                    .advisors(a -> a.param(ContextKeys.TRACEPARENT, existingTrace))
                    .call()
                    .chatClientResponse();
            String trace2 = String.valueOf(res2.context().get(ContextKeys.TRACEPARENT));
            if (!existingTrace.equals(trace2)) {
                return new ScenarioResult(7, name, false,
                        "Existing traceparent must be preserved, expected " + existingTrace + " but got: " + trace2);
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "DynamicTokenQuotaAdvisor (Pre-Allocation Reservation with Transactional Rollback)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            // Fixed estimator: 50 tokens
            DynamicTokenQuotaAdvisor advisor = new DynamicTokenQuotaAdvisor(100, text -> 50);
            if (advisor.getOrder() != 50) {
                return new ScenarioResult(8, name, false, "advisor.getOrder() must return 50, got: " + advisor.getOrder());
            }

            ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

            // Test 1: Successful call reconciles actual token usage
            // Model uses prompt=30, completion=20 -> total=50 tokens
            model.setTokenUsage(30, 20);
            ChatClientResponse res1 = client.prompt().user("Triage load balancer").call().chatClientResponse();
            if (advisor.getRemainingQuota() != 50) {
                return new ScenarioResult(8, name, false,
                        "Remaining quota after 50 token call should be 50, got: " + advisor.getRemainingQuota());
            }
            if (!Integer.valueOf(50).equals(res1.context().get(ContextKeys.REMAINING_QUOTA))) {
                return new ScenarioResult(8, name, false,
                        "Response context remainingQuota should be 50, got: " + res1.context().get(ContextKeys.REMAINING_QUOTA));
            }

            // Test 2: Downstream failure rolls back pre-allocated estimate
            model.failWith(new RuntimeException("Simulated network timeout"));
            boolean threw = false;
            try {
                client.prompt().user("Query telemetry").call().content();
            } catch (RuntimeException re) {
                threw = true;
            }
            if (!threw) {
                return new ScenarioResult(8, name, false, "Call should have thrown exception");
            }
            // Quota MUST be restored to 50 (refunded 50 tokens)
            if (advisor.getRemainingQuota() != 50) {
                return new ScenarioResult(8, name, false,
                        "Failed call must rollback pre-allocation! Remaining quota should be 50, got: " + advisor.getRemainingQuota());
            }

            // Test 3: Insufficient quota throws BudgetExceededException
            DynamicTokenQuotaAdvisor smallAdvisor = new DynamicTokenQuotaAdvisor(20, text -> 50);
            ChatClient smallClient = ChatClient.builder(model).defaultAdvisors(smallAdvisor).build();
            boolean budgetExceeded = false;
            try {
                smallClient.prompt().user("Query large report").call().content();
            } catch (BudgetExceededException bee) {
                String msg = bee.getMessage() != null ? bee.getMessage().toLowerCase() : "";
                if (!msg.contains("insufficient token quota")) {
                    return new ScenarioResult(8, name, false,
                            "BudgetExceededException message must contain 'Insufficient token quota', got: " + bee.getMessage());
                }
                budgetExceeded = true;
            }
            if (!budgetExceeded) {
                return new ScenarioResult(8, name, false,
                        "Should throw BudgetExceededException when remaining quota is less than estimated tokens");
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "CompositeAdvisorChainShortCircuit (Order-Based Short-Circuiting Pipeline)";
        try {
            FakeFailoverChatModel model = new FakeFailoverChatModel();
            AtomicLong clock = new AtomicLong(1000L);
            SlidingWindowRateLimiterAdvisor rateLimiter = new SlidingWindowRateLimiterAdvisor(1, 10000L, clock::get);
            DynamicTokenQuotaAdvisor quotaAdvisor = new DynamicTokenQuotaAdvisor(200, text -> 50);

            ChatClient client = ResilientGuardrailAdvisorsUnderTest.createShortCircuitingClient(model, rateLimiter, quotaAdvisor);

            // Call 1: Succeeds
            client.prompt().user("First allowed call").call().content();
            if (model.getCallCount() != 1) {
                return new ScenarioResult(9, name, false, "First call should invoke model once, got: " + model.getCallCount());
            }
            int quotaAfterCall1 = quotaAdvisor.getRemainingQuota();

            // Call 2: Rate limiter short-circuits pipeline
            boolean shortCircuited = false;
            try {
                client.prompt().user("Second blocked call").call().content();
            } catch (RateLimitExceededException rle) {
                shortCircuited = true;
            }
            if (!shortCircuited) {
                return new ScenarioResult(9, name, false, "Second call should be short-circuited by RateLimiter");
            }

            // Downstream model call count must STILL be 1
            if (model.getCallCount() != 1) {
                return new ScenarioResult(9, name, false,
                        "Downstream model must not be called when short-circuited, expected count 1, got: " + model.getCallCount());
            }

            // Downstream quota advisor must NOT have deducted quota
            if (quotaAdvisor.getRemainingQuota() != quotaAfterCall1) {
                return new ScenarioResult(9, name, false,
                        "Downstream quota advisor must not execute when short-circuited! Expected quota " + quotaAfterCall1 + ", got: " + quotaAdvisor.getRemainingQuota());
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "AutonomousResilientRemediationStack (Production Multi-Guardrail Integration)";
        try {
            FakeFailoverChatModel primaryModel = new FakeFailoverChatModel();
            FakeFailoverChatModel fallbackModel = new FakeFailoverChatModel();
            Set<String> allowedEntities = Set.of("cluster-alpha", "cluster-beta", "host-web-01");

            ChatClient stackClient = ResilientGuardrailAdvisorsUnderTest.createResilientProductionStack(
                    primaryModel,
                    fallbackModel,
                    5,       // rateLimitMax
                    60000L,  // windowMs
                    500,     // initialTokenQuota
                    allowedEntities
            );

            // Test 1: Normal flow through stack
            primaryModel.clear();
            primaryModel.enqueue("Remediation complete on host-web-01");
            ChatClientResponse res1 = stackClient.prompt()
                    .user("Remediate web tier")
                    .advisors(a -> a.param(ContextKeys.EXECUTION_MODE, "DRY_RUN"))
                    .call()
                    .chatClientResponse();

            if (!Boolean.TRUE.equals(res1.context().get(ContextKeys.AUDIT_DRY_RUN))) {
                return new ScenarioResult(10, name, false, "Stack must propagate audit.dryRun");
            }
            if (res1.context().get(ContextKeys.TRACEPARENT) == null) {
                return new ScenarioResult(10, name, false, "Stack must generate and propagate traceparent");
            }
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(10, name, false, "failoverTriggered should be false under normal execution");
            }
            if (!Boolean.FALSE.equals(res1.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(10, name, false, "groundingViolation should be false for valid host");
            }

            // Test 2: Failover + Grounding violation in fallback response
            primaryModel.clear();
            fallbackModel.clear();
            primaryModel.failWith(new DownstreamModelFailureException("Primary LLM connection reset"));
            fallbackModel.enqueue("Fallback recommendation targeting host-rogue-66 on cluster-delta");

            ChatClientResponse res2 = stackClient.prompt()
                    .user("Execute emergency failover action")
                    .call()
                    .chatClientResponse();

            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.FAILOVER_TRIGGERED))) {
                return new ScenarioResult(10, name, false, "Stack must trigger failover when primary fails");
            }
            if (!Boolean.TRUE.equals(res2.context().get(ContextKeys.GROUNDING_VIOLATION))) {
                return new ScenarioResult(10, name, false, "Stack grounding validator must inspect fallback output");
            }
            String res2Text = res2.chatResponse().getResult().getOutput().getText();
            if (!res2Text.startsWith("[UNGROUNDED_ENTITY_DETECTED] ")) {
                return new ScenarioResult(10, name, false,
                        "Ungrounded fallback response must be prefixed with '[UNGROUNDED_ENTITY_DETECTED] ', got: " + res2Text);
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("PHASE 04 EXERCISE 02: RESILIENT GUARDRAIL ADVISORS VERIFICATION REPORT");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String status = r.passed() ? "PASS" : "FAIL";
            if (r.passed()) passed++;
            System.out.printf("[%s] Scenario %02d: %s%n", status, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("       Detail: %s%n", r.errorDetail());
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Total: %d | Passed: %d | Failed: %d%n", results.size(), passed, results.size() - passed);
        System.out.println("===============================================================================");
    }
}
