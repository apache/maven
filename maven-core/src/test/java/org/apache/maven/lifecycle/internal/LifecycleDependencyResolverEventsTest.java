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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.message.internal.DefaultMessageBuilderFactory;
import org.codehaus.plexus.logging.Logger;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.project.artifact.DefaultProjectArtifactsCache;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that the event spies get the {@link DependencyResolutionResult}, whether the resolution succeeds or fails.
 */
class LifecycleDependencyResolverEventsTest {

    private ProjectDependenciesResolver dependenciesResolver;
    private EventSpyDispatcher eventSpyDispatcher;
    private LifecycleDependencyResolver resolver;
    private MavenSession session;
    private DependencyResolutionResult result;
    private DependencyResolutionException failure;

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        dependenciesResolver = mock(ProjectDependenciesResolver.class);
        eventSpyDispatcher = mock(EventSpyDispatcher.class);
        ProjectArtifactFactory artifactFactory = mock(ProjectArtifactFactory.class);
        when(artifactFactory.createArtifacts(any(MavenProject.class))).thenReturn(Collections.emptySet());
        resolver = new LifecycleDependencyResolver();
        setField(resolver, "dependenciesResolver", dependenciesResolver);
        setField(resolver, "eventSpyDispatcher", eventSpyDispatcher);
        setField(resolver, "projectArtifactsCache", new DefaultProjectArtifactsCache());
        setField(resolver, "messageBuilderFactory", new DefaultMessageBuilderFactory());
        setField(resolver, "artifactFactory", artifactFactory);
        setField(resolver, "logger", mock(Logger.class));
        session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(mock(RepositorySystemSession.class));
        when(session.getProjects()).thenReturn(Collections.emptyList());
        result = mock(DependencyResolutionResult.class);
        when(result.getUnresolvedDependencies()).thenReturn(Collections.emptyList());
        failure = new DependencyResolutionException(result, "cannot resolve", new RuntimeException());
    }

    private MavenProject newProject() {
        MavenProject project = new MavenProject();
        project.setRemoteArtifactRepositories(Collections.emptyList());
        project.setPluginArtifactRepositories(Collections.emptyList());
        return project;
    }

    private void resolve(boolean aggregating) throws LifecycleExecutionException {
        resolver.resolveProjectDependencies(
                newProject(), null, Collections.singletonList("compile"), session, aggregating, new HashSet<>());
    }

    private List<Object> events() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventSpyDispatcher, org.mockito.Mockito.atLeast(1)).onEvent(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void testResultIsFiredWhenTheResolutionSucceeds() throws Exception {
        when(dependenciesResolver.resolve(any(DependencyResolutionRequest.class)))
                .thenReturn(result);

        resolve(false);

        verify(eventSpyDispatcher, times(1)).onEvent(result);
    }

    @Test
    void testResultIsFiredWhenTheResolutionFails() throws Exception {
        when(dependenciesResolver.resolve(any(DependencyResolutionRequest.class)))
                .thenThrow(failure);

        assertThrows(LifecycleExecutionException.class, () -> resolve(false));

        verify(eventSpyDispatcher, times(1)).onEvent(result);
        // the request is fired first, then the result
        List<Object> events = events();
        assertSame(result, events.get(events.size() - 1));
    }

    @Test
    void testResultIsFiredOnceWhenAnAggregatorToleratesTheFailure() throws Exception {
        when(dependenciesResolver.resolve(any(DependencyResolutionRequest.class)))
                .thenThrow(failure);

        resolve(true);

        verify(eventSpyDispatcher, times(1)).onEvent(result);
    }
}