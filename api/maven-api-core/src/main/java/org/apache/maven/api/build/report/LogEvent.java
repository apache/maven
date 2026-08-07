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
 * A log message emitted during the build — the narrative stream that tells
 * you <em>what the build is doing</em>.
 * <p>
 * A {@code LogEvent} is the Maven equivalent of an SLF4J log line: it carries
 * a timestamp, a severity level, the emitting logger, and the message text.
 * Log events <strong>stream through</strong> the console and are recorded in
 * the build report, but they are not deduplicated or summarized.
 *
 * <h3>LogEvent vs {@link org.apache.maven.api.services.BuilderProblem BuilderProblem}</h3>
 * <p>
 * These two types serve complementary roles:
 * <ul>
 *   <li>{@code LogEvent} — <em>"something happened"</em>: informational progress
 *       ({@code "Compiling 42 source files"}, {@code "Downloading commons-lang3.jar"}).
 *       Streams to the console and is recorded in the build report.</li>
 *   <li>{@code BuilderProblem} — <em>"something needs attention"</em>: an actionable
 *       finding with a source location, deduplication key, and optional fix suggestion
 *       ({@code "unchecked cast at Foo.java:42"}). Collected, deduplicated, and
 *       summarized at the end of the build. Always {@code WARNING} severity or higher,
 *       always user-facing.</li>
 * </ul>
 * <p>
 * The deciding question is: <strong>can the user act on it?</strong> If yes, use
 * {@code BuilderProblem}. If it's informational or progress-related, use {@code LogEvent}.
 *
 * <h3>Audience</h3>
 * <p>
 * Each log event carries an {@link #audience()} that identifies who the message
 * is intended for. Audiences are <strong>cumulative</strong> — each tier includes
 * all messages from lower tiers:
 * <ul>
 *   <li>{@link Audience#USER} — messages the build user acts on:
 *       compilation results, test outcomes, dependency conflicts</li>
 *   <li>{@link Audience#PLUGIN} — adds plugin-internal messages:
 *       mojo parameters, plugin configuration details</li>
 *   <li>{@link Audience#INTERNAL} — adds Maven core internals:
 *       lifecycle ordering, model interpolation, resolver decisions</li>
 * </ul>
 * <p>
 * Console modes use the audience to filter output: {@code --console=rich} shows
 * only {@code USER} messages inline, while {@code -X} shows all three tiers.
 *
 * <h3>Capture hierarchy</h3>
 * <p>
 * Log events are captured at three levels forming a non-overlapping partition
 * of the full build log:
 * <ul>
 *   <li>{@link BuildReport#output()} — events outside any module lifecycle</li>
 *   <li>{@link ModuleReport#output()} — events during a module build but outside any mojo</li>
 *   <li>{@link MojoReport#output()} — events during a mojo execution</li>
 * </ul>
 *
 * @since 4.1.0
 * @see org.apache.maven.api.services.BuilderProblem
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

    /**
     * The intended audience for this log event.
     * <p>
     * Console modes use this to filter output — for example, {@code --console=rich}
     * shows only {@link Audience#USER} messages inline, while {@code -X} shows all
     * three tiers. The build report always records all events regardless of audience.
     * <p>
     * Defaults to {@link Audience#USER} if not specified.
     *
     * @return the audience tier, never {@code null}
     * @since 4.1.0
     */
    @Nonnull
    default Audience audience() {
        return Audience.USER;
    }

    /**
     * Identifies the intended audience for a log event.
     * <p>
     * Audiences are <strong>cumulative</strong>: each tier includes all messages
     * from the tiers below it. A console configured for {@code PLUGIN} will show
     * both {@code USER} and {@code PLUGIN} messages; a console configured for
     * {@code INTERNAL} shows everything.
     *
     * <table>
     *   <caption>Audience tiers and what they add</caption>
     *   <tr><th>Tier</th><th>Shows</th><th>Examples</th></tr>
     *   <tr><td>{@code USER}</td><td>Build outcomes and progress</td>
     *       <td>"Compiling 42 source files", "Tests run: 10, Failures: 0"</td></tr>
     *   <tr><td>{@code PLUGIN}</td><td>+ plugin internals</td>
     *       <td>Mojo parameter dumps, plugin configuration details</td></tr>
     *   <tr><td>{@code INTERNAL}</td><td>+ Maven core internals</td>
     *       <td>Lifecycle phase ordering, model interpolation, resolver traces</td></tr>
     * </table>
     *
     * @since 4.1.0
     */
    @Experimental
    enum Audience {
        /**
         * Messages intended for the build user: compilation results,
         * test outcomes, dependency conflicts, download progress.
         */
        USER,
        /**
         * Messages intended for plugin developers: mojo parameters,
         * classpath details, plugin-internal diagnostics. Includes
         * all {@link #USER} messages.
         */
        PLUGIN,
        /**
         * Messages intended for Maven core developers: lifecycle
         * ordering, model interpolation, resolver negotiation.
         * Includes all {@link #USER} and {@link #PLUGIN} messages.
         */
        INTERNAL
    }
}
