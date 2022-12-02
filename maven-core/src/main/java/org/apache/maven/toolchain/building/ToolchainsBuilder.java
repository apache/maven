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
package org.apache.maven.toolchain.building;

/**
 * Builds the effective toolchains from a user toolchains file and/or a global toolchains file.
 *
 * @author Robert Scholte
 * @since 3.3.0
 */
public interface ToolchainsBuilder {

    /**
     * Builds the effective toolchains of the specified toolchains files.
     *
     * @param request The toolchains building request that holds the parameters, must not be {@code null}.
     * @return The result of the toolchains building, never {@code null}.
     * @throws ToolchainsBuildingException If the effective toolchains could not be built.
     */
    ToolchainsBuildingResult build(ToolchainsBuildingRequest request) throws ToolchainsBuildingException;
}
