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
package org.apache.maven.internal.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.api.Project;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.spi.ProjectTransformer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectTransformerDispatcherTest {

    @Test
    void chainsTransformersAndWritesModelBack() throws Exception {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.test");
        mavenProject.setArtifactId("test");
        mavenProject.setVersion("1.0");

        MavenSession mavenSession = mock(MavenSession.class);
        InternalMavenSession apiSession = mock(InternalMavenSession.class);
        when(mavenSession.getSession()).thenReturn(apiSession);
        when(mavenSession.getProjects()).thenReturn(List.of(mavenProject));

        Project apiProject = mock(Project.class);
        Model originalModel = mavenProject.getModel().getDelegate();
        when(apiSession.getProject(mavenProject)).thenReturn(apiProject);
        when(apiProject.model()).thenReturn(originalModel);

        List<String> calls = new ArrayList<>();
        ProjectTransformer addProperty = (session, project, model) -> {
            calls.add("addProperty");
            return model.withProperties(java.util.Map.of("injected", "true"));
        };
        ProjectTransformer addProperty2 = (session, project, model) -> {
            calls.add("addProperty2");
            assertEquals("true", model.getProperties().get("injected"));
            return model.withProperties(java.util.Map.of("injected", "true", "second", "yes"));
        };

        ProjectTransformerDispatcher.transformProjects(mavenSession, List.of(addProperty, addProperty2));

        assertEquals(List.of("addProperty", "addProperty2"), calls);
        assertEquals("yes", mavenProject.getModel().getDelegate().getProperties().get("second"));
    }

    @Test
    void runtimeExceptionWrappedInMavenExecutionException() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.test");
        mavenProject.setArtifactId("test");
        mavenProject.setVersion("1.0");

        MavenSession mavenSession = mock(MavenSession.class);
        InternalMavenSession apiSession = mock(InternalMavenSession.class);
        when(mavenSession.getSession()).thenReturn(apiSession);
        when(mavenSession.getProjects()).thenReturn(List.of(mavenProject));

        Project apiProject = mock(Project.class);
        when(apiSession.getProject(mavenProject)).thenReturn(apiProject);
        when(apiProject.model()).thenReturn(mavenProject.getModel().getDelegate());

        RuntimeException cause = new RuntimeException("buggy extension");
        ProjectTransformer failing = (session, project, model) -> {
            throw cause;
        };

        MavenExecutionException ex =
                assertThrows(MavenExecutionException.class, () -> ProjectTransformerDispatcher.transformProjects(
                        mavenSession, List.of(failing)));

        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("buggy extension", ex.getCause().getMessage());
    }

    @Test
    void frozenFieldViolationThrowsMavenExecutionException() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.test");
        mavenProject.setArtifactId("test");
        mavenProject.setVersion("1.0");

        MavenSession mavenSession = mock(MavenSession.class);
        InternalMavenSession apiSession = mock(InternalMavenSession.class);
        when(mavenSession.getSession()).thenReturn(apiSession);
        when(mavenSession.getProjects()).thenReturn(List.of(mavenProject));

        Project apiProject = mock(Project.class);
        when(apiSession.getProject(mavenProject)).thenReturn(apiProject);
        when(apiProject.model()).thenReturn(mavenProject.getModel().getDelegate());

        ProjectTransformer changesGroupId = (session, project, model) -> model.withGroupId("hacked");

        MavenExecutionException ex =
                assertThrows(MavenExecutionException.class, () -> ProjectTransformerDispatcher.transformProjects(
                        mavenSession, List.of(changesGroupId)));

        assertTrue(ex.getMessage().contains("groupId/artifactId"));
        assertTrue(ex.getMessage().contains("must not change"));
    }
}
