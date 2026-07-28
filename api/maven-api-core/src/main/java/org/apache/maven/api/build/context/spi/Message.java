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
package org.apache.maven.api.build.context.spi;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.build.context.Severity;

/**
 * An immutable diagnostic message attached to a build resource.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 7798138299696868415L;

    private final int line;
    private final int column;
    private final String message;
    private final Severity severity;
    private final Throwable cause;
    private final int hashCode;

    /**
     * Creates a new message.
     *
     * @param line     the 1-based line number, or {@code 0} if unknown
     * @param column   the 1-based column number, or {@code 0} if unknown
     * @param message  the human-readable message text
     * @param severity the severity level
     * @param cause    the underlying cause, or {@code null}
     */
    public Message(
            int line, int column, @Nonnull String message, @Nonnull Severity severity, @Nullable Throwable cause) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.severity = severity;
        this.cause = cause;
        this.hashCode = Objects.hash(line, column, message, severity, cause);
    }

    /**
     * {@return the 1-based line number, or {@code 0} if unknown}
     */
    public int getLine() {
        return line;
    }

    /**
     * {@return the 1-based column number, or {@code 0} if unknown}
     */
    public int getColumn() {
        return column;
    }

    /**
     * {@return the human-readable message text}
     */
    @Nonnull
    public String getMessage() {
        return message;
    }

    /**
     * {@return the severity level}
     */
    @Nonnull
    public Severity getSeverity() {
        return severity;
    }

    /**
     * {@return the underlying cause, or {@code null}}
     */
    @Nullable
    public Throwable getCause() {
        return cause;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Message other)) {
            return false;
        }
        return line == other.line
                && column == other.column
                && Objects.equals(message, other.message)
                && Objects.equals(severity, other.severity)
                && Objects.equals(cause, other.cause);
    }
}
