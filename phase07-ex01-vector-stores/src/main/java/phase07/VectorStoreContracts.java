package phase07;

import org.springframework.ai.document.Document;

import java.util.*;

/**
 * Immutable domain records, configurations, and exception contracts for Phase 07 Exercise 01:
 * Vector Stores, In-Memory Embeddings, Document Chunking and Semantic Filtering.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class VectorStoreContracts {

    private VectorStoreContracts() {}

    /**
     * Chunk configuration for TokenTextSplitter.
     */
    public record ChunkConfig(
            int defaultChunkSize,
            int minChunkSizeChars,
            int minChunkLengthToEmbed,
            int maxNumChunks,
            boolean keepSeparator
    ) {
        public ChunkConfig {
            if (defaultChunkSize < 1) throw new IllegalArgumentException("defaultChunkSize must be >= 1");
            if (minChunkSizeChars < 1) throw new IllegalArgumentException("minChunkSizeChars must be >= 1");
            if (maxNumChunks < 1) throw new IllegalArgumentException("maxNumChunks must be >= 1");
        }
    }

    /**
     * Unified search result entity wrapping retrieved Document content and score.
     */
    public record SearchResult(
            String id,
            String content,
            Map<String, Object> metadata,
            double score
    ) {
        public SearchResult {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(content, "content must not be null");
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }
    }

    /**
     * Cache hit/miss statistics for embedding cache layer.
     */
    public record CacheStats(
            long hits,
            long misses
    ) {}

    /**
     * Raw knowledge article input for the ingestion pipeline.
     */
    public record RawKnowledgeItem(
            String id,
            String title,
            String content,
            Map<String, Object> metadata
    ) {
        public RawKnowledgeItem {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(content, "content must not be null");
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }
    }

    /**
     * Summary report emitted by the knowledge ingestion gateway.
     */
    public record IngestionReport(
            int rawItemCount,
            int chunkCount,
            int indexedCount,
            List<String> chunkIds
    ) {
        public IngestionReport {
            chunkIds = chunkIds != null ? List.copyOf(chunkIds) : List.of();
        }
    }

    /**
     * Configuration for Maximal Marginal Relevance (MMR) diversity search.
     */
    public record MmrConfig(
            int topK,
            double lambda,
            int candidateFetchMultiplier
    ) {
        public MmrConfig {
            if (topK < 1) throw new IllegalArgumentException("topK must be >= 1");
            if (lambda < 0.0 || lambda > 1.0) throw new IllegalArgumentException("lambda must be between 0.0 and 1.0");
            if (candidateFetchMultiplier < 1) throw new IllegalArgumentException("candidateFetchMultiplier must be >= 1");
        }
    }

    /**
     * Audit report emitted by the content-hash deduplication ingestion pipeline.
     */
    public record DedupReport(
            int totalSubmitted,
            int newOrUpdatedCount,
            int duplicateSkippedCount,
            List<String> indexedIds
    ) {
        public DedupReport {
            indexedIds = indexedIds != null ? List.copyOf(indexedIds) : List.of();
        }
    }

    /**
     * Exception thrown when vector store integrity or search verification gates fail.
     * <p>
     * Contract requirement: Message MUST contain "VectorStore constraint breached" (case-insensitive).
     */
    public static class VectorStoreBreachException extends RuntimeException {
        public VectorStoreBreachException(String message) {
            super(message);
        }
    }
}
