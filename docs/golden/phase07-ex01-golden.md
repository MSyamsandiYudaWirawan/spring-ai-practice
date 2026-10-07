# Golden Solution: Phase 07 Exercise 01 (Vector Stores, In-Memory Embeddings & Semantic Filtering)

## Overview
This golden solution implements all 10 scenarios of `phase07-ex01-vector-stores`, providing complete, tested implementations of normalized `Document` creation, token-bounded document splitting via `TokenTextSplitter`, unit L2 vector normalization, `SimpleVectorStore` indexing, top-K ranked retrieval, cutoff similarity threshold gating, `FilterExpressionBuilder` metadata filters, document lifecycle updates, cached embedding decorators, and end-to-end ingestion gateways.

Verified: `10 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase07-ex01-vector-stores/src/main/java/phase07/VectorStoreUnderTest.java`

```java
package phase07;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import phase07.VectorStoreContracts.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Golden implementation for Phase 07 Exercise 01.
 */
public class VectorStoreUnderTest {

    public static class DocumentFactory {
        public static Document createDocument(String id, String content, Map<String, Object> metadata) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (content == null || content.isBlank()) throw new IllegalArgumentException("content must not be blank");

            String normalized = content.trim();
            Map<String, Object> merged = new HashMap<>(metadata != null ? metadata : Map.of());
            merged.put("charCount", (long) normalized.length());
            merged.putIfAbsent("createdAt", Instant.now().toString());

            return Document.builder().id(id).text(normalized).metadata(merged).build();
        }
    }

    public static class TextChunkingPipeline {
        public static List<Document> splitDocument(Document document, ChunkConfig config) {
            if (document == null || config == null) throw new IllegalArgumentException("Inputs must not be null");

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(config.defaultChunkSize())
                    .withMinChunkSizeChars(config.minChunkSizeChars())
                    .withMinChunkLengthToEmbed(config.minChunkLengthToEmbed())
                    .withMaxNumChunks(config.maxNumChunks())
                    .withKeepSeparator(config.keepSeparator())
                    .build();

            List<Document> rawChunks = splitter.apply(List.of(document));
            List<Document> enriched = new ArrayList<>(rawChunks.size());
            for (int i = 0; i < rawChunks.size(); i++) {
                Document c = rawChunks.get(i);
                Map<String, Object> meta = new HashMap<>(c.getMetadata());
                meta.put("chunkIndex", i);
                meta.put("totalChunks", rawChunks.size());
                enriched.add(c.mutate().metadata(meta).build());
            }
            return enriched;
        }
    }

    public static class DeterministicVectorGenerator {
        public static float[] generateNormalizedVector(String text, int dimensions) {
            if (dimensions < 2) throw new IllegalArgumentException("dimensions must be >= 2");
            float[] vec = new float[dimensions];
            if (text == null || text.isBlank()) {
                vec[0] = 1.0f;
                return vec;
            }

            Random rand = new Random((long) text.trim().toLowerCase().hashCode());
            float sumSq = 0.0f;
            for (int i = 0; i < dimensions; i++) {
                vec[i] = (rand.nextFloat() * 2.0f) - 1.0f;
                sumSq += vec[i] * vec[i];
            }

            float norm = (float) Math.sqrt(sumSq);
            if (norm > 1e-6f) {
                for (int i = 0; i < dimensions; i++) {
                    vec[i] /= norm;
                }
            } else {
                vec[0] = 1.0f;
            }
            return vec;
        }
    }

    public static class VectorStoreIndexer {
        public static SimpleVectorStore createAndIndex(EmbeddingModel embeddingModel, List<Document> documents) {
            if (embeddingModel == null || documents == null) throw new IllegalArgumentException("Inputs must not be null");
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            store.add(documents);
            return store;
        }
    }

    public static class SimilaritySearchEngine {
        public static List<SearchResult> searchTopK(VectorStore vectorStore, String query, int topK) {
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            SearchRequest req = SearchRequest.builder().query(query).topK(topK).build();
            List<Document> docs = vectorStore.similaritySearch(req);
            return docs.stream()
                    .map(d -> new SearchResult(d.getId(), d.getText(), d.getMetadata(), d.getScore() != null ? d.getScore() : 0.0))
                    .toList();
        }
    }

    public static class ThresholdSimilarityFilter {
        public static List<SearchResult> searchWithThreshold(
                VectorStore vectorStore,
                String query,
                int topK,
                double minThreshold
        ) {
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid parameters");
            }
            if (minThreshold < 0.0 || minThreshold > 1.0) {
                throw new IllegalArgumentException("minThreshold must be between 0.0 and 1.0");
            }
            SearchRequest req = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(minThreshold)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(req);
            return docs.stream()
                    .map(d -> new SearchResult(d.getId(), d.getText(), d.getMetadata(), d.getScore() != null ? d.getScore() : 0.0))
                    .toList();
        }
    }

    public static class MetadataExpressionFilter {
        public static List<SearchResult> searchWithMetadataFilter(
                VectorStore vectorStore,
                String query,
                String targetCategory,
                String targetSeverity,
                int topK
        ) {
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid parameters");
            }
            if (targetCategory == null || targetCategory.isBlank() || targetSeverity == null || targetSeverity.isBlank()) {
                throw new IllegalArgumentException("Category and severity must be non-blank");
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            Filter.Expression expr = b.and(b.eq("category", targetCategory), b.eq("severity", targetSeverity)).build();

            SearchRequest req = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(expr)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(req);
            return docs.stream()
                    .map(d -> new SearchResult(d.getId(), d.getText(), d.getMetadata(), d.getScore() != null ? d.getScore() : 0.0))
                    .toList();
        }
    }

    public static class DocumentLifecycleManager {
        public static void deleteAndReindex(
                VectorStore vectorStore,
                List<String> documentIdsToDelete,
                List<Document> newDocuments
        ) {
            if (vectorStore == null) throw new IllegalArgumentException("vectorStore must not be null");
            if (documentIdsToDelete != null && !documentIdsToDelete.isEmpty()) {
                vectorStore.delete(documentIdsToDelete);
            }
            if (newDocuments != null && !newDocuments.isEmpty()) {
                vectorStore.add(newDocuments);
            }
        }
    }

    public static class CachedEmbeddingDecorator implements EmbeddingModel {
        private final EmbeddingModel delegate;
        private final Map<String, float[]> cache = new ConcurrentHashMap<>();
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);

        public CachedEmbeddingDecorator(EmbeddingModel delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        public CacheStats getStats() {
            return new CacheStats(hits.get(), misses.get());
        }

        @Override
        public float[] embed(Document document) {
            return embed(document != null ? document.getText() : "");
        }

        @Override
        public float[] embed(String text) {
            String key = text != null ? text : "";
            float[] cached = cache.get(key);
            if (cached != null) {
                hits.incrementAndGet();
                return cached;
            }
            misses.incrementAndGet();
            float[] computed = delegate.embed(key);
            cache.put(key, computed);
            return computed;
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            if (texts == null) return List.of();
            List<float[]> list = new ArrayList<>(texts.size());
            for (String t : texts) {
                list.add(embed(t));
            }
            return list;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> instructions = request != null ? request.getInstructions() : List.of();
            List<Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < instructions.size(); i++) {
                float[] vec = embed(instructions.get(i));
                embeddings.add(new Embedding(vec, i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public int dimensions() {
            return delegate.dimensions();
        }
    }

    public static class KnowledgeIngestionGateway {
        public static IngestionReport ingestAndVerify(
                VectorStore vectorStore,
                List<RawKnowledgeItem> rawItems,
                ChunkConfig chunkConfig,
                String testQuery,
                double minThreshold
        ) {
            if (vectorStore == null || rawItems == null || rawItems.isEmpty() || chunkConfig == null) {
                throw new IllegalArgumentException("Invalid gateway parameters");
            }

            List<Document> allChunks = new ArrayList<>();
            for (RawKnowledgeItem item : rawItems) {
                Map<String, Object> meta = new HashMap<>(item.metadata());
                meta.put("title", item.title());
                String fullText = item.title() + "\n" + item.content();
                Document rootDoc = DocumentFactory.createDocument(item.id(), fullText, meta);
                List<Document> chunks = TextChunkingPipeline.splitDocument(rootDoc, chunkConfig);
                allChunks.addAll(chunks);
            }

            vectorStore.add(allChunks);

            List<SearchResult> verification = ThresholdSimilarityFilter.searchWithThreshold(
                    vectorStore, testQuery, 5, minThreshold
            );

            if (verification.isEmpty()) {
                throw new VectorStoreBreachException("VectorStore constraint breached: knowledge verification query yielded zero matches for query: " + testQuery);
            }

            List<String> chunkIds = allChunks.stream().map(Document::getId).toList();
            return new IngestionReport(rawItems.size(), allChunks.size(), allChunks.size(), chunkIds);
        }
    }
}
```
