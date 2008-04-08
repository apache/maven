package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ModelUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Set of utilities used to create and manipulate MojoBindings, both singly and in collections that
 * constitute LifecycleBindings instances. Some of the methods contained here have fairly generic
 * names, but have a specialized purpose for this package (such as those that build plugin keys
 * that lack the version); therefore, this class and all of its methods are package-scoped.
 */
final class BindingUtils
{

    /**
     * Builds a mapping of groupId:artifactId --&gt; Plugin from the POM. If a plugin is listed
     * without a groupId, the {@link BindingUtils#createPluginKey(Plugin)} method will fill it in
     * using org.apache.maven.plugins.
     */
    static Map buildPluginMap( MavenProject project )
    {
        Map pluginMap = new HashMap();

        if ( project != null )
        {
            Build build = project.getBuild();
            if ( build != null )
            {
                for ( Iterator it = build.getPlugins().iterator(); it.hasNext(); )
                {
                    Plugin plugin = (Plugin) it.next();

                    pluginMap.put( createPluginKey( plugin ), plugin );
                }
            }
        }

        return pluginMap;
    }

    /**
     * Builds a mapping of groupId:artifactId --&gt; ReportPlugin from the POM. If a plugin is listed
     * without a groupId, the {@link BindingUtils#createPluginKey(Plugin)} method will fill it in
     * using org.apache.maven.plugins.
     */
    static Map buildReportPluginMap( MavenProject project )
    {
        Map pluginMap = new HashMap();

        if ( project != null )
        {
            Reporting reporting = project.getReporting();
            if ( reporting != null )
            {
                for ( Iterator it = reporting.getPlugins().iterator(); it.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) it.next();

                    pluginMap.put( createPluginKey( plugin.getGroupId(), plugin.getArtifactId() ), plugin );
                }
            }
        }

        return pluginMap;
    }

    /**
     * Builds a mapping of groupId:artifactId --&gt; Plugin from a PluginContainer, such as the build
     * or pluginManagement section of a POM. If a plugin is listed without a groupId, the
     * {@link BindingUtils#createPluginKey(Plugin)} method will fill it in using org.apache.maven.plugins.
     */
    static Map buildPluginMap( PluginContainer pluginContainer )
    {
        Map pluginMap = new HashMap();

        if ( pluginContainer != null )
        {
            for ( Iterator it = pluginContainer.getPlugins().iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();

                pluginMap.put( createPluginKey( plugin ), plugin );
            }
        }

        return pluginMap;
    }

    /**
     * Create a key for the given Plugin, for use in mappings. The key consists of groupId:artifactId,
     * where groupId == org.apache.maven.plugins if the Plugin instance has a groupId == null.
     */
    static String createPluginKey( Plugin plugin )
    {
        return createPluginKey( plugin.getGroupId(), plugin.getArtifactId() );
    }

    /**
     * Create a key for use in looking up Plugin instances from mappings. The key consists of
     * groupId:artifactId, where groupId == org.apache.maven.plugins if the supplied groupId
     * value == null.
     */
    static String createPluginKey( String groupId, String artifactId )
    {
        return ( groupId == null ? PluginDescriptor.getDefaultPluginGroupId() : groupId ) + ":" + artifactId;
    }

    /**
     * Merge the ReportPlugin and ReportSet configurations, with the ReportSet configuration taking
     * precedence.
     */
    static Object mergeConfigurations( ReportPlugin reportPlugin, ReportSet reportSet )
    {
        if ( ( reportPlugin == null ) && ( reportSet == null ) )
        {
            return null;
        }
        else if ( reportSet == null )
        {
            return reportPlugin.getConfiguration();
        }
        else if ( reportPlugin == null )
        {
            return reportSet.getConfiguration();
        }
        else
        {
            return mergeRawConfigurations( reportSet.getConfiguration(), reportPlugin.getConfiguration() );
        }
    }

    /**
     * Merge the Plugin and PluginExecution configurations, with the PluginExecution configuration
     * taking precedence.
     */
    static Object mergeConfigurations( Plugin plugin, PluginExecution execution )
    {
        if ( ( plugin == null ) && ( execution == null ) )
        {
            return null;
        }
        else if ( execution == null )
        {
            return plugin.getConfiguration();
        }
        else if ( plugin == null )
        {
            return execution.getConfiguration();
        }
        else
        {
            return mergeRawConfigurations( execution.getConfiguration(), plugin.getConfiguration() );
        }
    }

    /**
     * Merge two configurations, assuming they are Xpp3Dom instances. This method creates a defensive
     * copy of the dominant configuration before merging, to avoid polluting the original dominant
     * one.
     */
    static Object mergeRawConfigurations( Object dominant, Object recessive )
    {
        Xpp3Dom dominantConfig = (Xpp3Dom) dominant;
        Xpp3Dom recessiveConfig = (Xpp3Dom) recessive;

        if ( recessiveConfig == null )
        {
            return dominantConfig;
        }
        else if ( dominantConfig == null )
        {
            return recessiveConfig;
        }
        else
        {
            return Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( dominantConfig ), recessiveConfig );
        }
    }

    /**
     * Inject any plugin configuration available from the specified POM into the MojoBinding, after
     * first merging in the applicable configuration from the POM's pluginManagement section.
     */
    static void injectProjectConfiguration( MojoBinding binding, MavenProject project )
    {
        Map pluginMap = buildPluginMap( project );

        String key = createPluginKey( binding.getGroupId(), binding.getArtifactId() );
        Plugin plugin = (Plugin) pluginMap.get( key );

        if ( plugin == null )
        {
            plugin = new Plugin();
            plugin.setGroupId( binding.getGroupId() );
            plugin.setArtifactId( binding.getArtifactId() );
            plugin.setVersion( binding.getVersion() );
        }

        injectPluginManagementInfo( plugin, project );

        PluginExecution exec = (PluginExecution) plugin.getExecutionsAsMap().get( binding.getExecutionId() );

        Object configuration = mergeConfigurations( plugin, exec );

        ReportPlugin reportPlugin = (ReportPlugin) BindingUtils.buildReportPluginMap( project ).get( key );
        if ( reportPlugin != null )
        {
            Map reportSets = reportPlugin.getReportSetsAsMap();

            ReportSet reportSet = null;
            if ( ( reportSets != null ) && ( exec != null ) )
            {
                reportSet = (ReportSet) reportSets.get( exec.getId() );
            }

            Object reportConfig = BindingUtils.mergeConfigurations( reportPlugin, reportSet );

            // NOTE: This looks weird, but we must retain some consistency with
            // dominance of plugin configs, regardless of whether they're report
            // mojos or not.
            configuration = mergeRawConfigurations( reportConfig, configuration );
        }

        binding.setVersion( plugin.getVersion() );
        binding.setConfiguration( configuration );
    }

    /**
     * Inject any plugin configuration available from the specified POM into the MojoBindings
     * present in the given LifecycleBindings instance, after first merging in the configuration
     * from the POM's pluginManagement section.
     */
    static void injectProjectConfiguration( LifecycleBindings bindings, MavenProject project )
    {
        Map pluginsByVersionlessKey = buildPluginMap( project );
        Map reportPluginsByVersionlessKey = buildReportPluginMap( project );

        for ( Iterator lifecycleIt = bindings.getBindingList().iterator(); lifecycleIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) lifecycleIt.next();

            for ( Iterator phaseIt = binding.getPhasesInOrder().iterator(); phaseIt.hasNext(); )
            {
                Phase phase = (Phase) phaseIt.next();

                for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
                {
                    MojoBinding mojo = (MojoBinding) mojoIt.next();

                    String pluginKey = createPluginKey( mojo.getGroupId(), mojo.getArtifactId() );

                    Plugin plugin = (Plugin) pluginsByVersionlessKey.get( pluginKey );
                    ReportPlugin reportPlugin = (ReportPlugin) reportPluginsByVersionlessKey.get( pluginKey );

                    if ( plugin == null )
                    {
                        plugin = new Plugin();
                        plugin.setGroupId( mojo.getGroupId() );
                        plugin.setArtifactId( mojo.getArtifactId() );

                        if ( reportPlugin != null )
                        {
                            plugin.setVersion( reportPlugin.getVersion() );
                        }
                    }

                    injectPluginManagementInfo( plugin, project );

                    PluginExecution exec = (PluginExecution) plugin.getExecutionsAsMap().get( mojo.getExecutionId() );

                    mojo.setConfiguration( mergeConfigurations( plugin, exec ) );

                    mojo.setVersion( plugin.getVersion() );

                }
            }
        }
    }

    /**
     * Inject any applicable configuration available from the POM's pluginManagement section into the
     * specified Plugin instance.
     */
    static void injectPluginManagementInfo( Plugin plugin, MavenProject project )
    {
        if ( project == null )
        {
            return;
        }

        Build build = project.getBuild();
        if ( build == null )
        {
            return;
        }

        PluginManagement plugMgmt = build.getPluginManagement();
        if ( plugMgmt == null )
        {
            return;
        }

        Map plugMgmtMap = buildPluginMap( plugMgmt );

        String key = createPluginKey( plugin );

        Plugin mgmtPlugin = (Plugin) plugMgmtMap.get( key );

        if ( mgmtPlugin != null )
        {
            ModelUtils.mergePluginDefinitions( plugin, mgmtPlugin, false );
        }
    }

}
