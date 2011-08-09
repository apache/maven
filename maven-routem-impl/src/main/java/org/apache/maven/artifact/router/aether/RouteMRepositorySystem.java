package org.apache.maven.artifact.router.aether;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.router.RoutingRequestConverter;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.SyncContext;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeployResult;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallResult;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.resolution.VersionRequest;
import org.sonatype.aether.resolution.VersionResolutionException;
import org.sonatype.aether.resolution.VersionResult;
import org.sonatype.aether.util.graph.FilteringDependencyVisitor;

@Component( role = RepositorySystem.class, hint = "default" )
public class RouteMRepositorySystem
    implements RepositorySystem
{

    @Requirement( hint = "aether-default" )
    private RepositorySystem delegate;

    @Requirement
    private RoutingRequestConverter routeConverter;

    public CollectResult collectDependencies( RepositorySystemSession session, CollectRequest request )
        throws DependencyCollectionException
    {
        return delegate.collectDependencies( session, request );
    }

    public InstallResult install( RepositorySystemSession session, InstallRequest request )
        throws InstallationException
    {
        return delegate.install( session, request );
    }

    public DeployResult deploy( RepositorySystemSession session, DeployRequest request )
        throws DeploymentException
    {
        return delegate.deploy( session, request );
    }

    public LocalRepositoryManager newLocalRepositoryManager( LocalRepository localRepository )
    {
        return delegate.newLocalRepositoryManager( localRepository );
    }

    public SyncContext newSyncContext( RepositorySystemSession session, boolean shared )
    {
        return delegate.newSyncContext( session, shared );
    }

    public VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
        throws VersionRangeResolutionException
    {
        routeConverter.convert( request, session );

        return delegate.resolveVersionRange( session, request );
    }

    public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
        throws VersionResolutionException
    {
        routeConverter.convert( request, session );

        return delegate.resolveVersion( session, request );
    }

    public ArtifactDescriptorResult readArtifactDescriptor( RepositorySystemSession session,
                                                            ArtifactDescriptorRequest request )
        throws ArtifactDescriptorException
    {
        routeConverter.convert( request, session );

        return delegate.readArtifactDescriptor( session, request );
    }

    public ArtifactResult resolveArtifact( RepositorySystemSession session, ArtifactRequest request )
        throws ArtifactResolutionException
    {
        routeConverter.convert( request, session );

        return delegate.resolveArtifact( session, request );
    }

    public List<ArtifactResult> resolveArtifacts( RepositorySystemSession session,
                                                  Collection<? extends ArtifactRequest> requests )
        throws ArtifactResolutionException
    {
        routeConverter.convert( requests, session );

        return delegate.resolveArtifacts( session, requests );
    }

    public DependencyResult resolveDependencies( RepositorySystemSession session, DependencyRequest request )
        throws DependencyResolutionException
    {
        DependencyResult result = new DependencyResult( request );
        
        DependencyNode root;
        CollectRequest collectRequest = request.getCollectRequest();
        if ( collectRequest != null )
        {
            CollectResult collectResult;
            try
            {
                collectResult = collectDependencies( session, request.getCollectRequest() );
            }
            catch ( DependencyCollectionException e )
            {
                result.setCollectExceptions( e.getResult().getExceptions() );
                throw new DependencyResolutionException( result, e );
            }
            
            root = collectResult.getRoot();
        }
        else
        {
            root = request.getRoot();
        }
        
        result.setRoot( root );
        
        List<ArtifactResult> artifactResults;
        try
        {
            artifactResults = resolveDependencies( session, root, request.getFilter() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new DependencyResolutionException( result, e );
        }
        
        result.setArtifactResults( artifactResults );
        return result;
    }

    public List<ArtifactResult> resolveDependencies( RepositorySystemSession session, DependencyNode node,
                                                     DependencyFilter filter )
        throws ArtifactResolutionException
    {
        Set<ArtifactRequest> requests = new LinkedHashSet<ArtifactRequest>();
        Collection<DependencyNode> dependencies = traverse( node, filter );
        for ( DependencyNode dn : dependencies )
        {
            requests.add( new ArtifactRequest( dn ) );
        }
        
        return resolveArtifacts( session, requests );
    }

    public List<ArtifactResult> resolveDependencies( RepositorySystemSession session, CollectRequest request,
                                                     DependencyFilter filter )
        throws DependencyCollectionException, ArtifactResolutionException
    {
        DependencyRequest req = new DependencyRequest( request, filter );
        DependencyResult res;
        try
        {
            res = resolveDependencies( session, req );
        }
        catch ( DependencyResolutionException e )
        {
            Throwable cause = e.getCause();
            
            //NOTE: From our implementation above, only these two are possible!
            if ( cause instanceof ArtifactResolutionException )
            {
                throw (ArtifactResolutionException) cause;
            }
            else
            {
                throw (DependencyCollectionException ) cause;
            }
        }
        
        return res.getArtifactResults();
    }

    public List<MetadataResult> resolveMetadata( RepositorySystemSession session,
                                                 Collection<? extends MetadataRequest> requests )
    {
        Collection<? extends MetadataRequest> mappedRequests = routeConverter.mapMetadataRequests( requests, session );
        
        return delegate.resolveMetadata( session, mappedRequests );
    }
    
    private Set<DependencyNode> traverse( DependencyNode root, DependencyFilter filter )
    {
        FlatteningVisitor visitor = new FlatteningVisitor();
        root.accept( new FilteringDependencyVisitor( visitor, filter ) ); 
        
        return visitor.nodes;
    }

    private static final class FlatteningVisitor implements DependencyVisitor
    {
        Set<DependencyNode> nodes = new LinkedHashSet<DependencyNode>();

        public boolean visitEnter( DependencyNode node )
        {
            nodes.add( node );
            return true;
        }

        public boolean visitLeave( DependencyNode node )
        {
            return true;
        }
        
    }

}
