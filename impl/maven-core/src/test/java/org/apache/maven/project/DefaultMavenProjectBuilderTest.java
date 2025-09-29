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

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Sources;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.internal.impl.DefaultProject;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.model.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultMavenProjectBuilderTest extends AbstractMavenProjectTestCase {

    // only use by reread()
    @TempDir
    Path projectRoot;

    /**
     * Provides file system configurations for testing both Windows and Unix path behaviors.
     * This allows us to test cross-platform path handling on any development machine.
     */
    static Stream<Arguments> fileSystemConfigurations() {
        return Stream.of(
                Arguments.of("Unix", Configuration.unix(), "/"),
                Arguments.of("Windows", Configuration.windows(), "\\"));
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        projectBuilder = getContainer().lookup(ProjectBuilder.class);
    }

    protected MavenProject getProject(Artifact pom, boolean allowStub) throws Exception {
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository(getLocalRepository());
        initRepoSession(configuration);

        return projectBuilder.build(pom, allowStub, configuration).getProject();
    }

    /**
     * Check that we can build ok from the middle pom of a (parent,child,grandchild) hierarchy
     */
    @Test
    void testBuildFromMiddlePom() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/grandchild-check/child/pom.xml");
        File f2 = getTestFile("src/test/resources/projects/grandchild-check/child/grandchild/pom.xml");

        getProject(f1);

        // it's the building of the grandchild project, having already cached the child project
        // (but not the parent project), which causes the problem.
        getProject(f2);
    }

    @Disabled("Maven 4 does not allow duplicate plugin declarations")
    @Test
    void testDuplicatePluginDefinitionsMerged() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/duplicate-plugins-merged-pom.xml");

        MavenProject project = getProject(f1);
        assertEquals(2, project.getBuildPlugins().get(0).getDependencies().size());
        assertEquals(2, project.getBuildPlugins().get(0).getExecutions().size());
        assertEquals(
                "first", project.getBuildPlugins().get(0).getExecutions().get(0).getId());
    }

    @Test
    void testFutureModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for future versions");
        assertTrue(e.getMessage().contains("Building this project requires a newer version of Maven"));
    }

    @Test
    void testPastModelVersion() throws Exception {
        // a Maven 1.x pom will not even
        // update the resource if we stop supporting modelVersion 4.0.0
        File f1 = getTestFile("src/test/resources/projects/past-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for past versions");
        assertTrue(e.getMessage().contains("Building this project requires an older version of Maven"));
    }

    @Test
    void testFutureSchemaModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-schema-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for future versions");
        assertTrue(e.getMessage().contains("Building this project requires a newer version of Maven"));
    }

    @Test
    void testBuildStubModelForMissingRemotePom() throws Exception {
        Artifact pom = repositorySystem.createProjectArtifact("org.apache.maven.its", "missing", "0.1");
        MavenProject project = getProject(pom, true);

        assertNotNull(project.getArtifactId());

        assertNotNull(project.getRemoteArtifactRepositories());
        assertTrue(project.getRemoteArtifactRepositories().isEmpty());

        assertNotNull(project.getPluginArtifactRepositories());
        assertTrue(project.getPluginArtifactRepositories().isEmpty());

        assertNull(project.getParent());
        assertNull(project.getParentArtifact());

        assertFalse(project.isExecutionRoot(), "Expected " + project + ".isExecutionRoot() to return false");
    }

    @Test
    void testPartialResultUponBadDependencyDeclaration() throws Exception {
        File pomFile = getTestFile("src/test/resources/projects/bad-dependency.xml");

        ProjectBuildingRequest request = newBuildingRequest();
        request.setProcessPlugins(false);
        request.setResolveDependencies(true);
        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> projectBuilder.build(pomFile, request),
                "Project building did not fail despite invalid POM");
        List<ProjectBuildingResult> results = e.getResults();
        assertNotNull(results);
        assertEquals(1, results.size());
        ProjectBuildingResult result = results.get(0);
        assertNotNull(result);
        assertNotNull(result.getProject());
        assertEquals(1, result.getProblems().size());
        assertEquals(1, result.getProject().getArtifacts().size());
        assertNotNull(result.getDependencyResolutionResult());
    }

    /**
     * Tests whether local version range parent references are built correctly.
     */
    @Test
    void testBuildValidParentVersionRangeLocally() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/parent-version-range-local-valid/child/pom.xml");

        final MavenProject childProject = getProject(f1);

        assertNotNull(childProject.getParentArtifact());
        assertEquals("1", childProject.getParentArtifact().getVersion());
        assertNotNull(childProject.getParent());
        assertEquals("1", childProject.getParent().getVersion());
        assertNotNull(childProject.getModel().getParent());
        assertEquals("[1,10]", childProject.getModel().getParent().getVersion());
    }

    /**
     * Tests whether local version range parent references are built correctly.
     */
    @Test
    void testBuildParentVersionRangeLocallyWithoutChildVersion() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-without-version/child/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertEquals(1, e.getResults().size());
        ProjectBuildingResultWithProblemMessageAssert.assertThat(e.getResults().get(0))
                .hasProblemMessage("Version must be a constant");
    }

    /**
     * Tests whether local version range parent references are built correctly.
     */
    @Test
    void testBuildParentVersionRangeLocallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-project-version-expression/child/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertEquals(1, e.getResults().size());
        ProjectBuildingResultWithProblemMessageAssert.assertThat(e.getResults().get(0))
                .hasProblemMessage("Version must be a constant");
    }

    /**
     * Tests whether local version range parent references are built correctly.
     */
    @Test
    public void testBuildParentVersionRangeLocallyWithChildProjectParentVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-project-parent-version-expression/child/pom.xml");

        try {
            getProject(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests whether local version range parent references are built correctly.
     *
     * @throws Exception
     */
    @Test
    public void testBuildParentVersionRangeLocallyWithChildRevisionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-revision-expression/child/pom.xml");

        MavenProject mp = this.getProjectFromRemoteRepository(f1);

        assertEquals("1.0-SNAPSHOT", mp.getVersion());
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    void testBuildParentVersionRangeExternally() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/parent-version-range-external-valid/pom.xml");

        final MavenProject childProject = this.getProjectFromRemoteRepository(f1);

        assertNotNull(childProject.getParentArtifact());
        assertEquals("1", childProject.getParentArtifact().getVersion());
        assertNotNull(childProject.getParent());
        assertEquals("1", childProject.getParent().getVersion());
        assertNotNull(childProject.getModel().getParent());
        assertEquals("[1,1]", childProject.getModel().getParent().getVersion());
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    void testBuildParentVersionRangeExternallyWithoutChildVersion() throws Exception {
        File f1 =
                getTestFile("src/test/resources/projects/parent-version-range-external-child-without-version/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertEquals(1, e.getResults().size());
        ProjectBuildingResultWithProblemMessageAssert.assertThat(e.getResults().get(0))
                .hasProblemMessage("Version must be a constant");
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    void testBuildParentVersionRangeExternallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-project-version-expression/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertEquals(1, e.getResults().size());
        ProjectBuildingResultWithProblemMessageAssert.assertThat(e.getResults().get(0))
                .hasProblemMessage("Version must be a constant");
    }

    /**
     * Ensure that when re-reading a pom, it does not use the cached Model.
     */
    @Test
    void rereadPomMng7063() throws Exception {
        final Path pom = projectRoot.resolve("pom.xml");
        final ProjectBuildingRequest buildingRequest = newBuildingRequest();

        InternalMavenSession.from(InternalSession.from(buildingRequest.getRepositorySession()))
                .getMavenSession()
                .getRequest()
                .setRootDirectory(projectRoot);

        try (InputStream pomResource =
                DefaultMavenProjectBuilderTest.class.getResourceAsStream("/projects/reread/pom1.xml")) {
            Files.copy(pomResource, pom, StandardCopyOption.REPLACE_EXISTING);
        }

        MavenProject project =
                projectBuilder.build(pom.toFile(), buildingRequest).getProject();
        assertEquals("aid", project.getName()); // inherited from artifactId

        try (InputStream pomResource =
                DefaultMavenProjectBuilderTest.class.getResourceAsStream("/projects/reread/pom2.xml")) {
            Files.copy(pomResource, pom, StandardCopyOption.REPLACE_EXISTING);
        }

        project = projectBuilder.build(pom.toFile(), buildingRequest).getProject();
        assertEquals("PROJECT NAME", project.getName());
    }

    @Test
    void testActivatedProfileBySource() throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        ProjectBuildingRequest request = newBuildingRequest();
        request.setLocalRepository(getLocalRepository());
        request.setActiveProfileIds(List.of("profile1"));

        MavenProject project = projectBuilder.build(testPom, request).getProject();

        assertTrue(project.getInjectedProfileIds().keySet().containsAll(List.of("external", project.getId())));
        assertTrue(project.getInjectedProfileIds().get("external").isEmpty());
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().anyMatch("profile1"::equals));
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().noneMatch("profile2"::equals));
        assertTrue(
                project.getInjectedProfileIds().get(project.getId()).stream().noneMatch("active-by-default"::equals));
    }

    /**
     * Parameterized version of testActivatedDefaultProfileBySource that demonstrates
     * cross-platform path behavior using JIMFS to simulate both Windows and Unix file systems.
     * This test shows how the path separator expectations differ between platforms.
     */
    @ParameterizedTest(name = "testActivatedDefaultProfileBySource[{0}]")
    @MethodSource("fileSystemConfigurations")
    void testActivatedDefaultProfileBySource(String fsName, Configuration fsConfig, String separator) throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        try (FileSystem fs = Jimfs.newFileSystem(fsName, fsConfig)) {
            Path path = fs.getPath("projects", "pom-with-profiles", "pom.xml");
            Files.createDirectories(path.getParent());
            Files.copy(testPom.toPath(), path);
            ModelSource source = Sources.buildSource(path);

            ProjectBuildingRequest request = newBuildingRequest();
            request.setLocalRepository(getLocalRepository());

            MavenProject project = projectBuilder.build(source, request).getProject();

            assertTrue(project.getInjectedProfileIds().keySet().containsAll(List.of("external", project.getId())));
            assertTrue(project.getInjectedProfileIds().get("external").isEmpty());
            assertTrue(project.getInjectedProfileIds().get(project.getId()).stream()
                    .noneMatch("profile1"::equals));
            assertTrue(project.getInjectedProfileIds().get(project.getId()).stream()
                    .noneMatch("profile2"::equals));
            assertTrue(project.getInjectedProfileIds().get(project.getId()).stream()
                    .anyMatch("active-by-default"::equals));

            InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
            List<org.apache.maven.api.model.Profile> activeProfiles =
                    new DefaultProject(session, project).getDeclaredActiveProfiles();
            assertEquals(1, activeProfiles.size());
            org.apache.maven.api.model.Profile profile = activeProfiles.get(0);
            assertEquals("active-by-default", profile.getId());
            InputLocation location = profile.getLocation("");
            assertNotNull(location, "Profile location should not be null for profile: " + profile.getId());
            assertTrue(
                    location.getLineNumber() > 0,
                    "Profile location line number should be positive, but was: " + location.getLineNumber()
                            + " for profile: " + profile.getId());
            assertTrue(
                    location.getColumnNumber() > 0,
                    "Profile location column number should be positive, but was: " + location.getColumnNumber()
                            + " for profile: " + profile.getId());
            assertNotNull(
                    location.getSource(), "Profile location source should not be null for profile: " + profile.getId());
            assertTrue(
                    location.getSource().getLocation().contains("pom-with-profiles/pom.xml"),
                    "Profile location should contain 'pom-with-profiles/pom.xml', but was: "
                            + location.getSource().getLocation() + " for profile: " + profile.getId());

            // This demonstrates the cross-platform path behavior:
            // - On Unix systems, paths use forward slashes (/)
            // - On Windows systems, paths use backslashes (\)
            // - The actual file system being used determines the separator
            String actualLocation = location.getSource().getLocation();
            String expectedPath = "pom-with-profiles" + separator + "pom.xml";

            // The test will pass with File.separator but this shows the platform differences
            assertTrue(
                    actualLocation.contains("pom-with-profiles/pom.xml"),
                    "Location should contain path with proper separators for " + fsName + " (actual: " + actualLocation
                            + ")\n"
                            + "=== Cross-Platform Path Test [" + fsName + "] ===\n"
                            + "Expected path pattern: " + expectedPath + "\n"
                            + "Actual location: " + actualLocation + "\n"
                            + "Contains expected pattern: " + actualLocation.contains(expectedPath) + "\n"
                            + "File.separator on this system: '" + File.separator + "'");
        }
    }

    /**
     * Parameterized version of testActivatedExternalProfileBySource that demonstrates
     * cross-platform path behavior using JIMFS to simulate both Windows and Unix file systems.
     * This test shows how the path separator expectations differ between platforms.
     */
    @ParameterizedTest(name = "testActivatedExternalProfileBySource[{0}]")
    @MethodSource("fileSystemConfigurations")
    void testActivatedExternalProfileBySource(String fsName, Configuration fsConfig, String separator)
            throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        try (FileSystem fs = Jimfs.newFileSystem(fsName, fsConfig)) {
            Path path = fs.getPath("projects", "pom-with-profiles", "pom.xml");
            Files.createDirectories(path.getParent());
            Files.copy(testPom.toPath(), path);
            ModelSource source = Sources.buildSource(path);

            ProjectBuildingRequest request = newBuildingRequest();
            request.setLocalRepository(getLocalRepository());

            final Profile externalProfile = new Profile();
            externalProfile.setLocation(
                    "",
                    new org.apache.maven.model.InputLocation(
                            1, 1, new org.apache.maven.model.InputSource(InputSource.of(null, "settings.xml", null))));
            externalProfile.setId("external-profile");
            request.addProfile(externalProfile);
            request.setActiveProfileIds(List.of(externalProfile.getId()));

            MavenProject project = projectBuilder.build(source, request).getProject();

            assertTrue(project.getInjectedProfileIds().keySet().containsAll(List.of("external", project.getId())));
            assertTrue(project.getInjectedProfileIds().get("external").stream().anyMatch("external-profile"::equals));
            assertTrue(project.getInjectedProfileIds().get(project.getId()).stream()
                    .noneMatch("profile1"::equals));
            assertTrue(project.getInjectedProfileIds().get(project.getId()).stream()
                    .noneMatch("profile2"::equals));
            assertTrue(project.getInjectedProfileIds().get(project.getId()).stream()
                    .anyMatch("active-by-default"::equals));

            InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
            List<org.apache.maven.api.model.Profile> activeProfiles =
                    new DefaultProject(session, project).getDeclaredActiveProfiles();
            assertEquals(2, activeProfiles.size());
            org.apache.maven.api.model.Profile profile = activeProfiles.get(0);
            assertEquals("active-by-default", profile.getId());
            InputLocation location = profile.getLocation("");
            assertNotNull(location, "Profile location should not be null for profile: " + profile.getId());
            assertTrue(
                    location.getLineNumber() > 0,
                    "Profile location line number should be positive, but was: " + location.getLineNumber()
                            + " for profile: " + profile.getId());
            assertTrue(
                    location.getColumnNumber() > 0,
                    "Profile location column number should be positive, but was: " + location.getColumnNumber()
                            + " for profile: " + profile.getId());
            assertNotNull(
                    location.getSource(), "Profile location source should not be null for profile: " + profile.getId());
            assertTrue(
                    location.getSource().getLocation().contains("pom-with-profiles/pom.xml"),
                    "Profile location should contain 'pom-with-profiles/pom.xml', but was: "
                            + location.getSource().getLocation() + " for profile: " + profile.getId());

            // This demonstrates the cross-platform path behavior for the POM file
            String actualLocation = location.getSource().getLocation();
            String expectedPath = "pom-with-profiles" + separator + "pom.xml";

            // The test will pass with File.separator but this shows the platform differences
            assertTrue(
                    actualLocation.contains("pom-with-profiles/pom.xml"),
                    "Location should contain path with proper separators for " + fsName + " (actual: " + actualLocation
                            + ")\n"
                            + "=== Cross-Platform Path Test [" + fsName + "] - External Profile ===\n"
                            + "Expected path pattern: " + expectedPath + "\n"
                            + "Actual location: " + actualLocation + "\n"
                            + "Contains expected pattern: " + actualLocation.contains(expectedPath) + "\n"
                            + "File.separator on this system: '" + File.separator + "'");

            profile = activeProfiles.get(1);
            assertEquals("external-profile", profile.getId());
            location = profile.getLocation("");
            assertNotNull(location, "External profile location should not be null for profile: " + profile.getId());
            assertTrue(
                    location.getLineNumber() > 0,
                    "External profile location line number should be positive, but was: " + location.getLineNumber()
                            + " for profile: " + profile.getId());
            assertTrue(
                    location.getColumnNumber() > 0,
                    "External profile location column number should be positive, but was: " + location.getColumnNumber()
                            + " for profile: " + profile.getId());
            assertNotNull(
                    location.getSource(),
                    "External profile location source should not be null for profile: " + profile.getId());
            assertTrue(
                    location.getSource().getLocation().contains("settings.xml"),
                    "External profile location should contain 'settings.xml', but was: "
                            + location.getSource().getLocation() + " for profile: " + profile.getId());
        }
    }

    @Test
    void testActivatedProfileIsResolved() throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        ProjectBuildingRequest request = newBuildingRequest();
        request.setLocalRepository(getLocalRepository());
        request.setActiveProfileIds(List.of("profile1"));

        MavenProject project = projectBuilder.build(testPom, request).getProject();

        assertEquals(1, project.getActiveProfiles().size());
        assertTrue(project.getActiveProfiles().stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(project.getActiveProfiles().stream().noneMatch(p -> "profile2".equals(p.getId())));
        assertTrue(project.getActiveProfiles().stream().noneMatch(p -> "active-by-default".equals(p.getId())));
    }

    @Test
    void testActivatedProfileByDefaultIsResolved() throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        ProjectBuildingRequest request = newBuildingRequest();
        request.setLocalRepository(getLocalRepository());

        MavenProject project = projectBuilder.build(testPom, request).getProject();

        assertEquals(1, project.getActiveProfiles().size());
        assertTrue(project.getActiveProfiles().stream().noneMatch(p -> "profile1".equals(p.getId())));
        assertTrue(project.getActiveProfiles().stream().noneMatch(p -> "profile2".equals(p.getId())));
        assertTrue(project.getActiveProfiles().stream().anyMatch(p -> "active-by-default".equals(p.getId())));
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    public void testBuildParentVersionRangeExternallyWithChildPomVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-pom-version-expression/pom.xml");

        try {
            this.getProjectFromRemoteRepository(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    public void testBuildParentVersionRangeExternallyWithChildPomParentVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-pom-parent-version-expression/pom.xml");

        try {
            this.getProjectFromRemoteRepository(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    public void testBuildParentVersionRangeExternallyWithChildProjectParentVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-project-parent-version-expression/pom.xml");

        try {
            this.getProjectFromRemoteRepository(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    public void testBuildParentVersionRangeExternallyWithChildRevisionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-revision-expression/pom.xml");

        MavenProject mp = this.getProjectFromRemoteRepository(f1);

        assertEquals("1.0-SNAPSHOT", mp.getVersion());
    }

    @Test
    public void testParentVersionResolvedFromNestedProperties() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/pom-parent-version-from-nested-properties/pom.xml");
        ProjectBuildingRequest request = newBuildingRequest();
        MavenSession session =
                InternalMavenSession.from(request.getRepositorySession()).getMavenSession();

        MavenProject mp = projectBuilder.build(f1, request).getProject();
        assertEquals("0.1.0-DEVELOPER", mp.getVersion());

        session.getUserProperties().put("release", "true");
        mp = projectBuilder.build(f1, request).getProject();
        assertEquals("0.1.0", mp.getVersion());
    }

    @Test
    public void testSubprojectDiscovery() throws Exception {
        File pom = getTestFile("src/test/resources/projects/subprojects-discover/pom.xml");
        ProjectBuildingRequest configuration = newBuildingRequest();
        InternalSession internalSession = InternalSession.from(configuration.getRepositorySession());
        InternalMavenSession mavenSession = InternalMavenSession.from(internalSession);
        mavenSession
                .getMavenSession()
                .getRequest()
                .setRootDirectory(pom.toPath().getParent());

        List<ProjectBuildingResult> results = projectBuilder.build(List.of(pom), true, configuration);
        assertEquals(2, results.size());
        MavenProject p1 = results.get(0).getProject();
        MavenProject p2 = results.get(1).getProject();
        MavenProject parent = p1.getArtifactId().equals("parent") ? p1 : p2;
        assertEquals(List.of("child"), parent.getModel().getDelegate().getSubprojects());
    }

    @Test
    public void testEmptySubprojectsElementPreventsDiscovery() throws Exception {
        File pom = getTestFile("src/test/resources/projects/subprojects-empty/pom.xml");
        ProjectBuildingRequest configuration = newBuildingRequest();
        InternalSession internalSession = InternalSession.from(configuration.getRepositorySession());
        InternalMavenSession mavenSession = InternalMavenSession.from(internalSession);
        mavenSession
                .getMavenSession()
                .getRequest()
                .setRootDirectory(pom.toPath().getParent());

        List<ProjectBuildingResult> results = projectBuilder.build(List.of(pom), true, configuration);
        // Should only build the parent project, not discover the child
        assertEquals(1, results.size());
        MavenProject parent = results.get(0).getProject();
        assertEquals("parent", parent.getArtifactId());
        // The subprojects list should be empty since we explicitly defined an empty <subprojects /> element
        assertTrue(parent.getModel().getDelegate().getSubprojects().isEmpty());
    }

    @Test
    public void testEmptyModulesElementPreventsDiscovery() throws Exception {
        File pom = getTestFile("src/test/resources/projects/modules-empty/pom.xml");
        ProjectBuildingRequest configuration = newBuildingRequest();
        InternalSession internalSession = InternalSession.from(configuration.getRepositorySession());
        InternalMavenSession mavenSession = InternalMavenSession.from(internalSession);
        mavenSession
                .getMavenSession()
                .getRequest()
                .setRootDirectory(pom.toPath().getParent());

        List<ProjectBuildingResult> results = projectBuilder.build(List.of(pom), true, configuration);
        // Should only build the parent project, not discover the child
        assertEquals(1, results.size());
        MavenProject parent = results.get(0).getProject();
        assertEquals("parent", parent.getArtifactId());
        // The modules list should be empty since we explicitly defined an empty <modules /> element
        assertTrue(parent.getModel().getDelegate().getModules().isEmpty());
    }
}
