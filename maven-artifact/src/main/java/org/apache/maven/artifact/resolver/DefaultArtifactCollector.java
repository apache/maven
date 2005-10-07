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

                    // If it was optional, we don't add it or its children, just allow the update of the version and scope
                    if ( !node.getArtifact().isOptional() )
                    {
                        artifact.setDependencyTrail( node.getDependencyTrail() );

                        set.add( node );
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
        throws CyclicDependencyException, TransitiveArtifactResolutionException, OverConstrainedVersionException
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

                    // TODO: why do we force the version on it? what if they don't match?
                    if ( previousRange == null )
                    {
                        // version was already resolved
                        node.getArtifact().setVersion( previous.getArtifact().getVersion() );
                    }
                    else if ( currentRange == null )
                    {
                        // version was already resolved
                        previous.getArtifact().setVersion( node.getArtifact().getVersion() );
                    }
                    else
                    {
                        // TODO: shouldn't need to double up on this work, only done for simplicity of handling recommended
                        // version but the restriction is identical
                        previous.getArtifact().setVersionRange( previousRange.restrict( currentRange ) );
                        node.getArtifact().setVersionRange( currentRange.restrict( previousRange ) );
                    }

                    // Conflict Resolution
                    // TODO: use as conflict resolver(s), chain

                    // TODO: should this be part of mediation?
                    // previous one is more dominant
                    if ( previous.getDepth() <= node.getDepth() )
                    {
                        checkScopeUpdate( node, previous, listeners );
                    }
                    else
                    {
                        checkScopeUpdate( previous, node, listeners );
                    }

                    if ( previous.getDepth() <= node.getDepth() )
                    {
                        // previous was nearer
                        fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, node, previous.getArtifact() );
                        node.disable();
                    }
                    else
                    {
                        fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, previous, node.getArtifact() );
                        previous.disable();
                    }
                }
            }
        }
        else
        {
            previousNodes = new ArrayList();
            resolvedArtifacts.put( key, previousNodes );
        }
        previousNodes.add( node );

        fireEvent( ResolutionListener.INCLUDE_ARTIFACT, listeners, node );

        if ( node.isActive() )
        {
            fireEvent( ResolutionListener.PROCESS_CHILDREN, listeners, node );

            for ( Iterator i = node.getChildrenIterator(); i.hasNext(); )
            {
                ResolutionNode child = (ResolutionNode) i.next();
                // We leave in optional ones, but don't pick up its dependencies
                if ( !child.isResolved() && !child.getArtifact().isOptional() )
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

                        ResolutionGroup rGroup = source.retrieve( artifact, localRepository, remoteRepositories );
                        child.addDependencies( rGroup.getArtifacts(), rGroup.getResolutionRepositories(), filter );
                    }
                    catch ( CyclicDependencyException e )
                    {
                        // would like to throw this, but we have crappy stuff in the repo
                        // no logger to use here either just now

                        // TODO: should the remoteRepositories list be null here?!
                        fireEvent( ResolutionListener.OMIT_FOR_CYCLE, listeners,
                                   new ResolutionNode( e.getArtifact(), null, child ) );
                    }
                    catch ( ArtifactMetadataRetrievalException e )
                    {
                        artifact.setDependencyTrail( node.getDependencyTrail() );
                        throw new TransitiveArtifactResolutionException( e.getMessage(), artifact, remoteRepositories,
                                                                         e );
                    }

                    recurse( child, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source,
                             filter, listeners );
                }
            }

            fireEvent( ResolutionListener.FINISH_PROCESSING_CHILDREN, listeners, node );
        }
    }

    private void checkScopeUpdate( ResolutionNode farthest, ResolutionNode nearest, List listeners )
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
    }

    private void fireEvent( int event, List listeners, ResolutionNode node )
    {
        fireEvent( event, listeners, node, null );
    }

    private void fireEvent( int event, List listeners, ResolutionNode node, Artifact replacement )
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
                case ResolutionListener.MANAGE_ARTIFACT:
                    listener.manageArtifact( node.getArtifact(), replacement );
                    break;
                case ResolutionListener.SELECT_VERSION_FROM_RANGE:
                    listener.selectVersionFromRange( node.getArtifact() );
                    break;
                default:
                    throw new IllegalStateException( "Unknown event: " + event );
            }
        }
    }

}
