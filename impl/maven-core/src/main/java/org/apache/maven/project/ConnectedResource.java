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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.model.Resource;

/**
 * A Resource wrapper that maintains a connection to the underlying project model.
 * When includes/excludes are modified, the changes are propagated back to the project's SourceRoots.
 */
class ConnectedResource extends Resource {
    private final SourceRoot originalSourceRoot;
    private final ProjectScope scope;
    private final MavenProject project;

    ConnectedResource(SourceRoot sourceRoot, ProjectScope scope, MavenProject project) {
        super(org.apache.maven.api.model.Resource.newBuilder()
                .directory(sourceRoot.directory().toString())
                .includes(sourceRoot.includes())
                .excludes(sourceRoot.excludes())
                .filtering(Boolean.toString(sourceRoot.stringFiltering()))
                .build());
        this.originalSourceRoot = sourceRoot;
        this.scope = scope;
        this.project = project;
    }

    @Override
    public void addInclude(String include) {
        // Update the underlying Resource model
        super.addInclude(include);

        // Update the project's SourceRoots
        updateProjectSourceRoot();
    }

    @Override
    public void removeInclude(String include) {
        // Update the underlying Resource model
        super.removeInclude(include);

        // Update the project's SourceRoots
        updateProjectSourceRoot();
    }

    @Override
    public void addExclude(String exclude) {
        // Update the underlying Resource model
        super.addExclude(exclude);

        // Update the project's SourceRoots
        updateProjectSourceRoot();
    }

    @Override
    public void removeExclude(String exclude) {
        // Update the underlying Resource model
        super.removeExclude(exclude);

        // Update the project's SourceRoots
        updateProjectSourceRoot();
    }

    @Override
    public void setIncludes(List<String> includes) {
        // Update the underlying Resource model
        super.setIncludes(includes);

        // Update the project's SourceRoots
        updateProjectSourceRoot();
    }

    @Override
    public void setExcludes(List<String> excludes) {
        // Update the underlying Resource model
        super.setExcludes(excludes);

        // Update the project's SourceRoots
        updateProjectSourceRoot();
    }

    private void updateProjectSourceRoot() {
        // Convert the LinkedHashSet to a List to maintain order
        List<SourceRoot> sourcesList = new ArrayList<>(project.sources);

        // Find the index of the original SourceRoot
        int index = -1;
        for (int i = 0; i < sourcesList.size(); i++) {
            SourceRoot source = sourcesList.get(i);
            if (source.scope() == originalSourceRoot.scope()
                    && source.language() == originalSourceRoot.language()
                    && source.directory().equals(originalSourceRoot.directory())) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            // Replace the SourceRoot at the same position
            SourceRoot newSourceRoot = new DefaultSourceRoot(project.getBaseDirectory(), scope, this.getDelegate());
            sourcesList.set(index, newSourceRoot);

            // Update the project's sources, preserving order
            project.sources.clear();
            project.sources.addAll(sourcesList);
        }
    }
}
