/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Contributor.
 * 
 * @version $Revision$ $Date$
 */
public class Contributor
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
     * Field email
     */
    private String email;

    /**
     * Field url
     */
    private String url;

    /**
     * Field organization
     */
    private String organization;

    /**
     * Field roles
     */
    private java.util.List roles;

    /**
     * Field timezone
     */
    private String timezone;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addRole
     * 
     * @param string
     */
    public void addRole( String string )
    {
        getRoles().add( string );
    } //-- void addRole(String) 

    /**
     * Method getEmail
     */
    public String getEmail()
    {
        return this.email;
    } //-- String getEmail() 

    /**
     * Method getName
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method getOrganization
     */
    public String getOrganization()
    {
        return this.organization;
    } //-- String getOrganization() 

    /**
     * Method getRoles
     */
    public java.util.List getRoles()
    {
        if ( this.roles == null )
        {
            this.roles = new java.util.ArrayList();
        }

        return this.roles;
    } //-- java.util.List getRoles() 

    /**
     * Method getTimezone
     */
    public String getTimezone()
    {
        return this.timezone;
    } //-- String getTimezone() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method removeRole
     * 
     * @param string
     */
    public void removeRole( String string )
    {
        getRoles().remove( string );
    } //-- void removeRole(String) 

    /**
     * Method setEmail
     * 
     * @param email
     */
    public void setEmail( String email )
    {
        this.email = email;
    } //-- void setEmail(String) 

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
     * Method setOrganization
     * 
     * @param organization
     */
    public void setOrganization( String organization )
    {
        this.organization = organization;
    } //-- void setOrganization(String) 

    /**
     * Method setRoles
     * 
     * @param roles
     */
    public void setRoles( java.util.List roles )
    {
        this.roles = roles;
    } //-- void setRoles(java.util.List) 

    /**
     * Method setTimezone
     * 
     * @param timezone
     */
    public void setTimezone( String timezone )
    {
        this.timezone = timezone;
    } //-- void setTimezone(String) 

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