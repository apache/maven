package org.apache.maven.artifact.resolver.conflict;

import org.apache.maven.artifact.resolver.metadata.MetadataGraphEdge;

/**
 *  MetadataGraph edge selection policy. Complements 
 *  GraphConflictResolver by being injected into it
 * 
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 * 
 * @version $Id$
 */

public interface GraphConflictResolutionPolicy
{
    static String ROLE = GraphConflictResolutionPolicy.class.getName();

    public MetadataGraphEdge apply( 
			  MetadataGraphEdge e1
			, MetadataGraphEdge e2
			);
}
