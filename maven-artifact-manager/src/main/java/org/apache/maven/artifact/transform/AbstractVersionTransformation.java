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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.AbstractVersionArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.VersionArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Describes a version transformation during artifact resolution.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractVersionTransformation
    extends AbstractLogEnabled
    implements ArtifactTransformation
{
    protected WagonManager wagonManager;

    /**
     * @todo very primitve. Probably we can cache artifacts themselves in a central location, as well as reset the flag over time in a long running process.
     */
    private static Set resolvedArtifactCache = new HashSet();

    protected String resolveVersion( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        ArtifactMetadata localMetadata = resolveMetadata( artifact, localRepository, remoteRepositories );

        String version;

        if ( localMetadata == null )
        {
            version = artifact.getVersion();
        }
        else
        {
            VersionArtifactMetadata versionMetadata = (VersionArtifactMetadata) localMetadata;
            version = versionMetadata.constructVersion();
        }

        // TODO: also do this logging for other metadata?
        if ( getLogger().isDebugEnabled() )
        {
            if ( version != null && !version.equals( artifact.getBaseVersion() ) )
            {
                String message = artifact.getArtifactId() + ": resolved to version " + version;
                if ( artifact.getRepository() != null )
                {
                    message += " from repository " + artifact.getRepository().getId();
                }
                else
                {
                    message += " from local repository";
                }
                getLogger().debug( message );
            }
        }
        return version;
    }

    /**
     * @param artifact
     * @param localRepository
     * @param remoteRepositories
     * @return
     * @throws ArtifactMetadataRetrievalException
     * @todo share with DefaultRepositoryMetadataManager
     */
    private ArtifactMetadata resolveMetadata( Artifact artifact, ArtifactRepository localRepository,
                                              List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        VersionArtifactMetadata localMetadata;
        try
        {
            localMetadata = readFromLocalRepository( artifact, localRepository );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
        }

        boolean alreadyResolved = alreadyResolved( artifact );
        if ( !alreadyResolved )
        {
            boolean checkedUpdates = false;
            for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) i.next();

                ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots()
                    : repository.getReleases();

                if ( policy == null || !policy.isEnabled() )
                {
                    getLogger().debug( "Skipping disabled repository " + repository.getId() );
                }
                else
                {
                    // TODO: should be able to calculate this less often
                    boolean checkForUpdates = policy.checkOutOfDate( localMetadata.getLastModified() );

                    if ( checkForUpdates )
                    {
                        checkedUpdates = true;

                        getLogger().info(
                            artifact.getArtifactId() + ": checking for updates from " + repository.getId() );

                        VersionArtifactMetadata remoteMetadata;

                        try
                        {
                            remoteMetadata = retrieveFromRemoteRepository( artifact, repository, localMetadata,
                                                                           policy.getChecksumPolicy() );
                        }
                        catch ( ResourceDoesNotExistException e )
                        {
                            getLogger().debug( "Artifact version metadata for: " + artifact.getId() +
                                " could not be found on repository: " + repository.getId(), e );

                            continue;
                        }

                        int difference = remoteMetadata.compareTo( localMetadata );
                        if ( difference > 0 )
                        {
                            // remote is newer
                            artifact.setRepository( repository );

                            localMetadata = remoteMetadata;
                        }
                    }
                }
            }

            // touch the file if it was checked for updates, but don't create it if it doesn't exist remotely to avoid
            // storing SNAPSHOT as the actual version which doesn't exist remotely.
            if ( checkedUpdates && localMetadata.getLastModified().getTime() > 0 )
            {
                localMetadata.storeInLocalRepository( localRepository );
            }

            resolvedArtifactCache.add( getCacheKey( artifact ) );
        }

        // TODO: if the POM and JAR are inconsistent, this might mean that different version of each are used
        if ( artifact.getFile().exists() && !localMetadata.newerThanFile( artifact.getFile() ) )
        {
            if ( getLogger().isDebugEnabled() && !alreadyResolved )
            {
                // Locally installed file is newer, don't use the resolved version
                getLogger().debug( artifact.getArtifactId() + ": using locally installed snapshot" );
            }
            localMetadata = null;
        }
        return localMetadata;
    }

    protected VersionArtifactMetadata retrieveFromRemoteRepository( Artifact artifact,
                                                                    ArtifactRepository remoteRepository,
                                                                    ArtifactMetadata localMetadata,
                                                                    String checksumPolicy )
        throws ArtifactMetadataRetrievalException, ResourceDoesNotExistException
    {
        AbstractVersionArtifactMetadata metadata = createMetadata( artifact );

        metadata.retrieveFromRemoteRepository( remoteRepository, wagonManager, checksumPolicy );

        return metadata;
    }

    protected abstract AbstractVersionArtifactMetadata createMetadata( Artifact artifact );

    private VersionArtifactMetadata readFromLocalRepository( Artifact artifact, ArtifactRepository localRepository )
        throws IOException
    {
        // TODO: we could cache the results of this, perhaps inside the artifact repository?
        AbstractVersionArtifactMetadata metadata = createMetadata( artifact );
        metadata.readFromLocalRepository( localRepository );
        return metadata;
    }

    private boolean alreadyResolved( Artifact artifact )
    {
        return resolvedArtifactCache.contains( getCacheKey( artifact ) );
    }

    private static String getCacheKey( Artifact artifact )
    {
        // No type - one per POM
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
    }
}
