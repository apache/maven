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
package org.apache.maven.api.cli;

import java.util.List;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Defines a simple logging interface for Maven CLI operations. These operations happen "early", when there may
 * be no logging set up even. Implementations may be "accumulating", in which case {@link #drain()}  method should
 * be used.
 * <p>
 * This interface provides methods for logging messages at different severity levels
 * and supports logging with or without associated exceptions.
 *
 * @since 4.0.0
 */
@Experimental
public interface Logger {

    /**
     * Represents the severity levels for log messages.
     */
    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * Logs a message at the specified level without an associated exception.
     *
     * @param level the severity level of the message
     * @param message the message to be logged
     */
    default void log(@Nonnull Level level, @Nonnull String message) {
        log(level, message, null);
    }

    /**
     * Logs a message at the specified level with an associated exception.
     *
     * @param level the severity level of the message
     * @param message the message to be logged
     * @param error the associated exception, or null if not applicable
     */
    void log(@Nonnull Level level, @Nonnull String message, @Nullable Throwable error);

    /**
     * Logs a debug message without an associated exception.
     *
     * @param message the debug message to be logged
     */
    default void debug(String message) {
        log(Level.DEBUG, message);
    }

    /**
     * Logs a debug message with an associated exception.
     *
     * @param message the debug message to be logged
     * @param error the associated exception
     */
    default void debug(@Nonnull String message, @Nullable Throwable error) {
        log(Level.DEBUG, message, error);
    }

    /**
     * Logs an info message without an associated exception.
     *
     * @param message the info message to be logged
     */
    default void info(@Nonnull String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs an info message with an associated exception.
     *
     * @param message the info message to be logged
     * @param error the associated exception
     */
    default void info(@Nonnull String message, @Nullable Throwable error) {
        log(Level.INFO, message, error);
    }

    /**
     * Logs a warning message without an associated exception.
     *
     * @param message the warning message to be logged
     */
    default void warn(@Nonnull String message) {
        log(Level.WARN, message);
    }

    /**
     * Logs a warning message with an associated exception.
     *
     * @param message the warning message to be logged
     * @param error the associated exception
     */
    default void warn(@Nonnull String message, @Nullable Throwable error) {
        log(Level.WARN, message, error);
    }

    /**
     * Logs an error message without an associated exception.
     *
     * @param message the error message to be logged
     */
    default void error(@Nonnull String message) {
        log(Level.ERROR, message);
    }

    /**
     * Logs an error message with an associated exception.
     *
     * @param message the error message to be logged
     * @param error the associated exception
     */
    default void error(@Nonnull String message, @Nullable Throwable error) {
        log(Level.ERROR, message, error);
    }

    /**
     * Logger entries returned by {@link #drain()} method.
     * @param level The logging level, never {@code null}.
     * @param message The logging message, never {@code null}.
     * @param error The error, if applicable.
     */
    record Entry(@Nonnull Level level, @Nonnull String message, @Nullable Throwable error) {}

    /**
     * If this is an accumulating log, it will "drain" this instance. It returns the accumulated log entries, and
     * also "resets" this instance to empty (initial) state.
     */
    @Nonnull
    default List<Entry> drain() {
        return List.of();
    }
}
