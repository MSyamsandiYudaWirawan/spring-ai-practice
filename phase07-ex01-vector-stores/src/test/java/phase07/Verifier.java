package phase07;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import phase07.VectorStoreContracts.*;
import phase07.VectorStoreUnderTest.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 07 Exercise 01:
 * Vector Stores, In-Memory Embeddings, Document Chunking and Semantic Filtering.
 * <p>
 * 18 repetitive muscle-memory drills across 6 core framework topics (3 repetitions each).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {}

    public static void main(String[] args) {
        int selectedScenario = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selectedScenario = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selectedScenario);
        printReport(results);

        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @Test
    public void verifyAll() {
        List<ScenarioResult> results = runScenarios(0);
        printReport(results);
        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        if (!allPassed) {
            fail("Verifier detected scenario failures. See printed report above.");
        }
    }

    public static List<ScenarioResult> runScenarios(int selected) {
        List<ScenarioResult> list = new ArrayList<>();
        if (selected == 0 || selected == 1) list.add(verifyScenario1());
        if (selected == 0 || selected == 2) list.add(verifyScenario2());
        if (selected == 0 || selected == 3) list.add(verifyScenario3());
        if (selected == 0 || selected == 4) list.add(verifyScenario4());
        if (selected == 0 || selected == 5) list.add(verifyScenario5());
        if (selected == 0 || selected == 6) list.add(verifyScenario6());
        if (selected == 0 || selected == 7) list.add(verifyScenario7());
        if (selected == 0 || selected == 8) list.add(verifyScenario8());
        if (selected == 0 || selected == 9) list.add(verifyScenario9());
        if (selected == 0 || selected == 10) list.add(verifyScenario10());
        if (selected == 0 || selected == 11) list.add(verifyScenario11());
        if (selected == 0 || selected == 12) list.add(verifyScenario12());
        if (selected == 0 || selected == 13) list.add(verifyScenario13());
        if (selected == 0 || selected == 14) list.add(verifyScenario14());
        if (selected == 0 || selected == 15) list.add(verifyScenario15());
        if (selected == 0 || selected == 16) list.add(verifyScenario16());
        if (selected == 0 || selected == 17) list.add(verifyScenario17());
        if (selected == 0 || selected == 18) list.add(verifyScenario18());
        return list;
    }

    // =========================================================================
    // TOPIC 1: Document Creation & Mutation (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1() {
        String name = "DocumentFactory (Normalized Creation & Metadata Enrichment)";
        try {
            try {
                DocumentFactory.createDocument("", "content", Map.of());
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on blank id");
            } catch (IllegalArgumentException expected) {}

            try {
                DocumentFactory.createDocument("id-1", "   ", Map.of());
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on blank content");
            } catch (IllegalArgumentException expected) {}

            Document doc = DocumentFactory.createDocument("doc-1", " PostgreSQL pool saturation. ", Map.of("env", "PROD"));
            if (doc == null) {
                return new ScenarioResult(1, name, false, "DocumentFactory returned null document");
            }
            if (!"doc-1".equals(doc.getId())) {
                return new ScenarioResult(1, name, false, "Expected doc id 'doc-1', got: " + doc.getId());
            }
            if (!"PostgreSQL pool saturation.".equals(doc.getText())) {
                return new ScenarioResult(1, name, false, "Expected trimmed content, got: '" + doc.getText() + "'");
            }
            if (!"PROD".equals(doc.getMetadata().get("env"))) {
                return new ScenarioResult(1, name, false, "Metadata 'env' missing or incorrect");
            }
            if (!(doc.getMetadata().get("charCount") instanceof Long cc && cc == 27L)) {
                return new ScenarioResult(1, name, false, "Metadata 'charCount' expected 27L, got: " + doc.getMetadata().get("charCount"));
            }
            if (!doc.getMetadata().containsKey("createdAt") || doc.getMetadata().get("createdAt") == null) {
                return new ScenarioResult(1, name, false, "Metadata 'createdAt' must be populated");
            }
            return new ScenarioResult(1, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "DocumentEnricher (Document Mutation via mutate())";
        try {
            try {
                DocumentEnricher.enrichDocument(null, "prod", "v1.0");
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException on null document");
            } catch (IllegalArgumentException expected) {}

            try {
                Document base = Document.builder().id("d1").text("content").build();
                DocumentEnricher.enrichDocument(base, "  ", "v1.0");
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException on blank environment");
            } catch (IllegalArgumentException expected) {}

            Document baseDoc = Document.builder()
                    .id("base-1")
                    .text("High lock contention on table orders.")
                    .metadata(Map.of("cluster", "eu-central"))
                    .build();

            Document enriched = DocumentEnricher.enrichDocument(baseDoc, " prod ", "2.4.1");
            if (enriched == null) {
                return new ScenarioResult(2, name, false, "DocumentEnricher returned null");
            }
            if (!"base-1".equals(enriched.getId())) {
                return new ScenarioResult(2, name, false, "Enriched document id altered");
            }
            if (!"PROD".equals(enriched.getMetadata().get("environment"))) {
                return new ScenarioResult(2, name, false, "Expected environment 'PROD', got: " + enriched.getMetadata().get("environment"));
            }
            if (!"2.4.1".equals(enriched.getMetadata().get("version"))) {
                return new ScenarioResult(2, name, false, "Expected version '2.4.1', got: " + enriched.getMetadata().get("version"));
            }
            if (!enriched.getMetadata().containsKey("enrichedAt")) {
                return new ScenarioResult(2, name, false, "Expected enrichedAt metadata timestamp");
            }
            if (!"eu-central".equals(enriched.getMetadata().get("cluster"))) {
                return new ScenarioResult(2, name, false, "Existing metadata 'cluster' was dropped");
            }
            // Ensure original document is not mutated
            if (baseDoc.getMetadata().containsKey("environment")) {
                return new ScenarioResult(2, name, false, "Original document metadata was mutated directly");
            }
            return new ScenarioResult(2, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "DocumentBatchBuilder (Batch Creation from Domain Entities)";
        try {
            try {
                DocumentBatchBuilder.buildBatch(null);
                return new ScenarioResult(3, name, false, "Expected IllegalArgumentException on null articles");
            } catch (IllegalArgumentException expected) {}

            try {
                DocumentBatchBuilder.buildBatch(List.of());
                return new ScenarioResult(3, name, false, "Expected IllegalArgumentException on empty articles");
            } catch (IllegalArgumentException expected) {}

            List<RawArticle> articles = List.of(
                    new RawArticle("art-1", "  Latency spike in connection pool  ", "datadog", Map.of("severity", "HIGH")),
                    new RawArticle("art-2", "Redis cache eviction cascade", null, Map.of("severity", "MEDIUM"))
            );

            List<Document> batch = DocumentBatchBuilder.buildBatch(articles);
            if (batch == null || batch.size() != 2) {
                return new ScenarioResult(3, name, false, "Expected batch of 2 documents, got: " + (batch == null ? "null" : batch.size()));
            }

            Document d1 = batch.get(0);
            if (!"art-1".equals(d1.getId()) || !"Latency spike in connection pool".equals(d1.getText())) {
                return new ScenarioResult(3, name, false, "Document 1 id or trimmed text mismatch");
            }
            if (!(d1.getMetadata().get("wordCount") instanceof Long wc && wc == 5L)) {
                return new ScenarioResult(3, name, false, "Document 1 expected wordCount=5L, got: " + d1.getMetadata().get("wordCount"));
            }
            if (!"datadog".equals(d1.getMetadata().get("source"))) {
                return new ScenarioResult(3, name, false, "Document 1 source mismatch");
            }
            if (!Boolean.TRUE.equals(d1.getMetadata().get("indexed"))) {
                return new ScenarioResult(3, name, false, "Document 1 indexed flag missing");
            }

            Document d2 = batch.get(1);
            if (!"unknown".equals(d2.getMetadata().get("source"))) {
                return new ScenarioResult(3, name, false, "Document 2 null source should default to 'unknown', got: " + d2.getMetadata().get("source"));
            }
            if (!(d2.getMetadata().get("wordCount") instanceof Long wc2 && wc2 == 4L)) {
                return new ScenarioResult(3, name, false, "Document 2 expected wordCount=4L, got: " + d2.getMetadata().get("wordCount"));
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 2: TokenTextSplitter Chunking & Enrichment (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4() {
        String name = "BasicTextSplitter (Token Chunking with ChunkSize)";
        try {
            try {
                BasicTextSplitter.split(null, 10);
                return new ScenarioResult(4, name, false, "Expected IllegalArgumentException on null document");
            } catch (IllegalArgumentException expected) {}

            try {
                Document doc = Document.builder().id("d").text("hello").build();
                BasicTextSplitter.split(doc, 0);
                return new ScenarioResult(4, name, false, "Expected IllegalArgumentException on chunkSize < 1");
            } catch (IllegalArgumentException expected) {}

            String longContent = "Spring AI provides an abstraction over vector databases to support RAG workflows. ".repeat(25);
            Document doc = Document.builder().id("long-doc").text(longContent).build();

            List<Document> chunks = BasicTextSplitter.split(doc, 30);
            if (chunks == null || chunks.size() < 2) {
                return new ScenarioResult(4, name, false, "Expected document to be split into >= 2 chunks, got: " + (chunks == null ? "null" : chunks.size()));
            }
            for (Document c : chunks) {
                if (c.getText() == null || c.getText().isBlank()) {
                    return new ScenarioResult(4, name, false, "Encountered empty text in split chunk");
                }
            }
            return new ScenarioResult(4, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "ConfigurableTextSplitter (Multi-Parameter Token Splitter)";
        try {
            try {
                ConfigurableTextSplitter.splitWithConfig(null, new ChunkConfig(50, 10, 5, 20, true));
                return new ScenarioResult(5, name, false, "Expected IllegalArgumentException on null document");
            } catch (IllegalArgumentException expected) {}

            try {
                Document doc = Document.builder().id("d").text("hello").build();
                ConfigurableTextSplitter.splitWithConfig(doc, null);
                return new ScenarioResult(5, name, false, "Expected IllegalArgumentException on null config");
            } catch (IllegalArgumentException expected) {}

            String longContent = "The Quick brown fox jumps over the lazy dog in diagnostic test clusters. ".repeat(30);
            Document doc = Document.builder().id("doc-cfg").text(longContent).build();
            ChunkConfig config = new ChunkConfig(40, 15, 5, 50, true);

            List<Document> chunks = ConfigurableTextSplitter.splitWithConfig(doc, config);
            if (chunks == null || chunks.size() < 2) {
                return new ScenarioResult(5, name, false, "Expected >= 2 chunks, got: " + (chunks == null ? "null" : chunks.size()));
            }
            return new ScenarioResult(5, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "ChunkEnrichmentPipeline (Splitting & Sequence Index Tagging)";
        try {
            try {
                ChunkEnrichmentPipeline.splitAndEnrich(null, new ChunkConfig(50, 10, 5, 20, true));
                return new ScenarioResult(6, name, false, "Expected IllegalArgumentException on null document");
            } catch (IllegalArgumentException expected) {}

            String longContent = "Autonomous agents inspect production logs to discover systemic resource leaks. ".repeat(25);
            Document parent = Document.builder().id("parent-root").text(longContent).build();
            ChunkConfig config = new ChunkConfig(35, 10, 5, 30, true);

            List<Document> chunks = ChunkEnrichmentPipeline.splitAndEnrich(parent, config);
            if (chunks == null || chunks.size() < 2) {
                return new ScenarioResult(6, name, false, "Expected >= 2 enriched chunks, got: " + (chunks == null ? "null" : chunks.size()));
            }

            int total = chunks.size();
            for (int i = 0; i < total; i++) {
                Document c = chunks.get(i);
                if (!Integer.valueOf(i).equals(c.getMetadata().get("chunkIndex"))) {
                    return new ScenarioResult(6, name, false, "Chunk " + i + " expected chunkIndex=" + i + ", got: " + c.getMetadata().get("chunkIndex"));
                }
                if (!Integer.valueOf(total).equals(c.getMetadata().get("totalChunks"))) {
                    return new ScenarioResult(6, name, false, "Chunk " + i + " expected totalChunks=" + total + ", got: " + c.getMetadata().get("totalChunks"));
                }
                if (!"parent-root".equals(c.getMetadata().get("parentDocId"))) {
                    return new ScenarioResult(6, name, false, "Chunk " + i + " expected parentDocId='parent-root', got: " + c.getMetadata().get("parentDocId"));
                }
            }
            return new ScenarioResult(6, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 3: SimpleVectorStore Lifecycle Operations (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7() {
        String name = "VectorStoreIndexer (SimpleVectorStore Creation & Indexing)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            try {
                VectorStoreIndexer.createAndIndex(null, List.of());
                return new ScenarioResult(7, name, false, "Expected IllegalArgumentException on null model");
            } catch (IllegalArgumentException expected) {}

            List<Document> docs = List.of(
                    Document.builder().id("d1").text("Postgres connection timeout").build(),
                    Document.builder().id("d2").text("Garbage collection pause").build()
            );

            SimpleVectorStore store = VectorStoreIndexer.createAndIndex(fakeModel, docs);
            if (store == null) {
                return new ScenarioResult(7, name, false, "VectorStoreIndexer returned null store");
            }

            List<Document> found = store.similaritySearch(SearchRequest.builder().query("Postgres").topK(1).build());
            if (found.isEmpty() || !"d1".equals(found.get(0).getId())) {
                return new ScenarioResult(7, name, false, "Expected indexed document 'd1' to be retrieved");
            }
            return new ScenarioResult(7, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "VectorStoreDeleter (Targeted Document Deletion)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("keep-1").text("Memory leak in netty buffers").build(),
                    Document.builder().id("drop-1").text("Deadlock detected on account balance table").build()
            ));

            try {
                VectorStoreDeleter.deleteDocuments(null, List.of("drop-1"));
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException on null vectorStore");
            } catch (IllegalArgumentException expected) {}

            VectorStoreDeleter.deleteDocuments(store, List.of("drop-1"));

            List<Document> all = store.similaritySearch(SearchRequest.builder().query("Deadlock").topK(5).build());
            boolean dropFound = all.stream().anyMatch(d -> "drop-1".equals(d.getId()));
            if (dropFound) {
                return new ScenarioResult(8, name, false, "Deleted document 'drop-1' still exists in vector store");
            }
            return new ScenarioResult(8, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "VectorStoreLifecycleManager (Synchronized Sync Deletion & Addition)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("doc-stale").text("Old deployment config").build(),
                    Document.builder().id("doc-retain").text("Retained service catalog").build()
            ));

            try {
                VectorStoreLifecycleManager.syncDocuments(null, List.of(), List.of());
                return new ScenarioResult(9, name, false, "Expected IllegalArgumentException on null store");
            } catch (IllegalArgumentException expected) {}

            Document docNew = Document.builder().id("doc-fresh").text("Fresh deployment v2").build();
            VectorStoreLifecycleManager.syncDocuments(store, List.of("doc-stale"), List.of(docNew));

            List<Document> freshSearch = store.similaritySearch(SearchRequest.builder().query("Fresh deployment").topK(5).build());
            boolean staleFound = freshSearch.stream().anyMatch(d -> "doc-stale".equals(d.getId()));
            boolean freshFound = freshSearch.stream().anyMatch(d -> "doc-fresh".equals(d.getId()));

            List<Document> retainSearch = store.similaritySearch(SearchRequest.builder().query("Retained service catalog").topK(5).build());
            boolean retainFound = retainSearch.stream().anyMatch(d -> "doc-retain".equals(d.getId()));

            if (staleFound) {
                return new ScenarioResult(9, name, false, "Stale document 'doc-stale' was not deleted");
            }
            if (!freshFound) {
                return new ScenarioResult(9, name, false, "New document 'doc-fresh' was not added");
            }
            if (!retainFound) {
                return new ScenarioResult(9, name, false, "Retained document 'doc-retain' was erroneously deleted");
            }
            return new ScenarioResult(9, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 4: SearchRequest & Retrieval (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10() {
        String name = "TopKSearchEngine (Top-K Ranked Search & DTO Mapping)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("r1").text("Disk I/O bottleneck").metadata(Map.of("host", "node-1")).build(),
                    Document.builder().id("r2").text("Disk I/O read failure").metadata(Map.of("host", "node-2")).build(),
                    Document.builder().id("r3").text("CPU throttling on worker").metadata(Map.of("host", "node-3")).build()
            ));

            try {
                TopKSearchEngine.searchTopK(store, "   ", 2);
                return new ScenarioResult(10, name, false, "Expected IllegalArgumentException on blank query");
            } catch (IllegalArgumentException expected) {}

            try {
                TopKSearchEngine.searchTopK(store, "Disk", 0);
                return new ScenarioResult(10, name, false, "Expected IllegalArgumentException on topK < 1");
            } catch (IllegalArgumentException expected) {}

            List<SearchResult> results = TopKSearchEngine.searchTopK(store, "Disk I/O", 2);
            if (results == null || results.size() != 2) {
                return new ScenarioResult(10, name, false, "Expected exactly 2 top-k results, got: " + (results == null ? "null" : results.size()));
            }

            for (SearchResult sr : results) {
                if (sr.id() == null || sr.content() == null || sr.metadata() == null) {
                    return new ScenarioResult(10, name, false, "SearchResult fields must not be null");
                }
            }
            return new ScenarioResult(10, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "ThresholdSearchEngine (Similarity Threshold Gating)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("exact").text("Database deadlock detected in lock manager").build(),
                    Document.builder().id("unrelated").text("Front-end css styling color scheme").build()
            ));

            try {
                ThresholdSearchEngine.searchWithThreshold(store, "deadlock", 5, -0.1);
                return new ScenarioResult(11, name, false, "Expected IllegalArgumentException on negative threshold");
            } catch (IllegalArgumentException expected) {}

            try {
                ThresholdSearchEngine.searchWithThreshold(store, "deadlock", 5, 1.5);
                return new ScenarioResult(11, name, false, "Expected IllegalArgumentException on threshold > 1.0");
            } catch (IllegalArgumentException expected) {}

            List<SearchResult> matches = ThresholdSearchEngine.searchWithThreshold(
                    store, "Database deadlock detected in lock manager", 5, 0.95
            );
            if (matches.isEmpty()) {
                return new ScenarioResult(11, name, false, "Expected high-similarity exact match with threshold 0.95");
            }
            if (matches.stream().anyMatch(m -> "unrelated".equals(m.id()))) {
                return new ScenarioResult(11, name, false, "Unrelated document should have been filtered by threshold");
            }
            return new ScenarioResult(11, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(11, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "ScoredSearchEngine (Scored Retrieval & Descending Sort)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("doc-1").text("Kafka event log error in consumer group").build(),
                    Document.builder().id("doc-2").text("Kafka event log warning in broker partition").build(),
                    Document.builder().id("doc-3").text("Kafka event log info in producer cluster").build()
            ));

            try {
                ScoredSearchEngine.searchScoredAndSorted(null, "Kafka", 3, 0.1);
                return new ScenarioResult(12, name, false, "Expected IllegalArgumentException on null store");
            } catch (IllegalArgumentException expected) {}

            List<SearchResult> sorted = ScoredSearchEngine.searchScoredAndSorted(store, "Kafka event log", 3, 0.0);
            if (sorted == null || sorted.size() != 3) {
                return new ScenarioResult(12, name, false, "Expected 3 search results, got: " + (sorted == null ? "null" : sorted.size()));
            }

            for (int i = 0; i < sorted.size() - 1; i++) {
                if (sorted.get(i).score() < sorted.get(i + 1).score()) {
                    return new ScenarioResult(12, name, false, "Results not sorted descending by score: " + sorted.get(i).score() + " < " + sorted.get(i + 1).score());
                }
            }
            return new ScenarioResult(12, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(12, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 5: FilterExpressionBuilder Basics & Comparisons (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13() {
        String name = "EqualityFilterBuilder (EQ and NE Filter Expressions)";
        try {
            try {
                EqualityFilterBuilder.buildEqualityFilter("  ", "val", false);
                return new ScenarioResult(13, name, false, "Expected IllegalArgumentException on blank field");
            } catch (IllegalArgumentException expected) {}

            try {
                EqualityFilterBuilder.buildEqualityFilter("field", null, false);
                return new ScenarioResult(13, name, false, "Expected IllegalArgumentException on null value");
            } catch (IllegalArgumentException expected) {}

            Filter.Expression eqExpr = EqualityFilterBuilder.buildEqualityFilter("status", "ACTIVE", false);
            if (eqExpr == null || eqExpr.type() != Filter.ExpressionType.EQ) {
                return new ScenarioResult(13, name, false, "Expected Filter.ExpressionType.EQ, got: " + (eqExpr == null ? "null" : eqExpr.type()));
            }

            Filter.Expression neExpr = EqualityFilterBuilder.buildEqualityFilter("status", "ARCHIVED", true);
            if (neExpr == null || neExpr.type() != Filter.ExpressionType.NE) {
                return new ScenarioResult(13, name, false, "Expected Filter.ExpressionType.NE, got: " + (neExpr == null ? "null" : neExpr.type()));
            }
            return new ScenarioResult(13, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(13, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario14() {
        String name = "NumericRangeFilterBuilder (GTE & LTE Conjunction Expression)";
        try {
            try {
                NumericRangeFilterBuilder.buildRangeFilter("latency", 500.0, 100.0);
                return new ScenarioResult(14, name, false, "Expected IllegalArgumentException when min > max");
            } catch (IllegalArgumentException expected) {}

            try {
                NumericRangeFilterBuilder.buildRangeFilter(" ", 10.0, 20.0);
                return new ScenarioResult(14, name, false, "Expected IllegalArgumentException on blank field");
            } catch (IllegalArgumentException expected) {}

            Filter.Expression range = NumericRangeFilterBuilder.buildRangeFilter("latencyMs", 50.0, 250.0);
            if (range == null || range.type() != Filter.ExpressionType.AND) {
                return new ScenarioResult(14, name, false, "Expected top-level AND expression, got: " + (range == null ? "null" : range.type()));
            }

            if (!(range.left() instanceof Filter.Expression left && left.type() == Filter.ExpressionType.GTE)) {
                return new ScenarioResult(14, name, false, "Expected left operand to be GTE expression");
            }
            if (!(range.right() instanceof Filter.Expression right && right.type() == Filter.ExpressionType.LTE)) {
                return new ScenarioResult(14, name, false, "Expected right operand to be LTE expression");
            }
            return new ScenarioResult(14, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(14, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario15() {
        String name = "LogicalFilterBuilder (AND / OR Dual-Branch Conjunctions)";
        try {
            try {
                LogicalFilterBuilder.buildConjunctiveFilter("cat", "DB", " ", "HIGH", true);
                return new ScenarioResult(15, name, false, "Expected IllegalArgumentException on blank param");
            } catch (IllegalArgumentException expected) {}

            Filter.Expression andExpr = LogicalFilterBuilder.buildConjunctiveFilter("category", "DB", "severity", "CRITICAL", true);
            if (andExpr == null || andExpr.type() != Filter.ExpressionType.AND) {
                return new ScenarioResult(15, name, false, "Expected AND expression when requireBoth is true, got: " + (andExpr == null ? "null" : andExpr.type()));
            }

            Filter.Expression orExpr = LogicalFilterBuilder.buildConjunctiveFilter("category", "DB", "severity", "CRITICAL", false);
            if (orExpr == null || orExpr.type() != Filter.ExpressionType.OR) {
                return new ScenarioResult(15, name, false, "Expected OR expression when requireBoth is false, got: " + (orExpr == null ? "null" : orExpr.type()));
            }
            return new ScenarioResult(15, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(15, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // TOPIC 6: FilterExpressionBuilder Collections & Search Integration (Scenarios 16 - 18)
    // =========================================================================

    private static ScenarioResult verifyScenario16() {
        String name = "SetContainmentFilterBuilder (IN and NIN Set Expressions)";
        try {
            try {
                SetContainmentFilterBuilder.buildInFilter("tier", List.of(), false);
                return new ScenarioResult(16, name, false, "Expected IllegalArgumentException on empty values");
            } catch (IllegalArgumentException expected) {}

            Filter.Expression inExpr = SetContainmentFilterBuilder.buildInFilter("env", List.of("PROD", "STAGING"), false);
            if (inExpr == null || inExpr.type() != Filter.ExpressionType.IN) {
                return new ScenarioResult(16, name, false, "Expected IN expression, got: " + (inExpr == null ? "null" : inExpr.type()));
            }

            Filter.Expression ninExpr = SetContainmentFilterBuilder.buildInFilter("env", List.of("DEV"), true);
            if (ninExpr == null || ninExpr.type() != Filter.ExpressionType.NIN) {
                return new ScenarioResult(16, name, false, "Expected NIN expression, got: " + (ninExpr == null ? "null" : ninExpr.type()));
            }
            return new ScenarioResult(16, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(16, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario17() {
        String name = "NestedFilterBuilder (Complex Grouped Audit Expression)";
        try {
            try {
                NestedFilterBuilder.buildAuditFilter("PROD", "CRITICAL", List.of());
                return new ScenarioResult(17, name, false, "Expected IllegalArgumentException on empty teams");
            } catch (IllegalArgumentException expected) {}

            Filter.Expression nested = NestedFilterBuilder.buildAuditFilter("PROD", "CRITICAL", List.of("sre", "security"));
            if (nested == null || nested.type() != Filter.ExpressionType.AND) {
                return new ScenarioResult(17, name, false, "Expected top-level AND expression, got: " + (nested == null ? "null" : nested.type()));
            }

            if (!(nested.left() instanceof Filter.Expression left && left.type() == Filter.ExpressionType.EQ)) {
                return new ScenarioResult(17, name, false, "Expected left branch to be EQ expression");
            }
            if (!(nested.right() instanceof Filter.Expression right && right.type() == Filter.ExpressionType.OR)) {
                return new ScenarioResult(17, name, false, "Expected right branch to be OR expression");
            }
            return new ScenarioResult(17, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(17, name, false, "Exception: " + e.getMessage());
        }
    }

    private static ScenarioResult verifyScenario18() {
        String name = "FilteredSearchGateway (Integrated SearchRequest with FilterExpression)";
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("doc-match").text("Systemic database deadlock in cluster")
                            .metadata(Map.of("env", "PROD", "severity", "CRITICAL")).build(),
                    Document.builder().id("doc-wrong-env").text("Systemic database deadlock in cluster")
                            .metadata(Map.of("env", "DEV", "severity", "CRITICAL")).build(),
                    Document.builder().id("doc-wrong-sev").text("Systemic database deadlock in cluster")
                            .metadata(Map.of("env", "PROD", "severity", "LOW")).build()
            ));

            FilterExpressionBuilder b = new FilterExpressionBuilder();
            Filter.Expression filter = b.and(b.eq("env", "PROD"), b.eq("severity", "CRITICAL")).build();

            try {
                FilteredSearchGateway.searchWithFilter(null, "deadlock", filter, 5);
                return new ScenarioResult(18, name, false, "Expected IllegalArgumentException on null store");
            } catch (IllegalArgumentException expected) {}

            List<SearchResult> results = FilteredSearchGateway.searchWithFilter(store, "database deadlock", filter, 5);
            if (results == null || results.size() != 1) {
                return new ScenarioResult(18, name, false, "Expected exactly 1 matching result, got: " + (results == null ? "null" : results.size()));
            }

            if (!"doc-match".equals(results.get(0).id())) {
                return new ScenarioResult(18, name, false, "Expected match 'doc-match', got: " + results.get(0).id());
            }
            return new ScenarioResult(18, name, true, "OK");
        } catch (Exception e) {
            return new ScenarioResult(18, name, false, "Exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // REPORT FORMATTER
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 07 EXERCISE 01: VECTOR STORES, EMBEDDINGS & SEMANTIC FILTERING");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String tag = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", tag, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        System.out.println("===============================================================================");
    }
}
