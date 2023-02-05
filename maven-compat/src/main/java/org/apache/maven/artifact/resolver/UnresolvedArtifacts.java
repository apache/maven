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
package org.apache.maven.artifact.resolver;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * A simple recording of the Artifacts that could not be resolved for a given resolution request, along with
 * the remote repositories where attempts were made to resolve the artifacts.
 *
 * @author Jason van Zyl
 */
public class UnresolvedArtifacts {
    private Artifact originatingArtifact;

    private List<Artifact> artifacts;

    private List<ArtifactRepository> remoteRepositories;

    public UnresolvedArtifacts(
            Artifact originatingArtifact, List<Artifact> artifacts, List<ArtifactRepository> remoteRepositories) {
        this.originatingArtifact = originatingArtifact;

        this.artifacts = artifacts;

        this.remoteRepositories = remoteRepositories;
    }

    public Artifact getOriginatingArtifact() {
        return originatingArtifact;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }
}
