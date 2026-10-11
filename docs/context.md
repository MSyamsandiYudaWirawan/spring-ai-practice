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
| **Phase 06** | LLM-as-a-Judge & Evaluators | Evaluators, bias mitigation, trajectory audits, release gates | ✅ **COMPLETED** | 2 exercises (30 scenarios total) |
| **Phase 07** | RAG & Vector Stores | `VectorStore`, `DocumentRetriever`, contextual augmentation, query expansion | ✅ **COMPLETED** | 2 exercises (33 scenarios total) |
| **Phase 08** | Model Context Protocol (MCP) | MCP Server tools/resources/prompts, MCP Client adapters, multi-server routing | ✅ **COMPLETED** | 2 exercises (30 scenarios total) |
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
1. `phase07-ex01-vector-stores`: Vector Stores, In-Memory Embeddings, Document Chunking & Semantic Filtering (18 repetitive muscle-memory scenarios across 6 core framework topics: Document creation/mutation drills, TokenTextSplitter chunking/enrichment drills, SimpleVectorStore lifecycle drills, SearchRequest top-K/threshold/sorting drills, FilterExpressionBuilder comparison drills, and FilterExpressionBuilder collection/nested search drills). **18/18 PASSED (exit 0)**.
2. `phase07-ex02-rag-advisors`: Modular RAG Advisors, Context Augmenters, Document PostProcessors & Query Transformers (15 repetitive muscle-memory scenarios across 5 core framework topics: VectorStoreDocumentRetriever drills, ContextualQueryAugmenter formatting/fallback drills, DocumentPostProcessor deduplication/priority/score drills, Pre-retrieval QueryExpander/Transformer drills, and RetrievalAugmentationAdvisor assembly drills). **15/15 PASSED (exit 0)**.

### Phase 08 Completed History:
1. `phase08-ex01-mcp-server`: Model Context Protocol (MCP) Server Architecture & Protocol Specifications (15 repetitive muscle-memory scenarios across 5 core framework topics: Tool Definition drills, SyncToolSpecification execution & resilience drills, Resource specification drills, Prompt template drills, and Server capabilities/filtering drills). **15/15 PASSED (exit 0)**.
2. `phase08-ex02-mcp-client`: Model Context Protocol (MCP) Client Integration & Agentic Orchestration (15 repetitive muscle-memory scenarios across 5 core framework topics: Client capabilities/roots/specs drills, Adapting MCP tools to ToolCallbacks drills, Tool discovery/prefixing/collision drills, MCP Resource and Prompt retrieval drills, and Federated registry/reloading/sampling drills). **15/15 PASSED (exit 0)**.

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
- **Theme:** Deterministic Evaluators, Spring AI Evaluator Interface & Foundational LLM-as-a-Judge (15 Repetitive Scenarios across 5 Topics)
- **Pedagogical Mandate:** Cement Spring AI `Evaluator`, `EvaluationRequest`, `EvaluationResponse` primitives:
  - **Topic 1: Direct Spring AI Evaluator Adapters:** Direct `EvaluationRequest` & response extraction, RAG context-enriched evaluation, and official Spring AI `RelevancyEvaluator` adapter.
  - **Topic 2: Ground Truth & Threshold Scoring:** Exact match accuracy, categorical convergence with case-insensitive matching, and bounded single-metric score thresholding.
  - **Topic 3: Parsers & Rubric Extraction:** Numeric score extraction from prose, verdict & reasoning extraction, and structured JSON rubric parsing with fallback resilience.
  - **Topic 4: Multi-Criteria & Composite Evaluators:** Weighted multi-criteria evaluation, fail-fast short-circuit composite evaluator, and all-must-pass composite evaluator.
  - **Topic 5: Benchmark Execution & Quality Gates:** Batch evaluation suite runner, multi-metric pass rate aggregator, and production quality gate enforcer (`EvaluationThresholdBreachException`).
- **Verification:** `mvn test -pl phase06-ex01-evaluators` (0/15 baseline in practice mode, 15/15 in golden).
- **Target File:** [`phase06-ex01-evaluators/src/main/java/phase06/EvaluatorUnderTest.java`](./phase06-ex01-evaluators/src/main/java/phase06/EvaluatorUnderTest.java)

### Exercise 02: Advanced Repetitive + Difficult Judges (`phase06-ex02-advanced-judges`)
- **Theme:** Advanced LLM Judges, Bias Mitigation, Calibration & Trajectory Evaluation (15 Repetitive Scenarios across 5 Topics)
- **Pedagogical Mandate:** Solve production AI evaluation challenges strictly within the scope of AI engineering:
  - **Topic 1: Tournament & Voting Judges (Bias Mitigation):** Pairwise A/B tournament judge with position swap mitigation, 3-way tournament round-robin ranking leaderboard, and self-consistency majority voting with confidence calibration.
  - **Topic 2: RAG Triad Grounding & Attribution:** Atomic claim extraction & faithfulness scoring, claim-to-document citation mapping & attribution verification, and answer relevance & query drift / topic evasion detection.
  - **Topic 3: Calibrated Rubrics & Safety Gates:** Anchor-based calibrated rubric judge (few-shot Level 1/3/5 descriptors), adversarial refusal & jailbreak defense judge, and system prompt leakage & sensitive data exfiltration audit.
  - **Topic 4: Agent Trajectory & Reasoning Audits:** Multi-turn trajectory step efficiency & duplicate tool call detection, ping-pong cyclical loop & tool call recursion detection, and goal completion & trajectory convergence gate.
  - **Topic 5: Inter-Judge Calibration & Production Release Gates:** Statistical inter-judge agreement (Cohen's Kappa $\kappa$), multi-dimensional benchmark suite aggregator (`ReleaseBenchmarkReport`), and production release benchmark gate enforcer (`BenchmarkGateBreachException`).
- **Verification:** `mvn test -pl phase06-ex02-advanced-judges` (0/15 baseline in practice mode, 15/15 in golden).
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
- **Theme:** MCP Server Architecture, Tool/Resource/Prompt Specifications & Capabilities (15 Repetitive Foundational Scenarios across 5 topics)
- **Pedagogical Mandate:** Master Model Context Protocol (MCP) server specifications, JSON schema generation, handlers, and filters:
  1. `PrimitiveToolDefinition`: Primitive property types mapped to JSON schema object specifications.
  2. `StructuredToolDefinition`: Arbitrary structured property maps wrapped in tool schemas.
  3. `AuditValidatedToolDefinition`: Title- and audit-metadata-enriched tool schemas.
  4. `SyncToolSpecification`: Standard execution handler returning `CallToolResult` with `TextContent`.
  5. `ResilientToolSpecification`: Defensive execution handler trapping exceptions into `isError: true` results.
  6. `SpringAiToolCallbackAdapter`: Adapting native Spring AI `ToolCallback` to MCP `SyncToolSpecification` via `McpToolUtils`.
  7. `StaticTextResourceSpecification`: Static text resources served with MIME types.
  8. `DynamicTextResourceSpecification`: Dynamic text resources evaluated via functional content providers.
  9. `BinaryBlobResourceSpecification`: Base64 binary content served via `BlobResourceContents`.
  10. `SimplePromptSpecification`: Zero-argument static prompt templates.
  11. `ParameterizedPromptSpecification`: Multi-argument prompt templates with variable substitution.
  12. `RoleEnforcedPromptSpecification`: Explicit role assignment (`Role.ASSISTANT` or `Role.USER`) in prompt messages.
  13. `ServerCapabilitiesDeclaration`: Protocol feature negotiation flags (`tools`, `resources`, `prompts`, `logging`).
  14. `ToolAllowlistFilter`: Filtering tools against security allowlists.
  15. `ToolPrefixRouter`: Routing and filtering tools by namespace prefix.
- **Verification:** `mvn test-compile exec:java -pl phase08-ex01-mcp-server` or `mvn test -pl phase08-ex01-mcp-server` (0/15 baseline fail gate).
- **Target File:** [`phase08-ex01-mcp-server/src/main/java/phase08/McpServerUnderTest.java`](./phase08-ex01-mcp-server/src/main/java/phase08/McpServerUnderTest.java)

### Exercise 02: Advanced MCP Client & Multi-Server Federation (`phase08-ex02-mcp-client`)
- **Theme:** MCP Client Integration, Tool Providers & Agentic Orchestration (15 Repetitive Scenarios across 5 topics)
- **Pedagogical Mandate:** Master remote MCP client discovery, tool namespacing, and federated agent execution:
  1. `ClientCapabilities`: Declaring client protocol features (`roots`, `sampling`).
  2. `ClientRoots`: Validating and building `file://` `Root` descriptors.
  3. `ClientImplementation`: Declaring client implementation metadata (name, version, description).
  4. `ToolDefinitionAdapter`: Converting MCP `Tool` schemas to Spring AI `ToolDefinition` via `McpToolUtils`.
  5. `ToolCallbackAdapter`: Bridging synchronous MCP tool invocations to Spring AI `ToolCallback`.
  6. `ResilientToolCallback`: Defensive error boundaries catching failures and formatting `ERROR: ` text.
  7. `BatchToolDiscovery`: Querying remote tools and bulk-generating Spring AI `ToolCallback` lists.
  8. `PrefixedToolNaming`: Generating namespaced tool identifiers via `McpToolUtils.prefixedToolName`.
  9. `FederatedDiscovery`: Discovering tools across federated servers with collision-prevention naming.
  10. `SingleResourceReader`: Reading remote resources into formatted prompt context blocks.
  11. `MultiResourceAssembly`: Reading multiple remote resources and assembling them into delimited prompt contexts.
  12. `ParameterizedPromptFetcher`: Fetching prompt templates hosted on MCP servers with argument substitution.
  13. `MultiServerClientRegistry`: Federated client registry maintaining server mappings, prefixed tool index, and routing.
  14. `CatalogReloader`: Dynamically invalidating cached tools upon server updates and re-indexing active tools.
  15. `SamplingProtocolHandler`: Server-to-client LLM delegation (sampling protocol) under token ceilings.
- **Verification:** `mvn test-compile exec:java -pl phase08-ex02-mcp-client` or `mvn test -pl phase08-ex02-mcp-client` (0/15 baseline fail gate).
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
