/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Dependency.
 * 
 * @version $Revision$ $Date$
 */
public class Dependency
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field id
     */
    private String id;

    /**
     * Field groupId
     */
    private String groupId;

    /**
     * Field artifactId
     */
    private String artifactId;

    /**
     * Field version
     */
    private String version;

    /**
     * Field url
     */
    private String url;

    /**
     * Field jar
     */
    private String jar;

    /**
     * Field type
     */
    private String type = "jar";

    /**
     * Field properties
     */
    private java.util.Properties properties;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addProperty
     * 
     * @param key
     * @param value
     */
    public void addProperty( String key, String value )
    {
        getProperties().put( key, value );
    } //-- void addProperty(String, String) 

    /**
     * Method getArtifactId
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId() 

    /**
     * Method getGroupId
     */
    public String getGroupId()
    {
        return this.groupId;
    } //-- String getGroupId() 

    /**
     * Method getId
     */
    public String getId()
    {
        return this.id;
    } //-- String getId() 

    /**
     * Method getJar
     */
    public String getJar()
    {
        return this.jar;
    } //-- String getJar() 

    /**
     * Method getProperties
     */
    public java.util.Properties getProperties()
    {
        if ( this.properties == null )
        {
            this.properties = new java.util.Properties();
        }

        return this.properties;
    } //-- java.util.Properties getProperties() 

    /**
     * Method getType
     */
    public String getType()
    {
        return this.type;
    } //-- String getType() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method getVersion
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion() 

    /**
     * Method setArtifactId
     * 
     * @param artifactId
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    } //-- void setArtifactId(String) 

    /**
     * Method setGroupId
     * 
     * @param groupId
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    } //-- void setGroupId(String) 

    /**
     * Method setId
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId(String) 

    /**
     * Method setJar
     * 
     * @param jar
     */
    public void setJar( String jar )
    {
        this.jar = jar;
    } //-- void setJar(String) 

    /**
     * Method setProperties
     * 
     * @param properties
     */
    public void setProperties( java.util.Properties properties )
    {
        this.properties = properties;
    } //-- void setProperties(java.util.Properties) 

    /**
     * Method setType
     * 
     * @param type
     */
    public void setType( String type )
    {
        this.type = type;
    } //-- void setType(String) 

    /**
     * Method setUrl
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl(String) 

    /**
     * Method setVersion
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion(String) 

    public String toString()
    {
        return groupId + "/" + type + "s:" + artifactId + "-" + version;
    }

    public String getKey()
    {
        return getId() + ":" + getType();
    }

    public String getArtifactDirectory()
    {
        return getGroupId();
    }

    public String getArtifact()
    {
        // If the jar name has been explicty set then use that. This
        // is when the <jar/> element is explicity used in the POM.
        if ( getJar() != null )
        {
            return getJar();
        }

        return getArtifactId() + "-" + getVersion() + "." + getExtension();
    }

    public String getExtension()
    {
        if ( "ejb".equals( getType() ) || "plugin".equals( getType() ) || "aspect".equals( getType() ) )
            return "jar";
        return getType();
    }

    public boolean isAddedToClasspath()
    {
        return ( "jar".equals( getType() ) || "ejb".equals( getType() ) );
    }

    public boolean isPlugin()
    {
        return ( "plugin".equals( getType() ) );
    }

    public String getProperty( String property )
    {
        return getProperties().getProperty( property );
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof Dependency ) )
        {
            return false;
        }

        Dependency d = (Dependency) o;
        return getId().equals( d.getId() );
    }

    public int hashCode()
    {
        return getId().hashCode();
    }
}