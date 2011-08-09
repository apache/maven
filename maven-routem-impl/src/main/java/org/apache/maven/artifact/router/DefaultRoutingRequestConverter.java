package org.apache.maven.artifact.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.ArtifactRouterCache;
import org.apache.maven.artifact.router.GroupRoute;
import org.apache.maven.artifact.router.MirrorRoute;
import org.apache.maven.artifact.router.RoutingRequestConverter;
import org.apache.maven.artifact.router.loader.ArtifactRouterLoader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.metadata.Metadata.Nature;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRequest;

@Component( role = RoutingRequestConverter.class )
public class DefaultRoutingRequestConverter
    implements RoutingRequestConverter
{

    @Requirement
    private ArtifactRouterLoader routerLoader;

    public void convert( final VersionRangeRequest request, final RepositorySystemSession session )
    {
        Artifact artifact = request.getArtifact();
        List<RemoteRepository> repos = collectRoutingRepos( artifact, session, request.getRepositories() );
        request.setRepositories( repos );
    }

    public void convert( VersionRequest request, RepositorySystemSession session )
    {
        Artifact artifact = request.getArtifact();
        List<RemoteRepository> repos = collectRoutingRepos( artifact, session, request.getRepositories() );
        request.setRepositories( repos );
    }

    public void convert( ArtifactDescriptorRequest request, RepositorySystemSession session )
    {
        Artifact artifact = request.getArtifact();
        List<RemoteRepository> repos = collectRoutingRepos( artifact, session, request.getRepositories() );
        request.setRepositories( repos );
    }

    public void convert( ArtifactRequest request, RepositorySystemSession session )
    {
        Artifact artifact = request.getArtifact();
        List<RemoteRepository> repos = collectRoutingRepos( artifact, session, request.getRepositories() );
        request.setRepositories( repos );
    }

    public void convert( Collection<? extends ArtifactRequest> requests, RepositorySystemSession session )
    {
        for ( ArtifactRequest request : requests )
        {
            Artifact artifact = request.getArtifact();
            List<RemoteRepository> repos = collectRoutingRepos( artifact, session, request.getRepositories() );
            request.setRepositories( repos );
        }
    }

    public Collection<? extends MetadataRequest> mapMetadataRequests( Collection<? extends MetadataRequest> requests,
                                                                      RepositorySystemSession session )
    {
        Map<MetadataKey, MetadataRequest> metadataMap = new LinkedHashMap<MetadataKey, MetadataRequest>();
        for ( MetadataRequest mdr : requests )
        {
            MetadataKey mdk = new MetadataKey( mdr.getMetadata() );

            if ( !metadataMap.containsKey( mdk ) )
            {
                metadataMap.put( mdk, mdr );
            }
        }

        List<MetadataRequest> result = new ArrayList<MetadataRequest>();
        for ( MetadataRequest req : metadataMap.values() )
        {
            Metadata md = req.getMetadata();

            List<RemoteRepository> repos = collectRoutingRepos( md, session, req.getRepository() );
            for ( RemoteRepository repo : repos )
            {
                result.add( new MetadataRequest( md, repo, req.getRequestContext() ) );
            }
        }

        return result;
    }

    private List<RemoteRepository> collectRoutingRepos( Metadata md, RepositorySystemSession session,
                                                        RemoteRepository originalRepo )
    {
        Nature nature = md.getNature();
        boolean includeReleases = nature == Nature.RELEASE || nature == Nature.RELEASE_OR_SNAPSHOT;
        boolean includeSnapshots = nature == Nature.SNAPSHOT || nature == Nature.RELEASE_OR_SNAPSHOT;

        return collectRoutingRepos( md.getGroupId(), session, Collections.singletonList( originalRepo ),
                                    includeReleases, includeSnapshots );
    }

    private List<RemoteRepository> collectRoutingRepos( Artifact artifact, RepositorySystemSession session,
                                                        List<RemoteRepository> originalRepos )
    {
        boolean includeReleases = false;
        boolean includeSnapshots = false;
        for ( RemoteRepository repo : originalRepos )
        {
            if ( repo.getPolicy( false ).isEnabled() )
            {
                includeReleases = true;
            }
            else if ( repo.getPolicy( true ).isEnabled() )
            {
                includeSnapshots = true;
            }

            if ( includeSnapshots && includeReleases )
            {
                break;
            }
        }

        return collectRoutingRepos( artifact.getGroupId(), session, originalRepos, includeSnapshots, includeReleases );
    }

    private synchronized List<RemoteRepository> collectRoutingRepos( String groupId, RepositorySystemSession session,
                                                        List<RemoteRepository> originalRepos, boolean includeSnapshots,
                                                        boolean includeReleases )
    {
        ArtifactRouter router = getRouter( session );
        ArtifactRouterCache cache = getRouterCache( session );
        GroupRoute group = router.getGroup( groupId );

        RemoteRepository snaps = null;
        if ( includeSnapshots )
        {
            snaps = getGroupRepo( group, true, cache, router );
        }

        RemoteRepository release = null;
        if ( includeReleases )
        {
            release = getGroupRepo( group, false, cache, router );
        }
        
        List<RemoteRepository> repos = consolidateRepositories( snaps, release, originalRepos );
        
        return repos;
    }

    private List<RemoteRepository> consolidateRepositories( RemoteRepository snaps, RemoteRepository release,
                                                            List<RemoteRepository> originalRepos )
    {
        // Consolidate created repos with supplied repos
        // - map urls represented by created group repos
        //   * include direct url
        //   * include mirrored repos
        //   * separate between snaps mirrors and release mirrors
        // - iterate supplied repos
        //   * if url mirrored by created snapshot repo, eliminate from results
        //     > if releases also allowed in this repo, enable releases in created snapshot repo
        //   * if url mirrored by created release repo, eliminate from results
        //     > if snapshots also allowed in this repo, enable snapshots in created release repo

        Set<String> releaseMirrors = new HashSet<String>();
        releaseMirrors.add( release.getUrl() );
        if ( release.getMirroredRepositories() != null )
        {
            for ( RemoteRepository repo : release.getMirroredRepositories() )
            {
                releaseMirrors.add( repo.getUrl() );
            }
        }
        
        List<RemoteRepository> results = new ArrayList<RemoteRepository>();
        Set<String> captured = new HashSet<String>();
        
        results.add( release );
        captured.add( release.getUrl() );

        Set<String> snapsMirrors = new HashSet<String>();
        if ( snaps != release )
        {
            snapsMirrors.add( snaps.getUrl() );
            if ( snaps.getMirroredRepositories() != null )
            {
                for ( RemoteRepository repo : snaps.getMirroredRepositories() )
                {
                    snapsMirrors.add( repo.getUrl() );
                }
            }
            
            results.add( snaps );
            captured.add( snaps.getUrl() );
        }

        for ( RemoteRepository repo : originalRepos )
        {
            if ( releaseMirrors.contains( repo.getUrl() ) )
            {
                RepositoryPolicy snapshotPolicy = repo.getPolicy( true );
                RepositoryPolicy relSnapshotPolicy = release.getPolicy( true );
                
                if ( relSnapshotPolicy != null && !relSnapshotPolicy.isEnabled() && snapshotPolicy != null
                    && snapshotPolicy.isEnabled() )
                {
                    relSnapshotPolicy.setEnabled( true );
                }
            }
            else if ( snapsMirrors.contains( repo.getUrl() ) )
            {
                RepositoryPolicy releasePolicy = repo.getPolicy( false );
                RepositoryPolicy snapReleasePolicy = snaps.getPolicy( false );
                
                if ( snapReleasePolicy != null && !snapReleasePolicy.isEnabled() && releasePolicy != null
                    && releasePolicy.isEnabled() )
                {
                    snapReleasePolicy.setEnabled( true );
                }
            }
            else if ( !captured.contains( repo.getUrl() ) )
            {
                results.add( repo );
                captured.add( repo.getUrl() );
            }
        }
        
        return results;
    }

    private RemoteRepository getGroupRepo( GroupRoute group, boolean snapshots, ArtifactRouterCache cache, ArtifactRouter router )
    {
        RemoteRepository repo = cache.getGroupRepository( group, snapshots );
        if ( repo == null )
        {
            MirrorRoute mirror =
                router.selectSingleMirror( snapshots ? group.getCanonicalSnapshotsUrl()
                                : group.getCanonicalReleasesUrl() );

            repo = new RemoteRepository();

            repo.setRepositoryManager( false );
            repo.setId( mirror.getId() );
            repo.setUrl( mirror.getRouteUrl() );

            // FIXME: repository policy should probably come from globals, NOT from server-specified mirror config!
            repo.setPolicy( snapshots, mirror.toRepositoryPolicy() );
            
            Set<String> mirrorOfUrls = mirror.getMirrorOfUrls();
            if ( mirrorOfUrls != null )
            {
                List<RemoteRepository> mirrorOf = new ArrayList<RemoteRepository>( mirrorOfUrls.size() );
                for ( String url : mirrorOfUrls )
                {
                    RemoteRepository r = new RemoteRepository();
                    r.setUrl( url );
                    mirrorOf.add( r );
                }
                
                repo.setMirroredRepositories( mirrorOf );
            }
            
            cache.setGroupRepository( group, snapshots, repo );
        }

        return repo;
    }

    private synchronized ArtifactRouterCache getRouterCache( RepositorySystemSession session )
    {
        ArtifactRouterCache cache = (ArtifactRouterCache) session.getData().get( ArtifactRouterCache.SESSION_KEY );
        if ( cache == null )
        {
            cache = new ArtifactRouterCache();
            session.getData().set( ArtifactRouterCache.SESSION_KEY, cache );
        }

        return cache;
    }

    private synchronized ArtifactRouter getRouter( RepositorySystemSession session )
    {
        ArtifactRouter router = (ArtifactRouter) session.getData().get( ArtifactRouter.SESSION_KEY );
        if ( router == null )
        {
            router = routerLoader.loadDefault();
            session.getData().set( ArtifactRouter.SESSION_KEY, router );
        }

        return router;
    }

    // private static final class MetadataKey
    // {
    // private final String gid;
    //
    // private final String url;
    //
    // MetadataKey( Metadata metadata, RemoteRepository repo )
    // {
    // this.gid = metadata.getGroupId();
    // this.url = repo.getUrl();
    // }
    //
    // @Override
    // public int hashCode()
    // {
    // final int prime = 31;
    // int result = 1;
    // result = prime * result + ( ( gid == null ) ? 0 : gid.hashCode() );
    // result = prime * result + ( ( url == null ) ? 0 : url.hashCode() );
    // return result;
    // }
    //
    // @Override
    // public boolean equals( Object obj )
    // {
    // if ( this == obj )
    // return true;
    // if ( obj == null )
    // return false;
    // if ( getClass() != obj.getClass() )
    // return false;
    // MetadataKey other = (MetadataKey) obj;
    // if ( gid == null )
    // {
    // if ( other.gid != null )
    // return false;
    // }
    // else if ( !gid.equals( other.gid ) )
    // return false;
    // if ( url == null )
    // {
    // if ( other.url != null )
    // return false;
    // }
    // else if ( !url.equals( other.url ) )
    // return false;
    // return true;
    // }
    // }
    //
    private static final class MetadataKey
    {
        private final Metadata md;

        MetadataKey( Metadata md )
        {
            this.md = md;
        }

        public int hashCode()
        {
            final int prime = 13;
            int result = 1;
            result = prime * result + md.getGroupId().hashCode();
            result = prime * result + md.getArtifactId().hashCode();
            result = prime * result + md.getVersion().hashCode();
            result = prime * result + md.getType().hashCode();
            result = prime * result + md.getNature().hashCode();
            return result;
        }

        public boolean equals( Object other )
        {
            if ( other == this )
            {
                return true;
            }

            if ( !( other instanceof MetadataKey ) )
            {
                return false;
            }

            MetadataKey om = (MetadataKey) other;
            return md.getGroupId().equals( om.md.getGroupId() ) && md.getArtifactId().equals( om.md.getArtifactId() )
                && md.getVersion().equals( om.md.getVersion() ) && md.getType().equals( om.md.getType() )
                && md.getNature() == om.md.getNature();
        }
    }

}
