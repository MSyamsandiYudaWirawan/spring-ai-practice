# Phase 04 - Exercise 02: Resilient Guardrails, Failover Routing, and Transactional Token Quotas

## Purpose & Pedagogical Architecture
Phase 04 Exercise 01 established the mechanics of Spring AI's `CallAdvisor` interface. Exercise 02 reinforces that exact repetitive pattern (`public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain)`) across 10 progressively challenging, real-world SRE resilience scenarios:
- Rolling window rate limiting
- Preserving pinned system prompts during conversational history pruning
- Downstream LLM failover routing
- One-shot schema self-healing via `chain.copy(this)`
- Entity hallucination / grounding validation
- W3C distributed traceparent propagation
- Transactional token quota reservation with failure rollback
- Onion model short-circuiting

---

## The 10 Scenarios & Exact Contracts

| # | Advisor / Component | Order | Core Contract & Exception Substrings |
|---|---|---|---|
| 01 | `RuntimeContextAdvisor` | 10 | Inspects `request.context()` for `"executionMode"`. If `"DRY_RUN"`, prefixes `UserMessage` with `"[DRY_RUN] "`, sets response context `"audit.dryRun" = true` and `"processedBy" = "RuntimeContextAdvisor"`. Otherwise sets `"audit.dryRun" = false`. |
| 02 | `SlidingWindowTruncationAdvisor` | 20 | Separates `SystemMessage` from history; preserves `SystemMessage` at index 0; keeps only last `maxHistoryMessages` non-system messages; records dropped count in response context `"truncatedMessageCount"`. |
| 03 | `SlidingWindowRateLimiterAdvisor` | 30 | Rolling timestamp `Deque<Long>` with eviction (`<= now - windowMs`). If `size >= maxRequests`, throws `RateLimitExceededException` (must contain `"Rate limit exceeded"`). Enriches response context with `"remainingRequests"`. |
| 04 | `ModelFailoverAdvisor` | 80 | Wraps `chain.nextCall(request)`. If downstream throws `Exception`, calls `fallbackModel.call(...)` and returns `ChatClientResponse` with `"failoverTriggered" = true` and `"primaryError"`. If normal, `"failoverTriggered" = false`. |
| 05 | `SchemaSelfHealingAdvisor` | 70 | Validates output for `"\"status\": \"SUCCESS\""`. On violation, triggers one repair retry using `chain.copy(this).nextCall(retryReq)` with a `SCHEMA_REPAIR_NOTICE`. If retry fails, throws `SchemaValidationException` (must contain `"Schema validation failed"`). Marks `"schemaHealed"` (false on 1st pass, true on healed retry). |
| 06 | `GroundingValidationAdvisor` | 60 | Matches entity patterns (`\b(host-[a-z0-9-]+|cluster-[a-z0-9-]+)\b`). If any entity is absent from `allowedEntities`, prepends `"[UNGROUNDED_ENTITY_DETECTED] "` to response text, sets `"groundingViolation" = true` and records `"ungroundedEntities"`. Otherwise sets `"groundingViolation" = false`. |
| 07 | `TraceContextPropagationAdvisor` | 40 | Checks or generates W3C `traceparent` (`00-<32hex>-<16hex>-01`). Injects `"[traceparent=" + traceparent + "] "` into `UserMessage`, preserves in response context, sets `"propagationSuccess" = true`. |
| 08 | `DynamicTokenQuotaAdvisor` | 50 | Pre-allocates estimated tokens before call. If `remainingQuota < estimated`, throws `BudgetExceededException` (must contain `"Insufficient token quota"`). Reconciles with actual tokens used on success; **rolls back pre-allocation** if call throws! Exposes `getRemainingQuota()`. |
| 09 | `createShortCircuitingClient` | â€” | Combines `SlidingWindowRateLimiterAdvisor` (Order 30) with `DynamicTokenQuotaAdvisor` (Order 50). Verifies that when rate limit rejects, downstream quota advisor and model are never invoked. |
| 10 | `createResilientProductionStack` | â€” | Factory wiring the entire production stack in proper onion order (10 -> 30 -> 40 -> 50 -> 60 -> 80) coordinating context propagation, rate limiting, quota reservation, failover, and grounding validation. |

---

## Required Error Messages & Case-Insensitive Matching
The test verifier checks all exception messages case-insensitively using `.toLowerCase().contains(...)`:
- **Scenario 3:** `RateLimitExceededException` $\to$ message must contain `"Rate limit exceeded"`.
- **Scenario 5:** `SchemaValidationException` $\to$ message must contain `"Schema validation failed"`.
- **Scenario 8:** `BudgetExceededException` $\to$ message must contain `"Insufficient token quota"`.

---

## Verification Commands

To run all 10 scenario gates:
```bash
mvn test-compile exec:java -pl phase04-ex02-resilient-guardrails
```

To run a single scenario (e.g. Scenario 5):
```bash
mvn test-compile exec:java -pl phase04-ex02-resilient-guardrails -Dexec.args="5"
```

To run JUnit / Surefire test:
```bash
mvn test -pl phase04-ex02-resilient-guardrails
```
