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
    private boolean refreshed = false;

    private Map pluginDefinitionsByPrefix = new HashMap();
    private Map pluginDefinitionsByPackaging = new HashMap();

    public void addPluginMap( PluginMap pluginMap )
    {
        mappings.add( pluginMap );
        
        clearCache();
    }
    
    public void markRefreshed()
    {
        this.refreshed = true;
    }
    
    public boolean isRefreshed()
    {
        return refreshed;
    }
    
    public List getPluginMaps()
    {
        return mappings;
    }
    
    public void clear()
    {
        this.mappings = null;
        clearCache();
    }

    private void clearCache()
    {
        this.pluginDefinitionsByPackaging = null;
        this.pluginDefinitionsByPrefix = null;
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

    public Plugin getByPackaging( String packaging )
    {
        synchronized ( this )
        {
            if ( pluginDefinitionsByPackaging == null )
            {
                calculatePluginDefinitionsByPackaging();
            }
        }

        return (Plugin) pluginDefinitionsByPackaging.get( packaging );
    }

    private void calculatePluginDefinitionsByPackaging()
    {
        pluginDefinitionsByPackaging = new HashMap();

        for ( Iterator it = mappings.iterator(); it.hasNext(); )
        {
            PluginMap pluginMap = (PluginMap) it.next();

            String groupId = pluginMap.getGroupId();

            for ( Iterator pluginIterator = pluginMap.getPlugins().iterator(); pluginIterator.hasNext(); )
            {
                MappedPlugin mapping = (MappedPlugin) pluginIterator.next();

                String artifactId = mapping.getArtifactId();

                Plugin plugin = new Plugin();

                plugin.setGroupId( groupId );

                plugin.setArtifactId( artifactId );

                for ( Iterator packagingIterator = mapping.getPackagingHandlers().iterator(); packagingIterator.hasNext(); )
                {
                    String packaging = (String) packagingIterator.next();
                    
                    pluginDefinitionsByPackaging.put( packaging, plugin );
                }
            }
        }
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
