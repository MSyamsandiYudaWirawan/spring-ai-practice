package phase07;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import phase07.VectorStoreContracts.*;
import phase07.VectorStoreUnderTest.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 07 Exercise 01:
 * Vector Stores, In-Memory Embeddings, Document Chunking and Semantic Filtering.
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
        return list;
    }

    private static ScenarioResult verifyScenario1() {
        String name = "DocumentFactory (Normalized Creation & Metadata Enrichment)";
        try {
            // Validation guard
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
            if (!doc.getMetadata().containsKey("charCount") || !doc.getMetadata().containsKey("createdAt")) {
                return new ScenarioResult(1, name, false, "Required metadata 'charCount' or 'createdAt' missing");
            }

            return new ScenarioResult(1, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(1, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario2() {
        String name = "TextChunkingPipeline (Token Splitting & Index Tagging)";
        try {
            String longText = "PostgreSQL database connection pool exhaustion leads to high request queuing latency. "
                    + "When active connections exceed maximum capacity of 50, incoming queries block in thread pool. "
                    + "Diagnosis requires inspecting HikariCP telemetry pool metrics and thread dump deadlock states. "
                    + "Remediation involves adjusting pool size or releasing idle connections in repository transactions.";

            Document doc = Document.builder().id("doc-root").text(longText).metadata(Map.of("category", "DB")).build();
            ChunkConfig config = new ChunkConfig(15, 10, 5, 20, true);

            List<Document> chunks = TextChunkingPipeline.splitDocument(doc, config);
            if (chunks == null || chunks.isEmpty()) {
                return new ScenarioResult(2, name, false, "Splitter returned null or empty chunks");
            }
            if (chunks.size() < 2) {
                return new ScenarioResult(2, name, false, "Expected document to be split into >= 2 chunks, got: " + chunks.size());
            }

            for (int i = 0; i < chunks.size(); i++) {
                Document c = chunks.get(i);
                Object idx = c.getMetadata().get("chunkIndex");
                Object total = c.getMetadata().get("totalChunks");
                if (idx == null || (int) idx != i) {
                    return new ScenarioResult(2, name, false, "Chunk " + i + " missing or invalid 'chunkIndex' metadata: " + idx);
                }
                if (total == null || (int) total != chunks.size()) {
                    return new ScenarioResult(2, name, false, "Chunk " + i + " missing or invalid 'totalChunks' metadata: " + total);
                }
            }

            return new ScenarioResult(2, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(2, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario3() {
        String name = "DeterministicVectorGenerator (Unit Vector L2 Normalization)";
        try {
            float[] v1 = DeterministicVectorGenerator.generateNormalizedVector("kubernetes memory leak", 16);
            if (v1 == null || v1.length != 16) {
                return new ScenarioResult(3, name, false, "Expected vector of length 16, got: " + (v1 == null ? "null" : v1.length));
            }

            // Verify L2 norm == 1.0
            float sumSq = 0.0f;
            for (float f : v1) sumSq += f * f;
            float norm = (float) Math.sqrt(sumSq);
            if (Math.abs(norm - 1.0f) > 1e-4f) {
                return new ScenarioResult(3, name, false, "Expected unit L2 norm 1.0, got: " + norm);
            }

            // Determinism check
            float[] v2 = DeterministicVectorGenerator.generateNormalizedVector("kubernetes memory leak", 16);
            if (!Arrays.equals(v1, v2)) {
                return new ScenarioResult(3, name, false, "Deterministic generator returned non-identical vectors for identical text");
            }

            // Distinctness check
            float[] v3 = DeterministicVectorGenerator.generateNormalizedVector("redis cache eviction", 16);
            if (Arrays.equals(v1, v3)) {
                return new ScenarioResult(3, name, false, "Deterministic generator returned identical vectors for distinct texts");
            }

            return new ScenarioResult(3, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(3, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario4() {
        String name = "SimpleVectorStoreIndexer (In-Memory Vector Store Initialization)";
        try {
            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            List<Document> docs = List.of(
                    Document.builder().id("d1").text("Postgres Hikari connection leak").build(),
                    Document.builder().id("d2").text("JVM Garbage Collection pause duration").build()
            );

            SimpleVectorStore store = VectorStoreIndexer.createAndIndex(embeddingModel, docs);
            if (store == null) {
                return new ScenarioResult(4, name, false, "VectorStoreIndexer returned null store");
            }

            List<Document> results = store.similaritySearch("Hikari connection");
            if (results == null || results.isEmpty()) {
                return new ScenarioResult(4, name, false, "Indexed store returned zero results for query");
            }

            return new ScenarioResult(4, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(4, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario5() {
        String name = "SimilaritySearchEngine (Top-K Ranked Retrieval)";
        try {
            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            store.add(List.of(
                    Document.builder().id("d1").text("Kafka consumer group rebalance lag").metadata(Map.of("tier", "streaming")).build(),
                    Document.builder().id("d2").text("Kafka broker partition leader election").metadata(Map.of("tier", "streaming")).build(),
                    Document.builder().id("d3").text("Redis cluster failover timeout").metadata(Map.of("tier", "cache")).build()
            ));

            List<SearchResult> results = SimilaritySearchEngine.searchTopK(store, "Kafka consumer lag", 2);
            if (results == null || results.size() != 2) {
                return new ScenarioResult(5, name, false, "Expected exactly 2 top-k results, got: " + (results == null ? "null" : results.size()));
            }

            // Results must be sorted descending by score
            if (results.get(0).score() < results.get(1).score()) {
                return new ScenarioResult(5, name, false, "Results not sorted descending by similarity score: " + results);
            }

            return new ScenarioResult(5, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(5, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario6() {
        String name = "ThresholdSimilarityFilter (Cutoff Threshold Relevance Gate)";
        try {
            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            store.add(List.of(
                    Document.builder().id("exact").text("Database deadlock detected on user table").build(),
                    Document.builder().id("other").text("Unrelated CSS style stylesheet bundle").build()
            ));

            // Guard validation
            try {
                ThresholdSimilarityFilter.searchWithThreshold(store, "test", 5, 1.5);
                return new ScenarioResult(6, name, false, "Expected IllegalArgumentException for threshold > 1.0");
            } catch (IllegalArgumentException expected) {}

            List<SearchResult> highThreshold = ThresholdSimilarityFilter.searchWithThreshold(store, "Database deadlock detected on user table", 5, 0.99);
            if (highThreshold == null || highThreshold.isEmpty()) {
                return new ScenarioResult(6, name, false, "Expected at least 1 match exceeding threshold 0.99, got 0");
            }
            for (SearchResult r : highThreshold) {
                if (r.score() < 0.99) {
                    return new ScenarioResult(6, name, false, "Result score " + r.score() + " is below required threshold 0.99");
                }
            }

            return new ScenarioResult(6, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(6, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario7() {
        String name = "MetadataExpressionFilter (Structured FilterExpression Query)";
        try {
            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            store.add(List.of(
                    Document.builder().id("doc-match").text("Postgres connection pool exhaustion").metadata(Map.of("category", "DB", "severity", "CRITICAL")).build(),
                    Document.builder().id("doc-wrong-sev").text("Postgres slow query log alert").metadata(Map.of("category", "DB", "severity", "LOW")).build(),
                    Document.builder().id("doc-wrong-cat").text("Kubernetes OOMKilled worker node").metadata(Map.of("category", "K8S", "severity", "CRITICAL")).build()
            ));

            List<SearchResult> results = MetadataExpressionFilter.searchWithMetadataFilter(store, "Postgres", "DB", "CRITICAL", 5);
            if (results == null || results.isEmpty()) {
                return new ScenarioResult(7, name, false, "Expected at least 1 match with category=DB and severity=CRITICAL, got 0");
            }

            for (SearchResult r : results) {
                if (!"DB".equals(r.metadata().get("category")) || !"CRITICAL".equals(r.metadata().get("severity"))) {
                    return new ScenarioResult(7, name, false, "Returned document does not match metadata filters: " + r);
                }
            }

            return new ScenarioResult(7, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(7, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario8() {
        String name = "DocumentLifecycleManager (Dynamic Delete & Re-Index)";
        try {
            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            store.add(List.of(
                    Document.builder().id("doc-old").text("Legacy monolithic batch worker").build()
            ));

            List<Document> newDocs = List.of(
                    Document.builder().id("doc-new").text("Modern event-driven reactive consumer").build()
            );

            DocumentLifecycleManager.deleteAndReindex(store, List.of("doc-old"), newDocs);

            List<Document> searchOld = store.similaritySearch("Legacy monolithic batch worker");
            boolean oldFound = searchOld.stream().anyMatch(d -> "doc-old".equals(d.getId()));
            if (oldFound) {
                return new ScenarioResult(8, name, false, "Deleted document 'doc-old' was still found in vector store");
            }

            List<Document> searchNew = store.similaritySearch("Modern event-driven reactive consumer");
            boolean newFound = searchNew.stream().anyMatch(d -> "doc-new".equals(d.getId()));
            if (!newFound) {
                return new ScenarioResult(8, name, false, "Newly indexed document 'doc-new' was not found in vector store");
            }

            return new ScenarioResult(8, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(8, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario9() {
        String name = "CachedEmbeddingDecorator (Zero-Redundancy Vector Cache)";
        try {
            FakeEmbeddingModel underlying = new FakeEmbeddingModel(16);
            CachedEmbeddingDecorator cached = new CachedEmbeddingDecorator(underlying);

            // First call -> cache miss
            float[] v1 = cached.embed("kubernetes pod pending");
            CacheStats s1 = cached.getStats();
            if (s1.hits() != 0 || s1.misses() != 1 || underlying.getCallCount() != 1) {
                return new ScenarioResult(9, name, false, "Expected 0 hits, 1 miss after 1st call. Got: " + s1);
            }

            // Second call with same text -> cache hit
            float[] v2 = cached.embed("kubernetes pod pending");
            CacheStats s2 = cached.getStats();
            if (s2.hits() != 1 || s2.misses() != 1 || underlying.getCallCount() != 1) {
                return new ScenarioResult(9, name, false, "Expected 1 hit, 1 miss after 2nd call. Underlying model should not be called again. Got: " + s2 + ", callCount=" + underlying.getCallCount());
            }

            if (!Arrays.equals(v1, v2)) {
                return new ScenarioResult(9, name, false, "Cached vector differs from originally computed vector");
            }

            return new ScenarioResult(9, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(9, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario10() {
        String name = "KnowledgeIngestionGateway (End-to-End Ingestion & Verification Gate)";
        try {
            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

            List<RawKnowledgeItem> items = List.of(
                    new RawKnowledgeItem("kb-1", "JVM Garbage Collection Tuning",
                            "G1GC region sizing and pause time goals. ParallelGC throughput vs ZGC sub-millisecond latency trade-offs.",
                            Map.of("tier", "core")),
                    new RawKnowledgeItem("kb-2", "PostgreSQL Connection Pooling",
                            "HikariCP pool sizing calculation: connections = (core_count * 2) + effective_spindle_count.",
                            Map.of("tier", "data"))
            );
            ChunkConfig config = new ChunkConfig(20, 10, 5, 10, true);

            // Test 1: Successful ingestion and verification
            IngestionReport report = KnowledgeIngestionGateway.ingestAndVerify(
                    store, items, config, "PostgreSQL Connection Pooling", 0.70
            );

            if (report == null || report.rawItemCount() != 2 || report.chunkCount() < 2 || report.indexedCount() < 2) {
                return new ScenarioResult(10, name, false, "IngestionReport counts mismatch: " + report);
            }

            // Test 2: Unmatchable verification query triggers VectorStoreBreachException
            boolean breachCaught = false;
            try {
                KnowledgeIngestionGateway.ingestAndVerify(store, items, config, "Unrelated Quantum Computing Physics Query", 0.9999);
            } catch (VectorStoreBreachException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("vectorstore constraint breached")) {
                    breachCaught = true;
                } else {
                    return new ScenarioResult(10, name, false, "Exception message missing 'VectorStore constraint breached': " + ex.getMessage());
                }
            }

            if (!breachCaught) {
                return new ScenarioResult(10, name, false, "Expected VectorStoreBreachException when verification query yields 0 matches");
            }

            return new ScenarioResult(10, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(10, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario11() {
        String name = "MaximalMarginalRelevanceSearchEngine (MMR Diversity Re-ranking)";
        try {
            // Guard test
            try {
                MaximalMarginalRelevanceSearchEngine.searchMmr(null, null, null, null);
                return new ScenarioResult(11, name, false, "Expected IllegalArgumentException for null arguments");
            } catch (IllegalArgumentException expected) {}

            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

            // Seed 4 documents: doc1 and doc2 are near-duplicates; doc3 is kubernetes config; doc4 is postgresql
            Document doc1 = Document.builder().id("doc-1").text("Kubernetes pod OOMKilled memory limit exceeded container restart").build();
            Document doc2 = Document.builder().id("doc-2").text("Container OOMKilled kubernetes memory limit exceeded pod restart").build();
            Document doc3 = Document.builder().id("doc-3").text("ConfigMap environment variable misconfiguration in kubernetes cluster").build();
            Document doc4 = Document.builder().id("doc-4").text("PostgreSQL database connection pool timeout exhaustion").build();

            store.add(List.of(doc1, doc2, doc3, doc4));

            MmrConfig config = new MmrConfig(2, 0.5, 2);
            List<SearchResult> mmrResults = MaximalMarginalRelevanceSearchEngine.searchMmr(
                    store, embeddingModel, "kubernetes pod memory OOMKilled restart", config
            );

            if (mmrResults == null || mmrResults.isEmpty()) {
                return new ScenarioResult(11, name, false, "MMR returned null or empty result list");
            }

            if (mmrResults.size() != 2) {
                return new ScenarioResult(11, name, false, "Expected exactly 2 MMR results, got: " + mmrResults.size());
            }

            // In MMR with lambda 0.5, doc1 is chosen first.
            // doc2 is an almost identical duplicate of doc1, so doc3 should be prioritized over doc2 for diversity
            List<String> returnedIds = mmrResults.stream().map(SearchResult::id).toList();
            if (!returnedIds.contains("doc-1")) {
                return new ScenarioResult(11, name, false, "Expected primary relevant doc 'doc-1' to be selected in MMR");
            }
            if (returnedIds.contains("doc-2") && !returnedIds.contains("doc-3")) {
                return new ScenarioResult(11, name, false, "MMR failed to diversify: selected duplicate 'doc-2' instead of diverse 'doc-3'");
            }

            // Verify scores are populated and non-negative
            for (SearchResult r : mmrResults) {
                if (Double.isNaN(r.score()) || r.score() <= 0.0) {
                    return new ScenarioResult(11, name, false, "Invalid similarity score in SearchResult: " + r.score());
                }
            }

            return new ScenarioResult(11, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(11, name, false, "Exception: " + t.getMessage());
        }
    }

    private static ScenarioResult verifyScenario12() {
        String name = "ContentHashDeduplicationPipeline (Idempotent Chunk Ingestion)";
        try {
            // Guard test
            ContentHashDeduplicationPipeline pipeline = new ContentHashDeduplicationPipeline();
            try {
                pipeline.ingestWithDeduplication(null, null);
                return new ScenarioResult(12, name, false, "Expected IllegalArgumentException for null arguments");
            } catch (IllegalArgumentException expected) {}

            EmbeddingModel embeddingModel = new FakeEmbeddingModel(16);
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

            // Batch 1: 3 documents (doc1 and doc3 have identical content after trim/case normalization)
            Document doc1 = Document.builder().id("doc-1").text("JVM garbage collection pause latency spikes").build();
            Document doc2 = Document.builder().id("doc-2").text("HikariCP database pool saturation timeout").build();
            Document doc3 = Document.builder().id("doc-3").text("  jvm garbage collection pause latency spikes  ").build();

            DedupReport report1 = pipeline.ingestWithDeduplication(store, List.of(doc1, doc2, doc3));
            if (report1 == null) {
                return new ScenarioResult(12, name, false, "Pipeline returned null report for batch 1");
            }
            if (report1.totalSubmitted() != 3) {
                return new ScenarioResult(12, name, false, "Batch 1: expected totalSubmitted=3, got: " + report1.totalSubmitted());
            }
            if (report1.newOrUpdatedCount() != 2) {
                return new ScenarioResult(12, name, false, "Batch 1: expected newOrUpdatedCount=2, got: " + report1.newOrUpdatedCount());
            }
            if (report1.duplicateSkippedCount() != 1) {
                return new ScenarioResult(12, name, false, "Batch 1: expected duplicateSkippedCount=1, got: " + report1.duplicateSkippedCount());
            }
            if (!report1.indexedIds().contains("doc-1") || !report1.indexedIds().contains("doc-2") || report1.indexedIds().contains("doc-3")) {
                return new ScenarioResult(12, name, false, "Batch 1: indexed IDs mismatch: " + report1.indexedIds());
            }

            // Batch 2: Resubmit doc1 under new ID, plus new doc4
            Document doc1Resubmitted = Document.builder().id("doc-1-replay").text("jvm garbage collection pause latency spikes").build();
            Document doc4 = Document.builder().id("doc-4").text("Tomcat thread pool exhaustion").build();

            DedupReport report2 = pipeline.ingestWithDeduplication(store, List.of(doc1Resubmitted, doc4));
            if (report2.totalSubmitted() != 2) {
                return new ScenarioResult(12, name, false, "Batch 2: expected totalSubmitted=2, got: " + report2.totalSubmitted());
            }
            if (report2.newOrUpdatedCount() != 1) {
                return new ScenarioResult(12, name, false, "Batch 2: expected newOrUpdatedCount=1, got: " + report2.newOrUpdatedCount());
            }
            if (report2.duplicateSkippedCount() != 1) {
                return new ScenarioResult(12, name, false, "Batch 2: expected duplicateSkippedCount=1, got: " + report2.duplicateSkippedCount());
            }
            if (!report2.indexedIds().contains("doc-4") || report2.indexedIds().contains("doc-1-replay")) {
                return new ScenarioResult(12, name, false, "Batch 2: indexed IDs mismatch: " + report2.indexedIds());
            }

            if (pipeline.getIndexedHashes().size() != 3) {
                return new ScenarioResult(12, name, false, "Expected 3 indexed hashes in pipeline, got: " + pipeline.getIndexedHashes().size());
            }

            return new ScenarioResult(12, name, true, "OK");
        } catch (Throwable t) {
            return new ScenarioResult(12, name, false, "Exception: " + t.getMessage());
        }
    }

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 07 EXERCISE 01: VECTOR STORES, EMBEDDINGS & SEMANTIC FILTERING");
        System.out.println("===============================================================================");
        int passed = 0;
        for (ScenarioResult r : results) {
            String mark = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", mark, r.scenarioNumber(), r.name());
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
