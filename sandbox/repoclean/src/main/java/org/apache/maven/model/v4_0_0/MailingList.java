/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

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
     * Field post
     */
    private String post;

    /**
     * Field archive
     */
    private String archive;

    /**
     * Field otherArchives
     */
    private java.util.List otherArchives;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addOtherArchive
     * 
     * @param string
     */
    public void addOtherArchive( String string )
    {
        getOtherArchives().add( string );
    } //-- void addOtherArchive(String) 

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
     * Method getOtherArchives
     */
    public java.util.List getOtherArchives()
    {
        if ( this.otherArchives == null )
        {
            this.otherArchives = new java.util.ArrayList();
        }

        return this.otherArchives;
    } //-- java.util.List getOtherArchives() 

    /**
     * Method getPost
     */
    public String getPost()
    {
        return this.post;
    } //-- String getPost() 

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
     * Method removeOtherArchive
     * 
     * @param string
     */
    public void removeOtherArchive( String string )
    {
        getOtherArchives().remove( string );
    } //-- void removeOtherArchive(String) 

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
     * Method setOtherArchives
     * 
     * @param otherArchives
     */
    public void setOtherArchives( java.util.List otherArchives )
    {
        this.otherArchives = otherArchives;
    } //-- void setOtherArchives(java.util.List) 

    /**
     * Method setPost
     * 
     * @param post
     */
    public void setPost( String post )
    {
        this.post = post;
    } //-- void setPost(String) 

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