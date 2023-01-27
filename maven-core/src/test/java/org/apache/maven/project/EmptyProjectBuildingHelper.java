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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

/**
 * A stub implementation to bypass artifact resolution from repositories.
 *
 * @author Benjamin Bentmann
 */
public class EmptyProjectBuildingHelper implements ProjectBuildingHelper {

    public List<ArtifactRepository> createArtifactRepositories(
            List<Repository> pomRepositories,
            List<ArtifactRepository> externalRepositories,
            ProjectBuildingRequest request) {
        if (externalRepositories != null) {
            return externalRepositories;
        } else {
            return new ArrayList<>();
        }
    }

    public ProjectRealmCache.CacheRecord createProjectRealm(
            MavenProject project, Model model, ProjectBuildingRequest request) {
        return new ProjectRealmCache.CacheRecord(null, null);
    }

    public void selectProjectRealm(MavenProject project) {}
}
