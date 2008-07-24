package org.apache.maven.lifecycle.binding;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleBindingLoader;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
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
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Responsible for the gross construction of LifecycleBindings, or mappings of MojoBinding instances to different parts
 * of the three lifecycles: clean, build, and site. Also, handles transcribing these LifecycleBindings instances into
 * lists of MojoBinding's, which can be consumed by the LifecycleExecutor.
 *
 * @author jdcasey
 *
 */
public class DefaultLifecycleBindingManager
    implements LifecycleBindingManager, LogEnabled, Contextualizable
{
    private Map bindingsByPackaging;

    private Map legacyMappingsByPackaging;

    private PluginLoader pluginLoader;

    private MojoBindingFactory mojoBindingFactory;

    private LegacyLifecycleMappingParser legacyLifecycleMappingParser;

    private Logger logger;

    // configured. Moved out of DefaultLifecycleExecutor...
    private List<org.apache.maven.lifecycle.binding.Lifecycle> lifecycles;

    // configured. Moved out of DefaultLifecycleExecutor...
    private List defaultReports;

    // contextualized, used for setting lookup realm before retrieving lifecycle bindings for packaging.
    private PlexusContainer container;
    
    public List<org.apache.maven.lifecycle.binding.Lifecycle> getLifecycles()
    {
        return lifecycles;
    }

    /**
     * {@inheritDoc}
     */
    public LifecycleBindings getBindingsForPackaging( final MavenProject project, final MavenSession session )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        String packaging = project.getPackaging();

        LifecycleBindings bindings = null;

        ClassRealm projectRealm = session.getRealmManager().getProjectRealm( project.getGroupId(), project.getArtifactId(), project.getVersion() );

        ClassRealm oldRealm = container.setLookupRealm( projectRealm );

        try
        {
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
        }
        finally
        {
            container.setLookupRealm( oldRealm );
        }

        if ( bindings != null )
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
     * {@inheritDoc}
     */
    public LifecycleBindings getDefaultBindings( final MavenProject project ) throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = legacyLifecycleMappingParser.parseDefaultMappings( lifecycles );

        BindingUtils.injectProjectConfiguration( bindings, project );

        return bindings;
    }

    public void enableLogging( final Logger logger )
    {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    public LifecycleBindings getProjectCustomBindings( final MavenProject project, final MavenSession session, Set unbindableMojos )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
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
                                mojoBinding.setOrigin( MojoBinding.POM_ORIGIN );

                                logger.debug( "Mojo: " + MojoBindingUtils.toString( mojoBinding ) + ": determining binding phase." );

                                String phase = execution.getPhase();

                                logger.debug( "Phase from <execution/> section (merged with outer <plugin/> section) is: " + phase );

                                boolean pluginResolved = false;

                                if ( phase == null )
                                {
                                    if ( pluginDescriptor == null )
                                    {
                                        try
                                        {
                                            pluginDescriptor = pluginLoader.loadPlugin( plugin, project, session );
                                            pluginResolved = true;
                                        }
                                        catch ( PluginLoaderException e )
                                        {
                                            unbindableMojos.add( mojoBinding );

                                            String message = "Failed to load plugin descriptor for: "
                                                             + plugin
                                                             + ". Assigning this plugin to be resolved again just prior to its execution. "
                                                             + "NOTE, This may affect assignment of the mojo: "
                                                             + mojoBinding.getGoal()
                                                             + " if its default phase (given in the plugin descriptor) is used.";

                                            if ( logger.isDebugEnabled() )
                                            {
                                                logger.debug( message, e );
                                            }
                                            else
                                            {
                                                logger.warn( message + " Check debug output (-X) for more information." );
                                            }
                                        }
                                    }

                                    if ( pluginDescriptor != null )
                                    {
                                        if ( pluginDescriptor.getMojos() == null )
                                        {
                                            logger.error( "Somehow, the PluginDescriptor for plugin: " + plugin.getKey()
                                                            + " contains no mojos. This is highly irregular. Ignoring..." );

                                            unbindableMojos.add( mojoBinding );
                                            continue;
                                        }

                                        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
                                        phase = mojoDescriptor.getPhase();

                                        logger.debug( "Phase from plugin descriptor: " + mojoDescriptor.getFullGoalName() + " is: " + phase );
                                    }

                                    if ( phase == null )
                                    {
                                        if ( pluginResolved )
                                        {
                                            StringBuffer message = new StringBuffer();

                                            message.append( "\n\nNo lifecycle phase binding can be found for goal: " + goal );
                                            message.append( ",\nspecified as a part of the execution: " + execution.getId() );
                                            message.append( "in plugin: " );
                                            message.append( pluginDescriptor.getPluginLookupKey() );
                                            message.append( "\n\nThis plugin was resolved successfully." );
                                            message.append( "\nHowever, the mojo metadata it contains does not specify a default lifecycle phase binding." );
                                            message.append( "\n\nPlease provide a valid <phase/> specification for execution: " )
                                                   .append( execution.getId() )
                                                   .append( "\nin plugin: " )
                                                   .append( plugin.getKey() );
                                            message.append( "\n\n" );

                                            throw new LifecycleSpecificationException( message.toString() );
                                        }
                                        else
                                        {
                                            logger.warn( "\n\nSkipping addition to build-plan for goal: "
                                                          + goal
                                                          + " in execution: "
                                                          + execution.getId()
                                                          + " of plugin: "
                                                          + plugin.getKey()
                                                          + " because no phase information was available (either through the mojo descriptor, which is currently missing, or in the POM itself).\n\n" );

                                            unbindableMojos.add( mojoBinding );
                                            continue;
                                        }
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
     * {@inheritDoc}
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
            throw new LifecycleLoaderException( "Lifecycle '" + lifecycleId + "' not found in plugin" );
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
                        binding = mojoBindingFactory.parseMojoBinding( goal, project );
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
                    binding.setOrigin( MojoBinding.LIFECYCLE_MAPPING_ORIGIN );
                    binding.setOriginDescription( "Lifecycle overlay: " + lifecycleId );

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
     * {@inheritDoc}
     */
    public List getReportBindings( final MavenProject project, final MavenSession session )
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        if ( project.getModel().getReports() != null )
        {
            logger.warn( "Plugin contains a <reports/> section: this is IGNORED - please use <reporting/> instead." );
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
                    reports.addAll( getReportsForPlugin( reportPlugin, null, project, session ) );
                }
                else
                {
                    for ( Iterator j = reportSets.iterator(); j.hasNext(); )
                    {
                        ReportSet reportSet = (ReportSet) j.next();

                        reports.addAll( getReportsForPlugin( reportPlugin, reportSet, project, session ) );
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
                                      final MavenProject project, final MavenSession session ) throws LifecycleLoaderException
    {
        PluginDescriptor pluginDescriptor;
        try
        {
            pluginDescriptor = pluginLoader.loadReportPlugin( reportPlugin, project, session );
        }
        catch ( PluginLoaderException e )
        {
            throw new LifecycleLoaderException( "Failed to load report plugin: " + reportPlugin.getKey() + ". Reason: "
                            + e.getMessage(), e );
        }

        List reports = new ArrayList();
        for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) i.next();


            // FIXME: Can't we be smarter about what is and what is not a report???
            try
            {
                if ( !isReport( mojoDescriptor ) )
                {
                    continue;
                }
            }
            catch ( ClassNotFoundException e )
            {
                throw new LifecycleLoaderException( "Failed while verifying that mojo: " + mojoDescriptor.getId() + " is a report mojo. Reason: " + e.getMessage(), e );
            }

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
                binding.setOrigin( MojoBinding.POM_ORIGIN );

                BindingUtils.injectProjectConfiguration( binding, project );

                reports.add( binding );
            }
        }
        return reports;
    }

    private boolean isReport( MojoDescriptor mojoDescriptor )
        throws ClassNotFoundException
    {
        ClassRealm classRealm = mojoDescriptor.getPluginDescriptor().getClassRealm();
        String impl = mojoDescriptor.getImplementation();

        Class mojoClass = classRealm.loadClass( impl );
        Class reportClass = classRealm.loadClass( MavenReport.class.getName() );

        return reportClass.isAssignableFrom( mojoClass );
    }

    public void contextualize( Context ctx )
        throws ContextException
    {
        container = (PlexusContainer) ctx.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * {@inheritDoc}
     */
    public void resolveUnbindableMojos( final Set unbindableMojos,
                                        final MavenProject project,
                                        final MavenSession session,
                                        final LifecycleBindings lifecycleBindings )
        throws LifecycleSpecificationException
    {
        for ( Iterator it = unbindableMojos.iterator(); it.hasNext(); )
        {
            MojoBinding binding = (MojoBinding) it.next();
            PluginDescriptor pluginDescriptor;
            try
            {
                pluginDescriptor = pluginLoader.loadPlugin( binding, project, session );
            }
            catch ( PluginLoaderException e )
            {
                String message = "Failed to load plugin descriptor for: "
                                 + MojoBindingUtils.toString( binding )
                                 + ". Cannot discover it's default phase, specified in its plugin descriptor.";

                throw new LifecycleSpecificationException( message, e );
            }

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( binding.getGoal() );
            if ( mojoDescriptor == null )
            {
                throw new LifecycleSpecificationException( "Cannot find mojo descriptor for goal: " + binding.getGoal() + " in plugin: " + pluginDescriptor.getPluginLookupKey() );
            }

            String phase = mojoDescriptor.getPhase();
            if ( phase == null )
            {
                throw new LifecycleSpecificationException(
                                                           "Mojo descriptor: "
                                                                           + mojoDescriptor.getFullGoalName()
                                                                           + " doesn't have a default lifecycle phase. Please specify a <phase/> for this goal in your POM." );
            }

            LifecycleUtils.addMojoBinding( phase, binding, lifecycleBindings );
        }
    }

}
