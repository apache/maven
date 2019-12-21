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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.shared.utils.StringUtils;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.util.ConfigUtils;

/**
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultVersionResolver
    implements VersionResolver, Service
{

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    private static final String RELEASE = "RELEASE";

    private static final String LATEST = "LATEST";

    private static final String SNAPSHOT = "SNAPSHOT";

    private MetadataResolver metadataResolver;

    private SyncContextFactory syncContextFactory;

    private RepositoryEventDispatcher repositoryEventDispatcher;

    public DefaultVersionResolver()
    {
        // enable no-arg constructor
    }

    @Inject
    DefaultVersionResolver( MetadataResolver metadataResolver, SyncContextFactory syncContextFactory,
                            RepositoryEventDispatcher repositoryEventDispatcher )
    {
        setMetadataResolver( metadataResolver );
        setSyncContextFactory( syncContextFactory );
        setRepositoryEventDispatcher( repositoryEventDispatcher );
    }

    public void initService( ServiceLocator locator )
    {
        setMetadataResolver( locator.getService( MetadataResolver.class ) );
        setSyncContextFactory( locator.getService( SyncContextFactory.class ) );
        setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
    }

    public DefaultVersionResolver setMetadataResolver( MetadataResolver metadataResolver )
    {
        this.metadataResolver = Objects.requireNonNull( metadataResolver, "metadataResolver cannot be null" );
        return this;
    }

    public DefaultVersionResolver setSyncContextFactory( SyncContextFactory syncContextFactory )
    {
        this.syncContextFactory = Objects.requireNonNull( syncContextFactory, "syncContextFactory cannot be null" );
        return this;
    }

    public DefaultVersionResolver setRepositoryEventDispatcher( RepositoryEventDispatcher repositoryEventDispatcher )
    {
        this.repositoryEventDispatcher = Objects.requireNonNull( repositoryEventDispatcher,
            "repositoryEventDispatcher cannot be null" );
        return this;
    }

    @SuppressWarnings( "checkstyle:methodlength" )
    public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
        throws VersionResolutionException
    {
        RequestTrace trace = RequestTrace.newChild( request.getTrace(), request );

        Artifact artifact = request.getArtifact();

        String version = artifact.getVersion();

        VersionResult result = new VersionResult( request );

        Key cacheKey = null;
        RepositoryCache cache = session.getCache();
        if ( cache != null && !ConfigUtils.getBoolean( session, false, "aether.versionResolver.noCache" ) )
        {
            cacheKey = new Key( session, request );

            Object obj = cache.get( session, cacheKey );
            if ( obj instanceof Record )
            {
                Record record = (Record) obj;
                result.setVersion( record.version );
                result.setRepository(
                    getRepository( session, request.getRepositories(), record.repoClass, record.repoId ) );
                return result;
            }
        }

        Metadata metadata;

        if ( RELEASE.equals( version ) )
        {
            metadata = new DefaultMetadata( artifact.getGroupId(), artifact.getArtifactId(), MAVEN_METADATA_XML,
                                            Metadata.Nature.RELEASE );
        }
        else if ( LATEST.equals( version ) )
        {
            metadata = new DefaultMetadata( artifact.getGroupId(), artifact.getArtifactId(), MAVEN_METADATA_XML,
                                            Metadata.Nature.RELEASE_OR_SNAPSHOT );
        }
        else if ( version.endsWith( SNAPSHOT ) )
        {
            WorkspaceReader workspace = session.getWorkspaceReader();
            if ( workspace != null && workspace.findVersions( artifact ).contains( version ) )
            {
                metadata = null;
                result.setRepository( workspace.getRepository() );
            }
            else
            {
                metadata =
                    new DefaultMetadata( artifact.getGroupId(), artifact.getArtifactId(), version, MAVEN_METADATA_XML,
                                         Metadata.Nature.SNAPSHOT );
            }
        }
        else
        {
            metadata = null;
        }

        if ( metadata == null )
        {
            result.setVersion( version );
        }
        else
        {
            List<MetadataRequest> metadataReqs = new ArrayList<>( request.getRepositories().size() );

            metadataReqs.add( new MetadataRequest( metadata, null, request.getRequestContext() ) );

            for ( RemoteRepository repository : request.getRepositories() )
            {
                MetadataRequest metadataRequest =
                    new MetadataRequest( metadata, repository, request.getRequestContext() );
                metadataRequest.setDeleteLocalCopyIfMissing( true );
                metadataRequest.setFavorLocalRepository( true );
                metadataRequest.setTrace( trace );
                metadataReqs.add( metadataRequest );
            }

            List<MetadataResult> metadataResults = metadataResolver.resolveMetadata( session, metadataReqs );

            Map<String, VersionInfo> infos = new HashMap<>();

            for ( MetadataResult metadataResult : metadataResults )
            {
                result.addException( metadataResult.getException() );

                ArtifactRepository repository = metadataResult.getRequest().getRepository();
                if ( repository == null )
                {
                    repository = session.getLocalRepository();
                }

                Versioning v = readVersions( session, trace, metadataResult.getMetadata(), repository, result );
                merge( artifact, infos, v, repository );
            }

            if ( RELEASE.equals( version ) )
            {
                resolve( result, infos, RELEASE );
            }
            else if ( LATEST.equals( version ) )
            {
                if ( !resolve( result, infos, LATEST ) )
                {
                    resolve( result, infos, RELEASE );
                }

                if ( result.getVersion() != null && result.getVersion().endsWith( SNAPSHOT ) )
                {
                    VersionRequest subRequest = new VersionRequest();
                    subRequest.setArtifact( artifact.setVersion( result.getVersion() ) );
                    if ( result.getRepository() instanceof RemoteRepository )
                    {
                        RemoteRepository r = (RemoteRepository) result.getRepository();
                        subRequest.setRepositories( Collections.singletonList( r ) );
                    }
                    else
                    {
                        subRequest.setRepositories( request.getRepositories() );
                    }
                    VersionResult subResult = resolveVersion( session, subRequest );
                    result.setVersion( subResult.getVersion() );
                    result.setRepository( subResult.getRepository() );
                    for ( Exception exception : subResult.getExceptions() )
                    {
                        result.addException( exception );
                    }
                }
            }
            else
            {
                String key = SNAPSHOT + getKey( artifact.getClassifier(), artifact.getExtension() );
                merge( infos, SNAPSHOT, key );
                if ( !resolve( result, infos, key ) )
                {
                    result.setVersion( version );
                }
            }

            if ( StringUtils.isEmpty( result.getVersion() ) )
            {
                throw new VersionResolutionException( result );
            }
        }

        if ( cacheKey != null && metadata != null && isSafelyCacheable( session, artifact ) )
        {
            cache.put( session, cacheKey, new Record( result.getVersion(), result.getRepository() ) );
        }

        return result;
    }

    private boolean resolve( VersionResult result, Map<String, VersionInfo> infos, String key )
    {
        VersionInfo info = infos.get( key );
        if ( info != null )
        {
            result.setVersion( info.version );
            result.setRepository( info.repository );
        }
        return info != null;
    }

    private Versioning readVersions( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                     ArtifactRepository repository, VersionResult result )
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
                        try ( final InputStream in = new FileInputStream( metadata.getFile() ) )
                        {
                            versioning = new MetadataXpp3Reader().read( in, false ).getVersioning();

                            /*
                            NOTE: Users occasionally misuse the id "local" for remote repos which screws up the metadata
                            of the local repository. This is especially troublesome during snapshot resolution so we try
                            to handle that gracefully.
                             */
                            if ( versioning != null && repository instanceof LocalRepository
                                     && versioning.getSnapshot() != null
                                     && versioning.getSnapshot().getBuildNumber() > 0 )
                            {
                                final Versioning repaired = new Versioning();
                                repaired.setLastUpdated( versioning.getLastUpdated() );
                                repaired.setSnapshot( new Snapshot() );
                                repaired.getSnapshot().setLocalCopy( true );
                                versioning = repaired;
                                throw new IOException( "Snapshot information corrupted with remote repository data"
                                                           + ", please verify that no remote repository uses the id '"
                                                           + repository.getId() + "'" );

                            }
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

    private void merge( Artifact artifact, Map<String, VersionInfo> infos, Versioning versioning,
                        ArtifactRepository repository )
    {
        if ( StringUtils.isNotEmpty( versioning.getRelease() ) )
        {
            merge( RELEASE, infos, versioning.getLastUpdated(), versioning.getRelease(), repository );
        }

        if ( StringUtils.isNotEmpty( versioning.getLatest() ) )
        {
            merge( LATEST, infos, versioning.getLastUpdated(), versioning.getLatest(), repository );
        }

        for ( SnapshotVersion sv : versioning.getSnapshotVersions() )
        {
            if ( StringUtils.isNotEmpty( sv.getVersion() ) )
            {
                String key = getKey( sv.getClassifier(), sv.getExtension() );
                merge( SNAPSHOT + key, infos, sv.getUpdated(), sv.getVersion(), repository );
            }
        }

        Snapshot snapshot = versioning.getSnapshot();
        if ( snapshot != null && versioning.getSnapshotVersions().isEmpty() )
        {
            String version = artifact.getVersion();
            if ( snapshot.getTimestamp() != null && snapshot.getBuildNumber() > 0 )
            {
                String qualifier = snapshot.getTimestamp() + '-' + snapshot.getBuildNumber();
                version = version.substring( 0, version.length() - SNAPSHOT.length() ) + qualifier;
            }
            merge( SNAPSHOT, infos, versioning.getLastUpdated(), version, repository );
        }
    }

    private void merge( String key, Map<String, VersionInfo> infos, String timestamp, String version,
                        ArtifactRepository repository )
    {
        VersionInfo info = infos.get( key );
        if ( info == null )
        {
            info = new VersionInfo( timestamp, version, repository );
            infos.put( key, info );
        }
        else if ( info.isOutdated( timestamp ) )
        {
            info.version = version;
            info.repository = repository;
            info.timestamp = timestamp;
        }
    }

    private void merge( Map<String, VersionInfo> infos, String srcKey, String dstKey )
    {
        VersionInfo srcInfo = infos.get( srcKey );
        VersionInfo dstInfo = infos.get( dstKey );

        if ( dstInfo == null || ( srcInfo != null && dstInfo.isOutdated( srcInfo.timestamp )
            && srcInfo.repository != dstInfo.repository ) )
        {
            infos.put( dstKey, srcInfo );
        }
    }

    private String getKey( String classifier, String extension )
    {
        return StringUtils.clean( classifier ) + ':' + StringUtils.clean( extension );
    }

    private ArtifactRepository getRepository( RepositorySystemSession session,
                                              List<RemoteRepository> repositories, Class<?> repoClass,
                                              String repoId )
    {
        if ( repoClass != null )
        {
            if ( WorkspaceRepository.class.isAssignableFrom( repoClass ) )
            {
                return session.getWorkspaceReader().getRepository();
            }
            else if ( LocalRepository.class.isAssignableFrom( repoClass ) )
            {
                return session.getLocalRepository();
            }
            else
            {
                for ( RemoteRepository repository : repositories )
                {
                    if ( repoId.equals( repository.getId() ) )
                    {
                        return repository;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafelyCacheable( RepositorySystemSession session, Artifact artifact )
    {
        /*
         * The workspace/reactor is in flux so we better not assume definitive information for any of its
         * artifacts/projects.
         */

        WorkspaceReader workspace = session.getWorkspaceReader();
        if ( workspace == null )
        {
            return true;
        }

        Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifact( artifact );

        return workspace.findArtifact( pomArtifact ) == null;
    }

    private static class VersionInfo
    {

        String timestamp;

        String version;

        ArtifactRepository repository;

        VersionInfo( String timestamp, String version, ArtifactRepository repository )
        {
            this.timestamp = ( timestamp != null ) ? timestamp : "";
            this.version = version;
            this.repository = repository;
        }

        boolean isOutdated( String timestamp )
        {
            return timestamp != null && timestamp.compareTo( this.timestamp ) > 0;
        }

    }

    private static class Key
    {

        private final String groupId;

        private final String artifactId;

        private final String classifier;

        private final String extension;

        private final String version;

        private final String context;

        private final File localRepo;

        private final WorkspaceRepository workspace;

        private final List<RemoteRepository> repositories;

        private final int hashCode;

        Key( RepositorySystemSession session, VersionRequest request )
        {
            Artifact artifact = request.getArtifact();
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
            classifier = artifact.getClassifier();
            extension = artifact.getExtension();
            version = artifact.getVersion();
            localRepo = session.getLocalRepository().getBasedir();
            WorkspaceReader reader = session.getWorkspaceReader();
            workspace = ( reader != null ) ? reader.getRepository() : null;
            repositories = new ArrayList<>( request.getRepositories().size() );
            boolean repoMan = false;
            for ( RemoteRepository repository : request.getRepositories() )
            {
                if ( repository.isRepositoryManager() )
                {
                    repoMan = true;
                    repositories.addAll( repository.getMirroredRepositories() );
                }
                else
                {
                    repositories.add( repository );
                }
            }
            context = repoMan ? request.getRequestContext() : "";

            int hash = 17;
            hash = hash * 31 + groupId.hashCode();
            hash = hash * 31 + artifactId.hashCode();
            hash = hash * 31 + classifier.hashCode();
            hash = hash * 31 + extension.hashCode();
            hash = hash * 31 + version.hashCode();
            hash = hash * 31 + localRepo.hashCode();
            hash = hash * 31 + repositories.hashCode();
            hashCode = hash;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == this )
            {
                return true;
            }
            else if ( obj == null || !getClass().equals( obj.getClass() ) )
            {
                return false;
            }

            Key that = (Key) obj;
            return artifactId.equals( that.artifactId ) && groupId.equals( that.groupId ) && classifier.equals(
                that.classifier ) && extension.equals( that.extension ) && version.equals( that.version )
                && context.equals( that.context ) && localRepo.equals( that.localRepo )
                && Objects.equals( workspace, that.workspace ) && repositories.equals( that.repositories );
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

    }

    private static class Record
    {
        final String version;

        final String repoId;

        final Class<?> repoClass;

        Record( String version, ArtifactRepository repository )
        {
            this.version = version;
            if ( repository != null )
            {
                repoId = repository.getId();
                repoClass = repository.getClass();
            }
            else
            {
                repoId = null;
                repoClass = null;
            }
        }
    }

}
