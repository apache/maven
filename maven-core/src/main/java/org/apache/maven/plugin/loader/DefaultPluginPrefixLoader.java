package org.apache.maven.plugin.loader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.PluginManagerSupport;
import org.apache.maven.plugin.PluginMappingManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DefaultPluginPrefixLoader
    implements PluginPrefixLoader, LogEnabled
{

    private Logger logger;

    private PluginMappingManager pluginMappingManager;

    private MavenPluginCollector pluginCollector;

    private PluginManagerSupport pluginManagerSupport;

    /**
     * Determine the appropriate {@link PluginDescriptor} instance for use with the specified plugin
     * prefix, using the following strategies (in order):
     * <br/>
     * <ol>
     *   <li>Search for a plugin that has already been loaded with the specified prefix</li>
     *   <li>Search for a plugin configured in the POM that has a matching prefix</li>
     *   <li>Search the pluginGroups specified in the settings.xml for a matching plugin</li>
     *   <li>Use groupId == org.apache.maven.plugins, and artifactId == maven-&lt;prefix&gt;-plugin,
     *         and try to resolve based on that.</li>
     * </ol>
     */
    public Plugin findPluginForPrefix( String prefix,
                                       MavenProject project,
                                       MavenSession session )
        throws PluginLoaderException
    {
        Set descriptors = pluginCollector.getPluginDescriptorsForPrefix( prefix );
        Map projectPluginMap = project.getBuild().getPluginsAsMap();

        Plugin plugin = null;

        if ( descriptors != null )
        {
            PluginDescriptor pluginDescriptor = null;

            for ( Iterator it = descriptors.iterator(); it.hasNext(); )
            {
                PluginDescriptor pd = (PluginDescriptor) it.next();

                Plugin projectPlugin = (Plugin) projectPluginMap.get( pd.getPluginLookupKey() );
                if ( ( projectPlugin != null ) && ( projectPlugin.getVersion() != null )
                     && projectPlugin.getVersion().equals( pd.getVersion() ) )
                {
                    pluginDescriptor = pd;
                    break;
                }
            }

            plugin = toPlugin( pluginDescriptor );
        }

        if ( plugin == null )
        {
            PluginDescriptor pluginDescriptor = loadFromProjectForPrefixQuery( prefix, project, session );

            plugin = toPlugin( pluginDescriptor );
        }

        if ( plugin == null )
        {
            plugin = loadFromPrefixMapper( prefix, project, session );
        }


        if ( plugin == null )
        {
            plugin = new Plugin();
            plugin.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( prefix ) );

            PluginDescriptor pluginDescriptor = pluginManagerSupport.loadIsolatedPluginDescriptor( plugin,
                                                                                                   project,
                                                                                                   session );
            plugin = toPlugin( pluginDescriptor );
        }

        if ( plugin == null )
        {
            throw new PluginLoaderException( "Cannot find plugin with prefix: " + prefix );
        }

        return plugin;
    }

    private Plugin toPlugin( PluginDescriptor pluginDescriptor )
    {
        if ( pluginDescriptor == null )
        {
            return null;
        }

        Plugin plugin = new Plugin();

        plugin.setGroupId( pluginDescriptor.getGroupId() );
        plugin.setArtifactId( pluginDescriptor.getArtifactId() );
        plugin.setVersion( pluginDescriptor.getVersion() );

        return plugin;
    }

    /**
     * Look for a plugin configured in the current project that has a prefix matching the one
     * specified. Return the {@link PluginDescriptor} if a match is found.
     */
    private PluginDescriptor loadFromProjectForPrefixQuery( String prefix,
                                                            MavenProject project,
                                                            MavenSession session )
        throws PluginLoaderException
    {
        PluginDescriptor result = null;

        for ( Iterator it = project.getBuildPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            PluginDescriptor pluginDescriptor = pluginManagerSupport.loadIsolatedPluginDescriptor( plugin,
                                                                                                   project,
                                                                                                   session );

            if ( ( pluginDescriptor != null ) && prefix.equals( pluginDescriptor.getGoalPrefix() ) )
            {
                result = pluginDescriptor;
                break;
            }
        }

        return result;
    }

    /**
     * Look for a plugin in the pluginGroups specified in the settings.xml that has a prefix
     * matching the one specified. Return the {@link PluginDescriptor} if a match is found.
     */
    private Plugin loadFromPrefixMapper( String prefix,
                                         MavenProject project,
                                         MavenSession session )
        throws PluginLoaderException
    {
        Settings settings = session.getSettings();

        Plugin plugin = pluginMappingManager.getByPrefix( prefix,
                                                          settings.getPluginGroups(),
                                                          project.getRemoteArtifactRepositories(),
                                                          session.getLocalRepository() );

        if ( plugin != null )
        {
            Plugin projectPlugin = (Plugin) project.getBuild().getPluginsAsMap().get( plugin.getKey() );
            if ( ( projectPlugin != null ) && ( projectPlugin.getVersion() != null ) )
            {
                plugin.setVersion( projectPlugin.getVersion() );
            }
        }

        return plugin;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
