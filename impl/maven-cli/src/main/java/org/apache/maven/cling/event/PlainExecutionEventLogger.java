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
package org.apache.maven.cling.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.cling.utils.CLIReportingUtils.formatDuration;

/**
 * Compact execution event logger for CI and batch environments.
 * <p>
 * Produces one line per completed module instead of the verbose per-mojo
 * output of {@link ExecutionEventLogger}. Designed for CI log viewers
 * and LLM-based tools where signal density matters more than verbosity.
 * <p>
 * Example output:
 * <pre>
 * [INFO] maven-api-core ................................ SUCCESS [  2.1s]
 * [INFO] maven-core ..................................... FAILURE [  5.3s]
 * [INFO]
 * [INFO] BUILD FAILURE
 * [INFO] Total time:  32.1s
 * </pre>
 *
 * Selected via {@code --console=plain} or automatically in CI environments.
 *
 * @since 4.1.0
 * @see ExecutionEventLogger
 */
public class PlainExecutionEventLogger implements ExecutionListener {

    private static final int MAX_LOG_PREFIX_SIZE = 8; // "[ERROR] "
    private static final int PROJECT_STATUS_SUFFIX_SIZE = 20; // "SUCCESS [  0.000 s]"
    private static final int MIN_TERMINAL_WIDTH = 60;
    private static final int DEFAULT_TERMINAL_WIDTH = 80;
    private static final int MAX_TERMINAL_WIDTH = 130;
    private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;

    private final MessageBuilderFactory messageBuilderFactory;
    private final Logger logger;
    private int terminalWidth;
    private int lineLength;
    private int maxProjectNameLength;
    private int totalProjects;
    private int currentVisitedProjectCount;

    // Track project durations and build results natively
    private final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Duration> projectDurations = new ConcurrentHashMap<>();
    private final AtomicInteger passedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private volatile Instant sessionStartTime;
    private volatile boolean buildFailed;

    public void recordProjectDuration(String key, Duration duration) {
        if (key != null && duration != null) {
            projectDurations.put(key, duration);
        }
    }

    public void syncResult(boolean hasExceptions, int passed, int failed, int skipped) {
        if (hasExceptions) {
            buildFailed = true;
        }
        if (passedCount.get() == 0 && failedCount.get() == 0 && skippedCount.get() == 0) {
            passedCount.set(passed);
            failedCount.set(failed);
            skippedCount.set(skipped);
        }
    }

    public PlainExecutionEventLogger(MessageBuilderFactory messageBuilderFactory) {
        this(messageBuilderFactory, LoggerFactory.getLogger(PlainExecutionEventLogger.class));
    }

    public PlainExecutionEventLogger(MessageBuilderFactory messageBuilderFactory, Logger logger) {
        this(messageBuilderFactory, logger, -1);
    }

    public PlainExecutionEventLogger(MessageBuilderFactory messageBuilderFactory, Logger logger, int terminalWidth) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.messageBuilderFactory = messageBuilderFactory;
        this.terminalWidth = terminalWidth;
    }

    private void init() {
        if (maxProjectNameLength == 0) {
            if (terminalWidth < 0) {
                terminalWidth = messageBuilderFactory.getTerminalWidth();
            }
            terminalWidth = Math.min(
                    MAX_TERMINAL_WIDTH,
                    Math.max(terminalWidth <= 0 ? DEFAULT_TERMINAL_WIDTH : terminalWidth, MIN_TERMINAL_WIDTH));
            lineLength = terminalWidth - MAX_LOG_PREFIX_SIZE;
            maxProjectNameLength = lineLength - PROJECT_STATUS_SUFFIX_SIZE;
        }
    }

    private MessageBuilder builder() {
        return messageBuilderFactory.builder();
    }

    private static String chars(char c, int count) {
        return String.valueOf(c).repeat(Math.max(0, count));
    }

    private void infoMain(String msg) {
        logger.info(builder().strong(msg).toString());
    }

    // ---- Session lifecycle ----

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            logger.info("Scanning for projects...");
        }
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            sessionStartTime = MonotonicClock.now();
            Session session = event.session();
            List<Project> allProjects = getAllProjects(session);
            List<Project> projects =
                    (session != null && session.getProjects() != null) ? session.getProjects() : List.of();
            if (allProjects != null && !allProjects.isEmpty()) {
                totalProjects = allProjects.size();
                currentVisitedProjectCount = allProjects.size() - projects.size();
            } else {
                totalProjects = projects.size();
                currentVisitedProjectCount = 0;
            }
        }
    }

    private static List<Project> getAllProjects(Session session) {
        if (session == null) {
            return null;
        }
        try {
            java.lang.reflect.Method m = session.getClass().getMethod("getAllProjects");
            Object res = m.invoke(session);
            if (res instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Project> projs = (List<Project>) list;
                return projs;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            if (event.exception().isPresent()) {
                buildFailed = true;
            }
            logger.info("");
            logResult(event.session());
            logStats(event.session());
        }
    }

    // ---- Module lifecycle: one line per completed module ----

    @Override
    public void projectStarted(ExecutionEvent event) {
        event.project().ifPresent(project -> {
            projectStartTimes.put(projectKey(project), MonotonicClock.now());
        });
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            passedCount.incrementAndGet();
            logProjectLine(event, "SUCCESS");
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            failedCount.incrementAndGet();
            buildFailed = true;
            logProjectLine(event, "FAILURE");
        }
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            skippedCount.incrementAndGet();
            logProjectLine(event, "SKIPPED");
        }
    }

    // ---- Mojo lifecycle: suppressed in plain mode ----

    @Override
    public void mojoStarted(ExecutionEvent event) {
        // Suppressed in plain mode — plugin execution details go to build report
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        if (logger.isWarnEnabled()) {
            String goal = event.mojoExecution().map(MojoExecution::goal).orElse("unknown");
            logger.warn("Goal '{}' requires online mode for execution but Maven is currently offline, skipping", goal);
        }
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        // Suppressed in plain mode
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        // Suppressed in plain mode
    }

    // ---- Formatting helpers ----

    private static String projectKey(Project project) {
        String gid = project.groupId();
        String aid = project.artifactId();
        return (gid != null ? gid : "") + ":" + (aid != null ? aid : "");
    }

    private static String getProjectDisplayName(Project project) {
        if (project == null) {
            return "";
        }
        String name = null;
        try {
            if (project.model() != null) {
                name = project.model().getName();
            }
        } catch (Exception ignored) {
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return project.artifactId() != null ? project.artifactId() : "";
    }

    private Duration getProjectDuration(Project project) {
        if (project == null) {
            return null;
        }
        Duration recorded = projectDurations.remove(projectKey(project));
        if (recorded != null) {
            return recorded;
        }
        Instant start = projectStartTimes.remove(projectKey(project));
        return (start != null) ? Duration.between(start, MonotonicClock.now()) : null;
    }

    private void logProjectLine(ExecutionEvent event, String status) {
        Project project = event.project().orElse(null);
        if (project == null) {
            return;
        }

        StringBuilder buffer = new StringBuilder(128);
        buffer.append(getProjectDisplayName(project));
        buffer.append(' ');

        if (totalProjects > 1) {
            int number;
            synchronized (this) {
                number = ++currentVisitedProjectCount;
            }
            String progress = "[" + number + "/" + totalProjects + "]";
            buffer.append(progress);
            buffer.append(' ');
        }

        // Pad with dots to align status
        if (buffer.length() <= maxProjectNameLength) {
            while (buffer.length() < maxProjectNameLength) {
                buffer.append('.');
            }
            buffer.append(' ');
        }

        // Status with color
        MessageBuilder mb = builder();
        mb.a(buffer);
        switch (status) {
            case "SUCCESS":
                mb.success(status);
                break;
            case "FAILURE":
                mb.failure(status);
                break;
            default:
                mb.warning(status);
                break;
        }

        // Duration
        Duration execDuration = getProjectDuration(project);
        if (execDuration != null) {
            mb.a(" [");
            String duration = formatDuration(execDuration);
            int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - duration.length();
            if (padSize > 0) {
                mb.a(chars(' ', padSize));
            }
            mb.a(duration);
            mb.a(']');
        }

        logger.info(mb.toString());
    }

    private void logResult(Session session) {
        MessageBuilder buffer = builder();
        boolean hasExceptions = buildFailed || failedCount.get() > 0;
        if (hasExceptions) {
            buffer.failure("BUILD FAILURE");
        } else {
            buffer.success("BUILD SUCCESS");
        }

        int passed = passedCount.get();
        int failed = failedCount.get();
        int skipped = skippedCount.get();

        if (hasExceptions) {
            logger.error(buffer.toString());
        } else {
            logger.info(buffer.toString());
        }
        if (totalProjects > 1) {
            StringBuilder stats = new StringBuilder();
            stats.append(totalProjects).append(" modules");
            stats.append(" | ").append(passed).append(" passed");
            if (failed > 0) {
                stats.append(" | ").append(failed).append(" failed");
            }
            if (skipped > 0) {
                stats.append(" | ").append(skipped).append(" skipped");
            }
            logger.info(stats.toString());
        }
    }

    private void logStats(Session session) {
        Duration time =
                (sessionStartTime != null) ? Duration.between(sessionStartTime, MonotonicClock.now()) : Duration.ZERO;
        int concurrency = (session != null) ? session.getDegreeOfConcurrency() : 1;
        String wallClock = concurrency > 1 ? " (Wall Clock)" : "";
        logger.info("Total time:  {}{}", formatDuration(time), wallClock);

        // On failure, show Maven and Java version to help with bug reports (MNG-7372)
        if (buildFailed || failedCount.get() > 0) {
            logger.info("Maven:       {}", CLIReportingUtils.showVersionMinimal());
            logger.info(
                    "Java:        {} ({})",
                    System.getProperty("java.version", "<unknown>"),
                    System.getProperty("java.vendor", "<unknown>"));
        }

        logger.info("Full report: target/build-reports/build-report-latest.json");
    }
}
