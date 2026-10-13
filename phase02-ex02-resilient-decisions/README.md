# Phase 02 Exercise 02 — Performance Diagnostic Decision Gateway & Resilient Schema Repair

**Theme:** Autonomous Performance Optimization Decision Engine & Polymorphic Schema Validation (15 Muscle-Memory Drills).  
**Diagnostician Mapping:** Directly mirrors the core guarantees of `DecisionValidator`, `DecisionDto`, `FallbackDecisionExtractor`, and `SpringAiDecideTurn` in the Saga's DECIDE phase—injecting format blueprints, handling discriminated change unions (`kind=edits` vs `kind=template`), extracting unstructured reasoning prose, and executing closed-loop one-shot repair with strict circuit-breakers.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase02/DecisionEngineUnderTest.java`](src/main/java/phase02/DecisionEngineUnderTest.java) | The service under test — **FIX THIS FILE ONLY** |
| [`src/main/java/phase02/DecisionModelContracts.java`](src/main/java/phase02/DecisionModelContracts.java) | Immutable domain records, JSON samples, and contracts (DO NOT MODIFY) |
| [`src/test/java/phase02/FakeDecisionChatModel.java`](src/test/java/phase02/FakeDecisionChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase02/Verifier.java`](src/test/java/phase02/Verifier.java) | The evaluation gate (15 Drills, exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase02-ex02-golden.md`](../docs/golden/phase02-ex02-golden.md) | **SEALED** golden reference solution |

**Time-box:** **55 minutes** for all 15 drills + write-up.

---

## 2. Reproduction Commands

Run the verifier across all drills (or isolate an individual drill):

```powershell
# Run all 15 drills:
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions

# Or via Maven test runner:
mvn test -pl phase02-ex02-resilient-decisions

# Run a specific drill (1 through 15):
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="1"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="7"
mvn test-compile exec:java -pl phase02-ex02-resilient-decisions -Dexec.args="13"
```

Exit `99` = measured FAIL (findings detected with tiered progressive hints). Read the report.  
Exit `0` = PASS.

---

## 🧠 Architectural Theory & Decision Parsing Flow

In autonomous diagnostic agent sagas, the DECIDE phase converts unstructured LLM suggestions into strictly validated, polymorphic operational decisions:

```
                                AUTONOMOUS SAGA DECISION PARSING FLOW
[Baseline Telemetry] ──► [Prompt Injection] ──► [ChatClient] ──► [Raw LLM Stream]
                           • Format Instructions                     │
                           • Polymorphic Schema                      ▼
                                                        [Conversational Text Sanitizer]
                                                        • Regex code-fence stripping
                                                        • First '{' to last '}' boundary slicing
                                                                     │
                                                                     ▼
                                                        [Polymorphic Deserializer]
                                                        • Discriminated union:
                                                          - kind="template" -> Config changes
                                                          - kind="edits"    -> Source code diffs
                                                                     │
                                    ┌────────────────────────────────┴────────────────────────────────┐
                                    ▼                                                                 ▼
                            [Valid DecisionDto]                                            [Schema/Parse Violation]
                                    │                                                                 │
                                    ▼                                                                 ▼
                        [TigerStyle Invariants]                                           [Closed-Loop Repair Turn]
                        • Non-empty rationale                                             • Send parser error to LLM
                        • Valid file paths                                                • Strict 1-retry cap
                        • Legal parameters                                                • Circuit-break if 2nd fail
```

### 🧩 Core Spring AI Classes & Roles

| Class | Package | Role & Data Flow |
|---|---|---|
| `BeanOutputConverter<T>` | `org.springframework.ai.converter` | Generates polymorphic JSON Schema directives for prompt injection and deserializes candidate JSON strings. |
| `OptimizationDecision` | Practice domain contract | Polymorphic discriminated union representing either runtime config tweaks or targeted multi-file source code diffs. |
| `DecisionValidator` | Practice domain contract | Validates operational invariants: non-blank rationale, valid file target paths, and allowed parameter boundaries. |
| `Schema Repair Turn` | LLM-in-the-loop pattern | Resilient closed-loop retry quoting the exact Jackson or schema validation error back to the LLM with an immutable 1-retry limit. |

---

## 3. The 15 Drills (5 Topics x 3 Repetitions)

### Topic 1: Schema Directives & Direct Deserialization (Drills 1–3)
- **Drill 1:** `generateFormatInstructions` — Uses `BeanOutputConverter.getFormat()` to produce JSON schema directives for prompt injection.
- **Drill 2:** `parseDecision` (Template) — Deserializes clean JSON directly into `OptimizationDecision` with template optimization.
- **Drill 3:** `parseDecision` (Edits & Boundaries) — Deserializes polymorphic source-code edits payload; verifies fail-fast on null and blank input.

### Topic 2: Markdown & Conversational Sanitization (Drills 4–6)
- **Drill 4:** `extractFromConversationalText` (Fences) — Strips markdown code blocks (```` ```json ... ``` ````).
- **Drill 5:** `extractFromConversationalText` (Preamble & Epilogue) — Extracts JSON object bounded by first `{` and last `}` from raw conversation.
- **Drill 6:** `extractFromConversationalText` (Boundaries) — Enforces fail-fast exception when payload is null, blank, or contains no JSON brackets.

### Topic 3: Cross-Field Invariants & Discriminated Unions (Drills 7–9)
- **Drill 7:** `validateCrossFieldRules` (Valid Decisions) — Clears valid template and valid edits decisions without exception.
- **Drill 8:** `validateCrossFieldRules` (Template Rules & Hypothesis) — Enforces allowed templates (`jar-unpack`, `hikari-pool`, `virtual-threads`), null/empty edits, `^H[1-7]$`, and confidence in `[0.0, 1.0]`.
- **Drill 9:** `validateCrossFieldRules` (Edits Rules & Metrics) — Enforces non-empty edits list, null template, metrics in `{p95, median, rps, error_rate}`, and direction in `{strengthen, weaken}`.

### Topic 4: Generic Collections & Fallback Prose Extraction (Drills 10–12)
- **Drill 10:** `parseDecisionBatch` (Generic List) — Uses `ParameterizedTypeReference<List<OptimizationDecision>>` to avoid type erasure.
- **Drill 11:** `parseDecisionBatch` (Boundaries) — Validates fail-fast check throwing `IllegalArgumentException` on null/blank batch payload.
- **Drill 12:** `fallbackExtractFromProse` — Uses pre-compiled regex patterns to reconstruct diagnostic decisions when model responds in prose.

### Topic 5: Closed-Loop Feedback Repair & Circuit Breakers (Drills 13–15)
- **Drill 13:** `repairDecisionWithFeedback` (Recovery) — Catches syntax/validation failure on attempt 1, re-prompts with diagnostic error message, and succeeds on attempt 2.
- **Drill 14:** `repairDecisionWithFeedback` (First-Turn Pass) — Returns `attemptsUsed == 1` immediately without feedback retry when response is valid.
- **Drill 15:** `enforceStrictRepairCircuitBreaker` (Max Retries) — Enforces circuit breaker capped at exactly 1 retry (2 attempts), throwing `IllegalStateException` without invoking model a 3rd time.
