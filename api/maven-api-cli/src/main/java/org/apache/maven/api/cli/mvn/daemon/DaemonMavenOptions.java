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
package org.apache.maven.api.cli.mvn.daemon;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.mvn.MavenOptions;

/**
 * Daemon options.
 */
@Experimental
public interface DaemonMavenOptions extends MavenOptions {
    /**
     * Indicates whether raw streams should be used with daemon.
     *
     * @return an {@link Optional} containing true if raw streams are enabled, false if disabled, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> rawStreams();

    /**
     * Returns a new instance of {@link MavenOptions} with values interpolated using the given properties.
     *
     * @param properties a collection of property maps to use for interpolation
     * @return a new MavenOptions instance with interpolated values
     */
    @Nonnull
    DaemonMavenOptions interpolate(@Nonnull Collection<Map<String, String>> properties);
}
