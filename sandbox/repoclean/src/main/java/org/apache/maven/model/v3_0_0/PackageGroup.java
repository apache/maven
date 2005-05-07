/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class PackageGroup.
 * 
 * @version $Revision$ $Date$
 */
public class PackageGroup
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field title
     */
    private String title;

    /**
     * Field packages
     */
    private String packages;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getPackages
     */
    public String getPackages()
    {
        return this.packages;
    } //-- String getPackages() 

    /**
     * Method getTitle
     */
    public String getTitle()
    {
        return this.title;
    } //-- String getTitle() 

    /**
     * Method setPackages
     * 
     * @param packages
     */
    public void setPackages( String packages )
    {
        this.packages = packages;
    } //-- void setPackages(String) 

    /**
     * Method setTitle
     * 
     * @param title
     */
    public void setTitle( String title )
    {
        this.title = title;
    } //-- void setTitle(String) 

}