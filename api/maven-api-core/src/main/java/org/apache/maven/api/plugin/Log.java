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
package org.apache.maven.api.plugin;

import java.util.function.Supplier;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Provider;
import org.apache.maven.api.services.BuilderProblem;

/**
 * This interface supplies the API for providing feedback to the user from the {@code Mojo},
 * using standard Maven channels.
 * There should be no big surprises here, although you may notice that the methods accept
 * <code>java.lang.CharSequence</code> rather than <code>java.lang.String</code>. This is provided mainly as a
 * convenience, to enable developers to pass things like <code>java.lang.StringBuffer</code> directly into the logger,
 * rather than formatting first by calling <code>toString()</code>.
 *
 * @since 4.0.0
 */
@Experimental
@Provider
public interface Log {
    /**
     * {@return true if the <b>debug</b> error level is enabled}
     */
    boolean isDebugEnabled();

    /**
     * Sends a message to the user in the <b>debug</b> error level.
     *
     * @param content the message to log
     */
    void debug(CharSequence content);

    /**
     * Sends a message (and accompanying exception) to the user at the <b>debug</b> error level.
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content the message to log
     * @param error the error that caused this log
     */
    void debug(CharSequence content, Throwable error);

    /**
     * Sends an exception to the user in the <b>debug</b> error level.
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error the error that caused this log
     */
    void debug(Throwable error);

    void debug(Supplier<String> content);

    void debug(Supplier<String> content, Throwable error);

    /**
     * {@return true if the <b>info</b> error level is enabled}
     */
    boolean isInfoEnabled();

    /**
     * Sends a message to the user in the <b>info</b> error level.
     *
     * @param content the message to log
     */
    void info(CharSequence content);

    /**
     * Sends a message (and accompanying exception) to the user in the <b>info</b> error level.
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content the message to log
     * @param error the error that caused this log
     */
    void info(CharSequence content, Throwable error);

    /**
     * Sends an exception to the user in the <b>info</b> error level.
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error the error that caused this log
     */
    void info(Throwable error);

    void info(Supplier<String> content);

    void info(Supplier<String> content, Throwable error);

    /**
     * {@return true if the <b>warn</b> error level is enabled}
     */
    boolean isWarnEnabled();

    /**
     * Sends a message to the user in the <b>warn</b> error level.
     *
     * @param content the message to log
     */
    void warn(CharSequence content);

    /**
     * Sends a message (and accompanying exception) to the user in the <b>warn</b> error level.
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content the message to log
     * @param error the error that caused this log
     */
    void warn(CharSequence content, Throwable error);

    /**
     * Sends an exception to the user in the <b>warn</b> error level.
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error the error that caused this log
     */
    void warn(Throwable error);

    void warn(Supplier<String> content);

    void warn(Supplier<String> content, Throwable error);

    /**
     * {@return true if the <b>error</b> error level is enabled}
     */
    boolean isErrorEnabled();

    /**
     * Sends a message to the user in the <b>error</b> error level.
     *
     * @param content the message to log
     */
    void error(CharSequence content);

    /**
     * Sends a message (and accompanying exception) to the user in the <b>error</b> error level.
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content the message to log
     * @param error the error that caused this log
     */
    void error(CharSequence content, Throwable error);

    /**
     * Sends an exception to the user in the <b>error</b> error level.
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error the error that caused this log
     */
    void error(Throwable error);

    void error(Supplier<String> content);

    void error(Supplier<String> content, Throwable error);

    /**
     * Returns a child logger with the given name appended to this logger's name,
     * enabling hierarchical logger namespacing within a plugin.
     * <p>
     * For example, if the current logger is named {@code "compiler:compile"},
     * calling {@code child("diagnostics")} returns a logger named
     * {@code "compiler:compile.diagnostics"}.
     * <p>
     * This is useful when a plugin delegates to sub-components (e.g. options
     * resolution, diagnostic reporting, incremental build decisions) and wants
     * each component's log output to be independently filterable.
     *
     * @param name the child logger name segment (appended after a dot separator)
     * @return a child logger; the default implementation returns {@code this}
     * @since 4.1.0
     */
    @Nonnull
    default Log child(@Nonnull String name) {
        return this;
    }

    /**
     * Reports a structured {@link BuilderProblem} to the build's diagnostic collector.
     * <p>
     * Unlike {@link #warn(CharSequence)}, a structured problem carries a deduplication
     * {@linkplain BuilderProblem#getKey() key}, an optional
     * {@linkplain BuilderProblem#getSuggestion() suggestion}, and an optional
     * {@linkplain BuilderProblem#getDocumentationUrl() documentation URL} — enabling
     * Maven to deduplicate repeated warnings across modules and present an actionable
     * end-of-build summary.
     * <p>
     * The problem is also logged at the appropriate level (WARN or ERROR) so it
     * appears in the normal console output. Callers should <em>not</em> additionally
     * call {@link #warn(CharSequence)} for the same message, as that would produce
     * duplicate output.
     * <p>
     * The default implementation falls back to {@link #warn(CharSequence)} or
     * {@link #error(CharSequence)} based on the problem's severity.
     *
     * @param problem the structured problem to report
     * @since 4.1.0
     */
    default void problem(@Nonnull BuilderProblem problem) {
        if (problem.getSeverity() == BuilderProblem.Severity.ERROR
                || problem.getSeverity() == BuilderProblem.Severity.FATAL) {
            error(problem.getMessage());
        } else {
            warn(problem.getMessage());
        }
    }
}
