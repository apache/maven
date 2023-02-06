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
package org.apache.maven.plugin;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Provides basic services to manage Maven plugins and their mojos. This component is kept general in its design such
 * that the plugins/mojos can be used in arbitrary contexts. In particular, the mojos can be used for ordinary build
 * plugins as well as special purpose plugins like reports.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public interface MavenPluginManager {

    /**
     * Retrieves the descriptor for the specified plugin from its main artifact.
     *
     * @param plugin The plugin whose descriptor should be retrieved, must not be {@code null}.
     * @param repositories The plugin repositories to use for resolving the plugin's main artifact, must not be {@code
     *            null}.
     * @param session The repository session to use for resolving the plugin's main artifact, must not be {@code null}.
     * @return The plugin descriptor, never {@code null}.
     */
    PluginDescriptor getPluginDescriptor(
            Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException;

    /**
     * Retrieves the descriptor for the specified plugin goal from the plugin's main artifact.
     *
     * @param plugin The plugin whose mojo descriptor should be retrieved, must not be {@code null}.
     * @param goal The simple name of the mojo whose descriptor should be retrieved, must not be {@code null}.
     * @param repositories The plugin repositories to use for resolving the plugin's main artifact, must not be {@code
     *            null}.
     * @param session The repository session to use for resolving the plugin's main artifact, must not be {@code null}.
     * @return The mojo descriptor, never {@code null}.
     */
    MojoDescriptor getMojoDescriptor(
            Plugin plugin, String goal, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws MojoNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    InvalidPluginDescriptorException;

    /**
     * Verifies the specified plugin is compatible with the current Maven runtime.
     *
     * @param pluginDescriptor The descriptor of the plugin to check, must not be {@code null}.
     */
    void checkRequiredMavenVersion(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException;

    /**
     * Sets up the class realm for the specified plugin. Both the class realm and the plugin artifacts that constitute
     * it will be stored in the plugin descriptor.
     *
     * @param pluginDescriptor The plugin descriptor in which to save the class realm and the plugin artifacts, must not
     *            be {@code null}.
     * @param session The build session from which to pick the current project and repository settings, must not be
     *            {@code null}.
     * @param parent The parent class realm for the plugin, may be {@code null} to use the Maven core realm.
     * @param imports The packages/types to import from the parent realm, may be {@code null}.
     * @param filter The filter used to exclude certain plugin dependencies, may be {@code null}.
     */
    void setupPluginRealm(
            PluginDescriptor pluginDescriptor,
            MavenSession session,
            ClassLoader parent,
            List<String> imports,
            DependencyFilter filter)
            throws PluginResolutionException, PluginContainerException;

    /**
     * Sets up class realm for the specified build extensions plugin.
     *
     * @since 3.3.0
     */
    ExtensionRealmCache.CacheRecord setupExtensionsRealm(
            MavenProject project, Plugin plugin, RepositorySystemSession session) throws PluginManagerException;

    /**
     * Looks up the mojo for the specified mojo execution and populates its parameters from the configuration given by
     * the mojo execution. The mojo/plugin descriptor associated with the mojo execution provides the class realm to
     * lookup the mojo from. <strong>Warning:</strong> The returned mojo instance must be released via
     * {@link #releaseMojo(Object, MojoExecution)} when the mojo is no longer needed to free any resources allocated for
     * it.
     *
     * @param mojoInterface The component role of the mojo, must not be {@code null}.
     * @param session The build session in whose context the mojo will be used, must not be {@code null}.
     * @param mojoExecution The mojo execution to retrieve the mojo for, must not be {@code null}.
     * @return The ready-to-execute mojo, never {@code null}.
     */
    <T> T getConfiguredMojo(Class<T> mojoInterface, MavenSession session, MojoExecution mojoExecution)
            throws PluginConfigurationException, PluginContainerException;

    /**
     * Releases the specified mojo back to the container.
     *
     * @param mojo The mojo to release, may be {@code null}.
     * @param mojoExecution The mojo execution the mojo was originally retrieved for, must not be {@code null}.
     */
    void releaseMojo(Object mojo, MojoExecution mojoExecution);
}
