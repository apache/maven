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

import java.io.IOException;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Defines the contract for parsing Maven command-line arguments and creating an execution or invoker requests.
 *
 * @since 4.0.0
 */
@Experimental
public interface Parser {
    /**
     * Parses the given ParserRequest to create an {@link ExecutorRequest}.
     * This method does not interpret tool arguments.
     *
     * @param parserRequest the request containing all necessary information for parsing
     * @return the parsed executor request
     * @throws ParserException if there's an error during parsing of the request
     * @throws IOException if there's an I/O error during the parsing process
     */
    @Nonnull
    ExecutorRequest parseExecution(@Nonnull ParserRequest parserRequest) throws ParserException, IOException;

    /**
     * Parses the given ParserRequest to create an {@link InvokerRequest}.
     * This method does interpret tool arguments.
     *
     * @param parserRequest the request containing all necessary information for parsing
     * @return the parsed invoker request
     * @throws ParserException if there's an error during parsing of the request
     * @throws IOException if there's an I/O error during the parsing process
     */
    @Nonnull
    InvokerRequest parseInvocation(@Nonnull ParserRequest parserRequest) throws ParserException, IOException;
}
