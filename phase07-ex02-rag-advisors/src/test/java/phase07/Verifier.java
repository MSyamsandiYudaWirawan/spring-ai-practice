package phase07;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Gate Verifier for Phase 07 Exercise 02:
 * Modular RAG Advisors, Pre/Post Retrieval Pipelines and ChatClient Integration.
 * <p>
 * 15 repetitive muscle-memory drills across 5 core topics (3 repetitions each).
 * <p>
 * DO NOT MODIFY THIS FILE.
 * <p>
 * Exits with status 99 on FAIL (k6 convention: findings detected).
 * Exits with status 0 on PASS (all gates cleared).
 */
public class Verifier {

    public record ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail, List<String> hints) {
        public ScenarioResult(int scenarioNumber, String name, boolean passed, String errorDetail) {
            this(scenarioNumber, name, passed, errorDetail, List.of());
        }
    }

    public static void main(String[] args) {
        int selected = 0;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                selected = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        List<ScenarioResult> results = runScenarios(selected);
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
        RagAdvisorUnderTest underTest = new RagAdvisorUnderTest();
        List<ScenarioResult> list = new ArrayList<>();
        if (selected == 0 || selected == 1) list.add(verifyScenario1(underTest));
        if (selected == 0 || selected == 2) list.add(verifyScenario2(underTest));
        if (selected == 0 || selected == 3) list.add(verifyScenario3(underTest));
        if (selected == 0 || selected == 4) list.add(verifyScenario4(underTest));
        if (selected == 0 || selected == 5) list.add(verifyScenario5(underTest));
        if (selected == 0 || selected == 6) list.add(verifyScenario6(underTest));
        if (selected == 0 || selected == 7) list.add(verifyScenario7(underTest));
        if (selected == 0 || selected == 8) list.add(verifyScenario8(underTest));
        if (selected == 0 || selected == 9) list.add(verifyScenario9(underTest));
        if (selected == 0 || selected == 10) list.add(verifyScenario10(underTest));
        if (selected == 0 || selected == 11) list.add(verifyScenario11(underTest));
        if (selected == 0 || selected == 12) list.add(verifyScenario12(underTest));
        if (selected == 0 || selected == 13) list.add(verifyScenario13(underTest));
        if (selected == 0 || selected == 14) list.add(verifyScenario14(underTest));
        if (selected == 0 || selected == 15) list.add(verifyScenario15(underTest));
        return list;
    }

    // =========================================================================
    // TOPIC 1: VectorStoreDocumentRetriever (Scenarios 1 - 3)
    // =========================================================================

    private static ScenarioResult verifyScenario1(RagAdvisorUnderTest underTest) {
        String name = "VectorStoreDocumentRetriever (Basic Top-K Retrieval)";
        List<String> hints = List.of(
                "VectorStoreDocumentRetriever adapts a Spring AI VectorStore into the DocumentRetriever interface.",
                "Throw IllegalArgumentException if vectorStore is null or topK < 1.",
                "VectorStoreDocumentRetriever.builder().vectorStore(store).topK(topK).build();"
        );
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("d1").text("Kubernetes crash loop backoff").build(),
                    Document.builder().id("d2").text("Kubernetes pod eviction").build()
            ));

            try {
                underTest.buildBasicRetriever(null, 2);
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on null store", hints);
            } catch (IllegalArgumentException expected) {}

            try {
                underTest.buildBasicRetriever(store, 0);
                return new ScenarioResult(1, name, false, "Expected IllegalArgumentException on topK < 1", hints);
            } catch (IllegalArgumentException expected) {}

            DocumentRetriever retriever = underTest.buildBasicRetriever(store, 2);
            if (retriever == null) {
                return new ScenarioResult(1, name, false, "buildBasicRetriever returned null", hints);
            }

            List<Document> retrieved = retriever.retrieve(new Query("Kubernetes"));
            if (retrieved.isEmpty()) {
                return new ScenarioResult(1, name, false, "Retriever returned empty documents", hints);
            }
            return new ScenarioResult(1, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(1, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario2(RagAdvisorUnderTest underTest) {
        String name = "VectorStoreDocumentRetriever (Threshold-Gated Cutoff)";
        List<String> hints = List.of(
                "VectorStoreDocumentRetriever supports similarityThreshold for cutoff filtering.",
                "Validate similarityThreshold is within [0.0, 1.0]; throw IllegalArgumentException if out of bounds.",
                "VectorStoreDocumentRetriever.builder().vectorStore(store).topK(topK).similarityThreshold(threshold).build();"
        );
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("match").text("PostgreSQL high replication lag").build(),
                    Document.builder().id("other").text("Unrelated CSS styling colors").build()
            ));

            try {
                underTest.buildThresholdRetriever(store, 5, -0.1);
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException on threshold < 0.0", hints);
            } catch (IllegalArgumentException expected) {}

            try {
                underTest.buildThresholdRetriever(store, 5, 1.5);
                return new ScenarioResult(2, name, false, "Expected IllegalArgumentException on threshold > 1.0", hints);
            } catch (IllegalArgumentException expected) {}

            DocumentRetriever retriever = underTest.buildThresholdRetriever(store, 5, 0.9);
            if (retriever == null) {
                return new ScenarioResult(2, name, false, "buildThresholdRetriever returned null", hints);
            }

            List<Document> docs = retriever.retrieve(new Query("PostgreSQL high replication lag"));
            if (docs.isEmpty()) {
                return new ScenarioResult(2, name, false, "Expected high-similarity document to be retrieved", hints);
            }
            if (docs.stream().anyMatch(d -> "other".equals(d.getId()))) {
                return new ScenarioResult(2, name, false, "Unrelated document should have been filtered by threshold", hints);
            }
            return new ScenarioResult(2, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(2, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario3(RagAdvisorUnderTest underTest) {
        String name = "VectorStoreDocumentRetriever (FilterExpression Constrained)";
        List<String> hints = List.of(
                "VectorStoreDocumentRetriever can be constrained by a Filter.Expression.",
                "Throw IllegalArgumentException if filterExpression is null.",
                "VectorStoreDocumentRetriever.builder().vectorStore(store).topK(topK).filterExpression(filter).build();"
        );
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            store.add(List.of(
                    Document.builder().id("d-prod").text("Database deadlock detected").metadata(Map.of("env", "PROD")).build(),
                    Document.builder().id("d-dev").text("Database deadlock detected").metadata(Map.of("env", "DEV")).build()
            ));

            Filter.Expression filter = new FilterExpressionBuilder().eq("env", "PROD").build();

            try {
                underTest.buildFilterExpressionRetriever(store, 5, null);
                return new ScenarioResult(3, name, false, "Expected IllegalArgumentException on null filter", hints);
            } catch (IllegalArgumentException expected) {}

            DocumentRetriever retriever = underTest.buildFilterExpressionRetriever(store, 5, filter);
            if (retriever == null) {
                return new ScenarioResult(3, name, false, "buildFilterExpressionRetriever returned null", hints);
            }

            List<Document> docs = retriever.retrieve(new Query("Database deadlock"));
            if (docs.size() != 1 || !"d-prod".equals(docs.get(0).getId())) {
                return new ScenarioResult(3, name, false, "Expected only d-prod to match filter expression, got: " + docs.stream().map(Document::getId).toList(), hints);
            }
            return new ScenarioResult(3, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(3, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 2: ContextualQueryAugmenter & Prompt Augmentation (Scenarios 4 - 6)
    // =========================================================================

    private static ScenarioResult verifyScenario4(RagAdvisorUnderTest underTest) {
        String name = "ContextualQueryAugmenter (Custom Document Formatter)";
        List<String> hints = List.of(
                "ContextualQueryAugmenter formats retrieved documents into text and injects them into the user prompt.",
                "Use ContextualQueryAugmenter.builder().documentFormatter(...) with custom prefix and source tag.",
                "ContextualQueryAugmenter.builder().documentFormatter(docs -> docs.stream().map(d -> prefix + d.getMetadata().getOrDefault(\"source\", \"unknown\") + \": \" + d.getText()).collect(Collectors.joining(\"\\n\"))).build();"
        );
        try {
            try {
                underTest.buildCustomFormattedAugmenter(null, true);
                return new ScenarioResult(4, name, false, "Expected IllegalArgumentException on null headerPrefix", hints);
            } catch (IllegalArgumentException expected) {}

            QueryAugmenter augmenter = underTest.buildCustomFormattedAugmenter("SOURCE_", true);
            if (augmenter == null) {
                return new ScenarioResult(4, name, false, "buildCustomFormattedAugmenter returned null", hints);
            }

            List<Document> docs = List.of(
                    Document.builder().id("1").text("CPU spike").metadata(Map.of("source", "datadog")).build(),
                    Document.builder().id("2").text("Disk full").metadata(Map.of()).build()
            );

            Query augmented = augmenter.augment(new Query("diagnose issue"), docs);
            if (augmented == null || augmented.text() == null) {
                return new ScenarioResult(4, name, false, "Augmenter returned null augmented query", hints);
            }
            if (!augmented.text().contains("SOURCE_datadog: CPU spike")) {
                return new ScenarioResult(4, name, false, "Formatted document missing expected prefix and source", hints);
            }
            if (!augmented.text().contains("SOURCE_unknown: Disk full")) {
                return new ScenarioResult(4, name, false, "Default source 'unknown' not applied", hints);
            }
            return new ScenarioResult(4, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(4, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario5(RagAdvisorUnderTest underTest) {
        String name = "ContextualQueryAugmenter (Custom PromptTemplate Placeholders)";
        List<String> hints = List.of(
                "ContextualQueryAugmenter allows overriding the prompt template containing {context} and {query} placeholders.",
                "Validate promptTemplate is not null.",
                "ContextualQueryAugmenter.builder().promptTemplate(promptTemplate).build();"
        );
        try {
            try {
                underTest.buildTemplatedAugmenter(null);
                return new ScenarioResult(5, name, false, "Expected IllegalArgumentException on null promptTemplate", hints);
            } catch (IllegalArgumentException expected) {}

            PromptTemplate template = new PromptTemplate("=== KNOWLEDGE ===\n{context}\n=== QUESTION ===\n{query}");
            QueryAugmenter augmenter = underTest.buildTemplatedAugmenter(template);
            if (augmenter == null) {
                return new ScenarioResult(5, name, false, "buildTemplatedAugmenter returned null", hints);
            }

            List<Document> docs = List.of(Document.builder().id("1").text("Root cause is OOM.").build());
            Query augmented = augmenter.augment(new Query("What happened?"), docs);

            if (augmented == null || !augmented.text().contains("=== KNOWLEDGE ===") || !augmented.text().contains("Root cause is OOM.")) {
                return new ScenarioResult(5, name, false, "Custom template markers not found in augmented query text", hints);
            }
            return new ScenarioResult(5, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(5, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario6(RagAdvisorUnderTest underTest) {
        String name = "ContextualQueryAugmenter (Fallback Empty Context Guard)";
        List<String> hints = List.of(
                "ContextualQueryAugmenter can supply an explicit fallback message when no documents are retrieved.",
                "Reject blank fallback messages with IllegalArgumentException.",
                "ContextualQueryAugmenter.builder().emptyContextText(fallbackMessage).build();"
        );
        try {
            try {
                underTest.buildFallbackGuardedAugmenter("   ");
                return new ScenarioResult(6, name, false, "Expected IllegalArgumentException on blank fallbackMessage", hints);
            } catch (IllegalArgumentException expected) {}

            QueryAugmenter augmenter = underTest.buildFallbackGuardedAugmenter("No relevant documentation was found for this query.");
            if (augmenter == null) {
                return new ScenarioResult(6, name, false, "buildFallbackGuardedAugmenter returned null", hints);
            }

            Query augmentedEmpty = augmenter.augment(new Query("find secrets"), List.of());
            if (augmentedEmpty == null || !augmentedEmpty.text().contains("No relevant documentation was found")) {
                return new ScenarioResult(6, name, false, "Empty context fallback message was not applied", hints);
            }
            return new ScenarioResult(6, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(6, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 3: DocumentPostProcessor Filtering & Reranking (Scenarios 7 - 9)
    // =========================================================================

    private static ScenarioResult verifyScenario7(RagAdvisorUnderTest underTest) {
        String name = "DocumentPostProcessor (ID Deduplication)";
        List<String> hints = List.of(
                "DocumentPostProcessor filters or mutates retrieved documents before prompt augmentation.",
                "Deduplicate documents by ID while preserving first-seen order.",
                "(query, docs) -> { Set<String> seen = new HashSet<>(); return docs.stream().filter(d -> seen.add(d.getId())).toList(); };"
        );
        try {
            DocumentPostProcessor postProcessor = underTest.buildDeduplicatingPostProcessor();
            if (postProcessor == null) {
                return new ScenarioResult(7, name, false, "buildDeduplicatingPostProcessor returned null", hints);
            }

            List<Document> duplicates = List.of(
                    Document.builder().id("doc-1").text("version 1").build(),
                    Document.builder().id("doc-2").text("other").build(),
                    Document.builder().id("doc-1").text("version 2 (duplicate)").build()
            );

            List<Document> processed = postProcessor.process(new Query("q"), duplicates);
            if (processed == null || processed.size() != 2) {
                return new ScenarioResult(7, name, false, "Expected 2 documents after deduplication, got: " + (processed == null ? "null" : processed.size()), hints);
            }
            if (!"version 1".equals(processed.get(0).getText())) {
                return new ScenarioResult(7, name, false, "Expected first occurrence to be preserved", hints);
            }
            return new ScenarioResult(7, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(7, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario8(RagAdvisorUnderTest underTest) {
        String name = "DocumentPostProcessor (Metadata Priority Ranking & Truncation)";
        List<String> hints = List.of(
                "DocumentPostProcessor can re-rank retrieved documents by custom metadata properties.",
                "Throw IllegalArgumentException if maxDocs < 1; sort descending by metadata 'priority'.",
                "(query, docs) -> docs.stream().sorted((a, b) -> Integer.compare(getPrio(b), getPrio(a))).limit(maxDocs).toList();"
        );
        try {
            try {
                underTest.buildPriorityRankingPostProcessor(0);
                return new ScenarioResult(8, name, false, "Expected IllegalArgumentException on maxDocs < 1", hints);
            } catch (IllegalArgumentException expected) {}

            DocumentPostProcessor postProcessor = underTest.buildPriorityRankingPostProcessor(2);
            if (postProcessor == null) {
                return new ScenarioResult(8, name, false, "buildPriorityRankingPostProcessor returned null", hints);
            }

            List<Document> unranked = List.of(
                    Document.builder().id("low").text("low prio").metadata(Map.of("priority", 10)).build(),
                    Document.builder().id("high").text("high prio").metadata(Map.of("priority", 99)).build(),
                    Document.builder().id("mid").text("mid prio").metadata(Map.of("priority", 50)).build()
            );

            List<Document> ranked = postProcessor.process(new Query("q"), unranked);
            if (ranked == null || ranked.size() != 2) {
                return new ScenarioResult(8, name, false, "Expected 2 truncated documents, got: " + (ranked == null ? "null" : ranked.size()), hints);
            }
            if (!"high".equals(ranked.get(0).getId()) || !"mid".equals(ranked.get(1).getId())) {
                return new ScenarioResult(8, name, false, "Documents not ordered descending by priority", hints);
            }
            return new ScenarioResult(8, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(8, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario9(RagAdvisorUnderTest underTest) {
        String name = "DocumentPostProcessor (Score-Gated Filtering)";
        List<String> hints = List.of(
                "DocumentPostProcessor can drop documents whose similarity score is below a strict cutoff.",
                "Validate minScore >= 0.0; documents with null scores or score < minScore are discarded.",
                "(query, docs) -> docs.stream().filter(d -> d.getScore() != null && d.getScore() >= minScore).toList();"
        );
        try {
            try {
                underTest.buildScoreGatingPostProcessor(-0.1);
                return new ScenarioResult(9, name, false, "Expected IllegalArgumentException on minScore < 0.0", hints);
            } catch (IllegalArgumentException expected) {}

            DocumentPostProcessor postProcessor = underTest.buildScoreGatingPostProcessor(0.75);
            if (postProcessor == null) {
                return new ScenarioResult(9, name, false, "buildScoreGatingPostProcessor returned null", hints);
            }

            List<Document> scoredDocs = List.of(
                    Document.builder().id("good").text("relevant").score(0.85).build(),
                    Document.builder().id("bad").text("irrelevant").score(0.40).build(),
                    Document.builder().id("missing-score").text("no score").build()
            );

            List<Document> filtered = postProcessor.process(new Query("q"), scoredDocs);
            if (filtered == null || filtered.size() != 1 || !"good".equals(filtered.get(0).getId())) {
                return new ScenarioResult(9, name, false, "Expected only 'good' document with score >= 0.75, got: " + (filtered == null ? "null" : filtered.stream().map(Document::getId).toList()), hints);
            }
            return new ScenarioResult(9, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(9, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 4: Pre-Retrieval Query Transformers & Expanders (Scenarios 10 - 12)
    // =========================================================================

    private static ScenarioResult verifyScenario10(RagAdvisorUnderTest underTest) {
        String name = "MultiQueryExpander (Pre-Retrieval Query Expansion)";
        List<String> hints = List.of(
                "MultiQueryExpander uses an LLM to generate multiple alternative search queries for higher recall.",
                "Throw IllegalArgumentException on null chatClientBuilder or numberOfQueries < 1.",
                "MultiQueryExpander.builder().chatClientBuilder(builder).numberOfQueries(n).includeOriginal(includeOrig).build();"
        );
        try {
            ChatClient.Builder dummyBuilder = ChatClient.builder(new FakeRagChatModel());

            try {
                underTest.buildMultiQueryExpander(null, 3, true);
                return new ScenarioResult(10, name, false, "Expected IllegalArgumentException on null chatClientBuilder", hints);
            } catch (IllegalArgumentException expected) {}

            try {
                underTest.buildMultiQueryExpander(dummyBuilder, 0, true);
                return new ScenarioResult(10, name, false, "Expected IllegalArgumentException on numberOfQueries < 1", hints);
            } catch (IllegalArgumentException expected) {}

            QueryExpander expander = underTest.buildMultiQueryExpander(dummyBuilder, 3, true);
            if (expander == null) {
                return new ScenarioResult(10, name, false, "buildMultiQueryExpander returned null", hints);
            }
            return new ScenarioResult(10, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(10, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario11(RagAdvisorUnderTest underTest) {
        String name = "RewriteQueryTransformer (System-Targeted Query Rewriting)";
        List<String> hints = List.of(
                "RewriteQueryTransformer reframes the user prompt into a query optimized for a target search system.",
                "Validate targetSearchSystem is not blank.",
                "RewriteQueryTransformer.builder().chatClientBuilder(builder).targetSearchSystem(system).build();"
        );
        try {
            ChatClient.Builder dummyBuilder = ChatClient.builder(new FakeRagChatModel());

            try {
                underTest.buildRewriteTransformer(dummyBuilder, "  ");
                return new ScenarioResult(11, name, false, "Expected IllegalArgumentException on blank targetSearchSystem", hints);
            } catch (IllegalArgumentException expected) {}

            QueryTransformer transformer = underTest.buildRewriteTransformer(dummyBuilder, "Elasticsearch BM25");
            if (transformer == null) {
                return new ScenarioResult(11, name, false, "buildRewriteTransformer returned null", hints);
            }
            return new ScenarioResult(11, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(11, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario12(RagAdvisorUnderTest underTest) {
        String name = "TranslationQueryTransformer (Multilingual Target Language Transformation)";
        List<String> hints = List.of(
                "TranslationQueryTransformer translates the user query into the target index language.",
                "Validate targetLanguage is not null or blank.",
                "TranslationQueryTransformer.builder().chatClientBuilder(builder).targetLanguage(lang).build();"
        );
        try {
            ChatClient.Builder dummyBuilder = ChatClient.builder(new FakeRagChatModel());

            try {
                underTest.buildTranslationTransformer(dummyBuilder, "");
                return new ScenarioResult(12, name, false, "Expected IllegalArgumentException on blank targetLanguage", hints);
            } catch (IllegalArgumentException expected) {}

            QueryTransformer transformer = underTest.buildTranslationTransformer(dummyBuilder, "Japanese");
            if (transformer == null) {
                return new ScenarioResult(12, name, false, "buildTranslationTransformer returned null", hints);
            }
            return new ScenarioResult(12, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(12, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // TOPIC 5: RetrievalAugmentationAdvisor & ChatClient Integration (Scenarios 13 - 15)
    // =========================================================================

    private static ScenarioResult verifyScenario13(RagAdvisorUnderTest underTest) {
        String name = "RetrievalAugmentationAdvisor (Basic DocumentRetriever Binding)";
        List<String> hints = List.of(
                "RetrievalAugmentationAdvisor binds document retrieval into the ChatClient execution pipeline.",
                "Throw IllegalArgumentException on null DocumentRetriever.",
                "RetrievalAugmentationAdvisor.builder().documentRetriever(retriever).build();"
        );
        try {
            try {
                underTest.buildBasicAdvisor(null);
                return new ScenarioResult(13, name, false, "Expected IllegalArgumentException on null retriever", hints);
            } catch (IllegalArgumentException expected) {}

            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            DocumentRetriever retriever = VectorStoreDocumentRetriever.builder().vectorStore(store).build();

            RetrievalAugmentationAdvisor advisor = underTest.buildBasicAdvisor(retriever);
            if (advisor == null) {
                return new ScenarioResult(13, name, false, "buildBasicAdvisor returned null", hints);
            }
            return new ScenarioResult(13, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(13, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario14(RagAdvisorUnderTest underTest) {
        String name = "RetrievalAugmentationAdvisor (Retriever & QueryAugmenter Chaining)";
        List<String> hints = List.of(
                "RetrievalAugmentationAdvisor chains a DocumentRetriever with a custom QueryAugmenter.",
                "Throw IllegalArgumentException on null retriever or queryAugmenter.",
                "RetrievalAugmentationAdvisor.builder().documentRetriever(retriever).queryAugmenter(augmenter).build();"
        );
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            DocumentRetriever retriever = VectorStoreDocumentRetriever.builder().vectorStore(store).build();
            QueryAugmenter augmenter = underTest.buildFallbackGuardedAugmenter("Fallback text");

            try {
                underTest.buildAugmentedAdvisor(retriever, null);
                return new ScenarioResult(14, name, false, "Expected IllegalArgumentException on null queryAugmenter", hints);
            } catch (IllegalArgumentException expected) {}

            RetrievalAugmentationAdvisor advisor = underTest.buildAugmentedAdvisor(retriever, augmenter);
            if (advisor == null) {
                return new ScenarioResult(14, name, false, "buildAugmentedAdvisor returned null", hints);
            }
            return new ScenarioResult(14, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(14, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    private static ScenarioResult verifyScenario15(RagAdvisorUnderTest underTest) {
        String name = "RetrievalAugmentationAdvisor (Full Pipeline Assembly)";
        List<String> hints = List.of(
                "RetrievalAugmentationAdvisor can orchestrate pre-retrieval transformers, retrievers, post-processors, and augmenters.",
                "Validate retriever is not null; optionally configure transformer, postProcessors, and augmenter if present.",
                "RetrievalAugmentationAdvisor.builder().documentRetriever(retriever).queryTransformers(...).documentPostProcessors(...).queryAugmenter(...).build();"
        );
        try {
            EmbeddingModel fakeModel = new FakeEmbeddingModel();
            SimpleVectorStore store = SimpleVectorStore.builder(fakeModel).build();
            DocumentRetriever retriever = VectorStoreDocumentRetriever.builder().vectorStore(store).build();
            DocumentPostProcessor postProcessor = underTest.buildDeduplicatingPostProcessor();
            QueryAugmenter augmenter = underTest.buildFallbackGuardedAugmenter("Fallback");

            try {
                underTest.buildFullPipelineAdvisor(null, null, null, null);
                return new ScenarioResult(15, name, false, "Expected IllegalArgumentException on null retriever", hints);
            } catch (IllegalArgumentException expected) {}

            RetrievalAugmentationAdvisor advisor = underTest.buildFullPipelineAdvisor(retriever, null, postProcessor, augmenter);
            if (advisor == null) {
                return new ScenarioResult(15, name, false, "buildFullPipelineAdvisor returned null", hints);
            }
            return new ScenarioResult(15, name, true, "OK", hints);
        } catch (Exception e) {
            return new ScenarioResult(15, name, false, "Exception: " + e.getMessage(), hints);
        }
    }

    // =========================================================================
    // REPORT FORMATTER
    // =========================================================================

    private static void printReport(List<ScenarioResult> results) {
        System.out.println("===============================================================================");
        System.out.println("  PHASE 07 EXERCISE 02: MODULAR RAG ADVISORS & PIPELINES");
        System.out.println("===============================================================================");
        int passed = 0;
        String[] hintLabels = {"[HINT 1 (Concept)]", "[HINT 2 (Edge Case)]", "[HINT 3 (Code Pattern)]"};

        for (ScenarioResult r : results) {
            String tag = r.passed() ? "[PASS]" : "[FAIL]";
            System.out.printf("  %s Scenario %02d: %s%n", tag, r.scenarioNumber(), r.name());
            if (!r.passed()) {
                System.out.printf("         --> DETAIL: %s%n", r.errorDetail());
                if (r.hints() != null && !r.hints().isEmpty()) {
                    for (int i = 0; i < r.hints().size(); i++) {
                        String label = i < hintLabels.length ? hintLabels[i] : String.format("[HINT %d]", i + 1);
                        System.out.printf("         --> %s %s%n", label, r.hints().get(i));
                    }
                }
            } else {
                passed++;
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("  TOTAL: %d / %d PASSED%n", passed, results.size());
        if (passed == results.size()) {
            System.out.println("  VERDICT: ALL 15 GATES PASSED (exit code 0)");
        } else {
            System.out.printf("  VERDICT: %d FINDINGS DETECTED (exit code 99)%n", results.size() - passed);
        }
        System.out.println("===============================================================================");
    }
}
