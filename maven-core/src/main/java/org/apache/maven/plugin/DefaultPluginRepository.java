package org.apache.maven.plugin;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.project.builder.Mixer;
import org.apache.maven.execution.MavenSession;

import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;

@Component( role = PluginRepository.class)
public class DefaultPluginRepository implements PluginRepository
{
    @Requirement
    protected MavenPluginCollector pluginCollector;

    public Plugin findPluginById(String id, String mojoId) throws Exception
    {
        if(pluginCollector == null)
        {
            throw new IllegalArgumentException("pluginCollector: null");
        }

        if(id == null)
        {
            throw new IllegalArgumentException("id: null");
        }

        String[] token = id.split(":");
        if(token.length != 3)
        {
            throw new IllegalArgumentException("id: does not include complete id");
        }

        Plugin plugin = new Plugin();
        plugin.setGroupId(token[0]);
        plugin.setArtifactId(token[1]);
        plugin.setVersion(token[2]);

        PluginDescriptor descriptor = pluginCollector.getPluginDescriptor(plugin);
        if(descriptor == null)
        {
            return null;
        }

        for(MojoDescriptor mojo : (List<MojoDescriptor>) descriptor.getMojos())
        {   
            if(mojo.getId().equals(mojoId) && mojo.getMojoConfiguration() != null)
            {
                plugin.setConfiguration(Xpp3DomBuilder.build( new StringReader( mojo.getMojoConfiguration().toString() ) ));
            }
        }

        return plugin;
    }
}
