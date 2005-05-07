/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class FileSet.
 * 
 * @version $Revision$ $Date$
 */
public class FileSet
    extends PatternSet
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field directory
     */
    private String directory;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getDirectory
     */
    public String getDirectory()
    {
        return this.directory;
    } //-- String getDirectory() 

    /**
     * Method setDirectory
     * 
     * @param directory
     */
    public void setDirectory( String directory )
    {
        this.directory = directory;
    } //-- void setDirectory(String) 

}