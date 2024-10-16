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
package org.apache.maven.lifecycle.internal.stub;

import javax.inject.Provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 */
public class MojoExecutorStub extends MojoExecutor { // This is being lazy instead of making interface

    public final List<MojoExecution> executions = Collections.synchronizedList(new ArrayList<>());

    public MojoExecutorStub() {
        super(null, null, null, null, null, null);
    }

    public MojoExecutorStub(
            BuildPluginManager pluginManager,
            MavenPluginManager mavenPluginManager,
            LifecycleDependencyResolver lifeCycleDependencyResolver,
            ExecutionEventCatapult eventCatapult,
            Provider<MojosExecutionStrategy> mojosExecutionStrategy,
            MessageBuilderFactory messageBuilderFactory) {
        super(
                pluginManager,
                mavenPluginManager,
                lifeCycleDependencyResolver,
                eventCatapult,
                mojosExecutionStrategy,
                messageBuilderFactory);
    }

    @Override
    public void execute(MavenSession session, List<MojoExecution> mojoExecutions) throws LifecycleExecutionException {
        executions.addAll(mojoExecutions);
    }

    @Override
    public List<MavenProject> executeForkedExecutions(MojoExecution mojoExecution, MavenSession session)
            throws LifecycleExecutionException {
        return null;
    }

    public static MojoDescriptor createMojoDescriptor(Plugin plugin) {
        final PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setGroupId(plugin.getGroupId());
        descriptor.setArtifactId(plugin.getArtifactId());
        descriptor.setPlugin(plugin);
        descriptor.setVersion(plugin.getVersion());
        final MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(descriptor);
        return mojoDescriptor;
    }
}
