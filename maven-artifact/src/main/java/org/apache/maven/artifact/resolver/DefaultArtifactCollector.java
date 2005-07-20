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
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

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
                                             ArtifactMetadataSource source, ArtifactFilter filter,
                                             ArtifactFactory artifactFactory, List listeners )
        throws ArtifactResolutionException
    {
        return collect( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository, remoteRepositories,
                        source, filter, artifactFactory, listeners );
    }

    public ArtifactResolutionResult collect( Set artifacts, Artifact originatingArtifact, Map managedVersions,
                                             ArtifactRepository localRepository, List remoteRepositories,
                                             ArtifactMetadataSource source, ArtifactFilter filter,
                                             ArtifactFactory artifactFactory, List listeners )
        throws ArtifactResolutionException
    {
        Map resolvedArtifacts = new HashMap();

        ResolutionNode root = new ResolutionNode( originatingArtifact, remoteRepositories );
        root.addDependencies( artifacts, remoteRepositories, filter );

        recurse( root, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source, filter,
                 artifactFactory, listeners );

        Set set = new HashSet();

        for ( Iterator i = resolvedArtifacts.values().iterator(); i.hasNext(); )
        {
            ResolutionNode node = (ResolutionNode) i.next();
            if ( !node.equals( root ) )
            {
                Artifact artifact = node.getArtifact();

                artifact.setDependencyTrail( node.getDependencyTrail() );

                set.add( node );
            }
        }

        ArtifactResolutionResult result = new ArtifactResolutionResult();

        result.setArtifactResolutionNodes( set );

        return result;
    }

    private void recurse( ResolutionNode node, Map resolvedArtifacts, Map managedVersions,
                          ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source,
                          ArtifactFilter filter, ArtifactFactory artifactFactory, List listeners )
        throws CyclicDependencyException, TransitiveArtifactResolutionException
    {
        fireEvent( ResolutionListener.TEST_ARTIFACT, listeners, node );

        // TODO: conflict resolvers, shouldn't be munging original artifact perhaps?
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

        ResolutionNode previous = (ResolutionNode) resolvedArtifacts.get( key );
        if ( previous != null )
        {
            // TODO: use as conflict resolver(s), chain and introduce version mediation

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
                fireEvent( ResolutionListener.OMIT_FOR_NEARER, listeners, node, previous.getArtifact() );
                return;
            }
        }

        resolvedArtifacts.put( key, node );

        fireEvent( ResolutionListener.INCLUDE_ARTIFACT, listeners, node );

        fireEvent( ResolutionListener.PROCESS_CHILDREN, listeners, node );

        for ( Iterator i = node.getChildrenIterator(); i.hasNext(); )
        {
            ResolutionNode child = (ResolutionNode) i.next();
            if ( !child.isResolved() )
            {
                try
                {
                    ResolutionGroup rGroup = source.retrieve( child.getArtifact(), localRepository,
                                                              remoteRepositories );
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
                    child.getArtifact().setDependencyTrail( node.getDependencyTrail() );
                    throw new TransitiveArtifactResolutionException( e.getMessage(), child.getArtifact(),
                                                                     remoteRepositories, e );
                }

                recurse( child, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source, filter,
                         artifactFactory, listeners );
            }
        }

        fireEvent( ResolutionListener.FINISH_PROCESSING_CHILDREN, listeners, node );
    }

    private void checkScopeUpdate( ResolutionNode node, ResolutionNode previous, List listeners )
    {
        boolean updateScope = false;
        Artifact newArtifact = node.getArtifact();
        Artifact previousArtifact = previous.getArtifact();

        if ( Artifact.SCOPE_RUNTIME.equals( newArtifact.getScope() ) && (
            Artifact.SCOPE_TEST.equals( previousArtifact.getScope() ) ||
                Artifact.SCOPE_PROVIDED.equals( previousArtifact.getScope() ) ) )
        {
            updateScope = true;
        }

        if ( Artifact.SCOPE_COMPILE.equals( newArtifact.getScope() ) &&
            !Artifact.SCOPE_COMPILE.equals( previousArtifact.getScope() ) )
        {
            updateScope = true;
        }

        if ( updateScope )
        {
            fireEvent( ResolutionListener.UPDATE_SCOPE, listeners, previous, newArtifact );

            // previously we cloned the artifact, but it is more effecient to just update the scope
            // if problems are later discovered that the original object needs its original scope value, cloning may
            // again be appropriate
            previousArtifact.setScope( newArtifact.getScope() );
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
                case ResolutionListener.MANAGE_ARTIFACT:
                    listener.manageArtifact( node.getArtifact(), replacement );
                    break;
                default:
                    throw new IllegalStateException( "Unknown event: " + event );
            }
        }
    }

}
