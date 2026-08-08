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
import java.util.List;
import java.util.Objects;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
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
public class PlainExecutionEventLogger extends AbstractExecutionListener {

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
    private volatile int currentVisitedProjectCount;

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
            List<MavenProject> projects = event.getSession().getProjects();
            List<MavenProject> allProjects = event.getSession().getAllProjects();

            currentVisitedProjectCount = allProjects.size() - projects.size();
            totalProjects = allProjects.size();
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            logger.info("");
            logResult(event.getSession());
            logStats(event.getSession());
        }
    }

    // ---- Module lifecycle: one line per completed module ----

    @Override
    public void projectStarted(ExecutionEvent event) {
        // In plain mode, we only log when a project finishes (succeeded/failed/skipped)
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            logProjectLine(event, "SUCCESS");
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
            logProjectLine(event, "FAILURE");
        }
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        if (logger.isInfoEnabled()) {
            init();
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
            logger.warn(
                    "Goal '{}' requires online mode for execution but Maven is currently offline, skipping",
                    event.getMojoExecution().getGoal());
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

    private void logProjectLine(ExecutionEvent event, String status) {
        MavenProject project = event.getProject();
        MavenSession session = event.getSession();
        MavenExecutionResult result = session.getResult();
        BuildSummary buildSummary = result.getBuildSummary(project);

        StringBuilder buffer = new StringBuilder(128);
        buffer.append(project.getName());
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
        if (buildSummary != null) {
            mb.a(" [");
            String duration = formatDuration(buildSummary.getExecTime());
            int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - duration.length();
            if (padSize > 0) {
                mb.a(chars(' ', padSize));
            }
            mb.a(duration);
            mb.a(']');
        }

        logger.info(mb.toString());
    }

    private void logResult(MavenSession session) {
        MessageBuilder buffer = builder();
        if (session.getResult().hasExceptions()) {
            buffer.failure("BUILD FAILURE");
        } else {
            buffer.success("BUILD SUCCESS");
        }

        int passed = 0;
        int failed = 0;
        int skipped = 0;
        for (MavenProject project : session.getProjects()) {
            BuildSummary summary = session.getResult().getBuildSummary(project);
            if (summary instanceof BuildSuccess) {
                passed++;
            } else if (summary instanceof BuildFailure) {
                failed++;
            } else {
                skipped++;
            }
        }

        logger.info(buffer.toString());

        // Compact stats line: "12 modules | 11 passed | 1 failed | 0 skipped"
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

    private void logStats(MavenSession session) {
        Duration time = Duration.between(session.getRequest().getStartInstant(), MonotonicClock.now());
        String wallClock = session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";
        logger.info("Total time:  {}{}", formatDuration(time), wallClock);

        // On failure, show Maven and Java version to help with bug reports (MNG-7372)
        if (session.getResult().hasExceptions()) {
            logger.info("Maven:       {}", CLIReportingUtils.showVersionMinimal());
            logger.info(
                    "Java:        {} ({})",
                    System.getProperty("java.version", "<unknown>"),
                    System.getProperty("java.vendor", "<unknown>"));
        }

        logger.info("Full report: target/build-reports/build-report-latest.json");
    }
}
