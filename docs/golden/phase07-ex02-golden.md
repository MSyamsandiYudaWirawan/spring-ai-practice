# Phase 07 Exercise 02: Golden Reference Solution

## Overview
This golden solution implements **Modular RAG Advisors, Pre/Post Retrieval Pipelines, and the Enterprise RAG Gateway** using native Spring AI 2.0.1 RAG components (`org.springframework.ai.rag.*`).

## Verified Solution Code

```java
package phase07;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
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
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import phase07.RagAdvisorContracts.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phase 07 Exercise 02: Modular RAG Advisors, Pre/Post Retrieval Pipelines and Enterprise RAG Gateway.
 */
public class RagAdvisorUnderTest {

    /**
     * Scenario 01: VectorStoreDocumentRetriever Builder & Similarity Threshold.
     */
    public DocumentRetriever buildVectorRetriever(VectorStore store, int topK, double threshold) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();
    }

    /**
     * Scenario 02: Tenant-Isolated FilterExpression Retrieval.
     */
    public DocumentRetriever buildTenantFilteredRetriever(VectorStore store, String tenantId, int topK) {
        Filter.Expression filter = new FilterExpressionBuilder().eq("tenant", tenantId).build();
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(topK)
                .filterExpression(filter)
                .build();
    }

    /**
     * Scenario 03: Custom ContextualQueryAugmenter & Document Formatting.
     */
    public QueryAugmenter buildCustomAugmenter(String headerPrefix, boolean allowEmpty) {
        Function<List<Document>, String> formatter = docs -> {
            if (docs == null || docs.isEmpty()) return "";
            return docs.stream()
                    .map(d -> headerPrefix + d.getMetadata().getOrDefault("source", "unknown") + ": " + d.getText())
                    .collect(Collectors.joining("\n\n"));
        };

        PromptTemplate template = new PromptTemplate(
                "Context information is below.\n---------------------\n{context}\n---------------------\nGiven the context, answer the query: {query}"
        );

        return ContextualQueryAugmenter.builder()
                .promptTemplate(template)
                .documentFormatter(formatter)
                .allowEmptyContext(allowEmpty)
                .build();
    }

    /**
     * Scenario 04: Empty Context Guardrails & Fallback Prompting.
     */
    public QueryAugmenter buildFallbackAugmenter(String fallbackMessage) {
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(new PromptTemplate(fallbackMessage))
                .build();
    }

    /**
     * Scenario 05: DocumentPostProcessor Deduplication & Priority Reranking.
     */
    public DocumentPostProcessor buildDeduplicatingPriorityReranker(int maxDocs) {
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) return List.of();
            Map<String, Document> unique = new LinkedHashMap<>();
            for (Document d : documents) {
                unique.putIfAbsent(d.getId(), d);
            }
            return unique.values().stream()
                    .sorted((d1, d2) -> {
                        int p1 = ((Number) d1.getMetadata().getOrDefault("priority", 0)).intValue();
                        int p2 = ((Number) d2.getMetadata().getOrDefault("priority", 0)).intValue();
                        return Integer.compare(p2, p1);
                    })
                    .limit(maxDocs)
                    .toList();
        };
    }

    /**
     * Scenario 06: MultiQueryExpander Pre-Retrieval Expansion.
     */
    public QueryExpander buildMultiQueryExpander(ChatClient.Builder chatClientBuilder, int numberOfQueries, boolean includeOriginal) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(numberOfQueries)
                .includeOriginal(includeOriginal)
                .build();
    }

    /**
     * Scenario 07: RewriteQueryTransformer Query Rewriting.
     */
    public QueryTransformer buildRewriteTransformer(ChatClient.Builder chatClientBuilder, String targetSearchSystem) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetSearchSystem(targetSearchSystem)
                .build();
    }

    /**
     * Scenario 08: ConcatenationDocumentJoiner Multi-Query Merging.
     */
    public List<Document> joinMultiQueryResults(Map<Query, List<List<Document>>> queryResults) {
        return new ConcatenationDocumentJoiner().join(queryResults);
    }

    /**
     * Scenario 09: RetrievalAugmentationAdvisor End-to-End ChatClient Binding.
     */
    public RetrievalAugmentationAdvisor buildRetrievalAdvisor(DocumentRetriever retriever, QueryAugmenter augmenter, DocumentPostProcessor postProcessor) {
        RetrievalAugmentationAdvisor.Builder builder = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever);
        if (augmenter != null) {
            builder.queryAugmenter(augmenter);
        }
        if (postProcessor != null) {
            builder.documentPostProcessors(postProcessor);
        }
        return builder.build();
    }

    /**
     * Scenario 10: Guardrailed Enterprise Multi-Tenant RAG Gateway.
     */
    public RagGatewayResponse executeGatewayQuery(
            EnterpriseRagGatewayConfig config,
            RagGatewayRequest request,
            ChatClient.Builder chatClientBuilder,
            VectorStore store
    ) {
        if (request == null || request.userTenant() == null || request.userTenant().isBlank()
                || request.query() == null || request.query().isBlank()) {
            throw new RagGatewayBreachException("Invalid request: missing tenant or query");
        }
        if (request.userClearance() < config.minRequiredClearance()) {
            throw new RagGatewayBreachException("Access denied: clearance " + request.userClearance()
                    + " below required " + config.minRequiredClearance());
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filter = b.and(
                b.eq("tenant", request.userTenant()),
                b.lte("clearance", request.userClearance())
        ).build();

        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(store)
                .topK(config.topK())
                .similarityThreshold(config.similarityThreshold())
                .filterExpression(filter)
                .build();

        List<Document> rawDocs = retriever.retrieve(new Query(request.query()));
        if (rawDocs.isEmpty()) {
            if (!config.allowEmptyContext()) {
                throw new RagGatewayBreachException("Zero documents found for tenant " + request.userTenant()
                        + " matching clearance and similarity threshold");
            }
            return new RagGatewayResponse("No confidential documentation found for your clearance level.", List.of(), 0, true);
        }

        Map<String, Document> deduplicatedMap = new LinkedHashMap<>();
        for (Document d : rawDocs) {
            deduplicatedMap.putIfAbsent(d.getId(), d);
        }
        List<Document> deduplicated = deduplicatedMap.values().stream()
                .limit(config.maxDocs())
                .toList();

        DocumentPostProcessor postProcessor = (q, docs) -> deduplicated;
        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .documentPostProcessors(postProcessor)
                .build();

        ChatResponse chatResponse = chatClientBuilder.build().prompt()
                .advisors(advisor)
                .user(request.query())
                .call()
                .chatResponse();

        List<Citation> citations = deduplicated.stream()
                .map(d -> new Citation(d.getId(), (String) d.getMetadata().getOrDefault("source", "unknown"), d.getScore()))
                .toList();

        int totalTokens = (chatResponse != null && chatResponse.getMetadata().getUsage() != null)
                ? chatResponse.getMetadata().getUsage().getTotalTokens()
                : 0;

        String answer = (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null)
                ? chatResponse.getResult().getOutput().getText()
                : "";

        return new RagGatewayResponse(answer, citations, totalTokens, true);
    }
}
```

## Verification Command
```powershell
mvn clean test-compile exec:java -pl phase07-ex02-rag-advisors
```
Output:
```
VERIFICATION SUMMARY: 10 / 10 PASSED, 0 FAILED
```
