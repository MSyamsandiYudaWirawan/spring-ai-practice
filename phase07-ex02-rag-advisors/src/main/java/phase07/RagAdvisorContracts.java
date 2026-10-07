package phase07;

import java.util.List;
import java.util.Objects;

/**
 * Domain contracts and data transfer objects for Phase 07 Exercise 02 (RAG Advisors).
 * <p>
 * DO NOT MODIFY THIS FILE. All implementations must reside in {@link RagAdvisorUnderTest}.
 */
public final class RagAdvisorContracts {

    private RagAdvisorContracts() {}

    /**
     * Represents a cited source returned by RAG query execution.
     */
    public record Citation(String documentId, String source, Double score) {
        public Citation {
            Objects.requireNonNull(documentId, "documentId cannot be null");
            source = (source == null || source.isBlank()) ? "unknown" : source;
        }
    }

    /**
     * Configuration parameters for the Enterprise RAG Gateway release gate.
     */
    public record EnterpriseRagGatewayConfig(
            int topK,
            double similarityThreshold,
            int minRequiredClearance,
            int maxDocs,
            boolean allowEmptyContext
    ) {
        public EnterpriseRagGatewayConfig {
            if (topK <= 0) throw new IllegalArgumentException("topK must be > 0");
            if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
                throw new IllegalArgumentException("similarityThreshold must be between 0.0 and 1.0");
            }
            if (maxDocs <= 0) throw new IllegalArgumentException("maxDocs must be > 0");
        }
    }

    /**
     * User query request passed into the Enterprise RAG Gateway.
     */
    public record RagGatewayRequest(
            String userTenant,
            int userClearance,
            String query
    ) {
        public RagGatewayRequest {
            // Nulls allowed at instantiation so Gateway can validate and enforce invariants.
        }
    }

    /**
     * Response returned by the Enterprise RAG Gateway upon successful execution.
     */
    public record RagGatewayResponse(
            String answer,
            List<Citation> citations,
            int totalTokens,
            boolean success
    ) {
        public RagGatewayResponse {
            Objects.requireNonNull(answer, "answer cannot be null");
            citations = citations == null ? List.of() : List.copyOf(citations);
        }
    }

    /**
     * Audit log for Hypothetical Document Embedding (HyDE) query transformation.
     */
    public record HydeTransformationResult(
            String originalQuery,
            String hypotheticalDocument,
            boolean fallbackToOriginal
    ) {
        public HydeTransformationResult {
            Objects.requireNonNull(originalQuery, "originalQuery cannot be null");
            Objects.requireNonNull(hypotheticalDocument, "hypotheticalDocument cannot be null");
        }
    }

    /**
     * Result of packing retrieved documents into a finite token budget.
     */
    public record PackedContext(
            String packedContent,
            int totalEstimatedTokens,
            int packedDocumentCount,
            int droppedDocumentCount,
            List<String> packedDocIds
    ) {
        public PackedContext {
            Objects.requireNonNull(packedContent, "packedContent cannot be null");
            packedDocIds = packedDocIds == null ? List.of() : List.copyOf(packedDocIds);
        }
    }

    /**
     * Exception thrown when the Enterprise RAG Gateway encounters a clearance violation,
     * tenant breach, or disallowed empty context.
     */
    public static class RagGatewayBreachException extends RuntimeException {
        public RagGatewayBreachException(String message) {
            super(message);
        }

        public RagGatewayBreachException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
