package org.apache.maven.plugin.loader;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

/**
 * Signifies a failure to load a plugin. This is used to abstract the specific errors which may be
 * encountered at lower levels, and provide a dependable interface to the plugin-loading framework.
 * 
 * @author jdcasey
 *
 */
public class PluginLoaderException
    extends Exception
{

    private String pluginKey;

    public PluginLoaderException( Plugin plugin, String message, Throwable cause )
    {
        super( message, cause );
        this.pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message )
    {
        super( message );
        this.pluginKey = plugin.getKey();
    }
    
    public PluginLoaderException( String message )
    {
        super( message );
    }

    public PluginLoaderException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public PluginLoaderException( ReportPlugin plugin, String message, Throwable cause )
    {
        super( message, cause );
        this.pluginKey = plugin.getKey();
    }

    public PluginLoaderException( ReportPlugin plugin, String message )
    {
        super( message );
        this.pluginKey = plugin.getKey();
    }
    
    public String getPluginKey()
    {
        return pluginKey;
    }

}
