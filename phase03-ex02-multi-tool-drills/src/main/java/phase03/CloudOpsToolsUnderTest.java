package phase03;

import phase03.OpsToolContracts.ConfigUpdateResult;
import phase03.OpsToolContracts.DeploymentStatus;
import phase03.OpsToolContracts.HealthReport;
import phase03.OpsToolContracts.LogEntry;
import phase03.OpsToolContracts.LogLevel;
import phase03.OpsToolContracts.LogQueryFilter;
import phase03.OpsToolContracts.ToolEnvelope;
import phase03.OpsToolContracts.ToolExecutionAudit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service under test — THE ONLY FILE YOU MODIFY.
 * <p>
 * Practice repetitive muscle memory for Spring AI @Tool development:
 * - Annotate all tool methods with @Tool(description = "...")
 * - Annotate all parameters with @ToolParam(description = "...")
 * - Increment callCount on every invocation
 * - Validate input constraints, throwing IllegalArgumentException or returning ToolEnvelope.fail(...)
 * - Record audit entries via recordAudit(...)
 * - Return typed records or ToolEnvelope wrappers
 */
public class CloudOpsToolsUnderTest {

    private final List<ToolExecutionAudit> executionAudits = new CopyOnWriteArrayList<>();
    private int callCount = 0;
    private int purgeCount = 0;

    /**
     * Scenario 1: Restart a specific pod in a Kubernetes namespace.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount
     * - Throw IllegalArgumentException if namespace is null/blank or podName is null/blank
     * - Record audit: toolName="restartPod", arguments=Map of params, success=true/false
     * - Return true on success
     */
    public boolean restartPod(String namespace, String podName) {
        return false;
    }

    /**
     * Scenario 2: Scale a deployment to the desired replica count.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount
     * - Throw IllegalArgumentException if deployment is null/blank or replicas < 1 or replicas > 50
     * - Record audit: toolName="scaleReplicas", arguments=Map of params, success=true/false
     * - Return new DeploymentStatus(deployment, replicas, "SCALED")
     */
    public DeploymentStatus scaleReplicas(String deployment, int replicas) {
        return null;
    }

    /**
     * Scenario 3: Dynamically update the logging level for a backend service.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount
     * - Throw IllegalArgumentException if service is null/blank or level is null
     * - Record audit: toolName="updateLogLevel", arguments=Map of params, success=true/false
     * - Return new ConfigUpdateResult(service, level, true, "Log level updated to " + level)
     */
    public ConfigUpdateResult updateLogLevel(String service, LogLevel level) {
        return null;
    }

    /**
     * Scenario 4: Query audit logs using a structured filter record.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount
     * - Throw IllegalArgumentException if filter is null, filter.service() is null/blank, or filter.maxEntries() < 1
     * - Record audit: toolName="queryAuditLogs", arguments=Map.of("service", filter.service()), success=true/false
     * - Return List of 2 LogEntry items matching the filter's service and minLevel
     */
    public List<LogEntry> queryAuditLogs(LogQueryFilter filter) {
        return List.of();
    }

    /**
     * Scenario 5: Execute a synthetic health probe on a system component.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount
     * - Allowed components (case-insensitive): "api-gateway", "auth-service", "database", "cache"
     * - If component is null/blank or unknown, DO NOT throw: return ToolEnvelope.fail("...")
     *   * For unknown component: error message MUST contain "Unknown component" (e.g., "Unknown component: " + component)
     * - Record audit: toolName="executeHealthProbe", arguments=Map of params, success=true/false
     * - Return ToolEnvelope.ok(new HealthReport(normalizedComponent, "HEALTHY", 12L, Map.of("uptimeSec", 86400)))
     */
    public ToolEnvelope<HealthReport> executeHealthProbe(String component) {
        return null;
    }

    /**
     * Scenario 6: Trigger a point-in-time snapshot for a PostgreSQL or MySQL cluster.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount
     * - Validate clusterId matches regex "^[a-z0-9-]+$" and retentionDays is between 1 and 90
     * - If invalid, DO NOT throw: return ToolEnvelope.fail("...")
     *   * For invalid clusterId: error message MUST contain "Invalid clusterId" (e.g., "Invalid clusterId format: " + clusterId)
     *   * For invalid retentionDays: error message MUST contain "retentionDays" (e.g., "retentionDays must be between 1 and 90, got " + retentionDays)
     * - Record audit: toolName="triggerDatabaseSnapshot", arguments=Map of params, success=true/false
     * - Return ToolEnvelope.ok("Snapshot snapshot-" + clusterId + "-20261001 created with " + retentionDays + "d retention")
     */
    public ToolEnvelope<String> triggerDatabaseSnapshot(String clusterId, int retentionDays) {
        return null;
    }

    /**
     * Scenario 7: Purge cache keys matching a pattern, with a rate-limit of at most 3 purges per session.
     * <p>
     * Instructions:
     * - Add @Tool and @ToolParam annotations
     * - Increment callCount and purgeCount
     * - If purgeCount > 3, return ToolEnvelope.fail("...")
     *   * Error message MUST contain "Rate limit exceeded" (e.g., "Rate limit exceeded: maximum 3 cache purges allowed per turn")
     * - If pattern is null/blank, return ToolEnvelope.fail("pattern must not be null or blank")
     * - Record audit: toolName="purgeCacheKeys", arguments=Map of params, success=true/false
     * - Return ToolEnvelope.ok(42)
     */
    public ToolEnvelope<Integer> purgeCacheKeys(String pattern) {
        return null;
    }

    public int getCallCount() {
        return callCount;
    }

    public List<ToolExecutionAudit> getExecutionAudits() {
        return Collections.unmodifiableList(executionAudits);
    }

    public void resetSession() {
        this.callCount = 0;
        this.purgeCount = 0;
        this.executionAudits.clear();
    }

    private void recordAudit(String toolName, Map<String, Object> arguments, boolean success, String summary) {
        executionAudits.add(new ToolExecutionAudit(callCount, toolName, arguments, success, summary, System.currentTimeMillis()));
    }
}
