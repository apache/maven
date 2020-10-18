package org.apache.maven.plugin.logging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This interface supplies the API for providing feedback to the user from the <code>Mojo</code>, using standard
 * <code>Maven</code> channels.<br>
 * There should be no big surprises here, although you may notice that the methods accept
 * <code>java.lang.CharSequence</code> rather than <code>java.lang.String</code>. This is provided mainly as a
 * convenience, to enable developers to pass things like <code>java.lang.StringBuffer</code> directly into the logger,
 * rather than formatting first by calling <code>toString()</code>.
 *
 * @author jdcasey
 *
 * @deprecated Use SLF4J directly
 */
@Deprecated
public interface Log
{
    /**
     * @return true if the <b>debug</b> error level is enabled
     */
    boolean isDebugEnabled();

    /**
     * Send a message to the user in the <b>debug</b> error level.
     *
     * @param content
     */
    void debug( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>debug</b> error level.<br>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void debug( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>debug</b> error level.<br>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void debug( Throwable error );

    /**
     * @return true if the <b>info</b> error level is enabled
     */
    boolean isInfoEnabled();

    /**
     * Send a message to the user in the <b>info</b> error level.
     *
     * @param content
     */
    void info( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>info</b> error level.<br>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void info( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>info</b> error level.<br>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void info( Throwable error );

    /**
     * @return true if the <b>warn</b> error level is enabled
     */
    boolean isWarnEnabled();

    /**
     * Send a message to the user in the <b>warn</b> error level.
     *
     * @param content
     */
    void warn( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>warn</b> error level.<br>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void warn( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>warn</b> error level.<br>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void warn( Throwable error );

    /**
     * @return true if the <b>error</b> error level is enabled
     */
    boolean isErrorEnabled();

    /**
     * Send a message to the user in the <b>error</b> error level.
     *
     * @param content
     */
    void error( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>error</b> error level.<br>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void error( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>error</b> error level.<br>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void error( Throwable error );
}
