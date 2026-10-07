# Spring AI Practice Ground: Enterprise Agentic Engineering Drills

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-blue.svg)](https://spring.io/projects/spring-ai)
[![Architecture](https://img.shields.io/badge/Style-TigerStyle-red.svg)](https://github.com/tigerbeetle/tigerbeetle/blob/main/docs/TIGER_STYLE.md)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **Master the 80% of agentic engineering that tutorials skip:** structured schema repair, sandboxed tool execution, pre/post-call guardrails, deterministic offline harnesses, LLM-as-a-judge evaluators, in-memory RAG vector stores, and Model Context Protocol (MCP) server/client ecosystems.

---

## 🎯 Why This Repository Exists

Most Spring AI guides demonstrate a single `.prompt().user("Hello").call().content()` call and stop there.

In real-world enterprise agentic architectures (such as [**`agentic-performance-diagnostician`**](https://github.com/syamsandi/agentic-performance-diagnostician)), **pure LLM inference accounts for only ~20% of the codebase**. The remaining 80% consists of:
- **Resilient Structured Outputs:** Preamble/markdown fence stripping, discriminated union extraction, and closed-loop one-shot schema repair.
- **Sandboxed Tool Calling:** Hard turn bounds, path traversal containment, file-size circuit breakers, and audit logging.
- **Advisor Guardrails:** Onion-layered interceptors for cost caps, token quotas with transactional rollback, PII redaction, and downstream model failover.
- **Deterministic Offline Testing:** Scripted doubles, chaos matrix injection, streaming latency simulation, and virtual time seams (0 token cost, 100% deterministic, instant).
- **LLM-as-a-Judge & Evaluators:** Relevancy, factuality, noise-floor telemetry keep gates, rubric parsers, and tournament judges.
- **RAG & Vector Stores:** Text chunking, deterministic vector embeddings, cosine similarity, metadata filtering, contextual query augmentation, and query expansion.
- **Model Context Protocol (MCP):** MCP Server tool/resource/prompt specification exposure, MCP Client adapter integration, multi-server tool federation, and agentic gateway routing.

This repository provides **17 high-repetition deliberate practice drills (163 total scenarios)**, designed to build instinctive muscle memory across all core Spring AI primitives before assembling them into production agentic systems.

---

## 🏗️ The 8-Phase Curriculum

Each phase contains progressive drills:
1. **Exercise 01:** Core repetition drill (building instinctive fluency on foundational mechanics).
2. **Exercise 02:** Advanced resilient drill (edge cases, failure injection, circuit-breakers, and high-stress scenarios).

| Phase | Module | Focus | Scenarios |
|---|---|---|---|
| **Phase 01** | [`phase01-ex01-chat-basics`](phase01-ex01-chat-basics/)<br>[`phase01-ex02-chat-advanced`](phase01-ex02-chat-advanced/)<br>[`phase01-ex03-cloud-sre`](phase01-ex03-cloud-sre/) | `ChatClient` fluent API, system/user role separation, template binding, runtime options override. | 21 Scenarios |
| **Phase 02** | [`phase02-ex01-structured-outputs`](phase02-ex01-structured-outputs/)<br>[`phase02-ex02-resilient-decisions`](phase02-ex02-resilient-decisions/) | `BeanOutputConverter`, generic collections (`ParameterizedTypeReference`), markdown fence stripping, domain validation, 1-shot repair. | 16 Scenarios |
| **Phase 03** | [`phase03-ex01-tool-sandboxing`](phase03-ex01-tool-sandboxing/)<br>[`phase03-ex02-multi-tool-drills`](phase03-ex02-multi-tool-drills/) | Spring AI `@Tool` & `@ToolParam`, reflection registration (`ToolCallbacks`), sandboxing, path traversal guards, turn limits, audit trails. | 18 Scenarios |
| **Phase 04** | [`phase04-ex01-guardrail-advisors`](phase04-ex01-guardrail-advisors/)<br>[`phase04-ex02-resilient-guardrails`](phase04-ex02-resilient-guardrails/) | Spring AI `CallAdvisor` chain, security keyword circuit-breakers, dollar/token budget accumulators, sliding-window rate limiters, failover routing. | 20 Scenarios |
| **Phase 05** | [`phase05-ex01-offline-harness`](phase05-ex01-offline-harness/)<br>[`phase05-ex02-advanced-harness`](phase05-ex02-advanced-harness/) | Deterministic test harnesses, scripted FIFO doubles, predicate routing, fault injection, outgoing prompt spies, chaos matrices, virtual clock seams. | 20 Scenarios |
| **Phase 06** | [`phase06-ex01-evaluators`](phase06-ex01-evaluators/)<br>[`phase06-ex02-advanced-judges`](phase06-ex02-advanced-judges/) | Spring AI `Evaluator`, `RelevancyEvaluator`, `FactCheckingEvaluator`, noise floor keep gates, rubric score parsers, pairwise A/B tournament judges. | 20 Scenarios |
| **Phase 07** | [`phase07-ex01-vector-stores`](phase07-ex01-vector-stores/)<br>[`phase07-ex02-rag-advisors`](phase07-ex02-rag-advisors/) | In-memory `VectorStore`, text chunking, embedding generation, cosine similarity, metadata filtering, MMR diversity re-ranking, deduplication, `DocumentRetriever`, contextual prompt augmentation, query expansion, HyDE, token budget packing. | 24 Scenarios |
| **Phase 08** | [`phase08-ex01-mcp-server`](phase08-ex01-mcp-server/)<br>[`phase08-ex02-mcp-client`](phase08-ex02-mcp-client/) | Model Context Protocol (MCP): `ServerCapabilities`, JSON tool schemas, `SyncToolSpecification`, `ToolCallback` adaptation, URI templates, log notifications, remote client discovery, multi-server federation, dynamic tool reloading, sampling, agentic gateway. | 24 Scenarios |

---

## ⚡ Deliberate Practice Rules of Engagement

Every drill follows strict TigerStyle work-sample engineering:

```
phaseXX-exYY-<name>/
├── pom.xml
├── README.md                 # Specifications, constraints, and keywords
├── <Service>UnderTest.java   # THE ONLY FILE YOU MODIFY
├── ModelContracts.java       # Immutable records, schemas, and domain envelopes
├── FakeChatModel.java        # Deterministic offline mock (0 token cost)
└── Verifier.java             # Gatekeeper: exits 99 on failure, exits 0 on pass
```

1. **Touch Only `<Service>UnderTest.java`:** Contracts, models, test doubles, and verifiers are immutable. Weakening a verifier is specification gaming.
2. **Reproduce the FAIL First:** Run the verifier before writing code. Observe the exit code `99` and understand the exact defect mechanism.
3. **Fail-Fast Domain Invariants:** Validate non-null boundaries, check regexes, and defend against corrupted LLM outputs immediately (`Objects.requireNonNull`, `isBlank()`).
4. **Zero Flakiness:** All verifier requirements and mandatory error keywords are documented directly in the method Javadoc and exercise `README.md`.
5. **Zero Token Cost:** All drills execute 100% offline in milliseconds using deterministic test seams.

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** or later (temurin, zulu, or corretto)
- **Apache Maven 3.9+**

### Quickstart

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/spring-ai-practice.git
   cd spring-ai-practice
   ```

2. **Verify all modules compile cleanly:**
   ```bash
   mvn test-compile
   ```

3. **Pick an exercise and reproduce the failure (Gate 99):**
   ```bash
   mvn test -pl phase01-ex01-chat-basics
   ```
   *Expected output:*
   ```
   ======================================================================
                    PHASE 01 EXERCISE 01: VERIFICATION REPORT
   ======================================================================
   SCENARIO 1: askWithSystemInstructions (Role Separation)
     [FAIL] Role separation violated: System instructions were not routed via .system(...)
   ...
   VERDICT: FINDINGS DETECTED (exit code 99)
   ```

4. **Implement the solution:**
   Edit `phase01-ex01-chat-basics/src/main/java/phase01/ChatServiceUnderTest.java`.

5. **Clear the gate (Gate 0):**
   Re-run `mvn test -pl phase01-ex01-chat-basics` until all scenarios pass:
   ```
   SUMMARY: 3 PASSED, 0 FAILED
   VERDICT: ALL SCENARIOS PASSED (exit code 0)
   ```

---

## 🔗 Reference Architecture Connection

This practice suite is the companion curriculum to [**`agentic-performance-diagnostician`**](https://github.com/syamsandi/agentic-performance-diagnostician) — an autonomous, self-healing Java performance engineering agent that uses Spring AI to analyze JFR telemetry, generate bytecode/configuration optimizations, execute canaries, and apply automated compensatory rollback on regression.

By completing these 8 phases, you will understand every line of code inside enterprise autonomous agents — from prompt formatting and guardrails to RAG retrieval and federated Model Context Protocol (MCP) ecosystems.

---

## 📖 Reference Documentation

- [Spring AI 2.0.1 Reference Notes](docs/reference/spring-ai-2.0.1-reference.md)
- [Sealed Golden Reference Solutions](docs/golden/)

---

## 📄 License

This repository is licensed under the Apache License 2.0.
