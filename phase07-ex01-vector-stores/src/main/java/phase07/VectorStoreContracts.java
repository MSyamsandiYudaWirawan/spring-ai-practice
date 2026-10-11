package phase07;

import java.util.*;

/**
 * Immutable domain records and configurations for Phase 07 Exercise 01:
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
     * Raw article entity used for batch document creation drills.
     */
    public record RawArticle(
            String id,
            String text,
            String source,
            Map<String, Object> metadata
    ) {
        public RawArticle {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(text, "text must not be null");
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }
    }
}
