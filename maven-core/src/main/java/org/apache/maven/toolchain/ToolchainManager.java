package org.apache.maven.toolchain;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;


/**
 * Public API for a toolchain-aware plugin to get expected toolchain instance.
 *
 * @author mkleint
 * @author Robert Scholte
 * @since 2.0.9
 */
public interface ToolchainManager
{

    // NOTE: Some plugins like Surefire access this field directly!
    @Deprecated
    String ROLE = ToolchainManager.class.getName();

    /**
     * Retrieve toolchain of specified type from build context. It is expected that
     * <code>maven-toolchains-plugin</code> contains the configuration to select the appropriate
     * toolchain and is executed at the beginning of the build.
     *
     * @param type the type, must not be {@code null}
     * @param context the Maven session, must not be {@code null}
     * @return the toolchain selected by <code>maven-toolchains-plugin</code>
     */
    Toolchain getToolchainFromBuildContext( String type, MavenSession context );

    /**
     * Select all toolchains available in user settings matching the type and requirements,
     * independently from <code>maven-toolchains-plugin</code>.
     *
     * @param session the Maven session, must not be {@code null}
     * @param type the type, must not be {@code null}
     * @param requirements the requirements, may be {@code null}
     * @return the matching toolchains, never {@code null}
     * @since 3.3.0
     */
    List<Toolchain> getToolchains( MavenSession session, String type, Map<String, String> requirements );
}
