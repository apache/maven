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
 * Service to manage {@link Toolchain}s.
 *
 * @since 4.0.0
 */
@Experimental
public interface ToolchainManager extends Service {

    /**
     * The type identifying JDK toolchains
     */
    String TYPE_JDK = "jdk";

    /**
     *
     * @param session
     * @param type
     * @param requirements
     * @return the selected {@link Toolchain}s
     * @throws ToolchainManagerException if an exception occurs
     */
    @Nonnull
    List<Toolchain> getToolchains(@Nonnull Session session, String type, Map<String, String> requirements);

    /**
     *
     * @param session
     * @param type
     * @return the selected {@link Toolchain}
     * @throws ToolchainManagerException if an exception occurs
     */
    @Nonnull
    Optional<Toolchain> getToolchainFromBuildContext(@Nonnull Session session, String type)
            throws ToolchainManagerException;

    /**
     *
     * @param session
     * @param type
     * @return the selected {@link Toolchain}s
     * @throws ToolchainManagerException if an exception occurs
     */
    @Nonnull
    List<Toolchain> getToolchainsForType(@Nonnull Session session, String type) throws ToolchainManagerException;

    /**
     *
     * @param session
     * @param toolchain
     * @throws ToolchainManagerException if an exception occurs
     */
    void storeToolchainToBuildContext(@Nonnull Session session, Toolchain toolchain) throws ToolchainManagerException;
}
