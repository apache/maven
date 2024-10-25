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
package org.apache.maven.internal.impl;

import org.apache.maven.api.services.BuilderProblem;

/**
 * Describes a problem that was encountered during settings building. A problem can either be an exception that was
 * thrown or a simple string message. In addition, a problem carries a hint about its source, e.g. the settings file
 * that exhibits the problem.
 */
class DefaultBuilderProblem implements BuilderProblem {
    final String source;
    final int lineNumber;
    final int columnNumber;
    final Exception exception;
    final String message;
    final Severity severity;

    DefaultBuilderProblem(
            String source, int lineNumber, int columnNumber, Exception exception, String message, Severity severity) {
        this.source = source;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.exception = exception;
        this.message = message;
        this.severity = severity;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String getLocation() {
        StringBuilder buffer = new StringBuilder(256);
        if (getSource() != null && !getSource().isEmpty()) {
            buffer.append(getSource());
        }
        if (getLineNumber() > 0) {
            if (!buffer.isEmpty()) {
                buffer.append(", ");
            }
            buffer.append("line ").append(getLineNumber());
        }
        if (getColumnNumber() > 0) {
            if (!buffer.isEmpty()) {
                buffer.append(", ");
            }
            buffer.append("column ").append(getColumnNumber());
        }
        return buffer.toString();
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append('[').append(severity).append("]");
        String msg = message != null ? message : exception != null ? exception.getMessage() : null;
        if (msg != null && !msg.isEmpty()) {
            buffer.append(" ").append(msg);
        }
        String location = getLocation();
        if (!location.isEmpty()) {
            buffer.append(" @ ").append(location);
        }
        return buffer.toString();
    }
}
