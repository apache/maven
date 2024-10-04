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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Represents the base options supported by Maven tools.
 * This interface defines methods to access various configuration options
 * that can be set through command-line arguments or configuration files.
 *
 * @since 4.0.0
 */
@Experimental
public interface Options {
    /** Constant indicating that the options source is the command-line interface. */
    String SOURCE_CLI = "CLI";

    /**
     * Returns a simple designator of the options source, such as "cli", "maven.conf", etc.
     *
     * @return a string representing the source of the options
     */
    @Nonnull
    String source();

    /**
     * Returns the user-defined properties for the Maven execution.
     *
     * @return an {@link Optional} containing the map of user properties, or empty if not set
     */
    @Nonnull
    Optional<Map<String, String>> userProperties();

    /**
     * Indicates whether to show the version information and exit.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> showVersionAndExit();

    /**
     * Indicates whether to show the version information.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> showVersion();

    /**
     * Indicates whether to run in quiet mode.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> quiet();

    /**
     * Indicates whether to run in verbose mode.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> verbose();

    /**
     * Indicates whether to show error messages.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> showErrors();

    /**
     * Returns the severity level at which the build should fail.
     *
     * @return an {@link Optional} containing the fail-on-severity string, or empty if not set
     */
    @Nonnull
    Optional<String> failOnSeverity();

    /**
     * Indicates whether to run in non-interactive mode.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> nonInteractive();

    /**
     * Indicates whether to force interactive mode.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> forceInteractive();

    /**
     * Returns the path to an alternate user settings file.
     *
     * @return an {@link Optional} containing the file path, or empty if not set
     */
    @Nonnull
    Optional<String> altUserSettings();

    /**
     * Returns the path to an alternate project settings file.
     *
     * @return an {@link Optional} containing the file path, or empty if not set
     */
    @Nonnull
    Optional<String> altProjectSettings();

    /**
     * Returns the path to an alternate installation settings file.
     *
     * @return an {@link Optional} containing the file path, or empty if not set
     */
    @Nonnull
    Optional<String> altInstallationSettings();

    /**
     * Returns the path to an alternate user toolchains file.
     *
     * @return an {@link Optional} containing the file path, or empty if not set
     */
    @Nonnull
    Optional<String> altUserToolchains();

    /**
     * Returns the path to an alternate installation toolchains file.
     *
     * @return an {@link Optional} containing the file path, or empty if not set
     */
    @Nonnull
    Optional<String> altInstallationToolchains();

    /**
     * Returns the path to the log file.
     *
     * @return an {@link Optional} containing the file path, or empty if not set
     */
    @Nonnull
    Optional<String> logFile();

    /**
     * Returns the color setting for console output.
     *
     * @return an {@link Optional} containing the color setting, or empty if not set
     */
    @Nonnull
    Optional<String> color();

    /**
     * Indicates whether to show help information.
     *
     * @return an {@link Optional} containing the boolean flag, or empty if not set
     */
    @Nonnull
    Optional<Boolean> help();

    /**
     * Returns a new instance of {@link Options} with values interpolated using the given properties.
     *
     * @param properties a collection of property maps to use for interpolation
     * @return a new {@link Options} instance with interpolated values
     */
    @Nonnull
    Options interpolate(@Nonnull Collection<Map<String, String>> properties);

    /**
     * Emits warning messages if deprecated options are used.
     *
     * @param printWriter the PrintWriter to use for output
     */
    default void warnAboutDeprecatedOptions(@Nonnull ParserRequest request, @Nonnull PrintWriter printWriter) {}

    /**
     * Displays help information for these options.
     *
     * @param printWriter the PrintWriter to use for output
     */
    void displayHelp(@Nonnull ParserRequest request, @Nonnull PrintWriter printWriter);
}
