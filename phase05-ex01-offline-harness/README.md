# Phase 05 - Exercise 01: Deterministic Offline Test Harness & LLM Boundary Seams

## Purpose & Pedagogical Architecture
In complex agentic and Spring AI architectures, testing against live LLMs causes flakiness, latency, and costs. Trying to mock Spring AI's internal `ChatClient` builder chain directly is painful. 
The architectural solution used in resilient architectures is a **boundary seam**:
- Application loops talk to an interface we own: `ChatPort`.
- In production, `SpringAiChatPort` adapts `ChatPort` to Spring AI's `ChatClient`.
- In tests, deterministic test doubles (`ScriptedChatPort`, `PredicateRoutingChatPort`, `FaultInjectingChatPort`, `PromptCapturingChatPort`, `BudgetEnforcingChatPort`) run 100% offline in < 500ms with zero live API calls.

This drill builds instinctive muscle memory on writing, wiring, and verifying these test harness seams across **15 repetitive drills** structured into 5 core topics (Foundation $\to$ Variation $\to$ Real-world Composition).

---

## 🧠 Architectural Theory & Hexagonal Boundary Seam

Directly mocking Spring AI's fluent `ChatClient` in unit tests requires mocking 6+ internal builder classes, creating brittle tests that break on minor upgrades. Hexagonal boundary seams solve this:

```
                             HEXAGONAL SEAM & DETERMINISTIC TEST HARNESS
                                     [Application Agent Loop]
                                                │
                                                ▼ talks to domain seam
                                           [ChatPort]
                                                │
                    ┌───────────────────────────┴───────────────────────────┐
                    ▼ (Production)                                          ▼ (Deterministic Tests)
           [SpringAiChatPort]                                      [Deterministic Test Doubles]
                    │                                              (Runs in 0ms, 0 live tokens)
                    ▼                                                       │
            [ChatClient.prompt()]                                           ├─► ScriptedChatPort (FIFO Queue)
                    │                                                       ├─► PredicateRoutingChatPort (Pattern Match)
                    ▼                                                       ├─► PromptCapturingChatPort (Outgoing Spy)
            [Live LLM Provider]                                             ├─► FaultInjectingChatPort (Simulate 503/OOM)
                                                                            └─► BudgetEnforcingChatPort (Token Ceiling)
```

### 🧩 Core Seam Contracts & Roles

| Component | Nature | Role & Execution Behavior |
|---|---|---|
| `ChatPort` | Domain Port Interface | Minimal interface (`chat(system, user, tools) -> ChatResult`) owned by your application. |
| `SpringAiChatPort` | Production Adapter | Unpacks `ChatPort` parameters, invokes `ChatClient.prompt().call()`, and converts `ChatResponse` back to `ChatResult`. |
| `ScriptedChatPort` | FIFO Test Double | In-memory queue returning canned answers sequentially. Throws `IllegalStateException` on empty queue. |
| `PredicateRoutingChatPort` | Pattern-Matching Double | Dynamically evaluates prompt text against configured `Predicate<String>` rules, returning distinct answers per scenario. |
| `PromptCapturingChatPort` | Outgoing Test Spy | Captures outgoing prompt templates, system instructions, and tool bindings for strict JUnit assertions. |

---

## 15-Drill Clustered Syllabus

### Topic 1: Scripted FIFO & Inspection Doubles
- **Scenario 01: `ScriptedChatPort` (FIFO Response Queue Test Double)**  
  Foundation: Maintain an internal FIFO `Deque<ChatResult>`, track `callCount`, throw `IllegalStateException` on exhausted queue.
- **Scenario 02: `IntrospectableScriptedChatPort` (Queue Introspection & Reset)**  
  Variation: Add `peek()`, `remaining()`, `hasMore()`, and `clear()` state reset.
- **Scenario 03: `SequentialDialogueReplayer` (Multi-Turn Scripted Replay Seam)**  
  Real-world Composition: Multi-turn dialogue replayer validating prompt substrings per turn and verifying complete dialogue matching via `validateCompletion()`.

### Topic 2: Dynamic Pattern & Predicate Routing Doubles
- **Scenario 04: `PredicateRoutingChatPort` (Dynamic Pattern Matching Test Double)**  
  Foundation: Match user prompts against ordered `Predicate<String>` rules, with fallback `defaultResult`.
- **Scenario 05: `BiPredicateRoutingChatPort` (System & User Dual-Predicate Matcher)**  
  Variation: Dual-predicate matching evaluating both system prompt and user query (`BiPredicate<String, String>`).
- **Scenario 06: `RegexTemplateRoutingChatPort` (Parameterized Dynamic Response Generator)**  
  Real-world Composition: Regex pattern matching with dynamic capture group interpolation (`${1}`, `${2}`) and dynamic token accounting based on string lengths.

### Topic 3: Fault Injection & Resilience Testing
- **Scenario 07: `FaultInjectingChatPort` (Simulated Transient Fault & Retry Double)**  
  Foundation: Test double supporting both successful responses (`enqueueResult`) and exceptions (`enqueueFault`).
- **Scenario 08: `ConsecutiveFaultChatPort` (K-Failure Fault Burst Generator)**  
  Variation: Throw $K$ consecutive upstream faults before recovering and returning a recovery result; track `totalAttempts()`.
- **Scenario 09: `ExponentialBackoffRetrySimulator` (Virtual Time Retry Loop)**  
  Real-world Composition: Deterministic exponential backoff loop advancing a `TestClock` on simulated upstream failures without wall-clock sleep.

### Topic 4: Observation, Auditing & Budget Ceilings
- **Scenario 10: `PromptCapturingChatPort` (Outgoing Prompt Spy & Inspection Seam)**  
  Foundation: Decorate a `ChatPort` while capturing immutable `PromptCapture(system, user, toolBeans, timestamp)` records.
- **Scenario 11: `BudgetEnforcingChatPort` (Test Spend Ceiling Guardrail)**  
  Variation: Track cumulative token consumption; throw `HarnessBudgetExceededException` (must contain `"Offline token budget exceeded"`) when ceiling is breached.
- **Scenario 12: `AuditingCompositeChatPort` (Full Audit Trail Decorator)**  
  Real-world Composition: Full audit log recording per-call deltas, cumulative totals, timestamps, and reporting the peak token call via `peakTokenCall()`.

### Topic 5: Framework Adapters, Streaming & Autonomous Loops
- **Scenario 13: `ToolRegistrySpy` (Tool Bean Reflection & Discovery Seam)**  
  Foundation: Reflection discovery of `@Tool`-annotated methods on Spring beans.
- **Scenario 14: `InMemoryStreamingChatPort` (Deterministic Reactive Flux Test Double)**  
  Variation: Reactive streaming seam (`StreamingChatPort`) emitting token chunks deterministically via `Flux.fromIterable(chunks)`.
- **Scenario 15: `SpringAiChatPort & DeterministicDiagnosticLoop` (Multi-Turn Triage Harness)**  
  Real-world Composition: Connect the production `ChatClient` adapter and execute an autonomous 3-turn diagnostic investigation loop reporting findings and token costs.

---

## Tiered Progressive Hints in Verifier

Every scenario failure provides 3 progressive hint tiers directly in your terminal to guide you without frustration:
- `[HINT 1 (Concept)]`: Architectural concept and data structure to use.
- `[HINT 2 (Edge Case)]`: Specific edge case, null-safety check, or condition to handle.
- `[HINT 3 (Code Pattern)]`: Precise code pattern or API call to implement.

---

## Verification Commands

To run all 15 scenario gates:
```bash
mvn test-compile exec:java -pl phase05-ex01-offline-harness
```

To run a single scenario drill (e.g. Scenario 3):
```bash
mvn test-compile exec:java -pl phase05-ex01-offline-harness -Dexec.args="3"
```

To run JUnit / Surefire test:
```bash
mvn test -pl phase05-ex01-offline-harness
```
