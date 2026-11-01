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
 * @param throwable        the original exception, or {@code null}
 * @param sourceClassName  the source class name (Log API mojo FQCN or JUL source), or {@code null}
 * @param sourceMethodName the source method name (via StackWalker or JUL), or {@code null}
 * @param threadId         the originating thread ID, or {@code -1} if unavailable
 * @param sequenceNumber   the global sequence number for ordering, or {@code -1} if unavailable
 * @param threadName       the originating thread name, or {@code null} if unavailable
 * @param projectId        the project this event belongs to, or {@code null}
 * @param mojoId           the mojo execution that produced this event, or {@code null}
 */
public record DefaultLogEvent(
        Instant timestamp,
        LogLevel level,
        String message,
        String loggerName,
        String stackTrace,
        Throwable throwable,
        String sourceClassName,
        String sourceMethodName,
        long threadId,
        long sequenceNumber,
        String threadName,
        String projectId,
        String mojoId)
        implements LogEvent {

    /**
     * Convenience constructor for direct SLF4J events (no source metadata, no throwable, no thread name).
     */
    public DefaultLogEvent(Instant timestamp, LogLevel level, String message, String loggerName, String stackTrace) {
        this(timestamp, level, message, loggerName, stackTrace, null, null, null, -1, -1, null, null, null);
    }
}
