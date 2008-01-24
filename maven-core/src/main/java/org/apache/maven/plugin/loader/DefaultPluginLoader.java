package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginMappingManager;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for loading plugins, reports, and any components contained therein. Will resolve
 * plugin versions and plugin prefixes as necessary for plugin resolution.
 *
 * @author jdcasey
 *
 */
public class DefaultPluginLoader
    implements PluginLoader, LogEnabled
{

    private Logger logger;

    // FIXME: Move the functionality used from this into the PluginLoader when PluginManager refactor is complete.
    private PluginManager pluginManager;

    private PluginMappingManager pluginMappingManager;

    private MavenPluginCollector pluginCollector;

    /**
     * Load the {@link PluginDescriptor} instance for the plugin implied by the specified MojoBinding,
     * using the project for {@link ArtifactRepository} and other supplemental plugin information as
     * necessary.
     */
    public PluginDescriptor loadPlugin( MojoBinding mojoBinding, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor = null;

        Plugin plugin = new Plugin();
        plugin.setGroupId( mojoBinding.getGroupId() );
        plugin.setArtifactId( mojoBinding.getArtifactId() );
        plugin.setVersion( mojoBinding.getVersion() );

        pluginDescriptor = loadPlugin( plugin, project, session );

        // fill in any blanks once we know more about this plugin.
        if ( pluginDescriptor != null )
        {
            mojoBinding.setGroupId( pluginDescriptor.getGroupId() );
            mojoBinding.setArtifactId( pluginDescriptor.getArtifactId() );
            mojoBinding.setVersion( pluginDescriptor.getVersion() );
        }

        return pluginDescriptor;
    }

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
    public PluginDescriptor findPluginForPrefix( String prefix, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        Set descriptors = pluginCollector.getPluginDescriptorsForPrefix( prefix );
        Map projectPluginMap = project.getBuild().getPluginsAsMap();

        PluginDescriptor pluginDescriptor = null;
        if ( descriptors != null )
        {
            for ( Iterator it = descriptors.iterator(); it.hasNext(); )
            {
                PluginDescriptor pd = (PluginDescriptor) it.next();

                Plugin projectPlugin = (Plugin) projectPluginMap.get( pd.getPluginLookupKey() );
                if ( ( projectPlugin != null ) && ( projectPlugin.getVersion() != null ) && projectPlugin.getVersion().equals( pd.getVersion() ) )
                {
                    pluginDescriptor = pd;
                    break;
                }
            }
        }

        if ( pluginDescriptor == null )
        {
            pluginDescriptor = loadFromProject( prefix, project, session );
        }

        if ( pluginDescriptor == null )
        {
            pluginDescriptor = loadByPrefix( prefix, project, session );
        }

        if ( pluginDescriptor == null )
        {
            Plugin plugin = new Plugin();
            plugin.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( prefix ) );

            pluginDescriptor = loadPlugin( plugin, project, session );
        }

        if ( pluginDescriptor == null )
        {
            throw new PluginLoaderException( "Cannot find plugin with prefix: " + prefix );
        }

        return pluginDescriptor;
    }

    /**
     * Look for a plugin configured in the current project that has a prefix matching the one
     * specified. Return the {@link PluginDescriptor} if a match is found.
     */
    private PluginDescriptor loadFromProject( String prefix, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        PluginDescriptor result = null;

        for ( Iterator it = project.getBuildPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            PluginDescriptor pluginDescriptor = loadPlugin( plugin, project, session );
            if ( prefix.equals( pluginDescriptor.getGoalPrefix() ) )
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
    private PluginDescriptor loadByPrefix( String prefix, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        Settings settings = session.getSettings();

        Plugin plugin = pluginMappingManager.getByPrefix( prefix, settings.getPluginGroups(),
                                                          project.getRemoteArtifactRepositories(), session.getLocalRepository() );

        PluginDescriptor pluginDescriptor = null;
        if ( plugin != null )
        {
            Plugin projectPlugin = (Plugin) project.getBuild().getPluginsAsMap().get( plugin.getKey() );
            if ( ( projectPlugin != null ) && ( projectPlugin.getVersion() != null ) )
            {
                plugin.setVersion( projectPlugin.getVersion() );
            }

            pluginDescriptor = loadPlugin( plugin, project, session );
        }

        return pluginDescriptor;
    }

    /**
     * Load the {@link PluginDescriptor} instance for the specified plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental plugin information as necessary.
     */
    public PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        if ( plugin.getGroupId() == null )
        {
            plugin.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
        }

        try
        {
            PluginDescriptor result = pluginManager.verifyPlugin( plugin, project, session );

            // this has been simplified from the old code that injected the plugin management stuff, since
            // pluginManagement injection is now handled by the project method.
            project.addPlugin( plugin );

            return result;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    /**
     * Load the {@link PluginDescriptor} instance for the report plugin implied by the specified MojoBinding,
     * using the project for {@link ArtifactRepository} and other supplemental report/plugin information as
     * necessary.
     */
    public PluginDescriptor loadReportPlugin( MojoBinding mojoBinding, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        ReportPlugin plugin = new ReportPlugin();
        plugin.setGroupId( mojoBinding.getGroupId() );
        plugin.setArtifactId( mojoBinding.getArtifactId() );
        plugin.setVersion( mojoBinding.getVersion() );

        PluginDescriptor pluginDescriptor = loadReportPlugin( plugin, project, session );

        mojoBinding.setVersion( pluginDescriptor.getVersion() );

        return pluginDescriptor;
    }

    /**
     * Load the {@link PluginDescriptor} instance for the specified report plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental report/plugin information as necessary.
     */
    public PluginDescriptor loadReportPlugin( ReportPlugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        // TODO: Shouldn't we be injecting pluginManagement info here??

        try
        {
            return pluginManager.verifyReportPlugin( plugin, project, session );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
    }

}
