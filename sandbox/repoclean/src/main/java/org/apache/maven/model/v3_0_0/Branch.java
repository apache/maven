/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class Branch.
 * 
 * @version $Revision$ $Date$
 */
public class Branch
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field tag
     */
    private String tag;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getTag
     */
    public String getTag()
    {
        return this.tag;
    } //-- String getTag() 

    /**
     * Method setTag
     * 
     * @param tag
     */
    public void setTag( String tag )
    {
        this.tag = tag;
    } //-- void setTag(String) 

}