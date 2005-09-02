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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultRepositoryMetadataManager
    extends AbstractLogEnabled
    implements RepositoryMetadataManager
{
    // component requirement
    private WagonManager wagonManager;

    /**
     * @todo very primitive. Probably we can cache artifacts themselves in a central location, as well as reset the flag over time in a long running process.
     */
    private Set cachedMetadata = new HashSet();

    public void resolve( ArtifactMetadata metadata, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        boolean alreadyResolved = alreadyResolved( metadata );
        if ( !alreadyResolved )
        {
            File file = new File( localRepository.getBasedir(), localRepository.pathOfArtifactMetadata( metadata ) );

            boolean checkedUpdates = false;
            for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) i.next();

                ArtifactRepositoryPolicy policy = metadata.isSnapshot() ? repository.getSnapshots()
                    : repository.getReleases();

                if ( policy == null || !policy.isEnabled() )
                {
                    getLogger().debug( "Skipping disabled repository " + repository.getId() );
                }
                else
                {
                    // TODO: should be able to calculate this less often
                    boolean checkForUpdates = policy.checkOutOfDate( new Date( file.lastModified() ) );

                    if ( checkForUpdates )
                    {
                        checkedUpdates = true;

                        getLogger().info( metadata.getKey() + ": checking for updates from " + repository.getId() );

                        try
                        {
                            wagonManager.getArtifactMetadata( metadata, repository, file, policy.getChecksumPolicy() );
                            // TODO: ???
//                            metadata.setRepository( repository );
                        }
                        catch ( ResourceDoesNotExistException e )
                        {
                            getLogger().info( "Repository metadata " + metadata +
                                " could not be found on repository: " + repository.getId(), e );
                        }
                        catch ( TransferFailedException e )
                        {
                            throw new ArtifactMetadataRetrievalException( "Unable to retrieve metadata", e );
                        }
                    }
                }
            }

            // touch the file if it was checked for updates, but don't create it if it doesn't exist to avoid
            // storing SNAPSHOT as the actual version which doesn't exist remotely.
            if ( checkedUpdates )
            {
                if ( file.exists() )
                {
                    file.setLastModified( System.currentTimeMillis() );
                }
            }

            cachedMetadata.add( metadata.getKey() );
        }
    }

    private boolean alreadyResolved( ArtifactMetadata metadata )
    {
        return cachedMetadata.contains( metadata.getKey() );
    }

}
