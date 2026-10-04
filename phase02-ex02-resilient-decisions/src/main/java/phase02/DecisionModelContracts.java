package phase02;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable contracts and data records for Phase 02 Exercise 02:
 * Performance Diagnostic Decision Gateway & Resilient Schema Repair.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class DecisionModelContracts {

    private DecisionModelContracts() {}

    public static final Pattern HYPOTHESIS_PATTERN = Pattern.compile("^H[1-7]$");

    /**
     * Diagnostic hypothesis formulated by the agent.
     */
    public record Hypothesis(
            String category,
            double confidence,
            String rationale
    ) {
        public Hypothesis {
            if (category != null) category = category.trim().toUpperCase();
            if (rationale != null) rationale = rationale.trim();
        }
    }

    /**
     * Falsifiable prediction verified after benchmark execution.
     */
    public record Prediction(
            String metric,
            String direction,
            String signalToEliminate
    ) {
        public Prediction {
            if (metric != null) metric = metric.trim().toLowerCase();
            if (direction != null) direction = direction.trim().toLowerCase();
            if (signalToEliminate != null) signalToEliminate = signalToEliminate.trim();
        }
    }

    /**
     * Hypothesis ledger update tracking evidence convergence.
     */
    public record LedgerUpdate(
            String hypothesisCategory,
            String direction,
            String reason
    ) {
        public LedgerUpdate {
            if (hypothesisCategory != null) hypothesisCategory = hypothesisCategory.trim().toUpperCase();
            if (direction != null) direction = direction.trim().toLowerCase();
            if (reason != null) reason = reason.trim();
        }
    }

    /**
     * Targeted source code edit specification.
     */
    public record FileEdit(
            String path,
            String targetContent,
            String replacementContent
    ) {
        public FileEdit {
            if (path != null) path = path.trim();
        }
    }

    /**
     * Polymorphic change payload: either source code edits or a canned optimization template.
     */
    public record Change(
            String kind,
            List<FileEdit> edits,
            String template,
            Map<String, String> parameters
    ) {
        public Change {
            if (kind != null) kind = kind.trim().toLowerCase();
            if (template != null) template = template.trim().toLowerCase();
            if (parameters == null) parameters = Map.of();
        }
    }

    /**
     * Complete optimization decision payload emitted by the DECIDE turn.
     */
    public record OptimizationDecision(
            Hypothesis hypothesis,
            Prediction prediction,
            LedgerUpdate ledger,
            Change change
    ) {}

    /**
     * Diagnostic execution envelope tracking triage acceptance, data payload, and attempt count.
     */
    public record DecisionEnvelope<T>(
            boolean accepted,
            T data,
            String rejectionReason,
            int attemptsUsed
    ) {
        public static <T> DecisionEnvelope<T> accepted(T data, int attemptsUsed) {
            Objects.requireNonNull(data, "accepted data must not be null");
            return new DecisionEnvelope<>(true, data, null, attemptsUsed);
        }

        public static <T> DecisionEnvelope<T> rejected(String rejectionReason, int attemptsUsed) {
            Objects.requireNonNull(rejectionReason, "rejectionReason must not be null");
            return new DecisionEnvelope<>(false, null, rejectionReason, attemptsUsed);
        }
    }

    // =========================================================================
    // Test Double Fixtures & Sample Payloads
    // =========================================================================

    public static final String SAMPLE_VALID_TEMPLATE_DECISION_JSON = """
            {
              "hypothesis": {
                "category": "H3",
                "confidence": 0.95,
                "rationale": "High lock contention in nested fat-jar classloading URL handlers."
              },
              "prediction": {
                "metric": "p95",
                "direction": "improve",
                "signalToEliminate": "JavaMonitorEnter"
              },
              "ledger": {
                "hypothesisCategory": "H3",
                "direction": "strengthen",
                "reason": "Lock profiles correlate 71,000 JavaMonitorEnter events with fat-jar classloader"
              },
              "change": {
                "kind": "template",
                "edits": null,
                "template": "jar-unpack",
                "parameters": {
                  "mode": "exploded"
                }
              }
            }
            """;

    public static final String SAMPLE_VALID_EDITS_DECISION_JSON = """
            {
              "hypothesis": {
                "category": "H1",
                "confidence": 0.85,
                "rationale": "Connection pool starvation under 200 VU load."
              },
              "prediction": {
                "metric": "rps",
                "direction": "improve",
                "signalToEliminate": "ThreadPark"
              },
              "ledger": {
                "hypothesisCategory": "H1",
                "direction": "strengthen",
                "reason": "Hikari pool size cap at 10 caused 450ms wait times on connection acquisition"
              },
              "change": {
                "kind": "edits",
                "edits": [
                  {
                    "path": "src/main/resources/application.properties",
                    "targetContent": "spring.datasource.hikari.maximum-pool-size=10",
                    "replacementContent": "spring.datasource.hikari.maximum-pool-size=50"
                  }
                ],
                "template": null,
                "parameters": {}
              }
            }
            """;

    public static final String SAMPLE_MARKDOWN_WRAPPED_DECISION_JSON = """
            Here is the diagnostic optimization decision based on JFR call-stack analysis:
            ```json
            {
              "hypothesis": {
                "category": "H2",
                "confidence": 0.90,
                "rationale": "Excessive GC allocation pauses detected in ObjectAllocationSample."
              },
              "prediction": {
                "metric": "median",
                "direction": "improve",
                "signalToEliminate": "GCPhasePause"
              },
              "ledger": {
                "hypothesisCategory": "H2",
                "direction": "strengthen",
                "reason": "Eden space exhausted every 1.2s under k6 sustained load"
              },
              "change": {
                "kind": "template",
                "edits": null,
                "template": "virtual-threads",
                "parameters": {}
              }
            }
            ```
            Please apply this template to staging baseline.
            """;

    public static final String SAMPLE_DECISION_BATCH_JSON = """
            [
              {
                "hypothesis": {
                  "category": "H3",
                  "confidence": 0.95,
                  "rationale": "High lock contention in nested fat-jar."
                },
                "prediction": {
                  "metric": "p95",
                  "direction": "improve",
                  "signalToEliminate": "JavaMonitorEnter"
                },
                "ledger": {
                  "hypothesisCategory": "H3",
                  "direction": "strengthen",
                  "reason": "Lock profiles correlate with fat-jar"
                },
                "change": {
                  "kind": "template",
                  "edits": null,
                  "template": "jar-unpack",
                  "parameters": {}
                }
              },
              {
                "hypothesis": {
                  "category": "H4",
                  "confidence": 0.75,
                  "rationale": "Socket buffer bottleneck."
                },
                "prediction": {
                  "metric": "rps",
                  "direction": "improve",
                  "signalToEliminate": "SocketRead"
                },
                "ledger": {
                  "hypothesisCategory": "H4",
                  "direction": "strengthen",
                  "reason": "Socket read delays on remote DB query"
                },
                "change": {
                  "kind": "template",
                  "edits": null,
                  "template": "hikari-pool",
                  "parameters": {}
                }
              }
            ]
            """;

    public static final String SAMPLE_INVALID_CROSS_FIELD_JSON = """
            {
              "hypothesis": {
                "category": "H3",
                "confidence": 0.95,
                "rationale": "High lock contention."
              },
              "prediction": {
                "metric": "p95",
                "direction": "improve",
                "signalToEliminate": "JavaMonitorEnter"
              },
              "ledger": {
                "hypothesisCategory": "H3",
                "direction": "strengthen",
                "reason": "Lock profiles"
              },
              "change": {
                "kind": "template",
                "edits": null,
                "template": "",
                "parameters": {}
              }
            }
            """;

    public static final String SAMPLE_CORRUPTED_SYNTAX_JSON = """
            {
              "hypothesis": {
                "category": "H3",
                "confidence": 0.95,
                "rationale": "Lock contention in fat-jar
            """;

    public static final String SAMPLE_UNSTRUCTURED_PROSE_DECISION = """
            Based on the JFR event analysis, I formulate hypothesis H3 with confidence 0.88.
            The primary bottleneck is JavaMonitorEnter lock contention in URLClassPath.
            To eliminate JavaMonitorEnter and improve p95 latency, we should apply template jar-unpack.
            We should strengthen H3 because the thread dump confirms locking on Archive.
            """;
}
