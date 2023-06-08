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
package org.apache.maven.artifact.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * ResolutionGroup
 */
@Deprecated
public class ResolutionGroup extends org.apache.maven.repository.legacy.metadata.ResolutionGroup {

    public ResolutionGroup(
            Artifact pomArtifact, Set<Artifact> artifacts, List<ArtifactRepository> resolutionRepositories) {
        super(pomArtifact, artifacts, resolutionRepositories);
    }

    public ResolutionGroup(
            Artifact pomArtifact,
            Artifact relocatedArtifact,
            Set<Artifact> artifacts,
            Map<String, Artifact> managedVersions,
            List<ArtifactRepository> resolutionRepositories) {
        super(pomArtifact, relocatedArtifact, artifacts, managedVersions, resolutionRepositories);
    }
}
