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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.util.ConfigUtils;

//TODO remove the update check manager
//TODO separate into retriever and publisher
//TODO remove hardcoding of checksum logic

/**
 * Manages <a href="https://maven.apache.org/wagon">Wagon</a> related operations in Maven.
 */
@Component( role = WagonManager.class )
public class DefaultWagonManager
    implements WagonManager
{

    private static final String[] CHECKSUM_IDS =
    {
        "md5", "sha1"
    };

    /**
     * have to match the CHECKSUM_IDS
     */
    private static final String[] CHECKSUM_ALGORITHMS =
    {
        "MD5", "SHA-1"
    };

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    @Requirement
    private LegacySupport legacySupport;

    //
    // Retriever
    //
    @Override
    public void getArtifact( Artifact artifact, ArtifactRepository repository, TransferListener downloadMonitor,
                             boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOf( artifact );

        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

        if ( !policy.isEnabled() )
        {
            logger.debug( "Skipping disabled repository " + repository.getId() + " for resolution of "
                              + artifact.getId() );

        }
        else if ( artifact.isSnapshot() || !artifact.getFile().exists() )
        {
            if ( force || updateCheckManager.isUpdateRequired( artifact, repository ) )
            {
                logger.debug( "Trying repository " + repository.getId() + " for resolution of " + artifact.getId()
                                  + " from " + remotePath );

                try
                {
                    getRemoteFile( repository, artifact.getFile(), remotePath, downloadMonitor,
                                   policy.getChecksumPolicy(), false );

                    updateCheckManager.touch( artifact, repository, null );
                }
                catch ( ResourceDoesNotExistException e )
                {
                    updateCheckManager.touch( artifact, repository, null );
                    throw e;
                }
                catch ( TransferFailedException e )
                {
                    String error = ( e.getMessage() != null ) ? e.getMessage() : e.getClass().getSimpleName();
                    updateCheckManager.touch( artifact, repository, error );
                    throw e;
                }

                logger.debug( "  Artifact " + artifact.getId() + " resolved to " + artifact.getFile() );

                artifact.setResolved( true );
            }
            else if ( !artifact.getFile().exists() )
            {
                String error = updateCheckManager.getError( artifact, repository );
                if ( error != null )
                {
                    throw new TransferFailedException(
                        "Failure to resolve " + remotePath + " from " + repository.getUrl()
                            + " was cached in the local repository. "
                            + "Resolution will not be reattempted until the update interval of "
                            + repository.getId() + " has elapsed or updates are forced. Original error: " + error );

                }
                else
                {
                    throw new ResourceDoesNotExistException(
                        "Failure to resolve " + remotePath + " from " + repository.getUrl()
                            + " was cached in the local repository. "
                            + "Resolution will not be reattempted until the update interval of "
                            + repository.getId() + " has elapsed or updates are forced." );

                }
            }
        }
    }

    @Override
    public void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                             TransferListener downloadMonitor, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        TransferFailedException tfe = null;

        for ( ArtifactRepository repository : remoteRepositories )
        {
            try
            {
                getArtifact( artifact, repository, downloadMonitor, force );

                if ( artifact.isResolved() )
                {
                    artifact.setRepository( repository );
                    break;
                }
            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                logger.debug( "Unable to find artifact " + artifact.getId() + " in repository " + repository.getId()
                                  + " (" + repository.getUrl() + ")", e );

            }
            catch ( TransferFailedException e )
            {
                tfe = e;

                String msg =
                    "Unable to get artifact " + artifact.getId() + " from repository " + repository.getId() + " ("
                        + repository.getUrl() + "): " + e.getMessage();

                if ( logger.isDebugEnabled() )
                {
                    logger.warn( msg, e );
                }
                else
                {
                    logger.warn( msg );
                }
            }
        }

        // if it already exists locally we were just trying to force it - ignore the update
        if ( !artifact.getFile().exists() )
        {
            if ( tfe != null )
            {
                throw tfe;
            }
            else
            {
                throw new ResourceDoesNotExistException( "Unable to download the artifact from any repository" );
            }
        }
    }

    @Override
    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository repository, File destination,
                                     String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    @Override
    public void getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository repository,
                                                             File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    /**
     * Deal with connecting to a wagon repository taking into account authentication and proxies.
     *
     * @param wagon
     * @param repository
     *
     * @throws ConnectionException
     * @throws AuthenticationException
     */
    private void connectWagon( Wagon wagon, ArtifactRepository repository )
        throws ConnectionException, AuthenticationException
    {
        // MNG-5509
        // See org.eclipse.aether.connector.wagon.WagonRepositoryConnector.connectWagon(Wagon)
        if ( legacySupport.getRepositorySession() != null )
        {
            String userAgent = ConfigUtils.getString( legacySupport.getRepositorySession(), null,
                                                      ConfigurationProperties.USER_AGENT );

            if ( userAgent == null )
            {
                Properties headers = new Properties();

                headers.put( "User-Agent", ConfigUtils.getString( legacySupport.getRepositorySession(), "Maven",
                                                                  ConfigurationProperties.USER_AGENT ) );
                try
                {
                    Method setHttpHeaders = wagon.getClass().getMethod( "setHttpHeaders", Properties.class );
                    setHttpHeaders.invoke( wagon, headers );
                }
                catch ( NoSuchMethodException e )
                {
                    // normal for non-http wagons
                }
                catch ( Exception e )
                {
                    logger.debug( "Could not set user agent for wagon " + wagon.getClass().getName() + ": " + e );
                }
            }
        }

        if ( repository.getProxy() != null && logger.isDebugEnabled() )
        {
            logger.debug( "Using proxy " + repository.getProxy().getHost() + ":" + repository.getProxy().getPort()
                              + " for " + repository.getUrl() );

        }

        if ( repository.getAuthentication() != null && repository.getProxy() != null )
        {
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ), authenticationInfo( repository ),
                           proxyInfo( repository ) );

        }
        else if ( repository.getAuthentication() != null )
        {
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ),
                           authenticationInfo( repository ) );

        }
        else if ( repository.getProxy() != null )
        {
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ), proxyInfo( repository ) );
        }
        else
        {
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ) );
        }
    }

    private AuthenticationInfo authenticationInfo( ArtifactRepository repository )
    {
        AuthenticationInfo ai = new AuthenticationInfo();
        ai.setUserName( repository.getAuthentication().getUsername() );
        ai.setPassword( repository.getAuthentication().getPassword() );
        return ai;
    }

    private ProxyInfo proxyInfo( ArtifactRepository repository )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( repository.getProxy().getHost() );
        proxyInfo.setType( repository.getProxy().getProtocol() );
        proxyInfo.setPort( repository.getProxy().getPort() );
        proxyInfo.setNonProxyHosts( repository.getProxy().getNonProxyHosts() );
        proxyInfo.setUserName( repository.getProxy().getUserName() );
        proxyInfo.setPassword( repository.getProxy().getPassword() );
        return proxyInfo;
    }

    @SuppressWarnings( "checkstyle:methodlength" )
    @Override
    public void getRemoteFile( ArtifactRepository repository, File destination, String remotePath,
                               TransferListener downloadMonitor, String checksumPolicy, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String protocol = repository.getProtocol();

        Wagon wagon;

        try
        {
            wagon = getWagon( protocol );
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
            connectWagon( wagon, repository );

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
                    // TODO configure on repository
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
                        // if we catch a ChecksumFailedException, it means the transfer/read succeeded, but the
                        // checksum doesn't match. This could be a problem with the server (ibiblio HTTP-200 error
                        // page), so we'll try this up to two times. On the second try, we'll handle it as a bona-fide
                        // error, based on the repository's checksum checking policy.
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
                        logger.debug( "SHA1 not found, trying MD5: " + sha1TryException.getMessage() );

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
                            handleChecksumFailure( checksumPolicy, "Error retrieving checksum file for " + remotePath,
                                                   md5TryException );
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

                    if ( !temp.delete() )
                    {
                        temp.deleteOnExit();
                    }
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( "Error copying temporary file to the final destination: "
                                                           + e.getMessage(), e );

                }
            }
        }
    }

    //
    // Publisher
    //
    @Override
    public void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                             TransferListener downloadMonitor )
        throws TransferFailedException
    {
        putRemoteFile( deploymentRepository, source, deploymentRepository.pathOf( artifact ), downloadMonitor );
    }

    @Override
    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        logger.info( "Uploading " + artifactMetadata );
        putRemoteFile( repository, source, repository.pathOfRemoteRepositoryMetadata( artifactMetadata ), null );
    }

    @Override
    public void putRemoteFile( ArtifactRepository repository, File source, String remotePath,
                               TransferListener downloadMonitor )
        throws TransferFailedException
    {
        String protocol = repository.getProtocol();

        Wagon wagon;
        try
        {
            wagon = getWagon( protocol );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        Map<String, ChecksumObserver> checksums = new HashMap<>( 2 );

        Map<String, String> sums = new HashMap<>( 2 );

        // TODO configure these on the repository
        for ( int i = 0; i < CHECKSUM_IDS.length; i++ )
        {
            checksums.put( CHECKSUM_IDS[i], addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i] ) );
        }

        List<File> temporaryFiles = new ArrayList<>();

        try
        {
            try
            {
                connectWagon( wagon, repository );

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
                // TODO shouldn't need a file intermediatary - improve wagon to take a stream
                File temp = File.createTempFile( "maven-artifact", null );
                temp.deleteOnExit();
                FileUtils.fileWrite( temp.getAbsolutePath(), "UTF-8", sums.get( extension ) );

                temporaryFiles.add( temp );
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
            // MNG-4543
            cleanupTemporaryFiles( temporaryFiles );

            // Remove every checksum listener
            for ( String id : CHECKSUM_IDS )
            {
                TransferListener checksumListener = checksums.get( id );
                if ( checksumListener != null )
                {
                    wagon.removeTransferListener( checksumListener );
                }
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }
    }

    private void cleanupTemporaryFiles( List<File> files )
    {
        for ( File file : files )
        {
            // really don't care if it failed here only log warning
            if ( !file.delete() )
            {
                logger.warn( "skip failed to delete temporary file : " + file.getAbsolutePath() );
                file.deleteOnExit();
            }
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

    private void verifyChecksum( ChecksumObserver checksumObserver, File destination, File tempDestination,
                                 String remotePath, String checksumFileExtension, Wagon wagon )
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
            if ( expectedChecksum.regionMatches( true, 0, "MD", 0, 2 )
                     || expectedChecksum.regionMatches( true, 0, "SHA", 0, 3 ) )
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
                    checksumFile.delete(); // ignore if failed as we will overwrite
                }
                FileUtils.copyFile( tempChecksumFile, checksumFile );
                if ( !tempChecksumFile.delete() )
                {
                    tempChecksumFile.deleteOnExit();
                }
            }
            else
            {
                throw new ChecksumFailedException( "Checksum failed on download: local = '" + actualChecksum
                                                       + "'; remote = '" + expectedChecksum + "'" );

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

    @Override
    @Deprecated
    public Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException
    {
        return getWagon( repository.getProtocol() );
    }

    @Override
    @Deprecated
    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        if ( protocol == null )
        {
            throw new UnsupportedProtocolException( "Unspecified protocol" );
        }

        String hint = protocol.toLowerCase( java.util.Locale.ENGLISH );

        Wagon wagon;
        try
        {
            wagon = container.lookup( Wagon.class, hint );
        }
        catch ( ComponentLookupException e )
        {
            throw new UnsupportedProtocolException( "Cannot find wagon which supports the requested protocol: "
                                                        + protocol, e );

        }

        return wagon;
    }

}
