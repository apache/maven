/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class Organization.
 * 
 * @version $Revision$ $Date$
 */
public class Organization
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field name
     */
    private String name;

    /**
     * Field url
     */
    private String url;

    /**
     * Field logo
     */
    private String logo;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getLogo
     */
    public String getLogo()
    {
        return this.logo;
    } //-- String getLogo() 

    /**
     * Method getName
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method setLogo
     * 
     * @param logo
     */
    public void setLogo( String logo )
    {
        this.logo = logo;
    } //-- void setLogo(String) 

    /**
     * Method setName
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName(String) 

    /**
     * Method setUrl
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl(String) 

}