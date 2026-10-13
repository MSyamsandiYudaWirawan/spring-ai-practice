# Phase 02 Exercise 01 — Structured Outputs & Resilient Schema Extraction

**Theme:** BeanOutputConverter, Markdown Fence Sanitization, Generic ParameterizedTypeReference, Type-Safe Entity Extraction, TigerStyle Validation, and One-Shot Feedback Schema Repair Loops (**15 Muscle-Memory Drills**).  
**Architectural Principle:** Enforces: *"AI proposes, pipeline disposes. Model emits text; schema guarantees validity."*

---

## 🧠 Architectural Theory & Structured Output Flow

LLMs do not produce Java objects; they emit text streams. To build resilient production systems, raw text must pass through sanitization, schema conversion, invariant gating, and closed-loop repair:

```
                              STRUCTURED OUTPUT EXTRACTION PIPELINE
[Prompt / Advisory] ──► [ChatClient.call()] ──► [Raw LLM Output] (Markdown code fences, chatter)
                                                       │
                                                       ▼
                                         [Defensive JSON Sanitizer]
                                         • Strips ```json ... ``` fences
                                         • Slices first '{' / '[' to last '}' / ']'
                                                       │
                                                       ▼
                                      [BeanOutputConverter<T> Deserializer]
                                      • Single Record / POJO
                                      • Generic List<T> / Map<K,V> (ParameterizedTypeReference)
                                                       │
                           ┌───────────────────────────┴───────────────────────────┐
                           ▼                                                       ▼
                   [Valid Entity DTO]                                     [Malformed / Bad Schema]
                           │                                                       │
                           ▼                                                       ▼
               [TigerStyle Invariant Gate]                               [One-Shot Repair Protocol]
               • Regex / Ranges / Required Fields                        • Quotes error back to LLM
                           │                                             • Exactly 1 retry attempt
                           ▼                                                       │
                   [TriageEnvelope.ok(data)]                             [TriageEnvelope.rejected(err)]
```

### 🧩 Core Spring AI Classes & Roles

| Class | Package | Role & Data Flow |
|---|---|---|
| `BeanOutputConverter<T>` | `org.springframework.ai.converter` | Generates JSON Schema format instructions (`.getFormat()`) and deserializes JSON strings into typed Java instances via `.convert()`. |
| `ParameterizedTypeReference<T>` | `org.springframework.core` | Captures generic type parameters at runtime (e.g. `List<PatchPlan>`) to prevent JVM type erasure during JSON array deserialization. |
| `CallResponseSpec.entity(...)` | `org.springframework.ai.chat.client` | Fluent shortcut that attaches schema format instructions to the request and automatically converts the completion to type `T`. |
| `TriageEnvelope<T>` | Practice domain contract | Typed wrapper isolating deserialization failures (`ok` vs `rejected`), preventing unhandled runtime exceptions from crashing agent loops. |

---

## The 15 Drills & Exact Contracts

| # | Topic | Drill / Component | Description & Invariants |
|---|---|---|---|
| 01 | T1 | `parseVulnerabilityAssessment(rawJson)` | Direct JSON record deserialization via `BeanOutputConverter<VulnerabilityAssessment>`. Boundary checks: null or blank throws `IllegalArgumentException`. |
| 02 | T1 | `extractFromMarkdownFences(markdownPayload)` | Defensive markdown fence extraction (` ```json ... ``` `) and conversational preamble stripping (first `{` to last `}`) before deserialization. |
| 03 | T1 | `parsePatchPlan(rawJson)` | Single record deserialization for `PatchPlan` via `BeanOutputConverter<PatchPlan>` with validation. |
| 04 | T2 | `extractPatchPlanList(jsonArrayPayload)` | Generic list extraction via `BeanOutputConverter<>(new ParameterizedTypeReference<List<PatchPlan>>() {})`. |
| 05 | T2 | `extractVulnerabilityList(jsonArrayPayload)` | Generic list extraction for `List<VulnerabilityAssessment>` via `ParameterizedTypeReference`. |
| 06 | T2 | `extractSeverityMap(jsonMapPayload)` | Generic map extraction `Map<String, Severity>` via `ParameterizedTypeReference`. |
| 07 | T3 | `generateAssessmentWithEntity(client, advisoryText)` | ChatClient prompt execution via `.entity(VulnerabilityAssessment.class)`. Validates client/advisory non-null/non-blank. |
| 08 | T3 | `generatePatchPlansWithEntity(client, advisoryText)` | ChatClient prompt execution via `.entity(new ParameterizedTypeReference<List<PatchPlan>>() {})`. |
| 09 | T3 | `generateCustomEntity(client, advisoryText, Class<T> clazz)` | Parameterized ChatClient extraction dispatching to target record class. |
| 10 | T4 | `validateRecordInvariants(assessment)` | TigerStyle validation verifying CVE regex, CVSS range `[0.0, 10.0]`, package coordinate format, non-blank root cause, and non-empty mitigation steps. |
| 11 | T4 | `safeDeserializeWithFallback(jsonPayload)` | Wraps deserialization in `TriageEnvelope<VulnerabilityAssessment>`. Catches exceptions without crashing, returning `TriageEnvelope.rejected(error)`. |
| 12 | T4 | `safeBatchDeserializeWithFallback(jsonList)` | Batch deserialization returning `List<TriageEnvelope<VulnerabilityAssessment>>`, isolating malformed entries while accepting valid ones. |
| 13 | T5 | `repairWithOneShotRetry(client, cveIdentifier)` | When attempt 1 produces malformed output or schema violations, executes a feedback prompt ("Previous output failed schema validation...") and repairs output on attempt 2. |
| 14 | T5 | `enforceRetryLimit(client, cveIdentifier)` | When both attempt 1 and retry attempt 2 fail, strictly cuts off without executing a 3rd attempt, throwing `IllegalStateException("Schema repair exceeded maximum attempts (1)")`. |
| 15 | T5 | `autonomousResilientTriagePipeline(client, cveIdentifier)` | Complete production pipeline: sanitize fences $\to$ parse entity $\to$ validate invariants $\to$ repair if malformed $\to$ wrap in `TriageEnvelope`. |

---

## Tiered Progressive Hints in Verifier
Every failing scenario displays clear tiered hints:
- `DETAIL`: Exact mismatch or exception message.
- `[HINT 1 (Concept)]`: The core Spring AI structured output concept.
- `[HINT 2 (Edge Case)]`: Specific boundary checks, input normalization, or collection types.
- `[HINT 3 (Code Pattern)]`: Method calls (`.entity()`, `ParameterizedTypeReference`, fence cleanup logic).

---

## Verification Commands

To run all 15 scenario gates:
```powershell
mvn test-compile exec:java -pl phase02-ex01-structured-outputs
```

To run a single scenario (e.g. Scenario 10):
```powershell
mvn test-compile exec:java -pl phase02-ex01-structured-outputs "-Dexec.args=10"
```

To run via Maven test runner:
```powershell
mvn test -pl phase02-ex01-structured-outputs
```
