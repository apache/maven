package org.apache.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Repository;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.settings.MavenSettings;
import org.apache.maven.settings.MavenSettingsBuilder;
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
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

    protected PlexusContainer container;

    protected PluginDescriptorBuilder pluginDescriptorBuilder;

    protected List remotePluginRepositories;

    protected ArtifactFilter artifactFilter;

    protected PathTranslator pathTranslator;

    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    protected MavenSettingsBuilder mavenSettingsBuilder;

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

    public PluginDescriptor getPluginDescriptor( String groupId, String artifactId )
    {
        return (PluginDescriptor) pluginDescriptors.get( constructPluginKey( groupId, artifactId ) );
    }

    private static String constructPluginKey( String groupId, String artifactId )
    {
        return groupId + ":" + artifactId;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Set pluginsInProcess = new HashSet();

    public void processPluginDescriptor( MavenPluginDescriptor mavenPluginDescriptor )
        throws CycleDetectedException
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

            String key = constructPluginKey( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId() );
            pluginDescriptors.put( key, pluginDescriptor );
        }
    }

    // ----------------------------------------------------------------------
    // Plugin discovery
    // ----------------------------------------------------------------------

    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( !( componentSetDescriptor instanceof MavenPluginDescriptor ) )
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

    public boolean isPluginInstalled( String groupId, String artifactId )
    {
        return pluginDescriptors.containsKey( constructPluginKey( groupId, artifactId ) );
    }

    private static String getPluginId( String goalName )
    {
        String pluginId = goalName;

        if ( pluginId.indexOf( ":" ) > 0 )
        {
            pluginId = pluginId.substring( 0, pluginId.indexOf( ":" ) );
        }

        return "maven-" + pluginId + "-plugin";
    }

    // TODO: don't throw Exception
    public void verifyPluginForGoal( String goalName, MavenSession session )
        throws Exception
    {
        String pluginId = getPluginId( goalName );

        // TODO: hardcoding of group ID/artifact ID
        verifyPlugin( "maven", pluginId, session );
    }

    // TODO: don't throw Exception
    public void verifyPlugin( String groupId, String artifactId, MavenSession session )
        throws Exception
    {
        if ( !isPluginInstalled( groupId, artifactId ) )
        {
            ArtifactFactory artifactFactory = null;
            try
            {
                MavenProject project = session.getProject();

                org.apache.maven.model.Plugin pluginConfig = null;

                for ( Iterator it = project.getPlugins().iterator(); it.hasNext(); )
                {
                    org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) it.next();

                    if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                    {
                        pluginConfig = plugin;

                        break;
                    }
                }

                String version = null;

                if ( pluginConfig != null )
                {
                    if ( StringUtils.isEmpty( pluginConfig.getVersion() ) )
                    {
                        throw new PluginVersionNotConfiguredException( groupId, artifactId );
                    }
                    else
                    {
                        version = pluginConfig.getVersion();
                    }
                }

                // TODO: Default over to a sensible value (is 1.0-SNAPSHOT right?) Hardcoging of group ID also
                if ( StringUtils.isEmpty( version ) )
                {
                    version = "1.0-SNAPSHOT";
                }

                artifactFactory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );

                // TODO: more hard coding here...
                Artifact pluginArtifact = artifactFactory.createArtifact( "maven", artifactId, version, null,
                                                                          "maven-plugin", "jar", null );

                addPlugin( pluginArtifact, session );
            }
            finally
            {
                if ( artifactFactory != null )
                {
                    container.release( artifactFactory );
                }
            }
        }
    }

    // TODO: don't throw Exception
    protected void addPlugin( Artifact pluginArtifact, MavenSession session )
        throws Exception
    {
        ArtifactResolver artifactResolver = null;
        MavenProjectBuilder mavenProjectBuilder = null;

        try
        {
            artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

            MavenMetadataSource metadataSource = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

            ( (ArtifactEnabledContainer) container ).addComponent( pluginArtifact, artifactResolver,
                                                                   remotePluginRepositories,
                                                                   session.getLocalRepository(), metadataSource,
                                                                   artifactFilter );
        }
        finally
        {
            // TODO: watch out for the exceptions being thrown
            if ( artifactResolver != null )
            {
                container.release( artifactResolver );
            }
            if ( mavenProjectBuilder != null )
            {
                container.release( mavenProjectBuilder );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Plugin execution
    // ----------------------------------------------------------------------

    public void executeMojo( MavenSession session, String goalName )
        throws PluginExecutionException
    {
        try
        {
            verifyPluginForGoal( goalName, session );
        }
        catch ( Exception e )
        {
            throw new PluginExecutionException( "Unable to execute goal: " + goalName, e );
        }

        PluginExecutionRequest request = null;

        MojoDescriptor mojoDescriptor = getMojoDescriptor( goalName );
        if ( mojoDescriptor == null )
        {
            throw new PluginExecutionException( "Unable to find goal: " + goalName );
        }

        try
        {
            if ( mojoDescriptor.requiresDependencyResolution() )
            {

                ArtifactResolver artifactResolver = null;
                MavenProjectBuilder mavenProjectBuilder = null;

                try
                {
                    artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
                    mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

                    resolveTransitiveDependencies( session, artifactResolver, mavenProjectBuilder );
                    downloadDependencies( session, artifactResolver );
                }
                finally
                {
                    // TODO: watch out for the exceptions being thrown
                    if ( artifactResolver != null )
                    {
                        container.release( artifactResolver );
                    }
                    if ( mavenProjectBuilder != null )
                    {
                        container.release( mavenProjectBuilder );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new PluginExecutionException( "Unable to resolve required dependencies for goal", e );
        }

        Plugin plugin = null;

        try
        {
            plugin = (Plugin) container.lookup( Plugin.ROLE, goalName );

            plugin.setLog( session.getLog() );

            // TODO: remove
            boolean newMojoTechnique = false;
            try
            {
                plugin.getClass().getDeclaredMethod( "execute", new Class[0] );
                newMojoTechnique = true;
            }
            catch ( NoSuchMethodException e )
            {
                // intentionally ignored
            }

            if ( newMojoTechnique )
            {
                populateParameters( plugin, mojoDescriptor, session );
            }
            else
            {
                request = new PluginExecutionRequest( createParameters( mojoDescriptor, session ) );
            }

            // !! This is ripe for refactoring to an aspect.
            // Event monitoring.
            String event = MavenEvents.MOJO_EXECUTION;
            EventDispatcher dispatcher = session.getEventDispatcher();

            dispatcher.dispatchStart( event, goalName );
            try
            {
                if ( newMojoTechnique )
                {
                    plugin.execute();
                }
                else
                {
                    plugin.execute( request );
                }

                dispatcher.dispatchEnd( event, goalName );
            }
            catch ( PluginExecutionException e )
            {
                session.getEventDispatcher().dispatchError( event, goalName, e );
                throw e;
            }
            // End event monitoring.

        }
        catch ( PluginConfigurationException e )
        {
            throw new PluginExecutionException( "Error configuring plugin for execution.", e );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginExecutionException( "Error looking up plugin: ", e );
        }
        finally
        {
            try
            {
                releaseComponents( mojoDescriptor, request );

                container.release( plugin );
            }
            catch ( Exception e )
            {
                // TODO: better error handling, needed!
                e.printStackTrace();
            }
        }
    }

    // TODO: don't throw Exception
    private void releaseComponents( MojoDescriptor goal, PluginExecutionRequest request )
        throws Exception
    {
        if ( request != null && request.getParameters() != null )
        {
            for ( Iterator iterator = goal.getParameters().iterator(); iterator.hasNext(); )
            {
                Parameter parameter = (Parameter) iterator.next();

                String key = parameter.getName();

                String expression = parameter.getExpression();

                if ( expression != null && expression.startsWith( "#component" ) )
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

    private void populateParameters( Plugin plugin, MojoDescriptor mojoDescriptor, MavenSession session )
        throws PluginConfigurationException
    {
        // TODO: merge eventually, just to avoid reuse
        // TODO: probably want to use the plexus component configurator... then do the additional processing in
        //  createParameters afterwards. Not sure how we might find files that are nested in other objects... perhaps
        //  we add a "needs translation" to the mojo so such types can be translated (implementing some interface) and
        //  address their own file objects
        Map values = createParameters( mojoDescriptor, session );

        List parameters = mojoDescriptor.getParameters();

        for ( Iterator i = parameters.iterator(); i.hasNext(); )
        {
            Parameter param = (Parameter) i.next();
            String name = param.getName();
            Object value = values.get( name );

            Class clazz = plugin.getClass();
            try
            {
                Field f = clazz.getDeclaredField( name );
                boolean accessible = f.isAccessible();
                if ( !accessible )
                {
                    f.setAccessible( true );
                }

                f.set( plugin, value );

                if ( !accessible )
                {
                    f.setAccessible( false );
                }
            }
            catch ( NoSuchFieldException e )
            {
                throw new PluginConfigurationException( "Unable to set field '" + name + "' on '" + clazz + "'" );
            }
            catch ( IllegalAccessException e )
            {
                throw new PluginConfigurationException( "Unable to set field '" + name + "' on '" + clazz + "'" );
            }
        }
    }

    public Map createParameters( MojoDescriptor goal, MavenSession session )
        throws PluginConfigurationException
    {
        List parameters = goal.getParameters();

        Map map = new HashMap();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            String key = parameter.getName();

            String expression = parameter.getExpression();

            Object value = PluginParameterExpressionEvaluator.evaluate( expression, session );

            getLogger().debug( "Evaluated mojo parameter expression: \'" + expression + "\' to: " + value );

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

            String type = parameter.getType();

            if ( type != null && ( type.equals( "File" ) || type.equals( "java.io.File" ) ) )
            {
                value = pathTranslator.alignToBaseDirectory( (String) value,
                                                             session.getProject().getFile().getParentFile() );
                map.put( key, value );
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
            String pluginId = getPluginId( goalId );

            for ( Iterator iterator = project.getPlugins().iterator(); iterator.hasNext(); )
            {
                org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) iterator.next();

                // TODO: groupID not handled
                if ( pluginId.equals( plugin.getArtifactId() ) )
                {
                    map = CollectionUtils.mergeMaps( plugin.getConfiguration(), map );

                    // TODO: much less of this magic is needed - make the mojoDescriptor just store the first and second part
                    int index = goalId.indexOf( ':' );
                    if ( index >= 0 )
                    {
                        String goalName = goalId.substring( index + 1 );
                        for ( Iterator j = plugin.getGoals().iterator(); j.hasNext(); )
                        {
                            Goal goal = (Goal) j.next();
                            if ( goal.getId().equals( goalName ) )
                            {
                                map = CollectionUtils.mergeMaps( goal.getConfiguration(), map );
                                break;
                            }
                        }
                    }

                    return map;
                }
            }
        }

        return map;
    }

    public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter )
    {
        StringBuffer message = new StringBuffer();

        message.append( "The '" + parameter.getName() ).append( "' parameter is required for the execution of the " ).append(
            mojo.getId() ).append( " mojo and cannot be null." );

        return message.toString();
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void initialize()
    {
        // TODO: configure this from bootstrap or scan lib
        artifactFilter = new ExclusionSetFilter( new String[]{"maven-core", "maven-artifact", "maven-model",
                                                              "maven-settings", "maven-monitor", "maven-plugin",
                                                              "plexus-container-api", "plexus-container-default",
                                                              "plexus-artifact-container", "wagon-provider-api",
                                                              "classworlds"} );

        // TODO: move this to be configurable from the Maven component
        remotePluginRepositories = new ArrayList();

        // TODO: needs to be configured from the POM element

        MavenSettings settings = null;
        try
        {
            settings = mavenSettingsBuilder.buildSettings();
        }
        catch ( Exception e )
        {
            getLogger().error( "Failed to build MavenSettings from xml file. Using defaults.", e );
            settings = new MavenSettings();
        }

        Repository pluginRepo = new Repository();
        pluginRepo.setId( "plugin-repository" );
        pluginRepo.setUrl( "http://repo1.maven.org" );

        ArtifactRepository pluginRepository = artifactRepositoryFactory.createArtifactRepository( pluginRepo, settings );

        remotePluginRepositories.add( pluginRepository );
    }

    // ----------------------------------------------------------------------
    // Artifact resolution
    // ----------------------------------------------------------------------

    private void resolveTransitiveDependencies( MavenSession context, ArtifactResolver artifactResolver,
                                                MavenProjectBuilder mavenProjectBuilder )
        throws ArtifactResolutionException
    {
        MavenProject project = context.getProject();

        MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                context.getRemoteRepositories(),
                                                                                context.getLocalRepository(),
                                                                                sourceReader );

        project.addArtifacts( result.getArtifacts().values() );
    }

    // ----------------------------------------------------------------------
    // Artifact downloading
    // ----------------------------------------------------------------------

    private void downloadDependencies( MavenSession context, ArtifactResolver artifactResolver )
        throws ArtifactResolutionException
    {
        for ( Iterator it = context.getProject().getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            artifactResolver.resolve( artifact, context.getRemoteRepositories(), context.getLocalRepository() );
        }
    }

}

