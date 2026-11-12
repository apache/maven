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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSimpleLoggerTest {

    /**
     * Verify that when a log sink is installed via {@link LoggerManager},
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
