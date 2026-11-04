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
package org.apache.maven.cling.invoker.mvn;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.cli.Logger;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactFilteredOutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MavenInvoker#logFilteredOutHint(MavenContext, Throwable, MavenExecutionResult)}.
 */
class FilteredOutHintTest {

    /** Simple Logger that accumulates error-level messages for assertion. */
    private final List<String> errorLog = new ArrayList<>();

    private final Logger logger = new Logger() {
        @Override
        public void log(Logger.Level level, String message, Throwable error) {
            if (level == Logger.Level.ERROR) {
                errorLog.add(message);
            }
        }
    };

    /** Subclass that exposes the protected method without requiring a real Lookup. */
    static class TestableInvoker extends MavenInvoker {
        TestableInvoker() {
            super(mock(org.apache.maven.api.services.Lookup.class), null);
        }
    }

    private MavenContext context;
    private TestableInvoker invoker;

    @BeforeEach
    void setUp() {
        context = mock(MavenContext.class);
        context.logger = logger;
        invoker = new TestableInvoker();
    }

    private ArtifactResolutionException makeException(String groupId, String artifactId, String version) {
        RemoteRepository repo =
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
        ArtifactFilteredOutException filterEx = new ArtifactFilteredOutException(
                new DefaultArtifact(groupId + ":" + artifactId + ":jar:" + version), repo, "Prefix NOT allowed");
        ArtifactResult result = new ArtifactResult(new org.eclipse.aether.resolution.ArtifactRequest(
                new DefaultArtifact(groupId + ":" + artifactId + ":jar:" + version), List.of(repo), null));
        result.addException(filterEx);
        return new ArtifactResolutionException(List.of(result), "Could not resolve artifact");
    }

    private MavenProject makeProject(String groupId, String artifactId, String version) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        return project;
    }

    @Test
    void noHintWhenNoFilteredException() {
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        invoker.logFilteredOutHint(context, new RuntimeException("unrelated"), result);
        assertTrue(errorLog.isEmpty(), "No hint should be logged for unrelated exceptions");
    }

    @Test
    void hintForUnselectedReactorProject() {
        // lib is in all-projects but NOT in selected projects
        MavenProject lib = makeProject("com.example", "lib", "1.0-SNAPSHOT");
        MavenProject app = makeProject("com.example", "app", "1.0-SNAPSHOT");

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.setAllProjects(List.of(lib, app));
        result.setTopologicallySortedProjects(List.of(app)); // only app selected via -pl

        ArtifactResolutionException exception = makeException("com.example", "lib", "1.0-SNAPSHOT");

        invoker.logFilteredOutHint(context, exception, result);

        String combined = String.join(" ", errorLog);
        assertTrue(combined.contains("com.example:lib:1.0-SNAPSHOT"), "Should name the artifact");
        assertTrue(combined.contains("was not selected"), "Should indicate the project was not selected");
        assertTrue(combined.contains("-am"), "Should suggest -am");
    }

    @Test
    void hintForSelectedButUnbuiltReactorProject() {
        // lib is in both all-projects AND selected projects (but filtered out — ordering issue)
        MavenProject lib = makeProject("com.example", "lib", "1.0-SNAPSHOT");
        MavenProject app = makeProject("com.example", "app", "1.0-SNAPSHOT");

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.setAllProjects(List.of(lib, app));
        result.setTopologicallySortedProjects(List.of(lib, app)); // both selected

        ArtifactResolutionException exception = makeException("com.example", "lib", "1.0-SNAPSHOT");

        invoker.logFilteredOutHint(context, exception, result);

        String combined = String.join(" ", errorLog);
        assertTrue(combined.contains("com.example:lib:1.0-SNAPSHOT"), "Should name the artifact");
        assertTrue(combined.contains("has not been built yet"), "Should indicate not yet built");
    }

    @Test
    void hintForExternalArtifact() {
        // The filtered artifact is NOT a reactor project at all
        MavenProject app = makeProject("com.example", "app", "1.0-SNAPSHOT");

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.setAllProjects(List.of(app));
        result.setTopologicallySortedProjects(List.of(app));

        // The filtered artifact is a 3rd-party dependency
        ArtifactResolutionException exception = makeException("org.external", "library", "2.0");

        invoker.logFilteredOutHint(context, exception, result);

        String combined = String.join(" ", errorLog);
        assertTrue(combined.contains("org.external:library:2.0"), "Should name the artifact");
        assertTrue(combined.contains("remote repository filter"), "Should mention repo filter");
        assertTrue(
                combined.contains("-Daether.remoteRepositoryFilter.prefixes=false"),
                "Should provide workaround property");
    }

    @Test
    void hintForExternalArtifactWithEmptyReactor() {
        // No reactor at all (e.g. embedder scenario where graph wasn't built)
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        ArtifactResolutionException exception = makeException("org.external", "library", "2.0");

        invoker.logFilteredOutHint(context, exception, result);

        String combined = String.join(" ", errorLog);
        assertTrue(combined.contains("remote repository filter"), "Should fall back to repo-filter hint");
    }
}
