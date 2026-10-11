# Phase 05 — Exercise 02: Advanced Offline Chaos Harness & Resilient Seams

## Mission Overview
In **Phase 05 Exercise 01**, you mastered foundational, repetitive test double seams (`ScriptedChatPort`, `PredicateRoutingChatPort`, `PromptCapturingChatPort`, `BudgetEnforcingChatPort`).

In **Exercise 02**, you take this muscle memory to production-grade resilience: building **adverse, chaotic, and multi-tenant test doubles** that execute completely offline in < 1 second. You will simulate probabilistic network drops, reactive token latency, circuit breaker state machines, hierarchical multi-tenant token quotas, concurrency stress, and autonomous multi-turn chaos triage.

---

## 15-Drill Clustered Syllabus

### Topic 1: Chaos Injection & Payload Corruption (Drills 1 - 3)
- **Scenario 01: `ChaosInjectingChatPort` (Probabilistic Fault & Latency Injection)**  
  Foundation: Seeded probabilistic failure (`random.nextDouble() < matrix.failureRate()`), tracking total calls and failure counts.
- **Scenario 02: `CorruptingChaosChatPort` (Payload Truncation & Field Mangle Double)**  
  Variation: Downstream parser resilience testing via character truncation and regex-based JSON field mutation (`mangleField`).
- **Scenario 03: `CompositeChaosPipeline` (Multi-Stage Chaos Pipeline with Auditing)**  
  Real-world Composition: Multi-stage chaos pipeline (latency $\to$ probabilistic drop $\to$ corruption) generating an immutable audit trail (`ChaosAuditEntry`).

### Topic 2: Reactive Streaming Delays & Backpressure (Drills 4 - 6)
- **Scenario 04: `StreamingChunkLatencySimulator` (Reactive Chunk Delay & Backpressure)**  
  Foundation: Reactive `Flux<String>` chunk emission tracking and cumulative simulated latency without wall-clock sleep.
- **Scenario 05: `IntermittentDropStreamingChatPort` (Simulated Token Drops & Resumption)**  
  Variation: Intercept and drop streaming chunks matching a predicate while safely emitting the remaining tokens.
- **Scenario 06: `DeterministicConversationReplayer` (Trajectory Replay & State Verification)**  
  Real-world Composition: Replay a multi-turn conversation trajectory, assert agent step conformity, and accumulate total tokens.

### Topic 3: Rate Limiting & Circuit Breakers (Drills 7 - 9)
- **Scenario 07: `RateLimitBackoffChatPort` (Simulated Rate-Limit & Virtual Backoff Double)**  
  Foundation: Virtual exponential backoff retry loop on HTTP 429 errors using `AdvancedTestClock`; throws `RateLimitExhaustedException`.
- **Scenario 08: `CircuitBreakingChatPort` (Resilient State Machine Test Double)**  
  Variation: Full state machine double (`CLOSED` $\to$ `OPEN` $\to$ `HALF_OPEN` $\to$ `CLOSED`) with cooldown enforcement throwing `CircuitBreakerOpenException`.
- **Scenario 09: `AdaptiveThrottlingChatPort` (Sliding Window Dynamic Rate Limiter)**  
  Real-world Composition: Sliding window rate limiter pruning old timestamps and rejecting requests exceeding window capacity.

### Topic 4: Multi-Tenant Quotas & Thread-Safe Concurrency (Drills 10 - 12)
- **Scenario 10: `MultiTenantBudgetPartitionPort` (Hierarchical Token Quota Manager)**  
  Foundation: Per-tenant token allocation and quota enforcement throwing `TenantQuotaExceededException`.
- **Scenario 11: `TenantTierThrottlingChatPort` (Tier-Based Quotas & Policies)**  
  Variation: Subscription tier partitioning (`FREE`, `PRO`, `ENTERPRISE`) with dynamic usage accounting.
- **Scenario 12: `ConcurrencyRaceDetectorChatPort` (Thread-Safe Stress Double)**  
  Real-world Composition: Thread-safe double handling 200 concurrent calls across 20 threads with zero race conditions.

### Topic 5: Resilient Failover, Schema Conversion & Autonomous Loops (Drills 13 - 15)
- **Scenario 13: `ResilientFailoverChatPort` (Primary Endpoint Failure -> Fallback Stub)**  
  Foundation: Transparent failover from a failing primary endpoint to a secondary fallback double.
- **Scenario 14: `SchemaValidatingChatPort` (Spring AI BeanOutputConverter & Resilient Fallback)**  
  Variation: Structured output parsing with Spring AI's `BeanOutputConverter`, gracefully catching corrupted payloads and falling back to safe defaults.
- **Scenario 15: `AutonomousChaosTriageHarness` (Full Chaos Integration & Triage Lifecycle)**  
  Real-world Composition: Full 3-turn autonomous chaos triage loop combining failover, structured investigation, and reporting.

---

## Pedagogical Protocol & Guardrails
1. **Zero External Network / Instant Verification:** All scenarios run against in-memory doubles and virtual time fixtures. No live LLM calls, zero token costs.
2. **Deterministic & Non-Flaky Exception Contracts:**
   - `RateLimitExhaustedException`: message **MUST contain** `"Rate limit retries exhausted"` (case-insensitive).
   - `CircuitBreakerOpenException`: message **MUST contain** `"Circuit breaker is OPEN"` (case-insensitive).
   - `TenantQuotaExceededException`: message **MUST contain** `"Quota exceeded for tenant"` (case-insensitive).
3. **Tiered Progressive Hints in Verifier:**
   Every failure in the gate verifier prints 3 structured hints to guide your solution without frustration:
   - `[HINT 1 (Concept)]`: Architectural concept and data structure to use.
   - `[HINT 2 (Edge Case)]`: Specific edge case, null-safety check, or condition to handle.
   - `[HINT 3 (Code Pattern)]`: Precise code pattern or API call to implement.

---

## Verification Commands

To run all 15 scenario gates:
```bash
mvn test-compile exec:java -pl phase05-ex02-advanced-harness
```

To run a single scenario drill (e.g. Scenario 8):
```bash
mvn test-compile exec:java -pl phase05-ex02-advanced-harness -Dexec.args="8"
```

To run JUnit / Surefire test:
```bash
mvn test -pl phase05-ex02-advanced-harness
```
