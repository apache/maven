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
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.codehaus.plexus.ArtifactEnabledContainer;
import org.codehaus.plexus.ArtifactEnabledContainerException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.lang.reflect.Field;
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
    // TODO: share with type handler
    static String MAVEN_PLUGIN = "maven-plugin";

    protected Map mojoDescriptors;

    protected Map pluginDescriptors;

    protected PlexusContainer container;

    protected PluginDescriptorBuilder pluginDescriptorBuilder;

    protected ArtifactFilter artifactFilter;

    protected PathTranslator pathTranslator;

    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    protected MavenSettingsBuilder mavenSettingsBuilder;

    protected ComponentConfigurator configurator;

    private ArtifactFactory artifactFactory;

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
        return (PluginDescriptor) pluginDescriptors.get( PluginDescriptor.constructPluginKey( groupId, artifactId ) );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Set pluginsInProcess = new HashSet();

    public void processPluginDescriptor( MavenPluginDescriptor mavenPluginDescriptor )
        throws CycleDetectedException
    {
        PluginDescriptor pluginDescriptor = mavenPluginDescriptor.getPluginDescriptor();
        String key = pluginDescriptor.getId();

        if ( pluginsInProcess.contains( key ) )
        {
            return;
        }

        pluginsInProcess.add( key );

        for ( Iterator it = mavenPluginDescriptor.getMavenMojoDescriptors().iterator(); it.hasNext(); )
        {
            MavenMojoDescriptor mavenMojoDescriptor = (MavenMojoDescriptor) it.next();

            MojoDescriptor mojoDescriptor = mavenMojoDescriptor.getMojoDescriptor();

            mojoDescriptors.put( mojoDescriptor.getId(), mojoDescriptor );
        }

        pluginDescriptors.put( key, pluginDescriptor );
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
        return pluginDescriptors.containsKey( PluginDescriptor.constructPluginKey( groupId, artifactId ) );
    }

    public void verifyPluginForGoal( String goalName, MavenSession session )
        throws ArtifactResolutionException, PluginManagerException
    {
        String pluginId = PluginDescriptor.getPluginIdFromGoal( goalName );

        verifyPlugin( PluginDescriptor.getDefaultPluginGroupId(), pluginId, session );
    }

    public void verifyPlugin( String groupId, String artifactId, MavenSession session )
        throws ArtifactResolutionException, PluginManagerException
    {
        if ( !isPluginInstalled( groupId, artifactId ) )
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
                    // The model/project builder should have validated this already
                    String message = "The maven plugin with groupId: '" + groupId + "' and artifactId: '" + artifactId +
                        "' which was configured for use in this project does not have a version associated with it.";
                    throw new IllegalStateException( message );
                }
                else
                {
                    version = pluginConfig.getVersion();
                }
            }

            try
            {
                Artifact pluginArtifact = artifactFactory.createArtifact( groupId, artifactId, version, null,
                                                                          MAVEN_PLUGIN, null );

                addPlugin( pluginArtifact, session );
            }
            catch ( ArtifactEnabledContainerException e )
            {
                throw new PluginManagerException( "Error occurred in the artifact container attempting to download plugin " +
                                                  groupId + ":" + artifactId, e );
            }
            catch ( ArtifactResolutionException e )
            {
                if ( groupId.equals( e.getGroupId() ) && artifactId.equals( e.getArtifactId() ) &&
                    version.equals( e.getVersion() ) && "maven-plugin".equals( e.getType() ) )
                {
                    throw new PluginNotFoundException( groupId, artifactId, version, e );
                }
                else
                {
                    throw e;
                }
            }
            catch ( ComponentLookupException e )
            {
                throw new PluginManagerException(
                    "Internal configuration error while retrieving " + groupId + ":" + artifactId, e );
            }
        }
    }

    protected void addPlugin( Artifact pluginArtifact, MavenSession session )
        throws ArtifactEnabledContainerException, ArtifactResolutionException, ComponentLookupException
    {
        ArtifactResolver artifactResolver = null;
        MavenProjectBuilder mavenProjectBuilder = null;

        try
        {
            artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

            MavenMetadataSource metadataSource = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

            ( (ArtifactEnabledContainer) container ).addComponent( pluginArtifact, artifactResolver,
                                                                   session.getPluginRepositories(),
                                                                   session.getLocalRepository(), metadataSource,
                                                                   artifactFilter );
        }
        finally
        {
            if ( artifactResolver != null )
            {
                releaseComponent( artifactResolver );
            }
            if ( mavenProjectBuilder != null )
            {
                releaseComponent( mavenProjectBuilder );
            }
        }
    }

    private void releaseComponent( Object component )
    {
        try
        {
            container.release( component );
        }
        catch ( ComponentLifecycleException e )
        {
            getLogger().error( "Error releasing component - ignoring", e );
        }
    }

    // ----------------------------------------------------------------------
    // Plugin execution
    // ----------------------------------------------------------------------

    public void executeMojo( MavenSession session, String goalName )
        throws PluginExecutionException, PluginNotFoundException, PluginManagerException, ArtifactResolutionException
    {
        verifyPluginForGoal( goalName, session );

        PluginExecutionRequest request = null;

        MojoDescriptor mojoDescriptor = getMojoDescriptor( goalName );
        if ( mojoDescriptor == null )
        {
            throw new PluginExecutionException( "Unable to find goal: " + goalName );
        }

        if ( mojoDescriptor.getRequiresDependencyResolution() != null )
        {

            ArtifactResolver artifactResolver = null;
            MavenProjectBuilder mavenProjectBuilder = null;

            try
            {
                artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
                mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

                resolveTransitiveDependencies( session, artifactResolver, mavenProjectBuilder,
                                               mojoDescriptor.getRequiresDependencyResolution() );
                downloadDependencies( session, artifactResolver );
            }
            catch ( ComponentLookupException e )
            {
                throw new PluginManagerException( "Internal configuration error in plugin manager", e );
            }
            finally
            {
                if ( artifactResolver != null )
                {
                    releaseComponent( artifactResolver );
                }
                if ( mavenProjectBuilder != null )
                {
                    releaseComponent( mavenProjectBuilder );
                }
            }
        }

        Plugin plugin = null;

        try
        {
            plugin = (Plugin) container.lookup( Plugin.ROLE, goalName );

            plugin.setLog( session.getLog() );

            // TODO: remove
            boolean newMojoTechnique = checkMojoTechnique( plugin.getClass() );

            String goalId = null;

            // TODO: much less of this magic is needed - make the mojoDescriptor just store the first and second part
            int index = goalName.indexOf( ':' );
            if ( index >= 0 )
            {
                goalId = goalName.substring( index + 1 );
            }

            // TODO: can probable refactor these a little when only the new plugin technique is in place
            Xpp3Dom dom = session.getProject().getGoalConfiguration( PluginDescriptor.getPluginIdFromGoal( goalName ),
                                                                     goalId );

            PlexusConfiguration configuration;
            if ( dom == null )
            {
                configuration = new XmlPlexusConfiguration( "configuration" );
            }
            else
            {
                configuration = new XmlPlexusConfiguration( dom );
            }

            ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, pathTranslator );

            configuration = mergeConfiguration( configuration, mojoDescriptor.getConfiguration() );

            try
            {
                if ( newMojoTechnique )
                {
                    Map map = getPluginConfigurationFromExpressions( mojoDescriptor, configuration,
                                                                     expressionEvaluator );

                    populatePluginFields( plugin, configuration, map, expressionEvaluator );
                }
                else
                {
                    getLogger().warn( "WARNING: The mojo " + mojoDescriptor.getId() + " is using the OLD API" );

                    Map map = getPluginConfigurationFromExpressions( mojoDescriptor, configuration,
                                                                     expressionEvaluator );

                    request = createPluginRequest( configuration, map );
                }
            }
            catch ( ExpressionEvaluationException e )
            {
                throw new PluginExecutionException( "Unable to configure plugin", e );
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
            String msg = "Error configuring plugin for execution of .";
            throw new PluginExecutionException( msg, e );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginExecutionException( "Error looking up plugin: ", e );
        }
        finally
        {
            releaseComponent( plugin );
        }
    }

    private PlexusConfiguration mergeConfiguration( PlexusConfiguration dominant, PlexusConfiguration configuration )
    {
        // TODO: share with mergeXpp3Dom
        PlexusConfiguration[] children = configuration.getChildren();
        for ( int i = 0; i < children.length; i++ )
        {
            PlexusConfiguration child = children[i];
            PlexusConfiguration childDom = (XmlPlexusConfiguration) dominant.getChild( child.getName(), false );
            if ( childDom != null )
            {
                mergeConfiguration( childDom, child );
            }
            else
            {
                dominant.addChild( copyConfiguration( child ) );
            }
        }
        return dominant;
    }

    public static PlexusConfiguration copyConfiguration( PlexusConfiguration src )
    {
        // TODO: shouldn't be necessary
        XmlPlexusConfiguration dom = new XmlPlexusConfiguration( src.getName() );
        dom.setValue( src.getValue( null ) );

        String[] attributeNames = src.getAttributeNames();
        for ( int i = 0; i < attributeNames.length; i++ )
        {
            String attributeName = attributeNames[i];
            dom.setAttribute( attributeName, src.getAttribute( attributeName, null ) );
        }

        PlexusConfiguration[] children = src.getChildren();
        for ( int i = 0; i < children.length; i++ )
        {
            dom.addChild( copyConfiguration( children[i] ) );
        }

        return dom;
    }

    /**
     * @deprecated
     */
    private static boolean checkMojoTechnique( Class aClass )
    {
        boolean newMojoTechnique = false;
        try
        {
            aClass.getDeclaredMethod( "execute", new Class[0] );
            newMojoTechnique = true;
        }
        catch ( NoSuchMethodException e )
        {
            // intentionally ignored

            Class superclass = aClass.getSuperclass();
            if ( superclass != AbstractPlugin.class )
            {
                return checkMojoTechnique( superclass );
            }
        }
        return newMojoTechnique;
    }

    // ----------------------------------------------------------------------
    // Mojo Parameter Handling
    // ----------------------------------------------------------------------

    /**
     * @param configuration
     * @param map
     * @return
     * @deprecated
     */
    private static PluginExecutionRequest createPluginRequest( PlexusConfiguration configuration, Map map )
    {
        Map parameters = new HashMap();
        PlexusConfiguration[] children = configuration.getChildren();
        for ( int i = 0; i < children.length; i++ )
        {
            PlexusConfiguration child = children[i];
            parameters.put( child.getName(), child.getValue( null ) );
        }
        map = CollectionUtils.mergeMaps( map, parameters );
        return new PluginExecutionRequest( map );
    }

    private void populatePluginFields( Plugin plugin, PlexusConfiguration configuration, Map map,
                                       ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        try
        {
            configurator.configureComponent( plugin, configuration, expressionEvaluator );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new PluginConfigurationException( "Unable to parse the created DOM for plugin configuration", e );
        }

        // Configuration does not store objects, so the non-String fields are configured here
        // TODO: remove - this is for plugins built with alpha-1
        for ( Iterator i = map.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            if ( configuration.getChild( key ) != null )
            {
                continue;
            }

            Object value = map.get( key );

            if ( value != null )
            {
                Class clazz = plugin.getClass();
                try
                {
                    Field f = findPluginField( clazz, key );
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
                    throw new PluginConfigurationException( "Unable to set field '" + key + "' on '" + clazz + "'", e );
                }
                catch ( IllegalAccessException e )
                {
                    throw new PluginConfigurationException( "Unable to set field '" + key + "' on '" + clazz + "'", e );
                }
            }
        }
    }

    private Field findPluginField( Class clazz, String key )
        throws NoSuchFieldException
    {
        try
        {
            return clazz.getDeclaredField( key );
        }
        catch ( NoSuchFieldException e )
        {
            Class superclass = clazz.getSuperclass();
            if ( superclass != Object.class )
            {
                return findPluginField( superclass, key );
            }
            else
            {
                throw e;
            }
        }
    }

    /**
     * @deprecated
     */
    private Map getPluginConfigurationFromExpressions( MojoDescriptor goal, PlexusConfiguration configuration,
                                                       ExpressionEvaluator expressionEvaluator )
        throws ExpressionEvaluationException, PluginConfigurationException
    {
        List parameters = goal.getParameters();

        Map map = new HashMap();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            String key = parameter.getName();

            String expression;
            if ( configuration.getChild( key, false ) == null )
            {
                expression = parameter.getExpression();
            }
            else
            {
                expression = configuration.getChild( key, false ).getValue( null );

                if ( expression != null && parameter.getDeprecated() != null )
                {
                    if ( !expression.equals( goal.getConfiguration().getChild( key, false ).getValue( null ) ) )
                    {
                        getLogger().warn(
                            "DEPRECATED: " + parameter.getName() + " is deprecated.\n\t" + parameter.getDeprecated() );
                    }
                }
            }

            Object value = expressionEvaluator.evaluate( expression );

            getLogger().debug( "Evaluated mojo parameter expression: \'" + expression + "\' to: " + value );

            if ( value == null )
            {
                if ( parameter.getDefaultValue() != null )
                {
                    value = expressionEvaluator.evaluate( parameter.getDefaultValue() );
                }
            }

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

            map.put( key, value );
        }
        return map;
    }

    public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter )
    {
        StringBuffer message = new StringBuffer();

        message.append( "The '" + parameter.getName() );
        message.append( "' parameter is required for the execution of the " );
        message.append( mojo.getId() );
        message.append( " mojo and cannot be null." );

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
        artifactFilter =
            new ExclusionSetFilter(
                new String[]{"maven-core", "maven-artifact", "maven-model", "maven-settings", "maven-monitor",
                             "maven-plugin-api", "maven-plugin-descriptor", "plexus-container-default",
                             "plexus-artifact-container", "wagon-provider-api", "classworlds", "maven-plugin" /* Just re-added until all plugins are switched over...*/} );
    }

    // ----------------------------------------------------------------------
    // Artifact resolution
    // ----------------------------------------------------------------------

    private void resolveTransitiveDependencies( MavenSession context, ArtifactResolver artifactResolver,
                                                MavenProjectBuilder mavenProjectBuilder, String scope )
        throws ArtifactResolutionException
    {
        MavenProject project = context.getProject();

        MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

        ArtifactFilter filter = new ScopeArtifactFilter( scope );

        boolean systemOnline = !context.getSettings().getActiveProfile().isOffline();

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                context.getRemoteRepositories(),
                                                                                context.getLocalRepository(),
                                                                                sourceReader, filter );

        project.addArtifacts( result.getArtifacts().values(), artifactFactory );
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

            // TODO: should I get the modified artifacts back into the project?
            artifactResolver.resolve( artifact, context.getRemoteRepositories(), context.getLocalRepository() );
        }
    }

}

