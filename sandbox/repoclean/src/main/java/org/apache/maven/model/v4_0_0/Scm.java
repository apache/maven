/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Scm.
 * 
 * @version $Revision$ $Date$
 */
public class Scm
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field connection
     */
    private String connection;

    /**
     * Field developerConnection
     */
    private String developerConnection;

    /**
     * Field url
     */
    private String url;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getConnection
     */
    public String getConnection()
    {
        return this.connection;
    } //-- String getConnection() 

    /**
     * Method getDeveloperConnection
     */
    public String getDeveloperConnection()
    {
        return this.developerConnection;
    } //-- String getDeveloperConnection() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method setConnection
     * 
     * @param connection
     */
    public void setConnection( String connection )
    {
        this.connection = connection;
    } //-- void setConnection(String) 

    /**
     * Method setDeveloperConnection
     * 
     * @param developerConnection
     */
    public void setDeveloperConnection( String developerConnection )
    {
        this.developerConnection = developerConnection;
    } //-- void setDeveloperConnection(String) 

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