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
package org.apache.maven.plugin.internal;

import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.prefix.PluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Benjamin Bentmann
 */
@Component(role = PluginManager.class)
public class DefaultPluginManager implements PluginManager {

    @Requirement
    private PlexusContainer container;

    @Requirement
    private MavenPluginManager pluginManager;

    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    @Requirement
    private PluginPrefixResolver pluginPrefixResolver;

    @Requirement
    private LegacySupport legacySupport;

    public void executeMojo(MavenProject project, MojoExecution execution, MavenSession session)
            throws MojoExecutionException, ArtifactResolutionException, MojoFailureException, ArtifactNotFoundException,
                    InvalidDependencyVersionException, PluginManagerException, PluginConfigurationException {
        throw new UnsupportedOperationException();
    }

    public Object getPluginComponent(Plugin plugin, String role, String roleHint)
            throws PluginManagerException, ComponentLookupException {
        MavenSession session = legacySupport.getSession();

        PluginDescriptor pluginDescriptor;
        try {
            pluginDescriptor = pluginManager.getPluginDescriptor(
                    plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());

            pluginManager.setupPluginRealm(pluginDescriptor, session, null, null, null);
        } catch (Exception e) {
            throw new PluginManagerException(plugin, e.getMessage(), e);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginDescriptor.getClassRealm());

            return container.lookup(role, roleHint);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public Map<String, Object> getPluginComponents(Plugin plugin, String role)
            throws ComponentLookupException, PluginManagerException {
        MavenSession session = legacySupport.getSession();

        PluginDescriptor pluginDescriptor;
        try {
            pluginDescriptor = pluginManager.getPluginDescriptor(
                    plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());

            pluginManager.setupPluginRealm(pluginDescriptor, session, null, null, null);
        } catch (Exception e) {
            throw new PluginManagerException(plugin, e.getMessage(), e);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginDescriptor.getClassRealm());

            return container.lookupMap(role);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public Plugin getPluginDefinitionForPrefix(String prefix, MavenSession session, MavenProject project) {
        PluginPrefixRequest request = new DefaultPluginPrefixRequest(prefix, session);
        request.setPom(project.getModel());

        try {
            PluginPrefixResult result = pluginPrefixResolver.resolve(request);

            Plugin plugin = new Plugin();
            plugin.setGroupId(result.getGroupId());
            plugin.setArtifactId(result.getArtifactId());

            return plugin;
        } catch (NoPluginFoundForPrefixException e) {
            return null;
        }
    }

    public PluginDescriptor getPluginDescriptorForPrefix(String prefix) {
        MavenSession session = legacySupport.getSession();

        PluginPrefixRequest request = new DefaultPluginPrefixRequest(prefix, session);

        try {
            PluginPrefixResult result = pluginPrefixResolver.resolve(request);

            Plugin plugin = new Plugin();
            plugin.setGroupId(result.getGroupId());
            plugin.setArtifactId(result.getArtifactId());

            return loadPluginDescriptor(plugin, session.getCurrentProject(), session);
        } catch (Exception e) {
            return null;
        }
    }

    public PluginDescriptor loadPluginDescriptor(Plugin plugin, MavenProject project, MavenSession session)
            throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
                    InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException,
                    PluginNotFoundException, PluginVersionNotFoundException {
        return verifyPlugin(plugin, project, session.getSettings(), session.getLocalRepository());
    }

    public PluginDescriptor loadPluginFully(Plugin plugin, MavenProject project, MavenSession session)
            throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
                    InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException,
                    PluginNotFoundException, PluginVersionNotFoundException {
        PluginDescriptor pluginDescriptor = loadPluginDescriptor(plugin, project, session);

        try {
            pluginManager.setupPluginRealm(pluginDescriptor, session, null, null, null);
        } catch (PluginResolutionException e) {
            throw new PluginManagerException(plugin, e.getMessage(), e);
        }

        return pluginDescriptor;
    }

    public PluginDescriptor verifyPlugin(
            Plugin plugin, MavenProject project, Settings settings, ArtifactRepository localRepository)
            throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
                    InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException,
                    PluginNotFoundException, PluginVersionNotFoundException {
        MavenSession session = legacySupport.getSession();

        if (plugin.getVersion() == null) {
            PluginVersionRequest versionRequest = new DefaultPluginVersionRequest(
                    plugin, session.getRepositorySession(), project.getRemotePluginRepositories());
            plugin.setVersion(pluginVersionResolver.resolve(versionRequest).getVersion());
        }

        try {
            return pluginManager.getPluginDescriptor(
                    plugin, project.getRemotePluginRepositories(), session.getRepositorySession());
        } catch (PluginResolutionException e) {
            throw new PluginNotFoundException(plugin, project.getPluginArtifactRepositories());
        } catch (PluginDescriptorParsingException | InvalidPluginDescriptorException e) {
            throw new PluginManagerException(plugin, e.getMessage(), e);
        }
    }
}
