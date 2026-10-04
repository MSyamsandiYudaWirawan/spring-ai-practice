package phase01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 01 Exercise 02 (FinTech Fraud & Risk Operations).
 * DO NOT MODIFY THIS FILE.
 *
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
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "analyzeTransaction (Role Separation)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            String role = "You are a Level-3 AML compliance officer.";
            FraudModelContracts.Transaction tx = new FraudModelContracts.Transaction(
                    "TX-9901", "ACC-4410", 14500.00, "Global Wire Escrow", "Switzerland");
            service.analyzeTransaction(role, tx);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(1, name, false, "No prompt dispatched.");

            if (prompt.getSystemMessage() == null) {
                return new ScenarioResult(1, name, false,
                        "SystemMessage is null! Role was concatenated into UserMessage.\n" +
                        "  Fix: Use .system(complianceRole) and .user(tx.toString())");
            }

            if (!role.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "SystemMessage mismatch:\n  Expected: \"" + role + "\"\n  Actual:   \"" + prompt.getSystemMessage().getText() + "\"");
            }

            String userText = prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "";
            if (userText.contains("Role: ") || !userText.contains("TX-9901")) {
                return new ScenarioResult(1, name, false,
                        "UserMessage contains leaked 'Role: ' or missing transaction data:\n  Actual: \"" + userText + "\"");
            }

            return new ScenarioResult(1, name, true, "SystemMessage and UserMessage cleanly separated.");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "draftDisputeNotice (Map Template Binding with Special Characters)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            FraudModelContracts.DisputeNotice dispute = new FraudModelContracts.DisputeNotice(
                    "Sarah O'Connor", "DISP-882", 250.00, "USD",
                    "Unauthorized recurring card-not-present charge for $250.00 {sub_ref: 99}");
            service.draftDisputeNotice(dispute);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(2, name, false, "No UserMessage generated.");
            }

            String userText = prompt.getUserMessage().getText();
            if (userText.contains("{customerName}") || userText.contains("{disputeId}") ||
                userText.contains("{amount}") || userText.contains("{currency}") || userText.contains("{reason}")) {
                return new ScenarioResult(2, name, false,
                        "Unsubstituted placeholders found in dispute notice:\n  Actual: \"" + userText + "\"\n" +
                        "  Fix: Use .user(u -> u.text(DISPUTE_TEMPLATE).params(Map.of(...)))");
            }

            if (!userText.contains("Sarah O'Connor") || !userText.contains("DISP-882") ||
                !userText.contains("USD") || !userText.contains("250.0") || !userText.contains("$250.00 {sub_ref: 99}")) {
                return new ScenarioResult(2, name, false,
                        "Dispute template parameters corrupted or missing special characters ($ and {}):\n  Actual: \"" + userText + "\"");
            }

            return new ScenarioResult(2, name, true, "Template rendered with map parameters; special characters intact.");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "configureRegionalAuditor (Parameterized System Prompt Spec)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            String jurisdiction = "Singapore-MAS";
            String standard = "Notice-626";
            String query = "Audit cash deposits exceeding SGD 20,000";
            service.configureRegionalAuditor(jurisdiction, standard, query);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false,
                        "SystemMessage is null or missing!\n" +
                        "  Fix: Use .system(s -> s.text(REGIONAL_AUDITOR_SYSTEM_TEMPLATE).param(\"jurisdiction\", ...)...)");
            }

            String sysText = prompt.getSystemMessage().getText();
            if (sysText.contains("{jurisdiction}") || sysText.contains("{standard}")) {
                return new ScenarioResult(3, name, false,
                        "System prompt has unsubstituted placeholders:\n  Actual: \"" + sysText + "\"");
            }

            if (!sysText.contains("Singapore-MAS") || !sysText.contains("Notice-626")) {
                return new ScenarioResult(3, name, false,
                        "System prompt missing regional parameter bindings:\n  Actual: \"" + sysText + "\"");
            }

            String userText = prompt.getUserMessage() != null ? prompt.getUserMessage().getText() : "";
            if (!query.equals(userText)) {
                return new ScenarioResult(3, name, false,
                        "User query corrupted by system parameters:\n  Expected: \"" + query + "\"\n  Actual:   \"" + userText + "\"");
            }

            return new ScenarioResult(3, name, true, "Parameterized system prompt rendered cleanly via SystemSpec.");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "auditHighRiskTransfer (Call-Level Options Override)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            service.auditHighRiskTransfer("Urgent $5,000,000 transfer to offshore routing 99281");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(4, name, false,
                        "No ChatOptions attached to the high-risk prompt call.\n" +
                        "  Fix: Call .options(ChatOptions.builder().temperature(0.0).maxTokens(500))");
            }

            Double temp = prompt.getOptions().getTemperature();
            Integer maxTokens = prompt.getOptions().getMaxTokens();

            if (temp == null || Math.abs(temp - FraudModelContracts.OVERRIDE_TEMPERATURE) > 0.001) {
                return new ScenarioResult(4, name, false,
                        "Temperature override failed (inherited default instead of 0.0):\n" +
                        "  Expected: " + FraudModelContracts.OVERRIDE_TEMPERATURE + "\n  Actual:   " + temp);
            }

            if (maxTokens == null || maxTokens != FraudModelContracts.OVERRIDE_MAX_TOKENS) {
                return new ScenarioResult(4, name, false,
                        "maxTokens override failed:\n" +
                        "  Expected: " + FraudModelContracts.OVERRIDE_MAX_TOKENS + "\n  Actual:   " + maxTokens);
            }

            return new ScenarioResult(4, name, true, "Per-call options override succeeded (temp=0.0, maxTokens=500).");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "continueDisputeDialogue (Multi-Turn History Replay)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            List<Message> history = List.of(
                    new UserMessage("I noticed an incorrect $85 charge from Uber on May 12."),
                    new AssistantMessage("I can help with that. Did you take any rides on May 12?"),
                    new UserMessage("No, I was abroad with my car that entire week.")
            );
            String latest = "Here is my flight ticket confirmation as proof.";
            service.continueDisputeDialogue(history, latest);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(5, name, false, "No prompt dispatched.");

            List<Message> instructions = prompt.getInstructions();
            List<Message> nonSystem = instructions.stream()
                    .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                    .toList();

            // Expected: 3 history messages + 1 latest user message = 4 conversation messages
            if (nonSystem.size() < 4) {
                return new ScenarioResult(5, name, false,
                        "History messages were dropped! Found " + nonSystem.size() + " conversation messages, expected at least 4.\n" +
                        "  Fix: Use .messages(history).user(latestCustomerMessage)");
            }

            Message m0 = nonSystem.get(0);
            Message m1 = nonSystem.get(1);
            Message m2 = nonSystem.get(2);
            Message m3 = nonSystem.get(3);

            if (m0.getMessageType() != MessageType.USER || !m0.getText().contains("incorrect $85 charge") ||
                m1.getMessageType() != MessageType.ASSISTANT || !m1.getText().contains("Did you take any rides") ||
                m2.getMessageType() != MessageType.USER || !m2.getText().contains("No, I was abroad") ||
                m3.getMessageType() != MessageType.USER || !m3.getText().contains("flight ticket confirmation")) {
                return new ScenarioResult(5, name, false,
                        "Message sequence or message types corrupted during history replay.\n" +
                        "  Expected: [USER, ASSISTANT, USER, USER]");
            }

            return new ScenarioResult(5, name, true, "Multi-turn history replay preserved message types and sequence.");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "classifyTransactionIntent (Few-Shot In-Context Priming)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            String query = "Stop debiting my card for the quarterly fitness pass";
            service.classifyTransactionIntent(query);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) return new ScenarioResult(6, name, false, "No prompt dispatched.");

            List<Message> instructions = prompt.getInstructions();
            List<Message> nonSystem = instructions.stream()
                    .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                    .toList();

            // 3 few-shots (3 User + 3 Assistant = 6 messages) + 1 final User query = 7 messages
            if (nonSystem.size() < 7) {
                return new ScenarioResult(6, name, false,
                        "Few-shot exemplar messages not primed! Found " + nonSystem.size() + " conversation messages, expected 7.\n" +
                        "  Fix: Build UserMessage/AssistantMessage pairs from CLASSIFICATION_FEW_SHOTS and pass via .messages(...)");
            }

            // Verify interleaved few-shot structure
            boolean alternating = true;
            for (int i = 0; i < 6; i += 2) {
                if (nonSystem.get(i).getMessageType() != MessageType.USER ||
                    nonSystem.get(i + 1).getMessageType() != MessageType.ASSISTANT) {
                    alternating = false;
                    break;
                }
            }

            if (!alternating) {
                return new ScenarioResult(6, name, false,
                        "Few-shot examples must alternate between MessageType.USER and MessageType.ASSISTANT.");
            }

            Message last = nonSystem.get(nonSystem.size() - 1);
            if (last.getMessageType() != MessageType.USER || !query.equals(last.getText())) {
                return new ScenarioResult(6, name, false,
                        "Final message is not the target customer inquiry:\n  Actual: \"" + last.getText() + "\"");
            }

            return new ScenarioResult(6, name, true, "Few-shot exemplar messages correctly primed with alternating roles.");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "deriveExpeditedClient (ChatClient Mutation)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            ChatClient expeditedClient = service.deriveExpeditedClient();
            if (expeditedClient == null) {
                return new ScenarioResult(7, name, false, "deriveExpeditedClient returned null.");
            }

            // 1. Dispatch prompt on expedited client
            fakeModel.clear();
            expeditedClient.prompt().user("Breach detected on cluster 4").call().content();
            Prompt expPrompt = fakeModel.getLastPrompt();

            if (expPrompt == null || expPrompt.getSystemMessage() == null) {
                return new ScenarioResult(7, name, false,
                        "Expedited client has null system prompt!\n" +
                        "  Fix: Use chatClient.mutate().defaultSystem(EXPEDITED_SYSTEM_PROMPT).defaultOptions(...).build()");
            }

            if (!FraudModelContracts.EXPEDITED_SYSTEM_PROMPT.equals(expPrompt.getSystemMessage().getText())) {
                return new ScenarioResult(7, name, false,
                        "Expedited client did not inherit new default system prompt:\n" +
                        "  Expected: \"" + FraudModelContracts.EXPEDITED_SYSTEM_PROMPT + "\"\n" +
                        "  Actual:   \"" + expPrompt.getSystemMessage().getText() + "\"");
            }

            Double expTemp = expPrompt.getOptions() != null ? expPrompt.getOptions().getTemperature() : null;
            if (expTemp == null || Math.abs(expTemp - 0.1) > 0.001) {
                return new ScenarioResult(7, name, false,
                        "Expedited client default temperature mismatch (expected 0.1, got " + expTemp + ")");
            }

            // 2. Ensure base client was NOT mutated in-place
            fakeModel.clear();
            service.auditHighRiskTransfer("Standard check");
            Prompt basePrompt = fakeModel.getLastPrompt();
            if (basePrompt.getSystemMessage() != null &&
                FraudModelContracts.EXPEDITED_SYSTEM_PROMPT.equals(basePrompt.getSystemMessage().getText())) {
                return new ScenarioResult(7, name, false,
                        "Base client's default system prompt was overwritten! Mutation must return an isolated copy.");
            }

            return new ScenarioResult(7, name, true, "Derived client mutated cleanly without corrupting base client.");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "executeWithTokenAudit (Usage Metadata Extraction)";
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            fakeModel.setUsage(240, 88);
            ChatClient.Builder builder = ChatClient.builder(fakeModel);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(builder);

            FraudModelContracts.AuditResult result = service.executeWithTokenAudit("Scan transactions for structuring anomalies");
            if (result == null) {
                return new ScenarioResult(8, name, false, "executeWithTokenAudit returned null.");
            }

            if (result.promptTokens() == 0L || result.completionTokens() == 0L) {
                return new ScenarioResult(8, name, false,
                        "Token usage was not extracted from ChatResponse metadata (returned 0 tokens)!\n" +
                        "  Found: promptTokens=" + result.promptTokens() + ", completionTokens=" + result.completionTokens() + "\n" +
                        "  Fix: Call .call().chatResponse() and inspect response.getMetadata().getUsage()");
            }

            if (result.promptTokens() != 240L || result.completionTokens() != 88L) {
                return new ScenarioResult(8, name, false,
                        "Token usage mismatch: Expected promptTokens=240, completionTokens=88. Got " +
                        result.promptTokens() + " / " + result.completionTokens());
            }

            if (result.responseContent().isBlank()) {
                return new ScenarioResult(8, name, false, "responseContent is blank in AuditResult.");
            }

            return new ScenarioResult(8, name, true, "ChatResponse metadata usage (tokens in/out) cleanly extracted.");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("\n======================================================================");
        System.out.println("                 PHASE 01 EXERCISE 02: VERIFICATION REPORT             ");
        System.out.println("======================================================================");

        int passed = 0;
        int failed = 0;

        for (ScenarioResult res : results) {
            System.out.println("SCENARIO " + res.scenarioNumber() + ": " + res.name());
            if (res.passed()) {
                System.out.println("  [PASS] " + res.errorDetail());
                passed++;
            } else {
                System.out.println("  [FAIL] " + res.errorDetail().replace("\n", "\n         "));
                failed++;
            }
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.println("SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        if (failed > 0) {
            System.out.println("VERDICT: FAIL (exit code 99)");
        } else {
            System.out.println("VERDICT: ALL SCENARIOS PASSED (exit code 0)");
        }
        System.out.println("======================================================================\n");
    }
}
