package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Objects;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 *
 * Seeded with defects across 10 scenarios in Kubernetes Cloud SRE operations:
 * - Scenario 1: Boundary validation and role separation in pod crash analysis.
 * - Scenario 2: Parameterized resource exhaustion alert template.
 * - Scenario 3: Conditional environment policy injection.
 * - Scenario 4: Defensive token auditing with nullable Usage metadata.
 * - Scenario 5: Canary analysis client derivation via .mutate().
 * - Scenario 6: Fine-grained ChatOptions with stopSequences and penalties.
 * - Scenario 7: Alert classification with few-shot priming.
 * - Scenario 8: Multi-turn troubleshooting session history replay.
 * - Scenario 9: System envelope augmentation for incident paging.
 * - Scenario 10: Fail-fast structural invariant enforcement on response.
 */
public class SreOpsServiceUnderTest {

    private final ChatClient chatClient;

    public SreOpsServiceUnderTest(ChatClient.Builder clientBuilder) {
        Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
        this.chatClient = clientBuilder
                .defaultSystem(SreModelContracts.BASE_SRE_SYSTEM_PROMPT)
                .defaultOptions(ChatOptions.builder()
                        .temperature(SreModelContracts.BASE_TEMPERATURE)
                        .topP(SreModelContracts.BASE_TOP_P))
                .build();
    }

    /**
     * Scenario 1: Boundary validation and role separation.
     * Invariants: clusterId, namespace, podLogs must NOT be null or blank.
     * System message: "Cluster: " + clusterId + " | Namespace: " + namespace
     * User message:   podLogs
     */
    public String analyzePodCrash(String clusterId, String namespace, String podLogs) {
        // DEFECT (Scenario 1): Missing boundary checks and concatenates into user message.
        return chatClient.prompt()
                .user("Cluster: " + clusterId + " | Namespace: " + namespace + "\nLogs:\n" + podLogs)
                .call()
                .content();
    }

    /**
     * Scenario 2: Template parameter binding with complex types.
     * Must use SreModelContracts.RESOURCE_ALERT_TEMPLATE with .user(u -> u.text(...).params(Map.of(...)))
     * Placeholders: "serviceName", "cpuPercent", "memoryBytes", "p99LatencySec", "errorCodes"
     */
    public String renderResourceExhaustionAlert(SreModelContracts.AlertDetails alert) {
        // DEFECT (Scenario 2): Dispatches raw template without binding parameters.
        return chatClient.prompt()
                .user(SreModelContracts.RESOURCE_ALERT_TEMPLATE)
                .call()
                .content();
    }

    /**
     * Scenario 3: Conditional environment policy injection.
     * Invariants: env and anomalyDescription must not be null.
     * If env == PRODUCTION: system prompt must use PROD_POLICY.
     * Else: system prompt must use NON_PROD_POLICY.
     * User message: anomalyDescription.
     */
    public String triageWithEnvironmentPolicy(SreModelContracts.Environment env, String anomalyDescription) {
        // DEFECT (Scenario 3): Uses static base system prompt, ignoring env policy completely.
        return chatClient.prompt()
                .user(anomalyDescription)
                .call()
                .content();
    }

    /**
     * Scenario 4: Defensive token auditing with nullable Usage metadata.
     * Must execute diagnosis and extract prompt and completion tokens safely into SreAudit.
     */
    public SreModelContracts.SreAudit executeIncidentDiagnosisWithAudit(String incidentReport) {
        // DEFECT (Scenario 4): Calls .content() directly, returning 0 tokens.
        String content = chatClient.prompt()
                .user(incidentReport)
                .call()
                .content();
        return new SreModelContracts.SreAudit(content, 0L, 0L);
    }

    /**
     * Scenario 5: Client mutation with parameterized template.
     * Must derive a canary analysis ChatClient via .mutate():
     * - Invariants: canaryTrafficPercent between 0.0 and 100.0.
     * - defaultSystem: CANARY_SYSTEM_TEMPLATE with param "trafficPercent".
     * - defaultOptions: temperature = 0.1.
     */
    public ChatClient deriveCanaryAnalysisClient(double canaryTrafficPercent) {
        // DEFECT (Scenario 5): Returns base client without mutation.
        return this.chatClient;
    }

    /**
     * Scenario 6: Fine-grained ChatOptions.
     * Must configure call-level options:
     * - temperature: DETERMINISTIC_TEMPERATURE (0.0)
     * - maxTokens: RUNBOOK_MAX_TOKENS (300)
     * - stopSequences: RUNBOOK_STOP_SEQUENCES
     * - frequencyPenalty: RUNBOOK_FREQUENCY_PENALTY (0.5)
     */
    public String generateDeterministicRunbook(String alertName) {
        // DEFECT (Scenario 6): Dispatches without overriding options.
        return chatClient.prompt()
                .user(alertName)
                .call()
                .content();
    }

    /**
     * Scenario 7: Alert classification with few-shot priming.
     * Must prepend REMEDIATION_FEW_SHOTS as alternating UserMessage/AssistantMessage pairs,
     * followed by failureSymptom as the final user message.
     */
    public String classifyRemediationAction(String failureSymptom) {
        // DEFECT (Scenario 7): Dispatches with zero few-shot examples.
        return chatClient.prompt()
                .user(failureSymptom)
                .call()
                .content();
    }

    /**
     * Scenario 8: Multi-turn troubleshooting session history replay.
     * Must include all previous messages via .messages(history) followed by userReply via .user(...).
     */
    public String continueTroubleshootingSession(List<Message> history, String userReply) {
        // DEFECT (Scenario 8): Ignores history and only sends userReply.
        return chatClient.prompt()
                .user(userReply)
                .call()
                .content();
    }

    /**
     * Scenario 9: System envelope augmentation for incident paging.
     * System prompt template: "ON-CALL DISPATCH TO: {engineer}\n" + BASE_SRE_SYSTEM_PROMPT.
     */
    public String applyIncidentPagingEnvelope(String onCallEngineer, String incidentQuery) {
        // DEFECT (Scenario 9): Concatenates envelope into user message.
        return chatClient.prompt()
                .user("Dispatcher: " + onCallEngineer + "\nQuery: " + incidentQuery)
                .call()
                .content();
    }

    /**
     * Scenario 10: Fail-fast structural invariant enforcement on response.
     * Must call .chatResponse(), validate non-null output text, and throw IllegalStateException
     * if empty or malformed.
     */
    public String safeExecuteDiagnosis(String query) {
        // DEFECT (Scenario 10): Blindly calls .content() without structure validation.
        return chatClient.prompt()
                .user(query)
                .call()
                .content();
    }
}
