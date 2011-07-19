package org.apache.maven.router.repository;

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

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.GroupRoute;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.RequestTrace;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.ArtifactDownload;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.io.FileProcessor;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.util.listener.DefaultTransferEvent;
import org.sonatype.aether.util.listener.DefaultTransferResource;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link RepositoryConnector} implementation that resolves the proper repository URL using Route-M
 * routing information (groupId -&gt; canonical URL -&gt; mirror URL), then delegates transfers to
 * the appropriate <b>real</b> {@link WagonRepositoryConnector} for each artifact or metadata.
 * <br/>
 * <b>NOTE:</b> Artifact/metadata transfers sharing a groupId are collected and sent together to the appropriate
 * {@link WagonRepositoryConnector} when get(..) or put(..) is called.
 * 
 * @author John Casey
 */
class RoutingConnectorWrapper
    implements RepositoryConnector
{

    private static final String OPEN_CONNECTORS_KEY = RoutingConnectorWrapper.class.getName() + "#openConnectors";

    private static final String PUT_CONNECTOR_KEY = RoutingConnectorWrapper.class.getName() + "#putConnector";

    private final ArtifactRouter router;

    private final RepositorySystemSession session;
    
    private final RemoteRepository baseRepository;

    private final WagonProvider wagonProvider;

    private final WagonConfigurator wagonConfigurator;

    private final FileProcessor fileProcessor;

    private final Logger logger;
    
    public RoutingConnectorWrapper( RemoteRepository baseRepository, RepositorySystemSession session,
                                       WagonProvider wagonProvider, WagonConfigurator wagonConfigurator,
                                       FileProcessor fileProcessor, Logger logger )
    {
        this.baseRepository = baseRepository;
        this.wagonProvider = wagonProvider;
        this.wagonConfigurator = wagonConfigurator;
        this.fileProcessor = fileProcessor;
        this.logger = logger;
        
        ArtifactRouter router = (ArtifactRouter) session.getData().get( ArtifactRouter.SESSION_KEY );
        if ( router == null )
        {
            logger.debug( "Creating empty ArtifactRouter, since none has been initialized." );
            router = new ArtifactRouter();
            session.getData().set( ArtifactRouter.SESSION_KEY, router );
        }
        
        this.router = router;
        
        this.session = session;
    }

    public synchronized void get( Collection<? extends ArtifactDownload> artifactDownloads,
                     Collection<? extends MetadataDownload> metadataDownloads )
    {
        Map<GroupRoute, Set<ArtifactDownload>> artifactDownloadsByGroup = new HashMap<GroupRoute, Set<ArtifactDownload>>();
        Map<GroupRoute, Set<MetadataDownload>> metadataDownloadsByGroup = new HashMap<GroupRoute, Set<MetadataDownload>>();
        
        if ( artifactDownloads != null )
        {
            for ( ArtifactDownload artifactDownload : artifactDownloads )
            {
                GroupRoute route = router.getGroup( artifactDownload.getArtifact().getGroupId() );
                Set<ArtifactDownload> set = artifactDownloadsByGroup.get( route );
                if ( set == null )
                {
                    set = new LinkedHashSet<ArtifactDownload>();
                    artifactDownloadsByGroup.put( route, set );
                }
                
                set.add( artifactDownload );
                initConnector( route, "Artifact: " + artifactDownload.getArtifact().toString(), artifactDownload.getFile(), artifactDownload.getTrace() );
            }
        }
        
        if ( metadataDownloads != null )
        {
            for ( MetadataDownload metadataDownload : metadataDownloads )
            {
                GroupRoute route = router.getGroup( metadataDownload.getMetadata().getGroupId() );
                Set<MetadataDownload> set = metadataDownloadsByGroup.get( route );
                if ( set == null )
                {
                    set = new LinkedHashSet<MetadataDownload>();
                    metadataDownloadsByGroup.put( route, set );
                }
                
                set.add( metadataDownload );
                initConnector( route, "Metadata: " + metadataDownload.getMetadata().toString(), metadataDownload.getFile(), metadataDownload.getTrace() );
            }
        }
        
        Map<GroupRoute, RepositoryConnector> connectors = getOpenConnectors();
        for ( Map.Entry<GroupRoute, RepositoryConnector> entry : connectors.entrySet() )
        {
            GroupRoute route = entry.getKey();
            RepositoryConnector connector = entry.getValue();
            Set<ArtifactDownload> artifacts = artifactDownloadsByGroup.get( route );
            Set<MetadataDownload> metadatas = metadataDownloadsByGroup.get( route );

            if ( ( artifacts == null || artifacts.isEmpty() ) && ( metadatas == null || metadatas.isEmpty() ) )
            {
                continue;
            }

            connector.get( artifacts, metadatas );
        }
    }

    public synchronized void put( Collection<? extends ArtifactUpload> artifactUploads,
                     Collection<? extends MetadataUpload> metadataUploads )
    {
        try
        {
            WagonRepositoryConnector connector = getPutConnector( true );
            connector.put( artifactUploads, metadataUploads );
        }
        catch ( NoRepositoryConnectorException e )
        {
            logger.warn( "Cannot find Wagon for: " + baseRepository.getUrl(), e );
            if ( session.getTransferListener() != null )
            {
                DefaultTransferEvent event = new DefaultTransferEvent();
                
                String name =
                    ( artifactUploads == null ? 0 : artifactUploads.size() ) + " artifacts / "
                        + ( metadataUploads == null ? 0 : metadataUploads.size() ) + " metadata";
                
                RequestTrace trace = null;
                if ( artifactUploads != null )
                {
                    for ( ArtifactUpload artifactUpload : artifactUploads )
                    {
                        trace = artifactUpload.getTrace();
                        break;
                    }
                }
                
                if ( trace == null && metadataUploads != null )
                {
                    for ( MetadataUpload metadataUpload : metadataUploads )
                    {
                        trace = metadataUpload.getTrace();
                        break;
                    }
                }
                
                event.setResource( new DefaultTransferResource( baseRepository.getUrl(), name, null, trace ) );
                event.setRequestType( TransferEvent.RequestType.GET );
                event.setType( TransferEvent.EventType.FAILED );
                event.setException( e );

                session.getTransferListener().transferFailed( event );
            }
        }
    }

    public synchronized void close()
    {
        Map<GroupRoute, RepositoryConnector> openConnectors = getOpenConnectors();
        if ( openConnectors != null )
        {
            for ( RepositoryConnector connector : openConnectors.values() )
            {
                connector.close();
            }
            
        }
        
        setOpenConnectors( null );
        
        try
        {
            WagonRepositoryConnector putConnector = getPutConnector( false );
            if ( putConnector != null )
            {
                putConnector.close();
                session.getData().set( PUT_CONNECTOR_KEY, null );
            }
        }
        catch ( NoRepositoryConnectorException e )
        {
        }
    }

    private WagonRepositoryConnector getPutConnector( boolean create )
        throws NoRepositoryConnectorException
    {
        WagonRepositoryConnector putConnector = (WagonRepositoryConnector) session.getData().get( PUT_CONNECTOR_KEY );
        if ( create && putConnector == null )
        {
            putConnector = new WagonRepositoryConnector( wagonProvider, wagonConfigurator, baseRepository, session, fileProcessor, logger );
            session.getData().set( PUT_CONNECTOR_KEY, putConnector );
        }
        
        return putConnector;
    }

    private void initConnector( GroupRoute route, String name, File file, RequestTrace trace  )
    {
        Map<GroupRoute, RepositoryConnector> connectors = getOpenConnectors();
        if ( !connectors.containsKey( route ) )
        {
            RemoteRepository repo;
            if ( route == GroupRoute.DEFAULT )
            {
                repo = baseRepository;
            }
            else
            {
                repo = new RemoteRepository( baseRepository );
                repo.setId( baseRepository.getId() );
                repo.setUrl( route.getCanonicalUrl() );
                
                RemoteRepository mirroredRepo = session.getMirrorSelector().getMirror( repo );
                if ( mirroredRepo != null )
                {
                    repo = mirroredRepo;
                }
                
                Authentication auth = session.getAuthenticationSelector().getAuthentication( repo );
                repo.setAuthentication( auth );
                
                Proxy proxy = session.getProxySelector().getProxy( repo );
                repo.setProxy( proxy );
            }
            
            RepositoryConnector connector;
            try
            {
                connector = new WagonRepositoryConnector( wagonProvider, wagonConfigurator, repo, session, fileProcessor,
                                                     logger );
                connectors.put( route, connector );
            }
            catch ( NoRepositoryConnectorException e )
            {
                logger.warn( "Cannot find Wagon for: " + repo.getUrl(), e );
                if ( session.getTransferListener() != null )
                {
                    DefaultTransferEvent event = new DefaultTransferEvent();
                    event.setResource( new DefaultTransferResource( repo.getUrl(), name, file, trace ) );
                    event.setRequestType( TransferEvent.RequestType.GET );
                    event.setType( TransferEvent.EventType.FAILED );
                    event.setException( e );

                    session.getTransferListener().transferFailed( event );
                }
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private synchronized Map<GroupRoute, RepositoryConnector> getOpenConnectors()
    {
        Map<GroupRoute, RepositoryConnector> connectors =
            (Map<GroupRoute, RepositoryConnector>) session.getData().get( OPEN_CONNECTORS_KEY );

        if ( connectors == null )
        {
            connectors = new HashMap<GroupRoute, RepositoryConnector>();
            session.getData().set( OPEN_CONNECTORS_KEY, connectors );
        }

        return connectors;
    }
    
    private void setOpenConnectors( Map<GroupRoute, RepositoryConnector> connectors  )
    {
        session.getData().set( OPEN_CONNECTORS_KEY, connectors );
    }

}
