package org.apache.maven.repository.legacy.resolver.transform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author Jason van Zyl
 */
@Component( role = ArtifactTransformationManager.class )
public class DefaultArtifactTransformationManager
    implements ArtifactTransformationManager
{
    @Requirement( role = ArtifactTransformation.class, hints = { "release", "latest", "snapshot" } )
    private List<ArtifactTransformation> artifactTransformations;

    public void transformForResolve( Artifact artifact, RepositoryRequest request )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        for ( ArtifactTransformation transform : artifactTransformations )
        {
            transform.transformForResolve( artifact, request );
        }
    }

    public void transformForResolve( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                                     ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        for ( ArtifactTransformation transform : artifactTransformations )
        {
            transform.transformForResolve( artifact, remoteRepositories, localRepository );
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        for ( ArtifactTransformation transform : artifactTransformations )
        {
            transform.transformForInstall( artifact, localRepository );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository,
                                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        for ( ArtifactTransformation transform : artifactTransformations )
        {
            transform.transformForDeployment( artifact, remoteRepository, localRepository );
        }
    }

    public List<ArtifactTransformation> getArtifactTransformations()
    {
        return artifactTransformations;
    }
}
