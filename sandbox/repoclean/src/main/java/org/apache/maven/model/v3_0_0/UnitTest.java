/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class UnitTest.
 * 
 * @version $Revision$ $Date$
 */
public class UnitTest
    extends PatternSet
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field resources
     */
    private java.util.List resources;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addResource
     * 
     * @param resource
     */
    public void addResource( Resource resource )
    {
        getResources().add( resource );
    } //-- void addResource(Resource) 

    /**
     * Method getResources
     */
    public java.util.List getResources()
    {
        if ( this.resources == null )
        {
            this.resources = new java.util.ArrayList();
        }

        return this.resources;
    } //-- java.util.List getResources() 

    /**
     * Method removeResource
     * 
     * @param resource
     */
    public void removeResource( Resource resource )
    {
        getResources().remove( resource );
    } //-- void removeResource(Resource) 

    /**
     * Method setResources
     * 
     * @param resources
     */
    public void setResources( java.util.List resources )
    {
        this.resources = resources;
    } //-- void setResources(java.util.List) 

}