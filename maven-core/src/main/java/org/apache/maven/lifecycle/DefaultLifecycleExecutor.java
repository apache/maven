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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataReadException;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.CycleDetectedInPluginGraphException;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

//TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations
//      properly for the wiring and reference and external source for the lifecycle configuration.
//TODO: check for online status in the build plan and die if necessary
//TODO if ( mojoDescriptor.isProjectRequired() && !session.isUsingPOMsFromFilesystem() )
//{
//    throw new PluginExecutionException( mojoExecution, project, "Cannot execute mojo: " + mojoDescriptor.getGoal()
//        + ". It requires a project with an existing pom.xml, but the build is not using one." );
//}

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
    protected RepositorySystem repositorySystem;

    /**
     * These mappings correspond to packaging types, like WAR packaging, which configure a particular mojos
     * to run in a given phase.
     */
    @Requirement
    private Map<String, LifecycleMapping> lifecycleMappings;
    
    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;
    
    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")    
    private List<Lifecycle> lifecycles;

    private Map<String,Lifecycle> lifecycleMap;
    
    private Map<String, Lifecycle> phaseToLifecycleMap;

    public void execute( MavenSession session )
    {
        // TODO: Use a listener here instead of loggers
        
        logger.info(  "Build Order:" );
        
        logger.info( "" );
        
        for( MavenProject project : session.getProjects() )
        {
            logger.info( project.getName() );
        }
        
        logger.info( "" );
        
        MavenProject rootProject = session.getTopLevelProject();

        List<String> goals = session.getGoals();

        if ( goals.isEmpty() && rootProject != null )
        {
            String goal = rootProject.getDefaultGoal();

            if ( goal != null )
            {
                goals = Collections.singletonList( goal );
            }
        }
                
        for ( MavenProject currentProject : session.getProjects() )
        {
            logger.info( "Building " + currentProject.getName() );

            try
            {
                session.setCurrentProject( currentProject );

                MavenExecutionPlan executionPlan;

                try
                {
                    executionPlan = calculateExecutionPlan( session, goals.toArray( new String[] {} ) );
                }
                catch ( Exception e )
                {
                    session.getResult().addException( e );
                    return;
                }

                //TODO: once we have calculated the build plan then we should accurately be able to download
                // the project dependencies. Having it happen in the plugin manager is a tangled mess. We can optimize this
                // later by looking at the build plan. Would be better to just batch download everything required by the reactor.

                // mojoDescriptor.isDependencyResolutionRequired() is actually the scope of the dependency resolution required, not a boolean ... yah.
                try
                {
                    Set<Artifact> projectDependencies = projectDependenciesResolver.resolve( currentProject, executionPlan.getRequiredResolutionScope(), session.getLocalRepository(), currentProject.getRemoteArtifactRepositories() );
                    currentProject.setArtifacts( projectDependencies );    
                }
                catch ( ArtifactNotFoundException e )
                {
                    session.getResult().addException( e );
                    return;
                }
                catch ( ArtifactResolutionException e )
                {
                    session.getResult().addException( e );
                    return;
                }

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "=== BUILD PLAN ===" );
                    logger.debug( "Project:       " + currentProject );
                    for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
                    {
                        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
                        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
                        logger.debug( "------------------" );
                        logger.debug( "Goal:          " + pluginDescriptor.getGroupId() + ':' + pluginDescriptor.getArtifactId() + ':' + pluginDescriptor.getVersion() + ':' + mojoDescriptor.getGoal()
                            + ':' + mojoExecution.getExecutionId() );
                        logger.debug( "Configuration: " + String.valueOf( mojoExecution.getConfiguration() ) );
                    }
                    logger.debug( "==================" );
                }

                for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
                {
                    try
                    {
                        logger.info( executionDescription( mojoExecution, currentProject ) );
                        pluginManager.executeMojo( session, mojoExecution );
                    }
                    catch ( Exception e )
                    {
                        session.getResult().addException( e );
                        return;
                    }
                }                         
                
            }
            finally
            {
                session.setCurrentProject( null );
            }
        }        
    }        
        
    // 1. Find the lifecycle given the phase (default lifecycle when given install)
    // 2. Find the lifecycle mapping that corresponds to the project packaging (jar lifecycle mapping given the jar packaging)
    // 3. Find the mojos associated with the lifecycle given the project packaging (jar lifecycle mapping for the default lifecycle)
    // 4. Bind those mojos found in the lifecycle mapping for the packaging to the lifecycle
    // 5. Bind mojos specified in the project itself to the lifecycle
    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException
    {        
        MavenProject project = session.getCurrentProject();
                
        List<MojoExecution> phasesWithMojosToExecute = new ArrayList<MojoExecution>();
        
        List<MojoExecution> lifecyclePlan = new ArrayList<MojoExecution>();
                
        String requiredDependencyResolutionScope = null;
        
        for ( String task : tasks )
        {

            if ( task.indexOf( ":" ) > 0 )
            {
                // If this is a goal like "mvn modello:java" and the POM looks like the following:
                
                // <project>
                //   <modelVersion>4.0.0</modelVersion>
                //   <groupId>org.apache.maven.plugins</groupId>
                //   <artifactId>project-plugin-level-configuration-only</artifactId>
                //   <version>1.0.1</version>
                //   <build>
                //     <plugins>
                //       <plugin>
                //         <groupId>org.codehaus.modello</groupId>
                //         <artifactId>modello-maven-plugin</artifactId>
                //         <version>1.0.1</version>
                //         <configuration>
                //           <version>1.1.0</version>
                //           <models>
                //             <model>src/main/mdo/remote-resources.mdo</model>
                //           </models>
                //         </configuration>
                //       </plugin>
                //     </plugins>
                //   </build>
                // </project>                
                //
                // We want to 
                //
                // - take the plugin/configuration in the POM and merge it with the plugin's default configuration found in its plugin.xml
                // - attach that to the MojoExecution for its configuration
                // - give the MojoExecution an id of default-<goal>.
                
                MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session );

                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, "default-" + mojoDescriptor.getGoal() );
                
                populateMojoExecutionConfiguration( project, mojoExecution, true );

                lifecyclePlan.add( mojoExecution );
            }
            else
            {
                // 1.
                //
                // Based on the lifecycle phase we are given, let's find the corresponding lifecycle.
                //
                Lifecycle lifecycle = phaseToLifecycleMap.get( task );

                // 2. 
                //
                // If we are dealing with the "clean" or "site" lifecycle then there are currently no lifecycle mappings but there are default phases
                // that need to be run instead.
                //
                // Now we need to take into account the packaging type of the project. For a project of type WAR, the lifecycle where mojos are mapped
                // on to the given phases in the lifecycle are going to be a little different then, say, a project of type JAR.
                //

                // 3.
                //
                // Once we have the lifecycle mapping for the given packaging, we need to know whats phases we need to worry about executing.
                //

                // Create an ordered Map of the phases in the lifecycle to a list of mojos to execute.
                Map<String, List<MojoExecution>> phaseToMojoMapping = new LinkedHashMap<String, List<MojoExecution>>();

                // 4.

                //TODO: need to separate the lifecycles

                for ( String phase : lifecycle.getPhases() )
                {
                    List<MojoExecution> mojos = new ArrayList<MojoExecution>();

                    //TODO: remove hard coding
                    if ( phase.equals( "clean" ) )
                    {
                        mojos.add( new MojoExecution( "org.apache.maven.plugins", "maven-clean-plugin", "2.3", "clean", "default-clean" ) );
                    }

                    // This is just just laying out the initial structure of the mojos to run in each phase of the
                    // lifecycle. Everything is now done in the project builder correctly so this could likely
                    // go away shortly. We no longer need to pull out bits from the default lifecycle. The MavenProject
                    // comes to us intact as it should.

                    phaseToMojoMapping.put( phase, mojos );
                }

                // 5. Just build up the list of mojos that will execute for every phase.
                //
                // This will be useful for having the complete build plan and then we can filter/optimize later.
                //
                for ( Plugin plugin : project.getBuild().getPlugins() )
                {
                    for ( PluginExecution execution : plugin.getExecutions() )
                    {
                        // if the phase is specified then I don't have to go fetch the plugin yet and pull it down
                        // to examine the phase it is associated to.                
                        if ( execution.getPhase() != null )
                        {
                            for ( String goal : execution.getGoals() )
                            {
                                if ( phaseToMojoMapping.get( execution.getPhase() ) == null )
                                {
                                    // This is happening because executions in the POM are getting mixed into the clean lifecycle
                                    // So for the lifecycle mapping we need a map with the phases as keys so we can easily check
                                    // if this phase belongs to the given lifecycle. this shows the system is messed up. this
                                    // shouldn't happen.
                                    phaseToMojoMapping.put( execution.getPhase(), new ArrayList<MojoExecution>() );
                                }

                                MojoExecution mojoExecution = new MojoExecution( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), goal, execution.getId() );
                                phaseToMojoMapping.get( execution.getPhase() ).add( mojoExecution );
                            }
                        }
                        // if not then i need to grab the mojo descriptor and look at the phase that is specified
                        else
                        {
                            for ( String goal : execution.getGoals() )
                            {
                                MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session.getLocalRepository(), project.getRemoteArtifactRepositories() );

                                if ( mojoDescriptor.getPhase() != null && phaseToMojoMapping.get( mojoDescriptor.getPhase() ) != null )
                                {
                                    MojoExecution mojoExecution = new MojoExecution( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), goal, execution.getId() );
                                    phaseToMojoMapping.get( mojoDescriptor.getPhase() ).add( mojoExecution );
                                }
                            }
                        }
                    }
                }

                // 6. 
                //
                // We are only interested in the phases that correspond to the lifecycle we are trying to run. If we are running the "clean"
                // lifecycle we are not interested in goals -- like "generate-sources -- that belong to the default lifecycle.
                //        
                for ( String phase : phaseToMojoMapping.keySet() )
                {
                    phasesWithMojosToExecute.addAll( phaseToMojoMapping.get( phase ) );

                    if ( phase.equals( task ) )
                    {
                        break;
                    }
                }
            }
        }
              
        // 7. Now we create the correct configuration for the mojo to execute.
        //TODO: this needs to go to the model builder.
        //TODO: just used a hollowed out MojoExecution
        for ( MojoExecution mojoExecution : phasesWithMojosToExecute )
        {
            // These are bits that look like this:
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
            //                        
            MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( 
                mojoExecution.getGroupId(), mojoExecution.getArtifactId(), mojoExecution.getVersion(), mojoExecution.getGoal(), session.getLocalRepository(), project.getRemoteArtifactRepositories() );

            requiredDependencyResolutionScope = calculateRequiredDependencyResolutionScope( requiredDependencyResolutionScope, mojoDescriptor.isDependencyResolutionRequired() );          
            
            mojoExecution.setMojoDescriptor( mojoDescriptor );
            
            populateMojoExecutionConfiguration( project, mojoExecution, false );

            lifecyclePlan.add( mojoExecution );
        }        
        
        return new MavenExecutionPlan( lifecyclePlan, requiredDependencyResolutionScope );        
    }  

    // SCOPE_COMPILE
    // SCOPE_TEST
    // SCOPE_RUNTIME
    //
    String calculateRequiredDependencyResolutionScope( String currentRequiredDependencyResolutionScope, String inputScope )
    {
        if ( inputScope == null )
        {
            return currentRequiredDependencyResolutionScope;
        }
                
        if ( currentRequiredDependencyResolutionScope == null && inputScope != null )
        {
            return inputScope;
        }

        if ( currentRequiredDependencyResolutionScope.equals( Artifact.SCOPE_COMPILE ) && ( inputScope.equals(  Artifact.SCOPE_RUNTIME ) || inputScope.equals( Artifact.SCOPE_TEST ) ) )
        {
            return inputScope;
        }

        if ( currentRequiredDependencyResolutionScope.equals( Artifact.SCOPE_RUNTIME ) && inputScope.equals(  Artifact.SCOPE_TEST ) )
        {
            return inputScope;
        }        
        
        // Nothing changed we return what we were
        //
        return currentRequiredDependencyResolutionScope;
    }
    
    private String executionDescription( MojoExecution me, MavenProject project )
    {
        PluginDescriptor pd = me.getMojoDescriptor().getPluginDescriptor();
        StringBuffer sb = new StringBuffer();
        sb.append( "Executing " + pd.getArtifactId() + "[" + pd.getVersion() + "]: " + me.getMojoDescriptor().getGoal() + " on " + project.getArtifactId() );        
        return sb.toString();
    }
        
    private void populateMojoExecutionConfiguration( MavenProject project, MojoExecution mojoExecution, boolean directInvocation )
    {
        String g = mojoExecution.getGroupId();

        String a = mojoExecution.getArtifactId();

        Plugin plugin = project.getPlugin( g + ":" + a );

        if ( plugin != null )
        {
            for ( PluginExecution e : plugin.getExecutions() )
            {
                if ( mojoExecution.getExecutionId().equals( e.getId() ) )
                {
                    Xpp3Dom executionConfiguration = (Xpp3Dom) e.getConfiguration();

                    Xpp3Dom mojoConfiguration = extractMojoConfiguration( executionConfiguration, mojoExecution.getMojoDescriptor() );

                    mojoExecution.setConfiguration( mojoConfiguration );

                    return;
                }
            }
        }

        if ( directInvocation )
        {
            Xpp3Dom defaultDom = convert( mojoExecution.getMojoDescriptor() );

            if ( plugin != null && plugin.getConfiguration() != null )
            {
                Xpp3Dom projectDom = (Xpp3Dom) plugin.getConfiguration();
                projectDom = extractMojoConfiguration( projectDom, mojoExecution.getMojoDescriptor() );
                mojoExecution.setConfiguration( Xpp3Dom.mergeXpp3Dom( projectDom, defaultDom, Boolean.TRUE ) );
            }
            else
            {
                mojoExecution.setConfiguration( defaultDom );
            }
        }
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
        Xpp3Dom mojoConfiguration = new Xpp3Dom( executionConfiguration.getName() );

        Collection<String> mojoParameters = mojoDescriptor.getParameterMap().keySet();

        Map<String, String> aliases = new HashMap<String, String>();
        if ( mojoDescriptor.getParameters() != null )
        {
            for ( Parameter parameter : mojoDescriptor.getParameters() )
            {
                String alias = parameter.getAlias();
                if ( StringUtils.isNotEmpty( alias ) )
                {
                    aliases.put( alias, parameter.getName() );
                }
            }
        }

        for ( int i = 0; i < executionConfiguration.getChildCount(); i++ )
        {
            Xpp3Dom executionDom = executionConfiguration.getChild( i );
            String paramName = executionDom.getName();

            if ( mojoParameters.contains( paramName ) )
            {
                Xpp3Dom mojoDom = new Xpp3Dom( executionDom );
                mojoConfiguration.addChild( mojoDom );
            }
            else if ( aliases.containsKey( paramName ) )
            {
                Xpp3Dom mojoDom = new Xpp3Dom( aliases.get( paramName ) );
                mojoDom.setValue( executionDom.getValue() );

                for ( String attributeName : executionDom.getAttributeNames() )
                {
                    mojoDom.setAttribute( attributeName, executionDom.getAttribute( attributeName ) );
                }

                for ( Xpp3Dom child : executionDom.getChildren() )
                {
                    mojoDom.addChild( new Xpp3Dom( child ) );
                }

                mojoConfiguration.addChild( mojoDom );
            }
        }

        return mojoConfiguration;
    }
   
    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    MojoDescriptor getMojoDescriptor( String task, MavenSession session ) 
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException
    {        
        MavenProject project = session.getCurrentProject();
        
        String goal = null;
        
        Plugin plugin = null;

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
            
            plugin = findPluginForPrefix( prefix, session );
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
                     
        if ( plugin.getVersion() == null )
        {
            // We need to get it from the POM first before anything else
            //
            for ( Plugin pluginInPom : project.getBuildPlugins() )
            {
                if ( pluginInPom.getArtifactId().equals( plugin.getArtifactId() ) )
                {
                    plugin.setVersion( pluginInPom.getVersion() );
                    break;
                }
            }

            if ( plugin.getVersion() == null && project.getPluginManagement() != null )
            {
                for ( Plugin pluginInPom : project.getPluginManagement().getPlugins() )
                {
                    if ( pluginInPom.getArtifactId().equals( plugin.getArtifactId() ) )
                    {
                        plugin.setVersion( pluginInPom.getVersion() );
                        break;
                    }
                }
            }
            
            // If there is no version to be found then we need to look in the repository metadata for
            // this plugin and see what's specified as the latest release.
            //
            if ( plugin.getVersion() == null )
            {
                for ( ArtifactRepository repository : session.getCurrentProject().getRemoteArtifactRepositories() )
                {
                    String localPath = plugin.getGroupId().replace( '.', '/' ) + "/" + plugin.getArtifactId() + "/maven-metadata-" + repository.getId() + ".xml";

                    File destination = new File( session.getLocalRepository().getBasedir(), localPath );

                    if ( !destination.exists() )
                    {
                        try
                        {
                            String remotePath = plugin.getGroupId().replace( '.', '/' ) + "/" + plugin.getArtifactId() + "/maven-metadata.xml";

                            repositorySystem.retrieve( repository, destination, remotePath, session.getRequest().getTransferListener() );
                        }
                        catch ( TransferFailedException e )
                        {
                            continue;
                        }
                        catch ( ResourceDoesNotExistException e )
                        {
                            continue;
                        }
                    }

                    // We have retrieved the metadata
                    try
                    {
                        Metadata pluginMetadata = readMetadata( destination );

                        String release = pluginMetadata.getVersioning().getRelease();

                        if ( release != null )
                        {
                            plugin.setVersion( release );
                        }
                    }
                    catch ( RepositoryMetadataReadException e )
                    {
                        logger.warn( "Error reading plugin metadata: ", e );
                    }
                }
            }
        }        
        
        return pluginManager.getMojoDescriptor( plugin, goal, session.getLocalRepository(), project.getRemoteArtifactRepositories() );
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
                // compile -> org.apache.maven.plugins:maven-compiler-plugin:compile[,gid:aid:goal,...]
                //
                for ( Map.Entry<String, String> goalsForLifecyclePhase : lifecyclePhasesForPackaging.entrySet() )
                {
                    String phase = goalsForLifecyclePhase.getKey();
                    String goals = goalsForLifecyclePhase.getValue();
                    parseLifecyclePhaseDefinitions( plugins, phase, goals );
                }
            }
            else if ( lifecycle.getDefaultPhases() != null )
            {
                for ( String goals : lifecycle.getDefaultPhases() )
                {
                    parseLifecyclePhaseDefinitions( plugins, null, goals );
                }
            }        
        }

        return plugins.keySet();
    }        

    private void parseLifecyclePhaseDefinitions( Map<Plugin, Plugin> plugins, String phase, String goals )
    {
        for ( StringTokenizer tok = new StringTokenizer( goals, "," ); tok.hasMoreTokens(); )
        {
            String goal = tok.nextToken().trim();
            String[] p = StringUtils.split( goal, ":" );

            PluginExecution execution = new PluginExecution();
            // FIXME: Find a better execution id
            execution.setId( "default-" + p[2] );
            execution.setPhase( phase );
            execution.getGoals().add( p[2] );

            Plugin plugin = new Plugin();
            plugin.setGroupId( p[0] );
            plugin.setArtifactId( p[1] );

            Plugin existing = plugins.get( plugin );
            if ( existing != null )
            {
                plugin = existing;
            }
            else
            {
                plugins.put( plugin, plugin );
            }

            plugin.getExecutions().add( execution );
        }
    }
    
    private void populateDefaultConfigurationForPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories ) 
        throws LifecycleExecutionException
    {
        for( PluginExecution pluginExecution : plugin.getExecutions() )
        {
            for( String goal : pluginExecution.getGoals() )
            {
                Xpp3Dom dom = getDefaultPluginConfiguration( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), goal, localRepository, remoteRepositories );
                pluginExecution.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) pluginExecution.getConfiguration(), dom, Boolean.TRUE ) );
            }
        }
    }
    
    public void populateDefaultConfigurationForPlugins( Collection<Plugin> plugins, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories ) 
        throws LifecycleExecutionException
    {
        for( Plugin plugin : plugins )
        {            
            populateDefaultConfigurationForPlugin( plugin, localRepository, remoteRepositories );
        }
    }    
    
    private Xpp3Dom getDefaultPluginConfiguration( String groupId, String artifactId, String version, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories ) 
        throws LifecycleExecutionException
    {
        MojoDescriptor mojoDescriptor;
        
        try
        {
            mojoDescriptor = pluginManager.getMojoDescriptor( groupId, artifactId, version, goal, localRepository, remoteRepositories );
        }
        catch ( PluginNotFoundException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( PluginResolutionException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( PluginDescriptorParsingException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( CycleDetectedInPluginGraphException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( MojoNotFoundException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        }
        catch ( InvalidPluginDescriptorException e )
        {
            throw new LifecycleExecutionException( "Error getting default plugin information: ", e );
        } 
        
        return convert( mojoDescriptor );
    }
    
    public Xpp3Dom getMojoConfiguration( MojoDescriptor mojoDescriptor )
    {
        return convert( mojoDescriptor );
    }
        
    Xpp3Dom convert( MojoDescriptor mojoDescriptor  )
    {
        Xpp3Dom dom = new Xpp3Dom( "configuration" );

        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();

        PlexusConfiguration[] ces = c.getChildren();

        if ( ces != null )
        {
            for ( PlexusConfiguration ce : ces )
            {
                String value = ce.getValue( null );
                String defaultValue = ce.getAttribute( "default-value", null );
                if ( value != null || defaultValue != null )
                {
                    Xpp3Dom e = new Xpp3Dom( ce.getName() );
                    e.setValue( value );
                    if ( defaultValue != null )
                    {
                        e.setAttribute( "default-value", defaultValue );
                    }
                    dom.addChild( e );
                }
            }
        }

        return dom;
    }
                   
    private Map<String,Plugin> pluginPrefixes = new HashMap<String,Plugin>();
    
    //TODO: take repo mans into account as one may be aggregating prefixes of many
    //TODO: collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    //      or the user forces the issue
    public Plugin findPluginForPrefix( String prefix, MavenSession session )
        throws NoPluginFoundForPrefixException
    {
        // [prefix]:[goal]
        
        Plugin plugin = pluginPrefixes.get( prefix );
        
        if ( plugin != null )
        {
            return plugin;
        }
                        
        for ( ArtifactRepository repository : session.getCurrentProject().getRemoteArtifactRepositories() )
        {            
            for ( String pluginGroup : session.getPluginGroups() )
            {
                // org.apache.maven.plugins
                // org/apache/maven/plugins/maven-metadata.xml
                
                String localPath = pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata-" + repository.getId() + ".xml";
                                
                File destination = new File( session.getLocalRepository().getBasedir(), localPath );                
                                
                if ( !destination.exists() )
                {
                    try
                    {                        
                        String remotePath = pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata.xml";
                        
                        repositorySystem.retrieve( repository, destination, remotePath, session.getRequest().getTransferListener() );
                    }
                    catch ( TransferFailedException e )
                    {
                        continue;
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        continue;
                    }
                }

                // We have retrieved the metadata
                try
                {                    
                    Metadata pluginGroupMetadata = readMetadata( destination );
                    
                    List<org.apache.maven.artifact.repository.metadata.Plugin> plugins = pluginGroupMetadata.getPlugins();
                                        
                    if ( plugins != null )
                    {
                        for ( org.apache.maven.artifact.repository.metadata.Plugin metadataPlugin : plugins )
                        {
                            Plugin p = new Plugin();
                            p.setGroupId(  pluginGroup );
                            p.setArtifactId( metadataPlugin.getArtifactId() );                            
                            pluginPrefixes.put( metadataPlugin.getPrefix(), p );
                        }
                    }                    
                }
                catch ( RepositoryMetadataReadException e )
                {
                    logger.warn( "Error reading plugin group metadata: ", e );
                }
            }            
        }
                    
        plugin = pluginPrefixes.get( prefix );
        
        if ( plugin != null )
        {
            return plugin;
        }
        
        throw new NoPluginFoundForPrefixException( prefix );
    }  
    
    protected Metadata readMetadata( File mappingFile )
        throws RepositoryMetadataReadException
    {
        Metadata result;

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( reader, false );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "'", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }
        return result;
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
