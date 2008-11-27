package org.apache.maven.artifact.resolver.metadata;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactScopeEnum;

/**
 * Artifact Metadata that is resolved independent of Artifact itself.
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public class ArtifactMetadata
{
	/** 
	 * standard glorified artifact coordinates
	 */
    protected String groupId;
    protected String artifactId;
    protected String version;
    protected String type;
    protected ArtifactScopeEnum artifactScope;
    protected String classifier;

    /** 
     * explanation: why this MD was chosen over it's siblings
     * in the resulting structure (classpath for now) 
     */
    protected String why;
    
    /** dependencies of the artifact behind this metadata */
    protected Collection<ArtifactMetadata> dependencies;

    /** metadata URI */
    protected String uri;

    /** is metadata found anywhere */
    protected boolean resolved = false;
    
    /** does the actual artifact for this metadata exists */
    protected boolean artifactExists = false;
    /** artifact URI */
    protected String artifactUri;

    /** error message  */
    private String error;
    
    //------------------------------------------------------------------
    /**
     * 
     */
    public ArtifactMetadata( String name )
    {
    	if( name == null )
    		return;
    	int ind1 = name.indexOf(':');
    	int ind2 = name.lastIndexOf(':');
    	
    	if( ind1 == -1 || ind2 == -1 )
    		return;
    	
		this.groupId = name.substring(0, ind1);
    	if( ind1 == ind2 )
    	{
    		this.artifactId = name.substring(ind1+1);
    	}
    	else
    	{
    		this.artifactId = name.substring( ind1+1, ind2 );
    		this.version    = name.substring( ind2+1 );
    	}
    }
    
    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId,
                             String name,
                             String version )
    {
        this( groupId, name, version, null );
    }
    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId,
                             String name,
                             String version,
                             String type )
    {
        this( groupId, name, version, type, null );
    }

    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId,
                             String name,
                             String version,
                             String type,
                             ArtifactScopeEnum artifactScope )
    {
        this( groupId, name, version, type, artifactScope, null );
    }

    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId,
                             String name,
                             String version,
                             String type,
                             ArtifactScopeEnum artifactScope,
                             String classifier )
    {
        this( groupId, name, version, type, artifactScope, classifier, null );
    }
    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId,
                             String name,
                             String version,
                             String type,
                             ArtifactScopeEnum artifactScope,
                             String classifier
                             , String artifactUri
                             )
    {
        this( groupId, name, version, type, artifactScope, classifier, artifactUri, null, true, null );
    }
    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId
    						, String name
                             , String version
                             , String type
                             , ArtifactScopeEnum artifactScope
                             , String classifier
                             , String artifactUri
                             , String why
                             , boolean resolved
                             , String error
                             )
    {
        this.groupId = groupId;
        this.artifactId = name;
        this.version = version;
        this.type = type;
        this.artifactScope = artifactScope;
        this.classifier = classifier;
        this.artifactUri = artifactUri;
        this.why = why;
        this.resolved = resolved;
        this.error = error;
    }
    //------------------------------------------------------------------
    public ArtifactMetadata( String groupId
    						, String name
                             , String version
                             , String type
                             , String scopeString
                             , String classifier
                             , String artifactUri
                             , String why
                             , boolean resolved
                             , String error
                             )
    {
        this( groupId
        	, name
        	, version
        	, type
    		, scopeString == null ? ArtifactScopeEnum.DEFAULT_SCOPE : ArtifactScopeEnum.valueOf(scopeString)
    		, classifier
    		, artifactUri
    		, why
    		, resolved
    		, error
    		);
    }

    //------------------------------------------------------------------
    public ArtifactMetadata( Artifact af )
    {
        /*
        if ( af != null )
        {
            init( af );
        }
        */
    }
    //------------------------------------------------------------------
//    public void init( ArtifactMetadata af )
//    {
//        setGroupId( af.getGroupId() );
//        setArtifactId( af.getArtifactId() );
//        setVersion( af.getVersion() );
//        setType( af.getType() );
//        setScope( af.getScope() );
//        setClassifier( af.getClassifier() );
//        //setUri( af.getDownloadUrl() );
//
//        this.resolved = af.isResolved();
//    }

    //------------------------------------------------------------------
    @Override
    public String toString()
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    //------------------------------------------------------------------
    public String toDomainString()
    {
        return groupId + ":" + artifactId;
    }

    //------------------------------------------------------------------
    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String name )
    {
        this.artifactId = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getType()
    {
        return type;
    }

    public String getCheckedType()
    {
        return type == null ? "jar" : type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public ArtifactScopeEnum getArtifactScope()
    {
        return artifactScope == null ? ArtifactScopeEnum.DEFAULT_SCOPE : artifactScope;
    }

    public void setArtifactScope( ArtifactScopeEnum artifactScope )
    {
        this.artifactScope = artifactScope;
    }

    public void setScope( String scope )
    {
        this.artifactScope = scope == null
            ? ArtifactScopeEnum.DEFAULT_SCOPE
            : ArtifactScopeEnum.valueOf( scope )
            ;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public boolean isResolved()
    {
        return resolved;
    }

    public void setResolved( boolean resolved )
    {
        this.resolved = resolved;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri( String uri )
    {
        this.uri = uri;
    }

    public String getScope()
    {
        return getArtifactScope().getScope();
    }

    public ArtifactScopeEnum getScopeAsEnum()
    {
        return artifactScope == null ? ArtifactScopeEnum.DEFAULT_SCOPE : artifactScope;
    }
    
    public boolean isArtifactExists()
	{
		return artifactExists;
	}

	public void setArtifactExists(boolean artifactExists)
	{
		this.artifactExists = artifactExists;
	}
	
	
    public Collection<ArtifactMetadata> getDependencies()
	{
		return dependencies;
	}

	public void setDependencies(Collection<ArtifactMetadata> dependencies)
	{
		this.dependencies = dependencies;
	}

	public String getArtifactUri()
	{
		return artifactUri;
	}

	public void setArtifactUri(String artifactUri)
	{
		this.artifactUri = artifactUri;
	}

	
    public String getWhy()
	{
		return why;
	}
	public void setWhy(String why)
	{
		this.why = why;
	}
	//-------------------------------------------------------------------
	public String getError()
	{
		return error;
	}
	public void setError(String error)
	{
		this.error = error;
	}
	public boolean isError()
	{
		return error == null;
	}
	//------------------------------------------------------------------
	public String getDependencyConflictId()
    {
        return groupId + ":" + artifactId;
    }
    //------------------------------------------------------------------
    //------------------------------------------------------------------
}
