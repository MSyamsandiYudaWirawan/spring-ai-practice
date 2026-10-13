package phase07;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Exercise implementation under test for Phase 07 Exercise 02:
 * Modular RAG Advisors, Pre/Post Retrieval Pipelines and ChatClient Integration.
 * <p>
 * 15 repetitive muscle-memory drills across 5 core topics (3 repetitions each).
 * Implement all 15 scenarios in this file.
 */
public class RagAdvisorUnderTest {

    // =========================================================================
    // TOPIC 1: VectorStoreDocumentRetriever (Scenarios 1 - 3)
    // =========================================================================

    /**
     * Scenario 01: Basic VectorStoreDocumentRetriever with Top-K.
     * <p>
     * Instructions:
     * - Validate store != null and topK >= 1; throw {@link IllegalArgumentException} otherwise.
     * - Build and return VectorStoreDocumentRetriever using VectorStoreDocumentRetriever.builder():
     * - vectorStore(store)
     * - topK(topK)
     * - build()
     */
    public DocumentRetriever buildBasicRetriever(VectorStore store, int topK) {
        // DEFECT (Scenario 1): Returns null
        if (store == null || topK < 1) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .build();
    }

    /**
     * Scenario 02: Threshold-Gated VectorStoreDocumentRetriever.
     * <p>
     * Instructions:
     * - Validate store != null, topK >= 1, and 0.0 <= threshold <= 1.0; throw {@link IllegalArgumentException} otherwise.
     * - Build and return VectorStoreDocumentRetriever using:
     * - vectorStore(store)
     * - topK(topK)
     * - similarityThreshold(threshold)
     * - build()
     */
    public DocumentRetriever buildThresholdRetriever(VectorStore store, int topK, double threshold) {
        // DEFECT (Scenario 2): Returns null
        if (store == null || topK < 1 || threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Invalid input parameters");
        }

        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();
    }

    /**
     * Scenario 03: Metadata Filter-Constrained VectorStoreDocumentRetriever.
     * <p>
     * Instructions:
     * - Validate store != null, topK >= 1, and filterExpression != null; throw {@link IllegalArgumentException} otherwise.
     * - Build and return VectorStoreDocumentRetriever using:
     * - vectorStore(store)
     * - topK(topK)
     * - filterExpression(filterExpression)
     * - build()
     */
    public DocumentRetriever buildFilterExpressionRetriever(VectorStore store, int topK, Filter.Expression filterExpression) {
        // DEFECT (Scenario 3): Returns null
        if (store == null || topK < 1 || filterExpression == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }

        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .filterExpression(filterExpression)
                .build();
    }

    // =========================================================================
    // TOPIC 2: ContextualQueryAugmenter & Prompt Augmentation (Scenarios 4 - 6)
    // =========================================================================

    /**
     * Scenario 04: Custom Document Formatter Augmenter.
     * <p>
     * Instructions:
     * - Validate headerPrefix != null; throw {@link IllegalArgumentException} otherwise.
     * - Create a document formatter Function<List<Document>, String>:
     * - For each Document doc in the list:
     * headerPrefix + doc.getMetadata().getOrDefault("source", "unknown") + ": " + doc.getText()
     * - Join the formatted documents with "\n\n".
     * - Build and return ContextualQueryAugmenter using:
     * - documentFormatter(formatter)
     * - allowEmptyContext(allowEmpty)
     * - build()
     */
    public QueryAugmenter buildCustomFormattedAugmenter(String headerPrefix, boolean allowEmpty) {
        // DEFECT (Scenario 4): Returns null
        if (headerPrefix == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        Function<List<Document>, String> formatter = docs -> {
            StringBuilder res = new StringBuilder();

            for (Document doc : docs) {
                if (!res.isEmpty()) {
                    res.append("\n\n");
                }
                res.append(headerPrefix)
                        .append(doc.getMetadata().getOrDefault("source", "unknown"))
                        .append(": ")
                        .append(doc.getText());
            }
            return res.toString();
        };

        return ContextualQueryAugmenter.builder()
                .documentFormatter(formatter)
                .allowEmptyContext(allowEmpty)
                .build();
    }

    /**
     * Scenario 05: Templated Augmenter with Custom Placeholders.
     * <p>
     * Instructions:
     * - Validate customPromptTemplate != null; throw {@link IllegalArgumentException} otherwise.
     * - Build and return ContextualQueryAugmenter using:
     * - promptTemplate(customPromptTemplate)
     * - build()
     */
    public QueryAugmenter buildTemplatedAugmenter(PromptTemplate customPromptTemplate) {
        // DEFECT (Scenario 5): Returns null
        if (customPromptTemplate == null) {
            throw new IllegalArgumentException("Invalid input parameters");

        }
        return ContextualQueryAugmenter.builder()
                .promptTemplate(customPromptTemplate)
                .build();
    }

    /**
     * Scenario 06: Fallback-Guarded Empty Context Augmenter.
     * <p>
     * Instructions:
     * - Validate fallbackMessage != null and !fallbackMessage.isBlank(); throw {@link IllegalArgumentException} otherwise.
     * - Build and return ContextualQueryAugmenter using:
     * - allowEmptyContext(false)
     * - emptyContextPromptTemplate(new PromptTemplate(fallbackMessage))
     * - build()
     */
    public QueryAugmenter buildFallbackGuardedAugmenter(String fallbackMessage) {
        // DEFECT (Scenario 6): Returns null
        if (fallbackMessage == null || fallbackMessage.isBlank()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(new PromptTemplate(fallbackMessage))
                .build();
    }

    // =========================================================================
    // TOPIC 3: DocumentPostProcessor Filtering & Reranking (Scenarios 7 - 9)
    // =========================================================================

    /**
     * Scenario 07: Deduplicating DocumentPostProcessor.
     * <p>
     * Instructions:
     * - Return a DocumentPostProcessor implementation ((query, documents) -> List<Document>):
     * - If documents is null or empty, return List.of().
     * - Deduplicate documents by doc.getId() while preserving first-seen order.
     * - Return deduplicated list.
     */
    public DocumentPostProcessor buildDeduplicatingPostProcessor() {
        // DEFECT (Scenario 7): Returns null
        DocumentPostProcessor processor = (query, documents) -> {
            if (documents == null || documents.isEmpty()) {
                return List.of();
            }
            Set<String> visited = new HashSet<>();
            List<Document> res = new ArrayList<>();
            for (Document doc : documents) {
                if (visited.contains(doc.getId())) {
                    continue;
                }
                visited.add(doc.getId());
                res.add(doc);
            }
            return res;
        };
        return processor;
    }

    /**
     * Scenario 08: Priority-Ranking DocumentPostProcessor.
     * <p>
     * Instructions:
     * - Validate maxDocs >= 1; throw {@link IllegalArgumentException} otherwise.
     * - Return a DocumentPostProcessor implementation ((query, documents) -> List<Document>):
     * - If documents is null or empty, return List.of().
     * - Deduplicate documents by doc.getId().
     * - Sort descending by integer metadata "priority" (doc.getMetadata().getOrDefault("priority", 0)).
     * - Limit results to maxDocs.
     * - Return sorted list.
     */
    public DocumentPostProcessor buildPriorityRankingPostProcessor(int maxDocs) {
        if (maxDocs < 1) throw new IllegalArgumentException("maxDocs must be >= 1");
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) {
                return List.of();
            }

            Set<String> seen = new HashSet<>();
            List<Document> res = new ArrayList<>();
            for (Document doc : documents) {
                if (seen.contains(doc.getId())) continue;
                seen.add(doc.getId());
                res.add(doc);
            }
            res.sort((a, b) -> (Integer) b.getMetadata().getOrDefault("priority", 0) -
                    (Integer) a.getMetadata().getOrDefault("priority", 0));

            return res.size() > maxDocs ? res.subList(0, maxDocs) : res;
        };
    }

    /**
     * Scenario 09: Score-Gating DocumentPostProcessor.
     * <p>
     * Instructions:
     * - Validate 0.0 <= minScore <= 1.0; throw {@link IllegalArgumentException} otherwise.
     * - Return a DocumentPostProcessor implementation ((query, documents) -> List<Document>):
     * - If documents is null or empty, return List.of().
     * - Filter out documents where doc.getScore() == null or doc.getScore() < minScore.
     * - Return matching list.
     */
    public DocumentPostProcessor buildScoreGatingPostProcessor(double minScore) {
        // DEFECT (Scenario 9): Returns null
        if (minScore < 0.0 || minScore > 1.0) {
            throw new IllegalArgumentException("minScore must be between 0.0 and 1.0");
        }
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) {
                return List.of();
            }
            List<Document> res = new ArrayList<>();
            for (Document doc : documents) {
                if (doc.getScore() == null || doc.getScore() < minScore) {
                    continue;
                }
                res.add(doc);
            }
            return res;
        };
    }

    // =========================================================================
    // TOPIC 4: Pre-Retrieval Query Transformers & Expanders (Scenarios 10 - 12)
    // =========================================================================

    /**
     * Scenario 10: MultiQueryExpander Builder.
     * <p>
     * Instructions:
     * - Validate chatClientBuilder != null and numberOfQueries >= 1; throw {@link IllegalArgumentException} otherwise.
     * - Build and return MultiQueryExpander using MultiQueryExpander.builder():
     * - chatClientBuilder(chatClientBuilder)
     * - numberOfQueries(numberOfQueries)
     * - includeOriginal(includeOriginal)
     * - build()
     */
    public QueryExpander buildMultiQueryExpander(ChatClient.Builder chatClientBuilder, int numberOfQueries, boolean includeOriginal) {
        // DEFECT (Scenario 10): Returns null
        if (chatClientBuilder == null || numberOfQueries < 1) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(numberOfQueries)
                .includeOriginal(includeOriginal)
                .build();
    }

    /**
     * Scenario 11: RewriteQueryTransformer Builder.
     * <p>
     * Instructions:
     * - Validate chatClientBuilder != null and targetSearchSystem != null && !targetSearchSystem.isBlank();
     * throw {@link IllegalArgumentException} otherwise.
     * - Build and return RewriteQueryTransformer using RewriteQueryTransformer.builder():
     * - chatClientBuilder(chatClientBuilder)
     * - targetSearchSystem(targetSearchSystem)
     * - build()
     */
    public QueryTransformer buildRewriteTransformer(ChatClient.Builder chatClientBuilder, String targetSearchSystem) {
        // DEFECT (Scenario 11): Returns null
        if (chatClientBuilder == null || targetSearchSystem == null || targetSearchSystem.isBlank()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetSearchSystem(targetSearchSystem)
                .build();
    }

    /**
     * Scenario 12: TranslationQueryTransformer Builder.
     * <p>
     * Instructions:
     * - Validate chatClientBuilder != null and targetLanguage != null && !targetLanguage.isBlank();
     * throw {@link IllegalArgumentException} otherwise.
     * - Build and return TranslationQueryTransformer using TranslationQueryTransformer.builder():
     * - chatClientBuilder(chatClientBuilder)
     * - targetLanguage(targetLanguage)
     * - build()
     */
    public QueryTransformer buildTranslationTransformer(ChatClient.Builder chatClientBuilder, String targetLanguage) {
        // DEFECT (Scenario 12): Returns null
        if (chatClientBuilder == null || targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage(targetLanguage)
                .build();
    }

    // =========================================================================
    // TOPIC 5: RetrievalAugmentationAdvisor & ChatClient Integration (Scenarios 13 - 15)
    // =========================================================================

    /**
     * Scenario 13: Basic RetrievalAugmentationAdvisor.
     * <p>
     * Instructions:
     * - Validate retriever != null; throw {@link IllegalArgumentException} otherwise.
     * - Build and return RetrievalAugmentationAdvisor using RetrievalAugmentationAdvisor.builder():
     * - documentRetriever(retriever)
     * - build()
     */
    public RetrievalAugmentationAdvisor buildBasicAdvisor(DocumentRetriever retriever) {
        // DEFECT (Scenario 13): Returns null
        if (retriever == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
    }

    /**
     * Scenario 14: Augmented RetrievalAugmentationAdvisor.
     * <p>
     * Instructions:
     * - Validate retriever != null and queryAugmenter != null; throw {@link IllegalArgumentException} otherwise.
     * - Build and return RetrievalAugmentationAdvisor using RetrievalAugmentationAdvisor.builder():
     * - documentRetriever(retriever)
     * - queryAugmenter(queryAugmenter)
     * - build()
     */
    public RetrievalAugmentationAdvisor buildAugmentedAdvisor(DocumentRetriever retriever, QueryAugmenter queryAugmenter) {
        // DEFECT (Scenario 14): Returns null
        if (retriever == null || queryAugmenter == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    /**
     * Scenario 15: Full-Pipeline RetrievalAugmentationAdvisor.
     * <p>
     * Instructions:
     * - Validate retriever != null; throw {@link IllegalArgumentException} otherwise.
     * - Build and return RetrievalAugmentationAdvisor using RetrievalAugmentationAdvisor.builder():
     * - documentRetriever(retriever)
     * - if queryExpander != null: queryExpander(queryExpander)
     * - if postProcessor != null: documentPostProcessors(postProcessor)
     * - if queryAugmenter != null: queryAugmenter(queryAugmenter)
     * - build()
     */
    public RetrievalAugmentationAdvisor buildFullPipelineAdvisor(
            DocumentRetriever retriever,
            QueryExpander queryExpander,
            DocumentPostProcessor postProcessor,
            QueryAugmenter queryAugmenter
    ) {
        // DEFECT (Scenario 15): Returns null
        if (retriever == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        RetrievalAugmentationAdvisor.Builder builder = RetrievalAugmentationAdvisor.builder();
        if (queryExpander != null) builder.queryExpander(queryExpander);
        if (postProcessor != null) builder.documentPostProcessors(postProcessor);
        if (queryAugmenter != null) builder.queryAugmenter(queryAugmenter);

        return builder.documentRetriever(retriever).build();
    }
}
