/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Goal.
 * 
 * @version $Revision$ $Date$
 */
public class Goal
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
     * Field disabled
     */
    private Boolean disabled;

    /**
     * Field configuration
     */
    private java.util.Properties configuration;

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
     * Method getId
     */
    public String getId()
    {
        return this.id;
    } //-- String getId() 

    /**
     * Method isDisabled
     */
    public Boolean isDisabled()
    {
        return this.disabled;
    } //-- Boolean isDisabled() 

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
     * Method setId
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId(String) 

}