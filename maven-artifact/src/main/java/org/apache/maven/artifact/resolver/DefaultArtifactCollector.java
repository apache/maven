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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of the artifact collector.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultArtifactCollector
    implements ArtifactCollector
{
    public ArtifactResolutionResult collect( Set artifacts, Artifact originatingArtifact,
                                             ArtifactRepository localRepository, List remoteRepositories,
                                             ArtifactMetadataSource source, ArtifactFilter filter, List listeners )
        throws ArtifactResolutionException
    {
        return collect( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository, remoteRepositories,
                        source, filter, listeners );
    }

    public ArtifactResolutionResult collect( Set artifacts, Artifact originatingArtifact, Map managedVersions,
                                             ArtifactRepository localRepository, List remoteRepositories,
                                             ArtifactMetadataSource source, ArtifactFilter filter, List listeners )
        throws ArtifactResolutionException
    {
        Map resolvedArtifacts = new HashMap();

        ResolutionNode root = new ResolutionNode( originatingArtifact, remoteRepositories );

        root.addDependencies( artifacts, remoteRepositories, filter );

        recurse( root, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source, filter,
                 listeners );

        Set set = new HashSet();

        for ( Iterator i = resolvedArtifacts.values().iterator(); i.hasNext(); )
        {
            List nodes = (List) i.next();
            for ( Iterator j = nodes.iterator(); j.hasNext(); )
            {
                ResolutionNode node = (ResolutionNode) j.next();
                if ( !node.equals( root ) && node.isActive() )
                {
                    Artifact artifact = node.getArtifact();

                    if ( node.filterTrail( filter ) )
                    {
                        // If it was optional and not a direct dependency,
                        // we don't add it or its children, just allow the update of the version and scope
                        if ( node.isChildOfRootNode() || !artifact.isOptional() )
                        {
                            artifact.setDependencyTrail( node.getDependencyTrail() );

                            set.add( node );
                        }
                    }
                }
            }
        }

        ArtifactResolutionResult result = new ArtifactResolutionResult();
        result.setArtifactResolutionNodes( set );
        return result;
    }

    private void recurse( ResolutionNode node, Map resolvedArtifacts, Map managedVersions,
                          ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source,
                          ArtifactFilter filter, List listeners )
        throws CyclicDependencyException, ArtifactResolutionException, OverConstrainedVersionException
    {
        fireEvent( ResolutionListener.TEST_ARTIFACT, listeners, node );

        // TODO: use as a conflict resolver
        Object key = node.getKey();
        if ( managedVersions.containsKey( key ) )
        {
            Artifact artifact = (Artifact) managedVersions.get( key );

            fireEvent( ResolutionListener.MANAGE_ARTIFACT, listeners, node, artifact );

            if ( artifact.getVersion() != null )
            {
                node.getArtifact().setVersion( artifact.getVersion() );
            }
            if ( artifact.getScope() != null )
            {
                node.getArtifact().setScope( artifact.getScope() );
            }
        }

        List previousNodes = (List) resolvedArtifacts.get( key );
        if ( previousNodes != null )
        {
            for ( Iterator i = previousNodes.iterator(); i.hasNext(); )
            {
                ResolutionNode previous = (ResolutionNode) i.next();

                if ( previous.isActive() )
                {
                    // Version mediation
                    VersionRange previousRange = previous.getArtifact().getVersionRange();
                    VersionRange currentRange = node.getArtifact().getVersionRange();

                    if ( previousRange != null && currentRange != null )
                    {
                        // TODO: shouldn't need to double up on this work, only done for simplicity of handling recommended
                        // version but the restriction is identical
                        VersionRange newRange = previousRange.restrict( currentRange );
                        // TODO: ick. this forces the OCE that should have come from the previous call. It is still correct
                        if ( newRange.isSelectedVersionKnown( previous.getArtifact() ) )
                        {
                            fireEvent( ResolutionListener.RESTRICT_RANGE, listeners, node, previous.getArtifact(),
                                       newRange );
                        }
                        previous.getArtifact().setVersionRange( newRange );
                        node.getArtifact().setVersionRange( currentRange.restrict( previousRange ) );

                        //Select an appropriate available version from the (now restricted) range
                        //Note this version was selected before to get the appropriate POM
                        //But it was reset by the call to setVersionRange on restricting the version
                        ResolutionNode[] resetNodes = {previous, node};
                        for ( int j = 0; j < 2; j++ )
                        {
                            Artifact resetArtifact = resetNodes[j].getArtifact();
                            if ( resetArtifact.getVersion() == null && resetArtifact.getVersionRange() != null &&
                                resetArtifact.getAvailableVersions() != null )
                            {

                                resetArtifact.selectVersion( resetArtifact.getVersionRange().matchVersion(
                                    resetArtifact.getAvailableVersions() ).toString() );
                                fireEvent( ResolutionListener.SELECT_VERSION_FROM_RANGE, listeners, resetNodes[j] );
                            }
                        }
                    }

                    // Conflict Resolution
                    // TODO: use as conflict resolver(s), chain

                    // TODO: should this be part of mediation?
                    // previous one is more dominant
                    ResolutionNode nearest;
                    ResolutionNode farthest;
                    if ( previous.getDepth() <= node.getDepth() )
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
                        // if we need to update scope of nearest to use farthest scope, use the nearest version, but farthest scope
                        nearest.disable();
                        farthest.getArtifact().setVersion( nearest.getArtifact().getVersion() );
                    }
                    else
                    {
                        farthest.disable();
                    }
                    fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, farthest, nearest.getArtifact() );
                }
            }
        }
        else
        {
            previousNodes = new ArrayList();
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

            for ( Iterator i = node.getChildrenIterator(); i.hasNext(); )
            {
                ResolutionNode child = (ResolutionNode) i.next();
                // We leave in optional ones, but don't pick up its dependencies
                if ( !child.isResolved() && ( !child.getArtifact().isOptional() || child.isChildOfRootNode() ) )
                {
                    Artifact artifact = child.getArtifact();
                    try
                    {
                        if ( artifact.getVersion() == null )
                        {
                            // set the recommended version
                            // TODO: maybe its better to just pass the range through to retrieval and use a transformation?
                            ArtifactVersion version;
                            if ( !artifact.isSelectedVersionKnown() )
                            {
                                List versions = artifact.getAvailableVersions();
                                if ( versions == null )
                                {
                                    versions = source.retrieveAvailableVersions( artifact, localRepository,
                                                                                 remoteRepositories );
                                    artifact.setAvailableVersions( versions );
                                }

                                VersionRange versionRange = artifact.getVersionRange();

                                version = versionRange.matchVersion( versions );

                                if ( version == null )
                                {
                                    if ( versions.isEmpty() )
                                    {
                                        throw new OverConstrainedVersionException(
                                            "No versions are present in the repository for the artifact with a range " +
                                                versionRange, artifact, remoteRepositories );
                                    }
                                    else
                                    {
                                        throw new OverConstrainedVersionException( "Couldn't find a version in " +
                                            versions + " to match range " + versionRange, artifact,
                                                                                          remoteRepositories );
                                    }
                                }
                            }
                            else
                            {
                                version = artifact.getSelectedVersion();
                            }

                            artifact.selectVersion( version.toString() );
                            fireEvent( ResolutionListener.SELECT_VERSION_FROM_RANGE, listeners, child );
                        }

                        artifact.setDependencyTrail( node.getDependencyTrail() );
                        ResolutionGroup rGroup = source.retrieve( artifact, localRepository, remoteRepositories );

                        //TODO might be better to have source.retreive() throw a specific exception for this situation
                        //and catch here rather than have it return null
                        if ( rGroup == null )
                        {
                            //relocated dependency artifact is declared excluded, no need to add and recurse further
                            continue;
                        }

                        child.addDependencies( rGroup.getArtifacts(), rGroup.getResolutionRepositories(), filter );
                    }
                    catch ( CyclicDependencyException e )
                    {
                        // would like to throw this, but we have crappy stuff in the repo

                        fireEvent( ResolutionListener.OMIT_FOR_CYCLE, listeners,
                                   new ResolutionNode( e.getArtifact(), remoteRepositories, child ) );
                    }
                    catch ( ArtifactMetadataRetrievalException e )
                    {
                        artifact.setDependencyTrail( node.getDependencyTrail() );
                        throw new ArtifactResolutionException(
                            "Unable to get dependency information: " + e.getMessage(), artifact, remoteRepositories,
                            e );
                    }

                    recurse( child, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source,
                             filter, listeners );
                }
            }

            fireEvent( ResolutionListener.FINISH_PROCESSING_CHILDREN, listeners, node );
        }
    }

    /**
     * Check if the scope needs to be updated.
     * <a href="http://docs.codehaus.org/x/IGU#DependencyMediationandConflictResolution-Scoperesolution">More info</a>.
     *
     * @param farthest  farthest resolution node
     * @param nearest   nearest resolution node
     * @param listeners
     */
    boolean checkScopeUpdate( ResolutionNode farthest, ResolutionNode nearest, List listeners )
    {
        boolean updateScope = false;
        Artifact farthestArtifact = farthest.getArtifact();
        Artifact nearestArtifact = nearest.getArtifact();

        if ( Artifact.SCOPE_RUNTIME.equals( farthestArtifact.getScope() ) && (
            Artifact.SCOPE_TEST.equals( nearestArtifact.getScope() ) ||
                Artifact.SCOPE_PROVIDED.equals( nearestArtifact.getScope() ) ) )
        {
            updateScope = true;
        }

        if ( Artifact.SCOPE_COMPILE.equals( farthestArtifact.getScope() ) &&
            !Artifact.SCOPE_COMPILE.equals( nearestArtifact.getScope() ) )
        {
            updateScope = true;
        }

        // current POM rules all
        if ( nearest.getDepth() < 2 && updateScope )
        {
            updateScope = false;

            fireEvent( ResolutionListener.UPDATE_SCOPE_CURRENT_POM, listeners, nearest, farthestArtifact );
        }

        if ( updateScope )
        {
            fireEvent( ResolutionListener.UPDATE_SCOPE, listeners, nearest, farthestArtifact );

            // previously we cloned the artifact, but it is more effecient to just update the scope
            // if problems are later discovered that the original object needs its original scope value, cloning may
            // again be appropriate
            nearestArtifact.setScope( farthestArtifact.getScope() );
        }

        return updateScope;
    }

    private void fireEvent( int event, List listeners, ResolutionNode node )
    {
        fireEvent( event, listeners, node, null );
    }

    private void fireEvent( int event, List listeners, ResolutionNode node, Artifact replacement )
    {
        fireEvent( event, listeners, node, replacement, null );
    }

    private void fireEvent( int event, List listeners, ResolutionNode node, Artifact replacement,
                            VersionRange newRange )
    {
        for ( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            ResolutionListener listener = (ResolutionListener) i.next();

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
                    String version = node.getArtifact().getVersion();
                    String replacementVersion = replacement.getVersion();
                    if ( version != null ? !version.equals( replacementVersion ) : replacementVersion != null )
                    {
                        listener.omitForNearer( node.getArtifact(), replacement );
                    }
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
                case ResolutionListener.MANAGE_ARTIFACT:
                    listener.manageArtifact( node.getArtifact(), replacement );
                    break;
                case ResolutionListener.SELECT_VERSION_FROM_RANGE:
                    listener.selectVersionFromRange( node.getArtifact() );
                    break;
                case ResolutionListener.RESTRICT_RANGE:
                    if ( node.getArtifact().getVersionRange().hasRestrictions() ||
                        replacement.getVersionRange().hasRestrictions() )
                    {
                        listener.restrictRange( node.getArtifact(), replacement, newRange );
                    }
                    break;
                default:
                    throw new IllegalStateException( "Unknown event: " + event );
            }
        }
    }

}
