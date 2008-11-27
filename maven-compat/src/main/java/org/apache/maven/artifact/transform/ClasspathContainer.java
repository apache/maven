package org.apache.maven.artifact.transform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionException;
import org.apache.maven.artifact.resolver.metadata.MetadataTreeNode;

/**
 * classpath container that is aware of the classpath scope
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public class ClasspathContainer
implements Iterable<ArtifactMetadata>
{
	private List<ArtifactMetadata> classpath;
	private ArtifactScopeEnum      scope;
	
	//-------------------------------------------------------------------------------------------
	public ClasspathContainer(ArtifactScopeEnum scope)
	{
		this.scope = ArtifactScopeEnum.checkScope(scope);
	}
	//-------------------------------------------------------------------------------------------
	public ClasspathContainer(
			  List<ArtifactMetadata> classpath
			, ArtifactScopeEnum scope
							)
	{
		this(scope);
		this.classpath = classpath;
	}
	//-------------------------------------------------------------------------------------------
	public Iterator<ArtifactMetadata> iterator()
	{
		return classpath == null ? null : classpath.iterator() ;
	}
	//-------------------------------------------------------------------------------------------
	public ClasspathContainer add( ArtifactMetadata md )
	{
		if( classpath == null )
			classpath = new ArrayList<ArtifactMetadata>(16);
										
		classpath.add(md);
		
		return this;
	}
	//-------------------------------------------------------------------------------------------
	public List<ArtifactMetadata> getClasspath()
	{
		return classpath;
	}
	//-------------------------------------------------------------------------------------------
	public MetadataTreeNode getClasspathAsTree()
	throws MetadataResolutionException
	{
		if( classpath == null || classpath.size() < 1 )
			return null;
		
		MetadataTreeNode tree   = null;
		MetadataTreeNode parent = null;
		MetadataTreeNode node   = null;

		for( ArtifactMetadata md : classpath ) {
			node = new MetadataTreeNode( md, parent, md.isResolved(), md.getArtifactScope() );
			if( tree == null ) {
				tree = node;
			}
			
			if( parent != null ) {
				parent.setNChildren(1);
				parent.addChild(0, node);
			}
			
			parent = node;
			
		}
		return tree;
	}

	public void setClasspath(List<ArtifactMetadata> classpath)
	{
		this.classpath = classpath;
	}

	public ArtifactScopeEnum getScope()
	{
		return scope;
	}

	public void setScope(ArtifactScopeEnum scope)
	{
		this.scope = scope;
	}
	//-------------------------------------------------------------------------------------------
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(256);
		sb.append("[scope="+scope.getScope() );
		if(classpath != null)
			for( ArtifactMetadata md : classpath ) {
				sb.append(": "+md.toString()+'{'+md.getArtifactUri()+'}');
			}
		sb.append(']');
		return sb.toString();
	}
	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------
}
