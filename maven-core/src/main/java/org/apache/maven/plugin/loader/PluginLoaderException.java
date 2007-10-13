package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;

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

    public PluginLoaderException( Plugin plugin, String message, ArtifactResolutionException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, ArtifactNotFoundException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, PluginNotFoundException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, PluginVersionResolutionException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, InvalidVersionSpecificationException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, InvalidPluginException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, PluginManagerException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message, PluginVersionNotFoundException cause )
    {
        super( message, cause );
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( Plugin plugin, String message )
    {
        super( message );
        pluginKey = plugin.getKey();
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
        pluginKey = plugin.getKey();
    }

    public PluginLoaderException( ReportPlugin plugin, String message )
    {
        super( message );
        pluginKey = plugin.getKey();
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

}
