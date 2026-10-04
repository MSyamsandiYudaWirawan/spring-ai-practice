# Spring AI Practice — Session Context & Handoff State

**Document Purpose:** Master state and context handoff for future agent sessions and developer reference. Read this file first when resuming work in this workspace.

---

## 1. High-Level Mission & Pedagogical Protocol

- **Target Goal:** Bridge the gap from Spring AI beginner to authoring production-grade agentic architectures with cold muscle memory.
- **Reference Codebase Status:** The reference project [`agentic-performance-diagnostician`](https://github.com/syamsandi/agentic-performance-diagnostician) has achieved **v1.0 Capstone Completion (Steps 0–12 fully implemented and verify-gated)**. 
- **Core Value Proposition:** This practice repository enables engineers to build instinctive muscle memory across Spring AI primitives in isolation before assembling them into full-scale autonomous diagnostic systems like `agentic-performance-diagnostician`.
- **Core Problem Solved:** In complex agentic systems, pure LLM inference is ~20% of the code; 80% is plumbing, state management, schema validation, guardrails, and deterministic testing. Repetitive, isolated muscle-memory drills build the confidence needed to write this code cold.
- **Pedagogical Rules of Engagement:**
  1. **Strict Seam Separation:** Code-under-test (`*UnderTest.java`) is the **ONLY** file modified by the user. Rulers, contracts, mocks, and verifiers are immutable.
  2. **Deterministic Verifier Gate:** Exits with status `99` on measured failure (k6 finding convention), and status `0` only when all scenario gates pass.
  3. **Zero Token Cost / Instant Feedback:** Uses deterministic in-memory `ChatModel` test doubles (`FakeChatModel`, `FakeSecurityChatModel`) that record outgoing `Prompt` objects for assertion.
  4. **Sealed Golden Solutions:** Stored under `docs/golden/phaseXX-exYY-golden.md`. Unsealed only after the user attempts the exercise.
  5. **TigerStyle Discipline:** Explicit boundary validation (`Objects.requireNonNull`, `isBlank()`), defensive unboxing on nullable tokens, pure `.getText()` extraction, fail-fast invariants, no silent swallows.
  6. **Concise Package Naming:** Standard package names are concise: `phase01`, `phase02`, `phase03`, etc. (avoid deep nesting like `io.diag.practice...`).
  7. **Repetitive Muscle-Memory Drills (High Repetition Over Bespoke Plumbing):** Exercises must not be trivial 2-3 scenario tasks with unique, ad-hoc solutions. Each drill must provide 8–10 scenarios practicing framework mechanics repeatedly (annotations, parameter types, records, enums, envelopes, validations, audit trails, and client registrations) so developers build instinctive fluency without irrelevant plumbing.
  8. **Zero Flakiness & Explicit Contract Documentation (The Non-Flaky Test Rule):** Verifiers must NEVER rely on hidden, brittle string checks. If an assertion checks for error messages or substrings (e.g. `.contains(...)`), the exact expected substring or keyword contract MUST be explicitly documented in:
     - Method Javadoc/comments in the code-under-test (`*UnderTest.java`).
     - The exercise `README.md`.
     - In addition, verifiers must use case-resilient matching (`.toLowerCase().contains(...)`) with reasonable synonyms so tests are never flaky.
  9. **Zero Specification Guesswork:** A failing test must indicate a real logic or contract breach, never a guessing game about secret verifier keywords.

---

## 2. Curriculum & Exercise Progress Status

| Phase | Description | Reference Diagnostician Mapping | Status | Exercises Completed |
|---|---|---|---|---|
| **Phase 01** | `ChatClient` Fluent API & Prompt Templating | `DecideTurn`, `SystemPrompts` (Step 8) | ✅ **COMPLETED** | 3 exercises (21 scenarios total) |
| **Phase 02** | Structured Outputs & Resilient Schema Extraction | `DecisionValidator`, `DecisionDto` (Step 8) | ✅ **COMPLETED** | 2 exercises (16 scenarios total) |
| **Phase 03** | Multi-Turn Tool Calling (`@Tool`) & Sandboxing | `BoundedReadSource`, tool-call bounding (Step 8/9) | ✅ **COMPLETED** | 2 exercises (18 scenarios total) |
| **Phase 04** | Guardrails, Advisors & Token Budgets | `LoopConfig`, guardrail circuit-breakers (Step 9/12) | ✅ **COMPLETED** | 2 exercises (20 scenarios total) |
| **Phase 05** | Deterministic Testing & Offline Harness | `AgentLoopFactory`, mock loop seams (Step 9/11) | ✅ **COMPLETED** | 2 exercises (20 scenarios total) |
| **Phase 06** | LLM-as-a-Judge & Evaluators | `KeepRule`, `EvalScorer`, ground-truth scoring (Step 9/11) | ✅ **COMPLETED** | 2 exercises (20 scenarios total) |
| **Phase 07** | The Saga Agent Loop (Capstone I) | Full `AgentLoop`, `revertTo`, `DiagnosticTriage` (Step 9/12) | ✅ **COMPLETED** | 2 exercises (20 scenarios total) |
| **Phase 08** | Production Multi-Class Wiring & Spring Boot DI (Capstone II) | Multi-class Spring Boot microservice, auto-config, REST (Step 10/12) | ✅ **COMPLETED** | 2 exercises (20 scenarios total) |

### Phase 01 Completed History:
1. `phase01-ex01-chat-basics`: Core roles, prompt templating, builder defaults (3 scenarios). **PASSED (exit 0)**.
2. `phase01-ex02-chat-advanced`: FinTech Fraud Ops: role separation, Map templates, parameterized system spec, call-level options, history replay, few-shots, client mutation, usage audit (8 scenarios). **PASSED (exit 0)**.
3. `phase01-ex03-cloud-sre`: Kubernetes Remediation: fine-grained ChatOptions (`stopSequences`, `frequencyPenalty`, `maxTokens`), environment policy injection, defensive token auditing, paging envelopes, fail-fast response invariants (10 scenarios). **PASSED (exit 0)**.

### Phase 02 Completed History:
1. `phase02-ex01-structured-outputs`: Autonomous Security Vulnerability Assessment & Patch Proposal Gateway (8 scenarios: direct deserialization, markdown fence stripping, generic collections, entity dispatch, domain invariants, triage envelope, one-shot repair, retry limits). **PASSED (exit 0)**.
2. `phase02-ex02-resilient-decisions`: Performance Diagnostic Decision Gateway & Resilient Schema Repair (8 scenarios: schema format directives injection, direct deserialization, conversational sanitization, TigerStyle cross-field invariants, generic collections batch parsing, fallback prose diagnosis reconstruction, closed-loop one-shot repair, retry circuit-breaker). **PASSED (exit 0)**.

### Phase 03 Completed History:
1. `phase03-ex01-tool-sandboxing`: Autonomous Diagnostic Tool Sandboxing & Execution Bounding (8 scenarios). **7/8 PASSED**.
2. `phase03-ex02-multi-tool-drills`: Repetitive Spring AI `@Tool` Calling Drills (10 scenarios: primitive strings, numeric range, enum dispatch, structured record schemas, envelope wrapping, regex validation, quota rate limiting, reflection discovery, ChatClient tool registration, multi-tool audit logging). **10/10 PASSED (exit 0)**.

### Phase 04 Completed History:
1. `phase04-ex01-guardrail-advisors`: Production Agentic Guardrail Stack & Execution Budget Circuit-Breakers (10 scenarios: `AuditLoggingAdvisor`, `KeywordGuardrailAdvisor`, `SystemPromptEnforcingAdvisor`, `TokenBudgetAdvisor`, `CostCircuitBreakerAdvisor`, `LatencyGuardrailAdvisor`, `PiiMaskingAdvisor`, `OrderTrackingAdvisor`, default/call-level advisor assembly, and comprehensive integrated guardrail stack). **10/10 PASSED (exit 0)**.
2. `phase04-ex02-resilient-guardrails`: Resilient Guardrails, Failover Routing, and Transactional Token Quotas (10 scenarios: `RuntimeContextAdvisor`, `SlidingWindowTruncationAdvisor`, `SlidingWindowRateLimiterAdvisor`, `ModelFailoverAdvisor`, `SchemaSelfHealingAdvisor`, `GroundingValidationAdvisor`, `TraceContextPropagationAdvisor`, `DynamicTokenQuotaAdvisor`, short-circuiting client, and production resilient guardrail stack). **10/10 PASSED (exit 0)**.

---

## 3. Active Workspace State (Phase 05: Exercises 01 & 02)

### Exercise 01: Foundational Repetitive Seams (`phase05-ex01-offline-harness`)
- **Theme:** Deterministic Offline Test Harness & Double Seams (10 Repetitive Foundational Scenarios)
- **Pedagogical Mandate:** Build cold muscle memory on decoupled test double seams:
  1. `ScriptedChatPort`: FIFO queue test double (`enqueue`, `callCount`, `chat` polling with empty-queue exception).
  2. `PredicateRoutingChatPort`: dynamic pattern-matching test double (`when(matcher, result)`, `setDefaultResult`, ordered rule evaluation).
  3. `FaultInjectingChatPort`: simulated upstream transient faults and retries (`enqueueException`, `enqueueResult`, consecutive failure simulation).
  4. `PromptCapturingChatPort`: outgoing prompt spy and inspection seam (captures `system`, `user`, and `toolBeans`).
  5. `BudgetEnforcingChatPort`: offline test token spend ceiling guardrail (accumulates tokens and throws `"Offline token budget exceeded"` on breach).
  6. `ToolRegistrySpy`: tool bean reflection discovery seam (inspects `@Tool` methods on injected beans and returns tool names).
  7. `InMemoryStreamingChatPort`: reactive `Flux<String>` streaming double for deterministic token streaming tests.
  8. `VirtualTimeSimulator`: deterministic virtual clock seam (`advance`, `advanceMillis`, `simulateElapsedMillis` with zero thread sleep).
  9. `SpringAiChatPort`: framework adapter bridging domain `ChatPort` to Spring AI `ChatClient` with defensive usage extraction.
  10. `DeterministicDiagnosticLoop`: end-to-end multi-turn triage loop coordinating scripted responses, tool execution, token accounting, and virtual time.
- **Verification:** `mvn test-compile exec:java -pl phase05-ex01-offline-harness` (0/10 passing baseline).
- **Target File:** [`phase05-ex01-offline-harness/src/main/java/phase05/OfflineHarnessUnderTest.java`](./phase05-ex01-offline-harness/src/main/java/phase05/OfflineHarnessUnderTest.java)

### Exercise 02: Advanced Repetitive + Difficult Chaos Seams (`phase05-ex02-advanced-harness`)
- **Theme:** Advanced Offline Chaos Harness, Circuit Breakers, Streaming Latency, HTTP Stubs, and Conversation Replay (10 Advanced Repetitive Scenarios)
- **Pedagogical Mandate:** Stress resilient agent loop seams under adverse, production-like conditions entirely offline:
  1. `ChaosInjectingChatPort`: Seeded probabilistic failure (`random.nextDouble() < matrix.failureRate()`) & payload truncation.
  2. `StreamingChunkLatencySimulator`: Reactive `Flux<String>` chunk emission tracking and cumulative simulated latency.
  3. `RateLimitBackoffChatPort`: Virtual backoff retry loop on HTTP 429 / rate limits using `AdvancedTestClock` (`"Rate limit retries exhausted"`).
  4. `DeterministicConversationReplayer`: Replays a golden multi-turn trajectory and asserts agent state transitions.
  5. `CircuitBreakingChatPort`: State machine double (`CLOSED` -> `OPEN` -> `HALF_OPEN` -> `CLOSED`) with cooldown (`"Circuit breaker is OPEN"`).
  6. `MultiTenantBudgetPartitionPort`: Hierarchical per-tenant token allocation and quota enforcement (`"Quota exceeded for tenant"`).
  7. `ConcurrencyRaceDetectorChatPort`: Thread-safe double handling 200 concurrent calls with atomic token accounting.
  8. `AdversePayloadCorruptor & ResilientJsonExtractor`: Defends against truncated / corrupted JSON payloads with fallback extraction.
  9. `ResilientFailoverChatPort`: Transparent failover from primary failing endpoint to secondary fallback stub.
  10. `AutonomousChaosTriageHarness`: Full 3-turn autonomous chaos triage lifecycle combining circuit breakers, tenants, and recovery.
- **Current Student State:** 3/10 passed (Scenarios 1, 2, 8).
- **Verification:** `mvn test-compile exec:java -pl phase05-ex02-advanced-harness`.
- **Target File:** [`phase05-ex02-advanced-harness/src/main/java/phase05/AdvancedOfflineHarnessUnderTest.java`](./phase05-ex02-advanced-harness/src/main/java/phase05/AdvancedOfflineHarnessUnderTest.java)

---

## 4. Active Workspace State (Phase 06: Exercises 01 & 02)

### Exercise 01: Foundational Repetitive Evaluators (`phase06-ex01-evaluators`)
- **Theme:** Deterministic Evaluators, Spring AI Evaluator Interface & Foundational LLM-as-a-Judge (10 Repetitive Scenarios)
- **Pedagogical Mandate:** Cement Spring AI `Evaluator`, `EvaluationRequest`, `EvaluationResponse` primitives:
  1. `RelevancyEvaluatorAdapter`: Spring AI built-in `RelevancyEvaluator` for on-topic relevance.
  2. `FactCheckingEvaluatorAdapter`: Spring AI built-in `FactCheckingEvaluator` for factual context support.
  3. `NoiseFloorThresholdEvaluator`: Deterministic telemetry keep gate comparing deltas vs noise floors with failRate guard.
  4. `GroundTruthAccuracyEvaluator`: Root cause category convergence matching against ground-truth labels.
  5. `SingleMetricThresholdEvaluator`: Configurable numeric score gate with strict [0.0, 1.0] bounds.
  6. `RubricScoreParser`: Regex/structured extraction of normalized score and reasoning from formatted LLM output.
  7. `BinaryJudgeEvaluator`: Prompt-driven LLM-as-a-judge with binary `PASS`/`FAIL` verdict.
  8. `WeightedMultiCriteriaEvaluator`: Multi-dimension score aggregator with weight sum validation.
  9. `ShortCircuitCompositeEvaluator`: Chained evaluator composite with instant fail-fast short-circuiting.
  10. `BatchEvaluationRunner`: Benchmark suite runner computing pass rates and gating on minimum thresholds (`"Evaluation threshold breach"`).
- **Verification:** `mvn test-compile exec:java -pl phase06-ex01-evaluators` (0/10 baseline).
- **Target File:** [`phase06-ex01-evaluators/src/main/java/phase06/EvaluatorUnderTest.java`](./phase06-ex01-evaluators/src/main/java/phase06/EvaluatorUnderTest.java)

### Exercise 02: Advanced Repetitive + Difficult Judges (`phase06-ex02-advanced-judges`)
- **Theme:** Advanced LLM Judges, Bias Mitigation, Calibration & Trajectory Evaluation (10 Repetitive + Difficult Scenarios)
- **Pedagogical Mandate:** Solve production AI evaluation challenges strictly within the scope of AI engineering:
  1. `PositionBiasSwapperJudge`: Pairwise A/B tournament judge with position swap mitigation (`CANDIDATE_A`, `CANDIDATE_B`, `INCONCLUSIVE_OR_TIE`).
  2. `SelfConsistencyMajorityJudge`: Stochastic voting judge sampling $N$ inferences and enforcing majority consensus.
  3. `FaithfulnessClaimAttributionEvaluator`: RAG Triad sentence-level claim decomposition and grounding verification.
  4. `AnswerRelevanceQueryDriftEvaluator`: Query intent evaluation with topic drift detection.
  5. `FewShotCalibratedRubricJudge`: Anchor-based calibrated rubric (Levels 1, 3, 5) mitigating LLM grade inflation.
  6. `AdversarialRefusalSafetyJudge`: Evaluates clean refusal of jailbreak prompts without system prompt leakage.
  7. `SaddleSafeMechanismKeepEvaluator`: Full `KeepRule` v2 from Diagnostician (RPS/P95 keeps, fail rate guards, >50% mechanism signal reduction, tail bounds).
  8. `MultiTurnAgentTrajectoryEvaluator`: Agent loop audit detecting duplicate tool calls, ping-pong cycles, and step overruns.
  9. `InterJudgeAgreementEvaluator`: Statistical Cohen's Kappa ($\kappa$) calibrating judge decisions against ground truth.
  10. `AutonomousBenchmarkSuiteOrchestrator`: Multi-metric release gate evaluating datasets and enforcing production release criteria (`"Release benchmark gate breach"`).
- **Verification:** `mvn test-compile exec:java -pl phase06-ex02-advanced-judges` (0/10 baseline).
- **Target File:** [`phase06-ex02-advanced-judges/src/main/java/phase06/AdvancedJudgeUnderTest.java`](./phase06-ex02-advanced-judges/src/main/java/phase06/AdvancedJudgeUnderTest.java)

---

## 5. Active Workspace State (Phase 07: Exercises 01 & 02)

### Exercise 01: Foundational Saga Agent Loop (`phase07-ex01-saga-loop`)
- **Theme:** In-Memory Saga Agent Loop, State Machine, Compensation & Trajectory Auditing (10 Repetitive Scenarios)
- **Pedagogical Mandate:** Cement Saga FSM transitions and in-memory Git-like trees:
  1. `StateTransitionEngine`: FSM validator (`IDLE -> DECIDE -> APPLY -> MEASURE -> JUDGE -> COMPENSATE/FINISH`).
  2. `VirtualWorkspaceMemory`: Atomic Git-like tree (`commit`, `revertTo`, immutable snapshots).
  3. `DecideProposalExtractor`: DECIDE schema parsing with 1-shot feedback repair.
  4. `ApplyAndCompensateStep`: APPLY phase with automatic rollback on compilation failure.
  5. `NoiseFloorKeepGate`: MEASURE & JUDGE telemetry gate (P95/RPS floors + fail rate guard).
  6. `IterationCapCircuitBreaker`: Hard turn cutoff throwing `"Saga guardrail breached: Maximum iterations"`.
  7. `TokenBudgetGuardrail`: Token spend ceiling throwing `"Saga guardrail breached: Token budget"`.
  8. `TrajectoryAuditRecorder`: Trajectory event recorder capturing all lifecycle phases.
  9. `CheckpointRecoveryManager`: Save and resume state recovery preserving `lastKeptSha`.
  10. `AutonomousSagaLoopRunner`: End-to-end multi-turn orchestrator running until target reached.
- **Verification:** `mvn test-compile exec:java -pl phase07-ex01-saga-loop` (0/10 baseline fail gate).
- **Target File:** [`phase07-ex01-saga-loop/src/main/java/phase07/SagaLoopUnderTest.java`](./phase07-ex01-saga-loop/src/main/java/phase07/SagaLoopUnderTest.java)

### Exercise 02: Advanced Saga Resilience (`phase07-ex02-advanced-saga`)
- **Theme:** Advanced Saga Resilience, Speculative Branches, and HITL (10 Repetitive + Difficult Scenarios)
- **Pedagogical Mandate:** Enterprise agent loop resilience without external dependencies:
  1. `SpeculativeBranchManager`: Isolated multi-branch sandbox trials (`branch-A`, `branch-B`) and winner merge.
  2. `MultiStageDiagnosticTriageEngine`: 4-stage failure escalation hierarchy (`STAGE_1` to `STAGE_4`).
  3. `GuaranteedFinallyCompensationEngine`: Guaranteed finally-block rollback on unexpected JVM crash.
  4. `SaddleSafeMechanismKeepEngine`: Full `KeepRule` v2 mechanism reduction (>50% reduction + tail bound).
  5. `CyclicalPingPongDetector`: SHA-256 proposal hash cycle detector throwing `"Advanced Saga breach: Cyclical proposal"`.
  6. `AdaptiveFailureFeedbackEnricher`: Failure trace injection with structured prompt headers.
  7. `DynamicPhaseTokenBudgeter`: Partitioned per-phase token ceilings (`DECIDE`, `APPLY`, `MEASURE`, `JUDGE`).
  8. `SplitBrainCheckpointValidator`: Split-brain divergence detection and fallback recovery.
  9. `HumanInTheLoopApprovalGate`: Risk-based operator approval gate (`AUTO_APPROVE` vs `REQUIRE_HUMAN`).
  10. `ResilientAutonomousChaosSagaOrchestrator`: Multi-turn chaos orchestrator recovering from simulated failures.
- **Verification:** `mvn test-compile exec:java -pl phase07-ex02-advanced-saga` (0/10 baseline fail gate).
- **Target File:** [`phase07-ex02-advanced-saga/src/main/java/phase07/AdvancedSagaUnderTest.java`](./phase07-ex02-advanced-saga/src/main/java/phase07/AdvancedSagaUnderTest.java)

---

## 6. Active Workspace State (Phase 08: Production Microservice Capstone)

### Phase 08: Production Autonomous Incident Triage Microservice (`phase08-production-service`)
- **Theme:** Full Modular Production Spring Boot AI Microservice Capstone (Synthesizing Phases 01–07)
- **Pedagogical Mandate:** Real-world enterprise microservice repository replacing the single-file test pattern:
  1. `model/`: Domain models (`IncidentSeverity`, `DiagnosticTelemetry`, `TriageProposal`, `SagaState`, `TrajectoryEvent`, DTOs).
  2. `config/`: Spring Boot `@ConfigurationProperties("agent.triage")` and `@Configuration` dependency injection beans.
  3. `prompt/`: Dynamic templated incident prompts with variable substitution (Phase 01).
  4. `parser/`: Resilient JSON extraction, markdown fence stripping, and 1-shot repair (Phase 02).
  5. `tool/`: Sandboxed `@Tool` diagnostic beans (`K8sDiagnosticTool`, `JfrAnalysisTool`) with reflection discovery via `ToolCallbacks.from` (Phase 03).
  6. `advisor/`: Spring AI `CallAdvisor` pipeline (`SecurityKeywordAdvisor` at Order 10, `TokenBudgetAdvisor` at Order 20) (Phase 04).
  7. `evaluator/`: Telemetry keep gates vs noise floors and factual grounding evaluators (Phase 06).
  8. `saga/`: In-memory Git-like workspace (`commit`, `revertTo`) and 6-phase Saga FSM orchestrator (Phase 07).
  9. `repository/`: Thread-safe trajectory audit persistence (`InMemoryTrajectoryRepository`).
  10. `health/`: Spring Boot Actuator `HealthIndicator` (`AgentHealthIndicator`) probing model availability.
  11. `service/` & `web/`: `IncidentTriageService` and `IncidentTriageController` (`POST /api/incidents/triage`, `GET /api/incidents/{id}/trajectory`).
- **Verification & Practice State:** 
  - Complete starter stubs with rich TigerStyle Javadocs and `UnsupportedOperationException` active in `phase08-production-service/src/main/java/phase08/`.
  - Full Architecture & Class Requirements: [`docs/phase08-production-service-specification.md`](./docs/phase08-production-service-specification.md).
  - Sealed Golden Reference: [`docs/golden/phase08-production-service-golden.md`](./docs/golden/phase08-production-service-golden.md).
  - Test Suite: 13 test files (32 tests total) ready to guide step-by-step TDD implementation.

---

## 7. Key Engineering Gotchas & Discoveries

1. **Spring AI 2.0.1 / Spring Boot 4.1.1 Coordinates:**
   - Package `org.springframework.ai.converter.BeanOutputConverter` uses `tools.jackson` (Jackson 3).
   - In Spring Boot 4.1.1, Health classes moved from `org.springframework.boot.actuate.health.*` to `org.springframework.boot.health.contributor.Health` and `HealthIndicator`.
   - Tool reflection helper is `org.springframework.ai.support.ToolCallbacks.from(Object... beans)` (in `spring-ai-model`).
   - `CallAdvisor` directly extends `org.springframework.core.Ordered`. The method signature is `ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain)`.
2. **`EvaluationRequest` and `EvaluationResponse` in Spring AI 2.0.1:**
   - `EvaluationRequest` takes `(String userText, String responseContent)` or `(String userText, List<Document> dataList, String responseContent)`.
   - `EvaluationRequest` exposes `.getUserText()`, `.getDataList()`, `.getResponseContent()` (does NOT have `getMetadata()`).
3. **Defensive Token Auditing:**
   - `Usage.getPromptTokens()` and `getCompletionTokens()` return boxed `Integer` (nullable). Always check null before unboxing.
4. **Transparent Error Contracts & Non-Flaky Substring Assertions:**
   - Phase 06 Exercise 01 Scenario 10 requires `"Evaluation threshold breach"`.
   - Phase 06 Exercise 02 Scenario 10 requires `"Release benchmark gate breach"`.
   - Phase 07 Exercise 01 Scenarios 6/7 require `"Saga guardrail breached"`.
   - Phase 07 Exercise 02 Scenarios 5/7 require `"Advanced Saga breach"`.

---

## 8. Quick Commands Cheatsheet

```powershell
# Phase 06:
mvn test-compile exec:java -pl phase06-ex01-evaluators
mvn test-compile exec:java -pl phase06-ex02-advanced-judges

# Phase 07:
mvn test-compile exec:java -pl phase07-ex01-saga-loop
mvn test-compile exec:java -pl phase07-ex02-advanced-saga

# Phase 08 (Capstone Microservice):
mvn test -pl phase08-production-service

# Specific Phase 08 tests:
mvn test -pl phase08-production-service -Dtest=IncidentTriageEndToEndIntegrationTest
mvn test -pl phase08-production-service -Dtest=AutonomousSagaLoopTest

# Compile entire reactor across all 17 modules:
mvn test-compile
```
