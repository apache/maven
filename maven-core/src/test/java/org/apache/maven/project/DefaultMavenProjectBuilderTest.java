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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.util.FileUtils;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class DefaultMavenProjectBuilderTest extends AbstractMavenProjectTestCase {

    private List<File> filesToDelete = new ArrayList<>();

    private File localRepoDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        projectBuilder = lookup(ProjectBuilder.class);

        localRepoDir = new File(System.getProperty("java.io.tmpdir"), "local-repo." + System.currentTimeMillis());
        localRepoDir.mkdirs();

        filesToDelete.add(localRepoDir);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        if (!filesToDelete.isEmpty()) {
            for (File file : filesToDelete) {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        FileUtils.deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }

    protected MavenProject getProject(Artifact pom, boolean allowStub) throws Exception {
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository(getLocalRepository());
        initRepoSession(configuration);

        return projectBuilder.build(pom, allowStub, configuration).getProject();
    }

    /**
     * Check that we can build ok from the middle pom of a (parent,child,grandchild) hierarchy
     * @throws Exception
     */
    public void testBuildFromMiddlePom() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/grandchild-check/child/pom.xml");
        File f2 = getTestFile("src/test/resources/projects/grandchild-check/child/grandchild/pom.xml");

        getProject(f1);

        // it's the building of the grandchild project, having already cached the child project
        // (but not the parent project), which causes the problem.
        getProject(f2);
    }

    public void testDuplicatePluginDefinitionsMerged() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/duplicate-plugins-merged-pom.xml");

        MavenProject project = getProject(f1);
        assertEquals(2, project.getBuildPlugins().get(0).getDependencies().size());
        assertEquals(2, project.getBuildPlugins().get(0).getExecutions().size());
        assertEquals(
                "first", project.getBuildPlugins().get(0).getExecutions().get(0).getId());
    }

    public void testFutureModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-model-version-pom.xml");

        try {
            getProject(f1);
            fail("Expected to fail for future versions");
        } catch (ProjectBuildingException e) {
            assertContains("Building this project requires a newer version of Maven", e.getMessage());
        }
    }

    public void testPastModelVersion() throws Exception {
        // a Maven 1.x pom will not even
        // update the resource if we stop supporting modelVersion 4.0.0
        File f1 = getTestFile("src/test/resources/projects/past-model-version-pom.xml");

        try {
            getProject(f1);
            fail("Expected to fail for past versions");
        } catch (ProjectBuildingException e) {
            assertContains("Building this project requires an older version of Maven", e.getMessage());
        }
    }

    public void testFutureSchemaModelVersion() throws Exception {
        File f1 = getTestFile("src/test/resources/projects/future-schema-model-version-pom.xml");

        try {
            getProject(f1);
            fail("Expected to fail for future versions");
        } catch (ProjectBuildingException e) {
            assertContains("Building this project requires a newer version of Maven", e.getMessage());
        }
    }

    private void assertContains(String expected, String actual) {
        if (actual == null || !actual.contains(expected)) {
            fail("Expected: a string containing " + expected + "\nActual: "
                    + (actual == null ? "null" : "'" + actual + "'"));
        }
    }

    public void testBuildStubModelForMissingRemotePom() throws Exception {
        Artifact pom = repositorySystem.createProjectArtifact("org.apache.maven.its", "missing", "0.1");
        MavenProject project = getProject(pom, true);

        assertNotNull(project.getArtifactId());

        assertNotNull(project.getRemoteArtifactRepositories());
        assertFalse(project.getRemoteArtifactRepositories().isEmpty());

        assertNotNull(project.getPluginArtifactRepositories());
        assertFalse(project.getPluginArtifactRepositories().isEmpty());

        assertNull(project.getParent());
        assertNull(project.getParentArtifact());

        assertFalse(project.isExecutionRoot());
    }

    @Override
    protected ArtifactRepository getLocalRepository() throws Exception {
        ArtifactRepositoryLayout repoLayout = lookup(ArtifactRepositoryLayout.class, "default");
        ArtifactRepository r = repositorySystem.createArtifactRepository(
                "local", "file://" + localRepoDir.getAbsolutePath(), repoLayout, null, null);
        return r;
    }

    public void xtestLoop() throws Exception {
        while (true) {
            File f1 = getTestFile("src/test/resources/projects/duplicate-plugins-merged-pom.xml");
            getProject(f1);
        }
    }

    public void testPartialResultUponBadDependencyDeclaration() throws Exception {
        File pomFile = getTestFile("src/test/resources/projects/bad-dependency.xml");

        try {
            ProjectBuildingRequest request = newBuildingRequest();
            request.setProcessPlugins(false);
            request.setResolveDependencies(true);
            projectBuilder.build(pomFile, request);
            fail("Project building did not fail despite invalid POM");
        } catch (ProjectBuildingException e) {
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
    }

    public void testImportScopePomResolvesFromPropertyBasedRepository() throws Exception {
        File pomFile =
                getTestFile("src/test/resources/projects/import-scope-pom-resolves-from-property-based-repository.xml");
        ProjectBuildingRequest request = newBuildingRequest();
        request.setProcessPlugins(false);
        request.setResolveDependencies(true);
        projectBuilder.build(pomFile, request);
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception
     */
    public void testBuildValidParentVersionRangeLocally() throws Exception {
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
     * @throws Exception
     */
    public void testBuildParentVersionRangeLocallyWithoutChildVersion() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-without-version/child/pom.xml");

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
    public void testBuildParentVersionRangeLocallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-project-version-expression/child/pom.xml");

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
     * @throws Exception
     */
    public void testBuildParentVersionRangeExternally() throws Exception {
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
     * @throws Exception
     */
    public void testBuildParentVersionRangeExternallyWithoutChildVersion() throws Exception {
        File f1 =
                getTestFile("src/test/resources/projects/parent-version-range-external-child-without-version/pom.xml");

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
    public void testBuildParentVersionRangeExternallyWithChildProjectVersionExpression() throws Exception {
        File f1 = getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-project-version-expression/pom.xml");

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
}
