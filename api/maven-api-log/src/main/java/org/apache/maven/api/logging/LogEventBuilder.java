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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Fluent builder for a single log statement.
 * <p>
 * Instances are obtained from {@link Logger#atTrace()}, {@link Logger#atDebug()}, etc.
 * A builder <em>must</em> be consumed in a single chained call; it must not be
 * stored in a variable across statements, as implementations may reuse
 * thread-local state for zero-allocation dispatch.
 * <p>
 * When the target level is disabled the {@code Logger} returns a shared
 * {@code NopLogEventBuilder} singleton, making the entire fluent call chain a no-op
 * with zero allocations.
 *
 * <pre>{@code
 * log.atInfo()
 *    .withKeyValue("module", module.getId())
 *    .log("Compiling {} source files", count);
 * }</pre>
 *
 * <h2>Message format</h2>
 * <p>
 * The {@code log(String format, Object...)} family of methods uses a simple
 * positional placeholder syntax: each occurrence of {@code {}} in the format
 * string is replaced, in order, by the string representation of the corresponding
 * argument. No index, type specifier, or locale conversion is supported — this is
 * intentionally simpler than {@link java.text.MessageFormat} and avoids the
 * injection risk of {@code printf}-style specifiers.
 * <p>
 * Example: {@code log("Building {} module(s) in {}", count, dir)} produces
 * {@code "Building 3 module(s) in /tmp/build"} when {@code count=3} and
 * {@code dir="/tmp/build"}.
 * <p>
 * This convention is the same as the one used by SLF4J
 * (<a href="https://www.slf4j.org/api/org/slf4j/helpers/MessageFormatter.html">MessageFormatter</a>),
 * which is widely used in the Java ecosystem and is already the format employed
 * by {@code plugin.Log}, the API this interface replaces.
 *
 * @since 4.0.0
 */
@Experimental
public interface LogEventBuilder {

    /**
     * Attaches a structured key-value pair to this log event.
     * <p>
     * Structured key-value pairs are consumed by structured log appenders
     * (e.g. JSON, ECS, or Logstash layouts in Logback or Log4j 2) that capture
     * fields alongside the message text — rather than embedding metadata inside the
     * message string itself. This enables log aggregators such as Elasticsearch or
     * Splunk to filter and correlate events by field values directly.
     * <p>
     * In a Maven context, typical uses include attaching build metadata such as
     * project coordinates, mojo identifiers, and elapsed time as first-class indexed
     * fields. When running with a plain-text appender these pairs are silently ignored
     * or appended after the message, depending on the appender configuration.
     *
     * @param key   the field name, never {@code null}
     * @param value the string value
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withKeyValue(@Nonnull String key, String value);

    /**
     * Attaches a structured key-value pair with a {@code long} value.
     * <p>
     * See {@link #withKeyValue(String, String)} for the purpose of structured key-value pairs.
     *
     * @param key   the field name, never {@code null}
     * @param value the numeric value
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withKeyValue(@Nonnull String key, long value);

    /**
     * Attaches a structured key-value pair with a {@code boolean} value.
     * <p>
     * See {@link #withKeyValue(String, String)} for the purpose of structured key-value pairs.
     *
     * @param key   the field name, never {@code null}
     * @param value the boolean value
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withKeyValue(@Nonnull String key, boolean value);

    /**
     * Attaches a {@link Throwable} to this log event.
     *
     * @param t the throwable, may be {@code null} (no-op)
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withThrowable(@Nullable Throwable t);

    /**
     * Overrides the project identifier for this event.
     * <p>
     * <strong>Note:</strong> enrichment via this method is best-effort and
     * implementation-defined. Implementations that do not support per-event
     * project context may silently ignore the supplied value.
     *
     * @param projectId the project id, or {@code null} to clear
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withProjectId(String projectId);

    /**
     * Overrides the mojo execution identifier for this event.
     * <p>
     * <strong>Note:</strong> enrichment via this method is best-effort and
     * implementation-defined. Implementations that do not support per-event
     * mojo context may silently ignore the supplied value.
     *
     * @param mojoId the mojo id, or {@code null} to clear
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withMojoId(String mojoId);

    /**
     * Emits the log event with the given pre-formatted message.
     *
     * @param message the message, never {@code null}
     */
    void log(@Nonnull String message);

    /**
     * Emits the log event with a SLF4J-style {@code {}} template and a single argument.
     * <p>
     * Fast path that avoids varargs array allocation.
     *
     * @param format the message template
     * @param arg    the single argument
     */
    void log(@Nonnull String format, Object arg);

    /**
     * Emits the log event with a SLF4J-style {@code {}} template and two arguments.
     * <p>
     * Fast path that avoids varargs array allocation.
     *
     * @param format the message template
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    void log(@Nonnull String format, Object arg1, Object arg2);

    /**
     * Emits the log event with a SLF4J-style {@code {}} template and any number of arguments.
     *
     * @param format the message template
     * @param args   the arguments
     */
    void log(@Nonnull String format, Object... args);

    /**
     * Emits the log event with a lazily-evaluated message supplier.
     * <p>
     * The supplier is only invoked when the level is enabled. When this builder
     * is a NOP instance the supplier is never called.
     *
     * @param messageSupplier a supplier of the log message, never {@code null}
     */
    void log(@Nonnull java.util.function.Supplier<String> messageSupplier);
}
