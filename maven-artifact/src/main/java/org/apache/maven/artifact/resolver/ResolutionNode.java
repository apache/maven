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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ResolutionNode
{
    private final Artifact artifact;

    private List children;

    private final List parents;

    private final int depth;

    private final ResolutionNode parent;

    private final List remoteRepositories;

    public ResolutionNode( Artifact artifact, List remoteRepositories )
    {
        this.artifact = artifact;
        this.remoteRepositories = remoteRepositories;
        this.depth = 0;
        this.parents = Collections.EMPTY_LIST;
        this.parent = null;
    }

    public ResolutionNode( Artifact artifact, List remoteRepositories, ResolutionNode parent )
    {
        this.artifact = artifact;
        this.remoteRepositories = remoteRepositories;
        this.depth = parent.depth + 1;
        this.parents = new ArrayList();
        this.parents.addAll( parent.parents );
        this.parents.add( parent.getKey() );
        this.parent = parent;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public Object getKey()
    {
        return artifact.getDependencyConflictId();
    }

    public void addDependencies( Set artifacts, List remoteRepositories, ArtifactFilter filter )
        throws CyclicDependencyException, OverConstrainedVersionException
    {
        children = new ArrayList( artifacts.size() );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( filter == null || filter.include( a ) )
            {
                if ( parents.contains( a.getDependencyConflictId() ) )
                {
                    a.setDependencyTrail( getDependencyTrail() );

                    throw new CyclicDependencyException( "A dependency has introduced a cycle", a );
                }

                children.add( new ResolutionNode( a, remoteRepositories, this ) );
            }
        }
    }

    public List getDependencyTrail()
        throws OverConstrainedVersionException
    {
        List path = new LinkedList();
        ResolutionNode node = this;
        while ( node != null )
        {
            Artifact artifact = node.getArtifact();
            if ( artifact.getVersion() == null )
            {
                // set the recommended version
                VersionRange versionRange = artifact.getVersionRange();
                String version = versionRange.getSelectedVersion().toString();
                artifact.selectVersion( version );
            }

            path.add( 0, artifact.getId() );
            node = node.parent;
        }
        return path;
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

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

}
