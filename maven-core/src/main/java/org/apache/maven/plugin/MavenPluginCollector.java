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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MavenPluginCollector
    implements ComponentDiscoveryListener, LogEnabled
{
    private Set pluginsInProcess = new HashSet();

    private Map pluginDescriptors = new HashMap();

    private Map pluginIdsByPrefix = new HashMap();

    private Logger logger;

    // ----------------------------------------------------------------------
    // Mojo discovery
    // ----------------------------------------------------------------------
    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( componentSetDescriptor instanceof PluginDescriptor )
        {
            PluginDescriptor pluginDescriptor = (PluginDescriptor) componentSetDescriptor;

            String key = constructPluginKey( pluginDescriptor );

            if ( !pluginsInProcess.contains( key ) )
            {
                pluginsInProcess.add( key );

                pluginDescriptors.put( key, pluginDescriptor );
            }
        }
    }

    public String getId()
    {
        return "maven-plugin-collector";
    }

    public PluginDescriptor getPluginDescriptor( Plugin plugin )
    {
        String key = constructPluginKey( plugin );
        return (PluginDescriptor) pluginDescriptors.get( key );
    }

    private String constructPluginKey( Plugin plugin )
    {
        String version = ArtifactUtils.toSnapshotVersion( plugin.getVersion() );
        return plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + version;
    }

    private String constructPluginKey( PluginDescriptor pluginDescriptor )
    {
        String version = ArtifactUtils.toSnapshotVersion( pluginDescriptor.getVersion() );
        return pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId() + ":" + version;
    }

    public boolean isPluginInstalled( Plugin plugin )
    {
        String key = constructPluginKey( plugin );
        return pluginDescriptors.containsKey( key );
    }

    public Set getPluginDescriptorsForPrefix( String prefix )
    {
        Set result = new HashSet();
        for ( Iterator it = pluginDescriptors.values().iterator(); it.hasNext(); )
        {
            PluginDescriptor pd = (PluginDescriptor) it.next();
            if ( pd.getGoalPrefix().equals( prefix ) )
            {
                result.add( pd );
            }
        }

        return result;
    }

    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_INFO, "internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
