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

import org.apache.maven.api.annotations.Nonnull;

/**
 * Base options, options supported by our tools.
 */
public interface Options {
    @Nonnull
    Optional<Map<String, String>> userProperties();

    @Nonnull
    Optional<Boolean> showVersionAndExit();

    @Nonnull
    Optional<Boolean> showVersion();

    @Nonnull
    Optional<Boolean> quiet();

    @Nonnull
    Optional<Boolean> verbose();

    @Nonnull
    Optional<Boolean> showErrors();

    @Nonnull
    Optional<Boolean> nonInteractive();

    @Nonnull
    Optional<Boolean> forceInteractive();

    @Nonnull
    Optional<String> altUserSettings();

    @Nonnull
    Optional<String> altProjectSettings();

    @Nonnull
    Optional<String> altInstallationSettings();

    @Nonnull
    Optional<String> altUserToolchains();

    @Nonnull
    Optional<String> altInstallationToolchains();

    @Nonnull
    Optional<String> logFile();

    @Nonnull
    Optional<String> color();

    @Nonnull
    Optional<Boolean> help();

    /**
     * Returns new instance of {@link Options} that is result of interpolating this instance with given collection
     * of properties.
     */
    @Nonnull
    Options interpolate(Collection<Map<String, String>> properties);

    /**
     * Displays help.
     */
    void displayHelp(PrintWriter printWriter);
}
