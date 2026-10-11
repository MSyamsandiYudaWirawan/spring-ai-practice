import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cross-platform Java 21 Progress Tracker for Spring AI TigerStyle Mastery Curriculum.
 * Can be run directly via single-file source launcher:
 *   java ProgressTracker.java
 */
public class ProgressTracker {

    public record ExerciseDef(String id, int phase, String name, int drills) {}
    public record ExerciseResult(int passed, int total, String status, String timestamp) {}

    private static final List<ExerciseDef> EXERCISES = List.of(
            new ExerciseDef("phase01-ex01-chat-basics", 1, "Phase 01 Ex01: ChatClient Fluent API & Foundations", 15),
            new ExerciseDef("phase01-ex02-chat-advanced", 1, "Phase 01 Ex02: Advanced ChatClient & FinTech Ops", 15),
            new ExerciseDef("phase01-ex03-cloud-sre", 1, "Phase 01 Ex03: Cloud SRE & K8s Operations", 15),
            new ExerciseDef("phase02-ex01-structured-outputs", 2, "Phase 02 Ex01: Structured Outputs & CVE Triage", 15),
            new ExerciseDef("phase02-ex02-resilient-decisions", 2, "Phase 02 Ex02: Resilient Schemas & Decision Repair", 15),
            new ExerciseDef("phase03-ex01-tool-sandboxing", 3, "Phase 03 Ex01: Tool Callbacks & Sandboxing", 15),
            new ExerciseDef("phase03-ex02-multi-tool-drills", 3, "Phase 03 Ex02: Multi-Tool Chains & K8s Ops", 15),
            new ExerciseDef("phase04-ex01-guardrail-advisors", 4, "Phase 04 Ex01: Guardrail Advisors & Prompt Defense", 15),
            new ExerciseDef("phase04-ex02-resilient-guardrails", 4, "Phase 04 Ex02: Resilient Advisors & Failover", 15),
            new ExerciseDef("phase05-ex01-offline-harness", 5, "Phase 05 Ex01: Deterministic Offline Test Harness", 15),
            new ExerciseDef("phase05-ex02-advanced-harness", 5, "Phase 05 Ex02: Advanced Harness & Fault Injection", 15),
            new ExerciseDef("phase06-ex01-evaluators", 6, "Phase 06 Ex01: Deterministic Evaluators & Benchmarks", 15),
            new ExerciseDef("phase06-ex02-advanced-judges", 6, "Phase 06 Ex02: LLM-as-a-Judge & Alignment Evaluation", 15),
            new ExerciseDef("phase07-ex01-vector-stores", 7, "Phase 07 Ex01: Vector Stores & Similarity Search", 15),
            new ExerciseDef("phase07-ex02-rag-advisors", 7, "Phase 07 Ex02: RAG Advisors & Document Augmentation", 15),
            new ExerciseDef("phase08-ex01-mcp-server", 8, "Phase 08 Ex01: Model Context Protocol Server", 15),
            new ExerciseDef("phase08-ex02-mcp-client", 8, "Phase 08 Ex02: MCP Client Integration & Tool Routing", 15)
    );

    private static final Pattern TOTAL_PATTERN = Pattern.compile("TOTAL:\\s*(\\d+)\\s*/\\s*(\\d+)\\s*PASSED");

    public static void main(String[] args) {
        System.out.println();
        System.out.println("==========================================================================================");
        System.out.println("           SPRING AI TIGERSTYLE MASTERY - TERMINAL PROGRESS TRACKER                       ");
        System.out.println("       17 Exercises | 255 Muscle-Memory Drills | Production-Grade AI Architecture        ");
        System.out.println("==========================================================================================");
        System.out.println();

        int currentPhase = 0;
        int grandPassed = 0;
        int grandTotal = 0;

        for (ExerciseDef ex : EXERCISES) {
            if (ex.phase() != currentPhase) {
                currentPhase = ex.phase();
                System.out.printf("--- PHASE %02d ----------------------------------------------------------------------------%n", currentPhase);
            }

            ExerciseResult res = evaluateExercise(ex);
            grandPassed += res.passed();
            grandTotal += res.total();

            String pBar = renderProgressBar(res.passed(), res.total(), 16);
            String score = String.format("%2d/%2d", res.passed(), res.total());
            String namePadded = String.format("%-52s", ex.name());

            System.out.printf("  %-8s %s %s %s (%s)%n", res.status(), namePadded, score, pBar, res.timestamp());
        }

        double overallRatio = grandTotal > 0 ? (double) grandPassed / grandTotal : 0.0;
        double overallPct = overallRatio * 100.0;
        String overallBar = renderProgressBar(grandPassed, grandTotal, 30);

        String masteryTier;
        if (overallPct >= 100.0) {
            masteryTier = "TIGERSTYLE MASTER (100% Certified)";
        } else if (overallPct >= 75.0) {
            masteryTier = "AI SYSTEMS ARCHITECT";
        } else if (overallPct >= 50.0) {
            masteryTier = "PRODUCTION SPECIALIST";
        } else if (overallPct >= 25.0) {
            masteryTier = "AI APPRENTICE";
        } else {
            masteryTier = "CADET (Practice Drills Seeded)";
        }

        System.out.println();
        System.out.println("==========================================================================================");
        System.out.printf("  OVERALL PROGRESS:  %d / %d DRILLS PASSED  (%.1f%%)%n", grandPassed, grandTotal, overallPct);
        System.out.printf("  PROGRESS BAR:      %s%n", overallBar);
        System.out.printf("  MASTERY TIER:      %s%n", masteryTier);
        System.out.println("==========================================================================================");
        System.out.println();
    }

    private static ExerciseResult evaluateExercise(ExerciseDef ex) {
        File reportsDir = new File(ex.id(), "target" + File.separator + "surefire-reports");
        if (!reportsDir.exists() || !reportsDir.isDirectory()) {
            return new ExerciseResult(0, ex.drills(), "[ IDLE ]", "-");
        }

        File[] xmlFiles = reportsDir.listFiles((dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            return new ExerciseResult(0, ex.drills(), "[ IDLE ]", "-");
        }

        Arrays.sort(xmlFiles, Comparator.comparingLong(File::lastModified).reversed());
        File latestXml = xmlFiles[0];

        try {
            String content = Files.readString(latestXml.toPath());
            int passed = 0;
            int total = ex.drills();

            Matcher m = TOTAL_PATTERN.matcher(content);
            if (m.find()) {
                passed = Integer.parseInt(m.group(1));
                total = Integer.parseInt(m.group(2));
            } else if (content.contains("failures=\"0\"") && content.contains("errors=\"0\"")) {
                passed = ex.drills();
            }

            String status;
            if (passed == total) {
                status = "[ PASS ]";
            } else if (passed > 0) {
                status = "[ HALF ]";
            } else {
                status = "[DRILL ]";
            }

            long lastMod = latestXml.lastModified();
            String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(lastMod));

            return new ExerciseResult(passed, total, status, timeStr);
        } catch (IOException e) {
            return new ExerciseResult(0, ex.drills(), "[ ERR  ]", "-");
        }
    }

    private static String renderProgressBar(int passed, int total, int width) {
        if (total <= 0) return "[                    ]   0.0%";
        double ratio = Math.min(1.0, Math.max(0.0, (double) passed / total));
        double percent = ratio * 100.0;
        int filled = (int) Math.round(ratio * width);
        int empty = width - filled;

        String bar = "#".repeat(filled) + "-".repeat(empty);
        return String.format("[%s] %5.1f%%", bar, percent);
    }
}
