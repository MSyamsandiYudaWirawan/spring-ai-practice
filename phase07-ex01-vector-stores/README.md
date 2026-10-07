# Phase 07 — Exercise 01: Vector Stores, In-Memory Embeddings, Document Chunking & Semantic Filtering

## Mission Overview
In **Phase 07 Exercise 01**, you build foundational muscle memory on Spring AI's Vector Store and Embedding primitives:
- Creating normalized `Document` instances with metadata attributes via `Document.builder()`.
- Token-bounded text chunking and overlap handling via `TokenTextSplitter`.
- In-memory `SimpleVectorStore` creation and `EmbeddingModel` binding.
- Semantic top-K similarity search with score ranking (`SearchRequest`).
- Cutoff similarity threshold gating.
- Structured metadata filter expressions using `FilterExpressionBuilder`.
- Incremental document deletion and re-indexing.
- Zero-cost in-memory embedding cache decorator.
- End-to-end knowledge ingestion and retrieval verification gateway.
- Maximal Marginal Relevance (MMR) diversity search re-ranking.
- Content-hash idempotent ingestion and compaction pipeline.

---

## Pedagogical Protocol & Guardrails
1. **Strict Seam Boundary:**
   - The **ONLY** file you modify is [`src/main/java/phase07/VectorStoreUnderTest.java`](src/main/java/phase07/VectorStoreUnderTest.java).
   - Contracts in [`src/main/java/phase07/VectorStoreContracts.java`](src/main/java/phase07/VectorStoreContracts.java) and [`src/test/java/phase07/Verifier.java`](src/test/java/phase07/Verifier.java) are immutable.
2. **Deterministic Offline Execution:**
   - Runs 100% offline with zero live token costs using `FakeEmbeddingModel`.
3. **Non-Flaky Exception Contract:**
   - **Scenario 10:** [`VectorStoreBreachException`](src/main/java/phase07/VectorStoreContracts.java) message **MUST contain** `"VectorStore constraint breached"` (case-insensitive).

---

## 12 Scenarios Breakdown

| # | Component | Key Responsibility |
|---|---|---|
| **01** | `DocumentFactory` | Validates and creates normalized `Document`s enriched with metadata (`charCount`, `createdAt`). |
| **02** | `TextChunkingPipeline` | Splits documents using `TokenTextSplitter` and tags chunks with `chunkIndex` and `totalChunks`. |
| **03** | `DeterministicVectorGenerator` | Computes deterministic float vectors with unit L2 norm (`\|\|v\|\| == 1.0f`). |
| **04** | `VectorStoreIndexer` | Initializes `SimpleVectorStore` around an `EmbeddingModel` and indexes documents. |
| **05** | `SimilaritySearchEngine` | Executes top-K ranked retrieval via `SearchRequest` and maps results to `SearchResult`s. |
| **06** | `ThresholdSimilarityFilter` | Enforces minimum similarity score cutoffs via `similarityThreshold`. |
| **07** | `MetadataExpressionFilter` | Builds composite boolean filter expressions via `FilterExpressionBuilder`. |
| **08** | `DocumentLifecycleManager` | Atomically deletes obsolete document IDs and indexes fresh document versions. |
| **09** | `CachedEmbeddingDecorator` | Caches computed float vectors in memory to eliminate redundant embedding calls. |
| **10** | `KnowledgeIngestionGateway` | Ingests raw articles into chunked vector store entries and enforces searchability gates. |
| **11** | `MaximalMarginalRelevanceSearchEngine` | Re-ranks candidates using MMR diversity penalties ($\lambda \cdot \text{sim}(d,q) - (1-\lambda) \max \text{sim}(d,s)$). |
| **12** | `ContentHashDeduplicationPipeline` | Computes SHA-256 hashes of normalized chunks to skip redundant embeddings and prevent duplicate indexing. |

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
