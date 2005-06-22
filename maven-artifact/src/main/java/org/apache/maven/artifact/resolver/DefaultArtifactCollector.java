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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

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
                                             ArtifactMetadataSource source, ArtifactFilter filter,
                                             ArtifactFactory artifactFactory )
        throws ArtifactResolutionException
    {
        return collect( artifacts, originatingArtifact, Collections.EMPTY_MAP, localRepository, remoteRepositories,
                        source, filter, artifactFactory );
    }

    public ArtifactResolutionResult collect( Set artifacts, Artifact originatingArtifact, Map managedVersions,
                                             ArtifactRepository localRepository, List remoteRepositories,
                                             ArtifactMetadataSource source, ArtifactFilter filter,
                                             ArtifactFactory artifactFactory )
        throws ArtifactResolutionException
    {
        Map resolvedArtifacts = new HashMap();

        ResolutionNode root = new ResolutionNode( originatingArtifact );
        root.addDependencies( artifacts, filter );

        recurse( root, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source, filter,
                 artifactFactory );

        Set set = new HashSet();

        for ( Iterator i = resolvedArtifacts.values().iterator(); i.hasNext(); )
        {
            ResolutionNode node = (ResolutionNode) i.next();
            if ( node != root )
            {
                set.add( node.getArtifact() );
            }
        }

        ArtifactResolutionResult result = new ArtifactResolutionResult();

        result.setArtifacts( set );

        return result;
    }

    private void recurse( ResolutionNode node, Map resolvedArtifacts, Map managedVersions,
                          ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source,
                          ArtifactFilter filter, ArtifactFactory artifactFactory )
        throws ArtifactResolutionException
    {
        // TODO: conflict resolvers, shouldn't be munging original artifact perhaps?
        Object key = node.getKey();
        if ( managedVersions.containsKey( key ) )
        {
            Artifact artifact = (Artifact) managedVersions.get( key );
            // TODO: apply scope. assign whole artifact, except that the missing bits must be filled in
            if ( artifact.getVersion() != null )
            {
                node.getArtifact().setVersion( artifact.getVersion() );
            }
        }

        ResolutionNode previous = (ResolutionNode) resolvedArtifacts.get( key );
        if ( previous != null )
        {
            // TODO: conflict resolvers

            // previous one is more dominant
            if ( previous.getDepth() <= node.getDepth() )
            {
                boolean updateScope = false;
                Artifact newArtifact = node.getArtifact();
                Artifact previousArtifact = previous.getArtifact();

                if ( Artifact.SCOPE_RUNTIME.equals( newArtifact.getScope() ) &&
                    ( Artifact.SCOPE_TEST.equals( previousArtifact.getScope() ) ||
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
                    Artifact artifact = artifactFactory.createArtifact( previousArtifact.getGroupId(),
                                                                        previousArtifact.getArtifactId(),
                                                                        previousArtifact.getVersion(),
                                                                        newArtifact.getScope(),
                                                                        previousArtifact.getType() );
                    // TODO: can I just change the scope?
                    previous.setArtifact( artifact );
                }

                return;
            }
            else
            {
                boolean updateScope = false;
                Artifact previousArtifact = previous.getArtifact();
                Artifact newArtifact = node.getArtifact();

                if ( Artifact.SCOPE_RUNTIME.equals( previousArtifact.getScope() ) &&
                    ( Artifact.SCOPE_TEST.equals( newArtifact.getScope() ) ||
                        Artifact.SCOPE_PROVIDED.equals( newArtifact.getScope() ) ) )
                {
                    updateScope = true;
                }

                if ( Artifact.SCOPE_COMPILE.equals( previousArtifact.getScope() ) &&
                    !Artifact.SCOPE_COMPILE.equals( newArtifact.getScope() ) )
                {
                    updateScope = true;
                }

                if ( updateScope )
                {
                    Artifact artifact = artifactFactory.createArtifact( newArtifact.getGroupId(),
                                                                        newArtifact.getArtifactId(),
                                                                        newArtifact.getVersion(),
                                                                        previousArtifact.getScope(),
                                                                        newArtifact.getType() );
                    // TODO: can I just change the scope?
                    node.setArtifact( artifact );
                }

            }
        }

        resolvedArtifacts.put( key, node );

        for ( Iterator i = node.getChildrenIterator(); i.hasNext(); )
        {
            ResolutionNode child = (ResolutionNode) i.next();
            if ( !child.isResolved() )
            {
                try
                {
                    Set artifacts = source.retrieve( child.getArtifact(), localRepository, remoteRepositories );
                    child.addDependencies( artifacts, filter );
                }
                catch ( ArtifactMetadataRetrievalException e )
                {
                    throw new TransitiveArtifactResolutionException( e.getMessage(), child.getArtifact(),
                                                                     remoteRepositories, e );
                }

                recurse( child, resolvedArtifacts, managedVersions, localRepository, remoteRepositories, source, filter,
                         artifactFactory );
            }
        }
    }


    private static class ResolutionNode
    {
        private Artifact artifact;

        private final ResolutionNode parent;

        private List children = null;

        private final int depth;

        public ResolutionNode( Artifact artifact )
        {
            this.artifact = artifact;
            this.parent = null;
            this.depth = 0;
        }

        public ResolutionNode( Artifact artifact, ResolutionNode parent )
        {
            this.artifact = artifact;
            this.parent = parent;
            this.depth = parent.depth + 1;
        }

        public Artifact getArtifact()
        {
            return artifact;
        }

        public Object getKey()
        {
            return artifact.getDependencyConflictId();
        }

        public void addDependencies( Set artifacts, ArtifactFilter filter )
        {
            children = new ArrayList( artifacts.size() );

            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                if ( filter == null || filter.include( a ) )
                {
                    children.add( new ResolutionNode( a, this ) );
                }
            }
        }

        public boolean isResolved()
        {
            return children != null;
        }

        public Iterator getChildrenIterator()
        {
            return children.iterator();
        }

        public int getDepth()
        {
            return depth;
        }

        public void setArtifact( Artifact artifact )
        {
            this.artifact = artifact;
        }
    }
}
