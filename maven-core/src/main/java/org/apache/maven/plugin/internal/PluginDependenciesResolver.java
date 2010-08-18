package org.apache.maven.plugin.internal;

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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;

/**
 * Assists in resolving the dependencies of a plugin. <strong>Warning:</strong> This is an internal utility interface
 * that is only public for technical reasons, it is not part of the public API. In particular, this interface can be
 * changed or deleted without prior notice.
 * 
 * @since 3.0-alpha-7
 * @author Benjamin Bentmann
 */
public interface PluginDependenciesResolver
{

    /**
     * Resolves the main artifact of the specified plugin.
     *
     * @param plugin The plugin for which to resolve the main artifact, must not be {@code null}.
     * @param request A prepopulated resolution request that will be completed and used for the resolution, must not be
     *            {@code null}.
     * @return The resolved plugin artifact, never {@code null}.
     * @throws PluginResolutionException If the plugin artifact could not be resolved.
     */
    Artifact resolve( Plugin plugin, ArtifactResolutionRequest request )
        throws PluginResolutionException;

    /**
     * Resolves the runtime dependencies of the specified plugin.
     *
     * @param plugin The plugin for which to resolve the dependencies, must not be {@code null}.
     * @param pluginArtifact The plugin's main artifact, may be {@code null}.
     * @param request A prepopulated resolution request that will be completed and used for the resolution, must not be
     *            {@code null}.
     * @param dependencyFilter A filter to exclude artifacts from resolution, may be {@code null}.
     * @return The list of artifacts denoting the resolved plugin class path, never {@code null}.
     * @throws PluginResolutionException If any dependency could not be resolved.
     */
    List<Artifact> resolve( Plugin plugin, Artifact pluginArtifact, ArtifactResolutionRequest request,
                            ArtifactFilter dependencyFilter )
        throws PluginResolutionException;

}
