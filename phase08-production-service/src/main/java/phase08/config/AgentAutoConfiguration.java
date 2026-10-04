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

/**
 * Phase 08: Spring Boot Dependency Injection Auto-Configuration.
 * <p>
 * Wires the end-to-end agentic triage architecture into the Spring ApplicationContext.
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    /**
     * Wires the dynamic prompt templating component.
     */
    @Bean
    @ConditionalOnMissingBean
    public TriagePromptBuilder triagePromptBuilder() {
        return new TriagePromptBuilder();
    }

    /**
     * Wires the structured output proposal parser with 1-shot repair capabilities.
     */
    @Bean
    @ConditionalOnMissingBean
    public TriageProposalParser triageProposalParser() {
        return new TriageProposalParser();
    }

    /**
     * Wires the deterministic telemetry noise floor gate.
     */
    @Bean
    @ConditionalOnMissingBean
    public NoiseFloorGate noiseFloorGate() {
        return new NoiseFloorGate();
    }

    /**
     * Wires the claim grounding evaluator.
     */
    @Bean
    @ConditionalOnMissingBean
    public GroundingEvaluator groundingEvaluator() {
        return new GroundingEvaluator();
    }

    /**
     * Wires the token budget circuit-breaker advisor (Order 20).
     */
    @Bean
    @ConditionalOnMissingBean
    public TokenBudgetAdvisor tokenBudgetAdvisor(AgentProperties props) {
        return new TokenBudgetAdvisor(props.tokenBudget());
    }

    /**
     * Wires the fluent ChatClient.Builder factory for the injected ChatModel.
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    /**
     * Reflectively discovers and registers all @Tool methods across diagnostic beans.
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolCallback[] diagnosticTools(K8sDiagnosticTool k8sTool, JfrAnalysisTool jfrTool) {
        return ToolCallbacks.from(k8sTool, jfrTool);
    }

    /**
     * Assembles the ChatClient with default system prompt and ordered advisor pipeline.
     */
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

    /**
     * Wires the 6-phase Autonomous Saga Loop orchestrator.
     */
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
