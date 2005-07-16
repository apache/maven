package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ResolutionNode
{
    private Artifact artifact;

    private List children = null;

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
        throws CyclicDependencyException
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
    {
        List path = new LinkedList();
        ResolutionNode node = this;
        while ( node != null )
        {
            path.add( 0, node.getArtifact().getId() );
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

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }
    
    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

}
