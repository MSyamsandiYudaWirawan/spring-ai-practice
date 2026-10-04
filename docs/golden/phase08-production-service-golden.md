# Phase 08 Golden Solution: Production Autonomous Incident Triage Microservice

This document contains the verified golden reference implementations for all production microservice components in `phase08-production-service`.

---

## 1. Domain Models (`phase08.model`)

### `IncidentSeverity.java`
```java
package phase08.model;

public enum IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

### `DiagnosticTelemetry.java`
```java
package phase08.model;

public record DiagnosticTelemetry(
        double p95Ms,
        double rps,
        double errorRate
) {
    public static DiagnosticTelemetry baseline() {
        return new DiagnosticTelemetry(350.0, 100.0, 0.05);
    }
}
```

### `TriageProposal.java`
```java
package phase08.model;

public record TriageProposal(
        String hypothesis,
        String action,
        String targetFile,
        String patchContent,
        double confidence
) {}
```

### `SagaState.java`
```java
package phase08.model;

public enum SagaState {
    IDLE,
    DECIDE,
    APPLY,
    MEASURE,
    JUDGE,
    COMPENSATE,
    FINISH
}
```

### `TrajectoryEvent.java`
```java
package phase08.model;

import java.time.Instant;

public record TrajectoryEvent(
        String incidentId,
        int turn,
        SagaState state,
        String summary,
        Instant timestamp
) {}
```

### `TriageRequestDto.java` & `TriageResponseDto.java`
```java
package phase08.model;

public record TriageRequestDto(
        String incidentId,
        String serviceName,
        String description,
        IncidentSeverity severity
) {}
```
```java
package phase08.model;

public record TriageResponseDto(
        String incidentId,
        String rootCause,
        String status,
        int iterationsTaken,
        int tokensUsed,
        String headSha
) {}
```

---

## 2. Configuration (`phase08.config`)

### `AgentProperties.java`
```java
package phase08.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.triage")
public record AgentProperties(
        boolean enabled,
        String modelName,
        int maxIterations,
        int tokenBudget,
        double temperature,
        double p95FloorMs,
        double rpsFloor
) {
    public static AgentProperties defaults() {
        return new AgentProperties(true, "gpt-4o", 5, 4000, 0.2, 25.0, 15.0);
    }

    public void validate() {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName must not be blank");
        }
        if (maxIterations < 1 || maxIterations > 20) {
            throw new IllegalArgumentException("maxIterations must be between 1 and 20");
        }
        if (tokenBudget < 100) {
            throw new IllegalArgumentException("tokenBudget must be at least 100");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
        if (p95FloorMs < 0.0) {
            throw new IllegalArgumentException("p95FloorMs must be non-negative");
        }
        if (rpsFloor < 0.0) {
            throw new IllegalArgumentException("rpsFloor must be non-negative");
        }
    }
}
```

### `AgentAutoConfiguration.java`
```java
package phase08.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import phase08.advisor.SecurityKeywordAdvisor;
import phase08.advisor.TokenBudgetAdvisor;
import phase08.evaluator.GroundingEvaluator;
import phase08.evaluator.NoiseFloorGate;
import phase08.parser.TriageProposalParser;
import phase08.prompt.TriagePromptBuilder;
import phase08.repository.TrajectoryRepository;
import phase08.saga.AutonomousSagaLoop;
import phase08.tool.JfrAnalysisTool;
import phase08.tool.K8sDiagnosticTool;

@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TriagePromptBuilder triagePromptBuilder() {
        return new TriagePromptBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public TriageProposalParser triageProposalParser() {
        return new TriageProposalParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public NoiseFloorGate noiseFloorGate() {
        return new NoiseFloorGate();
    }

    @Bean
    @ConditionalOnMissingBean
    public GroundingEvaluator groundingEvaluator() {
        return new GroundingEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenBudgetAdvisor tokenBudgetAdvisor(AgentProperties props) {
        return new TokenBudgetAdvisor(props.tokenBudget());
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolCallback[] diagnosticTools(K8sDiagnosticTool k8sTool, JfrAnalysisTool jfrTool) {
        return ToolCallbacks.from(k8sTool, jfrTool);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            SecurityKeywordAdvisor securityAdvisor,
            TokenBudgetAdvisor tokenBudgetAdvisor
    ) {
        return builder
                .defaultSystem(TriagePromptBuilder.DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(securityAdvisor, tokenBudgetAdvisor)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AutonomousSagaLoop autonomousSagaLoop(
            ChatClient chatClient,
            ToolCallback[] tools,
            TriagePromptBuilder promptBuilder,
            TriageProposalParser parser,
            NoiseFloorGate noiseFloorGate,
            TrajectoryRepository repository,
            AgentProperties properties
    ) {
        return new AutonomousSagaLoop(
                chatClient,
                tools,
                promptBuilder,
                parser,
                noiseFloorGate,
                repository,
                properties
        );
    }
}
```

---

## 3. Prompt & Parser (`phase08.prompt`, `phase08.parser`)

### `TriagePromptBuilder.java`
```java
package phase08.prompt;

import phase08.model.DiagnosticTelemetry;

import java.util.Objects;

public class TriagePromptBuilder {

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are an automated production incident triage agent. Diagnose root cause and propose atomic fixes.";

    public String buildUserPrompt(String incidentId, String serviceName, String description, DiagnosticTelemetry telemetry) {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incidentId cannot be blank");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName cannot be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        Objects.requireNonNull(telemetry, "telemetry cannot be null");

        return String.format(
                "Incident: %s | Service: %s%nDescription: %s%nCurrent Telemetry: p95=%.1fms, rps=%.1f, err=%.2f%nPropose a concrete JSON triage proposal.",
                incidentId, serviceName, description, telemetry.p95Ms(), telemetry.rps(), telemetry.errorRate()
        );
    }
}
```

### `TriageProposalParser.java`
```java
package phase08.parser;

import org.springframework.ai.converter.BeanOutputConverter;
import phase08.model.TriageProposal;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriageProposalParser {

    private static final Pattern FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
    private final BeanOutputConverter<TriageProposal> converter = new BeanOutputConverter<>(TriageProposal.class);

    public String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        Matcher matcher = FENCE_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return raw.trim();
    }

    public TriageProposal parse(String raw) {
        String cleanJson = stripMarkdownFences(raw);
        TriageProposal proposal = converter.convert(cleanJson);
        if (proposal == null) {
            throw new IllegalArgumentException("Converted proposal is null");
        }
        if (proposal.action() == null || proposal.action().isBlank()) {
            throw new IllegalArgumentException("Proposal action cannot be blank");
        }
        if (proposal.hypothesis() == null || proposal.hypothesis().isBlank()) {
            throw new IllegalArgumentException("Proposal hypothesis cannot be blank");
        }
        if (proposal.confidence() < 0.0 || proposal.confidence() > 1.0) {
            throw new IllegalArgumentException("Proposal confidence must be between 0.0 and 1.0");
        }
        return proposal;
    }

    public Optional<TriageProposal> parseWithOneShotRepair(String raw, Function<String, String> repairFn) {
        try {
            return Optional.of(parse(raw));
        } catch (Exception initialError) {
            if (repairFn == null) {
                return Optional.empty();
            }
            try {
                String repairPrompt = "Schema parse failed: " + initialError.getMessage() + ". Fix and return ONLY valid JSON: " + raw;
                String repairedRaw = repairFn.apply(repairPrompt);
                return Optional.of(parse(repairedRaw));
            } catch (Exception repairError) {
                return Optional.empty();
            }
        }
    }
}
```

---

## 4. Diagnostic Tools (`phase08.tool`)

### `K8sDiagnosticTool.java`
```java
package phase08.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class K8sDiagnosticTool {

    @Tool(description = "Query status of pods running in the specified Kubernetes namespace")
    public String queryPodStatus(@ToolParam(description = "Kubernetes namespace") String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace cannot be blank");
        }
        return "Namespace " + namespace + ": 3 pods running, 0 failed, 0 restarts";
    }

    @Tool(description = "Fetch recent tail logs for a specific pod")
    public String fetchPodLogs(
            @ToolParam(description = "Target pod name") String podName,
            @ToolParam(description = "Number of tail lines (1 to 500)") int tailLines
    ) {
        if (podName == null || podName.isBlank()) {
            throw new IllegalArgumentException("podName cannot be blank");
        }
        if (tailLines < 1 || tailLines > 500) {
            throw new IllegalArgumentException("tailLines must be between 1 and 500");
        }
        return "Pod " + podName + " logs [tail " + tailLines + "]: ThreadPoolExecutor rejected execution, queue capacity 50 reached";
    }
}
```

### `JfrAnalysisTool.java`
```java
package phase08.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class JfrAnalysisTool {

    @Tool(description = "Query lock contention events from JFR recording")
    public String queryLockContention(@ToolParam(description = "Target microservice name") String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName cannot be blank");
        }
        return "Service " + serviceName + " lock contention: HikariCP connection pool acquisition lock duration 4200ms";
    }

    @Tool(description = "Query allocation and GC events from JFR recording")
    public String queryMemoryAllocations(@ToolParam(description = "Target microservice name") String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName cannot be blank");
        }
        return "Service " + serviceName + " memory allocations: High allocation rate in Jackson ObjectMapper deserialization";
    }
}
```

---

## 5. Advisors (`phase08.advisor`)

### `SecurityKeywordAdvisor.java`
```java
package phase08.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityKeywordAdvisor implements CallAdvisor, Ordered {

    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "drop database",
            "rm -rf /",
            "delete from users",
            "exfiltrate",
            "system-prompt-leak"
    );

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String promptText = request.prompt() != null && request.prompt().getContents() != null
                ? request.prompt().getContents().toLowerCase()
                : "";

        for (String blocked : BLOCKED_KEYWORDS) {
            if (promptText.contains(blocked)) {
                throw new SecurityException("Security guardrail violation: Prompt contains prohibited pattern: " + blocked);
            }
        }

        return chain.nextCall(request);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public String getName() {
        return "SecurityKeywordAdvisor";
    }
}
```

### `TokenBudgetAdvisor.java`
```java
package phase08.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicInteger;

public class TokenBudgetAdvisor implements CallAdvisor, Ordered {

    private final int tokenLimit;
    private final AtomicInteger accumulatedTokens = new AtomicInteger(0);

    public TokenBudgetAdvisor(int tokenLimit) {
        if (tokenLimit <= 0) {
            throw new IllegalArgumentException("tokenLimit must be positive");
        }
        this.tokenLimit = tokenLimit;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();
            if (usage != null) {
                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int genTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                accumulatedTokens.addAndGet(promptTokens + genTokens);
            }
        }

        if (accumulatedTokens.get() > tokenLimit) {
            throw new IllegalStateException("Token budget circuit breaker tripped: " + accumulatedTokens.get() + " exceeds limit " + tokenLimit);
        }

        return response;
    }

    public int getAccumulatedTokens() {
        return accumulatedTokens.get();
    }

    public void reset() {
        accumulatedTokens.set(0);
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public String getName() {
        return "TokenBudgetAdvisor";
    }
}
```

---

## 6. Evaluators (`phase08.evaluator`)

### `NoiseFloorGate.java`
```java
package phase08.evaluator;

import phase08.model.DiagnosticTelemetry;

public class NoiseFloorGate {

    public record KeepDecision(boolean keep, String reason) {}

    public KeepDecision evaluate(
            DiagnosticTelemetry baseline,
            DiagnosticTelemetry candidate,
            double p95FloorMs,
            double rpsFloor
    ) {
        if (baseline == null || candidate == null) {
            throw new IllegalArgumentException("Telemetry cannot be null");
        }

        double p95Delta = baseline.p95Ms() - candidate.p95Ms();
        double rpsDelta = candidate.rps() - baseline.rps();
        boolean failRateSafe = candidate.errorRate() <= baseline.errorRate();

        boolean p95Cleared = p95Delta > p95FloorMs;
        boolean rpsCleared = rpsDelta > rpsFloor;

        if ((p95Cleared || rpsCleared) && failRateSafe) {
            String type = p95Cleared ? "P95 latency" : "Throughput RPS";
            return new KeepDecision(true, "Keep confirmed: " + type + " cleared floor without error regression");
        }

        if ((p95Cleared || rpsCleared) && !failRateSafe) {
            return new KeepDecision(false, "Rejected: Metric improved but error rate regressed (" + candidate.errorRate() + " > " + baseline.errorRate() + ")");
        }

        return new KeepDecision(false, "Rejected: Metrics delta within noise floor (p95Delta=" + p95Delta + ", rpsDelta=" + rpsDelta + ")");
    }
}
```

### `GroundingEvaluator.java`
```java
package phase08.evaluator;

import java.util.List;

public class GroundingEvaluator {

    public boolean isGrounded(String hypothesis, List<String> toolObservations) {
        if (hypothesis == null || hypothesis.isBlank()) {
            throw new IllegalArgumentException("hypothesis cannot be blank");
        }
        if (toolObservations == null || toolObservations.isEmpty()) {
            return false;
        }

        String lowerHypothesis = hypothesis.toLowerCase();
        for (String observation : toolObservations) {
            if (observation == null) continue;
            String lowerObs = observation.toLowerCase();

            for (String keyword : List.of("thread", "lock", "connection", "pool", "queue", "memory", "gc", "oom")) {
                if (lowerHypothesis.contains(keyword) && lowerObs.contains(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }
}
```

---

## 7. Saga & Persistence (`phase08.saga`, `phase08.repository`)

### `VirtualWorkspace.java`
```java
package phase08.saga;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VirtualWorkspace {

    private String headSha;
    private final Map<String, String> files = new HashMap<>();
    private final Map<String, Map<String, String>> commitHistory = new HashMap<>();

    public VirtualWorkspace(String initialSha, Map<String, String> initialFiles) {
        if (initialSha == null || initialSha.isBlank()) {
            throw new IllegalArgumentException("initialSha cannot be blank");
        }
        this.headSha = initialSha;
        if (initialFiles != null) {
            this.files.putAll(initialFiles);
            this.commitHistory.put(initialSha, new HashMap<>(initialFiles));
        } else {
            this.commitHistory.put(initialSha, new HashMap<>());
        }
    }

    public synchronized void commit(String sha, Map<String, String> newFiles) {
        if (sha == null || sha.isBlank()) {
            throw new IllegalArgumentException("Commit sha cannot be blank");
        }
        this.headSha = sha;
        if (newFiles != null) {
            this.files.clear();
            this.files.putAll(newFiles);
        }
        this.commitHistory.put(sha, new HashMap<>(this.files));
    }

    public synchronized void revertTo(String targetSha) {
        if (!commitHistory.containsKey(targetSha)) {
            throw new IllegalArgumentException("Cannot revert: Unknown SHA " + targetSha);
        }
        this.headSha = targetSha;
        this.files.clear();
        this.files.putAll(commitHistory.get(targetSha));
    }

    public synchronized void writeFile(String path, String content) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("File path cannot be blank");
        }
        files.put(path, content != null ? content : "");
    }

    public synchronized String readFile(String path) {
        return files.get(path);
    }

    public synchronized String getHeadSha() {
        return headSha;
    }

    public synchronized Map<String, String> getFiles() {
        return Collections.unmodifiableMap(new HashMap<>(files));
    }
}
```

### `AutonomousSagaLoop.java`
```java
package phase08.saga;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import phase08.config.AgentProperties;
import phase08.evaluator.NoiseFloorGate;
import phase08.model.*;
import phase08.parser.TriageProposalParser;
import phase08.prompt.TriagePromptBuilder;
import phase08.repository.TrajectoryRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

public class AutonomousSagaLoop {

    private final ChatClient chatClient;
    private final ToolCallback[] tools;
    private final TriagePromptBuilder promptBuilder;
    private final TriageProposalParser parser;
    private final NoiseFloorGate noiseFloorGate;
    private final TrajectoryRepository repository;
    private final AgentProperties properties;

    public AutonomousSagaLoop(
            ChatClient chatClient,
            ToolCallback[] tools,
            TriagePromptBuilder promptBuilder,
            TriageProposalParser parser,
            NoiseFloorGate noiseFloorGate,
            TrajectoryRepository repository,
            AgentProperties properties
    ) {
        this.chatClient = chatClient;
        this.tools = tools != null ? tools : new ToolCallback[0];
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.noiseFloorGate = noiseFloorGate;
        this.repository = repository;
        this.properties = properties;
    }

    public TriageResponseDto runLoop(
            String incidentId,
            String serviceName,
            String description,
            VirtualWorkspace workspace,
            Function<TriageProposal, DiagnosticTelemetry> telemetrySupplier
    ) {
        String lastKeptSha = workspace.getHeadSha();
        DiagnosticTelemetry currentBaseline = DiagnosticTelemetry.baseline();
        int iteration = 0;
        String finalStatus = "UNRESOLVED";
        String rootCause = "UNKNOWN";

        recordEvent(incidentId, 0, SagaState.IDLE, "Loop initialized at sha=" + lastKeptSha);

        while (iteration < properties.maxIterations()) {
            iteration++;
            recordEvent(incidentId, iteration, SagaState.DECIDE, "Starting decision turn " + iteration);

            String prompt = promptBuilder.buildUserPrompt(incidentId, serviceName, description, currentBaseline);
            String rawLlmResponse = chatClient.prompt()
                    .user(prompt)
                    .tools((Object[]) tools)
                    .call()
                    .content();

            Optional<TriageProposal> proposalOpt = parser.parseWithOneShotRepair(rawLlmResponse, repairPrompt ->
                    chatClient.prompt().user(repairPrompt).call().content()
            );

            if (proposalOpt.isEmpty()) {
                recordEvent(incidentId, iteration, SagaState.DECIDE, "Schema parse failed after 1-shot repair; skipping");
                continue;
            }

            TriageProposal proposal = proposalOpt.get();
            rootCause = proposal.hypothesis();
            recordEvent(incidentId, iteration, SagaState.APPLY, "Applying proposal: " + proposal.action());

            if (proposal.targetFile() != null && proposal.patchContent() != null) {
                workspace.writeFile(proposal.targetFile(), proposal.patchContent());
            }

            recordEvent(incidentId, iteration, SagaState.MEASURE, "Harvesting telemetry for proposal");
            DiagnosticTelemetry candidateTelemetry = telemetrySupplier != null
                    ? telemetrySupplier.apply(proposal)
                    : currentBaseline;

            recordEvent(incidentId, iteration, SagaState.JUDGE, "Evaluating telemetry delta");
            NoiseFloorGate.KeepDecision verdict = noiseFloorGate.evaluate(
                    currentBaseline,
                    candidateTelemetry,
                    properties.p95FloorMs(),
                    properties.rpsFloor()
            );

            if (verdict.keep()) {
                String newSha = "sha-" + iteration;
                workspace.commit(newSha, workspace.getFiles());
                lastKeptSha = newSha;
                currentBaseline = candidateTelemetry;
                finalStatus = "RESOLVED";
                recordEvent(incidentId, iteration, SagaState.FINISH, "Decision KEPT, committed " + newSha + ": " + verdict.reason());
                break;
            } else {
                recordEvent(incidentId, iteration, SagaState.COMPENSATE, "Decision REJECTED, rolling back to " + lastKeptSha);
                workspace.revertTo(lastKeptSha);
            }
        }

        recordEvent(incidentId, iteration, SagaState.FINISH, "Loop terminated with status " + finalStatus);
        return new TriageResponseDto(incidentId, rootCause, finalStatus, iteration, 0, workspace.getHeadSha());
    }

    private void recordEvent(String incidentId, int iteration, SagaState state, String summary) {
        if (repository != null) {
            repository.save(incidentId, new TrajectoryEvent(incidentId, iteration, state, summary, Instant.now()));
        }
    }
}
```

### `InMemoryTrajectoryRepository.java`
```java
package phase08.repository;

import org.springframework.stereotype.Repository;
import phase08.model.TrajectoryEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryTrajectoryRepository implements TrajectoryRepository {

    private final Map<String, List<TrajectoryEvent>> store = new ConcurrentHashMap<>();

    @Override
    public void save(String incidentId, TrajectoryEvent event) {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incidentId cannot be blank");
        }
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        store.computeIfAbsent(incidentId, k -> new CopyOnWriteArrayList<>()).add(event);
    }

    @Override
    public List<TrajectoryEvent> findByIncidentId(String incidentId) {
        if (incidentId == null) {
            return List.of();
        }
        List<TrajectoryEvent> list = store.get(incidentId);
        return list != null ? Collections.unmodifiableList(list) : List.of();
    }
}
```

---

## 8. Actuator Health, Service & Controller (`phase08.health`, `phase08.service`, `phase08.web`)

### `AgentHealthIndicator.java`
```java
package phase08.health;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AgentHealthIndicator implements HealthIndicator {

    private final ChatModel chatModel;
    private final String modelName;

    @org.springframework.beans.factory.annotation.Autowired
    public AgentHealthIndicator(ChatModel chatModel) {
        this(chatModel, "default-llm");
    }

    public AgentHealthIndicator(ChatModel chatModel, String modelName) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.modelName = modelName != null ? modelName : "unknown";
    }

    @Override
    public Health health() {
        try {
            var response = chatModel.call(new Prompt("health-check-ping"));
            if (response != null && response.getResult() != null) {
                return Health.up()
                        .withDetail("model", modelName)
                        .withDetail("status", "AVAILABLE")
                        .build();
            }
            return Health.down()
                    .withDetail("model", modelName)
                    .withDetail("status", "EMPTY_RESPONSE")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("model", modelName)
                    .withDetail("status", "UNAVAILABLE")
                    .build();
        }
    }
}
```

### `IncidentTriageService.java`
```java
package phase08.service;

import org.springframework.stereotype.Service;
import phase08.config.AgentProperties;
import phase08.model.DiagnosticTelemetry;
import phase08.model.TriageRequestDto;
import phase08.model.TriageResponseDto;
import phase08.model.TrajectoryEvent;
import phase08.repository.TrajectoryRepository;
import phase08.saga.AutonomousSagaLoop;
import phase08.saga.VirtualWorkspace;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class IncidentTriageService {

    private final AutonomousSagaLoop sagaLoop;
    private final TrajectoryRepository repository;
    private final AgentProperties properties;

    public IncidentTriageService(
            AutonomousSagaLoop sagaLoop,
            TrajectoryRepository repository,
            AgentProperties properties
    ) {
        this.sagaLoop = Objects.requireNonNull(sagaLoop, "sagaLoop required");
        this.repository = Objects.requireNonNull(repository, "repository required");
        this.properties = Objects.requireNonNull(properties, "properties required");
    }

    public TriageResponseDto triageIncident(TriageRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (request.incidentId() == null || request.incidentId().isBlank()) {
            throw new IllegalArgumentException("incidentId cannot be blank");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }

        VirtualWorkspace workspace = new VirtualWorkspace("sha-0", Map.of(
                "App.java", "// Baseline application source code",
                "application.properties", "server.tomcat.threads.max=10"
        ));

        return sagaLoop.runLoop(
                request.incidentId(),
                request.serviceName() != null ? request.serviceName() : "core-service",
                request.description(),
                workspace,
                proposal -> {
                    String patch = proposal.patchContent() != null ? proposal.patchContent().toLowerCase() : "";
                    if (patch.contains("hikari") || patch.contains("threads") || patch.contains("pool")) {
                        return new DiagnosticTelemetry(200.0, 180.0, 0.01);
                    }
                    return new DiagnosticTelemetry(340.0, 105.0, 0.05);
                }
        );
    }

    public List<TrajectoryEvent> getTrajectory(String incidentId) {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incidentId cannot be blank");
        }
        return repository.findByIncidentId(incidentId);
    }
}
```

### `IncidentTriageController.java`
```java
package phase08.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phase08.model.TrajectoryEvent;
import phase08.model.TriageRequestDto;
import phase08.model.TriageResponseDto;
import phase08.service.IncidentTriageService;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/incidents")
public class IncidentTriageController {

    private final IncidentTriageService triageService;

    public IncidentTriageController(IncidentTriageService triageService) {
        this.triageService = Objects.requireNonNull(triageService, "triageService required");
    }

    @PostMapping("/triage")
    public ResponseEntity<TriageResponseDto> triageIncident(@RequestBody TriageRequestDto request) {
        if (request == null || request.incidentId() == null || request.incidentId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.description() == null || request.description().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            TriageResponseDto response = triageService.triageIncident(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/{id}/trajectory")
    public ResponseEntity<List<TrajectoryEvent>> getTrajectory(@PathVariable("id") String incidentId) {
        if (incidentId == null || incidentId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<TrajectoryEvent> trajectory = triageService.getTrajectory(incidentId);
        return ResponseEntity.ok(trajectory);
    }
}
```
