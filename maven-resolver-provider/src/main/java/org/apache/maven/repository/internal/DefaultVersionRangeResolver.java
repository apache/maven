package org.apache.maven.repository.internal;

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

import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.sisu.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Benjamin Bentmann
 */
@Named
@Singleton
@Component( role = VersionRangeResolver.class, hint = "default" )
public class DefaultVersionRangeResolver
    implements VersionRangeResolver, Service
{

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    @SuppressWarnings( "unused" )
    @Requirement( role = LoggerFactory.class )
    private Logger logger = NullLoggerFactory.LOGGER;

    @Requirement
    private MetadataResolver metadataResolver;

    @Requirement
    private SyncContextFactory syncContextFactory;

    @Requirement
    private RepositoryEventDispatcher repositoryEventDispatcher;

    @Requirement( role = VersionRangeResultFilter.class, optional = true )
    private VersionRangeResultFilter versionRangeResultFilter = new DefaultVersionRangeResultFilter();

    public DefaultVersionRangeResolver()
    {
        // enable default constructor
    }

    @Inject
    DefaultVersionRangeResolver( MetadataResolver metadataResolver, SyncContextFactory syncContextFactory,
                                 RepositoryEventDispatcher repositoryEventDispatcher, LoggerFactory loggerFactory,
                                 @Nullable VersionRangeResultFilter versionRangeResultFilter )
    {
        setMetadataResolver( metadataResolver );
        setSyncContextFactory( syncContextFactory );
        setLoggerFactory( loggerFactory );
        setRepositoryEventDispatcher( repositoryEventDispatcher );
        if ( versionRangeResultFilter != null )
        {
            setVersionRangeResultFilter( versionRangeResultFilter );
        }
    }

    public void initService( ServiceLocator locator )
    {
        setLoggerFactory( locator.getService( LoggerFactory.class ) );
        setMetadataResolver( locator.getService( MetadataResolver.class ) );
        setSyncContextFactory( locator.getService( SyncContextFactory.class ) );
        setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
        final VersionRangeResultFilter versionRangeResultFilter = locator.getService( VersionRangeResultFilter.class );
        if ( versionRangeResultFilter != null )
        {
            setVersionRangeResultFilter( versionRangeResultFilter );
        }
    }

    public DefaultVersionRangeResolver setLoggerFactory( LoggerFactory loggerFactory )
    {
        this.logger = NullLoggerFactory.getSafeLogger( loggerFactory, getClass() );
        return this;
    }

    void setLogger( LoggerFactory loggerFactory )
    {
        // plexus support
        setLoggerFactory( loggerFactory );
    }

    public DefaultVersionRangeResolver setMetadataResolver( MetadataResolver metadataResolver )
    {
        this.metadataResolver = Validate.notNull( metadataResolver, "metadataResolver cannot be null" );
        return this;
    }

    public DefaultVersionRangeResolver setSyncContextFactory( SyncContextFactory syncContextFactory )
    {
        this.syncContextFactory = Validate.notNull( syncContextFactory, "syncContextFactory cannot be null" );
        return this;
    }

    public DefaultVersionRangeResolver setRepositoryEventDispatcher(
        RepositoryEventDispatcher repositoryEventDispatcher )
    {
        this.repositoryEventDispatcher = Validate.notNull( repositoryEventDispatcher,
            "repositoryEventDispatcher cannot be null" );
        return this;
    }

    public DefaultVersionRangeResolver setVersionRangeResultFilter( VersionRangeResultFilter versionRangeResultFilter )
    {
        this.versionRangeResultFilter = Validate.notNull( versionRangeResultFilter,
                "versionRangeResultFilter cannot be null" );
        return this;
    }

    public VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
        throws VersionRangeResolutionException
    {
        VersionRangeResult result = new VersionRangeResult( request );

        VersionScheme versionScheme = new GenericVersionScheme();

        VersionConstraint versionConstraint;
        try
        {
            versionConstraint = versionScheme.parseVersionConstraint( request.getArtifact().getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            result.addException( e );
            throw new VersionRangeResolutionException( result );
        }

        result.setVersionConstraint( versionConstraint );

        if ( versionConstraint.getRange() == null )
        {
            result.addVersion( versionConstraint.getVersion() );
        }
        else
        {
            Map<String, ArtifactRepository> versionIndex = getVersions( session, result, request );

            List<Version> versions = new ArrayList<>();
            for ( Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet() )
            {
                try
                {
                    Version ver = versionScheme.parseVersion( v.getKey() );
                    if ( versionConstraint.containsVersion( ver ) )
                    {
                        versions.add( ver );
                        result.setRepository( ver, v.getValue() );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    result.addException( e );
                }
            }

            Collections.sort( versions );
            result.setVersions( versions );
        }

        return versionRangeResultFilter.filterVersionRangeResult( result );
    }

    private Map<String, ArtifactRepository> getVersions( RepositorySystemSession session, VersionRangeResult result,
                                                         VersionRangeRequest request )
    {
        RequestTrace trace = RequestTrace.newChild( request.getTrace(), request );

        Map<String, ArtifactRepository> versionIndex = new HashMap<>();

        Metadata metadata =
            new DefaultMetadata( request.getArtifact().getGroupId(), request.getArtifact().getArtifactId(),
                                 MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT );

        List<MetadataRequest> metadataRequests = new ArrayList<>( request.getRepositories().size() );

        metadataRequests.add( new MetadataRequest( metadata, null, request.getRequestContext() ) );

        for ( RemoteRepository repository : request.getRepositories() )
        {
            MetadataRequest metadataRequest = new MetadataRequest( metadata, repository, request.getRequestContext() );
            metadataRequest.setDeleteLocalCopyIfMissing( true );
            metadataRequest.setTrace( trace );
            metadataRequests.add( metadataRequest );
        }

        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata( session, metadataRequests );

        WorkspaceReader workspace = session.getWorkspaceReader();
        if ( workspace != null )
        {
            List<String> versions = workspace.findVersions( request.getArtifact() );
            for ( String version : versions )
            {
                versionIndex.put( version, workspace.getRepository() );
            }
        }

        for ( MetadataResult metadataResult : metadataResults )
        {
            result.addException( metadataResult.getException() );

            ArtifactRepository repository = metadataResult.getRequest().getRepository();
            if ( repository == null )
            {
                repository = session.getLocalRepository();
            }

            Versioning versioning = readVersions( session, trace, metadataResult.getMetadata(), repository, result );
            for ( String version : versioning.getVersions() )
            {
                if ( !versionIndex.containsKey( version ) )
                {
                    versionIndex.put( version, repository );
                }
            }
        }

        return versionIndex;
    }

    private Versioning readVersions( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                     ArtifactRepository repository, VersionRangeResult result )
    {
        Versioning versioning = null;

        try
        {
            if ( metadata != null )
            {
                try ( SyncContext syncContext = syncContextFactory.newInstance( session, true ) )
                {
                    syncContext.acquire( null, Collections.singleton( metadata ) );

                    if ( metadata.getFile() != null && metadata.getFile().exists() )
                    {
                        try ( InputStream in = new FileInputStream( metadata.getFile() ) )
                        {
                            org.apache.maven.artifact.repository.metadata.Metadata m =
                                new MetadataXpp3Reader().read( in, false );

                            versioning = m.getVersioning();
                        }
                    }
                }
            }
        }
        catch ( Exception e )
        {
            invalidMetadata( session, trace, metadata, repository, e );
            result.addException( e );
        }

        return ( versioning != null ) ? versioning : new Versioning();
    }

    private void invalidMetadata( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                  ArtifactRepository repository, Exception exception )
    {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder( session, EventType.METADATA_INVALID );
        event.setTrace( trace );
        event.setMetadata( metadata );
        event.setException( exception );
        event.setRepository( repository );

        repositoryEventDispatcher.dispatch( event.build() );
    }

}
