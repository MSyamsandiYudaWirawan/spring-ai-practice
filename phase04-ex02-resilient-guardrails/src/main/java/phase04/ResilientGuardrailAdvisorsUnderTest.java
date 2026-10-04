package phase04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice implementing resilient Spring AI CallAdvisor guardrails, failover routing,
 * schema healing, and transactional token quota circuit-breakers:
 * 1. RuntimeContextAdvisor (Order 10)
 * 2. SlidingWindowTruncationAdvisor (Order 20)
 * 3. SlidingWindowRateLimiterAdvisor (Order 30)
 * 4. ModelFailoverAdvisor (Order 80)
 * 5. SchemaSelfHealingAdvisor (Order 70)
 * 6. GroundingValidationAdvisor (Order 60)
 * 7. TraceContextPropagationAdvisor (Order 40)
 * 8. DynamicTokenQuotaAdvisor (Order 50)
 * 9. createShortCircuitingClient
 * 10. createResilientProductionStack
 */
public class ResilientGuardrailAdvisorsUnderTest {

    /**
     * Scenario 1: Pre/Post-Call Dynamic Execution Mode Context Injection.
     * <p>
     * Instructions:
     * - Inspect request context for key "executionMode" (defaults to "LIVE" if absent or null).
     * - If "DRY_RUN" (case-insensitive):
     *     - Prepend "[DRY_RUN] " to the UserMessage text in prompt instructions.
     *     - Mutate request context: set "audit.dryRun" = true, "processedBy" = "RuntimeContextAdvisor".
     *     - Execute chain.nextCall(mutatedRequest).
     *     - Mutate response context: ensure "audit.dryRun" = true and "processedBy" = "RuntimeContextAdvisor".
     * - If not "DRY_RUN" (e.g. "LIVE"):
     *     - Mutate request context: set "audit.dryRun" = false, "processedBy" = "RuntimeContextAdvisor".
     *     - Execute chain.nextCall(mutatedRequest).
     *     - Mutate response context: ensure "audit.dryRun" = false and "processedBy" = "RuntimeContextAdvisor".
     * - getOrder() returns 10.
     * - getName() returns "RuntimeContextAdvisor".
     */
    public static class RuntimeContextAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 1): Bypasses executionMode inspection, prompt prefixing, and context enrichment
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 10
            return 0;
        }

        @Override
        public String getName() {
            return "RuntimeContextAdvisor";
        }
    }

    /**
     * Scenario 2: Conversation History Sliding Window Truncation with Pinned SystemMessage.
     * <p>
     * Instructions:
     * - Inspect request.prompt().getInstructions().
     * - Separate messages into SystemMessage (if any) and non-system messages (UserMessage, AssistantMessage, etc.).
     * - If nonSystemMessages.size() > maxHistoryMessages:
     *     - Keep only the last maxHistoryMessages of non-system messages.
     *     - Calculate dropped non-system count: nonSystemMessages.size() - maxHistoryMessages.
     * - Else:
     *     - Keep all non-system messages, dropped count = 0.
     * - Reassemble instructions: SystemMessage(s) pinned at the start (index 0), followed by retained non-system messages.
     * - Mutate request with the reassembled Prompt.
     * - Call chain.nextCall(mutatedRequest).
     * - Mutate response context: set "truncatedMessageCount" = dropped count.
     * - getOrder() returns 20.
     * - getName() returns "SlidingWindowTruncationAdvisor".
     */
    public static class SlidingWindowTruncationAdvisor implements CallAdvisor {
        private final int maxHistoryMessages;

        public SlidingWindowTruncationAdvisor(int maxHistoryMessages) {
            this.maxHistoryMessages = maxHistoryMessages;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 2): Bypasses history truncation and SystemMessage pinning
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 20
            return 0;
        }

        @Override
        public String getName() {
            return "SlidingWindowTruncationAdvisor";
        }
    }

    /**
     * Scenario 3: Rolling Timestamp Deque Sliding Window Rate Limiter.
     * <p>
     * Instructions:
     * - Maintain an internal Deque<Long> of call timestamps.
     * - In adviseCall, get current time: long now = clock.getAsLong().
     * - Synchronized on the timestamps deque:
     *     - Evict all entries where timestamp <= (now - windowMs).
     *     - If deque.size() >= maxRequestsPerWindow:
     *         throw new RateLimitExceededException("Rate limit exceeded: maximum " + maxRequestsPerWindow + " requests allowed in " + windowMs + "ms window");
     *         (Message MUST contain "Rate limit exceeded").
     *     - Add 'now' to the deque.
     *     - Calculate remaining = maxRequestsPerWindow - deque.size().
     * - Call chain.nextCall(request).
     * - Mutate response context: set "remainingRequests" = remaining.
     * - getOrder() returns 30.
     * - getName() returns "SlidingWindowRateLimiterAdvisor".
     */
    public static class SlidingWindowRateLimiterAdvisor implements CallAdvisor {
        private final int maxRequestsPerWindow;
        private final long windowMs;
        private final LongSupplier clock;
        private final Deque<Long> callTimestamps = new ArrayDeque<>();

        public SlidingWindowRateLimiterAdvisor(int maxRequestsPerWindow, long windowMs, LongSupplier clock) {
            this.maxRequestsPerWindow = maxRequestsPerWindow;
            this.windowMs = windowMs;
            this.clock = Objects.requireNonNull(clock);
        }

        public SlidingWindowRateLimiterAdvisor(int maxRequestsPerWindow, long windowMs) {
            this(maxRequestsPerWindow, windowMs, System::currentTimeMillis);
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 3): Bypasses rate limiting check and deque eviction
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 30
            return 0;
        }

        @Override
        public String getName() {
            return "SlidingWindowRateLimiterAdvisor";
        }
    }

    /**
     * Scenario 4: Downstream Model Resilient Failover Routing.
     * <p>
     * Instructions:
     * - Try executing chain.nextCall(request).
     * - If successful:
     *     - Return response mutated with context "failoverTriggered" = false.
     * - If an Exception is caught:
     *     - Invoke fallbackModel.call(request.prompt()).
     *     - Construct ChatClientResponse with:
     *         .chatResponse(fallbackChatResponse)
     *         .context(request.context())
     *         .context("failoverTriggered", true)
     *         .context("primaryError", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
     *         .build()
     * - getOrder() returns 80.
     * - getName() returns "ModelFailoverAdvisor".
     */
    public static class ModelFailoverAdvisor implements CallAdvisor {
        private final ChatModel fallbackModel;

        public ModelFailoverAdvisor(ChatModel fallbackModel) {
            this.fallbackModel = Objects.requireNonNull(fallbackModel, "fallbackModel must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 4): Bypasses failover routing on downstream exception
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 80
            return 0;
        }

        @Override
        public String getName() {
            return "ModelFailoverAdvisor";
        }
    }

    /**
     * Scenario 5: Automated One-Shot Schema Repair Retry Loop.
     * <p>
     * Instructions:
     * - Call chain.nextCall(request).
     * - Extract response text from output.
     * - Check if output contains "\"status\": \"SUCCESS\"":
     *     - If valid: mutate response context with "schemaHealed" = false, return response.
     *     - If invalid:
     *         - Attempt at most ONE repair retry using chain.copy(this).nextCall(retryReq).
     *         - Build repair prompt instructions:
     *             - original prompt instructions
     *             - AssistantMessage with the invalid output text
     *             - UserMessage: "SCHEMA_REPAIR_NOTICE: The output was invalid and missing '\"status\": \"SUCCESS\"'. Please return compliant JSON schema."
     *         - Call chain.copy(this).nextCall(retryReq).
     *         - Check retry response text:
     *             - If retry output contains "\"status\": \"SUCCESS\"":
     *                 mutate retry response context with "schemaHealed" = true, return response.
     *             - Else:
     *                 throw new SchemaValidationException("Schema validation failed: Assistant output does not meet required schema after retry");
     *                 (Message MUST contain "Schema validation failed").
     * - getOrder() returns 70.
     * - getName() returns "SchemaSelfHealingAdvisor".
     */
    public static class SchemaSelfHealingAdvisor implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 5): Bypasses schema validation and one-shot repair retry
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 70
            return 0;
        }

        @Override
        public String getName() {
            return "SchemaSelfHealingAdvisor";
        }
    }

    /**
     * Scenario 6: Entity Grounding & Hallucination Detector.
     * <p>
     * Instructions:
     * - Call chain.nextCall(request).
     * - Extract assistant output text.
     * - Match entity tokens using entityPattern (default matches \b(host-[a-z0-9-]+|cluster-[a-z0-9-]+)\b).
     * - Any entity found that is NOT in allowedEntities is ungrounded.
     * - If ungrounded entities are detected:
     *     - Prepend "[UNGROUNDED_ENTITY_DETECTED] " to the assistant output text.
     *     - Recreate ChatResponse wrapping the updated Generation with the modified text.
     *     - Mutate response with new ChatResponse and context:
     *         "groundingViolation" = true
     *         "ungroundedEntities" = Set of ungrounded entities
     * - If all entities are grounded (or none matched):
     *     - Mutate response context: "groundingViolation" = false.
     * - getOrder() returns 60.
     * - getName() returns "GroundingValidationAdvisor".
     */
    public static class GroundingValidationAdvisor implements CallAdvisor {
        private final Set<String> allowedEntities;
        private final Pattern entityPattern;

        public GroundingValidationAdvisor(Set<String> allowedEntities, Pattern entityPattern) {
            this.allowedEntities = Objects.requireNonNull(allowedEntities);
            this.entityPattern = Objects.requireNonNull(entityPattern);
        }

        public GroundingValidationAdvisor(Set<String> allowedEntities) {
            this(allowedEntities, Pattern.compile("\\b(host-[a-z0-9-]+|cluster-[a-z0-9-]+)\\b"));
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 6): Bypasses entity grounding detection and prefix mutation
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 60
            return 0;
        }

        @Override
        public String getName() {
            return "GroundingValidationAdvisor";
        }
    }

    /**
     * Scenario 7: W3C Distributed Traceparent Propagation Advisor.
     * <p>
     * Instructions:
     * - Check request context for key "traceparent".
     * - If present and non-blank: use it.
     * - If absent or blank: generate valid W3C traceparent ("00-" + 32hex + "-" + 16hex + "-01").
     * - Inject traceparent notice by prepending "[traceparent=" + traceparent + "] " to UserMessage text.
     * - Mutate request with updated Prompt and context "traceparent" = traceparent.
     * - Call chain.nextCall(mutatedRequest).
     * - Mutate response context: set "traceparent" = traceparent, "propagationSuccess" = true.
     * - getOrder() returns 40.
     * - getName() returns "TraceContextPropagationAdvisor".
     */
    public static class TraceContextPropagationAdvisor implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 7): Bypasses traceparent generation and context propagation
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 40
            return 0;
        }

        @Override
        public String getName() {
            return "TraceContextPropagationAdvisor";
        }
    }

    /**
     * Scenario 8: Dynamic Pre-Allocation Token Quota Advisor with Transactional Rollback.
     * <p>
     * Instructions:
     * - Estimate required tokens: tokenEstimator.applyAsInt(userMessageText).
     * - In synchronized block:
     *     - If remainingQuota < estimatedTokens:
     *         throw new BudgetExceededException("Insufficient token quota: required " + estimatedTokens + ", remaining " + remainingQuota);
     *         (Message MUST contain "Insufficient token quota").
     *     - Deduct estimatedTokens: remainingQuota -= estimatedTokens.
     * - In try block:
     *     - Execute chain.nextCall(request).
     *     - Extract actual total tokens from response metadata usage (if available, else fallback to estimatedTokens).
     *     - In synchronized block: reconcile remainingQuota = remainingQuota + estimatedTokens - actualTokens.
     *     - Mutate response context: set "remainingQuota" = remainingQuota.
     * - In catch (RuntimeException ex):
     *     - In synchronized block: rollback pre-allocation: remainingQuota += estimatedTokens.
     *     - Rethrow ex.
     * - getOrder() returns 50.
     * - getName() returns "DynamicTokenQuotaAdvisor".
     */
    public static class DynamicTokenQuotaAdvisor implements CallAdvisor {
        private int remainingQuota;
        private final ToIntFunction<String> tokenEstimator;

        public DynamicTokenQuotaAdvisor(int initialQuota, ToIntFunction<String> tokenEstimator) {
            this.remainingQuota = initialQuota;
            this.tokenEstimator = Objects.requireNonNull(tokenEstimator);
        }

        public DynamicTokenQuotaAdvisor(int initialQuota) {
            this(initialQuota, text -> Math.max(10, text != null ? text.length() / 4 : 10));
        }

        public synchronized int getRemainingQuota() {
            return remainingQuota;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            // DEFECT (Scenario 8): Bypasses quota reservation and rollback
            return chain.nextCall(request);
        }

        @Override
        public int getOrder() {
            // DEFECT: Must return 50
            return 0;
        }

        @Override
        public String getName() {
            return "DynamicTokenQuotaAdvisor";
        }
    }

    /**
     * Scenario 9: Composite Short-Circuiting Client Factory.
     * <p>
     * Instructions:
     * - Assembles a ChatClient with rateLimiter and quotaAdvisor advisors.
     */
    public static ChatClient createShortCircuitingClient(ChatModel model, CallAdvisor rateLimiter, CallAdvisor quotaAdvisor) {
        // DEFECT (Scenario 9): Returns unconfigured client
        return null;
    }

    /**
     * Scenario 10: Full Autonomous Resilient Production Stack Factory.
     * <p>
     * Instructions:
     * - Combines all resilient advisors in proper onion order:
     *     - Order 10: RuntimeContextAdvisor
     *     - Order 30: SlidingWindowRateLimiterAdvisor(rateLimitMax, windowMs)
     *     - Order 40: TraceContextPropagationAdvisor
     *     - Order 50: DynamicTokenQuotaAdvisor(initialTokenQuota)
     *     - Order 60: GroundingValidationAdvisor(allowedEntities)
     *     - Order 80: ModelFailoverAdvisor(fallbackModel)
     */
    public static ChatClient createResilientProductionStack(
            ChatModel primaryModel,
            ChatModel fallbackModel,
            int rateLimitMax,
            long windowMs,
            int initialTokenQuota,
            Set<String> allowedEntities
    ) {
        // DEFECT (Scenario 10): Returns empty client without resilient advisors
        return null;
    }
}
