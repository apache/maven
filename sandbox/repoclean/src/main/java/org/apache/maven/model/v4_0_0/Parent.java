/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Parent.
 * 
 * @version $Revision$ $Date$
 */
public class Parent
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field artifactId
     */
    private String artifactId;

    /**
     * Field groupId
     */
    private String groupId;

    /**
     * Field version
     */
    private String version;

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
     * Method setVersion
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion(String) 

}