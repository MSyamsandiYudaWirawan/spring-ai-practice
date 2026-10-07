# Phase 05 - Exercise 01: Deterministic Offline Test Harness & LLM Boundary Seams

## Purpose & Pedagogical Architecture
In complex agentic and Spring AI architectures, testing against live LLMs causes flakiness, latency, and costs. Trying to mock Spring AI's internal `ChatClient` builder chain directly is painful. 
The architectural solution used in resilient architectures is a **boundary seam**:
- Application loops talk to an interface we own: `ChatPort`.
- In production, `SpringAiChatPort` adapts `ChatPort` to Spring AI's `ChatClient`.
- In tests, deterministic test doubles (`ScriptedChatPort`, `PredicateRoutingChatPort`, `FaultInjectingChatPort`, `PromptCapturingChatPort`, `BudgetEnforcingChatPort`) run 100% offline in < 500ms with zero live API calls.

This drill builds instinctive muscle memory on writing, wiring, and verifying these test harness seams across 10 foundational scenarios.

---

## The 10 Scenarios & Contracts

| # | Component | Responsibility & Contract |
|---|---|---|
| **01** | `ScriptedChatPort` | FIFO queue of scripted responses (`ChatResult(text, tokensIn, tokensOut)`). Increments `callCount()`. If queue is exhausted, throws `IllegalStateException`. |
| **02** | `PredicateRoutingChatPort` | Dynamic response routing based on matching predicates (`Predicate<String>`) on the user prompt. Falls back to `defaultResult`. |
| **03** | `FaultInjectingChatPort` | Chaos injection double supporting both successful responses (`enqueueResult`) and exceptions (`enqueueFault`). Allows testing transient 503/timeout recovery loops. |
| **04** | `PromptCapturingChatPort` | Spy delegating to another `ChatPort` while recording all outgoing `PromptCapture(system, user, toolBeans, timestamp)`. |
| **05** | `BudgetEnforcingChatPort` | Test spend ceiling wrapper: accumulates tokens across calls; throws `HarnessBudgetExceededException` (must contain `"Offline token budget exceeded"`) if cumulative tokens exceed maximum budget. |
| **06** | `ToolRegistrySpy` | Reflection inspection utility: extracts method names (or `@Tool(name = "...")`) of all `@Tool`-annotated methods on tool beans without executing them. |
| **07** | `InMemoryStreamingChatPort` | Reactive streaming double (`StreamingChatPort`): emits token chunks as a deterministic `Flux<String>` from `Flux.fromIterable(chunks)`. |
| **08** | `VirtualTimeSimulator` | Time advancement seam: executes an action and advances `TestClock` by simulated milliseconds without wall-clock sleep. |
| **09** | `SpringAiChatPort` | Production adapter: implements `ChatPort` by delegating to Spring AI's `ChatClient`, extracting output text and safely mapping `Usage` metadata tokens. |
| **10** | `DeterministicDiagnosticLoop` | Multi-turn triage loop runner: executes an autonomous 3-turn diagnostic investigation against `ChatPort`, returning `DiagnosticTriageReport` with exact turn counts and total tokens. |

---

## Non-Flaky Exception Contract
- **Scenario 5:** `HarnessBudgetExceededException` message MUST contain `"Offline token budget exceeded"` (checked case-insensitively with `.toLowerCase().contains(...)`).

---

## Verification Commands

To run all 10 scenario gates:
```bash
mvn test-compile exec:java -pl phase05-ex01-offline-harness
```

To run a single scenario (e.g. Scenario 1):
```bash
mvn test-compile exec:java -pl phase05-ex01-offline-harness -Dexec.args="1"
```

To run JUnit / Surefire test:
```bash
mvn test -pl phase05-ex01-offline-harness
```
