package org.apache.maven.repository.legacy;

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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.repository.LegacyRepositorySystem;
import org.codehaus.plexus.component.annotations.Component;

/**
 * A dummy component to satisfy the requirements of {@link LegacyRepositorySystem}.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ArtifactMetadataSource.class )
public class DummyArtifactMetadataSource
    implements ArtifactMetadataSource
{

    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                     List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return null;
    }

    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                                            List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return null;
    }

    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact,
                                                                                    ArtifactRepository localRepository,
                                                                                    ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        return null;
    }

}
