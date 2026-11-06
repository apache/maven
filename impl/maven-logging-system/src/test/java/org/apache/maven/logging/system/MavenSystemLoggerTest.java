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
package org.apache.maven.logging.system;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.Logger;
import org.apache.maven.api.logging.LoggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSystemLoggerTest {

    private LoggerManager originalManager;

    @BeforeEach
    void setUp() {
        originalManager = LoggerManager.get();
    }

    @AfterEach
    void tearDown() {
        LoggerManager.set(originalManager);
    }

    @Test
    void finderReturnsLogger() {
        MavenLoggerFinder finder = new MavenLoggerFinder();
        System.Logger logger = finder.getLogger("test.logger", getClass().getModule());
        assertNotNull(logger);
        assertEquals("test.logger", logger.getName());
    }

    @Test
    void levelMapping() {
        MavenSystemLogger logger = new MavenSystemLogger("test");
        // ALL and TRACE should be loggable (BufferingLoggerManager enables all levels)
        assertTrue(logger.isLoggable(System.Logger.Level.ALL));
        assertTrue(logger.isLoggable(System.Logger.Level.TRACE));
        assertTrue(logger.isLoggable(System.Logger.Level.ERROR));
        // OFF should never be loggable
        assertFalse(logger.isLoggable(System.Logger.Level.OFF));
    }

    @Test
    void logMessageWithThrowable() {
        List<LogEvent> events = new ArrayList<>();
        LogSink captureSink = events::add;
        LoggerManager.get().setLogSink(captureSink);

        MavenSystemLogger logger = new MavenSystemLogger("test.throwable");
        RuntimeException ex = new RuntimeException("boom");
        logger.log(System.Logger.Level.ERROR, null, "failed", ex);

        assertEquals(1, events.size());
        LogEvent event = events.get(0);
        assertEquals(LogLevel.ERROR, event.level());
        assertEquals("failed", event.message());
        assertSame(ex, event.throwable());
    }

    @Test
    void logMessageWithFormatArgs() {
        List<LogEvent> events = new ArrayList<>();
        LogSink captureSink = events::add;
        LoggerManager.get().setLogSink(captureSink);

        MavenSystemLogger logger = new MavenSystemLogger("test.format");
        logger.log(System.Logger.Level.INFO, (ResourceBundle) null, "Hello {0}, count={1}", "world", 42);

        assertEquals(1, events.size());
        LogEvent event = events.get(0);
        // Template args are always lazily formatted; the formatted result must be correct
        assertEquals("Hello world, count=42", event.message());
    }

    @Test
    void bufferingAndDrain() {
        // Reset to get a fresh BufferingLoggerManager
        LoggerManager.set(null);
        // Force re-discovery — returns BufferingLoggerManager
        // since no ServiceLoader provider is on the test classpath
        LoggerManager.get();

        MavenSystemLogger logger = new MavenSystemLogger("test.buffer");
        logger.log(System.Logger.Level.WARNING, (ResourceBundle) null, "buffered message", (Object[]) null);

        // Install a new manager with a capturing sink — this triggers drain
        List<LogEvent> events = new ArrayList<>();
        LogSink captureSink = events::add;
        SimpleTestManager testManager = new SimpleTestManager(captureSink);
        LoggerManager.set(testManager);

        assertTrue(events.size() >= 1, "Expected at least one event after drain");
        assertEquals("buffered message", events.get(0).message());
        assertEquals(LogLevel.WARN, events.get(0).level());
    }

    /**
     * Minimal LoggerManager for testing drain behavior.
     */
    static class SimpleTestManager implements LoggerManager {
        private LogSink sink;

        SimpleTestManager(LogSink sink) {
            this.sink = sink;
        }

        @Override
        public Logger getLogger(String name) {
            return LoggerManager.get().getLogger(name);
        }

        @Override
        public LogSink getLogSink() {
            return sink;
        }

        @Override
        public void setLogSink(LogSink sink) {
            this.sink = sink;
        }

        @Override
        public boolean isLevelEnabled(String loggerName, LogLevel level) {
            return true;
        }

        @Override
        public void setRootLoggerLevel(LogLevel level) {}

        @Override
        public void reconfigure() {}

        @Override
        public boolean hasReached(LogLevel level) {
            return false;
        }
    }
}
