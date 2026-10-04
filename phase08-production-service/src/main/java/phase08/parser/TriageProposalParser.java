package phase08.parser;

import org.springframework.ai.converter.BeanOutputConverter;
import phase08.model.TriageProposal;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Phase 02: Structured Output Schema Extraction & 1-Shot Repair.
 * <p>
 * Handles markdown code block stripping, schema validation against TriageProposal,
 * and closed-loop one-shot repair upon malformed JSON generation.
 */
public class TriageProposalParser {

    private static final Pattern FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
    private final BeanOutputConverter<TriageProposal> converter = new BeanOutputConverter<>(TriageProposal.class);

    /**
     * Sanitizes markdown code fences from raw LLM output.
     * <p>
     * Instructions:
     * - If raw is null, return "".
     * - Match raw against FENCE_PATTERN:
     *   - If a markdown fence (```json ... ``` or ``` ... ```) is found, extract group 1 and trim whitespace.
     *   - If no fence is found, return raw.trim().
     */
    public String stripMarkdownFences(String raw) {
        throw new UnsupportedOperationException("TODO: Implement stripMarkdownFences");
    }

    /**
     * Parses clean JSON or markdown-fenced text into a validated TriageProposal record.
     * <p>
     * Instructions:
     * - Clean raw output using stripMarkdownFences(raw).
     * - Convert cleaned string using converter.convert(cleanJson).
     * - Fail-Fast Invariant Validations:
     *   - If proposal is null, throw IllegalArgumentException("Converted proposal is null").
     *   - If proposal.action() is null or blank, throw IllegalArgumentException("Proposal action cannot be blank").
     *   - If proposal.hypothesis() is null or blank, throw IllegalArgumentException("Proposal hypothesis cannot be blank").
     *   - If proposal.confidence() is < 0.0 or > 1.0, throw IllegalArgumentException("Proposal confidence must be between 0.0 and 1.0").
     * - Return the validated TriageProposal.
     */
    public TriageProposal parse(String raw) {
        throw new UnsupportedOperationException("TODO: Implement parse");
    }

    /**
     * Attempts direct parse; on failure, performs an automated 1-shot repair retry.
     * <p>
     * Instructions:
     * - Try calling parse(raw).
     *   - If successful, return Optional.of(proposal).
     * - Catch initial exception:
     *   - If repairFn is null, return Optional.empty().
     *   - Construct repair prompt quoting the failure:
     *     "Schema parse failed: " + initialError.getMessage() + ". Fix and return ONLY valid JSON: " + raw
     *   - Execute repairFn.apply(repairPrompt) to obtain the repaired response.
     *   - Try calling parse(repairedResponse).
     *     - If successful, return Optional.of(repairedProposal).
     *     - If repair parsing fails or throws, catch and return Optional.empty().
     */
    public Optional<TriageProposal> parseWithOneShotRepair(String raw, Function<String, String> repairFn) {
        throw new UnsupportedOperationException("TODO: Implement parseWithOneShotRepair");
    }
}
