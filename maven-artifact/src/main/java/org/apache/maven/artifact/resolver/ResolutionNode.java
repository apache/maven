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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;

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

    private boolean active = true;

    private List trail;

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
        if ( !artifacts.isEmpty() )
        {
            children = new ArrayList( artifacts.size() );

            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                if ( parents.contains( a.getDependencyConflictId() ) )
                {
                    a.setDependencyTrail( getDependencyTrail() );

                    throw new CyclicDependencyException( "A dependency has introduced a cycle", a );
                }

                children.add( new ResolutionNode( a, remoteRepositories, this ) );
            }
        }
        else
        {
            children = Collections.EMPTY_LIST;
        }
        trail = null;
    }

    public List getDependencyTrail()
        throws OverConstrainedVersionException
    {
        List trial = getTrail();

        List ret = new ArrayList( trial.size() );
        for ( Iterator i = trial.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            ret.add( artifact.getId() );
        }
        return ret;
    }

    private List getTrail()
        throws OverConstrainedVersionException
    {
        if ( trail == null )
        {
            List ids = new LinkedList();
            ResolutionNode node = this;
            while ( node != null )
            {
                Artifact artifact = node.getArtifact();
                if ( artifact.getVersion() == null )
                {
                    // set the recommended version
                    String version = artifact.getSelectedVersion().toString();
                    artifact.selectVersion( version );
                }

                ids.add( 0, artifact );
                node = node.parent;
            }
            trail = ids;
        }
        return trail;
    }

    public boolean isResolved()
    {
        return children != null;
    }

    public boolean isChildOfRootNode()
    {
        return parent != null && parent.parent == null;
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

    public boolean isActive()
    {
        return active;
    }

    public void enable()
    {
        this.active = true;
        // TODO: if it was null, we really need to go find them now... or is this taken care of by the ordering?
        if ( children != null )
        {
            for ( Iterator i = children.iterator(); i.hasNext(); )
            {
                ResolutionNode node = (ResolutionNode) i.next();
                node.enable();
            }
        }
    }

    public void disable()
    {
        this.active = false;
        if ( children != null )
        {
            for ( Iterator i = children.iterator(); i.hasNext(); )
            {
                ResolutionNode node = (ResolutionNode) i.next();
                node.disable();
            }
        }
    }

    public boolean filterTrail( ArtifactFilter filter )
        throws OverConstrainedVersionException
    {
        boolean success = true;
        if ( filter != null )
        {
            for ( Iterator i = getTrail().iterator(); i.hasNext() && success; )
            {
                Artifact artifact = (Artifact) i.next();
                if ( !filter.include( artifact ) )
                {
                    success = false;
                }
            }
        }
        return success;
    }

    public String toString()
    {
        return artifact.toString() + " (" + depth + "; " + ( active ? "enabled" : "disabled" ) + ")";
    }

}
