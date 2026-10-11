# Phase 07 — Exercise 01: Vector Stores, In-Memory Embeddings, Document Chunking & Semantic Filtering

## Mission Overview
In **Phase 07 Exercise 01**, you build cold muscle memory on Spring AI's Vector Store, Document, and Metadata Filtering primitives through **18 repetitive drills organized into 6 core framework topics (3 repetitions per topic)**:
- **Topic 1: Document Creation & Mutation:** `Document.builder()`, `doc.mutate()`, and batch generation.
- **Topic 2: Token Text Splitting:** `TokenTextSplitter` basic chunking, multi-param boundary configs, and chunk enrichment.
- **Topic 3: Vector Store Lifecycle:** In-memory `SimpleVectorStore` indexing, targeted deletion, and synchronized updates.
- **Topic 4: Search & Retrieval:** Top-K `SearchRequest`, similarity threshold gating, and score sorting.
- **Topic 5: FilterExpression Basics & Comparisons:** `FilterExpressionBuilder` equality (`eq`/`ne`), ranges (`gte`/`lte`), and logical conjunctions (`and`/`or`).
- **Topic 6: FilterExpression Collections & Search Integration:** Set containment (`in`/`nin`), nested logical groups, and full filtered search execution.

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase07/VectorStoreUnderTest.java`](src/main/java/phase07/VectorStoreUnderTest.java).
   - Contracts in [`src/main/java/phase07/VectorStoreContracts.java`](src/main/java/phase07/VectorStoreContracts.java) and [`src/test/java/phase07/Verifier.java`](src/test/java/phase07/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Runs 100% offline with zero live token costs using `FakeEmbeddingModel`.

---

## 18 Scenarios Breakdown (6 Topics × 3 Repetitions)

| # | Topic | Component | Key Responsibility |
|---|---|---|---|
| **01** | **Topic 1: Document** | `DocumentFactory` | Validates and creates normalized `Document`s enriched with metadata (`charCount`, `createdAt`). |
| **02** | **Topic 1: Document** | `DocumentEnricher` | Mutates existing documents via `doc.mutate().metadata(...)` with environment and version tags. |
| **03** | **Topic 1: Document** | `DocumentBatchBuilder` | Converts batch `RawArticle` entities into `Document`s with derived `wordCount` and `source`. |
| **04** | **Topic 2: Splitting** | `BasicTextSplitter` | Splits document into token chunks using `TokenTextSplitter.builder().withChunkSize().build()`. |
| **05** | **Topic 2: Splitting** | `ConfigurableTextSplitter` | Configures `TokenTextSplitter` with min/max chunk sizes, max chunks, and separators. |
| **06** | **Topic 2: Splitting** | `ChunkEnrichmentPipeline` | Splits documents and enriches chunks with `chunkIndex`, `totalChunks`, and `parentDocId`. |
| **07** | **Topic 3: VectorStore** | `VectorStoreIndexer` | Initializes `SimpleVectorStore` around `EmbeddingModel` and indexes documents. |
| **08** | **Topic 3: VectorStore** | `VectorStoreDeleter` | Deletes targeted documents from `VectorStore` by ID list. |
| **09** | **Topic 3: VectorStore** | `VectorStoreLifecycleManager` | Synchronizes store state by atomically deleting obsolete IDs and indexing fresh documents. |
| **10** | **Topic 4: Search** | `TopKSearchEngine` | Executes top-K ranked retrieval via `SearchRequest` and maps results to `SearchResult` records. |
| **11** | **Topic 4: Search** | `ThresholdSearchEngine` | Filters retrieval candidates using `similarityThreshold(threshold)`. |
| **12** | **Topic 4: Search** | `ScoredSearchEngine` | Executes scored similarity search and sorts results descending by score. |
| **13** | **Topic 5: Filter DSL** | `EqualityFilterBuilder` | Builds `EQ` and `NE` metadata expressions via `FilterExpressionBuilder`. |
| **14** | **Topic 5: Filter DSL** | `NumericRangeFilterBuilder` | Builds range expressions combining `GTE` and `LTE` inside an `AND` conjunction. |
| **15** | **Topic 5: Filter DSL** | `LogicalFilterBuilder` | Builds dual-branch `AND` or `OR` conjunction expressions with field-value pairs. |
| **16** | **Topic 6: Filter Search** | `SetContainmentFilterBuilder` | Builds set containment `IN` and `NIN` expressions from collections. |
| **17** | **Topic 6: Filter Search** | `NestedFilterBuilder` | Builds complex nested expressions: `(env == targetEnv AND (severity == minSev OR team IN teams))`. |
| **18** | **Topic 6: Filter Search** | `FilteredSearchGateway` | Executes end-to-end `SearchRequest` combining query, top-K, and `Filter.Expression` against `VectorStore`. |

---

## Verification Commands

```powershell
# Run the complete test suite:
mvn test-compile exec:java -pl phase07-ex01-vector-stores

# Run a single scenario (e.g. Scenario 5):
mvn test-compile exec:java -pl phase07-ex01-vector-stores "-Dexec.args=5"

# Run JUnit / Surefire test:
mvn test -pl phase07-ex01-vector-stores
```
