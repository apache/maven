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

import java.util.Map;

import org.apache.maven.api.annotations.Experimental;

/**
 * Represents a toolchain in the Maven build system.
 *
 * <p>A toolchain is a set of tools that can be used to build a project.
 * This interface allows users to define and configure various toolchains
 * that can be utilized by Maven during the build process. Toolchains can
 * include compilers, interpreters, and other tools that are necessary
 * for building a project in a specific environment.</p>
 *
 * <p>Toolchains are defined in the Maven toolchains.xml file and can be
 * referenced in the project's POM file. This allows for greater flexibility
 * and control over the build environment, enabling developers to specify
 * the exact versions of tools they wish to use.</p>
 *
 * <p>
 * Toolchains can be obtained through the {@link org.apache.maven.api.services.ToolchainManager ToolchainManager}
 * service. This service provides methods to retrieve and manage toolchains defined
 * in the Maven configuration.
 * </p>
 *
 * <p>
 * The following are key functionalities provided by the Toolchain interface:</p><ul>
 *   <li>Access to the type of the toolchain (e.g., JDK, compiler).</li>
 *   <li>Retrieval of the specific version of the toolchain.</li>
 *   <li>Configuration of toolchain properties to match the project's requirements.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * Toolchain toolchain = ...; // Obtain a Toolchain instance
 * String type = toolchain.getType(); // Get the type of the toolchain
 * String version = toolchain.getVersion(); // Get the version of the toolchain
 * </pre>
 *
 *
 * @since 4.0.0
 * @see JavaToolchain
 * @see org.apache.maven.api.services.ToolchainManager
 */
@Experimental
public interface Toolchain {
    /**
     * Gets the type of toolchain.
     *
     * @return the toolchain type
     */
    String getType();

    /**
     * Gets the platform tool executable.
     *
     * @param toolName the tool platform independent tool name
     * @return file representing the tool executable, or null if the tool cannot be found
     */
    String findTool(String toolName);

    /**
     * Let the toolchain decide if it matches requirements defined
     * in the toolchain plugin configuration.
     *
     * @param requirements key value pair, may not be {@code null}
     * @return {@code true} if the requirements match, otherwise {@code false}
     */
    boolean matchesRequirements(Map<String, String> requirements);
}
