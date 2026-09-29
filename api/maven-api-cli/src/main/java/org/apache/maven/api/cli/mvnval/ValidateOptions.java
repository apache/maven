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
package org.apache.maven.api.cli.mvnval;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;

/**
 * Options specific to the POM validation tool.
 *
 * @since 4.1.0
 */
@Experimental
public interface ValidateOptions extends Options {

    /**
     * Returns the requested output format, matched case-insensitively against {@code text} and
     * {@code json}. An unrecognised value is rejected as a usage error.
     *
     * @return an {@link Optional} holding the requested format, or empty when not given, in
     *         which case {@code text} is used
     */
    @Nonnull
    Optional<String> format();

    /**
     * Returns the POM files to validate, as given on the command line and resolved against the
     * working directory. The list is never empty; when no POM is named the {@link Optional} is
     * empty instead, and {@code ./pom.xml} is validated.
     *
     * @return an {@link Optional} holding the paths, or empty when none were given
     */
    @Nonnull
    Optional<List<String>> poms();

    /**
     * Returns how far the model builder should run, matched case-insensitively against {@code raw},
     * which stops at the raw model and reaches no repository, and {@code effective}, which resolves
     * parents and imports as well. An unrecognised value is rejected as a usage error.
     *
     * @return an {@link Optional} holding the requested mode, or empty when not given, in which
     *         case {@code effective} is used
     */
    @Nonnull
    Optional<String> mode();

    /**
     * Returns the local repository to resolve into, in place of the configured one, resolved
     * against the working directory. Without effect in {@code raw} mode, which resolves nothing.
     *
     * @return an {@link Optional} holding the path, or empty when not given
     */
    @Nonnull
    Optional<String> localRepository();

    /**
     * Returns whether to resolve into a directory created for this run and deleted when it ends, so
     * that validating a POM leaves the configured local repository untouched. Without effect in
     * {@code raw} mode, which resolves nothing.
     *
     * @return an {@link Optional} holding the choice, or empty when not given
     */
    @Nonnull
    Optional<Boolean> tempLocalRepository();
}
