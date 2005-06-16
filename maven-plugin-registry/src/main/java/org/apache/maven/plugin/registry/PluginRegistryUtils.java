package org.apache.maven.plugin.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

public final class PluginRegistryUtils
{

    private PluginRegistryUtils()
    {
        // don't allow construction.
    }

    public static void merge( PluginRegistry dominant, PluginRegistry recessive, String recessiveSourceLevel )
    {
        // can't merge into something that doesn't exist...
        if ( dominant == null )
        {
            return;
        }

        List recessivePlugins = null;
        
        if(recessive != null)
        {
            recessivePlugins = recessive.getPlugins();
        }
        else
        {
            recessivePlugins = Collections.EMPTY_LIST;
        }
        
        shallowMergePlugins( dominant, recessivePlugins, recessiveSourceLevel );
    }

    public static void recursivelySetSourceLevel( PluginRegistry pluginRegistry, String sourceLevel )
    {
        if ( pluginRegistry == null )
        {
            return;
        }

        pluginRegistry.setSourceLevel( sourceLevel );

        for ( Iterator it = pluginRegistry.getPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            plugin.setSourceLevel( sourceLevel );
        }
    }

    private static void shallowMergePlugins( PluginRegistry dominant, List recessive, String recessiveSourceLevel )
    {
        Map dominantByKey = dominant.getPluginsByKey();

        List dominantPlugins = dominant.getPlugins();

        for ( Iterator it = recessive.iterator(); it.hasNext(); )
        {
            Plugin recessivePlugin = (Plugin) it.next();

            if ( !dominantByKey.containsKey( recessivePlugin.getKey() ) )
            {
                recessivePlugin.setSourceLevel( recessiveSourceLevel );

                dominantPlugins.add( recessivePlugin );
            }
        }

        dominant.flushPluginsByKey();
    }

    public static PluginRegistry extractUserPluginRegistry( PluginRegistry pluginRegistry )
    {
        PluginRegistry userRegistry = null;

        // check if this registry is entirely made up of global settings
        if ( pluginRegistry != null && !PluginRegistry.GLOBAL_LEVEL.equals( pluginRegistry.getSourceLevel() ) )
        {
            userRegistry = new PluginRegistry();

            List plugins = new ArrayList();

            for ( Iterator it = pluginRegistry.getPlugins().iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();

                if ( TrackableBase.USER_LEVEL.equals( plugin.getSourceLevel() ) )
                {
                    plugins.add( plugin );
                }
            }

            userRegistry.setPlugins( plugins );

            userRegistry.setFile( pluginRegistry.getFile() );
        }

        return userRegistry;
    }

}
