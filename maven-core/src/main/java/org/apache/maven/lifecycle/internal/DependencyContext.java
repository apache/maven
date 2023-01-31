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
import java.util.TreeSet;

import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Context of dependency artifacts for a particular project.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (class extract only)
 */
// TODO From a concurrency perspective, this class is not good. The combination of mutable/immutable state is not nice
public class DependencyContext {

    private static final Collection<?> UNRESOLVED = Arrays.asList();

    private final MavenProject project;

    private final Collection<String> scopesToCollectForCurrentProject;

    private final Collection<String> scopesToResolveForCurrentProject;

    private final Collection<String> scopesToCollectForAggregatedProjects;

    private final Collection<String> scopesToResolveForAggregatedProjects;

    private volatile Collection<?> lastDependencyArtifacts = UNRESOLVED;

    private volatile int lastDependencyArtifactCount = -1;

    public DependencyContext(
            MavenProject project, Collection<String> scopesToCollect, Collection<String> scopesToResolve) {
        this.project = project;
        scopesToCollectForCurrentProject = scopesToCollect;
        scopesToResolveForCurrentProject = scopesToResolve;
        scopesToCollectForAggregatedProjects = Collections.synchronizedSet(new TreeSet<String>());
        scopesToResolveForAggregatedProjects = Collections.synchronizedSet(new TreeSet<String>());
    }

    public MavenProject getProject() {
        return project;
    }

    public Collection<String> getScopesToCollectForCurrentProject() {
        return scopesToCollectForCurrentProject;
    }

    public Collection<String> getScopesToResolveForCurrentProject() {
        return scopesToResolveForCurrentProject;
    }

    public Collection<String> getScopesToCollectForAggregatedProjects() {
        return scopesToCollectForAggregatedProjects;
    }

    public Collection<String> getScopesToResolveForAggregatedProjects() {
        return scopesToResolveForAggregatedProjects;
    }

    public boolean isResolutionRequiredForCurrentProject() {
        return lastDependencyArtifacts != project.getDependencyArtifacts()
                || (lastDependencyArtifacts != null && lastDependencyArtifactCount != lastDependencyArtifacts.size());
    }

    public boolean isResolutionRequiredForAggregatedProjects(
            Collection<String> scopesToCollect, Collection<String> scopesToResolve) {
        boolean required = scopesToCollectForAggregatedProjects.addAll(scopesToCollect)
                || scopesToResolveForAggregatedProjects.addAll(scopesToResolve);
        return required;
    }

    public void synchronizeWithProjectState() {
        lastDependencyArtifacts = project.getDependencyArtifacts();
        lastDependencyArtifactCount = (lastDependencyArtifacts != null) ? lastDependencyArtifacts.size() : 0;
    }
}
