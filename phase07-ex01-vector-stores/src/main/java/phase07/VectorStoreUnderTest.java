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
 * Exercise implementation under test for Phase 07 Exercise 01:
 * Vector Stores, In-Memory Embeddings, Document Chunking and Semantic Filtering.
 * <p>
 * 18 repetitive muscle-memory drills organized into 6 core topics (3 repetitions each).
 * Implement all 18 scenarios in this file.
 */
public class VectorStoreUnderTest {

    // =========================================================================
    // TOPIC 1: Document Creation & Mutation (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 1: Basic Document Creation with Metadata Normalization.
     * <p>
     * Instructions:
     * - Validate that id and content are not null and not blank; throw {@link IllegalArgumentException} otherwise.
     * - Trim whitespace from content.
     * - Create a mutable copy of the provided metadata (or an empty map if null).
     * - Enforce required metadata:
     *   - Put "charCount" mapped to (long) content.length().
     *   - If "createdAt" is absent, put Instant.now().toString().
     * - Build and return immutable Document via {@link Document#builder()}.
     */
    public static class DocumentFactory {
        public static Document createDocument(String id, String content, Map<String, Object> metadata) {
            // DEFECT (Scenario 1): Blindly returns null without validation
            return null;
        }
    }

    /**
     * Scenario 2: Document Mutation & Tag Enrichment via mutate().
     * <p>
     * Instructions:
     * - Validate document != null, environment != null && !environment.isBlank(), version != null && !version.isBlank();
     *   throw {@link IllegalArgumentException} otherwise.
     * - Copy existing document metadata into a mutable map.
     * - Add "environment" -> environment.trim().toUpperCase().
     * - Add "version" -> version.trim().
     * - Add "enrichedAt" -> Instant.now().toString().
     * - Return updated document using document.mutate().metadata(updatedMetadata).build().
     */
    public static class DocumentEnricher {
        public static Document enrichDocument(Document document, String environment, String version) {
            // DEFECT (Scenario 2): Returns document unchanged without mutation
            if (document == null || environment == null || environment.isBlank() || version == null || version.isBlank()) {
                throw new IllegalArgumentException("Invalid arguments");
            }
            return document;
        }
    }

    /**
     * Scenario 3: Batch Document Generator from Raw Entities.
     * <p>
     * Instructions:
     * - Validate articles != null and !articles.isEmpty(); throw {@link IllegalArgumentException} otherwise.
     * - For each RawArticle:
     *   - Validate id and text are not null and not blank; throw {@link IllegalArgumentException} otherwise.
     *   - Trim text.
     *   - Copy article.metadata() into a mutable map.
     *   - Put "wordCount" -> (long) trimmedText.split("\\s+").length.
     *   - Put "source" -> (article.source() != null && !article.source().isBlank()) ? article.source().trim() : "unknown".
     *   - Put "indexed" -> true.
     *   - Build Document via Document.builder().id(article.id()).text(trimmedText).metadata(meta).build().
     * - Return list of constructed Documents.
     */
    public static class DocumentBatchBuilder {
        public static List<Document> buildBatch(List<RawArticle> articles) {
            // DEFECT (Scenario 3): Returns empty list
            if (articles == null || articles.isEmpty()) throw new IllegalArgumentException("articles must not be empty");
            return List.of();
        }
    }

    // =========================================================================
    // TOPIC 2: TokenTextSplitter Chunking & Enrichment (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 4: Basic Token Text Splitting.
     * <p>
     * Instructions:
     * - Validate document != null and chunkSize >= 1; throw {@link IllegalArgumentException} otherwise.
     * - Construct TokenTextSplitter via TokenTextSplitter.builder().withChunkSize(chunkSize).build().
     * - Call splitter.apply(List.of(document)) and return the chunked documents.
     */
    public static class BasicTextSplitter {
        public static List<Document> split(Document document, int chunkSize) {
            // DEFECT (Scenario 4): Returns unchunked document list
            if (document == null || chunkSize < 1) throw new IllegalArgumentException("Invalid arguments");
            return List.of(document);
        }
    }

    /**
     * Scenario 5: Configurable Token Text Splitting with Boundary Settings.
     * <p>
     * Instructions:
     * - Validate document != null and config != null; throw {@link IllegalArgumentException} otherwise.
     * - Construct TokenTextSplitter via TokenTextSplitter.builder():
     *   - withChunkSize(config.defaultChunkSize())
     *   - withMinChunkSizeChars(config.minChunkSizeChars())
     *   - withMinChunkLengthToEmbed(config.minChunkLengthToEmbed())
     *   - withMaxNumChunks(config.maxNumChunks())
     *   - withKeepSeparator(config.keepSeparator())
     *   - build()
     * - Return splitter.apply(List.of(document)).
     */
    public static class ConfigurableTextSplitter {
        public static List<Document> splitWithConfig(Document document, ChunkConfig config) {
            // DEFECT (Scenario 5): Returns unchunked document
            if (document == null || config == null) throw new IllegalArgumentException("Inputs must not be null");
            return List.of(document);
        }
    }

    /**
     * Scenario 6: Chunk Splitting with Sequence & Hierarchy Enrichment.
     * <p>
     * Instructions:
     * - Validate document != null and config != null; throw {@link IllegalArgumentException} otherwise.
     * - Split document using TokenTextSplitter built with config parameters.
     * - For each chunk document in the returned list (index i from 0 to size-1):
     *   - Copy chunk metadata.
     *   - Put "chunkIndex" -> i (Integer).
     *   - Put "totalChunks" -> rawChunks.size() (Integer).
     *   - Put "parentDocId" -> document.getId().
     *   - Mutate chunk: chunk.mutate().metadata(meta).build().
     * - Return list of enriched chunk documents.
     */
    public static class ChunkEnrichmentPipeline {
        public static List<Document> splitAndEnrich(Document document, ChunkConfig config) {
            // DEFECT (Scenario 6): Returns un-enriched single document
            if (document == null || config == null) throw new IllegalArgumentException("Inputs must not be null");
            return List.of(document);
        }
    }

    // =========================================================================
    // TOPIC 3: SimpleVectorStore Lifecycle Operations (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 7: SimpleVectorStore In-Memory Indexer.
     * <p>
     * Instructions:
     * - Validate embeddingModel != null and documents != null; throw {@link IllegalArgumentException} otherwise.
     * - Construct SimpleVectorStore using SimpleVectorStore.builder(embeddingModel).build().
     * - Call store.add(documents).
     * - Return the initialized vector store.
     */
    public static class VectorStoreIndexer {
        public static SimpleVectorStore createAndIndex(EmbeddingModel embeddingModel, List<Document> documents) {
            // DEFECT (Scenario 7): Returns null store
            if (embeddingModel == null || documents == null) throw new IllegalArgumentException("Inputs must not be null");
            return null;
        }
    }

    /**
     * Scenario 8: VectorStore Targeted Document Deletion.
     * <p>
     * Instructions:
     * - Validate vectorStore != null and documentIds != null; throw {@link IllegalArgumentException} otherwise.
     * - If !documentIds.isEmpty(), call vectorStore.delete(documentIds).
     */
    public static class VectorStoreDeleter {
        public static void deleteDocuments(VectorStore vectorStore, List<String> documentIds) {
            // DEFECT (Scenario 8): Does not call delete
            if (vectorStore == null || documentIds == null) throw new IllegalArgumentException("Inputs must not be null");
        }
    }

    /**
     * Scenario 9: VectorStore Synchronized Lifecycle Manager (Delete + Add).
     * <p>
     * Instructions:
     * - Validate vectorStore != null; throw {@link IllegalArgumentException} otherwise.
     * - If toDelete != null && !toDelete.isEmpty(): call vectorStore.delete(toDelete).
     * - If toAdd != null && !toAdd.isEmpty(): call vectorStore.add(toAdd).
     */
    public static class VectorStoreLifecycleManager {
        public static void syncDocuments(VectorStore vectorStore, List<String> toDelete, List<Document> toAdd) {
            // DEFECT (Scenario 9): No-op
            if (vectorStore == null) throw new IllegalArgumentException("vectorStore must not be null");
        }
    }

    // =========================================================================
    // TOPIC 4: SearchRequest & Retrieval (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: Top-K Ranked Similarity Search.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, query != null && !query.isBlank(), and topK >= 1; throw {@link IllegalArgumentException} otherwise.
     * - Build SearchRequest via SearchRequest.builder().query(query).topK(topK).build().
     * - Call vectorStore.similaritySearch(req).
     * - Map each matching Document to SearchResult:
     *   - id: doc.getId()
     *   - content: doc.getText()
     *   - metadata: doc.getMetadata()
     *   - score: doc.getScore() != null ? doc.getScore() : 0.0
     * - Return List of SearchResult.
     */
    public static class TopKSearchEngine {
        public static List<SearchResult> searchTopK(VectorStore vectorStore, String query, int topK) {
            // DEFECT (Scenario 10): Returns empty list
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            return List.of();
        }
    }

    /**
     * Scenario 11: Similarity Cutoff Threshold Filter.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, query != null && !query.isBlank(), topK >= 1, and 0.0 <= threshold <= 1.0;
     *   throw {@link IllegalArgumentException} otherwise.
     * - Build SearchRequest via SearchRequest.builder()
     *   - query(query)
     *   - topK(topK)
     *   - similarityThreshold(threshold)
     *   - build()
     * - Execute vectorStore.similaritySearch(req).
     * - Map matching Documents to List of SearchResult.
     */
    public static class ThresholdSearchEngine {
        public static List<SearchResult> searchWithThreshold(VectorStore vectorStore, String query, int topK, double threshold) {
            // DEFECT (Scenario 11): Returns empty list
            if (vectorStore == null || query == null || query.isBlank() || topK < 1 || threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            return List.of();
        }
    }

    /**
     * Scenario 12: Scored & Descending-Sorted Similarity Search.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, query != null && !query.isBlank(), topK >= 1, and 0.0 <= threshold <= 1.0;
     *   throw {@link IllegalArgumentException} otherwise.
     * - Build SearchRequest with query, topK, and similarityThreshold.
     * - Execute similaritySearch(req).
     * - Map to SearchResult list.
     * - Sort results descending by score (highest score first).
     * - Return sorted list.
     */
    public static class ScoredSearchEngine {
        public static List<SearchResult> searchScoredAndSorted(VectorStore vectorStore, String query, int topK, double threshold) {
            // DEFECT (Scenario 12): Returns empty list
            if (vectorStore == null || query == null || query.isBlank() || topK < 1 || threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            return List.of();
        }
    }

    // =========================================================================
    // TOPIC 5: FilterExpressionBuilder Basics & Comparisons (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Equality & Inequality Filter Expression Building.
     * <p>
     * Instructions:
     * - Validate field != null && !field.isBlank() and value != null; throw {@link IllegalArgumentException} otherwise.
     * - Using FilterExpressionBuilder:
     *   - If !negate: return b.eq(field, value).build().
     *   - If negate: return b.ne(field, value).build().
     */
    public static class EqualityFilterBuilder {
        public static Filter.Expression buildEqualityFilter(String field, String value, boolean negate) {
            // DEFECT (Scenario 13): Returns null expression
            if (field == null || field.isBlank() || value == null) {
                throw new IllegalArgumentException("Field and value must not be blank/null");
            }
            return null;
        }
    }

    /**
     * Scenario 14: Numeric Range Filter Expression Building.
     * <p>
     * Instructions:
     * - Validate field != null && !field.isBlank() and minValue <= maxValue; throw {@link IllegalArgumentException} otherwise.
     * - Using FilterExpressionBuilder:
     *   - Return b.and(b.gte(field, minValue), b.lte(field, maxValue)).build().
     */
    public static class NumericRangeFilterBuilder {
        public static Filter.Expression buildRangeFilter(String field, double minValue, double maxValue) {
            // DEFECT (Scenario 14): Returns null expression
            if (field == null || field.isBlank() || minValue > maxValue) {
                throw new IllegalArgumentException("Invalid range parameters");
            }
            return null;
        }
    }

    /**
     * Scenario 15: Logical Conjunctive (AND / OR) Filter Expression Building.
     * <p>
     * Instructions:
     * - Validate catField, catVal, sevField, and sevVal are all non-null and non-blank; throw {@link IllegalArgumentException} otherwise.
     * - Using FilterExpressionBuilder:
     *   - If requireBoth is true: return b.and(b.eq(catField, catVal), b.eq(sevField, sevVal)).build().
     *   - Else: return b.or(b.eq(catField, catVal), b.eq(sevField, sevVal)).build().
     */
    public static class LogicalFilterBuilder {
        public static Filter.Expression buildConjunctiveFilter(
                String catField, String catVal,
                String sevField, String sevVal,
                boolean requireBoth
        ) {
            // DEFECT (Scenario 15): Returns null expression
            if (catField == null || catField.isBlank() || catVal == null || catVal.isBlank()
                    || sevField == null || sevField.isBlank() || sevVal == null || sevVal.isBlank()) {
                throw new IllegalArgumentException("All filter fields and values must be non-blank");
            }
            return null;
        }
    }

    // =========================================================================
    // TOPIC 6: FilterExpressionBuilder Collections & Search Integration (Scenarios 16 - 18)
    // =========================================================================

    /**
     * Scenario 16: Set Containment (IN / NIN) Filter Expression Building.
     * <p>
     * Instructions:
     * - Validate field != null && !field.isBlank() and values != null && !values.isEmpty(); throw {@link IllegalArgumentException} otherwise.
     * - Using FilterExpressionBuilder:
     *   - If !negate: return b.in(field, values.toArray()).build().
     *   - If negate: return b.nin(field, values.toArray()).build().
     */
    public static class SetContainmentFilterBuilder {
        public static Filter.Expression buildInFilter(String field, List<String> values, boolean negate) {
            // DEFECT (Scenario 16): Returns null expression
            if (field == null || field.isBlank() || values == null || values.isEmpty()) {
                throw new IllegalArgumentException("Field and values must be valid");
            }
            return null;
        }
    }

    /**
     * Scenario 17: Nested Group Filter Expression Building.
     * <p>
     * Instructions:
     * - Validate env != null && !env.isBlank(), minSeverity != null && !minSeverity.isBlank(),
     *   and targetTeams != null && !targetTeams.isEmpty(); throw {@link IllegalArgumentException} otherwise.
     * - Construct the nested expression representing:
     *   (environment == env) AND ((severity == minSeverity) OR (team IN targetTeams))
     * - Using FilterExpressionBuilder:
     *   return b.and(
     *       b.eq("environment", env),
     *       b.or(b.eq("severity", minSeverity), b.in("team", targetTeams.toArray()))
     *   ).build().
     */
    public static class NestedFilterBuilder {
        public static Filter.Expression buildAuditFilter(String env, String minSeverity, List<String> targetTeams) {
            // DEFECT (Scenario 17): Returns null expression
            if (env == null || env.isBlank() || minSeverity == null || minSeverity.isBlank() || targetTeams == null || targetTeams.isEmpty()) {
                throw new IllegalArgumentException("Inputs must not be null or blank");
            }
            return null;
        }
    }

    /**
     * Scenario 18: Integrated Metadata Filtered Similarity Search.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, query != null && !query.isBlank(), filterExpression != null, and topK >= 1;
     *   throw {@link IllegalArgumentException} otherwise.
     * - Build SearchRequest via SearchRequest.builder()
     *   - query(query)
     *   - topK(topK)
     *   - filterExpression(filterExpression)
     *   - build()
     * - Call vectorStore.similaritySearch(req).
     * - Map returned documents to List of SearchResult.
     * - Return results.
     */
    public static class FilteredSearchGateway {
        public static List<SearchResult> searchWithFilter(
                VectorStore vectorStore,
                String query,
                Filter.Expression filterExpression,
                int topK
        ) {
            // DEFECT (Scenario 18): Returns empty list without executing filtered search
            if (vectorStore == null || query == null || query.isBlank() || filterExpression == null || topK < 1) {
                throw new IllegalArgumentException("Invalid search arguments");
            }
            return List.of();
        }
    }
}
