/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

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
     * Field type
     */
    private String type = "jar";

    /**
     * Field scope
     */
    private String scope = "compile";

    //-----------/
    //- Methods -/
    //-----------/

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
     * Method getScope
     */
    public String getScope()
    {
        return this.scope;
    } //-- String getScope() 

    /**
     * Method getType
     */
    public String getType()
    {
        return this.type;
    } //-- String getType() 

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
     * Method setScope
     * 
     * @param scope
     */
    public void setScope( String scope )
    {
        this.scope = scope;
    } //-- void setScope(String) 

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

    public String getId()
    {
        return groupId + ":" + artifactId + ":" + type + ":" + version;
    }

    public String getManagementKey()
    {
        return groupId + ":" + artifactId + ":" + type;
    }
}