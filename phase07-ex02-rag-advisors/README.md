# Phase 07 Exercise 02: Modular RAG Advisors, Pre/Post Retrieval Pipelines & ChatClient Integration

## Focus & Learning Objectives
This exercise builds cold muscle memory on Spring AI 2.0.1's native modular RAG package (`org.springframework.ai.rag.*`) through **15 repetitive drills organized into 5 core topics (3 repetitions each)**:
- **Topic 1: VectorStoreDocumentRetriever:** (1) Basic top-K, (2) threshold gating, (3) metadata filter expressions.
- **Topic 2: ContextualQueryAugmenter:** (4) Custom document formatter, (5) custom prompt template placeholders, (6) fallback empty context guard.
- **Topic 3: DocumentPostProcessor:** (7) ID deduplication, (8) metadata priority ranking & truncation, (9) score-gated filtering.
- **Topic 4: Pre-Retrieval Query Transformers:** (10) `MultiQueryExpander`, (11) `RewriteQueryTransformer`, (12) `TranslationQueryTransformer`.
- **Topic 5: RetrievalAugmentationAdvisor:** (13) Basic retriever binding, (14) retriever + augmenter chaining, (15) full pipeline assembly.

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
