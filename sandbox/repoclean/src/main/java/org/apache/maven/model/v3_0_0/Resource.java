/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

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

    /**
     * Field filtering
     */
    private boolean filtering = false;

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
     * Method isFiltering
     */
    public boolean isFiltering()
    {
        return this.filtering;
    } //-- boolean isFiltering() 

    /**
     * Method setFiltering
     * 
     * @param filtering
     */
    public void setFiltering( boolean filtering )
    {
        this.filtering = filtering;
    } //-- void setFiltering(boolean) 

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