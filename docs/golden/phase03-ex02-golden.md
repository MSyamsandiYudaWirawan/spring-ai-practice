# Phase 03 Exercise 02 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `restartPod` (Primitive Parameters & Validation)

### TigerStyle Fix
```java
@Tool(description = "Restart a specific pod in a Kubernetes namespace.")
public boolean restartPod(
        @ToolParam(description = "Target Kubernetes namespace") String namespace,
        @ToolParam(description = "Target pod name to restart") String podName
) {
    callCount++;
    if (namespace == null || namespace.isBlank()) {
        recordAudit("restartPod", Map.of("namespace", namespace == null ? "" : namespace), false, "namespace must not be blank");
        throw new IllegalArgumentException("namespace must not be blank");
    }
    if (podName == null || podName.isBlank()) {
        recordAudit("restartPod", Map.of("namespace", namespace, "podName", podName == null ? "" : podName), false, "podName must not be blank");
        throw new IllegalArgumentException("podName must not be blank");
    }

    recordAudit("restartPod", Map.of("namespace", namespace, "podName", podName), true, "Restarted pod " + podName);
    return true;
}
```

---

## 2. Scenario 2: `scaleReplicas` (Numeric Range & Record Return)

### TigerStyle Fix
```java
@Tool(description = "Scale a deployment to the desired replica count.")
public DeploymentStatus scaleReplicas(
        @ToolParam(description = "Deployment name") String deployment,
        @ToolParam(description = "Target replica count between 1 and 50") int replicas
) {
    callCount++;
    if (deployment == null || deployment.isBlank()) {
        recordAudit("scaleReplicas", Map.of(), false, "deployment must not be blank");
        throw new IllegalArgumentException("deployment must not be blank");
    }
    if (replicas < 1 || replicas > 50) {
        recordAudit("scaleReplicas", Map.of("deployment", deployment, "replicas", replicas), false, "replicas must be between 1 and 50");
        throw new IllegalArgumentException("replicas must be between 1 and 50 inclusive, got " + replicas);
    }

    DeploymentStatus status = new DeploymentStatus(deployment, replicas, "SCALED");
    recordAudit("scaleReplicas", Map.of("deployment", deployment, "replicas", replicas), true, "Scaled to " + replicas);
    return status;
}
```

---

## 3. Scenario 3: `updateLogLevel` (Enum Parameter Dispatch)

### TigerStyle Fix
```java
@Tool(description = "Dynamically update the logging level for a backend service.")
public ConfigUpdateResult updateLogLevel(
        @ToolParam(description = "Target service name") String service,
        @ToolParam(description = "New logging level (DEBUG, INFO, WARN, ERROR)") LogLevel level
) {
    callCount++;
    if (service == null || service.isBlank()) {
        recordAudit("updateLogLevel", Map.of(), false, "service must not be blank");
        throw new IllegalArgumentException("service must not be blank");
    }
    if (level == null) {
        recordAudit("updateLogLevel", Map.of("service", service), false, "level must not be null");
        throw new IllegalArgumentException("level must not be null");
    }

    ConfigUpdateResult result = new ConfigUpdateResult(service, level, true, "Log level updated to " + level);
    recordAudit("updateLogLevel", Map.of("service", service, "level", level.name()), true, "Log level updated to " + level);
    return result;
}
```

---

## 4. Scenario 4: `queryAuditLogs` (Structured Record Input Schema & List Return)

### TigerStyle Fix
```java
@Tool(description = "Query audit logs using a structured filter record.")
public List<LogEntry> queryAuditLogs(
        @ToolParam(description = "Search filter specification") LogQueryFilter filter
    ) {
    callCount++;
    if (filter == null || filter.service() == null || filter.service().isBlank()) {
        recordAudit("queryAuditLogs", Map.of(), false, "filter and service must not be blank");
        throw new IllegalArgumentException("filter and service must not be blank");
    }
    if (filter.maxEntries() < 1) {
        recordAudit("queryAuditLogs", Map.of(), false, "maxEntries must be >= 1");
        throw new IllegalArgumentException("maxEntries must be >= 1");
    }

    List<LogEntry> entries = List.of(
            new LogEntry(filter.service(), filter.minLevel(), "Request handled with latency 42ms", System.currentTimeMillis() - 5000),
            new LogEntry(filter.service(), filter.minLevel(), "Upstream pool acquired", System.currentTimeMillis())
    );
    recordAudit("queryAuditLogs", Map.of("service", filter.service()), true, "Retrieved " + entries.size() + " logs");
    return entries;
}
```

---

## 5. Scenario 5: `executeHealthProbe` (Envelope Wrapping & Exception Isolation)

### TigerStyle Fix
```java
@Tool(description = "Execute a synthetic health probe on a system component.")
public ToolEnvelope<HealthReport> executeHealthProbe(
        @ToolParam(description = "Component name: api-gateway, auth-service, database, cache") String component
) {
    callCount++;
    if (component == null || component.isBlank()) {
        recordAudit("executeHealthProbe", Map.of(), false, "component must not be blank");
        return ToolEnvelope.fail("component must not be null or blank");
    }

    String normalized = component.trim().toLowerCase();
    if (!List.of("api-gateway", "auth-service", "database", "cache").contains(normalized)) {
        String err = "Unknown component: " + component;
        recordAudit("executeHealthProbe", Map.of("component", component), false, err);
        return ToolEnvelope.fail(err);
    }

    HealthReport report = new HealthReport(normalized, "HEALTHY", 12L, Map.of("uptimeSec", 86400));
    recordAudit("executeHealthProbe", Map.of("component", normalized), true, "Component is HEALTHY");
    return ToolEnvelope.ok(report);
}
```

---

## 6. Scenario 6: `triggerDatabaseSnapshot` (Regex Validation & Envelope Return)

### TigerStyle Fix
```java
@Tool(description = "Trigger a point-in-time snapshot for a PostgreSQL or MySQL cluster.")
public ToolEnvelope<String> triggerDatabaseSnapshot(
        @ToolParam(description = "Database cluster identifier (lowercase alphanumeric with dashes)") String clusterId,
        @ToolParam(description = "Retention period in days between 1 and 90") int retentionDays
) {
    callCount++;
    if (clusterId == null || !clusterId.matches("^[a-z0-9-]+$")) {
        String err = "Invalid clusterId format: " + clusterId + " (expected lowercase alphanumeric with dashes)";
        recordAudit("triggerDatabaseSnapshot", Map.of(), false, err);
        return ToolEnvelope.fail(err);
    }
    if (retentionDays < 1 || retentionDays > 90) {
        String err = "retentionDays must be between 1 and 90, got " + retentionDays;
        recordAudit("triggerDatabaseSnapshot", Map.of("clusterId", clusterId), false, err);
        return ToolEnvelope.fail(err);
    }

    String msg = "Snapshot snapshot-" + clusterId + "-20261001 created with " + retentionDays + "d retention";
    recordAudit("triggerDatabaseSnapshot", Map.of("clusterId", clusterId, "retentionDays", retentionDays), true, msg);
    return ToolEnvelope.ok(msg);
}
```

---

## 7. Scenario 7: `purgeCacheKeys` (Stateful Quota & Rate-Limiting Guard)

### TigerStyle Fix
```java
@Tool(description = "Purge cache keys matching a pattern, with a rate-limit of at most 3 purges per session.")
public ToolEnvelope<Integer> purgeCacheKeys(
        @ToolParam(description = "Cache key wildcard pattern, e.g. user:* or session:*") String pattern
) {
    callCount++;
    purgeCount++;

    if (purgeCount > 3) {
        String err = "Rate limit exceeded: maximum 3 cache purges allowed per turn";
        recordAudit("purgeCacheKeys", Map.of("pattern", pattern == null ? "" : pattern), false, err);
        return ToolEnvelope.fail(err);
    }

    if (pattern == null || pattern.isBlank()) {
        recordAudit("purgeCacheKeys", Map.of(), false, "pattern must not be blank");
        return ToolEnvelope.fail("pattern must not be null or blank");
    }

    recordAudit("purgeCacheKeys", Map.of("pattern", pattern), true, "Purged 42 keys");
    return ToolEnvelope.ok(42);
}
```
