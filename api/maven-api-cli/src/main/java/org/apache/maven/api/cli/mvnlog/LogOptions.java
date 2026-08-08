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
import org.apache.maven.api.cli.Options;

/**
 * Defines the options specific to the Maven build log viewer tool ({@code mvnlog}).
 * This interface extends the general {@link Options} interface, adding log-viewing options.
 *
 * @since 4.1.0
 */
@Experimental
public interface LogOptions extends Options {
    /**
     * Whether to show detailed diagnostics (warnings and errors) from the build.
     *
     * @return an {@link Optional} containing {@code true} if diagnostics should be shown
     */
    Optional<Boolean> diagnostics();

    /**
     * Whether to show detailed failure information including stack traces.
     *
     * @return an {@link Optional} containing {@code true} if failures should be shown in detail
     */
    Optional<Boolean> failures();

    /**
     * Whether to show a full per-mojo timing breakdown.
     *
     * @return an {@link Optional} containing {@code true} if the full breakdown should be shown
     */
    Optional<Boolean> full();

    /**
     * Whether to list all available build reports instead of showing one.
     *
     * @return an {@link Optional} containing {@code true} if reports should be listed
     */
    Optional<Boolean> list();

    /**
     * Whether to output the raw JSON build report instead of formatted text.
     * Useful for piping to tools like {@code jq} or for programmatic consumption.
     *
     * @return an {@link Optional} containing {@code true} if raw JSON should be output
     */
    Optional<Boolean> json();

    /**
     * Returns the path to a specific build report file to display.
     * If not specified, defaults to {@code target/build-reports/build-report-latest.json}.
     *
     * @return an {@link Optional} containing the report file path, or empty if not specified
     */
    Optional<String> reportFile();
}
