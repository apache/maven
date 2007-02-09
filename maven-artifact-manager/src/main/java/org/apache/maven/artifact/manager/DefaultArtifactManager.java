package org.apache.maven.artifact.manager;

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
import org.apache.maven.wagon.manager.NotOnlineException;
import org.apache.maven.wagon.manager.RepositoryNotFoundException;
import org.apache.maven.wagon.manager.WagonConfigurationException;
import org.apache.maven.wagon.manager.WagonManager;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DefaultArtifactManager 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultArtifactManager
    extends AbstractLogEnabled
    implements ArtifactManager
{
    private WagonManager wagonManager;

    public void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws TransferFailedException
    {
        putRemoteFile( deploymentRepository, source, deploymentRepository.pathOf( artifact ) );
    }

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        getLogger().info( "Uploading " + artifactMetadata );
        putRemoteFile( repository, source, repository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
    }

    private void putRemoteFile( ArtifactRepository repository, File source, String remotePath )
        throws TransferFailedException
    {
        failIfNotOnline();

        addWagonRepository( repository );

        Wagon wagon = null;

        try
        {
            wagon = wagonManager.getWagon( repository.getId() );

            Map checksums = new HashMap( 2 );
            Map sums = new HashMap( 2 );

            // TODO: configure these on the repository
            try
            {
                ChecksumObserver checksumObserver = new ChecksumObserver( "MD5" );
                wagon.addTransferListener( checksumObserver );
                checksums.put( "md5", checksumObserver );

                checksumObserver = new ChecksumObserver( "SHA-1" );
                wagon.addTransferListener( checksumObserver );
                checksums.put( "sha1", checksumObserver );
            }
            catch ( NoSuchAlgorithmException e )
            {
                throw new TransferFailedException( "Unable to add checksum methods: " + e.getMessage(), e );
            }

            wagon.connect();

            wagon.put( source, remotePath );

            // Pre-store the checksums as any future puts will overwrite them
            for ( Iterator i = checksums.keySet().iterator(); i.hasNext(); )
            {
                String extension = (String) i.next();
                ChecksumObserver observer = (ChecksumObserver) checksums.get( extension );
                sums.put( extension, observer.getActualChecksum() );
            }

            // We do this in here so we can checksum the artifact metadata too, otherwise it could be metadata itself
            for ( Iterator i = checksums.keySet().iterator(); i.hasNext(); )
            {
                String extension = (String) i.next();

                // TODO: shouldn't need a file intermediatary - improve wagon to take a stream
                File temp = File.createTempFile( "maven-artifact", null );
                temp.deleteOnExit();
                FileUtils.fileWrite( temp.getAbsolutePath(), (String) sums.get( extension ) );

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
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Protocol not supported.", e );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new TransferFailedException( "Repository not found.", e );
        }
        catch ( WagonConfigurationException e )
        {
            throw new TransferFailedException( "Wagon configuration exception.", e );
        }
        catch ( NotOnlineException e )
        {
            throw new TransferFailedException( "Wagon has been configured to be offline: " + e.getMessage(), e );
        }
        finally
        {
            // Note: releaseWagon will disconnect too.
            wagonManager.releaseWagon( wagon );
        }
    }

    private void addWagonRepository( ArtifactRepository repository )
    {
        if ( repository instanceof Repository )
        {
            wagonManager.addRepository( (Repository) repository );
        }
        else
        {
            wagonManager.addRepository( new Repository( repository.getId(), repository.getUrl() ) );
        }
    }

    public void getArtifact( Artifact artifact, List remoteRepositories )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        // TODO [BP]: The exception handling here needs some work
        boolean successful = false;
        for ( Iterator iter = remoteRepositories.iterator(); iter.hasNext() && !successful; )
        {
            ArtifactRepository repository = (ArtifactRepository) iter.next();

            try
            {
                getArtifact( artifact, repository );

                successful = artifact.isResolved();
            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                getLogger().warn(
                                  "Unable to get resource '" + artifact.getId() + "' from repository "
                                      + repository.getId() + " (" + repository.getUrl() + ")" );
            }
            catch ( TransferFailedException e )
            {
                getLogger().warn(
                                  "Unable to get resource '" + artifact.getId() + "' from repository "
                                      + repository.getId() + " (" + repository.getUrl() + ")" );
            }
        }

        // if it already exists locally we were just trying to force it - ignore the update
        if ( !successful && !artifact.getFile().exists() )
        {
            throw new ResourceDoesNotExistException( "Unable to download the artifact from any repository" );
        }
    }

    public void getArtifact( Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOf( artifact );

        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

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
            getLogger().debug( "Trying repository " + repository.getId() );
            getRemoteFile( repository, artifact.getFile(), remotePath, policy.getChecksumPolicy(), false );
            getLogger().debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }
    }

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository repository, File destination,
                                     String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, checksumPolicy, true );
    }

    private void getRemoteFile( ArtifactRepository repository, File destination, String remotePath,
                                String checksumPolicy, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        // TODO: better exceptions - transfer failed is not enough?

        failIfNotOnline();

        addWagonRepository( repository );

        Wagon wagon = null;

        File temp = new File( destination + ".tmp" );
        temp.deleteOnExit();

        boolean downloaded = false;

        try
        {
            wagon = wagonManager.getWagon( repository.getId() );

            // TODO: configure on repository
            ChecksumObserver md5ChecksumObserver;
            ChecksumObserver sha1ChecksumObserver;
            try
            {
                md5ChecksumObserver = new ChecksumObserver( "MD5" );
                wagon.addTransferListener( md5ChecksumObserver );

                sha1ChecksumObserver = new ChecksumObserver( "SHA-1" );
                wagon.addTransferListener( sha1ChecksumObserver );
            }
            catch ( NoSuchAlgorithmException e )
            {
                throw new TransferFailedException( "Unable to add checksum methods: " + e.getMessage(), e );
            }

            wagon.connect();

            boolean firstRun = true;
            boolean retry = true;

            // this will run at most twice. The first time, the firstRun flag is turned off, and if the retry flag
            // is set on the first run, it will be turned off and not re-set on the second try. This is because the
            // only way the retry flag can be set is if ( firstRun == true ).
            while ( firstRun || retry )
            {
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

                if ( downloaded )
                {
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
                            getLogger().warn( "*** CHECKSUM FAILED - " + e.getMessage() + " - RETRYING" );
                            retry = true;
                        }
                        else
                        {
                            handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                        }
                    }
                    catch ( ResourceDoesNotExistException sha1TryException )
                    {
                        getLogger().debug( "SHA1 not found, trying MD5", sha1TryException );

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
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Transfer Failed: " + e.getMessage(), e );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new TransferFailedException( "Internal Error: " + e.getMessage(), e );
        }
        catch ( WagonConfigurationException e )
        {
            throw new TransferFailedException( "Wagon Configuration Error: " + e.getMessage(), e );
        }
        catch ( NotOnlineException e )
        {
            throw new TransferFailedException( "Wagon has been configured to be offline: " + e.getMessage(), e );
        }
        finally
        {
            wagonManager.releaseWagon( wagon );
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
                    throw new TransferFailedException( "Error copying temporary file to the final destination: "
                        + e.getMessage(), e );
                }
            }
        }
    }

    private void failIfNotOnline()
        throws TransferFailedException
    {
        if ( !isOnline() )
        {
            throw new TransferFailedException( "System is offline." );
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
            getLogger().warn( "*** CHECKSUM FAILED - " + message + " - IGNORING" );
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

            String expectedChecksum = FileUtils.fileRead( tempChecksumFile );

            // remove whitespaces at the end
            expectedChecksum = expectedChecksum.trim();

            // check for 'MD5 (name) = CHECKSUM'
            if ( expectedChecksum.startsWith( "MD5" ) )
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
            if ( expectedChecksum.equals( actualChecksum ) )
            {
                File checksumFile = new File( destination + checksumFileExtension );
                if ( checksumFile.exists() )
                {
                    checksumFile.delete();
                }
                FileUtils.copyFile( tempChecksumFile, checksumFile );
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

    public boolean isOnline()
    {
        return wagonManager.isOnline();
    }

    public WagonManager getWagonManager()
    {
        return wagonManager;
    }

    /*    public void registerWagons( Collection wagons, PlexusContainer extensionContainer )
     {
     for ( Iterator i = wagons.iterator(); i.hasNext(); )
     {
     availableWagons.put( i.next(), extensionContainer );
     }
     }*/
}