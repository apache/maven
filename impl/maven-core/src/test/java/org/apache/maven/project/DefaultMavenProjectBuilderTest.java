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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
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
import org.mockito.Mockito;

import static org.apache.maven.project.ProjectBuildingResultWithProblemMessageMatcher.projectBuildingResultWithProblemMessage;
import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
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
    void buildFromMiddlePom() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/grandchild-check/child/pom.xml");
        File f2 = getTestFile("src/test/resources/projects/grandchild-check/child/grandchild/pom.xml");

        getProject(f1);

        // it's the building of the grandchild project, having already cached the child project
        // (but not the parent project), which causes the problem.
        getProject(f2);
    }

    @Disabled("Maven 4 does not allow duplicate plugin declarations")
    @Test
    void duplicatePluginDefinitionsMerged() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/duplicate-plugins-merged-pom.xml");

        MavenProject project = getProject(f1);
        assertEquals(2, project.getBuildPlugins().get(0).getDependencies().size());
        assertEquals(2, project.getBuildPlugins().get(0).getExecutions().size());
        assertEquals(
                "first", project.getBuildPlugins().get(0).getExecutions().get(0).getId());
    }

    @Test
    void futureModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for future versions");
        assertThat(e.getMessage(), containsString("Building this project requires a newer version of Maven"));
    }

    @Test
    void pastModelVersion() throws Exception {
        // a Maven 1.x pom will not even
        // update the resource if we stop supporting modelVersion 4.0.0
        File f1 = getTestFile("src/test/resources/projects/past-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for past versions");
        assertThat(e.getMessage(), containsString("Building this project requires an older version of Maven"));
    }

    @Test
    void futureSchemaModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-schema-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for future versions");
        assertThat(e.getMessage(), containsString("Building this project requires a newer version of Maven"));
    }

    @Test
    void buildStubModelForMissingRemotePom() throws Exception {
        Artifact pom = repositorySystem.createProjectArtifact("org.apache.maven.its", "missing", "0.1");
        MavenProject project = getProject(pom, true);

        assertNotNull(project.getArtifactId());

        assertNotNull(project.getRemoteArtifactRepositories());
        assertTrue(project.getRemoteArtifactRepositories().isEmpty());

        assertNotNull(project.getPluginArtifactRepositories());
        assertTrue(project.getPluginArtifactRepositories().isEmpty());

        assertNull(project.getParent());
        assertNull(project.getParentArtifact());

        assertFalse(project.isExecutionRoot());
    }

    @Test
    void partialResultUponBadDependencyDeclaration() throws Exception {
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
    void buildValidParentVersionRangeLocally() throws Exception {
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
    void buildParentVersionRangeLocallyWithoutChildVersion() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-without-version/child/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Tests whether local version range parent references are built correctly.
     */
    @Test
    void buildParentVersionRangeLocallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-project-version-expression/child/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Tests whether local version range parent references are built correctly.
     */
    @Test
    void buildParentVersionRangeLocallyWithChildProjectParentVersionExpression() throws Exception {
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
    void buildParentVersionRangeLocallyWithChildRevisionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-revision-expression/child/pom.xml");

        MavenProject mp = this.getProjectFromRemoteRepository(f1);

        assertEquals("1.0-SNAPSHOT", mp.getVersion());
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    void buildParentVersionRangeExternally() throws Exception {
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
    void buildParentVersionRangeExternallyWithoutChildVersion() throws Exception {
        File f1 =
                getTestFile("src/test/resources/projects/parent-version-range-external-child-without-version/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Tests whether external version range parent references are built correctly.
     */
    @Test
    void buildParentVersionRangeExternallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-project-version-expression/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
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
        assertThat(project.getName(), is("aid")); // inherited from artifactId

        try (InputStream pomResource =
                DefaultMavenProjectBuilderTest.class.getResourceAsStream("/projects/reread/pom2.xml")) {
            Files.copy(pomResource, pom, StandardCopyOption.REPLACE_EXISTING);
        }

        project = projectBuilder.build(pom.toFile(), buildingRequest).getProject();
        assertThat(project.getName(), is("PROJECT NAME"));
    }

    @Test
    void activatedProfileBySource() throws Exception {
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

    @Test
    void activatedDefaultProfileBySource() throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        ProjectBuildingRequest request = newBuildingRequest();
        request.setLocalRepository(getLocalRepository());

        MavenProject project = projectBuilder.build(testPom, request).getProject();

        assertTrue(project.getInjectedProfileIds().keySet().containsAll(List.of("external", project.getId())));
        assertTrue(project.getInjectedProfileIds().get("external").isEmpty());
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().noneMatch("profile1"::equals));
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().noneMatch("profile2"::equals));
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().anyMatch("active-by-default"::equals));

        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        List<org.apache.maven.api.model.Profile> activeProfiles =
                new DefaultProject(session, project).getDeclaredActiveProfiles();
        assertEquals(1, activeProfiles.size());
        org.apache.maven.api.model.Profile profile = activeProfiles.get(0);
        assertEquals("active-by-default", profile.getId());
        InputLocation location = profile.getLocation("");
        assertNotNull(location);
        assertThat(location.getLineNumber(), greaterThan(0));
        assertThat(location.getColumnNumber(), greaterThan(0));
        assertNotNull(location.getSource());
        assertThat(location.getSource().getLocation(), containsString("pom-with-profiles/pom.xml"));
    }

    @Test
    void activatedExternalProfileBySource() throws Exception {
        File testPom = getTestFile("src/test/resources/projects/pom-with-profiles/pom.xml");

        ProjectBuildingRequest request = newBuildingRequest();
        request.setLocalRepository(getLocalRepository());

        final Profile externalProfile = new Profile();
        externalProfile.setLocation(
                "",
                new org.apache.maven.model.InputLocation(
                        1, 1, new org.apache.maven.model.InputSource(new InputSource(null, "settings.xml", null))));
        externalProfile.setId("external-profile");
        request.addProfile(externalProfile);
        request.setActiveProfileIds(List.of(externalProfile.getId()));

        MavenProject project = projectBuilder.build(testPom, request).getProject();

        assertTrue(project.getInjectedProfileIds().keySet().containsAll(List.of("external", project.getId())));
        assertTrue(project.getInjectedProfileIds().get("external").stream().anyMatch("external-profile"::equals));
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().noneMatch("profile1"::equals));
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().noneMatch("profile2"::equals));
        assertTrue(project.getInjectedProfileIds().get(project.getId()).stream().anyMatch("active-by-default"::equals));

        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        List<org.apache.maven.api.model.Profile> activeProfiles =
                new DefaultProject(session, project).getDeclaredActiveProfiles();
        assertEquals(2, activeProfiles.size());
        org.apache.maven.api.model.Profile profile = activeProfiles.get(0);
        assertEquals("active-by-default", profile.getId());
        InputLocation location = profile.getLocation("");
        assertNotNull(location);
        assertThat(location.getLineNumber(), greaterThan(0));
        assertThat(location.getColumnNumber(), greaterThan(0));
        assertNotNull(location.getSource());
        assertThat(location.getSource().getLocation(), containsString("pom-with-profiles/pom.xml"));
        profile = activeProfiles.get(1);
        assertEquals("external-profile", profile.getId());
        location = profile.getLocation("");
        assertNotNull(location);
        assertThat(location.getLineNumber(), greaterThan(0));
        assertThat(location.getColumnNumber(), greaterThan(0));
        assertNotNull(location.getSource());
        assertThat(location.getSource().getLocation(), containsString("settings.xml"));
    }

    @Test
    void activatedProfileIsResolved() throws Exception {
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
    void activatedProfileByDefaultIsResolved() throws Exception {
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
    void buildParentVersionRangeExternallyWithChildPomVersionExpression() throws Exception {
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
    void buildParentVersionRangeExternallyWithChildPomParentVersionExpression() throws Exception {
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
    void buildParentVersionRangeExternallyWithChildProjectParentVersionExpression() throws Exception {
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
    void buildParentVersionRangeExternallyWithChildRevisionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-revision-expression/pom.xml");

        MavenProject mp = this.getProjectFromRemoteRepository(f1);

        assertEquals("1.0-SNAPSHOT", mp.getVersion());
    }

    @Test
    void parentVersionResolvedFromNestedProperties() throws Exception {
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
    void subprojectDiscovery() throws Exception {
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
}
