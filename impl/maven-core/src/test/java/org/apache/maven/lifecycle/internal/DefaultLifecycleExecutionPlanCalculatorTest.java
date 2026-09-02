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

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLifecycleExecutionPlanCalculatorTest {
    @Test
    void doesNotResolveProjectPluginsForDirectGoal() throws Exception {
        MojoDescriptorCreator mojoDescriptorCreator = mock(MojoDescriptorCreator.class);
        LifecyclePluginResolver lifecyclePluginResolver = mock(LifecyclePluginResolver.class);
        MavenSession session = mock(MavenSession.class);
        MavenProject project = new MavenProject();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.plugins");
        pluginDescriptor.setArtifactId("maven-help-plugin");
        pluginDescriptor.setVersion("1.0");
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);
        mojoDescriptor.setGoal("help");
        when(mojoDescriptorCreator.getMojoDescriptor("help:help", session, project))
                .thenReturn(mojoDescriptor);

        DefaultLifecycleExecutionPlanCalculator calculator = new DefaultLifecycleExecutionPlanCalculator(
                mock(BuildPluginManager.class),
                mock(DefaultLifecycles.class),
                mojoDescriptorCreator,
                lifecyclePluginResolver);

        calculator.calculateExecutionPlan(session, project, List.of(new GoalTask("help:help")), false);

        verify(lifecyclePluginResolver, never()).resolveMissingPluginVersions(project, session);
    }

    @Test
    void resolvesProjectPluginsForLifecycleTask() throws Exception {
        LifecyclePluginResolver lifecyclePluginResolver = mock(LifecyclePluginResolver.class);
        MavenSession session = mock(MavenSession.class);
        MavenProject project = new MavenProject();
        DefaultLifecycles defaultLifecycles = mock(DefaultLifecycles.class);
        Lifecycle lifecycle = new Lifecycle("default", List.of("validate"), Map.of());
        LifecycleMappingDelegate lifecycleMappingDelegate = mock(LifecycleMappingDelegate.class);

        when(defaultLifecycles.get("validate")).thenReturn(lifecycle);
        when(lifecycleMappingDelegate.calculateLifecycleMappings(session, project, lifecycle, "validate"))
                .thenReturn(Map.of());

        DefaultLifecycleExecutionPlanCalculator calculator = new DefaultLifecycleExecutionPlanCalculator(
                mock(BuildPluginManager.class),
                defaultLifecycles,
                mock(MojoDescriptorCreator.class),
                lifecyclePluginResolver,
                lifecycleMappingDelegate,
                Map.of(),
                Map.of());

        calculator.calculateExecutionPlan(session, project, List.of(new LifecycleTask("validate")), false);

        verify(lifecyclePluginResolver).resolveMissingPluginVersions(project, session);
    }
}
