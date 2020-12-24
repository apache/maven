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

import org.apache.maven.execution.MavenSession;

/**
 * Component for use by the <code>maven-toolchains-plugin</code> only.
 * It provides API: <ol>
 * <li>to retrieve every toolchains available in user settings,</li>
 * <li>to store chosen toolchain into build context for later use by toolchain-aware plugins.</li>
 * </ol>
 *
 * @author mkleint
 * @since 2.0.9
 * @see ToolchainManager#getToolchainFromBuildContext(String, MavenSession)
 */
public interface ToolchainManagerPrivate
{

    /**
     * Retrieves every toolchains of given type available in user settings.
     *
     * @param type the type, must not be {@code null}
     * @param context the Maven session, must not be {@code null}
     * @since 3.0 (addition of the <code>MavenSession</code> parameter)
     */
    ToolchainPrivate[] getToolchainsForType( String type, MavenSession context )
        throws MisconfiguredToolchainException;

    /**
     * Stores the toolchain into build context for later use by toolchain-aware plugins.
     *
     * @param toolchain the toolchain to store, must not be {@code null}
     * @param context the Maven session, must not be {@code null}
     * @since 2.0.9
     * @see ToolchainManager#getToolchainFromBuildContext(String, MavenSession)
     */
    void storeToolchainToBuildContext( ToolchainPrivate toolchain, MavenSession context );

}
