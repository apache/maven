/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class MailingList.
 * 
 * @version $Revision$ $Date$
 */
public class MailingList
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
     * Field subscribe
     */
    private String subscribe;

    /**
     * Field unsubscribe
     */
    private String unsubscribe;

    /**
     * Field archive
     */
    private String archive;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getArchive
     */
    public String getArchive()
    {
        return this.archive;
    } //-- String getArchive() 

    /**
     * Method getName
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method getSubscribe
     */
    public String getSubscribe()
    {
        return this.subscribe;
    } //-- String getSubscribe() 

    /**
     * Method getUnsubscribe
     */
    public String getUnsubscribe()
    {
        return this.unsubscribe;
    } //-- String getUnsubscribe() 

    /**
     * Method setArchive
     * 
     * @param archive
     */
    public void setArchive( String archive )
    {
        this.archive = archive;
    } //-- void setArchive(String) 

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
     * Method setSubscribe
     * 
     * @param subscribe
     */
    public void setSubscribe( String subscribe )
    {
        this.subscribe = subscribe;
    } //-- void setSubscribe(String) 

    /**
     * Method setUnsubscribe
     * 
     * @param unsubscribe
     */
    public void setUnsubscribe( String unsubscribe )
    {
        this.unsubscribe = unsubscribe;
    } //-- void setUnsubscribe(String) 

}