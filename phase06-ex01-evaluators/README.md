# Phase 06 — Exercise 01: Deterministic Evaluators & Benchmark Gates

## Focus & Learning Objectives
In **Phase 06 Exercise 01**, you master the foundational evaluation interfaces and contracts of Spring AI and deterministic metric gates:
- **Spring AI Evaluator Primitives:** Building `EvaluationRequest`, `EvaluationResponse`, and integrating Spring AI's built-in `RelevancyEvaluator`.
- **Deterministic Metric & Accuracy Evaluators:** Exact string ground truth matching, categorical set membership, and bounded score windows.
- **LLM Judge Rubric Parsing:** Parsing numeric scores, binary verdicts with reasoning, and structured JSON outputs with markdown fence stripping.
- **Composite Evaluators:** Aggregating weighted multi-criteria, fail-fast short-circuit chains, and all-must-pass consensus rules.
- **Benchmark Suite Execution & Quality Gates:** Running dataset batches, aggregating multi-metric summaries, and enforcing quality gate thresholds (`EvaluationGateException`).

---

## Repetitive Muscle Memory Structure (15 Scenarios)

### Topic 1: Spring AI Evaluator Interface & Request/Response Basics (Scenarios 1 – 3)
1. **Scenario 01: Direct EvaluationRequest & Threshold Evaluation** — Construct basic `EvaluationRequest` and gate score against threshold.
2. **Scenario 02: Context-Enriched EvaluationRequest (RAG Triad Triplet)** — Construct `EvaluationRequest` with `List<Document>` context and record metadata.
3. **Scenario 03: Spring AI Built-in Evaluator Adapter** — Adapt built-in `RelevancyEvaluator` with exception fallback guards.

### Topic 2: Deterministic Metric & Accuracy Evaluators (Scenarios 4 – 6)
4. **Scenario 04: Exact Match Ground Truth Evaluator** — Case-insensitive exact comparison against expected labels.
5. **Scenario 05: Categorical Convergence Evaluator** — Set membership validation against allowed taxonomy categories.
6. **Scenario 06: Bounded Numeric Score Evaluator** — Numeric window verification ensuring scores fall within [min, max].

### Topic 3: LLM Rubric Parsing & Score Extraction (Scenarios 7 – 9)
7. **Scenario 07: Single Numeric Rubric Score Parser** — Regex parsing for fraction, decimal, and integer scores with reason text.
8. **Scenario 08: Multi-Field Verdict & Reasoning Rubric Parser** — Extracting `PASS`/`FAIL` verdict and reasoning fields.
9. **Scenario 09: Structured JSON Rubric Parser** — Stripping markdown code fences and extracting JSON rubric maps.

### Topic 4: Composite & Chained Evaluators (Scenarios 10 – 12)
10. **Scenario 10: Weighted Multi-Criteria Evaluator** — Weighted linear combination of criterion scores with sum-to-1.0 validation.
11. **Scenario 11: Fail-Fast Short-Circuit Composite Evaluator** — Sequential chain execution that immediately returns on first failure.
12. **Scenario 12: AllMustPass Composite Evaluator** — Strict consensus requiring 100% pass across all evaluators with aggregated feedback.

### Topic 5: Benchmark Runner & Suite Aggregation (Scenarios 13 – 15)
13. **Scenario 13: Simple Batch Evaluation Runner** — Executing a single evaluator across request datasets and computing pass rate / mean score.
14. **Scenario 14: Multi-Metric Benchmark Suite Runner** — Running multiple evaluators concurrently across request datasets.
15. **Scenario 15: Quality Gate Threshold Enforcer** — Gating releases against minimum pass-rate and average-score thresholds.

---

## Verification Commands

```powershell
mvn test -pl phase06-ex01-evaluators
```

A verified reference implementation is available in `docs/golden/phase06-ex01-golden.md`.
