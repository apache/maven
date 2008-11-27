package org.apache.maven.artifact.resolver.metadata;

import org.apache.maven.artifact.ArtifactScopeEnum;

/**
 * metadata graph vertice - just a wrapper around artifact's metadata
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 */
public class MetadataGraphVertex
    implements Comparable<MetadataGraphVertex>
{
    ArtifactMetadata md;

    // indications to use these in comparrison
    private boolean compareVersion = false;
    private boolean compareScope   = false;
    
	public MetadataGraphVertex( ArtifactMetadata md )
    {
        super();
        this.md = md;
    }
    
	public MetadataGraphVertex( ArtifactMetadata md, boolean compareVersion, boolean compareScope )
    {
        this(md);
        this.compareVersion = compareVersion;
        this.compareScope = compareScope;
    }

    public ArtifactMetadata getMd()
    {
        return md;
    }

    public void setMd( ArtifactMetadata md )
    {
        this.md = md;
    }
	//---------------------------------------------------------------------
    public boolean isCompareVersion()
	{
		return compareVersion;
	}

	public void setCompareVersion(boolean compareVersion)
	{
		this.compareVersion = compareVersion;
	}

	public boolean isCompareScope()
	{
		return compareScope;
	}

	public void setCompareScope(boolean compareScope)
	{
		this.compareScope = compareScope;
	}

	//---------------------------------------------------------------------
	@Override
	public String toString()
	{
		return "["+ (md == null ? "no metadata" : md.toString()) + "]";
	}
	//---------------------------------------------------------------------
	private static int compareStrings( String s1, String s2 )
	{
		if( s1 == null && s2 == null )
			return 0;

		if( s1 == null && s2 != null )
			return -1;

		if( s1 != null && s2 == null )
			return 1;
		
		return s1.compareTo(s2);
	}
	//---------------------------------------------------------------------
	public int compareTo(MetadataGraphVertex vertex)
	{
		if( vertex == null || vertex.getMd() == null )
			return 1;
		
		ArtifactMetadata vmd = vertex.getMd();

		if( vmd == null )
		{
			if( md == null )
				return 0;
			else
				return 1;
		}
		
		int g = compareStrings( md.groupId, vmd.groupId );
		
		if( g == 0 )
		{
			int a = compareStrings( md.artifactId, vmd.artifactId );
			if( a == 0 )
			{
				if( compareVersion )
				{
					int v = compareStrings( md.version, vmd.version );
					if( v == 0) {
						if( compareScope ) {
							String s1 = ArtifactScopeEnum.checkScope( md.artifactScope).getScope();
							String s2 = ArtifactScopeEnum.checkScope(vmd.artifactScope).getScope();
							return s1.compareTo(s2);
						}
						else
							return 0;
					}
					else
						return v;
				}
				else
					return 0;
			}
			else
				return a;
		}
		
		return g;
	}
	//---------------------------------------------------------------------
	@Override
	public boolean equals(Object vo)
	{
		if( vo == null || !(vo instanceof MetadataGraphVertex) )
			return false;
		return compareTo( (MetadataGraphVertex)vo ) == 0;
	}
	//---------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		if( md == null )
			return super.hashCode();
		StringBuilder hashString = new StringBuilder(128);
		hashString.append( md.groupId+"|" );
		hashString.append( md.artifactId+"|" );
		
		if( compareVersion )
			hashString.append(md.version + "|");
		
		if( compareScope )
			hashString.append(md.getArtifactScope() + "|");
		
		return  hashString.toString().hashCode();

//		BASE64Encoder b64 = new BASE64Encoder();
//		return  b64.encode( hashString.toString().getBytes() ).hashCode();
	}
	
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
}
