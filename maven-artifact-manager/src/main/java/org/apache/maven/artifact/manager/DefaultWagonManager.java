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
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.manager.NotOnlineException;
import org.apache.maven.wagon.manager.RepositoryNotFoundException;
import org.apache.maven.wagon.manager.RepositorySettings;
import org.apache.maven.wagon.manager.WagonConfigurationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * DefaultWagonManager 
 *
 * @version $Id$
 * @deprecated in favor of {@link ArtifactManager} and {@link WagonManager}
 */
public class DefaultWagonManager
    extends AbstractLogEnabled
    implements WagonManager
{
    private ArtifactManager artifactManager;

    private TransferListener downloadMonitor;

    private RepositorySettings getRepositorySettings( String repositoryId )
    {
        return artifactManager.getWagonManager().getRepositorySettings( repositoryId );
    }

    public void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey,
                                       String passphrase )
    {
        RepositorySettings settings = getRepositorySettings( repositoryId );
        settings.setAuthentication( username, password, privateKey, passphrase );
    }

    public void addConfiguration( String repositoryId, Xpp3Dom configuration )
    {
        RepositorySettings settings = getRepositorySettings( repositoryId );
        settings.setConfiguration( new XmlPlexusConfiguration( configuration ) );
    }

    public void addMirror( String id, String mirrorOf, String url )
    {
        RepositorySettings settings = getRepositorySettings( mirrorOf );
        Repository repository = new Repository( id, url );
        artifactManager.getWagonManager().addRepository( repository );
        settings.addMirror( repository.getId() );
    }

    public void addPermissionInfo( String repositoryId, String filePermissions, String directoryPermissions )
    {
        RepositorySettings settings = getRepositorySettings( repositoryId );
        settings.setPermissions( /* group */null, filePermissions, directoryPermissions );
    }

    public void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts )
    {
        artifactManager.getWagonManager().addProxy( protocol, host, port, username, password, nonProxyHosts );
    }

    public void getArtifact( Artifact artifact, List remoteRepositories )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        artifactManager.getArtifact( artifact, remoteRepositories );
    }

    public void getArtifact( Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        artifactManager.getArtifact( artifact, repository );
    }

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination,
                                     String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        artifactManager.getArtifactMetadata( metadata, remoteRepository, destination, checksumPolicy );
    }

    public AuthenticationInfo getAuthenticationInfo( String id )
    {
        RepositorySettings settings = getRepositorySettings( id );
        return settings.getAuthentication();
    }

    public ProxyInfo getProxy( String protocol )
    {
        return artifactManager.getWagonManager().getProxy( protocol );
    }

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        return null;
    }

    public Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException
    {
        try
        {
            return artifactManager.getWagonManager().getWagon( repository.getId() );
        }
        catch ( RepositoryNotFoundException e )
        {
            // Not throwing to maintain API contract.
            // TODO: Need to remove this class in prior to maven 2.1 anyway.
            getLogger().error( e.getMessage(), e );
        }
        catch ( WagonConfigurationException e )
        {
            // Not throwing to maintain API contract.
            // TODO: Need to remove this class in prior to maven 2.1 anyway.
            getLogger().error( e.getMessage(), e );
        }
        catch ( NotOnlineException e )
        {
            // Not throwing to maintain API contract.
            // TODO: Need to remove this class in prior to maven 2.1 anyway.
            getLogger().error( e.getMessage(), e );
        }

        return null;
    }

    public boolean isOnline()
    {
        return artifactManager.isOnline();
    }

    public void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws TransferFailedException
    {
        artifactManager.putArtifact( source, artifact, deploymentRepository );
    }

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException
    {
        artifactManager.putArtifactMetadata( source, artifactMetadata, repository );
    }

    public void registerWagons( Collection wagons, PlexusContainer extensionContainer )
    {
        /* do nothing */
    }

    public void setDownloadMonitor( TransferListener monitor )
    {
        // Remove previous (to maintain API contract)
        if ( this.downloadMonitor != null )
        {
            artifactManager.getWagonManager().removeTransferListener( this.downloadMonitor );
        }

        this.downloadMonitor = monitor;

        // Add new (to maintain API contract)
        if ( this.downloadMonitor != null )
        {
            artifactManager.getWagonManager().addTransferListener( this.downloadMonitor );
        }
    }

    public void setInteractive( boolean interactive )
    {
        artifactManager.getWagonManager().setInteractive( interactive );
    }

    public void setOnline( boolean online )
    {
        artifactManager.getWagonManager().setOnline( online );
    }
}
