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

import javax.inject.Inject;

import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class PluginManagerTest extends AbstractCoreMavenComponentTestCase {
    @Inject
    private DefaultBuildPluginManager pluginManager;

    protected String getProjectsDirectory() {
        return "src/test/projects/plugin-manager";
    }

    @Test
    void testPluginLoading() throws Exception {
        MavenSession session = createMavenSession(null);
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.its.plugins");
        plugin.setArtifactId("maven-it-plugin");
        plugin.setVersion("0.1");
        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(
                plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
        assertNotNull(pluginDescriptor);
    }

    @Test
    void testMojoDescriptorRetrieval() throws Exception {
        MavenSession session = createMavenSession(null);
        String goal = "it";
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.its.plugins");
        plugin.setArtifactId("maven-it-plugin");
        plugin.setVersion("0.1");

        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor(
                plugin,
                goal,
                session.getCurrentProject().getRemotePluginRepositories(),
                session.getRepositorySession());
        assertNotNull(mojoDescriptor);
        assertEquals(goal, mojoDescriptor.getGoal());
        // igorf: plugin realm comes later
        // assertNotNull( mojoDescriptor.getRealm() );

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
        assertNotNull(pluginDescriptor);
        assertEquals("org.apache.maven.its.plugins", pluginDescriptor.getGroupId());
        assertEquals("maven-it-plugin", pluginDescriptor.getArtifactId());
        assertEquals("0.1", pluginDescriptor.getVersion());
    }

    // test a build where projects use different versions of the same plugin

    @Test
    void testThatPluginDependencyThatHasSystemScopeIsResolved() throws Exception {
        MavenSession session = createMavenSession(getProject("project-contributing-system-scope-plugin-dep"));
        MavenProject project = session.getCurrentProject();
        Plugin plugin = project.getPlugin("org.apache.maven.its.plugins:maven-it-plugin");

        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setLocalRepository(getLocalRepository());
        repositoryRequest.setRemoteRepositories(getPluginArtifactRepositories());

        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(
                plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
        pluginManager.getPluginRealm(session, pluginDescriptor);
        List<Artifact> artifacts = pluginDescriptor.getArtifacts();

        for (Artifact a : artifacts) {
            if (a.getGroupId().equals("org.apache.maven.its.mng3586")
                    && a.getArtifactId().equals("tools")) {
                // The system scoped dependencies will be present in the classloader for the plugin
                return;
            }
        }

        fail("Can't find the system scoped dependency in the plugin artifacts.");
    }

    // -----------------------------------------------------------------------------------------------
    // Testing help
    // -----------------------------------------------------------------------------------------------

    protected void assertPluginDescriptor(
            MojoDescriptor mojoDescriptor, String groupId, String artifactId, String version) {
        assertNotNull(mojoDescriptor);
        PluginDescriptor pd = mojoDescriptor.getPluginDescriptor();
        assertNotNull(pd);
        assertEquals(groupId, pd.getGroupId());
        assertEquals(artifactId, pd.getArtifactId());
        assertEquals(version, pd.getVersion());
    }

    @Test
    void testPluginRealmCache() throws Exception {
        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setLocalRepository(getLocalRepository());
        repositoryRequest.setRemoteRepositories(getPluginArtifactRepositories());

        // prime realm cache
        MavenSession session = createMavenSession(getProject("project-contributing-system-scope-plugin-dep"));
        MavenProject project = session.getCurrentProject();
        Plugin plugin = project.getPlugin("org.apache.maven.its.plugins:maven-it-plugin");

        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(
                plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
        pluginManager.getPluginRealm(session, pluginDescriptor);

        assertEquals(1, pluginDescriptor.getDependencies().size());

        for (ComponentDescriptor<?> descriptor : pluginDescriptor.getComponents()) {
            assertNotNull(descriptor.getRealm());
            assertNotNull(descriptor.getImplementationClass());
        }

        // reload plugin realm from cache
        session = createMavenSession(getProject("project-contributing-system-scope-plugin-dep"));
        project = session.getCurrentProject();
        plugin = project.getPlugin("org.apache.maven.its.plugins:maven-it-plugin");

        pluginDescriptor = pluginManager.loadPlugin(
                plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
        pluginManager.getPluginRealm(session, pluginDescriptor);

        assertEquals(1, pluginDescriptor.getDependencies().size());

        for (ComponentDescriptor<?> descriptor : pluginDescriptor.getComponents()) {
            assertNotNull(descriptor.getRealm());
            assertNotNull(descriptor.getImplementationClass());
        }
    }

    @Test
    void testBuildExtensionsPluginLoading() throws Exception {
        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setLocalRepository(getLocalRepository());
        repositoryRequest.setRemoteRepositories(getPluginArtifactRepositories());

        // prime realm cache
        MavenSession session = createMavenSession(getProject("project-with-build-extensions-plugin"));
        MavenProject project = session.getCurrentProject();
        Plugin plugin = project.getPlugin("org.apache.maven.its.plugins:maven-it-plugin");

        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(
                plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
        ClassRealm pluginRealm = pluginManager.getPluginRealm(session, pluginDescriptor);

        assertEquals(pluginRealm, pluginDescriptor.getComponents().get(0).getRealm());
    }
}
