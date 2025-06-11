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
package org.apache.maven.api.cli.mvnup;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;

/**
 * Defines the options specific to the Maven upgrade tool.
 * This interface extends the general {@link Options} interface, adding upgrade-specific configuration options.
 *
 * @since 4.0.0
 */
@Experimental
public interface UpgradeOptions extends Options {
    /**
     * Should the operation be forced (ie overwrite existing files, if any).
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    Optional<Boolean> force();

    /**
     * Should imply "yes" to all questions.
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    Optional<Boolean> yes();

    /**
     * Returns the list of upgrade goals to be executed.
     * These goals can include operations like "check", "dependencies", "plugins", etc.
     *
     * @return an {@link Optional} containing the list of goals, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> goals();

    /**
     * Returns the target POM model version for upgrades.
     * Supported values include "4.0.0" and "4.1.0".
     *
     * @return an {@link Optional} containing the model version, or empty if not specified
     */
    @Nonnull
    Optional<String> modelVersion();

    /**
     * Returns the directory to use as starting point for POM discovery.
     * If not specified, the current directory will be used.
     *
     * @return an {@link Optional} containing the directory path, or empty if not specified
     */
    @Nonnull
    Optional<String> directory();

    /**
     * Should use inference when upgrading (remove redundant information).
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    @Nonnull
    Optional<Boolean> infer();

    /**
     * Should fix Maven 4 compatibility issues in POMs.
     * This includes fixing unsupported combine attributes, duplicate dependencies,
     * unsupported expressions, and other Maven 4 validation issues.
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    @Nonnull
    Optional<Boolean> model();

    /**
     * Should upgrade plugins known to fail with Maven 4 to their minimum compatible versions.
     * This includes upgrading plugins like maven-exec-plugin, maven-enforcer-plugin,
     * flatten-maven-plugin, and maven-shade-plugin to versions that work with Maven 4.
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    @Nonnull
    Optional<Boolean> plugins();

    /**
     * Should apply all upgrade options (equivalent to --model-version 4.1.0 --infer --model --plugins).
     * This is a convenience option that combines model upgrade, inference, compatibility fixes, and plugin upgrades.
     *
     * @return an {@link Optional} containing the boolean value {@code true} if specified, or empty
     */
    @Nonnull
    Optional<Boolean> all();
}
