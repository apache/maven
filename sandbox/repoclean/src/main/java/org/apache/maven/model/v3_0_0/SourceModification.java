/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class SourceModification.
 * 
 * @version $Revision$ $Date$
 */
public class SourceModification
    extends FileSet
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field className
     */
    private String className;

    /**
     * Field property
     */
    private String property;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getClassName
     */
    public String getClassName()
    {
        return this.className;
    } //-- String getClassName() 

    /**
     * Method getProperty
     */
    public String getProperty()
    {
        return this.property;
    } //-- String getProperty() 

    /**
     * Method setClassName
     * 
     * @param className
     */
    public void setClassName( String className )
    {
        this.className = className;
    } //-- void setClassName(String) 

    /**
     * Method setProperty
     * 
     * @param property
     */
    public void setProperty( String property )
    {
        this.property = property;
    } //-- void setProperty(String) 

}