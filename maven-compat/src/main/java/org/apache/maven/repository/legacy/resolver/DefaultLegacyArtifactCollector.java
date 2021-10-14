package org.apache.maven.repository.legacy.resolver;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.CyclicDependencyException;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.ResolutionListenerForDepMgmt;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ManagedVersionMap;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.apache.maven.repository.legacy.resolver.conflict.ConflictResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Jason van Zyl
 */
@Component( role = LegacyArtifactCollector.class )
public class DefaultLegacyArtifactCollector
    implements LegacyArtifactCollector
{

    @Requirement( hint = "nearest" )
    private ConflictResolver defaultConflictResolver;

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport legacySupport;

    private void injectSession( final ArtifactResolutionRequest request )
    {
        final MavenSession session = legacySupport.getSession();

        if ( session != null )
        {
            request.setOffline( session.isOffline() );
            request.setForceUpdate( session.getRequest().isUpdateSnapshots() );
            request.setServers( session.getRequest().getServers() );
            request.setMirrors( session.getRequest().getMirrors() );
            request.setProxies( session.getRequest().getProxies() );
        }
    }

    @SuppressWarnings( "checkstyle:parameternumber" )
    public ArtifactResolutionResult collect( final Set<Artifact> artifacts, final Artifact originatingArtifact,
                                             final Map<String, Artifact> managedVersions, final ArtifactRepository localRepository,
                                             final List<ArtifactRepository> remoteRepositories,
                                             final ArtifactMetadataSource source, final ArtifactFilter filter,
                                             final List<ResolutionListener> listeners,
                                             final List<ConflictResolver> conflictResolvers )
    {
        final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        injectSession( request );
        return collect( artifacts, originatingArtifact, managedVersions, request, source, filter, listeners,
                        conflictResolvers );
    }

    @SuppressWarnings( "checkstyle:parameternumber" )
    public ArtifactResolutionResult collect( final Set<Artifact> artifacts, final Artifact originatingArtifact,
                                             final Map<String, Artifact> managedVersions,
                                             final ArtifactResolutionRequest repositoryRequest,
                                             final ArtifactMetadataSource source, final ArtifactFilter filter,
                                             final List<ResolutionListener> listeners,
                                             List<ConflictResolver> conflictResolvers )
    {
        final ArtifactResolutionResult result = new ArtifactResolutionResult();

        result.setOriginatingArtifact( originatingArtifact );

        if ( conflictResolvers == null )
        {
            conflictResolvers = Collections.singletonList( defaultConflictResolver );
        }

        final Map<Object, List<ResolutionNode>> resolvedArtifacts = new LinkedHashMap<>();

        final ResolutionNode root = new ResolutionNode( originatingArtifact, repositoryRequest.getRemoteRepositories() );

        try
        {
            root.addDependencies( artifacts, repositoryRequest.getRemoteRepositories(), filter );
        }
        catch ( final CyclicDependencyException e )
        {
            result.addCircularDependencyException( e );

            return result;
        }
        catch ( final OverConstrainedVersionException e )
        {
            result.addVersionRangeViolation( e );

            return result;
        }

        final ManagedVersionMap versionMap = getManagedVersionsMap( originatingArtifact, managedVersions );

        try
        {
            recurse( result, root, resolvedArtifacts, versionMap, repositoryRequest, source, filter, listeners,
                     conflictResolvers );
        }
        catch ( final CyclicDependencyException e )
        {
            logger.debug( "While recursing: " + e.getMessage(), e );
            result.addCircularDependencyException( e );
        }
        catch ( final OverConstrainedVersionException e )
        {
            logger.debug( "While recursing: " + e.getMessage(), e );
            result.addVersionRangeViolation( e );
        }
        catch ( final ArtifactResolutionException e )
        {
            logger.debug( "While recursing: " + e.getMessage(), e );
            result.addErrorArtifactException( e );
        }

        final Set<ResolutionNode> set = new LinkedHashSet<>();

        for ( final List<ResolutionNode> nodes : resolvedArtifacts.values() )
        {
            for ( final ResolutionNode node : nodes )
            {
                if ( !node.equals( root ) && node.isActive() )
                {
                    final Artifact artifact = node.getArtifact();

                    try
                    {
                        if ( node.filterTrail( filter ) )
                        {
                            // If it was optional and not a direct dependency,
                            // we don't add it or its children, just allow the update of the version and artifactScope
                            if ( node.isChildOfRootNode() || !artifact.isOptional() )
                            {
                                artifact.setDependencyTrail( node.getDependencyTrail() );

                                set.add( node );

                                // This is required right now.
                                result.addArtifact( artifact );
                            }
                        }
                    }
                    catch ( final OverConstrainedVersionException e )
                    {
                        result.addVersionRangeViolation( e );
                    }
                }
            }
        }

        result.setArtifactResolutionNodes( set );

        return result;
    }

    /**
     * Get the map of managed versions, removing the originating artifact if it is also in managed versions
     *
     * @param originatingArtifact artifact we are processing
     * @param managedVersions original managed versions
     */
    private ManagedVersionMap getManagedVersionsMap( final Artifact originatingArtifact,
                                                     final Map<String, Artifact> managedVersions )
    {
        ManagedVersionMap versionMap;
        if ( managedVersions instanceof ManagedVersionMap )
        {
            versionMap = (ManagedVersionMap) managedVersions;
        }
        else
        {
            versionMap = new ManagedVersionMap( managedVersions );
        }

        // remove the originating artifact if it is also in managed versions to avoid being modified during resolution
        final Artifact managedOriginatingArtifact = versionMap.get( originatingArtifact.getDependencyConflictId() );

        if ( managedOriginatingArtifact != null )
        {
            // TODO we probably want to warn the user that he is building an artifact with
            // different values than in dependencyManagement
            if ( managedVersions instanceof ManagedVersionMap )
            {
                /* avoid modifying the managedVersions parameter creating a new map */
                versionMap = new ManagedVersionMap( managedVersions );
            }
            versionMap.remove( originatingArtifact.getDependencyConflictId() );
        }

        return versionMap;
    }

    @SuppressWarnings( { "checkstyle:parameternumber", "checkstyle:methodlength" } )
    private void recurse( final ArtifactResolutionResult result, final ResolutionNode node,
                          final Map<Object, List<ResolutionNode>> resolvedArtifacts, final ManagedVersionMap managedVersions,
                          final ArtifactResolutionRequest request, final ArtifactMetadataSource source, final ArtifactFilter filter,
                          final List<ResolutionListener> listeners, final List<ConflictResolver> conflictResolvers )
        throws ArtifactResolutionException
    {
        fireEvent( ResolutionListener.TEST_ARTIFACT, listeners, node );

        final Object key = node.getKey();

        // TODO Does this check need to happen here? Had to add the same call
        // below when we iterate on child nodes -- will that suffice?
        if ( managedVersions.containsKey( key ) )
        {
            manageArtifact( node, managedVersions, listeners );
        }

        List<ResolutionNode> previousNodes = resolvedArtifacts.get( key );

        if ( previousNodes != null )
        {
            for ( final ResolutionNode previous : previousNodes )
            {
                try
                {
                    if ( previous.isActive() )
                    {
                        // Version mediation
                        final VersionRange previousRange = previous.getArtifact().getVersionRange();
                        final VersionRange currentRange = node.getArtifact().getVersionRange();

                        if ( ( previousRange != null ) && ( currentRange != null ) )
                        {
                            // TODO shouldn't need to double up on this work, only done for simplicity of handling
                            // recommended
                            // version but the restriction is identical
                            final VersionRange newRange = previousRange.restrict( currentRange );
                            // TODO ick. this forces the OCE that should have come from the previous call. It is still
                            // correct
                            if ( newRange.isSelectedVersionKnown( previous.getArtifact() ) )
                            {
                                fireEvent( ResolutionListener.RESTRICT_RANGE, listeners, node, previous.getArtifact(),
                                           newRange );
                            }
                            previous.getArtifact().setVersionRange( newRange );
                            node.getArtifact().setVersionRange( currentRange.restrict( previousRange ) );

                            // Select an appropriate available version from the (now restricted) range
                            // Note this version was selected before to get the appropriate POM
                            // But it was reset by the call to setVersionRange on restricting the version
                            final ResolutionNode[] resetNodes =
                            {
                                previous, node
                            };
                            for ( int j = 0; j < 2; j++ )
                            {
                                final Artifact resetArtifact = resetNodes[j].getArtifact();

                                // MNG-2123: if the previous node was not a range, then it wouldn't have any available
                                // versions. We just clobbered the selected version above. (why? i have no idea.)
                                // So since we are here and this is ranges we must go figure out the version (for a
                                // third time...)
                                if ( resetArtifact.getVersion() == null && resetArtifact.getVersionRange() != null )
                                {

                                    // go find the version. This is a total hack. See previous comment.
                                    List<ArtifactVersion> versions = resetArtifact.getAvailableVersions();
                                    if ( versions == null )
                                    {
                                        try
                                        {
                                            final MetadataResolutionRequest metadataRequest =
                                                new DefaultMetadataResolutionRequest( request );

                                            metadataRequest.setArtifact( resetArtifact );
                                            versions = source.retrieveAvailableVersions( metadataRequest );
                                            resetArtifact.setAvailableVersions( versions );
                                        }
                                        catch ( final ArtifactMetadataRetrievalException e )
                                        {
                                            resetArtifact.setDependencyTrail( node.getDependencyTrail() );
                                            throw new ArtifactResolutionException(
                                                "Unable to get dependency information: "
                                                    + e.getMessage(), resetArtifact, request.getRemoteRepositories(),
                                                e );

                                        }
                                    }
                                    // end hack

                                    // MNG-2861: match version can return null
                                    final ArtifactVersion selectedVersion = resetArtifact.getVersionRange().
                                        matchVersion( resetArtifact.getAvailableVersions() );

                                    if ( selectedVersion != null )
                                    {
                                        resetArtifact.selectVersion( selectedVersion.toString() );
                                    }
                                    else
                                    {
                                        throw new OverConstrainedVersionException(
                                            "Unable to find a version in " + resetArtifact.getAvailableVersions()
                                                + " to match the range " + resetArtifact.getVersionRange(),
                                            resetArtifact );

                                    }

                                    fireEvent( ResolutionListener.SELECT_VERSION_FROM_RANGE, listeners, resetNodes[j] );
                                }
                            }
                        }

                        // Conflict Resolution
                        ResolutionNode resolved = null;
                        for ( final Iterator<ConflictResolver> j = conflictResolvers.iterator();
                              resolved == null && j.hasNext(); )
                        {
                            final ConflictResolver conflictResolver = j.next();

                            resolved = conflictResolver.resolveConflict( previous, node );
                        }

                        if ( resolved == null )
                        {
                            // TODO add better exception that can detail the two conflicting artifacts
                            final ArtifactResolutionException are =
                                new ArtifactResolutionException( "Cannot resolve artifact version conflict between "
                                                                     + previous.getArtifact().getVersion() + " and "
                                                                     + node.getArtifact().getVersion(),
                                                                 previous.getArtifact() );

                            result.addVersionRangeViolation( are );
                        }

                        if ( ( resolved != previous ) && ( resolved != node ) )
                        {
                            // TODO add better exception
                            result.addVersionRangeViolation( new ArtifactResolutionException(
                                "Conflict resolver returned unknown resolution node: ",
                                resolved.getArtifact() ) );

                        }

                        // TODO should this be part of mediation?
                        // previous one is more dominant
                        final ResolutionNode nearest;
                        final ResolutionNode farthest;

                        if ( resolved == previous )
                        {
                            nearest = previous;
                            farthest = node;
                        }
                        else
                        {
                            nearest = node;
                            farthest = previous;
                        }

                        if ( checkScopeUpdate( farthest, nearest, listeners ) )
                        {
                            // if we need to update artifactScope of nearest to use farthest artifactScope, use the
                            // nearest version, but farthest artifactScope
                            nearest.disable();
                            farthest.getArtifact().setVersion( nearest.getArtifact().getVersion() );
                            fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, nearest, farthest.getArtifact() );
                        }
                        else
                        {
                            farthest.disable();
                            fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, farthest, nearest.getArtifact() );
                        }
                    }
                }
                catch ( final OverConstrainedVersionException e )
                {
                    result.addVersionRangeViolation( e );
                }
            }
        }
        else
        {
            previousNodes = new ArrayList<>();

            resolvedArtifacts.put( key, previousNodes );
        }
        previousNodes.add( node );

        if ( node.isActive() )
        {
            fireEvent( ResolutionListener.INCLUDE_ARTIFACT, listeners, node );
        }

        // don't pull in the transitive deps of a system-scoped dependency.
        if ( node.isActive() && !Artifact.SCOPE_SYSTEM.equals( node.getArtifact().getScope() ) )
        {
            fireEvent( ResolutionListener.PROCESS_CHILDREN, listeners, node );

            final Artifact parentArtifact = node.getArtifact();

            for ( final Iterator<ResolutionNode> i = node.getChildrenIterator(); i.hasNext(); )
            {
                final ResolutionNode child = i.next();

                try
                {

                    // We leave in optional ones, but don't pick up its dependencies
                    if ( !child.isResolved() && ( !child.getArtifact().isOptional() || child.isChildOfRootNode() ) )
                    {
                        final Artifact artifact = child.getArtifact();
                        artifact.setDependencyTrail( node.getDependencyTrail() );
                        final List<ArtifactRepository> childRemoteRepositories = child.getRemoteRepositories();

                        final MetadataResolutionRequest metadataRequest =
                            new DefaultMetadataResolutionRequest( request );
                        metadataRequest.setArtifact( artifact );
                        metadataRequest.setRemoteRepositories( childRemoteRepositories );

                        try
                        {
                            ResolutionGroup rGroup;

                            Object childKey;
                            do
                            {
                                childKey = child.getKey();

                                if ( managedVersions.containsKey( childKey ) )
                                {
                                    // If this child node is a managed dependency, ensure
                                    // we are using the dependency management version
                                    // of this child if applicable b/c we want to use the
                                    // managed version's POM, *not* any other version's POM.
                                    // We retrieve the POM below in the retrieval step.
                                    manageArtifact( child, managedVersions, listeners );

                                    // Also, we need to ensure that any exclusions it presents are
                                    // added to the artifact before we retrieve the metadata
                                    // for the artifact; otherwise we may end up with unwanted
                                    // dependencies.
                                    final Artifact ma = managedVersions.get( childKey );
                                    final ArtifactFilter managedExclusionFilter = ma.getDependencyFilter();
                                    if ( null != managedExclusionFilter )
                                    {
                                        if ( null != artifact.getDependencyFilter() )
                                        {
                                            final AndArtifactFilter aaf = new AndArtifactFilter();
                                            aaf.add( artifact.getDependencyFilter() );
                                            aaf.add( managedExclusionFilter );
                                            artifact.setDependencyFilter( aaf );
                                        }
                                        else
                                        {
                                            artifact.setDependencyFilter( managedExclusionFilter );
                                        }
                                    }
                                }

                                if ( artifact.getVersion() == null )
                                {
                                    // set the recommended version
                                    // TODO maybe its better to just pass the range through to retrieval and use a
                                    // transformation?
                                    final ArtifactVersion version;
                                    if ( !artifact.isSelectedVersionKnown() )
                                    {
                                        List<ArtifactVersion> versions = artifact.getAvailableVersions();
                                        if ( versions == null )
                                        {
                                            versions = source.retrieveAvailableVersions( metadataRequest );
                                            artifact.setAvailableVersions( versions );
                                        }

                                        Collections.sort( versions );

                                        final VersionRange versionRange = artifact.getVersionRange();

                                        version = versionRange.matchVersion( versions );

                                        if ( version == null )
                                        {
                                            if ( versions.isEmpty() )
                                            {
                                                throw new OverConstrainedVersionException(
                                                    "No versions are present in the repository for the artifact"
                                                        + " with a range " + versionRange, artifact,
                                                    childRemoteRepositories );

                                            }

                                            throw new OverConstrainedVersionException(
                                                "Couldn't find a version in " + versions + " to match range "
                                                    + versionRange, artifact, childRemoteRepositories );

                                        }
                                    }
                                    else
                                    {
                                        version = artifact.getSelectedVersion();
                                    }

                                    artifact.selectVersion( version.toString() );
                                    fireEvent( ResolutionListener.SELECT_VERSION_FROM_RANGE, listeners, child );
                                }

                                rGroup = source.retrieve( metadataRequest );

                                if ( rGroup == null )
                                {
                                    break;
                                }
                            }
                            while ( !childKey.equals( child.getKey() ) );

                            if ( parentArtifact != null && parentArtifact.getDependencyFilter() != null
                                     && !parentArtifact.getDependencyFilter().include( artifact ) )
                            {
                                // MNG-3769: the [probably relocated] artifact is excluded.
                                // We could process exclusions on relocated artifact details in the
                                // MavenMetadataSource.createArtifacts(..) step, BUT that would
                                // require resolving the POM from the repository very early on in
                                // the build.
                                continue;
                            }

                            // TODO might be better to have source.retrieve() throw a specific exception for this
                            // situation
                            // and catch here rather than have it return null
                            if ( rGroup == null )
                            {
                                // relocated dependency artifact is declared excluded, no need to add and recurse
                                // further
                                continue;
                            }

                            child.addDependencies( rGroup.getArtifacts(), rGroup.getResolutionRepositories(), filter );

                        }
                        catch ( final CyclicDependencyException e )
                        {
                            // would like to throw this, but we have crappy stuff in the repo

                            fireEvent( ResolutionListener.OMIT_FOR_CYCLE, listeners,
                                       new ResolutionNode( e.getArtifact(), childRemoteRepositories, child ) );
                        }
                        catch ( final ArtifactMetadataRetrievalException e )
                        {
                            artifact.setDependencyTrail( node.getDependencyTrail() );

                            throw new ArtifactResolutionException( "Unable to get dependency information for "
                                                                       + artifact.getId() + ": " + e.getMessage(),
                                                                   artifact, childRemoteRepositories, e );

                        }

                        final ArtifactResolutionRequest subRequest = new ArtifactResolutionRequest( metadataRequest );
                        subRequest.setServers( request.getServers() );
                        subRequest.setMirrors( request.getMirrors() );
                        subRequest.setProxies( request.getProxies() );
                        recurse( result, child, resolvedArtifacts, managedVersions, subRequest, source, filter,
                                 listeners, conflictResolvers );

                    }
                }
                catch ( final OverConstrainedVersionException e )
                {
                    result.addVersionRangeViolation( e );
                }
                catch ( final ArtifactResolutionException e )
                {
                    result.addMetadataResolutionException( e );
                }
            }

            fireEvent( ResolutionListener.FINISH_PROCESSING_CHILDREN, listeners, node );
        }
    }

    private void manageArtifact( final ResolutionNode node, final ManagedVersionMap managedVersions,
                                 final List<ResolutionListener> listeners )
    {
        final Artifact artifact = managedVersions.get( node.getKey() );

        // Before we update the version of the artifact, we need to know
        // whether we are working on a transitive dependency or not. This
        // allows depMgmt to always override transitive dependencies, while
        // explicit child override depMgmt (viz. depMgmt should only
        // provide defaults to children, but should override transitives).
        // We can do this by calling isChildOfRootNode on the current node.
        if ( ( artifact.getVersion() != null )
                 && ( !node.isChildOfRootNode() || node.getArtifact().getVersion() == null ) )
        {
            fireEvent( ResolutionListener.MANAGE_ARTIFACT_VERSION, listeners, node, artifact );
            node.getArtifact().setVersion( artifact.getVersion() );
        }

        if ( ( artifact.getScope() != null ) && ( !node.isChildOfRootNode() || node.getArtifact().getScope() == null ) )
        {
            fireEvent( ResolutionListener.MANAGE_ARTIFACT_SCOPE, listeners, node, artifact );
            node.getArtifact().setScope( artifact.getScope() );
        }

        if ( Artifact.SCOPE_SYSTEM.equals( node.getArtifact().getScope() ) && ( node.getArtifact().getFile() == null )
                 && ( artifact.getFile() != null ) )
        {
            fireEvent( ResolutionListener.MANAGE_ARTIFACT_SYSTEM_PATH, listeners, node, artifact );
            node.getArtifact().setFile( artifact.getFile() );
        }
    }

    /**
     * Check if the artifactScope needs to be updated. <a
     * href="http://docs.codehaus.org/x/IGU#DependencyMediationandConflictResolution-Scoperesolution">More info</a>.
     *
     * @param farthest farthest resolution node
     * @param nearest nearest resolution node
     * @param listeners
     */
    boolean checkScopeUpdate( final ResolutionNode farthest, final ResolutionNode nearest, final List<ResolutionListener> listeners )
    {
        boolean updateScope = false;
        final Artifact farthestArtifact = farthest.getArtifact();
        final Artifact nearestArtifact = nearest.getArtifact();

        /* farthest is runtime and nearest has lower priority, change to runtime */
        if ( Artifact.SCOPE_RUNTIME.equals( farthestArtifact.getScope() )
                 && ( Artifact.SCOPE_TEST.equals( nearestArtifact.getScope() )
                      || Artifact.SCOPE_PROVIDED.equals( nearestArtifact.getScope() ) ) )
        {
            updateScope = true;
        }

        /* farthest is compile and nearest is not (has lower priority), change to compile */
        if ( Artifact.SCOPE_COMPILE.equals( farthestArtifact.getScope() )
                 && !Artifact.SCOPE_COMPILE.equals( nearestArtifact.getScope() ) )
        {
            updateScope = true;
        }

        /* current POM rules all, if nearest is in current pom, do not update its artifactScope */
        if ( ( nearest.getDepth() < 2 ) && updateScope )
        {
            updateScope = false;

            fireEvent( ResolutionListener.UPDATE_SCOPE_CURRENT_POM, listeners, nearest, farthestArtifact );
        }

        if ( updateScope )
        {
            fireEvent( ResolutionListener.UPDATE_SCOPE, listeners, nearest, farthestArtifact );

            // previously we cloned the artifact, but it is more efficient to just update the artifactScope
            // if problems are later discovered that the original object needs its original artifactScope value,
            // cloning may
            // again be appropriate
            nearestArtifact.setScope( farthestArtifact.getScope() );
        }

        return updateScope;
    }

    private void fireEvent( final int event, final List<ResolutionListener> listeners, final ResolutionNode node )
    {
        fireEvent( event, listeners, node, null );
    }

    private void fireEvent( final int event, final List<ResolutionListener> listeners, final ResolutionNode node, final Artifact replacement )
    {
        fireEvent( event, listeners, node, replacement, null );
    }

    private void fireEvent( final int event, final List<ResolutionListener> listeners, final ResolutionNode node, final Artifact replacement,
                            final VersionRange newRange )
    {
        for ( final ResolutionListener listener : listeners )
        {
            switch ( event )
            {
                case ResolutionListener.TEST_ARTIFACT:
                    listener.testArtifact( node.getArtifact() );
                    break;
                case ResolutionListener.PROCESS_CHILDREN:
                    listener.startProcessChildren( node.getArtifact() );
                    break;
                case ResolutionListener.FINISH_PROCESSING_CHILDREN:
                    listener.endProcessChildren( node.getArtifact() );
                    break;
                case ResolutionListener.INCLUDE_ARTIFACT:
                    listener.includeArtifact( node.getArtifact() );
                    break;
                case ResolutionListener.OMIT_FOR_NEARER:
                    listener.omitForNearer( node.getArtifact(), replacement );
                    break;
                case ResolutionListener.OMIT_FOR_CYCLE:
                    listener.omitForCycle( node.getArtifact() );
                    break;
                case ResolutionListener.UPDATE_SCOPE:
                    listener.updateScope( node.getArtifact(), replacement.getScope() );
                    break;
                case ResolutionListener.UPDATE_SCOPE_CURRENT_POM:
                    listener.updateScopeCurrentPom( node.getArtifact(), replacement.getScope() );
                    break;
                case ResolutionListener.MANAGE_ARTIFACT_VERSION:
                    if ( listener instanceof ResolutionListenerForDepMgmt )
                    {
                        final ResolutionListenerForDepMgmt asImpl = (ResolutionListenerForDepMgmt) listener;
                        asImpl.manageArtifactVersion( node.getArtifact(), replacement );
                    }
                    else
                    {
                        listener.manageArtifact( node.getArtifact(), replacement );
                    }
                    break;
                case ResolutionListener.MANAGE_ARTIFACT_SCOPE:
                    if ( listener instanceof ResolutionListenerForDepMgmt )
                    {
                        final ResolutionListenerForDepMgmt asImpl = (ResolutionListenerForDepMgmt) listener;
                        asImpl.manageArtifactScope( node.getArtifact(), replacement );
                    }
                    else
                    {
                        listener.manageArtifact( node.getArtifact(), replacement );
                    }
                    break;
                case ResolutionListener.MANAGE_ARTIFACT_SYSTEM_PATH:
                    if ( listener instanceof ResolutionListenerForDepMgmt )
                    {
                        final ResolutionListenerForDepMgmt asImpl = (ResolutionListenerForDepMgmt) listener;
                        asImpl.manageArtifactSystemPath( node.getArtifact(), replacement );
                    }
                    else
                    {
                        listener.manageArtifact( node.getArtifact(), replacement );
                    }
                    break;
                case ResolutionListener.SELECT_VERSION_FROM_RANGE:
                    listener.selectVersionFromRange( node.getArtifact() );
                    break;
                case ResolutionListener.RESTRICT_RANGE:
                    if ( node.getArtifact().getVersionRange().hasRestrictions()
                             || replacement.getVersionRange().hasRestrictions() )
                    {
                        listener.restrictRange( node.getArtifact(), replacement, newRange );
                    }
                    break;
                default:
                    throw new IllegalStateException( "Unknown event: " + event );
            }
        }
    }

    @SuppressWarnings( "checkstyle:parameternumber" )
    public ArtifactResolutionResult collect( final Set<Artifact> artifacts, final Artifact originatingArtifact,
                                             final Map<String, Artifact> managedVersions, final ArtifactRepository localRepository,
                                             final List<ArtifactRepository> remoteRepositories,
                                             final ArtifactMetadataSource source, final ArtifactFilter filter,
                                             final List<ResolutionListener> listeners )
    {
        return collect( artifacts, originatingArtifact, managedVersions, localRepository, remoteRepositories, source,
                        filter, listeners, null );
    }

    public ArtifactResolutionResult collect( final Set<Artifact> artifacts, final Artifact originatingArtifact,
                                             final ArtifactRepository localRepository,
                                             final List<ArtifactRepository> remoteRepositories,
                                             final ArtifactMetadataSource source, final ArtifactFilter filter,
                                             final List<ResolutionListener> listeners )
    {
        return collect( artifacts, originatingArtifact, null, localRepository, remoteRepositories, source, filter,
                        listeners );
    }

}
