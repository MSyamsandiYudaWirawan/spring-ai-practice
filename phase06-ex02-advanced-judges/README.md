# Phase 06 â€” Exercise 02: Advanced LLM Judges, Bias Mitigation & Trajectory Evaluation

## Mission Overview
In **Phase 06 Exercise 01**, you mastered foundational, repetitive evaluation primitives (`Evaluator`, `RelevancyEvaluator`, `FactCheckingEvaluator`, noise floors, and basic rubrics).

In **Phase 06 Exercise 02**, you tackle **repetitive and difficult** production AI evaluation challenges while remaining strictly within the scope of AI engineering:
- Pairwise A/B tournament judging with position swap mitigation.
- Self-consistency majority voting and confidence calibration.
- RAG Triad claim-level attribution and hallucination detection.
- Answer relevance and query drift gates.
- Anchor-based calibrated rubrics to eliminate LLM grade inflation.
- Adversarial safety, jailbreak, and system prompt exfiltration refusal grading.
- Saddle-Safe mechanism keep rules (the complete `KeepRule` v2 from `agentic-performance-diagnostician`).
- Multi-turn agent trajectory audits detecting redundant tool loops and cyclical ping-pongs.
- Statistical inter-judge agreement (Cohen's Kappa calibration).
- Autonomous benchmark suite orchestrator and production release gates.

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase06/AdvancedJudgeUnderTest.java`](src/main/java/phase06/AdvancedJudgeUnderTest.java).
   - Contracts in [`src/main/java/phase06/AdvancedJudgeContracts.java`](src/main/java/phase06/AdvancedJudgeContracts.java) and [`src/test/java/phase06/Verifier.java`](src/test/java/phase06/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Runs 100% offline with zero live API token costs using `FakeJudgeChatModel`.
3. **Non-Flaky Exception Contract:**
   - **Scenario 10:** [`BenchmarkGateBreachException`](src/main/java/phase06/AdvancedJudgeContracts.java) message **MUST contain** `"Release benchmark gate breach"` (case-insensitive).

---

## 10 Scenarios Breakdown

| # | Component | Key AI Mechanics |
|---|---|---|
| **01** | `PositionBiasSwapperJudge` | Pairwise A/B tournament judge: runs evaluation twice with candidate positions swapped; detects LLM first-position bias. |
| **02** | `SelfConsistencyMajorityJudge` | Stochastic voting judge: samples $N$ repeated evaluations, computes consensus confidence, and enforces majority threshold. |
| **03** | `FaithfulnessClaimAttributionEvaluator` | RAG Triad hallucination detector: breaks responses into atomic claims, verifies context attribution, and scores groundedness. |
| **04** | `AnswerRelevanceQueryDriftEvaluator` | Query intent evaluator: detects topic evasion and tangential query drift. |
| **05** | `FewShotCalibratedRubricJudge` | Calibrated rubric evaluator: injects explicit anchor examples (Levels 1, 3, 5) to eliminate LLM grade inflation across 3 dimensions. |
| **06** | `AdversarialRefusalSafetyJudge` | Jailbreak defense judge: checks if agent cleanly refused harmful prompts without leakage or preachy lectures. |
| **07** | `SaddleSafeMechanismKeepEvaluator` | Complete `KeepRule` v2 from Diagnostician: evaluates classic RPS/P95 keeps, fail rate guards, mechanism signal reductions (> 50%), and tail latency bounds. |
| **08** | `MultiTurnAgentTrajectoryEvaluator` | Agent loop auditor: evaluates multi-turn reasoning traces; penalizes redundant tool calls and cyclical loops. |
| **09** | `InterJudgeAgreementEvaluator` | Statistical calibration: calculates Cohen's Kappa ($\kappa$) between judge decisions and ground-truth labels. |
| **10** | `AutonomousBenchmarkSuiteOrchestrator` | Release benchmark gate: runs full multi-metric evaluation across dataset and enforces release gate criteria. |

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
