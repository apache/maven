package org.apache.maven.bridge.legacy;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * @author Jason van Zyl
 */
@Singleton
class ArtifactResolver
{
    @Inject
    private Logger logger;

    @Inject
    private ArtifactCollector artifactCollector;

    @Inject
    private ArtifactMetadataSource source;

    @Inject
    private PlexusContainer container;

    @Inject
    private LegacySupport legacySupport;

    @Inject
    private RepositorySystem repoSystem;

    private final Executor executor;

    ArtifactResolver()
    {
        int threads = Integer.getInteger( "maven.artifact.threads", 5 );
        if ( threads <= 1 )
        {
            executor = new Executor()
            {
                public void execute( Runnable command )
                {
                    command.run();
                }
            };
        }
        else
        {
            executor = new ThreadPoolExecutor( threads, threads, 3, TimeUnit.SECONDS,
                                               new LinkedBlockingQueue<Runnable>(), new DaemonThreadCreator() );
        }
    }

    private RepositorySystemSession getSession( ArtifactRepository localRepository )
    {
        return LegacyLocalRepositoryManager.overlay( localRepository, legacySupport.getRepositorySession(),
                                                     repoSystem );
    }

    private void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                          RepositorySystemSession session )
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
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " has no file attached",
                                                     artifact );
            }

            if ( !systemFile.exists() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " not found in path: "
                    + systemFile, artifact );
            }

            if ( !systemFile.isFile() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " is not a file: " + systemFile,
                                                     artifact );
            }

            artifact.setResolved( true );

            return;
        }

        if ( !artifact.isResolved() )
        {
            ArtifactResult result;

            try
            {
                ArtifactRequest artifactRequest = new ArtifactRequest();
                artifactRequest.setArtifact( RepositoryUtils.toArtifact( artifact ) );
                artifactRequest.setRepositories( RepositoryUtils.toRepos( remoteRepositories ) );

                // Maven 2.x quirk: an artifact always points at the local repo, regardless whether resolved or not
                LocalRepositoryManager lrm = session.getLocalRepositoryManager();
                String path = lrm.getPathForLocalArtifact( artifactRequest.getArtifact() );
                artifact.setFile( new File( lrm.getRepository().getBasedir(), path ) );

                result = repoSystem.resolveArtifact( session, artifactRequest );
            }
            catch ( org.eclipse.aether.resolution.ArtifactResolutionException e )
            {
                if ( e.getCause() instanceof org.eclipse.aether.transfer.ArtifactNotFoundException )
                {
                    throw new ArtifactNotFoundException( e.getMessage(), artifact, remoteRepositories, e );
                }
                else
                {
                    throw new ArtifactResolutionException( e.getMessage(), artifact, remoteRepositories, e );
                }
            }

            artifact.selectVersion( result.getArtifact().getVersion() );
            artifact.setFile( result.getArtifact().getFile() );
            artifact.setResolved( true );

            if ( artifact.isSnapshot() )
            {
                Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                if ( matcher.matches() )
                {
                    Snapshot snapshot = new Snapshot();
                    snapshot.setTimestamp( matcher.group( 2 ) );
                    try
                    {
                        snapshot.setBuildNumber( Integer.parseInt( matcher.group( 3 ) ) );
                        artifact.addMetadata( new SnapshotArtifactRepositoryMetadata( artifact, snapshot ) );
                    }
                    catch ( NumberFormatException e )
                    {
                        logger.warn( "Invalid artifact version " + artifact.getVersion() + ": " + e.getMessage() );
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    @SuppressWarnings( "checkstyle:methodlength" )
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        Artifact rootArtifact = request.getArtifact();
        Set<Artifact> artifacts = request.getArtifactDependencies();
        Map<String, Artifact> managedVersions = request.getManagedVersionMap();
        List<ResolutionListener> listeners = request.getListeners();
        ArtifactFilter collectionFilter = request.getCollectionFilter();
        ArtifactFilter resolutionFilter = request.getResolutionFilter();
        RepositorySystemSession session = getSession( request.getLocalRepository() );

        // TODO: hack because metadata isn't generated in m2e correctly and i want to run the maven i have in the
        // workspace
        if ( source == null )
        {
            try
            {
                source = container.lookup( ArtifactMetadataSource.class );
            }
            catch ( ComponentLookupException e )
            {
                // won't happen
            }
        }

        if ( listeners == null )
        {
            listeners = new ArrayList<>();

            if ( logger.isDebugEnabled() )
            {
                listeners.add( new DebugResolutionListener( logger ) );
            }
        }

        ArtifactResolutionResult result = new ArtifactResolutionResult();

        // The root artifact may, or may not be resolved so we need to check before we attempt to resolve.
        // This is often an artifact like a POM that is taken from disk and we already have hold of the
        // file reference. But this may be a Maven Plugin that we need to resolve from a remote repository
        // as well as its dependencies.

        if ( request.isResolveRoot() /* && rootArtifact.getFile() == null */ )
        {
            try
            {
                resolve( rootArtifact, request.getRemoteRepositories(), session );
            }
            catch ( ArtifactResolutionException e )
            {
                result.addErrorArtifactException( e );
                return result;
            }
            catch ( ArtifactNotFoundException e )
            {
                result.addMissingArtifact( request.getArtifact() );
                return result;
            }
        }

        ArtifactResolutionRequest collectionRequest = request;

        if ( request.isResolveTransitively() )
        {
            MetadataResolutionRequest metadataRequest = new DefaultMetadataResolutionRequest( request );

            metadataRequest.setArtifact( rootArtifact );
            metadataRequest.setResolveManagedVersions( managedVersions == null );

            try
            {
                ResolutionGroup resolutionGroup = source.retrieve( metadataRequest );

                if ( managedVersions == null )
                {
                    managedVersions = resolutionGroup.getManagedVersions();
                }

                Set<Artifact> directArtifacts = resolutionGroup.getArtifacts();

                if ( artifacts == null || artifacts.isEmpty() )
                {
                    artifacts = directArtifacts;
                }
                else
                {
                    List<Artifact> allArtifacts = new ArrayList<>();
                    allArtifacts.addAll( artifacts );
                    allArtifacts.addAll( directArtifacts );

                    Map<String, Artifact> mergedArtifacts = new LinkedHashMap<>();
                    for ( Artifact artifact : allArtifacts )
                    {
                        String conflictId = artifact.getDependencyConflictId();
                        if ( !mergedArtifacts.containsKey( conflictId ) )
                        {
                            mergedArtifacts.put( conflictId, artifact );
                        }
                    }

                    artifacts = new LinkedHashSet<>( mergedArtifacts.values() );
                }

                collectionRequest = new ArtifactResolutionRequest( request );
                collectionRequest.setServers( request.getServers() );
                collectionRequest.setMirrors( request.getMirrors() );
                collectionRequest.setProxies( request.getProxies() );
                collectionRequest.setRemoteRepositories( resolutionGroup.getResolutionRepositories() );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                ArtifactResolutionException are =
                    new ArtifactResolutionException( "Unable to get dependency information for " + rootArtifact.getId()
                        + ": " + e.getMessage(), rootArtifact, metadataRequest.getRemoteRepositories(), e );
                result.addMetadataResolutionException( are );
                return result;
            }
        }

        if ( artifacts == null || artifacts.isEmpty() )
        {
            if ( request.isResolveRoot() )
            {
                result.addArtifact( rootArtifact );
            }
            return result;
        }

        // After the collection we will have the artifact object in the result but they will not be resolved yet.
        result = artifactCollector.collect( artifacts, rootArtifact, managedVersions, collectionRequest, source,
                                            collectionFilter, listeners, null );

        // We have metadata retrieval problems, or there are cycles that have been detected
        // so we give this back to the calling code and let them deal with this information
        // appropriately.

        if ( result.hasMetadataResolutionExceptions() || result.hasVersionRangeViolations()
            || result.hasCircularDependencyExceptions() )
        {
            logger.info( "Failure detected." );
            return result;
        }

        if ( result.getArtifactResolutionNodes() != null )
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            CountDownLatch latch = new CountDownLatch( result.getArtifactResolutionNodes().size() );

            for ( ResolutionNode node : result.getArtifactResolutionNodes() )
            {
                Artifact artifact = node.getArtifact();

                if ( resolutionFilter == null || resolutionFilter.include( artifact ) )
                {
                    executor.execute( new ResolveTask( classLoader, latch, artifact, session,
                                                       node.getRemoteRepositories(), result ) );
                }
                else
                {
                    latch.countDown();
                }
            }
            try
            {
                latch.await();
            }
            catch ( InterruptedException e )
            {
                result.addErrorArtifactException( new ArtifactResolutionException( "Resolution interrupted",
                                                                                   rootArtifact, e ) );
            }
        }

        // We want to send the root artifact back in the result but we need to do this after the other dependencies
        // have been resolved.
        if ( request.isResolveRoot() )
        {
            // Add the root artifact (as the first artifact to retain logical order of class path!)
            Set<Artifact> allArtifacts = new LinkedHashSet<>();
            allArtifacts.add( rootArtifact );
            allArtifacts.addAll( result.getArtifacts() );
            result.setArtifacts( allArtifacts );
        }

        return result;
    }

    /**
     * ThreadCreator for creating daemon threads with fixed ThreadGroup-name.
     */
    static final class DaemonThreadCreator
        implements ThreadFactory
    {
        static final String THREADGROUP_NAME = "org.apache.maven.artifact.resolver.DefaultArtifactResolver";

        static final ThreadGroup GROUP = new ThreadGroup( THREADGROUP_NAME );

        static final AtomicInteger THREAD_NUMBER = new AtomicInteger( 1 );

        public Thread newThread( Runnable r )
        {
            Thread newThread = new Thread( GROUP, r, "resolver-" + THREAD_NUMBER.getAndIncrement() );
            newThread.setDaemon( true );
            newThread.setContextClassLoader( null );
            return newThread;
        }
    }

    private class ResolveTask
        implements Runnable
    {

        private final ClassLoader classLoader;

        private final CountDownLatch latch;

        private final Artifact artifact;

        private final RepositorySystemSession session;

        private final List<ArtifactRepository> remoteRepositories;

        private final ArtifactResolutionResult result;

        ResolveTask( ClassLoader classLoader, CountDownLatch latch, Artifact artifact,
                            RepositorySystemSession session, List<ArtifactRepository> remoteRepositories,
                            ArtifactResolutionResult result )
        {
            this.classLoader = classLoader;
            this.latch = latch;
            this.artifact = artifact;
            this.session = session;
            this.remoteRepositories = remoteRepositories;
            this.result = result;
        }

        public void run()
        {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( classLoader );
                resolve( artifact, remoteRepositories, session );
            }
            catch ( ArtifactNotFoundException anfe )
            {
                // These are cases where the artifact just isn't present in any of the remote repositories
                // because it wasn't deployed, or it was deployed in the wrong place.

                synchronized ( result )
                {
                    result.addMissingArtifact( artifact );
                }
            }
            catch ( ArtifactResolutionException e )
            {
                // This is really a wagon TransferFailedException so something went wrong after we successfully
                // retrieved the metadata.

                synchronized ( result )
                {
                    result.addErrorArtifactException( e );
                }
            }
            finally
            {
                latch.countDown();
                Thread.currentThread().setContextClassLoader( old );

            }
        }

    }

    @PreDestroy
    public void dispose()
    {
        if ( executor instanceof ExecutorService )
        {
            ( (ExecutorService) executor ).shutdownNow();
        }
    }

}
