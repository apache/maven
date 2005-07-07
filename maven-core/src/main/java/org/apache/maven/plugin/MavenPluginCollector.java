package org.apache.maven.plugin;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.HashMap;
import java.util.HashSet;
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

            //            String key = pluginDescriptor.getId();
            // TODO: see comment in getPluginDescriptor
            String key = pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId();

            if ( !pluginsInProcess.contains( key ) )
            {
                pluginsInProcess.add( key );

                pluginDescriptors.put( key, pluginDescriptor );

                // TODO: throw an (not runtime) exception if there is a prefix overlap - means doing so elsewhere
                // we also need to deal with multiple versions somehow - currently, first wins
                if ( !pluginIdsByPrefix.containsKey( pluginDescriptor.getGoalPrefix() ) )
                {
                    pluginIdsByPrefix.put( pluginDescriptor.getGoalPrefix(), pluginDescriptor.getId() );
                }
            }
        }
    }
    
    public PluginDescriptor getPluginDescriptor( String groupId, String artifactId, String version )
    {
        // TODO: include version, but can't do this in the plugin manager as it is not resolved to the right version
        // at that point. Instead, move the duplication check to the artifact container, or store it locally based on
        // the unresolved version?
        return (PluginDescriptor) pluginDescriptors.get( Plugin.constructKey( groupId, artifactId ) );
    }
    
    public boolean isPluginInstalled( String groupId, String artifactId )
    {
        //        String key = PluginDescriptor.constructPluginKey( groupId, artifactId, version );
        // TODO: see comment in getPluginDescriptor
        return pluginDescriptors.containsKey( Plugin.constructKey( groupId, artifactId ) );
    }

    public String getPluginIdFromPrefix( String prefix )
    {
        if ( !pluginIdsByPrefix.containsKey( prefix ) )
        {
            // TODO: lookup remotely
        }
        return (String) pluginIdsByPrefix.get( prefix );
    }
}
