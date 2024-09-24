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

import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.model.ModelCache;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.InternalSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.project.ProjectBuildingResultWithProblemMessageMatcher.projectBuildingResultWithProblemMessage;
import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultMavenProjectBuilderTest extends AbstractMavenProjectTestCase {
    @TempDir
    File localRepoDir;

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
     *
     * @throws Exception in case of issue
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
        assertThat(e.getMessage(), containsString("Building this project requires a newer version of Maven"));
    }

    @Test
    void testPastModelVersion() throws Exception {
        // a Maven 1.x pom will not even
        // update the resource if we stop supporting modelVersion 4.0.0
        File f1 = getTestFile("src/test/resources/projects/past-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for past versions");
        assertThat(e.getMessage(), containsString("Building this project requires an older version of Maven"));
    }

    @Test
    void testFutureSchemaModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-schema-model-version-pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class, () -> getProject(f1), "Expected to fail for future versions");
        assertThat(e.getMessage(), containsString("Building this project requires a newer version of Maven"));
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

        assertFalse(project.isExecutionRoot());
    }

    @Override
    protected ArtifactRepository getLocalRepository() throws Exception {
        return repositorySystem.createLocalRepository(getLocalRepositoryPath());
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
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    void testBuildValidParentVersionRangeLocally() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/parent-version-range-local-valid/child/pom.xml");

        final MavenProject childProject = getProject(f1);

        assertNotNull(childProject.getParentArtifact());
        assertEquals(childProject.getParentArtifact().getVersion(), "1");
        assertNotNull(childProject.getParent());
        assertEquals(childProject.getParent().getVersion(), "1");
        assertNotNull(childProject.getModel().getParent());
        assertEquals(childProject.getModel().getParent().getVersion(), "[1,10]");
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    void testBuildParentVersionRangeLocallyWithoutChildVersion() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-without-version/child/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    void testBuildParentVersionRangeLocallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-project-version-expression/child/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildParentVersionRangeLocallyWithChildProjectParentVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-project-parent-version-expression/child/pom.xml");

        try {
            getProject(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
            assertThat(e.getMessage(), containsString("Version must be a constant"));
        }
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildParentVersionRangeLocallyWithChildRevisionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-revision-expression/child/pom.xml");

        MavenProject mp = this.getProjectFromRemoteRepository(f1);

        assertEquals("1.0-SNAPSHOT", mp.getVersion());
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    void testBuildParentVersionRangeExternally() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/parent-version-range-external-valid/pom.xml");

        final MavenProject childProject = this.getProjectFromRemoteRepository(f1);

        assertNotNull(childProject.getParentArtifact());
        assertEquals(childProject.getParentArtifact().getVersion(), "1");
        assertNotNull(childProject.getParent());
        assertEquals(childProject.getParent().getVersion(), "1");
        assertNotNull(childProject.getModel().getParent());
        assertEquals(childProject.getModel().getParent().getVersion(), "[1,1]");
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    void testBuildParentVersionRangeExternallyWithoutChildVersion() throws Exception {
        File f1 =
                getTestFile("src/test/resources/projects/parent-version-range-external-child-without-version/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    void testBuildParentVersionRangeExternallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-project-version-expression/pom.xml");

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository(f1),
                "Expected 'ProjectBuildingException' not thrown.");
        assertThat(e.getResults(), contains(projectBuildingResultWithProblemMessage("Version must be a constant")));
    }

    /**
     * Ensure that when re-reading a pom, it should not use the cached Model
     *
     * @throws Exception in case of issue
     */
    @Test
    void rereadPom_mng7063() throws Exception {
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

        // clear the cache
        InternalSession.from(buildingRequest.getRepositorySession())
                .getData()
                .get(SessionData.key(ModelCache.class))
                .clear();

        try (InputStream pomResource =
                DefaultMavenProjectBuilderTest.class.getResourceAsStream("/projects/reread/pom2.xml")) {
            Files.copy(pomResource, pom, StandardCopyOption.REPLACE_EXISTING);
        }

        project = projectBuilder.build(pom.toFile(), buildingRequest).getProject();
        assertThat(project.getName(), is("PROJECT NAME"));
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildParentVersionRangeExternallyWithChildPomVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-pom-version-expression/pom.xml");

        try {
            this.getProjectFromRemoteRepository(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
            assertThat(e.getMessage(), containsString("Version must be a constant"));
        }
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildParentVersionRangeExternallyWithChildPomParentVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-pom-parent-version-expression/pom.xml");

        try {
            this.getProjectFromRemoteRepository(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
            assertThat(e.getMessage(), containsString("Version must be a constant"));
        }
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildParentVersionRangeExternallyWithChildProjectParentVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-project-parent-version-expression/pom.xml");

        try {
            this.getProjectFromRemoteRepository(f1);
            fail("Expected 'ProjectBuildingException' not thrown.");
        } catch (final ProjectBuildingException e) {
            assertNotNull(e.getMessage());
            assertThat(e.getMessage(), containsString("Version must be a constant"));
        }
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildParentVersionRangeExternallyWithChildRevisionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-revision-expression/pom.xml");

        MavenProject mp = this.getProjectFromRemoteRepository(f1);

        assertEquals("1.0-SNAPSHOT", mp.getVersion());
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
        MavenProject child = p1.getArtifactId().equals("parent") ? p2 : p1;
        assertEquals(List.of("child"), parent.getModel().getDelegate().getSubprojects());
    }
}
