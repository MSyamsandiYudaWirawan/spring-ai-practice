# Phase 05 â€” Exercise 02: Advanced Offline Chaos Harness & Resilient Seams

## Mission Overview
In **Phase 05 Exercise 01**, you mastered foundational, repetitive test double seams (`ScriptedChatPort`, `PredicateRoutingChatPort`, `PromptCapturingChatPort`, `BudgetEnforcingChatPort`).

In **Exercise 02**, you take this muscle memory to production-grade resilience: building **adverse, chaotic, and multi-tenant test doubles** that execute completely offline in < 1 second. You will simulate probabilistic network drops, reactive token latency, in-memory HTTP/1.1 socket stubs, state machine circuit breakers, multi-tenant token partition quotas, concurrency stress, and autonomous multi-turn chaos triage.

---

## Pedagogical Protocol & Guardrails
1. **Zero External Network / Instant Verification:** All scenarios run against in-memory doubles and virtual time fixtures. No live LLM calls, zero token costs, zero socket servers.
2. **Deterministic & Non-Flaky Exception Contracts:**
   - [`RateLimitExhaustedException`](src/main/java/phase05/AdvancedHarnessContracts.java#L80-L84): message **MUST contain** `"Rate limit retries exhausted"` (case-insensitive).
   - [`CircuitBreakerOpenException`](src/main/java/phase05/AdvancedHarnessContracts.java#L80-L84): message **MUST contain** `"Circuit breaker is OPEN"` (case-insensitive).
   - [`TenantQuotaExceededException`](src/main/java/phase05/AdvancedHarnessContracts.java#L89-L93): message **MUST contain** `"Quota exceeded for tenant"` (case-insensitive).
3. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`AdvancedOfflineHarnessUnderTest.java`](src/main/java/phase05/AdvancedOfflineHarnessUnderTest.java).
   - All files in [`AdvancedHarnessContracts.java`](src/main/java/phase05/AdvancedHarnessContracts.java) and [`Verifier.java`](src/test/java/phase05/Verifier.java) are immutable.

---

## 10 Scenarios Breakdown

| Scenario | Class | Key Mechanics |
|---|---|---|
| **01** | `ChaosInjectingChatPort` | Seeded probabilistic failure (`random.nextDouble() < matrix.failureRate()`) & payload truncation |
| **02** | `StreamingChunkLatencySimulator` | Reactive `Flux<String>` chunk emission tracking and cumulative simulated latency |
| **03** | `RateLimitBackoffChatPort` | Virtual backoff retry loop on HTTP 429 / rate limits using `AdvancedTestClock` |
| **04** | `DeterministicConversationReplayer` | Replays a golden multi-turn trajectory and asserts agent state transitions |
| **05** | `CircuitBreakingChatPort` | State machine double (`CLOSED` -> `OPEN` -> `HALF_OPEN` -> `CLOSED`) with cooldown |
| **06** | `MultiTenantBudgetPartitionPort` | Hierarchical per-tenant token allocation and quota enforcement |
| **07** | `ConcurrencyRaceDetectorChatPort` | Thread-safe double handling 200 concurrent calls with atomic token accounting |
| **08** | `AdversePayloadCorruptor` & `ResilientJsonExtractor` | Defends against truncated / corrupted JSON payloads with fallback extraction |
| **09** | `ResilientFailoverChatPort` | Transparent failover from primary failing endpoint to secondary fallback stub |
| **10** | `AutonomousChaosTriageHarness` | Full 3-turn autonomous chaos triage lifecycle combining circuit breakers, tenants, and recovery |

---

## Verification Commands

```powershell
# Run the complete test suite:
mvn test-compile exec:java -pl phase05-ex02-advanced-harness

# Run a single scenario (e.g. Scenario 5):
mvn test-compile exec:java -pl phase05-ex02-advanced-harness "-Dexec.args=5"
```
