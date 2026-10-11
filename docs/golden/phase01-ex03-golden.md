# Phase 01 Exercise 03 — Golden Reference Solution
## Cloud SRE & Kubernetes Operations (15 Drills)

```java
package phase01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service under test for Phase 01 Exercise 03: Cloud SRE & Kubernetes Operations (15 Drills).
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
        if (clusterId == null || clusterId.isBlank() || namespace == null || namespace.isBlank() || podLogs == null || podLogs.isBlank()) {
            throw new IllegalArgumentException("Parameters must not be null or blank");
        }
        return chatClient.prompt()
                .system("Cluster: " + clusterId + " | Namespace: " + namespace)
                .user(podLogs)
                .call()
                .content();
    }

    /**
     * Drill 3: Incident paging dynamic system envelope.
     */
    public String applyIncidentPagingEnvelope(String onCallEngineer, String incidentQuery) {
        if (onCallEngineer == null || onCallEngineer.isBlank() || incidentQuery == null || incidentQuery.isBlank()) {
            throw new IllegalArgumentException("Parameters must not be null or blank");
        }
        return chatClient.prompt()
                .system("ON-CALL DISPATCH TO: " + onCallEngineer + "\n" + SreModelContracts.BASE_SRE_SYSTEM_PROMPT)
                .user(incidentQuery)
                .call()
                .content();
    }

    /**
     * Drills 4 & 5: Parameterized resource exhaustion alert template with fail-fast validation.
     */
    public String renderResourceExhaustionAlert(SreModelContracts.AlertDetails alert) {
        Objects.requireNonNull(alert, "alert must not be null");
        return chatClient.prompt()
                .user(u -> u.text(SreModelContracts.RESOURCE_ALERT_TEMPLATE)
                        .params(Map.of(
                                "serviceName", alert.serviceName(),
                                "cpuPercent", alert.cpuPercent(),
                                "memoryBytes", alert.memoryBytes(),
                                "p99LatencySec", alert.p99LatencySec(),
                                "errorCodes", alert.errorCodes())))
                .call()
                .content();
    }

    /**
     * Drill 6: Conditional environment policy injection.
     */
    public String triageWithEnvironmentPolicy(SreModelContracts.Environment env, String anomalyDescription) {
        if (env == null) {
            throw new IllegalArgumentException("env must not be null");
        }
        if (anomalyDescription == null || anomalyDescription.isBlank()) {
            throw new IllegalArgumentException("anomalyDescription must not be null or blank");
        }
        String policy = env == SreModelContracts.Environment.PRODUCTION
                ? SreModelContracts.PROD_POLICY
                : SreModelContracts.NON_PROD_POLICY;
        return chatClient.prompt()
                .system(policy)
                .user(anomalyDescription)
                .call()
                .content();
    }

    /**
     * Drills 7, 8 & 9: Fine-grained ChatOptions with immutability and fail-fast validation.
     */
    public String generateDeterministicRunbook(String alertName) {
        if (alertName == null || alertName.isBlank()) {
            throw new IllegalArgumentException("alertName must not be null or blank");
        }
        return chatClient.prompt()
                .user(alertName)
                .options(ChatOptions.builder()
                        .temperature(SreModelContracts.DETERMINISTIC_TEMPERATURE)
                        .maxTokens(SreModelContracts.RUNBOOK_MAX_TOKENS)
                        .stopSequences(SreModelContracts.RUNBOOK_STOP_SEQUENCES)
                        .frequencyPenalty(SreModelContracts.RUNBOOK_FREQUENCY_PENALTY))
                .call()
                .content();
    }

    /**
     * Drills 10 & 11: Multi-turn troubleshooting session history replay with boundary validation.
     */
    public String continueTroubleshootingSession(List<Message> history, String userReply) {
        if (history == null) {
            throw new IllegalArgumentException("history must not be null");
        }
        if (userReply == null || userReply.isBlank()) {
            throw new IllegalArgumentException("userReply must not be null or blank");
        }
        return chatClient.prompt()
                .messages(history)
                .user(userReply)
                .call()
                .content();
    }

    /**
     * Drill 12: Alert classification with few-shot priming.
     */
    public String classifyRemediationAction(String failureSymptom) {
        if (failureSymptom == null || failureSymptom.isBlank()) {
            throw new IllegalArgumentException("failureSymptom must not be null or blank");
        }
        List<Message> messages = new ArrayList<>();
        for (SreModelContracts.FewShotPair pair : SreModelContracts.REMEDIATION_FEW_SHOTS) {
            messages.add(new UserMessage(pair.symptom()));
            messages.add(new AssistantMessage(pair.action()));
        }
        return chatClient.prompt()
                .messages(messages)
                .user(failureSymptom)
                .call()
                .content();
    }

    /**
     * Drill 13: Canary analysis client derivation via .mutate().
     */
    public ChatClient deriveCanaryAnalysisClient(double canaryTrafficPercent) {
        if (canaryTrafficPercent < 0.0 || canaryTrafficPercent > 100.0) {
            throw new IllegalArgumentException("canaryTrafficPercent must be between 0.0 and 100.0");
        }
        return this.chatClient.mutate()
                .defaultSystem(s -> s.text(SreModelContracts.CANARY_SYSTEM_TEMPLATE)
                        .param("trafficPercent", canaryTrafficPercent))
                .defaultOptions(ChatOptions.builder().temperature(0.1))
                .build();
    }

    /**
     * Drill 14: Token usage audit extraction from ChatResponse metadata.
     */
    public SreModelContracts.SreAudit executeIncidentDiagnosisWithAudit(String incidentReport) {
        if (incidentReport == null || incidentReport.isBlank()) {
            throw new IllegalArgumentException("incidentReport must not be null or blank");
        }
        var response = chatClient.prompt()
                .user(incidentReport)
                .call()
                .chatResponse();
        String content = response != null && response.getResult() != null && response.getResult().getOutput() != null
                ? response.getResult().getOutput().getText() : "";
        long promptTokens = 0L;
        long completionTokens = 0L;
        if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            promptTokens = response.getMetadata().getUsage().getPromptTokens();
            completionTokens = response.getMetadata().getUsage().getCompletionTokens();
        }
        return new SreModelContracts.SreAudit(content, promptTokens, completionTokens);
    }

    /**
     * Drill 15: Fail-fast structural invariant enforcement on response.
     */
    public String safeExecuteDiagnosis(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be null or blank");
        }
        var response = chatClient.prompt()
                .user(query)
                .call()
                .chatResponse();
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            throw new IllegalStateException("Empty response from ChatModel");
        }
        var output = response.getResult().getOutput();
        if (output == null || output.getText() == null || output.getText().isBlank()) {
            throw new IllegalStateException("Malformed response from ChatModel: blank output");
        }
        return output.getText();
    }
}
```
