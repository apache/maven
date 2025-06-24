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

import java.util.Optional;

import org.apache.maven.api.Toolchain;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.toolchain.ToolchainModel;

/**
 * Factory interface for creating toolchain instances from configuration models.
 *
 * <p>This factory is responsible for instantiating concrete toolchain implementations
 * based on toolchain model configurations or default settings.</p>
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface ToolchainFactory {
    /**
     * Creates a toolchain instance from the provided model configuration.
     *
     * @param model The toolchain configuration model
     * @return A configured toolchain instance
     * @throws ToolchainFactoryException if toolchain creation fails
     */
    @Nonnull
    Toolchain createToolchain(@Nonnull ToolchainModel model) throws ToolchainFactoryException;

    /**
     * Creates a default toolchain instance using system defaults.
     *
     * @return Optional containing the default toolchain if available
     * @throws ToolchainFactoryException if default toolchain creation fails
     */
    @Nonnull
    Optional<Toolchain> createDefaultToolchain() throws ToolchainFactoryException;
}
