package org.apache.maven.plugin;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
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

}
