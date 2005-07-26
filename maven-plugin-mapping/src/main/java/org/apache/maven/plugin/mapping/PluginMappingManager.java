package org.apache.maven.plugin.mapping;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.model.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PluginMappingManager
{
    private List mappings = new ArrayList();

    private boolean refreshed;

    private Map pluginDefinitionsByPrefix = new HashMap();

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
        this.pluginDefinitionsByPrefix = null;
    }

    public Plugin getByPrefix( String pluginPrefix )
    {
        synchronized ( this ) {
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
