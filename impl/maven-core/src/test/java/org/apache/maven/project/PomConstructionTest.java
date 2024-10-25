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
package org.apache.maven.project;

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.MavenTestHelper;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.harness.PomTestWrapper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
class PomConstructionTest {
    private static String BASE_DIR = "src/test";

    private static String BASE_POM_DIR = BASE_DIR + "/resources-project-builder";

    private static String BASE_MIXIN_DIR = BASE_DIR + "/resources-mixins";

    @Inject
    private DefaultProjectBuilder projectBuilder;

    @Inject
    private MavenRepositorySystem repositorySystem;

    @Inject
    private PlexusContainer container;

    private File testDirectory;

    @BeforeEach
    void setUp() throws Exception {
        testDirectory = new File(getBasedir(), BASE_POM_DIR);
        new File(getBasedir(), BASE_MIXIN_DIR);
        EmptyLifecycleBindingsInjector.useEmpty();
    }

    /**
     * Will throw exception if url is empty. MNG-4050
     *
     * @throws Exception in case of issue
     */
    @Test
    void testEmptyUrl() throws Exception {
        buildPom("empty-distMng-repo-url");
    }

    /**
     * Tests that modules is not overridden by profile
     *
     * @throws Exception in case of issue
     */
    /* MNG-786*/
    @Test
    void testProfileModules() throws Exception {
        PomTestWrapper pom = buildPom("profile-module", "a");
        assertEquals("test-prop", pom.getValue("properties[1]/b")); // verifies profile applied
        assertEquals(4, ((List<?>) pom.getValue("modules")).size());
        assertEquals("module-2", pom.getValue("modules[1]"));
        assertEquals("module-1", pom.getValue("modules[2]"));
        assertEquals("module-3", pom.getValue("modules[3]"));
        assertEquals("module-4", pom.getValue("modules[4]"));
    }

    /**
     * Will throw an exception if it doesn't find parent(s) in build
     *
     * @throws Exception in case of issue
     */
    @Test
    void testParentInheritance() throws Exception {
        buildPom("parent-inheritance/sub");
    }

    /*MNG-3995*/
    @Test
    void testExecutionConfigurationJoin() throws Exception {
        PomTestWrapper pom = buildPom("execution-configuration-join");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/configuration[1]/fileset[1]")).size());
    }

    /*MNG-3803*/
    @Test
    void testPluginConfigProperties() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-properties");
        assertEquals(
                "my.property", pom.getValue("build/plugins[1]/configuration[1]/systemProperties[1]/property[1]/name"));
    }

    /*MNG-3900*/
    @Test
    void testProfilePropertiesInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("profile-properties-interpolation", "interpolation-profile");
        assertEquals("PASSED", pom.getValue("properties[1]/test"));
        assertEquals("PASSED", pom.getValue("properties[1]/property"));
    }

    /*MNG-7750*/
    private void checkBuildPluginWithArtifactId(
            List<Plugin> plugins, String artifactId, String expectedId, String expectedConfig) {
        Plugin plugin = plugins.stream()
                .filter(p -> p.getArtifactId().equals(artifactId))
                .findFirst()
                .orElse(null);
        assertNotNull(plugin, "Unable to find plugin with artifactId: " + artifactId);
        List<PluginExecution> pluginExecutions = plugin.getExecutions();
        PluginExecution pluginExecution = pluginExecutions.stream()
                .filter(pe -> pe.getId().equals(expectedId))
                .findFirst()
                .orElse(null);
        assertNotNull(pluginExecution, "Wrong id for \"" + artifactId + "\"");

        String config = pluginExecution.getConfiguration().toString();
        assertTrue(
                config.contains(expectedConfig),
                "Wrong config for \"" + artifactId + "\": (" + config + ") does not contain :" + expectedConfig);
    }

    private boolean isActiveProfile(MavenProject project, Profile activeProfile) {
        return project.getActiveProfiles().stream().anyMatch(p -> p.getId().equals(activeProfile.getId()));
    }

    @Test
    void testBuildPluginInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("plugin-interpolation-build", "activeProfile");
        Model originalModel = pom.getMavenProject().getOriginalModel();

        // =============================================
        assertEquals("||${project.basedir}||", originalModel.getProperties().get("prop-outside"));

        List<Plugin> outsidePlugins = originalModel.getBuild().getPlugins();
        assertEquals(1, outsidePlugins.size());

        checkBuildPluginWithArtifactId(
                outsidePlugins,
                "plugin-all-profiles",
                "Outside ||${project.basedir}||",
                "<plugin-all-profiles-out>Outside ||${project.basedir}||</plugin-all-profiles-out>");

        // =============================================
        Profile activeProfile = originalModel.getProfiles().stream()
                .filter(profile -> profile.getId().equals("activeProfile"))
                .findFirst()
                .orElse(null);
        assertNotNull(activeProfile, "Unable to find the activeProfile");

        assertTrue(
                isActiveProfile(pom.getMavenProject(), activeProfile),
                "The activeProfile should be active in the maven project");

        assertEquals("||${project.basedir}||", activeProfile.getProperties().get("prop-active"));

        List<Plugin> activeProfilePlugins = activeProfile.getBuild().getPlugins();
        assertEquals(2, activeProfilePlugins.size(), "Number of active profile plugins");

        checkBuildPluginWithArtifactId(
                activeProfilePlugins,
                "plugin-all-profiles",
                "Active all ||${project.basedir}||",
                "<plugin-all-profiles-in>Active all ||${project.basedir}||</plugin-all-profiles-in>");

        checkBuildPluginWithArtifactId(
                activeProfilePlugins,
                "only-active-profile",
                "Active only ||${project.basedir}||",
                "<plugin-in-active-profile-only>Active only ||${project.basedir}||</plugin-in-active-profile-only>");

        // =============================================

        Profile inactiveProfile = originalModel.getProfiles().stream()
                .filter(profile -> profile.getId().equals("inactiveProfile"))
                .findFirst()
                .orElse(null);
        assertNotNull(inactiveProfile, "Unable to find the inactiveProfile");

        assertFalse(
                isActiveProfile(pom.getMavenProject(), inactiveProfile),
                "The inactiveProfile should NOT be active in the maven project");

        assertEquals("||${project.basedir}||", inactiveProfile.getProperties().get("prop-inactive"));

        List<Plugin> inactiveProfilePlugins = inactiveProfile.getBuild().getPlugins();
        assertEquals(2, inactiveProfilePlugins.size(), "Number of active profile plugins");

        checkBuildPluginWithArtifactId(
                inactiveProfilePlugins,
                "plugin-all-profiles",
                "Inactive all ||${project.basedir}||",
                "<plugin-all-profiles-ina>Inactive all ||${project.basedir}||</plugin-all-profiles-ina>");

        checkBuildPluginWithArtifactId(
                inactiveProfilePlugins,
                "only-inactive-profile",
                "Inactive only ||${project.basedir}||",
                "<plugin-in-inactive-only>Inactive only ||${project.basedir}||</plugin-in-inactive-only>");
    }

    private void checkReportPluginWithArtifactId(
            List<ReportPlugin> plugins, String artifactId, String expectedId, String expectedConfig) {
        ReportPlugin plugin = plugins.stream()
                .filter(p -> p.getArtifactId().equals(artifactId))
                .findFirst()
                .orElse(null);
        assertNotNull(plugin, "Unable to find plugin with artifactId: " + artifactId);
        List<ReportSet> pluginReportSets = plugin.getReportSets();
        ReportSet reportSet = pluginReportSets.stream()
                .filter(rs -> rs.getId().equals(expectedId))
                .findFirst()
                .orElse(null);
        assertNotNull(reportSet, "Wrong id for \"" + artifactId + "\"");

        String config = reportSet.getConfiguration().toString();
        assertTrue(
                config.contains(expectedConfig),
                "Wrong config for \"" + artifactId + "\": (" + config + ") does not contain :" + expectedConfig);
    }

    @Test
    void testReportingPluginInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("plugin-interpolation-reporting", "activeProfile");
        Model originalModel = pom.getMavenProject().getOriginalModel();

        // =============================================
        assertEquals("||${project.basedir}||", originalModel.getProperties().get("prop-outside"));

        List<ReportPlugin> outsidePlugins = originalModel.getReporting().getPlugins();
        assertEquals(1, outsidePlugins.size(), "Wrong number of plugins found");

        checkReportPluginWithArtifactId(
                outsidePlugins,
                "plugin-all-profiles",
                "Outside ||${project.basedir}||",
                "<plugin-all-profiles-out>Outside ||${project.basedir}||</plugin-all-profiles-out>");

        // =============================================
        Profile activeProfile = originalModel.getProfiles().stream()
                .filter(profile -> profile.getId().equals("activeProfile"))
                .findFirst()
                .orElse(null);
        assertNotNull(activeProfile, "Unable to find the activeProfile");

        assertTrue(
                isActiveProfile(pom.getMavenProject(), activeProfile),
                "The activeProfile should be active in the maven project");

        assertEquals("||${project.basedir}||", activeProfile.getProperties().get("prop-active"));

        List<ReportPlugin> activeProfilePlugins = activeProfile.getReporting().getPlugins();
        assertEquals(2, activeProfilePlugins.size(), "The activeProfile should be active in the maven project");

        checkReportPluginWithArtifactId(
                activeProfilePlugins,
                "plugin-all-profiles",
                "Active all ||${project.basedir}||",
                "<plugin-all-profiles-in>Active all ||${project.basedir}||</plugin-all-profiles-in>");

        checkReportPluginWithArtifactId(
                activeProfilePlugins,
                "only-active-profile",
                "Active only ||${project.basedir}||",
                "<plugin-in-active-profile-only>Active only ||${project.basedir}||</plugin-in-active-profile-only>");

        // =============================================

        Profile inactiveProfile = originalModel.getProfiles().stream()
                .filter(profile -> profile.getId().equals("inactiveProfile"))
                .findFirst()
                .orElse(null);
        assertNotNull(inactiveProfile, "Unable to find the inactiveProfile");

        assertFalse(
                isActiveProfile(pom.getMavenProject(), inactiveProfile),
                "The inactiveProfile should NOT be active in the maven project");

        assertEquals("||${project.basedir}||", inactiveProfile.getProperties().get("prop-inactive"));

        List<ReportPlugin> inactiveProfilePlugins =
                inactiveProfile.getReporting().getPlugins();
        assertEquals(2, inactiveProfilePlugins.size(), "Number of active profile plugins");

        checkReportPluginWithArtifactId(
                inactiveProfilePlugins,
                "plugin-all-profiles",
                "Inactive all ||${project.basedir}||",
                "<plugin-all-profiles-ina>Inactive all ||${project.basedir}||</plugin-all-profiles-ina>");

        checkReportPluginWithArtifactId(
                inactiveProfilePlugins,
                "only-inactive-profile",
                "Inactive only ||${project.basedir}||",
                "<plugin-in-inactive-only>Inactive only ||${project.basedir}||</plugin-in-inactive-only>");
    }

    // Some better conventions for the test poms needs to be created and each of these tests
    // that represent a verification of a specification item needs to be a couple lines at most.
    // The expressions help a lot, but we need a clean to pick up a directory of POMs, automatically load
    // them into a resolver, create the expression to extract the data to validate the Model, and the URI
    // to validate the properties. We also need a way to navigate from the Tex specification documents to
    // the test in question and vice versa. A little Eclipse plugin would do the trick.
    public void testThatExecutionsWithoutIdsAreMergedAndTheChildWins() throws Exception {
        PomTestWrapper tester = buildPom("micromailer");
        assertModelEquals(tester, "child-descriptor", "build/plugins[1]/executions[1]/goals[1]");
    }

    /*MNG-
    public void testDependencyScope()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "dependency-scope/sub" );

    }*/

    /*MNG- 4010*/
    @Test
    void testDuplicateExclusionsDependency() throws Exception {
        PomTestWrapper pom = buildPom("duplicate-exclusions-dependency/sub");
        assertEquals(1, ((List<?>) pom.getValue("dependencies[1]/exclusions")).size());
    }

    /*MNG- 4008*/
    @Test
    void testMultipleFilters() throws Exception {
        PomTestWrapper pom = buildPom("multiple-filters");
        assertEquals(4, ((List<?>) pom.getValue("build/filters")).size());
    }

    /* MNG-4005: postponed to 3.1
    public void testValidationErrorUponNonUniqueDependencyKey()
        throws Exception
    {
        try
        {
            buildPom( "unique-dependency-key/deps" );
            fail( "Non-unique dependency keys did not cause validation error" );
        }
        catch ( ProjectBuildingException e )
        {
            // expected
        }
    }

    @Test
    public void testValidationErrorUponNonUniqueDependencyManagementKey()
        throws Exception
    {
        try
        {
            buildPom( "unique-dependency-key/dep-mgmt" );
            fail( "Non-unique dependency keys did not cause validation error" );
        }
        catch ( ProjectBuildingException e )
        {
            // expected
        }
    }

    @Test
    public void testValidationErrorUponNonUniqueDependencyKeyInProfile()
        throws Exception
    {
        try
        {
            buildPom( "unique-dependency-key/deps-in-profile" );
            fail( "Non-unique dependency keys did not cause validation error" );
        }
        catch ( ProjectBuildingException e )
        {
            // expected
        }
    }

    @Test
    public void testValidationErrorUponNonUniqueDependencyManagementKeyInProfile()
        throws Exception
    {
        try
        {
            buildPom( "unique-dependency-key/dep-mgmt-in-profile" );
            fail( "Non-unique dependency keys did not cause validation error" );
        }
        catch ( ProjectBuildingException e )
        {
            // expected
        }
    }
    */

    @Test
    void testDuplicateDependenciesCauseLastDeclarationToBePickedInLenientMode() throws Exception {
        PomTestWrapper pom = buildPom("unique-dependency-key/deps", true, null, null);
        assertEquals(1, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("0.2", pom.getValue("dependencies[1]/version"));
    }

    /* MNG-3567*/
    @Test
    void testParentInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("parent-interpolation/sub");
        pom = new PomTestWrapper(pom.getMavenProject().getParent());
        assertEquals("1.3.0-SNAPSHOT", pom.getValue("build/plugins[1]/version"));
    }

    /*
    public void testMaven()
        throws Exception
    {
        PomTestWrapper pom =  buildPomFromMavenProject( "maven-build/sub/pom.xml", null );

        for( String s: pom.getMavenProject().getTestClasspathElements() )
        {
            System.out.println( s );
        }

    }
    */

    /* MNG-3567*/
    @Test
    void testPluginManagementInherited() throws Exception {
        PomTestWrapper pom = buildPom("pluginmanagement-inherited/sub");
        assertEquals("1.0-alpha-21", pom.getValue("build/plugins[1]/version"));
    }

    /* MNG-2174*/
    @Test
    void testPluginManagementDependencies() throws Exception {
        PomTestWrapper pom = buildPom("plugin-management-dependencies/sub", "test");
        assertEquals("1.0-alpha-21", pom.getValue("build/plugins[1]/version"));
        assertEquals("1.0", pom.getValue("build/plugins[1]/dependencies[1]/version"));
    }

    /* MNG-3877*/
    @Test
    void testReportingInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("reporting-interpolation");
        assertEquals(
                createPath(Arrays.asList(
                        System.getProperty("user.dir"),
                        "src",
                        "test",
                        "resources-project-builder",
                        "reporting-interpolation",
                        "target",
                        "site")),
                pom.getValue("reporting/outputDirectory"));
    }

    @Test
    void testPluginOrder() throws Exception {
        PomTestWrapper pom = buildPom("plugin-order");
        assertEquals("plexus-component-metadata", pom.getValue("build/plugins[1]/artifactId"));
        assertEquals("maven-surefire-plugin", pom.getValue("build/plugins[2]/artifactId"));
    }

    @Test
    void testErroneousJoiningOfDifferentPluginsWithEqualDependencies() throws Exception {
        PomTestWrapper pom = buildPom("equal-plugin-deps");
        assertEquals("maven-it-plugin-a", pom.getValue("build/plugins[1]/artifactId"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/dependencies")).size());
        assertEquals("maven-it-plugin-b", pom.getValue("build/plugins[2]/artifactId"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/dependencies")).size());
    }

    /** MNG-3821 */
    @Test
    void testErroneousJoiningOfDifferentPluginsWithEqualExecutionIds() throws Exception {
        PomTestWrapper pom = buildPom("equal-plugin-exec-ids");
        assertEquals("maven-it-plugin-a", pom.getValue("build/plugins[1]/artifactId"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("maven-it-plugin-b", pom.getValue("build/plugins[2]/artifactId"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("maven-it-plugin-a", pom.getValue("reporting/plugins[1]/artifactId"));
        assertEquals(1, ((List<?>) pom.getValue("reporting/plugins[1]/reportSets")).size());
        assertEquals("maven-it-plugin-b", pom.getValue("reporting/plugins[2]/artifactId"));
        assertEquals(1, ((List<?>) pom.getValue("reporting/plugins[1]/reportSets")).size());
    }

    /** MNG-3998 */
    @Test
    void testExecutionConfiguration() throws Exception {
        PomTestWrapper pom = buildPom("execution-configuration");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("src/main/mdo/nexus.xml", (pom.getValue("build/plugins[1]/executions[1]/configuration[1]/model")));
        assertEquals(
                "src/main/mdo/security.xml", (pom.getValue("build/plugins[1]/executions[2]/configuration[1]/model")));
    }

    /*
        public void testPluginConfigDuplicate()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-duplicate/dup" );
    }
    */
    @Test
    void testSingleConfigurationInheritance() throws Exception {
        PomTestWrapper pom = buildPom("single-configuration-inheritance");

        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/configuration[1]/rules")).size());
        assertEquals(
                "2.0.6",
                pom.getValue(
                        "build/plugins[1]/executions[1]/configuration[1]/rules[1]/requireMavenVersion[1]/version"));
        assertEquals(
                "[1.4,)",
                pom.getValue("build/plugins[1]/executions[1]/configuration[1]/rules[1]/requireJavaVersion[1]/version"));
    }

    @Test
    void testConfigWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("config-with-plugin-mng");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals(
                "src/main/mdo/security.xml", pom.getValue("build/plugins[1]/executions[2]/configuration[1]/model"));
        assertEquals("1.0.8", pom.getValue("build/plugins[1]/executions[1]/configuration[1]/version"));
    }

    /** MNG-3965 */
    @Test
    void testExecutionConfigurationSubcollections() throws Exception {
        PomTestWrapper pom = buildPom("execution-configuration-subcollections");
        assertEquals(
                2,
                ((List<?>) pom.getValue("build/plugins[1]/executions[1]/configuration[1]/rules[1]/bannedDependencies"))
                        .size());
    }

    /** MNG-3985 */
    @Test
    void testMultipleRepositories() throws Exception {
        PomTestWrapper pom = buildPom("multiple-repos/sub");
        assertEquals(2, ((List<?>) pom.getValue("repositories")).size());
    }

    /** MNG-3965 */
    @Test
    void testMultipleExecutionIds() throws Exception {
        PomTestWrapper pom = buildPom("dual-execution-ids/sub");
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
    }

    /** MNG-3997 */
    @Test
    void testConsecutiveEmptyElements() throws Exception {
        buildPom("consecutive_empty_elements");
    }

    @Test
    void testOrderOfGoalsFromPluginExecutionWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-goals-order/wo-plugin-mgmt");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/goals")).size());
        assertEquals("b", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("a", pom.getValue("build/plugins[1]/executions[1]/goals[2]"));
        assertEquals("d", pom.getValue("build/plugins[1]/executions[1]/goals[3]"));
        assertEquals("c", pom.getValue("build/plugins[1]/executions[1]/goals[4]"));
        assertEquals("e", pom.getValue("build/plugins[1]/executions[1]/goals[5]"));
    }

    /* MNG-3886*/
    @Test
    void testOrderOfGoalsFromPluginExecutionWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-goals-order/w-plugin-mgmt");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/goals")).size());
        assertEquals("b", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("a", pom.getValue("build/plugins[1]/executions[1]/goals[2]"));
        assertEquals("d", pom.getValue("build/plugins[1]/executions[1]/goals[3]"));
        assertEquals("c", pom.getValue("build/plugins[1]/executions[1]/goals[4]"));
        assertEquals("e", pom.getValue("build/plugins[1]/executions[1]/goals[5]"));
    }

    @Test
    void testOrderOfPluginExecutionsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-order/wo-plugin-mgmt");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("b", pom.getValue("build/plugins[1]/executions[1]/id"));
        assertEquals("a", pom.getValue("build/plugins[1]/executions[2]/id"));
        assertEquals("d", pom.getValue("build/plugins[1]/executions[3]/id"));
        assertEquals("c", pom.getValue("build/plugins[1]/executions[4]/id"));
        assertEquals("e", pom.getValue("build/plugins[1]/executions[5]/id"));
    }

    /* MNG-3887 */
    @Test
    void testOrderOfPluginExecutionsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-order/w-plugin-mgmt");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("b", pom.getValue("build/plugins[1]/executions[1]/id"));
        assertEquals("a", pom.getValue("build/plugins[1]/executions[2]/id"));
        assertEquals("d", pom.getValue("build/plugins[1]/executions[3]/id"));
        assertEquals("c", pom.getValue("build/plugins[1]/executions[4]/id"));
        assertEquals("e", pom.getValue("build/plugins[1]/executions[5]/id"));
    }

    @Test
    void testMergeOfPluginExecutionsWhenChildInheritsPluginVersion() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-merging-wo-version/sub");
        assertEquals(4, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
    }

    /* MNG-3943*/
    @Test
    void testMergeOfPluginExecutionsWhenChildAndParentUseDifferentPluginVersions() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-merging-version-insensitive/sub");
        assertEquals(4, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
    }

    @Test
    void testInterpolationWithXmlMarkup() throws Exception {
        PomTestWrapper pom = buildPom("xml-markup-interpolation");
        assertEquals("<?xml version='1.0'?>Tom&Jerry", pom.getValue("properties/xmlTest"));
    }

    /* MNG-3925 */
    @Test
    void testOrderOfMergedPluginExecutionsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("merged-plugin-exec-order/wo-plugin-mgmt/sub");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("parent-1", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("parent-2", pom.getValue("build/plugins[1]/executions[2]/goals[1]"));
        assertEquals("child-default", pom.getValue("build/plugins[1]/executions[3]/goals[1]"));
        assertEquals("child-1", pom.getValue("build/plugins[1]/executions[4]/goals[1]"));
        assertEquals("child-2", pom.getValue("build/plugins[1]/executions[5]/goals[1]"));
    }

    @Test
    void testOrderOfMergedPluginExecutionsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("merged-plugin-exec-order/w-plugin-mgmt/sub");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("parent-1", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("parent-2", pom.getValue("build/plugins[1]/executions[2]/goals[1]"));
        assertEquals("child-default", pom.getValue("build/plugins[1]/executions[3]/goals[1]"));
        assertEquals("child-1", pom.getValue("build/plugins[1]/executions[4]/goals[1]"));
        assertEquals("child-2", pom.getValue("build/plugins[1]/executions[5]/goals[1]"));
    }

    /* MNG-3984*/
    @Test
    void testDifferentContainersWithSameId() throws Exception {
        PomTestWrapper pom = buildPom("join-different-containers-same-id");
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/goals")).size());
        assertEquals(
                1,
                ((List<?>) pom.getValue(
                                "build/pluginManagement/plugins[@artifactId='maven-it-plugin-b']/executions[1]/goals"))
                        .size());
    }

    /* MNG-3937*/
    @Test
    void testOrderOfMergedPluginExecutionGoalsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("merged-plugin-exec-goals-order/wo-plugin-mgmt/sub");

        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/goals")).size());
        assertEquals("child-a", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("merged", pom.getValue("build/plugins[1]/executions[1]/goals[2]"));
        assertEquals("child-b", pom.getValue("build/plugins[1]/executions[1]/goals[3]"));
        assertEquals("parent-b", pom.getValue("build/plugins[1]/executions[1]/goals[4]"));
        assertEquals("parent-a", pom.getValue("build/plugins[1]/executions[1]/goals[5]"));
    }

    @Test
    void testOrderOfMergedPluginExecutionGoalsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("merged-plugin-exec-goals-order/w-plugin-mgmt/sub");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/goals")).size());
        assertEquals("child-a", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("merged", pom.getValue("build/plugins[1]/executions[1]/goals[2]"));
        assertEquals("child-b", pom.getValue("build/plugins[1]/executions[1]/goals[3]"));
        assertEquals("parent-b", pom.getValue("build/plugins[1]/executions[1]/goals[4]"));
        assertEquals("parent-a", pom.getValue("build/plugins[1]/executions[1]/goals[5]"));
    }

    /*MNG-3938*/
    @Test
    void testOverridingOfInheritedPluginExecutionsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-merging/wo-plugin-mgmt/sub");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("child-default", pom.getValue("build/plugins[1]/executions[@id='default']/phase"));
        assertEquals("child-non-default", pom.getValue("build/plugins[1]/executions[@id='non-default']/phase"));
    }

    /* MNG-3938 */
    @Test
    void testOverridingOfInheritedPluginExecutionsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-merging/w-plugin-mgmt/sub");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("child-default", pom.getValue("build/plugins[1]/executions[@id='default']/phase"));
        assertEquals("child-non-default", pom.getValue("build/plugins[1]/executions[@id='non-default']/phase"));
    }

    /* MNG-3906*/
    @Test
    void testOrderOfMergedPluginDependenciesWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("merged-plugin-class-path-order/wo-plugin-mgmt/sub");

        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/dependencies")).size());
        assertNotNull(pom.getValue("build/plugins[1]/dependencies[1]"));
        assertEquals("c", pom.getValue("build/plugins[1]/dependencies[1]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[1]/version"));
        assertEquals("a", pom.getValue("build/plugins[1]/dependencies[2]/artifactId"));
        assertEquals("2", pom.getValue("build/plugins[1]/dependencies[2]/version"));
        assertEquals("b", pom.getValue("build/plugins[1]/dependencies[3]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[3]/version"));
        assertEquals("e", pom.getValue("build/plugins[1]/dependencies[4]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[4]/version"));
        assertEquals("d", pom.getValue("build/plugins[1]/dependencies[5]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[5]/version"));
    }

    @Test
    void testOrderOfMergedPluginDependenciesWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("merged-plugin-class-path-order/w-plugin-mgmt/sub");
        assertEquals(5, ((List<?>) pom.getValue("build/plugins[1]/dependencies")).size());
        assertEquals("c", pom.getValue("build/plugins[1]/dependencies[1]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[1]/version"));
        assertEquals("a", pom.getValue("build/plugins[1]/dependencies[2]/artifactId"));
        assertEquals("2", pom.getValue("build/plugins[1]/dependencies[2]/version"));
        assertEquals("b", pom.getValue("build/plugins[1]/dependencies[3]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[3]/version"));
        assertEquals("e", pom.getValue("build/plugins[1]/dependencies[4]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[4]/version"));
        assertEquals("d", pom.getValue("build/plugins[1]/dependencies[5]/artifactId"));
        assertEquals("1", pom.getValue("build/plugins[1]/dependencies[5]/version"));
    }

    @Test
    void testInterpolationOfNestedBuildDirectories() throws Exception {
        PomTestWrapper pom = buildPom("nested-build-dir-interpolation");
        assertEquals(
                new File(pom.getBasedir(), "target/classes/dir0"), new File((String) pom.getValue("properties/dir0")));
        assertEquals(new File(pom.getBasedir(), "src/test/dir1"), new File((String) pom.getValue("properties/dir1")));
        assertEquals(
                new File(pom.getBasedir(), "target/site/dir2"), new File((String) pom.getValue("properties/dir2")));
    }

    @Test
    void testAppendArtifactIdOfChildToInheritedUrls() throws Exception {
        PomTestWrapper pom = buildPom("url-inheritance/sub");
        assertEquals("https://parent.url/child", pom.getValue("url"));
        assertEquals("https://parent.url/org", pom.getValue("organization/url"));
        assertEquals("https://parent.url/license.txt", pom.getValue("licenses[1]/url"));
        assertEquals("https://parent.url/viewvc/child", pom.getValue("scm/url"));
        assertEquals("https://parent.url/scm/child", pom.getValue("scm/connection"));
        assertEquals("https://parent.url/scm/child", pom.getValue("scm/developerConnection"));
        assertEquals("https://parent.url/issues", pom.getValue("issueManagement/url"));
        assertEquals("https://parent.url/ci", pom.getValue("ciManagement/url"));
        assertEquals("https://parent.url/dist", pom.getValue("distributionManagement/repository/url"));
        assertEquals("https://parent.url/snaps", pom.getValue("distributionManagement/snapshotRepository/url"));
        assertEquals("https://parent.url/site/child", pom.getValue("distributionManagement/site/url"));
        assertEquals("https://parent.url/download", pom.getValue("distributionManagement/downloadUrl"));
    }

    /* MNG-3846*/
    @Test
    void testAppendArtifactIdOfParentAndChildToInheritedUrls() throws Exception {
        PomTestWrapper pom = buildPom("url-inheritance/another-parent/sub");
        assertEquals("https://parent.url/ap/child", pom.getValue("url"));
        assertEquals("https://parent.url/org", pom.getValue("organization/url"));
        assertEquals("https://parent.url/license.txt", pom.getValue("licenses[1]/url"));
        assertEquals("https://parent.url/viewvc/ap/child", pom.getValue("scm/url"));
        assertEquals("https://parent.url/scm/ap/child", pom.getValue("scm/connection"));
        assertEquals("https://parent.url/scm/ap/child", pom.getValue("scm/developerConnection"));
        assertEquals("https://parent.url/issues", pom.getValue("issueManagement/url"));
        assertEquals("https://parent.url/ci", pom.getValue("ciManagement/url"));
        assertEquals("https://parent.url/dist", pom.getValue("distributionManagement/repository/url"));
        assertEquals("https://parent.url/snaps", pom.getValue("distributionManagement/snapshotRepository/url"));
        assertEquals("https://parent.url/site/ap/child", pom.getValue("distributionManagement/site/url"));
        assertEquals("https://parent.url/download", pom.getValue("distributionManagement/downloadUrl"));
    }

    @Test
    void testNonInheritedElementsInSubtreesOverriddenByChild() throws Exception {
        PomTestWrapper pom = buildPom("limited-inheritance/child");
        assertNull(pom.getValue("organization/url"));
        assertNull(pom.getValue("issueManagement/system"));
        assertEquals(0, ((List<?>) pom.getValue("ciManagement/notifiers")).size());
        assertEquals("child-distros", pom.getValue("distributionManagement/repository/id"));
        assertEquals("ssh://child.url/distros", pom.getValue("distributionManagement/repository/url"));
        assertNull(pom.getValue("distributionManagement/repository/name"));
        assertEquals(true, pom.getValue("distributionManagement/repository/uniqueVersion"));
        assertEquals("default", pom.getValue("distributionManagement/repository/layout"));
        assertEquals("child-snaps", pom.getValue("distributionManagement/snapshotRepository/id"));
        assertEquals("ssh://child.url/snaps", pom.getValue("distributionManagement/snapshotRepository/url"));
        assertNull(pom.getValue("distributionManagement/snapshotRepository/name"));
        assertEquals(true, pom.getValue("distributionManagement/snapshotRepository/uniqueVersion"));
        assertEquals("default", pom.getValue("distributionManagement/snapshotRepository/layout"));
        assertEquals("child-site", pom.getValue("distributionManagement/site/id"));
        assertEquals("scp://child.url/site", pom.getValue("distributionManagement/site/url"));
        assertNull(pom.getValue("distributionManagement/site/name"));
    }

    @Test
    void testXmlTextCoalescing() throws Exception {
        PomTestWrapper pom = buildPom("xml-coalesce-text");
        assertEquals("A  Test  Project Property", pom.getValue("properties/prop0"));
        assertEquals("That's a test!", pom.getValue("properties/prop1"));
        assertEquals(
                32 * 1024,
                pom.getValue("properties/prop2")
                        .toString()
                        .trim()
                        .replaceAll("[\n\r]", "")
                        .length());
    }

    @Test
    void testFullInterpolationOfNestedExpressions() throws Exception {
        PomTestWrapper pom = buildPom("full-interpolation");
        for (int i = 0; i < 24; i++) {
            String index = ((i < 10) ? "0" : "") + i;
            assertEquals("PASSED", pom.getValue("properties/property" + index));
        }
    }

    @Test
    void testInterpolationWithBasedirAlignedDirectories() throws Exception {
        PomTestWrapper pom = buildPom("basedir-aligned-interpolation");
        assertEquals(
                new File(pom.getBasedir(), "src/main/java"),
                new File(pom.getValue("properties/buildMainSrc").toString()));
        assertEquals(
                new File(pom.getBasedir(), "src/test/java"),
                new File(pom.getValue("properties/buildTestSrc").toString()));
        assertEquals(
                new File(pom.getBasedir(), "src/main/scripts"),
                new File(pom.getValue("properties/buildScriptSrc").toString()));
        assertEquals(
                new File(pom.getBasedir(), "target"),
                new File(pom.getValue("properties/buildOut").toString()));
        assertEquals(
                new File(pom.getBasedir(), "target/classes"),
                new File(pom.getValue("properties/buildMainOut").toString()));
        assertEquals(
                new File(pom.getBasedir(), "target/test-classes"),
                new File(pom.getValue("properties/buildTestOut").toString()));
        assertEquals(
                new File(pom.getBasedir(), "target/site"),
                new File(pom.getValue("properties/siteOut").toString()));
    }

    /* MNG-3944*/
    @Test
    void testInterpolationOfBasedirInPomWithUnusualName() throws Exception {
        PomTestWrapper pom = buildPom("basedir-interpolation/pom-with-unusual-name.xml");
        assertEquals(pom.getBasedir(), new File(pom.getValue("properties/prop0").toString()));
        assertEquals(pom.getBasedir(), new File(pom.getValue("properties/prop1").toString()));
    }

    /* MNG-3979 */
    @Test
    void testJoiningOfContainersWhenChildHasEmptyElements() throws Exception {
        PomTestWrapper pom = buildPom("id-container-joining-with-empty-elements/sub");
        assertNotNull(pom);
    }

    @Test
    void testOrderOfPluginConfigurationElementsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-order/wo-plugin-mgmt");
        assertEquals("one", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[1]"));
        assertEquals("two", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[2]"));
        assertEquals("three", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[3]"));
        assertEquals("four", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[4]"));
    }

    /* MNG-3827*/
    @Test
    void testOrderOfPluginConfigurationElementsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-order/w-plugin-mgmt");
        assertEquals("one", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[1]"));
        assertEquals("two", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[2]"));
        assertEquals("three", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[3]"));
        assertEquals("four", pom.getValue("build/plugins[1]/configuration/stringParams/stringParam[4]"));
    }

    @Test
    void testOrderOfPluginExecutionConfigurationElementsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-config-order/wo-plugin-mgmt");
        String prefix = "build/plugins[1]/executions[1]/configuration/";
        assertEquals("one", pom.getValue(prefix + "stringParams/stringParam[1]"));
        assertEquals("two", pom.getValue(prefix + "stringParams/stringParam[2]"));
        assertEquals("three", pom.getValue(prefix + "stringParams/stringParam[3]"));
        assertEquals("four", pom.getValue(prefix + "stringParams/stringParam[4]"));
        assertEquals("key1", pom.getValue(prefix + "propertiesParam/property[1]/name"));
        assertEquals("key2", pom.getValue(prefix + "propertiesParam/property[2]/name"));
    }

    /* MNG-3864*/
    @Test
    void testOrderOfPluginExecutionConfigurationElementsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-config-order/w-plugin-mgmt");
        String prefix = "build/plugins[1]/executions[1]/configuration/";
        assertEquals("one", pom.getValue(prefix + "stringParams/stringParam[1]"));
        assertEquals("two", pom.getValue(prefix + "stringParams/stringParam[2]"));
        assertEquals("three", pom.getValue(prefix + "stringParams/stringParam[3]"));
        assertEquals("four", pom.getValue(prefix + "stringParams/stringParam[4]"));
        assertEquals("key1", pom.getValue(prefix + "propertiesParam/property[1]/name"));
        assertEquals("key2", pom.getValue(prefix + "propertiesParam/property[2]/name"));
    }

    /* MNG-3836*/
    @Test
    void testMergeOfInheritedPluginConfiguration() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-merging/child");

        String prefix = "build/plugins[1]/configuration/";
        assertEquals("PASSED", pom.getValue(prefix + "propertiesFile"));
        assertEquals("PASSED", pom.getValue(prefix + "parent"));
        assertEquals("PASSED-1", pom.getValue(prefix + "stringParams/stringParam[1]"));
        assertEquals("PASSED-3", pom.getValue(prefix + "stringParams/stringParam[2]"));
        assertEquals("PASSED-2", pom.getValue(prefix + "stringParams/stringParam[3]"));
        assertEquals("PASSED-4", pom.getValue(prefix + "stringParams/stringParam[4]"));
        assertEquals("PASSED-1", pom.getValue(prefix + "listParam/listParam[1]"));
        assertEquals("PASSED-3", pom.getValue(prefix + "listParam/listParam[2]"));
        assertEquals("PASSED-2", pom.getValue(prefix + "listParam/listParam[3]"));
        assertEquals("PASSED-4", pom.getValue(prefix + "listParam/listParam[4]"));
    }

    /* MNG-2591 */
    @Test
    void testAppendOfInheritedPluginConfigurationWithNoProfile() throws Exception {
        testAppendOfInheritedPluginConfiguration("no-profile");
    }

    /* MNG-2591*/
    @Test
    void testAppendOfInheritedPluginConfigurationWithActiveProfile() throws Exception {
        testAppendOfInheritedPluginConfiguration("with-profile");
    }

    private void testAppendOfInheritedPluginConfiguration(String test) throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-append/" + test + "/subproject");
        String prefix = "build/plugins[1]/configuration/";
        assertEquals("PARENT-1", pom.getValue(prefix + "stringParams/stringParam[1]"));
        assertEquals("PARENT-3", pom.getValue(prefix + "stringParams/stringParam[2]"));
        assertEquals("PARENT-2", pom.getValue(prefix + "stringParams/stringParam[3]"));
        assertEquals("PARENT-4", pom.getValue(prefix + "stringParams/stringParam[4]"));
        assertEquals("CHILD-1", pom.getValue(prefix + "stringParams/stringParam[5]"));
        assertEquals("CHILD-3", pom.getValue(prefix + "stringParams/stringParam[6]"));
        assertEquals("CHILD-2", pom.getValue(prefix + "stringParams/stringParam[7]"));
        assertEquals("CHILD-4", pom.getValue(prefix + "stringParams/stringParam[8]"));
        assertNull(pom.getValue(prefix + "stringParams/stringParam[9]"));
        assertEquals("PARENT-1", pom.getValue(prefix + "listParam/listParam[1]"));
        assertEquals("PARENT-3", pom.getValue(prefix + "listParam/listParam[2]"));
        assertEquals("PARENT-2", pom.getValue(prefix + "listParam/listParam[3]"));
        assertEquals("PARENT-4", pom.getValue(prefix + "listParam/listParam[4]"));
        assertEquals("CHILD-1", pom.getValue(prefix + "listParam/listParam[5]"));
        assertEquals("CHILD-3", pom.getValue(prefix + "listParam/listParam[6]"));
        assertEquals("CHILD-2", pom.getValue(prefix + "listParam/listParam[7]"));
        assertEquals("CHILD-4", pom.getValue(prefix + "listParam/listParam[8]"));
        assertNull(pom.getValue(prefix + "listParam/listParam[9]"));
    }

    /* MNG-4000 */
    @Test
    void testMultiplePluginExecutionsWithAndWithoutIdsWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-w-and-wo-id/wo-plugin-mgmt");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("log-string", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("log-string", pom.getValue("build/plugins[1]/executions[2]/goals[1]"));
    }

    @Test
    void testMultiplePluginExecutionsWithAndWithoutIdsWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-w-and-wo-id/w-plugin-mgmt");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("log-string", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals("log-string", pom.getValue("build/plugins[1]/executions[2]/goals[1]"));
    }

    @Test
    void testDependencyOrderWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("dependency-order/wo-plugin-mgmt");
        assertEquals(4, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("a", pom.getValue("dependencies[1]/artifactId"));
        assertEquals("c", pom.getValue("dependencies[2]/artifactId"));
        assertEquals("b", pom.getValue("dependencies[3]/artifactId"));
        assertEquals("d", pom.getValue("dependencies[4]/artifactId"));
    }

    @Test
    void testDependencyOrderWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("dependency-order/w-plugin-mgmt");
        assertEquals(4, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("a", pom.getValue("dependencies[1]/artifactId"));
        assertEquals("c", pom.getValue("dependencies[2]/artifactId"));
        assertEquals("b", pom.getValue("dependencies[3]/artifactId"));
        assertEquals("d", pom.getValue("dependencies[4]/artifactId"));
    }

    @Test
    void testBuildDirectoriesUsePlatformSpecificFileSeparator() throws Exception {
        PomTestWrapper pom = buildPom("platform-file-separator");
        assertPathWithNormalizedFileSeparators(pom.getValue("build/directory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/outputDirectory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/testOutputDirectory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/sourceDirectory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/testSourceDirectory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/resources[1]/directory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/testResources[1]/directory"));
        assertPathWithNormalizedFileSeparators(pom.getValue("build/filters[1]"));
        assertPathWithNormalizedFileSeparators(pom.getValue("reporting/outputDirectory"));
    }

    /* MNG-4008 */
    @Test
    void testMergedFilterOrder() throws Exception {
        PomTestWrapper pom = buildPom("merged-filter-order/sub");

        assertEquals(7, ((List<?>) pom.getValue("build/filters")).size());
        assertThat(pom.getValue("build/filters[1]").toString(), endsWith("child-a.properties"));
        assertThat(pom.getValue("build/filters[2]").toString(), endsWith("child-c.properties"));
        assertThat(pom.getValue("build/filters[3]").toString(), endsWith("child-b.properties"));
        assertThat(pom.getValue("build/filters[4]").toString(), endsWith("child-d.properties"));
        assertThat(pom.getValue("build/filters[5]").toString(), endsWith("parent-c.properties"));
        assertThat(pom.getValue("build/filters[6]").toString(), endsWith("parent-b.properties"));
        assertThat(pom.getValue("build/filters[7]").toString(), endsWith("parent-d.properties"));
    }

    /** MNG-4027*/
    @Test
    void testProfileInjectedDependencies() throws Exception {
        PomTestWrapper pom = buildPom("profile-injected-dependencies");
        assertEquals(4, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("a", pom.getValue("dependencies[1]/artifactId"));
        assertEquals("c", pom.getValue("dependencies[2]/artifactId"));
        assertEquals("b", pom.getValue("dependencies[3]/artifactId"));
        assertEquals("d", pom.getValue("dependencies[4]/artifactId"));
    }

    /** IT-0021*/
    @Test
    void testProfileDependenciesMultipleProfiles() throws Exception {
        PomTestWrapper pom = buildPom("profile-dependencies-multiple-profiles", "profile-1", "profile-2");
        assertEquals(2, ((List<?>) pom.getValue("dependencies")).size());
    }

    @Test
    void testDependencyInheritance() throws Exception {
        PomTestWrapper pom = buildPom("dependency-inheritance/sub");
        assertEquals(1, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("4.13.1", pom.getValue("dependencies[1]/version"));
    }

    /** MNG-4034 */
    @Test
    void testManagedProfileDependency() throws Exception {
        PomTestWrapper pom = this.buildPom("managed-profile-dependency/sub", "maven-core-it");
        assertEquals(1, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("org.apache.maven.its", pom.getValue("dependencies[1]/groupId"));
        assertEquals("maven-core-it-support", pom.getValue("dependencies[1]/artifactId"));
        assertEquals("1.3", pom.getValue("dependencies[1]/version"));
        assertEquals("runtime", pom.getValue("dependencies[1]/scope"));
        assertEquals(1, ((List<?>) pom.getValue("dependencies[1]/exclusions")).size());
        assertEquals("commons-lang", pom.getValue("dependencies[1]/exclusions[1]/groupId"));
    }

    /** MNG-4040 */
    @Test
    void testProfileModuleInheritance() throws Exception {
        PomTestWrapper pom = this.buildPom("profile-module-inheritance/sub", "dist");
        assertEquals(0, ((List<?>) pom.getValue("modules")).size());
    }

    /** MNG-3621 */
    @Test
    void testUncPath() throws Exception {
        PomTestWrapper pom = this.buildPom("unc-path/sub");
        assertEquals("file:////host/site/test-child", pom.getValue("distributionManagement/site/url"));
    }

    /** MNG-2006 */
    @Test
    void testUrlAppendWithChildPathAdjustment() throws Exception {
        PomTestWrapper pom = this.buildPom("url-append/child");
        assertEquals("https://project.url/child", pom.getValue("url"));
        assertEquals("https://viewvc.project.url/child", pom.getValue("scm/url"));
        assertEquals("https://scm.project.url/child", pom.getValue("scm/connection"));
        assertEquals("https://scm.project.url/child", pom.getValue("scm/developerConnection"));
        assertEquals("https://site.project.url/child", pom.getValue("distributionManagement/site/url"));
    }

    /** MNG-0479 */
    @Test
    void testRepoInheritance() throws Exception {
        PomTestWrapper pom = this.buildPom("repo-inheritance");
        assertEquals(1, ((List<?>) pom.getValue("repositories")).size());
        assertEquals("it0043", pom.getValue("repositories[1]/name"));
    }

    @Test
    void testEmptyScm() throws Exception {
        PomTestWrapper pom = this.buildPom("empty-scm");
        assertNull(pom.getValue("scm"));
    }

    @Test
    void testPluginConfigurationUsingAttributesWithoutPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-attributes/wo-plugin-mgmt");
        assertEquals("src", pom.getValue("build/plugins[1]/configuration/domParam/copy/@todir"));
        assertEquals("true", pom.getValue("build/plugins[1]/configuration/domParam/copy/@overwrite"));
        assertEquals("target", pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@dir"));
        assertNull(pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@todir"));
        assertNull(pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@overwrite"));
    }

    /** MNG-4053*/
    @Test
    void testPluginConfigurationUsingAttributesWithPluginManagement() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-attributes/w-plugin-mgmt");
        assertEquals("src", pom.getValue("build/plugins[1]/configuration/domParam/copy/@todir"));
        assertEquals("true", pom.getValue("build/plugins[1]/configuration/domParam/copy/@overwrite"));
        assertEquals("target", pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@dir"));
        assertNull(pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@todir"));
        assertNull(pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@overwrite"));
    }

    @Test
    void testPluginConfigurationUsingAttributesWithPluginManagementAndProfile() throws Exception {
        PomTestWrapper pom = buildPom("plugin-config-attributes/w-profile", "maven-core-it");
        assertEquals("src", pom.getValue("build/plugins[1]/configuration/domParam/copy/@todir"));
        assertEquals("true", pom.getValue("build/plugins[1]/configuration/domParam/copy/@overwrite"));
        assertEquals("target", pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@dir"));
        assertNull(pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@todir"));
        assertNull(pom.getValue("build/plugins[1]/configuration/domParam/copy/fileset/@overwrite"));
    }

    @Test
    void testPomEncoding() throws Exception {
        PomTestWrapper pom = buildPom("pom-encoding/utf-8");
        assertEquals("TEST-CHARS: \u00DF\u0131\u03A3\u042F\u05D0\u20AC", pom.getValue("description"));
        pom = buildPom("pom-encoding/latin-1");
        assertEquals("TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF", pom.getValue("description"));
    }

    /* MNG-4070 */
    @Test
    void testXmlWhitespaceHandling() throws Exception {
        PomTestWrapper pom = buildPom("xml-whitespace/sub");
        assertEquals("org.apache.maven.its.mng4070", pom.getValue("groupId"));
    }

    /* MNG-3760*/
    @Test
    void testInterpolationOfBaseUri() throws Exception {
        PomTestWrapper pom = buildPom("baseuri-interpolation/pom.xml");
        assertNotEquals(
                pom.getBasedir().toURI().toString(),
                pom.getValue("properties/prop1").toString());
    }

    /* MNG-6386 */
    @Test
    void testInterpolationOfRfc3986BaseUri() throws Exception {
        PomTestWrapper pom = buildPom("baseuri-interpolation/pom.xml");
        String prop1 = pom.getValue("properties/prop1").toString();
        assertEquals(pom.getBasedir().toPath().toUri().toASCIIString(), prop1);
        assertThat(prop1, startsWith("file:///"));
    }

    /* MNG-3811*/
    @Test
    void testReportingPluginConfig() throws Exception {
        PomTestWrapper pom = buildPom("reporting-plugin-config/sub");

        assertEquals(3, ((List<?>) pom.getValue("reporting/plugins[1]/configuration/stringParams")).size());
        assertEquals("parentParam", pom.getValue("reporting/plugins[1]/configuration/stringParams[1]/stringParam[1]"));
        assertEquals("childParam", pom.getValue("reporting/plugins[1]/configuration/stringParams[1]/stringParam[2]"));
        assertEquals(
                "  preserve space  ",
                pom.getValue("reporting/plugins[1]/configuration/stringParams[1]/stringParam[3]"));
        assertEquals("true", pom.getValue("reporting/plugins[1]/configuration/booleanParam"));
    }

    @Test
    void testPropertiesNoDuplication() throws Exception {
        PomTestWrapper pom = buildPom("properties-no-duplication/sub");
        assertEquals(4, ((Properties) pom.getValue("properties")).size());
        assertEquals("child", pom.getValue("properties/pomProfile"));
    }

    @Test
    void testPomInheritance() throws Exception {
        PomTestWrapper pom = buildPom("pom-inheritance/child-1");
        assertEquals("parent-description", pom.getValue("description"));
        assertEquals("jar", pom.getValue("packaging"));
    }

    @Test
    void testCompleteModelWithoutParent() throws Exception {
        PomTestWrapper pom = buildPom("complete-model/wo-parent");

        testCompleteModel(pom);
    }

    @Test
    void testCompleteModelWithParent() throws Exception {
        PomTestWrapper pom = buildPom("complete-model/w-parent/sub");

        testCompleteModel(pom);
    }

    private void testCompleteModel(PomTestWrapper pom) throws Exception {
        assertEquals("4.0.0", pom.getValue("modelVersion"));

        assertEquals("org.apache.maven.its.mng", pom.getValue("groupId"));
        assertEquals("test", pom.getValue("artifactId"));
        assertEquals("0.2", pom.getValue("version"));
        assertEquals("pom", pom.getValue("packaging"));

        assertEquals("project-name", pom.getValue("name"));
        assertEquals("project-description", pom.getValue("description"));
        assertEquals("https://project.url/", pom.getValue("url"));
        assertEquals("2009", pom.getValue("inceptionYear"));

        assertEquals("project-org", pom.getValue("organization/name"));
        assertEquals("https://project-org.url/", pom.getValue("organization/url"));

        assertEquals(1, ((List<?>) pom.getValue("licenses")).size());
        assertEquals("project-license", pom.getValue("licenses[1]/name"));
        assertEquals("https://project.url/license", pom.getValue("licenses[1]/url"));
        assertEquals("repo", pom.getValue("licenses[1]/distribution"));
        assertEquals("free", pom.getValue("licenses[1]/comments"));

        assertEquals(1, ((List<?>) pom.getValue("developers")).size());
        assertEquals("dev", pom.getValue("developers[1]/id"));
        assertEquals("project-developer", pom.getValue("developers[1]/name"));
        assertEquals("developer@", pom.getValue("developers[1]/email"));
        assertEquals("https://developer", pom.getValue("developers[1]/url"));
        assertEquals("developer", pom.getValue("developers[1]/organization"));
        assertEquals("https://devel.org", pom.getValue("developers[1]/organizationUrl"));
        assertEquals("-1", pom.getValue("developers[1]/timezone"));
        assertEquals("yes", pom.getValue("developers[1]/properties/developer"));
        assertEquals(1, ((List<?>) pom.getValue("developers[1]/roles")).size());
        assertEquals("devel", pom.getValue("developers[1]/roles[1]"));

        assertEquals(1, ((List<?>) pom.getValue("contributors")).size());
        assertEquals("project-contributor", pom.getValue("contributors[1]/name"));
        assertEquals("contributor@", pom.getValue("contributors[1]/email"));
        assertEquals("https://contributor", pom.getValue("contributors[1]/url"));
        assertEquals("contributor", pom.getValue("contributors[1]/organization"));
        assertEquals("https://contrib.org", pom.getValue("contributors[1]/organizationUrl"));
        assertEquals("+1", pom.getValue("contributors[1]/timezone"));
        assertEquals("yes", pom.getValue("contributors[1]/properties/contributor"));
        assertEquals(1, ((List<?>) pom.getValue("contributors[1]/roles")).size());
        assertEquals("contrib", pom.getValue("contributors[1]/roles[1]"));

        assertEquals(1, ((List<?>) pom.getValue("mailingLists")).size());
        assertEquals("project-mailing-list", pom.getValue("mailingLists[1]/name"));
        assertEquals("subscribe@", pom.getValue("mailingLists[1]/subscribe"));
        assertEquals("unsubscribe@", pom.getValue("mailingLists[1]/unsubscribe"));
        assertEquals("post@", pom.getValue("mailingLists[1]/post"));
        assertEquals("mail-archive", pom.getValue("mailingLists[1]/archive"));
        assertEquals(1, ((List<?>) pom.getValue("mailingLists[1]/otherArchives")).size());
        assertEquals("other-archive", pom.getValue("mailingLists[1]/otherArchives[1]"));

        assertEquals("2.0.1", pom.getValue("prerequisites/maven"));

        assertEquals("https://project.url/trunk", pom.getValue("scm/url"));
        assertEquals("https://project.url/scm", pom.getValue("scm/connection"));
        assertEquals("https://project.url/scm", pom.getValue("scm/developerConnection"));
        assertEquals("TAG", pom.getValue("scm/tag"));

        assertEquals("issues", pom.getValue("issueManagement/system"));
        assertEquals("https://project.url/issues", pom.getValue("issueManagement/url"));

        assertEquals("ci", pom.getValue("ciManagement/system"));
        assertEquals("https://project.url/ci", pom.getValue("ciManagement/url"));
        assertEquals(1, ((List<?>) pom.getValue("ciManagement/notifiers")).size());
        assertEquals("irc", pom.getValue("ciManagement/notifiers[1]/type"));
        assertEquals("ci@", pom.getValue("ciManagement/notifiers[1]/address"));
        assertEquals(Boolean.TRUE, pom.getValue("ciManagement/notifiers[1]/sendOnError"));
        assertEquals(Boolean.FALSE, pom.getValue("ciManagement/notifiers[1]/sendOnFailure"));
        assertEquals(Boolean.FALSE, pom.getValue("ciManagement/notifiers[1]/sendOnWarning"));
        assertEquals(Boolean.FALSE, pom.getValue("ciManagement/notifiers[1]/sendOnSuccess"));
        assertEquals("ci", pom.getValue("ciManagement/notifiers[1]/configuration/ciProp"));

        assertEquals("project.distros", pom.getValue("distributionManagement/repository/id"));
        assertEquals("distros", pom.getValue("distributionManagement/repository/name"));
        assertEquals("https://project.url/dist", pom.getValue("distributionManagement/repository/url"));
        assertEquals(Boolean.TRUE, pom.getValue("distributionManagement/repository/uniqueVersion"));

        assertEquals("project.snaps", pom.getValue("distributionManagement/snapshotRepository/id"));
        assertEquals("snaps", pom.getValue("distributionManagement/snapshotRepository/name"));
        assertEquals("https://project.url/snaps", pom.getValue("distributionManagement/snapshotRepository/url"));
        assertEquals(Boolean.FALSE, pom.getValue("distributionManagement/snapshotRepository/uniqueVersion"));

        assertEquals("project.site", pom.getValue("distributionManagement/site/id"));
        assertEquals("docs", pom.getValue("distributionManagement/site/name"));
        assertEquals("https://project.url/site", pom.getValue("distributionManagement/site/url"));

        assertEquals("https://project.url/download", pom.getValue("distributionManagement/downloadUrl"));
        assertEquals("reloc-gid", pom.getValue("distributionManagement/relocation/groupId"));
        assertEquals("reloc-aid", pom.getValue("distributionManagement/relocation/artifactId"));
        assertEquals("reloc-version", pom.getValue("distributionManagement/relocation/version"));
        assertEquals("project-reloc-msg", pom.getValue("distributionManagement/relocation/message"));

        assertEquals(1, ((List<?>) pom.getValue("modules")).size());
        assertEquals("sub", pom.getValue("modules[1]"));

        assertEquals(4, ((Map<?, ?>) pom.getValue("properties")).size());
        assertEquals("project-property", pom.getValue("properties[1]/itProperty"));
        assertEquals("UTF-8", pom.getValue("properties[1]/project.build.sourceEncoding"));
        assertEquals("UTF-8", pom.getValue("properties[1]/project.reporting.outputEncoding"));
        assertEquals("1980-02-01T00:00:00Z", pom.getValue("properties[1]/project.build.outputTimestamp"));

        assertEquals(1, ((List<?>) pom.getValue("dependencyManagement/dependencies")).size());
        assertEquals("org.apache.maven.its", pom.getValue("dependencyManagement/dependencies[1]/groupId"));
        assertEquals("managed-dep", pom.getValue("dependencyManagement/dependencies[1]/artifactId"));
        assertEquals("0.1", pom.getValue("dependencyManagement/dependencies[1]/version"));
        assertEquals("war", pom.getValue("dependencyManagement/dependencies[1]/type"));
        assertEquals("runtime", pom.getValue("dependencyManagement/dependencies[1]/scope"));
        assertEquals(Boolean.FALSE, pom.getValue("dependencyManagement/dependencies[1]/optional"));
        assertEquals(1, ((List<?>) pom.getValue("dependencyManagement/dependencies[1]/exclusions")).size());
        assertEquals(
                "org.apache.maven.its", pom.getValue("dependencyManagement/dependencies[1]/exclusions[1]/groupId"));
        assertEquals(
                "excluded-managed-dep", pom.getValue("dependencyManagement/dependencies[1]/exclusions[1]/artifactId"));

        assertEquals(1, ((List<?>) pom.getValue("dependencies")).size());
        assertEquals("org.apache.maven.its", pom.getValue("dependencies[1]/groupId"));
        assertEquals("dep", pom.getValue("dependencies[1]/artifactId"));
        assertEquals("0.2", pom.getValue("dependencies[1]/version"));
        assertEquals("ejb", pom.getValue("dependencies[1]/type"));
        assertEquals("test", pom.getValue("dependencies[1]/scope"));
        assertEquals(Boolean.TRUE, pom.getValue("dependencies[1]/optional"));
        assertEquals(1, ((List<?>) pom.getValue("dependencies[1]/exclusions")).size());
        assertEquals("org.apache.maven.its", pom.getValue("dependencies[1]/exclusions[1]/groupId"));
        assertEquals("excluded-dep", pom.getValue("dependencies[1]/exclusions[1]/artifactId"));

        assertEquals(1, ((List<?>) pom.getValue("repositories")).size());
        assertEquals("project-remote-repo", pom.getValue("repositories[1]/id"));
        assertEquals("https://project.url/remote", pom.getValue("repositories[1]/url"));
        assertEquals("repo", pom.getValue("repositories[1]/name"));

        assertEquals("test", pom.getValue("build/defaultGoal"));
        assertEquals("coreit", pom.getValue("build/finalName"));

        assertPathSuffixEquals("build", pom.getValue("build/directory"));
        assertPathSuffixEquals("build/main", pom.getValue("build/outputDirectory"));
        assertPathSuffixEquals("build/test", pom.getValue("build/testOutputDirectory"));
        assertPathSuffixEquals("sources/main", pom.getValue("build/sourceDirectory"));
        assertPathSuffixEquals("sources/test", pom.getValue("build/testSourceDirectory"));
        assertPathSuffixEquals("sources/scripts", pom.getValue("build/scriptSourceDirectory"));

        assertEquals(1, ((List<?>) pom.getValue("build/filters")).size());
        assertPathSuffixEquals("src/main/filter/it.properties", pom.getValue("build/filters[1]"));

        assertEquals(1, ((List<?>) pom.getValue("build/resources")).size());
        assertPathSuffixEquals("res/main", pom.getValue("build/resources[1]/directory"));
        assertPathSuffixEquals("main", pom.getValue("build/resources[1]/targetPath"));
        assertEquals(Boolean.TRUE, pom.getValue("build/resources[1]/filtering"));
        assertEquals(1, ((List<?>) pom.getValue("build/resources[1]/includes")).size());
        assertPathSuffixEquals("main.included", pom.getValue("build/resources[1]/includes[1]"));
        assertEquals(1, ((List<?>) pom.getValue("build/resources[1]/excludes")).size());
        assertPathSuffixEquals("main.excluded", pom.getValue("build/resources[1]/excludes[1]"));

        assertEquals(1, ((List<?>) pom.getValue("build/testResources")).size());
        assertPathSuffixEquals("res/test", pom.getValue("build/testResources[1]/directory"));
        assertPathSuffixEquals("test", pom.getValue("build/testResources[1]/targetPath"));
        assertEquals(Boolean.TRUE, pom.getValue("build/testResources[1]/filtering"));
        assertEquals(1, ((List<?>) pom.getValue("build/testResources[1]/includes")).size());
        assertPathSuffixEquals("test.included", pom.getValue("build/testResources[1]/includes[1]"));
        assertEquals(1, ((List<?>) pom.getValue("build/testResources[1]/excludes")).size());
        assertPathSuffixEquals("test.excluded", pom.getValue("build/testResources[1]/excludes[1]"));

        assertEquals(1, ((List<?>) pom.getValue("build/extensions")).size());
        assertEquals("org.apache.maven.its.ext", pom.getValue("build/extensions[1]/groupId"));
        assertEquals("ext", pom.getValue("build/extensions[1]/artifactId"));
        assertEquals("3.0", pom.getValue("build/extensions[1]/version"));

        assertEquals(1, ((List<?>) pom.getValue("build/plugins")).size());
        assertEquals("org.apache.maven.its.plugins", pom.getValue("build/plugins[1]/groupId"));
        assertEquals("maven-it-plugin-build", pom.getValue("build/plugins[1]/artifactId"));
        assertEquals("2.1-SNAPSHOT", pom.getValue("build/plugins[1]/version"));
        assertEquals("test.properties", pom.getValue("build/plugins[1]/configuration/outputFile"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/executions")).size());
        assertEquals("test", pom.getValue("build/plugins[1]/executions[1]/id"));
        assertEquals("validate", pom.getValue("build/plugins[1]/executions[1]/phase"));
        assertEquals("pom.properties", pom.getValue("build/plugins[1]/executions[1]/configuration/outputFile"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/executions[1]/goals")).size());
        assertEquals("eval", pom.getValue("build/plugins[1]/executions[1]/goals[1]"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/dependencies")).size());
        assertEquals("org.apache.maven.its", pom.getValue("build/plugins[1]/dependencies[1]/groupId"));
        assertEquals("build-plugin-dep", pom.getValue("build/plugins[1]/dependencies[1]/artifactId"));
        assertEquals("0.3", pom.getValue("build/plugins[1]/dependencies[1]/version"));
        assertEquals("zip", pom.getValue("build/plugins[1]/dependencies[1]/type"));
        assertEquals(1, ((List<?>) pom.getValue("build/plugins[1]/dependencies[1]/exclusions")).size());
        assertEquals("org.apache.maven.its", pom.getValue("build/plugins[1]/dependencies[1]/exclusions[1]/groupId"));
        assertEquals(
                "excluded-build-plugin-dep", pom.getValue("build/plugins[1]/dependencies[1]/exclusions[1]/artifactId"));

        assertEquals(Boolean.TRUE, pom.getValue("reporting/excludeDefaults"));
        assertPathSuffixEquals("docs", pom.getValue("reporting/outputDirectory"));

        assertEquals(1, ((List<?>) pom.getValue("reporting/plugins")).size());
        assertEquals("org.apache.maven.its.plugins", pom.getValue("reporting/plugins[1]/groupId"));
        assertEquals("maven-it-plugin-reporting", pom.getValue("reporting/plugins[1]/artifactId"));
        assertEquals("2.0-SNAPSHOT", pom.getValue("reporting/plugins[1]/version"));
        assertEquals("test.html", pom.getValue("reporting/plugins[1]/configuration/outputFile"));
        assertEquals(1, ((List<?>) pom.getValue("reporting/plugins[1]/reportSets")).size());
        assertEquals("it", pom.getValue("reporting/plugins[1]/reportSets[1]/id"));
        assertEquals("index.html", pom.getValue("reporting/plugins[1]/reportSets[1]/configuration/outputFile"));
        assertEquals(1, ((List<?>) pom.getValue("reporting/plugins[1]/reportSets[1]/reports")).size());
        assertEquals("run", pom.getValue("reporting/plugins[1]/reportSets[1]/reports[1]"));
    }

    /* MNG-2309*/
    @Test
    void testProfileInjectionOrder() throws Exception {
        PomTestWrapper pom = buildPom("profile-injection-order", "pom-a", "pom-b", "pom-e", "pom-c", "pom-d");
        assertEquals("e", pom.getValue("properties[1]/pomProperty"));
    }

    @Test
    void testPropertiesInheritance() throws Exception {
        PomTestWrapper pom = buildPom("properties-inheritance/sub");
        assertEquals("parent-property", pom.getValue("properties/parentProperty"));
        assertEquals("child-property", pom.getValue("properties/childProperty"));
        assertEquals("child-override", pom.getValue("properties/overriddenProperty"));
    }

    /* MNG-4102*/
    @Test
    void testInheritedPropertiesInterpolatedWithValuesFromChildWithoutProfiles() throws Exception {
        PomTestWrapper pom = buildPom("inherited-properties-interpolation/no-profile/sub");

        assertEquals("CHILD", pom.getValue("properties/overridden"));
        assertEquals("CHILD", pom.getValue("properties/interpolated"));
    }

    /* MNG-4102 */
    @Test
    void testInheritedPropertiesInterpolatedWithValuesFromChildWithActiveProfiles() throws Exception {
        PomTestWrapper pom = buildPom("inherited-properties-interpolation/active-profile/sub");

        assertEquals(1, pom.getMavenProject().getModel().getProfiles().size());

        buildPom("inherited-properties-interpolation/active-profile/sub", "it-parent", "it-child");
        assertEquals("CHILD", pom.getValue("properties/overridden"));
        assertEquals("CHILD", pom.getValue("properties/interpolated"));
    }

    /* MNG-3545 */
    @Test
    void testProfileDefaultActivation() throws Exception {
        PomTestWrapper pom = buildPom("profile-default-deactivation", "profile4");
        assertEquals(1, pom.getMavenProject().getActiveProfiles().size());
        assertEquals(1, ((List<?>) pom.getValue("build/plugins")).size());
        assertEquals("2.1", pom.getValue("build/plugins[1]/version"));
    }

    /* MNG-1995 */
    @Test
    void testBooleanInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("boolean-interpolation");
        assertEquals(true, pom.getValue("repositories[1]/releases/enabled"));
        assertEquals(true, pom.getValue("build/resources[1]/filtering"));
    }

    /* MNG-3899 */
    @Test
    void testBuildExtensionInheritance() throws Exception {
        PomTestWrapper pom = buildPom("build-extension-inheritance/sub");
        assertEquals(3, ((List<?>) pom.getValue("build/extensions")).size());
        assertEquals("b", pom.getValue("build/extensions[1]/artifactId"));
        assertEquals("a", pom.getValue("build/extensions[2]/artifactId"));
        assertEquals("0.2", pom.getValue("build/extensions[2]/version"));
        assertEquals("c", pom.getValue("build/extensions[3]/artifactId"));
    }

    /*MNG-1957*/
    @Test
    void testJdkActivation() throws Exception {
        Properties props = new Properties();
        props.put("java.version", "1.5.0_15");

        PomTestWrapper pom = buildPom("jdk-activation", props, null);
        assertEquals(3, pom.getMavenProject().getActiveProfiles().size());
        assertEquals("PASSED", pom.getValue("properties/jdkProperty3"));
        assertEquals("PASSED", pom.getValue("properties/jdkProperty2"));
        assertEquals("PASSED", pom.getValue("properties/jdkProperty1"));
    }

    /* MNG-2174 */
    @Test
    void testProfilePluginMngDependencies() throws Exception {
        PomTestWrapper pom = buildPom("profile-plugin-mng-dependencies/sub", "maven-core-it");
        assertEquals("a", pom.getValue("build/plugins[1]/dependencies[1]/artifactId"));
    }

    /** MNG-4116 */
    @Test
    void testPercentEncodedUrlsMustNotBeDecoded() throws Exception {
        PomTestWrapper pom = this.buildPom("url-no-decoding");
        assertEquals("https://maven.apache.org/spacy%20path", pom.getValue("url"));
        assertEquals("https://svn.apache.org/viewvc/spacy%20path", pom.getValue("scm/url"));
        assertEquals("scm:svn:svn+ssh://svn.apache.org/spacy%20path", pom.getValue("scm/connection"));
        assertEquals("scm:svn:svn+ssh://svn.apache.org/spacy%20path", pom.getValue("scm/developerConnection"));
        assertEquals("https://issues.apache.org/spacy%20path", pom.getValue("issueManagement/url"));
        assertEquals("https://ci.apache.org/spacy%20path", pom.getValue("ciManagement/url"));
        assertEquals(
                "scm:svn:svn+ssh://dist.apache.org/spacy%20path",
                pom.getValue("distributionManagement/repository/url"));
        assertEquals(
                "scm:svn:svn+ssh://snap.apache.org/spacy%20path",
                pom.getValue("distributionManagement/snapshotRepository/url"));
        assertEquals("scm:svn:svn+ssh://site.apache.org/spacy%20path", pom.getValue("distributionManagement/site/url"));
    }

    @Test
    void testPluginManagementInheritance() throws Exception {
        PomTestWrapper pom = this.buildPom("plugin-management-inheritance");
        assertEquals(
                "0.1-stub-SNAPSHOT",
                pom.getValue("build/pluginManagement/plugins[@artifactId='maven-compiler-plugin']/version"));
    }

    @Test
    void testProfilePlugins() throws Exception {
        PomTestWrapper pom = this.buildPom("profile-plugins", "standard");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins")).size());
        assertEquals("maven-assembly2-plugin", pom.getValue("build/plugins[2]/artifactId"));
    }

    @Test
    void testPluginInheritanceSimple() throws Exception {
        PomTestWrapper pom = this.buildPom("plugin-inheritance-simple/sub");
        assertEquals(2, ((List<?>) pom.getValue("build/plugins")).size());
    }

    @Test
    void testPluginManagementDuplicate() throws Exception {
        PomTestWrapper pom = this.buildPom("plugin-management-duplicate/sub");
        assertEquals(7, ((List<?>) pom.getValue("build/pluginManagement/plugins")).size());
    }

    @Test
    void testDistributionManagement() throws Exception {
        PomTestWrapper pom = this.buildPom("distribution-management");
        assertEquals("legacy", pom.getValue("distributionManagement/repository/layout"));
    }

    @Test
    void testDependencyScopeInheritance() throws Exception {
        PomTestWrapper pom = buildPom("dependency-scope-inheritance/sub");
        String scope = (String) pom.getValue("dependencies[1]/scope");
        assertEquals("compile", scope);
    }

    @Test
    void testDependencyScope() throws Exception {
        buildPom("dependency-scope/sub");
    }

    // This will fail on a validation error if incorrect
    public void testDependencyManagementWithInterpolation() throws Exception {
        buildPom("dependency-management-with-interpolation/sub");
    }

    @Test
    void testInterpolationWithSystemProperty() throws Exception {
        Properties sysProps = new Properties();
        sysProps.setProperty("system.property", "PASSED");
        PomTestWrapper pom = buildPom("system-property-interpolation", sysProps, null);
        assertEquals("PASSED", pom.getValue("name"));
    }

    /* MNG-4129 */
    @Test
    void testPluginExecutionInheritanceWhenChildDoesNotDeclarePlugin() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-inheritance/wo-merge");
        @SuppressWarnings("unchecked")
        List<PluginExecution> executions = (List<PluginExecution>) pom.getValue(
                "build/pluginsAsMap[@name='org.apache.maven.its.plugins:maven-it-plugin-log-file']/executions");
        assertEquals(1, executions.size());
        assertEquals("inherited-execution", executions.get(0).getId());
    }

    @Test
    void testPluginExecutionInheritanceWhenChildDoesDeclarePluginAsWell() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-inheritance/w-merge");
        @SuppressWarnings("unchecked")
        List<PluginExecution> executions = (List<PluginExecution>) pom.getValue(
                "build/pluginsAsMap[@name='org.apache.maven.its.plugins:maven-it-plugin-log-file']/executions");
        assertEquals(1, executions.size());
        assertEquals("inherited-execution", executions.get(0).getId());
    }

    /* MNG-4193 */
    @Test
    void testValidationErrorUponNonUniqueArtifactRepositoryId() throws Exception {
        assertThrows(
                ProjectBuildingException.class,
                () -> buildPom("unique-repo-id/artifact-repo"),
                "Non-unique repository ids did not cause validation error");
    }

    /* MNG-4193 */
    @Test
    void testValidationErrorUponNonUniquePluginRepositoryId() throws Exception {
        assertThrows(
                ProjectBuildingException.class,
                () -> buildPom("unique-repo-id/plugin-repo"),
                "Non-unique repository ids did not cause validation error");
    }

    /* MNG-4193 */
    @Test
    void testValidationErrorUponNonUniqueArtifactRepositoryIdInProfile() throws Exception {
        assertThrows(
                ProjectBuildingException.class,
                () -> buildPom("unique-repo-id/artifact-repo-in-profile"),
                "Non-unique repository ids did not cause validation error");
    }

    /* MNG-4193 */
    @Test
    void testValidationErrorUponNonUniquePluginRepositoryIdInProfile() throws Exception {
        assertThrows(
                ProjectBuildingException.class,
                () -> buildPom("unique-repo-id/plugin-repo-in-profile"),
                "Non-unique repository ids did not cause validation error");
    }

    /** MNG-3843 */
    @Test
    void testPrerequisitesAreNotInherited() throws Exception {
        PomTestWrapper pom = buildPom("prerequisites-inheritance/child");
        assertSame(null, pom.getValue("prerequisites"));
    }

    @Test
    void testLicensesAreInheritedButNotAggregated() throws Exception {
        PomTestWrapper pom = buildPom("licenses-inheritance/child-2");
        assertEquals(1, ((List<?>) pom.getValue("licenses")).size());
        assertEquals("child-license", pom.getValue("licenses[1]/name"));
        assertEquals("https://child.url/license", pom.getValue("licenses[1]/url"));
    }

    @Test
    void testDevelopersAreInheritedButNotAggregated() throws Exception {
        PomTestWrapper pom = buildPom("developers-inheritance/child-2");
        assertEquals(1, ((List<?>) pom.getValue("developers")).size());
        assertEquals("child-developer", pom.getValue("developers[1]/name"));
    }

    @Test
    void testContributorsAreInheritedButNotAggregated() throws Exception {
        PomTestWrapper pom = buildPom("contributors-inheritance/child-2");
        assertEquals(1, ((List<?>) pom.getValue("contributors")).size());
        assertEquals("child-contributor", pom.getValue("contributors[1]/name"));
    }

    @Test
    void testMailingListsAreInheritedButNotAggregated() throws Exception {
        PomTestWrapper pom = buildPom("mailing-lists-inheritance/child-2");
        assertEquals(1, ((List<?>) pom.getValue("mailingLists")).size());
        assertEquals("child-mailing-list", pom.getValue("mailingLists[1]/name"));
    }

    @Test
    void testPluginInheritanceOrder() throws Exception {
        PomTestWrapper pom = buildPom("plugin-inheritance-order/child");

        assertEquals("maven-it-plugin-log-file", pom.getValue("build/plugins[1]/artifactId"));
        assertEquals("maven-it-plugin-expression", pom.getValue("build/plugins[2]/artifactId"));
        assertEquals("maven-it-plugin-configuration", pom.getValue("build/plugins[3]/artifactId"));

        assertEquals("maven-it-plugin-log-file", pom.getValue("reporting/plugins[1]/artifactId"));
        assertEquals("maven-it-plugin-expression", pom.getValue("reporting/plugins[2]/artifactId"));
        assertEquals("maven-it-plugin-configuration", pom.getValue("reporting/plugins[3]/artifactId"));
    }

    @Test
    void testCliPropsDominateProjectPropsDuringInterpolation() throws Exception {
        Properties props = new Properties();
        props.setProperty("testProperty", "PASSED");
        PomTestWrapper pom = buildPom("interpolation-cli-wins", null, props);

        assertEquals("PASSED", pom.getValue("properties/interpolatedProperty"));
    }

    @Test
    void testParentPomPackagingMustBePom() throws Exception {
        assertThrows(
                ProjectBuildingException.class,
                () -> buildPom("parent-pom-packaging/sub"),
                "Wrong packaging of parent POM was not rejected");
    }

    /** MNG-522, MNG-3018 */
    @Test
    void testManagedPluginConfigurationAppliesToImplicitPluginsIntroducedByPackaging() throws Exception {
        PomTestWrapper pom = buildPom("plugin-management-for-implicit-plugin/child");
        assertEquals(
                "passed.txt",
                pom.getValue("build/plugins[@artifactId='maven-resources-plugin']/configuration/pathname"));
        assertEquals(
                "passed.txt",
                pom.getValue("build/plugins[@artifactId='maven-it-plugin-log-file']/configuration/logFile"));
    }

    @Test
    void testDefaultPluginsExecutionContributedByPackagingExecuteBeforeUserDefinedExecutions() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-order-and-default-exec");
        @SuppressWarnings("unchecked")
        List<PluginExecution> executions =
                (List<PluginExecution>) pom.getValue("build/plugins[@artifactId='maven-resources-plugin']/executions");
        assertNotNull(executions);
        assertEquals(4, executions.size());
        assertEquals("default-resources", executions.get(0).getId());
        assertEquals("default-testResources", executions.get(1).getId());
        assertEquals("test-1", executions.get(2).getId());
        assertEquals("test-2", executions.get(3).getId());
    }

    @Test
    void testPluginDeclarationsRetainPomOrderAfterInjectionOfDefaultPlugins() throws Exception {
        PomTestWrapper pom = buildPom("plugin-exec-order-with-lifecycle");
        @SuppressWarnings("unchecked")
        List<Plugin> plugins = (List<Plugin>) pom.getValue("build/plugins");
        int resourcesPlugin = -1;
        int customPlugin = -1;
        for (int i = 0; i < plugins.size(); i++) {
            Plugin plugin = plugins.get(i);
            if ("maven-resources-plugin".equals(plugin.getArtifactId())) {
                assertThat(resourcesPlugin, lessThan(0));
                resourcesPlugin = i;
            } else if ("maven-it-plugin-log-file".equals(plugin.getArtifactId())) {
                assertThat(customPlugin, lessThan(0));
                customPlugin = i;
            }
        }
        assertEquals(customPlugin, resourcesPlugin - 1, plugins.toString());
    }

    /** MNG-4415 */
    @Test
    void testPluginOrderAfterMergingWithInheritedPlugins() throws Exception {
        PomTestWrapper pom = buildPom("plugin-inheritance-merge-order/sub");

        List<String> expected = new ArrayList<>();
        expected.add("maven-it-plugin-error");
        expected.add("maven-it-plugin-configuration");
        expected.add("maven-it-plugin-dependency-resolution");
        expected.add("maven-it-plugin-packaging");
        expected.add("maven-it-plugin-log-file");
        expected.add("maven-it-plugin-expression");
        expected.add("maven-it-plugin-fork");
        expected.add("maven-it-plugin-touch");

        List<String> actual = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Plugin> plugins = (List<Plugin>) pom.getValue("build/plugins");
        for (Plugin plugin : plugins) {
            actual.add(plugin.getArtifactId());
        }

        actual.retainAll(expected);

        assertEquals(actual, expected);
    }

    /** MNG-4416 */
    @Test
    void testPluginOrderAfterMergingWithInjectedPlugins() throws Exception {
        PomTestWrapper pom = buildPom("plugin-injection-merge-order");

        List<String> expected = new ArrayList<>();
        expected.add("maven-it-plugin-error");
        expected.add("maven-it-plugin-configuration");
        expected.add("maven-it-plugin-dependency-resolution");
        expected.add("maven-it-plugin-packaging");
        expected.add("maven-it-plugin-log-file");
        expected.add("maven-it-plugin-expression");
        expected.add("maven-it-plugin-fork");
        expected.add("maven-it-plugin-touch");

        List<String> actual = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Plugin> plugins = (List<Plugin>) pom.getValue("build/plugins");
        for (Plugin plugin : plugins) {
            actual.add(plugin.getArtifactId());
        }

        actual.retainAll(expected);

        assertEquals(actual, expected);
    }

    @Test
    void testProjectArtifactIdIsNotInheritedButMandatory() throws Exception {
        assertThrows(
                ProjectBuildingException.class,
                () -> buildPom("artifact-id-inheritance/child"),
                "Missing artifactId did not cause validation error");
    }

    private void assertPathSuffixEquals(String expected, Object actual) {
        String a = actual.toString();
        a = a.substring(a.length() - expected.length()).replace('\\', '/');
        assertEquals(expected, a);
    }

    private void assertPathWithNormalizedFileSeparators(Object value) {
        assertEquals(new File(value.toString()).getPath(), value.toString());
    }

    private PomTestWrapper buildPom(String pomPath, String... profileIds) throws Exception {
        return buildPom(pomPath, null, null, profileIds);
    }

    private PomTestWrapper buildPom(
            String pomPath, Properties systemProperties, Properties userProperties, String... profileIds)
            throws Exception {
        return buildPom(pomPath, false, systemProperties, userProperties, profileIds);
    }

    private PomTestWrapper buildPom(
            String pomPath,
            boolean lenientValidation,
            Properties systemProperties,
            Properties userProperties,
            String... profileIds)
            throws Exception {
        File pomFile = new File(testDirectory, pomPath);
        if (pomFile.isDirectory()) {
            pomFile = new File(pomFile, "pom.xml");
        }

        ProjectBuildingRequest config = new DefaultProjectBuildingRequest();

        String localRepoUrl =
                System.getProperty("maven.repo.local", System.getProperty("user.home") + "/.m2/repository");
        localRepoUrl = "file://" + localRepoUrl;
        config.setLocalRepository(repositorySystem.createArtifactRepository(
                "local", localRepoUrl, new DefaultRepositoryLayout(), null, null));
        config.setActiveProfileIds(Arrays.asList(profileIds));
        config.setSystemProperties(systemProperties);
        config.setUserProperties(userProperties);
        config.setValidationLevel(
                lenientValidation
                        ? ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0
                        : ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        LocalRepository localRepo =
                new LocalRepository(config.getLocalRepository().getBasedir());
        repoSession.setLocalRepositoryManager(
                new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, localRepo));
        config.setRepositorySession(repoSession);

        InternalSession iSession = InternalSession.from(repoSession);
        InternalMavenSession mSession = InternalMavenSession.from(iSession);
        Path root = pomFile.getParentFile().toPath();
        while (root != null
                && !Files.isDirectory(root.resolve(".mvn"))
                && Files.isRegularFile(root.resolve("../pom.xml"))) {
            root = root.getParent();
        }
        mSession.getMavenSession().getRequest().setRootDirectory(root);

        return new PomTestWrapper(pomFile, projectBuilder.build(pomFile, config).getProject());
    }

    protected void assertModelEquals(PomTestWrapper pom, Object expected, String expression) {
        assertEquals(expected, pom.getValue(expression));
    }

    private static String createPath(List<String> elements) {
        StringBuilder buffer = new StringBuilder(256);
        for (String s : elements) {
            buffer.append(s).append(File.separator);
        }
        return buffer.toString().substring(0, buffer.toString().length() - 1);
    }
}
