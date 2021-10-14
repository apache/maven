package org.apache.maven.artifact.repository.metadata;

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

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.repository.legacy.UpdateCheckManager;
import org.apache.maven.repository.legacy.WagonManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jason van Zyl
 */
@Component( role = RepositoryMetadataManager.class )
public class DefaultRepositoryMetadataManager
    extends AbstractLogEnabled
    implements RepositoryMetadataManager
{
    @Requirement
    private WagonManager wagonManager;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    public void resolve( final RepositoryMetadata metadata, final List<ArtifactRepository> remoteRepositories,
                         final ArtifactRepository localRepository )
        throws RepositoryMetadataResolutionException
    {
        final RepositoryRequest request = new DefaultRepositoryRequest();
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        resolve( metadata, request );
    }

    public void resolve( final RepositoryMetadata metadata, final RepositoryRequest request )
        throws RepositoryMetadataResolutionException
    {
        final ArtifactRepository localRepo = request.getLocalRepository();
        final List<ArtifactRepository> remoteRepositories = request.getRemoteRepositories();

        if ( !request.isOffline() )
        {
            Date localCopyLastModified = null;
            if ( metadata.getBaseVersion() != null )
            {
                localCopyLastModified = getLocalCopyLastModified( localRepo, metadata );
            }

            for ( final ArtifactRepository repository : remoteRepositories )
            {
                final ArtifactRepositoryPolicy policy = metadata.getPolicy( repository );

                final File file =
                    new File( localRepo.getBasedir(), localRepo.pathOfLocalRepositoryMetadata( metadata, repository ) );
                final boolean update;

                if ( !policy.isEnabled() )
                {
                    update = false;

                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Skipping update check for " + metadata.getKey() + " (" + file
                                               + ") from disabled repository " + repository.getId() + " ("
                                               + repository.getUrl() + ")" );
                    }
                }
                else if ( request.isForceUpdate() )
                {
                    update = true;
                }
                else if ( localCopyLastModified != null && !policy.checkOutOfDate( localCopyLastModified ) )
                {
                    update = false;

                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug(
                            "Skipping update check for " + metadata.getKey() + " (" + file + ") from repository "
                                + repository.getId() + " (" + repository.getUrl() + ") in favor of local copy" );
                    }
                }
                else
                {
                    update = updateCheckManager.isUpdateRequired( metadata, repository, file );
                }

                if ( update )
                {
                    getLogger().info( metadata.getKey() + ": checking for updates from " + repository.getId() );
                    try
                    {
                        wagonManager.getArtifactMetadata( metadata, repository, file, policy.getChecksumPolicy() );
                    }
                    catch ( final ResourceDoesNotExistException e )
                    {
                        getLogger().debug( metadata + " could not be found on repository: " + repository.getId() );

                        // delete the local copy so the old details aren't used.
                        if ( file.exists() )
                        {
                            if ( !file.delete() )
                            {
                                // sleep for 10ms just in case this is windows holding a file lock
                                try
                                {
                                    Thread.sleep( 10 );
                                }
                                catch ( final InterruptedException ie )
                                {
                                    // ignore
                                }
                                file.delete(); // if this fails, forget about it
                            }
                        }
                    }
                    catch ( final TransferFailedException e )
                    {
                        getLogger().warn( metadata + " could not be retrieved from repository: " + repository.getId()
                                              + " due to an error: " + e.getMessage() );
                        getLogger().debug( "Exception", e );
                    }
                    finally
                    {
                        updateCheckManager.touch( metadata, repository, file );
                    }
                }

                // TODO should this be inside the above check?
                // touch file so that this is not checked again until interval has passed
                if ( file.exists() )
                {
                    file.setLastModified( System.currentTimeMillis() );
                }
            }
        }

        try
        {
            mergeMetadata( metadata, remoteRepositories, localRepo );
        }
        catch ( final RepositoryMetadataStoreException e )
        {
            throw new RepositoryMetadataResolutionException(
                "Unable to store local copy of metadata: " + e.getMessage(), e );
        }
    }

    private Date getLocalCopyLastModified( final ArtifactRepository localRepository, final RepositoryMetadata metadata )
    {
        final String metadataPath = localRepository.pathOfLocalRepositoryMetadata( metadata, localRepository );
        final File metadataFile = new File( localRepository.getBasedir(), metadataPath );
        return metadataFile.isFile() ? new Date( metadataFile.lastModified() ) : null;
    }

    private void mergeMetadata( final RepositoryMetadata metadata, final List<ArtifactRepository> remoteRepositories,
                                final ArtifactRepository localRepository )
        throws RepositoryMetadataStoreException
    {
        // TODO currently this is first wins, but really we should take the latest by comparing either the
        // snapshot timestamp, or some other timestamp later encoded into the metadata.
        // TODO this needs to be repeated here so the merging doesn't interfere with the written metadata
        //  - we'd be much better having a pristine input, and an ongoing metadata for merging instead

        final Map<ArtifactRepository, Metadata> previousMetadata = new HashMap<>();
        ArtifactRepository selected = null;
        for ( final ArtifactRepository repository : remoteRepositories )
        {
            final ArtifactRepositoryPolicy policy = metadata.getPolicy( repository );

            if ( policy.isEnabled() && loadMetadata( metadata, repository, localRepository, previousMetadata ) )
            {
                metadata.setRepository( repository );
                selected = repository;
            }
        }
        if ( loadMetadata( metadata, localRepository, localRepository, previousMetadata ) )
        {
            metadata.setRepository( null );
            selected = localRepository;
        }

        updateSnapshotMetadata( metadata, previousMetadata, selected, localRepository );
    }

    private void updateSnapshotMetadata( final RepositoryMetadata metadata,
                                         final Map<ArtifactRepository, Metadata> previousMetadata,
                                         final ArtifactRepository selected, final ArtifactRepository localRepository )
        throws RepositoryMetadataStoreException
    {
        // TODO this could be a lot nicer... should really be in the snapshot transformation?
        if ( metadata.isSnapshot() )
        {
            final Metadata prevMetadata = metadata.getMetadata();

            for ( final ArtifactRepository repository : previousMetadata.keySet() )
            {
                final Metadata m = previousMetadata.get( repository );
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
                }
                else
                {
                    if ( ( m.getVersioning() != null ) && ( m.getVersioning().getSnapshot() != null )
                        && m.getVersioning().getSnapshot().isLocalCopy() )
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

    private boolean loadMetadata( final RepositoryMetadata repoMetadata, final ArtifactRepository remoteRepository,
                                  final ArtifactRepository localRepository,
                                  final Map<ArtifactRepository, Metadata> previousMetadata )
    {
        boolean setRepository = false;

        final File metadataFile = new File( localRepository.getBasedir(),
                                      localRepository.pathOfLocalRepositoryMetadata( repoMetadata, remoteRepository ) );

        if ( metadataFile.exists() )
        {
            final Metadata metadata;

            try
            {
                metadata = readMetadata( metadataFile );
            }
            catch ( final RepositoryMetadataReadException e )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().warn( e.getMessage(), e );
                }
                else
                {
                    getLogger().warn( e.getMessage() );
                }
                return setRepository;
            }

            if ( repoMetadata.isSnapshot() && ( previousMetadata != null ) )
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
     * TODO share with DefaultPluginMappingManager.
     */
    protected Metadata readMetadata( final File mappingFile )
        throws RepositoryMetadataReadException
    {
        final Metadata result;

        try ( final Reader reader = ReaderFactory.newXmlReader( mappingFile ) )
        {
            final MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( reader, false );
        }
        catch ( final FileNotFoundException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "'", e );
        }
        catch ( final IOException | XmlPullParserException e )
        {
            throw new RepositoryMetadataReadException(
                "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        return result;
    }

    /**
     * Ensures the last updated timestamp of the specified metadata does not refer to the future and fixes the local
     * metadata if necessary to allow proper merging/updating of metadata during deployment.
     */
    private void fixTimestamp( final File metadataFile, final Metadata metadata, final Metadata reference )
    {
        boolean changed = false;

        if ( metadata != null && reference != null )
        {
            final Versioning versioning = metadata.getVersioning();
            final Versioning versioningRef = reference.getVersioning();
            if ( versioning != null && versioningRef != null )
            {
                final String lastUpdated = versioning.getLastUpdated();
                final String now = versioningRef.getLastUpdated();
                if ( lastUpdated != null && now != null && now.compareTo( lastUpdated ) < 0 )
                {
                    getLogger().warn(
                        "The last updated timestamp in " + metadataFile + " refers to the future (now = " + now
                            + ", lastUpdated = " + lastUpdated + "). Please verify that the clocks of all"
                            + " deploying machines are reasonably synchronized." );
                    versioning.setLastUpdated( now );
                    changed = true;
                }
            }
        }

        if ( changed )
        {
            getLogger().debug( "Repairing metadata in " + metadataFile );

            try ( final Writer writer = WriterFactory.newXmlWriter( metadataFile ) )
            {
                new MetadataXpp3Writer().write( writer, metadata );
            }
            catch ( final IOException e )
            {
                final String msg = "Could not write fixed metadata to " + metadataFile + ": " + e.getMessage();
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().warn( msg, e );
                }
                else
                {
                    getLogger().warn( msg );
                }
            }
        }
    }

    public void resolveAlways( final RepositoryMetadata metadata, final ArtifactRepository localRepository,
                               final ArtifactRepository remoteRepository )
        throws RepositoryMetadataResolutionException
    {
        final File file;
        try
        {
            file = getArtifactMetadataFromDeploymentRepository( metadata, localRepository, remoteRepository );
        }
        catch ( final TransferFailedException e )
        {
            throw new RepositoryMetadataResolutionException(
                metadata + " could not be retrieved from repository: " + remoteRepository.getId() + " due to an error: "
                    + e.getMessage(), e );
        }

        try
        {
            if ( file.exists() )
            {
                final Metadata prevMetadata = readMetadata( file );
                metadata.setMetadata( prevMetadata );
            }
        }
        catch ( final RepositoryMetadataReadException e )
        {
            throw new RepositoryMetadataResolutionException( e.getMessage(), e );
        }
    }

    private File getArtifactMetadataFromDeploymentRepository( final ArtifactMetadata metadata, final ArtifactRepository localRepo,
                                                              final ArtifactRepository remoteRepository )
        throws TransferFailedException
    {
        final File file =
            new File( localRepo.getBasedir(), localRepo.pathOfLocalRepositoryMetadata( metadata, remoteRepository ) );

        try
        {
            wagonManager.getArtifactMetadataFromDeploymentRepository( metadata, remoteRepository, file,
                                                                      ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
        }
        catch ( final ResourceDoesNotExistException e )
        {
            getLogger().info(
                metadata + " could not be found on repository: " + remoteRepository.getId() + ", so will be created" );

            // delete the local copy so the old details aren't used.
            if ( file.exists() )
            {
                if ( !file.delete() )
                {
                    // sleep for 10ms just in case this is windows holding a file lock
                    try
                    {
                        Thread.sleep( 10 );
                    }
                    catch ( final InterruptedException ie )
                    {
                        // ignore
                    }
                    file.delete(); // if this fails, forget about it
                }
            }
        }
        finally
        {
            if ( metadata instanceof RepositoryMetadata )
            {
                updateCheckManager.touch( (RepositoryMetadata) metadata, remoteRepository, file );
            }
        }
        return file;
    }

    public void deploy( final ArtifactMetadata metadata, final ArtifactRepository localRepository,
                        final ArtifactRepository deploymentRepository )
        throws RepositoryMetadataDeploymentException
    {
        final File file;
        if ( metadata instanceof RepositoryMetadata )
        {
            getLogger().info( "Retrieving previous metadata from " + deploymentRepository.getId() );
            try
            {
                file = getArtifactMetadataFromDeploymentRepository( metadata, localRepository, deploymentRepository );
            }
            catch ( final TransferFailedException e )
            {
                throw new RepositoryMetadataDeploymentException(
                    metadata + " could not be retrieved from repository: " + deploymentRepository.getId()
                        + " due to an error: " + e.getMessage(), e );
            }

            if ( file.isFile() )
            {
                try
                {
                    fixTimestamp( file, readMetadata( file ), ( (RepositoryMetadata) metadata ).getMetadata() );
                }
                catch ( final RepositoryMetadataReadException e )
                {
                    // will be reported via storeInlocalRepository
                }
            }
        }
        else
        {
            // It's a POM - we don't need to retrieve it first
            file = new File( localRepository.getBasedir(),
                             localRepository.pathOfLocalRepositoryMetadata( metadata, deploymentRepository ) );
        }

        try
        {
            metadata.storeInLocalRepository( localRepository, deploymentRepository );
        }
        catch ( final RepositoryMetadataStoreException e )
        {
            throw new RepositoryMetadataDeploymentException( "Error installing metadata: " + e.getMessage(), e );
        }

        try
        {
            wagonManager.putArtifactMetadata( file, metadata, deploymentRepository );
        }
        catch ( final TransferFailedException e )
        {
            throw new RepositoryMetadataDeploymentException( "Error while deploying metadata: " + e.getMessage(), e );
        }
    }

    public void install( final ArtifactMetadata metadata, final ArtifactRepository localRepository )
        throws RepositoryMetadataInstallationException
    {
        try
        {
            metadata.storeInLocalRepository( localRepository, localRepository );
        }
        catch ( final RepositoryMetadataStoreException e )
        {
            throw new RepositoryMetadataInstallationException( "Error installing metadata: " + e.getMessage(), e );
        }
    }

}
