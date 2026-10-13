# Phase 01 Exercise 03 — Cloud SRE & Kubernetes Remediation Gateway

**Theme:** Cloud SRE & Kubernetes Operations Incident Gateway (15 Muscle-Memory Drills).  
**Core Competency:** Complete TigerStyle defensive programming and Spring AI `ChatClient` muscle memory: fail-fast boundary validation, role separation, template binding with complex types, conditional environment policy injection, defensive token usage unboxing, parameterized client mutation, fine-grained LLM runtime options, few-shot demonstration priming, multi-turn history preservation, system paging envelopes, and structural LLM output invariant enforcement.

---

## 1. Setup & Files

| File | Role |
|---|---|
| [`src/main/java/phase01/SreOpsServiceUnderTest.java`](src/main/java/phase01/SreOpsServiceUnderTest.java) | The service under test — **FIX THIS FILE ONLY** |
| [`src/main/java/phase01/SreModelContracts.java`](src/main/java/phase01/SreModelContracts.java) | Immutable domain records, templates, policies, and contracts (DO NOT MODIFY) |
| [`src/test/java/phase01/FakeChatModel.java`](src/test/java/phase01/FakeChatModel.java) | Deterministic in-memory `ChatModel` mock (DO NOT MODIFY) |
| [`src/test/java/phase01/Verifier.java`](src/test/java/phase01/Verifier.java) | The evaluation gate (15 Drills, exits 99 on FAIL, 0 on PASS) |
| [`docs/golden/phase01-ex03-golden.md`](../docs/golden/phase01-ex03-golden.md) | **SEALED** golden reference solution |

**Time-box:** **60 minutes** for all 15 drills + write-up.

---

## 2. Reproduction Commands

Run the verifier across all drills (or isolate an individual drill):

```powershell
# Run all 15 drills:
mvn test-compile exec:java -pl phase01-ex03-cloud-sre

# Or via Maven test runner:
mvn test -pl phase01-ex03-cloud-sre

# Run a specific drill (1 through 15):
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="1"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="7"
mvn test-compile exec:java -pl phase01-ex03-cloud-sre -Dexec.args="13"
```

Exit `99` = measured FAIL (findings detected with tiered progressive hints). Read the report.  
Exit `0` = PASS.

---

## 🧠 Architectural Theory & Incident Triage Flow

In mission-critical Cloud SRE operations, LLM prompts must enforce strict role boundaries, fail-fast boundary invariants, and token quota telemetry:

```
                                CLOUD SRE INCIDENT TRIAGE LIFECYCLE
[Pod Crash Event] ──► [SRE Invariant Checks] ──► [System & User Envelopes] ──► [ChatClient]
 (Cluster, Pod, Logs)  (Non-blank namespace)     • SRE System Persona           (Options, Quotas)
                                                 • Paging Dispatch Metadata             │
                                                 • Crash Dump Log Context               ▼
                                                                                   [ChatResponse]
                                                                                        │
                                        ┌───────────────────────────────────────────────┴────────┐
                                        ▼                                                        ▼
                              [Incident Root Cause]                                    [Telemetry Accounting]
                              `response.getResult()`                                 `response.getMetadata()`
                               • Diagnostic Summary                                   • Token Usage Check
                               • Mitigation Actions                                   • Budget Verification
```

### 🧩 Core Spring AI Classes & Roles

| Class | Package | Role & Data Flow |
|---|---|---|
| `ChatClient` | `org.springframework.ai.chat.client` | Executes SRE root-cause triage requests with configured cluster personas and timeout policies. |
| `PromptSystemSpec` | `org.springframework.ai.chat.client` | Configures system role envelopes with on-call paging metadata and cluster security rules. |
| `PromptUserSpec` | `org.springframework.ai.chat.client` | Parameterizes crash context: binds pod names, namespaces, exit codes, and raw container stderr. |
| `ChatOptions` / `Builder` | `org.springframework.ai.chat.prompt` | Locks temperature to near-zero ($0.1 - 0.2$) to ensure deterministic root-cause recommendations. |
| `Usage` | `org.springframework.ai.chat.metadata` | Verifies and accumulates token expenditure to prevent runaway automated triage loops. |

---

## 3. The 15 Drills (5 Topics x 3 Repetitions)

### Topic 1: Role Separation, Cluster Invariants & Paging Envelopes (Drills 1–3)
- **Drill 1:** `analyzePodCrash` (Role Separation) — Semantic role separation with cluster/namespace in SystemMessage and logs in UserMessage.
- **Drill 2:** `analyzePodCrash` (Boundaries) — Fail-fast boundary checks on null/blank clusterId, namespace, or podLogs.
- **Drill 3:** `applyIncidentPagingEnvelope` (Paging Envelope) — System envelope augmentation with on-call dispatcher info and base SRE prompt.

### Topic 2: Parameterized Templates & Conditional Environment Policies (Drills 4–6)
- **Drill 4:** `renderResourceExhaustionAlert` (Map Binding) — Parameter map binding with 5 numeric and collection parameters.
- **Drill 5:** `renderResourceExhaustionAlert` (Boundaries) — Fail-fast validation verifying null alert throws exception.
- **Drill 6:** `triageWithEnvironmentPolicy` (Policy Injection) — Conditional policy branching (`PROD_POLICY` vs `NON_PROD_POLICY`).

### Topic 3: Fine-Grained Options & Deterministic Runbooks (Drills 7–9)
- **Drill 7:** `generateDeterministicRunbook` (Options Attachment) — Call-level options override: temperature 0.0, maxTokens 300, stopSequences, and frequencyPenalty 0.5.
- **Drill 8:** `generateDeterministicRunbook` (Base Immutability) — Verifies base client defaults (temperature 0.4) remain intact on subsequent turns.
- **Drill 9:** `generateDeterministicRunbook` (Boundaries) — Fail-fast boundary validation on null/blank alertName.

### Topic 4: Conversational Troubleshooting & Few-Shot Remediations (Drills 10–12)
- **Drill 10:** `continueTroubleshootingSession` (History Replay) — Injects conversational history via `.messages(history).user(userReply)`.
- **Drill 11:** `continueTroubleshootingSession` (Boundaries) — Fail-fast boundary checks on null history or null/blank userReply.
- **Drill 12:** `classifyRemediationAction` (Few-Shot Priming) — Prepends alternating few-shot pairs from `REMEDIATION_FEW_SHOTS`.

### Topic 5: Client Mutation, Token Auditing & Structural Invariants (Drills 13–15)
- **Drill 13:** `deriveCanaryAnalysisClient` (Canary Mutation) — Derives canary client via `chatClient.mutate()` with parameterized traffic percent and temperature 0.1.
- **Drill 14:** `executeIncidentDiagnosisWithAudit` (Usage Metadata) — Extracts prompt and completion tokens defensively from `response.getMetadata().getUsage()`.
- **Drill 15:** `safeExecuteDiagnosis` (Structural Response Validation) — Fail-fast structural invariant checking verifying non-empty, non-malformed response output.
