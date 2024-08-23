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
package org.apache.maven.api;

import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * The proto session, material used to create {@link Session}.
 *
 * @since 4.0.0
 */
@Experimental
@ThreadSafe
public interface ProtoSession {

    /**
     * The Maven version.
     *
     * @return the maven version, never {@code null}
     */
    @Nonnull
    Version getMavenVersion();

    /**
     * User properties as immutable map.
     *
     * @return the user properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getUserProperties();

    /**
     * System properties as immutable map.
     *
     * @return the system properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getSystemProperties();

    /**
     * Gets the directory of the topmost project being built, usually the current directory or the
     * directory pointed at by the {@code -f/--file} command line argument.
     *
     * @return the top directory, never {@code null}.
     */
    @Nonnull
    Path getTopDirectory();

    /**
     * Gets the root directory of the session, which is the root directory for the top directory project.
     *
     * @throws IllegalStateException if the root directory could not be found
     * @see #getTopDirectory()
     * @see Project#getRootDirectory()
     */
    @Nonnull
    Path getRootDirectory();
}
