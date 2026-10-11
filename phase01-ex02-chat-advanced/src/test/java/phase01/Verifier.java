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
 * Gate Verifier for Phase 01 Exercise 02: Advanced ChatClient & FinTech Operations.
 * <p>
 * 15 Repetitive Muscle-Memory Drills (5 Topics x 3 Repetitions) with Tiered Progressive Hints.
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL.
 * Exits with status 0 on PASS.
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
        // Topic 1: Role Separation & Parameterized System Specs (Drills 1 - 3)
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        // Topic 2: Parameterized Templates & Dispute Notifications (Drills 4 - 6)
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        // Topic 3: Call-Level Options Overrides & High-Risk Transfers (Drills 7 - 9)
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        // Topic 4: Conversational History & Few-Shot In-Context Learning (Drills 10 - 12)
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        // Topic 5: Client Mutation & Token Usage Audit Metadata (Drills 13 - 15)
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        return list;
    }

    // =========================================================================
    // Topic 1: Role Separation & Parameterized System Specs (Drills 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "analyzeTransaction (Role Separation Direct Semantic Role Match)";
        List<String> hints = List.of(
                "Maintain strict semantic role separation between complianceRole and transaction data.",
                "Use `.system(complianceRole)` and `.user(tx.toString())` on the request spec.",
                "Do NOT concatenate the compliance role into the user prompt."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            String complianceRole = "You are a FinTech AML Tier 2 Compliance Officer.";
            FraudModelContracts.Transaction tx = new FraudModelContracts.Transaction(
                    "TX-9901", "ACC-4412", 9500.00, "Luxury Watches Ltd", "CY");
            service.analyzeTransaction(complianceRole, tx);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(1, name, false, "SystemMessage is null! Use .system(complianceRole).", hints);
            }
            if (!complianceRole.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(1, name, false,
                        "SystemMessage mismatch: expected '" + complianceRole + "', got '" + prompt.getSystemMessage().getText() + "'", hints);
            }
            if (prompt.getUserMessage() == null || !prompt.getUserMessage().getText().contains("TX-9901")) {
                return new ScenarioResult(1, name, false, "UserMessage missing transaction details: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "analyzeTransaction (Fail-Fast Boundary Invariants)";
        List<String> hints = List.of(
                "Enforce fail-fast input validation before dispatching to the LLM.",
                "Throw IllegalArgumentException or NullPointerException when complianceRole is null or blank, or tx is null.",
                "Check `if (complianceRole == null || complianceRole.isBlank()) throw new IllegalArgumentException(...); Objects.requireNonNull(tx);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));
            FraudModelContracts.Transaction tx = new FraudModelContracts.Transaction(
                    "TX-1", "ACC-1", 100.0, "Shop", "US");

            try {
                service.analyzeTransaction(null, tx);
                return new ScenarioResult(2, name, false, "null complianceRole must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.analyzeTransaction("   ", tx);
                return new ScenarioResult(2, name, false, "blank complianceRole must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.analyzeTransaction("Role", null);
                return new ScenarioResult(2, name, false, "null tx must throw NullPointerException or IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "configureRegionalAuditor (Parameterized System Prompt Specification)";
        List<String> hints = List.of(
                "Use `.system(s -> s.text(REGIONAL_AUDITOR_SYSTEM_TEMPLATE).param(\"jurisdiction\", jurisdiction).param(\"standard\", standard))`.",
                "Ensure user query is set via `.user(auditQuery)` without leaking jurisdiction data into user message.",
                "Validate parameters: throw IllegalArgumentException if jurisdiction, standard, or auditQuery is null or blank."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            String jurisdiction = "Singapore-MAS";
            String standard = "Notice-626";
            String query = "Audit cash deposits exceeding SGD 20,000";
            service.configureRegionalAuditor(jurisdiction, standard, query);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(3, name, false, "SystemMessage is null or missing.", hints);
            }
            String sysText = prompt.getSystemMessage().getText();
            if (sysText.contains("{jurisdiction}") || sysText.contains("{standard}")) {
                return new ScenarioResult(3, name, false, "System prompt has un-substituted placeholders: " + sysText, hints);
            }
            if (!sysText.contains("Singapore-MAS") || !sysText.contains("Notice-626")) {
                return new ScenarioResult(3, name, false, "System prompt missing regional parameter bindings: " + sysText, hints);
            }
            if (prompt.getUserMessage() == null || !query.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(3, name, false, "User query corrupted by system parameters: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 2: Parameterized Templates & Dispute Notifications (Drills 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "draftDisputeNotice (Map Parameter Binding & Placeholder Elimination)";
        List<String> hints = List.of(
                "Use `.user(u -> u.text(DISPUTE_TEMPLATE).params(Map.of(...)))` to bind dispute parameters.",
                "Placeholders required: customerName, disputeId, amount, currency, reason.",
                "Verify prompt does not contain `{customerName}` or other placeholder tokens."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            FraudModelContracts.DisputeNotice dispute = new FraudModelContracts.DisputeNotice(
                    "Sarah O'Connor", "DISP-882", 250.00, "USD", "Unauthorized charge");
            service.draftDisputeNotice(dispute);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(4, name, false, "UserMessage is null.", hints);
            }
            String userText = prompt.getUserMessage().getText();
            if (userText.contains("{customerName}") || userText.contains("{disputeId}") ||
                userText.contains("{amount}") || userText.contains("{currency}") || userText.contains("{reason}")) {
                return new ScenarioResult(4, name, false, "Un-substituted placeholders found in dispute notice: " + userText, hints);
            }
            if (!userText.contains("Sarah O'Connor") || !userText.contains("DISP-882") ||
                !userText.contains("250.0") || !userText.contains("USD")) {
                return new ScenarioResult(4, name, false, "Dispute parameters missing from prompt: " + userText, hints);
            }

            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "draftDisputeNotice (Fail-Fast Null Dispute Invariant)";
        List<String> hints = List.of(
                "Fail fast when input domain record is null.",
                "Throw NullPointerException or IllegalArgumentException if dispute is null.",
                "Use `Objects.requireNonNull(dispute, 'dispute must not be null');`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.draftDisputeNotice(null);
                return new ScenarioResult(5, name, false, "null dispute must throw NullPointerException or IllegalArgumentException", hints);
            } catch (NullPointerException | IllegalArgumentException ignored) {}

            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "draftDisputeNotice (Special Character and Symbol Preservation)";
        List<String> hints = List.of(
                "Financial inputs contain currency symbols ($), curly braces, and quotes that must survive templating.",
                "Do NOT perform manual String.replace which can break on regex symbols or curly brackets.",
                "Spring AI parameter map binding properly preserves special characters without corruption."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            FraudModelContracts.DisputeNotice dispute = new FraudModelContracts.DisputeNotice(
                    "Sarah O'Connor", "DISP-882", 250.00, "USD",
                    "Unauthorized recurring card-not-present charge for $250.00 {sub_ref: 99}");
            service.draftDisputeNotice(dispute);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getUserMessage() == null) {
                return new ScenarioResult(6, name, false, "UserMessage is null.", hints);
            }
            String userText = prompt.getUserMessage().getText();
            if (!userText.contains("$250.00 {sub_ref: 99}")) {
                return new ScenarioResult(6, name, false, "Special characters in reason corrupted: " + userText, hints);
            }

            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 3: Call-Level Options Overrides & High-Risk Transfers (Drills 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "auditHighRiskTransfer (Call-Level Options Override)";
        List<String> hints = List.of(
                "High-risk financial operations require deterministic zero-temperature auditing.",
                "Attach call-level options using `.options(ChatOptions.builder().temperature(0.0).maxTokens(500))`.",
                "Verify prompt options contain temperature 0.0 and maxTokens 500."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            service.auditHighRiskTransfer("Urgent $5,000,000 transfer to offshore routing 99281");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(7, name, false, "No ChatOptions attached to the high-risk prompt call.", hints);
            }
            Double temp = prompt.getOptions().getTemperature();
            Integer maxTokens = prompt.getOptions().getMaxTokens();
            if (temp == null || Math.abs(temp - FraudModelContracts.OVERRIDE_TEMPERATURE) > 0.001) {
                return new ScenarioResult(7, name, false,
                        "Temperature override failed. Expected: " + FraudModelContracts.OVERRIDE_TEMPERATURE + ", got: " + temp, hints);
            }
            if (maxTokens == null || maxTokens != FraudModelContracts.OVERRIDE_MAX_TOKENS) {
                return new ScenarioResult(7, name, false,
                        "maxTokens override failed. Expected: " + FraudModelContracts.OVERRIDE_MAX_TOKENS + ", got: " + maxTokens, hints);
            }

            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "auditHighRiskTransfer (Base Client Temperature Default Preserved)";
        List<String> hints = List.of(
                "Call-level option overrides must NOT mutate the shared ChatClient instance.",
                "Subsequent standard calls must retain the base temperature (0.7) and topP (0.95).",
                "Ensure ChatClient immutability is maintained."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            // First call overrides options
            service.auditHighRiskTransfer("High risk transfer");

            // Second call uses standard classifyTransactionIntent
            service.classifyTransactionIntent("Normal inquiry");

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getOptions() == null) {
                return new ScenarioResult(8, name, false, "Options missing from subsequent call.", hints);
            }
            Double temp = prompt.getOptions().getTemperature();
            if (temp == null || Math.abs(temp - FraudModelContracts.BASE_TEMPERATURE) > 0.001) {
                return new ScenarioResult(8, name, false,
                        "Subsequent call did not retain base temperature 0.7: got " + temp, hints);
            }

            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "auditHighRiskTransfer (Fail-Fast Boundary Validation)";
        List<String> hints = List.of(
                "Enforce fail-fast input validation on high-risk transfer audit inputs.",
                "Throw IllegalArgumentException if transferDetails is null or blank.",
                "Check `if (transferDetails == null || transferDetails.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.auditHighRiskTransfer(null);
                return new ScenarioResult(9, name, false, "null transferDetails must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.auditHighRiskTransfer("   ");
                return new ScenarioResult(9, name, false, "blank transferDetails must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 4: Conversational History & Few-Shot In-Context Learning (Drills 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "continueDisputeDialogue (Multi-Turn History Replay)";
        List<String> hints = List.of(
                "Inject prior conversational messages via `.messages(history).user(latestCustomerMessage)`.",
                "Preserve UserMessage and AssistantMessage order in prompt instructions.",
                "Ensure the latestCustomerMessage is appended as the trailing user turn."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            List<Message> history = List.of(
                    new UserMessage("I did not authorize transaction TX-882"),
                    new AssistantMessage("I have locked card ending in 4412. Did you share your PIN?"),
                    new UserMessage("No, I was abroad with my phone turned off.")
            );
            String latest = "Can I request a provisional credit?";
            service.continueDisputeDialogue(history, latest);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) {
                return new ScenarioResult(10, name, false, "No prompt dispatched.", hints);
            }
            List<Message> instructions = prompt.getInstructions();
            if (instructions.size() < 4) {
                return new ScenarioResult(10, name, false, "Prompt expected at least 4 messages (3 history + 1 latest), got: " + instructions.size(), hints);
            }
            if (!latest.equals(prompt.getUserMessage().getText())) {
                return new ScenarioResult(10, name, false, "Latest customer message not found at end of dialogue: " + prompt.getUserMessage(), hints);
            }

            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "continueDisputeDialogue (History Boundary Validation)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary validation on multi-turn dispute dialogue inputs.",
                "Throw IllegalArgumentException if history is null, or if latestCustomerMessage is null or blank.",
                "Check `if (history == null || latestCustomerMessage == null || latestCustomerMessage.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.continueDisputeDialogue(null, "Valid message");
                return new ScenarioResult(11, name, false, "null history must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.continueDisputeDialogue(List.of(), null);
                return new ScenarioResult(11, name, false, "null latestCustomerMessage must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.continueDisputeDialogue(List.of(), "   ");
                return new ScenarioResult(11, name, false, "blank latestCustomerMessage must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "classifyTransactionIntent (Few-Shot In-Context Learning Priming)";
        List<String> hints = List.of(
                "Prime LLM classification using few-shot examples from CLASSIFICATION_FEW_SHOTS.",
                "For each FewShotExample, create UserMessage(ex.userText()) and AssistantMessage(ex.assistantReply()).",
                "Chain few-shot messages followed by the final user message for customerInquiry."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            String inquiry = "There is a recurring charge from Netflix that I never signed up for";
            service.classifyTransactionIntent(inquiry);

            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null) {
                return new ScenarioResult(12, name, false, "No prompt dispatched.", hints);
            }
            List<Message> instructions = prompt.getInstructions();
            // Expected 3 few-shot pairs (6 messages) + 1 user query = at least 7 messages (or + 1 default system)
            if (instructions.size() < 7) {
                return new ScenarioResult(12, name, false,
                        "Few-shot priming missing! Expected at least 7 messages (6 few-shot pairs + 1 query), got: " + instructions.size(), hints);
            }

            boolean hasAssistant = instructions.stream().anyMatch(m -> m.getMessageType() == MessageType.ASSISTANT);
            if (!hasAssistant) {
                return new ScenarioResult(12, name, false, "Few-shot priming must include AssistantMessage replies.", hints);
            }

            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Topic 5: Client Mutation & Token Usage Audit Metadata (Drills 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "deriveExpeditedClient (ChatClient Mutation via .mutate())";
        List<String> hints = List.of(
                "Derive an expedited ChatClient using `this.chatClient.mutate()`.",
                "Configure `.defaultSystem(EXPEDITED_SYSTEM_PROMPT)` and `.defaultOptions(ChatOptions.builder().temperature(0.1))`.",
                "Verify base client remains unmutated (temperature 0.7) while derived client uses new defaults."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            ChatClient expedited = service.deriveExpeditedClient();
            if (expedited == null) {
                return new ScenarioResult(13, name, false, "deriveExpeditedClient returned null.", hints);
            }

            // Test derived client
            expedited.prompt().user("Emergency incident mitigation").call().content();
            Prompt prompt = fakeModel.getLastPrompt();
            if (prompt == null || prompt.getSystemMessage() == null) {
                return new ScenarioResult(13, name, false, "Mutated client missing system message.", hints);
            }
            if (!FraudModelContracts.EXPEDITED_SYSTEM_PROMPT.equals(prompt.getSystemMessage().getText())) {
                return new ScenarioResult(13, name, false, "Mutated client system prompt mismatch: " + prompt.getSystemMessage().getText(), hints);
            }
            if (prompt.getOptions() == null || Math.abs(prompt.getOptions().getTemperature() - 0.1) > 0.001) {
                return new ScenarioResult(13, name, false, "Mutated client temperature mismatch: " + (prompt.getOptions() != null ? prompt.getOptions().getTemperature() : "null"), hints);
            }

            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(13, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "executeWithTokenAudit (Token Usage Metadata Extraction)";
        List<String> hints = List.of(
                "Call `.chatResponse()` on the ChatClient request spec instead of `.content()` to access ChatResponse.",
                "Extract `response.getMetadata().getUsage()` to retrieve promptTokens and completionTokens.",
                "Return `new AuditResult(content, promptTokens, completionTokens)`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            fakeModel.setUsage(185, 75);
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            FraudModelContracts.AuditResult result = service.executeWithTokenAudit("Audit compliance log entries");
            if (result == null) {
                return new ScenarioResult(14, name, false, "executeWithTokenAudit returned null.", hints);
            }
            if (result.promptTokens() != 185L || result.completionTokens() != 75L) {
                return new ScenarioResult(14, name, false,
                        "Token counts mismatch. Expected promptTokens=185, completionTokens=75, got: " + result, hints);
            }
            if (result.responseContent() == null || result.responseContent().isBlank()) {
                return new ScenarioResult(14, name, false, "Response content missing from audit result.", hints);
            }

            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(14, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "executeWithTokenAudit (Fail-Fast Boundary Validation)";
        List<String> hints = List.of(
                "Enforce fail-fast boundary checking on operationalPrompt.",
                "Throw IllegalArgumentException if operationalPrompt is null or blank.",
                "Check `if (operationalPrompt == null || operationalPrompt.isBlank()) throw new IllegalArgumentException(...);`."
        );
        try {
            FakeChatModel fakeModel = new FakeChatModel();
            FraudOpsServiceUnderTest service = new FraudOpsServiceUnderTest(ChatClient.builder(fakeModel));

            try {
                service.executeWithTokenAudit(null);
                return new ScenarioResult(15, name, false, "null operationalPrompt must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            try {
                service.executeWithTokenAudit("   ");
                return new ScenarioResult(15, name, false, "blank operationalPrompt must throw IllegalArgumentException", hints);
            } catch (IllegalArgumentException | NullPointerException ignored) {}

            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Throwable t) {
            return new ScenarioResult(15, name, false, "Exception: " + t.getMessage(), hints);
        }
    }

    // =========================================================================
    // Report Formatting
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 01 EXERCISE 02: ADVANCED CHAT & FINTECH OPERATIONS REPORT");
        System.out.println("===============================================================================");
        int passed = 0;
        String[] hintLabels = {"[HINT 1 (Concept)]", "[HINT 2 (Edge Case)]", "[HINT 3 (Code Pattern)]"};

        for (ScenarioResult r : results) {
            String status = r.passed() ? "[PASS]" : "[FAIL]";
            if (r.passed()) passed++;
            System.out.printf("  %s Scenario %02d: %s%n", status, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null && !r.hints().isEmpty()) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        String label = i < hintLabels.length ? hintLabels[i] : String.format("[HINT %d]", i + 1);
                        System.out.printf("         --> %s %s%n", label, r.hints().get(i));
                    }
                }
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        if (passed == results.size()) {
            System.out.println("  VERDICT: ALL 15 GATES PASSED (exit code 0)");
        } else {
            System.out.printf("  VERDICT: %d FINDINGS DETECTED (exit code 99)%n", results.size() - passed);
        }
        System.out.println("===============================================================================");
    }
}
