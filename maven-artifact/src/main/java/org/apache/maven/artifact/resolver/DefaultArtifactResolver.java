package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.AbstractArtifactComponent;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.TransferFailedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ArtifactSourceReader to ArtifactMetadataReader
// Possibly give the resolver the metadata reader
// resolver.setMetadataReader( foo )
// resolver.setRepositories( Set )
// resolveTransitively instead of parameter

public class DefaultArtifactResolver
    extends AbstractArtifactComponent
    implements ArtifactResolver
{
    private WagonManager wagonManager;

    public Artifact resolve( Artifact artifact,
                             Set remoteRepositories,
                             ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        // ----------------------------------------------------------------------
        // Perform any transformation on the artifacts
        // ----------------------------------------------------------------------

        // ----------------------------------------------------------------------
        // Check for the existence of the artifact in the specified local
        // ArtifactRepository. If it is present then simply return as the request
        // for resolution has been satisfied.
        // ----------------------------------------------------------------------

        setLocalRepositoryPath( artifact, localRepository );

        if ( artifact.exists() )
        {
            return artifact;
        }

        try
        {
            wagonManager.get( artifact, remoteRepositories, localRepository );
        }
        catch ( TransferFailedException e )
        {
            throw new ArtifactResolutionException( artifactNotFound( artifact,
                                                                     remoteRepositories,
                                                                     localRepository ), e );
        }

        return artifact;
    }

    private String artifactNotFound( Artifact artifact,
                                     Set remoteRepositories,
                                     ArtifactRepository localRepository )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "The artifact is not present locally as:" )
            .append( "\n" )
            .append( artifact.getPath() )
            .append( "\n" )
            .append( "or in any of the specified remote repositories:" )
            .append( "\n" );

        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

            sb.append( remoteRepository.getUrl() );
        }

        return sb.toString();
    }

    public Set resolve( Set artifacts,
                        Set remoteRepositories,
                        ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        Set resolvedArtifacts = new HashSet();

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            Artifact resolvedArtifact = resolve( artifact, remoteRepositories, localRepository );

            resolvedArtifacts.add( resolvedArtifact );
        }

        return resolvedArtifacts;
    }

    // ----------------------------------------------------------------------
    // Transitive modes
    // ----------------------------------------------------------------------

    public ArtifactResolutionResult resolveTransitively( Set artifacts,
                                                         Set remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        ArtifactResolutionResult artifactResolutionResult;

        try
        {
            artifactResolutionResult = collect( artifacts,
                                                localRepository,
                                                remoteRepositories,
                                                source );
        }
        catch ( TransitiveArtifactResolutionException e )
        {
            throw new ArtifactResolutionException( "Error transitively resolving artifacts: ", e );
        }

        for ( Iterator i = artifactResolutionResult.getArtifacts().values().iterator(); i.hasNext(); )
        {
            resolve( (Artifact) i.next(), remoteRepositories, localRepository );
        }

        return artifactResolutionResult;
    }

    public ArtifactResolutionResult resolveTransitively( Artifact artifact,
                                                         Set remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        Set s = new HashSet();

        s.add( artifact );

        return resolveTransitively( s, remoteRepositories, localRepository, source );
    }


    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public ArtifactResolutionResult collect( Set artifacts,
                                             ArtifactRepository localRepository,
                                             Set remoteRepositories,
                                             ArtifactMetadataSource source )
        throws TransitiveArtifactResolutionException
    {
        ArtifactResolutionResult result = new ArtifactResolutionResult();

        Map resolvedArtifacts = new HashMap();

        List queue = new LinkedList();

        queue.add( artifacts );

        while ( !queue.isEmpty() )
        {
            Set currentArtifacts = (Set) queue.remove( 0 );

            for ( Iterator i = currentArtifacts.iterator(); i.hasNext(); )
            {
                Artifact newArtifact = (Artifact) i.next();

                String id = newArtifact.getConflictId();

                if ( resolvedArtifacts.containsKey( id ) )
                {
                    Artifact knownArtifact = (Artifact) resolvedArtifacts.get( id );

                    String newVersion = newArtifact.getVersion();

                    String knownVersion = knownArtifact.getVersion();

                    if ( !newVersion.equals( knownVersion ) )
                    {
                        addConflict( result, knownArtifact, newArtifact );
                    }
                }
                else
                {
                    //It's the first time we have encountered this artifact
                    resolvedArtifacts.put( id, newArtifact );

                    Set referencedDependencies = null;

                    try
                    {
                        referencedDependencies = source.retrieve( newArtifact );
                    }
                    catch ( ArtifactMetadataRetrievalException e )
                    {
                        throw new TransitiveArtifactResolutionException( "Error retrieving metadata: ", e );
                    }

                    // the pom for given dependency exisit we will add it to the queue
                    queue.add( referencedDependencies );
                }
            }
        }

        // the dependencies list is keyed by groupId+artifactId+type
        // so it must be 'rekeyed' to the complete id: groupId+artifactId+type+version

        Map artifactResult = result.getArtifacts();

        for ( Iterator it = resolvedArtifacts.values().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            setLocalRepositoryPath( artifact, localRepository );

            artifactResult.put( artifact.getId(), artifact );
        }

        return result;
    }

    private void addConflict( ArtifactResolutionResult result, Artifact knownArtifact, Artifact newArtifact )
    {
        List conflicts;

        conflicts = (List) result.getConflicts().get( newArtifact.getConflictId() );

        if ( conflicts == null )
        {
            conflicts = new LinkedList();

            conflicts.add( knownArtifact );

            result.getConflicts().put( newArtifact.getConflictId(), conflicts );
        }

        conflicts.add( newArtifact );
    }
}
