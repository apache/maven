package org.apache.maven.plugin;

/*
 * ====================================================================
 * Copyright 2001-2004 The Apache Software Foundation. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.GoalExecutionException;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.ArtifactEnabledContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.CollectionUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultPluginManager
    extends AbstractLogEnabled
    implements PluginManager, ComponentDiscoveryListener, Initializable, Contextualizable
{
    static String MAVEN_PLUGIN = "maven-plugin";

    protected Map mojoDescriptors;

    protected Map pluginDescriptors;

    protected ArtifactResolver artifactResolver;

    protected ArtifactHandlerManager artifactHandlerManager;

    protected PlexusContainer container;

    protected PluginDescriptorBuilder pluginDescriptorBuilder;

    protected Set remotePluginRepositories;

    protected ArtifactFilter artifactFilter;

    protected MavenProjectBuilder mavenProjectBuilder;

    public DefaultPluginManager()
    {
        mojoDescriptors = new HashMap();

        pluginDescriptors = new HashMap();

        pluginDescriptorBuilder = new PluginDescriptorBuilder();
    }

    // ----------------------------------------------------------------------
    // Goal descriptors
    // ----------------------------------------------------------------------

    public Map getMojoDescriptors()
    {
        return mojoDescriptors;
    }

    /**
     * Mojo descriptors are looked up using their id which is of the form
     * <pluginId>: <mojoId>. So this might be archetype:create for example which
     * is the create mojo that resides in the archetype plugin.
     * 
     * @param name
     * @return
     */
    public MojoDescriptor getMojoDescriptor( String name )
    {
        return (MojoDescriptor) mojoDescriptors.get( name );
    }

    public PluginDescriptor getPluginDescriptor( String pluginId )
    {
        return (PluginDescriptor) pluginDescriptors.get( pluginId );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Set pluginsInProcess = new HashSet();

    public void processPluginDescriptor( MavenPluginDescriptor mavenPluginDescriptor ) throws CycleDetectedException
    {
        if ( pluginsInProcess.contains( mavenPluginDescriptor.getPluginId() ) )
        {
            return;
        }

        pluginsInProcess.add( mavenPluginDescriptor.getPluginId() );

        PluginDescriptor pluginDescriptor = mavenPluginDescriptor.getPluginDescriptor();

        for ( Iterator it = mavenPluginDescriptor.getMavenMojoDescriptors().iterator(); it.hasNext(); )
        {
            MavenMojoDescriptor mavenMojoDescriptor = (MavenMojoDescriptor) it.next();

            MojoDescriptor mojoDescriptor = mavenMojoDescriptor.getMojoDescriptor();

            mojoDescriptors.put( mojoDescriptor.getId(), mojoDescriptor );

            pluginDescriptors.put( pluginDescriptor.getId(), pluginDescriptor );
        }
    }

    // ----------------------------------------------------------------------
    // Plugin discovery
    // ----------------------------------------------------------------------

    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( !(componentSetDescriptor instanceof MavenPluginDescriptor) )
        {
            return;
        }

        MavenPluginDescriptor pluginDescriptor = (MavenPluginDescriptor) componentSetDescriptor;

        try
        {
            processPluginDescriptor( pluginDescriptor );
        }
        catch ( CycleDetectedException e )
        {
            getLogger().error( "A cycle was detected in the goal graph: ", e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public boolean isPluginInstalled( String pluginId )
    {
        return pluginDescriptors.containsKey( pluginId );
    }

    private String getPluginId( String goalName )
    {
        if ( goalName.indexOf( ":" ) > 0 )
        {
            return goalName.substring( 0, goalName.indexOf( ":" ) );
        }

        return goalName;
    }

    // TODO: don't throw Exception
    public void verifyPluginForGoal( String goalName, MavenSession session ) throws Exception
    {
        String pluginId = getPluginId( goalName );

        verifyPlugin( pluginId, session );
    }

    // TODO: don't throw Exception
    public void verifyPlugin( String pluginId, MavenSession session ) throws Exception
    {
        if ( !isPluginInstalled( pluginId ) )
        {
            //!! This is entirely crappy. We need a better naming for plugin
            // artifact ids and
            //   we definitely need better version extraction support.

            String artifactId = "maven-" + pluginId + "-plugin";

            String version = "1.0-SNAPSHOT";

            Artifact pluginArtifact = new DefaultArtifact( "maven", artifactId, version, "plugin", "jar" );

            addPlugin( pluginArtifact, session );
        }
    }

    // TODO: don't throw Exception
    protected void addPlugin( Artifact pluginArtifact, MavenSession session ) throws Exception
    {
        // TODO: these should be configured, not instantiated here

        artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

        mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

        MavenMetadataSource metadataSource = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

        ((ArtifactEnabledContainer) container).addComponent( pluginArtifact,
                                                             artifactResolver,
                                                             remotePluginRepositories,
                                                             session.getLocalRepository(),
                                                             metadataSource,
                                                             artifactFilter );
    }

    // ----------------------------------------------------------------------
    // Plugin execution
    // ----------------------------------------------------------------------

    public PluginExecutionResponse executeMojo( MavenSession session, String goalName ) throws GoalExecutionException
    {
        try
        {
            verifyPluginForGoal( goalName, session );
        }
        catch ( Exception e )
        {
            throw new GoalExecutionException( "Unable to execute goal: " + goalName, e );
        }

        PluginExecutionRequest request;

        PluginExecutionResponse response;

        MojoDescriptor mojoDescriptor = getMojoDescriptor( goalName );
        if ( mojoDescriptor == null )
        {
            throw new GoalExecutionException( "Unable to find goal: " + goalName );
        }

        try
        {
            if ( mojoDescriptor.requiresDependencyResolution() )
            {
                resolveTransitiveDependencies( session );

                downloadDependencies( session );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new GoalExecutionException( "Unable to resolve required dependencies for goal", e );
        }

        try
        {
            //            getLogger().info( "[" + mojoDescriptor.getId() + "]" );

            request = new PluginExecutionRequest( DefaultPluginManager.createParameters( mojoDescriptor, session ) );

            request.setLog( session.getLog() );
        }
        catch ( PluginConfigurationException e )
        {
            throw new GoalExecutionException( "Error configuring plugin for execution.", e );
        }

        response = new PluginExecutionResponse();

        Plugin plugin = null;

        try
        {
            plugin = (Plugin) container.lookup( Plugin.ROLE, goalName );

            // !! This is ripe for refactoring to an aspect.
            // Event monitoring.
            String event = MavenEvents.MOJO_EXECUTION;
            EventDispatcher dispatcher = session.getEventDispatcher();

            dispatcher.dispatchStart( event, goalName );
            try
            {
                plugin.execute( request, response );

                dispatcher.dispatchEnd( event, goalName );
            }
            catch ( Exception e )
            {
                session.getEventDispatcher().dispatchError( event, goalName, e );
                throw e;
            }
            // End event monitoring.

            releaseComponents( mojoDescriptor, request );

            container.release( plugin );
        }
        catch ( ComponentLookupException e )
        {
            throw new GoalExecutionException( "Error looking up plugin: ", e );
        }
        catch ( Exception e )
        {
            throw new GoalExecutionException( "Error executing plugin: ", e );
        }

        return response;
    }

    // TODO: don't throw Exception
    private void releaseComponents( MojoDescriptor goal, PluginExecutionRequest request ) throws Exception
    {
        if ( request != null && request.getParameters() != null )
        {
            for ( Iterator iterator = goal.getParameters().iterator(); iterator.hasNext(); )
            {
                Parameter parameter = (Parameter) iterator.next();

                String key = parameter.getName();

                String expression = parameter.getExpression();

                if ( expression != null & expression.startsWith( "#component" ) )
                {
                    Object component = request.getParameter( key );

                    container.release( component );
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Mojo Parameter Handling
    // ----------------------------------------------------------------------

    public static Map createParameters( MojoDescriptor goal, MavenSession session ) throws PluginConfigurationException
    {
        Map map = null;

        List parameters = goal.getParameters();

        if ( parameters != null )
        {
            map = new HashMap();

            for ( int i = 0; i < parameters.size(); i++ )
            {
                Parameter parameter = (Parameter) parameters.get( i );

                String key = parameter.getName();

                String expression = parameter.getExpression();

                Object value = PluginParameterExpressionEvaluator.evaluate( expression, session );

                if ( value == null )
                {
                    if ( parameter.getDefaultValue() != null )
                    {
                        value = PluginParameterExpressionEvaluator.evaluate( parameter.getDefaultValue(), session );
                    }
                }

                map.put( key, value );
            }

            if ( session.getProject() != null )
            {
                map = mergeProjectDefinedPluginConfiguration( session.getProject(), goal.getId(), map );
            }
        }

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            String key = parameter.getName();

            Object value = map.get( key );

            // ----------------------------------------------------------------------
            // We will perform a basic check here for parameters values that are
            // required. Required parameters can't be null so we throw an
            // Exception in the case where they are. We probably want some
            // pluggable
            // mechanism here but this will catch the most obvious of
            // misconfigurations.
            // ----------------------------------------------------------------------

            if ( value == null && parameter.isRequired() )
            {
                throw new PluginConfigurationException( createPluginParameterRequiredMessage( goal, parameter ) );
            }
        }

        return map;
    }

    public static Map mergeProjectDefinedPluginConfiguration( MavenProject project, String goalId, Map map )
    {
        // ----------------------------------------------------------------------
        // I would like to be able to lookup the Plugin object using a key but
        // we have a limitation in modello that will be remedied shortly. So
        // for now I have to iterate through and see what we have.
        // ----------------------------------------------------------------------

        if ( project.getPlugins() != null )
        {
            String pluginId = goalId.substring( 0, goalId.indexOf( ":" ) );

            for ( Iterator iterator = project.getPlugins().iterator(); iterator.hasNext(); )
            {
                org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) iterator.next();

                if ( pluginId.equals( plugin.getId() ) )
                {
                    return CollectionUtils.mergeMaps( plugin.getConfiguration(), map );
                }
            }
        }

        return map;
    }

    public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter )
    {
        StringBuffer message = new StringBuffer();

        message.append( "The '" + parameter.getName() )
               .append( "' parameter is required for the execution of the " )
               .append( mojo.getId() )
               .append( " mojo and cannot be null." );

        return message.toString();
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void contextualize( Context context ) throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void initialize()
    {
        // TODO: configure this from bootstrap or scan lib
        artifactFilter = new ExclusionSetFilter( new String[] {
            "maven-core",
            "maven-artifact",
            "maven-model",
            "maven-monitor",
            "maven-plugin",
            "plexus-container-api",
            "plexus-container-default",
            "plexus-artifact-container",
            "wagon-provider-api",
            "classworlds" } );

        // TODO: move this to be configurable from the Maven component
        remotePluginRepositories = new HashSet();

        // TODO: needs to be configured from the POM element
        remotePluginRepositories.add( new ArtifactRepository( "plugin-repository", "http://repo1.maven.org" ) );
    }

    // ----------------------------------------------------------------------
    // Artifact resolution
    // ----------------------------------------------------------------------

    private void resolveTransitiveDependencies( MavenSession context ) throws ArtifactResolutionException
    {
        MavenProject project = context.getProject();

        MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                context.getRemoteRepositories(),
                                                                                context.getLocalRepository(),
                                                                                sourceReader );

        project.getArtifacts().addAll( result.getArtifacts().values() );
    }

    // ----------------------------------------------------------------------
    // Artifact downloading
    // ----------------------------------------------------------------------

    private void downloadDependencies( MavenSession context ) throws GoalExecutionException
    {
        try
        {
            for ( Iterator it = context.getProject().getArtifacts().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                artifactResolver.resolve( artifact, context.getRemoteRepositories(), context.getLocalRepository() );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new GoalExecutionException( "Can't resolve artifact: ", e );
        }
    }

}

