/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class IssueManagement.
 * 
 * @version $Revision$ $Date$
 */
public class IssueManagement
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field system
     */
    private String system;

    /**
     * Field url
     */
    private String url;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getSystem
     */
    public String getSystem()
    {
        return this.system;
    } //-- String getSystem() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method setSystem
     * 
     * @param system
     */
    public void setSystem( String system )
    {
        this.system = system;
    } //-- void setSystem(String) 

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