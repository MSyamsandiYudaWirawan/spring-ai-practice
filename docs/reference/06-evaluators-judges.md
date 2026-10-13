# Spring AI 2.0.1 — Evaluators & LLM-as-a-Judge Architecture

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 06 (`phase06-evaluators-benchmarks`) & Model Verification

---

Evaluating LLM applications requires a dual-track strategy:
1. **Deterministic Mathematical Gates:** Evaluating hard metrics (latency, RPS, error rates, token usage) against noise floor baselines.
2. **Qualitative & Semantic Evaluators:** Evaluating relevance, factual groundedness, query drift, safety, and rubric alignment using Spring AI's `Evaluator` interfaces and LLM-as-a-judge techniques.

---

## 1. Maven Coordinates & Module Breakdown

Evaluation types in Spring AI 2.0.1 are split across two core jars:

```xml
<!-- Core evaluation contracts: Evaluator, EvaluationRequest, EvaluationResponse -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-commons</artifactId>
</dependency>

<!-- Chat-specific evaluators: RelevancyEvaluator, FactCheckingEvaluator -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-client-chat</artifactId>
</dependency>
```

---

## 2. Core Evaluation Interfaces & Data Contracts

### 2.1 The `Evaluator` Interface
Located in package `org.springframework.ai.evaluation`:

```java
public interface Evaluator {
    EvaluationResponse evaluate(EvaluationRequest evaluationRequest);

    default String doGetSupportingData(EvaluationRequest evaluationRequest) {
        // Concatenates documents in evaluationRequest.getDataList()
    }
}
```

### 2.2 `EvaluationRequest` Contract
```java
package org.springframework.ai.evaluation;

public class EvaluationRequest {
    public EvaluationRequest(String userText, String responseContent);
    public EvaluationRequest(List<Document> dataList, String responseContent);
    public EvaluationRequest(String userText, List<Document> dataList, String responseContent);

    public String getUserText();
    public List<Document> getDataList();
    public String getResponseContent();
}
```

> [!IMPORTANT]
> `EvaluationRequest` exposes **only** `.getUserText()`, `.getDataList()`, and `.getResponseContent()`. It does **not** have a `getMetadata()` method. Pass reference context or retrieved chunks via `dataList` (`List<Document>`).

### 2.3 `EvaluationResponse` Contract
```java
package org.springframework.ai.evaluation;

public class EvaluationResponse {
    public EvaluationResponse(boolean pass, float score, String feedback, Map<String, Object> metadata);
    public EvaluationResponse(boolean pass, String feedback, Map<String, Object> metadata);

    public boolean isPass();
    public float getScore();
    public String getFeedback();
    public Map<String, Object> getMetadata();
}
```

---

## 3. Built-In Spring AI Evaluators

### 3.1 `RelevancyEvaluator`
Located in `org.springframework.ai.chat.evaluation.RelevancyEvaluator`. Evaluates whether the generated response directly aligns with the query and provided context.

```java
// Creation via Builder:
RelevancyEvaluator evaluator = RelevancyEvaluator.builder()
        .chatClientBuilder(chatClientBuilder)
        .build();

// Execution:
EvaluationRequest request = new EvaluationRequest(query, contextDocs, response);
EvaluationResponse response = evaluator.evaluate(request);

boolean passed = response.isPass(); // true if model answers "YES"
float score = response.getScore();  // 1.0f on YES, 0.0f on NO
```

**Internal Framework Prompt Template:**
```text
Your task is to evaluate if the response for the query
is in line with the context information provided.

You have two options to answer. Either YES or NO.

Answer YES, if the response for the query
is in line with context information otherwise NO.

Query:
{query}

Response:
{response}

Context:
{context}

Answer:
```

**Parsing Rule:** Spring AI strips the output and checks `.equalsIgnoreCase("yes")`. If true, `isPass = true, score = 1.0f`; otherwise `isPass = false, score = 0.0f`.

---

### 3.2 `FactCheckingEvaluator`
Located in `org.springframework.ai.chat.evaluation.FactCheckingEvaluator`. Evaluates whether a claim (in response) is factually supported by context documents.

```java
// Creation via Builder:
FactCheckingEvaluator evaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();

// Execution:
EvaluationRequest request = new EvaluationRequest(contextDocs, claimText);
EvaluationResponse response = evaluator.evaluate(request);
```

**Internal Framework Prompt Template:**
```text
Evaluate whether or not the following claim is supported by the provided document.
Respond with "yes" if the claim is supported, or "no" if it is not.

Document:
{document}

Claim:
{claim}
```

**Parsing Rule:** Checks `output.strip().equalsIgnoreCase("yes")` to determine pass/fail.

---

## 4. Deterministic Telemetry Gating (`KeepRule` v2)

In autonomous agentic architectures, qualitative LLM evaluation must be paired with mathematical threshold gates to prevent accepting illusory improvements caused by telemetry noise:

```java
public class NoiseFloorEvaluator {
    public static KeepDecision evaluate(LoadReportDto ref, LoadReportDto result, NoiseFloors floors) {
        double p95Improve = ref.latency().p95() - result.latency().p95();
        double rpsImprove = result.rps() - ref.rps();

        boolean rpsImproved = rpsImprove > floors.rpsFloor();
        boolean p95Improved = p95Improve > floors.p95FloorMs();
        boolean failGuard = result.failRate() <= ref.failRate();

        // 1. Classic keep: metric improved beyond noise floor AND error rate did not worsen
        if ((rpsImproved || p95Improved) && failGuard) {
            String keepType = rpsImproved ? "RPS" : "P95";
            return new KeepDecision(true, keepType, "Classic keep: " + keepType + " cleared floor");
        }

        // 2. Regression guard: improved but fail rate worsened -> REJECT
        if ((rpsImproved || p95Improved) && !failGuard) {
            return new KeepDecision(false, null, "improved beyond floor but failRate worsened");
        }

        return new KeepDecision(false, null, "Deltas within noise floor");
    }
}
```

---

## 5. Advanced LLM-as-a-Judge Engineering & Bias Mitigation

### 5.1 Position Bias Mitigation (Pairwise A/B Tournaments)
LLMs exhibit severe positional bias: when given two candidate answers, they prefer the first candidate ("Option 1") in >70% of evaluations regardless of quality.

**The Mitigation Protocol:**
1. **Run 1:** Present `(Candidate A, Candidate B)`. Judge picks winner.
2. **Run 2:** Swap presentation order to `(Candidate B, Candidate A)`. Judge picks winner.
3. **Verdict Matrix:**
   - Run 1 chose Option 1 (A) and Run 2 chose Option 2 (A) $\rightarrow$ **Winner: Candidate A** (unbiased).
   - Run 1 chose Option 2 (B) and Run 2 chose Option 1 (B) $\rightarrow$ **Winner: Candidate B** (unbiased).
   - Run 1 and Run 2 chose the *same option position* (e.g. Option 1 in both runs) $\rightarrow$ **Position Bias Detected**, outcome is marked `INCONCLUSIVE_OR_TIE`.

---

### 5.2 Self-Consistency Majority Voting (Confidence Sampling)
Single judge calls on borderline decisions are noisy. Sampling $N$ inferences (e.g. $N=3$ or $N=5$) with temperature > 0 creates a voting ensemble:
- **Consensus Rule:** $\text{Majority Pass} = \text{passVotes} > \frac{N}{2}$.
- **Confidence Metric:** $\text{Confidence} = \frac{\max(\text{passVotes}, \text{failVotes})}{N}$.
- If $\text{Confidence} < \text{threshold}$ (e.g. 0.70 for 3-2 split), reject the decision as inconclusive.

---

### 5.3 RAG Triad Claim-Level Attribution (Hallucination Detection)
Evaluating whole paragraphs allows subtle hallucinations to slip through. Decompose responses into atomic factual claims:
1. Extract atomic claims from the candidate response.
2. Verify each claim against context documents individually.
3. Compute Groundedness / Faithfulness:
   $$\text{Faithfulness Score} = \frac{\text{Supported Claims}}{\text{Total Claims}}$$
4. Isolate unsupported claims for regression tracking.

---

### 5.4 Anchor-Based Calibrated Rubrics (Grade Inflation Mitigation)
Zero-shot LLM judges inflate scores (awarding 4/5 or 5/5 to mediocre answers). Calibrate scoring by embedding explicit few-shot anchors in the prompt:
- **Anchor 1 (Level 1 - Poor):** Severe hallucination, missing query intent, or unsafe code.
- **Anchor 3 (Level 3 - Adequate):** Factually correct but incomplete, verbose, or lacking edge-case handling.
- **Anchor 5 (Level 5 - Exemplary):** Grounded, concise, handles edge cases, zero hallucinations.

Evaluate across separate dimensions (`Correctness`, `Completeness`, `Conciseness`) and normalize to $[0.0, 1.0]$.

---

### 5.5 Statistical Inter-Judge Agreement (Cohen's Kappa)
To calibrate an LLM judge against human annotations or ground-truth datasets, compute **Cohen's Kappa ($\kappa$)**:
$$\kappa = \frac{P_o - P_e}{1 - P_e}$$
- $P_o$: Observed proportional agreement between Judge 1 and Judge 2.
- $P_e$: Probability of chance agreement based on marginal totals.
- **Interpretation:** $\kappa \ge 0.70$ indicates substantial agreement; $\kappa < 0.40$ indicates unacceptable judge drift.
