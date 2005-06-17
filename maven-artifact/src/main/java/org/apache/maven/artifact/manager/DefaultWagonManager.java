package org.apache.maven.artifact.manager;

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
import org.apache.maven.artifact.ChecksumFailedException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultWagonManager
    extends AbstractLogEnabled
    implements WagonManager, Contextualizable
{
    private PlexusContainer container;

    // TODO: proxies, authentication and mirrors are via settings, and should come in via an alternate method - perhaps
    // attached to ArtifactRepository before the method is called (so AR would be composed of WR, not inherit it)
    private Map proxies = new HashMap();

    private Map authenticationInfoMap = new HashMap();

    private Map mirrors = new HashMap();

    private TransferListener downloadMonitor;

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        Wagon wagon;

        try
        {
            wagon = (Wagon) container.lookup( Wagon.ROLE, protocol );
        }
        catch ( ComponentLookupException e )
        {
            throw new UnsupportedProtocolException( "Cannot find wagon which supports the requested protocol: " +
                                                    protocol, e );
        }

        return wagon;
    }

    public void putArtifact( File source, Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException
    {
        putRemoteFile( repository, source, repository.pathOf( artifact ), downloadMonitor );
    }

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        getLogger().info( "Uploading " + artifactMetadata );
        putRemoteFile( repository, source, repository.pathOfMetadata( artifactMetadata ), null );
    }

    private void putRemoteFile( ArtifactRepository repository, File source, String remotePath,
                                TransferListener downloadMonitor )
        throws TransferFailedException
    {
        String protocol = repository.getProtocol();

        Wagon wagon = null;
        try
        {
            wagon = getWagon( protocol );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: ", e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

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
            throw new TransferFailedException( "Unable to add checksum methods", e );
        }

        try
        {
            wagon.connect( repository, getAuthenticationInfo( repository.getId() ), getProxy( protocol ) );

            wagon.put( source, remotePath );

            wagon.removeTransferListener( downloadMonitor );

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
            throw new TransferFailedException( "Connection failed: ", e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: ", e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: ", e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new TransferFailedException( "Resource to deploy not found: ", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error creating temporary file for deployment: ", e );
        }
        finally
        {
            disconnectWagon( wagon );

            releaseWagon( wagon );
        }
    }

    public void getArtifact( Artifact artifact, List remoteRepositories, File destination )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        // TODO [BP]: The exception handling here needs some work
        boolean successful = false;
        for ( Iterator iter = remoteRepositories.iterator(); iter.hasNext() && !successful; )
        {
            ArtifactRepository repository = (ArtifactRepository) iter.next();

            try
            {
                getArtifact( artifact, repository, destination );

                successful = true;
            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                getLogger().warn( "Unable to get resource from repository " + repository.getUrl() );
            }
        }

        if ( !successful )
        {
            throw new ResourceDoesNotExistException( "Unable to download the artifact from any repository" );
        }
    }

    public void getArtifact( Artifact artifact, ArtifactRepository repository, File destination )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOf( artifact );

        getRemoteFile( repository, destination, remotePath, downloadMonitor );
    }

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = remoteRepository.pathOfMetadata( metadata );

        getLogger().info( "Retrieving " + metadata );
        getRemoteFile( remoteRepository, destination, remotePath, null );
    }

    private void getRemoteFile( ArtifactRepository repository, File destination, String remotePath,
                                TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException, ChecksumFailedException
    {
        // TODO: better excetpions - transfer failed is not enough?

        Wagon wagon;

        String protocol = repository.getProtocol();
        try
        {
            wagon = getWagon( protocol );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: ", e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        // TODO: configure on repository
        ChecksumObserver checksumObserver;
        try
        {
            checksumObserver = new ChecksumObserver( "MD5" );
            wagon.addTransferListener( checksumObserver );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new TransferFailedException( "Unable to add checksum methods", e );
        }

        File temp = new File( destination + ".tmp" );
        temp.deleteOnExit();

        try
        {
            Repository mirror = getMirror( repository.getId() );
            if ( mirror != null )
            {
                repository = repository.createMirror( mirror );
            }

            wagon.connect( repository, getAuthenticationInfo( repository.getId() ), getProxy( protocol ) );

            // This should take care of creating destination directory now on
            wagon.get( remotePath, temp );

            try
            {
                // grab it first, because it's about to change...
                String actualChecksum = checksumObserver.getActualChecksum();

                File checksumFile = new File( destination + ".md5" );
                wagon.get( remotePath + ".md5", checksumFile );

                String expectedChecksum = FileUtils.fileRead( checksumFile );
                if ( !expectedChecksum.equals( actualChecksum ) )
                {
                    getLogger().warn(
                        "*** CHECKSUM MISMATCH - currently disabled fail due to bad repository checksums ***" );

                    // TODO: optionally retry?
                    /*                   throw new ChecksumFailedException( "Checksum failed on download: local = '" + actualChecksum +
                     "'; remote = '" + expectedChecksum + "'" );
                     */
                }
            }
            catch ( ResourceDoesNotExistException e )
            {
                getLogger().warn( "No checksum exists - assuming a valid download" );
            }
            catch ( IOException e )
            {
                getLogger().error( "Unable to read checksum - assuming a valid download", e );
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: ", e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: ", e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: ", e );
        }
        finally
        {
            disconnectWagon( wagon );

            releaseWagon( wagon );
        }

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
                throw new TransferFailedException( "Error copying temporary file to the final destination: ", e );
            }
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
            getLogger().error( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }

    private void releaseWagon( Wagon wagon )
    {
        try
        {
            container.release( wagon );
        }
        catch ( ComponentLifecycleException e )
        {
            getLogger().error( "Problem releasing wagon - ignoring: " + e.getMessage() );
        }
    }

    public ProxyInfo getProxy( String protocol )
    {
        return (ProxyInfo) proxies.get( protocol );
    }

    public AuthenticationInfo getAuthenticationInfo( String id )
    {
        return (AuthenticationInfo) authenticationInfoMap.get( id );
    }

    public Repository getMirror( String mirrorOf )
    {
        return (Repository) mirrors.get( mirrorOf );
    }

    /**
     * Set the proxy used for a particular protocol.
     *
     * @param protocol the protocol (required)
     * @param host the proxy host name (required)
     * @param port the proxy port (required)
     * @param username the username for the proxy, or null if there is none
     * @param password the password for the proxy, or null if there is none
     * @param nonProxyHosts the set of hosts not to use the proxy for. Follows Java system
     * property format: <code>*.foo.com|localhost</code>.
     * @todo [BP] would be nice to configure this via plexus in some way
     */
    public void addProxy( String protocol, String host, int port, String username, String password,
                          String nonProxyHosts )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setType( protocol );
        proxyInfo.setPort( port );
        proxyInfo.setNonProxyHosts( nonProxyHosts );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );

        proxies.put( protocol, proxyInfo );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * @todo I'd rather not be setting this explicitly.
     */
    public void setDownloadMonitor( TransferListener downloadMonitor )
    {
        this.downloadMonitor = downloadMonitor;
    }

    public void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey,
                                       String passphrase )
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();

        authInfo.setUserName( username );

        authInfo.setPassword( password );

        authInfo.setPrivateKey( privateKey );

        authInfo.setPassphrase( passphrase );

        authenticationInfoMap.put( repositoryId, authInfo );
    }

    public void addMirror( String id, String mirrorOf, String url )
    {
        Repository mirror = new Repository( id, url );

        mirrors.put( mirrorOf, mirror );
    }
}
