package org.apache.maven.artifact.resolver;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class DefaultArtifactResolver
    extends AbstractLogEnabled
    implements ArtifactResolver
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private static final int DEFAULT_POOL_SIZE = 5;

    private WagonManager wagonManager;

    private ArtifactTransformationManager transformationManager;

    protected ArtifactFactory artifactFactory;

    private ArtifactCollector artifactCollector;
    private final ThreadPoolExecutor resolveArtifactPool;

    public DefaultArtifactResolver()
    {
        super();
        resolveArtifactPool = 
            new ThreadPoolExecutor( DEFAULT_POOL_SIZE, DEFAULT_POOL_SIZE, 3, TimeUnit.SECONDS,
                                    new LinkedBlockingQueue() );
    }

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
        if ( artifact == null )
        {
            return;
        }

        if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            File systemFile = artifact.getFile();

            if ( systemFile == null )
            {
                throw new ArtifactNotFoundException(
                    "System artifact: " + artifact + " has no file attached", artifact );
            }

            if ( !systemFile.isFile() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact
                    + " is not a file: " + systemFile, artifact );
            }

            if ( !systemFile.exists() )
            {
                throw new ArtifactNotFoundException(
                    "System artifact: " + artifact + " not found in path: " + systemFile,
                    artifact );
            }

            artifact.setResolved( true );
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

            transformationManager.transformForResolve( artifact, remoteRepositories, localRepository );

            boolean localCopy = false;
            for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
            {
                ArtifactMetadata m = (ArtifactMetadata) i.next();
                if ( m instanceof SnapshotArtifactRepositoryMetadata )
                {
                    SnapshotArtifactRepositoryMetadata snapshotMetadata = (SnapshotArtifactRepositoryMetadata) m;

                    Metadata metadata = snapshotMetadata.getMetadata();
                    if ( metadata != null )
                    {
                        Versioning versioning = metadata.getVersioning();
                        if ( versioning != null )
                        {
                            Snapshot snapshot = versioning.getSnapshot();
                            if ( snapshot != null )
                            {
                                localCopy = snapshot.isLocalCopy();
                            }
                        }
                    }
                }
            }

            File destination = artifact.getFile();
            List repositories = remoteRepositories;

            // TODO: would prefer the snapshot transformation took care of this. Maybe we need a "shouldresolve" flag.
            if ( artifact.isSnapshot() && artifact.getBaseVersion().equals( artifact.getVersion() ) &&
                destination.exists() && !localCopy && wagonManager.isOnline() )
            {
                Date comparisonDate = new Date( destination.lastModified() );

                // cull to list of repositories that would like an update
                repositories = new ArrayList( remoteRepositories );
                for ( Iterator i = repositories.iterator(); i.hasNext(); )
                {
                    ArtifactRepository repository = (ArtifactRepository) i.next();
                    ArtifactRepositoryPolicy policy = repository.getSnapshots();
                    if ( !policy.isEnabled() || !policy.checkOutOfDate( comparisonDate ) )
                    {
                        i.remove();
                    }
                }

                if ( !repositories.isEmpty() )
                {
                    // someone wants to check for updates
                    force = true;
                }
            }

            if ( !destination.exists() || force )
            {
                if ( !wagonManager.isOnline() )
                {
                    throw new ArtifactNotFoundException( "System is offline.", artifact );
                }

                try
                {
                    // TODO: force should be passed to the wagon manager
                    if ( artifact.getRepository() != null )
                    {
                        // the transformations discovered the artifact - so use it exclusively
                        wagonManager.getArtifact( artifact, artifact.getRepository() );
                    }
                    else
                    {
                        wagonManager.getArtifact( artifact, repositories );
                    }

                    if ( !artifact.isResolved() && !destination.exists() )
                    {
                        throw new ArtifactResolutionException(
                            "Failed to resolve artifact, possibly due to a repository list that is not appropriately equipped for this artifact's metadata.",
                            artifact, getMirroredRepositories( remoteRepositories ) );
                    }
                }
                catch ( ResourceDoesNotExistException e )
                {
                    throw new ArtifactNotFoundException( e.getMessage(), artifact,
                                                         getMirroredRepositories( remoteRepositories ), e );
                }
                catch ( TransferFailedException e )
                {
                    throw new ArtifactResolutionException( e.getMessage(), artifact,
                                                           getMirroredRepositories( remoteRepositories ), e );
                }
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
                if ( !copy.exists() || copy.lastModified() != destination.lastModified()
                    || copy.length() != destination.length() )
                {
                    // recopy file if it was reresolved, or doesn't exist.
                    try
                    {
                        FileUtils.copyFile( destination, copy );
                        copy.setLastModified( destination.lastModified() );
                    }
                    catch ( IOException e )
                    {
                        throw new ArtifactResolutionException(
                            "Unable to copy resolved artifact for local use: " + e.getMessage(), artifact,
                            getMirroredRepositories( remoteRepositories ), e );
                    }
                }
                artifact.setFile( copy );
                artifact.selectVersion( version );
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
        artifactResolutionResult =
            artifactCollector.collect( artifacts, originatingArtifact, managedVersions, localRepository,
                                       remoteRepositories, source, filter, listeners );

        List resolvedArtifacts = Collections.synchronizedList( new ArrayList() );
        List missingArtifacts = Collections.synchronizedList( new ArrayList() );
        CountDownLatch latch = new CountDownLatch( artifactResolutionResult.getArtifactResolutionNodes().size() );
        Map nodesByGroupId = new HashMap();
        for ( Iterator i = artifactResolutionResult.getArtifactResolutionNodes().iterator(); i.hasNext(); )
        {
            ResolutionNode node = (ResolutionNode) i.next();
            List nodes = (List) nodesByGroupId.get( node.getArtifact().getGroupId() );
            if ( nodes == null )
            {
                nodes = new ArrayList();
                nodesByGroupId.put( node.getArtifact().getGroupId(), nodes );
            }
            nodes.add( node );
        }

        List resolutionExceptions = Collections.synchronizedList( new ArrayList() );
        try
        {
            for ( Iterator i = nodesByGroupId.values().iterator(); i.hasNext(); )
            {
                List nodes = (List) i.next();
                resolveArtifactPool.execute( new ResolveArtifactTask( resolveArtifactPool, latch, nodes,
                                                                      localRepository, resolvedArtifacts,
                                                                      missingArtifacts, resolutionExceptions ) );
            }
            latch.await();
        }
        catch ( InterruptedException e )
        {
            throw new ArtifactResolutionException( "Resolution interrupted", originatingArtifact, e );
        }

        if ( !resolutionExceptions.isEmpty() )
        {
            throw (ArtifactResolutionException) resolutionExceptions.get( 0 );
        }
        
        if ( missingArtifacts.size() > 0 )
        {
            throw new MultipleArtifactsNotFoundException( originatingArtifact, resolvedArtifacts, missingArtifacts,
                                                          getMirroredRepositories( remoteRepositories ) );
        }

        return artifactResolutionResult;
    }

    private List getMirroredRepositories( List remoteRepositories )
    {
        Map repos = new HashMap();
        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();
            ArtifactRepository repo = wagonManager.getMirrorRepository( repository );
            repos.put( repo.getId(), repo );
        }
        return new ArrayList( repos.values() );
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

    private class ResolveArtifactTask
        implements Runnable
    {
        private List nodes;

        private ArtifactRepository localRepository;

        private List resolvedArtifacts;

        private List missingArtifacts;

        private CountDownLatch latch;

        private ThreadPoolExecutor pool;

        private List resolutionExceptions;

        public ResolveArtifactTask( ThreadPoolExecutor pool, CountDownLatch latch, List nodes,
                                    ArtifactRepository localRepository, List resolvedArtifacts, List missingArtifacts,
                                    List resolutionExceptions )
        {
            this.nodes = nodes;
            this.localRepository = localRepository;
            this.resolvedArtifacts = resolvedArtifacts;
            this.missingArtifacts = missingArtifacts;
            this.latch = latch;
            this.pool = pool;
            this.resolutionExceptions = resolutionExceptions;
        }

        public void run()
        {
            Iterator i = nodes.iterator();
            ResolutionNode node = (ResolutionNode) i.next();
            i.remove();
            try
            {
                resolveArtifact( node );
            }
            catch ( ArtifactResolutionException e )
            {
                resolutionExceptions.add( e );
            }
            finally 
            {
                latch.countDown();

                if ( i.hasNext() )
                {
                    pool.execute( new ResolveArtifactTask( pool, latch, nodes, localRepository, resolvedArtifacts,
                                                           missingArtifacts, resolutionExceptions ) );
                }
            }
        }

        private void resolveArtifact( ResolutionNode node )
            throws ArtifactResolutionException
        {
            try
            {
                resolve( node.getArtifact(), node.getRemoteRepositories(), localRepository );
                resolvedArtifacts.add( node.getArtifact() );
            }
            catch ( ArtifactNotFoundException anfe )
            {
                getLogger().debug( anfe.getMessage(), anfe );

                missingArtifacts.add( node.getArtifact() );
            }
        }
    }

    public synchronized void configureNumberOfThreads( int threads )
    {
        resolveArtifactPool.setCorePoolSize( threads );
        resolveArtifactPool.setMaximumPoolSize( threads );
    }

    void setWagonManager( WagonManager wagonManager )
    {
        this.wagonManager = wagonManager;
    }
}
