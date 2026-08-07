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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.build.report.MojoReport;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.slf4j.MavenSimpleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

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
 * Log capture: installs a structured {@link MavenSimpleLogger.LogEventSink}
 * that receives level, logger name, message, and throwable independently of
 * the formatted console output. Uses thread-based tracking to associate events
 * with the currently-executing mojo or module.
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
     * Maximum number of log events captured per scope (mojo, module, or build).
     * Beyond this, events are dropped and a truncation notice is appended.
     */
    static final int MAX_LOG_EVENTS_PER_SCOPE = 500;

    // ---- Mutable state, populated during the build ----

    /** Per-project mojo tracking: project key -> list of in-flight/completed mojos. */
    private final Map<String, List<MojoTiming>> mojoTimings = new ConcurrentHashMap<>();

    /** Per-project start instants for duration computation. */
    private final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();

    /** Per-mojo start instants for duration computation. */
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();

    /** Session-level state - set once on SessionStarted. */
    private volatile MavenSession session;

    // ---- Log capture state ----

    /**
     * Maps thread ID -> mojo key for the currently-executing mojo on that thread.
     * Lifecycle events and mojo execution run on the same thread, so this is safe
     * for parallel builds with {@code -T}.
     */
    private final Map<Long, String> currentMojoByThread = new ConcurrentHashMap<>();

    /** Per-mojo log buffers: mojo key -> captured log events. */
    private final Map<String, List<LogEvent>> mojoLogBuffers = new ConcurrentHashMap<>();

    /**
     * Maps thread ID -> project key for the currently-building project on that thread.
     * Used to route log events that occur between mojo executions to the module-level buffer.
     */
    private final Map<Long, String> currentProjectByThread = new ConcurrentHashMap<>();

    /** Per-module log buffers: project key -> events captured outside any mojo. */
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
        installLogCapture();
    }

    private void onSessionEnded(ExecutionEvent event) {
        removeLogCapture();

        MavenSession endSession = event.getSession();
        if (endSession == null) {
            return;
        }

        try {
            BuildReport report = buildReport(endSession);
            writeReport(report, endSession);
        } catch (Exception e) {
            // Never let the report collector crash the build
            LOGGER.debug("Failed to produce build report: {}", e.getMessage(), e);
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
     * Installs a structured {@link MavenSimpleLogger.LogEventSink} to capture
     * log events with level, logger name, message, and throwable. This runs
     * in parallel to the existing console output pipeline - no wrapping or
     * forwarding needed.
     */
    private void installLogCapture() {
        MavenSimpleLogger.setLogEventSink(this::captureLogEvent);
    }

    private void removeLogCapture() {
        MavenSimpleLogger.setLogEventSink(null);
    }

    private void captureLogEvent(int level, String loggerName, String message, Throwable throwable) {
        long threadId = Thread.currentThread().getId();

        LogLevel logLevel = toLogLevel(level);
        Instant timestamp = MonotonicClock.now();
        String stackTrace = throwable != null ? truncateStackTrace(throwable) : null;
        LogEvent logEvent = new DefaultLogEvent(timestamp, logLevel, message, loggerName, stackTrace);

        // 1. Mojo-level: event belongs to the currently-executing mojo on this thread
        String mKey = currentMojoByThread.get(threadId);
        if (mKey != null) {
            List<LogEvent> buffer = mojoLogBuffers.get(mKey);
            if (buffer != null && buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buffer.add(logEvent);
            }
            return;
        }

        // 2. Module-level: project is active but no mojo is running
        String pKey = currentProjectByThread.get(threadId);
        if (pKey != null) {
            List<LogEvent> buffer = moduleLogBuffers.get(pKey);
            if (buffer != null && buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buffer.add(logEvent);
            }
            return;
        }

        // 3. Build-level: no project active (startup, reactor summary, post-build)
        if (buildLogBuffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
            buildLogBuffer.add(logEvent);
        }
    }

    private static LogLevel toLogLevel(int level) {
        return switch (level) {
            case LocationAwareLogger.TRACE_INT -> LogLevel.TRACE;
            case LocationAwareLogger.DEBUG_INT -> LogLevel.DEBUG;
            case LocationAwareLogger.INFO_INT -> LogLevel.INFO;
            case LocationAwareLogger.WARN_INT -> LogLevel.WARN;
            default -> LogLevel.ERROR;
        };
    }

    // ---- Report assembly ----

    BuildReport buildReport(MavenSession endSession) {
        Instant now = MonotonicClock.now();
        Instant startInstant = endSession.getRequest().getStartInstant();
        if (startInstant == null) {
            startInstant = now;
        }
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
                List.of(),
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
                // Unknown summary type - use its timing
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
                // Windows or restricted filesystem - fall back to a plain copy
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

    // ---- Utility methods ----

    private static String projectKey(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }

    private static String mojoKey(MavenProject project, MojoExecution mojo) {
        return projectKey(project) + "#" + mojo.getGoal() + "@" + mojo.getExecutionId();
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
