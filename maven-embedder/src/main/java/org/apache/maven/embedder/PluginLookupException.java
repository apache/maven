package org.apache.maven.embedder;

import org.apache.maven.model.Plugin;

public class PluginLookupException
    extends MavenEmbedderException
{

    private final Plugin plugin;

    public PluginLookupException( Plugin plugin, String message,
                                  Throwable cause )
    {
        super( message, cause );
        this.plugin = plugin;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }

}
