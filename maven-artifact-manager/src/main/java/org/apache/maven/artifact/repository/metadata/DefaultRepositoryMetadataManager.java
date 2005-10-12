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
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    public void resolve( RepositoryMetadata metadata, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        boolean alreadyResolved = alreadyResolved( metadata );
        if ( !alreadyResolved )
        {
            for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) i.next();

                ArtifactRepositoryPolicy policy = metadata.isSnapshot() ? repository.getSnapshots()
                    : repository.getReleases();

                if ( !policy.isEnabled() )
                {
                    getLogger().debug( "Skipping disabled repository " + repository.getId() );
                }
                else if ( repository.isBlacklisted() )
                {
                    getLogger().debug( "Skipping blacklisted repository " + repository.getId() );
                }
                else
                {
                    File file = new File( localRepository.getBasedir(),
                                          localRepository.pathOfLocalRepositoryMetadata( metadata, repository ) );

                    boolean checkForUpdates = policy.checkOutOfDate( new Date( file.lastModified() ) );

                    if ( checkForUpdates )
                    {
                        getLogger().info( metadata.getKey() + ": checking for updates from " + repository.getId() );

                        resolveAlways( metadata, repository, file, policy.getChecksumPolicy(), true );
                    }

                    // touch file so that this is not checked again until interval has passed
                    if ( file.exists() )
                    {
                        file.setLastModified( System.currentTimeMillis() );
                    }
                    else
                    {
                        // this ensures that files are not continuously checked when they don't exist remotely
                        metadata.storeInLocalRepository( localRepository, repository );
                    }
                }
            }
            cachedMetadata.add( metadata.getKey() );
        }
        // TODO: currently this is first wins, but really we should take the latest by comparing either the
        // snapshot timestamp, or some other timestamp later encoded into the metadata.
        // TODO: this needs to be repeated here so the merging doesn't interfere with the written metadata
        //  - we'd be much better having a pristine input, and an ongoing metadata for merging instead

        Map previousMetadata = new HashMap();
        ArtifactRepository selected = null;
        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            ArtifactRepositoryPolicy policy = metadata.isSnapshot() ? repository.getSnapshots()
                : repository.getReleases();

            if ( policy.isEnabled() && !repository.isBlacklisted() )
            {
                if ( loadMetadata( metadata, repository, localRepository, previousMetadata ) )
                {
                    metadata.setRepository( repository );
                    selected = repository;
                }
            }
        }
        if ( loadMetadata( metadata, localRepository, localRepository, previousMetadata ) )
        {
            metadata.setRepository( null );
            selected = localRepository;
        }

        // TODO: this could be a lot nicer... should really be in the snapshot transformation?
        if ( metadata.isSnapshot() )
        {
            Metadata prevMetadata = metadata.getMetadata();

            for ( Iterator i = previousMetadata.keySet().iterator(); i.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) i.next();
                Metadata m = (Metadata) previousMetadata.get( repository );
                if ( repository.equals( selected ) )
                {
                    if ( m.getVersioning() == null )
                    {
                        m.setVersioning( new Versioning() );
                    }

                    if ( m.getVersioning().getSnapshot() == null )
                    {
                        m.getVersioning().setSnapshot( new Snapshot() );
                    }

                    if ( !m.getVersioning().getSnapshot().isLocalCopy() )
                    {
                        m.getVersioning().getSnapshot().setLocalCopy( true );
                        metadata.setMetadata( m );
                        metadata.storeInLocalRepository( localRepository, repository );
                    }
                }
                else
                {
                    if ( m.getVersioning() != null && m.getVersioning().getSnapshot() != null &&
                        m.getVersioning().getSnapshot().isLocalCopy() )
                    {
                        m.getVersioning().getSnapshot().setLocalCopy( false );
                        metadata.setMetadata( m );
                        metadata.storeInLocalRepository( localRepository, repository );
                    }
                }
            }

            metadata.setMetadata( prevMetadata );
        }
    }

    private boolean loadMetadata( RepositoryMetadata repoMetadata, ArtifactRepository remoteRepository,
                                  ArtifactRepository localRepository, Map previousMetadata )
        throws ArtifactMetadataRetrievalException
    {
        boolean setRepository = false;

        File metadataFile = new File( localRepository.getBasedir(),
                                      localRepository.pathOfLocalRepositoryMetadata( repoMetadata, remoteRepository ) );

        if ( metadataFile.exists() )
        {
            Metadata metadata = readMetadata( metadataFile );

            if ( repoMetadata.isSnapshot() && previousMetadata != null )
            {
                previousMetadata.put( remoteRepository, metadata );
            }

            if ( repoMetadata.getMetadata() != null )
            {
                setRepository = repoMetadata.getMetadata().merge( metadata );
            }
            else
            {
                repoMetadata.setMetadata( metadata );
                setRepository = true;
            }
        }
        return setRepository;
    }

    /**
     * @todo share with DefaultPluginMappingManager.
     */
    protected static Metadata readMetadata( File mappingFile )
        throws ArtifactMetadataRetrievalException
    {
        Metadata result;

        Reader fileReader = null;
        try
        {
            fileReader = new FileReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot read version information from: " + mappingFile, e );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot read version information from: " + mappingFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot parse version information from: " + mappingFile, e );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
        return result;
    }

    public void resolveAlways( RepositoryMetadata metadata, ArtifactRepository localRepository,
                               ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( !wagonManager.isOnline() )
        {
            // metadata is required for deployment, can't be offline
            throw new ArtifactMetadataRetrievalException(
                "System is offline. Cannot resolve required metadata:\n" + metadata.extendedToString() );
        }

        File file = new File( localRepository.getBasedir(),
                              localRepository.pathOfLocalRepositoryMetadata( metadata, remoteRepository ) );

        resolveAlways( metadata, remoteRepository, file, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN, false );

        if ( file.exists() )
        {
            Metadata prevMetadata = readMetadata( file );
            metadata.setMetadata( prevMetadata );
        }
    }

    private void resolveAlways( ArtifactMetadata metadata, ArtifactRepository repository, File file,
                                String checksumPolicy, boolean allowBlacklisting )
        throws ArtifactMetadataRetrievalException
    {
        if ( !wagonManager.isOnline() )
        {
            if ( allowBlacklisting )
            {
                getLogger().debug(
                    "System is offline. Cannot resolve metadata:\n" + metadata.extendedToString() + "\n\n" );
                return;
            }
            else
            {
                // metadata is required for deployment, can't be offline
                throw new ArtifactMetadataRetrievalException(
                    "System is offline. Cannot resolve required metadata:\n" + metadata.extendedToString() );
            }
        }

        try
        {
            wagonManager.getArtifactMetadata( metadata, repository, file, checksumPolicy );
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().debug( metadata + " could not be found on repository: " + repository.getId() );

            // delete the local copy so the old details aren't used.
            if ( file.exists() )
            {
                file.delete();
            }
        }
        catch ( TransferFailedException e )
        {
            getLogger().warn( metadata + " could not be found on repository: " + repository.getId() +
                " due to an error: " + e.getMessage() );
            getLogger().info( "Repository '" + repository.getId() + "' will be blacklisted" );
            getLogger().debug( "Exception", e );
            repository.setBlacklisted( allowBlacklisting );
        }
    }

    private boolean alreadyResolved( ArtifactMetadata metadata )
    {
        return cachedMetadata.contains( metadata.getKey() );
    }

    public void deploy( ArtifactMetadata metadata, ArtifactRepository localRepository,
                        ArtifactRepository deploymentRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( !wagonManager.isOnline() )
        {
            // deployment shouldn't silently fail when offline
            throw new ArtifactMetadataRetrievalException(
                "System is offline. Cannot deploy metadata:\n" + metadata.extendedToString() );
        }

        getLogger().info( "Retrieving previous metadata from " + deploymentRepository.getId() );

        File file = new File( localRepository.getBasedir(),
                              localRepository.pathOfLocalRepositoryMetadata( metadata, deploymentRepository ) );

        resolveAlways( metadata, deploymentRepository, file, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN, false );

        metadata.storeInLocalRepository( localRepository, deploymentRepository );

        try
        {
            wagonManager.putArtifactMetadata( file, metadata, deploymentRepository );
        }
        catch ( TransferFailedException e )
        {
            // TODO: wrong exception
            throw new ArtifactMetadataRetrievalException( "Unable to retrieve metadata", e );
        }
    }

    public void install( ArtifactMetadata metadata, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        metadata.storeInLocalRepository( localRepository, localRepository );
    }
}
