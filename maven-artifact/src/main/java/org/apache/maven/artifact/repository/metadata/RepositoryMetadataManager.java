package org.apache.maven.artifact.repository.metadata;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

public interface RepositoryMetadataManager
{
    void resolve( ArtifactMetadata repositoryMetadata, List repositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    void resolveAlways( ArtifactMetadata metadata, ArtifactRepository localRepository,
                        ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException;
}
