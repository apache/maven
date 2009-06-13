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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

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
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

// TODO: the antrun plugin has its own configurator, the only plugin that does. might need to think about how that works
// TODO: remove the coreArtifactFilterManager

@Component(role = PluginManager.class)
public class DefaultPluginManager
    implements PluginManager
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
    private PluginCache pluginCache;

    /**
     * 
     * @param plugin
     * @param localRepository
     * @param remoteRepositories
     * @return PluginDescriptor The component descriptor for the Maven plugin.
     * @throws PluginNotFoundException The plugin could not be found in any repositories.
     * @throws PluginResolutionException The plugin could be found but could not be resolved.
     * @throws InvalidPluginDescriptorException 
     * @throws PlexusConfigurationException A discovered component descriptor cannot be read, or or can't be parsed correctly. Shouldn't 
     *                                      happen but if someone has made a descriptor by hand it's possible.
     * @throws CycleDetectedInComponentGraphException A cycle has been detected in the component graph for a plugin that has been dynamically loaded.
     */
    public synchronized PluginDescriptor loadPlugin( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, InvalidPluginDescriptorException
    {
        PluginDescriptor pluginDescriptor = pluginCache.getPluginDescriptor( plugin, localRepository, remoteRepositories );
        
        if ( pluginDescriptor != null )
        {
            return pluginDescriptor;
        }

        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( pluginArtifact )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( remoteRepositories )
            .setResolveTransitively( false );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );            
        }

        try
        {
            if ( pluginArtifact.getFile().isFile() )
            {
                JarFile pluginJar = new JarFile( pluginArtifact.getFile() );
                try
                {
                    ZipEntry pluginDescriptorEntry = pluginJar.getEntry( getComponentDescriptorLocation() );
    
                    if ( pluginDescriptorEntry != null )
                    {
                        InputStream is = pluginJar.getInputStream( pluginDescriptorEntry );
    
                        pluginDescriptor = parsebuildPluginDescriptor( is );
                    }
                }
                finally
                {
                    pluginJar.close();
                }
            }
            else
            {
                File pluginXml = new File( pluginArtifact.getFile(), getComponentDescriptorLocation() );

                if ( pluginXml.canRead() )
                {
                    InputStream is = new BufferedInputStream( new FileInputStream( pluginXml ) );
                    try
                    {
                        pluginDescriptor = parsebuildPluginDescriptor( is );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }
                }
            }

            String pluginKey = constructPluginKey( plugin );

            if ( pluginDescriptor == null )
            {
                throw new InvalidPluginDescriptorException( "Invalid or missing Plugin Descriptor for " + pluginKey );
            }

            // Check the internal consistent of a plugin descriptor when it is discovered. Most of the time the plugin descriptor is generated
            // by the maven-plugin-plugin, but if you happened to have created one by hand and it's incorrect this validator will report
            // the problem to the user.
            //
            MavenPluginValidator validator = new MavenPluginValidator( pluginArtifact );

            validator.validate( pluginDescriptor );

            if ( validator.hasErrors() )                                                                                                                        
            {          
                throw new InvalidPluginDescriptorException( "Invalid Plugin Descriptor for " + pluginKey, validator.getErrors() );
            }        

            pluginDescriptor.setPlugin( plugin );
            pluginDescriptor.setPluginArtifact( pluginArtifact );

            pluginCache.putPluginDescriptor( plugin, localRepository, remoteRepositories, pluginDescriptor );

            return pluginDescriptor;
        
        }
        catch ( IOException e )
        {
            throw new PluginDescriptorParsingException( plugin, e );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new PluginDescriptorParsingException( plugin, e );
        }
    }

    private PluginDescriptor parsebuildPluginDescriptor( InputStream is )
        throws IOException, PlexusConfigurationException
    {
        PluginDescriptor pluginDescriptor;
        XmlStreamReader reader = ReaderFactory.newXmlReader( is );

        InterpolationFilterReader interpolationFilterReader = new InterpolationFilterReader( new BufferedReader( reader ), container.getContext().getContextData() );

        pluginDescriptor = builder.build( interpolationFilterReader );
        return pluginDescriptor;
    }

    // TODO: Turn this into a component so it can be tested.
    //
    List<Artifact> getPluginArtifacts( Artifact pluginArtifact, Plugin pluginAsSpecifiedInPom, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
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
        
        for( Dependency dependencySpecifiedInProject : pluginAsSpecifiedInPom.getDependencies() )
        {
            // Right now if you add override dependencies they will not be operated on by the metadata source. The metadata source first grabs the plugins
            // defined dependencies and then the result is merged with the overrides. The overrides don't pass through the metadata source which is where the
            // Artifact.setFile( file ) method is called. We should eventually take care of this in the resolver.
            Artifact a = repositorySystem.createDependencyArtifact( dependencySpecifiedInProject );
            dependenciesToResolveForPlugin.add( a );                            
        }
        
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( pluginArtifact )
            // So this in fact are overrides ... 
            .setArtifactDependencies( dependenciesToResolveForPlugin )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( remoteRepositories )
            .setFilter( filter )
            .setResolveTransitively( true );
            //.setResolveRoot( false );
        
        ArtifactResolutionResult result = repositorySystem.resolve( request );
        resolutionErrorHandler.throwErrors( request, result );

        logger.debug( "Using the following artifacts for classpath of: " + pluginArtifact.getId() + ":\n\n" + result.getArtifacts().toString().replace( ',', '\n' ) );

        return new ArrayList<Artifact>( result.getArtifacts() );
    }

    // ----------------------------------------------------------------------
    // Mojo execution
    // ----------------------------------------------------------------------

    public void executeMojo( MavenSession session, MojoExecution mojoExecution )
        throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginManagerException
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

        ClassRealm pluginRealm = getPluginRealm( session, mojoDescriptor.getPluginDescriptor() );            
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
        catch ( PluginManagerException e )
        {
            throw new PluginExecutionException( mojoExecution, project, e );
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

    /**
     * TODO pluginDescriptor classRealm and artifacts are set as a side effect of this
     *      call, which is not nice.
     */
    public synchronized ClassRealm getPluginRealm( MavenSession session, PluginDescriptor pluginDescriptor ) 
        throws PluginManagerException
    {
        ClassRealm pluginRealm = pluginDescriptor.getClassRealm();
        if ( pluginRealm != null )
        {
            return pluginRealm;
        }

        Plugin plugin = pluginDescriptor.getPlugin();
        ArtifactRepository localRepository = session.getLocalRepository();
        List<ArtifactRepository> remoteRepositories = session.getCurrentProject().getPluginArtifactRepositories();

        PluginCache.CacheRecord cacheRecord = pluginCache.get( plugin, localRepository, remoteRepositories );

        if ( cacheRecord != null )
        {
            pluginDescriptor.setClassRealm( cacheRecord.realm );
            pluginDescriptor.setArtifacts( new ArrayList<Artifact>( cacheRecord.artifacts ) );

            return pluginRealm;
        }

        pluginRealm = createPluginRealm( plugin );

        Artifact pluginArtifact = pluginDescriptor.getPluginArtifact();

        List<Artifact> pluginArtifacts;

        try
        {
            pluginArtifacts = getPluginArtifacts( pluginArtifact, plugin, localRepository, remoteRepositories );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new IllegalStateException( e ); // XXX
        }
        catch ( ArtifactResolutionException e )
        {
            throw new IllegalStateException( e ); // XXX
        }

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

        pluginDescriptor.setClassRealm( pluginRealm );
        pluginDescriptor.setArtifacts( pluginArtifacts );
        
        try
        {
            for ( ComponentDescriptor componentDescriptor : pluginDescriptor.getComponents() )
            {
                componentDescriptor.setRealm( pluginRealm );
                container.addComponentDescriptor( componentDescriptor );
            }

            container.discoverComponents( pluginRealm );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new PluginManagerException( plugin, e.getMessage(), e );
        }
        catch ( CycleDetectedInComponentGraphException e )
        {
            throw new PluginManagerException( plugin, e.getMessage(), e );
        }

        pluginCache.put( plugin, localRepository, remoteRepositories, pluginRealm, pluginArtifacts );
        
        return pluginRealm;
    }

    /**
     * Creates ClassRealm with unique id for the given plugin
     */
    private ClassRealm createPluginRealm( Plugin plugin ) 
        throws PluginManagerException
    {
        ClassWorld world = ((MutablePlexusContainer) container).getClassWorld();

        String baseRealmId = constructPluginKey( plugin );
        String realmId = baseRealmId;

        synchronized ( world )
        {
            for ( int i = 0; i < 100; i++ )
            {
                try
                {
                    ClassRealm pluginRealm = world.newRealm( realmId );
                    pluginRealm.setParentRealm( container.getContainerRealm() );
                    return pluginRealm;
                }
                catch ( DuplicateRealmException e )
                {
                    realmId = baseRealmId + "-" + i;
                }
            }
        }

        throw new PluginManagerException( plugin, "Could not create ClassRealm", (Throwable) null );
    }

    private Mojo getConfiguredMojo( MavenSession session, MavenProject project, MojoExecution mojoExecution, ClassRealm pluginRealm )
        throws PluginConfigurationException, PluginManagerException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        // We are forcing the use of the plugin realm for all lookups that might occur during
        // the lifecycle that is part of the lookup. Here we are specifically trying to keep
        // lookups that occur in contextualize calls in line with the right realm.
        ClassRealm oldLookupRealm = container.setLookupRealm( pluginRealm );

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( pluginRealm );
        container.setLookupRealm( pluginRealm );

        try
        {
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

            return mojo;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldClassLoader );
            container.setLookupRealm( oldLookupRealm );
        }

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

    public MojoDescriptor getMojoDescriptor( String groupId, String artifactId, String version, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, InvalidPluginDescriptorException
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId( groupId );        
        plugin.setArtifactId( artifactId );
        plugin.setVersion( version );
        
        return getMojoDescriptor( plugin, goal, localRepository, remoteRepositories );
    }
        
    public MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, CycleDetectedInPluginGraphException, MojoNotFoundException, InvalidPluginDescriptorException
    {
        PluginDescriptor pluginDescriptor = loadPlugin( plugin, localRepository, remoteRepositories );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );

        if ( mojoDescriptor == null )
        {
            throw new MojoNotFoundException( goal, pluginDescriptor );
        }

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

    // ----------------------------------------------------------------------
    // Component Discovery Listener
    // ----------------------------------------------------------------------

    public String constructPluginKey( Plugin plugin )
    {
        String version = ArtifactUtils.toSnapshotVersion( plugin.getVersion() );
        return plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + version;
    }

    public String constructPluginKey( PluginDescriptor pluginDescriptor )
    {
        String version = ArtifactUtils.toSnapshotVersion( pluginDescriptor.getVersion() );
        return pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId() + ":" + version;
    }
}
