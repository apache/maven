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
package org.apache.maven.plugin.version;

/**
 * Resolves a version for a plugin.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public interface PluginVersionResolver {

    /**
     * Resolves the version for the specified request.
     *
     * @param request The request that holds the details about the plugin and the repositories to consult, must not be
     *            {@code null}.
     * @return The result of the version resolution, never {@code null}.
     * @throws PluginVersionResolutionException If the plugin version could not be resolved.
     */
    PluginVersionResult resolve(PluginVersionRequest request) throws PluginVersionResolutionException;
}
