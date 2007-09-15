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

import com.sun.jmx.remote.util.OrderClassLoaders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class MavenPluginCollector
    extends AbstractLogEnabled
    implements ComponentDiscoveryListener
{
    /**
     * Map from pluginDescriptor.getKey (groupId:artifactId) into (ordered) map from version into pluginDescriptor
     * Internal map is ordered to make sure that builds are determinic (used pluginVersion is determined)
     */
    private Map/* <String,OrderedMap<String,PluginDescriptor>> */pluginDescriptors = new HashMap();

    private Map/* <String,OrderedMap<String,PluginDescriptor>> */pluginIdsByPrefix = new HashMap();

    public String getId()
    {
        return "maven-plugin-collector";
    }
    
    // ----------------------------------------------------------------------
    // Mojo discovery
    // ----------------------------------------------------------------------
    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( componentSetDescriptor instanceof PluginDescriptor )
        {
            PluginDescriptor pluginDescriptor = (PluginDescriptor) componentSetDescriptor;
            
            putIntoPluginDescriptors( pluginDescriptor );
            putIntoPluginIdsByPrefix( pluginDescriptor );
        }
    }

    public PluginDescriptor getPluginDescriptor( Plugin plugin )
    {
        SortedMap/* <String,PluginDescriptor> */pluginVersions = (SortedMap) pluginDescriptors.get( plugin.getKey() );
        if ( pluginVersions != null )
        {
            PluginDescriptor res;
            if ( plugin.getVersion() != null )
            {
                res = (PluginDescriptor) pluginVersions.get( plugin.getVersion() );
            }
            else
            {
                res = getDefaultPluginDescriptorVersion( pluginVersions );
            }
           return res;
        }
        else
        {
            return null;
        }
    }

    private PluginDescriptor getDefaultPluginDescriptorVersion( SortedMap pluginVersions )
    {
        if ( pluginVersions.size() > 0 )
        {
            return (PluginDescriptor) pluginVersions.get( pluginVersions.lastKey() );
        }
        else
        {
            return null;
        }
    }

    public boolean isPluginInstalled( Plugin plugin )
    {
        // TODO: see comment in getPluginDescriptor
        return getPluginDescriptor( plugin ) != null;
    }

    public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
    {
        return getPluginDescriptorForPrefix( prefix, null );
    }

    public PluginDescriptor getPluginDescriptorForPrefix( String prefix, String version )
    {
        SortedMap/* <String,PluginDescriptor> */pluginVersions = (SortedMap) pluginIdsByPrefix.get( prefix );
        if ( pluginVersions != null )
        {
            PluginDescriptor res;
            if ( version != null )
            {
                res = (PluginDescriptor) pluginVersions.get( version );
            }
            else
            {
                res = getDefaultPluginDescriptorVersion( pluginVersions );
            }
            return res;
        }
        else
        {
            return null;
        }
    }

//    public void flushPluginDescriptor( Plugin plugin )
//    {
//        getPluginDescriptor( plugin ).cleanPluginDescriptor();
//    }

    /**
     * Puts given pluginDescriptor into pluginDescriptors map (if the map does not contains plugin for specified maven
     * version)
     * 
     * @param pluginDescriptor
     */
    protected void putIntoPluginDescriptors( PluginDescriptor pluginDescriptor )
    {
        String key = Plugin.constructKey( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId() );

        SortedMap/* <String,PluginDescriptor> */descriptorsVersions = (SortedMap) pluginDescriptors.get( key );
        if ( descriptorsVersions == null )
        {
            descriptorsVersions = new TreeMap();
            pluginDescriptors.put( key, descriptorsVersions );
        }

        putIntoVersionsMap( descriptorsVersions, pluginDescriptor );
    }

    protected void putIntoVersionsMap( SortedMap/* <String(version),PluginDescriptor> */pluginVersions,
                                       PluginDescriptor pluginDescriptor )
    {
        if ( !pluginVersions.containsKey( pluginDescriptor.getVersion() ) )
        {
            pluginVersions.put( pluginDescriptor.getVersion(), pluginDescriptor );
        }
    }

    protected void putIntoPluginIdsByPrefix( PluginDescriptor pluginDescriptor )
    {
        String goalPrefix = pluginDescriptor.getGoalPrefix();

        SortedMap/* <String,PluginDescriptor> */descriptorsVersions = (SortedMap) pluginIdsByPrefix.get( goalPrefix );
        if ( descriptorsVersions == null )
        {
            descriptorsVersions = new TreeMap();
            pluginIdsByPrefix.put( goalPrefix, descriptorsVersions );
        }

        putIntoVersionsMap( descriptorsVersions, pluginDescriptor );
    }

}
