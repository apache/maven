package org.apache.maven.artifact.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.wagon.events.TransferListener;

/**
 * A resolution request allows you to either use an existing MavenProject, or a coordinate (gid:aid:version)
 * to process a POMs dependencies.
 *
 * @author Jason van Zyl
 */
public class ArtifactResolutionRequest
{
    private Artifact artifact;

    // Needs to go away
    private Set<Artifact> artifactDependencies;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    // Not sure what to do with this?
    // Scope
    // Lock down lists
    private ArtifactFilter filter;

    // Needs to go away
    private List<ResolutionListener> listeners = new ArrayList<ResolutionListener>();

    // This is like a filter but overrides all transitive versions 
    private Map managedVersionMap;

    // This should not be in here, it's a component
    private ArtifactMetadataSource metadataSource;

    private TransferListener transferListener;
    
    private boolean resolveRoot = true;

    private boolean resolveTransitively = false;
        
    public Artifact getArtifact()
    {
        return artifact;
    }

    public ArtifactResolutionRequest setArtifact( Artifact artifact )
    {
        this.artifact = artifact;

        return this;
    }

    public ArtifactResolutionRequest setArtifactDependencies( Set<Artifact> artifactDependencies )
    {
        this.artifactDependencies = artifactDependencies;

        return this;
    }

    public Set<Artifact> getArtifactDependencies()
    {
        return artifactDependencies;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public ArtifactResolutionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public List<ArtifactRepository> getRemoteRepostories()
    {
        return remoteRepositories;
    }

    public ArtifactResolutionRequest setRemoteRepostories( List<ArtifactRepository> remoteRepostories )
    {
        this.remoteRepositories = remoteRepostories;

        return this;
    }

    public ArtifactFilter getFilter()
    {
        return filter;
    }

    public ArtifactResolutionRequest setFilter( ArtifactFilter filter )
    {
        this.filter = filter;

        return this;
    }

    public List<ResolutionListener> getListeners()
    {
        return listeners;
    }

    public ArtifactResolutionRequest setListeners( List<ResolutionListener> listeners )
    {        
        this.listeners = listeners;
        
        return this;
    }
    
    public ArtifactResolutionRequest addListener( ResolutionListener listener )
    {
        listeners.add( listener );

        return this;
    }

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    public ArtifactMetadataSource getMetadataSource()
    {
        return metadataSource;
    }

    public ArtifactResolutionRequest setMetadataSource( ArtifactMetadataSource metadataSource )
    {
        this.metadataSource = metadataSource;

        return this;
    }

    public Map getManagedVersionMap()
    {
        return managedVersionMap;
    }

    public ArtifactResolutionRequest setManagedVersionMap( Map managedVersionMap )
    {
        this.managedVersionMap = managedVersionMap;

        return this;
    }

    public ArtifactResolutionRequest setResolveRoot( boolean resolveRoot )
    {
        this.resolveRoot = resolveRoot;
        
        return this;
    }
    
    public boolean isResolveRoot()
    {
        return resolveRoot;
    }        

    public ArtifactResolutionRequest setResolveTransitively( boolean resolveDependencies )
    {
        this.resolveTransitively = resolveDependencies;
        
        return this;
    }
    
    public boolean isResolveTransitively()
    {
        return resolveTransitively;
    }        
    
    public TransferListener getTransferListener()
    {
        return transferListener;
    }

    public ArtifactResolutionRequest setTransferListener( TransferListener transferListener )
    {
        this.transferListener = transferListener;
        return this;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer()
                .append( "REQUEST: " ).append(  "\n" )
                .append( "artifact: " ).append( artifact ).append(  "\n" )
                .append( artifactDependencies ).append(  "\n" )
                .append( "localRepository: " ).append(  localRepository ).append(  "\n" )
                .append( "remoteRepositories: " ).append(  remoteRepositories ).append(  "\n" )
                .append( "metadataSource: " ).append(  metadataSource ).append(  "\n" );
        
        return sb.toString();
    }
}
