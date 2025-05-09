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
package org.apache.maven.api.spi;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.services.MavenException;

@Experimental
public class ModelParserException extends MavenException {

    /**
     * The one-based index of the line containing the error.
     */
    private final int lineNumber;

    /**
     * The one-based index of the column containing the error.
     */
    private final int columnNumber;

    public ModelParserException() {
        this(null, null);
    }

    public ModelParserException(String message) {
        this(message, null);
    }

    public ModelParserException(String message, Throwable cause) {
        this(message, -1, -1, cause);
    }

    public ModelParserException(String message, int lineNumber, int columnNumber, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public ModelParserException(Throwable cause) {
        this(null, cause);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}
