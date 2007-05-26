package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleBindingLoader;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Lifecycle;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.collections.ActiveMap;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Responsible for the gross construction of LifecycleBindings, or mappings of MojoBinding instances to different parts
 * of the three lifecycles: clean, build, and site. Also, handles transcribing these LifecycleBindings instances into
 * lists of MojoBinding's, which can be consumed by the LifecycleExecutor.
 * 
 * @author jdcasey
 * 
 */
public class DefaultLifecycleBindingManager implements LifecycleBindingManager, LogEnabled
{

    private ActiveMap bindingsByPackaging;

    private ActiveMap legacyMappingsByPackaging;

    private PluginLoader pluginLoader;

    private MojoBindingFactory mojoBindingFactory;

    private LegacyLifecycleMappingParser legacyLifecycleMappingParser;

    private Logger logger;

    // configured. Moved out of DefaultLifecycleExecutor...
    private List legacyLifecycles;

    // configured. Moved out of DefaultLifecycleExecutor...
    private List defaultReports;

    /**
     * Retrieve the LifecycleBindings given by the lifecycle mapping component/file for the project's packaging. Any
     * applicable mojo configuration will be injected into the LifecycleBindings from the POM.
     */
    public LifecycleBindings getBindingsForPackaging( final MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        String packaging = project.getPackaging();

        LifecycleBindings bindings = null;

        LifecycleBindingLoader loader = (LifecycleBindingLoader) bindingsByPackaging.get( packaging );
        if ( loader != null )
        {
            bindings = loader.getBindings();
        }

        // TODO: Remove this once we no longer have to support legacy-style lifecycle mappings
        if ( bindings == null )
        {
            LifecycleMapping mapping = (LifecycleMapping) legacyMappingsByPackaging.get( packaging );
            if ( mapping != null )
            {
                bindings = legacyLifecycleMappingParser.parseMappings( mapping, packaging );
            }
        }

        if ( bindings == null )
        {
            bindings = searchPluginsWithExtensions( project );
        }
        else
        {
            BindingUtils.injectProjectConfiguration( bindings, project );
        }

        if ( bindings == null )
        {
            bindings = getDefaultBindings( project );
        }

        return bindings;
    }

    /**
     * Search all plugins configured in the POM that have extensions == true, looking for either a
     * {@link LifecycleBindingLoader} instance, or a {@link LifecycleMapping} instance that matches the project's
     * packaging. For the first match found, construct the corresponding LifecycleBindings instance and return it after
     * POM configurations have been injected into any appropriate MojoBinding instances contained within.
     */
    private LifecycleBindings searchPluginsWithExtensions( final MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        List plugins = project.getBuildPlugins();
        String packaging = project.getPackaging();

        LifecycleBindings bindings = null;

        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();
            if ( plugin.isExtensions() )
            {
                LifecycleBindingLoader loader = null;

                try
                {
                    loader =
                        (LifecycleBindingLoader) pluginLoader.loadPluginComponent( LifecycleBindingLoader.ROLE,
                                                                                   packaging, plugin, project );
                }
                catch ( ComponentLookupException e )
                {
                    logger.debug( LifecycleBindingLoader.ROLE + " for packaging: " + packaging
                                    + " could not be retrieved from plugin: " + plugin.getKey() + ".\nReason: "
                                    + e.getMessage(), e );
                }
                catch ( PluginLoaderException e )
                {
                    throw new LifecycleLoaderException( "Failed to load plugin: " + plugin.getKey() + ". Reason: "
                                    + e.getMessage(), e );
                }

                if ( loader != null )
                {
                    bindings = loader.getBindings();
                }

                // TODO: Remove this once we no longer have to support legacy-style lifecycle mappings
                if ( bindings == null )
                {
                    LifecycleMapping mapping = null;
                    try
                    {
                        mapping =
                            (LifecycleMapping) pluginLoader.loadPluginComponent( LifecycleMapping.ROLE, packaging,
                                                                                 plugin, project );
                    }
                    catch ( ComponentLookupException e )
                    {
                        logger.debug( LifecycleMapping.ROLE + " for packaging: " + packaging
                                        + " could not be retrieved from plugin: " + plugin.getKey() + ".\nReason: "
                                        + e.getMessage(), e );
                    }
                    catch ( PluginLoaderException e )
                    {
                        throw new LifecycleLoaderException( "Failed to load plugin: " + plugin.getKey() + ". Reason: "
                                        + e.getMessage(), e );
                    }

                    if ( mapping != null )
                    {
                        bindings = legacyLifecycleMappingParser.parseMappings( mapping, packaging );
                    }
                }

                if ( bindings != null )
                {
                    break;
                }
            }
        }

        if ( bindings != null )
        {
            BindingUtils.injectProjectConfiguration( bindings, project );
        }

        return bindings;
    }

    /**
     * Construct the LifecycleBindings for the default lifecycle mappings, including injection of configuration from the
     * project into each MojoBinding, where appropriate.
     */
    public LifecycleBindings getDefaultBindings( final MavenProject project ) throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = legacyLifecycleMappingParser.parseDefaultMappings( legacyLifecycles );

        BindingUtils.injectProjectConfiguration( bindings, project );

        return bindings;
    }

    public void enableLogging( final Logger logger )
    {
        this.logger = logger;
    }

    /**
     * Construct the LifecycleBindings that constitute the extra mojos bound to the lifecycle within the POM itself.
     */
    public LifecycleBindings getProjectCustomBindings( final MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        String projectId = project.getId();

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setPackaging( project.getPackaging() );

        List plugins = project.getBuildPlugins();
        if ( plugins != null )
        {
            for ( Iterator it = plugins.iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();
                BindingUtils.injectPluginManagementInfo( plugin, project );

                PluginDescriptor pluginDescriptor = null;

                List executions = plugin.getExecutions();
                if ( executions != null )
                {
                    for ( Iterator execIt = executions.iterator(); execIt.hasNext(); )
                    {
                        PluginExecution execution = (PluginExecution) execIt.next();

                        List goals = execution.getGoals();
                        if ( ( goals != null ) && !goals.isEmpty() )
                        {
                            for ( Iterator goalIterator = goals.iterator(); goalIterator.hasNext(); )
                            {
                                String goal = (String) goalIterator.next();

                                if ( goal == null )
                                {
                                    logger.warn( "Execution: " + execution.getId() + " in plugin: " + plugin.getKey()
                                                    + " in the POM has a null goal." );
                                    continue;
                                }

                                MojoBinding mojoBinding = new MojoBinding();

                                mojoBinding.setGroupId( plugin.getGroupId() );
                                mojoBinding.setArtifactId( plugin.getArtifactId() );
                                mojoBinding.setVersion( plugin.getVersion() );
                                mojoBinding.setGoal( goal );
                                mojoBinding.setConfiguration( BindingUtils.mergeConfigurations( plugin, execution ) );
                                mojoBinding.setExecutionId( execution.getId() );
                                mojoBinding.setOrigin( "POM" );

                                String phase = execution.getPhase();
                                if ( phase == null )
                                {
                                    if ( pluginDescriptor == null )
                                    {
                                        try
                                        {
                                            pluginDescriptor = pluginLoader.loadPlugin( plugin, project );
                                        }
                                        catch ( PluginLoaderException e )
                                        {
                                            throw new LifecycleLoaderException( "Failed to load plugin: " + plugin
                                                            + ". Reason: " + e.getMessage(), e );
                                        }
                                    }

                                    if ( pluginDescriptor.getMojos() == null )
                                    {
                                        logger.error( "Somehow, the PluginDescriptor for plugin: " + plugin.getKey()
                                                        + " contains no mojos. This is highly irregular. Ignoring..." );
                                        continue;
                                    }

                                    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
                                    phase = mojoDescriptor.getPhase();

                                    if ( phase == null )
                                    {
                                        throw new LifecycleSpecificationException( "No phase specified for goal: "
                                                        + goal + " in plugin: " + plugin.getKey() + " from POM: "
                                                        + projectId );
                                    }
                                }

                                LifecycleUtils.addMojoBinding( phase, mojoBinding, bindings );
                            }
                        }
                    }
                }
            }
        }

        LifecycleUtils.setOrigin( bindings, "POM" );

        return bindings;
    }

    /**
     * Construct the LifecycleBindings that constitute the mojos mapped to the lifecycles by an overlay specified in a
     * plugin. Inject mojo configuration from the POM into all appropriate MojoBinding instances.
     */
    public LifecycleBindings getPluginLifecycleOverlay( final PluginDescriptor pluginDescriptor,
                                                        final String lifecycleId, final MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        Lifecycle lifecycleOverlay = null;

        try
        {
            lifecycleOverlay = pluginDescriptor.getLifecycleMapping( lifecycleId );
        }
        catch ( IOException e )
        {
            throw new LifecycleLoaderException( "Unable to read lifecycle mapping file: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new LifecycleLoaderException( "Unable to parse lifecycle mapping file: " + e.getMessage(), e );
        }

        if ( lifecycleOverlay == null )
        {
            throw new LifecycleLoaderException( "LegacyLifecycle '" + lifecycleId + "' not found in plugin" );
        }

        LifecycleBindings bindings = new LifecycleBindings();

        for ( Iterator i = lifecycleOverlay.getPhases().iterator(); i.hasNext(); )
        {
            org.apache.maven.plugin.lifecycle.Phase phase = (org.apache.maven.plugin.lifecycle.Phase) i.next();
            List phaseBindings = new ArrayList();

            for ( Iterator j = phase.getExecutions().iterator(); j.hasNext(); )
            {
                Execution exec = (Execution) j.next();

                for ( Iterator k = exec.getGoals().iterator(); k.hasNext(); )
                {
                    String goal = (String) k.next();

                    // Here we are looking to see if we have a mojo from an external plugin.
                    // If we do then we need to lookup the plugin descriptor for the externally
                    // referenced plugin so that we can overly the execution into the lifecycle.
                    // An example of this is the corbertura plugin that needs to call the surefire
                    // plugin in forking mode.
                    //
                    // <phase>
                    // <id>test</id>
                    // <executions>
                    // <execution>
                    // <goals>
                    // <goal>org.apache.maven.plugins:maven-surefire-plugin:test</goal>
                    // </goals>
                    // <configuration>
                    // <classesDirectory>${project.build.directory}/generated-classes/cobertura</classesDirectory>
                    // <ignoreFailures>true</ignoreFailures>
                    // <forkMode>once</forkMode>
                    // </configuration>
                    // </execution>
                    // </executions>
                    // </phase>

                    // ----------------------------------------------------------------------
                    //
                    // ----------------------------------------------------------------------

                    MojoBinding binding;
                    if ( goal.indexOf( ":" ) > 0 )
                    {
                        binding = mojoBindingFactory.parseMojoBinding( goal, project, false );
                    }
                    else
                    {
                        binding = new MojoBinding();
                        binding.setGroupId( pluginDescriptor.getGroupId() );
                        binding.setArtifactId( pluginDescriptor.getArtifactId() );
                        binding.setVersion( pluginDescriptor.getVersion() );
                        binding.setGoal( goal );
                    }

                    Xpp3Dom configuration = (Xpp3Dom) exec.getConfiguration();
                    if ( phase.getConfiguration() != null )
                    {
                        configuration =
                            Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( (Xpp3Dom) phase.getConfiguration() ), configuration );
                    }

                    binding.setConfiguration( configuration );
                    binding.setOrigin( lifecycleId );

                    LifecycleUtils.addMojoBinding( phase.getId(), binding, bindings );
                    phaseBindings.add( binding );
                }
            }

            if ( phase.getConfiguration() != null )
            {
                // Merge in general configuration for a phase.
                // TODO: this is all kind of backwards from the POMM. Let's align it all under 2.1.
                // We should create a new lifecycle executor for modelVersion >5.0.0
                // [jdcasey; 08-March-2007] Not sure what the above to-do references...how _should_
                // this work??
                for ( Iterator j = phaseBindings.iterator(); j.hasNext(); )
                {
                    MojoBinding binding = (MojoBinding) j.next();

                    Xpp3Dom configuration =
                        Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( (Xpp3Dom) phase.getConfiguration() ),
                                              (Xpp3Dom) binding.getConfiguration() );

                    binding.setConfiguration( configuration );
                }
            }

        }

        return bindings;
    }

    /**
     * Retrieve the list of MojoBinding instances that correspond to the reports configured for the specified project.
     * Inject all appropriate configuration from the POM for each MojoBinding, using the following precedence rules:
     * <br/>
     * <ol>
     * <li>report-set-level configuration</li>
     * <li>reporting-level configuration</li>
     * <li>execution-level configuration</li>
     * <li>plugin-level configuration</li>
     * </ol>
     */
    public List getReportBindings( final MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        if ( project.getModel().getReports() != null )
        {
            logger.error( "Plugin contains a <reports/> section: this is IGNORED - please use <reporting/> instead." );
        }

        List reportPlugins = getReportPluginsForProject( project );

        List reports = new ArrayList();
        if ( reportPlugins != null )
        {
            for ( Iterator it = reportPlugins.iterator(); it.hasNext(); )
            {
                ReportPlugin reportPlugin = (ReportPlugin) it.next();

                List reportSets = reportPlugin.getReportSets();

                if ( ( reportSets == null ) || reportSets.isEmpty() )
                {
                    reports.addAll( getReportsForPlugin( reportPlugin, null, project ) );
                }
                else
                {
                    for ( Iterator j = reportSets.iterator(); j.hasNext(); )
                    {
                        ReportSet reportSet = (ReportSet) j.next();

                        reports.addAll( getReportsForPlugin( reportPlugin, reportSet, project ) );
                    }
                }
            }
        }
        return reports;
    }

    /**
     * Retrieve the ReportPlugin instances referenced in the specified POM.
     */
    private List getReportPluginsForProject( final MavenProject project )
    {
        List reportPlugins = project.getReportPlugins();

        if ( ( project.getReporting() == null ) || !project.getReporting().isExcludeDefaults() )
        {
            if ( reportPlugins == null )
            {
                reportPlugins = new ArrayList();
            }
            else
            {
                reportPlugins = new ArrayList( reportPlugins );
            }

            for ( Iterator i = defaultReports.iterator(); i.hasNext(); )
            {
                String report = (String) i.next();

                StringTokenizer tok = new StringTokenizer( report, ":" );
                if ( tok.countTokens() != 2 )
                {
                    logger.warn( "Invalid default report ignored: '" + report + "' (must be groupId:artifactId)" );
                }
                else
                {
                    String groupId = tok.nextToken();
                    String artifactId = tok.nextToken();

                    boolean found = false;
                    for ( Iterator j = reportPlugins.iterator(); j.hasNext() && !found; )
                    {
                        ReportPlugin reportPlugin = (ReportPlugin) j.next();
                        if ( reportPlugin.getGroupId().equals( groupId )
                                        && reportPlugin.getArtifactId().equals( artifactId ) )
                        {
                            found = true;
                        }
                    }

                    if ( !found )
                    {
                        ReportPlugin reportPlugin = new ReportPlugin();
                        reportPlugin.setGroupId( groupId );
                        reportPlugin.setArtifactId( artifactId );
                        reportPlugins.add( reportPlugin );
                    }
                }
            }
        }

        return reportPlugins;
    }

    /**
     * Retrieve any reports from the specified ReportPlugin which are referenced in the specified POM.
     */
    private List getReportsForPlugin( final ReportPlugin reportPlugin, final ReportSet reportSet,
                                      final MavenProject project ) throws LifecycleLoaderException
    {
        PluginDescriptor pluginDescriptor;
        try
        {
            pluginDescriptor = pluginLoader.loadReportPlugin( reportPlugin, project );
        }
        catch ( PluginLoaderException e )
        {
            throw new LifecycleLoaderException( "Failed to load report plugin: " + reportPlugin.getKey() + ". Reason: "
                            + e.getMessage(), e );
        }

        String pluginKey = BindingUtils.createPluginKey( reportPlugin.getGroupId(), reportPlugin.getArtifactId() );
        Plugin plugin = (Plugin) BindingUtils.buildPluginMap( project ).get( pluginKey );

        List reports = new ArrayList();
        for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) i.next();

            // TODO: check ID is correct for reports
            // if the POM configured no reports, give all from plugin
            if ( ( reportSet == null ) || reportSet.getReports().contains( mojoDescriptor.getGoal() ) )
            {
                String id = null;
                if ( reportSet != null )
                {
                    id = reportSet.getId();
                }

                MojoBinding binding = new MojoBinding();
                binding.setGroupId( pluginDescriptor.getGroupId() );
                binding.setArtifactId( pluginDescriptor.getArtifactId() );
                binding.setVersion( pluginDescriptor.getVersion() );
                binding.setGoal( mojoDescriptor.getGoal() );
                binding.setExecutionId( id );
                binding.setOrigin( "POM" );

                Object reportConfig = BindingUtils.mergeConfigurations( reportPlugin, reportSet );

                if ( plugin == null )
                {
                    plugin = new Plugin();
                    plugin.setGroupId( pluginDescriptor.getGroupId() );
                    plugin.setArtifactId( pluginDescriptor.getArtifactId() );
                }

                BindingUtils.injectPluginManagementInfo( plugin, project );

                Map execMap = plugin.getExecutionsAsMap();
                PluginExecution exec = (PluginExecution) execMap.get( id );

                Object pluginConfig = plugin.getConfiguration();
                if ( exec != null )
                {
                    pluginConfig = BindingUtils.mergeConfigurations( plugin, exec );
                }

                reportConfig = BindingUtils.mergeRawConfigurations( reportConfig, pluginConfig );

                binding.setConfiguration( reportConfig );

                reports.add( binding );
            }
        }
        return reports;
    }

}
