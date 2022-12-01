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
package org.apache.maven.settings.io;

import java.io.IOException;

/**
 * Signals a failure to parse the settings due to invalid syntax (e.g. non-wellformed XML or unknown elements).
 *
 * @author Benjamin Bentmann
 */
public class SettingsParseException extends IOException {

    /**
     * The one-based index of the line containing the error.
     */
    private final int lineNumber;

    /**
     * The one-based index of the column containing the error.
     */
    private final int columnNumber;

    /**
     * Creates a new parser exception with the specified details.
     *
     * @param message The error message, may be {@code null}.
     * @param lineNumber The one-based index of the line containing the error or {@code -1} if unknown.
     * @param columnNumber The one-based index of the column containing the error or {@code -1} if unknown.
     */
    public SettingsParseException(String message, int lineNumber, int columnNumber) {
        super(message);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Creates a new parser exception with the specified details.
     *
     * @param message The error message, may be {@code null}.
     * @param lineNumber The one-based index of the line containing the error or {@code -1} if unknown.
     * @param columnNumber The one-based index of the column containing the error or {@code -1} if unknown.
     * @param cause The nested cause of this error, may be {@code null}.
     */
    public SettingsParseException(String message, int lineNumber, int columnNumber, Throwable cause) {
        super(message);
        initCause(cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Gets the one-based index of the line containing the error.
     *
     * @return The one-based index of the line containing the error or a non-positive value if unknown.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets the one-based index of the column containing the error.
     *
     * @return The one-based index of the column containing the error or non-positive value if unknown.
     */
    public int getColumnNumber() {
        return columnNumber;
    }
}
