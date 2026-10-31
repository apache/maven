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

import javax.inject.Inject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.spi.LifecycleParticipant;
import org.apache.maven.api.spi.LifecycleParticipantException;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.LifecycleParticipantDispatcher;
import org.apache.maven.internal.impl.MapBackedSessionData;
import org.apache.maven.internal.transformation.TransformerManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.lifecycle.internal.stub.MojoExecutorStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PlexusTest
class LifecycleModuleBuilderTest {
    @Inject
    PlexusContainer container;

    @Test
    void testCurrentProject() throws Exception {
        List<MavenProject> currentProjects = new ArrayList<>();
        MojoExecutorStub mojoExecutor = new MojoExecutorStub() {
            @Override
            public void execute(MavenSession session, List<MojoExecution> mojoExecutions)
                    throws LifecycleExecutionException {
                super.execute(session, mojoExecutions);
                currentProjects.add(session.getCurrentProject());
            }
        };

        final DefaultMavenExecutionResult defaultMavenExecutionResult = new DefaultMavenExecutionResult();
        MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        mavenExecutionRequest.setExecutionListener(new AbstractExecutionListener());
        mavenExecutionRequest.setGoals(Arrays.asList("clean"));
        final MavenSession session = new MavenSession(
                null,
                new DefaultRepositorySystemSession(h -> false),
                mavenExecutionRequest,
                defaultMavenExecutionResult);
        final ProjectDependencyGraphStub dependencyGraphStub = new ProjectDependencyGraphStub();
        session.setProjectDependencyGraph(dependencyGraphStub);
        session.setProjects(dependencyGraphStub.getSortedProjects());

        LifecycleModuleBuilder moduleBuilder = container.lookup(LifecycleModuleBuilder.class);
        set(moduleBuilder, "mojoExecutor", mojoExecutor);

        LifecycleStarter ls = container.lookup(LifecycleStarter.class);
        ls.execute(session);

        assertNull(session.getCurrentProject());
        assertEquals(
                Arrays.asList(
                        ProjectDependencyGraphStub.A,
                        ProjectDependencyGraphStub.B,
                        ProjectDependencyGraphStub.C,
                        ProjectDependencyGraphStub.X,
                        ProjectDependencyGraphStub.Y,
                        ProjectDependencyGraphStub.Z),
                currentProjects);
    }

    @Test
    void participantFailureAfterSuccessDoesNotNotifyProjectFailure() throws Exception {
        MojoExecutor mojoExecutor = mock(MojoExecutor.class);
        BuilderCommon builderCommon = mock(BuilderCommon.class);
        ExecutionEventCatapult eventCatapult = mock(ExecutionEventCatapult.class);
        ProjectExecutionListener projectExecutionListener = mock(ProjectExecutionListener.class);
        TransformerManager transformerManager = mock(TransformerManager.class);
        LifecycleModuleBuilder moduleBuilder = new LifecycleModuleBuilder(
                mojoExecutor, builderCommon, eventCatapult, List.of(projectExecutionListener), transformerManager);

        MavenProject project = new MavenProject();
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        DefaultMavenExecutionResult result = new DefaultMavenExecutionResult();
        MavenSession session = new MavenSession(null, new DefaultRepositorySystemSession(h -> false), request, result);
        InternalMavenSession apiSession = mock(InternalMavenSession.class);
        when(apiSession.getData()).thenReturn(new MapBackedSessionData());
        when(apiSession.getProject(project)).thenReturn(mock(Project.class));
        session.setSession(apiSession);
        LifecycleParticipantDispatcher.setParticipants(session, List.of(new LifecycleParticipant() {
            @Override
            public void afterProjectExecution(org.apache.maven.api.spi.ProjectExecutionEvent event)
                    throws LifecycleParticipantException {
                throw new LifecycleParticipantException("callback failed");
            }
        }));

        when(builderCommon.resolveBuildPlan(eq(session), eq(project), any(), any()))
                .thenReturn(new MavenExecutionPlan(List.of(), null));
        ReactorContext reactorContext = new ReactorContext(
                result,
                Thread.currentThread().getContextClassLoader(),
                new ReactorBuildStatus(new ProjectDependencyGraphStub()));

        moduleBuilder.buildProject(session, reactorContext, project, new TaskSegment(false));

        verify(projectExecutionListener).afterProjectExecutionSuccess(any(ProjectExecutionEvent.class));
        verify(projectExecutionListener, never()).afterProjectExecutionFailure(any(ProjectExecutionEvent.class));
        verify(builderCommon).handleBuildError(eq(reactorContext), eq(session), eq(session), eq(project), any(), any());
    }

    static void set(Object obj, String field, Object v) throws NoSuchFieldException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, v);
    }
}
