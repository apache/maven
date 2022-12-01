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
package org.apache.maven.building;

/**
 * Describes a problem that was encountered during settings building. A problem can either be an exception that was
 * thrown or a simple string message. In addition, a problem carries a hint about its source, e.g. the settings file
 * that exhibits the problem.
 *
 * @author Benjamin Bentmann
 * @author Robert Scholte
 */
class DefaultProblem implements Problem {

    private final String source;

    private final int lineNumber;

    private final int columnNumber;

    private final String message;

    private final Exception exception;

    private final Severity severity;

    /**
     * Creates a new problem with the specified message and exception.
     * Either {@code message} or {@code exception} is required
     *
     * @param message The message describing the problem, may be {@code null}.
     * @param severity The severity level of the problem, may be {@code null} to default to
     *            {@link SettingsProblem.Severity#ERROR}.
     * @param source A hint about the source of the problem like a file path, may be {@code null}.
     * @param lineNumber The one-based index of the line containing the problem or {@code -1} if unknown.
     * @param columnNumber The one-based index of the column containing the problem or {@code -1} if unknown.
     * @param exception The exception that caused this problem, may be {@code null}.
     */
    DefaultProblem(
            String message, Severity severity, String source, int lineNumber, int columnNumber, Exception exception) {
        this.message = message;
        this.severity = (severity != null) ? severity : Severity.ERROR;
        this.source = (source != null) ? source : "";
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.exception = exception;
    }

    public String getSource() {
        return source;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getLocation() {
        StringBuilder buffer = new StringBuilder(256);

        if (getSource().length() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(getSource());
        }

        if (getLineNumber() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("line ").append(getLineNumber());
        }

        if (getColumnNumber() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("column ").append(getColumnNumber());
        }

        return buffer.toString();
    }

    public Exception getException() {
        return exception;
    }

    public String getMessage() {
        String msg;

        if (message != null && message.length() > 0) {
            msg = message;
        } else {
            msg = exception.getMessage();

            if (msg == null) {
                msg = "";
            }
        }

        return msg;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);

        buffer.append('[').append(getSeverity()).append("] ");
        buffer.append(getMessage());
        buffer.append(" @ ").append(getLocation());

        return buffer.toString();
    }
}
