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
package org.apache.maven.api.build.report;

import java.time.Instant;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * A structured log event captured during the build.
 * <p>
 * Each event carries the log level, timestamp, message, and optionally
 * the logger name and a stack trace. This replaces raw log line strings
 * in the build report, enabling programmatic filtering by level and
 * correlation by timestamp.
 * <p>
 * Events originating from the Maven Log API or from JUL
 * ({@code java.util.logging}) carry additional source metadata: the
 * source class name, source method name, and thread identifier.
 * For Log API events the source class name is the mojo implementation
 * FQCN; for JUL events it comes from {@code LogRecord}.  Events from
 * direct SLF4J logging have these fields set to {@code null}.
 *
 * @since 4.1.0
 */
@Experimental
public interface LogEvent {

    /**
     * When this log event was produced (wall-clock time).
     *
     * @return the event instant, never {@code null}
     */
    @Nonnull
    Instant timestamp();

    /**
     * The severity level of this log event.
     *
     * @return the log level, never {@code null}
     */
    @Nonnull
    LogLevel level();

    /**
     * The log message, without level prefix or timestamp formatting.
     *
     * @return the formatted message, never {@code null}
     */
    @Nonnull
    String message();

    /**
     * The name of the logger that produced this event
     * (e.g. {@code "org.apache.maven.plugins.compiler.CompilerMojo"}).
     *
     * @return the logger name, or {@code null} if unavailable
     */
    @Nullable
    String loggerName();

    /**
     * The stack trace associated with this event, if an exception was logged.
     * <p>
     * The trace is formatted as a multi-line string and may be truncated
     * for very deep stack traces.
     *
     * @return the stack trace string, or {@code null} if no exception was logged
     */
    @Nullable
    String stackTrace();

    /**
     * The fully formatted log line as rendered for console output, including
     * the level prefix, timestamp, and any ANSI styling applied by the logger.
     * <p>
     * This is the string that would be printed to the terminal in verbose mode.
     * Console renderers that just need pass-through output can use this directly,
     * while renderers that apply custom formatting (e.g. rich mode) can use the
     * structured fields ({@link #level()}, {@link #message()}) instead.
     * <p>
     * May be {@code null} if the event was created outside the SLF4J pipeline
     * (e.g. in tests or by programmatic construction).
     *
     * @return the formatted log line, or {@code null}
     */
    @Nullable
    String formattedMessage();

    // ---- Source metadata (populated for Log API and JUL events) ----

    /**
     * The fully qualified class name of the source that issued the log call.
     * <p>
     * For Maven Log API events this is the mojo implementation class name.
     * For JUL events it is the value from {@code LogRecord.getSourceClassName()}.
     * For direct SLF4J logging it is {@code null}.
     *
     * @return the source class name, or {@code null}
     * @since 4.1.0
     */
    @Nullable
    default String sourceClassName() {
        return null;
    }

    /**
     * The method name of the source that issued the log call.
     * <p>
     * For Maven Log API events this is resolved via {@link StackWalker}.
     * For JUL events it is the value from {@code LogRecord.getSourceMethodName()}.
     * For direct SLF4J logging it is {@code null}.
     *
     * @return the source method name, or {@code null}
     * @since 4.1.0
     */
    @Nullable
    default String sourceMethodName() {
        return null;
    }

    /**
     * The thread identifier from which this log event originated.
     * <p>
     * Populated for both Log API and JUL events.  Returns {@code -1}
     * if the thread ID is not available (i.e. for direct SLF4J events).
     *
     * @return the thread ID, or {@code -1} if unavailable
     * @since 4.1.0
     */
    default long threadId() {
        return -1;
    }

    /**
     * A monotonically increasing sequence number for total ordering of
     * log events, useful when multiple events share the same timestamp.
     * <p>
     * Assigned by the logging pipeline when the event is captured,
     * providing a global ordering across all event sources (Log API,
     * JUL, and direct SLF4J).
     *
     * @return the sequence number, always non-negative
     * @since 4.1.0
     */
    default long sequenceNumber() {
        return -1;
    }
}
