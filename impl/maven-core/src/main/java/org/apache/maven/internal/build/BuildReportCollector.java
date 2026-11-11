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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.api.BuildEnvironment;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.build.report.MojoReport;
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
import org.slf4j.MDC;

/**
 * Collects build lifecycle events and produces a structured {@link BuildReport}
 * at the end of the session.
 *
 * @since 4.1.0
 */
@Singleton
@Named
public final class BuildReportCollector extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildReportCollector.class);

    static final String REPORT_DIR = "build-reports";
    static final String REPORT_LATEST = "build-report-latest.json";
    static final String TRACE_ID_MDC_KEY = "otel.trace_id";
    static final String SPAN_ID_MDC_KEY = "otel.span_id";

    private static final int MAX_STACKTRACE_LINES = 30;

    static final int MAX_LOG_EVENTS_PER_SCOPE = 500;

    private final Map<String, List<MojoTiming>> mojoTimings = new ConcurrentHashMap<>();
    private final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();
    private final Map<String, TraceContext> projectTraceContexts = new ConcurrentHashMap<>();
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();
    private final Map<String, TraceContext> mojoTraceContexts = new ConcurrentHashMap<>();

    private volatile MavenSession session;
    private volatile BuildEnvironment buildEnvironment;

    /** JVM shutdown hook used to persist a partial build report. */
    private volatile Thread shutdownHook;

    /** Ensures that normal completion and shutdown do not write competing reports. */
    private final AtomicBoolean reportWritten = new AtomicBoolean();

    private final Map<Long, String> currentMojoByThread = new ConcurrentHashMap<>();
    private final Map<String, List<LogEvent>> mojoLogBuffers = new ConcurrentHashMap<>();
    private final Map<Long, String> currentProjectByThread = new ConcurrentHashMap<>();
    private final Map<String, List<LogEvent>> moduleLogBuffers = new ConcurrentHashMap<>();
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
                case MojoSkipped:
                    onMojoSkipped(executionEvent);
                    break;
                default:
                    break;
            }
        }
    }

    // ---- Event handlers ----

    private void onSessionStarted(ExecutionEvent event) {
        this.session = event.getSession();
        this.buildEnvironment = DefaultBuildEnvironment.from(this.session);
        this.reportWritten.set(false);

        Thread hook = new Thread(
                () -> {
                    MavenSession activeSession = this.session;

                    if (activeSession == null || !reportWritten.compareAndSet(false, true)) {
                        return;
                    }

                    try {
                        LOGGER.debug("JVM shutting down: writing partial build report");
                        BuildReport report = buildReport(activeSession);
                        writeReport(report, activeSession, true);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to write partial build report during shutdown", e);
                    }
                },
                "maven-build-report-shutdown-hook");

        try {
            Runtime.getRuntime().addShutdownHook(hook);
            this.shutdownHook = hook;
        } catch (IllegalStateException e) {
            LOGGER.debug("JVM shutdown already in progress", e);
        }

        installLogCapture();
    }

    private void onSessionEnded(ExecutionEvent event) {
        removeLogCapture();

        Thread hook = shutdownHook;
        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException e) {
                LOGGER.debug("JVM shutdown already in progress", e);
            } catch (SecurityException e) {
                LOGGER.debug("Unable to remove build report shutdown hook", e);
            } finally {
                shutdownHook = null;
            }
        }

        MavenSession endSession = event.getSession();

        if (endSession == null || !reportWritten.compareAndSet(false, true)) {
            return;
        }

        try {
            BuildReport report = buildReport(endSession);
            writeReport(report, endSession);
        } catch (Exception e) {
            LOGGER.debug("Failed to produce build report: {}", e.getMessage(), e);
        }
    }

    // ---- Event handlers ----

    private void onProjectStarted(ExecutionEvent event) {
        String key = projectKey(event.getProject());
        projectStartTimes.put(key, MonotonicClock.now());
        projectTraceContexts.put(key, currentTraceContext());
        mojoTimings.putIfAbsent(key, Collections.synchronizedList(new ArrayList<>()));
        moduleLogBuffers.put(key, Collections.synchronizedList(new ArrayList<>()));
        currentProjectByThread.put(Thread.currentThread().getId(), key);
    }

    private void onProjectFinished(ExecutionEvent event) {
        currentProjectByThread.remove(Thread.currentThread().getId());
    }

    private void onMojoStarted(ExecutionEvent event) {
        String mKey = mojoKey(event.getProject(), event.getMojoExecution());
        mojoStartTimes.put(mKey, MonotonicClock.now());
        mojoTraceContexts.put(mKey, currentTraceContext());

        currentMojoByThread.put(Thread.currentThread().getId(), mKey);
        mojoLogBuffers.put(mKey, Collections.synchronizedList(new ArrayList<>()));
    }

    private void onMojoFinished(ExecutionEvent event) {
        MojoExecution mojo = event.getMojoExecution();
        MavenProject project = event.getProject();
        String mKey = mojoKey(project, mojo);
        String pKey = projectKey(project);

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

        BuildStatus status =
                event.getType() == ExecutionEvent.Type.MojoSucceeded ? BuildStatus.SUCCESS : BuildStatus.FAILURE;

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
                traceContext.traceId,
                traceContext.spanId,
                output);

        mojoTimings
                .computeIfAbsent(pKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(timing);
    }

    private void onMojoSkipped(ExecutionEvent event) {
        MojoExecution mojo = event.getMojoExecution();
        MavenProject project = event.getProject();
        String pKey = projectKey(project);

        Instant now = MonotonicClock.now();
        TraceContext traceContext = TraceContext.EMPTY;

        MojoTiming timing = new MojoTiming(
                mojo.getGroupId(),
                mojo.getArtifactId(),
                mojo.getVersion(),
                mojo.getGoal(),
                mojo.getExecutionId(),
                mojo.getLifecyclePhase(),
                BuildStatus.SKIPPED,
                now,
                Duration.ZERO,
                traceContext.traceId,
                traceContext.spanId,
                List.of());

        mojoTimings
                .computeIfAbsent(pKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(timing);
    }

    // ---- Structured log capture ----

    /**
     * Registers a callback on ProjectBuildLogAppender to receive the
     * already-formed LogEvent objects from the main logging pipeline.
     */
    private void installLogCapture() {
        ProjectBuildLogAppender.setReportCapture(this::captureLogEvent);
    }

    private void removeLogCapture() {
        ProjectBuildLogAppender.setReportCapture(null);
    }

    /**
     * Routes a LogEvent to the appropriate buffer based on the
     * current thread's lifecycle context.
     */
    private void captureLogEvent(LogEvent event) {
        long threadId = Thread.currentThread().getId();

        // Mojo-level output
        String mKey = currentMojoByThread.get(threadId);
        if (mKey != null) {
            List<LogEvent> buffer = mojoLogBuffers.get(mKey);
            if (buffer != null && buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buffer.add(event);
            }
            return;
        }

        // Module-level output
        String pKey = currentProjectByThread.get(threadId);
        if (pKey != null) {
            List<LogEvent> buffer = moduleLogBuffers.get(pKey);
            if (buffer != null && buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buffer.add(event);
            }
            return;
        }

        // Build-level output
        synchronized (buildLogBuffer) {
            if (buildLogBuffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buildLogBuffer.add(event);
            }
        }
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

        List<ModuleReport> moduleReports = new ArrayList<>();

        for (MavenProject project : endSession.getProjects()) {
            moduleReports.add(buildModuleReport(project, endSession));
        }

        List<FailureReport> failureReports = new ArrayList<>();

        if (result != null) {
            for (MavenProject project : endSession.getProjects()) {
                BuildSummary summary = result.getBuildSummary(project);

                if (summary instanceof BuildFailure buildFailure) {
                    failureReports.add(buildFailureReport(project, buildFailure));
                }
            }
        }

        String mavenVersion = endSession.getSystemProperties().getProperty("maven.version", "unknown");

        String javaVersion = System.getProperty("java.version", "unknown");
        List<String> goals = endSession.getGoals();

        MavenProject topProject = endSession.getTopLevelProject();

        String projectId = topProject != null
                ? topProject.getGroupId() + ":" + topProject.getArtifactId() + ":" + topProject.getVersion()
                : "unknown";

        boolean multiModule = endSession.getProjects().size() > 1;
        int threads = endSession.getRequest().getDegreeOfConcurrency();

        List<LogEvent> buildOutput;

        synchronized (buildLogBuffer) {
            buildOutput = List.copyOf(buildLogBuffer);
        }

        return new DefaultBuildReport(
                buildEnvironment != null ? buildEnvironment : DefaultBuildEnvironment.from(endSession),
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
                duration = summary.getExecTime();
            } else {
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
                            t.traceId,
                            t.spanId,
                            t.output))
                    .toList();
        }

        // Module-level log events
        List<LogEvent> moduleLogBuffer = moduleLogBuffers.getOrDefault(key, Collections.emptyList());

        List<LogEvent> moduleOutput;

        synchronized (moduleLogBuffer) {
            moduleOutput = List.copyOf(moduleLogBuffer);
        }

        TraceContext traceContext = projectTraceContexts.getOrDefault(key, TraceContext.EMPTY);

        return new DefaultModuleReport(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                status,
                moduleStartTime,
                duration,
                traceContext.traceId,
                traceContext.spanId,
                mojoReports,
                moduleOutput);
    }

    private FailureReport buildFailureReport(MavenProject project, BuildFailure buildFailure) {

        String module = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();

        String mojoId = null;

        List<MojoTiming> timings = mojoTimings.getOrDefault(projectKey(project), Collections.emptyList());

        synchronized (timings) {
            for (MojoTiming timing : timings) {
                if (timing.status == BuildStatus.FAILURE) {
                    mojoId = timing.artifactId + ":" + timing.version + ":" + timing.goal;
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
        writeReport(report, endSession, false);
    }

    void writeReport(BuildReport report, MavenSession endSession, boolean partial) {

        Path topDirectory = endSession.getTopDirectory();

        if (topDirectory == null) {
            LOGGER.debug("No top directory available, skipping build report");
            return;
        }

        Path reportsDir = topDirectory.resolve("target").resolve(REPORT_DIR);

        try {
            Files.createDirectories(reportsDir);

            String json = BuildReportJsonWriter.toJson(report);

            // Mark reports written during shutdown as partial.
            if (partial) {
                json = addPartialFlag(json);
            }

            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(report.startTime());

            Path timestampedFile = reportsDir.resolve("build-report-" + timestamp + ".json");

            Path tmpFile = Files.createTempFile(reportsDir, ".build-report-", ".tmp");

            try {
                Files.writeString(tmpFile, json);
                atomicMove(tmpFile, timestampedFile);
            } catch (IOException e) {
                Files.deleteIfExists(tmpFile);
                throw e;
            }

            // Latest symlink, or copy on filesystems that do not support symlinks.
            Path latestFile = reportsDir.resolve(REPORT_LATEST);

            try {
                Path tmpLink = Files.createTempFile(reportsDir, ".latest-", ".tmp");

                Files.delete(tmpLink);
                Files.createSymbolicLink(tmpLink, timestampedFile.getFileName());

                atomicMove(tmpLink, latestFile);
            } catch (UnsupportedOperationException | IOException symEx) {
                Files.writeString(latestFile, json);
            }

            LOGGER.debug("Build report written to {}", timestampedFile);

        } catch (IOException e) {
            LOGGER.warn("Failed to write build report to {}: {}", reportsDir, e.getMessage());
        }
    }

    /**
     * Adds the partial flag to the root JSON object.
     */
    private static String addPartialFlag(String json) {
        int lastBrace = json.lastIndexOf('}');

        if (lastBrace < 0) {
            return json;
        }

        String beforeBrace = json.substring(0, lastBrace).stripTrailing();

        if (beforeBrace.endsWith("{")) {
            return beforeBrace + "\"partial\":true}";
        }

        return beforeBrace + ",\"partial\":true}";
    }

    /**
     * Attempts an atomic move; falls back to a regular move when
     * the filesystem does not support ATOMIC_MOVE.
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

    private static TraceContext currentTraceContext() {
        return new TraceContext(nonBlank(MDC.get(TRACE_ID_MDC_KEY)), nonBlank(MDC.get(SPAN_ID_MDC_KEY)));
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
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
            String traceId,
            String spanId,
            List<LogEvent> output) {}

    private record TraceContext(String traceId, String spanId) {
        private static final TraceContext EMPTY = new TraceContext(null, null);
    }
}
