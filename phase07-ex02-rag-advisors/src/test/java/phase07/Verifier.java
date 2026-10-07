package phase07;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import phase07.RagAdvisorContracts.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verification harness for Phase 07 Exercise 02 (RAG Advisors).
 * Executes 10 deterministic offline tests against {@link RagAdvisorUnderTest}.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class Verifier {

    private static final AtomicInteger passed = new AtomicInteger(0);
    private static final AtomicInteger failed = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  PHASE 07 EXERCISE 02: MODULAR RAG ADVISORS & GATEWAY VERIFIER  ");
        System.out.println("=================================================================\n");

        RagAdvisorUnderTest underTest = new RagAdvisorUnderTest();

        testScenario01(underTest);
        testScenario02(underTest);
        testScenario03(underTest);
        testScenario04(underTest);
        testScenario05(underTest);
        testScenario06(underTest);
        testScenario07(underTest);
        testScenario08(underTest);
        testScenario09(underTest);
        testScenario10(underTest);

        System.out.println("\n-----------------------------------------------------------------");
        System.out.printf("VERIFICATION SUMMARY: %d / 10 PASSED, %d FAILED%n", passed.get(), failed.get());
        System.out.println("-----------------------------------------------------------------");

        if (failed.get() > 0) {
            System.exit(99);
        } else {
            System.exit(0);
        }
    }

    @org.junit.jupiter.api.Test
    public void verifyAll() {
        RagAdvisorUnderTest underTest = new RagAdvisorUnderTest();
        passed.set(0);
        failed.set(0);

        testScenario01(underTest);
        testScenario02(underTest);
        testScenario03(underTest);
        testScenario04(underTest);
        testScenario05(underTest);
        testScenario06(underTest);
        testScenario07(underTest);
        testScenario08(underTest);
        testScenario09(underTest);
        testScenario10(underTest);

        org.junit.jupiter.api.Assertions.assertEquals(0, failed.get(), "Verifier detected scenario failures.");
    }

    private static void pass(String name) {
        passed.incrementAndGet();
        System.out.println("  [PASS] " + name);
    }

    private static void fail(String name, String detail) {
        failed.incrementAndGet();
        System.out.println("  [FAIL] " + name);
        if (detail != null && !detail.isBlank()) {
            System.out.println("         --> DETAIL: " + detail);
        }
    }

    private static VectorStore createPopulatedStore() {
        FakeEmbeddingModel embeddingModel = new FakeEmbeddingModel(128);
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        Document doc1 = Document.builder()
                .id("doc-kafka-01")
                .text("Kafka cluster broker timeout tuning and consumer rebalance latency")
                .metadata(Map.of("tenant", "fintech", "source", "kafka-guide.md", "clearance", 2, "priority", 10))
                .build();

        Document doc2 = Document.builder()
                .id("doc-redis-02")
                .text("Redis distributed memory cache cluster replication and latency")
                .metadata(Map.of("tenant", "fintech", "source", "redis-guide.md", "clearance", 1, "priority", 5))
                .build();

        Document doc3 = Document.builder()
                .id("doc-health-03")
                .text("Healthcare patient record retention policy HIPAA compliance latency")
                .metadata(Map.of("tenant", "healthcare", "source", "hipaa-guide.md", "clearance", 3, "priority", 20))
                .build();

        store.add(List.of(doc1, doc2, doc3));
        return store;
    }

    private static void testScenario01(RagAdvisorUnderTest underTest) {
        String name = "Scenario 01: VectorStoreDocumentRetriever Builder & Similarity Threshold";
        try {
            VectorStore store = createPopulatedStore();
            DocumentRetriever retriever = underTest.buildVectorRetriever(store, 2, 0.5);

            if (retriever == null) {
                fail(name, "buildVectorRetriever returned null");
                return;
            }

            List<Document> docs = retriever.retrieve(new Query("Kafka cluster timeout"));
            if (docs == null || docs.isEmpty()) {
                fail(name, "Expected matching documents for 'Kafka cluster timeout', got empty");
                return;
            }

            if (!docs.get(0).getId().equals("doc-kafka-01")) {
                fail(name, "Expected top document 'doc-kafka-01', got: " + docs.get(0).getId());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario02(RagAdvisorUnderTest underTest) {
        String name = "Scenario 02: Tenant-Isolated FilterExpression Retrieval";
        try {
            VectorStore store = createPopulatedStore();
            DocumentRetriever retriever = underTest.buildTenantFilteredRetriever(store, "healthcare", 5);

            if (retriever == null) {
                fail(name, "buildTenantFilteredRetriever returned null");
                return;
            }

            List<Document> docs = retriever.retrieve(new Query("latency"));
            if (docs == null || docs.isEmpty()) {
                fail(name, "Expected healthcare documents matching 'latency', got empty");
                return;
            }

            for (Document d : docs) {
                if (!"healthcare".equals(d.getMetadata().get("tenant"))) {
                    fail(name, "Leaked non-healthcare tenant document: " + d.getId() + " (" + d.getMetadata().get("tenant") + ")");
                    return;
                }
            }

            if (!docs.get(0).getId().equals("doc-health-03")) {
                fail(name, "Expected 'doc-health-03', got: " + docs.get(0).getId());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario03(RagAdvisorUnderTest underTest) {
        String name = "Scenario 03: Custom ContextualQueryAugmenter & Document Formatting";
        try {
            QueryAugmenter augmenter = underTest.buildCustomAugmenter("[SRC: ", true);
            if (augmenter == null) {
                fail(name, "buildCustomAugmenter returned null");
                return;
            }

            Document d1 = Document.builder()
                    .id("d1")
                    .text("Replication factor must be 3.")
                    .metadata(Map.of("source", "ops.md"))
                    .build();

            Query query = new Query("What is the replication factor?");
            Query augmented = augmenter.augment(query, List.of(d1));

            if (augmented == null || augmented.text() == null) {
                fail(name, "Augmented query is null");
                return;
            }

            if (!augmented.text().contains("[SRC: ops.md: Replication factor must be 3.")) {
                fail(name, "Augmented query missing formatted source header: " + augmented.text());
                return;
            }

            if (!augmented.text().contains("What is the replication factor?")) {
                fail(name, "Augmented query missing original query text: " + augmented.text());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario04(RagAdvisorUnderTest underTest) {
        String name = "Scenario 04: Empty Context Guardrails & Fallback Prompting";
        try {
            String fallback = "The requested information is outside the current security boundary.";
            QueryAugmenter fallbackAugmenter = underTest.buildFallbackAugmenter(fallback);

            if (fallbackAugmenter == null) {
                fail(name, "buildFallbackAugmenter returned null");
                return;
            }

            Query query = new Query("What is the root password?");
            Query augmented = fallbackAugmenter.augment(query, List.of());

            if (augmented == null || !fallback.equals(augmented.text())) {
                fail(name, "Expected fallback prompt '" + fallback + "', got: " + (augmented != null ? augmented.text() : "null"));
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario05(RagAdvisorUnderTest underTest) {
        String name = "Scenario 05: DocumentPostProcessor Deduplication & Priority Reranking";
        try {
            DocumentPostProcessor postProcessor = underTest.buildDeduplicatingPriorityReranker(2);
            if (postProcessor == null) {
                fail(name, "buildDeduplicatingPriorityReranker returned null");
                return;
            }

            Document d1 = Document.builder().id("id-1").text("low priority").metadata(Map.of("priority", 10)).build();
            Document d2 = Document.builder().id("id-2").text("high priority").metadata(Map.of("priority", 50)).build();
            Document d1Dup = Document.builder().id("id-1").text("dup of id-1").metadata(Map.of("priority", 10)).build();
            Document d3 = Document.builder().id("id-3").text("mid priority").metadata(Map.of("priority", 30)).build();

            List<Document> processed = postProcessor.process(new Query("test"), List.of(d1, d2, d1Dup, d3));
            if (processed == null || processed.size() != 2) {
                fail(name, "Expected exactly 2 post-processed documents, got: " + (processed != null ? processed.size() : "null"));
                return;
            }

            // Expected order: id-2 (50), id-3 (30)
            if (!processed.get(0).getId().equals("id-2") || !processed.get(1).getId().equals("id-3")) {
                fail(name, "Expected [id-2, id-3], got: [" + processed.get(0).getId() + ", " + processed.get(1).getId() + "]");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario06(RagAdvisorUnderTest underTest) {
        String name = "Scenario 06: MultiQueryExpander Pre-Retrieval Expansion";
        try {
            FakeRagChatModel chatModel = new FakeRagChatModel();
            chatModel.enqueue("k8s pod latency tuning\nk8s ingress timeout optimization");

            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
            QueryExpander expander = underTest.buildMultiQueryExpander(chatClientBuilder, 2, true);

            if (expander == null) {
                fail(name, "buildMultiQueryExpander returned null");
                return;
            }

            List<Query> expanded = expander.expand(new Query("k8s latency"));
            if (expanded == null || expanded.size() != 3) {
                fail(name, "Expected 3 queries (original + 2 variants), got: " + (expanded != null ? expanded.size() : "null"));
                return;
            }

            if (!"k8s latency".equals(expanded.get(0).text())) {
                fail(name, "Expected original query first, got: " + expanded.get(0).text());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario07(RagAdvisorUnderTest underTest) {
        String name = "Scenario 07: RewriteQueryTransformer Query Rewriting";
        try {
            FakeRagChatModel chatModel = new FakeRagChatModel();
            chatModel.enqueue("How to configure Spring AI vector store similarity threshold?");

            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
            QueryTransformer transformer = underTest.buildRewriteTransformer(chatClientBuilder, "enterprise knowledge base");

            if (transformer == null) {
                fail(name, "buildRewriteTransformer returned null");
                return;
            }

            Query rewritten = transformer.transform(new Query("how do i set the threshold again?"));
            if (rewritten == null || !rewritten.text().equals("How to configure Spring AI vector store similarity threshold?")) {
                fail(name, "Expected rewritten query text, got: " + (rewritten != null ? rewritten.text() : "null"));
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario08(RagAdvisorUnderTest underTest) {
        String name = "Scenario 08: ConcatenationDocumentJoiner Multi-Query Merging";
        try {
            Query q1 = new Query("query 1");
            Query q2 = new Query("query 2");

            Document docA = Document.builder().id("doc-A").text("Doc A").score(0.85).build();
            Document docB = Document.builder().id("doc-B").text("Doc B").score(0.95).build();
            Document docADup = Document.builder().id("doc-A").text("Doc A copy").score(0.70).build();

            Map<Query, List<List<Document>>> queryResults = new LinkedHashMap<>();
            queryResults.put(q1, List.of(List.of(docA)));
            queryResults.put(q2, List.of(List.of(docB, docADup)));

            List<Document> joined = underTest.joinMultiQueryResults(queryResults);
            if (joined == null || joined.size() != 2) {
                fail(name, "Expected 2 deduplicated documents, got: " + (joined != null ? joined.size() : "null"));
                return;
            }

            // Sorted by score descending: doc-B (0.95), doc-A (0.85)
            if (!joined.get(0).getId().equals("doc-B") || !joined.get(1).getId().equals("doc-A")) {
                fail(name, "Expected order [doc-B, doc-A], got: [" + joined.get(0).getId() + ", " + joined.get(1).getId() + "]");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario09(RagAdvisorUnderTest underTest) {
        String name = "Scenario 09: RetrievalAugmentationAdvisor End-to-End ChatClient Binding";
        try {
            VectorStore store = createPopulatedStore();
            DocumentRetriever retriever = underTest.buildVectorRetriever(store, 2, 0.5);

            RetrievalAugmentationAdvisor advisor = underTest.buildRetrievalAdvisor(retriever, null, null);
            if (advisor == null) {
                fail(name, "buildRetrievalAdvisor returned null");
                return;
            }

            FakeRagChatModel chatModel = new FakeRagChatModel();
            chatModel.enqueue("The Kafka cluster broker timeout should be adjusted to 30000ms.");

            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(advisor)
                    .build();

            ChatResponse response = chatClient.prompt()
                    .user("Kafka cluster timeout")
                    .call()
                    .chatResponse();

            if (response == null || !response.getResult().getOutput().getText().contains("Kafka cluster broker timeout")) {
                fail(name, "ChatClient response did not return expected generated text");
                return;
            }

            // Verify the advisor populated DOCUMENT_CONTEXT metadata
            Object docContext = response.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
            if (docContext == null) {
                fail(name, "ChatResponse metadata missing 'rag_document_context'");
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }

    private static void testScenario10(RagAdvisorUnderTest underTest) {
        String name = "Scenario 10: Guardrailed Enterprise Multi-Tenant RAG Gateway";
        try {
            VectorStore store = createPopulatedStore();
            FakeRagChatModel chatModel = new FakeRagChatModel();
            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

            EnterpriseRagGatewayConfig config = new EnterpriseRagGatewayConfig(3, 0.4, 2, 2, false);

            // Subtest A: Missing tenant / query
            try {
                underTest.executeGatewayQuery(config, new RagGatewayRequest("", 2, "query"), chatClientBuilder, store);
                fail(name, "Expected RagGatewayBreachException for empty tenant");
                return;
            } catch (RagGatewayBreachException expected) {
                // pass
            }

            // Subtest B: Clearance violation
            try {
                underTest.executeGatewayQuery(config, new RagGatewayRequest("fintech", 1, "Kafka timeout"), chatClientBuilder, store);
                fail(name, "Expected RagGatewayBreachException for insufficient clearance");
                return;
            } catch (RagGatewayBreachException expected) {
                // pass
            }

            // Subtest C: Empty context when allowEmptyContext = false
            try {
                underTest.executeGatewayQuery(config, new RagGatewayRequest("nonexistent", 5, "unknown query"), chatClientBuilder, store);
                fail(name, "Expected RagGatewayBreachException when no docs found and allowEmptyContext=false");
                return;
            } catch (RagGatewayBreachException expected) {
                // pass
            }

            // Subtest D: Empty context when allowEmptyContext = true
            EnterpriseRagGatewayConfig allowEmptyConfig = new EnterpriseRagGatewayConfig(3, 0.4, 2, 2, true);
            RagGatewayResponse emptyResp = underTest.executeGatewayQuery(allowEmptyConfig, new RagGatewayRequest("fintech", 5, "quantum physics non matching query"), chatClientBuilder, store);
            if (emptyResp == null || !emptyResp.answer().contains("No confidential documentation found") || !emptyResp.citations().isEmpty()) {
                fail(name, "Expected polite fallback with empty citations when allowEmptyContext=true, got: " + emptyResp);
                return;
            }

            // Subtest E: Successful execution
            chatModel.enqueue("Recommended Kafka consumer session timeout is 45000ms based on cluster guide.");
            chatModel.setTokenUsage(200, 50);

            RagGatewayResponse successResp = underTest.executeGatewayQuery(config, new RagGatewayRequest("fintech", 2, "Kafka timeout"), chatClientBuilder, store);
            if (successResp == null || !successResp.success()) {
                fail(name, "Expected successful gateway response");
                return;
            }

            if (!successResp.answer().contains("Recommended Kafka consumer")) {
                fail(name, "Expected gateway answer, got: " + successResp.answer());
                return;
            }

            if (successResp.citations().isEmpty() || !successResp.citations().get(0).documentId().equals("doc-kafka-01")) {
                fail(name, "Expected citation for doc-kafka-01, got: " + successResp.citations());
                return;
            }

            if (successResp.totalTokens() != 250) {
                fail(name, "Expected 250 total tokens, got: " + successResp.totalTokens());
                return;
            }

            pass(name);
        } catch (Throwable t) {
            fail(name, t.getMessage());
        }
    }
}
