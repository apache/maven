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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A step with no dependency scopes must not overwrite the artifacts resolved for another step of the
 * same project, while the scope filter must still be applied (MNG-3297). See #14081.
 */
class MojoExecutorEmptyScopeTest {

    private static final Collection<String> COMPILE_SCOPES =
            Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED);

    private final MavenProject project = new MavenProject(new Model());

    private final LifecycleDependencyResolver resolver = mock(LifecycleDependencyResolver.class);

    private final MavenSession session = mock(MavenSession.class);

    private final MojoExecutor executor = new MojoExecutor(null, null, resolver, null, null, null);

    private static Artifact artifact(String artifactId, String scope) {
        return new DefaultArtifact("g", artifactId, "1", scope, "jar", null, new DefaultArtifactHandler("jar"));
    }

    private void setUpResolver() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<String> scopes = invocation.getArgument(2);
                    Set<Artifact> resolved = new LinkedHashSet<>();
                    if (scopes.contains(Artifact.SCOPE_COMPILE)) {
                        resolved.add(artifact("compile-dep", Artifact.SCOPE_COMPILE));
                    }
                    if (scopes.contains(Artifact.SCOPE_PROVIDED)) {
                        resolved.add(artifact("provided-dep", Artifact.SCOPE_PROVIDED));
                    }
                    project.setResolvedArtifacts(resolved);
                    if (project.getDependencyArtifacts() == null) {
                        project.setDependencyArtifacts(resolved);
                    }
                    return null;
                })
                .when(resolver)
                .resolveProjectDependencies(any(), any(), any(), any(), anyBoolean(), any());
    }

    private static MojoDescriptor mojo(String scope) {
        MojoDescriptor descriptor = new MojoDescriptor();
        descriptor.setPluginDescriptor(new PluginDescriptor());
        if (scope != null) {
            descriptor.setDependencyResolutionRequired(scope);
        }
        return descriptor;
    }

    @Test
    void emptyScopeStepKeepsResolvedArtifacts() throws Exception {
        setUpResolver();
        DependencyContext compileStep = new DependencyContext(project, Collections.emptyList(), COMPILE_SCOPES);
        executor.ensureDependenciesAreResolved(mojo("compile"), session, compileStep);
        assertEquals(2, project.getArtifacts().size());
        verify(resolver).resolveProjectDependencies(any(), any(), any(), any(), anyBoolean(), any());
        clearInvocations(resolver);

        // A fresh step with no scopes must not resolve (and therefore must not clear) the artifacts.
        DependencyContext noScopeStep =
                new DependencyContext(project, Collections.emptyList(), Collections.emptyList());
        executor.ensureDependenciesAreResolved(mojo(null), session, noScopeStep);

        verifyNoInteractions(resolver);
        assertEquals(0, project.getArtifacts().size(), "the no-scope mojo must still see no artifacts");

        // A later mojo of the original step must still see the artifacts resolved for that step.
        executor.ensureDependenciesAreResolved(mojo("compile"), session, compileStep);
        assertEquals(2, project.getArtifacts().size(), "the original step's artifacts must survive");
    }
}
