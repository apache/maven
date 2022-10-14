package org.apache.maven.plugin.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.plugin.MavenPluginPrerequisiteChecker;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.StringUtils;

@Named
@Singleton
public class MavenPluginJavaPrerequisiteChecker
    implements MavenPluginPrerequisiteChecker
{

    @Override
    public void accept( PluginDescriptor pluginDescriptor ) throws PluginIncompatibleException
    {
        String requiredJavaVersion = pluginDescriptor.getRequiredJavaVersion();
        if ( StringUtils.isNotBlank( requiredJavaVersion ) )
        {
            if ( JdkVersionProfileActivator.isJavaVersionCompatible( requiredJavaVersion, 
                                                                     System.getProperty( "java.version" ) ) )
            {
                throw new PluginIncompatibleException( pluginDescriptor.getPlugin(),
                                                       "The plugin " + pluginDescriptor.getId()
                                                           + " requires Java version " + requiredJavaVersion );
            }
        }
    }

}
