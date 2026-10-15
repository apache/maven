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
package org.apache.maven.logging.slf4j;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSimpleLoggerTest {

    boolean colorEnabled;

    @BeforeEach
    void setup() {
        colorEnabled = MessageUtils.isColorEnabled();
        MessageUtils.setColorEnabled(false);
    }

    @AfterEach
    void tearDown() {
        MessageUtils.setColorEnabled(colorEnabled);
    }

    @Test
    void includesCauseAndSuppressedExceptionsWhenWritingThrowables(TestInfo testInfo) {
        Exception causeOfSuppressed = new NoSuchElementException("cause of suppressed");
        Exception suppressed = new IllegalStateException("suppressed", causeOfSuppressed);
        suppressed.addSuppressed(new IllegalArgumentException(
                "suppressed suppressed", new ArrayIndexOutOfBoundsException("suppressed suppressed cause")));
        Exception cause = new IllegalArgumentException("cause");
        cause.addSuppressed(suppressed);
        Exception throwable = new RuntimeException("top-level", cause);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new MavenSimpleLogger("logger").writeThrowable(throwable, new PrintStream(output));

        String actual = output.toString(UTF_8);
        List<String> actualLines = Arrays.asList(actual.split(System.lineSeparator()));

        Class<?> testClass = testInfo.getTestClass().get();
        String testMethodName = testInfo.getTestMethod().get().getName();
        String testClassStackTraceLinePattern = "at " + testClass.getName() + "." + testMethodName + "\\("
                + testClass.getSimpleName() + ".java:\\d+\\)";
        List<String> expectedLines = Arrays.asList(
                "java.lang.RuntimeException: top-level",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "Caused by: java.lang.IllegalArgumentException: cause",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Suppressed: java.lang.IllegalStateException: suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Suppressed: java.lang.IllegalArgumentException: suppressed suppressed",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Caused by: java.lang.ArrayIndexOutOfBoundsException: suppressed suppressed cause",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Caused by: java.util.NoSuchElementException: cause of suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>");

        assertLinesMatch(expectedLines, actualLines);
    }

    /**
     * Verify that when a {@link LogSink} is installed via {@link LoggerManager},
     * {@code MavenSimpleLogger} routes events to the sink instead of stdout.
     */
    @Test
    void writeRoutesToSinkWhenInstalled() {
        DefaultLoggerManager manager = new DefaultLoggerManager();
        LoggerManager.set(manager);
        try {
            List<LogEvent> captured = new ArrayList<>();
            manager.setLogSink(event -> captured.add(event));

            MavenSimpleLogger logger = new MavenSimpleLogger("test.logger");
            logger.info("hello sink");

            assertEquals(1, captured.size(), "sink should have been called exactly once");
            LogEvent event = captured.get(0);
            assertEquals(LogLevel.INFO, event.level());
            assertEquals("test.logger", event.loggerName());
            assertNotNull(event.message(), "message must not be null");
            assertTrue(event.message().contains("hello sink"), "message must contain the log text");
        } finally {
            LoggerManager.set(null);
        }
    }

    /**
     * Verify that a deeply nested cause chain is truncated at MAX_THROWABLE_DEPTH
     * and that a truncation notice is emitted.
     */
    @Test
    void writeThrowableTruncatesAtMaxDepth() {
        // Build a cause chain of depth 21 — one beyond the limit of 20
        RuntimeException root = new RuntimeException("root");
        RuntimeException current = root;
        for (int i = 0; i < 21; i++) {
            RuntimeException next = new RuntimeException("cause-" + i);
            current.initCause(next);
            current = next;
        }

        List<String> lines = new ArrayList<>();
        new MavenSimpleLogger("test").writeThrowable(root, lines::add);

        assertTrue(
                lines.stream().anyMatch(l -> l.contains("truncated at depth")),
                "Expected a truncation notice after reaching MAX_THROWABLE_DEPTH");
    }

    /**
     * Verify that when a throwable is present, the sink receives a {@link LogEvent}
     * with the exception accessible via {@link LogEvent#throwable()}.
     */
    @Test
    void writeRoutesThrowableToSink() {
        DefaultLoggerManager manager = new DefaultLoggerManager();
        LoggerManager.set(manager);
        try {
            List<LogEvent> captured = new ArrayList<>();
            manager.setLogSink(event -> captured.add(event));

            MavenSimpleLogger logger = new MavenSimpleLogger("test.logger");
            RuntimeException ex = new RuntimeException("boom");
            logger.error("oops", ex);

            assertEquals(1, captured.size(), "sink should have been called exactly once");
            LogEvent event = captured.get(0);
            assertNotNull(event.message(), "message must not be null");
            assertTrue(event.message().contains("oops"), "message must contain the log text");
            assertEquals(ex, event.throwable(), "sink must receive the original throwable");
        } finally {
            LoggerManager.set(null);
        }
    }
}
