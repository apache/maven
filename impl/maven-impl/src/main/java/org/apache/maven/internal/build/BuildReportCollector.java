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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.Constants;
import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.MojoReport;
import org.apache.maven.api.build.report.SubprojectReport;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.spi.SpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Collects build lifecycle events and produces a structured {@link BuildReport}
 * at the end of the session.
 * <p>
 * Implements {@link ExecutionListener} for typed lifecycle callbacks,
 * {@link LogSink} for receiving log events from {@code ProjectBuildLogAppender},
 * and {@link SpiService} for auto-discovery via the DI container.
 * <p>
 * Thread-safe: concurrent subproject builds (with {@code -T}) each write to their
 * own entry in a {@link ConcurrentHashMap}.
 * <p>
 * Log capture: receives already-enriched {@link LogEvent} objects from
 * {@code ProjectBuildLogAppender} via the {@link LogSink#accept(LogEvent)} method.
 * Uses thread-based tracking to associate events with the currently-executing
 * mojo or subproject.
 *
 * @since 4.2.0
 */
public final class BuildReportCollector implements ExecutionListener, LogSink, SpiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildReportCollector.class);

    static final String REPORT_DIR = "build-reports";
    static final String REPORT_LATEST = "build-report-latest.json";
    static final String TRACE_ID_MDC_KEY = "otel.trace_id";
    static final String SPAN_ID_MDC_KEY = "otel.span_id";

    private static final int DEFAULT_MAX_STACKTRACE_LINES = 30;

    // ---- Per-session config (read from system properties at SessionStarted) ----

    /** Cap on log events per scope; set at session start from {@link Constants#MAVEN_BUILD_REPORT_MAX_LOG_EVENTS}. */
    private volatile int maxLogEventsPerScope = 500;

    /** Cap on stack-trace lines per failure; set at session start from {@link Constants#MAVEN_BUILD_REPORT_MAX_STACKTRACE_LINES}. */
    private volatile int maxStacktraceLines = DEFAULT_MAX_STACKTRACE_LINES;

    /** Cap on throwable lines per log event; set at session start from {@link Constants#MAVEN_BUILD_REPORT_MAX_THROWABLE_LINES}. */
    private volatile int maxThrowableLines = BuildReportJsonWriter.DEFAULT_MAX_THROWABLE_LINES;

    // ---- Mutable state, populated during the build ----

    /** Per-project mojo tracking: project key -> queue of in-flight/completed mojos. */
    private final Map<String, Queue<MojoTiming>> mojoTimings = new ConcurrentHashMap<>();

    /** Per-project start instants for duration computation. */
    private final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();

    /** Per-project trace context captured when the project starts. */
    private final Map<String, TraceContext> projectTraceContexts = new ConcurrentHashMap<>();

    /** Per-mojo start instants for duration computation. */
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();

    /** Per-mojo trace context captured when the mojo starts. */
    private final Map<String, TraceContext> mojoTraceContexts = new ConcurrentHashMap<>();

    /** Per-project build status tracked from project events. */
    private final Map<String, BuildStatus> projectStatuses = new ConcurrentHashMap<>();

    /** Per-project build duration tracked from start/finish events. */
    private final Map<String, Duration> projectDurations = new ConcurrentHashMap<>();

    /** Per-project failure causes. */
    private final Map<String, Throwable> projectFailures = new ConcurrentHashMap<>();

    /** Session-level state - set once on SessionStarted. */
    private volatile Session session;

    /** Build invocation - captured once at SessionStarted, immutable thereafter. */
    private volatile BuildInvocation buildInvocation;

    /** Session start time. */
    private volatile Instant sessionStartTime;

    // ---- Log capture state ----

    /**
     * Maps thread ID -> mojo key for the currently-executing mojo on that thread.
     * Lifecycle events and mojo execution run on the same thread, so this is safe
     * for parallel builds with {@code -T}.
     */
    private final Map<Long, String> currentMojoByThread = new ConcurrentHashMap<>();

    /** Per-mojo log buffers: mojo key -> captured log events. */
    private final Map<String, BlockingQueue<LogEvent>> mojoLogBuffers = new ConcurrentHashMap<>();

    /**
     * Maps thread ID -> project key for the currently-building project on that thread.
     * Used to route log events that occur between mojo executions to the subproject-level buffer.
     */
    private final Map<Long, String> currentProjectByThread = new ConcurrentHashMap<>();

    /** Per-subproject log buffers: project key -> events captured outside any mojo. */
    private final Map<String, BlockingQueue<LogEvent>> subprojectLogBuffers = new ConcurrentHashMap<>();

    /** Build-level log buffer: events captured outside any subproject lifecycle. Created at session start. */
    private volatile BlockingQueue<LogEvent> buildLogBuffer = new ArrayBlockingQueue<>(500);

    // ---- LogSink implementation ----

    /**
     * Receives pre-formed {@link LogEvent} objects from {@code ProjectBuildLogAppender}.
     * Routes to the appropriate buffer (mojo, subproject, or build-level) based on
     * the current thread's lifecycle context.
     * <p>
     * No-op when no session is active.
     */
    @Override
    public void accept(LogEvent event) {
        // No-op when no session is active
        if (session == null) {
            return;
        }
        long threadId = Thread.currentThread().getId();
        // 1. Mojo-level
        String mKey = currentMojoByThread.get(threadId);
        if (mKey != null) {
            BlockingQueue<LogEvent> buffer = mojoLogBuffers.get(mKey);
            if (buffer != null) {
                buffer.offer(event);
            }
            return;
        }
        // 2. Module-level
        String pKey = currentProjectByThread.get(threadId);
        if (pKey != null) {
            BlockingQueue<LogEvent> buffer = subprojectLogBuffers.get(pKey);
            if (buffer != null) {
                buffer.offer(event);
            }
            return;
        }
        // 3. Build-level
        buildLogBuffer.offer(event);
    }

    // ---- ExecutionListener typed callbacks ----

    @Override
    public void sessionStarted(ExecutionEvent event) {
        this.session = event.session();
        this.sessionStartTime = MonotonicClock.now();
        this.buildInvocation = session.buildInvocation();
        // Read configurable caps from session properties (or fall back to defaults)
        this.maxLogEventsPerScope = readIntProperty(session, Constants.MAVEN_BUILD_REPORT_MAX_LOG_EVENTS, 500);
        this.maxStacktraceLines = readIntProperty(
                session, Constants.MAVEN_BUILD_REPORT_MAX_STACKTRACE_LINES, DEFAULT_MAX_STACKTRACE_LINES);
        this.maxThrowableLines = readIntProperty(
                session,
                Constants.MAVEN_BUILD_REPORT_MAX_THROWABLE_LINES,
                BuildReportJsonWriter.DEFAULT_MAX_THROWABLE_LINES);
        this.buildLogBuffer = new ArrayBlockingQueue<>(this.maxLogEventsPerScope);
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        Session endSession = event.session();
        if (endSession == null) {
            return;
        }

        try {
            boolean hasFailure = event.exception().isPresent();
            BuildReport report = buildReport(endSession, hasFailure);
            writeReport(report, endSession);
        } catch (Exception e) {
            // Never let the report collector crash the build
            LOGGER.debug("Failed to produce build report: {}", e.getMessage(), e);
        } finally {
            clearSessionState();
        }
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        event.project().ifPresent(project -> {
            String key = projectKey(project);
            projectStartTimes.put(key, MonotonicClock.now());
            projectTraceContexts.put(key, currentTraceContext());
            mojoTimings.putIfAbsent(key, new ConcurrentLinkedQueue<>());
            subprojectLogBuffers.put(key, new ArrayBlockingQueue<>(maxLogEventsPerScope));
            currentProjectByThread.put(Thread.currentThread().getId(), key);
        });
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        event.project().ifPresent(project -> {
            String key = projectKey(project);
            Instant start = projectStartTimes.get(key);
            Instant now = MonotonicClock.now();
            projectStatuses.put(key, BuildStatus.SUCCESS);
            if (start != null) {
                projectDurations.put(key, Duration.between(start, now));
            }
        });
        currentProjectByThread.remove(Thread.currentThread().getId());
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        event.project().ifPresent(project -> {
            String key = projectKey(project);
            Instant start = projectStartTimes.get(key);
            Instant now = MonotonicClock.now();
            projectStatuses.put(key, BuildStatus.FAILURE);
            if (start != null) {
                projectDurations.put(key, Duration.between(start, now));
            }
            event.exception().ifPresent(ex -> projectFailures.put(key, ex));
        });
        currentProjectByThread.remove(Thread.currentThread().getId());
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        event.project().ifPresent(project -> {
            String key = projectKey(project);
            projectStatuses.put(key, BuildStatus.SKIPPED);
        });
        currentProjectByThread.remove(Thread.currentThread().getId());
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        event.project()
                .ifPresent(project -> event.mojoExecution().ifPresent(mojo -> {
                    String mKey = mojoKey(project, mojo);
                    mojoStartTimes.put(mKey, MonotonicClock.now());
                    mojoTraceContexts.put(mKey, currentTraceContext());

                    currentMojoByThread.put(Thread.currentThread().getId(), mKey);
                    mojoLogBuffers.put(mKey, new ArrayBlockingQueue<>(maxLogEventsPerScope));
                }));
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        onMojoFinished(event, BuildStatus.SUCCESS);
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        onMojoFinished(event, BuildStatus.FAILURE);
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        event.project()
                .ifPresent(project -> event.mojoExecution().ifPresent(mojo -> {
                    String pKey = projectKey(project);
                    Instant now = MonotonicClock.now();
                    TraceContext traceContext = TraceContext.EMPTY;

                    MojoTiming timing = new MojoTiming(
                            mojo.plugin().getModel().getGroupId(),
                            mojo.plugin().getModel().getArtifactId(),
                            mojo.plugin().getModel().getVersion(),
                            mojo.goal(),
                            mojo.executionId().orElse(null),
                            mojo.lifecyclePhase().orElse(null),
                            BuildStatus.SKIPPED,
                            now,
                            Duration.ZERO,
                            traceContext.traceId,
                            traceContext.spanId,
                            List.of());

                    mojoTimings
                            .computeIfAbsent(pKey, k -> new ConcurrentLinkedQueue<>())
                            .add(timing);
                }));
    }

    private void onMojoFinished(ExecutionEvent event, BuildStatus status) {
        event.project()
                .ifPresent(project -> event.mojoExecution().ifPresent(mojo -> {
                    String mKey = mojoKey(project, mojo);
                    String pKey = projectKey(project);

                    // Unregister the mojo from this thread
                    currentMojoByThread.remove(Thread.currentThread().getId());

                    Instant now = MonotonicClock.now();
                    Instant startInstant = mojoStartTimes.remove(mKey);
                    TraceContext traceContext = mojoTraceContexts.remove(mKey);
                    if (traceContext == null) {
                        traceContext = TraceContext.EMPTY;
                    }
                    if (startInstant == null) {
                        startInstant = now;
                    }
                    Duration duration = Duration.between(startInstant, now);

                    // Drain the log buffer for this mojo
                    BlockingQueue<LogEvent> logBuffer = mojoLogBuffers.remove(mKey);
                    List<LogEvent> output = logBuffer != null ? new ArrayList<>(logBuffer) : List.of();

                    MojoTiming timing = new MojoTiming(
                            mojo.plugin().getModel().getGroupId(),
                            mojo.plugin().getModel().getArtifactId(),
                            mojo.plugin().getModel().getVersion(),
                            mojo.goal(),
                            mojo.executionId().orElse(null),
                            mojo.lifecyclePhase().orElse(null),
                            status,
                            startInstant,
                            duration,
                            traceContext.traceId,
                            traceContext.spanId,
                            output);

                    mojoTimings
                            .computeIfAbsent(pKey, k -> new ConcurrentLinkedQueue<>())
                            .add(timing);
                }));
    }

    // ---- State management ----

    /**
     * Resets all mutable per-session fields. Called at the end of each session so that
     * when the DI container (e.g. mvnd) reuses this singleton across builds the maps do
     * not accumulate stale data from prior builds.
     */
    private void clearSessionState() {
        mojoTimings.clear();
        projectStartTimes.clear();
        projectTraceContexts.clear();
        mojoStartTimes.clear();
        mojoTraceContexts.clear();
        projectStatuses.clear();
        projectDurations.clear();
        projectFailures.clear();
        currentMojoByThread.clear();
        mojoLogBuffers.clear();
        currentProjectByThread.clear();
        subprojectLogBuffers.clear();
        buildLogBuffer.clear();
        session = null;
        buildInvocation = null;
        sessionStartTime = null;
        maxLogEventsPerScope = 500;
        maxStacktraceLines = DEFAULT_MAX_STACKTRACE_LINES;
        maxThrowableLines = BuildReportJsonWriter.DEFAULT_MAX_THROWABLE_LINES;
    }

    // ---- Report assembly ----

    BuildReport buildReport(Session endSession, boolean hasFailure) {
        Instant now = MonotonicClock.now();
        Instant startInstant = sessionStartTime != null ? sessionStartTime : now;
        Duration totalDuration = Duration.between(startInstant, now);

        BuildStatus overallStatus = hasFailure ? BuildStatus.FAILURE : BuildStatus.SUCCESS;

        // Collect subproject reports
        List<SubprojectReport> subprojectReports = new ArrayList<>();
        for (Project project : endSession.getProjects()) {
            subprojectReports.add(buildSubprojectReport(project));
        }

        // Collect failures
        List<FailureReport> failureReports = new ArrayList<>();
        for (Project project : endSession.getProjects()) {
            String key = projectKey(project);
            Throwable cause = projectFailures.get(key);
            if (cause != null || projectStatuses.get(key) == BuildStatus.FAILURE) {
                failureReports.add(buildFailureReport(project, cause));
            }
        }

        // Metadata
        String mavenVersion = endSession.getSystemProperties().getOrDefault("maven.version", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");
        List<String> goals = buildInvocation != null ? buildInvocation.goals() : List.of();
        List<Project> projects = endSession.getProjects();
        Project topProject = projects.isEmpty() ? null : projects.get(0);
        String projectId = topProject != null
                ? topProject.groupId() + ":" + topProject.artifactId() + ":" + topProject.version()
                : "unknown";
        boolean multiSubproject = projects.size() > 1;
        int threads = endSession.getDegreeOfConcurrency();

        // Build-level log events (outside any subproject lifecycle)
        List<LogEvent> buildOutput = new ArrayList<>(buildLogBuffer);

        return new DefaultBuildReport(
                buildInvocation != null ? buildInvocation : DefaultBuildInvocation.from(endSession),
                overallStatus,
                totalDuration,
                startInstant,
                mavenVersion,
                javaVersion,
                goals,
                projectId,
                multiSubproject,
                threads,
                subprojectReports,
                failureReports,
                // TODO: wire to Maven's problem collector (BuilderProblem events) in a follow-up PR
                List.of(),
                buildOutput);
    }

    private SubprojectReport buildSubprojectReport(Project project) {
        String key = projectKey(project);

        BuildStatus status = projectStatuses.getOrDefault(key, BuildStatus.SKIPPED);
        Duration duration = projectDurations.getOrDefault(key, Duration.ZERO);
        Instant subprojectStartTime =
                projectStartTimes.getOrDefault(key, sessionStartTime != null ? sessionStartTime : MonotonicClock.now());

        // Mojo reports
        Queue<MojoTiming> timings = mojoTimings.getOrDefault(key, new ConcurrentLinkedQueue<>());
        List<MojoReport> mojoReports = timings.stream()
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
                        t.traceId,
                        t.spanId,
                        t.output))
                .toList();

        // Module-level log events (between mojos)
        BlockingQueue<LogEvent> subprojectLogBuffer =
                subprojectLogBuffers.getOrDefault(key, new ArrayBlockingQueue<>(1));
        List<LogEvent> subprojectOutput = new ArrayList<>(subprojectLogBuffer);

        TraceContext traceContext = projectTraceContexts.getOrDefault(key, TraceContext.EMPTY);

        return new DefaultSubprojectReport(
                project.groupId(),
                project.artifactId(),
                project.version(),
                status,
                subprojectStartTime,
                duration,
                traceContext.traceId,
                traceContext.spanId,
                mojoReports,
                subprojectOutput);
    }

    private FailureReport buildFailureReport(Project project, Throwable cause) {
        String subproject = project.groupId() + ":" + project.artifactId() + ":" + project.version();

        // Try to find which mojo failed — walk in reverse to pick the last (proximate) failure
        Queue<MojoTiming> timings = mojoTimings.getOrDefault(projectKey(project), new ConcurrentLinkedQueue<>());
        String mojoId = timings.stream()
                .filter(t -> t.status == BuildStatus.FAILURE)
                .reduce((a, b) -> b) // last element
                .map(t -> t.artifactId + ":" + t.version + ":" + t.goal)
                .orElse(null);

        String message = cause != null ? cause.getMessage() : "Unknown error";
        String stackTrace = cause != null ? truncateStackTrace(cause) : null;

        Instant failureTimestamp = MonotonicClock.now();
        String exceptionType = cause != null ? cause.getClass().getSimpleName() : null;

        return new DefaultFailureReport(
                subproject,
                mojoId,
                failureTimestamp,
                exceptionType,
                message != null ? message : "Unknown error",
                stackTrace);
    }

    // ---- JSON persistence ----

    void writeReport(BuildReport report, Session endSession) {
        Path topDirectory = endSession.getTopDirectory();
        if (topDirectory == null) {
            LOGGER.debug("No top directory available, skipping build report");
            return;
        }

        Path reportsDir = topDirectory.resolve("target").resolve(REPORT_DIR);

        try {
            Files.createDirectories(reportsDir);

            // Timestamped file: build-report-20250729T143000Z.json
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(report.startTime());
            Path timestampedFile = reportsDir.resolve("build-report-" + timestamp + ".json");

            // Write to a temp file, then atomic-move into place so a crash
            // never leaves a half-written report on disk.
            Path tmpFile = Files.createTempFile(reportsDir, ".build-report-", ".tmp");
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8)) {
                    BuildReportJsonWriter.write(writer, report, maxThrowableLines);
                }
                atomicMove(tmpFile, timestampedFile);
            } catch (IOException e) {
                Files.deleteIfExists(tmpFile);
                throw e;
            }

            // Latest symlink (or copy on filesystems that don't support symlinks)
            Path latestFile = reportsDir.resolve(REPORT_LATEST);
            try {
                Path tmpLink = Files.createTempFile(reportsDir, ".latest-", ".tmp");
                Files.delete(tmpLink); // createTempFile creates a regular file
                Files.createSymbolicLink(tmpLink, timestampedFile.getFileName());
                atomicMove(tmpLink, latestFile);
            } catch (UnsupportedOperationException | IOException symEx) {
                // Windows or restricted filesystem - fall back to a plain copy
                Files.copy(timestampedFile, latestFile, StandardCopyOption.REPLACE_EXISTING);
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

    private static String projectKey(Project project) {
        return project.groupId() + ":" + project.artifactId();
    }

    private static String mojoKey(Project project, MojoExecution mojo) {
        return projectKey(project) + "#" + mojo.goal() + "@"
                + mojo.executionId().orElse("");
    }

    private static TraceContext currentTraceContext() {
        return new TraceContext(nonBlank(MDC.get(TRACE_ID_MDC_KEY)), nonBlank(MDC.get(SPAN_ID_MDC_KEY)));
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int readIntProperty(Session s, String key, int defaultValue) {
        String raw = s.getUserProperties().get(key);
        if (raw == null) {
            raw = s.getSystemProperties().get(key);
        }
        if (raw != null) {
            try {
                int v = Integer.parseInt(raw.trim());
                if (v > 0) {
                    return v;
                }
                LOGGER.warn("Build report property {} must be a positive integer; using default {}", key, defaultValue);
            } catch (NumberFormatException e) {
                LOGGER.warn(
                        "Build report property {} is not a valid integer ('{}'); using default {}",
                        key,
                        raw,
                        defaultValue);
            }
        }
        return defaultValue;
    }

    String truncateStackTrace(Throwable t) {
        return truncateStackTrace(t, maxStacktraceLines);
    }

    static String truncateStackTrace(Throwable t, int maxLines) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();
        String[] lines = full.split("\\r?\\n");
        if (lines.length <= maxLines) {
            // Normalise line endings (PrintWriter uses \r\n on Windows)
            return full.replace("\r\n", "\n");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append("... ").append(lines.length - maxLines).append(" more lines truncated\n");
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
            String traceId,
            String spanId,
            List<LogEvent> output) {}

    private record TraceContext(String traceId, String spanId) {
        private static final TraceContext EMPTY = new TraceContext(null, null);
    }
}
