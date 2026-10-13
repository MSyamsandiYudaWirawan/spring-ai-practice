# Spring AI 2.0.1 — Reference Documentation & Architecture Index

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Java Version:** Java 21+ (`maven.compiler.parameters=true`)

This documentation suite captures verified signatures, accessors, contracts, and production patterns in `spring-ai-client-chat-2.0.1.jar`, `spring-ai-model-2.0.1.jar`, `spring-ai-vector-store-2.0.1.jar`, and `spring-ai-mcp-2.0.1.jar`.

To keep documentation clean, navigable, and maintainable, the reference manual is divided into modular topic guides corresponding to each architectural tier:

---

## 📚 Modular Topic Reference Guides

| Module | Topic | Curriculum Phase | Key Contracts & Architecture |
|---|---|---|---|
| [**01. ChatClient & Core Accessors**](01-chat-client.md) | ChatClient Fundamentals | Phase 01 | Maven BOM, `ChatClient.builder()`, `PromptUserSpec`, `CallResponseSpec`, Accessor Chains, Fake `ChatResponse` |
| [**02. Structured Outputs & Schema Repair**](02-structured-outputs.md) | Structured Output & Schema | Phase 02 | `BeanOutputConverter`, `ParameterizedTypeReference`, Markdown Fence Sanitization, One-Shot Schema Repair |
| [**03. Tool Calling & Callbacks**](03-tool-calling.md) | Tool Calling & Functions | Phase 03 | `@Tool`, `@ToolParam`, `ToolCallbacks.from()`, `ToolEnvelope`, Fail-Fast Invariants, Rate Guards |
| [**04. CallAdvisors & Guardrail Pipelines**](04-guardrail-advisors.md) | Advisors & Guardrails | Phase 04 | `CallAdvisor`, Onion Model Ordering, `chain.copy(this)` Retry Loop, Quota Pre-Allocation, Model Failover |
| [**05. Deterministic Testing & Test Harness**](05-testing-harness.md) | Offline Deterministic Testing | Phase 05 | `ChatPort` Hexagonal Seam, `ScriptedChatPort`, `PromptCaptor`, `TestClock`, Streaming `StepVerifier` |
| [**06. Evaluators & LLM Judges**](06-evaluators-judges.md) | Verification & Evaluation | Phase 06 | `Evaluator`, `RelevancyEvaluator`, `FactCheckingEvaluator`, Noise Floor Telemetry Gating, A/B Tournaments, Cohen's Kappa |
| [**07. RAG & Vector Stores**](07-rag-vector-stores.md) | Retrieval-Augmented Generation | Phase 07 | `Document`, `VectorStore`, `FilterExpressionBuilder`, Cosine Sim, RRF, `RetrievalAugmentationAdvisor`, Pre/Post Transformers |
| [**08. Model Context Protocol (MCP)**](08-model-context-protocol.md) | Distributed Agent Tooling | Phase 08 | MCP Host/Client/Server, `SyncToolSpecification`, RFC 6570 Resource Templates, Multi-Server Tool Routing, Sampling Protocol |
| [**09. Enterprise Architecture & Saga Loops**](09-enterprise-architecture.md) | Saga Loops & Enterprise Mesh | Phase 09 | 6-Phase Agent Saga FSM, Guaranteed Finally Compensations, Multi-Stage Triage, Boot 4.1.1 Relocations, Resilience Mesh |

---

## ⚡ Quick Lookup Cheat Sheet

### 1. Maven Dependency Management
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>2.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>4.1.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Core Package Relocations in Spring Boot 4.1.1 & Spring AI 2.0.1
| Feature | Old Location / Deprecated | Spring AI 2.0.1 / Spring Boot 4.1.1 Location |
|---|---|---|
| `BeanOutputConverter` | `org.springframework.ai.chat.converter.*` | `org.springframework.ai.converter.BeanOutputConverter` |
| `ToolCallbacks` | `org.springframework.ai.tool.ToolCallbacks` | `org.springframework.ai.support.ToolCallbacks` |
| `CallAdvisor` | `AroundAdvisor` / legacy client advisors | `org.springframework.ai.chat.client.advisor.api.CallAdvisor` |
| Actuator Health | `org.springframework.boot.actuate.health.*` | `org.springframework.boot.health.contributor.*` |
| Evaluation Contracts | monolithic evaluator package | Split into `spring-ai-commons` (`Evaluator`) & `spring-ai-client-chat` (`RelevancyEvaluator`) |

### 3. Fluent `ChatClient` Invocation Chain
```java
ChatClient client = ChatClient.builder(chatModel)
        .defaultSystem("System instructions")
        .defaultOptions(ChatOptions.builder().temperature(0.2))
        .build();

String answer = client.prompt()
        .user(u -> u.text("Analyze {service}").param("service", "auth-api"))
        .call()
        .content();
```

---

## 🔍 Detailed Topics by Category

- **Core Client & Prompts:**
  - [Maven Coordinates](01-chat-client.md#1-maven-coordinates)
  - [ChatClient Creation & Builder Options](01-chat-client.md#2-chatclient-creation--configuration)
  - [Prompt Specification & Call Outcomes](01-chat-client.md#3-chatclient-prompt--execution-flow)
  - [Unpacking ChatResponse & Usage Accessors](01-chat-client.md#4-chatresponse-prompt-and-accessor-chains)
  - [Constructing Fake ChatResponse Test Double](01-chat-client.md#5-test-double-constructing-fake-chatresponse)

- **Structured Output & Ingestion:**
  - [BeanOutputConverter & Collections](02-structured-outputs.md#1-structured-outputs--beanoutputconverter)
  - [Defensive Markdown Fence Stripping](02-structured-outputs.md#2-defensive-markdown-fence-stripping--sanitization)
  - [The One-Shot Schema Repair Protocol](02-structured-outputs.md#3-the-saga-decide-one-shot-schema-repair-protocol)

- **Tool Execution:**
  - [@Tool, @ToolParam & ToolCallbacks](03-tool-calling.md#1-tool-calling-tool-toolparam-and-toolcallbacks)
  - [Tool Registration & Invocations](03-tool-calling.md#2-tool-discovery--chatclient-registration)
  - [Exception Isolation & Envelopes](03-tool-calling.md#3-tool-invariants--best-practices)

- **Interception & Guardrails:**
  - [CallAdvisor & Onion Execution Ordering](04-guardrail-advisors.md#1-calladvisor--guardrail-pipeline)
  - [Client-Side Advisor Parameter Injection](04-guardrail-advisors.md#21-client-side-advisor-parameter-injection)
  - [The Single-Pass Chain Rule & `chain.copy(this)`](04-guardrail-advisors.md#22-the-single-pass-chain-rule--closed-loop-retries-via-chaincopythis)
  - [Transactional Quota Pre-Allocation & Rollback](04-guardrail-advisors.md#23-transactional-pre-allocation--failure-rollback)
  - [Resilient Failover Routing](04-guardrail-advisors.md#24-resilient-failover-routing-in-the-onion-architecture)

- **Deterministic Testing:**
  - [The ChatPort Hexagonal Seam](05-testing-harness.md#1-the-architectural-seam-problem-direct-mocking-vs-port-interfaces)
  - [Scripted FIFO & Predicate Doubles](05-testing-harness.md#2-scripted-fifo--pattern-matching-test-doubles)
  - [Outgoing Prompt Spy](05-testing-harness.md#3-outgoing-prompt-spy-promptcaptor)
  - [Controllable Virtual Time (TestClock)](05-testing-harness.md#4-controllable-virtual-time-testclock)
  - [Deterministic Reactive Flux Testing](05-testing-harness.md#5-deterministic-reactive-flux-testing)

- **Evaluation & Benchmarks:**
  - [Evaluation Interfaces & Request/Response Contracts](06-evaluators-judges.md#2-core-evaluation-interfaces--data-contracts)
  - [Built-In Relevancy & Fact-Checking Evaluators](06-evaluators-judges.md#3-built-in-spring-ai-evaluators)
  - [Deterministic Telemetry Noise Floor Gating](06-evaluators-judges.md#4-deterministic-telemetry-gating-keeprule-v2)
  - [Position Bias Mitigation & Pairwise Tournaments](06-evaluators-judges.md#51-position-bias-mitigation-pairwise-ab-tournaments)
  - [Calibrated Rubrics & Cohen's Kappa](06-evaluators-judges.md#54-anchor-based-calibrated-rubrics-grade-inflation-mitigation)

- **RAG & Vector Search:**
  - [Document & VectorStore Contracts](07-rag-vector-stores.md#2-core-document--search-contracts)
  - [Metadata Filter DSL](07-rag-vector-stores.md#24-metadata-filter-dsl-filterexpression--filterexpressionbuilder)
  - [Cosine Similarity & Reciprocal Rank Fusion (RRF)](07-rag-vector-stores.md#3-text-splitting--embedding-mechanics)
  - [Spring AI Modular RAG Advisor Stack](07-rag-vector-stores.md#4-spring-ai-modular-rag-advisor-stack)

- **Model Context Protocol (MCP):**
  - [MCP Server Primitives & SyncToolSpecification](08-model-context-protocol.md#3-mcp-server-primitives-phase08-ex01)
  - [RFC 6570 Resource Templates & Log Notifications](08-model-context-protocol.md#36-parameterized-resource-uri-templates-rfc-6570)
  - [MCP Client & Namespaced Tool Prefixing](08-model-context-protocol.md#4-mcp-client-primitives-phase08-ex02)
  - [Federated Multi-Server Tool Routing](08-model-context-protocol.md#43-federated-multi-server-tool-routing)
  - [Dynamic Tool Reload & Sampling Protocol](08-model-context-protocol.md#45-dynamic-tool-list-reloading-notificationstoolslist_changed)

- **Enterprise & Autonomous Loops:**
  - [6-Phase Agent Saga State Machine](09-enterprise-architecture.md#11-autonomous-agent-state-machine-6-phase-saga-loop)
  - [Guaranteed Finally Compensations](09-enterprise-architecture.md#12-guaranteed-finally-compensations)
  - [Multi-Stage Failure Triage](09-enterprise-architecture.md#13-multi-stage-diagnostic-failure-triage)
  - [Enterprise Resilience Mesh (Tenant Routing, Circuit Breaker, Idempotency, Draining)](09-enterprise-architecture.md#23-enterprise-resilience-mesh-patterns)
