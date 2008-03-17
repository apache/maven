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
import org.apache.maven.plugin.identifier.PluginCoordinate;
import org.apache.maven.plugin.identifier.PluginIdentifier;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains references to the plugins that have been loaded,
 * indexed in a way so that they can be looked up.
 * 
 * It listens for new plugins being loaded, and adds them into the index
 * as this happens.
 * 
 */
public class MavenPluginCollector
    extends AbstractLogEnabled
    implements ComponentDiscoveryListener
{
    /**
     * A 1st-past-the post list of prefixes to descriptors
     * Map {prefix:String -> PluginDescriptor}
     */
    private Map pluginIdsByPrefix = new HashMap();

    /**
     * Map {PluginCoordinate -> PluginDescriptor}}
     */
    private Map pluginDescriptors = new HashMap();

    /**
     * Keep track of the order that particular plugins have been
     * seen, in case a move to a non 1st past the post for 
     * descriptors. 
     * 
     * Map {PluginIdentifier -> List {PluginDescriptor}}
     */
    private Map pluginDeclarations = new HashMap();
    
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
                        
            PluginCoordinate coordinate = new PluginCoordinate( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion() );
            PluginIdentifier identifier = coordinate.getIdentifier();
            
        	// Don't overwrite any existing found descriptor, as things
        	// like the ClassRealm that has been constructed will disappear,
        	// causing reactor builds to fail.
        	if( !pluginDescriptors.containsKey(coordinate))
        	{
        		pluginDescriptors.put( coordinate, pluginDescriptor );
        	} 
           
            // Store that we've seen the plugin by prefix, for the 1st past the post
            if( !pluginDeclarations.containsKey(identifier) )
            {
            	pluginDeclarations.put(identifier, new ArrayList());

                // TODO: throw an (not runtime) exception if there is a prefix overlap - means doing so elsewhere
                // we also need to deal with multiple versions somehow - currently, first wins
                if ( !pluginIdsByPrefix.containsKey( pluginDescriptor.getGoalPrefix() ) )
                {
                    pluginIdsByPrefix.put( pluginDescriptor.getGoalPrefix(), pluginDescriptor );
                }
            }
            ((List)pluginDeclarations.get(identifier)).add(pluginDescriptor);
        }
    }

    public PluginDescriptor getPluginDescriptor( Plugin plugin )
    {
        // TODO: include version, but can't do this in the plugin manager as it is not resolved to the right version
        // at that point. Instead, move the duplication check to the artifact container, or store it locally based on
        // the unresolved version?
      
    	// Has the plugin got a version specified (think that it ought to)
    	PluginCoordinate coordinate = new PluginCoordinate(plugin);
    	PluginIdentifier pluginIdentifier = coordinate.getIdentifier();
    	
    	if( plugin.getVersion() != null )
        {
    		PluginDescriptor pluginDescriptor = (PluginDescriptor)pluginDescriptors.get(coordinate);
    		if( pluginDescriptor != null )
    		{
    			return pluginDescriptor;
    		}
        }
    	
    	getLogger().error("No version for " + pluginIdentifier);
    	
    	return null;
    }

    public boolean isPluginInstalled( Plugin plugin )
    {      
        // TODO: see comment in getPluginDescriptor
    
        PluginCoordinate coordinate = new PluginCoordinate(plugin);
        
        return pluginDescriptors.containsKey(coordinate);
    }
    
    public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
    {
        return (PluginDescriptor) pluginIdsByPrefix.get( prefix );
    }

    public void flushPluginDescriptor( Plugin plugin )
    {
        PluginCoordinate coordinate = new PluginCoordinate(plugin);
        PluginIdentifier identifier = coordinate.getIdentifier();
                  
        pluginDeclarations.remove( identifier );
        pluginDescriptors.remove ( coordinate );
        
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
