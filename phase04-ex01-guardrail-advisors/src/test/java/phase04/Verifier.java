package phase04;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import phase04.AdvisorContracts.*;
import phase04.GuardrailAdvisorsUnderTest.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 04 Exercise 01:
 * Guardrails, Advisors, and Token Budget Circuit-Breakers (10 Scenarios).
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
        String name = "AuditLoggingAdvisor (Pre/Post-Call Context & Audit Trail)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            AuditLoggingAdvisor advisor = new AuditLoggingAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            String response = client.prompt().user("Triage payment timeout in order-service").call().content();
            if (response == null || response.isBlank()) {
                return new ScenarioResult(1, name, false, "ChatClient prompt execution returned null or blank response.");
            }

            List<AdvisorAuditRecord> records = advisor.getAuditRecords();
            if (records == null || records.size() != 1) {
                return new ScenarioResult(1, name, false, "Expected exactly 1 audit record, got: " + (records != null ? records.size() : "null"));
            }

            AdvisorAuditRecord record = records.get(0);
            if (record.correlationId() == null || record.correlationId().isBlank()) {
                return new ScenarioResult(1, name, false, "correlationId must not be null or blank.");
            }
            if (!record.promptContent().contains("Triage payment timeout")) {
                return new ScenarioResult(1, name, false, "Audit record did not capture user prompt content: " + record.promptContent());
            }
            if (record.latencyMs() < 0) {
                return new ScenarioResult(1, name, false, "Audit record reported negative latency: " + record.latencyMs());
            }

            return new ScenarioResult(1, name, true, "AuditLoggingAdvisor captured pre/post call metadata and correlationId.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception during AuditLoggingAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "KeywordGuardrailAdvisor (Pre-Call Prompt Security)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            List<String> blocked = List.of("DROP TABLE", "DELETE FROM", "rm -rf", "eval(");
            KeywordGuardrailAdvisor advisor = new KeywordGuardrailAdvisor(blocked);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            // 1. Safe prompt succeeds
            String safe = client.prompt().user("SELECT * FROM metrics WHERE id = 10").call().content();
            if (safe == null || safe.isBlank()) {
                return new ScenarioResult(2, name, false, "Safe prompt failed unexpectedly.");
            }

            // 2. Dangerous prompt blocked with PromptSecurityException
            try {
                client.prompt().user("Emergency mitigation: DROP TABLE staging_transactions").call().content();
                return new ScenarioResult(2, name, false, "Dangerous prompt with DROP TABLE did not throw PromptSecurityException.");
            } catch (PromptSecurityException pse) {
                if (pse.getMessage() == null || (!pse.getMessage().toLowerCase().contains("blocked keyword") && !pse.getMessage().toLowerCase().contains("drop table"))) {
                    return new ScenarioResult(2, name, false, "PromptSecurityException error message must mention 'Blocked keyword'. Got: " + pse.getMessage());
                }
            }

            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(2, name, false, "Blocked prompt must NOT reach ChatModel. Expected model calls: 1, got: " + fakeModel.getCallCount());
            }

            return new ScenarioResult(2, name, true, "KeywordGuardrailAdvisor safely blocked malicious prompt before model execution.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception during KeywordGuardrailAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "SystemPromptEnforcingAdvisor (Pre-Call Policy Injection)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            String policy = "MANDATORY COMPLIANCE: All operations must be idempotent and preserve audit integrity.";
            SystemPromptEnforcingAdvisor advisor = new SystemPromptEnforcingAdvisor(policy);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            client.prompt().user("Analyze slow queries in database").call().content();

            if (fakeModel.getLastPrompt() == null) {
                return new ScenarioResult(3, name, false, "No prompt was dispatched to the underlying model.");
            }

            boolean policyFound = fakeModel.getLastPrompt().getInstructions().stream()
                    .anyMatch(msg -> msg.getMessageType() == MessageType.SYSTEM && msg.getText().contains(policy));

            if (!policyFound) {
                return new ScenarioResult(3, name, false, "Enforced system policy was not injected into the prompt. Found messages: " + fakeModel.getLastPrompt().getInstructions());
            }

            return new ScenarioResult(3, name, true, "SystemPromptEnforcingAdvisor injected mandatory policy into prompt before model dispatch.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception during SystemPromptEnforcingAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "TokenBudgetAdvisor (Post-Call Usage Accumulator)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            TokenBudgetAdvisor advisor = new TokenBudgetAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.setTokenUsage(150, 50); // call 1: 200 total
            client.prompt().user("Step 1").call().content();

            fakeModel.setTokenUsage(200, 100); // call 2: 300 total
            client.prompt().user("Step 2").call().content();

            fakeModel.setTokenUsage(null, null); // call 3: defensive null test
            client.prompt().user("Step 3").call().content();

            TokenUsageSummary summary = advisor.getSummary();
            if (summary == null) {
                return new ScenarioResult(4, name, false, "getSummary() returned null.");
            }
            if (summary.callCount() != 3) {
                return new ScenarioResult(4, name, false, "Expected callCount == 3, got: " + summary.callCount());
            }
            if (summary.promptTokens() != 350) {
                return new ScenarioResult(4, name, false, "Expected cumulative promptTokens == 350, got: " + summary.promptTokens());
            }
            if (summary.completionTokens() != 150) {
                return new ScenarioResult(4, name, false, "Expected cumulative completionTokens == 150, got: " + summary.completionTokens());
            }
            if (summary.totalTokens() != 500) {
                return new ScenarioResult(4, name, false, "Expected cumulative totalTokens == 500, got: " + summary.totalTokens());
            }

            return new ScenarioResult(4, name, true, "TokenBudgetAdvisor cleanly accumulated token usage across calls with defensive null unboxing.");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception during TokenBudgetAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "CostCircuitBreakerAdvisor (Post-Call Dollar Cap)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            // $2.50 per Mtok prompt, $10.00 per Mtok completion. Cap = $0.0050 (0.50 cents)
            CostBudgetConfig config = new CostBudgetConfig(0.0050, 2.50, 10.00);
            CostCircuitBreakerAdvisor advisor = new CostCircuitBreakerAdvisor(config);
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            // Call 1: 500 in, 200 out -> (500*2.5 + 200*10)/1M = (1250 + 2000)/1M = $0.00325 (Below $0.0050)
            fakeModel.setTokenUsage(500, 200);
            client.prompt().user("Normal inspection turn").call().content();

            if (advisor.getCumulativeCostUsd() <= 0.0) {
                return new ScenarioResult(5, name, false, "advisor did not track cumulative cost.");
            }

            // Call 2: 600 in, 300 out -> (1500 + 3000)/1M = $0.0045 -> Total $0.00775 (Breaches $0.0050 cap!)
            try {
                fakeModel.setTokenUsage(600, 300);
                client.prompt().user("Heavy remediation turn").call().content();
                return new ScenarioResult(5, name, false, "Cost exceeding maxCostUsd did not throw BudgetExceededException.");
            } catch (BudgetExceededException bee) {
                if (bee.getMessage() == null || (!bee.getMessage().toLowerCase().contains("budget limit exceeded") && !bee.getMessage().toLowerCase().contains("budget"))) {
                    return new ScenarioResult(5, name, false, "BudgetExceededException message must mention 'Budget limit exceeded'. Got: " + bee.getMessage());
                }
            }

            return new ScenarioResult(5, name, true, "CostCircuitBreakerAdvisor enforced dollar budget cap and tripped circuit-breaker.");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception during CostCircuitBreakerAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "LatencyGuardrailAdvisor (Execution Wall-Clock Timeout)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            LatencyGuardrailAdvisor advisor = new LatencyGuardrailAdvisor(100L); // 100ms cap
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            // Call 1: Fast call (10ms) -> passes
            fakeModel.setArtificialDelayMs(10L);
            client.prompt().user("Fast query").call().content();

            // Call 2: Slow call (180ms) -> trips LatencyTimeoutException
            fakeModel.setArtificialDelayMs(180L);
            try {
                client.prompt().user("Stuck query").call().content();
                return new ScenarioResult(6, name, false, "Call exceeding 100ms did not throw LatencyTimeoutException.");
            } catch (LatencyTimeoutException lte) {
                if (lte.getMessage() == null || (!lte.getMessage().toLowerCase().contains("latency") && !lte.getMessage().toLowerCase().contains("exceeded"))) {
                    return new ScenarioResult(6, name, false, "LatencyTimeoutException message must mention 'latency'. Got: " + lte.getMessage());
                }
            }

            return new ScenarioResult(6, name, true, "LatencyGuardrailAdvisor detected slow execution and enforced wall-clock limit.");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception during LatencyGuardrailAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "PiiMaskingAdvisor (Post-Call Response Redaction)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            PiiMaskingAdvisor advisor = new PiiMaskingAdvisor();
            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisor).build();

            fakeModel.enqueue("Contact engineer alice.smith@example.org or bob@corp.internal, SSN is 123-45-6789.");
            String response = client.prompt().user("Who was on-call?").call().content();

            if (response == null || response.contains("alice.smith@example.org") || response.contains("bob@corp.internal")) {
                return new ScenarioResult(7, name, false, "PII email was not redacted from response: " + response);
            }
            if (response.contains("123-45-6789")) {
                return new ScenarioResult(7, name, false, "PII SSN was not redacted from response: " + response);
            }
            if (!response.contains("[REDACTED_EMAIL]") || !response.contains("[REDACTED_SSN]")) {
                return new ScenarioResult(7, name, false, "Expected [REDACTED_EMAIL] and [REDACTED_SSN] tokens in output. Got: " + response);
            }

            return new ScenarioResult(7, name, true, "PiiMaskingAdvisor sanitized sensitive emails and SSNs from model response.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception during PiiMaskingAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "OrderTrackingAdvisor (Advisor Precedence & Order)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            List<ExecutionOrderTrace> traces = new ArrayList<>();

            // Advisor A has order 10 (runs earlier in pre-call)
            OrderTrackingAdvisor advisorA = new OrderTrackingAdvisor("AdvisorA", 10, traces);
            // Advisor B has order 20 (runs later in pre-call)
            OrderTrackingAdvisor advisorB = new OrderTrackingAdvisor("AdvisorB", 20, traces);

            ChatClient client = ChatClient.builder(fakeModel).defaultAdvisors(advisorB, advisorA).build();
            client.prompt().user("Verify onion ordering").call().content();

            if (traces.size() != 4) {
                return new ScenarioResult(8, name, false, "Expected 4 order traces (2 pre-call, 2 post-call), got: " + traces.size());
            }

            // Expected onion order: AdvisorA (pre), AdvisorB (pre), AdvisorB (post), AdvisorA (post)
            if (!"AdvisorA".equals(traces.get(0).advisorName()) || !"PRE_CALL".equals(traces.get(0).phase())) {
                return new ScenarioResult(8, name, false, "Trace 0 must be AdvisorA PRE_CALL. Got: " + traces.get(0));
            }
            if (!"AdvisorB".equals(traces.get(1).advisorName()) || !"PRE_CALL".equals(traces.get(1).phase())) {
                return new ScenarioResult(8, name, false, "Trace 1 must be AdvisorB PRE_CALL. Got: " + traces.get(1));
            }
            if (!"AdvisorB".equals(traces.get(2).advisorName()) || !"POST_CALL".equals(traces.get(2).phase())) {
                return new ScenarioResult(8, name, false, "Trace 2 must be AdvisorB POST_CALL. Got: " + traces.get(2));
            }
            if (!"AdvisorA".equals(traces.get(3).advisorName()) || !"POST_CALL".equals(traces.get(3).phase())) {
                return new ScenarioResult(8, name, false, "Trace 3 must be AdvisorA POST_CALL. Got: " + traces.get(3));
            }

            return new ScenarioResult(8, name, true, "Advisors executed in strict onion-model order based on getOrder() precedence.");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception during OrderTrackingAdvisor verification: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "ChatClient_AdvisorRegistration (Default & Call-Level Advisors)";
        try {
            FakeAdvisorChatModel fakeModel = new FakeAdvisorChatModel();
            AuditLoggingAdvisor defaultAdvisor = new AuditLoggingAdvisor();
            AuditLoggingAdvisor callLevelAdvisor = new AuditLoggingAdvisor();

            ChatClient client = new GuardrailAdvisorsUnderTest().createGuardedChatClient(fakeModel, defaultAdvisor);

            // Execute with both default and call-level advisor
            String resp = client.prompt()
                    .user("Diagnose cache miss surge")
                    .advisors(callLevelAdvisor)
                    .call()
                    .content();

            if (resp == null || resp.isBlank()) {
                return new ScenarioResult(9, name, false, "Prompt returned null or blank response.");
            }

            if (defaultAdvisor.getAuditRecords().size() != 1) {
                return new ScenarioResult(9, name, false, "Default advisor did not capture call. Size: " + defaultAdvisor.getAuditRecords().size());
            }
            if (callLevelAdvisor.getAuditRecords().size() != 1) {
                return new ScenarioResult(9, name, false, "Call-level advisor did not capture call. Size: " + callLevelAdvisor.getAuditRecords().size());
            }

            return new ScenarioResult(9, name, true, "ChatClient cleanly executed both default and call-level advisors.");
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception during ChatClient advisor registration: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "ComprehensiveSagaGuardrailSuite (Integrated Multi-Guardrail Stack)";
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

            // Turn 1: Valid turn with PII in response
            fakeModel.setTokenUsage(200, 100);
            fakeModel.enqueue("Resolved issue by admin admin@cluster.local with ID 987-65-4321");
            String res1 = guardedClient.prompt().user("Action step 1").call().content();

            if (res1.contains("admin@cluster.local") || res1.contains("987-65-4321")) {
                return new ScenarioResult(10, name, false, "PII leaked in integrated stack: " + res1);
            }
            if (!res1.contains("[REDACTED_EMAIL]") || !res1.contains("[REDACTED_SSN]")) {
                return new ScenarioResult(10, name, false, "PII masking tokens missing: " + res1);
            }

            // Turn 2: Malicious prompt blocked by KeywordGuardrail without touching model
            try {
                guardedClient.prompt().user("Please run rm -rf /tmp/data").call().content();
                return new ScenarioResult(10, name, false, "Integrated stack failed to block rm -rf prompt.");
            } catch (PromptSecurityException ignored) {}

            // Model should have received only 1 call
            if (fakeModel.getCallCount() != 1) {
                return new ScenarioResult(10, name, false, "Blocked prompt leaked to model in integrated stack.");
            }

            if (tokenAdvisor.getSummary().callCount() != 1) {
                return new ScenarioResult(10, name, false, "Token counter mismatch in integrated stack: " + tokenAdvisor.getSummary());
            }

            if (auditAdvisor.getAuditRecords().size() != 1) {
                return new ScenarioResult(10, name, false, "Audit records mismatch in integrated stack: " + auditAdvisor.getAuditRecords().size());
            }

            return new ScenarioResult(10, name, true, "Full production guardrail stack enforced security, policy, budget, PII, and audit invariants.");
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception during ComprehensiveSagaGuardrailSuite verification: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("                 PHASE 04 EXERCISE 01: VERIFICATION REPORT             ");
        System.out.println("======================================================================");
        int passed = 0;
        int failed = 0;

        for (ScenarioResult r : results) {
            System.out.printf("SCENARIO %2d: %s%n", r.scenarioNumber(), r.name());
            if (r.passed()) {
                System.out.println("  [PASS] " + r.errorDetail());
                passed++;
            } else {
                System.out.println("  [FAIL] " + r.errorDetail());
                failed++;
            }
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.printf("SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
        if (failed == 0) {
            System.out.println("VERDICT: ALL SCENARIOS PASSED (exit code 0)");
        } else {
            System.out.println("VERDICT: FINDINGS DETECTED (exit code 99)");
        }
        System.out.println("======================================================================");
        System.out.println();
    }
}
