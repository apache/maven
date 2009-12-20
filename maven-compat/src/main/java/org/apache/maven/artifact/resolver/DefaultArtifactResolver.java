package org.apache.maven.artifact.resolver;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.DefaultLocalRepositoryMaintainerEvent;
import org.apache.maven.repository.LocalRepositoryMaintainer;
import org.apache.maven.repository.LocalRepositoryMaintainerEvent;
import org.apache.maven.repository.legacy.TransferListenerAdapter;
import org.apache.maven.repository.legacy.WagonManager;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.apache.maven.repository.legacy.resolver.conflict.ConflictResolver;
import org.apache.maven.repository.legacy.resolver.transform.ArtifactTransformationManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Jason van Zyl
 */
@Component(role = ArtifactResolver.class)
public class DefaultArtifactResolver
    implements ArtifactResolver
{
    @Requirement 
    private Logger logger;
    
    @Requirement
    private WagonManager wagonManager;

    @Requirement
    private ArtifactTransformationManager transformationManager;

    @Requirement
    protected ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactCollector artifactCollector;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private ArtifactMetadataSource source;
    
    @Requirement
    private PlexusContainer container;

    @Requirement( optional = true )
    private LocalRepositoryMaintainer localRepositoryMaintainer;

    @Requirement
    private LegacySupport legacySupport;

    private void injectSession( RepositoryRequest request )
    {
        MavenSession session = legacySupport.getSession();

        if ( session != null )
        {
            request.setOffline( session.isOffline() );
            request.setTransferListener( session.getRequest().getTransferListener() );
        }
    }

    private void injectSession( ArtifactResolutionRequest request )
    {
        MavenSession session = legacySupport.getSession();

        if ( session != null )
        {
            request.setOffline( session.isOffline() );
            request.setServers( session.getRequest().getServers() );
            request.setMirrors( session.getRequest().getMirrors() );
            request.setProxies( session.getRequest().getProxies() );
            request.setTransferListener( session.getRequest().getTransferListener() );
        }
    }

    public void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository, TransferListener resolutionListener )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        RepositoryRequest request = new DefaultRepositoryRequest();
        injectSession( request );
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        resolve( artifact, request, resolutionListener, false );
    }

    public void resolveAlways( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        RepositoryRequest request = new DefaultRepositoryRequest();
        injectSession( request );
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        resolve( artifact, request, null, true );
    }

    private void resolve( Artifact artifact, RepositoryRequest request, TransferListener downloadMonitor, boolean force )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        if ( artifact == null )
        {
            return;
        }

        File destination;
        
        if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            File systemFile = artifact.getFile();

            if ( systemFile == null )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " has no file attached", artifact );
            }

            if ( !systemFile.exists() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " not found in path: " + systemFile, artifact );
            }

            if ( !systemFile.isFile() )
            {
                throw new ArtifactNotFoundException( "System artifact: " + artifact + " is not a file: " + systemFile, artifact );
            }

            artifact.setResolved( true );
            
            return;
        }

        ArtifactRepository localRepository = request.getLocalRepository();

        List<ArtifactRepository> remoteRepositories = request.getRemoteRepositories();

        if ( !artifact.isResolved() )
        {
            // ----------------------------------------------------------------------
            // Check for the existence of the artifact in the specified local
            // ArtifactRepository. If it is present then simply return as the
            // request for resolution has been satisfied.
            // ----------------------------------------------------------------------

            artifact = localRepository.find( artifact );
            
            if ( artifact.isResolved() )
            {
                return;
            }

            transformationManager.transformForResolve( artifact, request );

            destination = artifact.getFile();

            if ( !request.isOffline() && ( force || !destination.exists() || isMutable( artifact ) ) )
            {
                try
                {
                    if ( artifact.getRepository() != null )
                    {
                        // the transformations discovered the artifact - so use it exclusively
                        wagonManager.getArtifact( artifact, artifact.getRepository(), downloadMonitor, request.isForceUpdate() );
                    }
                    else
                    {
                        wagonManager.getArtifact( artifact, remoteRepositories, downloadMonitor, request.isForceUpdate() );
                    }

                    if ( localRepositoryMaintainer != null )
                    {
                        LocalRepositoryMaintainerEvent event =
                            new DefaultLocalRepositoryMaintainerEvent( localRepository, artifact, null );
                        localRepositoryMaintainer.artifactDownloaded( event );
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
            }

            if ( destination.exists() )
            {
                artifact.setResolved( true );
            }
            else
            {
                if ( request.isOffline() )
                {
                    throw new ArtifactResolutionException( "The repository system is offline"
                        + " and the requested artifact is not locally available at " + destination, artifact,
                                                           remoteRepositories );
                }
                else
                {
                    throw new ArtifactResolutionException( "Failed to resolve artifact, possibly due to a "
                        + "repository list that is not appropriately equipped for this artifact's metadata.", artifact,
                                                           remoteRepositories );
                }
            }
                                        
            // 1.0-SNAPSHOT
            //
            // 1)         pom = 1.0-SNAPSHOT
            // 2)         pom = 1.0-yyyymmdd.hhmmss
            // 3) baseVersion = 1.0-SNAPSHOT
            if ( artifact.isSnapshot() && isTimestamped( artifact ) )
            {
                String version = artifact.getVersion();

                // 1.0-SNAPSHOT
                artifact.selectVersion( artifact.getBaseVersion() );

                // Make a file with a 1.0-SNAPSHOT format
                File copy = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
                
                // if the timestamped version was resolved or the copy doesn't exist then copy a version
                // of the file like 1.0-SNAPSHOT. Even if there is a timestamped version the non-timestamped
                // version will be created.
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
                        throw new ArtifactResolutionException( "Unable to copy resolved artifact for local use: " + e.getMessage(), artifact, remoteRepositories, e );
                    }
                }

                // We are only going to use the 1.0-SNAPSHOT version
                artifact.setFile( copy );

                // Set the version to the 1.0-yyyymmdd.hhmmss version
                artifact.selectVersion( version );
            }
        }
    }

    private boolean isMutable( Artifact artifact )
    {
        return artifact.isSnapshot() && !isTimestamped( artifact ) && !isLocalCopy( artifact );
    }

    private boolean isTimestamped( Artifact artifact )
    {
        return !artifact.getBaseVersion().equals( artifact.getVersion() );
    }

    private boolean isLocalCopy( Artifact artifact )
    {
        boolean localCopy = false;

        for ( ArtifactMetadata m : artifact.getMetadataList() )
        {
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
                            // TODO is it possible to have more than one SnapshotArtifactRepositoryMetadata
                            localCopy = snapshot.isLocalCopy();
                        }
                    }
                }
            }
        }

        return localCopy;
    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository, remoteRepositories, source, filter );

    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source, null );
    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source, filter, null );
    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, localRepository, remoteRepositories, source, null );
    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, List<ResolutionListener> listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository,
                                    remoteRepositories, source, null, listeners );
    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveTransitively( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source, filter, listeners, null );
    }

    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact, Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter, List<ResolutionListener> listeners,
                                                         List<ConflictResolver> conflictResolvers )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( originatingArtifact )
            .setResolveRoot( false )
            // This is required by the surefire plugin
            .setArtifactDependencies( artifacts )            
            .setManagedVersionMap( managedVersions )
            .setLocalRepository( localRepository )
            .setRemoteRepositories( remoteRepositories )
            .setCollectionFilter( filter )
            .setListeners( listeners );

        injectSession( request );

        return resolveWithExceptions( request );
    }

    public ArtifactResolutionResult resolveWithExceptions( ArtifactResolutionRequest request )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactResolutionResult result = resolve( request );

        // We have collected all the problems so let's mimic the way the old code worked and just blow up right here.
        // That's right lets just let it rip right here and send a big incomprehensible blob of text at unsuspecting
        // users. Bad dog!

        resolutionErrorHandler.throwErrors( request, result );

        return result;
    }

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        Artifact rootArtifact = request.getArtifact();
        Set<Artifact> artifacts = request.getArtifactDependencies();
        Map managedVersions = request.getManagedVersionMap();
        List<ResolutionListener> listeners = request.getListeners();
        ArtifactFilter collectionFilter = request.getCollectionFilter();                       
        ArtifactFilter resolutionFilter = request.getResolutionFilter();
        TransferListener transferListener = TransferListenerAdapter.newAdapter( request.getTransferListener() );
        
        //TODO: hack because metadata isn't generated in m2e correctly and i want to run the maven i have in the workspace
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
            listeners = new ArrayList<ResolutionListener>();

            if ( logger.isDebugEnabled() )
            {
                listeners.add( new DebugResolutionListener( logger ) );
            }

            listeners.add( new WarningResolutionListener( logger ) );
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
                resolve( rootArtifact, request, transferListener, false );
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
                    List<Artifact> allArtifacts = new ArrayList<Artifact>();
                    allArtifacts.addAll( artifacts );
                    allArtifacts.addAll( directArtifacts );

                    Map<String, Artifact> mergedArtifacts = new LinkedHashMap<String, Artifact>();
                    for ( Artifact artifact : allArtifacts )
                    {
                        String conflictId = artifact.getDependencyConflictId();
                        if ( !mergedArtifacts.containsKey( conflictId ) )
                        {
                            mergedArtifacts.put( conflictId, artifact );
                        }
                    }

                    artifacts = new LinkedHashSet<Artifact>( mergedArtifacts.values() );
                }

                collectionRequest = new ArtifactResolutionRequest( request );
                collectionRequest.setServers( request.getServers() );
                collectionRequest.setMirrors( request.getMirrors() );
                collectionRequest.setProxies( request.getProxies() );
                collectionRequest.setRemoteRepositories( resolutionGroup.getResolutionRepositories() );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                // need to add metadata resolution exception
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
        result =
            artifactCollector.collect( artifacts, rootArtifact, managedVersions, collectionRequest, source,
                                       collectionFilter, listeners, null );
                        
        // We have metadata retrieval problems, or there are cycles that have been detected
        // so we give this back to the calling code and let them deal with this information
        // appropriately.

        if ( result.hasMetadataResolutionExceptions() || result.hasVersionRangeViolations() || result.hasCircularDependencyExceptions() )
        {
            return result;
        }
                
        if ( result.getArtifactResolutionNodes() != null )
        {
            ArtifactResolutionRequest childRequest = new ArtifactResolutionRequest( request );

            for ( ResolutionNode node : result.getArtifactResolutionNodes() )
            {
                Artifact artifact = node.getArtifact();

                try
                {
                    if ( resolutionFilter == null || resolutionFilter.include( artifact ) )
                    {
                        childRequest.setRemoteRepositories( node.getRemoteRepositories() );

                        resolve( artifact, childRequest, transferListener, false );
                    }
                }
                catch ( ArtifactNotFoundException anfe )
                {
                    // These are cases where the artifact just isn't present in any of the remote repositories
                    // because it wasn't deployed, or it was deployed in the wrong place.

                    result.addMissingArtifact( artifact );
                }
                catch ( ArtifactResolutionException e )
                {
                    // This is really a wagon TransferFailedException so something went wrong after we successfully
                    // retrieved the metadata.

                    result.addErrorArtifactException( e );
                }
            }
        }
                
        // We want to send the root artifact back in the result but we need to do this after the other dependencies
        // have been resolved.
        if ( request.isResolveRoot() )
        {            
            // Add the root artifact (as the first artifact to retain logical order of class path!)
            Set<Artifact> allArtifacts = new LinkedHashSet<Artifact>();
            allArtifacts.add( rootArtifact );
            allArtifacts.addAll( result.getArtifacts() );
            result.setArtifacts( allArtifacts );
        }                        
                 
        return result;
    }

    public void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        resolve( artifact, remoteRepositories, localRepository, null );
    }
}
