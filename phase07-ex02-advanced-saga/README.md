# Phase 07 â€” Exercise 02: Advanced Saga Agent Resilience & HITL Seams

## Mission Overview
In **Phase 07 Exercise 02**, you take Saga Agent Loop orchestration to production-grade resilience:
- Speculative multi-branch execution and isolated sandbox merging.
- 4-stage diagnostic failure triage hierarchy.
- Guaranteed `finally`-block compensatory rollback on crashes.
- Complete `KeepRule` v2 saddle-safe mechanism reduction.
- Cyclical ping-pong detection and infinite loop trapping.
- Adaptive failure feedback injection into prompt context.
- Dynamic phase-partitioned token budgeting.
- Split-brain checkpoint corruption recovery.
- Risk-based Human-in-the-loop (HITL) approval pause seams.
- Resilient autonomous chaos lifecycle orchestration.

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase07/AdvancedSagaUnderTest.java`](src/main/java/phase07/AdvancedSagaUnderTest.java).
   - Contracts in [`src/main/java/phase07/AdvancedSagaContracts.java`](src/main/java/phase07/AdvancedSagaContracts.java) and [`src/test/java/phase07/Verifier.java`](src/test/java/phase07/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Runs 100% offline with zero live token costs using `FakeAdvancedSagaChatModel`.
3. **Non-Flaky Exception Contract:**
   - **Scenarios 5 & 7:** [`AdvancedSagaBreachException`](src/main/java/phase07/AdvancedSagaContracts.java) message **MUST contain** `"Advanced Saga breach"` (case-insensitive).

---

## 10 Scenarios Breakdown

| # | Component | Key AI Mechanics |
|---|---|---|
| **01** | `SpeculativeBranchManager` | Forks isolated branches for candidate proposals, measures both, merges the winner, and rolls back the loser. |
| **02** | `MultiStageDiagnosticTriageEngine` | 4-stage failure escalation: Tool retry -> Schema repair -> Compensation revert -> Guardrail termination. |
| **03** | `GuaranteedFinallyCompensationEngine` | Wraps risky executions in `try-finally` blocks to guarantee zero dirty unjudged workspace states on crash. |
| **04** | `SaddleSafeMechanismKeepEngine` | Complete `KeepRule` v2: requires >50% signal reduction, fail rate guard, and tail latency bounding. |
| **05** | `CyclicalPingPongDetector` | Detects repeated or inverted proposals across turns and aborts with `AdvancedSagaBreachException`. |
| **06** | `AdaptiveFailureFeedbackEnricher` | Extracts historical failure traces and truncates them into enriched prompt context for subsequent turns. |
| **07** | `DynamicPhaseTokenBudgeter` | Enforces partitioned token budget ceilings across Analysis (30%), Synthesis (50%), and Verification (20%). |
| **08** | `SplitBrainCheckpointValidator` | Detects divergent checkpoint SHAs missing from commit history and recovers by rolling back to root SHA. |
| **09** | `HumanInTheLoopApprovalGate` | Pauses state machine for human operator review when proposal risk score exceeds threshold. |
| **10** | `ResilientAutonomousChaosSagaOrchestrator` | Coordinates full 4-turn autonomous agent lifecycle under adverse conditions, terminating with `FINISH`. |

---

## Verification Commands

```powershell
# Run the complete test suite:
mvn test-compile exec:java -pl phase07-ex02-advanced-saga

# Run a single scenario (e.g. Scenario 4):
mvn test-compile exec:java -pl phase07-ex02-advanced-saga "-Dexec.args=4"

# Run JUnit / Surefire test:
mvn test -pl phase07-ex02-advanced-saga
```
