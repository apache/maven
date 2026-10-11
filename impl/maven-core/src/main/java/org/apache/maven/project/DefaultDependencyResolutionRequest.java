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
package org.apache.maven.project;

import java.util.Collection;
import java.util.Set;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;

/**
 * @deprecated use {@code org.apache.maven.api.services.ProjectBuilder} instead
 */
@Deprecated(since = "4.0.0")
public class DefaultDependencyResolutionRequest implements DependencyResolutionRequest {

    private MavenProject project;

    private DependencyFilter filter;

    private RepositorySystemSession session;

    private Set<String> excludedScopes = Set.of();

    public DefaultDependencyResolutionRequest() {
        // enables default constructor
    }

    public DefaultDependencyResolutionRequest(MavenProject project, RepositorySystemSession session) {
        setMavenProject(project);
        setRepositorySession(session);
    }

    @Override
    public DependencyFilter getResolutionFilter() {
        return filter;
    }

    @Override
    public MavenProject getMavenProject() {
        return project;
    }

    @Override
    public RepositorySystemSession getRepositorySession() {
        return session;
    }

    @Override
    public DependencyResolutionRequest setResolutionFilter(DependencyFilter filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public DependencyResolutionRequest setMavenProject(MavenProject project) {
        this.project = project;
        return this;
    }

    @Override
    public DependencyResolutionRequest setRepositorySession(RepositorySystemSession repositorySession) {
        this.session = repositorySession;
        return this;
    }

    /**
     * Returns the scopes of the direct dependencies that are left out of the collected dependency graph.
     *
     * @return the excluded scopes, never {@code null}
     * @since 4.1.0
     */
    public Set<String> getExcludedScopes() {
        return excludedScopes;
    }

    /**
     * Sets the scopes of the direct dependencies that are left out of the collected dependency graph, so that they
     * do not take part in the conflict resolution of the dependencies of the other scopes.
     *
     * @param excludedScopes the excluded scopes, may be {@code null}
     * @return this request, never {@code null}
     * @since 4.1.0
     */
    public DefaultDependencyResolutionRequest setExcludedScopes(Collection<String> excludedScopes) {
        this.excludedScopes = excludedScopes != null ? Set.copyOf(excludedScopes) : Set.of();
        return this;
    }
}
