/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class PluginManagement.
 * 
 * @version $Revision$ $Date$
 */
public class PluginManagement
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field plugins
     */
    private java.util.List plugins;

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
     * Method removePlugin
     * 
     * @param plugin
     */
    public void removePlugin( Plugin plugin )
    {
        getPlugins().remove( plugin );
    } //-- void removePlugin(Plugin) 

    /**
     * Method setPlugins
     * 
     * @param plugins
     */
    public void setPlugins( java.util.List plugins )
    {
        this.plugins = plugins;
    } //-- void setPlugins(java.util.List) 

}