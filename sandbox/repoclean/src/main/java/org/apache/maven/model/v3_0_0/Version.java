/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class Version.
 * 
 * @version $Revision$ $Date$
 */
public class Version
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
     * Field tag
     */
    private String tag;

    /**
     * Field id
     */
    private String id;

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
     * Method getTag
     */
    public String getTag()
    {
        return this.tag;
    } //-- String getTag() 

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
     * Method setTag
     * 
     * @param tag
     */
    public void setTag( String tag )
    {
        this.tag = tag;
    } //-- void setTag(String) 

    public String toString()
    {
        return getId();
    }
}