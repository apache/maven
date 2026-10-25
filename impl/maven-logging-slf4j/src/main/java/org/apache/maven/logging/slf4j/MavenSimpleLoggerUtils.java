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

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.logging.impl.DefaultLogEvent;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Static helpers for converting SLF4J events to the Maven logging API.
 *
 * @since 4.1.0
 */
public final class MavenSimpleLoggerUtils {

    private MavenSimpleLoggerUtils() {}

    /**
     * Converts an SLF4J level integer to a {@link LogLevel}.
     */
    public static LogLevel toLogLevel(int level) {
        return switch (level) {
            case LocationAwareLogger.TRACE_INT -> LogLevel.TRACE;
            case LocationAwareLogger.DEBUG_INT -> LogLevel.DEBUG;
            case LocationAwareLogger.INFO_INT -> LogLevel.INFO;
            case LocationAwareLogger.WARN_INT -> LogLevel.WARN;
            default -> LogLevel.ERROR;
        };
    }

    /**
     * Creates a {@link LogEvent} from SLF4J-sourced parameters.
     * SLF4J events have no source metadata (class/method/threadId).
     */
    @SuppressWarnings("deprecation") // Thread.getId()
    public static LogEvent toLogEvent(int level, String loggerName, String cleanMessage, Throwable throwable) {
        return new DefaultLogEvent(
                MonotonicClock.now(),
                toLogLevel(level),
                cleanMessage != null ? cleanMessage : "",
                loggerName,
                null,
                throwable,
                null,
                null,
                Thread.currentThread().getId(),
                -1,
                Thread.currentThread().getName(),
                null,
                null);
    }
}
