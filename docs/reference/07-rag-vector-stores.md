# Spring AI 2.0.1 — Retrieval-Augmented Generation (RAG) & Vector Stores

[← Back to Reference Index](spring-ai-2.0.1-reference.md)

**BOM:** `org.springframework.ai:spring-ai-bom:2.0.1`  
**Paired Boot:** `org.springframework.boot:spring-boot-dependencies:4.1.1`  
**Curriculum Alignment:** Phase 07 (`phase07-rag-vector-stores`) & Semantic Retrieval

---

Spring AI 2.0.1 provides a modular, production-grade RAG framework decoupled into pre-retrieval, retrieval, post-retrieval, and generation phases.

---

## 1. Key Dependencies

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store</artifactId>
</dependency>
<!-- For specific backends: pgvector, redis, qdrant, weaviate, neo4j, etc. -->
```

---

## 2. Core Document & Search Contracts

### 2.1 `Document` Contract
```java
package org.springframework.ai.document;

public class Document {
    public Document(String text);
    public Document(String text, Map<String, Object> metadata);
    public Document(String id, String text, Map<String, Object> metadata);

    public String getId();
    public String getText();
    public Map<String, Object> getMetadata();
    public Double getScore();
    public float[] getEmbedding();
    public Document mutate();
}
```

### 2.2 `VectorStore` Contract
```java
package org.springframework.ai.vectorstore;

public interface VectorStore {
    void add(List<Document> documents);
    void delete(List<String> idList);
    List<Document> similaritySearch(String query);
    List<Document> similaritySearch(SearchRequest request);
}
```

### 2.3 `SearchRequest` Fluent Builder
```java
SearchRequest request = SearchRequest.builder()
        .query("out of memory error")
        .topK(5)
        .similarityThreshold(0.75)
        .filterExpression("category == 'incident' && severity >= 3")
        .build();
```

### 2.4 Metadata Filter DSL (`Filter.Expression` & `FilterExpressionBuilder`)
Spring AI provides a type-safe metadata filtering DSL:

```java
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

FilterExpressionBuilder b = new FilterExpressionBuilder();

// Equals
Filter.Expression expr = b.eq("tier", "enterprise").build();

// In list
Filter.Expression inExpr = b.in("environment", List.of("prod", "staging")).build();

// Boolean Combinations
Filter.Expression complex = b.and(
        b.eq("status", "ACTIVE"),
        b.or(b.eq("role", "ADMIN"), b.gte("clearanceLevel", 4))
).build();
```

---

## 3. Text Splitting & Embedding Mechanics

1. **Fixed Token / Word Boundary Chunking:**
   - Overlap window ensures context continuity across chunk seams (e.g., chunk size 100, overlap 20).
   - Invariant: `overlapTokens < chunkSizeTokens`.
2. **Normalized Cosine Similarity:**
   $$\text{CosineSimilarity}(\vec{A}, \vec{B}) = \frac{\vec{A} \cdot \vec{B}}{\|\vec{A}\|_2 \|\vec{B}\|_2}$$
   - Guard against zero-vector division by zero: if magnitude is $0.0$, return $0.0$.
   - Clamp numerical drift: `Math.max(-1.0, Math.min(1.0, similarity))`.
3. **Reciprocal Rank Fusion (RRF):**
   Combine keyword BM25 and vector semantic ranks:
   $$\text{RRF}(d) = \sum_{m \in \{\text{keyword}, \text{semantic}\}} \frac{1}{60 + \text{rank}_m(d)}$$

---

## 4. Spring AI Modular RAG Advisor Stack

The Spring AI RAG pipeline follows four distinct lifecycle phases:

```
User Query ──► [Pre-Retrieval] ──► [Retrieval] ──► [Post-Retrieval] ──► [Augmentation / Advisor] ──► LLM Call
                 • Multi-Query       • VectorStore     • Reranker            • Prompt Augmenter
                 • Query Rewrite     • Metadata Filter • Deduplication       • Token Packing
```

### 4.1 Document Retriever
```java
public interface DocumentRetriever {
    List<Document> retrieve(Query query);
}
```
Default implementation: `VectorStoreDocumentRetriever.builder().vectorStore(store).topK(5).similarityThreshold(0.7).build()`.

### 4.2 Query Transformation & Expansion
- **`MultiQueryExpander`**: Generates $N$ semantic query variants using an LLM to mitigate lexical mismatch.
- **`RewriteQueryTransformer`**: Resolves conversational pronouns (`"What about that one?"` -> `"What about the PetClinic database bottleneck?"`) using chat history.
- **`CompressionQueryTransformer`**: Compresses multi-turn conversation history and follow-up prompts into a concise, standalone search query.
- **`TranslationQueryTransformer`**: Translates user queries into the target corpus/index language.
- **`HydeQueryTransformer` (HyDE)**: Hypothetical Document Embeddings. Generates a synthetic candidate document using an LLM, embeds the synthetic document, and queries the vector store to find true documents nearest to the answer space.

```java
// Pre-retrieval Query Transformers:
QueryTransformer compressionTransformer = CompressionQueryTransformer.builder()
        .chatClientBuilder(chatClientBuilder)
        .build();

QueryTransformer rewriteTransformer = RewriteQueryTransformer.builder()
        .chatClientBuilder(chatClientBuilder)
        .targetSearchSystem("VectorStore BM25")
        .build();

QueryTransformer translationTransformer = TranslationQueryTransformer.builder()
        .chatClientBuilder(chatClientBuilder)
        .targetLanguage("Spanish")
        .build();
```

### 4.3 Post-Retrieval Reranking, Deduplication & Budget Packing
- **`ConcatenationDocumentJoiner`**: Merges document lists from multi-query retrievals, deduplicating by document ID or text content.
- **`DocumentPostProcessor`**: Custom reranking, metadata recency scoring, or diversity filtering.
- **`MaximalMarginalRelevanceSearchEngine` (MMR)**: Re-ranks candidate documents to balance relevance to the query against redundancy with already selected documents ($\lambda \cdot \text{Sim}(d_i, q) - (1 - \lambda) \cdot \max_{d_j \in S} \text{Sim}(d_i, d_j)$).
- **`ContentHashDeduplicationPipeline`**: Computes deterministic SHA-256 digests over chunk text and metadata to guarantee idempotent ingestion and eliminate duplicate chunks.
- **`TokenBudgetPacker`**: Packs retrieved documents up to a strict token ceiling, dynamically truncating or omitting documents to prevent context window overflow.

### 4.4 Contextual Query Augmentation & Advisor
- **`ContextualQueryAugmenter`**: Formats documents into a contextual prompt block (`<context>...</context>`).
- **`EmptyContextFallbackAugmenter`**: Handles empty retrieval results gracefully with pre-defined fallback prompt or out-of-domain notification.
- **`RetrievalAugmentationAdvisor`**: Implements `CallAdvisor` to wire the entire pipeline into Spring AI `ChatClient`.

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
        .documentRetriever(retriever)
        .queryTransformers(List.of(rewriteTransformer))
        .queryExpander(multiQueryExpander)
        .documentJoiner(joiner)
        .documentPostProcessors(List.of(reranker))
        .queryAugmenter(augmenter)
        .build();

ChatClient client = ChatClient.builder(chatModel)
        .defaultAdvisors(advisor)
        .build();
```
