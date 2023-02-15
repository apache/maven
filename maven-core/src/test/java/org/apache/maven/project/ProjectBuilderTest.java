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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ProjectBuilderTest extends AbstractCoreMavenComponentTestCase {
    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/project-builder";
    }

    public void testSystemScopeDependencyIsPresentInTheCompileClasspathElements() throws Exception {
        File pom = getProject("it0063");

        Properties eps = new Properties();
        eps.setProperty("jre.home", new File(pom.getParentFile(), "jdk/jre").getPath());

        MavenSession session = createMavenSession(pom, eps);
        MavenProject project = session.getCurrentProject();

        // Here we will actually not have any artifacts because the ProjectDependenciesResolver is not involved here. So
        // right now it's not valid to ask for artifacts unless plugins require the artifacts.

        project.getCompileClasspathElements();
    }

    public void testBuildFromModelSource() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsource/module01/pom.xml");
        MavenSession mavenSession = createMavenSession(pomFile);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        ModelSource modelSource = new FileModelSource(pomFile);
        ProjectBuildingResult result =
                lookup(org.apache.maven.project.ProjectBuilder.class).build(modelSource, configuration);

        assertNotNull(result.getProject().getParentFile());
    }

    public void testVersionlessManagedDependency() throws Exception {
        File pomFile = new File("src/test/resources/projects/versionless-managed-dependency.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        try {
            lookup(org.apache.maven.project.ProjectBuilder.class).build(pomFile, configuration);
            fail();
        } catch (ProjectBuildingException e) {
            // this is expected
        }
    }

    public void testResolveDependencies() throws Exception {
        File pomFile = new File("src/test/resources/projects/basic-resolveDependencies.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        configuration.setResolveDependencies(true);

        // single project build entry point
        ProjectBuildingResult result =
                lookup(org.apache.maven.project.ProjectBuilder.class).build(pomFile, configuration);
        assertEquals(1, result.getProject().getArtifacts().size());
        // multi projects build entry point
        List<ProjectBuildingResult> results = lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(Collections.singletonList(pomFile), false, configuration);
        assertEquals(1, results.size());
        MavenProject mavenProject = results.get(0).getProject();
        assertEquals(1, mavenProject.getArtifacts().size());

        final MavenProject project = mavenProject;
        final AtomicInteger artifactsResultInAnotherThread = new AtomicInteger();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                artifactsResultInAnotherThread.set(project.getArtifacts().size());
            }
        });
        t.start();
        t.join();
        assertEquals(project.getArtifacts().size(), artifactsResultInAnotherThread.get());
    }

    public void testDontResolveDependencies() throws Exception {
        File pomFile = new File("src/test/resources/projects/basic-resolveDependencies.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        configuration.setResolveDependencies(false);

        // single project build entry point
        ProjectBuildingResult result =
                lookup(org.apache.maven.project.ProjectBuilder.class).build(pomFile, configuration);
        assertEquals(0, result.getProject().getArtifacts().size());
        // multi projects build entry point
        List<ProjectBuildingResult> results = lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(Collections.singletonList(pomFile), false, configuration);
        assertEquals(1, results.size());
        MavenProject mavenProject = results.get(0).getProject();
        assertEquals(0, mavenProject.getArtifacts().size());
    }

    public void testReadModifiedPoms() throws Exception {
        String initialValue = System.setProperty(
                DefaultProjectBuilder.DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY, Boolean.toString(true));
        // TODO a similar test should be created to test the dependency management (basically all usages
        // of DefaultModelBuilder.getCache() are affected by MNG-6530

        Path tempDir = Files.createTempDirectory(null);
        FileUtils.copyDirectory(new File("src/test/resources/projects/grandchild-check"), tempDir.toFile());
        try {
            MavenSession mavenSession = createMavenSession(null);
            ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
            configuration.setRepositorySession(mavenSession.getRepositorySession());
            org.apache.maven.project.ProjectBuilder projectBuilder =
                    lookup(org.apache.maven.project.ProjectBuilder.class);
            File child = new File(tempDir.toFile(), "child/pom.xml");
            // build project once
            projectBuilder.build(child, configuration);
            // modify parent
            File parent = new File(tempDir.toFile(), "pom.xml");
            String parentContent = FileUtils.readFileToString(parent, "UTF-8");
            parentContent = parentContent.replace(
                    "<packaging>pom</packaging>",
                    "<packaging>pom</packaging><properties><addedProperty>addedValue</addedProperty></properties>");
            FileUtils.write(parent, parentContent, "UTF-8");
            // re-build pom with modified parent
            ProjectBuildingResult result = projectBuilder.build(child, configuration);
            assertThat(result.getProject().getProperties(), hasKey((Object) "addedProperty"));
        } finally {
            if (initialValue == null) {
                System.clearProperty(DefaultProjectBuilder.DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY);
            } else {
                System.setProperty(DefaultProjectBuilder.DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY, initialValue);
            }
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    public void testReadErroneousMavenProjectContainsReference() throws Exception {
        File pomFile = new File("src/test/resources/projects/artifactMissingVersion.xml").getAbsoluteFile();
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder = lookup(org.apache.maven.project.ProjectBuilder.class);

        // single project build entry point
        try {
            projectBuilder.build(pomFile, configuration);
        } catch (ProjectBuildingException ex) {
            assertEquals(1, ex.getResults().size());
            MavenProject project = ex.getResults().get(0).getProject();
            assertNotNull(project);
            assertEquals("testArtifactMissingVersion", project.getArtifactId());
            assertEquals(pomFile, project.getFile());
        }

        // multi projects build entry point
        try {
            projectBuilder.build(Collections.singletonList(pomFile), false, configuration);
        } catch (ProjectBuildingException ex) {
            assertEquals(1, ex.getResults().size());
            MavenProject project = ex.getResults().get(0).getProject();
            assertNotNull(project);
            assertEquals("testArtifactMissingVersion", project.getArtifactId());
            assertEquals(pomFile, project.getFile());
        }
    }

    public void testReadInvalidPom() throws Exception {
        File pomFile = new File("src/test/resources/projects/badPom.xml").getAbsoluteFile();
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder = lookup(org.apache.maven.project.ProjectBuilder.class);

        // single project build entry point
        try {
            projectBuilder.build(pomFile, configuration);
        } catch (InvalidArtifactRTException iarte) {
            assertThat(iarte.getMessage(), containsString("The groupId cannot be empty."));
        }

        // multi projects build entry point
        try {
            projectBuilder.build(Collections.singletonList(pomFile), false, configuration);
        } catch (ProjectBuildingException ex) {
            assertEquals(1, ex.getResults().size());
            MavenProject project = ex.getResults().get(0).getProject();
            assertNotNull(project);
            assertNotSame(0, ex.getResults().get(0).getProblems().size());
        }
    }

    public void testReadParentAndChildWithRegularVersionSetParentFile() throws Exception {
        List<File> toRead = new ArrayList<>(2);
        File parentPom = getProject("MNG-6723");
        toRead.add(parentPom);
        toRead.add(new File(parentPom.getParentFile(), "child/pom.xml"));
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder = lookup(org.apache.maven.project.ProjectBuilder.class);

        // read poms separately
        boolean parentFileWasFoundOnChild = false;
        for (File file : toRead) {
            List<ProjectBuildingResult> results =
                    projectBuilder.build(Collections.singletonList(file), false, configuration);
            assertResultShowNoError(results);
            MavenProject project = findChildProject(results);
            if (project != null) {
                assertEquals(parentPom, project.getParentFile());
                parentFileWasFoundOnChild = true;
            }
        }
        assertTrue(parentFileWasFoundOnChild);

        // read projects together
        List<ProjectBuildingResult> results = projectBuilder.build(toRead, false, configuration);
        assertResultShowNoError(results);
        assertEquals(parentPom, findChildProject(results).getParentFile());
        Collections.reverse(toRead);
        results = projectBuilder.build(toRead, false, configuration);
        assertResultShowNoError(results);
        assertEquals(parentPom, findChildProject(results).getParentFile());
    }

    private MavenProject findChildProject(List<ProjectBuildingResult> results) {
        for (ProjectBuildingResult result : results) {
            if (result.getPomFile().getParentFile().getName().equals("child")) {
                return result.getProject();
            }
        }
        return null;
    }

    private void assertResultShowNoError(List<ProjectBuildingResult> results) {
        for (ProjectBuildingResult result : results) {
            assertThat(result.getProblems(), is(empty()));
            assertNotNull(result.getProject());
        }
    }

    public void testBuildProperties() throws Exception {
        File file = new File(getProject("MNG-6716").getParentFile(), "project/pom.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        configuration.setResolveDependencies(true);
        List<ProjectBuildingResult> result = projectBuilder.build(Collections.singletonList(file), true, configuration);
        MavenProject project = result.get(0).getProject();
        // verify a few typical parameters are not duplicated
        assertEquals(1, project.getTestCompileSourceRoots().size());
        assertEquals(1, project.getCompileSourceRoots().size());
        assertEquals(1, project.getMailingLists().size());
        assertEquals(1, project.getResources().size());
    }

    public void testPropertyInPluginManagementGroupId() throws Exception {
        File pom = getProject("MNG-6983");

        MavenSession session = createMavenSession(pom);
        MavenProject project = session.getCurrentProject();

        for (Plugin buildPlugin : project.getBuildPlugins()) {
            assertNotNull("Missing version for build plugin " + buildPlugin.getKey(), buildPlugin.getVersion());
        }
    }

    public void testLocationTrackingResolution() throws Exception {
        File pom = getProject("MNG-7648");

        MavenSession session = createMavenSession(pom);
        MavenProject project = session.getCurrentProject();

        InputLocation dependencyLocation = null;
        for (Dependency dependency : project.getDependencies()) {
            if (dependency.getManagementKey().equals("org.apache.maven.its:a:jar")) {
                dependencyLocation = dependency.getLocation("version");
            }
        }
        assertNotNull("missing dependency", dependencyLocation);
        assertEquals(
                "org.apache.maven.its:bom:0.1", dependencyLocation.getSource().getModelId());

        InputLocation pluginLocation = null;
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getKey().equals("org.apache.maven.plugins:maven-clean-plugin")) {
                pluginLocation = plugin.getLocation("version");
            }
        }
        assertNotNull("missing build plugin", pluginLocation);
        assertEquals(
                "org.apache.maven.its:parent:0.1", pluginLocation.getSource().getModelId());
    }
}
