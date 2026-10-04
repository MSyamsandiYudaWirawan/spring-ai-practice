# Phase 08: Production Autonomous Incident Triage Microservice (Capstone)

> ðŸ“˜ **Full Architecture, Class Requirements & Test Specifications:** See [docs/phase08-production-service-specification.md](../docs/phase08-production-service-specification.md) for detailed class contracts, domain invariants, and test specifications.

## 1. Architectural Overview
This capstone synthesizes all foundational concepts mastered across **Phases 01 through 07** into an enterprise-grade, multi-class Spring Boot microservice:

```
[ Inbound HTTP POST /api/incidents/triage ]
                      |
                      v
       [ IncidentTriageController ]
                      |
                      v
        [ IncidentTriageService ]
                      |
                      v
          [ AutonomousSagaLoop ] <-----------------------------+
         /          |           \                              |
        v           v            v                             |
 [ ChatClient ] [ Tools ]  [ TrajectoryRepository ]            |
        |           |                                          |
 [ Advisors ]       +----> [ K8sDiagnosticTool ]               |
   - Security              [ JfrAnalysisTool ]                 |
   - TokenBudget                                               |
        |                                                      |
 [ ChatModel ] (Seam)                                          |
        |                                                      |
        v                                                      |
 [ TriageProposalParser ] (Phase 2: 1-shot repair)             |
        |                                                      |
        v                                                      |
 [ VirtualWorkspace ] (Phase 7: Git commit/revert)             |
        |                                                      |
        v                                                      |
 [ NoiseFloorGate ] (Phase 6: Telemetry Delta Keep) -----------+
```

---

## 2. Knowledge Synthesis Matrix (Phases 01 â€“ 07)

| Phase | Framework Mechanics | Implemented Component in Phase 08 |
|---|---|---|
| **Phase 01** | System/user prompt templating, dynamic context parameters | [`TriagePromptBuilder.java`](src/main/java/phase08/prompt/TriagePromptBuilder.java) |
| **Phase 02** | Structured outputs, markdown fence stripping, 1-shot repair | [`TriageProposalParser.java`](src/main/java/phase08/parser/TriageProposalParser.java) |
| **Phase 03** | `@Tool` calling, parameter validation, dynamic reflection | [`K8sDiagnosticTool.java`](src/main/java/phase08/tool/K8sDiagnosticTool.java), [`JfrAnalysisTool.java`](src/main/java/phase08/tool/JfrAnalysisTool.java) |
| **Phase 04** | `CallAdvisor`, `Ordered`, token budget circuit-breaker | [`SecurityKeywordAdvisor.java`](src/main/java/phase08/advisor/SecurityKeywordAdvisor.java), [`TokenBudgetAdvisor.java`](src/main/java/phase08/advisor/TokenBudgetAdvisor.java) |
| **Phase 05** | Decoupled `ChatModel` seam, offline deterministic double | [`FakeProductionChatModel.java`](src/test/java/phase08/testdouble/FakeProductionChatModel.java) |
| **Phase 06** | Telemetry delta keep gates, noise floors, grounding check | [`NoiseFloorGate.java`](src/main/java/phase08/evaluator/NoiseFloorGate.java), [`GroundingEvaluator.java`](src/main/java/phase08/evaluator/GroundingEvaluator.java) |
| **Phase 07** | 6-phase Saga FSM, atomic git-like tree, compensation | [`VirtualWorkspace.java`](src/main/java/phase08/saga/VirtualWorkspace.java), [`AutonomousSagaLoop.java`](src/main/java/phase08/saga/AutonomousSagaLoop.java) |
| **Phase 08** | Multi-class DI, `@ConfigurationProperties`, Actuator, REST | [`AgentProperties.java`](src/main/java/phase08/config/AgentProperties.java), [`AgentAutoConfiguration.java`](src/main/java/phase08/config/AgentAutoConfiguration.java), [`IncidentTriageController.java`](src/main/java/phase08/web/IncidentTriageController.java) |

---

## 3. Package Structure
```
phase08-production-service/
â”œâ”€â”€ src/main/java/phase08/
â”‚   â”œâ”€â”€ Application.java               # Spring Boot Application entry point
â”‚   â”œâ”€â”€ config/
â”‚   â”‚   â”œâ”€â”€ AgentProperties.java       # @ConfigurationProperties("agent.triage")
â”‚   â”‚   â””â”€â”€ AgentAutoConfiguration.java # Bean wiring and dependency injection
â”‚   â”œâ”€â”€ model/
â”‚   â”‚   â”œâ”€â”€ IncidentSeverity.java      # LOW, MEDIUM, HIGH, CRITICAL
â”‚   â”‚   â”œâ”€â”€ DiagnosticTelemetry.java   # p95Ms, rps, errorRate
â”‚   â”‚   â”œâ”€â”€ TriageProposal.java        # Structured output schema
â”‚   â”‚   â”œâ”€â”€ SagaState.java             # IDLE, DECIDE, APPLY, MEASURE, JUDGE, COMPENSATE, FINISH
â”‚   â”‚   â”œâ”€â”€ TrajectoryEvent.java       # Audit record
â”‚   â”‚   â”œâ”€â”€ TriageRequestDto.java      # REST Request DTO
â”‚   â”‚   â””â”€â”€ TriageResponseDto.java     # REST Response DTO
â”‚   â”œâ”€â”€ prompt/
â”‚   â”‚   â””â”€â”€ TriagePromptBuilder.java   # Dynamic templated prompts
â”‚   â”œâ”€â”€ parser/
â”‚   â”‚   â””â”€â”€ TriageProposalParser.java  # Schema extraction & 1-shot repair
â”‚   â”œâ”€â”€ tool/
â”‚   â”‚   â”œâ”€â”€ K8sDiagnosticTool.java     # @Tool pod inspection
â”‚   â”‚   â””â”€â”€ JfrAnalysisTool.java       # @Tool lock/allocation profiling
â”‚   â”œâ”€â”€ advisor/
â”‚   â”‚   â”œâ”€â”€ SecurityKeywordAdvisor.java# Prompt security filter (Order 10)
â”‚   â”‚   â””â”€â”€ TokenBudgetAdvisor.java    # Token spend circuit breaker (Order 20)
â”‚   â”œâ”€â”€ evaluator/
â”‚   â”‚   â”œâ”€â”€ NoiseFloorGate.java        # Telemetry evaluation vs noise floors
â”‚   â”‚   â””â”€â”€ GroundingEvaluator.java    # Claim grounding evaluator
â”‚   â”œâ”€â”€ saga/
â”‚   â”‚   â”œâ”€â”€ VirtualWorkspace.java      # Atomic git-like tree (commit & revertTo)
â”‚   â”‚   â””â”€â”€ AutonomousSagaLoop.java    # Orchestrator of the 6-phase Saga FSM
â”‚   â”œâ”€â”€ repository/
â”‚   â”‚   â”œâ”€â”€ TrajectoryRepository.java  # Interface
â”‚   â”‚   â””â”€â”€ InMemoryTrajectoryRepository.java # Thread-safe implementation
â”‚   â”œâ”€â”€ health/
â”‚   â”‚   â””â”€â”€ AgentHealthIndicator.java  # Actuator model availability probe
â”‚   â”œâ”€â”€ service/
â”‚   â”‚   â””â”€â”€ IncidentTriageService.java # Business orchestration service
â”‚   â””â”€â”€ web/
â”‚       â””â”€â”€ IncidentTriageController.java # REST API controller
```

---

## 4. Test Suite Execution

Run the complete test suite across all classes:
```bash
mvn test -pl phase08-production-service
```

Run a specific class unit test:
```bash
# Configuration & Properties
mvn test -pl phase08-production-service -Dtest=AgentPropertiesTest

# Prompt Templating
mvn test -pl phase08-production-service -Dtest=TriagePromptBuilderTest

# Structured Outputs & 1-Shot Repair
mvn test -pl phase08-production-service -Dtest=TriageProposalParserTest

# Dynamic Tools
mvn test -pl phase08-production-service -Dtest=DiagnosticToolsTest

# Guardrail Advisors
mvn test -pl phase08-production-service -Dtest=SecurityKeywordAdvisorTest
mvn test -pl phase08-production-service -Dtest=TokenBudgetAdvisorTest

# Noise Floor Evaluator
mvn test -pl phase08-production-service -Dtest=NoiseFloorGateTest

# In-Memory Git Workspace & Saga Loop
mvn test -pl phase08-production-service -Dtest=VirtualWorkspaceTest
mvn test -pl phase08-production-service -Dtest=AutonomousSagaLoopTest

# Actuator Health
mvn test -pl phase08-production-service -Dtest=AgentHealthIndicatorTest

# REST Controller
mvn test -pl phase08-production-service -Dtest=IncidentTriageControllerTest

# End-to-End Spring Boot Integration Test
mvn test -pl phase08-production-service -Dtest=IncidentTriageEndToEndIntegrationTest
```
