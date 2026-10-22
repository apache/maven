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
import java.util.Objects;

import org.apache.maven.api.ExecutionEvent;
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
 * Execution event logger for the rich terminal mode ({@code --console=rich}).
 * <p>
 * In rich mode, the {@link RichBuildEventListener} manages a JLine status bar at
 * the bottom of the terminal showing live reactor progress. This logger is deliberately
 * minimal — it suppresses the verbose per-mojo and per-project banners that
 * {@link ExecutionEventLogger} produces, since the status bar replaces them.
 * <p>
 * What this logger DOES print (above the status bar):
 * <ul>
 *   <li>One line per completed module (like {@link PlainExecutionEventLogger})</li>
 *   <li>Build result summary ({@code BUILD SUCCESS/FAILURE})</li>
 *   <li>Compact module statistics and timing</li>
 *   <li>Pointer to the structured build report</li>
 * </ul>
 * <p>
 * What the status bar shows (managed by {@link RichBuildEventListener}):
 * <ul>
 *   <li>Currently building modules with active mojo name</li>
 *   <li>Reactor progress ({@code [n/total]}) and elapsed time</li>
 *   <li>Active downloads with progress</li>
 * </ul>
 *
 * @since 4.1.0
 * @see RichBuildEventListener
 * @see PlainExecutionEventLogger
 */
public class RichExecutionEventLogger extends AbstractExecutionEventLogger {

    private static final int MAX_LOG_PREFIX_SIZE = 8; // "[ERROR] "
    private static final int PROJECT_STATUS_SUFFIX_SIZE = 20; // "SUCCESS [  0.000 s]"
    private static final int MIN_TERMINAL_WIDTH = 60;
    private static final int DEFAULT_TERMINAL_WIDTH = 80;
    private static final int MAX_TERMINAL_WIDTH = 130;
    private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;

    private final MessageBuilderFactory messageBuilderFactory;
    private final Logger logger;
    private final RichBuildEventListener buildEventListener;
    private int terminalWidth;
    private int lineLength;
    private int maxProjectNameLength;
    private int totalProjects;
    private int currentVisitedProjectCount;
    private volatile Instant sessionStartTime;

    public RichExecutionEventLogger(
            MessageBuilderFactory messageBuilderFactory, RichBuildEventListener buildEventListener) {
        this(messageBuilderFactory, buildEventListener, LoggerFactory.getLogger(RichExecutionEventLogger.class));
    }

    public RichExecutionEventLogger(
            MessageBuilderFactory messageBuilderFactory, RichBuildEventListener buildEventListener, Logger logger) {
        this(messageBuilderFactory, buildEventListener, logger, -1);
    }

    public RichExecutionEventLogger(
            MessageBuilderFactory messageBuilderFactory,
            RichBuildEventListener buildEventListener,
            Logger logger,
            int terminalWidth) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.messageBuilderFactory = messageBuilderFactory;
        this.buildEventListener = Objects.requireNonNull(buildEventListener, "buildEventListener cannot be null");
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

            // Initialize the status bar
            buildEventListener.initReactor(session);
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();

            // Tear down the status bar before printing summary
            buildEventListener.tearDown();

            buildEventListener.log("");
            if (event.exception().isPresent()) {
                buildFailed = true;
            }
            logResult(event.session());
            logStats(event.session());
        }
    }

    // ---- Module lifecycle ----
    // In rich mode, per-module success lines are suppressed — the status bar already
    // shows checkmarks and the counter for every module.
    // Only FAILURE and SKIPPED scroll above the status bar since they're actionable.

    @Override
    public void projectStarted(ExecutionEvent event) {
        event.project().ifPresent(project -> {
            projectStartTimes.put(projectKey(project), MonotonicClock.now());
        });
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        passedCount.incrementAndGet();
        // Suppressed in rich mode — status bar checkmarks indicate completion
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

    // ---- Mojo lifecycle: suppressed (status bar shows active mojo) ----

    @Override
    public void mojoStarted(ExecutionEvent event) {
        buildEventListener.mojoStarted(event);
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
        // Suppressed in rich mode
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        // Suppressed in rich mode
    }

    // ---- Formatting helpers ----

    private void logProjectLine(ExecutionEvent event, String status) {
        Project project = event.project().orElse(null);
        if (project == null) {
            return;
        }

        StringBuilder buffer = new StringBuilder(128);

        // Status icon
        switch (status) {
            case "SUCCESS":
                buffer.append(" \u2713 ");
                break;
            case "FAILURE":
                buffer.append(" \u2717 ");
                break;
            default:
                buffer.append(" \u25cb ");
                break;
        }

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
        int effectiveMax = maxProjectNameLength - 3; // account for status icon
        if (buffer.length() <= effectiveMax) {
            while (buffer.length() < effectiveMax) {
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

        buildEventListener.log(mb.toString());
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

        buildEventListener.log(buffer.toString());

        // Compact stats line
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
            buildEventListener.log(stats.toString());
        }

        // Warning/error summary — warnings are suppressed inline in rich mode,
        // so the count and a command hint help the user find them.
        int warnings = buildEventListener.getWarningCount();
        int errors = buildEventListener.getErrorCount();
        if (warnings > 0 || errors > 0) {
            MessageBuilder diag = builder();
            diag.a("Diagnostics: ");
            if (warnings > 0) {
                diag.warning(warnings + " warning" + (warnings > 1 ? "s" : ""));
            }
            if (warnings > 0 && errors > 0) {
                diag.a(", ");
            }
            if (errors > 0) {
                diag.failure(errors + " error" + (errors > 1 ? "s" : ""));
            }
            diag.a(" \u2014 run ").strong("mvnlog").a(" to see details");
            buildEventListener.log(diag.toString());
        }
    }

    private void logStats(Session session) {
        Duration time =
                (sessionStartTime != null) ? Duration.between(sessionStartTime, MonotonicClock.now()) : Duration.ZERO;
        int concurrency = (session != null) ? session.getDegreeOfConcurrency() : 1;
        String wallClock = concurrency > 1 ? " (Wall Clock)" : "";
        buildEventListener.log("Total time:  " + formatDuration(time) + wallClock);

        // On failure, show Maven and Java version to help with bug reports (MNG-7372)
        if (buildFailed || failedCount.get() > 0) {
            buildEventListener.log("Maven:       " + CLIReportingUtils.showVersionMinimal());
            buildEventListener.log("Java:        " + System.getProperty("java.version", "<unknown>") + " ("
                    + System.getProperty("java.vendor", "<unknown>") + ")");
        }

        buildEventListener.log("Full report: target/build-reports/build-report-latest.json");
    }
}
