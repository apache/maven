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
package org.apache.maven.api.build.spi;

import java.io.Serializable;
import java.util.Objects;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.build.Severity;

@Experimental
@ThreadSafe
@Immutable
public class Message implements Serializable {

    private static final long serialVersionUID = 7798138299696868415L;

    private final int line;
    private final int column;
    private final String message;
    private final Severity severity;
    private final Throwable cause;
    private final int hashCode;

    public Message(int line, int column, String message, Severity severity, Throwable cause) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.severity = severity;
        this.cause = cause;
        this.hashCode = Objects.hash(line, column, message, severity, cause);
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

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

        if (!(obj instanceof Message)) {
            return false;
        }

        Message other = (Message) obj;

        return line == other.line
                && column == other.column
                && Objects.equals(message, other.message)
                && Objects.equals(severity, other.severity)
                && Objects.equals(cause, other.cause);
    }
}
