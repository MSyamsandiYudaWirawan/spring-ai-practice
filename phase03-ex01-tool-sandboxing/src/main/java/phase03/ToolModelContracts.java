package phase03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable contracts and data records for Phase 03 Exercise 01:
 * Autonomous Diagnostic Tool Sandboxing & Execution Bounding.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public final class ToolModelContracts {

    private ToolModelContracts() {}

    public static final long MAX_FILE_SIZE_BYTES = 100 * 1024L; // 100 KB cap (§10.34)
    public static final int DEFAULT_TOOL_BOUND = 10;             // Bound cap (§5.5, D1)

    /**
     * Standard diagnostic tool envelope isolating all exceptions from the LLM turn.
     * Guaranteed to serialize cleanly back to the model as JSON.
     */
    public record ToolEnvelope<T>(
            boolean ok,
            T data,
            String error
    ) {
        public static <T> ToolEnvelope<T> ok(T data) {
            return new ToolEnvelope<>(true, data, null);
        }

        public static <T> ToolEnvelope<T> fail(String error) {
            Objects.requireNonNull(error, "error message must not be null");
            return new ToolEnvelope<>(false, null, error);
        }
    }

    /**
     * Trajectory audit record capturing intermediate tool execution events.
     */
    public record ToolCallEvent(
            int sequence,
            String action,
            Map<String, Object> parameters,
            boolean ok,
            String detail,
            long timestamp
    ) {
        public ToolCallEvent {
            Objects.requireNonNull(action, "action must not be null");
            if (parameters == null) parameters = Map.of();
        }
    }

    /**
     * Helper to set up the reproducible test sandbox directory on disk.
     */
    public static void setupSandboxFixture(Path sandboxRoot) throws IOException {
        Objects.requireNonNull(sandboxRoot, "sandboxRoot must not be null");

        // 1. Valid source and configuration files
        Path srcProps = sandboxRoot.resolve("src/main/resources/application.properties");
        Files.createDirectories(srcProps.getParent());
        Files.writeString(srcProps, """
                server.port=8080
                spring.datasource.hikari.maximum-pool-size=10
                spring.datasource.hikari.connection-timeout=30000
                """);

        Path srcJava = sandboxRoot.resolve("src/main/java/io/diag/target/TargetApplication.java");
        Files.createDirectories(srcJava.getParent());
        Files.writeString(srcJava, """
                package io.diag.target;

                public class TargetApplication {
                    public static void main(String[] args) {
                        System.out.println("Target started");
                    }
                }
                """);

        Path pomXml = sandboxRoot.resolve("pom.xml");
        Files.writeString(pomXml, """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>io.diag.target</groupId>
                    <artifactId>target-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """);

        // 2. Oversized binary / heap file (110 KB > 100 KB cap)
        Path largeLog = sandboxRoot.resolve("logs/large_heap_dump.bin");
        Files.createDirectories(largeLog.getParent());
        byte[] largeBytes = new byte[110 * 1024];
        Arrays.fill(largeBytes, (byte) 'A');
        Files.write(largeLog, largeBytes);

        // 3. Sensitive / prohibited files
        Path envFile = sandboxRoot.resolve(".env");
        Files.writeString(envFile, "DATABASE_PASSWORD=supersecret_token_never_expose\n");

        Path gitConfig = sandboxRoot.resolve(".git/config");
        Files.createDirectories(gitConfig.getParent());
        Files.writeString(gitConfig, "[core]\n\trepositoryformatversion = 0\n");
    }
}
