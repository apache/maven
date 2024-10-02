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
package org.apache.maven.api.cli.mvn.forked;

import java.io.IOException;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvn.MavenParser;

/**
 * Defines the contract for parsing command-line arguments specific to forked Maven executions.
 * This interface extends the {@link MavenParser}, specializing it for creating {@link ForkedMavenInvokerRequest} objects.
 *
 * @since 4.0.0
 */
@Experimental
public interface ForkedMavenParser extends MavenParser<ForkedMavenInvokerRequest> {
    /**
     * Parses the given ParserRequest to create a ForkedMavenInvokerRequest.
     * This method is responsible for interpreting the contents of the ParserRequest
     * and constructing the appropriate ForkedMavenInvokerRequest object for forked Maven operations.
     *
     * @param parserRequest the request containing all necessary information for parsing
     * @return the parsed ForkedMavenInvokerRequest
     * @throws ParserException if there's an error during parsing of the request
     * @throws IOException if there's an I/O error during the parsing process
     */
    @Nonnull
    ForkedMavenInvokerRequest parse(@Nonnull ParserRequest parserRequest) throws ParserException, IOException;
}
