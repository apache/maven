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
package org.apache.maven.lifecycle.internal;

import javax.inject.Provider;

import java.util.List;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MojoExecutor#executeForkedExecutions} null-guard behavior
 * and {@link ProjectIndex} lookup semantics.
 *
 * @see <a href="https://github.com/apache/maven/issues/12600">MNG-12600</a>
 */
@ExtendWith(MockitoExtension.class)
class MojoExecutorForkedExecutionTest {

    @Mock
    private BuildPluginManager pluginManager;

    @Mock
    private MavenPluginManager mavenPluginManager;

    @Mock
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Mock
    private ExecutionEventCatapult eventCatapult;

    @Mock
    private Provider<MojosExecutionStrategy> mojosExecutionStrategy;

    @Mock
    private MessageBuilderFactory messageBuilderFactory;

    @InjectMocks
    private MojoExecutor mojoExecutor;

    /**
     * Verifies that {@link ProjectIndex} returns null for projects not in the reactor,
     * which is the precondition that caused the original NPE (issue #12600).
     */
    @Test
    void projectIndexReturnsNullForUnknownProject() {
        MavenProject knownProject = createProject("org.test", "known", "1.0");
        ProjectIndex index = new ProjectIndex(List.of(knownProject));

        // Known project has an index entry
        assertNotNull(index.getIndices().get("org.test:known:1.0"));
        assertEquals(0, index.getIndices().get("org.test:known:1.0"));
        assertNotNull(index.getProjects().get("org.test:known:1.0"));

        // Unknown project returns null from both maps — this is the scenario
        // that would cause NPE via auto-unboxing without the null guard
        assertNull(index.getIndices().get("org.test:unknown:2.0"));
        assertNull(index.getProjects().get("org.test:unknown:2.0"));
    }

    /**
     * Verifies that {@link MojoExecutor#executeForkedExecutions} throws a
     * {@link LifecycleExecutionException} with a descriptive message when the
     * forked execution references a project not present in the reactor,
     * instead of throwing a {@link NullPointerException} from auto-unboxing.
     */
    @Test
    void executeForkedExecutionsThrowsDescriptiveExceptionForUnknownProject() throws Exception {
        MavenProject knownProject = createProject("org.test", "known", "1.0");
        ProjectIndex projectIndex = new ProjectIndex(List.of(knownProject));

        // Set up a MojoExecution with forked executions referencing an unknown project
        MojoExecution mojoExecution = new MojoExecution(mock(MojoDescriptor.class));
        String unknownProjectId = "org.test:unknown:2.0";
        mojoExecution.setForkedExecutions(unknownProjectId, List.of());

        // Mock the session chain: MavenSession -> Session -> SessionData -> ProjectIndex
        MavenSession mavenSession = mock(MavenSession.class);
        Session apiSession = mock(Session.class);
        SessionData sessionData = mock(SessionData.class);

        when(mavenSession.getSession()).thenReturn(apiSession);
        when(mavenSession.getCurrentProject()).thenReturn(knownProject);
        when(apiSession.getData()).thenReturn(sessionData);
        when(sessionData.computeIfAbsent(any(), any())).thenReturn(projectIndex);

        // Should throw LifecycleExecutionException, not NullPointerException
        LifecycleExecutionException ex = assertThrows(
                LifecycleExecutionException.class,
                () -> mojoExecutor.executeForkedExecutions(mojoExecution, mavenSession));

        assertTrue(
                ex.getMessage().contains(unknownProjectId), "Exception message should contain the unknown project ID");
        assertTrue(ex.getMessage().contains("not in the reactor"), "Exception message should mention the reactor");
    }

    private static MavenProject createProject(String groupId, String artifactId, String version) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        return project;
    }
}
