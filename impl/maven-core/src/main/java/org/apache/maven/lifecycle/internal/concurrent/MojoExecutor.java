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
import java.util.concurrent.locks.Lock;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final SessionData.Key<Map<MavenProject, Lock>> PROJECT_LOCKS =
            (SessionData.Key) SessionData.key(Map.class, MojoExecutor.class);

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

    /**
     * The concurrent builder runs independent steps of the same project in parallel, for instance
     * {@code process-resources} next to {@code compile}. Before a mojo runs, the dependencies required by its
     * step are resolved and stored, together with the mojo's scope filter, on the shared {@link MavenProject},
     * and the mojo reads them back while it executes. Executions on the same project must therefore not
     * overlap, or a mojo sees the dependencies resolved for another step. Executions on different projects
     * still run in parallel.
     */
    @Override
    protected NoExceptionCloseable getProjectLock(MavenSession session, MojoDescriptor mojoDescriptor) {
        Lock lock = session.getSession()
                .getData()
                .computeIfAbsent(PROJECT_LOCKS, ConcurrentHashMap::new)
                .computeIfAbsent(session.getCurrentProject(), p -> new ReentrantLock());
        lock.lock();
        return lock::unlock;
    }
}
