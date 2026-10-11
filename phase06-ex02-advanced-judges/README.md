# Phase 06 — Exercise 02: Advanced LLM Judges, Bias Mitigation & Trajectory Evaluation

## Mission Overview
In **Phase 06 Exercise 01**, you mastered foundational evaluation primitives (`Evaluator`, `RelevancyEvaluator`, `FactCheckingEvaluator`, noise floors, and basic rubrics).

In **Phase 06 Exercise 02**, you tackle **repetitive production AI evaluation drills** structured into 5 core framework topics with 3 back-to-back repetitions each (Foundation → Variation → Real-World Composition), totaling 15 scenarios:
1. **Topic 1: Tournament & Voting Judges (Bias Mitigation)**: Pairwise A/B tournament judge with position swap mitigation, 3-way round-robin ranking leaderboard, and self-consistency majority voting with confidence calibration.
2. **Topic 2: RAG Triad Grounding & Attribution**: Atomic claim extraction & faithfulness scoring, claim-to-document citation mapping & attribution verification, and answer relevance & query drift / topic evasion detection.
3. **Topic 3: Calibrated Rubrics & Safety Gates**: Anchor-based calibrated rubric judge (few-shot Level 1/3/5 descriptors), adversarial refusal & jailbreak defense judge, and system prompt leakage & sensitive data exfiltration audit.
4. **Topic 4: Agent Trajectory & Reasoning Audits**: Multi-turn trajectory step efficiency & duplicate tool call detection, ping-pong cyclical loop & tool call recursion detection, and goal completion & trajectory convergence gate.
5. **Topic 5: Inter-Judge Calibration & Production Release Gates**: Statistical inter-judge agreement (Cohen's Kappa $\kappa$), multi-dimensional benchmark suite aggregator (`ReleaseBenchmarkReport`), and production release benchmark gate enforcer (`BenchmarkGateBreachException`).

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase06/AdvancedJudgeUnderTest.java`](src/main/java/phase06/AdvancedJudgeUnderTest.java).
   - Contracts in [`src/main/java/phase06/AdvancedJudgeContracts.java`](src/main/java/phase06/AdvancedJudgeContracts.java) and [`src/test/java/phase06/Verifier.java`](src/test/java/phase06/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Runs 100% offline with zero live API token costs using `FakeJudgeChatModel`.
3. **Non-Flaky Exception Contract:**
   - **Scenario 15:** [`BenchmarkGateBreachException`](src/main/java/phase06/AdvancedJudgeContracts.java) message **MUST contain** `"Release benchmark gate breach"` (case-insensitive).

---

## 15 Scenarios Breakdown (5 Topics × 3 Repetitions)

| Topic | # | Component / Method | Repetition Focus |
|---|---|---|---|
| **Topic 1: Tournament & Voting** | **01** | `judgePairwise` | **Foundation**: Pairwise A/B tournament judge with position swap mitigation; detects first-position bias. |
| | **02** | `rankRoundRobin` | **Variation**: 3-Way tournament round-robin matches; aggregates win rates and returns ranked leaderboard. |
| | **03** | `evaluateConsensus` | **Composition**: Self-consistency majority voting across $N$ stochastic runs; computes confidence and average score. |
| **Topic 2: RAG Triad Grounding** | **04** | `evaluateFaithfulness` | **Foundation**: Atomic claim extraction and entailment verification against retrieved context docs. |
| | **05** | `evaluateAttribution` | **Variation**: Claim-to-document citation mapping; identifies specific context doc index grounded per claim. |
| | **06** | `evaluateRelevance` | **Composition**: Direct query relevance evaluation and topic drift / evasive tangent detection. |
| **Topic 3: Rubrics & Safety** | **07** | `evaluateWithAnchors` | **Foundation**: Calibrated multi-criteria rubric judge with few-shot anchor descriptors (Levels 1, 3, 5). |
| | **08** | `evaluateRefusal` | **Variation**: Adversarial jailbreak and prompt injection defense; evaluates safe refusal vs preachiness. |
| | **09** | `auditExfiltration` | **Composition**: System prompt leakage and confidential token exfiltration audit. |
| **Topic 4: Trajectory Audits** | **10** | `evaluateTrajectory` | **Foundation**: Multi-turn agent trajectory step efficiency; penalizes duplicate tool calls and step overruns. |
| | **11** | `auditTrajectoryLoops` | **Variation**: Ping-pong cyclical recursion and tool call loop detection ($A \to B \to A \to B$). |
| | **12** | `evaluateConvergence` | **Composition**: Comprehensive goal completion and trajectory convergence gate within step budget. |
| **Topic 5: Calibration & Release** | **13** | `calculateCohenKappa` | **Foundation**: Statistical inter-judge agreement ($\kappa$) between raters or model vs ground truth. |
| | **14** | `aggregateBenchmark` | **Variation**: Multi-dimensional benchmark dataset aggregator across faithfulness, relevance, accuracy, efficiency. |
| | **15** | `runReleaseBenchmark` | **Composition**: Production release benchmark gate enforcer; raises `BenchmarkGateBreachException` on violation. |

---

## Verification Commands

```powershell
# Run the complete test suite:
mvn test-compile exec:java -pl phase06-ex02-advanced-judges

# Run a single scenario (e.g. Scenario 7):
mvn test-compile exec:java -pl phase06-ex02-advanced-judges "-Dexec.args=7"

# Run JUnit / Surefire test:
mvn test -pl phase06-ex02-advanced-judges
```
