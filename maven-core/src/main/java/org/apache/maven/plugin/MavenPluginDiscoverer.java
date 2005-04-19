package org.apache.maven.plugin;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.plugin.descriptor.Dependency;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.component.discovery.AbstractComponentDiscoverer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class MavenPluginDiscoverer
    extends AbstractComponentDiscoverer
{
    private PluginDescriptorBuilder builder;

    public MavenPluginDiscoverer()
    {
        builder = new PluginDescriptorBuilder();
    }

    public String getComponentDescriptorLocation()
    {
        return "META-INF/maven/plugin.xml";
    }

    public ComponentSetDescriptor createComponentDescriptors( Reader componentDescriptorConfiguration, String source )
        throws PlexusConfigurationException
    {
        PluginDescriptor pluginDescriptor;

        try
        {
            pluginDescriptor = builder.build( componentDescriptorConfiguration );
        }
        catch ( PlexusConfigurationException e )
        {
            // If the plugin is not valid, we cannot continue as it may make the lifecycle ebhave differently than expected
            throw new PlexusConfigurationException( "Cannot process plugin descriptor: " + source, e );
        }

        // ----------------------------------------------------------------------
        // We take the plugin descriptor as it has been built by the maven-plugin-descriptor
        // code. This descriptor is specific to maven-plugin-descriptor and we are now
        // going to adapt it into a ComponentSetDescriptor that can be
        // utlized by Plexus.
        // ----------------------------------------------------------------------

        ComponentSetDescriptor componentSet = new MavenPluginDescriptor( pluginDescriptor );

        // TODO: no group
        componentSet.setId( pluginDescriptor.getArtifactId() );

        // ----------------------------------------------------------------------
        // If the ComponentSet states any dependencies then we want to collect
        // them and store them for later use.
        // ----------------------------------------------------------------------

        if ( pluginDescriptor.getDependencies() != null )
        {
            List dependencies = new ArrayList();

            for ( Iterator it = pluginDescriptor.getDependencies().iterator(); it.hasNext(); )
            {
                dependencies.add( new MavenPluginDependency( (Dependency) it.next() ) );
            }

            componentSet.setDependencies( dependencies );
        }

        // ----------------------------------------------------------------------
        // Process each of the component descriptors within the ComponentSet.
        // ----------------------------------------------------------------------

        List componentDescriptors = new ArrayList();

        for ( Iterator iterator = pluginDescriptor.getMojos().iterator(); iterator.hasNext(); )
        {
            ComponentDescriptor cd = new MavenMojoDescriptor( (MojoDescriptor) iterator.next() );

            componentDescriptors.add( cd );
        }

        componentSet.setComponents( componentDescriptors );

        return componentSet;
    }
}
