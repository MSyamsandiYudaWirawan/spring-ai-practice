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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Exercise implementation under test for Phase 07 Exercise 01:
 * Vector Stores, In-Memory Embeddings, Document Chunking, Semantic Filtering and MMR.
 * <p>
 * Students implement all 12 scenarios in this file.
 */
public class VectorStoreUnderTest {

    /**
     * Scenario 1: Document Creation & Metadata Normalization.
     * <p>
     * Instructions:
     * - Validate that id and content are not null and not blank; throw {@link IllegalArgumentException} otherwise.
     * - Trim whitespace from content.
     * - Create a mutable copy of the provided metadata (or an empty map if null).
     * - Enforce required metadata:
     *   - Put "charCount" mapped to (long) content.length().
     *   - If "createdAt" is absent, put Instant.now().toString() (or a fixed timestamp).
     * - Build and return immutable Document via {@link Document#builder()}.
     */
    public static class DocumentFactory {
        public static Document createDocument(String id, String content, Map<String, Object> metadata) {
            // DEFECT (Scenario 1): Blindly returns null without validation
            return null;
        }
    }

    /**
     * Scenario 2: Token-Based Document Chunking Pipeline.
     * <p>
     * Instructions:
     * - Validate document != null and config != null; throw {@link IllegalArgumentException} otherwise.
     * - Construct a {@link TokenTextSplitter} using {@link TokenTextSplitter#builder()}:
     *   - withChunkSize(config.defaultChunkSize())
     *   - withMinChunkSizeChars(config.minChunkSizeChars())
     *   - withMinChunkLengthToEmbed(config.minChunkLengthToEmbed())
     *   - withMaxNumChunks(config.maxNumChunks())
     *   - withKeepSeparator(config.keepSeparator())
     *   - build()
     * - Split document using splitter.apply(List.of(document)).
     * - For each chunk document in the returned list, enrich its metadata with:
     *   - "chunkIndex" -> index (0-based integer)
     *   - "totalChunks" -> total chunks count (integer)
     * - Return list of enriched chunk documents.
     */
    public static class TextChunkingPipeline {
        public static List<Document> splitDocument(Document document, ChunkConfig config) {
            // DEFECT (Scenario 2): Returns original document unchunked
            if (document == null || config == null) throw new IllegalArgumentException("Inputs must not be null");
            return List.of(document);
        }
    }

    /**
     * Scenario 3: Deterministic Unit Vector Generation & L2 Normalization.
     * <p>
     * Instructions:
     * - Validate dimensions >= 2; throw {@link IllegalArgumentException} otherwise.
     * - Given input text, generate a float array of length dimensions:
     *   - If text is null or blank, return a vector where index 0 is 1.0f and other elements are 0.0f.
     *   - Otherwise, split text into words via regex {@code [^a-zA-Z0-9]+}.
     *   - For each non-blank token, seed a {@link Random} with (long) token.hashCode().
     *   - Add (rand.nextFloat() * 2.0f) - 1.0f to each vector component.
     * - Compute L2 norm: sqrt(sum(v_i * v_i)).
     * - If norm > 1e-6f, divide each element by norm to achieve unit length (||v|| == 1.0f).
     * - Otherwise set index 0 to 1.0f.
     * - Return the normalized float array.
     */
    public static class DeterministicVectorGenerator {
        public static float[] generateNormalizedVector(String text, int dimensions) {
            // DEFECT (Scenario 3): Returns unnormalized zeros
            if (dimensions < 2) throw new IllegalArgumentException("dimensions must be >= 2");
            return new float[dimensions];
        }
    }

    /**
     * Scenario 4: SimpleVectorStore In-Memory Indexer.
     * <p>
     * Instructions:
     * - Validate embeddingModel != null and documents != null; throw {@link IllegalArgumentException} otherwise.
     * - Construct SimpleVectorStore using {@link SimpleVectorStore#builder(EmbeddingModel)}.
     * - Call store.add(documents).
     * - Return the initialized vector store.
     */
    public static class VectorStoreIndexer {
        public static SimpleVectorStore createAndIndex(EmbeddingModel embeddingModel, List<Document> documents) {
            // DEFECT (Scenario 4): Returns unindexed store
            if (embeddingModel == null || documents == null) throw new IllegalArgumentException("Inputs must not be null");
            return null;
        }
    }

    /**
     * Scenario 5: Top-K Ranked Semantic Similarity Search.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, query != null && !query.isBlank(), and topK >= 1; throw {@link IllegalArgumentException} otherwise.
     * - Build {@link SearchRequest} via {@link SearchRequest#builder()}:
     *   - query(query)
     *   - topK(topK)
     *   - build()
     * - Call vectorStore.similaritySearch(request).
     * - Map each matching {@link Document} to a {@link SearchResult}:
     *   - id: doc.getId()
     *   - content: doc.getText()
     *   - metadata: doc.getMetadata()
     *   - score: doc.getScore() != null ? doc.getScore() : 0.0
     * - Return List of SearchResult.
     */
    public static class SimilaritySearchEngine {
        public static List<SearchResult> searchTopK(VectorStore vectorStore, String query, int topK) {
            // DEFECT (Scenario 5): Returns empty list
            if (vectorStore == null || query == null || query.isBlank() || topK < 1) {
                throw new IllegalArgumentException("Invalid search parameters");
            }
            return List.of();
        }
    }

    /**
     * Scenario 6: Cutoff Score Threshold Similarity Filter.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, query != null && !query.isBlank(), topK >= 1, and 0.0 <= minThreshold <= 1.0; throw {@link IllegalArgumentException} otherwise.
     * - Build SearchRequest with similarityThreshold(minThreshold).
     * - Execute similaritySearch(request).
     * - Convert returned documents to List<SearchResult>.
     * - Return matching SearchResults.
     */
    public static class ThresholdSimilarityFilter {
        public static List<SearchResult> searchWithThreshold(
                VectorStore vectorStore,
                String query,
                int topK,
                double minThreshold
        ) {
            // DEFECT (Scenario 6): Ignores threshold parameter
            if (minThreshold < 0.0 || minThreshold > 1.0) {
                throw new IllegalArgumentException("minThreshold must be between 0.0 and 1.0");
            }
            return List.of();
        }
    }

    /**
     * Scenario 7: Metadata Filter Expression Search.
     * <p>
     * Instructions:
     * - Validate targetCategory and targetSeverity are not null and not blank; throw {@link IllegalArgumentException} otherwise.
     * - Use {@link FilterExpressionBuilder}:
     *   FilterExpressionBuilder b = new FilterExpressionBuilder();
     *   Filter.Expression expr = b.and(b.eq("category", targetCategory), b.eq("severity", targetSeverity)).build();
     * - Build SearchRequest with query, topK, and filterExpression(expr).
     * - Execute vectorStore.similaritySearch(request).
     * - Convert returned documents to List<SearchResult>.
     * - Return matching SearchResults.
     */
    public static class MetadataExpressionFilter {
        public static List<SearchResult> searchWithMetadataFilter(
                VectorStore vectorStore,
                String query,
                String targetCategory,
                String targetSeverity,
                int topK
        ) {
            // DEFECT (Scenario 7): Returns empty list without building filter expression
            if (targetCategory == null || targetCategory.isBlank() || targetSeverity == null || targetSeverity.isBlank()) {
                throw new IllegalArgumentException("Category and severity must be non-blank");
            }
            return List.of();
        }
    }

    /**
     * Scenario 8: Document Lifecycle Management (Delete & Re-Index).
     * <p>
     * Instructions:
     * - Validate vectorStore != null; throw {@link IllegalArgumentException} otherwise.
     * - If documentIdsToDelete != null && !documentIdsToDelete.isEmpty():
     *   - Call vectorStore.delete(documentIdsToDelete).
     * - If newDocuments != null && !newDocuments.isEmpty():
     *   - Call vectorStore.add(newDocuments).
     */
    public static class DocumentLifecycleManager {
        public static void deleteAndReindex(
                VectorStore vectorStore,
                List<String> documentIdsToDelete,
                List<Document> newDocuments
        ) {
            // DEFECT (Scenario 8): Does not delete or add documents
            if (vectorStore == null) throw new IllegalArgumentException("vectorStore must not be null");
        }
    }

    /**
     * Scenario 9: Cached In-Memory Embedding Model Decorator.
     * <p>
     * Instructions:
     * - Wrap the delegate {@link EmbeddingModel} with an in-memory cache (e.g. {@link ConcurrentHashMap}).
     * - Maintain atomic counters for cache hits and misses.
     * - In embed(String text):
     *   - If text is in cache: increment hits, return cached float[].
     *   - Else: increment misses, call delegate.embed(text), store in cache, return float[].
     * - In call(EmbeddingRequest request):
     *   - For each instruction text in request, resolve through cache (delegating missing entries).
     * - Provide getStats() returning {@link CacheStats}.
     */
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
            // DEFECT (Scenario 9): Always delegates without caching
            return delegate.embed(text);
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
            // DEFECT (Scenario 9): Blindly delegates request
            return delegate.call(request);
        }

        @Override
        public int dimensions() {
            return delegate.dimensions();
        }
    }

    /**
     * Scenario 10: End-to-End Knowledge Ingestion & Verification Gateway.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, rawItems != null && !rawItems.isEmpty(), chunkConfig != null; throw {@link IllegalArgumentException} otherwise.
     * - For each RawKnowledgeItem in rawItems:
     *   - Build Document with id, fullText (item.title() + "\n" + item.content()), and metadata (merging "title" -> item.title()).
     *   - Split document using {@link TextChunkingPipeline#splitDocument(Document, ChunkConfig)}.
     *   - Collect all resulting chunk documents.
     * - Index all chunks into vectorStore via vectorStore.add(allChunks).
     * - Verify knowledge accessibility:
     *   - Perform threshold search: {@link ThresholdSimilarityFilter#searchWithThreshold(VectorStore, String, int, double)}
     *     with testQuery, topK = 5, and minThreshold.
     *   - If search returns 0 results:
     *     throw new {@link VectorStoreBreachException}("VectorStore constraint breached: knowledge verification query yielded zero matches for query: " + testQuery);
     * - Return {@link IngestionReport} with rawItemCount = rawItems.size(), chunkCount = allChunks.size(), indexedCount = allChunks.size(), and list of chunk IDs.
     */
    public static class KnowledgeIngestionGateway {
        public static IngestionReport ingestAndVerify(
                VectorStore vectorStore,
                List<RawKnowledgeItem> rawItems,
                ChunkConfig chunkConfig,
                String testQuery,
                double minThreshold
        ) {
            // DEFECT (Scenario 10): Returns empty report without indexing or verification
            if (vectorStore == null || rawItems == null || rawItems.isEmpty() || chunkConfig == null) {
                throw new IllegalArgumentException("Invalid gateway parameters");
            }
            return new IngestionReport(0, 0, 0, List.of());
        }
    }

    /**
     * Scenario 11: Maximal Marginal Relevance (MMR) Diversity Search Engine.
     * <p>
     * Instructions:
     * - Validate vectorStore != null, embeddingModel != null, query != null && !query.isBlank(), and config != null;
     *   throw {@link IllegalArgumentException} otherwise.
     * - Generate query embedding: float[] queryEmbedding = embeddingModel.embed(query).
     * - Determine candidate pool size: candidateK = config.topK() * config.candidateFetchMultiplier().
     * - Retrieve candidate documents via vectorStore.similaritySearch(
     *     SearchRequest.builder().query(query).topK(candidateK).build()
     *   ).
     * - If candidates is empty, return List.of().
     * - For each candidate document, resolve its embedding vector (float[]) via embeddingModel.embed(doc.getText()).
     * - Helper cosine similarity between float[] u and v:
     *   dotProduct / (sqrt(sum(u_i^2)) * sqrt(sum(v_i^2))). Return 0.0 if either norm is 0.
     * - Run greedy MMR selection loop:
     *   - Maintain List<Document> selected = new ArrayList<>()
     *   - Maintain List<Document> remaining = new ArrayList<>(candidates)
     *   - Target count = Math.min(config.topK(), candidates.size())
     *   - While selected.size() < targetCount and !remaining.isEmpty():
     *     - For each candidate d in remaining:
     *       - simToQuery = cosineSimilarity(emb(d), queryEmbedding)
     *       - maxSimToSelected = 0.0
     *       - For each s in selected:
     *         maxSimToSelected = Math.max(maxSimToSelected, cosineSimilarity(emb(d), emb(s)))
     *       - mmrScore = config.lambda() * simToQuery - (1.0 - config.lambda()) * maxSimToSelected
     *     - Pick candidate with highest mmrScore. Transfer from remaining to selected.
     * - Map selected documents to List<SearchResult> using doc.getId(), doc.getText(), doc.getMetadata(),
     *   and score = cosineSimilarity(emb(doc), queryEmbedding).
     * - Return list of SearchResult.
     */
    public static class MaximalMarginalRelevanceSearchEngine {
        public static List<SearchResult> searchMmr(
                VectorStore vectorStore,
                EmbeddingModel embeddingModel,
                String query,
                MmrConfig config
        ) {
            // DEFECT (Scenario 11): Returns empty list without computing MMR
            if (vectorStore == null || embeddingModel == null || query == null || query.isBlank() || config == null) {
                throw new IllegalArgumentException("Inputs must not be null or blank");
            }
            return List.of();
        }
    }

    /**
     * Scenario 12: Content-Hash Ingestion Deduplication & Compaction Pipeline.
     * <p>
     * Instructions:
     * - Validate vectorStore != null and incomingDocuments != null; throw {@link IllegalArgumentException} otherwise.
     * - Maintain an internal thread-safe set of known content hashes (e.g. Set<String> indexedHashes = ConcurrentHashMap.newKeySet()).
     * - For each Document doc in incomingDocuments:
     *   - Normalize content: doc.getText().trim().toLowerCase().
     *   - Compute SHA-256 hex digest of normalized content.
     *   - If indexedHashes contains the digest:
     *     - Increment duplicateSkippedCount.
     *   - Else:
     *     - Register digest into indexedHashes.
     *     - Enrich document metadata with "contentHash" -> digest and "indexedAt" -> Instant.now().toString().
     *     - Add enriched document to toIndex list.
     *     - Increment newOrUpdatedCount.
     * - If !toIndex.isEmpty():
     *   - Call vectorStore.add(toIndex).
     * - Return DedupReport(incomingDocuments.size(), newOrUpdatedCount, duplicateSkippedCount, indexedDocIds).
     */
    public static class ContentHashDeduplicationPipeline {
        private final Set<String> indexedHashes = ConcurrentHashMap.newKeySet();

        public DedupReport ingestWithDeduplication(VectorStore vectorStore, List<Document> incomingDocuments) {
            // DEFECT (Scenario 12): Returns empty report without indexing
            if (vectorStore == null || incomingDocuments == null) {
                throw new IllegalArgumentException("Inputs must not be null");
            }
            return new DedupReport(incomingDocuments.size(), 0, incomingDocuments.size(), List.of());
        }

        public Set<String> getIndexedHashes() {
            return Collections.unmodifiableSet(indexedHashes);
        }
    }
}
