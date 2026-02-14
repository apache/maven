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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectBuilderTest extends AbstractCoreMavenComponentTestCase {
    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/project-builder";
    }

    @Test
    void testSystemScopeDependencyIsPresentInTheCompileClasspathElements() throws Exception {
        File pom = getProject("it0063");

        Properties eps = new Properties();
        eps.setProperty("jre.home", new File(pom.getParentFile(), "jdk/jre").getPath());

        MavenSession session = createMavenSession(pom, eps);
        MavenProject project = session.getCurrentProject();

        // Here we will actually not have any artifacts because the ProjectDependenciesResolver is not involved here. So
        // right now it's not valid to ask for artifacts unless plugins require the artifacts.

        project.getCompileClasspathElements();
    }

    @Test
    void testBuildFromModelSource() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsource/module01/pom.xml");
        MavenSession mavenSession = createMavenSession(pomFile);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pomFile, configuration);

        assertNotNull(result.getProject().getParentFile());
    }

    @Test
    void testVersionlessManagedDependency() throws Exception {
        File pomFile = new File("src/test/resources/projects/versionless-managed-dependency.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingException e = assertThrows(ProjectBuildingException.class, () -> getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pomFile, configuration));
        assertEquals(1, e.getResults().size());
        ProjectBuildingResultWithProblemMessageAssert.assertThat(e.getResults().get(0))
                .hasProblemMessage(
                        "'dependencies.dependency.version' for groupId='org.apache.maven.its', artifactId='a', type='jar' is missing");
        ProjectBuildingResultWithLocationAssert.assertThat(e.getResults().get(0))
                .hasLocation(5, 9);
    }

    @Test
    void testResolveDependencies() throws Exception {
        File pomFile = new File("src/test/resources/projects/basic-resolveDependencies.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        configuration.setResolveDependencies(true);

        // single project build entry point
        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pomFile, configuration);
        assertEquals(1, result.getProject().getArtifacts().size());
        // multi projects build entry point
        List<ProjectBuildingResult> results = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
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

    @Test
    void testDontResolveDependencies() throws Exception {
        File pomFile = new File("src/test/resources/projects/basic-resolveDependencies.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        configuration.setResolveDependencies(false);

        // single project build entry point
        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pomFile, configuration);
        assertEquals(0, result.getProject().getArtifacts().size());
        // multi projects build entry point
        List<ProjectBuildingResult> results = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(Collections.singletonList(pomFile), false, configuration);
        assertEquals(1, results.size());
        MavenProject mavenProject = results.get(0).getProject();
        assertEquals(0, mavenProject.getArtifacts().size());
    }

    @Test
    void testReadModifiedPoms(@TempDir Path tempDir) throws Exception {
        // TODO a similar test should be created to test the dependency management (basically all usages
        // of DefaultModelBuilder.getCache() are affected by MNG-6530

        FileUtils.copyDirectoryStructure(new File("src/test/resources/projects/grandchild-check"), tempDir.toFile());

        MavenSession mavenSession = createMavenSession(null);
        mavenSession.getRequest().setRootDirectory(tempDir);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup(org.apache.maven.project.ProjectBuilder.class);
        File child = new File(tempDir.toFile(), "child/pom.xml");
        // build project once
        projectBuilder.build(child, configuration);
        // modify parent
        File parent = new File(tempDir.toFile(), "pom.xml");
        String parentContent = new String(Files.readAllBytes(parent.toPath()), StandardCharsets.UTF_8);
        parentContent = parentContent.replace(
                "<packaging>pom</packaging>",
                "<packaging>pom</packaging><properties><addedProperty>addedValue</addedProperty></properties>");
        Files.write(parent.toPath(), parentContent.getBytes(StandardCharsets.UTF_8));
        // re-build pom with modified parent
        ProjectBuildingResult result = projectBuilder.build(child, configuration);
        assertTrue(result.getProject().getProperties().containsKey("addedProperty"));
    }

    @Test
    void testReadErroneousMavenProjectContainsReference() throws Exception {
        File pomFile = new File("src/test/resources/projects/artifactMissingVersion/pom.xml").getAbsoluteFile();
        MavenSession mavenSession = createMavenSession(null);
        mavenSession.getRequest().setRootDirectory(pomFile.getParentFile().toPath());
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup(org.apache.maven.project.ProjectBuilder.class);

        // single project build entry point
        ProjectBuildingException ex1 =
                assertThrows(ProjectBuildingException.class, () -> projectBuilder.build(pomFile, configuration));

        assertEquals(1, ex1.getResults().size());
        MavenProject project1 = ex1.getResults().get(0).getProject();
        assertNotNull(project1);
        assertEquals("testArtifactMissingVersion", project1.getArtifactId());
        assertEquals(pomFile, project1.getFile());

        // multi projects build entry point
        ProjectBuildingException ex2 = assertThrows(
                ProjectBuildingException.class,
                () -> projectBuilder.build(Collections.singletonList(pomFile), true, configuration));

        assertEquals(1, ex2.getResults().size());
        MavenProject project2 = ex2.getResults().get(0).getProject();
        assertNotNull(project2);
        assertEquals("testArtifactMissingVersion", project2.getArtifactId());
        assertEquals(pomFile, project2.getFile());
    }

    @Test
    void testReadInvalidPom() throws Exception {
        File pomFile = new File("src/test/resources/projects/badPom.xml").getAbsoluteFile();
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_STRICT);
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup(org.apache.maven.project.ProjectBuilder.class);

        // single project build entry point
        Exception ex = assertThrows(Exception.class, () -> projectBuilder.build(pomFile, configuration));
        assertTrue(ex.getMessage().contains("Received non-all-whitespace CHARACTERS or CDATA event"));

        // multi projects build entry point
        ProjectBuildingException pex = assertThrows(
                ProjectBuildingException.class,
                () -> projectBuilder.build(Collections.singletonList(pomFile), false, configuration));
        assertEquals(1, pex.getResults().size());
        assertNotNull(pex.getResults().get(0).getPomFile());
        assertTrue(pex.getResults().get(0).getProblems().size() > 0);
        ProjectBuildingResultWithProblemMessageAssert.assertThat(
                        pex.getResults().get(0))
                .hasProblemMessage("Received non-all-whitespace CHARACTERS or CDATA event in nextTag()");
    }

    @Test
    void testReadParentAndChildWithRegularVersionSetParentFile() throws Exception {
        List<File> toRead = new ArrayList<>(2);
        File parentPom = getProject("MNG-6723");
        toRead.add(parentPom);
        toRead.add(new File(parentPom.getParentFile(), "child/pom.xml"));
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup(org.apache.maven.project.ProjectBuilder.class);

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
            assertTrue(result.getProblems().isEmpty());
            assertNotNull(result.getProject());
        }
    }

    @Test
    void testBuildProperties() throws Exception {
        File file = new File(getProject("MNG-6716").getParentFile(), "project/pom.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        configuration.setResolveDependencies(true);
        List<ProjectBuildingResult> result =
                projectBuilder.build(Collections.singletonList(file), false, configuration);
        MavenProject project = result.get(0).getProject();
        // verify a few typical parameters are not duplicated
        assertEquals(1, project.getTestCompileSourceRoots().size());
        assertEquals(1, project.getCompileSourceRoots().size());
        assertEquals(1, project.getMailingLists().size());
        assertEquals(1, project.getResources().size());
    }

    @Test
    void testPropertyInPluginManagementGroupId() throws Exception {
        File pom = getProject("MNG-6983");

        MavenSession session = createMavenSession(pom);
        MavenProject project = session.getCurrentProject();

        for (Plugin buildPlugin : project.getBuildPlugins()) {
            assertNotNull(buildPlugin.getVersion(), "Missing version for build plugin " + buildPlugin.getKey());
        }
    }

    @Test
    void testBuildFromModelSourceResolvesBasedir() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");
        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());
        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pomFile, configuration);

        assertEquals(
                pomFile.getAbsoluteFile(),
                result.getProject().getModel().getPomFile().getAbsoluteFile());
        int errors = 0;
        for (ModelProblem p : result.getProblems()) {
            if (p.getSeverity() == ModelProblem.Severity.ERROR) {
                errors++;
            }
        }
        assertEquals(0, errors);
    }

    @Test
    void testLocationTrackingResolution() throws Exception {
        File pom = getProject("MNG-7648");

        MavenSession session = createMavenSession(pom);
        MavenProject project = session.getCurrentProject();

        InputLocation dependencyLocation = null;
        for (Dependency dependency : project.getDependencies()) {
            if (dependency.getManagementKey().equals("org.apache.maven.its:a:jar")) {
                dependencyLocation = dependency.getLocation("version");
            }
        }
        assertNotNull(dependencyLocation, "missing dependency");
        assertEquals(
                "org.apache.maven.its:bom:0.1", dependencyLocation.getSource().getModelId());

        InputLocation pluginLocation = null;
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getKey().equals("org.apache.maven.plugins:maven-clean-plugin")) {
                pluginLocation = plugin.getLocation("version");
            }
        }
        assertNotNull(pluginLocation, "missing build plugin");
        assertEquals(
                "org.apache.maven.its:parent:0.1", pluginLocation.getSource().getModelId());
    }
    /**
     * Tests that a project with multiple modules defined in sources is detected as modular,
     * and module-aware resource roots are injected for each module.
     * <p>
     * Acceptance Criterion: AC2 (unified source tracking for all lang/scope combinations)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     */
    @Test
    void testModularSourcesInjectResourceRoots() throws Exception {
        File pom = getProject("modular-sources");

        MavenSession session = createMavenSession(pom);
        MavenProject project = session.getCurrentProject();

        // Get all resource source roots for main scope
        List<SourceRoot> mainResourceRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)
                .toList();

        // Should have resource roots for both modules
        Set<String> modules = mainResourceRoots.stream()
                .map(SourceRoot::module)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        assertEquals(2, modules.size(), "Should have resource roots for 2 modules");
        assertTrue(modules.contains("org.foo.moduleA"), "Should have resource root for moduleA");
        assertTrue(modules.contains("org.foo.moduleB"), "Should have resource root for moduleB");

        // Get all resource source roots for test scope
        List<SourceRoot> testResourceRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.RESOURCES)
                .toList();

        // Should have test resource roots for both modules
        Set<String> testModules = testResourceRoots.stream()
                .map(SourceRoot::module)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        assertEquals(2, testModules.size(), "Should have test resource roots for 2 modules");
        assertTrue(testModules.contains("org.foo.moduleA"), "Should have test resource root for moduleA");
        assertTrue(testModules.contains("org.foo.moduleB"), "Should have test resource root for moduleB");
    }

    /**
     * Tests that when modular sources are configured alongside explicit legacy resources, an error is raised.
     * <p>
     * This verifies the behavior described in the design:
     * - Modular projects with explicit legacy {@code <resources>} configuration should raise an error
     * - The modular resource roots are injected instead of using the legacy configuration
     * <p>
     * Acceptance Criteria:
     * - AC2 (unified source tracking for all lang/scope combinations)
     * - AC8 (legacy directories error - supersedes AC7 which originally used WARNING)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3858462609">AC8 definition</a>
     */
    @Test
    void testModularSourcesWithExplicitResourcesIssuesError() throws Exception {
        File pom = getProject("modular-sources-with-explicit-resources");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify errors are raised for conflicting legacy resources (AC8)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("cannot be used"))
                .toList();

        assertEquals(2, errors.size(), "Should have 2 errors (one for resources, one for testResources)");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains("<resources>")),
                "Should error about conflicting <resources>");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains("<testResources>")),
                "Should error about conflicting <testResources>");

        // Verify modular resources are still injected correctly
        List<SourceRoot> mainResourceRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)
                .toList();

        assertEquals(2, mainResourceRoots.size(), "Should have 2 modular resource roots (one per module)");

        Set<String> mainModules = mainResourceRoots.stream()
                .map(SourceRoot::module)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        assertEquals(2, mainModules.size(), "Should have resource roots for 2 modules");
        assertTrue(mainModules.contains("org.foo.moduleA"), "Should have resource root for moduleA");
        assertTrue(mainModules.contains("org.foo.moduleB"), "Should have resource root for moduleB");
    }

    /**
     * Tests AC8: ALL legacy directories are rejected when {@code <sources>} is configured.
     * <p>
     * Modular project with Java in {@code <sources>} for MAIN scope and explicit legacy
     * {@code <sourceDirectory>} that differs from default. The legacy directory is rejected
     * because modular projects cannot use legacy directories (content cannot be dispatched
     * between modules).
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testModularWithJavaSourcesRejectsLegacySourceDirectory() throws Exception {
        File pom = getProject("modular-java-with-explicit-source-dir");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify ERROR for <sourceDirectory> (MAIN scope has Java in <sources>)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("cannot be used"))
                .filter(p -> p.getMessage().contains("<sourceDirectory>"))
                .toList();

        assertEquals(1, errors.size(), "Should have 1 error for <sourceDirectory>");

        // Verify modular source is used, not legacy
        List<SourceRoot> mainJavaRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)
                .toList();
        assertEquals(1, mainJavaRoots.size(), "Should have 1 modular main Java source root");
        assertEquals("org.foo.app", mainJavaRoots.get(0).module().orElse(null), "Should have module org.foo.app");

        // Legacy sourceDirectory is NOT used
        assertFalse(
                mainJavaRoots.get(0).directory().toString().contains("src/custom/main/java"),
                "Legacy sourceDirectory should not be used");
    }

    /**
     * Tests AC8: Modular project rejects legacy {@code <testSourceDirectory>} even when
     * {@code <sources>} has NO Java for TEST scope.
     * <p>
     * Modular project with NO Java in {@code <sources>} for TEST scope and explicit legacy
     * {@code <testSourceDirectory>} that differs from default. The legacy directory is rejected
     * because modular projects cannot use legacy directories (content cannot be dispatched
     * between modules).
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testModularWithoutTestSourcesRejectsLegacyTestSourceDirectory() throws Exception {
        File pom = getProject("modular-no-test-java-with-explicit-test-source-dir");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify ERROR for <testSourceDirectory> (modular projects reject all legacy directories)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("cannot be used"))
                .filter(p -> p.getMessage().contains("<testSourceDirectory>"))
                .toList();

        assertEquals(1, errors.size(), "Should have 1 error for <testSourceDirectory>");

        // No test Java sources (legacy rejected, none in <sources>)
        List<SourceRoot> testJavaRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)
                .toList();
        assertEquals(0, testJavaRoots.size(), "Should have no test Java sources");
    }

    /**
     * Tests AC9: explicit legacy directories raise an error in non-modular projects when
     * {@code <sources>} has Java for that scope.
     * <p>
     * This test uses a non-modular project (no {@code <module>} attribute) with both:
     * <ul>
     *   <li>{@code <sources>} with main and test Java sources</li>
     *   <li>Explicit {@code <sourceDirectory>} and {@code <testSourceDirectory>} (conflicting)</li>
     * </ul>
     * Both legacy directories should trigger ERROR because {@code <sources>} has Java.
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testClassicSourcesWithExplicitLegacyDirectories() throws Exception {
        File pom = getProject("classic-sources-with-explicit-legacy");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        // Verify errors are raised for conflicting legacy directories (AC9)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("cannot be used"))
                .toList();

        assertEquals(2, errors.size(), "Should have 2 errors (one for sourceDirectory, one for testSourceDirectory)");

        // Verify error messages mention the conflicting elements
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains("<sourceDirectory>")),
                "Should have error for <sourceDirectory>");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains("<testSourceDirectory>")),
                "Should have error for <testSourceDirectory>");
    }

    /**
     * Tests AC9: Non-modular project with only resources in {@code <sources>} uses implicit Java fallback.
     * <p>
     * When {@code <sources>} contains only resources (no Java sources), the legacy
     * {@code <sourceDirectory>} and {@code <testSourceDirectory>} are used as implicit fallback.
     * This enables incremental adoption of {@code <sources>} - customize resources while
     * keeping the default Java directory structure.
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testNonModularResourcesOnlyWithImplicitJavaFallback() throws Exception {
        File pom = getProject("non-modular-resources-only");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify NO errors - legacy directories are used as fallback (AC9)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("cannot be used"))
                .toList();

        assertEquals(0, errors.size(), "Should have no errors - legacy directories used as fallback (AC9)");

        // Verify resources from <sources> are used
        List<SourceRoot> mainResources = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)
                .toList();
        assertTrue(
                mainResources.stream().anyMatch(sr -> sr.directory()
                        .toString()
                        .replace(File.separatorChar, '/')
                        .contains("src/main/custom-resources")),
                "Should have custom main resources from <sources>");

        // Verify legacy Java directories are used as fallback
        List<SourceRoot> mainJavaRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)
                .toList();
        assertEquals(1, mainJavaRoots.size(), "Should have 1 main Java source (implicit fallback)");
        assertTrue(
                mainJavaRoots
                        .get(0)
                        .directory()
                        .toString()
                        .replace(File.separatorChar, '/')
                        .endsWith("src/main/java"),
                "Should use default src/main/java as fallback");

        List<SourceRoot> testJavaRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)
                .toList();
        assertEquals(1, testJavaRoots.size(), "Should have 1 test Java source (implicit fallback)");
        assertTrue(
                testJavaRoots
                        .get(0)
                        .directory()
                        .toString()
                        .replace(File.separatorChar, '/')
                        .endsWith("src/test/java"),
                "Should use default src/test/java as fallback");
    }

    /**
     * Tests AC9 violation: Non-modular project with only resources in {@code <sources>} and explicit legacy directories.
     * <p>
     * AC9 allows implicit fallback to legacy directories (when they match defaults).
     * When legacy directories differ from the default, this is explicit configuration,
     * which violates AC9's "implicit" requirement, so an ERROR is raised.
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testNonModularResourcesOnlyWithExplicitLegacyDirectoriesRejected() throws Exception {
        File pom = getProject("non-modular-resources-only-explicit-legacy");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify ERRORs for explicit legacy directories (differ from default)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("cannot be used"))
                .toList();

        assertEquals(2, errors.size(), "Should have 2 errors for explicit legacy directories");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains("<sourceDirectory>")),
                "Should error about <sourceDirectory>");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains("<testSourceDirectory>")),
                "Should error about <testSourceDirectory>");

        // Verify resources from <sources> are still used
        List<SourceRoot> mainResources = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)
                .toList();
        assertTrue(
                mainResources.stream().anyMatch(sr -> sr.directory()
                        .toString()
                        .replace(File.separatorChar, '/')
                        .contains("src/main/custom-resources")),
                "Should have custom main resources from <sources>");

        // Verify NO Java source roots (legacy was rejected, none in <sources>)
        List<SourceRoot> mainJavaRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)
                .toList();
        assertEquals(0, mainJavaRoots.size(), "Should have no main Java sources (legacy rejected)");

        List<SourceRoot> testJavaRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)
                .toList();
        assertEquals(0, testJavaRoots.size(), "Should have no test Java sources (legacy rejected)");
    }

    /**
     * Tests AC8: Modular project with Java in {@code <sources>} and physical default legacy directories.
     * <p>
     * Even when legacy directories use Super POM defaults (no explicit override),
     * if the physical directories exist on the filesystem, an ERROR is raised.
     * This is because modular projects use paths like {@code src/<module>/main/java},
     * so content in {@code src/main/java} would be silently ignored.
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testModularWithPhysicalDefaultLegacyDirectory() throws Exception {
        File pom = getProject("modular-with-physical-legacy");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        // Verify ERRORs are raised for physical presence of default directories (AC8)
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy directory")
                        && p.getMessage().contains("exists"))
                .toList();

        // Should have 2 errors: one for src/main/java, one for src/test/java
        assertEquals(2, errors.size(), "Should have 2 errors for physical legacy directories");
        // Use File.separator for platform-independent path matching (backslash on Windows)
        String mainJava = "src" + File.separator + "main" + File.separator + "java";
        String testJava = "src" + File.separator + "test" + File.separator + "java";
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains(mainJava)),
                "Should error about physical src/main/java");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains(testJava)),
                "Should error about physical src/test/java");
    }

    /**
     * Tests AC8: Modular project with only resources in {@code <sources>} and physical default legacy directories.
     * <p>
     * Even when {@code <sources>} only contains resources (no Java), if the physical
     * default directories exist, an ERROR is raised for modular projects.
     * Unlike non-modular projects (AC9), modular projects cannot use legacy directories as fallback.
     *
     * @see <a href="https://github.com/apache/maven/issues/11701#issuecomment-3897961755">Issue #11701 (AC8/AC9)</a>
     */
    @Test
    void testModularResourcesOnlyWithPhysicalDefaultLegacyDirectory() throws Exception {
        File pom = getProject("modular-resources-only-with-physical-legacy");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        // Verify ERRORs are raised for physical presence of default directories (AC8)
        // Unlike non-modular (AC9), modular projects cannot use legacy as fallback
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Legacy directory")
                        && p.getMessage().contains("exists"))
                .toList();

        // Should have 2 errors: one for src/main/java, one for src/test/java
        assertEquals(
                2, errors.size(), "Should have 2 errors for physical legacy directories (no AC9 fallback for modular)");
        // Use File.separator for platform-independent path matching (backslash on Windows)
        String mainJava = "src" + File.separator + "main" + File.separator + "java";
        String testJava = "src" + File.separator + "test" + File.separator + "java";
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains(mainJava)),
                "Should error about physical src/main/java");
        assertTrue(
                errors.stream().anyMatch(e -> e.getMessage().contains(testJava)),
                "Should error about physical src/test/java");
    }

    /**
     * Tests that mixing modular and non-modular sources within {@code <sources>} is not allowed.
     * <p>
     * A project must be either fully modular (all sources have a module) or fully classic
     * (no sources have a module). Mixing them within the same project is not supported
     * because the compiler plugin cannot handle such configurations.
     * <p>
     * This verifies:
     * - An ERROR is reported when both modular and non-modular sources exist in {@code <sources>}
     * - sourceDirectory is not used because {@code <sources>} exists
     * <p>
     * Acceptance Criteria:
     * - AC1 (boolean flags eliminated - uses hasSources() for source detection)
     * - AC6 (mixed sources error - mixing modular and classic sources within {@code <sources>}
     *   triggers an ERROR)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     */
    @Test
    void testSourcesMixedModulesWithinSources() throws Exception {
        File pom = getProject("sources-mixed-modules");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        // Verify an ERROR is reported for mixing modular and non-modular sources
        List<ModelProblem> errors = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .filter(p -> p.getMessage().contains("Mixed modular and classic sources"))
                .toList();

        assertEquals(1, errors.size(), "Should have 1 error for mixed modular/classic configuration");
        assertTrue(errors.get(0).getMessage().contains("lang=java"), "Error should mention java language");
        assertTrue(errors.get(0).getMessage().contains("scope=main"), "Error should mention main scope");
    }

    /**
     * Tests that multiple source directories for the same (lang, scope, module) combination
     * are allowed and all are added as source roots.
     * <p>
     * This is a valid use case for Phase 2: users may have generated sources alongside regular sources,
     * both belonging to the same module. Different directories = different identities = not duplicates.
     * <p>
     * Acceptance Criterion: AC2 (unified source tracking - multiple directories per module supported)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     */
    @Test
    void testMultipleDirectoriesSameModule() throws Exception {
        File pom = getProject("multiple-directories-same-module");

        MavenSession session = createMavenSession(pom);
        MavenProject project = session.getCurrentProject();

        // Get main Java source roots
        List<SourceRoot> mainJavaRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)
                .toList();

        // Should have 2 main sources: both for com.example.app but different directories
        assertEquals(2, mainJavaRoots.size(), "Should have 2 main Java source roots for same module");

        // Both should be for the same module
        long moduleCount = mainJavaRoots.stream()
                .filter(sr -> "com.example.app".equals(sr.module().orElse(null)))
                .count();
        assertEquals(2, moduleCount, "Both main sources should be for com.example.app module");

        // One should be implicit directory, one should be generated-sources
        boolean hasImplicitDir = mainJavaRoots.stream().anyMatch(sr -> sr.directory()
                .toString()
                .replace(File.separatorChar, '/')
                .contains("src/com.example.app/main/java"));
        boolean hasGeneratedDir = mainJavaRoots.stream().anyMatch(sr -> sr.directory()
                .toString()
                .replace(File.separatorChar, '/')
                .contains("target/generated-sources/com.example.app/java"));

        assertTrue(hasImplicitDir, "Should have implicit source directory for module");
        assertTrue(hasGeneratedDir, "Should have generated-sources directory for module");

        // Get test Java source roots
        List<SourceRoot> testJavaRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)
                .toList();

        // Should have 2 test sources: both for com.example.app
        assertEquals(2, testJavaRoots.size(), "Should have 2 test Java source roots for same module");

        // Both test sources should be for the same module
        long testModuleCount = testJavaRoots.stream()
                .filter(sr -> "com.example.app".equals(sr.module().orElse(null)))
                .count();
        assertEquals(2, testModuleCount, "Both test sources should be for com.example.app module");
    }

    /**
     * Tests duplicate handling with enabled discriminator.
     * <p>
     * Test scenario:
     * - Same (lang, scope, module, directory) with enabled=true appearing twice → triggers WARNING
     * - Same identity with enabled=false → should be filtered out (disabled sources are no-ops)
     * - Different modules should be added normally
     * <p>
     * Verifies:
     * - First enabled source wins, subsequent duplicates trigger WARNING
     * - Disabled sources don't count as duplicates
     * - Different modules are unaffected
     * <p>
     * Acceptance Criteria:
     * - AC3 (duplicate detection - duplicates trigger WARNING)
     * - AC4 (first enabled wins - duplicates are skipped)
     * - AC5 (disabled sources unchanged - still added but filtered by getEnabledSourceRoots)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     */
    @Test
    void testDuplicateEnabledSources() throws Exception {
        File pom = getProject("duplicate-enabled-sources");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify warnings are issued for duplicate enabled sources
        List<ModelProblem> duplicateWarnings = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.WARNING)
                .filter(p -> p.getMessage().contains("Duplicate enabled source"))
                .toList();

        // We have 2 duplicate pairs: main scope and test scope for com.example.dup
        assertEquals(2, duplicateWarnings.size(), "Should have 2 duplicate warnings (main and test scope)");

        // Get main Java source roots
        List<SourceRoot> mainJavaRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)
                .toList();

        // Should have 2 main sources: 1 for com.example.dup (first wins) + 1 for com.example.other
        // Note: MavenProject.addSourceRoot still adds all sources, but tracking only counts first enabled
        assertEquals(2, mainJavaRoots.size(), "Should have 2 main Java source roots");

        // Verify com.example.other module is present
        boolean hasOtherModule = mainJavaRoots.stream()
                .anyMatch(sr -> "com.example.other".equals(sr.module().orElse(null)));
        assertTrue(hasOtherModule, "Should have source root for com.example.other module");

        // Verify com.example.dup module is present (first enabled wins)
        boolean hasDupModule = mainJavaRoots.stream()
                .anyMatch(sr -> "com.example.dup".equals(sr.module().orElse(null)));
        assertTrue(hasDupModule, "Should have source root for com.example.dup module");

        // Get test Java source roots
        List<SourceRoot> testJavaRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)
                .toList();

        // Test scope has 1 source for com.example.dup (first wins)
        assertEquals(1, testJavaRoots.size(), "Should have 1 test Java source root");

        // Verify it's for the dup module
        assertEquals(
                "com.example.dup",
                testJavaRoots.get(0).module().orElse(null),
                "Test source root should be for com.example.dup module");
    }
}
