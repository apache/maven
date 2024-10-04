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
import org.apache.maven.api.services.MessageBuilderFactory;

/**
 * Defines the contract for parsing Maven command-line arguments and creating an InvokerRequest.
 * This interface is responsible for interpreting the command-line input and constructing
 * the appropriate {@link InvokerRequest} object.
 *
 * @param <R> the type of {@link InvokerRequest} produced by this parser, extending {@link InvokerRequest}
 *
 * @since 4.0.0
 */
@Experimental
public interface Parser<R extends InvokerRequest<? extends Options>> {
    /**
     * Parses the given Maven arguments to create an InvokerRequest.
     * This is a convenience method that internally creates a ParserRequest using
     * {@link ParserRequest#mvn(String[], Logger, MessageBuilderFactory)}.
     *
     * @param args the command-line arguments
     * @param logger the logger to use during parsing
     * @param messageBuilderFactory the factory for creating message builders
     * @return the parsed InvokerRequest
     * @throws ParserException if there's an error during parsing of the command or arguments
     * @throws IOException if there's an I/O error during the parsing process
     */
    @Nonnull
    default R mvn(@Nonnull String[] args, @Nonnull Logger logger, @Nonnull MessageBuilderFactory messageBuilderFactory)
            throws ParserException, IOException {
        return parse(ParserRequest.mvn(args, logger, messageBuilderFactory).build());
    }

    /**
     * Parses the given ParserRequest to create an InvokerRequest.
     * This method is responsible for interpreting the contents of the ParserRequest
     * and constructing the appropriate InvokerRequest object.
     *
     * @param parserRequest the request containing all necessary information for parsing
     * @return the parsed InvokerRequest
     * @throws ParserException if there's an error during parsing of the request
     * @throws IOException if there's an I/O error during the parsing process
     */
    @Nonnull
    R parse(@Nonnull ParserRequest parserRequest) throws ParserException, IOException;
}
