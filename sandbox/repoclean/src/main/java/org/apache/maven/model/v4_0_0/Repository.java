/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Repository.
 * 
 * @version $Revision$ $Date$
 */
public class Repository
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
     * Field name
     */
    private String name;

    /**
     * Field url
     */
    private String url;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getId
     */
    public String getId()
    {
        return this.id;
    } //-- String getId() 

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
     * Method setId
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId(String) 

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

    public boolean equals( Object obj )
    {
        Repository other = (Repository) obj;

        boolean retValue = false;

        if ( id != null )
        {
            retValue = id.equals( other.id );
        }

        return retValue;
    }
}