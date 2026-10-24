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
package org.apache.maven.logging.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.Constants;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.plugin.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultLogger}: sink-based dispatch, level gating, child propagation.
 */
class DefaultLoggerTest {

    private String originalLevel;

    @BeforeEach
    void setUp() {
        originalLevel = System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        LoggerManager.set(new DefaultLoggerManager());
    }

    @AfterEach
    void tearDown() {
        if (originalLevel != null) {
            System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, originalLevel);
        } else {
            System.clearProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        }
    }

    /** A simple capturing sink that accepts all events. */
    static class CapturingSink implements LogSink {
        final List<LogEvent> events = new ArrayList<>();

        @Override
        public void accept(LogEvent event) {
            events.add(event);
        }
    }

    @Test
    void warnWithSupplierAndThrowableDispatchesToSink() {
        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger log = new DefaultLogger("test.logger");
        RuntimeException ex = new RuntimeException("test");

        log.warn(() -> "warning message", ex);

        assertEquals(1, sink.events.size());
        LogEvent e = sink.events.get(0);
        assertEquals(LogLevel.WARN, e.level());
        assertEquals("warning message", e.message());
        assertEquals(ex, e.throwable());
    }

    @Test
    void traceDispatchesToSink() {
        System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, "trace");
        LoggerManager.set(new DefaultLoggerManager());

        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger log = new DefaultLogger("test.logger");

        log.trace("trace message");

        assertEquals(1, sink.events.size());
        assertEquals(LogLevel.TRACE, sink.events.get(0).level());
        assertEquals("trace message", sink.events.get(0).message());
    }

    @Test
    void traceIsNoOpWhenLevelDisablesIt() {
        System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, "warn");
        LoggerManager.set(new DefaultLoggerManager());

        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger log = new DefaultLogger("test.logger");

        log.trace("should not be logged");
        log.debug("should not be logged");

        assertTrue(sink.events.isEmpty());
    }

    @Test
    void childInheritsContextAndSink() {
        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger parent = new DefaultLogger("com.example.MyMojo", "my-project", "compile@default");

        Log child = parent.child("diagnostics");

        assertNotSame(parent, child);
        assertTrue(child instanceof DefaultLogger, "child should be a DefaultLogger");
        assertEquals("com.example.MyMojo.diagnostics", ((DefaultLogger) child).name());

        // Child events carry the same projectId/mojoId
        child.info("child message");
        assertEquals(1, sink.events.size());
        LogEvent e = sink.events.get(0);
        assertEquals("my-project", e.projectId());
        assertEquals("compile@default", e.mojoId());
        assertEquals("com.example.MyMojo.diagnostics", e.loggerName());
    }

    @Test
    void eventsCarryProjectIdAndMojoId() {
        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger log = new DefaultLogger("com.example.MyMojo", "my-project", "compile@default");

        log.info("hello");

        assertEquals(1, sink.events.size());
        LogEvent e = sink.events.get(0);
        assertEquals("my-project", e.projectId());
        assertEquals("compile@default", e.mojoId());
        assertEquals(LogLevel.INFO, e.level());
        assertEquals("hello", e.message());
    }

    @Test
    void fluentBuilderDefersFormatting() {
        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger log = new DefaultLogger("test.logger");

        log.atInfo().log("hello {} and {}", "world", 42);

        assertEquals(1, sink.events.size());
        LogEvent e = sink.events.get(0);
        assertEquals("hello {} and {}", e.messageTemplate());
        assertEquals(2, e.messageArgs().length);
        assertEquals("world", e.messageArgs()[0]);
        assertEquals(42, e.messageArgs()[1]);
        assertEquals("hello world and 42", e.message());
    }

    @Test
    void fluentBuilderPassesKeyValuesAsStructuredData() {
        CapturingSink sink = new CapturingSink();
        LoggerManager.get().setLogSink(sink);
        DefaultLogger log = new DefaultLogger("test.logger");

        log.atInfo().withKeyValue("module", "core").withKeyValue("count", 5).log("compiling");

        assertEquals(1, sink.events.size());
        LogEvent e = sink.events.get(0);
        assertEquals("compiling", e.message());
        assertEquals("core", e.keyValues().get("module"));
        assertEquals("5", e.keyValues().get("count"));
    }

    /**
     * Verify that the default {@code Log.isTraceEnabled()} returns false,
     * preventing {@code AbstractMethodError} for third-party implementors.
     */
    @Test
    void defaultTraceIsDisabledOnMinimalImpl() {
        Log minimal = new Log() {
            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(CharSequence c) {}

            @Override
            public void debug(CharSequence c, Throwable e) {}

            @Override
            public void debug(Throwable e) {}

            @Override
            public void debug(java.util.function.Supplier<String> c) {}

            @Override
            public void debug(java.util.function.Supplier<String> c, Throwable e) {}

            @Override
            public boolean isInfoEnabled() {
                return false;
            }

            @Override
            public void info(CharSequence c) {}

            @Override
            public void info(CharSequence c, Throwable e) {}

            @Override
            public void info(Throwable e) {}

            @Override
            public void info(java.util.function.Supplier<String> c) {}

            @Override
            public void info(java.util.function.Supplier<String> c, Throwable e) {}

            @Override
            public boolean isWarnEnabled() {
                return false;
            }

            @Override
            public void warn(CharSequence c) {}

            @Override
            public void warn(CharSequence c, Throwable e) {}

            @Override
            public void warn(Throwable e) {}

            @Override
            public void warn(java.util.function.Supplier<String> c) {}

            @Override
            public void warn(java.util.function.Supplier<String> c, Throwable e) {}

            @Override
            public boolean isErrorEnabled() {
                return false;
            }

            @Override
            public void error(CharSequence c) {}

            @Override
            public void error(CharSequence c, Throwable e) {}

            @Override
            public void error(Throwable e) {}

            @Override
            public void error(java.util.function.Supplier<String> c) {}

            @Override
            public void error(java.util.function.Supplier<String> c, Throwable e) {}
        };

        assertEquals(false, minimal.isTraceEnabled());
        minimal.trace("should be a no-op");
        minimal.trace("no-op", new RuntimeException());
        minimal.trace(new RuntimeException());
        minimal.trace(() -> "no-op");
        minimal.trace(() -> "no-op", new RuntimeException());
    }
}
