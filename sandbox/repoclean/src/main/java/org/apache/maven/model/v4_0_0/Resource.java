/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class Resource.
 * 
 * @version $Revision$ $Date$
 */
public class Resource
    extends FileSet
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field targetPath
     */
    private String targetPath;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getTargetPath
     */
    public String getTargetPath()
    {
        return this.targetPath;
    } //-- String getTargetPath() 

    /**
     * Method setTargetPath
     * 
     * @param targetPath
     */
    public void setTargetPath( String targetPath )
    {
        this.targetPath = targetPath;
    } //-- void setTargetPath(String) 

}