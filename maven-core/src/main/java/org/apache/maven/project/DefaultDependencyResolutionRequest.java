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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;

/**
 */
public class DefaultDependencyResolutionRequest implements DependencyResolutionRequest {

    private MavenProject project;

    private DependencyFilter filter;

    private RepositorySystemSession session;

    private Collection<String> collectScopesToExclude;

    private Collection<String> resolveScopesToExclude;

    public DefaultDependencyResolutionRequest() {
        // enables default constructor
    }

    public DefaultDependencyResolutionRequest(MavenProject project, RepositorySystemSession session) {
        setMavenProject(project);
        setRepositorySession(session);
    }

    public DependencyFilter getResolutionFilter() {
        return filter;
    }

    public MavenProject getMavenProject() {
        return project;
    }

    public RepositorySystemSession getRepositorySession() {
        return session;
    }

    public DependencyResolutionRequest setResolutionFilter(DependencyFilter filter) {
        this.filter = filter;
        return this;
    }

    public DependencyResolutionRequest setMavenProject(MavenProject project) {
        this.project = project;
        return this;
    }

    public DependencyResolutionRequest setRepositorySession(RepositorySystemSession repositorySession) {
        this.session = repositorySession;
        return this;
    }

    public void setCollectScopesToExclude(Collection<String> collectScopesToExclude) {
        this.collectScopesToExclude = collectScopesToExclude;
    }

    public void setResolveScopesToExclude(Collection<String> resolveScopesToExclude) {
        this.resolveScopesToExclude = resolveScopesToExclude;
    }

    public Collection<String> getCollectScopesToExclude() {
        return collectScopesToExclude;
    }

    public Collection<String> getResolveScopesToExclude() {
        return resolveScopesToExclude;
    }
}
