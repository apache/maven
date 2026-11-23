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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLogEvent;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ProjectBuildLogAppender}: ThreadLocal enrichment, no-copy fast path,
 * and reportCapture forwarding.
 */
class ProjectBuildLogAppenderTest {

    private List<LogEvent> received;
    private BuildEventListener listener;

    @BeforeEach
    void setUp() {
        LoggerManager.set(new DefaultLoggerManager());
        received = new ArrayList<>();
        listener = new NoopBuildEventListener() {
            @Override
            public void projectLogMessage(String projectId, LogEvent event) {
                received.add(event);
            }
        };
        ProjectBuildLogAppender.setProjectId(null);
        ProjectBuildLogAppender.setMojoId(null);
        ProjectBuildLogAppender.setReportCapture(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        LoggerManager.set(null);
        ProjectBuildLogAppender.setProjectId(null);
        ProjectBuildLogAppender.setMojoId(null);
        ProjectBuildLogAppender.setReportCapture(null);
    }

    /**
     * When {@code event.projectId()} is null, the Sink must fall back to the
     * ThreadLocal and enrich the forwarded event with the correct projectId/mojoId.
     */
    @Test
    void threadLocalFallbackEnrichesProjectId() throws Exception {
        ProjectBuildLogAppender.setProjectId("my-project");
        ProjectBuildLogAppender.setMojoId("my-mojo");

        // event carries no projectId / mojoId → must be enriched from ThreadLocal
        DefaultLogEvent raw = new DefaultLogEvent(null, LogLevel.INFO, "hello", "org.example.Foo", null);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener)) {
            LoggerManager.get().getLogSink().accept(raw);
        }

        assertEquals(1, received.size());
        LogEvent enriched = received.get(0);
        assertEquals("my-project", enriched.projectId(), "projectId must be enriched from ThreadLocal");
        assertEquals("my-mojo", enriched.mojoId(), "mojoId must be enriched from ThreadLocal");
    }

    /**
     * When the event already carries the correct projectId/mojoId and is a
     * {@link DefaultLogEvent}, the original instance must be forwarded without copying.
     */
    @Test
    void noWrapWhenProjectIdAlreadyCorrect() throws Exception {
        ProjectBuildLogAppender.setProjectId("project-a");
        ProjectBuildLogAppender.setMojoId("mojo-a");

        // Constructor: (timestamp, level, message, loggerName, throwable,
        //               sourceClassName, sourceMethodName, threadId, sequenceNumber,
        //               threadName, projectId, mojoId, keyValues, messageTemplate, messageArgs)
        DefaultLogEvent original = new DefaultLogEvent(
                null,
                LogLevel.WARN,
                "already set",
                "org.example.Bar",
                null,
                null,
                null,
                0L,
                0L,
                "main",
                "project-a", // matches ThreadLocal
                "mojo-a", // matches ThreadLocal
                null,
                null,
                null);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener)) {
            LoggerManager.get().getLogSink().accept(original);
        }

        assertEquals(1, received.size());
        assertSame(original, received.get(0), "original DefaultLogEvent must be forwarded without copying");
    }

    /**
     * {@code reportCapture} must receive a copy of every forwarded event.
     */
    @Test
    void reportCaptureReceivesForwardedEvent() throws Exception {
        List<LogEvent> captured = new ArrayList<>();
        ProjectBuildLogAppender.setReportCapture(captured::add);
        ProjectBuildLogAppender.setProjectId("proj");

        DefaultLogEvent raw = new DefaultLogEvent(null, LogLevel.DEBUG, "debug msg", "org.example.Baz", null);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener)) {
            LoggerManager.get().getLogSink().accept(raw);
        }

        assertEquals(1, received.size(), "listener must receive the event");
        assertEquals(1, captured.size(), "reportCapture must also receive the event");
        assertEquals("proj", captured.get(0).projectId(), "captured event must carry enriched projectId");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Minimal no-op implementation of {@link BuildEventListener}. */
    private abstract static class NoopBuildEventListener implements BuildEventListener {
        @Override
        public void sessionStarted(org.apache.maven.execution.ExecutionEvent event) {}

        @Override
        public void projectStarted(String projectId) {}

        @Override
        public void projectFinished(String projectId) {}

        @Override
        public void executionFailure(String projectId, boolean halted, String exception) {}

        @Override
        public void mojoStarted(org.apache.maven.execution.ExecutionEvent event) {}

        @Override
        public void finish(int exitCode) {}

        @Override
        public void fail(Throwable t) {}

        @Override
        public void log(String msg) {}

        @Override
        public void transfer(String projectId, org.eclipse.aether.transfer.TransferEvent e) {}
    }
}
