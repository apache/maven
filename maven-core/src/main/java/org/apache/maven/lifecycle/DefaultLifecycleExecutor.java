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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.PluginLoaderException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

//TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations
//      properly for the wiring and reference and external source for the lifecycle configuration.
//TODO: Inside an IDE we are replacing the notion of our reactor with a workspace. In both of these cases
//      we need to layer these as local repositories.

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

    @Requirement
    private Map<String, LifecycleMapping> lifecycleMappings;
    
    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")    
    private List<Lifecycle> lifecycles;

    private Map<String, Lifecycle> phaseToLifecycleMap;

    public void execute( MavenSession session )
        throws BuildFailureException, LifecycleExecutionException
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
            throw new BuildFailureException( "\n\nYou must specify at least one goal. Try 'mvn install' to build or 'mvn --help' for options \nSee http://maven.apache.org for more information.\n\n" );
        }

        executeTaskSegments( goals, session, rootProject );
    }

    public List<String> getLifecyclePhases()
    {
        for ( Lifecycle lifecycle : lifecycles )
        {
            if ( lifecycle.getId().equals( "default" ) )
            {
                return (List<String>) lifecycle.getPhases();
            }
        }

        return null;
    }

    private void executeTaskSegments( List<String> goals, MavenSession session, MavenProject rootProject )
        throws LifecycleExecutionException, BuildFailureException
    {
        List<MavenProject> sortedProjects = session.getSortedProjects();

        for ( MavenProject currentProject : sortedProjects )
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
        throws BuildFailureException, LifecycleExecutionException
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
        catch ( BuildFailureException e )
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
        throws LifecycleExecutionException, BuildFailureException
    {
        List<MojoDescriptor> lifecyclePlan = calculateLifecyclePlan( task, session );        
        
        for( MojoDescriptor md : lifecyclePlan )
        {
            System.out.println( md.getFullGoalName() );
        }        
        
        /*
        for ( MojoExecution mojoExecution : goals )
         {
             MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

             try
             {
                 pluginManager.executeMojo( project, mojoExecution, session );
             }
             catch ( PluginManagerException e )
             {
                 throw new LifecycleExecutionException( "Internal error in the plugin manager executing goal '" + mojoDescriptor.getId() + "': " + e.getMessage(), e );
             }
             catch ( MojoFailureException e )
             {
                 throw new BuildFailureException( e.getMessage(), e );
             }
             catch ( PluginConfigurationException e )
             {
                 throw new LifecycleExecutionException( e.getMessage(), e );
             }
         }         
          */

    }

    // 1. Find the lifecycle given the phase (default lifecycle when given install)
    // 2. Find the lifecycle mapping that corresponds to the project packaging (jar lifecycle mapping given the jar packaging)
    // 3. Find the mojos associated with the lifecycle given the project packaging (jar lifecycle mapping for the default lifecycle)
    // 4. Bind those mojos found in the lifecycle mapping for the packaging to the lifecycle
    // 5. Bind mojos specified in the project itself to the lifecycle
    public List<MojoDescriptor> calculateLifecyclePlan( String task, MavenSession session )
        throws LifecycleExecutionException
    {
        // Extract the project from the session
        MavenProject project = session.getCurrentProject();
        
        // 1. 
        Lifecycle lifecycle = phaseToLifecycleMap.get( task );
        
        // 2. 
        LifecycleMapping lifecycleMappingForPackaging = lifecycleMappings.get( project.getPackaging() );
        
        // 3.
        Map<String, String> lifecyclePhasesForPackaging = lifecycleMappingForPackaging.getLifecycles().get( "default" ).getPhases();
        
        // Create an order Map of the phases in the lifecycle to a list of mojos to execute.
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
            if ( phase.equals( task ) )
            {
                break;
            }
        }
              
        // 5. 
        
        for( Plugin plugin : project.getBuild().getPlugins() )
        {
            for( PluginExecution execution : plugin.getExecutions() )
            {
                // if the phase is specified then I don't have to go fetch the plugin yet and pull it down
                // to examine the phase it is associated to.                
                if ( execution.getPhase() != null )
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
                        MojoDescriptor md = getMojoDescriptor( s, session, project);
                        phaseToMojoMapping.get( md.getPhase() ).add( s );
                    }
                }
            }
        }
               
        List<MojoDescriptor> lifecyclePlan = new ArrayList<MojoDescriptor>(); 
        
        // We need to turn this into a set of MojoExecutions
        for( List<String> mojos : phaseToMojoMapping.values() )
        {
            for( String mojo : mojos )
            {
                // These are bits that look like this:
                //
                // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
                //
                lifecyclePlan.add( getMojoDescriptor( mojo, session, project ) );
            }
        }  
        
        return lifecyclePlan;
    }  
           
    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project )
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

            plugin = pluginManager.findPluginForPrefix( prefix, project, session );

            // Search plugin in the current POM
            if ( plugin == null )
            {
                for ( Plugin buildPlugin : project.getBuildPlugins() )
                {
                    PluginDescriptor desc;
                    
                    try
                    {
                        desc = pluginManager.loadPlugin( buildPlugin, project, session );
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

        if ( plugin.getVersion() == null )
        {
            for ( Plugin buildPlugin : project.getBuildPlugins() )
            {
                if ( buildPlugin.getKey().equals( plugin.getKey() ) )
                {
                    plugin = buildPlugin;
                    break;
                }
            }

            project.injectPluginManagementInfo( plugin );
        }

        MojoDescriptor mojoDescriptor;
        
        try
        {
            mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session );
        }
        catch ( PluginLoaderException e )
        {
            throw new LifecycleExecutionException( "Error loading MojoDescriptor.", e );
        }        
        
        // this has been simplified from the old code that injected the plugin management stuff, since
        // pluginManagement injection is now handled by the project method.
        project.addPlugin( plugin );        
        
        return mojoDescriptor;
    }

    public void initialize()
        throws InitializationException
    {
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
        }
    }
}
