/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class CiManagement.
 * 
 * @version $Revision$ $Date$
 */
public class CiManagement
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

    /**
     * Field notifiers
     */
    private java.util.List notifiers;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addNotifier
     * 
     * @param notifier
     */
    public void addNotifier( Notifier notifier )
    {
        getNotifiers().add( notifier );
    } //-- void addNotifier(Notifier) 

    /**
     * Method getNotifiers
     */
    public java.util.List getNotifiers()
    {
        if ( this.notifiers == null )
        {
            this.notifiers = new java.util.ArrayList();
        }

        return this.notifiers;
    } //-- java.util.List getNotifiers() 

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
     * Method removeNotifier
     * 
     * @param notifier
     */
    public void removeNotifier( Notifier notifier )
    {
        getNotifiers().remove( notifier );
    } //-- void removeNotifier(Notifier) 

    /**
     * Method setNotifiers
     * 
     * @param notifiers
     */
    public void setNotifiers( java.util.List notifiers )
    {
        this.notifiers = notifiers;
    } //-- void setNotifiers(java.util.List) 

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