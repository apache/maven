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
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.LegacyArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
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
 * @todo try and refactor to remove abstract methods - not particular happy about current design
 */
public abstract class AbstractVersionTransformation
    extends AbstractLogEnabled
    implements ArtifactTransformation
{
    protected RepositoryMetadataManager repositoryMetadataManager;

    protected WagonManager wagonManager;

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private static Set resolvedArtifactCache = new HashSet();

    protected String resolveVersion( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        RepositoryMetadata metadata;
        if ( !artifact.isSnapshot() || Artifact.LATEST_VERSION.equals( artifact.getBaseVersion() ) )
        {
            metadata = new ArtifactRepositoryMetadata( artifact );
        }
        else
        {
            metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        }

        repositoryMetadataManager.resolve( metadata, remoteRepositories, localRepository );

        Metadata repoMetadata = metadata.getMetadata();
        String version = null;
        if ( repoMetadata != null && repoMetadata.getVersioning() != null )
        {
            version = constructVersion( repoMetadata.getVersioning(), artifact.getBaseVersion() );
        }

        if ( version == null )
        {
            version = resolveLegacyVersion( artifact, localRepository, remoteRepositories );
            if ( version == null )
            {
                version = artifact.getBaseVersion();
            }
        }

        // TODO: also do this logging for other metadata?
        // TODO: figure out way to avoid duplicated message
        if ( getLogger().isDebugEnabled() )
        {
            if ( !version.equals( artifact.getBaseVersion() ) )
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
            else
            {
                // Locally installed file is newer, don't use the resolved version
                getLogger().debug( artifact.getArtifactId() + ": using locally installed snapshot" );
            }
        }
        return version;
    }

    protected abstract String constructVersion( Versioning versioning, String baseVersion );

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private String resolveLegacyVersion( Artifact artifact, ArtifactRepository localRepository,
                                         List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        LegacyArtifactMetadata localMetadata = createLegacyMetadata( artifact );
        File f = new File( localRepository.getBasedir(),
                           localRepository.pathOfLocalRepositoryMetadata( localMetadata, null ) );
        if ( f.exists() )
        {
            try
            {
                localMetadata.readFromFile( f );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
            }
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

                if ( !policy.isEnabled() )
                {
                    getLogger().debug( "Legacy metadata: " + artifact.getId() + ": Skipping disabled repository " +
                        repository.getId() + " (" + repository.getUrl() + ")" );
                }
                else
                {
                    boolean checkForUpdates = policy.checkOutOfDate( localMetadata.getLastModified() );

                    if ( checkForUpdates )
                    {
                        checkedUpdates = true;

                        LegacyArtifactMetadata remoteMetadata;

                        try
                        {
                            remoteMetadata = createLegacyMetadata( artifact );

                            remoteMetadata.retrieveFromRemoteRepository( repository, wagonManager,
                                                                         policy.getChecksumPolicy() );

                            getLogger().warn( "Using old-style versioning metadata from remote repo for " + artifact );

                            int difference = remoteMetadata.compareTo( localMetadata );
                            if ( difference > 0 )
                            {
                                // remote is newer
                                artifact.setRepository( repository );

                                localMetadata = remoteMetadata;
                            }
                        }
                        catch ( ResourceDoesNotExistException e )
                        {
                            getLogger().debug( "Legacy metadata for: " + artifact.getId() +
                                " could not be found on repository: " + repository.getId(), e );
                        }
                        catch ( ArtifactMetadataRetrievalException e )
                        {
                            getLogger().warn( "Legacy metadata for: " + artifact.getId() +
                                " could not be found on repository: " + repository.getId(), e );
                        }
                    }
                    else
                    {
                        getLogger().debug( "resolveMetaData: " + artifact.getId() + ": NOT checking for updates from " +
                            repository.getId() + " (" + repository.getUrl() + ")" );
                    }
                }
            }

            // touch the file if it was checked for updates, but don't create it if it did't exist to avoid
            // storing SNAPSHOT as the actual version which doesn't exist remotely.
            if ( checkedUpdates && localMetadata.getLastModified().getTime() > 0 )
            {
                localMetadata.storeInLocalRepository( localRepository );
            }

            resolvedArtifactCache.add( getCacheKey( artifact ) );
        }

        if ( artifact.getFile().exists() && !localMetadata.newerThanFile( artifact.getFile() ) )
        {
            if ( getLogger().isDebugEnabled() && !alreadyResolved )
            {
                // Locally installed file is newer, don't use the resolved version
                getLogger().debug( artifact.getArtifactId() + ": using locally installed snapshot" );
            }
            localMetadata = null;
        }
        return localMetadata != null ? localMetadata.constructVersion() : null;
    }

    protected abstract LegacyArtifactMetadata createLegacyMetadata( Artifact artifact );

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private boolean alreadyResolved( Artifact artifact )
    {
        return resolvedArtifactCache.contains( getCacheKey( artifact ) );
    }

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private static String getCacheKey( Artifact artifact )
    {
        // No type - one per POM
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
    }
}
