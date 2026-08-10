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
package org.apache.maven.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.internal.build.DefaultLogEvent;
import org.apache.maven.internal.impl.DefaultLog;
import org.apache.maven.slf4j.MavenJulHandler;
import org.apache.maven.slf4j.MavenSimpleLogger;
import org.slf4j.MDC;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Forwards log messages to the client as structured {@link LogEvent} objects.
 * <p>
 * Installs itself as a {@link MavenSimpleLogger.LogSink} to intercept all
 * SLF4J log output, enrich it with structured metadata (level, logger name,
 * clean message, formatted output), and forward to the active
 * {@link BuildEventListener}.
 */
public class ProjectBuildLogAppender implements AutoCloseable {

    private static final String KEY_PROJECT_ID = "maven.project.id";
    private static final String KEY_MOJO_ID = "maven.mojo.id";
    private static final ThreadLocal<String> PROJECT_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> MOJO_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> FORKING_PROJECT_ID = new InheritableThreadLocal<>();

    public static String getProjectId() {
        return PROJECT_ID.get();
    }

    public static void setProjectId(String projectId) {
        String forkingProjectId = FORKING_PROJECT_ID.get();
        if (forkingProjectId != null) {
            if (projectId != null) {
                projectId = forkingProjectId + "/" + projectId;
            } else {
                projectId = forkingProjectId;
            }
        }
        if (projectId != null) {
            PROJECT_ID.set(projectId);
            MDC.put(KEY_PROJECT_ID, projectId);
        } else {
            PROJECT_ID.remove();
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    public static String getMojoId() {
        return MOJO_ID.get();
    }

    /**
     * Sets or clears the mojo execution identifier in both the thread-local
     * and the SLF4J MDC.  The value is available to any SLF4J appender via
     * the MDC key {@code maven.mojo.id} and to JUL-bridged messages through
     * the same MDC path.
     * <p>
     * Format: {@code "prefix:goal@executionId"}
     * (e.g. {@code "compiler:compile@default-compile"}).
     *
     * @param mojoId the mojo identifier, or {@code null} to clear
     */
    public static void setMojoId(String mojoId) {
        if (mojoId != null) {
            MOJO_ID.set(mojoId);
            MDC.put(KEY_MOJO_ID, mojoId);
        } else {
            MOJO_ID.remove();
            MDC.remove(KEY_MOJO_ID);
        }
    }

    public static void setForkingProjectId(String forkingProjectId) {
        if (forkingProjectId != null) {
            FORKING_PROJECT_ID.set(forkingProjectId);
        } else {
            FORKING_PROJECT_ID.remove();
        }
    }

    public static void updateMdc() {
        String id = getProjectId();
        if (id != null) {
            MDC.put(KEY_PROJECT_ID, id);
        } else {
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    /**
     * Global sequence counter for total ordering of log events across
     * all sources (Log API, JUL, direct SLF4J).  Incremented atomically
     * in {@link #accept} which is called synchronously on the logging thread.
     */
    private static final AtomicLong SEQUENCE = new AtomicLong();

    /**
     * Callback for build report log capture. Receives the fully-formed
     * {@link LogEvent} produced by {@link #accept}, eliminating the need
     * for a second capture pipeline in {@code MavenSimpleLogger}.
     * <p>
     * Set by {@code BuildReportCollector} at session start, cleared at
     * session end. The callback runs synchronously on the logging thread.
     */
    private static volatile Consumer<LogEvent> reportCapture;

    /**
     * Sets the report capture callback.
     *
     * @param capture the callback, or {@code null} to remove
     */
    public static void setReportCapture(Consumer<LogEvent> capture) {
        ProjectBuildLogAppender.reportCapture = capture;
    }

    private final BuildEventListener buildEventListener;

    public ProjectBuildLogAppender(BuildEventListener buildEventListener) {
        this.buildEventListener = buildEventListener;
        MavenSimpleLogger.setLogSink(this::accept);
    }

    protected void accept(
            int level, String loggerName, String cleanMessage, String formattedMessage, Throwable throwable) {
        String projectId = MDC.get(KEY_PROJECT_ID);
        Instant timestamp = MonotonicClock.now();
        LogLevel logLevel = toLogLevel(level);
        String stackTrace = throwable != null ? formatStackTrace(throwable) : null;

        long seq = SEQUENCE.getAndIncrement();

        // Read source metadata: JUL events carry it via MavenJulHandler,
        // Log API events carry it via DefaultLog's ThreadLocal.
        MavenJulHandler.JulMetadata julMeta = MavenJulHandler.getJulMetadata();
        DefaultLog.LogApiMetadata logApiMeta = DefaultLog.getLogApiMetadata();
        String sourceClassName;
        String sourceMethodName;
        long threadId;
        if (julMeta != null) {
            sourceClassName = julMeta.sourceClassName();
            sourceMethodName = julMeta.sourceMethodName();
            threadId = julMeta.threadId();
        } else if (logApiMeta != null) {
            sourceClassName = logApiMeta.sourceClassName();
            sourceMethodName = logApiMeta.sourceMethodName();
            threadId = logApiMeta.threadId();
        } else {
            sourceClassName = null;
            sourceMethodName = null;
            threadId = -1;
        }
        LogEvent event = new DefaultLogEvent(
                timestamp,
                logLevel,
                cleanMessage,
                loggerName,
                stackTrace,
                formattedMessage,
                sourceClassName,
                sourceMethodName,
                threadId,
                seq);
        buildEventListener.projectLogMessage(projectId, event);

        // Forward to build report collector (if active)
        Consumer<LogEvent> capture = reportCapture;
        if (capture != null) {
            capture.accept(event);
        }
    }

    @Override
    public void close() throws Exception {
        MavenSimpleLogger.setLogSink(null);
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

    private static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
