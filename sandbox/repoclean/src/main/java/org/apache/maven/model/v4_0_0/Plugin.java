/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Plugin.
 * 
 * @version $Revision$ $Date$
 */
public class Plugin
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field groupId
     */
    private String groupId = "maven";

    /**
     * Field artifactId
     */
    private String artifactId;

    /**
     * Field version
     */
    private String version;

    /**
     * Field disabled
     */
    private Boolean disabled;

    /**
     * Field configuration
     */
    private java.util.Properties configuration;

    /**
     * Field goals
     */
    private java.util.List goals;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addConfiguration
     * 
     * @param key
     * @param value
     */
    public void addConfiguration( String key, String value )
    {
        getConfiguration().put( key, value );
    } //-- void addConfiguration(String, String) 

    /**
     * Method addGoal
     * 
     * @param goal
     */
    public void addGoal( Goal goal )
    {
        getGoals().add( goal );
    } //-- void addGoal(Goal) 

    /**
     * Method getArtifactId
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId() 

    /**
     * Method getConfiguration
     */
    public java.util.Properties getConfiguration()
    {
        if ( this.configuration == null )
        {
            this.configuration = new java.util.Properties();
        }

        return this.configuration;
    } //-- java.util.Properties getConfiguration() 

    /**
     * Method getGoals
     */
    public java.util.List getGoals()
    {
        if ( this.goals == null )
        {
            this.goals = new java.util.ArrayList();
        }

        return this.goals;
    } //-- java.util.List getGoals() 

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
     * Method isDisabled
     */
    public Boolean isDisabled()
    {
        return this.disabled;
    } //-- Boolean isDisabled() 

    /**
     * Method removeGoal
     * 
     * @param goal
     */
    public void removeGoal( Goal goal )
    {
        getGoals().remove( goal );
    } //-- void removeGoal(Goal) 

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
     * Method setConfiguration
     * 
     * @param configuration
     */
    public void setConfiguration( java.util.Properties configuration )
    {
        this.configuration = configuration;
    } //-- void setConfiguration(java.util.Properties) 

    /**
     * Method setDisabled
     * 
     * @param disabled
     */
    public void setDisabled( Boolean disabled )
    {
        this.disabled = disabled;
    } //-- void setDisabled(Boolean) 

    /**
     * Method setGoals
     * 
     * @param goals
     */
    public void setGoals( java.util.List goals )
    {
        this.goals = goals;
    } //-- void setGoals(java.util.List) 

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