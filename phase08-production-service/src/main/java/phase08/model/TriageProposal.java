package phase08.model;

/**
 * Structured output extracted from LLM decision turn.
 */
public record TriageProposal(
        String action,
        String hypothesis,
        String targetFile,
        String patchContent,
        double confidence,
        String rationale
) {}
