package org.apache.maven.lifecycle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginLoaderException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

//TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations
//      properly for the wiring and reference and external source for the lifecycle configuration.
//TODO: Inside an IDE we are replacing the notion of our reactor with a workspace. In both of these cases
//      we need to layer these as local repositories.
//TODO: Cache the lookups of the PluginDescriptors

/**
 * @author Jason van Zyl
 */
public class DefaultLifecycleExecutor
    implements LifecycleExecutor, Initializable
{
    @Requirement
    private Logger logger;

    @Requirement
    private PluginManager pluginManager;

    /**
     * These mappings correspond to packaging types, like WAR packaging, which configure a particular mojos
     * to run in a given phase.
     */
    @Requirement
    private Map<String, LifecycleMapping> lifecycleMappings;
    
    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")    
    private List<Lifecycle> lifecycles;

    private Map<String,Lifecycle> lifecycleMap;
    
    private Map<String, Lifecycle> phaseToLifecycleMap;

    public void execute( MavenSession session )
        throws LifecycleExecutionException, MojoFailureException
    {
        // TODO: This is dangerous, particularly when it's just a collection of loose-leaf projects being built
        // within the same reactor (using an inclusion pattern to gather them up)...
        MavenProject rootProject = session.getReactorManager().getTopLevelProject();

        List<String> goals = session.getGoals();

        if ( goals.isEmpty() && rootProject != null )
        {
            String goal = rootProject.getDefaultGoal();

            if ( goal != null )
            {
                goals = Collections.singletonList( goal );
            }
        }

        if ( goals.isEmpty() )
        {
            throw new LifecycleExecutionException( "\n\nYou must specify at least one goal. Try 'mvn install' to build or 'mvn --help' for options \nSee http://maven.apache.org for more information.\n\n" );
        }
        
        for ( MavenProject currentProject : session.getSortedProjects() )
        {
            if ( !session.getReactorManager().isBlackListed( currentProject ) )
            {
                logger.info( "Building " + currentProject.getName() );

                long buildStartTime = System.currentTimeMillis();

                try
                {
                    session.setCurrentProject( currentProject );

                    for ( String goal : goals )
                    {
                        String target = currentProject.getId() + " ( " + goal + " )";
                        executeGoalAndHandleFailures( goal, session, currentProject, buildStartTime, target );
                    }
                }
                finally
                {
                    session.setCurrentProject( null );
                }

                session.getReactorManager().registerBuildSuccess( currentProject, System.currentTimeMillis() - buildStartTime );
            }
        }        
    }

    private void executeGoalAndHandleFailures( String task, MavenSession session, MavenProject project, long buildStartTime, String target )
        throws LifecycleExecutionException, MojoFailureException
    {
        try
        {
            executeGoal( task, session, project );
        }
        catch ( LifecycleExecutionException e )
        {
            if ( handleExecutionFailure( session, project, e, task, buildStartTime ) )
            {
                throw e;
            }
        }
    }

    private boolean handleExecutionFailure( MavenSession session, MavenProject project, Exception e, String task, long buildStartTime )
    {
        //TODO: we shouldn't be registering build failures with the reactor manager, it should be in the session.
        ReactorManager rm = session.getReactorManager();
        
        rm.registerBuildFailure( project, e, task, System.currentTimeMillis() - buildStartTime );

        if ( ReactorManager.FAIL_FAST.equals( rm.getFailureBehavior() ) )
        {
            return true;
        }
        else if ( ReactorManager.FAIL_AT_END.equals( rm.getFailureBehavior() ) )
        {
            rm.blackList( project );
        }
        // if NEVER, don't blacklist
        return false;
    }
    
    private void executeGoal( String task, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, MojoFailureException
    {
        List<MojoExecution> lifecyclePlan = calculateLifecyclePlan( task, session );        
        
        for ( MojoExecution mojoExecution : lifecyclePlan )
        {            
            try
            {                
                logger.info( executionDescription( mojoExecution ) );
                mojoExecution.getMojoDescriptor().getRealm().display();
//                System.out.println( "!!!");
//                System.out.println( mojoExecution.getConfiguration() );
                pluginManager.executeMojo( session, mojoExecution );
            }
            catch ( PluginExecutionException e )
            {
                // This looks like a duplicate
                throw new LifecycleExecutionException( "Error executing goal.", e );                                        
            }
            catch ( PluginConfigurationException e )
            {
                // If the mojo can't actually be configured
                throw new LifecycleExecutionException( "Error executing goal.", e );                                        
            }
        }         
    }
    
    private String executionDescription( MojoExecution me )
    {
        PluginDescriptor pd = me.getMojoDescriptor().getPluginDescriptor();
        StringBuffer sb = new StringBuffer();
        sb.append( "Executing " + pd.getArtifactId() + "[" + pd.getVersion() + "]: " + me.getMojoDescriptor().getGoal() );        
        return sb.toString();
    }
    
    // 1. Find the lifecycle given the phase (default lifecycle when given install)
    // 2. Find the lifecycle mapping that corresponds to the project packaging (jar lifecycle mapping given the jar packaging)
    // 3. Find the mojos associated with the lifecycle given the project packaging (jar lifecycle mapping for the default lifecycle)
    // 4. Bind those mojos found in the lifecycle mapping for the packaging to the lifecycle
    // 5. Bind mojos specified in the project itself to the lifecycle
    public List<MojoExecution> calculateLifecyclePlan( String lifecyclePhase, MavenSession session )
        throws LifecycleExecutionException
    {        
        // Extract the project from the session
        MavenProject project = session.getCurrentProject();
        
        // 1.
        //
        // Based on the lifecycle phase we are given, let's find the corresponding lifecycle.
        //
        Lifecycle lifecycle = phaseToLifecycleMap.get( lifecyclePhase );                
        
        // 2. 
        //
        // If we are dealing with the "clean" or "site" lifecycle then there are currently no lifecycle mappings but there are default phases
        // that need to be run instead.
        //
        // Now we need to take into account the packaging type of the project. For a project of type WAR, the lifecycle where mojos are mapped
        // on to the given phases in the lifecycle are going to be a little different then, say, a project of type JAR.
        //
           
        Map<String, String> lifecyclePhasesForPackaging;
        
        if ( lifecyclePhase.equals( "clean" ) )
        {
            lifecyclePhasesForPackaging = new HashMap<String,String>();
            
            for( String phase : lifecycle.getDefaultPhases() )
            {
                lifecyclePhasesForPackaging.put( "clean", "org.apache.maven.plugins:maven-clean-plugin:clean" );
            }
        }
        else
        {
            LifecycleMapping lifecycleMappingForPackaging = lifecycleMappings.get( project.getPackaging() );
          
            lifecyclePhasesForPackaging = lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() ).getPhases();            
        }
                
        // 3.
        //
        // Once we have the lifecycle mapping for the given packaging, we need to know whats phases we need to worry about executing.
        //
                        
        // Create an ordered Map of the phases in the lifecycle to a list of mojos to execute.
        Map<String,List<String>> phaseToMojoMapping = new LinkedHashMap<String,List<String>>();
        
        // 4. 
        for ( String phase : lifecycle.getPhases() )
        {   
            List<String> mojos = new ArrayList<String>(); 
            
            // Bind the mojos in the lifecycle mapping for the packaging to the lifecycle itself. If
            // we can find the specified phase in the packaging them grab those mojos and add them to 
            // the list we are going to execute.
            String mojo = lifecyclePhasesForPackaging.get( phase );
            
            if ( mojo != null )
            {
                mojos.add( mojo );
            }
            
            phaseToMojoMapping.put( phase, mojos );    
            
            // We only want to execute up to and including the specified lifecycle phase.
            if ( phase.equals( lifecyclePhase ) )
            {
                break;
            }
        }
              
        // 5. 
        //
        // We are only interested in the phases that correspond to the lifecycle we are trying to run. If we are running the "clean"
        // lifecycle we are not interested in goals -- like "generate-sources -- that belong to the default lifecycle.
        //
        for( Plugin plugin : project.getBuild().getPlugins() )
        {            
            for( PluginExecution execution : plugin.getExecutions() )
            {
                // if the phase is specified then I don't have to go fetch the plugin yet and pull it down
                // to examine the phase it is associated to.                
                if ( execution.getPhase() != null && execution.getPhase().equals( lifecyclePhase ) )
                {
                    for( String goal : execution.getGoals() )
                    {
                        String s = plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion() + ":" + goal;
                        phaseToMojoMapping.get( execution.getPhase() ).add( s );
                    }                    
                }                
                // if not then i need to grab the mojo descriptor and look at
                // the phase that is specified
                else
                {
                    for( String goal : execution.getGoals() )
                    {
                        String s = plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion() + ":" + goal;
                        MojoDescriptor md = getMojoDescriptor( s, session.getCurrentProject(), session.getLocalRepository() );
                                                
                        // need to know if this plugin belongs to a phase in the lifecycle that's running
                        if ( md.getPhase() != null && phaseToMojoMapping.get( md.getPhase() ) != null )
                        {
                            phaseToMojoMapping.get( md.getPhase() ).add( s );
                        }
                        
                        //TODO Here we need to break when we have reached the desired phase.
                    }
                }
            }
        }
               
        List<MojoExecution> lifecyclePlan = new ArrayList<MojoExecution>(); 
                        
        // We need to turn this into a set of MojoExecutions
        for( List<String> mojos : phaseToMojoMapping.values() )
        {
            for( String mojo : mojos )
            {
                // These are bits that look like this:
                //
                // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
                //
                MojoDescriptor mojoDescriptor = getMojoDescriptor( mojo, project, session.getLocalRepository() );                                
                
                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
                                
                String g = mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId();
                
                String a = mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId();
                
                Plugin p = project.getPlugin( g + ":" + a );
                
                for( PluginExecution e : p.getExecutions() )
                {
                    for( String goal : e.getGoals() )
                    {
                        if ( mojoDescriptor.getGoal().equals( goal ) )
                        {
                            Xpp3Dom executionConfiguration = (Xpp3Dom) e.getConfiguration();

                            Xpp3Dom mojoConfiguration =
                                extractMojoConfiguration( executionConfiguration, mojoDescriptor );

                            mojoExecution.setConfiguration( mojoConfiguration );
                        }
                    }
                }
                
                lifecyclePlan.add( mojoExecution );
            }
        }  
                
        return lifecyclePlan;
    }  

    /**
     * Extracts the configuration for a single mojo from the specified execution configuration by discarding any
     * non-applicable parameters. This is necessary because a plugin execution can have multiple goals with different
     * parametes whose default configurations are all aggregated into the execution configuration. However, the
     * underlying configurator will error out when trying to configure a mojo parameter that is specified in the
     * configuration but not present in the mojo instance.
     * 
     * @param executionConfiguration The configuration from the plugin execution, must not be {@code null}.
     * @param mojoDescriptor The descriptor for the mojo being configured, must not be {@code null}.
     * @return The configuration for the mojo, never {@code null}.
     */
    private Xpp3Dom extractMojoConfiguration( Xpp3Dom executionConfiguration, MojoDescriptor mojoDescriptor )
    {
        Xpp3Dom mojoConfiguration = new Xpp3Dom( executionConfiguration );

        Collection<String> mojoParameters = mojoDescriptor.getParameterMap().keySet();

        for ( int i = mojoConfiguration.getChildCount() - 1; i >= 0; i-- )
        {
            String mojoParameter = mojoConfiguration.getChild( i ).getName();
            if ( !mojoParameters.contains( mojoParameter ) )
            {
                mojoConfiguration.removeChild( i );
            }
        }

        return mojoConfiguration;
    }

    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    MojoDescriptor getMojoDescriptor( String task, MavenProject project, ArtifactRepository localRepository )
    //MojoDescriptor getMojoDescriptor( String groupId, String artifactId, String version, String goal, MavenProject project, ArtifactRepository localRepository )
        throws LifecycleExecutionException
    {        
        String goal;
        
        Plugin plugin;

        StringTokenizer tok = new StringTokenizer( task, ":" );
        int numTokens = tok.countTokens();
        
        if ( numTokens == 2 )
        {
            String prefix = tok.nextToken();
            goal = tok.nextToken();

            // This is the case where someone has executed a single goal from the command line
            // of the form:
            //
            // mvn remote-resources:process
            //
            // From the metadata stored on the server which has been created as part of a standard
            // Maven plugin deployment we will find the right PluginDescriptor from the remote
            // repository.

            plugin = pluginManager.findPluginForPrefix( prefix, project );

            // Search plugin in the current POM
            if ( plugin == null )
            {
                for ( Plugin buildPlugin : project.getBuildPlugins() )
                {
                    PluginDescriptor desc;
                    
                    try
                    {
                        desc = pluginManager.loadPlugin( buildPlugin, project, localRepository );
                    }
                    catch ( PluginLoaderException e )
                    {
                        throw new LifecycleExecutionException( "Error loading PluginDescriptor.", e );                        
                    }

                    if ( prefix.equals( desc.getGoalPrefix() ) )
                    {
                        plugin = buildPlugin;
                    }
                }
            }
        }
        else if ( numTokens == 3 || numTokens == 4 )
        {
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );

            if ( numTokens == 4 )
            {
                plugin.setVersion( tok.nextToken() );
            }

            goal = tok.nextToken();
        }
        else
        {
            String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or" + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";
            throw new LifecycleExecutionException( message );
        }

        for ( Plugin buildPlugin : project.getBuildPlugins() )
        {
            if ( buildPlugin.getKey().equals( plugin.getKey() ) )
            {
                if ( plugin.getVersion() == null || plugin.getVersion().equals( buildPlugin.getVersion() ) )
                {
                    plugin = buildPlugin;
                }
                break;
            }
        }

        MojoDescriptor mojoDescriptor;
        
        try
        {
            mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, project, localRepository );
        }
        catch ( PluginLoaderException e )
        {
            throw new LifecycleExecutionException( "Error loading MojoDescriptor.", e );
        }        
                
        return mojoDescriptor;
    }
    
    public void initialize()
        throws InitializationException
    {
        lifecycleMap = new HashMap<String,Lifecycle>();
        
        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly so
        // that they don't interfere with internally defined lifecycles.

        phaseToLifecycleMap = new HashMap<String,Lifecycle>();

        for ( Lifecycle lifecycle : lifecycles )
        {                        
            for ( String phase : lifecycle.getPhases() )
            {                
                // The first definition wins.
                if ( !phaseToLifecycleMap.containsKey( phase ) )
                {
                    phaseToLifecycleMap.put( phase, lifecycle );
                }
            }
            
            lifecycleMap.put( lifecycle.getId(), lifecycle );
        }
    }   
    
    public List<String> getLifecyclePhases()
    {
        for ( Lifecycle lifecycle : lifecycles )
        {
            if ( lifecycle.getId().equals( "default" ) )
            {
                return lifecycle.getPhases();
            }
        }

        return null;
    }   
    
    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.
    
    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    //
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        Map<Plugin, Plugin> plugins = new LinkedHashMap<Plugin, Plugin>();
        
        for ( Lifecycle lifecycle : lifecycles )
        {
            LifecycleMapping lifecycleMappingForPackaging = lifecycleMappings.get( packaging );

            org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration = lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );                                                           
            
            if ( lifecycleConfiguration != null )
            {
                Map<String, String> lifecyclePhasesForPackaging = lifecycleConfiguration.getPhases();

                // These are of the form:
                //
                // org.apache.maven.plugins:maven-compiler-plugin:compile
                //
                parseLifecyclePhaseDefinitions( plugins, lifecyclePhasesForPackaging.values() );
            }
            else if ( lifecycle.getDefaultPhases() != null )
            {
                parseLifecyclePhaseDefinitions( plugins, lifecycle.getDefaultPhases() );
            }        
        }

        return plugins.keySet();
    }        

    private void parseLifecyclePhaseDefinitions( Map<Plugin, Plugin> plugins,
                                                 Collection<String> lifecyclePhaseDefinitions )
    {
        for ( String lifecyclePhaseDefinition : lifecyclePhaseDefinitions )
        {
            Plugin plugin = populatePluginWithInformationSpecifiedInLifecyclePhaseDefinition( lifecyclePhaseDefinition );
            Plugin existing = plugins.get( plugin );
            if ( existing != null )
            {
                existing.getExecutions().addAll( plugin.getExecutions() );
            }
            else
            {
                plugins.put( plugin, plugin );
            }
        }
    }

    private Plugin populatePluginWithInformationSpecifiedInLifecyclePhaseDefinition( String lifecyclePhaseDefinition )
    {
        String[] p = StringUtils.split( lifecyclePhaseDefinition, ":" );
        Plugin plugin = new Plugin();
        plugin.setGroupId( p[0] );
        plugin.setArtifactId( p[1] );
        PluginExecution execution = new PluginExecution();
        // FIXME: Find a better execution id
        execution.setId( "default-" + p[2] );
        execution.setGoals( new ArrayList<String>( Arrays.asList( new String[] { p[2] } ) ) );
        plugin.setExecutions( new ArrayList<PluginExecution>( Arrays.asList( new PluginExecution[] { execution } ) ) );
        return plugin;
    }
    
    public Set<Plugin> populateDefaultConfigurationForPlugins( Set<Plugin> plugins, MavenProject project, ArtifactRepository localRepository ) 
        throws LifecycleExecutionException
    {
        for( Plugin p: plugins )
        {
            for( PluginExecution e : p.getExecutions() )
            {
                for( String g : e.getGoals() )
                {
                    Xpp3Dom dom = getDefaultPluginConfiguration( p.getGroupId(), p.getArtifactId(), p.getVersion(), g, project, localRepository );
                    e.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) e.getConfiguration(), dom, Boolean.TRUE ) );
                }
            }
        }
        
        return plugins;
    }    
    
    public Xpp3Dom getDefaultPluginConfiguration( String groupId, String artifactId, String version, String goal, MavenProject project, ArtifactRepository localRepository ) 
        throws LifecycleExecutionException
    {
        //return new Xpp3Dom( "configuration" );
        return convert( getMojoDescriptor( groupId+":"+artifactId+":"+version+":"+goal, project, localRepository ) );
    }
    
    public Xpp3Dom getMojoConfiguration( MojoDescriptor mojoDescriptor )
    {
        return convert( mojoDescriptor );
    }
    
    
    public Xpp3Dom convert( MojoDescriptor mojoDescriptor  )
    {        
        Xpp3Dom dom = new Xpp3Dom( "configuration" );

        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();
                
        PlexusConfiguration[] ces = c.getChildren();
        
        for( PlexusConfiguration ce : ces )
        {            
            String defaultValue = ce.getAttribute( "default-value", null );
            if ( ce.getValue( null ) != null || defaultValue != null )
            {
                Xpp3Dom e = new Xpp3Dom( ce.getName() );
                e.setValue( ce.getValue( null ) );
                if ( defaultValue != null )
                {
                    e.setAttribute( "default-value", defaultValue );
                }
                dom.addChild( e );
            }
        }

        return dom;
    }
    
    // assign all values
    // validate everything is fine
    private Xpp3Dom processConfiguration( MavenSession session, MojoExecution mojoExecution )
    {
        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, mojoExecution );

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
        
        Map<String,Parameter> parameters = mojoDescriptor.getParameterMap();

        Xpp3Dom configuration = mojoExecution.getConfiguration();
        
        for( Xpp3Dom c : configuration.getChildren() )
        {
            String configurationName = c.getName();
            
            Parameter parameter = parameters.get( configurationName );
            
            // Read-only
            
            if ( !parameter.isEditable() )
            {
                
            }
            
            
            
            try
            {
                Object value = expressionEvaluator.evaluate( c.getValue() );
                if ( value == null )
                {
                    String e = c.getAttribute( "default-value" );
                    if ( e != null )
                    {
                        System.out.println( ">> " + e );
                        value = expressionEvaluator.evaluate( e );
                    }                    
                }
                
                if ( value instanceof String || value instanceof File )
                c.setValue( value.toString() );
            }
            catch ( ExpressionEvaluationException e )
            {
                // do nothing
            }
        }
        
        return mojoExecution.getConfiguration();
    }
    
    
    // These are checks that should be available in real time to IDEs

    /*
    checkRequiredMavenVersion( plugin, localRepository, project.getRemoteArtifactRepositories() );
        // Validate against non-editable (@readonly) parameters, to make sure users aren't trying to override in the POM.
        //validatePomConfiguration( mojoDescriptor, pomConfiguration );            
        //checkDeprecatedParameters( mojoDescriptor, pomConfiguration );
        //checkRequiredParameters( mojoDescriptor, pomConfiguration, expressionEvaluator );        
    
    public void checkRequiredMavenVersion( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        // if we don't have the required Maven version, then ignore an update
        if ( ( pluginProject.getPrerequisites() != null ) && ( pluginProject.getPrerequisites().getMaven() != null ) )
        {
            DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion( pluginProject.getPrerequisites().getMaven() );

            if ( runtimeInformation.getApplicationInformation().getVersion().compareTo( requiredVersion ) < 0 )
            {
                throw new PluginVersionResolutionException( plugin.getGroupId(), plugin.getArtifactId(), "Plugin requires Maven version " + requiredVersion );
            }
        }
    }
    
   private void checkDeprecatedParameters( MojoDescriptor mojoDescriptor, PlexusConfiguration extractedMojoConfiguration )
        throws PlexusConfigurationException
    {
        if ( ( extractedMojoConfiguration == null ) || ( extractedMojoConfiguration.getChildCount() < 1 ) )
        {
            return;
        }

        List<Parameter> parameters = mojoDescriptor.getParameters();

        if ( ( parameters != null ) && !parameters.isEmpty() )
        {
            for ( Parameter param : parameters )
            {
                if ( param.getDeprecated() != null )
                {
                    boolean warnOfDeprecation = false;
                    PlexusConfiguration child = extractedMojoConfiguration.getChild( param.getName() );

                    if ( ( child != null ) && ( child.getValue() != null ) )
                    {
                        warnOfDeprecation = true;
                    }
                    else if ( param.getAlias() != null )
                    {
                        child = extractedMojoConfiguration.getChild( param.getAlias() );
                        if ( ( child != null ) && ( child.getValue() != null ) )
                        {
                            warnOfDeprecation = true;
                        }
                    }

                    if ( warnOfDeprecation )
                    {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append( "In mojo: " ).append( mojoDescriptor.getGoal() ).append( ", parameter: " ).append( param.getName() );

                        if ( param.getAlias() != null )
                        {
                            buffer.append( " (alias: " ).append( param.getAlias() ).append( ")" );
                        }

                        buffer.append( " is deprecated:" ).append( "\n\n" ).append( param.getDeprecated() ).append( "\n" );

                        logger.warn( buffer.toString() );
                    }
                }
            }
        }
    }    
    
   private void checkRequiredParameters( MojoDescriptor goal, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        // TODO: this should be built in to the configurator, as we presently double process the expressions

        List<Parameter> parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

        List<Parameter> invalidParameters = new ArrayList<Parameter>();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = parameters.get( i );

            if ( parameter.isRequired() )
            {
                // the key for the configuration map we're building.
                String key = parameter.getName();

                Object fieldValue = null;
                String expression = null;
                PlexusConfiguration value = configuration.getChild( key, false );
                try
                {
                    if ( value != null )
                    {
                        expression = value.getValue( null );

                        fieldValue = expressionEvaluator.evaluate( expression );

                        if ( fieldValue == null )
                        {
                            fieldValue = value.getAttribute( "default-value", null );
                        }
                    }

                    if ( ( fieldValue == null ) && StringUtils.isNotEmpty( parameter.getAlias() ) )
                    {
                        value = configuration.getChild( parameter.getAlias(), false );
                        if ( value != null )
                        {
                            expression = value.getValue( null );
                            fieldValue = expressionEvaluator.evaluate( expression );
                            if ( fieldValue == null )
                            {
                                fieldValue = value.getAttribute( "default-value", null );
                            }
                        }
                    }
                }
                catch ( ExpressionEvaluationException e )
                {
                    throw new PluginConfigurationException( goal.getPluginDescriptor(), e.getMessage(), e );
                }

                // only mark as invalid if there are no child nodes
                if ( ( fieldValue == null ) && ( ( value == null ) || ( value.getChildCount() == 0 ) ) )
                {
                    parameter.setExpression( expression );
                    invalidParameters.add( parameter );
                }
            }
        }

        if ( !invalidParameters.isEmpty() )
        {
            throw new PluginParameterException( goal, invalidParameters );
        }
    }

    private void validatePomConfiguration( MojoDescriptor goal, PlexusConfiguration pomConfiguration )
        throws PluginConfigurationException
    {
        List<Parameter> parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = parameters.get( i );

            // the key for the configuration map we're building.
            String key = parameter.getName();

            PlexusConfiguration value = pomConfiguration.getChild( key, false );

            if ( ( value == null ) && StringUtils.isNotEmpty( parameter.getAlias() ) )
            {
                key = parameter.getAlias();
                value = pomConfiguration.getChild( key, false );
            }

            if ( value != null )
            {
                // Make sure the parameter is either editable/configurable, or else is NOT specified in the POM
                if ( !parameter.isEditable() )
                {
                    StringBuffer errorMessage = new StringBuffer().append( "ERROR: Cannot override read-only parameter: " );
                    errorMessage.append( key );
                    errorMessage.append( " in goal: " ).append( goal.getFullGoalName() );

                    throw new PluginConfigurationException( goal.getPluginDescriptor(), errorMessage.toString() );
                }

                String deprecated = parameter.getDeprecated();
                if ( StringUtils.isNotEmpty( deprecated ) )
                {
                    logger.warn( "DEPRECATED [" + parameter.getName() + "]: " + deprecated );
                }
            }
        }
    }    
    
    */
}
