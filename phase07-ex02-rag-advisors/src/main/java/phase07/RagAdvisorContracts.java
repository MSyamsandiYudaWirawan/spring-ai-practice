package phase07;

import java.util.Objects;

/**
 * Domain contracts for Phase 07 Exercise 02 (RAG Advisors).
 * <p>
 * DO NOT MODIFY THIS FILE.
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
}
