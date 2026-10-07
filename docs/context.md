# Spring AI Practice — Session Context & Handoff State

**Document Purpose:** Master state and context handoff for future agent sessions and developer reference. Read this file first when resuming work in this workspace.

---

## 1. High-Level Mission & Pedagogical Protocol

- **Target Goal:** Bridge the gap from Spring AI beginner to authoring production-grade agentic architectures with cold muscle memory.
- **Core Value Proposition:** This practice repository enables engineers to build instinctive muscle memory across Spring AI primitives in isolation before assembling them into full-scale autonomous diagnostic and remediation systems.
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
   10. **Few-Shot Template for AI Drill Synthesis:** Because contracts, models, test doubles, and verifiers are strictly decoupled and self-contained, exercises act as clean few-shot templates. Developers can prompt an external LLM with any existing verifier and contract to synthesize endless custom edge-case scenarios on demand.

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
| **Phase 07** | RAG & Vector Stores | `VectorStore`, `DocumentRetriever`, contextual augmentation, query expansion | ✅ **COMPLETED** | 2 exercises (24 scenarios total) |
| **Phase 08** | Model Context Protocol (MCP) | MCP Server tools/resources/prompts, MCP Client adapters, multi-server routing | ✅ **COMPLETED** | 2 exercises (24 scenarios total) |
| **Phase 09** | Multi-Agent Orchestration & Hierarchical Delegation | Supervisor routing, specialized sub-agent handoffs, consensus voting | 📋 **IN PLANNING** | Future Roadmap Phase |
| **Phase 10** | Long-Term Agent Memory & Context Compaction | Semantic episodic memory recall, sliding-window compaction, entity graphs | 📋 **IN PLANNING** | Future Roadmap Phase |
| **Phase 11** | Human-in-the-Loop (HITL) & Streaming Control Gates | Reactive streaming approval gates, manual authorization checkpoints | 📋 **IN PLANNING** | Future Roadmap Phase |
| **Phase 12** | Durable State Machine Checkpointing & Crash Recovery | Spring StateMachine, durable saga patterns, Postgres loop persistence | 📋 **IN PLANNING** | Future Roadmap Phase |

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

### Phase 07 Completed History:
1. `phase07-ex01-vector-stores`: In-Memory Vector Store, Token Text Splitting, Embedding Generation, Metadata Filtering, MMR Diversity & Deduplication (12 scenarios: `FixedTokenChunker`, `DeterministicEmbeddingGenerator`, `CosineSimilarityEngine`, `InMemoryVectorStoreEngine`, `MetadataFilteringEngine`, `BatchDocumentIngestionPipeline`, `ThresholdSimilaritySearchEngine`, `HybridKeywordVectorSearchEngine`, `DocumentLifecycleManager`, `AutonomousKnowledgeBaseIngestor`, `MaximalMarginalRelevanceSearchEngine`, `ContentHashDeduplicationPipeline`). **PASSED (exit 0)**.
2. `phase07-ex02-rag-advisors`: Advanced RAG Advisors, Context Augmentation, Query Expansion, HyDE & Token Budget Packing (12 scenarios: `VectorStoreDocumentRetriever`, `FilterExpressionDocumentRetriever`, `ContextualQueryAugmenter`, `EmptyContextFallbackAugmenter`, `PriorityRerankingPostProcessor`, `MultiQueryExpander`, `RewriteQueryTransformer`, `ConcatenationDocumentJoiner`, `RetrievalAugmentationAdvisor`, `EnterpriseRagGateway`, `HydeQueryTransformer`, `TokenBudgetPacker`). **PASSED (exit 0)**.

### Phase 08 Completed History:
1. `phase08-ex01-mcp-server`: MCP Server Tool & Resource Specification, Schemas, URI Templates & Log Notifications (12 scenarios: `ServerCapabilitiesBuilder`, `ToolSpecificationGenerator`, `SyncToolExecutionHandler`, `ToolExecutionErrorWrapper`, `SpringAiToolCallbackAdapter`, `ResourceRegistrationHandler`, `PromptRegistrationHandler`, `ToolAllowlistSecurityFilter`, `McpServerAuditRecorder`, `EnterpriseMcpServerRegistry`, `ParameterizedResourceTemplateMatcher`, `McpProtocolLogNotificationDispatcher`). **PASSED (exit 0)**.
2. `phase08-ex02-mcp-client`: MCP Client Integration, Multi-Server Tool Routing, Dynamic Tool Reloading & Sampling (12 scenarios: `McpClientAdapterBuilder`, `RemoteToolDefinitionAdapter`, `McpSyncToolCallbackAdapter`, `RemoteToolDiscoveryEngine`, `PrefixedToolNameResolver`, `RemoteResourceContentReader`, `RemotePromptTemplateRenderer`, `McpToolExecutionFaultHandler`, `MultiServerClientRegistry`, `EnterpriseAgenticGateway`, `DynamicToolListReloader`, `McpSamplingHandler`). **PASSED (exit 0)**.

### Upcoming Roadmap Phases (In Planning):
1. **Phase 09: Multi-Agent Orchestration & Hierarchical Delegation**
   - *Target Mechanics:* Supervisor/coordinator router agents, specialized sub-agent task delegation, peer-to-peer message envelopes, consensus voting strategies, and worker handoffs.
   - *Production Diagnostician Link:* Orchestrating specialized diagnostic sub-agents (e.g. MemoryAgent, LockContentionAgent, DiskIOAgent) under a single MasterCoordinator.
2. **Phase 10: Long-Term Agent Memory & Sliding Context Compaction**
   - *Target Mechanics:* Semantic episodic memory recall, sliding-window conversation compaction, entity graph extraction, automated token-window budget truncation, and persistent conversation snapshots.
   - *Production Diagnostician Link:* Preserving multi-benchmark hypothesis history across agent runs without overflowing context boundaries.
3. **Phase 11: Human-in-the-Loop (HITL) & Streaming Control Gates**
   - *Target Mechanics:* Interruptible reactive streaming (`Flux<ServerSentEvent>`), manual approval checkpoints for sensitive tool execution, pause-and-resume workflows, and user clarification dialogues.
   - *Production Diagnostician Link:* Gatekeeping high-impact runtime operations (e.g., git commits, container restarts, or JVM argument overrides) behind operator sign-off.
4. **Phase 12: Durable State Machine Checkpointing & Crash Recovery**
   - *Target Mechanics:* Resilient cyclic execution loops using Spring StateMachine / durable saga patterns, checkpointing cycle state to Postgres, and graceful recovery from container restarts or network partitions.
   - *Production Diagnostician Link:* Robustly maintaining the `DECIDE -> APPLY -> MEASURE -> JUDGE` state machine across node crashes.

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

### Exercise 01: Foundational Vector Stores & Ingestion (`phase07-ex01-vector-stores`)
- **Theme:** In-Memory Vector Store, Token Text Splitting, Embedding Generation & Metadata Filtering (12 Repetitive Foundational Scenarios)
- **Pedagogical Mandate:** Cement core vector store mechanics, vector search algorithms, and ingestion pipelines:
  1. `FixedTokenChunker`: Text chunking by token/word boundary with configurable overlap.
  2. `DeterministicEmbeddingGenerator`: Dimension-normalized float[] vector generation.
  3. `CosineSimilarityEngine`: Dot product / magnitude cosine similarity calculation clamped to [-1.0, 1.0].
  4. `InMemoryVectorStoreEngine`: Vector store supporting document addition with embeddings and top-K similarity search.
  5. `MetadataFilteringEngine`: Key-value equality metadata filtering on retrieved search results.
  6. `BatchDocumentIngestionPipeline`: Ingestion pipeline chunking raw text, generating embeddings, and storing documents.
  7. `ThresholdSimilaritySearchEngine`: Cosine similarity search with minimum similarity threshold cutoff.
  8. `HybridKeywordVectorSearchEngine`: Reciprocal Rank Fusion (RRF) combining keyword and semantic similarity scores.
  9. `DocumentLifecycleManager`: Ingestion, update, and deletion of vector documents by ID.
  10. `AutonomousKnowledgeBaseIngestor`: End-to-end ingestion and query pipeline with ingestion summaries.
  11. `MaximalMarginalRelevanceSearchEngine`: MMR diversity re-ranking balancing relevance vs redundancy.
  12. `ContentHashDeduplicationPipeline`: SHA-256 chunk deduplication and idempotent knowledge ingestion.
- **Verification:** `mvn test-compile exec:java -pl phase07-ex01-vector-stores` (0/12 baseline fail gate).
- **Target File:** [`phase07-ex01-vector-stores/src/main/java/phase07/VectorStoreUnderTest.java`](./phase07-ex01-vector-stores/src/main/java/phase07/VectorStoreUnderTest.java)

### Exercise 02: Advanced RAG Advisors & Query Transformation (`phase07-ex02-rag-advisors`)
- **Theme:** Advanced RAG Advisors, Context Augmentation & Query Expansion (12 Repetitive + Difficult Scenarios)
- **Pedagogical Mandate:** Master Spring AI RAG primitives, query expansion, and advisor interceptors:
  1. `VectorStoreDocumentRetriever`: Top-K document retriever querying a VectorStore with a search query.
  2. `FilterExpressionDocumentRetriever`: Metadata-filtered document retrieval using Spring AI `Filter.Expression`.
  3. `ContextualQueryAugmenter`: Prompt augmentation formatting retrieved documents into structured context blocks.
  4. `EmptyContextFallbackAugmenter`: Graceful degradation and fallback prompt when zero documents match.
  5. `PriorityRerankingPostProcessor`: `DocumentPostProcessor` reranking documents by priority/recency metadata score.
  6. `MultiQueryExpander`: LLM-powered query expansion generating alternative semantic queries.
  7. `RewriteQueryTransformer`: Conversational query rewriting resolving ambiguous pronouns into standalone queries.
  8. `ConcatenationDocumentJoiner`: Merging and deduplicating document lists from multi-query retrievals.
  9. `RetrievalAugmentationAdvisor`: Full Spring AI `CallAdvisor` intercepting requests, retrieving context, and augmenting prompts.
  10. `EnterpriseRagGateway`: Complete end-to-end RAG orchestrator with retrieval, threshold filtering, reranking, and generation.
  11. `HydeQueryTransformer`: Hypothetical Document Embeddings generating synthetic passages for retrieval.
  12. `TokenBudgetPacker`: Context packing dynamically truncating documents to fit strictly within a token ceiling.
- **Verification:** `mvn test-compile exec:java -pl phase07-ex02-rag-advisors` (0/12 baseline fail gate).
- **Target File:** [`phase07-ex02-rag-advisors/src/main/java/phase07/RagAdvisorUnderTest.java`](./phase07-ex02-rag-advisors/src/main/java/phase07/RagAdvisorUnderTest.java)

---

## 6. Active Workspace State (Phase 08: Model Context Protocol)

### Exercise 01: Foundational MCP Server (`phase08-ex01-mcp-server`)
- **Theme:** MCP Server Tool & Resource Specification, Schemas & Transport Handlers (12 Repetitive Foundational Scenarios)
- **Pedagogical Mandate:** Master Model Context Protocol (MCP) server specifications, JSON schema generation, and tool adapters:
  1. `ServerCapabilitiesBuilder`: Building compliant MCP `ServerCapabilities` (tools, resources, prompts, logging).
  2. `ToolSpecificationGenerator`: Converting tool specifications to compliant `McpSchema.Tool` JSON schema definitions.
  3. `SyncToolExecutionHandler`: Registering and executing `SyncToolSpecification` with JSON argument parsing.
  4. `ToolExecutionErrorWrapper`: Formatting exceptions into MCP error results (`isError: true`, content text error message).
  5. `SpringAiToolCallbackAdapter`: Adapting Spring AI `ToolCallback` to MCP `SyncToolSpecification` via `McpToolUtils`.
  6. `ResourceRegistrationHandler`: Exposing static/dynamic `Resource` endpoints and handling resource read requests.
  7. `PromptRegistrationHandler`: Registering parameterized MCP `Prompt` templates with variable arguments.
  8. `ToolAllowlistSecurityFilter`: Intercepting and enforcing authorized tool names.
  9. `McpServerAuditRecorder`: Recording inbound tool call telemetry, latencies, and execution outcomes.
  10. `EnterpriseMcpServerRegistry`: Central orchestrator registering tools, resources, prompts, and routing execution requests.
  11. `ParameterizedResourceTemplateMatcher`: RFC 6570 URI template variable matching and extraction.
  12. `McpProtocolLogNotificationDispatcher`: Protocol log notification dispatching with severity threshold filtering.
- **Verification:** `mvn test-compile exec:java -pl phase08-ex01-mcp-server` (0/12 baseline fail gate).
- **Target File:** [`phase08-ex01-mcp-server/src/main/java/phase08/McpServerUnderTest.java`](./phase08-ex01-mcp-server/src/main/java/phase08/McpServerUnderTest.java)

### Exercise 02: Advanced MCP Client & Multi-Server Federation (`phase08-ex02-mcp-client`)
- **Theme:** MCP Client Integration, Multi-Server Tool Routing & Agentic Execution (12 Repetitive + Difficult Scenarios)
- **Pedagogical Mandate:** Master remote MCP client discovery, tool namespacing, and federated agent execution:
  1. `ClientCapabilities`: Declaring client protocol features (`roots`, `sampling`).
  2. `ToolDefinitionAdapter`: Converting MCP `Tool` schemas to Spring AI `ToolDefinition`.
  3. `ToolCallbackAdapter`: Bridging MCP synchronous tool invocations to Spring AI `ToolCallback`.
  4. `MultiToolDiscovery`: Batch adapting client tool catalogs to callback providers.
  5. `PrefixedToolNameResolver`: Generating namespaced tool identifiers to avoid collisions.
  6. `ResourceContextFormatter`: Fetching MCP resources and formatting them into prompt contexts.
  7. `PromptMessageTextFetcher`: Retrieving parameterized prompt templates hosted on MCP servers.
  8. `ToolExecutionResilience`: Defensive error boundaries catching failures and formatting error signals.
  9. `MultiServerClientRegistry`: Multi-server client registry maintaining connections and tool routing maps.
  10. `EnterpriseAgenticGateway`: End-to-end agentic gateway orchestrating multi-server discovery, tool calls, and LLM responses.
  11. `DynamicToolListReloader`: Invalidating cached tool definitions upon server catalog updates and re-indexing active tools.
  12. `McpSamplingHandler`: Server-to-client LLM delegation (sampling protocol) under token ceilings.
- **Verification:** `mvn test-compile exec:java -pl phase08-ex02-mcp-client` (0/12 baseline fail gate).
- **Target File:** [`phase08-ex02-mcp-client/src/main/java/phase08/McpClientUnderTest.java`](./phase08-ex02-mcp-client/src/main/java/phase08/McpClientUnderTest.java)

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
   - Phase 08 Exercise 01 Scenario 08 requires `"Unauthorized MCP tool call"`.
   - Phase 08 Exercise 02 Scenario 06 requires `"MCP execution failure"`.
5. **Spring AI MCP Tool Name Prefixing Behavior:**
   - `McpToolUtils.prefixedToolName(serverPrefix, toolName)` uses an internal `shorten` function that compresses multi-letter server names to prefixes (e.g. `"github"` -> `"g"`, `"create_issue"` -> `"g_create_issue"`). Verifiers check tool names using `.endsWith(toolName)` or `McpToolUtils.prefixedToolName(...)`.
   - In `MultiServerClientRegistry`, mapping both `prefixedName -> client` and `prefixedName -> originalToolName` guarantees $O(1)$ dispatch regardless of shortening conventions.
6. **Spring AI `JsonHelper` API:**
   - In Spring AI 2.0.1, the Map deserialization method on `JsonHelper` is `jsonHelper.fromJsonToMap(jsonString)` (do not use `.toMap(...)`).
7. **`ChatClient.Builder` Callback Registration:**
   - Prefer `chatClientBuilder.defaultToolCallbacks(List<ToolCallback>)` over varargs arrays to avoid deprecation warnings.

---

## 8. Quick Commands Cheatsheet

```powershell
# Phase 01:
mvn test-compile exec:java -pl phase01-ex01-chat-basics
mvn test-compile exec:java -pl phase01-ex02-chat-advanced
mvn test-compile exec:java -pl phase01-ex03-cloud-sre

# Phase 02:
mvn test-compile exec:java -pl phase02-ex01-structured-outputs
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions

# Phase 03:
mvn test-compile exec:java -pl phase03-ex01-tool-sandboxing
mvn test-compile exec:java -pl phase03-ex02-multi-tool-drills

# Phase 04:
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors
mvn test-compile exec:java -pl phase04-ex02-resilient-guardrails

# Phase 05:
mvn test-compile exec:java -pl phase05-ex01-offline-harness
mvn test-compile exec:java -pl phase05-ex02-advanced-harness

# Phase 06:
mvn test-compile exec:java -pl phase06-ex01-evaluators
mvn test-compile exec:java -pl phase06-ex02-advanced-judges

# Phase 07:
mvn test-compile exec:java -pl phase07-ex01-vector-stores
mvn test-compile exec:java -pl phase07-ex02-rag-advisors

# Phase 08:
mvn test-compile exec:java -pl phase08-ex01-mcp-server
mvn test-compile exec:java -pl phase08-ex02-mcp-client

# Compile entire reactor across all 17 modules:
mvn test-compile

# Run all test suites across the reactor:
mvn test
```
