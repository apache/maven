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
package org.apache.maven.api.cli;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.services.MavenException;

/**
 * Represents an exception that occurs during the parsing of Maven command-line arguments.
 * This exception is typically thrown when there are user errors in the command-line input,
 * such as invalid arguments or references to missing files. When this exception is thrown,
 * it indicates that the Maven execution should be stopped and the user should correct the issue.
 *
 * @since 4.0.0
 */
@Experimental
public class ParserException extends MavenException {
    /**
     * Constructs a new ParserException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ParserException(String message) {
        super(message);
    }

    /**
     * Constructs a new ParserException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of the exception
     */
    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
