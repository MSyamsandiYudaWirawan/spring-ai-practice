# Phase 06 â€” Exercise 01: Deterministic Evaluators & LLM-as-a-Judge

## Mission Overview
In **Phase 06 Exercise 01**, you master the foundational evaluation interfaces and contracts of Spring AI and deterministic metric gates.
In robust agentic architectures, evaluating whether an AI recommendation improved system health or answered a query requires both:
1. **Deterministic mathematical gates** (e.g. `NoiseFloorEvaluator` and `GroundTruthAccuracyEvaluator`).
2. **Spring AI LLM-as-a-Judge evaluators** (`Evaluator`, `RelevancyEvaluator`, `FactCheckingEvaluator`, rubric-based judging, and multi-criteria scoring).

This drill provides 10 high-repetition scenarios building instinctive muscle memory on creating, adapting, and chaining evaluators.

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase06/EvaluatorUnderTest.java`](src/main/java/phase06/EvaluatorUnderTest.java).
   - Contracts in [`src/main/java/phase06/EvaluatorContracts.java`](src/main/java/phase06/EvaluatorContracts.java) and [`src/test/java/phase06/Verifier.java`](src/test/java/phase06/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Evaluator tests run 100% offline using deterministic in-memory `FakeEvaluatorChatModel` fixtures with zero live API calls.
3. **Non-Flaky Exception Contract:**
   - **Scenario 10:** [`EvaluationGateException`](src/main/java/phase06/EvaluatorContracts.java) message **MUST contain** `"Evaluation threshold breach"` (case-insensitive).

---

## 10 Scenarios Breakdown

| # | Component | Responsibility & Contract |
|---|---|---|
| **01** | `RelevancyEvaluatorAdapter` | Adapts Spring AI's built-in `RelevancyEvaluator` via `RelevancyEvaluator.builder().chatClientBuilder(...)`. Evaluates if response aligns with query and context. |
| **02** | `FactCheckingEvaluatorAdapter` | Adapts Spring AI's built-in `FactCheckingEvaluator` via `FactCheckingEvaluator.builder(chatClientBuilder).build()`. Checks if claim is supported by context documents. |
| **03** | `NoiseFloorThresholdEvaluator` | Deterministic telemetry keep gate (from Diagnostician `KeepRule` v2): verifies telemetry deltas clear noise floors (`rpsFloor`, `p95FloorMs`) and guards against `failRate` regressions. |
| **04** | `GroundTruthAccuracyEvaluator` | Deterministic root-cause convergence evaluator (from Diagnostician `EvalScorerImpl`): checks if diagnosed category matches ground truth (case-insensitive after trim). |
| **05** | `SingleMetricThresholdEvaluator` | Configurable numeric threshold gate: validates `0.0 <= threshold <= 1.0` and asserts whether incoming score meets or exceeds threshold. |
| **06** | `RubricScoreParser` | Structured LLM judge parser: extracts normalized score (e.g. `4/5` -> `0.80`, `0.95` -> `0.95`) and reasoning text from formatted LLM judge output. Pre-compiled patterns provided in class. |
| **07** | `BinaryJudgeEvaluator` | Prompt-driven LLM-as-a-judge: prompts `ChatClient` with explicit evaluation instructions and parses `PASS`/`FAIL` verdict. |
| **08** | `WeightedMultiCriteriaEvaluator` | Multi-dimension score aggregator: validates criterion weights sum to 1.0, computes weighted composite score, and evaluates against passing threshold. |
| **09** | `ShortCircuitCompositeEvaluator` | Chained evaluator composite with fast-fail: evaluates chain sequentially; immediately returns on first failure without executing remaining evaluators. |
| **10** | `BatchEvaluationRunner` | Benchmark suite runner: executes batch of evaluation requests, computes `BenchmarkSummary` (`passRate`, `averageScore`), and enforces minimum pass-rate threshold. |

---

## Verification Commands

```powershell
# Run the complete test suite:
mvn test-compile exec:java -pl phase06-ex01-evaluators

# Run a single scenario (e.g. Scenario 3):
mvn test-compile exec:java -pl phase06-ex01-evaluators "-Dexec.args=3"

# Run JUnit / Surefire test:
mvn test -pl phase06-ex01-evaluators
```
