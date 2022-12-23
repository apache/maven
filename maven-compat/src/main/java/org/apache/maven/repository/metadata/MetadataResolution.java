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
package org.apache.maven.repository.metadata;

import java.util.Collection;

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 *
 * @author Jason van Zyl
 *
 */
public class MetadataResolution {
    /** resolved MD  */
    private ArtifactMetadata artifactMetadata;

    /** repositories, added by this POM  */
    private Collection<ArtifactRepository> metadataRepositories;
    // -------------------------------------------------------------------
    public MetadataResolution(ArtifactMetadata artifactMetadata) {
        this.artifactMetadata = artifactMetadata;
    }
    // -------------------------------------------------------------------
    public MetadataResolution(ArtifactMetadata artifactMetadata, Collection<ArtifactRepository> metadataRepositories) {
        this(artifactMetadata);
        this.metadataRepositories = metadataRepositories;
    }
    // -------------------------------------------------------------------
    public Collection<ArtifactRepository> getMetadataRepositories() {
        return metadataRepositories;
    }

    public void setMetadataRepositories(Collection<ArtifactRepository> metadataRepositories) {
        this.metadataRepositories = metadataRepositories;
    }
    // -------------------------------------------------------------------
    public ArtifactMetadata getArtifactMetadata() {
        return artifactMetadata;
    }

    public void setArtifactMetadata(ArtifactMetadata artifactMetadata) {
        this.artifactMetadata = artifactMetadata;
    }
    // -------------------------------------------------------------------
    // -------------------------------------------------------------------
}
