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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultArtifactResolver
    extends AbstractLogEnabled
    implements ArtifactResolver
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private WagonManager wagonManager;

    private ArtifactTransformationManager transformationManager;

    protected ArtifactFactory artifactFactory;

    private ArtifactCollector artifactCollector;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        resolve( artifact, remoteRepositories, localRepository, false );
    }

    public void resolveAlways( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        resolve( artifact, remoteRepositories, localRepository, true );
    }

    private void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository,
                          boolean force )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        if ( artifact != null )
        {
            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                File systemFile = artifact.getFile();

                if ( !systemFile.exists() )
                {
                    throw new ArtifactNotFoundException(
                        "System artifact: " + artifact + " not found in path: " + systemFile, artifact );
                }
                else
                {
                    artifact.setResolved( true );
                }
            }
            else if ( !artifact.isResolved() )
            {
                // ----------------------------------------------------------------------
                // Check for the existence of the artifact in the specified local
                // ArtifactRepository. If it is present then simply return as the
                // request for resolution has been satisfied.
                // ----------------------------------------------------------------------

                String localPath = localRepository.pathOf( artifact );

                artifact.setFile( new File( localRepository.getBasedir(), localPath ) );

                try
                {
                    transformationManager.transformForResolve( artifact, remoteRepositories, localRepository );
                }
                catch ( ArtifactMetadataRetrievalException e )
                {
                    throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
                }

                File destination = artifact.getFile();
                boolean resolved = false;
                if ( !destination.exists() || force )
                {
                    if ( !wagonManager.isOnline() )
                    {
                        throw new ArtifactResolutionException( "System is offline.", artifact );
                    }

                    try
                    {
                        if ( artifact.getRepository() != null )
                        {
                            // the transformations discovered the artifact - so use it exclusively
                            wagonManager.getArtifact( artifact, artifact.getRepository() );
                        }
                        else
                        {
                            wagonManager.getArtifact( artifact, remoteRepositories );
                        }

                        if ( !artifact.isResolved() )
                        {
                            throw new ArtifactResolutionException(
                                "Failed to resolve artifact, possibly due to a repository list that is not appropriately equipped for this artifact's metadata.",
                                artifact, remoteRepositories );
                        }
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        throw new ArtifactNotFoundException( e.getMessage(), artifact, remoteRepositories, e );
                    }
                    catch ( TransferFailedException e )
                    {
                        throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
                    }

                    resolved = true;
                }
                else if ( destination.exists() )
                {
                    // locally resolved...no need to hit the remote repo.
                    artifact.setResolved( true );
                }

                if ( artifact.isSnapshot() && !artifact.getBaseVersion().equals( artifact.getVersion() ) )
                {
                    String version = artifact.getVersion();
                    artifact.selectVersion( artifact.getBaseVersion() );
                    File copy = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
                    if ( resolved || !copy.exists() )
                    {
                        // recopy file if it was reresolved, or doesn't exist.
                        try
                        {
                            FileUtils.copyFile( destination, copy );
                        }
                        catch ( IOException e )
                        {
                            throw new ArtifactResolutionException( "Unable to copy resolved artifact for local use",
                                                                   artifact, remoteRepositories, e );
                        }
                    }
                    artifact.setFile( copy );
                    artifact.selectVersion( version );
                }
            }
        }
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         ArtifactRepository localRepository, List remoteRepositories,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository,
                                    remoteRepositories, source, filter );

    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         Map managedVersions, ArtifactRepository localRepository,
                                                         List remoteRepositories, ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, managedVersions, localRepository,
                                    remoteRepositories, source, null );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         Map managedVersions, ArtifactRepository localRepository,
                                                         List remoteRepositories, ArtifactMetadataSource source,
                                                         ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO: this is simplistic
        List listeners = new ArrayList();
        if ( getLogger().isDebugEnabled() )
        {
            listeners.add( new DebugResolutionListener( getLogger() ) );
        }

        listeners.add( new WarningResolutionListener( getLogger() ) );

        return resolveTransitively( artifacts, originatingArtifact, managedVersions, localRepository,
                                    remoteRepositories, source, filter, listeners );

    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         Map managedVersions, ArtifactRepository localRepository,
                                                         List remoteRepositories, ArtifactMetadataSource source,
                                                         ArtifactFilter filter, List listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactResolutionResult artifactResolutionResult;
        artifactResolutionResult = artifactCollector.collect( artifacts, originatingArtifact, managedVersions,
                                                              localRepository, remoteRepositories, source, filter,
                                                              listeners );

        for ( Iterator i = artifactResolutionResult.getArtifactResolutionNodes().iterator(); i.hasNext(); )
        {
            ResolutionNode node = (ResolutionNode) i.next();
            resolve( node.getArtifact(), node.getRemoteRepositories(), localRepository );
        }

        return artifactResolutionResult;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         List remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, localRepository, remoteRepositories, source, null );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         List remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, List listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository,
                                    remoteRepositories, source, null, listeners );
    }

}