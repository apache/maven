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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Provides the positional index of the project
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (extracted class only)
 */
// TODO Kristian wonders if this class really is necessary and if it overlaps other concepts.
public final class ProjectIndex {

    private final Map<String, MavenProject> projects;

    private final Map<String, Integer> indices;

    public ProjectIndex(List<MavenProject> projects) {
        this.projects = new HashMap<>(projects.size() * 2);
        this.indices = new HashMap<>(projects.size() * 2);

        for (int i = 0; i < projects.size(); i++) {
            MavenProject project = projects.get(i);
            String key = BuilderCommon.getKey(project);

            this.getProjects().put(key, project);
            this.getIndices().put(key, i);
        }
    }

    public Map<String, MavenProject> getProjects() {
        return projects;
    }

    public Map<String, Integer> getIndices() {
        return indices;
    }
}
