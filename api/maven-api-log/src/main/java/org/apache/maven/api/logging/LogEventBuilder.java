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

/**
 * Fluent builder for a single log statement.
 * <p>
 * Instances are obtained from {@link Log#atTrace()}, {@link Log#atDebug()}, etc.
 * A builder <em>must</em> be consumed in a single chained call; it must not be
 * stored in a variable across statements, as implementations may reuse
 * thread-local state for zero-allocation dispatch.
 * <p>
 * When the target level is disabled the {@code Log} returns a shared
 * {@code NopLogBuilder} singleton, making the entire fluent call chain a no-op
 * with zero allocations.
 *
 * <pre>{@code
 * log.atInfo()
 *    .withKeyValue("module", module.getId())
 *    .log("Compiling {} source files", count);
 * }</pre>
 *
 * @since 4.1.0
 */
@Experimental
public interface LogEventBuilder {

    /**
     * Attaches a structured key-value pair to this log event.
     *
     * @param key   the key, never {@code null}
     * @param value the string value
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withKeyValue(@Nonnull String key, String value);

    /**
     * Attaches a structured key-value pair with a {@code long} value.
     *
     * @param key   the key, never {@code null}
     * @param value the numeric value
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withKeyValue(@Nonnull String key, long value);

    /**
     * Attaches a structured key-value pair with a {@code boolean} value.
     *
     * @param key   the key, never {@code null}
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
    LogEventBuilder withThrowable(Throwable t);

    /**
     * Overrides the project identifier for this event.
     * <p>
     * The default path resolves {@code projectId} from the current thread's
     * {@code LogContext}; use this method only in the rare cases where the
     * ThreadLocal context is incorrect (e.g. async hand-offs).
     *
     * @param projectId the project id, or {@code null} to clear
     * @return this builder
     */
    @Nonnull
    LogEventBuilder withProjectId(String projectId);

    /**
     * Overrides the mojo execution identifier for this event.
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
