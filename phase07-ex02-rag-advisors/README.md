# Phase 07 Exercise 02: Modular RAG Advisors, Pre/Post Retrieval Pipelines and Enterprise RAG Gateway

## Focus & Learning Objectives
This exercise teaches production RAG design patterns using Spring AI 2.0.1's native modular RAG package (`org.springframework.ai.rag.*`):
- **Document Retrievers:** Building and tuning `VectorStoreDocumentRetriever` with similarity thresholds and metadata filters.
- **Query Transformations:** Rewriting ambiguous user questions using `RewriteQueryTransformer`.
- **Query Expansion:** Expanding single queries into multi-perspective variants via `MultiQueryExpander`.
- **Retrieval Joining:** Deduplicating and ranking multi-query search results via `ConcatenationDocumentJoiner`.
- **Post-Retrieval Processing:** Implementing `DocumentPostProcessor` for metadata-based reranking and deduplication.
- **Contextual Augmentation:** Formatting document context and enforcing empty-context guardrails via `ContextualQueryAugmenter`.
- **End-to-End ChatClient Integration:** Binding `RetrievalAugmentationAdvisor` into Spring AI's `ChatClient`.
- **Enterprise Multi-Tenant Gateway:** Enforcing tenant isolation, clearance gates, citations tracking, and hallucination prevention.

---

## Scenarios
1. **Scenario 01: VectorStoreDocumentRetriever Builder & Similarity Threshold**
   - Configure retriever with topK, similarityThreshold, and underlying `VectorStore`.
2. **Scenario 02: Tenant-Isolated FilterExpression Retrieval**
   - Construct a `Filter.Expression` with `FilterExpressionBuilder` to guarantee multi-tenant data separation.
3. **Scenario 03: Custom ContextualQueryAugmenter & Document Formatting**
   - Format source metadata headers and render `{context}` and `{query}` prompt placeholders.
4. **Scenario 04: Empty Context Guardrails & Fallback Prompting**
   - Configure `ContextualQueryAugmenter` with `allowEmptyContext(false)` and fallback prompting to prevent hallucination.
5. **Scenario 05: DocumentPostProcessor Deduplication & Priority Reranking**
   - Deduplicate documents by ID and sort by metadata `priority` integer descending.
6. **Scenario 06: MultiQueryExpander Pre-Retrieval Expansion**
   - Use `ChatClient.Builder` to expand a query into alternative search queries.
7. **Scenario 07: RewriteQueryTransformer Query Rewriting**
   - Use `ChatClient.Builder` to rewrite vague conversational queries for search indexing.
8. **Scenario 08: ConcatenationDocumentJoiner Multi-Query Merging**
   - Combine multi-query results, deduplicating IDs and sorting by cosine score descending.
9. **Scenario 09: RetrievalAugmentationAdvisor End-to-End ChatClient Binding**
   - Bind advisor into `ChatClient.builder().defaultAdvisors(advisor)`.
10. **Scenario 10: Guardrailed Enterprise Multi-Tenant RAG Gateway**
    - Production RAG gate: validate request, enforce tenant clearance, query vector store, post-process results, call ChatClient, and return citations and token metrics.
11. **Scenario 11: Hypothetical Document Embeddings (HyDE) Query Transformer**
    - Pre-generate synthetic answer passages using an LLM to query the vector store in answer space, with graceful fallback to the original query on empty output.
12. **Scenario 12: Token-Budget Context Packing & Dynamic Truncator**
    - Greedily pack retrieved documents up to a strict context window token ceiling, cleanly truncating oversized documents and auditing packed vs dropped documents.

---

## Verification
Run the offline verification harness:
```powershell
mvn test-compile exec:java -pl phase07-ex02-rag-advisors
```

Or run standard Maven tests:
```powershell
mvn test -pl phase07-ex02-rag-advisors
```

A verified reference implementation is available in `docs/golden/phase07-ex02-golden.md`.
