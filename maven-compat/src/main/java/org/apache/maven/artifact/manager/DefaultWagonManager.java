package org.apache.maven.artifact.manager;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

@Component(role = WagonManager.class)
public class DefaultWagonManager
    implements WagonManager
{
    private static final String[] CHECKSUM_IDS = { "md5", "sha1" };

    /** have to match the CHECKSUM_IDS */
    private static final String[] CHECKSUM_ALGORITHMS = { "MD5", "SHA-1" };

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    /** Map( String, XmlPlexusConfiguration ) with the repository id and the wagon configuration */
    private Map<String, XmlPlexusConfiguration> serverConfigurationMap = new HashMap<String, XmlPlexusConfiguration>();

    @Requirement(role = Wagon.class)
    private Map<String,Wagon> wagons;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    private String httpUserAgent = "Apache-Maven/3.0-alpha-1";

    private TransferListener downloadMonitor;

    public void setDownloadMonitor( TransferListener downloadMonitor )
    {
        this.downloadMonitor = downloadMonitor;
    }

    public Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException, WagonConfigurationException
    {
        String protocol = repository.getProtocol();
        
        if ( protocol == null )
        {
            throw new UnsupportedProtocolException( "The repository " + repository + " does not specify a protocol" );
        }

        Wagon wagon = getWagon( protocol );

        configureWagon( wagon, repository.getId(), protocol );

        return wagon;
    }

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        if ( protocol == null )
        {
            throw new UnsupportedProtocolException( "Unspecified protocol" );
        }

        String hint = protocol.toLowerCase( java.util.Locale.ENGLISH );
        Wagon wagon = (Wagon) wagons.get( hint );

        if ( wagon == null )
        {
            throw new UnsupportedProtocolException( "Cannot find wagon which supports the requested protocol: " + protocol );
        }

        return wagon;
    }

    public void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws TransferFailedException
    {
        putRemoteFile( deploymentRepository, source, deploymentRepository.pathOf( artifact ), downloadMonitor );
    }

    public void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository, TransferListener downloadMonitor )
        throws TransferFailedException
    {
        putRemoteFile( deploymentRepository, source, deploymentRepository.pathOf( artifact ), downloadMonitor );
    }

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        logger.info( "Uploading " + artifactMetadata );
        putRemoteFile( repository, source, repository.pathOfRemoteRepositoryMetadata( artifactMetadata ), null );
    }

    private void putRemoteFile( ArtifactRepository repository, File source, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException
    {
        String protocol = repository.getProtocol();

        Wagon wagon;
        try
        {
            wagon = getWagon( protocol );

            configureWagon( wagon, repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        Map<String, ChecksumObserver> checksums = new HashMap<String, ChecksumObserver>( 2 );

        Map<String, String> sums = new HashMap<String, String>( 2 );

        // TODO: configure these on the repository
        for ( int i = 0; i < CHECKSUM_IDS.length; i++ )
        {
            checksums.put( CHECKSUM_IDS[i], addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i] ) );
        }

        try
        {
            try
            {                
                wagon.connect( new Repository( repository.getId(), repository.getUrl() ) );

                wagon.put( source, remotePath );
            }
            finally
            {
                if ( downloadMonitor != null )
                {
                    wagon.removeTransferListener( downloadMonitor );
                }
            }

            // Pre-store the checksums as any future puts will overwrite them
            for ( String extension : checksums.keySet() )
            {
                ChecksumObserver observer = checksums.get( extension );
                sums.put( extension, observer.getActualChecksum() );
            }

            // We do this in here so we can checksum the artifact metadata too, otherwise it could be metadata itself
            for ( String extension : checksums.keySet() )
            {
                // TODO: shouldn't need a file intermediatary - improve wagon to take a stream
                File temp = File.createTempFile( "maven-artifact", null );
                temp.deleteOnExit();
                FileUtils.fileWrite( temp.getAbsolutePath(), "UTF-8", sums.get( extension ) );

                wagon.put( temp, remotePath + "." + extension );
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: " + e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: " + e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: " + e.getMessage(), e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new TransferFailedException( "Resource to deploy not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error creating temporary file for deployment: " + e.getMessage(), e );
        }
        finally
        {
            // Remove every checksum listener
            for ( String aCHECKSUM_IDS : CHECKSUM_IDS )
            {
                TransferListener checksumListener = checksums.get( aCHECKSUM_IDS );
                if ( checksumListener != null )
                {
                    wagon.removeTransferListener( checksumListener );
                }
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }
    }

    private ChecksumObserver addChecksumObserver( Wagon wagon, String algorithm )
        throws TransferFailedException
    {
        try
        {
            ChecksumObserver checksumObserver = new ChecksumObserver( algorithm );
            wagon.addTransferListener( checksumObserver );
            return checksumObserver;
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new TransferFailedException( "Unable to add checksum for unsupported algorithm " + algorithm, e );
        }
    }

    // NOTE: It is not possible that this method throws TransferFailedException under current conditions.
    // FIXME: Change the throws clause to reflect the fact that we're never throwing TransferFailedException
    public void getArtifact( Artifact artifact, ArtifactRepository remoteRepository, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getArtifact( artifact, remoteRepository, downloadMonitor, force );
    }

    public void getArtifact( Artifact artifact, ArtifactRepository remoteRepository )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getArtifact( artifact, remoteRepository, downloadMonitor, true );
    }

    public void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getArtifact( artifact, remoteRepositories, downloadMonitor, true );
    }

    public void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories, TransferListener downloadMonitor, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        for ( ArtifactRepository repository : remoteRepositories )
        {
            try
            {
                getArtifact( artifact, repository, downloadMonitor, force );

                if ( artifact.isResolved() )
                {
                    break;
                }
            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                logger.debug( "Unable to get resource '" + artifact.getId() + "' from repository " + repository.getId() + " (" + repository.getUrl() + ")", e );
            }
            catch ( TransferFailedException e )
            {
                logger.debug( "Unable to get resource '" + artifact.getId() + "' from repository " + repository.getId() + " (" + repository.getUrl() + ")", e );
            }
        }        
        
        // if it already exists locally we were just trying to force it - ignore the update
        if ( !artifact.getFile().exists() )
        {
            throw new ResourceDoesNotExistException( "Unable to download the artifact from any repository" );
        }
    }

    public void getArtifact( Artifact artifact, ArtifactRepository repository, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        getArtifact( artifact, repository, downloadMonitor, true );
    }

    //TODO: all of this needs to move into the repository system
    
    public void getArtifact( Artifact artifact, ArtifactRepository repository, TransferListener downloadMonitor, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOf( artifact );
        
        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();
                
        boolean updateCheckIsRequired = updateCheckManager.isUpdateRequired( artifact, repository );
        
        if ( !policy.isEnabled() )
        {            
            logger.debug( "Skipping disabled repository " + repository.getId() );
        }
        else if ( repository.isBlacklisted() )
        {
            logger.debug( "Skipping blacklisted repository " + repository.getId() );
        }
        // If the artifact is a snapshot, we need to determine whether it's time to check this repository for an update:
        // 1. If it's forced, then check
        // 2. If the updateInterval has been exceeded since the last check for this artifact on this repository, then check.        
        else if ( artifact.isSnapshot() && ( force || updateCheckIsRequired ) )
        {
            logger.debug( "Trying repository " + repository.getId() );

            try
            {
                getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );
            }
            finally
            {
                updateCheckManager.touch( artifact, repository );
            }

            logger.debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }

        // XXX: This is not really intended for the long term - unspecified POMs should be converted to failures
        //      meaning caching would be unnecessary. The code for this is here instead of the MavenMetadataSource
        //      to keep the logic related to update checks enclosed, and so to keep the rules reasonably consistent
        //      with release metadata
        else if ( "pom".equals( artifact.getType() ) && !artifact.getFile().exists() )
        {
            // if POM is not present locally, try and get it if it's forced, out of date, or has not been attempted yet  
            if ( force || updateCheckManager.isPomUpdateRequired( artifact, repository ) )
            {
                logger.debug( "Trying repository " + repository.getId() );

                try
                {
                    getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );
                }
                catch ( ResourceDoesNotExistException e )
                {
                    // cache the POM failure
                    updateCheckManager.touch( artifact, repository );

                    throw e;
                }

                logger.debug( "  Artifact resolved" );

                artifact.setResolved( true );
            }
            else
            {
                // cached failure - pass on the failure
                throw new ResourceDoesNotExistException( "Failure was cached in the local repository" );
            }
        }

        // If it's not a snapshot artifact, then we don't care what the force flag says. If it's on the local
        // system, it's resolved. Releases are presumed to be immutable, so release artifacts are not ever updated.
        // NOTE: This is NOT the case for metadata files on relese-only repositories. This metadata may contain information
        // about successive releases, so it should be checked using the same updateInterval/force characteristics as snapshot
        // artifacts, above.

        // don't write touch-file for release artifacts.
        else if ( !artifact.isSnapshot() )
        {
            logger.debug( "Trying repository " + repository.getId() );

            getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );

            logger.debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }
    }

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository repository, File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    public void getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository repository, File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    private void getRemoteFile( ArtifactRepository repository, File destination, String remotePath, TransferListener downloadMonitor, String checksumPolicy, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String protocol = repository.getProtocol();
        
        Wagon wagon;

        try
        {
            wagon = getWagon( protocol );

            configureWagon( wagon, repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }
        
        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        File temp = new File( destination + ".tmp" );

        temp.deleteOnExit();

        boolean downloaded = false;

        try
        {            
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ) );

            boolean firstRun = true;
            boolean retry = true;

            // this will run at most twice. The first time, the firstRun flag is turned off, and if the retry flag
            // is set on the first run, it will be turned off and not re-set on the second try. This is because the
            // only way the retry flag can be set is if ( firstRun == true ).
            while ( firstRun || retry )
            {
                ChecksumObserver md5ChecksumObserver = null;
                ChecksumObserver sha1ChecksumObserver = null;
                try
                {
                    // TODO: configure on repository
                    int i = 0;

                    md5ChecksumObserver = addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i++] );
                    sha1ChecksumObserver = addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i++] );

                    // reset the retry flag.
                    retry = false;

                    // This should take care of creating destination directory now on
                    if ( destination.exists() && !force )
                    {
                        try
                        {
                            downloaded = wagon.getIfNewer( remotePath, temp, destination.lastModified() );

                            if ( !downloaded )
                            {
                                // prevent additional checks of this artifact until it expires again
                                destination.setLastModified( System.currentTimeMillis() );
                            }
                        }
                        catch ( UnsupportedOperationException e )
                        {
                            // older wagons throw this. Just get() instead
                            wagon.get( remotePath, temp );

                            downloaded = true;
                        }
                    }
                    else
                    {
                        wagon.get( remotePath, temp );
                        downloaded = true;
                    }
                }
                finally
                {
                    wagon.removeTransferListener( md5ChecksumObserver );
                    wagon.removeTransferListener( sha1ChecksumObserver );
                }

                if ( downloaded )
                {
                    // keep the checksum files from showing up on the download monitor...
                    if ( downloadMonitor != null )
                    {
                        wagon.removeTransferListener( downloadMonitor );
                    }

                    // try to verify the SHA-1 checksum for this file.
                    try
                    {
                        verifyChecksum( sha1ChecksumObserver, destination, temp, remotePath, ".sha1", wagon );
                    }
                    catch ( ChecksumFailedException e )
                    {
                        // if we catch a ChecksumFailedException, it means the transfer/read succeeded, but the checksum
                        // doesn't match. This could be a problem with the server (ibiblio HTTP-200 error page), so we'll
                        // try this up to two times. On the second try, we'll handle it as a bona-fide error, based on the
                        // repository's checksum checking policy.
                        if ( firstRun )
                        {
                            logger.warn( "*** CHECKSUM FAILED - " + e.getMessage() + " - RETRYING" );
                            retry = true;
                        }
                        else
                        {
                            handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                        }
                    }
                    catch ( ResourceDoesNotExistException sha1TryException )
                    {
                        logger.debug( "SHA1 not found, trying MD5", sha1TryException );

                        // if this IS NOT a ChecksumFailedException, it was a problem with transfer/read of the checksum
                        // file...we'll try again with the MD5 checksum.
                        try
                        {
                            verifyChecksum( md5ChecksumObserver, destination, temp, remotePath, ".md5", wagon );
                        }
                        catch ( ChecksumFailedException e )
                        {
                            // if we also fail to verify based on the MD5 checksum, and the checksum transfer/read
                            // succeeded, then we need to determine whether to retry or handle it as a failure.
                            if ( firstRun )
                            {
                                retry = true;
                            }
                            else
                            {
                                handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                            }
                        }
                        catch ( ResourceDoesNotExistException md5TryException )
                        {
                            // this was a failed transfer, and we don't want to retry.
                            handleChecksumFailure( checksumPolicy, "Error retrieving checksum file for " + remotePath, md5TryException );
                        }
                    }

                    // reinstate the download monitor...
                    if ( downloadMonitor != null )
                    {
                        wagon.addTransferListener( downloadMonitor );
                    }
                }

                // unset the firstRun flag, so we don't get caught in an infinite loop...
                firstRun = false;
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: " + e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: " + e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: " + e.getMessage(), e );
        }
        finally
        {
            // Remove remaining TransferListener instances (checksum handlers removed in above finally clause)
            if ( downloadMonitor != null )
            {
                wagon.removeTransferListener( downloadMonitor );
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }

        if ( downloaded )
        {
            if ( !temp.exists() )
            {
                throw new ResourceDoesNotExistException( "Downloaded file does not exist: " + temp );
            }

            // The temporary file is named destination + ".tmp" and is done this way to ensure
            // that the temporary file is in the same file system as the destination because the
            // File.renameTo operation doesn't really work across file systems.
            // So we will attempt to do a File.renameTo for efficiency and atomicity, if this fails
            // then we will use a brute force copy and delete the temporary file.

            if ( !temp.renameTo( destination ) )
            {
                try
                {
                    FileUtils.copyFile( temp, destination );

                    temp.delete();
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( "Error copying temporary file to the final destination: " + e.getMessage(), e );
                }
            }
        }
    }

    private void handleChecksumFailure( String checksumPolicy, String message, Throwable cause )
        throws ChecksumFailedException
    {
        if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals( checksumPolicy ) )
        {
            throw new ChecksumFailedException( message, cause );
        }
        else if ( !ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals( checksumPolicy ) )
        {
            // warn if it is set to anything other than ignore
            logger.warn( "*** CHECKSUM FAILED - " + message + " - IGNORING" );
        }
        // otherwise it is ignore
    }

    private void verifyChecksum( ChecksumObserver checksumObserver, File destination, File tempDestination, String remotePath, String checksumFileExtension, Wagon wagon )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException
    {
        try
        {
            // grab it first, because it's about to change...
            String actualChecksum = checksumObserver.getActualChecksum();

            File tempChecksumFile = new File( tempDestination + checksumFileExtension + ".tmp" );
            tempChecksumFile.deleteOnExit();
            wagon.get( remotePath + checksumFileExtension, tempChecksumFile );

            String expectedChecksum = FileUtils.fileRead( tempChecksumFile, "UTF-8" );

            // remove whitespaces at the end
            expectedChecksum = expectedChecksum.trim();

            // check for 'ALGO (name) = CHECKSUM' like used by openssl
            if ( expectedChecksum.regionMatches( true, 0, "MD", 0, 2 ) || expectedChecksum.regionMatches( true, 0, "SHA", 0, 3 ) )
            {
                int lastSpacePos = expectedChecksum.lastIndexOf( ' ' );
                expectedChecksum = expectedChecksum.substring( lastSpacePos + 1 );
            }
            else
            {
                // remove everything after the first space (if available)
                int spacePos = expectedChecksum.indexOf( ' ' );

                if ( spacePos != -1 )
                {
                    expectedChecksum = expectedChecksum.substring( 0, spacePos );
                }
            }
            if ( expectedChecksum.equalsIgnoreCase( actualChecksum ) )
            {
                File checksumFile = new File( destination + checksumFileExtension );
                if ( checksumFile.exists() )
                {
                    checksumFile.delete();
                }
                FileUtils.copyFile( tempChecksumFile, checksumFile );
                tempChecksumFile.delete();
            }
            else
            {
                throw new ChecksumFailedException( "Checksum failed on download: local = '" + actualChecksum + "'; remote = '" + expectedChecksum + "'" );
            }
        }
        catch ( IOException e )
        {
            throw new ChecksumFailedException( "Invalid checksum file", e );
        }
    }

    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            logger.error( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }

    private void releaseWagon( String protocol, Wagon wagon )
    {
        try
        {
            container.release( wagon );
        }
        catch ( ComponentLifecycleException e )
        {
            logger.error( "Problem releasing wagon - ignoring: " + e.getMessage() );
            logger.debug( "", e );
        }
    }

    /**
     * Checks the URL to see if this repository refers to an external repository
     * 
     * @param originalRepository
     * @return true if external.
     */
    public boolean isExternalRepo( ArtifactRepository originalRepository )
    {
        try
        {
            URL url = new URL( originalRepository.getUrl() );
            return !( url.getHost().equals( "localhost" ) || url.getHost().equals( "127.0.0.1" ) || url.getProtocol().equals( "file" ) );
        }
        catch ( MalformedURLException e )
        {
            // bad url just skip it here. It should have been validated already, but the wagon lookup will deal with it
            return false;
        }
    }
    
    /**
     * Applies the server configuration to the wagon
     * 
     * @param wagon the wagon to configure
     * @param repository the repository that has the configuration
     * @throws WagonConfigurationException wraps any error given during configuration of the wagon
     *             instance
     */
    private void configureWagon( Wagon wagon, ArtifactRepository repository )
        throws WagonConfigurationException
    {
        configureWagon( wagon, repository.getId(), repository.getProtocol() );
    }

    private void configureWagon( Wagon wagon, String repositoryId, String protocol )
        throws WagonConfigurationException
    {
        PlexusConfiguration config = (PlexusConfiguration) serverConfigurationMap.get( repositoryId );

        if ( config != null )
        {
            ComponentConfigurator componentConfigurator = null;

            try
            {
                componentConfigurator = new BasicComponentConfigurator();

                componentConfigurator.configureComponent( wagon, config, container.getContainerRealm() );
            }
            catch ( ComponentConfigurationException e )
            {
                throw new WagonConfigurationException( repositoryId, "Unable to apply wagon configuration.", e );
            }
            finally
            {
                if ( componentConfigurator != null )
                {
                    try
                    {
                        container.release( componentConfigurator );
                    }
                    catch ( ComponentLifecycleException e )
                    {
                        logger.error( "Problem releasing configurator - ignoring: " + e.getMessage() );
                    }
                }

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setHttpUserAgent( String userAgent )
    {
        this.httpUserAgent = userAgent;
    }

    /**
     * {@inheritDoc}
     */
    public String getHttpUserAgent()
    {
        return httpUserAgent;
    }

    public Set<String> getSupportProtocols()
    {
        return wagons.keySet();
    }
}
