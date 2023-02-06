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
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

public class PluginManagerTest extends AbstractCoreMavenComponentTestCase {
    @Requirement
    private DefaultBuildPluginManager pluginManager;

    protected void setUp() throws Exception {
        super.setUp();
        pluginManager = (DefaultBuildPluginManager) lookup(BuildPluginManager.class);
    }

    @Override
    protected void tearDown() throws Exception {
        pluginManager = null;
        super.tearDown();
    }

    protected String getProjectsDirectory() {
        return "src/test/projects/plugin-manager";
    }

    public void testPluginLoading() throws Exception {
        MavenSession session = createMavenSession(null);
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.its.plugins");
        plugin.setArtifactId("maven-it-plugin");
        plugin.setVersion("0.1");
        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(
                plugin, session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
        assertNotNull(pluginDescriptor);
    }

    public void testMojoDescriptorRetrieval() throws Exception {
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

    // -----------------------------------------------------------------------------------------------
    // Tests which exercise the lifecycle executor when it is dealing with individual goals.
    // -----------------------------------------------------------------------------------------------

    // TODO These two tests display a lack of symmetry with respect to the input which is a free form string and the
    //      mojo descriptor which comes back. All the free form parsing needs to be done somewhere else, this is
    //      really the function of the CLI, and then the pre-processing of that output still needs to be fed into
    //      a hinting process which helps flesh out the full specification of the plugin. The plugin manager should
    //      only deal in concrete terms -- all version finding mumbo jumbo is a customization to base functionality
    //      the plugin manager provides.

    public void testRemoteResourcesPlugin() throws Exception {
        // TODO turn an equivalent back on when the RR plugin is released.

        /*

        This will not work until the RR plugin is released to get rid of the binding to the reporting exception which is a mistake.

        This happens after removing the reporting API from the core:

        java.lang.NoClassDefFoundError: org/apache/maven/reporting/MavenReportException

        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );
        String goal = "process";

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-remote-resources-plugin" );
        plugin.setVersion( "1.0-beta-2" );

        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session.getCurrentProject(), session.getLocalRepository() );
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-remote-resources-plugin", "1.0-beta-2" );
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
        pluginManager.executeMojo( session, mojoExecution );
        */
    }

    // TODO this will be the basis of the customizable lifecycle execution so need to figure this out quickly.
    public void testSurefirePlugin() throws Exception {
        /*
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );
        String goal = "test";

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-surefire-plugin" );
        plugin.setVersion( "2.4.2" );

        // The project has already been fully interpolated so getting the raw mojoDescriptor is not going to have the processes configuration.
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session.getLocalRepository(), session.getCurrentProject().getPluginArtifactRepositories() );
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-surefire-plugin", "2.4.2" );

        System.out.println( session.getCurrentProject().getBuild().getPluginsAsMap() );

        Xpp3Dom configuration = (Xpp3Dom) session.getCurrentProject().getBuild().getPluginsAsMap().get( plugin.getKey() ).getExecutions().get( 0 ).getConfiguration();
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, configuration );
        pluginManager.executeMojo( session, mojoExecution );
        */
    }

    public void testMojoConfigurationIsMergedCorrectly() throws Exception {}

    /**
     * The case where the user wants to specify an alternate version of the underlying tool. Common case
     * is in the Antlr plugin which comes bundled with a version of Antlr but the user often times needs
     * to use a specific version. We need to make sure the version that they specify takes precedence.
     */
    public void testMojoWhereInternallyStatedDependencyIsOverriddenByProject() throws Exception {}

    /**
     * The case where you have a plugin in the current build that you want to be used on projects in
     * the current build.
     */
    public void testMojoThatIsPresentInTheCurrentBuild() throws Exception {}

    /**
     * This is the case where the Mojo wants to execute on every project and then do something at the end
     * with the results of each project.
     */
    public void testAggregatorMojo() throws Exception {}

    /**
     * This is the case where a Mojo needs the lifecycle run to a certain phase before it can do
     * anything useful.
     */
    public void testMojoThatRequiresExecutionToAGivenPhaseBeforeExecutingItself() throws Exception {}

    // test that mojo which does not require dependency resolution trigger no downloading of dependencies

    // test interpolation of basedir values in mojo configuration

    // test a build where projects use different versions of the same plugin

    public void testThatPluginDependencyThatHasSystemScopeIsResolved() throws Exception {
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

    public void testPluginRealmCache() throws Exception {
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

    public void testBuildExtensionsPluginLoading() throws Exception {
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
