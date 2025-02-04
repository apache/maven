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
package org.apache.maven.api.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service interface for managing Maven toolchains, which provide abstraction for different
 * build tools and environments.
 *
 * <p>A toolchain represents a specific build tool configuration (e.g., JDK, compiler) that can be
 * used during the Maven build process. This service allows for retrieving, storing, and managing
 * these toolchains.</p>
 *
 * @since 4.0.0
 */
@Experimental
public interface ToolchainManager extends Service {

    /**
     * Retrieves toolchains matching the specified type and requirements.
     *
     * @param session The Maven session context
     * @param type The type of toolchain (e.g., "jdk", "compiler")
     * @param requirements Key-value pairs specifying toolchain requirements (e.g., "version": "11")
     * @return List of matching toolchains, never null
     * @throws ToolchainManagerException if toolchain retrieval fails
     */
    @Nonnull
    List<Toolchain> getToolchains(@Nonnull Session session, String type, Map<String, String> requirements);

    /**
     * Retrieves all toolchains of the specified type without additional requirements.
     *
     * @param session The Maven session context
     * @param type The type of toolchain to retrieve
     * @return List of matching toolchains, never null
     * @throws ToolchainManagerException if toolchain retrieval fails
     */
    @Nonnull
    default List<Toolchain> getToolchains(@Nonnull Session session, @Nonnull String type)
            throws ToolchainManagerException {
        return getToolchains(session, type, null);
    }

    /**
     * Retrieves the currently active toolchain from the build context.
     *
     * @param session The Maven session context
     * @param type The type of toolchain to retrieve
     * @return Optional containing the toolchain if found
     * @throws ToolchainManagerException if toolchain retrieval fails
     */
    @Nonnull
    Optional<Toolchain> getToolchainFromBuildContext(@Nonnull Session session, @Nonnull String type)
            throws ToolchainManagerException;

    /**
     * Stores a toolchain in the build context for later retrieval.
     *
     * @param session The Maven session context
     * @param toolchain The toolchain to store
     * @throws ToolchainManagerException if storing the toolchain fails
     */
    void storeToolchainToBuildContext(@Nonnull Session session, @Nonnull Toolchain toolchain)
            throws ToolchainManagerException;
}
