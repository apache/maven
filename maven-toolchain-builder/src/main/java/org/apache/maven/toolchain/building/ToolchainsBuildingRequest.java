package org.apache.maven.toolchain.building;

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

import org.apache.maven.building.Source;

/**
 * Collects toolchains that control the building of effective toolchains.
 *
 * @author Robert Scholte
 * @since 3.3.0
 */
public interface ToolchainsBuildingRequest
{

    /**
     * Gets the global toolchains source.
     *
     * @return The global toolchains source or {@code null} if none.
     */
    Source getGlobalToolchainsSource();

    /**
     * Sets the global toolchains source. If both user toolchains and a global toolchains are given, the user toolchains
     * take precedence.
     *
     * @param globalToolchainsSource The global toolchains source, may be {@code null} to disable global toolchains.
     * @return This request, never {@code null}.
     */
    ToolchainsBuildingRequest setGlobalToolchainsSource( Source globalToolchainsSource );

    /**
     * Gets the user toolchains source.
     *
     * @return The user toolchains source or {@code null} if none.
     */
    Source getUserToolchainsSource();

    /**
     * Sets the user toolchains source. If both user toolchains and a global toolchains are given, the user toolchains
     * take precedence.
     *
     * @param userToolchainsSource The user toolchains source, may be {@code null} to disable user toolchains.
     * @return This request, never {@code null}.
     */
    ToolchainsBuildingRequest setUserToolchainsSource( Source userToolchainsSource );
}
