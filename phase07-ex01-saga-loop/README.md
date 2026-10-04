# Phase 07 â€” Exercise 01: In-Memory Saga Agent Loop & State Machine

## Mission Overview
In **Phase 07 Exercise 01**, you master orchestrating the core autonomous Saga Agent Loop:
- The discrete lifecycle state machine (`DECIDE -> APPLY -> MEASURE -> JUDGE -> FINISH/ABORTED`).
- In-memory virtual code workspace with atomic commit and compensatory rollback (`revertTo`).
- Decision schema parsing with one-shot closed-loop repair.
- Compensating transactions on apply or build failures (`REVERTED`).
- Telemetry evaluation against baseline noise floors (`KEPT`).
- Runaway execution guardrails (iteration cap, token ceiling) and trajectory event logging.
- Checkpoint persistence and resume recovery.

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase07/SagaLoopUnderTest.java`](src/main/java/phase07/SagaLoopUnderTest.java).
   - Contracts in [`src/main/java/phase07/SagaContracts.java`](src/main/java/phase07/SagaContracts.java) and [`src/test/java/phase07/Verifier.java`](src/test/java/phase07/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Runs 100% offline with zero live token costs using `FakeSagaChatModel`.
3. **Non-Flaky Exception Contract:**
   - **Scenarios 6 & 7:** [`SagaLoopBreachException`](src/main/java/phase07/SagaContracts.java) message **MUST contain** `"Saga guardrail breached"` (case-insensitive).

---

## 10 Scenarios Breakdown

| # | Component | Key Responsibility |
|---|---|---|
| **01** | `StateTransitionEngine` | Enforces valid lifecycle state transitions; rejects illegal jumps with `IllegalStateException`. |
| **02** | `VirtualWorkspaceMemory` | In-memory atomic git-like tree: supports `commit(sha, files)`, `revertTo(sha)`, and file snapshot retrieval. |
| **03** | `DecideProposalExtractor` | Parses structured `DecisionProposal` from LLM output with fence-stripping and 1-shot repair retry. |
| **04** | `ApplyAndCompensateStep` | Applies changes to workspace; if compilation check fails, immediately compensates (`revertTo(lastKeptSha)`). |
| **05** | `NoiseFloorKeepGate` | Compares telemetry deltas against noise floors (`rpsFloor`, `p95FloorMs`) with fail rate guard. |
| **06** | `IterationCapCircuitBreaker` | Enforces maximum turn limits, cleanly transitioning to `ABORTED` on exhaustion. |
| **07** | `TokenBudgetGuardrail` | Accumulates token spend and triggers immediate circuit breaker on ceiling breach. |
| **08** | `TrajectoryAuditRecorder` | Thread-safe audit log recording state transitions, actions, and token usages. |
| **09** | `CheckpointRecoveryManager` | Persists snapshot of `lastKeptSha` and telemetry; cleanly restores dirty workspace on resume. |
| **10** | `AutonomousSagaLoopRunner` | Coordinates end-to-end multi-turn Saga loop across all phases and produces `LoopSummary`. |

---

## Verification Commands

```powershell
# Run the complete test suite:
mvn test-compile exec:java -pl phase07-ex01-saga-loop

# Run a single scenario (e.g. Scenario 4):
mvn test-compile exec:java -pl phase07-ex01-saga-loop "-Dexec.args=4"

# Run JUnit / Surefire test:
mvn test -pl phase07-ex01-saga-loop
```
