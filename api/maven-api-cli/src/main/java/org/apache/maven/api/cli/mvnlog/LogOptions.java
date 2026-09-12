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
package org.apache.maven.api.cli.mvnlog;

import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;

/**
 * Defines the options specific to the Maven build log viewer tool.
 *
 * @since 4.0.0
 */
@Experimental
public interface LogOptions extends Options {
    /**
     * Should start the local HTTP server for interactive build report viewing.
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    @Nonnull
    Optional<Boolean> web();

    /**
     * Returns the port on which to start the local HTTP server.
     *
     * @return an {@link Optional} containing the port value, or empty if not specified
     */
    @Nonnull
    Optional<Integer> port();

    /**
     * Returns the specific build report file to view.
     *
     * @return an {@link Optional} containing the file path, or empty if not specified
     */
    @Nonnull
    Optional<String> file();
}
