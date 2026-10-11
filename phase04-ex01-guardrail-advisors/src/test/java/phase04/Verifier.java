package phase04;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import phase04.AdvisorContracts.*;
import phase04.GuardrailAdvisorsUnderTest.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 04 Exercise 01:
 * Guardrails, Advisors, and Token Budget Circuit-Breakers (15 Repetitive Drills).
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
        // Topic 1: Pre-Call Security & Policy Injections
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Observation, Auditing & Context Propagation
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Token Accumulation & Budget Circuit Breakers
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Post-Call Sanitization & Latency Bounds
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Multi-Advisor Composition & Production Stacks
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Pre-Call Security & Policy Injections (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "KeywordGuardrailAdvisor (Pre-Call Prompt Security)";
        List<String> hints = List.of(
                "Inspect every message in request.prompt().getInstructions().",
                "Check if message text contains any blocked keyword (case-insensitive).",
                "Throw PromptSecurityException containing 'Blocked keyword' if found."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            List<String> blocked = List.of("DROP TABLE", "DELETE FROM", "rm -rf", "eval(");
            KeywordGuardrailAdvisor advisor = new KeywordGuardrailAdvisor(blocked);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            String safe = client.prompt().user("SELECT * FROM metrics WHERE id = 10").call().content();
            if (safe == null || safe.isBlank()) {
                return new ScenarioResult(1, name, false, "Safe prompt failed unexpectedly.", hints);
            }

            try {
                client.prompt().user("Emergency mitigation: DROP TABLE staging_transactions").call().content();
                return new ScenarioResult(1, name, false, "Dangerous prompt did not throw PromptSecurityException.", hints);
            } catch (PromptSecurityException pse) {
                if (pse.getMessage() == null || (!pse.getMessage().toLowerCase().contains("blocked keyword") && !pse.getMessage().toLowerCase().contains("drop table"))) {
                    return new ScenarioResult(1, name, false, "PromptSecurityException error message must mention 'Blocked keyword'. Got: " + pse.getMessage(), hints);
                }
            }

            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(1, name, false, "Blocked prompt must NOT reach ChatModel. Expected model calls: 1, got: " + fakeModel.getCallCount(), hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "RegexPatternGuardrailAdvisor (Pre-Call Pattern Blocking)";
        List<String> hints = List.of(
                "Compile regex patterns in constructor: List<Pattern>.",
                "In adviseCall(), test each instruction message text against patterns.",
                "Throw PromptSecurityException containing 'Pattern violation' if any pattern matches."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            List<String> patterns = List.of("(?i).*(;|&&|\\|\\|)\\s*rm\\s+-rf.*", "(?i).*SELECT\\s+.*\\s+FROM\\s+passwords.*");
            RegexPatternGuardrailAdvisor advisor = new RegexPatternGuardrailAdvisor(patterns);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            String safe = client.prompt().user("SELECT name FROM users WHERE id = 1").call().content();
            if (safe == null || safe.isBlank()) {
                return new ScenarioResult(2, name, false, "Safe prompt failed unexpectedly.", hints);
            }

            try {
                client.prompt().user("Check status ; rm -rf /var/logs").call().content();
                return new ScenarioResult(2, name, false, "Injected command did not throw PromptSecurityException.", hints);
            } catch (PromptSecurityException pse) {
                if (pse.getMessage() == null || !pse.getMessage().toLowerCase().contains("pattern violation")) {
                    return new ScenarioResult(2, name, false, "Exception message must contain 'Pattern violation', got: " + pse.getMessage(), hints);
                }
            }

            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(2, name, false, "Blocked regex prompt must not reach ChatModel. Got calls: " + fakeModel.getCallCount(), hints);
            }

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "SystemPromptEnforcingAdvisor (Pre-Call Policy Injection)";
        List<String> hints = List.of(
                "Check if any message in request.prompt().getInstructions() contains enforcedPolicy.",
                "If not present, prepend new SystemMessage(enforcedPolicy) at index 0.",
                "Mutate request with updated Prompt: request.mutate().prompt(new Prompt(updatedInstructions, options)).build()."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            String policy = "MANDATORY COMPLIANCE: All operations must be idempotent and preserve audit integrity.";
            SystemPromptEnforcingAdvisor advisor = new SystemPromptEnforcingAdvisor(policy);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            client.prompt().user("Analyze slow queries in database").call().content();

            if (fakeModel.getLastPrompt() == null) {
                return new ScenarioResult(3, name, false, "No prompt was dispatched to the underlying model.", hints);
            }

            boolean policyFound = fakeModel.getLastPrompt().getInstructions().stream()
                    .anyMatch(msg -> msg.getMessageType() == MessageType.SYSTEM && msg.getText().contains(policy));

            if (!policyFound) {
                return new ScenarioResult(3, name, false, "Enforced system policy was not injected into the prompt.", hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Observation, Auditing & Context Propagation (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "AuditLoggingAdvisor (Pre/Post-Call Context & Audit Trail)";
        List<String> hints = List.of(
                "Generate UUID correlationId and enrich request context: request.mutate().context('correlationId', correlationId).build().",
                "Measure execution latency around chain.nextCall(mutatedRequest).",
                "Record AdvisorAuditRecord(getName(), promptText, responseText, correlationId, latencyMs, System.currentTimeMillis())."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            AuditLoggingAdvisor advisor = new AuditLoggingAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            String response = client.prompt().user("Triage payment timeout in order-service").call().content();
            if (response == null || response.isBlank()) {
                return new ScenarioResult(4, name, false, "ChatClient prompt execution returned null or blank response.", hints);
            }

            List<AdvisorAuditRecord> records = advisor.getAuditRecords();
            if (records == null || records.size() != 1) {
                return new ScenarioResult(4, name, false, "Expected exactly 1 audit record, got: " + (records != null ? records.size() : "null"), hints);
            }

            AdvisorAuditRecord record = records.get(0);
            if (record.correlationId() == null || record.correlationId().isBlank()) {
                return new ScenarioResult(4, name, false, "correlationId must not be null or blank.", hints);
            }
            if (!record.promptContent().contains("Triage payment timeout")) {
                return new ScenarioResult(4, name, false, "Audit record did not capture user prompt content: " + record.promptContent(), hints);
            }
            if (record.latencyMs() < 0) {
                return new ScenarioResult(4, name, false, "Audit record reported negative latency: " + record.latencyMs(), hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "OrderTrackingAdvisor (Advisor Precedence & Onion Order)";
        List<String> hints = List.of(
                "In adviseCall(), record new ExecutionOrderTrace(advisorName, 'PRE_CALL', timestamp).",
                "Execute chain.nextCall(request).",
                "Record new ExecutionOrderTrace(advisorName, 'POST_CALL', timestamp) after the call."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            List<ExecutionOrderTrace> traces = new ArrayList<>();

            OrderTrackingAdvisor advisorA = new OrderTrackingAdvisor("AdvisorA", 10, traces);
            OrderTrackingAdvisor advisorB = new OrderTrackingAdvisor("AdvisorB", 20, traces);

            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisorB, advisorA).build();
            client.prompt().user("Verify onion ordering").call().content();

            if (traces.size() != 4) {
                return new ScenarioResult(5, name, false, "Expected 4 order traces, got: " + traces.size(), hints);
            }
            if (!"AdvisorA".equals(traces.get(0).advisorName()) || !"PRE_CALL".equals(traces.get(0).phase())) {
                return new ScenarioResult(5, name, false, "Trace 0 must be AdvisorA PRE_CALL. Got: " + traces.get(0), hints);
            }
            if (!"AdvisorB".equals(traces.get(1).advisorName()) || !"PRE_CALL".equals(traces.get(1).phase())) {
                return new ScenarioResult(5, name, false, "Trace 1 must be AdvisorB PRE_CALL. Got: " + traces.get(1), hints);
            }
            if (!"AdvisorB".equals(traces.get(2).advisorName()) || !"POST_CALL".equals(traces.get(2).phase())) {
                return new ScenarioResult(5, name, false, "Trace 2 must be AdvisorB POST_CALL. Got: " + traces.get(2), hints);
            }
            if (!"AdvisorA".equals(traces.get(3).advisorName()) || !"POST_CALL".equals(traces.get(3).phase())) {
                return new ScenarioResult(5, name, false, "Trace 3 must be AdvisorA POST_CALL. Got: " + traces.get(3), hints);
            }

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "ContextEnrichmentAdvisor (Metadata Context Propagation)";
        List<String> hints = List.of(
                "Take a Map<String, Object> metadata in constructor.",
                "In adviseCall(), merge metadata into request context using request.mutate().context(metadata).build().",
                "Return chain.nextCall(mutatedRequest)."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            Map<String, Object> metadata = Map.of("tenantId", "sre-prod-01", "environment", "production");
            ContextEnrichmentAdvisor advisor = new ContextEnrichmentAdvisor(metadata);

            AuditLoggingAdvisor downstreamSpy = new AuditLoggingAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor, downstreamSpy).build();

            client.prompt().user("Enriched query").call().content();

            if (advisor.getInjectedCount() != 1) {
                return new ScenarioResult(6, name, false, "Expected injectedCount 1, got: " + advisor.getInjectedCount(), hints);
            }
            if (!advisor.getLastEnrichedContext().containsKey("tenantId") || !"sre-prod-01".equals(advisor.getLastEnrichedContext().get("tenantId"))) {
                return new ScenarioResult(6, name, false, "Context metadata tenantId mismatch: " + advisor.getLastEnrichedContext(), hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Token Accumulation & Budget Circuit Breakers (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "TokenBudgetAdvisor (Post-Call Usage Accumulator)";
        List<String> hints = List.of(
                "Extract Usage from response.chatResponse().getMetadata().getUsage().",
                "Defensively handle null usage, getPromptTokens(), and getCompletionTokens().",
                "Accumulate promptTokens, completionTokens, totalTokens, and callCount."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            TokenBudgetAdvisor advisor = new TokenBudgetAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.setTokenUsage(150, 50);
            client.prompt().user("Step 1").call().content();

            fakeModel.setTokenUsage(200, 100);
            client.prompt().user("Step 2").call().content();

            fakeModel.setTokenUsage(null, null);
            client.prompt().user("Step 3").call().content();

            TokenUsageSummary summary = advisor.getSummary();
            if (summary == null || summary.callCount() != 3 || summary.totalTokens() != 500) {
                return new ScenarioResult(7, name, false, "Summary mismatch: " + summary, hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "CostCircuitBreakerAdvisor (Post-Call Dollar Cap)";
        List<String> hints = List.of(
                "Calculate cost: (pt * priceInPerMtok + ct * priceOutPerMtok) / 1_000_000.0.",
                "Accumulate cumulativeCostUsd.",
                "Throw BudgetExceededException containing 'Budget limit exceeded' when cumulativeCostUsd > config.maxCostUsd()."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            CostBudgetConfig config = new CostBudgetConfig(0.0050, 2.50, 10.00);
            CostCircuitBreakerAdvisor advisor = new CostCircuitBreakerAdvisor(config);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.setTokenUsage(500, 200);
            client.prompt().user("Normal inspection turn").call().content();

            if (advisor.getCumulativeCostUsd() <= 0.0) {
                return new ScenarioResult(8, name, false, "Cumulative cost not tracked", hints);
            }

            try {
                fakeModel.setTokenUsage(600, 300);
                client.prompt().user("Heavy remediation turn").call().content();
                return new ScenarioResult(8, name, false, "Cost exceeding maxCostUsd did not throw BudgetExceededException.", hints);
            } catch (BudgetExceededException bee) {
                if (bee.getMessage() == null || (!bee.getMessage().toLowerCase().contains("budget limit exceeded") && !bee.getMessage().toLowerCase().contains("budget"))) {
                    return new ScenarioResult(8, name, false, "BudgetExceededException message must mention 'Budget limit exceeded'. Got: " + bee.getMessage(), hints);
                }
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "MaxTurnsAdvisor (Conversation Session Turn Limiter)";
        List<String> hints = List.of(
                "Track turnCount in atomic or synchronized counter.",
                "Increment turnCount on every adviseCall().",
                "If turnCount > maxTurnsAllowed, throw BudgetExceededException containing 'Maximum conversation turns exceeded'."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            MaxTurnsAdvisor advisor = new MaxTurnsAdvisor(2);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            client.prompt().user("Turn 1").call().content();
            client.prompt().user("Turn 2").call().content();

            if (advisor.getTurnCount() != 2) {
                return new ScenarioResult(9, name, false, "Expected turnCount 2, got: " + advisor.getTurnCount(), hints);
            }

            boolean caught = false;
            try {
                client.prompt().user("Turn 3 - exceeds max").call().content();
            } catch (BudgetExceededException bee) {
                caught = true;
                if (!bee.getMessage().toLowerCase().contains("maximum conversation turns exceeded")) {
                    return new ScenarioResult(9, name, false, "Exception message must contain 'Maximum conversation turns exceeded', got: " + bee.getMessage(), hints);
                }
            }
            if (!caught) {
                return new ScenarioResult(9, name, false, "Expected BudgetExceededException on turn 3 exceeding limit", hints);
            }

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Post-Call Sanitization & Latency Bounds (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "LatencyGuardrailAdvisor (Execution Wall-Clock Timeout)";
        List<String> hints = List.of(
                "Measure elapsed = System.currentTimeMillis() - start around chain.nextCall(request).",
                "If elapsed > maxLatencyMs, throw LatencyTimeoutException.",
                "Exception message must mention 'latency' or 'exceeded'."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            LatencyGuardrailAdvisor advisor = new LatencyGuardrailAdvisor(100L);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.setArtificialDelayMs(10L);
            client.prompt().user("Fast query").call().content();

            fakeModel.setArtificialDelayMs(180L);
            try {
                client.prompt().user("Stuck query").call().content();
                return new ScenarioResult(10, name, false, "Call exceeding 100ms did not throw LatencyTimeoutException.", hints);
            } catch (LatencyTimeoutException lte) {
                if (lte.getMessage() == null || (!lte.getMessage().toLowerCase().contains("latency") && !lte.getMessage().toLowerCase().contains("exceeded"))) {
                    return new ScenarioResult(10, name, false, "LatencyTimeoutException message must mention 'latency'. Got: " + lte.getMessage(), hints);
                }
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "PiiMaskingAdvisor (Post-Call Response Redaction)";
        List<String> hints = List.of(
                "Use regex patterns to find emails and SSNs.",
                "Replace matches with [REDACTED_EMAIL] and [REDACTED_SSN].",
                "Mutate response: response.mutate().chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage(masked))), metadata)).build()."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            PiiMaskingAdvisor advisor = new PiiMaskingAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.enqueue("Contact engineer alice.smith@example.org or bob@corp.internal, SSN is 123-45-6789.");
            String response = client.prompt().user("Who was on-call?").call().content();

            if (response == null || response.contains("alice.smith@example.org") || response.contains("bob@corp.internal") || response.contains("123-45-6789")) {
                return new ScenarioResult(11, name, false, "PII was not redacted from response: " + response, hints);
            }
            if (!response.contains("[REDACTED_EMAIL]") || !response.contains("[REDACTED_SSN]")) {
                return new ScenarioResult(11, name, false, "Expected [REDACTED_EMAIL] and [REDACTED_SSN] tokens in output. Got: " + response, hints);
            }

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(11, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "SecretKeyRedactionAdvisor (API Key & Token Redaction)";
        List<String> hints = List.of(
                "Target secrets with regex: 'Bearer\\s+[A-Za-z0-9_\\-\\.]{20,}' and 'AKIA[0-9A-Z]{16}'.",
                "Replace secrets with '[REDACTED_SECRET]'.",
                "Mutate and return response with redacted AssistantMessage."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            SecretKeyRedactionAdvisor advisor = new SecretKeyRedactionAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.enqueue("Use credentials: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 and AWS_KEY=AKIAIOSFODNN7EXAMPLE");
            String response = client.prompt().user("Fetch credentials").call().content();

            if (response == null || response.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9") || response.contains("AKIAIOSFODNN7EXAMPLE")) {
                return new ScenarioResult(12, name, false, "Secret tokens leaked in output: " + response, hints);
            }
            if (!response.contains("[REDACTED_SECRET]")) {
                return new ScenarioResult(12, name, false, "Expected [REDACTED_SECRET] replacement in output: " + response, hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(12, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Multi-Advisor Composition & Production Stacks (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "ChatClientAdvisorRegistration (Default & Call-Level Advisors)";
        List<String> hints = List.of(
                "Use factory.createGuardedChatClient(chatModel, defaultAdvisor).",
                "Invoke client.prompt().user(...).advisors(callLevelAdvisor).call().content().",
                "Verify both default and call-level advisors record audit entries."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            AuditLoggingAdvisor defaultAdvisor = new AuditLoggingAdvisor();
            AuditLoggingAdvisor callLevelAdvisor = new AuditLoggingAdvisor();

            ChatClient client = new GuardrailAdvisorsUnderTest().createGuardedChatClient(fakeModel, defaultAdvisor);

            String resp = client.prompt()
                    .user("Diagnose cache miss surge")
                    .advisors(callLevelAdvisor)
                    .call()
                    .content();

            if (resp == null || resp.isBlank()) {
                return new ScenarioResult(13, name, false, "Prompt returned null or blank response.", hints);
            }
            if (defaultAdvisor.getAuditRecords().size() != 1 || callLevelAdvisor.getAuditRecords().size() != 1) {
                return new ScenarioResult(13, name, false, "Advisors did not capture call records", hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(13, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "DynamicBypassGuardrailAdvisor (Role-Based Guardrail Bypass)";
        List<String> hints = List.of(
                "In adviseCall(), check if request.context().get('role') equals 'ADMIN'.",
                "If role is 'ADMIN', bypass inner advisor: return chain.nextCall(request).",
                "Otherwise, delegate to innerAdvisor.adviseCall(request, chain)."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            KeywordGuardrailAdvisor inner = new KeywordGuardrailAdvisor(List.of("DROP TABLE"));
            DynamicBypassGuardrailAdvisor bypassAdvisor = new DynamicBypassGuardrailAdvisor(inner);

            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(bypassAdvisor).build();

            // 1. Regular user gets blocked
            boolean blocked = false;
            try {
                client.prompt().user("DROP TABLE users").call().content();
            } catch (PromptSecurityException pse) {
                blocked = true;
            }
            if (!blocked) {
                return new ScenarioResult(14, name, false, "Regular user prompt should be blocked by inner guardrail", hints);
            }

            // 2. Admin user with context role bypasses guardrail
            String adminResult = client.prompt()
                    .user("DROP TABLE users")
                    .advisors(new ContextEnrichmentAdvisor(Map.of("role", "ADMIN")))
                    .call()
                    .content();

            if (adminResult == null || adminResult.isBlank()) {
                return new ScenarioResult(14, name, false, "Admin user prompt should bypass guardrail and execute successfully", hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(14, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "ComprehensiveSagaGuardrailSuite (Integrated Multi-Guardrail Stack)";
        List<String> hints = List.of(
                "Build ChatClient with ordered advisor pipeline.",
                "Pipeline: KeywordGuardrail, SystemPromptEnforcing, TokenBudget, PiiMasking, AuditLogging.",
                "Ensure blocked prompts fail fast without reaching model."
        );
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            GuardrailAdvisorsUnderTest factory = new GuardrailAdvisorsUnderTest();

            List<String> blocked = List.of("DROP TABLE", "rm -rf");
            String policy = "COMPLIANCE: strictly audited";
            CostBudgetConfig budget = new CostBudgetConfig(0.010, 2.0, 5.0);
            TokenBudgetAdvisor tokenAdvisor = new TokenBudgetAdvisor();
            AuditLoggingAdvisor auditAdvisor = new AuditLoggingAdvisor();

            ChatClient guardedClient = factory.createProductionDiagnosticStack(
                    fakeModel,
                    blocked,
                    policy,
                    budget,
                    tokenAdvisor,
                    auditAdvisor
            );

            fakeModel.setTokenUsage(200, 100);
            fakeModel.enqueue("Resolved issue by admin admin@cluster.local with ID 987-65-4321");
            String res1 = guardedClient.prompt().user("Action step 1").call().content();

            if (res1.contains("admin@cluster.local") || res1.contains("987-65-4321")) {
                return new ScenarioResult(15, name, false, "PII leaked in integrated stack: " + res1, hints);
            }
            if (!res1.contains("[REDACTED_EMAIL]") || !res1.contains("[REDACTED_SSN]")) {
                return new ScenarioResult(15, name, false, "PII masking tokens missing: " + res1, hints);
            }

            try {
                guardedClient.prompt().user("Please run rm -rf /tmp/data").call().content();
                return new ScenarioResult(15, name, false, "Integrated stack failed to block rm -rf prompt.", hints);
            } catch (PromptSecurityException ignored) {}

            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(15, name, false, "Blocked prompt leaked to model in integrated stack.", hints);
            }
            if (tokenAdvisor.getSummary().callCount() != 1) {
                return new ScenarioResult(15, name, false, "Token counter mismatch in integrated stack: " + tokenAdvisor.getSummary(), hints);
            }
            if (auditAdvisor.getAuditRecords().size() != 1) {
                return new ScenarioResult(15, name, false, "Audit records mismatch in integrated stack: " + auditAdvisor.getAuditRecords().size(), hints);
            }

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(15, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 04 EXERCISE 01: GUARDRAIL ADVISORS & BUDGET CIRCUITS REPORT");
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
