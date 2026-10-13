# Phase 07 Exercise 02: Modular RAG Advisors, Pre/Post Retrieval Pipelines & ChatClient Integration

## Focus & Learning Objectives
This exercise builds cold muscle memory on Spring AI 2.0.1's native modular RAG package (`org.springframework.ai.rag.*`) through **15 repetitive drills organized into 5 core topics (3 repetitions each)**:
- **Topic 1: VectorStoreDocumentRetriever:** (1) Basic top-K, (2) threshold gating, (3) metadata filter expressions.
- **Topic 2: ContextualQueryAugmenter:** (4) Custom document formatter, (5) custom prompt template placeholders, (6) fallback empty context guard.
- **Topic 3: DocumentPostProcessor:** (7) ID deduplication, (8) metadata priority ranking & truncation, (9) score-gated filtering.
- **Topic 4: Pre-Retrieval Query Transformers:** (10) `MultiQueryExpander`, (11) `RewriteQueryTransformer`, (12) `TranslationQueryTransformer`.
- **Topic 5: RetrievalAugmentationAdvisor:** (13) Basic retriever binding, (14) retriever + augmenter chaining, (15) full pipeline assembly.

---

## 🧠 Architectural Theory & Pipeline Lifecycle

In Spring AI 2.0.1, Retrieval-Augmented Generation (RAG) is decoupled into a modular 4-stage pipeline orchestrated by `RetrievalAugmentationAdvisor` (which implements `CallAdvisor`):

```
                                  RETRIEVAL AUGMENTATION ADVISOR
                   ┌────────────────────────────────────────────────────────┐
                   │                                                        │
[User Query] ──────┼──► [1. Pre-Retrieval]      QueryTransformer            │
                   │    • MultiQueryExpander    (Rewrites, expands,         │
                   │    • RewriteQuery           or compresses queries)     │
                   │    • TranslationQuery                                  │
                   │            │                                           │
                   │            ▼                                           │
                   │    [2. Retrieval]          DocumentRetriever           │
                   │    • VectorStoreRetriever  (Fetches top-K candidates   │
                   │    • Metadata FilterExpr    from VectorStore)          │
                   │            │                                           │
                   │            ▼                                           │
                   │    [3. Post-Retrieval]     DocumentPostProcessor       │
                   │    • Deduplication         (Cleans, scores, sorts,     │
                   │    • Priority Sorting       or trims candidates)       │
                   │    • Score Gating                                      │
                   │            │                                           │
                   │            ▼                                           │
                   │    [4. Augmentation]       QueryAugmenter              │
                   │    • Contextual Augmenter  (Formats chunks into prompt │
                   │    • Empty Context Guard    block <context>...</context>)
                   │            │                                           │
                   └────────────┼───────────────────────────────────────────┘
                                ▼
                     [Augmented Prompt] ──► ChatClient ──► LLM
```

### 🧩 Core Spring AI Classes & Roles

| Class | Package | Role & Data Flow |
|---|---|---|
| `Query` | `org.springframework.ai.rag` | Immutable value object wrapping prompt text and optional history. |
| `QueryTransformer` | `o.s.ai.rag.preretrieval.query.transformation` | **Stage 1 (Pre-Retrieval):** Transforms `Query -> Query` (e.g. resolving pronouns via `RewriteQueryTransformer`, multilingual translation via `TranslationQueryTransformer`, or compressing conversation via `CompressionQueryTransformer`). |
| `MultiQueryExpander` | `o.s.ai.rag.preretrieval.query.expansion` | **Stage 1 (Pre-Retrieval):** Expands 1 prompt into $N$ query variations to maximize vector recall across lexical mismatches. |
| `DocumentRetriever` | `o.s.ai.rag.retrieval.search` | **Stage 2 (Retrieval):** Executes `retrieve(Query) -> List<Document>` against a vector store. Implemented by `VectorStoreDocumentRetriever`. |
| `DocumentPostProcessor` | `o.s.ai.rag.postretrieval.document` | **Stage 3 (Post-Retrieval):** Cleans, re-ranks, or deduplicates candidate `List<Document>` before prompt injection. |
| `QueryAugmenter` | `o.s.ai.rag.generation.augmentation` | **Stage 4 (Augmentation):** Injects retrieved documents into prompt templates (`{context}` + `{query}`). Implemented by `ContextualQueryAugmenter`. |
| `RetrievalAugmentationAdvisor` | `o.s.ai.rag.advisor` | **The Orchestrator:** Implements `CallAdvisor` to wire stages 1–4 seamlessly around any `ChatClient.prompt().call()`. |

---

## 15 Scenarios Breakdown (5 Topics × 3 Repetitions)

| # | Topic | Component | Key Responsibility |
|---|---|---|---|
| **01** | **Retriever** | `buildBasicRetriever` | Builds `VectorStoreDocumentRetriever` with store and top-K parameter. |
| **02** | **Retriever** | `buildThresholdRetriever` | Configures `VectorStoreDocumentRetriever` with similarity cutoff threshold. |
| **03** | **Retriever** | `buildFilterExpressionRetriever` | Configures `VectorStoreDocumentRetriever` with tenant/metadata `Filter.Expression`. |
| **04** | **Augmenter** | `buildCustomFormattedAugmenter` | Builds `ContextualQueryAugmenter` with custom document formatting function. |
| **05** | **Augmenter** | `buildTemplatedAugmenter` | Builds `ContextualQueryAugmenter` with custom `{context}` and `{query}` prompt template. |
| **06** | **Augmenter** | `buildFallbackGuardedAugmenter` | Configures `allowEmptyContext(false)` and fallback empty-context prompt template. |
| **07** | **PostProcessor** | `buildDeduplicatingPostProcessor` | Deduplicates candidate documents by `doc.getId()` preserving initial order. |
| **08** | **PostProcessor** | `buildPriorityRankingPostProcessor` | Deduplicates and sorts documents descending by `"priority"` metadata, limited to `maxDocs`. |
| **09** | **PostProcessor** | `buildScoreGatingPostProcessor` | Filters candidate documents below a minimum cosine score threshold. |
| **10** | **Transformer** | `buildMultiQueryExpander` | Configures `MultiQueryExpander` with `numberOfQueries` and `includeOriginal`. |
| **11** | **Transformer** | `buildRewriteTransformer` | Configures `RewriteQueryTransformer` with target search system. |
| **12** | **Transformer** | `buildTranslationTransformer` | Configures `TranslationQueryTransformer` with target language. |
| **13** | **Advisor** | `buildBasicAdvisor` | Binds `DocumentRetriever` into `RetrievalAugmentationAdvisor`. |
| **14** | **Advisor** | `buildAugmentedAdvisor` | Chains `DocumentRetriever` and `QueryAugmenter` into `RetrievalAugmentationAdvisor`. |
| **15** | **Advisor** | `buildFullPipelineAdvisor` | Assembles full pipeline (retriever + expander + postprocessor + augmenter) into `RetrievalAugmentationAdvisor`. |

---

## Verification
Run standard Maven tests:
```powershell
mvn test -pl phase07-ex02-rag-advisors
```

Or run single scenario:
```powershell
mvn test-compile exec:java -pl phase07-ex02-rag-advisors "-Dexec.args=1"
```

A verified reference implementation is available in `docs/golden/phase07-ex02-golden.md`.
