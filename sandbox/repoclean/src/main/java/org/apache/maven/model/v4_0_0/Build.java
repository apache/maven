/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

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
     * Field sourceDirectory
     */
    private String sourceDirectory;

    /**
     * Field scriptSourceDirectory
     */
    private String scriptSourceDirectory;

    /**
     * Field testSourceDirectory
     */
    private String testSourceDirectory;

    /**
     * Field resources
     */
    private java.util.List resources;

    /**
     * Field testResources
     */
    private java.util.List testResources;

    /**
     * Field directory
     */
    private String directory;

    /**
     * Field outputDirectory
     */
    private String outputDirectory;

    /**
     * Field finalName
     */
    private String finalName;

    /**
     * Field testOutputDirectory
     */
    private String testOutputDirectory;

    /**
     * Field plugins
     */
    private java.util.List plugins;

    /**
     * Field pluginManagement
     */
    private PluginManagement pluginManagement;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addPlugin
     * 
     * @param plugin
     */
    public void addPlugin( Plugin plugin )
    {
        getPlugins().add( plugin );
    } //-- void addPlugin(Plugin) 

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
     * Method addTestResource
     * 
     * @param resource
     */
    public void addTestResource( Resource resource )
    {
        getTestResources().add( resource );
    } //-- void addTestResource(Resource) 

    /**
     * Method getDirectory
     */
    public String getDirectory()
    {
        return this.directory;
    } //-- String getDirectory() 

    /**
     * Method getFinalName
     */
    public String getFinalName()
    {
        return this.finalName;
    } //-- String getFinalName() 

    /**
     * Method getOutputDirectory
     */
    public String getOutputDirectory()
    {
        return this.outputDirectory;
    } //-- String getOutputDirectory() 

    /**
     * Method getPluginManagement
     */
    public PluginManagement getPluginManagement()
    {
        return this.pluginManagement;
    } //-- PluginManagement getPluginManagement() 

    /**
     * Method getPlugins
     */
    public java.util.List getPlugins()
    {
        if ( this.plugins == null )
        {
            this.plugins = new java.util.ArrayList();
        }

        return this.plugins;
    } //-- java.util.List getPlugins() 

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
     * Method getScriptSourceDirectory
     */
    public String getScriptSourceDirectory()
    {
        return this.scriptSourceDirectory;
    } //-- String getScriptSourceDirectory() 

    /**
     * Method getSourceDirectory
     */
    public String getSourceDirectory()
    {
        return this.sourceDirectory;
    } //-- String getSourceDirectory() 

    /**
     * Method getTestOutputDirectory
     */
    public String getTestOutputDirectory()
    {
        return this.testOutputDirectory;
    } //-- String getTestOutputDirectory() 

    /**
     * Method getTestResources
     */
    public java.util.List getTestResources()
    {
        if ( this.testResources == null )
        {
            this.testResources = new java.util.ArrayList();
        }

        return this.testResources;
    } //-- java.util.List getTestResources() 

    /**
     * Method getTestSourceDirectory
     */
    public String getTestSourceDirectory()
    {
        return this.testSourceDirectory;
    } //-- String getTestSourceDirectory() 

    /**
     * Method removePlugin
     * 
     * @param plugin
     */
    public void removePlugin( Plugin plugin )
    {
        getPlugins().remove( plugin );
    } //-- void removePlugin(Plugin) 

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
     * Method removeTestResource
     * 
     * @param resource
     */
    public void removeTestResource( Resource resource )
    {
        getTestResources().remove( resource );
    } //-- void removeTestResource(Resource) 

    /**
     * Method setDirectory
     * 
     * @param directory
     */
    public void setDirectory( String directory )
    {
        this.directory = directory;
    } //-- void setDirectory(String) 

    /**
     * Method setFinalName
     * 
     * @param finalName
     */
    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    } //-- void setFinalName(String) 

    /**
     * Method setOutputDirectory
     * 
     * @param outputDirectory
     */
    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    } //-- void setOutputDirectory(String) 

    /**
     * Method setPluginManagement
     * 
     * @param pluginManagement
     */
    public void setPluginManagement( PluginManagement pluginManagement )
    {
        this.pluginManagement = pluginManagement;
    } //-- void setPluginManagement(PluginManagement) 

    /**
     * Method setPlugins
     * 
     * @param plugins
     */
    public void setPlugins( java.util.List plugins )
    {
        this.plugins = plugins;
    } //-- void setPlugins(java.util.List) 

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
     * Method setScriptSourceDirectory
     * 
     * @param scriptSourceDirectory
     */
    public void setScriptSourceDirectory( String scriptSourceDirectory )
    {
        this.scriptSourceDirectory = scriptSourceDirectory;
    } //-- void setScriptSourceDirectory(String) 

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
     * Method setTestOutputDirectory
     * 
     * @param testOutputDirectory
     */
    public void setTestOutputDirectory( String testOutputDirectory )
    {
        this.testOutputDirectory = testOutputDirectory;
    } //-- void setTestOutputDirectory(String) 

    /**
     * Method setTestResources
     * 
     * @param testResources
     */
    public void setTestResources( java.util.List testResources )
    {
        this.testResources = testResources;
    } //-- void setTestResources(java.util.List) 

    /**
     * Method setTestSourceDirectory
     * 
     * @param testSourceDirectory
     */
    public void setTestSourceDirectory( String testSourceDirectory )
    {
        this.testSourceDirectory = testSourceDirectory;
    } //-- void setTestSourceDirectory(String) 

}