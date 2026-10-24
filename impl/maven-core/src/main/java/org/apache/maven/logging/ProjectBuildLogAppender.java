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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLogEvent;
import org.apache.maven.logging.impl.DefaultLoggerManager;

/**
 * Forwards log messages to the client as structured {@link LogEvent} objects.
 * <p>
 * Installs itself as a {@link LogSink} on the {@link LoggerManager}
 * to intercept all log output, enrich it with structured metadata (level, logger name,
 * clean message, formatted output), and forward to the active
 * {@link BuildEventListener}.
 * <p>
 * Enriched events are enqueued into a lock-free {@link ConcurrentLinkedQueue}
 * and drained by a single thread at a time, so that formatting and I/O
 * happen off the logging thread's hot path. This eliminates contention
 * when multiple threads log concurrently during parallel builds.
 */
public class ProjectBuildLogAppender implements AutoCloseable {

    private static final ThreadLocal<String> PROJECT_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> MOJO_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> FORKING_PROJECT_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> FORKING_MOJO_ID = new InheritableThreadLocal<>();

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
        } else {
            PROJECT_ID.remove();
        }
    }

    public static String getMojoId() {
        return MOJO_ID.get();
    }

    /**
     * Sets or clears the mojo execution identifier in the thread-local context.
     * When clearing ({@code null}), if a forking mojo ID was saved, it is restored.
     *
     * @param mojoId the mojo identifier, or {@code null} to clear
     */
    public static void setMojoId(String mojoId) {
        if (mojoId != null) {
            MOJO_ID.set(mojoId);
        } else {
            String forkingMojoId = FORKING_MOJO_ID.get();
            if (forkingMojoId != null) {
                MOJO_ID.set(forkingMojoId);
            } else {
                MOJO_ID.remove();
            }
        }
    }

    public static void setForkingProjectId(String forkingProjectId) {
        if (forkingProjectId != null) {
            FORKING_PROJECT_ID.set(forkingProjectId);
        } else {
            FORKING_PROJECT_ID.remove();
        }
    }

    /**
     * Saves or clears the mojo ID of the forking mojo, so it can be
     * restored when the fork completes.
     *
     * @param forkingMojoId the forking mojo identifier, or {@code null} to clear
     */
    public static void setForkingMojoId(String forkingMojoId) {
        if (forkingMojoId != null) {
            FORKING_MOJO_ID.set(forkingMojoId);
        } else {
            FORKING_MOJO_ID.remove();
        }
    }

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private static volatile Consumer<LogEvent> reportCapture;

    public static void setReportCapture(Consumer<LogEvent> capture) {
        ProjectBuildLogAppender.reportCapture = capture;
    }

    private final BuildEventListener buildEventListener;
    private final ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock drainLock = new ReentrantLock();
    private volatile boolean needsMetadata;

    public ProjectBuildLogAppender(BuildEventListener buildEventListener) {
        this.buildEventListener = buildEventListener;
        LoggerManager mgr = LoggerManager.get();
        if (mgr == null) {
            new DefaultLoggerManager();
            mgr = LoggerManager.get();
        }
        mgr.setLogSink(new Sink());
    }

    public void setNeedsMetadata(boolean needsMetadata) {
        this.needsMetadata = needsMetadata;
    }

    private class Sink implements LogSink {
        @Override
        public void accept(LogEvent event) {
            final LogEvent full;
            String projectId = event.projectId() != null ? event.projectId() : getProjectId();
            String mojoId = event.mojoId() != null ? event.mojoId() : getMojoId();
            if (event instanceof DefaultLogEvent && projectId == event.projectId() && mojoId == event.mojoId()) {
                full = event;
            } else {
                String stackTrace = event.throwable() != null ? formatStackTrace(event.throwable()) : null;
                full = new DefaultLogEvent(
                        event.timestamp() != null ? event.timestamp() : MonotonicClock.now(),
                        event.level(),
                        event.message(),
                        event.loggerName(),
                        stackTrace,
                        event.throwable(),
                        event.sourceClassName(),
                        event.sourceMethodName(),
                        event.threadId(),
                        SEQUENCE.getAndIncrement(),
                        event.threadName(),
                        projectId,
                        mojoId,
                        event.keyValues(),
                        event.messageTemplate(),
                        event.messageArgs());
            }

            queue.add(full);
            tryDrain();
        }

        @Override
        public boolean needsMetadata() {
            return needsMetadata || reportCapture != null;
        }
    }

    private void tryDrain() {
        if (drainLock.tryLock()) {
            try {
                drain();
            } finally {
                drainLock.unlock();
            }
            if (!queue.isEmpty() && drainLock.tryLock()) {
                try {
                    drain();
                } finally {
                    drainLock.unlock();
                }
            }
        }
    }

    private void drain() {
        LogEvent event;
        while ((event = queue.poll()) != null) {
            buildEventListener.projectLogMessage(event.projectId(), event);

            Consumer<LogEvent> capture = reportCapture;
            if (capture != null) {
                capture.accept(event);
            }
        }
    }

    @Override
    public void close() throws Exception {
        drainLock.lock();
        try {
            drain();
        } finally {
            drainLock.unlock();
        }
        LoggerManager mgr = LoggerManager.get();
        if (mgr != null) {
            mgr.setLogSink(null);
        }
    }

    private static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
