package org.apache.maven.plugin;

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

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextMapAdapter;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

// TODO: get plugin groups
// TODO: separate out project downloading, something should decide before the plugin executes. it should not happen inside this
// TODO: the antrun plugin has its own configurator, the only plugin that does. might need to think about how that works
// TODO: remove the coreArtifactFilterManager

@Component(role = PluginManager.class)
public class DefaultPluginManager
    implements PluginManager, ComponentDiscoverer, ComponentDiscoveryListener
{
    @Requirement
    private Logger logger;

    @Requirement
    protected PlexusContainer container;

    @Requirement
    protected ArtifactFilterManager coreArtifactFilterManager;

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private PluginClassLoaderCache pluginClassLoaderCache;
    
    private Map<String, PluginDescriptor> pluginDescriptors;

    public DefaultPluginManager()
    {
        pluginDescriptors = new HashMap<String, PluginDescriptor>();
    }

    // This should be template method code for allowing subclasses to assist in contributing search/hint information
    public Plugin findPluginForPrefix( String prefix, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
    {
        //Use the plugin managers capabilities to get information to augement the request

        return null;
        //return getByPrefix( prefix, session.getPluginGroups(), project.getRemoteArtifactRepositories(), session.getLocalRepository() );
    }

    public PluginDescriptor loadPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor = getPluginDescriptor( plugin );

        // There are cases where plugins are discovered but not actually populated. These are edge cases where you are working in the IDE on
        // Maven itself so this speaks to a problem we have with the system not starting entirely clean.
        if ( pluginDescriptor != null && pluginDescriptor.getClassRealm() != null )
        {
            return pluginDescriptor;
        }

        try
        {
            return addPlugin( plugin, localRepository, remoteRepositories );
        }
        catch ( ArtifactResolutionException e )
        // PluginResolutionException - a problem that occurs resolving the plugin artifact or its deps
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        // PluginNotFoundException - the plugin itself cannot be found in any repositories
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginContainerException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
    }

    private String pluginKey( Plugin plugin )
    {
        return plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion();
    }

    protected PluginDescriptor addPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactNotFoundException, ArtifactResolutionException, PluginContainerException, PluginVersionNotFoundException
    {
        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( pluginArtifact )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( remoteRepositories );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        resolutionErrorHandler.throwErrors( request, result );

        ClassRealm pluginRealm = pluginClassLoaderCache.get( constructPluginKey( plugin ) );
        
        if ( pluginRealm != null )            
        {
            return getPluginDescriptor( plugin );            
        }            
            
        pluginRealm = container.createChildRealm( pluginKey( plugin ) );

        Set<Artifact> pluginArtifacts = getPluginArtifacts( pluginArtifact, plugin, localRepository, remoteRepositories );

        for ( Artifact a : pluginArtifacts )
        {
            try
            {
                pluginRealm.addURL( a.getFile().toURI().toURL() );
            }
            catch ( MalformedURLException e )
            {
                // Not going to happen
            }
        }
                
        try
        {
            container.discoverComponents( pluginRealm );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new PluginContainerException( plugin, pluginRealm, "Error scanning plugin realm for components.", e );
        }
        catch ( ComponentRepositoryException e )
        {
            throw new PluginContainerException( plugin, pluginRealm, "Error scanning plugin realm for components.", e );
        }

        pluginClassLoaderCache.put( constructPluginKey( plugin ), pluginRealm );
                
        return getPluginDescriptor( plugin );
    }

    private Set<Artifact> getPluginArtifacts( Artifact pluginArtifact, Plugin pluginAsSpecifiedinPom, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        AndArtifactFilter filter = new AndArtifactFilter();
        filter.add( coreArtifactFilterManager.getCoreArtifactFilter() );
        filter.add( new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM ) );

        Set<Artifact> dependenciesToResolveForPlugin = new LinkedHashSet<Artifact>();

        // The case where we have a plugin that can host multiple versions of a particular tool. Say the 
        // Antlr plugin which has many versions and you may want the plugin to execute with version 2.7.1 of
        // Antlr versus 2.7.2. In this case the project itself would specify dependencies within the plugin
        // element.

        // These dependencies might called override dependencies. We want anything in this set of override
        // any of the resolved dependencies of the plugin artifact.
        
        // We would almost always want the everything to be resolved from the root but we have this special case
        // of overrides from the project itself which confused the interface.
        
        for( Dependency dependencySpecifiedInProject : pluginAsSpecifiedinPom.getDependencies() )
        {
            dependenciesToResolveForPlugin.add( repositorySystem.createDependencyArtifact( dependencySpecifiedInProject ) );
        }
        
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( pluginArtifact )
            // So this in fact are overrides ... 
            .setArtifactDependencies( dependenciesToResolveForPlugin )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( remoteRepositories )
            .setFilter( filter )
            .setResolveTransitively( true );
        
        ArtifactResolutionResult result = repositorySystem.resolve( request );
        resolutionErrorHandler.throwErrors( request, result );

        logger.debug( "Using the following artifacts for classpath of: " + pluginArtifact.getId() + ":\n\n" + result.getArtifacts().toString().replace( ',', '\n' ) );

        return result.getArtifacts();
    }

    // ----------------------------------------------------------------------
    // Mojo execution
    // ----------------------------------------------------------------------

    public void executeMojo( MavenSession session, MojoExecution mojoExecution )
        throws MojoFailureException, PluginExecutionException, PluginConfigurationException
    {
        MavenProject project = session.getCurrentProject();

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        String goalName = mojoDescriptor.getFullGoalName();

        Mojo mojo = null;

        String goalExecId = goalName;
        if ( mojoExecution.getExecutionId() != null )
        {
            goalExecId += " {execution: " + mojoExecution.getExecutionId() + "}";
        }

        // by this time, the pluginDescriptor has had the correct realm setup from getConfiguredMojo(..)
        ClassRealm pluginRealm = pluginClassLoaderCache.get( constructPluginKey( mojoDescriptor.getPluginDescriptor() ) );            
        ClassRealm oldLookupRealm = container.getLookupRealm();
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {                        
            mojo = getConfiguredMojo( session, project, mojoExecution, pluginRealm );

            Thread.currentThread().setContextClassLoader( pluginRealm );

            // NOTE: DuplicateArtifactAttachmentException is currently unchecked, so be careful removing this try/catch!
            // This is necessary to avoid creating compatibility problems for existing plugins that use
            // MavenProjectHelper.attachArtifact(..).
            try
            {
                mojo.execute();
            }
            catch ( DuplicateArtifactAttachmentException e )
            {
                throw new PluginExecutionException( mojoExecution, project, e );
            }
        }
        catch ( MojoExecutionException e )
        {
            throw new PluginExecutionException( mojoExecution, project, e );
        }
        catch ( MojoFailureException e )
        {
            throw e;
        }

        catch ( PluginManagerException e )
        {
            throw new PluginExecutionException( mojoExecution, project, e.getMessage() );
        }
        finally
        {
            if ( mojo != null )
            {
                try
                {
                    container.release( mojo );
                }
                catch ( ComponentLifecycleException e )
                {
                    logger.debug( "Error releasing mojo for: " + goalExecId, e );
                }
            }

            if ( oldLookupRealm != null )
            {
                container.setLookupRealm( null );
            }

            Thread.currentThread().setContextClassLoader( oldClassLoader );
        }
    }

    private Mojo getConfiguredMojo( MavenSession session, MavenProject project, MojoExecution mojoExecution, ClassRealm pluginRealm )
        throws PluginConfigurationException, PluginManagerException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        // We are forcing the use of the plugin realm for all lookups that might occur during
        // the lifecycle that is part of the lookup. Here we are specifically trying to keep
        // lookups that occur in contextualize calls in line with the right realm.
        container.setLookupRealm( pluginRealm );

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( pluginRealm );

        Mojo mojo;

        try
        {
            mojo = container.lookup( Mojo.class, mojoDescriptor.getRoleHint() );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginContainerException( mojoDescriptor, pluginRealm, "Unable to find the mojo '" + mojoDescriptor.getRoleHint() + "' in the plugin '" + pluginDescriptor.getPluginLookupKey()
                + "'", e );
        }

        if ( mojo instanceof ContextEnabled )
        {
            //TODO: find somewhere better to put the plugin context.
            Map<String, Object> pluginContext = null;

            if ( pluginContext != null )
            {
                pluginContext.put( "project", project );

                pluginContext.put( "pluginDescriptor", pluginDescriptor );

                ( (ContextEnabled) mojo ).setPluginContext( pluginContext );
            }
        }

        mojo.setLog( new DefaultLog( logger ) );

        Xpp3Dom dom = mojoExecution.getConfiguration();

        PlexusConfiguration pomConfiguration;

        if ( dom == null )
        {
            pomConfiguration = new XmlPlexusConfiguration( "configuration" );
        }
        else
        {
            pomConfiguration = new XmlPlexusConfiguration( dom );
        }

        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, mojoExecution );

        populatePluginFields( mojo, mojoDescriptor, pluginRealm, pomConfiguration, expressionEvaluator );

        Thread.currentThread().setContextClassLoader( oldClassLoader );

        return mojo;
    }

    // ----------------------------------------------------------------------
    // Mojo Parameter Handling
    // ----------------------------------------------------------------------

    private void populatePluginFields( Mojo mojo, MojoDescriptor mojoDescriptor, ClassRealm realm, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        ComponentConfigurator configurator = null;

        try
        {
            String configuratorId = mojoDescriptor.getComponentConfigurator();

            // TODO: could the configuration be passed to lookup and the configurator known to plexus via the descriptor
            // so that this meethod could entirely be handled by a plexus lookup?
            if ( StringUtils.isNotEmpty( configuratorId ) )
            {
                configurator = container.lookup( ComponentConfigurator.class, configuratorId );
            }
            else
            {
                configurator = container.lookup( ComponentConfigurator.class, "basic" );
            }

            ConfigurationListener listener = new DebugConfigurationListener( logger );

            logger.debug( "Configuring mojo '" + mojoDescriptor.getId() + "' with " + ( configuratorId == null ? "basic" : configuratorId ) + " configurator -->" );

            // This needs to be able to use methods
            configurator.configureComponent( mojo, configuration, expressionEvaluator, realm, listener );

            logger.debug( "-- end configuration --" );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(), "Unable to parse the created DOM for plugin configuration", e );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(), "Unable to retrieve component configurator for plugin configuration", e );
        }
        catch ( LinkageError e )
        {
            if ( logger.isFatalErrorEnabled() )
            {
                logger.fatalError( configurator.getClass().getName() + "#configureComponent(...) caused a linkage error (" + e.getClass().getName() + ") and may be out-of-date. Check the realms:" );

                ClassRealm pluginRealm = mojoDescriptor.getPluginDescriptor().getClassRealm();
                StringBuffer sb = new StringBuffer();
                sb.append( "Plugin realm = " + pluginRealm.getId() ).append( '\n' );
                for ( int i = 0; i < pluginRealm.getURLs().length; i++ )
                {
                    sb.append( "urls[" + i + "] = " + pluginRealm.getURLs()[i] );
                    if ( i != ( pluginRealm.getURLs().length - 1 ) )
                    {
                        sb.append( '\n' );
                    }
                }
                logger.fatalError( sb.toString() );

                ClassRealm containerRealm = container.getContainerRealm();
                sb = new StringBuffer();
                sb.append( "Container realm = " + containerRealm.getId() ).append( '\n' );
                for ( int i = 0; i < containerRealm.getURLs().length; i++ )
                {
                    sb.append( "urls[" + i + "] = " + containerRealm.getURLs()[i] );
                    if ( i != ( containerRealm.getURLs().length - 1 ) )
                    {
                        sb.append( '\n' );
                    }
                }
                logger.fatalError( sb.toString() );
            }

            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(), e.getClass().getName() + ": " + e.getMessage(), new ComponentConfigurationException( e ) );
        }
        finally
        {
            if ( configurator != null )
            {
                try
                {
                    container.release( configurator );
                }
                catch ( ComponentLifecycleException e )
                {
                    logger.debug( "Failed to release plugin container - ignoring." );
                }
            }
        }
    }

    public MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor = loadPlugin( plugin, localRepository, remoteRepositories );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );

        return mojoDescriptor;
    }

    // ----------------------------------------------------------------------
    // Component Discovery
    // ----------------------------------------------------------------------

    private PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

    public String getComponentDescriptorLocation()
    {
        return "META-INF/maven/plugin.xml";
    }

    public ComponentSetDescriptor createComponentDescriptors( Reader componentDescriptorConfiguration, String source )
        throws PlexusConfigurationException
    {
        return builder.build( componentDescriptorConfiguration, source );
    }

    public List<ComponentSetDescriptor> findComponents( Context context, ClassRealm realm )
        throws PlexusConfigurationException
    {
        List<ComponentSetDescriptor> componentSetDescriptors = new ArrayList<ComponentSetDescriptor>();

        Enumeration<URL> resources;
        try
        {
            // We don't always want to scan parent realms. For plexus
            // testcase, most components are in the root classloader so that needs to be scanned,
            // but for child realms, we don't.
            if ( realm.getParentRealm() != null )
            {
                resources = realm.findRealmResources( getComponentDescriptorLocation() );
            }
            else
            {
                resources = realm.findResources( getComponentDescriptorLocation() );
            }
        }
        catch ( IOException e )
        {
            throw new PlexusConfigurationException( "Unable to retrieve resources for: " + getComponentDescriptorLocation() + " in class realm: " + realm.getId() );
        }

        for ( URL url : Collections.list( resources ) )
        {
            Reader reader = null;

            try
            {
                URLConnection conn = url.openConnection();

                conn.setUseCaches( false );

                conn.connect();

                reader = ReaderFactory.newXmlReader( conn.getInputStream() );

                InterpolationFilterReader interpolationFilterReader = new InterpolationFilterReader( reader, new ContextMapAdapter( context ) );

                ComponentSetDescriptor componentSetDescriptor = createComponentDescriptors( interpolationFilterReader, url.toString() );

                if ( componentSetDescriptor.getComponents() != null )
                {
                    for ( ComponentDescriptor<?> cd : componentSetDescriptor.getComponents() )
                    {
                        cd.setComponentSetDescriptor( componentSetDescriptor );
                        cd.setRealm( realm );
                    }
                }

                componentSetDescriptors.add( componentSetDescriptor );
            }
            catch ( IOException ex )
            {
                throw new PlexusConfigurationException( "Error reading configuration " + url, ex );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        return componentSetDescriptors;
    }

    // ----------------------------------------------------------------------
    // Component Discovery Listener
    // ----------------------------------------------------------------------

    private Set pluginsInProcess = new HashSet();

    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( componentSetDescriptor instanceof PluginDescriptor )
        {
            PluginDescriptor pluginDescriptor = (PluginDescriptor) componentSetDescriptor;

            String key = constructPluginKey( pluginDescriptor );

            if ( !pluginsInProcess.contains( key ) )
            {
                pluginsInProcess.add( key );

                pluginDescriptors.put( key, pluginDescriptor );
            }
        }
    }

    public PluginDescriptor getPluginDescriptor( Plugin plugin )
    {
        return pluginDescriptors.get( constructPluginKey( plugin ) );
    }

    private String constructPluginKey( Plugin plugin )
    {
        String version = ArtifactUtils.toSnapshotVersion( plugin.getVersion() );
        return plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + version;
    }

    private String constructPluginKey( PluginDescriptor pluginDescriptor )
    {
        String version = ArtifactUtils.toSnapshotVersion( pluginDescriptor.getVersion() );
        return pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId() + ":" + version;
    }
}
