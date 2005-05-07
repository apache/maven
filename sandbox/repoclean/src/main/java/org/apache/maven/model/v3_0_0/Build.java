/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class Build.
 * 
 * @version $Revision$ $Date$
 */
public class Build
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field nagEmailAddress
     */
    private String nagEmailAddress;

    /**
     * Field sourceDirectory
     */
    private String sourceDirectory;

    /**
     * Field unitTestSourceDirectory
     */
    private String unitTestSourceDirectory;

    /**
     * Field aspectSourceDirectory
     */
    private String aspectSourceDirectory;

    /**
     * Field integrationUnitTestSourceDirectory
     */
    private String integrationUnitTestSourceDirectory;

    /**
     * Field sourceModifications
     */
    private java.util.List sourceModifications;

    /**
     * Field unitTest
     */
    private UnitTest unitTest = new UnitTest();

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
     * Method addSourceModification
     * 
     * @param sourceModification
     */
    public void addSourceModification( SourceModification sourceModification )
    {
        getSourceModifications().add( sourceModification );
    } //-- void addSourceModification(SourceModification) 

    /**
     * Method getAspectSourceDirectory
     */
    public String getAspectSourceDirectory()
    {
        return this.aspectSourceDirectory;
    } //-- String getAspectSourceDirectory() 

    /**
     * Method getIntegrationUnitTestSourceDirectory
     */
    public String getIntegrationUnitTestSourceDirectory()
    {
        return this.integrationUnitTestSourceDirectory;
    } //-- String getIntegrationUnitTestSourceDirectory() 

    /**
     * Method getNagEmailAddress
     */
    public String getNagEmailAddress()
    {
        return this.nagEmailAddress;
    } //-- String getNagEmailAddress() 

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
     * Method getSourceDirectory
     */
    public String getSourceDirectory()
    {
        return this.sourceDirectory;
    } //-- String getSourceDirectory() 

    /**
     * Method getSourceModifications
     */
    public java.util.List getSourceModifications()
    {
        if ( this.sourceModifications == null )
        {
            this.sourceModifications = new java.util.ArrayList();
        }

        return this.sourceModifications;
    } //-- java.util.List getSourceModifications() 

    /**
     * Method getUnitTest
     */
    public UnitTest getUnitTest()
    {
        return this.unitTest;
    } //-- UnitTest getUnitTest() 

    /**
     * Method getUnitTestSourceDirectory
     */
    public String getUnitTestSourceDirectory()
    {
        return this.unitTestSourceDirectory;
    } //-- String getUnitTestSourceDirectory() 

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
     * Method removeSourceModification
     * 
     * @param sourceModification
     */
    public void removeSourceModification( SourceModification sourceModification )
    {
        getSourceModifications().remove( sourceModification );
    } //-- void removeSourceModification(SourceModification) 

    /**
     * Method setAspectSourceDirectory
     * 
     * @param aspectSourceDirectory
     */
    public void setAspectSourceDirectory( String aspectSourceDirectory )
    {
        this.aspectSourceDirectory = aspectSourceDirectory;
    } //-- void setAspectSourceDirectory(String) 

    /**
     * Method setIntegrationUnitTestSourceDirectory
     * 
     * @param integrationUnitTestSourceDirectory
     */
    public void setIntegrationUnitTestSourceDirectory( String integrationUnitTestSourceDirectory )
    {
        this.integrationUnitTestSourceDirectory = integrationUnitTestSourceDirectory;
    } //-- void setIntegrationUnitTestSourceDirectory(String) 

    /**
     * Method setNagEmailAddress
     * 
     * @param nagEmailAddress
     */
    public void setNagEmailAddress( String nagEmailAddress )
    {
        this.nagEmailAddress = nagEmailAddress;
    } //-- void setNagEmailAddress(String) 

    /**
     * Method setResources
     * 
     * @param resources
     */
    public void setResources( java.util.List resources )
    {
        this.resources = resources;
    } //-- void setResources(java.util.List) 

    /**
     * Method setSourceDirectory
     * 
     * @param sourceDirectory
     */
    public void setSourceDirectory( String sourceDirectory )
    {
        this.sourceDirectory = sourceDirectory;
    } //-- void setSourceDirectory(String) 

    /**
     * Method setSourceModifications
     * 
     * @param sourceModifications
     */
    public void setSourceModifications( java.util.List sourceModifications )
    {
        this.sourceModifications = sourceModifications;
    } //-- void setSourceModifications(java.util.List) 

    /**
     * Method setUnitTest
     * 
     * @param unitTest
     */
    public void setUnitTest( UnitTest unitTest )
    {
        this.unitTest = unitTest;
    } //-- void setUnitTest(UnitTest) 

    /**
     * Method setUnitTestSourceDirectory
     * 
     * @param unitTestSourceDirectory
     */
    public void setUnitTestSourceDirectory( String unitTestSourceDirectory )
    {
        this.unitTestSourceDirectory = unitTestSourceDirectory;
    } //-- void setUnitTestSourceDirectory(String) 

}