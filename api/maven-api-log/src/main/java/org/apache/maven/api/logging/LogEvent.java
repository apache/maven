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
package org.apache.maven.api.logging;

import java.time.Instant;
import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
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
 * FQCN; for JUL events it comes from {@code LogRecord}. Events from
 * direct SLF4J logging have these fields set to {@code null}.
 * <p>
 * Events produced via the fluent {@link LogEventBuilder} API may additionally
 * carry structured key-value pairs ({@link #keyValues()}) and the raw
 * unformatted message template ({@link #messageTemplate()},
 * {@link #messageArgs()}) alongside the formatted {@link #message()}.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
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
     * <p>
     * When the event was produced via {@link LogEventBuilder#log(String, Object...)}
     * this is the formatted result. When no template args were supplied it is
     * the literal message string.
     *
     * @return the log message, never {@code null}
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
     * The original exception that triggered this log event, if available.
     *
     * @return the original throwable, or {@code null}
     */
    @Nullable
    default Throwable throwable() {
        return null;
    }

    // ---- Source metadata (populated for Log API and JUL events) ----

    /**
     * The fully qualified class name of the source that issued the log call.
     * <p>
     * For Maven Log API events this is the mojo implementation class name.
     * For JUL events it is the value from {@code LogRecord.getSourceClassName()}.
     * For direct SLF4J logging it is {@code null}.
     *
     * @return the source class name, or {@code null}
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
     */
    @Nullable
    default String sourceMethodName() {
        return null;
    }

    /**
     * The thread identifier from which this log event originated.
     *
     * @return the thread ID, or {@code -1} if unavailable
     */
    default long threadId() {
        return -1;
    }

    /**
     * The name of the thread from which this log event originated.
     *
     * @return the thread name, or {@code null} if unavailable
     */
    @Nullable
    default String threadName() {
        return null;
    }

    /**
     * A monotonically increasing sequence number for total ordering of log events.
     *
     * @return the sequence number, or {@code -1} if unavailable
     */
    default long sequenceNumber() {
        return -1;
    }

    /**
     * The project this log event belongs to
     * (e.g. {@code "org.apache.maven:maven-core:4.1.0-SNAPSHOT"}), or {@code null}
     * if the event was not produced in the context of a specific project.
     *
     * @return the project identifier, or {@code null}
     */
    @Nullable
    default String projectId() {
        return null;
    }

    /**
     * The mojo execution that produced this event
     * (e.g. {@code "compiler:compile@default-compile"}), or {@code null}
     * if the event was logged outside a mojo execution.
     *
     * @return the mojo execution identifier, or {@code null}
     */
    @Nullable
    default String mojoId() {
        return null;
    }

    // ---- Structured data (populated for fluent LogBuilder events) ----

    /**
     * Structured key-value pairs attached to this event via
     * {@link LogEventBuilder#withKeyValue(String, String)} and its overloads.
     * <p>
     * Returns an empty map when no key-value pairs were supplied.
     *
     * @return an immutable map of key-value pairs, never {@code null}
     */
    @Nonnull
    default Map<String, String> keyValues() {
        return Map.of();
    }

    /**
     * The unformatted message template as passed to
     * {@link LogEventBuilder#log(String, Object...)}, or {@code null} when the
     * event was produced with a pre-formatted message (no template args).
     *
     * @return the raw message template, or {@code null}
     */
    @Nullable
    default String messageTemplate() {
        return null;
    }

    /**
     * The raw argument array passed to
     * {@link LogEventBuilder#log(String, Object...)}, or {@code null} when no
     * template args were supplied.
     *
     * @return the raw args array, or {@code null}
     */
    @Nullable
    default Object[] messageArgs() {
        return null;
    }
}
