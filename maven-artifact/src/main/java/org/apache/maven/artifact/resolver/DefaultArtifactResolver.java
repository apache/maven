package org.apache.maven.artifact.resolver;

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
import org.apache.maven.artifact.construction.ArtifactConstructionSupport;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @todo create an AbstractArtifactResolver that does the transitive boilerplate
 */
public class DefaultArtifactResolver
    extends AbstractLogEnabled
    implements ArtifactResolver
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private WagonManager wagonManager;

    private ArtifactHandlerManager artifactHandlerManager;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    private ArtifactConstructionSupport artifactConstructionSupport = new ArtifactConstructionSupport();

    public Artifact resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        // ----------------------------------------------------------------------
        // Perform any transformation on the artifacts
        // ----------------------------------------------------------------------

        // ----------------------------------------------------------------------
        // Check for the existence of the artifact in the specified local
        // ArtifactRepository. If it is present then simply return as the
        // request
        // for resolution has been satisfied.
        // ----------------------------------------------------------------------

        try
        {
            Logger logger = getLogger();
            logger.debug( "Resolving: " + artifact.getId() + " from:\n" + "{localRepository: " + localRepository +
                          "}\n" + "{remoteRepositories: " + remoteRepositories + "}" );

            artifact.setPath( artifactHandlerManager.getLocalRepositoryArtifactPath( artifact, localRepository ) );

            if ( artifact.exists() )
            {
                return artifact;
            }

            wagonManager.get( artifact, remoteRepositories, localRepository );
        }
        catch ( TransferFailedException e )
        {
            throw new ArtifactResolutionException( artifactNotFound( artifact, remoteRepositories ), e );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactResolutionException( "Error resolving artifact: ", e );
        }

        return artifact;
    }

    private static final String LS = System.getProperty( "line.separator" );

    private String artifactNotFound( Artifact artifact, List remoteRepositories )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "The artifact is not present locally as:" ).append( LS ).append( LS ).append( artifact.getPath() ).append(
            LS ).append( LS ).append( "or in any of the specified remote repositories:" ).append( LS ).append( LS );

        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

            sb.append( remoteRepository.getUrl() );
            if ( i.hasNext() )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }

    public Set resolve( Set artifacts, List remoteRepositories, ArtifactRepository localRepository )
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

    public ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException
    {
        ArtifactResolutionResult artifactResolutionResult;

        try
        {
            artifactResolutionResult = collect( artifacts, localRepository, remoteRepositories, source, filter );
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

    public ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return resolveTransitively( artifacts, remoteRepositories, localRepository, source, null );
    }

    public ArtifactResolutionResult resolveTransitively( Artifact artifact, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return resolveTransitively( Collections.singleton( artifact ), remoteRepositories, localRepository, source );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private ArtifactResolutionResult collect( Set artifacts, ArtifactRepository localRepository,
                                              List remoteRepositories, ArtifactMetadataSource source,
                                              ArtifactFilter filter )
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

                    // TODO: scope handler
                    boolean updateScope = false;
                    if ( Artifact.SCOPE_RUNTIME.equals( newArtifact.getScope() ) &&
                        Artifact.SCOPE_TEST.equals( knownArtifact.getScope() ) )
                    {
                        updateScope = true;
                    }

                    if ( Artifact.SCOPE_COMPILE.equals( newArtifact.getScope() ) &&
                        !Artifact.SCOPE_COMPILE.equals( knownArtifact.getScope() ) )
                    {
                        updateScope = true;
                    }

                    if ( updateScope )
                    {
                        // TODO: Artifact factory?
                        // TODO: [jc] Is this a better way to centralize artifact construction here?
                        Artifact artifact = artifactConstructionSupport.createArtifact( knownArtifact.getGroupId(),
                                                                                        knownArtifact.getArtifactId(),
                                                                                        knownVersion,
                                                                                        newArtifact.getScope(),
                                                                                        knownArtifact.getType() );
                        resolvedArtifacts.put( artifact.getConflictId(), artifact );
                    }
                }
                else
                {
                    // ----------------------------------------------------------------------
                    // It's the first time we have encountered this artifact
                    // ----------------------------------------------------------------------

                    if ( filter != null && !filter.include( newArtifact ) )
                    {
                        continue;
                    }

                    resolvedArtifacts.put( id, newArtifact );

                    Set referencedDependencies = null;

                    try
                    {
                        referencedDependencies = source.retrieve( newArtifact, localRepository, remoteRepositories );
                    }
                    catch ( ArtifactMetadataRetrievalException e )
                    {
                        throw new TransitiveArtifactResolutionException( "Error retrieving metadata [" + newArtifact +
                                                                         "] : ", e );
                    }

                    // the pom for given dependency exisit we will add it to the
                    // queue
                    queue.add( referencedDependencies );
                }
            }
        }

        // ----------------------------------------------------------------------
        // the dependencies list is keyed by groupId+artifactId+type
        // so it must be 'rekeyed' to the complete id:
        // groupId+artifactId+type+version
        // ----------------------------------------------------------------------

        Map artifactResult = result.getArtifacts();

        for ( Iterator it = resolvedArtifacts.values().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            try
            {
                artifact.setPath( artifactHandlerManager.getLocalRepositoryArtifactPath( artifact, localRepository ) );
            }
            catch ( ArtifactPathFormatException e )
            {
                throw new TransitiveArtifactResolutionException( "Error collecting artifact: ", e );
            }

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