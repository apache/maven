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
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultWagonManager
    extends AbstractLogEnabled
    implements WagonManager, Contextualizable
{
    private PlexusContainer container;

    private Map proxies = new HashMap();

    private TransferListener downloadMonitor;

    private ArtifactHandlerManager artifactHandlerManager;

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

    // TODO: don't throw exception
    private void releaseWagon( Wagon wagon )
        throws Exception
    {
        container.release( wagon );
    }

    public void putArtifact( File source, Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException
    {
        try
        {
            putRemoteFile( repository, source, repository.pathOf( artifact ) );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new TransferFailedException( "Path of artifact could not be determined: ", e );
        }
    }

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        try
        {
            putRemoteFile( repository, source, repository.pathOfMetadata( artifactMetadata ) );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new TransferFailedException( "Path of artifact could not be determined: ", e );
        }
    }

    private void putRemoteFile( ArtifactRepository repository, File source, String remotePath )
        throws TransferFailedException
    {
        Wagon wagon = null;
        try
        {
            wagon = getWagon( repository.getProtocol() );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: ", e );
        }

        // TODO: probably don't want this on metadata...
        // TODO: not working well on upload, commented out for now
//        if ( downloadMonitor != null )
//        {
//            wagon.addTransferListener( downloadMonitor );
//        }

        try
        {
            wagon.connect( repository, getProxy( repository.getProtocol() ) );

            wagon.put( source, remotePath );

            // TODO [BP]: put all disconnects in finally
            wagon.disconnect();
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
        finally
        {
            try
            {
                releaseWagon( wagon );
            }
            catch ( Exception e )
            {
                throw new TransferFailedException( "Unable to release wagon", e );
            }
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
        String remotePath = null;
        try
        {
            remotePath = repository.pathOf( artifact );
        }
        catch ( ArtifactPathFormatException e )
        {
            // TODO may be more appropriate to propogate the APFE
            throw new TransferFailedException( "Failed to determine path for artifact", e );
        }

        getRemoteFile( repository, destination, remotePath );
    }

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath;
        try
        {
            remotePath = remoteRepository.pathOfMetadata( metadata );
        }
        catch ( ArtifactPathFormatException e )
        {
            // TODO may be more appropriate to propogate APFE
            throw new TransferFailedException( "Failed to determine path for artifact", e );
        }

        getRemoteFile( remoteRepository, destination, remotePath );
    }

    private void getRemoteFile( ArtifactRepository repository, File destination, String remotePath )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        Wagon wagon;

        try
        {
            wagon = getWagon( repository.getProtocol() );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: ", e );
        }

        // ----------------------------------------------------------------------
        // These can certainly be configurable ... registering listeners
        // ...

        //ChecksumObserver md5SumObserver = new ChecksumObserver();

        // ----------------------------------------------------------------------

        //wagon.addTransferListener( md5SumObserver );

        // TODO: probably don't want this on metadata...
        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        // TODO [BP]: do this handling in Wagon itself
        if ( !destination.getParentFile().exists() )
        {
            destination.getParentFile().mkdirs();
        }

        File temp = new File( destination + ".tmp" );
        temp.deleteOnExit();

        try
        {
            wagon.connect( repository, getProxy( repository.getProtocol() ) );

            wagon.get( remotePath, temp );

            // TODO [BP]: put all disconnects in finally
            wagon.disconnect();
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
            try
            {
                releaseWagon( wagon );
            }
            catch ( Exception e )
            {
                throw new TransferFailedException( "Release of wagon failed: ", e );
            }
        }

        if ( !temp.exists() )
        {
            throw new TransferFailedException( "Downloaded file does not exist: " + temp );
        }

        // The temporary file is named destination + ".tmp" and is done this
        // way to ensure
        // that the temporary file is in the same file system as the
        // destination because the
        // File.renameTo operation doesn't really work across file systems.
        // So we will attempt
        // to do a File.renameTo for efficiency and atomicity, if this fails
        // then we will use
        // a brute force copy and delete the temporary file.

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

    private ProxyInfo getProxy( String protocol )
    {
        return (ProxyInfo) proxies.get( protocol );
    }

    /**
     * Set the proxy used for a particular protocol.
     *
     * @param protocol      the protocol (required)
     * @param host          the proxy host name (required)
     * @param port          the proxy port (required)
     * @param username      the username for the proxy, or null if there is none
     * @param password      the password for the proxy, or null if there is none
     * @param nonProxyHosts the set of hosts not to use the proxy for. Follows Java system
     *                      property format: <code>*.foo.com|localhost</code>.
     * @todo [BP] would be nice to configure this via plexus in some way
     */
    public void setProxy( String protocol, String host, int port, String username, String password,
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

}