package org.apache.maven.repository.metadata;

import org.apache.maven.repository.metadata.GraphConflictResolutionPolicy;
import org.apache.maven.repository.metadata.MetadataGraphEdge;
import org.codehaus.plexus.PlexusTestCase;

/**
 *
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 * 
 */

public class DefaultGraphConflictResolutionPolicyTest
extends PlexusTestCase
{
	GraphConflictResolutionPolicy policy;
	MetadataGraphEdge e1;
	MetadataGraphEdge e2;
	MetadataGraphEdge e3;
    //------------------------------------------------------------------------------------------
    @Override
	protected void setUp() throws Exception
	{
		super.setUp();
    	policy = (GraphConflictResolutionPolicy) lookup( GraphConflictResolutionPolicy.ROLE, "default" );
    	e1 = new MetadataGraphEdge( "1.1", true, null, null, 2, 1 );
    	e2 = new MetadataGraphEdge( "1.2", true, null, null, 3, 2 );
    	e3 = new MetadataGraphEdge( "1.2", true, null, null, 2, 3 );
	}
    //------------------------------------------------------------------------------------------
    public void testDefaultPolicy()
        throws Exception
    {
    	MetadataGraphEdge res;
    	
    	res = policy.apply( e1, e2 );
    	assertEquals( "Wrong depth edge selected", "1.1", res.getVersion() );
    	
    	res = policy.apply( e1, e3 );
    	assertEquals( "Wrong version edge selected", "1.2", res.getVersion() );
    }
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
}
