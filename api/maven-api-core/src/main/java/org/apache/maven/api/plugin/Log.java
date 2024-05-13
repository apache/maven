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
import org.apache.maven.api.annotations.Provider;

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
     * {@return true if the <b>debug</b> error level is enabled}.
     */
    boolean isDebugEnabled();

    /**
     * Sends a message to the user in the <b>debug</b> error level.
     *
     * @param content the message to log
     */
    void debug(CharSequence content);

    /**
     * Sends a message (and accompanying exception) to the user in the <b>debug</b> error level.
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
     * {@return true if the <b>info</b> error level is enabled}.
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
     * {@return true if the <b>warn</b> error level is enabled}.
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
     * {@return true if the <b>error</b> error level is enabled}.
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
}
