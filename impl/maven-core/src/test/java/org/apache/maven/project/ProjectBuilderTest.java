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
     * Tests that when modular sources are configured alongside explicit legacy resources,
     * the legacy resources are ignored and a warning is issued.
     * <p>
     * This verifies the behavior described in the design:
     * - Modular projects with explicit legacy {@code <resources>} configuration should issue a warning
     * - The modular resource roots are injected instead of using the legacy configuration
     * <p>
     * Acceptance Criterion: AC2 (unified source tracking for all lang/scope combinations)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     */
    @Test
    void testModularSourcesWithExplicitResourcesIssuesWarning() throws Exception {
        File pom = getProject("modular-sources-with-explicit-resources");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify warnings are issued for ignored legacy resources
        List<ModelProblem> warnings = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.WARNING)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("ignored"))
                .toList();

        assertEquals(2, warnings.size(), "Should have 2 warnings (one for resources, one for testResources)");
        assertTrue(
                warnings.stream().anyMatch(w -> w.getMessage().contains("<resources>")),
                "Should warn about ignored <resources>");
        assertTrue(
                warnings.stream().anyMatch(w -> w.getMessage().contains("<testResources>")),
                "Should warn about ignored <testResources>");

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
     * Tests that legacy sourceDirectory and testSourceDirectory are ignored in modular projects.
     * <p>
     * In modular projects, legacy directories are unconditionally ignored because it is not clear
     * how to dispatch their content between different modules. A warning is emitted if these
     * properties are explicitly set (differ from Super POM defaults).
     * <p>
     * This verifies:
     * - WARNINGs are emitted for explicitly set legacy directories in modular projects
     * - sourceDirectory and testSourceDirectory are both ignored
     * - Only modular sources from {@code <sources>} are used
     * <p>
     * Acceptance Criteria:
     * - AC1 (boolean flags eliminated - uses hasSources() for main/test detection)
     * - AC7 (legacy directories warning - {@code <sourceDirectory>} and {@code <testSourceDirectory>}
     *   are unconditionally ignored with a WARNING in modular projects)
     *
     * @see <a href="https://github.com/apache/maven/issues/11612">Issue #11612</a>
     */
    @Test
    void testMixedSourcesModularMainClassicTest() throws Exception {
        File pom = getProject("mixed-sources");

        MavenSession mavenSession = createMavenSession(null);
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(mavenSession.getRepositorySession());

        ProjectBuildingResult result = getContainer()
                .lookup(org.apache.maven.project.ProjectBuilder.class)
                .build(pom, configuration);

        MavenProject project = result.getProject();

        // Verify WARNINGs are emitted for explicitly set legacy directories
        List<ModelProblem> warnings = result.getProblems().stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.WARNING)
                .filter(p -> p.getMessage().contains("Legacy") && p.getMessage().contains("ignored in modular project"))
                .toList();

        // Should have 2 warnings: one for sourceDirectory, one for testSourceDirectory
        assertEquals(2, warnings.size(), "Should have 2 warnings for ignored legacy directories");
        assertTrue(
                warnings.stream().anyMatch(w -> w.getMessage().contains("<sourceDirectory>")),
                "Should warn about ignored <sourceDirectory>");
        assertTrue(
                warnings.stream().anyMatch(w -> w.getMessage().contains("<testSourceDirectory>")),
                "Should warn about ignored <testSourceDirectory>");

        // Get main Java source roots - should have modular sources, not classic sourceDirectory
        List<SourceRoot> mainJavaRoots = project.getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)
                .toList();

        // Should have 2 modular main Java sources (moduleA and moduleB)
        assertEquals(2, mainJavaRoots.size(), "Should have 2 modular main Java source roots");

        Set<String> mainModules = mainJavaRoots.stream()
                .map(SourceRoot::module)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        assertEquals(2, mainModules.size(), "Should have main sources for 2 modules");
        assertTrue(mainModules.contains("org.foo.moduleA"), "Should have main source for moduleA");
        assertTrue(mainModules.contains("org.foo.moduleB"), "Should have main source for moduleB");

        // Verify the classic sourceDirectory is NOT used (should be ignored)
        boolean hasClassicMainSource = mainJavaRoots.stream().anyMatch(sr -> sr.directory()
                .toString()
                .replace(File.separatorChar, '/')
                .contains("src/classic/main/java"));
        assertTrue(!hasClassicMainSource, "Classic sourceDirectory should be ignored");

        // Test sources should NOT be added (legacy testSourceDirectory is ignored in modular projects)
        List<SourceRoot> testJavaRoots = project.getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)
                .toList();
        assertEquals(0, testJavaRoots.size(), "Should have no test Java sources (legacy is ignored)");
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
     * - sourceDirectory is ignored because {@code <source scope="main" lang="java">} exists
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
