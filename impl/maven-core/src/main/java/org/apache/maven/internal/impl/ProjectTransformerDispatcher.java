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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.api.Project;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.spi.ProjectTransformer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * Dispatches {@link ProjectTransformer} instances over all reactor projects,
 * chaining each transformer's output model as the next transformer's input.
 */
public final class ProjectTransformerDispatcher {

    private ProjectTransformerDispatcher() {}

    public static void transformProjects(MavenSession session, Collection<ProjectTransformer> transformers)
            throws MavenExecutionException {
        if (transformers.isEmpty()) {
            return;
        }
        List<ProjectTransformer> chain = List.copyOf(transformers);
        InternalMavenSession apiSession = InternalMavenSession.from(session.getSession());
        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        try {
            for (MavenProject mavenProject : session.getProjects()) {
                Project project = apiSession.getProject(mavenProject);
                Model model = project.model();

                for (ProjectTransformer transformer : chain) {
                    thread.setContextClassLoader(transformer.getClass().getClassLoader());
                    try {
                        model = transformer.transformProject(apiSession, project, model);
                    } catch (RuntimeException e) {
                        throw new MavenExecutionException(
                                "Project transformer failed for " + mavenProject.getId(), e);
                    }
                    if (model == null) {
                        throw new MavenExecutionException(
                                "Project transformer "
                                        + transformer.getClass().getName() + " returned null model for "
                                        + mavenProject.getId(),
                                (Throwable) null);
                    }
                }

                if (model != project.model()) {
                    validateFrozenFields(project.model(), model, mavenProject.getId());
                    mavenProject.setModel(new org.apache.maven.model.Model(model));
                }
            }
        } finally {
            thread.setContextClassLoader(originalClassLoader);
        }
    }

    private static void validateFrozenFields(Model original, Model transformed, String projectId)
            throws MavenExecutionException {
        if (!Objects.equals(original.getGroupId(), transformed.getGroupId())
                || !Objects.equals(original.getArtifactId(), transformed.getArtifactId())) {
            throw new MavenExecutionException(
                    "Project transformer must not change groupId or artifactId of " + projectId, (Throwable) null);
        }
        if (!Objects.equals(original.getParent(), transformed.getParent())) {
            throw new MavenExecutionException(
                    "Project transformer must not change <parent> of " + projectId, (Throwable) null);
        }
        if (!Objects.equals(original.getMixins(), transformed.getMixins())) {
            throw new MavenExecutionException(
                    "Project transformer must not change <mixins> of " + projectId, (Throwable) null);
        }
        if (!Objects.equals(original.getModules(), transformed.getModules())) {
            throw new MavenExecutionException(
                    "Project transformer must not change <modules> of " + projectId, (Throwable) null);
        }
        if (!Objects.equals(original.getProfiles(), transformed.getProfiles())) {
            throw new MavenExecutionException(
                    "Project transformer must not change <profiles> of " + projectId, (Throwable) null);
        }
    }
}
