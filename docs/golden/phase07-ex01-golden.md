# Golden Solution: Phase 07 Exercise 01 (Vector Stores, In-Memory Embeddings, Document Chunking & Metadata Filtering)

## Overview
This golden solution implements all 18 scenarios of `phase07-ex01-vector-stores`, structured as 6 repetitive 3-drill clusters targeting core Spring AI framework mechanics:
1. `Document` creation and `doc.mutate()` enrichment (Scenarios 1–3)
2. `TokenTextSplitter` chunking and sequence metadata tagging (Scenarios 4–6)
3. `SimpleVectorStore` indexing, deletion, and synchronized updates (Scenarios 7–9)
4. `SearchRequest` top-K retrieval, cutoff threshold gating, and scored sorting (Scenarios 10–12)
5. `FilterExpressionBuilder` basics: `eq`/`ne`, `gte`/`lte`, and `and`/`or` (Scenarios 13–15)
6. `FilterExpressionBuilder` collections (`in`/`nin`), nested logical groups, and integrated filtered search (Scenarios 16–18)

Verified: `18 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase07-ex01-vector-stores/src/main/java/phase07/VectorStoreUnderTest.java`

```java
package phase07;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import phase07.VectorStoreContracts.*;

import java.time.Instant;
import java.util.*;

/**
 * Golden implementation for Phase 07 Exercise 01:
 * Vector Stores, In-Memory Embeddings, Document Chunking and Semantic Filtering.
 * <p>
 * 18 repetitive muscle-memory drills across 6 core framework topics (3 repetitions each).
 */
public class VectorStoreUnderTest {

    // =========================================================================
    // TOPIC 1: Document Creation & Mutation (Scenarios 1 - 3)
    // =========================================================================

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

    public static class DocumentEnricher {
        public static Document enrichDocument(Document document, String environment, String version) {
            if (document == null) throw new IllegalArgumentException("document must not be null");
            if (environment == null || environment.isBlank()) throw new IllegalArgumentException("environment must not be blank");
            if (version == null || version.isBlank()) throw new IllegalArgumentException("version must not be blank");

            Map<String, Object> updated = new HashMap<>(document.getMetadata());
            updated.put("environment", environment.trim().toUpperCase());
            updated.put("version", version.trim());
            updated.put("enrichedAt", Instant.now().toString());

            return document.mutate().metadata(updated).build();
        }
    }

    public static class DocumentBatchBuilder {
        public static List<Document> buildBatch(List<RawArticle> articles) {
            if (articles == null || articles.isEmpty()) {
                throw new IllegalArgumentException("articles must not be null or empty");
            }

            List<Document> batch = new ArrayList<>(articles.size());
            for (RawArticle article : articles) {
                if (article.id() == null || article.id().isBlank() || article.text() == null || article.text().isBlank()) {
                    throw new IllegalArgumentException("article id and text must not be blank");
                }

                String trimmed = article.text().trim();
                Map<String, Object> meta = new HashMap<>(article.metadata());
                meta.put("wordCount", (long) trimmed.split("\\s+").length);
                meta.put("source", (article.source() != null && !article.source().isBlank()) ? article.source().trim() : "unknown");
                meta.put("indexed", true);

                batch.add(Document.builder().id(article.id()).text(trimmed).metadata(meta).build());
            }
            return batch;
        }
    }

    // =========================================================================
    // TOPIC 2: TokenTextSplitter Chunking & Enrichment (Scenarios 4 - 6)
    // =========================================================================

    public static class BasicTextSplitter {
        public static List<Document> split(Document document, int chunkSize) {
            if (document == null || chunkSize < 1) {
                throw new IllegalArgumentException("document must not be null and chunkSize must be >= 1");
            }
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .build();
            return splitter.apply(List.of(document));
        }
    }

    public static class ConfigurableTextSplitter {
        public static List<Document> splitWithConfig(Document document, ChunkConfig config) {
            if (document == null || config == null) {
                throw new IllegalArgumentException("document and config must not be null");
            }
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(config.defaultChunkSize())
                    .withMinChunkSizeChars(config.minChunkSizeChars())
                    .withMinChunkLengthToEmbed(config.minChunkLengthToEmbed())
                    .withMaxNumChunks(config.maxNumChunks())
                    .withKeepSeparator(config.keepSeparator())
                    .build();
            return splitter.apply(List.of(document));
        }
    }

    public static class ChunkEnrichmentPipeline {
        public static List<Document> splitAndEnrich(Document document, ChunkConfig config) {
            if (document == null || config == null) {
                throw new IllegalArgumentException("document and config must not be null");
            }

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
                meta.put("parentDocId", document.getId());
                enriched.add(c.mutate().metadata(meta).build());
            }
            return enriched;
        }
    }

    // =========================================================================
    // TOPIC 3: SimpleVectorStore Lifecycle Operations (Scenarios 7 - 9)
    // =========================================================================

    public static class VectorStoreIndexer {
        public static SimpleVectorStore createAndIndex(EmbeddingModel embeddingModel, List<Document> documents) {
            if (embeddingModel == null || documents == null) {
                throw new IllegalArgumentException("embeddingModel and documents must not be null");
            }
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            store.add(documents);
            return store;
        }
    }

    public static class VectorStoreDeleter {
        public static void deleteDocuments(VectorStore vectorStore, List<String> documentIds) {
            if (vectorStore == null || documentIds == null) {
                throw new IllegalArgumentException("vectorStore and documentIds must not be null");
            }
            if (!documentIds.isEmpty()) {
                vectorStore.delete(documentIds);
            }
        }
    }

    public static class VectorStoreLifecycleManager {
        public static void syncDocuments(VectorStore vectorStore, List<String> toDelete, List<Document> toAdd) {
            if (vectorStore == null) {
                throw new IllegalArgumentException("vectorStore must not be null");
            }
            if (toDelete != null && !toDelete.isEmpty()) {
                vectorStore.delete(toDelete);
            }
            if (toAdd != null && !toAdd.isEmpty()) {
                vectorStore.add(toAdd);
            }
        }
    }

    // =========================================================================
    // TOPIC 4: SearchRequest & Retrieval (Scenarios 10 - 12)
    // =========================================================================

    public static class TopKSearchEngine {
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

    public static class ThresholdSearchEngine {
        public static List<SearchResult> searchWithThreshold(VectorStore vectorStore, String query, int topK, double threshold) {
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("threshold must be between 0.0 and 1.0");
            }
            SearchRequest req = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(req);
            return docs.stream()
                    .map(d -> new SearchResult(d.getId(), d.getText(), d.getMetadata(), d.getScore() != null ? d.getScore() : 0.0))
                    .toList();
        }
    }

    public static class ScoredSearchEngine {
        public static List<SearchResult> searchScoredAndSorted(VectorStore vectorStore, String query, int topK, double threshold) {
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("threshold must be between 0.0 and 1.0");
            }
            SearchRequest req = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(req);
            List<SearchResult> results = new ArrayList<>(docs.stream()
                    .map(d -> new SearchResult(d.getId(), d.getText(), d.getMetadata(), d.getScore() != null ? d.getScore() : 0.0))
                    .toList());
            results.sort(Comparator.comparingDouble(SearchResult::score).reversed());
            return results;
        }
    }

    // =========================================================================
    // TOPIC 5: FilterExpressionBuilder Basics & Comparisons (Scenarios 13 - 15)
    // =========================================================================

    public static class EqualityFilterBuilder {
        public static Filter.Expression buildEqualityFilter(String field, String value, boolean negate) {
            if (field == null || field.isBlank() || value == null) {
                throw new IllegalArgumentException("Field and value must not be blank/null");
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return negate ? b.ne(field, value).build() : b.eq(field, value).build();
        }
    }

    public static class NumericRangeFilterBuilder {
        public static Filter.Expression buildRangeFilter(String field, double minValue, double maxValue) {
            if (field == null || field.isBlank() || minValue > maxValue) {
                throw new IllegalArgumentException("Invalid range parameters");
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return b.and(b.gte(field, minValue), b.lte(field, maxValue)).build();
        }
    }

    public static class LogicalFilterBuilder {
        public static Filter.Expression buildConjunctiveFilter(
                String catField, String catVal,
                String sevField, String sevVal,
                boolean requireBoth
        ) {
            if (catField == null || catField.isBlank() || catVal == null || catVal.isBlank()
                    || sevField == null || sevField.isBlank() || sevVal == null || sevVal.isBlank()) {
                throw new IllegalArgumentException("All filter fields and values must be non-blank");
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return requireBoth
                    ? b.and(b.eq(catField, catVal), b.eq(sevField, sevVal)).build()
                    : b.or(b.eq(catField, catVal), b.eq(sevField, sevVal)).build();
        }
    }

    // =========================================================================
    // TOPIC 6: FilterExpressionBuilder Collections & Search Integration (Scenarios 16 - 18)
    // =========================================================================

    public static class SetContainmentFilterBuilder {
        public static Filter.Expression buildInFilter(String field, List<String> values, boolean negate) {
            if (field == null || field.isBlank() || values == null || values.isEmpty()) {
                throw new IllegalArgumentException("Field and values must be valid");
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return negate
                    ? b.nin(field, values.toArray()).build()
                    : b.in(field, values.toArray()).build();
        }
    }

    public static class NestedFilterBuilder {
        public static Filter.Expression buildAuditFilter(String env, String minSeverity, List<String> targetTeams) {
            if (env == null || env.isBlank() || minSeverity == null || minSeverity.isBlank() || targetTeams == null || targetTeams.isEmpty()) {
                throw new IllegalArgumentException("Inputs must not be null or blank");
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return b.and(
                    b.eq("environment", env),
                    b.or(b.eq("severity", minSeverity), b.in("team", targetTeams.toArray()))
            ).build();
        }
    }

    public static class FilteredSearchGateway {
        public static List<SearchResult> searchWithFilter(
                VectorStore vectorStore,
                String query,
                Filter.Expression filterExpression,
                int topK
        ) {
            if (vectorStore == null || query == null || query.isBlank() || filterExpression == null || topK < 1) {
                throw new IllegalArgumentException("Invalid search arguments");
            }
            SearchRequest req = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(filterExpression)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(req);
            return docs.stream()
                    .map(d -> new SearchResult(d.getId(), d.getText(), d.getMetadata(), d.getScore() != null ? d.getScore() : 0.0))
                    .toList();
        }
    }
}
```
