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
package org.apache.maven.internal.build;

import java.time.Instant;

import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.LogLevel;

/**
 * Immutable implementation of {@link LogEvent}.
 * <p>
 * Public to allow construction from other packages within the Maven
 * implementation (e.g. {@code ProjectBuildLogAppender}).
 *
 * @param timestamp        when the event was produced
 * @param level            the severity level
 * @param message          the clean log message (without level prefix or ANSI)
 * @param loggerName       the name of the logger, or {@code null}
 * @param stackTrace       the stack trace string, or {@code null}
 * @param formattedMessage the fully formatted console line, or {@code null}
 * @param sourceClassName  the JUL source class name, or {@code null}
 * @param sourceMethodName the JUL source method name, or {@code null}
 * @param threadId         the JUL thread ID, or {@code -1} if unavailable
 */
public record DefaultLogEvent(
        Instant timestamp,
        LogLevel level,
        String message,
        String loggerName,
        String stackTrace,
        String formattedMessage,
        String sourceClassName,
        String sourceMethodName,
        long threadId)
        implements LogEvent {

    /**
     * Convenience constructor for events without JUL metadata
     * (i.e. events from the SLF4J pipeline).
     */
    public DefaultLogEvent(
            Instant timestamp,
            LogLevel level,
            String message,
            String loggerName,
            String stackTrace,
            String formattedMessage) {
        this(timestamp, level, message, loggerName, stackTrace, formattedMessage, null, null, -1);
    }

    /**
     * Convenience constructor for events created without a formatted message
     * (e.g. in tests or programmatic construction).
     */
    DefaultLogEvent(Instant timestamp, LogLevel level, String message, String loggerName, String stackTrace) {
        this(timestamp, level, message, loggerName, stackTrace, null, null, null, -1);
    }
}
