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

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Builds the effective toolchains from a user toolchains file and/or an installation toolchains file.
 *
 * @since 4.0.0
 */
@Experimental
public interface ToolchainsBuilder extends Service {

    /**
     * Builds the effective toolchains for the specified toolchains files.
     *
     * @param request the toolchains building request that holds the parameters, must not be {@code null}
     * @return the result of the toolchains building, never {@code null}
     * @throws ToolchainsBuilderException if the effective toolchains could not be built
     */
    ToolchainsBuilderResult build(ToolchainsBuilderRequest request);

    /**
     * Builds the effective toolchains for the specified toolchains sources.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param installationToolchainsFile The {@link Source} pointing to the installation toolchains, must not be {@code null}
     * @param userToolchainsSource The {@link Source} pointing to the user toolchains, must not be {@code null}
     * @throws ToolchainsBuilderException if the project cannot be created
     * @throws IllegalArgumentException if an argument is {@code null} or invalid
     * @see #build(ToolchainsBuilderRequest)
     */
    @Nonnull
    default ToolchainsBuilderResult build(
            @Nonnull Session session,
            @Nonnull Source installationToolchainsFile,
            @Nonnull Source userToolchainsSource) {
        return build(ToolchainsBuilderRequest.build(session, installationToolchainsFile, userToolchainsSource));
    }
}
