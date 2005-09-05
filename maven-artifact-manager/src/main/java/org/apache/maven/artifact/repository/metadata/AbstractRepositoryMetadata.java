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

import java.io.IOException;

/**
 * Shared methods of the repository metadata handling.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractRepositoryMetadata
    implements ArtifactMetadata
{
    public String getRemoteFilename()
    {
        return "maven-metadata.xml";
    }

    public String getLocalFilename( ArtifactRepository repository )
    {
        return "maven-metadata-" + repository.getKey() + ".xml";
    }

    public void storeInLocalRepository( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        try
        {
            updateRepositoryMetadata( localRepository, remoteRepository );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Error updating group repository metadata", e );
        }
    }

    protected abstract void updateRepositoryMetadata( ArtifactRepository localRepository,
                                                      ArtifactRepository remoteRepository )
        throws IOException;
}
