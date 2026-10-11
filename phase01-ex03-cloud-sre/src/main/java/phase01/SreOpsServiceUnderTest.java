package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Objects;

/**
 * Service under test — Phase 01 Exercise 03: Cloud SRE & Kubernetes Operations.
 * <p>
 * Practice mode: Contains intentional defects across all 15 drills (0/15 passing).
 * Follow the tiered progressive hints in Verifier output to implement each gate.
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
     * Drills 1 & 2: Boundary validation and role separation.
     */
    public String analyzePodCrash(String clusterId, String namespace, String podLogs) {
        // DEFECT (Drills 1 & 2): Concatenates into user message without boundary checks.
        return chatClient.prompt()
                .user("Cluster: " + clusterId + " | Namespace: " + namespace + "\nLogs:\n" + podLogs)
                .call()
                .content();
    }

    /**
     * Drill 3: Incident paging dynamic system envelope.
     */
    public String applyIncidentPagingEnvelope(String onCallEngineer, String incidentQuery) {
        // DEFECT (Drill 3): Returns null without configuring paging system envelope.
        return null;
    }

    /**
     * Drills 4 & 5: Parameterized resource exhaustion alert template with fail-fast validation.
     */
    public String renderResourceExhaustionAlert(SreModelContracts.AlertDetails alert) {
        // DEFECT (Drills 4 & 5): Dispatches raw un-substituted template and lacks null-check.
        return chatClient.prompt()
                .user(SreModelContracts.RESOURCE_ALERT_TEMPLATE)
                .call()
                .content();
    }

    /**
     * Drill 6: Conditional environment policy injection.
     */
    public String triageWithEnvironmentPolicy(SreModelContracts.Environment env, String anomalyDescription) {
        // DEFECT (Drill 6): Returns null without branching on environment policy.
        return null;
    }

    /**
     * Drills 7, 8 & 9: Fine-grained ChatOptions with immutability and fail-fast validation.
     */
    public String generateDeterministicRunbook(String alertName) {
        // DEFECT (Drills 7, 8 & 9): Returns null without setting fine-grained ChatOptions.
        return null;
    }

    /**
     * Drills 10 & 11: Multi-turn troubleshooting session history replay with boundary validation.
     */
    public String continueTroubleshootingSession(List<Message> history, String userReply) {
        // DEFECT (Drills 10 & 11): Returns null without chaining message history.
        return null;
    }

    /**
     * Drill 12: Alert classification with few-shot priming.
     */
    public String classifyRemediationAction(String failureSymptom) {
        // DEFECT (Drill 12): Returns null without prepending few-shot remediation pairs.
        return null;
    }

    /**
     * Drill 13: Canary analysis client derivation via .mutate().
     */
    public ChatClient deriveCanaryAnalysisClient(double canaryTrafficPercent) {
        // DEFECT (Drill 13): Returns null without deriving mutated canary client.
        return null;
    }

    /**
     * Drill 14: Token usage audit extraction from ChatResponse metadata.
     */
    public SreModelContracts.SreAudit executeIncidentDiagnosisWithAudit(String incidentReport) {
        // DEFECT (Drill 14): Returns null without extracting token usage metadata.
        return null;
    }

    /**
     * Drill 15: Fail-fast structural invariant enforcement on response.
     */
    public String safeExecuteDiagnosis(String query) {
        // DEFECT (Drill 15): Returns null without inspecting chatResponse output structure.
        return null;
    }
}
