package org.apache.maven.plugin.mapping;

import org.apache.maven.model.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PluginMappingManager
{

    private List mappings = new ArrayList();

    private Map pluginDefinitionsByPrefix = new HashMap();

    public void addPluginMap( PluginMap pluginMap )
    {
        mappings.add( pluginMap );
        
        // flush the cache.
        pluginDefinitionsByPrefix = null;
    }

    public Plugin getByPrefix( String pluginPrefix )
    {
        synchronized ( this )
        {
            if ( pluginDefinitionsByPrefix == null )
            {
                calculatePluginDefinitionsByPrefix();
            }
        }

        return (Plugin) pluginDefinitionsByPrefix.get( pluginPrefix );
    }

    private void calculatePluginDefinitionsByPrefix()
    {
        pluginDefinitionsByPrefix = new HashMap();

        for ( Iterator it = mappings.iterator(); it.hasNext(); )
        {
            PluginMap pluginMap = (PluginMap) it.next();

            String groupId = pluginMap.getGroupId();

            for ( Iterator pluginIterator = pluginMap.getPlugins().iterator(); pluginIterator.hasNext(); )
            {
                MappedPlugin mapping = (MappedPlugin) pluginIterator.next();

                String prefix = mapping.getPrefix();

                String artifactId = mapping.getArtifactId();

                Plugin plugin = new Plugin();

                plugin.setGroupId( groupId );

                plugin.setArtifactId( artifactId );

                pluginDefinitionsByPrefix.put( prefix, plugin );
            }
        }
    }

}
