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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

/**
 * @author Kristian Rosenvold
 */
public class ProjectDependenciesResolverStub
        implements ProjectDependenciesResolver, org.apache.maven.project.ProjectDependenciesResolver {
    public Set<Artifact> resolve(MavenProject project, Collection<String> scopesToResolve, MavenSession session)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        return new HashSet<>();
    }

    public Set<Artifact> resolve(
            MavenProject project,
            Collection<String> scopesToCollect,
            Collection<String> scopesToResolve,
            MavenSession session)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        return new HashSet<>();
    }

    public Set<Artifact> resolve(
            Collection<? extends MavenProject> projects, Collection<String> scopes, MavenSession session)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        return new HashSet<>();
    }

    public Set<Artifact> resolve(
            MavenProject project,
            Collection<String> scopesToCollect,
            Collection<String> scopesToResolve,
            MavenSession session,
            Set<Artifact> ignoreableArtifacts)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        return new HashSet<>();
    }

    public DependencyResolutionResult resolve(DependencyResolutionRequest request)
            throws DependencyResolutionException {
        return new DependencyResolutionResult() {

            public List<Dependency> getUnresolvedDependencies() {
                return Collections.emptyList();
            }

            public List<Dependency> getResolvedDependencies() {
                return Collections.emptyList();
            }

            public List<Exception> getResolutionErrors(Dependency dependency) {
                return Collections.emptyList();
            }

            public DependencyNode getDependencyGraph() {
                return new DefaultDependencyNode((Dependency) null);
            }

            public List<Dependency> getDependencies() {
                return Collections.emptyList();
            }

            public List<Exception> getCollectionErrors() {
                return Collections.emptyList();
            }
        };
    }
}
