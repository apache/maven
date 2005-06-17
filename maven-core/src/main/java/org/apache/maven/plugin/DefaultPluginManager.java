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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.artifact.resolver.filter.InversionArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.version.PluginVersionManager;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
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
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
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
    protected Map pluginDescriptors;

    protected Map pluginDescriptorsByPrefix;

    protected PlexusContainer container;

    protected PluginDescriptorBuilder pluginDescriptorBuilder;

    protected ArtifactFilter artifactFilter;

    protected PathTranslator pathTranslator;

    private Set pluginsInProcess = new HashSet();

    private Log mojoLogger;

    public DefaultPluginManager()
    {
        pluginDescriptors = new HashMap();

        pluginDescriptorsByPrefix = new HashMap();

        pluginDescriptorBuilder = new PluginDescriptorBuilder();
    }

    // ----------------------------------------------------------------------
    // Mojo discovery
    // ----------------------------------------------------------------------

    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( componentSetDescriptor instanceof PluginDescriptor )
        {
            PluginDescriptor pluginDescriptor = (PluginDescriptor) componentSetDescriptor;

            //            String key = pluginDescriptor.getId();
            // TODO: see comment in getPluginDescriptor
            String key = pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId();

            if ( !pluginsInProcess.contains( key ) )
            {
                pluginsInProcess.add( key );

                pluginDescriptors.put( key, pluginDescriptor );

                // TODO: throw an (not runtime) exception if there is a prefix overlap - means doing so elsewhere
                if ( !pluginDescriptorsByPrefix.containsKey( pluginDescriptor.getGoalPrefix() ) )
                {
                    pluginDescriptorsByPrefix.put( pluginDescriptor.getGoalPrefix(), pluginDescriptor );
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private PluginDescriptor getPluginDescriptor( String groupId, String artifactId, String version )
    {
        //        String key = PluginDescriptor.constructPluginKey( groupId, artifactId, version );
        // TODO: include version, but can't do this in the plugin manager as it is not resolved to the right version
        // at that point. Instead, move the duplication check to the artifact container, or store it locally based on
        // the unresolved version?
        String key = groupId + ":" + artifactId;
        return (PluginDescriptor) pluginDescriptors.get( key );
    }

    private PluginDescriptor getPluginDescriptor( String prefix )
    {
        return (PluginDescriptor) pluginDescriptorsByPrefix.get( prefix );
    }

    private boolean isPluginInstalled( String pluginKey )
    {
        //        String key = PluginDescriptor.constructPluginKey( groupId, artifactId, version );
        // TODO: see comment in getPluginDescriptor
        return pluginDescriptors.containsKey( pluginKey );
    }

    private boolean isPluginInstalledForPrefix( String prefix )
    {
        return pluginDescriptorsByPrefix.containsKey( prefix );
    }

    public PluginDescriptor verifyPlugin( String prefix )
    {
        if ( !isPluginInstalledForPrefix( prefix ) )
        {
            // TODO: lookup remotely
        }
        return getPluginDescriptor( prefix );
    }

    public PluginDescriptor verifyPlugin( String groupId, String artifactId, String version, MavenProject project,
                                          Settings settings, ArtifactRepository localRepository )
        throws ArtifactResolutionException, PluginManagerException, PluginVersionResolutionException
    {
        String pluginKey = groupId + ":" + artifactId;

        // TODO: this should be possibly outside
        // [HTTP-301] All version-resolution logic has been moved to DefaultPluginVersionManager. :)
        if ( version == null )
        {
            PluginVersionManager pluginVersionManager = null;

            try
            {
                pluginVersionManager = (PluginVersionManager) container.lookup( PluginVersionManager.ROLE );

                version = pluginVersionManager.resolvePluginVersion( groupId, artifactId, project, settings,
                                                                     localRepository );
            }
            catch ( ComponentLookupException e )
            {
                throw new PluginVersionResolutionException( groupId, artifactId,
                                                            "Cannot retrieve an instance of the PluginVersionManager",
                                                            e );
            }
            finally
            {
                releaseComponent( pluginVersionManager );
            }
        }

        // TODO: this might result in an artifact "RELEASE" being resolved continuously
        if ( !isPluginInstalled( pluginKey ) )
        {
            ArtifactFactory artifactFactory = null;
            try
            {
                artifactFactory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );

                Artifact pluginArtifact = artifactFactory.createArtifact( groupId, artifactId, version,
                                                                          Artifact.SCOPE_RUNTIME,
                                                                          MojoDescriptor.MAVEN_PLUGIN, null );

                addPlugin( pluginKey, pluginArtifact, project, localRepository );

                version = pluginArtifact.getBaseVersion();
            }
            catch ( PlexusContainerException e )
            {
                throw new PluginManagerException(
                    "Error occurred in the artifact container attempting to download plugin " + groupId + ":" +
                        artifactId, e );
            }
            catch ( ArtifactResolutionException e )
            {
                if (
                    ( groupId == null || artifactId == null || version == null ||
                        ( groupId.equals( e.getGroupId() ) && artifactId.equals( e.getArtifactId() ) &&
                            version.equals( e.getVersion() ) ) ) && "maven-plugin".equals( e.getType() ) )
                {
                    throw new PluginNotFoundException( e );
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
            finally
            {
                if ( artifactFactory != null )
                {
                    releaseComponent( artifactFactory );
                }

            }
        }
        return getPluginDescriptor( groupId, artifactId, version );
    }

    protected void addPlugin( String pluginKey, Artifact pluginArtifact, MavenProject project,
                              ArtifactRepository localRepository )
        throws ArtifactResolutionException, ComponentLookupException, PlexusContainerException
    {
        ArtifactResolver artifactResolver = null;
        MavenProjectBuilder mavenProjectBuilder = null;
        ArtifactFactory artifactFactory = null;

        try
        {
            artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );
            artifactFactory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );

            MavenMetadataSource metadataSource = new MavenMetadataSource( artifactResolver, mavenProjectBuilder,
                                                                          artifactFactory );

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections
                .singleton( pluginArtifact ), project.getRemoteArtifactRepositories(), localRepository, metadataSource,
                                              artifactFilter );

            Map resolved = result.getArtifacts();

            List files = new ArrayList();

            for ( Iterator it = resolved.values().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                files.add( artifact.getFile() );
            }

            container.createChildContainer( pluginKey, files, Collections.EMPTY_MAP,
                                            Collections.singletonList( this ) );

            // this plugin's descriptor should have been discovered by now, so we should be able to circle
            // around and set the artifacts.
            PluginDescriptor addedPlugin = (PluginDescriptor) pluginDescriptors.get( pluginKey );

            ArtifactFilter distroProvidedFilter = new InversionArtifactFilter( artifactFilter );

            ArtifactResolutionResult distroProvidedResult = artifactResolver.resolveTransitively( Collections
                .singleton( pluginArtifact ), project.getRemoteArtifactRepositories(), localRepository, metadataSource,
                                              distroProvidedFilter );

            Map distroProvided = distroProvidedResult.getArtifacts();

            List unfilteredArtifactList = new ArrayList( resolved.size() + distroProvided.size() );

            unfilteredArtifactList.addAll( resolved.values() );
            unfilteredArtifactList.addAll( distroProvided.values() );

            addedPlugin.setArtifacts( unfilteredArtifactList );
        }
        finally
        {
            if ( artifactFactory != null )
            {
                releaseComponent( artifactFactory );
            }
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
    // Mojo execution
    // ----------------------------------------------------------------------

    public void executeMojo( MavenSession session, GoalInstance goalInstance )
        throws ArtifactResolutionException, PluginManagerException, MojoExecutionException
    {
        PlexusContainer pluginContainer = null;

        MojoDescriptor mojoDescriptor = goalInstance.getMojoDescriptor();

        if ( mojoDescriptor.isDependencyResolutionRequired() != null )
        {

            ArtifactResolver artifactResolver = null;
            MavenProjectBuilder mavenProjectBuilder = null;
            ArtifactFactory artifactFactory = null;

            try
            {
                artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
                mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );
                artifactFactory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );

                resolveTransitiveDependencies( session, artifactResolver, mavenProjectBuilder, mojoDescriptor
                    .isDependencyResolutionRequired(), artifactFactory );
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
                if ( artifactFactory != null )
                {
                    releaseComponent( artifactFactory );
                }
            }
        }

        Mojo plugin = null;

        String goalName = mojoDescriptor.getFullGoalName();

        try
        {
            String pluginKey = mojoDescriptor.getPluginDescriptor().getPluginLookupKey();

            pluginContainer = container.getChildContainer( pluginKey );

            if ( pluginContainer == null )
            {
                throw new PluginConfigurationException( "Cannot find PlexusContainer for plugin: " + pluginKey );
            }

            plugin = (Mojo) pluginContainer.lookup( Mojo.ROLE, mojoDescriptor.getRoleHint() );
            plugin.setLog( mojoLogger );

            String goalId = goalInstance.getGoalId();

            PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

            Xpp3Dom dom = goalInstance.getCalculatedConfiguration();

            PlexusConfiguration pomConfiguration;
            if ( dom == null )
            {
                pomConfiguration = new XmlPlexusConfiguration( "configuration" );
            }
            else
            {
                pomConfiguration = new XmlPlexusConfiguration( dom );
            }

            // Validate against non-editable (@readonly) parameters, to make sure users aren't trying to
            // override in the POM.
            validatePomConfiguration( mojoDescriptor, pomConfiguration );

            PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration, mojoDescriptor
                .getMojoConfiguration() );

            // TODO: plexus
            //            PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration,
            //                                                                          mojoDescriptor.getConfiguration() );

            ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, pluginDescriptor,
                                                                                              pathTranslator,
                                                                                              getLogger() );

            checkRequiredParameters( mojoDescriptor, mergedConfiguration, expressionEvaluator, plugin );

            populatePluginFields( plugin, mojoDescriptor, mergedConfiguration, pluginContainer, expressionEvaluator );

            // !! This is ripe for refactoring to an aspect.
            // Event monitoring.
            String event = MavenEvents.MOJO_EXECUTION;
            EventDispatcher dispatcher = session.getEventDispatcher();

            String goalExecId = goalName;

            if ( goalInstance.getExecutionId() != null )
            {
                goalExecId += " {execution: " + goalInstance.getExecutionId() + "}";
            }

            dispatcher.dispatchStart( event, goalExecId );
            try
            {
                plugin.execute();

                dispatcher.dispatchEnd( event, goalExecId );
            }
            catch ( MojoExecutionException e )
            {
                session.getEventDispatcher().dispatchError( event, goalExecId, e );

                throw e;
            }
            // End event monitoring.

        }
        catch ( PluginConfigurationException e )
        {
            String msg = "Error configuring plugin for execution of '" + goalName + "'.";
            throw new MojoExecutionException( msg, e );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoExecutionException( "Error looking up plugin: ", e );
        }
        finally
        {
            try
            {
                pluginContainer.release( plugin );
            }
            catch ( ComponentLifecycleException e )
            {
                if ( getLogger().isErrorEnabled() )
                {
                    getLogger().error( "Error releasing plugin - ignoring.", e );
                }
            }
        }
    }

    private void checkRequiredParameters( MojoDescriptor goal, PlexusConfiguration configuration,
                                          ExpressionEvaluator expressionEvaluator, Mojo plugin )
        throws PluginConfigurationException
    {
        // TODO: this should be built in to the configurator, as we presently double process the expressions

        List parameters = goal.getParameters();

        List invalidParameters = new ArrayList();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

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

                    if ( fieldValue == null && StringUtils.isNotEmpty( parameter.getAlias() ) )
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
                    throw new PluginConfigurationException( "Bad expression", e );
                }

                if ( fieldValue == null && goal.getComponentConfigurator() == null )
                {
                    try
                    {
                        // TODO: remove in beta-1
                        Field field = findPluginField( plugin.getClass(), parameter.getName() );
                        boolean accessible = field.isAccessible();
                        if ( !accessible )
                        {
                            field.setAccessible( true );
                        }
                        fieldValue = field.get( plugin );
                        if ( !accessible )
                        {
                            field.setAccessible( false );
                        }
                        if ( fieldValue != null )
                        {
                            getLogger().warn(
                                "DEPRECATED: using default-value to set the default value of field '" +
                                    parameter.getName() + "'" );
                        }
                    }
                    catch ( NoSuchFieldException e )
                    {
                        throw new PluginConfigurationException( "Unable to find field to check default value", e );
                    }
                    catch ( IllegalAccessException e )
                    {
                        throw new PluginConfigurationException( "Unable to read field to check default value", e );
                    }
                }

                if ( fieldValue == null )
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
        List parameters = goal.getParameters();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            // the key for the configuration map we're building.
            String key = parameter.getName();

            PlexusConfiguration value = pomConfiguration.getChild( key, false );

            if ( value == null && StringUtils.isNotEmpty( parameter.getAlias() ) )
            {
                key = parameter.getAlias();
                value = pomConfiguration.getChild( key, false );
            }

            if ( value != null )
            {
                // Make sure the parameter is either editable/configurable, or else is NOT specified in the POM
                if ( !parameter.isEditable() )
                {
                    StringBuffer errorMessage = new StringBuffer()
                        .append( "ERROR: Cannot override read-only parameter: " );
                    errorMessage.append( key );
                    errorMessage.append( " in goal: " ).append( goal.getFullGoalName() );

                    throw new PluginConfigurationException( errorMessage.toString() );
                }

                String deprecated = parameter.getDeprecated();
                if ( StringUtils.isNotEmpty( deprecated ) )
                {
                    getLogger().warn( "DEPRECATED [" + parameter.getName() + "]: " + deprecated );
                }
            }
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

    // ----------------------------------------------------------------------
    // Mojo Parameter Handling
    // ----------------------------------------------------------------------

    private void populatePluginFields( Mojo plugin, MojoDescriptor mojoDescriptor, PlexusConfiguration configuration,
                                       PlexusContainer pluginContainer, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        ComponentConfigurator configurator = null;

        try
        {
            String configuratorId = mojoDescriptor.getComponentConfigurator();

            // TODO: should this be known to the component factory instead? And if so, should configuration be part of lookup?
            if ( StringUtils.isNotEmpty( configuratorId ) )
            {
                configurator = (ComponentConfigurator) pluginContainer.lookup( ComponentConfigurator.ROLE,
                                                                               configuratorId );
            }
            else
            {
                configurator = (ComponentConfigurator) pluginContainer.lookup( ComponentConfigurator.ROLE );
            }

            configurator.configureComponent( plugin, configuration, expressionEvaluator, pluginContainer
                .getContainerRealm() );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new PluginConfigurationException( "Unable to parse the created DOM for plugin configuration", e );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginConfigurationException(
                "Unable to retrieve component configurator for plugin configuration", e );
        }
        finally
        {
            if ( configurator != null )
            {
                try
                {
                    pluginContainer.release( configurator );
                }
                catch ( ComponentLifecycleException e )
                {
                    getLogger().debug( "Failed to release plugin container - ignoring." );
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

    public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter,
                                                               String expression )
    {
        StringBuffer message = new StringBuffer();

        message.append( "The '" + parameter.getName() );
        message.append( "' parameter is required for the execution of the " );
        message.append( mojo.getFullGoalName() );
        message.append( " mojo and cannot be null." );
        if ( expression != null )
        {
            message.append( " The retrieval expression was: " ).append( expression );
        }

        return message.toString();
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );

        LoggerManager manager = null;

        try
        {
            manager = (LoggerManager) container.lookup( LoggerManager.ROLE );

            mojoLogger = new DefaultLog( manager.getLoggerForComponent( Mojo.ROLE ) );
        }
        catch ( ComponentLookupException e )
        {
            throw new ContextException( "Error locating a logger manager", e );
        }
        finally
        {
            if ( manager != null )
            {
                try
                {
                    container.release( manager );
                }
                catch ( ComponentLifecycleException e )
                {
                    getLogger().error( "Error releasing the logger manager - ignoring", e );
                }
            }
        }
    }

    public void initialize()
    {
        // TODO: configure this from bootstrap or scan lib
        artifactFilter = new ExclusionSetFilter(
            new String[]{"classworlds", "maven-artifact", "maven-core", "maven-model", "maven-monitor", "maven-plugin-api", "maven-plugin-descriptor", "maven-project", "maven-settings", "plexus-container-default", "plexus-utils", "wagon-provider-api", "wagon-ssh", "wagon-http-lightweight", "wagon-file"} );
    }

    // ----------------------------------------------------------------------
    // Artifact resolution
    // ----------------------------------------------------------------------

    private void resolveTransitiveDependencies( MavenSession context, ArtifactResolver artifactResolver,
                                                MavenProjectBuilder mavenProjectBuilder, String scope,
                                                ArtifactFactory artifactFactory )
        throws ArtifactResolutionException
    {
        MavenProject project = context.getProject();

        MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, mavenProjectBuilder,
                                                                    artifactFactory );

        ArtifactFilter filter = new ScopeArtifactFilter( scope );

        boolean systemOnline = !context.getSettings().isOffline();

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(), context
            .getRemoteRepositories(), context.getLocalRepository(), sourceReader, filter );

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

        Set pluginArtifacts = new HashSet();
        for ( Iterator it = context.getProject().getPluginArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            artifactResolver.resolve( artifact, context.getRemoteRepositories(), context.getLocalRepository() );

            pluginArtifacts.add( artifact );
        }
        context.getProject().setPluginArtifacts( pluginArtifacts );

        artifactResolver.resolve( context.getProject().getParentArtifact(), context.getRemoteRepositories(), context
            .getLocalRepository() );
    }

}
