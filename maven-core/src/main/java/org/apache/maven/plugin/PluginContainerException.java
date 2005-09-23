package org.apache.maven.plugin;

import org.apache.maven.model.Plugin;

public class PluginContainerException
    extends PluginManagerException
{

    private final Plugin plugin;
    private final String originalMessage;

    public PluginContainerException( Plugin plugin, String message, Exception e )
    {
        super( "Error configuring container for: " + plugin.getKey() + ". Message was: " + message, e );
        
        this.plugin = plugin;
        this.originalMessage = message;
    }

    public PluginContainerException( Plugin plugin, String message )
    {
        super( "Error configuring container for: " + plugin.getKey() + ". Message was: " + message );
        
        this.plugin = plugin;
        this.originalMessage = message;
    }
    
    public String getOriginalMessage()
    {
        return originalMessage;
    }
    
    public Plugin getPlugin()
    {
        return plugin;
    }

}
