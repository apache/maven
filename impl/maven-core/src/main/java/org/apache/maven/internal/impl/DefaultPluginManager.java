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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Objects;

import org.apache.maven.api.Plugin;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.PluginManager;
import org.apache.maven.api.services.PluginManagerException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecyclePluginResolver;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.internal.impl.CoreUtils.cast;

@Named
@Singleton
public final class DefaultPluginManager implements PluginManager {

    private final MavenPluginManager pluginManager;
    private final LifecyclePluginResolver lifecyclePluginResolver;
    private final Lookup lookup;

    @Inject
    public DefaultPluginManager(
            MavenPluginManager pluginManager, LifecyclePluginResolver lifecyclePluginResolver, Lookup lookup) {
        this.pluginManager = requireNonNull(pluginManager, "pluginManager");
        this.lifecyclePluginResolver = requireNonNull(lifecyclePluginResolver, "lifecyclePluginResolver");
        this.lookup = requireNonNull(lookup, "lookup");
    }

    @Override
    public Plugin loadPlugin(Session session, Project project, org.apache.maven.api.model.Plugin plugin) {
        requireNonNull(session, "session");
        requireNonNull(project, "project");
        requireNonNull(plugin, "plugin");

        InternalMavenSession internalSession = InternalMavenSession.from(session);
        MavenProject mavenProject =
                cast(DefaultProject.class, project, "project").getProject();
        MavenSession executionSession = internalSession.getMavenSession().clone();
        executionSession.setCurrentProject(mavenProject);

        try {
            lifecyclePluginResolver.resolveMissingPluginVersions(mavenProject, executionSession);
            org.apache.maven.model.Plugin legacyPlugin = findPlugin(mavenProject, plugin);
            PluginDescriptor descriptor = pluginManager.getPluginDescriptor(
                    legacyPlugin, mavenProject.getRemotePluginRepositories(), executionSession.getRepositorySession());
            pluginManager.setupPluginRealm(descriptor, executionSession, null, null, null);
            return new DefaultPlugin(internalSession, legacyPlugin, descriptor);
        } catch (Exception e) {
            throw new PluginManagerException(
                    "Unable to load plugin " + plugin.getGroupId() + ":" + plugin.getArtifactId(), e);
        }
    }

    private org.apache.maven.model.Plugin findPlugin(
            MavenProject project, org.apache.maven.api.model.Plugin requestedPlugin) {
        return project.getBuildPlugins().stream()
                .filter(candidate -> Objects.equals(candidate.getGroupId(), requestedPlugin.getGroupId())
                        && Objects.equals(candidate.getArtifactId(), requestedPlugin.getArtifactId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plugin " + requestedPlugin.getGroupId() + ":"
                        + requestedPlugin.getArtifactId() + " is not declared by project " + project.getId()));
    }

    @Override
    public <T> List<T> lookupList(Plugin plugin, Class<T> type) {
        requireNonNull(plugin, "plugin");
        requireNonNull(type, "type");

        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(plugin.getClassLoader());
            return List.copyOf(lookup.lookupList(type));
        } finally {
            thread.setContextClassLoader(originalClassLoader);
        }
    }
}
