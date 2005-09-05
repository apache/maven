package org.apache.maven.artifact.transform;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Iterator;
import java.util.List;

public class DefaultArtifactTransformationManager
    implements ArtifactTransformationManager
{
    private List artifactTransformations;

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            transform.transformForResolve( artifact, remoteRepositories, localRepository );
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            transform.transformForInstall( artifact, localRepository );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository,
                                        ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            transform.transformForDeployment( artifact, remoteRepository, localRepository );
        }
    }

}
