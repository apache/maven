package org.apache.maven.artifact.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.artifact.resolver.conflict.GraphConflictResolutionException;
import org.apache.maven.artifact.resolver.conflict.GraphConflictResolver;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataGraph;
import org.apache.maven.artifact.resolver.metadata.MetadataGraphEdge;
import org.apache.maven.artifact.resolver.metadata.MetadataGraphVertex;

/**
 * default implementation of the metadata classpath transformer
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 * @plexus.component
 *
 */
public class DefaultClasspathTransformation
implements ClasspathTransformation
{
    /** @plexus.requirement */
    GraphConflictResolver conflictResolver;
    //----------------------------------------------------------------------------------------------------
	public ClasspathContainer transform(
						  MetadataGraph dirtyGraph
						, ArtifactScopeEnum scope
						, boolean resolve
										)
    throws MetadataGraphTransformationException
	{
		try {
			if(    dirtyGraph == null
				|| dirtyGraph.isEmpty()
			)
				return null;

			MetadataGraph cleanGraph = conflictResolver.resolveConflicts( dirtyGraph, scope );

			if(    cleanGraph == null
				|| cleanGraph.isEmpty() 
			)
				return null;
			
			ClasspathContainer cpc = new ClasspathContainer( scope );
			if( cleanGraph.isEmptyEdges() ) {
				// single entry in the classpath, populated from itself
				ArtifactMetadata amd = cleanGraph.getEntry().getMd();
				cpc.add( amd );
			} else {
				ClasspathGraphVisitor v = new ClasspathGraphVisitor( cleanGraph, cpc );
				MetadataGraphVertex entry = cleanGraph.getEntry();
				ArtifactMetadata md = entry.getMd();
				// entry point
				v.visit( entry ); //, md.getVersion(), md.getArtifactUri() );
			}
			
			return cpc;
		} catch (GraphConflictResolutionException e) {
			throw new MetadataGraphTransformationException(e);
		}
	}
    //===================================================================================================
    /**
     * Helper class to traverse graph. Required to make the containing method thread-safe
     * and yet use class level data to lessen stack usage in recursion
     */
    private class ClasspathGraphVisitor
    {
    	MetadataGraph graph;
    	ClasspathContainer cpc;
    	List<MetadataGraphVertex> visited;
    	//-----------------------------------------------------------------------
    	protected ClasspathGraphVisitor( MetadataGraph cleanGraph, ClasspathContainer cpc )
    	{
    		this.cpc = cpc;
    		this.graph = cleanGraph;

    		visited = new ArrayList<MetadataGraphVertex>( cleanGraph.getVertices().size() );
    	}
    	//-----------------------------------------------------------------------
    	protected void visit( MetadataGraphVertex node ) //, String version, String artifactUri )
    	{
    		ArtifactMetadata md = node.getMd();
    		if( visited.contains(node) )
    			return;

    		cpc.add( md );
//
//    		TreeSet<MetadataGraphEdge> deps = new TreeSet<MetadataGraphEdge>(
//    					new Comparator<MetadataGraphEdge>() 
//    					{
//    						public int compare( MetadataGraphEdge e1
//    										  , MetadataGraphEdge e2
//    										  )
//    						{
//    							if( e1.getDepth() == e2.getDepth() )
//    							{
//    								if( e2.getPomOrder() == e1.getPomOrder() )
//    									return e1.getTarget().toString().compareTo(e2.getTarget().toString() );
//
//    								return e2.getPomOrder() - e1.getPomOrder();
//    							}
//
//    							return e2.getDepth() - e1.getDepth();
//    						}
//    					}
//    				);

    		List<MetadataGraphEdge> exits = graph.getExcidentEdges(node);
    		
    		if( exits != null && exits.size() > 0)
    		{
    			MetadataGraphEdge[] sortedExits = exits.toArray( new MetadataGraphEdge[exits.size()] );
    			Arrays.sort( sortedExits
    					, 
    					new Comparator<MetadataGraphEdge>() 
    					{
    						public int compare( MetadataGraphEdge e1
    										  , MetadataGraphEdge e2
    										  )
    						{
    							if( e1.getDepth() == e2.getDepth() )
    							{
    								if( e2.getPomOrder() == e1.getPomOrder() )
    									return e1.getTarget().toString().compareTo(e2.getTarget().toString() );
    								return e2.getPomOrder() - e1.getPomOrder();
    							}

    							return e2.getDepth() - e1.getDepth();
    						}
    					}
    			);
    			
    			for( MetadataGraphEdge e : sortedExits ) {
    				MetadataGraphVertex targetNode = e.getTarget();
    				targetNode.getMd().setArtifactScope( e.getScope() );
    				targetNode.getMd().setWhy( e.getSource().getMd().toString() );
    				visit( targetNode );
    			}
    		}	
    		
    	}
    	//-----------------------------------------------------------------------
    	//-----------------------------------------------------------------------
    }
    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------
}



