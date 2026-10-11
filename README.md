# Spring AI Practice Ground: Agentic Engineering Drills

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-blue.svg)](https://spring.io/projects/spring-ai)
[![Architecture](https://img.shields.io/badge/Style-TigerStyle-red.svg)](https://github.com/tigerbeetle/tigerbeetle/blob/main/docs/TIGER_STYLE.md)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **Master the 80% of agentic engineering that tutorials skip:** structured schema repair, sandboxed tool execution, pre/post-call guardrails, deterministic offline harnesses, LLM-as-a-judge evaluators, in-memory RAG vector stores, and Model Context Protocol (MCP) server/client ecosystems.

---

## 🎯 Why This Repository Exists

Most Spring AI guides demonstrate a single `.prompt().user("Hello").call().content()` call and stop there.

In real-world agentic architectures, **pure LLM inference accounts for only ~20% of the codebase**. The remaining 80% consists of:
- **Resilient Structured Outputs:** Preamble/markdown fence stripping, discriminated union extraction, and closed-loop one-shot schema repair.
- **Sandboxed Tool Calling:** Hard turn bounds, path traversal containment, file-size circuit breakers, and audit logging.
- **Advisor Guardrails:** Onion-layered interceptors for cost caps, token quotas with transactional rollback, PII redaction, and downstream model failover.
- **Deterministic Offline Testing:** Scripted doubles, chaos matrix injection, streaming latency simulation, and virtual time seams (0 token cost, 100% deterministic, instant).
- **LLM-as-a-Judge & Evaluators:** Relevancy, factuality, noise-floor telemetry keep gates, rubric parsers, and tournament judges.
- **RAG & Vector Stores:** Text chunking, deterministic vector embeddings, cosine similarity, metadata filtering, contextual query augmentation, and query expansion.
- **Model Context Protocol (MCP):** MCP Server tool/resource/prompt specification exposure, MCP Client adapter integration, multi-server tool federation, and agentic gateway routing.

This repository provides **17 high-repetition deliberate practice exercises (258 total muscle-memory drills)**, designed to build instinctive fluency across all core Spring AI primitives before assembling them into production agentic systems.

---

## 🏗️ The Curriculum & Roadmap

Each module follows a strict **repetitive muscle-memory format** (5 core framework topics × 3 back-to-back repetitions = 15 drills per exercise):
1. **Exercise 01:** Core repetition drill (building instinctive fluency on foundational mechanics).
2. **Exercise 02:** Advanced resilient drill (edge cases, failure injection, circuit-breakers, and high-stress scenarios).
3. **Exercise 03 (where applicable):** Cloud SRE / FinTech specialization drills.

| Phase | Module | Focus | Drills |
|---|---|---|---|
| **Phase 01** | [`phase01-ex01-chat-basics`](phase01-ex01-chat-basics/)<br>[`phase01-ex02-chat-advanced`](phase01-ex02-chat-advanced/)<br>[`phase01-ex03-cloud-sre`](phase01-ex03-cloud-sre/) | `ChatClient` fluent API, system/user role separation, prompt templates, runtime options override, entity mapping, and multi-turn conversational loops. | 45 Drills (3 × 15) |
| **Phase 02** | [`phase02-ex01-structured-outputs`](phase02-ex01-structured-outputs/)<br>[`phase02-ex02-resilient-decisions`](phase02-ex02-resilient-decisions/) | `BeanOutputConverter`, generic collections (`ParameterizedTypeReference`), markdown fence stripping, domain validation, and 1-shot resilient schema repair. | 30 Drills (2 × 15) |
| **Phase 03** | [`phase03-ex01-tool-sandboxing`](phase03-ex01-tool-sandboxing/)<br>[`phase03-ex02-multi-tool-drills`](phase03-ex02-multi-tool-drills/) | Spring AI `@Tool` & `@ToolParam`, programmatic `ToolCallbacks`, execution sandboxing, path traversal guards, turn limits, and audit trails. | 30 Drills (2 × 15) |
| **Phase 04** | [`phase04-ex01-guardrail-advisors`](phase04-ex01-guardrail-advisors/)<br>[`phase04-ex02-resilient-guardrails`](phase04-ex02-resilient-guardrails/) | Spring AI `CallAdvisor` chain, security keyword circuit-breakers, dollar/token budget accumulators, sliding-window rate limiters, and failover routing. | 30 Drills (2 × 15) |
| **Phase 05** | [`phase05-ex01-offline-harness`](phase05-ex01-offline-harness/)<br>[`phase05-ex02-advanced-harness`](phase05-ex02-advanced-harness/) | Deterministic test harnesses, scripted FIFO doubles, predicate routing, fault injection, outgoing prompt spies, chaos matrices, and virtual clock seams. | 30 Drills (2 × 15) |
| **Phase 06** | [`phase06-ex01-evaluators`](phase06-ex01-evaluators/)<br>[`phase06-ex02-advanced-judges`](phase06-ex02-advanced-judges/) | Spring AI `Evaluator`, `RelevancyEvaluator`, noise floor keep gates, rubric score parsers, pairwise A/B tournament judges, and calibration gates. | 30 Drills (2 × 15) |
| **Phase 07** | [`phase07-ex01-vector-stores`](phase07-ex01-vector-stores/)<br>[`phase07-ex02-rag-advisors`](phase07-ex02-rag-advisors/) | In-memory `VectorStore`, text chunking (`TokenTextSplitter`), document enrichment, cosine similarity, metadata filtering (`FilterExpressionBuilder`), `DocumentRetriever`, contextual query augmentation, and query expansion. | 33 Drills (18 + 15) |
| **Phase 08** | [`phase08-ex01-mcp-server`](phase08-ex01-mcp-server/)<br>[`phase08-ex02-mcp-client`](phase08-ex02-mcp-client/) | Model Context Protocol (MCP): `ServerCapabilities`, JSON tool schemas, `SyncToolSpecification`, `ToolCallback` adaptation, resource URIs, prompt templates, multi-server federation, catalog reload, and sampling delegation. | 30 Drills (2 × 15) |
| **TOTAL** | **17 Modules Across 8 Phases** | **Production-grade Spring AI & Agentic Architecture** | **258 Drills** |

---

## 💡 Tiered Progressive Hints System

Every failing test verifier prints **3-Tiered Progressive Hints** alongside failure details to prevent frustration while preserving learning:
- **`[HINT 1 (Concept)]`**: Explains the underlying framework abstraction or architectural invariant.
- **`[HINT 2 (Edge Case)]`**: Highlights the specific boundary check, exception condition, or fail-fast invariant.
- **`[HINT 3 (Code Pattern)]`**: Provides the exact idiomatic Spring AI code pattern needed to pass the gate.

Example terminal output when running a drill:
```
  [FAIL] Scenario 01: askDirectUserMessage (Direct User Prompt & Extraction)
         --> DETAIL: Expected 'Database connection pool saturated', got: null
         --> [HINT 1 (Concept)] Call ChatClient fluent API starting from client.prompt().
         --> [HINT 2 (Edge Case)] Throw IllegalArgumentException if userQuery is null or blank.
         --> [HINT 3 (Code Pattern)] return client.prompt().user(userQuery).call().content();
```

---

## 📊 Interactive Terminal Progress Trackers

Track your curriculum progress across all 17 modules and 258 drills with live progress bars and mastery tiers.

### PowerShell CLI (`tracker.ps1`)
```powershell
# Display full curriculum dashboard
.\tracker.ps1

# Run tests across all 17 modules and update progress
.\tracker.ps1 -RunAll

# Run and inspect a specific phase (e.g. Phase 01)
.\tracker.ps1 -Phase 1

# Run and inspect a single exercise module
.\tracker.ps1 -Module phase01-ex01-chat-basics

# Launch interactive menu
.\tracker.ps1 -Interactive
```

### Cross-Platform Java Tracker (`ProgressTracker.java`)
Single-file source launcher requiring zero external dependencies:
```bash
java ProgressTracker.java
```

*Sample Dashboard Output:*
```
==========================================================================================
           SPRING AI TIGERSTYLE MASTERY - INTERACTIVE PROGRESS TRACKER                    
       17 Exercises | 258 Muscle-Memory Drills | Production-Grade AI Architecture        
==========================================================================================

--- PHASE 01 ----------------------------------------------------------------------------
  [ PASS ] Phase 01 Ex01: ChatClient Fluent API & Foundations   15/15 [################] 100.0%
  [ PASS ] Phase 01 Ex02: Advanced ChatClient & FinTech Ops     15/15 [################] 100.0%
  [ PASS ] Phase 01 Ex03: Cloud SRE & K8s Operations            15/15 [################] 100.0%
...
==========================================================================================
  OVERALL PROGRESS:  258 / 258 DRILLS PASSED  (100.0%)
  PROGRESS BAR:      [##############################] 100.0%
  MASTERY TIER:      GRANDMASTER (100% Production Ready)
==========================================================================================
```

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
2. **Reproduce the FAIL First:** Run the verifier before writing code. Observe the exit code `99` and review the tiered progressive hints.
3. **Fail-Fast Domain Invariants:** Validate non-null boundaries, check regexes, and defend against corrupted LLM outputs immediately (`Objects.requireNonNull`, `isBlank()`).
4. **Zero Flakiness:** All verifier requirements and mandatory error keywords are documented directly in the method Javadoc and exercise `README.md`.
5. **Zero Token Cost:** All drills execute 100% offline in milliseconds using deterministic test seams.
6. **Reference Golden Solutions:** When stuck, refer to the verified reference solutions in [`docs/golden/`](docs/golden/).

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

4. **Implement the solution:**
   Edit `phase01-ex01-chat-basics/src/main/java/phase01/ChatServiceUnderTest.java`.

5. **Clear the gate (Gate 0):**
   Re-run `mvn test -pl phase01-ex01-chat-basics` until all 15 scenarios pass:
   ```
   TOTAL: 15 / 15 PASSED
   VERDICT: ALL 15 GATES PASSED (exit code 0)
   ```

6. **Track your progress:**
   ```bash
   java ProgressTracker.java
   ```

---

## 📖 Reference Documentation

- [Spring AI 2.0.1 Reference Notes](docs/reference/spring-ai-2.0.1-reference.md)
- [Sealed Golden Reference Solutions](docs/golden/)

---

## 📄 License

This repository is licensed under the Apache License 2.0.
