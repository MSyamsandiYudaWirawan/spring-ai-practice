package phase01;

import java.util.Objects;

/**
 * Immutable contracts and constants for Phase 01 Exercise 01.
 * DO NOT MODIFY THIS FILE.
 */
public final class ModelContracts {

    private ModelContracts() {}

    /**
     * Diagnosis context input.
     * Mirrors the Diagnostician's DecideContext inputs (targetApp, baseline p95, top hotspot).
     */
    public record DiagnosisContext(
            String targetApp,
            int baselineP95Ms,
            String jfrTopFrame
    ) {
        public DiagnosisContext {
            Objects.requireNonNull(targetApp, "targetApp must not be null");
            if (targetApp.isBlank()) throw new IllegalArgumentException("targetApp cannot be blank");
            if (baselineP95Ms < 0) throw new IllegalArgumentException("baselineP95Ms must be >= 0");
            Objects.requireNonNull(jfrTopFrame, "jfrTopFrame must not be null");
            if (jfrTopFrame.isBlank()) throw new IllegalArgumentException("jfrTopFrame cannot be blank");
        }
    }

    /**
     * Standard template for diagnosis hypothesis prompt.
     * Contains 3 parameter placeholders: {targetApp}, {baselineP95}, {topFrame}.
     */
    public static final String DIAGNOSIS_PROMPT_TEMPLATE =
            "Diagnosing target application: {targetApp} with baseline p95: {baselineP95}ms. Top hotspot frame: {topFrame}. Propose root cause hypothesis.";

    /**
     * Expected system prompt configured as default on the ChatClient.
     */
    public static final String EXPECTED_DEFAULT_SYSTEM_PROMPT =
            "You are a TigerStyle performance diagnostician. Be concise and precise.";

    /**
     * Expected default temperature and topP options.
     */
    public static final double EXPECTED_DEFAULT_TEMPERATURE = 0.2;
    public static final double EXPECTED_DEFAULT_TOP_P = 0.9;
}
