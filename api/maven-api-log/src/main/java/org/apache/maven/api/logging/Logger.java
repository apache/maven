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

import java.util.function.Supplier;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.Provider;

/**
 * Unified logging interface for Maven plugins and CLI bootstrap code.
 * <p>
 * Replaces both {@code org.apache.maven.api.plugin.Log} (mojo logging) and
 * {@code org.apache.maven.api.cli.Logger} (CLI bootstrap logging) with a single
 * interface that supports a fluent builder API, structured key-value pairs, and
 * lazy message evaluation.
 * <p>
 * Instances are obtained via dependency injection. A {@code @Singleton} bean
 * may hold a long-lived {@code Logger} instance safely: context fields such as
 * {@code projectId} and {@code mojoId} are resolved from a per-thread
 * {@code LogContext} at event dispatch time, not at injection time.
 *
 * <h2>Fluent API (preferred)</h2>
 * <pre>{@code
 * log.atInfo()
 *    .withKeyValue("artifact", artifact.getId())
 *    .log("Resolved {} files", count);
 * }</pre>
 *
 * <h2>Legacy convenience methods</h2>
 * <pre>{@code
 * log.info("Compiling {} source files", count);
 * log.warn("Deprecated configuration key: {}", key, e);
 * }</pre>
 *
 * @since 4.1.0
 */
@Experimental
@Provider
public interface Logger {

    /**
     * The name of this logger, typically the fully qualified class name of
     * the component that owns it.
     *
     * @return the logger name, never {@code null}
     */
    @Nonnull
    String name();

    /**
     * Returns {@code true} if the given level is enabled for this logger.
     *
     * @param level the level to test, never {@code null}
     * @return {@code true} if enabled
     */
    boolean isEnabled(@Nonnull LogLevel level);

    /** @return {@code true} if {@link LogLevel#TRACE} is enabled */
    default boolean isTraceEnabled() {
        return isEnabled(LogLevel.TRACE);
    }

    /** @return {@code true} if {@link LogLevel#DEBUG} is enabled */
    default boolean isDebugEnabled() {
        return isEnabled(LogLevel.DEBUG);
    }

    /** @return {@code true} if {@link LogLevel#INFO} is enabled */
    default boolean isInfoEnabled() {
        return isEnabled(LogLevel.INFO);
    }

    /** @return {@code true} if {@link LogLevel#WARN} is enabled */
    default boolean isWarnEnabled() {
        return isEnabled(LogLevel.WARN);
    }

    /** @return {@code true} if {@link LogLevel#ERROR} is enabled */
    default boolean isErrorEnabled() {
        return isEnabled(LogLevel.ERROR);
    }

    // ---- Fluent entry points ----

    /**
     * Returns a {@link LogEventBuilder} for {@link LogLevel#TRACE}.
     * <p>
     * When TRACE is disabled, returns a singleton no-op builder (zero allocations).
     *
     * @return a log builder, never {@code null}
     */
    @Nonnull
    LogEventBuilder atTrace();

    /**
     * Returns a {@link LogEventBuilder} for {@link LogLevel#DEBUG}.
     *
     * @return a log builder, never {@code null}
     */
    @Nonnull
    LogEventBuilder atDebug();

    /**
     * Returns a {@link LogEventBuilder} for {@link LogLevel#INFO}.
     *
     * @return a log builder, never {@code null}
     */
    @Nonnull
    LogEventBuilder atInfo();

    /**
     * Returns a {@link LogEventBuilder} for {@link LogLevel#WARN}.
     *
     * @return a log builder, never {@code null}
     */
    @Nonnull
    LogEventBuilder atWarn();

    /**
     * Returns a {@link LogEventBuilder} for {@link LogLevel#ERROR}.
     *
     * @return a log builder, never {@code null}
     */
    @Nonnull
    LogEventBuilder atError();

    /**
     * Returns a {@link LogEventBuilder} for the given level.
     *
     * @param level the target level, never {@code null}
     * @return a log builder, never {@code null}
     */
    @Nonnull
    LogEventBuilder at(@Nonnull LogLevel level);

    // ---- Legacy convenience: trace ----

    /** Logs a TRACE message. */
    default void trace(@Nonnull String message) {
        atTrace().log(message);
    }

    /** Logs a TRACE message with one {@code {}} argument. */
    default void trace(@Nonnull String format, @Nullable Object arg) {
        atTrace().log(format, arg);
    }

    /** Logs a TRACE message with two {@code {}} arguments. */
    default void trace(@Nonnull String format, @Nullable Object arg1, @Nullable Object arg2) {
        atTrace().log(format, arg1, arg2);
    }

    /** Logs a TRACE message with varargs arguments. */
    default void trace(@Nonnull String format, Object... args) {
        atTrace().log(format, args);
    }

    /** Logs a TRACE message with a throwable. */
    default void trace(@Nonnull String message, @Nullable Throwable t) {
        atTrace().withThrowable(t).log(message);
    }

    /** Logs a lazily-evaluated TRACE message. */
    default void trace(@Nonnull Supplier<String> message) {
        atTrace().log(message);
    }

    /** Logs a lazily-evaluated TRACE message with a throwable. */
    default void trace(@Nonnull Supplier<String> message, @Nullable Throwable t) {
        atTrace().withThrowable(t).log(message);
    }

    // ---- Legacy convenience: debug ----

    /** Logs a DEBUG message. */
    default void debug(@Nonnull String message) {
        atDebug().log(message);
    }

    /** Logs a DEBUG message with one {@code {}} argument. */
    default void debug(@Nonnull String format, @Nullable Object arg) {
        atDebug().log(format, arg);
    }

    /** Logs a DEBUG message with two {@code {}} arguments. */
    default void debug(@Nonnull String format, @Nullable Object arg1, @Nullable Object arg2) {
        atDebug().log(format, arg1, arg2);
    }

    /** Logs a DEBUG message with varargs arguments. */
    default void debug(@Nonnull String format, Object... args) {
        atDebug().log(format, args);
    }

    /** Logs a DEBUG message with a throwable. */
    default void debug(@Nonnull String message, @Nullable Throwable t) {
        atDebug().withThrowable(t).log(message);
    }

    /** Logs a lazily-evaluated DEBUG message. */
    default void debug(@Nonnull Supplier<String> message) {
        atDebug().log(message);
    }

    /** Logs a lazily-evaluated DEBUG message with a throwable. */
    default void debug(@Nonnull Supplier<String> message, @Nullable Throwable t) {
        atDebug().withThrowable(t).log(message);
    }

    // ---- Legacy convenience: info ----

    /** Logs an INFO message. */
    default void info(@Nonnull String message) {
        atInfo().log(message);
    }

    /** Logs an INFO message with one {@code {}} argument. */
    default void info(@Nonnull String format, @Nullable Object arg) {
        atInfo().log(format, arg);
    }

    /** Logs an INFO message with two {@code {}} arguments. */
    default void info(@Nonnull String format, @Nullable Object arg1, @Nullable Object arg2) {
        atInfo().log(format, arg1, arg2);
    }

    /** Logs an INFO message with varargs arguments. */
    default void info(@Nonnull String format, Object... args) {
        atInfo().log(format, args);
    }

    /** Logs an INFO message with a throwable. */
    default void info(@Nonnull String message, @Nullable Throwable t) {
        atInfo().withThrowable(t).log(message);
    }

    /** Logs a lazily-evaluated INFO message. */
    default void info(@Nonnull Supplier<String> message) {
        atInfo().log(message);
    }

    /** Logs a lazily-evaluated INFO message with a throwable. */
    default void info(@Nonnull Supplier<String> message, @Nullable Throwable t) {
        atInfo().withThrowable(t).log(message);
    }

    // ---- Legacy convenience: warn ----

    /** Logs a WARN message. */
    default void warn(@Nonnull String message) {
        atWarn().log(message);
    }

    /** Logs a WARN message with one {@code {}} argument. */
    default void warn(@Nonnull String format, @Nullable Object arg) {
        atWarn().log(format, arg);
    }

    /** Logs a WARN message with two {@code {}} arguments. */
    default void warn(@Nonnull String format, @Nullable Object arg1, @Nullable Object arg2) {
        atWarn().log(format, arg1, arg2);
    }

    /** Logs a WARN message with varargs arguments. */
    default void warn(@Nonnull String format, Object... args) {
        atWarn().log(format, args);
    }

    /** Logs a WARN message with a throwable. */
    default void warn(@Nonnull String message, @Nullable Throwable t) {
        atWarn().withThrowable(t).log(message);
    }

    /** Logs a lazily-evaluated WARN message. */
    default void warn(@Nonnull Supplier<String> message) {
        atWarn().log(message);
    }

    /** Logs a lazily-evaluated WARN message with a throwable. */
    default void warn(@Nonnull Supplier<String> message, @Nullable Throwable t) {
        atWarn().withThrowable(t).log(message);
    }

    // ---- Legacy convenience: error ----

    /** Logs an ERROR message. */
    default void error(@Nonnull String message) {
        atError().log(message);
    }

    /** Logs an ERROR message with one {@code {}} argument. */
    default void error(@Nonnull String format, @Nullable Object arg) {
        atError().log(format, arg);
    }

    /** Logs an ERROR message with two {@code {}} arguments. */
    default void error(@Nonnull String format, @Nullable Object arg1, @Nullable Object arg2) {
        atError().log(format, arg1, arg2);
    }

    /** Logs an ERROR message with varargs arguments. */
    default void error(@Nonnull String format, Object... args) {
        atError().log(format, args);
    }

    /** Logs an ERROR message with a throwable. */
    default void error(@Nonnull String message, @Nullable Throwable t) {
        atError().withThrowable(t).log(message);
    }

    /** Logs a lazily-evaluated ERROR message. */
    default void error(@Nonnull Supplier<String> message) {
        atError().log(message);
    }

    /** Logs a lazily-evaluated ERROR message with a throwable. */
    default void error(@Nonnull Supplier<String> message, @Nullable Throwable t) {
        atError().withThrowable(t).log(message);
    }

    // ---- Static factory ----

    /**
     * Returns a {@code Logger} for the given class, using the fully-qualified
     * class name as the logger name.
     * <p>
     * Shorthand for {@code LoggerManager.get().getLogger(clazz)}. Intended for
     * use at field-initialization time:
     * <pre>{@code
     * private static final Logger log = Logger.of(MyComponent.class);
     * }</pre>
     *
     * @param clazz the class whose name is used as the logger name, never {@code null}
     * @return a {@code Logger} instance, never {@code null}
     * @since 4.1.0
     */
    @Nonnull
    static Logger of(@Nonnull Class<?> clazz) {
        return LoggerManager.get().getLogger(clazz);
    }

    /**
     * Returns a {@code Logger} with the given name.
     * <p>
     * Shorthand for {@code LoggerManager.get().getLogger(name)}. Intended for
     * framework code or dynamic logger names:
     * <pre>{@code
     * Logger log = Logger.of("org.apache.maven.my-component");
     * }</pre>
     *
     * @param name the logger name, never {@code null}
     * @return a {@code Logger} instance, never {@code null}
     * @since 4.1.0
     */
    @Nonnull
    static Logger of(@Nonnull String name) {
        return LoggerManager.get().getLogger(name);
    }

    // ---- Child logger ----

    /**
     * Returns a child logger whose name is this logger's name with
     * {@code "." + name} appended.
     *
     * <p>The child logger inherits this logger's structured context —
     * {@code projectId}, {@code mojoId} and the active {@link LogSink} —
     * so events emitted by sub-components are automatically tagged with the
     * correct project and mojo execution without any additional injection.
     * This is the preferred way for a mojo to pass a scoped logger to a
     * helper or service it delegates to.</p>
     *
     * @param name the child suffix, never {@code null}
     * @return a child {@code Logger} instance that inherits this logger's context, never {@code null}
     */
    @Nonnull
    Logger child(@Nonnull String name);
}
