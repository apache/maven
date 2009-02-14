package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MavenPluginCollector
    extends AbstractLogEnabled
    implements ComponentDiscoveryListener
{

    private Set pluginsInProcess = new HashSet();

    private Map pluginDescriptors = new HashMap();

    private Map pluginIdsByPrefix = new HashMap();

    // ----------------------------------------------------------------------
    // Mojo discovery
    // ----------------------------------------------------------------------
    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( componentSetDescriptor instanceof PluginDescriptor )
        {
            PluginDescriptor pluginDescriptor = (PluginDescriptor) componentSetDescriptor;
            
            String key = PluginUtils.constructVersionedKey( pluginDescriptor );
            
            if ( !pluginsInProcess.contains( key ) )
            {
                pluginsInProcess.add( key );

                pluginDescriptors.put( key, pluginDescriptor );

                // TODO: throw an (not runtime) exception if there is a prefix overlap - means doing so elsewhere
                // we also need to deal with multiple versions somehow - currently, first wins
                if ( !pluginIdsByPrefix.containsKey( pluginDescriptor.getGoalPrefix() ) )
                {
                    pluginIdsByPrefix.put( pluginDescriptor.getGoalPrefix(), pluginDescriptor );
                }
            }
        }
    }

    public PluginDescriptor getPluginDescriptor( Plugin plugin )
    {
        String key = PluginUtils.constructVersionedKey( plugin );
        return (PluginDescriptor) pluginDescriptors.get( key );
    }

    public boolean isPluginInstalled( Plugin plugin )
    {
        String key = PluginUtils.constructVersionedKey( plugin );
        return pluginDescriptors.containsKey( key );
    }

    public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
    {
        return (PluginDescriptor) pluginIdsByPrefix.get( prefix );
    }

    public void flushPluginDescriptor( Plugin plugin )
    {
        String key = PluginUtils.constructVersionedKey( plugin );
        pluginsInProcess.remove( key );
        pluginDescriptors.remove( key );
        
        for ( Iterator it = pluginIdsByPrefix.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            
            if ( key.equals( PluginUtils.constructVersionedKey( (PluginDescriptor) entry.getValue() ) ) )
            {
                it.remove();
            }
        }
    }

}
