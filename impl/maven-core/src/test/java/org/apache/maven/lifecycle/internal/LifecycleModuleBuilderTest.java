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

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
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

    static void set(Object obj, String field, Object v) throws NoSuchFieldException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, v);
    }
}
