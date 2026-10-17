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
package org.apache.maven.lifecycle.internal.concurrent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;

@Named("concurrent")
@Singleton
public class MojoExecutor extends org.apache.maven.lifecycle.internal.MojoExecutor {

    @Inject
    public MojoExecutor(
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final SessionData.Key<Map<MavenProject, ReentrantLock>> PROJECT_EXECUTION_LOCKS =
            (SessionData.Key) SessionData.key(Map.class, MojoExecutor.class);

    /**
     * Prepares the dependencies of a mojo by replacing the resolved artifacts and the artifact filter of the shared
     * {@link MavenProject}, which the mojo reads when it is configured. This is done for every mojo, also for the
     * ones that need no dependency, as they get an empty filter. The executions of a single project are therefore
     * serialized, otherwise a mojo may be configured with the artifacts prepared for another one, so that the
     * compiler misses {@code provided} dependencies. Executions of different projects still run in parallel.
     */
    @Override
    protected NoExceptionCloseable getProjectLock(MavenSession session, MojoDescriptor mojoDescriptor) {
        Map<MavenProject, ReentrantLock> locks =
                session.getSession().getData().computeIfAbsent(PROJECT_EXECUTION_LOCKS, ConcurrentHashMap::new);
        ReentrantLock lock = locks.computeIfAbsent(session.getCurrentProject(), p -> new ReentrantLock());
        lock.lock();
        return lock::unlock;
    }
}
