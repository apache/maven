/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.build;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.build.report.MojoReport;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.logging.ProjectBuildLogAppender;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects build lifecycle events and produces a structured {@link BuildReport}
 * at the end of the session.
 * <p>
 * Registered as an {@link org.apache.maven.eventspy.EventSpy} via {@code @Named}/{@code @Singleton},
 * following the same pattern as {@code DefaultPluginValidationManager}.
 * <p>
 * Thread-safe: concurrent module builds (with {@code -T}) each write to their
 * own entry in a {@link ConcurrentHashMap}.
 * <p>
 * Log capture: registers a callback on {@link ProjectBuildLogAppender} to
 * receive the already-formed {@link LogEvent} objects produced by the main
 * logging pipeline. Uses thread-based tracking to associate events with
 * the currently-executing mojo or module.
 *
 * @since 4.1.0
 */
@Singleton
@Named
public final class BuildReportCollector extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildReportCollector.class);

    static final String REPORT_DIR = "build-reports";
    static final String REPORT_LATEST = "build-report-latest.json";

    private static final int MAX_STACKTRACE_LINES = 30;

    /**
     * Logger names excluded from SLF4J auto-collection because these classes
     * already pipe structured {@link org.apache.maven.api.services.BuilderProblem}
     * objects directly to the {@link DefaultDiagnosticCollector}. Without this
     * exclusion, each problem would be counted twice: once from the direct pipe
     * and once from the SLF4J WARN interception.
     */
    private static final Set<String> EXCLUDED_LOGGERS = Set.of(
            BuildReportCollector.class.getName(),
            "org.apache.maven.DefaultMaven",
            "org.apache.maven.project.collector.DefaultProjectsSelector",
            "org.apache.maven.plugin.internal.DefaultPluginValidationManager");

    private final DefaultDiagnosticCollector diagnosticCollector;

    @Inject
    public BuildReportCollector(DefaultDiagnosticCollector diagnosticCollector) {
        this.diagnosticCollector = diagnosticCollector;
    }

    /**
     * No-arg constructor for tests that don't need diagnostic collection.
     */
    BuildReportCollector() {
        this(new DefaultDiagnosticCollector());
    }

    /**
     * Returns the diagnostic collector used by this build report collector.
     * Plugins can inject {@link DefaultDiagnosticCollector} directly, but this
     * accessor is provided for internal use and testing.
     */
    DefaultDiagnosticCollector getDiagnosticCollector() {
        return diagnosticCollector;
    }

    /**
     * Maximum number of log events captured per scope (mojo, module, or build).
     * Beyond this, events are dropped and a truncation notice is appended.
     */
    static final int MAX_LOG_EVENTS_PER_SCOPE = 500;

    // ---- Mutable state, populated during the build ----

    /** Per-project mojo tracking: project key → list of in-flight/completed mojos. */
    private final Map<String, List<MojoTiming>> mojoTimings = new ConcurrentHashMap<>();

    /** Per-project start instants for duration computation. */
    private final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();

    /** Per-mojo start instants for duration computation. */
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();

    /** Session-level state — set once on SessionStarted. */
    private volatile MavenSession session;

    // ---- Log capture state ----

    /**
     * Maps thread ID → mojo key for the currently-executing mojo on that thread.
     * Lifecycle events and mojo execution run on the same thread, so this is safe
     * for parallel builds with {@code -T}.
     */
    private final Map<Long, String> currentMojoByThread = new ConcurrentHashMap<>();

    /** Per-mojo log buffers: mojo key → captured log events. */
    private final Map<String, List<LogEvent>> mojoLogBuffers = new ConcurrentHashMap<>();

    /**
     * Maps thread ID → project key for the currently-building project on that thread.
     * Used to route log events that occur between mojo executions to the module-level buffer.
     */
    private final Map<Long, String> currentProjectByThread = new ConcurrentHashMap<>();

    /** Per-module log buffers: project key → events captured outside any mojo. */
    private final Map<String, List<LogEvent>> moduleLogBuffers = new ConcurrentHashMap<>();

    /** Build-level log buffer: events captured outside any module lifecycle. */
    private final List<LogEvent> buildLogBuffer = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent executionEvent) {
            switch (executionEvent.getType()) {
                case SessionStarted:
                    onSessionStarted(executionEvent);
                    break;
                case SessionEnded:
                    onSessionEnded(executionEvent);
                    break;
                case ProjectStarted:
                    onProjectStarted(executionEvent);
                    break;
                case ProjectSucceeded:
                case ProjectFailed:
                case ProjectSkipped:
                    onProjectFinished(executionEvent);
                    break;
                case MojoStarted:
                    onMojoStarted(executionEvent);
                    break;
                case MojoSucceeded:
                case MojoFailed:
                    onMojoFinished(executionEvent);
                    break;
                default:
                    break;
            }
        }
    }

    // ---- Event handlers ----

    private void onSessionStarted(ExecutionEvent event) {
        this.session = event.getSession();
        configureDiagnosticSuppression();
        installLogCapture();
    }

    /**
     * Reads the {@code maven.diagnostic.suppress} user property and configures
     * the diagnostic collector to suppress matching keys. The property accepts
     * a comma-separated list of keys or patterns:
     * <ul>
     *   <li>Exact keys: {@code -Dmaven.diagnostic.suppress=deprecated-source-target}</li>
     *   <li>Prefix wildcards: {@code -Dmaven.diagnostic.suppress=auto:*} (suppresses all
     *       auto-collected warnings from Maven 3 plugins)</li>
     *   <li>Multiple: {@code -Dmaven.diagnostic.suppress=key1,key2,auto:*}</li>
     * </ul>
     */
    private void configureDiagnosticSuppression() {
        if (session == null) {
            return;
        }
        String suppressProp = session.getUserProperties().getProperty("maven.diagnostic.suppress");
        if (suppressProp == null || suppressProp.isBlank()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        for (String token : suppressProp.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                keys.add(trimmed);
            }
        }
        if (!keys.isEmpty()) {
            diagnosticCollector.setSuppressedKeys(keys);
            LOGGER.debug("Diagnostic suppression configured: {}", keys);
        }
    }

    private void onSessionEnded(ExecutionEvent event) {
        removeLogCapture();

        MavenSession endSession = event.getSession();
        if (endSession == null) {
            return;
        }

        // Read warning mode from user properties (set by MavenInvoker from --warning-mode)
        String warningMode = endSession.getUserProperties().getProperty("maven.build.warningMode", "summary");

        BuildReport report = buildReport(endSession);
        writeReport(report, endSession);

        if (!"none".equalsIgnoreCase(warningMode)) {
            printDiagnosticSummary();
        }

        // --warning-mode=fail: fail the build if any warnings were collected
        if ("fail".equalsIgnoreCase(warningMode) && diagnosticCollector.hasWarnings()) {
            int warningCount = 0;
            for (DefaultDiagnosticSummary entry : diagnosticCollector.getSummary()) {
                if (entry.problem().getSeverity() == BuilderProblem.Severity.WARNING) {
                    warningCount += entry.count();
                }
            }
            endSession
                    .getResult()
                    .addException(new RuntimeException(
                            "Build has " + warningCount + " warning(s) and --warning-mode=fail is set"));
        }
    }

    private void onProjectStarted(ExecutionEvent event) {
        String key = projectKey(event.getProject());
        projectStartTimes.put(key, MonotonicClock.now());
        mojoTimings.putIfAbsent(key, Collections.synchronizedList(new ArrayList<>()));
        moduleLogBuffers.put(key, Collections.synchronizedList(new ArrayList<>()));
        currentProjectByThread.put(Thread.currentThread().getId(), key);
    }

    private void onProjectFinished(ExecutionEvent event) {
        // Unregister the project from this thread so subsequent log events
        // fall through to the build-level buffer
        currentProjectByThread.remove(Thread.currentThread().getId());
    }

    private void onMojoStarted(ExecutionEvent event) {
        String mKey = mojoKey(event.getProject(), event.getMojoExecution());
        mojoStartTimes.put(mKey, MonotonicClock.now());

        // Register the current mojo for this thread so the log event sink
        // can associate events with this mojo execution
        currentMojoByThread.put(Thread.currentThread().getId(), mKey);
        mojoLogBuffers.put(mKey, Collections.synchronizedList(new ArrayList<>()));
    }

    private void onMojoFinished(ExecutionEvent event) {
        MojoExecution mojo = event.getMojoExecution();
        MavenProject project = event.getProject();
        String mKey = mojoKey(project, mojo);
        String pKey = projectKey(project);

        // Unregister the mojo from this thread
        currentMojoByThread.remove(Thread.currentThread().getId());

        Instant now = MonotonicClock.now();
        Instant startInstant = mojoStartTimes.remove(mKey);
        if (startInstant == null) {
            startInstant = now;
        }
        Duration duration = Duration.between(startInstant, now);

        BuildStatus status =
                event.getType() == ExecutionEvent.Type.MojoSucceeded ? BuildStatus.SUCCESS : BuildStatus.FAILURE;

        // Drain the log buffer for this mojo
        List<LogEvent> logBuffer = mojoLogBuffers.remove(mKey);
        List<LogEvent> output = logBuffer != null ? List.copyOf(logBuffer) : List.of();

        MojoTiming timing = new MojoTiming(
                mojo.getGroupId(),
                mojo.getArtifactId(),
                mojo.getVersion(),
                mojo.getGoal(),
                mojo.getExecutionId(),
                mojo.getLifecyclePhase(),
                status,
                startInstant,
                duration,
                output);

        mojoTimings
                .computeIfAbsent(pKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(timing);
    }

    // ---- Structured log capture ----

    /**
     * Registers a callback on {@link ProjectBuildLogAppender} to receive the
     * already-formed {@link LogEvent} objects from the main logging pipeline.
     * This eliminates the need for a separate capture path and ensures the
     * build report captures the same enriched events (with sequence number,
     * source metadata) as the console output.
     */
    private void installLogCapture() {
        ProjectBuildLogAppender.setReportCapture(this::captureLogEvent);
    }

    private void removeLogCapture() {
        ProjectBuildLogAppender.setReportCapture(null);
    }

    /**
     * Routes a pre-formed {@link LogEvent} to the appropriate buffer
     * (mojo, module, or build-level) based on the current thread's
     * lifecycle context.
     */
    private void captureLogEvent(LogEvent event) {
        long threadId = Thread.currentThread().getId();

        // Auto-collect WARN-level log events as build problems, giving Maven 3 plugins
        // automatic deduplication and summary at end of build without code changes.
        // Skip loggers that already pipe structured BuilderProblems directly to the
        // DiagnosticCollector (avoiding double-counting), and our own logger to avoid
        // feedback loops from problem summary printing.
        if (event.level() == LogLevel.WARN
                && event.message() != null
                && !EXCLUDED_LOGGERS.contains(event.loggerName())) {
            String syntheticKey = syntheticDiagnosticKey(event.loggerName(), event.message());
            diagnosticCollector.report(BuilderProblem.builder()
                    .source(event.loggerName())
                    .message(event.message())
                    .severity(BuilderProblem.Severity.WARNING)
                    .key(syntheticKey)
                    .build());
        }

        // 1. Mojo-level: event belongs to the currently-executing mojo on this thread
        String mKey = currentMojoByThread.get(threadId);
        if (mKey != null) {
            List<LogEvent> buffer = mojoLogBuffers.get(mKey);
            if (buffer != null && buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buffer.add(event);
            }
            return;
        }

        // 2. Module-level: project is active but no mojo is running
        String pKey = currentProjectByThread.get(threadId);
        if (pKey != null) {
            List<LogEvent> buffer = moduleLogBuffers.get(pKey);
            if (buffer != null && buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buffer.add(event);
            }
            return;
        }

        // 3. Build-level: no project active (startup, reactor summary, post-build)
        if (buildLogBuffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
            buildLogBuffer.add(event);
        }
    }

    // ---- Report assembly ----

    BuildReport buildReport(MavenSession endSession) {
        Instant now = MonotonicClock.now();
        Instant startInstant = endSession.getRequest().getStartInstant();
        Duration totalDuration = Duration.between(startInstant, now);

        MavenExecutionResult result = endSession.getResult();
        boolean hasFailures = result != null && result.hasExceptions();
        BuildStatus overallStatus = hasFailures ? BuildStatus.FAILURE : BuildStatus.SUCCESS;

        // Collect module reports
        List<ModuleReport> moduleReports = new ArrayList<>();
        for (MavenProject project : endSession.getProjects()) {
            moduleReports.add(buildModuleReport(project, endSession));
        }

        // Collect failures
        List<FailureReport> failureReports = new ArrayList<>();
        if (result != null) {
            for (MavenProject project : endSession.getProjects()) {
                BuildSummary summary = result.getBuildSummary(project);
                if (summary instanceof BuildFailure buildFailure) {
                    failureReports.add(buildFailureReport(project, buildFailure));
                }
            }
        }

        // Metadata
        String mavenVersion = endSession.getSystemProperties().getProperty("maven.version", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");
        List<String> goals = endSession.getGoals();
        MavenProject topProject = endSession.getTopLevelProject();
        String projectId = topProject != null
                ? topProject.getGroupId() + ":" + topProject.getArtifactId() + ":" + topProject.getVersion()
                : "unknown";
        boolean multiModule = endSession.getProjects().size() > 1;
        int threads = endSession.getRequest().getDegreeOfConcurrency();

        // Problems (deduplicated)
        List<BuilderProblem> problems = diagnosticCollector.getProblems();

        // Build-level log events (outside any module lifecycle)
        List<LogEvent> buildOutput = List.copyOf(buildLogBuffer);

        return new DefaultBuildReport(
                overallStatus,
                totalDuration,
                startInstant,
                mavenVersion,
                javaVersion,
                goals,
                projectId,
                multiModule,
                threads,
                moduleReports,
                failureReports,
                problems,
                buildOutput);
    }

    private ModuleReport buildModuleReport(MavenProject project, MavenSession endSession) {
        String key = projectKey(project);

        // Duration from BuildSummary (preferred) or fallback to our own tracking
        MavenExecutionResult result = endSession.getResult();
        Duration duration = Duration.ZERO;
        BuildStatus status = BuildStatus.SKIPPED;
        Instant moduleStartTime =
                projectStartTimes.getOrDefault(key, endSession.getRequest().getStartInstant());

        if (result != null) {
            BuildSummary summary = result.getBuildSummary(project);
            if (summary instanceof BuildSuccess) {
                status = BuildStatus.SUCCESS;
                duration = summary.getExecTime();
            } else if (summary instanceof BuildFailure) {
                status = BuildStatus.FAILURE;
                duration = summary.getExecTime();
            } else if (summary != null) {
                // Unknown summary type — use its timing
                duration = summary.getExecTime();
            } else {
                // No summary means skipped
                Instant start = projectStartTimes.get(key);
                if (start != null) {
                    duration = Duration.between(start, MonotonicClock.now());
                }
            }
        }

        // Mojo reports
        List<MojoTiming> timings = mojoTimings.getOrDefault(key, Collections.emptyList());
        List<MojoReport> mojoReports;
        synchronized (timings) {
            mojoReports = timings.stream()
                    .map(t -> (MojoReport) new DefaultMojoReport(
                            t.groupId,
                            t.artifactId,
                            t.version,
                            t.goal,
                            t.executionId,
                            t.phase,
                            t.status,
                            t.startTime,
                            t.duration,
                            t.output))
                    .toList();
        }

        // Module-level log events (between mojos)
        List<LogEvent> moduleLogBuffer = moduleLogBuffers.getOrDefault(key, Collections.emptyList());
        List<LogEvent> moduleOutput;
        synchronized (moduleLogBuffer) {
            moduleOutput = List.copyOf(moduleLogBuffer);
        }

        return new DefaultModuleReport(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                status,
                moduleStartTime,
                duration,
                mojoReports,
                moduleOutput);
    }

    private FailureReport buildFailureReport(MavenProject project, BuildFailure buildFailure) {
        String module = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();

        // Try to find which mojo failed
        String mojoId = null;
        List<MojoTiming> timings = mojoTimings.getOrDefault(projectKey(project), Collections.emptyList());
        synchronized (timings) {
            for (MojoTiming t : timings) {
                if (t.status == BuildStatus.FAILURE) {
                    mojoId = t.artifactId + ":" + t.version + ":" + t.goal;
                    break;
                }
            }
        }

        Throwable cause = buildFailure.getCause();
        String message = cause != null ? cause.getMessage() : "Unknown error";
        String stackTrace = cause != null ? truncateStackTrace(cause) : null;

        Instant failureTimestamp = MonotonicClock.now();
        String exceptionType = cause != null ? cause.getClass().getSimpleName() : null;

        return new DefaultFailureReport(
                module,
                mojoId,
                failureTimestamp,
                exceptionType,
                message != null ? message : "Unknown error",
                stackTrace);
    }

    // ---- JSON persistence ----

    void writeReport(BuildReport report, MavenSession endSession) {
        Path topDirectory = endSession.getTopDirectory();
        if (topDirectory == null) {
            LOGGER.debug("No top directory available, skipping build report");
            return;
        }

        Path reportsDir = topDirectory.resolve("target").resolve(REPORT_DIR);

        try {
            Files.createDirectories(reportsDir);
            String json = BuildReportJsonWriter.toJson(report);

            // Timestamped file: build-report-20250729T143000Z.json
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(report.startTime());
            Path timestampedFile = reportsDir.resolve("build-report-" + timestamp + ".json");

            // Write to a temp file, then atomic-move into place so a crash
            // never leaves a half-written report on disk.
            Path tmpFile = Files.createTempFile(reportsDir, ".build-report-", ".tmp");
            try {
                Files.writeString(tmpFile, json);
                atomicMove(tmpFile, timestampedFile);
            } catch (IOException e) {
                Files.deleteIfExists(tmpFile);
                throw e;
            }

            // Latest symlink (or copy on filesystems that don't support symlinks)
            Path latestFile = reportsDir.resolve(REPORT_LATEST);
            try {
                // Atomic symlink swap: create new link, then rename over the old one
                Path tmpLink = Files.createTempFile(reportsDir, ".latest-", ".tmp");
                Files.delete(tmpLink); // createTempFile creates a regular file
                Files.createSymbolicLink(tmpLink, timestampedFile.getFileName());
                atomicMove(tmpLink, latestFile);
            } catch (UnsupportedOperationException | IOException symEx) {
                // Windows or restricted filesystem — fall back to a plain copy
                Files.writeString(latestFile, json);
            }

            LOGGER.debug("Build report written to {}", timestampedFile);
        } catch (IOException e) {
            LOGGER.warn("Failed to write build report to {}: {}", reportsDir, e.getMessage());
        }
    }

    /**
     * Attempts an atomic move; falls back to a plain move if the filesystem
     * does not support {@code ATOMIC_MOVE}.
     */
    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ---- Diagnostic summary ----

    /**
     * Prints a deduplicated summary of diagnostics (warnings and errors) at the
     * end of the build. This ensures important messages are not lost in scrollback.
     */
    void printDiagnosticSummary() {
        List<DefaultDiagnosticSummary> summary = diagnosticCollector.getSummary();
        if (summary.isEmpty()) {
            return;
        }

        // Count unique warnings and total occurrences
        int uniqueWarnings = 0;
        int totalOccurrences = 0;
        int uniqueErrors = 0;
        for (DefaultDiagnosticSummary entry : summary) {
            BuilderProblem.Severity sev = entry.problem().getSeverity();
            if (sev == BuilderProblem.Severity.WARNING) {
                uniqueWarnings++;
                totalOccurrences += entry.count();
            } else if (sev == BuilderProblem.Severity.ERROR) {
                uniqueErrors++;
                totalOccurrences += entry.count();
            }
        }

        if (uniqueWarnings == 0 && uniqueErrors == 0) {
            return;
        }

        // Print header
        StringBuilder header = new StringBuilder();
        if (uniqueWarnings > 0) {
            header.append(uniqueWarnings).append(" warning");
            if (uniqueWarnings > 1) {
                header.append('s');
            }
        }
        if (uniqueErrors > 0) {
            if (header.length() > 0) {
                header.append(", ");
            }
            header.append(uniqueErrors).append(" error");
            if (uniqueErrors > 1) {
                header.append('s');
            }
        }
        if (totalOccurrences > (uniqueWarnings + uniqueErrors)) {
            header.append(" (").append(totalOccurrences).append(" total occurrences)");
        }

        // Print the summary at INFO level. We intentionally do NOT re-print individual
        // warning messages here — they were already logged inline at WARN level. Re-printing
        // the raw message text would double warning counts in log parsers, trigger
        // --fail-on-severity WARN again, and confuse tools that search for specific text.
        // Full details are available in target/build-reports/.
        LOGGER.info("");
        LOGGER.info("Diagnostics: {} — see target/{}/{} for details", header, REPORT_DIR, REPORT_LATEST);
    }

    // ---- Utility methods ----

    private static String projectKey(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }

    private static String mojoKey(MavenProject project, MojoExecution mojo) {
        return projectKey(project) + "#" + mojo.getGoal() + "@" + mojo.getExecutionId();
    }

    /**
     * Generates a stable deduplication key for a warning intercepted from a
     * Maven 3 plugin's {@code Log.warn()} call. The key is derived from the
     * logger name and a normalized hash of the message text, so that the same
     * warning from different modules or files deduplicates correctly.
     * <p>
     * File-specific coordinates (paths, line numbers) are stripped before hashing
     * so that "Foo.java:42: unchecked cast" and "Bar.java:99: unchecked cast"
     * map to the same key.
     */
    static String syntheticDiagnosticKey(String loggerName, String message) {
        // Strip file coordinates for dedup: remove paths and line/column numbers
        String normalized = message.replaceAll("\\S+\\.java:\\d+(:\\d+)?:?\\s*", "")
                .replaceAll("\\S+[\\\\/][\\w.]+:\\d+", "")
                .trim();
        // Use a short logger suffix to namespace the key
        String loggerSuffix = loggerName;
        int lastDot = loggerName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < loggerName.length() - 1) {
            loggerSuffix = loggerName.substring(lastDot + 1);
        }
        return "auto:" + loggerSuffix + ":" + Integer.toHexString(normalized.hashCode());
    }

    static String truncateStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();
        String[] lines = full.split("\n");
        if (lines.length <= MAX_STACKTRACE_LINES) {
            return full;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STACKTRACE_LINES; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append("... ").append(lines.length - MAX_STACKTRACE_LINES).append(" more lines truncated\n");
        return sb.toString();
    }

    // ---- Internal records ----

    record MojoTiming(
            String groupId,
            String artifactId,
            String version,
            String goal,
            String executionId,
            String phase,
            BuildStatus status,
            Instant startTime,
            Duration duration,
            List<LogEvent> output) {}
}
