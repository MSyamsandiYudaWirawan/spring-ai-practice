# Phase 04 — Exercise 01: Guardrails, Advisors & Token Budget Circuit-Breakers

## Mission Overview
In **Phase 04 Exercise 01**, you master cold muscle memory building Spring AI `CallAdvisor` implementations: pre/post call interception, context enrichment, keyword security, policy injection, token usage tracking, dollar cost limits, wall-clock latency caps, PII masking, secret token redaction, onion ordering (`getOrder()`), and full production multi-guardrail stacks across **15 repetitive drills**.

---

## 15-Drill Clustered Syllabus

### Topic 1: Pre-Call Security & Policy Injections (Drills 1 - 3)
- **Scenario 01: `KeywordGuardrailAdvisor` (Pre-Call Prompt Security)**  
  Foundation: Inspect prompt messages for blocked keywords (case-insensitive); throw `PromptSecurityException` containing `"Blocked keyword"`.
- **Scenario 02: `RegexPatternGuardrailAdvisor` (Pre-Call Pattern Blocking)**  
  Variation: Multi-regex pattern matcher catching command injections and SQL injection escapes; throws `PromptSecurityException` containing `"Pattern violation"`.
- **Scenario 03: `SystemPromptEnforcingAdvisor` (Pre-Call Policy Injection)**  
  Real-world Composition: Idempotently inject compliance and auditing instructions into the prompt before dispatching to the model.

### Topic 2: Observation, Auditing & Context Propagation (Drills 4 - 6)
- **Scenario 04: `AuditLoggingAdvisor` (Pre/Post-Call Context & Audit Trail)**  
  Foundation: Generate UUID `correlationId`, mutate request context, measure elapsed wall-clock latency, and record `AdvisorAuditRecord`.
- **Scenario 05: `OrderTrackingAdvisor` (Advisor Precedence & Onion Order)**  
  Variation: Trace execution order (`PRE_CALL` and `POST_CALL`) to verify Spring AI's nested onion pipeline honoring `getOrder()`.
- **Scenario 06: `ContextEnrichmentAdvisor` (Metadata Context Propagation)**  
  Real-world Composition: Propagate tenant ID, environment, and security role into `request.context()`.

### Topic 3: Token Accumulation & Budget Circuit Breakers (Drills 7 - 9)
- **Scenario 07: `TokenBudgetAdvisor` (Post-Call Usage Accumulator)**  
  Foundation: Accumulate prompt, completion, and total tokens across calls with defensive null-safe unboxing.
- **Scenario 08: `CostCircuitBreakerAdvisor` (Post-Call Dollar Cap)**  
  Variation: Calculate exact dollar spend using `CostBudgetConfig` and trip `BudgetExceededException` (must contain `"Budget limit exceeded"`).
- **Scenario 09: `MaxTurnsAdvisor` (Conversation Session Turn Limiter)**  
  Real-world Composition: Enforce hard conversation limits per session, throwing `BudgetExceededException` (must contain `"Maximum conversation turns exceeded"`).

### Topic 4: Post-Call Sanitization & Latency Bounds (Drills 10 - 12)
- **Scenario 10: `LatencyGuardrailAdvisor` (Execution Wall-Clock Timeout)**  
  Foundation: Enforce strict execution time bounds; throw `LatencyTimeoutException` (must contain `"latency"`).
- **Scenario 11: `PiiMaskingAdvisor` (Post-Call Response Redaction)**  
  Variation: Sanitize email addresses and Social Security Numbers (SSN), replacing them with `[REDACTED_EMAIL]` and `[REDACTED_SSN]`.
- **Scenario 12: `SecretKeyRedactionAdvisor` (API Key & Token Redaction)**  
  Real-world Composition: Redact high-entropy Bearer tokens and AWS access keys from model completions with `[REDACTED_SECRET]`.

### Topic 5: Multi-Advisor Composition & Production Stacks (Drills 13 - 15)
- **Scenario 13: `ChatClientAdvisorRegistration` (Default & Call-Level Advisors)**  
  Foundation: Wire advisors at the `ChatClient.builder().defaultAdvisors(...)` level and execute with prompt-level overrides.
- **Scenario 14: `DynamicBypassGuardrailAdvisor` (Role-Based Guardrail Bypass)**  
  Variation: Conditionally bypass security guardrails when request context contains `"role": "ADMIN"`.
- **Scenario 15: `ComprehensiveSagaGuardrailSuite` (Integrated Multi-Guardrail Stack)**  
  Real-world Composition: Assemble a complete production guardrail pipeline ordering security, policy, latency, PII, and auditing seamlessly.

---

## Pedagogical Protocol & Guardrails
1. **Tiered Progressive Hints in Verifier:**
   Every scenario failure provides 3 progressive hint tiers directly in your terminal:
   - `[HINT 1 (Concept)]`: Architectural concept and data structure to use.
   - `[HINT 2 (Edge Case)]`: Specific edge case, null-safety check, or condition to handle.
   - `[HINT 3 (Code Pattern)]`: Precise code pattern or API call to implement.
2. **Explicit Exception Contracts:**
   - `PromptSecurityException`: must mention `"Blocked keyword"` or `"Pattern violation"`.
   - `BudgetExceededException`: must mention `"Budget limit exceeded"` or `"Maximum conversation turns exceeded"`.
   - `LatencyTimeoutException`: must mention `"latency"`.

---

## Verification Commands

To run all 15 scenario gates:
```bash
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors
```

To run a single scenario drill (e.g. Scenario 7):
```bash
mvn test-compile exec:java -pl phase04-ex01-guardrail-advisors -Dexec.args="7"
```

To run JUnit / Surefire test:
```bash
mvn test -pl phase04-ex01-guardrail-advisors
```
