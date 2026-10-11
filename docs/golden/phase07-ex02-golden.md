# Phase 07 Exercise 02: Golden Reference Solution

## Overview
This golden solution implements **Modular RAG Advisors, Context Augmenters, Document PostProcessors, Query Transformers & RetrievalAugmentationAdvisor Pipelines** across all 15 scenarios using native Spring AI 2.0.1 components (`org.springframework.ai.rag.*`).

Structured into 5 core topics (3 repetitive variations each):
1. `VectorStoreDocumentRetriever`: Basic top-K, similarity threshold gating, metadata filter expression (Scenarios 1–3)
2. `ContextualQueryAugmenter`: Custom doc formatter, custom prompt template placeholders, fallback empty context guard (Scenarios 4–6)
3. `DocumentPostProcessor`: ID deduplication, metadata priority ranking, score cutoff filtering (Scenarios 7–9)
4. Pre-Retrieval Query Transformers: `MultiQueryExpander`, `RewriteQueryTransformer`, `TranslationQueryTransformer` (Scenarios 10–12)
5. `RetrievalAugmentationAdvisor`: Basic retriever binding, retriever + augmenter, full end-to-end pipeline (Scenarios 13–15)

Verified: `15 PASSED, 0 FAILED (exit code 0)`

---

## File: `phase07-ex02-rag-advisors/src/main/java/phase07/RagAdvisorUnderTest.java`

```java
package phase07;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
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

import java.util.*;
import java.util.function.Function;

/**
 * Golden implementation for Phase 07 Exercise 02:
 * Modular RAG Advisors, Pre/Post Retrieval Pipelines and ChatClient Integration.
 */
public class RagAdvisorUnderTest {

    public DocumentRetriever buildBasicRetriever(VectorStore store, int topK) {
        if (store == null || topK < 1) {
            throw new IllegalArgumentException("store cannot be null and topK must be >= 1");
        }
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .build();
    }

    public DocumentRetriever buildThresholdRetriever(VectorStore store, int topK, double threshold) {
        if (store == null || topK < 1) {
            throw new IllegalArgumentException("store cannot be null and topK must be >= 1");
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0");
        }
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();
    }

    public DocumentRetriever buildFilterExpressionRetriever(VectorStore store, int topK, Filter.Expression filterExpression) {
        if (store == null || topK < 1 || filterExpression == null) {
            throw new IllegalArgumentException("Parameters cannot be null and topK must be >= 1");
        }
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .filterExpression(filterExpression)
                .build();
    }

    public QueryAugmenter buildCustomFormattedAugmenter(String headerPrefix, boolean allowEmpty) {
        if (headerPrefix == null) {
            throw new IllegalArgumentException("headerPrefix cannot be null");
        }
        Function<List<Document>, String> formatter = docs -> {
            if (docs == null || docs.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < docs.size(); i++) {
                Document d = docs.get(i);
                String src = (String) d.getMetadata().getOrDefault("source", "unknown");
                sb.append(headerPrefix).append(src).append(": ").append(d.getText());
                if (i < docs.size() - 1) {
                    sb.append("\n\n");
                }
            }
            return sb.toString();
        };

        return ContextualQueryAugmenter.builder()
                .documentFormatter(formatter)
                .allowEmptyContext(allowEmpty)
                .build();
    }

    public QueryAugmenter buildTemplatedAugmenter(PromptTemplate customPromptTemplate) {
        if (customPromptTemplate == null) {
            throw new IllegalArgumentException("customPromptTemplate cannot be null");
        }
        return ContextualQueryAugmenter.builder()
                .promptTemplate(customPromptTemplate)
                .build();
    }

    public QueryAugmenter buildFallbackGuardedAugmenter(String fallbackMessage) {
        if (fallbackMessage == null || fallbackMessage.isBlank()) {
            throw new IllegalArgumentException("fallbackMessage cannot be blank");
        }
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(new PromptTemplate(fallbackMessage))
                .build();
    }

    public DocumentPostProcessor buildDeduplicatingPostProcessor() {
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) return List.of();
            Set<String> seen = new HashSet<>();
            List<Document> result = new ArrayList<>();
            for (Document doc : documents) {
                if (seen.add(doc.getId())) {
                    result.add(doc);
                }
            }
            return result;
        };
    }

    public DocumentPostProcessor buildPriorityRankingPostProcessor(int maxDocs) {
        if (maxDocs < 1) {
            throw new IllegalArgumentException("maxDocs must be >= 1");
        }
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) return List.of();
            Set<String> seen = new HashSet<>();
            List<Document> deduplicated = new ArrayList<>();
            for (Document doc : documents) {
                if (seen.add(doc.getId())) {
                    deduplicated.add(doc);
                }
            }
            deduplicated.sort((a, b) -> {
                int p1 = ((Number) a.getMetadata().getOrDefault("priority", 0)).intValue();
                int p2 = ((Number) b.getMetadata().getOrDefault("priority", 0)).intValue();
                return Integer.compare(p2, p1);
            });
            return deduplicated.stream().limit(maxDocs).toList();
        };
    }

    public DocumentPostProcessor buildScoreGatingPostProcessor(double minScore) {
        if (minScore < 0.0 || minScore > 1.0) {
            throw new IllegalArgumentException("minScore must be between 0.0 and 1.0");
        }
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) return List.of();
            return documents.stream()
                    .filter(d -> d.getScore() != null && d.getScore() >= minScore)
                    .toList();
        };
    }

    public QueryExpander buildMultiQueryExpander(ChatClient.Builder chatClientBuilder, int numberOfQueries, boolean includeOriginal) {
        if (chatClientBuilder == null || numberOfQueries < 1) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(numberOfQueries)
                .includeOriginal(includeOriginal)
                .build();
    }

    public QueryTransformer buildRewriteTransformer(ChatClient.Builder chatClientBuilder, String targetSearchSystem) {
        if (chatClientBuilder == null || targetSearchSystem == null || targetSearchSystem.isBlank()) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetSearchSystem(targetSearchSystem)
                .build();
    }

    public QueryTransformer buildTranslationTransformer(ChatClient.Builder chatClientBuilder, String targetLanguage) {
        if (chatClientBuilder == null || targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage(targetLanguage)
                .build();
    }

    public RetrievalAugmentationAdvisor buildBasicAdvisor(DocumentRetriever retriever) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever cannot be null");
        }
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
    }

    public RetrievalAugmentationAdvisor buildAugmentedAdvisor(DocumentRetriever retriever, QueryAugmenter queryAugmenter) {
        if (retriever == null || queryAugmenter == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    public RetrievalAugmentationAdvisor buildFullPipelineAdvisor(
            DocumentRetriever retriever,
            QueryExpander queryExpander,
            DocumentPostProcessor postProcessor,
            QueryAugmenter queryAugmenter
    ) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever cannot be null");
        }
        RetrievalAugmentationAdvisor.Builder b = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever);
        if (queryExpander != null) b.queryExpander(queryExpander);
        if (postProcessor != null) b.documentPostProcessors(postProcessor);
        if (queryAugmenter != null) b.queryAugmenter(queryAugmenter);
        return b.build();
    }
}
```
