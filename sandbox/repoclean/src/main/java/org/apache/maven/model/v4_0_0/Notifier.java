/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Notifier.
 * 
 * @version $Revision$ $Date$
 */
public class Notifier
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field type
     */
    private String type = "email";

    /**
     * Field address
     */
    private String address;

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
     * Method getAddress
     */
    public String getAddress()
    {
        return this.address;
    } //-- String getAddress() 

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
     * Method getType
     */
    public String getType()
    {
        return this.type;
    } //-- String getType() 

    /**
     * Method setAddress
     * 
     * @param address
     */
    public void setAddress( String address )
    {
        this.address = address;
    } //-- void setAddress(String) 

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
     * Method setType
     * 
     * @param type
     */
    public void setType( String type )
    {
        this.type = type;
    } //-- void setType(String) 

}