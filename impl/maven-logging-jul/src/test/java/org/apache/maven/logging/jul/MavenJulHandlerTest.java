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
package org.apache.maven.logging.jul;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Timeout(10)
class MavenJulHandlerTest {

    private DefaultLoggerManager testManager;

    @BeforeEach
    void installTestManager() {
        testManager = new DefaultLoggerManager();
        testManager.setRootLoggerLevel(LogLevel.TRACE);
        testManager.install();
    }

    @AfterEach
    void clearTestManager() {
        testManager.uninstall();
        testManager = null;
    }

    @ParameterizedTest(name = "JUL {0} -> LogLevel.{1}")
    @CsvSource({
        "FINEST, TRACE",
        "FINER, DEBUG",
        "FINE, DEBUG",
        "CONFIG, INFO",
        "INFO, INFO",
        "WARNING, WARN",
        "SEVERE, ERROR",
    })
    void julLevelMapsToCorrectLogLevel(String julLevelName, String expectedLogLevel) {
        Level julLevel = Level.parse(julLevelName);
        LogLevel actual = MavenJulHandler.julLevelToLogLevel(julLevel);
        assertEquals(LogLevel.valueOf(expectedLogLevel), actual);
    }

    @Test
    void finestMapsToTrace() {
        assertEquals(LogLevel.TRACE, MavenJulHandler.julLevelToLogLevel(Level.FINEST));
    }

    @Test
    void configMapsToInfo() {
        assertEquals(LogLevel.INFO, MavenJulHandler.julLevelToLogLevel(Level.CONFIG));
    }

    @Test
    void publishIsReentrantSafe() {
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        testManager.setLogSink(captured::set);

        MavenJulHandler handler = new MavenJulHandler();
        LogRecord record = new LogRecord(Level.INFO, "nested");

        MavenJulHandler.setInPublishForTest(true);
        try {
            handler.publish(record);
        } finally {
            MavenJulHandler.setInPublishForTest(false);
        }
        assertNull(captured.get(), "Re-entrant publish must be silently dropped");
    }

    @Test
    void publishWithParameterizedMessagePreservesTemplateAndArgs() {
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        testManager.setLogSink(new LogSink() {
            @Override
            public void accept(LogEvent event) {
                captured.set(event);
            }

            @Override
            public boolean needsMetadata() {
                return false;
            }
        });

        MavenJulHandler handler = new MavenJulHandler();
        LogRecord record = new LogRecord(Level.INFO, "hello {0}");
        record.setParameters(new Object[] {"world"});
        handler.publish(record);

        LogEvent event = captured.get();
        assertNotNull(event, "event should have been captured");
        // Template is preserved as-is — no eager MessageFormat substitution.
        assertEquals("hello {0}", event.messageTemplate());
        List<?> args = event.messageArgs();
        assertNotNull(args);
        assertEquals(1, args.size());
        assertEquals("world", args.get(0));
        // message() computes the formatted string lazily.
        assertEquals("hello world", event.message());
    }

    @Test
    void publishApostropheIsNotMangledByMessageFormat() {
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        testManager.setLogSink(new LogSink() {
            @Override
            public void accept(LogEvent event) {
                captured.set(event);
            }

            @Override
            public boolean needsMetadata() {
                return false;
            }
        });

        MavenJulHandler handler = new MavenJulHandler();
        // A message with a literal apostrophe and a positional placeholder.
        // MessageFormat would corrupt "it's {0}" → "its {0}" (apostrophe eaten) or
        // require escaping as "it''s {0}" to get "it's {0}".
        // DefaultLogEvent.formatMessage() uses simple positional substitution that
        // does NOT treat apostrophes specially, so "it's {0}" → "it's fine".
        LogRecord record = new LogRecord(Level.WARNING, "it's {0}");
        record.setParameters(new Object[] {"fine"});
        handler.publish(record);

        LogEvent event = captured.get();
        assertNotNull(event);
        // Raw template preserved — apostrophe intact, no escaping needed.
        assertEquals("it's {0}", event.messageTemplate());
        // Lazy formatting produces the correct string without mangling the apostrophe.
        assertEquals("it's fine", event.message());
    }

    @Test
    void publishWithNoParametersHasNullArgs() {
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        testManager.setLogSink(new LogSink() {
            @Override
            public void accept(LogEvent event) {
                captured.set(event);
            }

            @Override
            public boolean needsMetadata() {
                return false;
            }
        });

        MavenJulHandler handler = new MavenJulHandler();
        LogRecord record = new LogRecord(Level.INFO, "plain message");
        // No parameters set → messageArgs should be null.
        handler.publish(record);

        LogEvent event = captured.get();
        assertNotNull(event);
        assertEquals("plain message", event.messageTemplate());
        assertNull(event.messageArgs());
        assertEquals("plain message", event.message());
    }
}
