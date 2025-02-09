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
import org.apache.maven.api.annotations.Nonnull;

/**
 * Defines the contract for parsing Maven command-line arguments and creating an execution or invoker requests.
 *
 * @since 4.0.0
 */
@Experimental
public interface Parser {
    /**
     * Parses the given ParserRequest to create an {@link InvokerRequest}.
     * This method does interpret tool arguments.
     *
     * @param parserRequest the request containing all necessary information for parsing
     * @return the parsed invoker request. Caller must start by checking {@link InvokerRequest#parsingFailed()} as
     * if there are parser errors, this request may not be fully processed and should immediately be failed.
     */
    @Nonnull
    InvokerRequest parseInvocation(@Nonnull ParserRequest parserRequest);
}
