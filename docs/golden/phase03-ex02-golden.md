# Phase 03 Exercise 02 — Golden Reference Solution
## Repetitive Spring AI @Tool Calling Drills (15 Drills)

```java
package phase03;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import phase03.OpsToolContracts.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Service under test — Phase 03 Exercise 02:
 * Repetitive Spring AI @Tool Calling Drills (15 Scenarios).
 */
public class CloudOpsToolsUnderTest {

    private static final Pattern CLUSTER_ID_PATTERN = Pattern.compile("^[a-z0-9-]+$");
    private static final Set<String> ALLOWED_COMPONENTS = Set.of("api-gateway", "auth-service", "database", "cache");

    private final List<ToolExecutionAudit> executionAudits = new CopyOnWriteArrayList<>();
    private int callCount = 0;
    private int purgeCount = 0;

    private synchronized int nextCallIndex() {
        return ++callCount;
    }

    private void recordAudit(String toolName, Map<String, Object> arguments, boolean success, String summary) {
        executionAudits.add(new ToolExecutionAudit(callCount, toolName, arguments, success, summary, System.currentTimeMillis()));
    }

    /**
     * Drill 1: Restart a specific pod in a Kubernetes namespace.
     */
    @Tool(description = "Restart a specific pod in a Kubernetes namespace.")
    public boolean restartPod(
            @ToolParam(description = "Target Kubernetes namespace") String namespace,
            @ToolParam(description = "Name of the pod to restart") String podName
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("namespace", namespace != null ? namespace : "null", "podName", podName != null ? podName : "null");
        if (namespace == null || namespace.isBlank()) {
            recordAudit("restartPod", args, false, "namespace must not be null or blank");
            throw new IllegalArgumentException("namespace must not be null or blank");
        }
        if (podName == null || podName.isBlank()) {
            recordAudit("restartPod", args, false, "podName must not be null or blank");
            throw new IllegalArgumentException("podName must not be null or blank");
        }
        recordAudit("restartPod", args, true, "Pod restarted");
        return true;
    }

    /**
     * Drill 2: Scale a deployment to the desired replica count.
     */
    @Tool(description = "Scale a deployment to the desired replica count.")
    public DeploymentStatus scaleReplicas(
            @ToolParam(description = "Deployment name") String deployment,
            @ToolParam(description = "Desired replica count (1 to 50)") int replicas
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("deployment", deployment != null ? deployment : "null", "replicas", replicas);
        if (deployment == null || deployment.isBlank()) {
            recordAudit("scaleReplicas", args, false, "deployment must not be null or blank");
            throw new IllegalArgumentException("deployment must not be null or blank");
        }
        if (replicas < 1 || replicas > 50) {
            recordAudit("scaleReplicas", args, false, "replicas must be between 1 and 50");
            throw new IllegalArgumentException("replicas must be between 1 and 50, got: " + replicas);
        }
        recordAudit("scaleReplicas", args, true, "Scaled to " + replicas);
        return new DeploymentStatus(deployment, replicas, "SCALED");
    }

    /**
     * Drill 3: Safely drain worker node workloads.
     */
    @Tool(description = "Safely drain worker node workloads before maintenance.")
    public NodeDrainResult drainNode(
            @ToolParam(description = "Target worker node hostname") String nodeName,
            @ToolParam(description = "Grace period in seconds (0 to 3600)") int gracePeriodSeconds
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("nodeName", nodeName != null ? nodeName : "null", "gracePeriodSeconds", gracePeriodSeconds);
        if (nodeName == null || nodeName.isBlank()) {
            recordAudit("drainNode", args, false, "nodeName must not be null or blank");
            throw new IllegalArgumentException("nodeName must not be null or blank");
        }
        if (gracePeriodSeconds < 0 || gracePeriodSeconds > 3600) {
            recordAudit("drainNode", args, false, "gracePeriodSeconds must be between 0 and 3600");
            throw new IllegalArgumentException("gracePeriodSeconds must be between 0 and 3600, got: " + gracePeriodSeconds);
        }
        recordAudit("drainNode", args, true, "Node drained");
        return new NodeDrainResult(nodeName, gracePeriodSeconds, "DRAINED");
    }

    /**
     * Drill 4: Dynamically update the logging level for a backend service.
     */
    @Tool(description = "Dynamically update the logging level for a backend service.")
    public ConfigUpdateResult updateLogLevel(
            @ToolParam(description = "Target service name") String service,
            @ToolParam(description = "Desired LogLevel (DEBUG, INFO, WARN, ERROR)") LogLevel level
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("service", service != null ? service : "null", "level", level != null ? level.name() : "null");
        if (service == null || service.isBlank()) {
            recordAudit("updateLogLevel", args, false, "service must not be null or blank");
            throw new IllegalArgumentException("service must not be null or blank");
        }
        if (level == null) {
            recordAudit("updateLogLevel", args, false, "level must not be null");
            throw new IllegalArgumentException("level must not be null");
        }
        recordAudit("updateLogLevel", args, true, "Log level updated to " + level);
        return new ConfigUpdateResult(service, level, true, "Log level updated to " + level);
    }

    /**
     * Drill 5: Query audit logs using a structured filter record.
     */
    @Tool(description = "Query audit logs using a structured filter record.")
    public List<LogEntry> queryAuditLogs(
            @ToolParam(description = "Log query filter parameters") LogQueryFilter filter
    ) {
        nextCallIndex();
        if (filter == null) {
            recordAudit("queryAuditLogs", Map.of(), false, "filter must not be null");
            throw new IllegalArgumentException("filter must not be null");
        }
        Map<String, Object> args = Map.of("service", filter.service() != null ? filter.service() : "null");
        if (filter.service() == null || filter.service().isBlank()) {
            recordAudit("queryAuditLogs", args, false, "filter service must not be null or blank");
            throw new IllegalArgumentException("filter service must not be null or blank");
        }
        if (filter.maxEntries() < 1) {
            recordAudit("queryAuditLogs", args, false, "maxEntries must be >= 1");
            throw new IllegalArgumentException("maxEntries must be >= 1");
        }
        recordAudit("queryAuditLogs", args, true, "Retrieved log entries");
        return List.of(
                new LogEntry(filter.service(), filter.minLevel(), "Audit event 1 for " + filter.service(), System.currentTimeMillis()),
                new LogEntry(filter.service(), filter.minLevel(), "Audit event 2 for " + filter.service(), System.currentTimeMillis())
        );
    }

    /**
     * Drill 6: Batch restart multiple pods in a namespace.
     */
    @Tool(description = "Batch restart multiple pods in a namespace.")
    public Map<String, Boolean> batchRestartPods(
            @ToolParam(description = "Target namespace") String namespace,
            @ToolParam(description = "List of pod names to restart") List<String> podNames
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("namespace", namespace != null ? namespace : "null");
        if (namespace == null || namespace.isBlank()) {
            recordAudit("batchRestartPods", args, false, "namespace must not be null or blank");
            throw new IllegalArgumentException("namespace must not be null or blank");
        }
        if (podNames == null || podNames.isEmpty()) {
            recordAudit("batchRestartPods", args, false, "podNames must not be null or empty");
            throw new IllegalArgumentException("podNames must not be null or empty");
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String p : podNames) {
            result.put(p, true);
        }
        recordAudit("batchRestartPods", args, true, "Batch restarted " + podNames.size() + " pods");
        return Collections.unmodifiableMap(result);
    }

    /**
     * Drill 7: Execute a synthetic health probe on a system component.
     */
    @Tool(description = "Execute a synthetic health probe on a system component.")
    public ToolEnvelope<HealthReport> executeHealthProbe(
            @ToolParam(description = "Component name (api-gateway, auth-service, database, cache)") String component
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("component", component != null ? component : "null");
        if (component == null || component.isBlank()) {
            String err = "Unknown component: null or blank";
            recordAudit("executeHealthProbe", args, false, err);
            return ToolEnvelope.fail(err);
        }
        String normalized = component.trim().toLowerCase();
        if (!ALLOWED_COMPONENTS.contains(normalized)) {
            String err = "Unknown component: " + component;
            recordAudit("executeHealthProbe", args, false, err);
            return ToolEnvelope.fail(err);
        }
        recordAudit("executeHealthProbe", args, true, "Health probe passed");
        return ToolEnvelope.ok(new HealthReport(normalized, "HEALTHY", 12L, Map.of("uptimeSec", 86400)));
    }

    /**
     * Drill 8: Trigger a point-in-time snapshot for a database cluster.
     */
    @Tool(description = "Trigger a point-in-time snapshot for a database cluster.")
    public ToolEnvelope<String> triggerDatabaseSnapshot(
            @ToolParam(description = "Database cluster ID (lowercase alphanumeric and hyphens)") String clusterId,
            @ToolParam(description = "Retention period in days (1 to 90)") int retentionDays
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("clusterId", clusterId != null ? clusterId : "null", "retentionDays", retentionDays);
        if (clusterId == null || !CLUSTER_ID_PATTERN.matcher(clusterId).matches()) {
            String err = "Invalid clusterId format: " + clusterId;
            recordAudit("triggerDatabaseSnapshot", args, false, err);
            return ToolEnvelope.fail(err);
        }
        if (retentionDays < 1 || retentionDays > 90) {
            String err = "retentionDays must be between 1 and 90, got: " + retentionDays;
            recordAudit("triggerDatabaseSnapshot", args, false, err);
            return ToolEnvelope.fail(err);
        }
        String success = "Snapshot snapshot-" + clusterId + "-20261001 created with " + retentionDays + "d retention";
        recordAudit("triggerDatabaseSnapshot", args, true, success);
        return ToolEnvelope.ok(success);
    }

    /**
     * Drill 9: Rollback a deployment to a target revision.
     */
    @Tool(description = "Rollback a deployment to a target revision.")
    public ToolEnvelope<String> rollbackDeployment(
            @ToolParam(description = "Deployment name to rollback") String deployment,
            @ToolParam(description = "Target revision number (>= 1)") int targetRevision
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("deployment", deployment != null ? deployment : "null", "targetRevision", targetRevision);
        if (deployment == null || deployment.isBlank()) {
            String err = "deployment must not be null or blank";
            recordAudit("rollbackDeployment", args, false, err);
            return ToolEnvelope.fail(err);
        }
        if (targetRevision < 1) {
            String err = "Invalid revision: " + targetRevision + " must be >= 1";
            recordAudit("rollbackDeployment", args, false, err);
            return ToolEnvelope.fail(err);
        }
        String success = "Rollback of " + deployment + " to revision " + targetRevision + " completed";
        recordAudit("rollbackDeployment", args, true, success);
        return ToolEnvelope.ok(success);
    }

    /**
     * Drill 10: Purge cache keys matching a pattern, with a rate-limit of at most 3 purges per session.
     */
    @Tool(description = "Purge cache keys matching a pattern (rate limited to 3 per session).")
    public synchronized ToolEnvelope<Integer> purgeCacheKeys(
            @ToolParam(description = "Cache key pattern to purge") String pattern
    ) {
        nextCallIndex();
        Map<String, Object> args = Map.of("pattern", pattern != null ? pattern : "null");
        if (pattern == null || pattern.isBlank()) {
            String err = "pattern must not be null or blank";
            recordAudit("purgeCacheKeys", args, false, err);
            return ToolEnvelope.fail(err);
        }
        purgeCount++;
        if (purgeCount > 3) {
            String err = "Rate limit exceeded: maximum 3 cache purges allowed per turn";
            recordAudit("purgeCacheKeys", args, false, err);
            return ToolEnvelope.fail(err);
        }
        recordAudit("purgeCacheKeys", args, true, "Purged 42 keys");
        return ToolEnvelope.ok(42);
    }

    public int getCallCount() {
        return callCount;
    }

    public int getPurgeCount() {
        return purgeCount;
    }

    public List<ToolExecutionAudit> getExecutionAudits() {
        return Collections.unmodifiableList(executionAudits);
    }

    public synchronized void resetSession() {
        this.callCount = 0;
        this.purgeCount = 0;
        this.executionAudits.clear();
    }
}
```
