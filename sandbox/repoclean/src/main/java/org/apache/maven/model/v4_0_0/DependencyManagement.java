/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class DependencyManagement.
 * 
 * @version $Revision$ $Date$
 */
public class DependencyManagement
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field dependencies
     */
    private java.util.List dependencies;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addDependency
     * 
     * @param dependency
     */
    public void addDependency( Dependency dependency )
    {
        getDependencies().add( dependency );
    } //-- void addDependency(Dependency) 

    /**
     * Method getDependencies
     */
    public java.util.List getDependencies()
    {
        if ( this.dependencies == null )
        {
            this.dependencies = new java.util.ArrayList();
        }

        return this.dependencies;
    } //-- java.util.List getDependencies() 

    /**
     * Method removeDependency
     * 
     * @param dependency
     */
    public void removeDependency( Dependency dependency )
    {
        getDependencies().remove( dependency );
    } //-- void removeDependency(Dependency) 

    /**
     * Method setDependencies
     * 
     * @param dependencies
     */
    public void setDependencies( java.util.List dependencies )
    {
        this.dependencies = dependencies;
    } //-- void setDependencies(java.util.List) 

}