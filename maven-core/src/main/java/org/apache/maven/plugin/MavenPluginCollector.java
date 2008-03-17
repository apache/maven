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
            
            // TODO: see comment in getPluginDescriptor
            String key = Plugin.constructKey( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId() );
            
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
        // TODO: include version, but can't do this in the plugin manager as it is not resolved to the right version
        // at that point. Instead, move the duplication check to the artifact container, or store it locally based on
        // the unresolved version?
        return (PluginDescriptor) pluginDescriptors.get( plugin.getKey() );
    }

    public boolean isPluginInstalled( Plugin plugin )
    {
        // TODO: see comment in getPluginDescriptor
        return pluginDescriptors.containsKey( plugin.getKey() );
    }

    public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
    {
        return (PluginDescriptor) pluginIdsByPrefix.get( prefix );
    }

    public void flushPluginDescriptor( Plugin plugin )
    {
        pluginsInProcess.remove( plugin.getKey() );
        pluginDescriptors.remove( plugin.getKey() );
        
        for ( Iterator it = pluginIdsByPrefix.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            
            if ( plugin.getKey().equals( entry.getValue() ) )
            {
                it.remove();
            }
        }
    }

}
