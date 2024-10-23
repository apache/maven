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
package org.apache.maven.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectSelectorTest {
    private final ProjectSelector sut = new ProjectSelector();
    private final MavenExecutionRequest mavenExecutionRequest = mock(MavenExecutionRequest.class);

    @Test
    void getBaseDirectoryFromRequestWhenDirectoryIsNullReturnNull() {
        when(mavenExecutionRequest.getBaseDirectory()).thenReturn(null);

        final File baseDirectoryFromRequest = sut.getBaseDirectoryFromRequest(mavenExecutionRequest);

        assertThat(baseDirectoryFromRequest, nullValue());
    }

    @Test
    void getBaseDirectoryFromRequestWhenDirectoryIsValidReturnFile() {
        when(mavenExecutionRequest.getBaseDirectory()).thenReturn("path/to/file");

        final File baseDirectoryFromRequest = sut.getBaseDirectoryFromRequest(mavenExecutionRequest);

        assertThat(baseDirectoryFromRequest, notNullValue());
        assertThat(baseDirectoryFromRequest.getPath(), is(new File("path/to/file").getPath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {":wrong-selector", "wrong-selector"})
    @EmptySource
    void isMatchingProjectNoMatchOnSelectorReturnsFalse(String selector) {
        final boolean result = sut.isMatchingProject(createMavenProject("maven-core"), selector, null);
        assertThat(result, is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {":maven-core", "org.apache.maven:maven-core"})
    void isMatchingProjectMatchOnSelectorReturnsTrue(String selector) {
        final boolean result = sut.isMatchingProject(createMavenProject("maven-core"), selector, null);
        assertThat(result, is(true));
    }

    @Test
    void isMatchingProjectMatchOnFileReturnsTrue() throws IOException {
        final File tempFile = File.createTempFile("maven-core-unit-test-pom", ".xml");
        final String selector = tempFile.getName();
        final MavenProject mavenProject = createMavenProject("maven-core");
        mavenProject.setFile(tempFile);

        final boolean result = sut.isMatchingProject(mavenProject, selector, tempFile.getParentFile());

        tempFile.delete();
        assertThat(result, is(true));
    }

    @Test
    void isMatchingProjectMatchOnDirectoryReturnsTrue(@TempDir File tempDir) {
        String selector = "maven-core";
        final File tempProjectDir = new File(tempDir, "maven-core");
        tempProjectDir.mkdir();
        final MavenProject mavenProject = createMavenProject("maven-core");
        mavenProject.setFile(new File(tempProjectDir, "some-file.xml"));

        final boolean result = sut.isMatchingProject(mavenProject, selector, tempDir);

        tempProjectDir.delete();
        assertThat(result, is(true));
    }

    @Test
    void getOptionalProjectsBySelectorsReturnsMatches() {
        final HashSet<String> selectors = new HashSet<>();
        selectors.add(":maven-core");
        selectors.add(":optional");

        final MavenProject mavenProject = createMavenProject("maven-core");
        final List<MavenProject> listOfProjects = Collections.singletonList(mavenProject);

        final Set<MavenProject> optionalProjectsBySelectors =
                sut.getOptionalProjectsBySelectors(mavenExecutionRequest, listOfProjects, selectors);

        assertThat(optionalProjectsBySelectors.size(), is(1));
        assertThat(optionalProjectsBySelectors, contains(mavenProject));
    }

    @Test
    void getRequiredProjectsBySelectorsThrowsMavenExecutionException() {
        final HashSet<String> selectors = new HashSet<>();
        selectors.add(":maven-core");
        selectors.add(":required");

        final MavenProject mavenProject = createMavenProject("maven-core");
        final List<MavenProject> listOfProjects = Collections.singletonList(mavenProject);

        final MavenExecutionException exception = assertThrows(
                MavenExecutionException.class,
                () -> sut.getRequiredProjectsBySelectors(mavenExecutionRequest, listOfProjects, selectors));
        assertThat(exception.getMessage(), containsString("Could not find"));
        assertThat(exception.getMessage(), containsString(":required"));
    }

    @Test
    void getRequiredProjectsBySelectorsReturnsProject() throws MavenExecutionException {
        final HashSet<String> selectors = new HashSet<>();
        selectors.add(":maven-core");

        final MavenProject mavenProject = createMavenProject("maven-core");
        final List<MavenProject> listOfProjects = Collections.singletonList(mavenProject);

        final Set<MavenProject> requiredProjectsBySelectors =
                sut.getRequiredProjectsBySelectors(mavenExecutionRequest, listOfProjects, selectors);

        assertThat(requiredProjectsBySelectors.size(), is(1));
        assertThat(requiredProjectsBySelectors, contains(mavenProject));
    }

    @Test
    void getRequiredProjectsBySelectorsReturnsProjectWithChildProjects() throws MavenExecutionException {
        when(mavenExecutionRequest.isRecursive()).thenReturn(true);

        final HashSet<String> selectors = new HashSet<>();
        selectors.add(":maven-core");

        final MavenProject mavenProject = createMavenProject("maven-core");
        final MavenProject child = createMavenProject("maven-core-child");
        mavenProject.setCollectedProjects(Collections.singletonList(child));
        final List<MavenProject> listOfProjects = Collections.singletonList(mavenProject);

        final Set<MavenProject> requiredProjectsBySelectors =
                sut.getRequiredProjectsBySelectors(mavenExecutionRequest, listOfProjects, selectors);

        assertThat(requiredProjectsBySelectors.size(), is(2));
        assertThat(requiredProjectsBySelectors, contains(mavenProject, child));
    }

    @Test
    void getOptionalProjectsBySelectorsReturnsProjectWithChildProjects() {
        when(mavenExecutionRequest.isRecursive()).thenReturn(true);

        final HashSet<String> selectors = new HashSet<>();
        selectors.add(":maven-core");

        final MavenProject mavenProject = createMavenProject("maven-core");
        final MavenProject child = createMavenProject("maven-core-child");
        mavenProject.setCollectedProjects(Collections.singletonList(child));
        final List<MavenProject> listOfProjects = Collections.singletonList(mavenProject);

        final Set<MavenProject> optionalProjectsBySelectors =
                sut.getOptionalProjectsBySelectors(mavenExecutionRequest, listOfProjects, selectors);

        assertThat(optionalProjectsBySelectors.size(), is(2));
        assertThat(optionalProjectsBySelectors, contains(mavenProject, child));
    }

    private MavenProject createMavenProject(String artifactId) {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.apache.maven");
        mavenProject.setArtifactId(artifactId);
        mavenProject.setVersion("1.0");
        mavenProject.setFile(new File(artifactId, "some-dir"));
        mavenProject.setCollectedProjects(new ArrayList<>());
        return mavenProject;
    }
}
