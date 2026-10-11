<#
.SYNOPSIS
    Interactive Terminal Progress Tracker for Spring AI TigerStyle Mastery Curriculum.
    Scans all 17 exercises and tracks progress across all 255 muscle-memory drills.

.DESCRIPTION
    Scans surefire reports and test results across all modules to display a live
    interactive dashboard of your progress through the curriculum.
    Supports running tests for specific phases or the entire curriculum.

.PARAMETER RunAll
    Executes 'mvn test' across all 17 modules and refreshes the dashboard.

.PARAMETER Phase
    Runs tests for all exercises in a specific phase (1 through 8).

.PARAMETER Module
    Runs tests for a single specific exercise module name.

.EXAMPLE
    .\tracker.ps1
    Displays the instant status dashboard across all 17 exercises.

.EXAMPLE
    .\tracker.ps1 -Phase 1
    Runs tests for Phase 01 and displays updated progress.

.EXAMPLE
    .\tracker.ps1 -RunAll
    Runs full evaluation across all 17 exercises.
#>

[CmdletBinding()]
param (
    [switch]$RunAll,
    [int]$Phase = 0,
    [string]$Module = "",
    [switch]$Interactive
)

$ErrorActionPreference = "Continue"

$Exercises = @(
    @{ Id = "phase01-ex01-chat-basics";         Phase = 1; Name = "Phase 01 Ex01: ChatClient Fluent API & Foundations";        Drills = 15 },
    @{ Id = "phase01-ex02-chat-advanced";       Phase = 1; Name = "Phase 01 Ex02: Advanced ChatClient & FinTech Ops";           Drills = 15 },
    @{ Id = "phase01-ex03-cloud-sre";           Phase = 1; Name = "Phase 01 Ex03: Cloud SRE & K8s Operations";                  Drills = 15 },
    @{ Id = "phase02-ex01-structured-outputs";  Phase = 2; Name = "Phase 02 Ex01: Structured Outputs & CVE Triage";             Drills = 15 },
    @{ Id = "phase02-ex02-resilient-decisions"; Phase = 2; Name = "Phase 02 Ex02: Resilient Schemas & Decision Repair";        Drills = 15 },
    @{ Id = "phase03-ex01-tool-sandboxing";     Phase = 3; Name = "Phase 03 Ex01: Tool Callbacks & Sandboxing";                Drills = 15 },
    @{ Id = "phase03-ex02-multi-tool-drills";   Phase = 3; Name = "Phase 03 Ex02: Multi-Tool Chains & K8s Ops";                Drills = 15 },
    @{ Id = "phase04-ex01-guardrail-advisors";  Phase = 4; Name = "Phase 04 Ex01: Guardrail Advisors & Prompt Defense";        Drills = 15 },
    @{ Id = "phase04-ex02-resilient-guardrails";Phase = 4; Name = "Phase 04 Ex02: Resilient Advisors & Failover";             Drills = 15 },
    @{ Id = "phase05-ex01-offline-harness";     Phase = 5; Name = "Phase 05 Ex01: Deterministic Offline Test Harness";         Drills = 15 },
    @{ Id = "phase05-ex02-advanced-harness";    Phase = 5; Name = "Phase 05 Ex02: Advanced Harness & Fault Injection";         Drills = 15 },
    @{ Id = "phase06-ex01-evaluators";          Phase = 6; Name = "Phase 06 Ex01: Deterministic Evaluators & Benchmarks";       Drills = 15 },
    @{ Id = "phase06-ex02-advanced-judges";     Phase = 6; Name = "Phase 06 Ex02: LLM-as-a-Judge & Alignment Evaluation";      Drills = 15 },
    @{ Id = "phase07-ex01-vector-stores";       Phase = 7; Name = "Phase 07 Ex01: Vector Stores & Similarity Search";          Drills = 15 },
    @{ Id = "phase07-ex02-rag-advisors";        Phase = 7; Name = "Phase 07 Ex02: RAG Advisors & Document Augmentation";       Drills = 15 },
    @{ Id = "phase08-ex01-mcp-server";          Phase = 8; Name = "Phase 08 Ex01: Model Context Protocol Server";              Drills = 15 },
    @{ Id = "phase08-ex02-mcp-client";          Phase = 8; Name = "Phase 08 Ex02: MCP Client Integration & Tool Routing";      Drills = 15 }
)

function Get-ExerciseResult($ex) {
    $dir = $ex.Id
    $reportsDir = Join-Path $dir "target\surefire-reports"
    $xmlFiles = Get-ChildItem -Path $reportsDir -Filter "TEST-*.xml" -ErrorAction SilentlyContinue

    if (-not $xmlFiles -or $xmlFiles.Count -eq 0) {
        return @{ Passed = 0; Total = $ex.Drills; Status = "NOT_RUN"; Time = "-" }
    }

    # Use the newest report file
    $latestXml = $xmlFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $content = Get-Content -Path $latestXml.FullName -Raw

    $passed = 0
    $total = $ex.Drills

    # Check for TOTAL: X / 15 PASSED in stdout
    if ($content -match "TOTAL:\s*(\d+)\s*/\s*(\d+)\s*PASSED") {
        $passed = [int]$matches[1]
        $total = [int]$matches[2]
    } elseif ($content -match 'failures="0"' -and $content -match 'errors="0"') {
        $passed = $ex.Drills
    } else {
        # Count [PASS] occurrences
        $passMatches = [regex]::Matches($content, "\[PASS\]")
        $passed = $passMatches.Count
        if ($passed -gt $ex.Drills) { $passed = $ex.Drills }
    }

    $status = if ($passed -eq $total) { "PASSED" } elseif ($passed -gt 0) { "PARTIAL" } else { "PRACTICE" }
    $timeStr = $latestXml.LastWriteTime.ToString("yyyy-MM-dd HH:mm")

    return @{ Passed = $passed; Total = $total; Status = $status; Time = $timeStr }
}

function Render-ProgressBar($passed, $total, $width = 20) {
    if ($total -le 0) { return "[                    ]   0.0%" }
    $ratio = [math]::Min(1.0, [math]::Max(0.0, ($passed / $total)))
    $percent = [math]::Round($ratio * 100, 1)
    $filledChars = [math]::Round($ratio * $width)
    $emptyChars = $width - $filledChars

    $bar = ("#" * $filledChars) + ("-" * $emptyChars)
    $pctStr = "{0,5:F1}%" -f $percent
    return "[$bar] $pctStr"
}

function Show-Dashboard {
    Clear-Host
    Write-Host ""
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    Write-Host "           SPRING AI TIGERSTYLE MASTERY - INTERACTIVE PROGRESS TRACKER                    " -ForegroundColor Yellow
    Write-Host "       17 Exercises | 255 Muscle-Memory Drills | Production-Grade AI Architecture        " -ForegroundColor DarkCyan
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    Write-Host ""

    $currentPhase = 0
    $grandPassed = 0
    $grandTotal = 0

    foreach ($ex in $Exercises) {
        if ($ex.Phase -ne $currentPhase) {
            $currentPhase = $ex.Phase
            Write-Host ("--- PHASE {0:D2} ----------------------------------------------------------------------------" -f $currentPhase) -ForegroundColor DarkGray
        }

        $res = Get-ExerciseResult $ex
        $grandPassed += $res.Passed
        $grandTotal += $res.Total

        $pBar = Render-ProgressBar $res.Passed $res.Total 16
        $scoreStr = "{0,2}/{1,2}" -f $res.Passed, $res.Total

        $statusBadge = switch ($res.Status) {
            "PASSED"   { "[ PASS ]" }
            "PARTIAL"  { "[ HALF ]" }
            "PRACTICE" { "[DRILL ]" }
            "NOT_RUN"  { "[ IDLE ]" }
        }

        $badgeColor = switch ($res.Status) {
            "PASSED"   { "Green" }
            "PARTIAL"  { "Yellow" }
            "PRACTICE" { "Magenta" }
            "NOT_RUN"  { "DarkGray" }
        }

        $namePadded = $ex.Name.PadRight(52)
        Write-Host "  " -NoNewline
        Write-Host $statusBadge -ForegroundColor $badgeColor -NoNewline
        Write-Host " $namePadded $scoreStr $pBar " -NoNewline
        Write-Host "($($res.Time))" -ForegroundColor DarkGray
    }

    $overallRatio = if ($grandTotal -gt 0) { $grandPassed / $grandTotal } else { 0 }
    $overallPct = [math]::Round($overallRatio * 100, 1)
    $overallBar = Render-ProgressBar $grandPassed $grandTotal 30

    $masteryTier = if ($overallPct -ge 100.0) {
        "TIGERSTYLE MASTER (100% Certified)"
    } elseif ($overallPct -ge 75.0) {
        "AI SYSTEMS ARCHITECT"
    } elseif ($overallPct -ge 50.0) {
        "PRODUCTION SPECIALIST"
    } elseif ($overallPct -ge 25.0) {
        "AI APPRENTICE"
    } else {
        "CADET (Practice Drills Seeded)"
    }

    Write-Host ""
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    Write-Host ("  OVERALL PROGRESS:  {0} / {1} DRILLS PASSED  ({2:F1}%)" -f $grandPassed, $grandTotal, $overallPct) -ForegroundColor Yellow
    Write-Host "  PROGRESS BAR:      $overallBar" -ForegroundColor Green
    Write-Host "  MASTERY TIER:      $masteryTier" -ForegroundColor Cyan
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    Write-Host ""
}

# Execution Dispatcher
if ($Module -ne "") {
    Write-Host "Running tests for module: $Module..." -ForegroundColor Cyan
    mvn test -pl $Module
    Show-Dashboard
    exit 0
}

if ($Phase -ge 1 -and $Phase -le 8) {
    $phaseModules = ($Exercises | Where-Object { $_.Phase -eq $Phase } | ForEach-Object { $_.Id }) -join ","
    Write-Host "Running tests for Phase $Phase modules: $phaseModules..." -ForegroundColor Cyan
    mvn test -pl $phaseModules
    Show-Dashboard
    exit 0
}

if ($RunAll) {
    Write-Host "Running full test suite across all 17 curriculum modules..." -ForegroundColor Cyan
    mvn test
    Show-Dashboard
    exit 0
}

if ($Interactive) {
    while ($true) {
        Show-Dashboard
        Write-Host "  COMMANDS:" -ForegroundColor Yellow
        Write-Host "    [1-8]  Run tests for Phase (e.g. 1 for Phase 01)"
        Write-Host "    [A]    Run ALL tests (Full Curriculum Evaluation)"
        Write-Host "    [R]    Refresh Dashboard"
        Write-Host "    [Q]    Quit"
        Write-Host ""
        $choice = Read-Host "Select option"
        if ($choice -eq "Q" -or $choice -eq "q") { break }
        elseif ($choice -eq "A" -or $choice -eq "a") {
            mvn test
        } elseif ($choice -match "^[1-8]$") {
            $selPhase = [int]$choice
            $phaseModules = ($Exercises | Where-Object { $_.Phase -eq $selPhase } | ForEach-Object { $_.Id }) -join ","
            mvn test -pl $phaseModules
        }
    }
    exit 0
}

# Default action: render dashboard
Show-Dashboard
