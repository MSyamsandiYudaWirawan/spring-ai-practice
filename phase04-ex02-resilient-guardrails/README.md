# Phase 04 - Exercise 02: Resilient Guardrails, Failover Routing, and Transactional Token Quotas

## Purpose & Pedagogical Architecture
Phase 04 Exercise 01 established the mechanics of Spring AI's `CallAdvisor` interface. Exercise 02 reinforces that exact repetitive pattern (`public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain)`) across **15 muscle-memory drills** organized into 5 progressive topic clusters (3 repetitive variations per cluster):

- **Topic 1: Context Augmentation & History Truncation** (Drills 01–03)
- **Topic 2: Rate Limiting & Distributed Tracing** (Drills 04–06)
- **Topic 3: Failover Routing & Circuit Breaking** (Drills 07–09)
- **Topic 4: Schema Healing & Hallucination Grounding** (Drills 10–12)
- **Topic 5: Transactional Quotas & Production Stacks** (Drills 13–15)

---

## The 15 Drills & Exact Contracts

| # | Topic | Drill / Component | Order | Core Contract & Invariants |
|---|---|---|---|---|
| 01 | T1 | `RuntimeContextAdvisor` | 10 | Inspects `request.context()` for `"executionMode"`. If `"DRY_RUN"`, prefixes `UserMessage` with `"[DRY_RUN] "`, sets response context `"audit.dryRun" = true` and `"processedBy" = "RuntimeContextAdvisor"`. Otherwise sets `"audit.dryRun" = false`. |
| 02 | T1 | `SlidingWindowTruncationAdvisor` | 20 | Separates `SystemMessage` from history; preserves `SystemMessage` at index 0; keeps only last `maxHistoryMessages` non-system messages; records dropped count in response context `"truncatedMessageCount"`. |
| 03 | T1 | `SystemPromptDynamicAugmentorAdvisor` | 25 | Injects dynamic tenant/env tags into existing system message; records `"dynamicSystemTags"`. |
| 04 | T2 | `SlidingWindowRateLimiterAdvisor` | 30 | Rolling timestamp `Deque<Long>` with eviction (`<= now - windowMs`). If `size >= maxRequests`, throws `RateLimitExceededException` (must contain `"Rate limit exceeded"`). Enriches response context with `"remainingRequests"`. |
| 05 | T2 | `TraceContextPropagationAdvisor` | 40 | Checks or generates W3C `traceparent` (`00-<32hex>-<16hex>-01`). Injects `"[traceparent=" + traceparent + "] "` into `UserMessage`, preserves in response context, sets `"propagationSuccess" = true`. |
| 06 | T2 | `CompositeRateAndTraceAdvisor` | 35 | Ingress composite chaining rate limiting and traceparent injection in a single unified advisor. |
| 07 | T3 | `ModelFailoverAdvisor` | 80 | Wraps `chain.nextCall(request)`. If downstream throws `Exception`, calls `fallbackModel.call(...)` and returns `ChatClientResponse` with `"failoverTriggered" = true` and `"primaryError"`. If normal, `"failoverTriggered" = false`. |
| 08 | T3 | `CircuitBreakerFailoverAdvisor` | 85 | Circuit breaker tracking consecutive failures. When threshold reached, trips to OPEN and routes directly to fallback model with `"circuitTripped" = true`. |
| 09 | T3 | `AdaptiveMultiModelRouterAdvisor` | 88 | 3-tier cascade: primary $\to$ secondary $\to$ offlineStub. Sets `"cascadeFailoverLevel"` (0, 1, or 2). |
| 10 | T4 | `SchemaSelfHealingAdvisor` | 70 | Validates output for JSON with `"status"`. On violation, triggers one repair retry using `chain.copy(this).nextCall(retryReq)` with a `SCHEMA_REPAIR_NOTICE`. If retry fails, throws `SchemaValidationException` (must contain `"Schema validation failed"`). Sets `"schemaHealed"`. |
| 11 | T4 | `GroundingValidationAdvisor` | 60 | Matches entity patterns (`\b(host-[a-z0-9-]+|cluster-[a-z0-9-]+)\b`). If any entity is absent from `allowedEntities`, prepends `"[UNGROUNDED_ENTITY_DETECTED] "` to response text, sets `"groundingViolation" = true` and records `"ungroundedEntities"`. Otherwise sets `"groundingViolation" = false`. |
| 12 | T4 | `FactualityStrictGroundingAdvisor` | 65 | Strict entity grounding detector redacting ungrounded facts to `[REDACTED_UNGROUNDED_FACTS]`. |
| 13 | T5 | `DynamicTokenQuotaAdvisor` | 50 | Pre-allocates estimated tokens before call. If `remainingQuota < estimated`, throws `BudgetExceededException` (must contain `"Insufficient token quota"`). Reconciles with actual tokens used on success; **rolls back pre-allocation** if call throws! Exposes `getRemainingQuota()`. |
| 14 | T5 | `createShortCircuitingClient` | — | Combines `SlidingWindowRateLimiterAdvisor` (Order 30) with `DynamicTokenQuotaAdvisor` (Order 50). Verifies that when rate limit rejects, downstream quota advisor and model are never invoked. |
| 15 | T5 | `createResilientProductionStack` | — | Factory wiring the entire production stack in proper onion order (10 -> 20 -> 30 -> 40 -> 50 -> 60 -> 80) coordinating context propagation, rate limiting, quota reservation, failover, and grounding validation. |

---

## Tiered Progressive Hints in Verifier
Every failing scenario displays clear tiered hints:
- `DETAIL`: Exact mismatch or exception message.
- `[HINT 1 (Concept)]`: The core Spring AI concept, order precedence, or invariant.
- `[HINT 2 (Edge Case)]`: Specific edge conditions, context keys, or exception types.
- `[HINT 3 (Code Pattern)]`: Structural code patterns (`chain.copy(this)`, atomic arithmetic, regex matching).

---

## Verification Commands

To run all 15 scenario gates:
```bash
mvn test-compile exec:java -pl phase04-ex02-resilient-guardrails
```

To run a single scenario (e.g. Scenario 10):
```bash
mvn test-compile exec:java -pl phase04-ex02-resilient-guardrails -Dexec.args="10"
```

To run JUnit / Surefire test:
```bash
mvn test -pl phase04-ex02-resilient-guardrails
```
