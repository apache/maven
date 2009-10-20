package org.apache.maven.plugin.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MavenPluginValidator;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
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
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Provides basic services to manage Maven plugins and their mojos. This component is kept general in its design such
 * that the plugins/mojos can be used in arbitrary contexts. In particular, the mojos can be used for ordinary build
 * plugins as well as special purpose plugins like reports.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = MavenPluginManager.class )
public class DefaultMavenPluginManager
    implements MavenPluginManager
{

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private ClassRealmManager classRealmManager;

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private ArtifactFilterManager artifactFilterManager;

    @Requirement
    private PluginDescriptorCache pluginDescriptorCache;

    @Requirement
    private PluginRealmCache pluginRealmCache;

    private PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

    public synchronized PluginDescriptor getPluginDescriptor( Plugin plugin, RepositoryRequest repositoryRequest )
        throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException
    {
        PluginDescriptorCache.Key cacheKey = pluginDescriptorCache.createKey( plugin, repositoryRequest );

        PluginDescriptor pluginDescriptor = pluginDescriptorCache.get( cacheKey );

        if ( pluginDescriptor == null )
        {
            Artifact pluginArtifact = resolvePluginArtifact( plugin, repositoryRequest );

            pluginDescriptor = extractPluginDescriptor( pluginArtifact, plugin );

            pluginDescriptorCache.put( cacheKey, pluginDescriptor );
        }

        pluginDescriptor.setPlugin( plugin );

        return pluginDescriptor;
    }

    private Artifact resolvePluginArtifact( Plugin plugin, RepositoryRequest repositoryRequest )
        throws PluginResolutionException
    {
        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( repositoryRequest );
        request.setArtifact( pluginArtifact );
        request.setResolveTransitively( false );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        return pluginArtifact;
    }

    private PluginDescriptor extractPluginDescriptor( Artifact pluginArtifact, Plugin plugin )
        throws PluginDescriptorParsingException, InvalidPluginDescriptorException
    {
        PluginDescriptor pluginDescriptor = null;

        File pluginFile = pluginArtifact.getFile();

        try
        {
            if ( pluginFile.isFile() )
            {
                JarFile pluginJar = new JarFile( pluginFile, false );
                try
                {
                    ZipEntry pluginDescriptorEntry = pluginJar.getEntry( getPluginDescriptorLocation() );

                    if ( pluginDescriptorEntry != null )
                    {
                        InputStream is = pluginJar.getInputStream( pluginDescriptorEntry );

                        pluginDescriptor = parsePluginDescriptor( is, plugin, pluginFile.getAbsolutePath() );
                    }
                }
                finally
                {
                    pluginJar.close();
                }
            }
            else
            {
                File pluginXml = new File( pluginFile, getPluginDescriptorLocation() );

                if ( pluginXml.isFile() )
                {
                    InputStream is = new BufferedInputStream( new FileInputStream( pluginXml ) );
                    try
                    {
                        pluginDescriptor = parsePluginDescriptor( is, plugin, pluginXml.getAbsolutePath() );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }
                }
            }

            if ( pluginDescriptor == null )
            {
                throw new IOException( "No plugin descriptor found at " + getPluginDescriptorLocation() );
            }
        }
        catch ( IOException e )
        {
            throw new PluginDescriptorParsingException( plugin, pluginFile.getAbsolutePath(), e );
        }

        MavenPluginValidator validator = new MavenPluginValidator( pluginArtifact );

        validator.validate( pluginDescriptor );

        if ( validator.hasErrors() )
        {
            throw new InvalidPluginDescriptorException( "Invalid plugin descriptor for " + plugin.getId() + " ("
                + pluginFile + ")", validator.getErrors() );
        }

        pluginDescriptor.setPluginArtifact( pluginArtifact );

        return pluginDescriptor;
    }

    private String getPluginDescriptorLocation()
    {
        return "META-INF/maven/plugin.xml";
    }

    private PluginDescriptor parsePluginDescriptor( InputStream is, Plugin plugin, String descriptorLocation )
        throws PluginDescriptorParsingException
    {
        try
        {
            Reader reader = ReaderFactory.newXmlReader( is );

            PluginDescriptor pluginDescriptor = builder.build( reader, descriptorLocation );

            return pluginDescriptor;
        }
        catch ( IOException e )
        {
            throw new PluginDescriptorParsingException( plugin, descriptorLocation, e );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new PluginDescriptorParsingException( plugin, descriptorLocation, e );
        }
    }

    public MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, RepositoryRequest repositoryRequest )
        throws MojoNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        InvalidPluginDescriptorException
    {
        PluginDescriptor pluginDescriptor = getPluginDescriptor( plugin, repositoryRequest );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );

        if ( mojoDescriptor == null )
        {
            throw new MojoNotFoundException( goal, pluginDescriptor );
        }

        return mojoDescriptor;
    }

    public synchronized void setupPluginRealm( PluginDescriptor pluginDescriptor, MavenSession session,
                                               ClassLoader parent, List<String> imports )
        throws PluginResolutionException, PluginContainerException
    {
        Plugin plugin = pluginDescriptor.getPlugin();

        MavenProject project = session.getCurrentProject();

        PluginRealmCache.CacheRecord cacheRecord =
            pluginRealmCache.get( plugin, parent, imports, session.getLocalRepository(),
                                  project.getPluginArtifactRepositories() );

        if ( cacheRecord != null )
        {
            pluginDescriptor.setClassRealm( cacheRecord.realm );
            pluginDescriptor.setArtifacts( new ArrayList<Artifact>( cacheRecord.artifacts ) );
        }
        else
        {
            createPluginRealm( pluginDescriptor, session, parent, imports );

            cacheRecord =
                pluginRealmCache.put( plugin, parent, imports, session.getLocalRepository(),
                                      project.getPluginArtifactRepositories(), pluginDescriptor.getClassRealm(),
                                      pluginDescriptor.getArtifacts() );
        }

        pluginRealmCache.register( project, cacheRecord );
    }

    private void createPluginRealm( PluginDescriptor pluginDescriptor, MavenSession session, ClassLoader parent,
                                    List<String> imports )
        throws PluginResolutionException, PluginContainerException
    {
        Plugin plugin = pluginDescriptor.getPlugin();

        if ( plugin == null )
        {
            throw new IllegalArgumentException( "incomplete plugin descriptor, plugin missing" );
        }

        Artifact pluginArtifact = pluginDescriptor.getPluginArtifact();

        if ( pluginArtifact == null )
        {
            throw new IllegalArgumentException( "incomplete plugin descriptor, plugin artifact missing" );
        }

        MavenProject project = session.getCurrentProject();

        RepositoryRequest request = new DefaultRepositoryRequest();
        request.setLocalRepository( session.getLocalRepository() );
        request.setRemoteRepositories( project.getPluginArtifactRepositories() );
        request.setCache( session.getRepositoryCache() );
        request.setOffline( session.isOffline() );
        request.setTransferListener( session.getRequest().getTransferListener() );

        List<Artifact> pluginArtifacts =
            resolvePluginArtifacts( plugin, pluginArtifact, request, project.getExtensionArtifactFilter() );

        ClassRealm pluginRealm = classRealmManager.createPluginRealm( plugin, parent, imports );

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Populating plugin realm for " + plugin.getId() );
        }

        List<Artifact> exposedPluginArtifacts = new ArrayList<Artifact>();

        for ( Artifact artifact : pluginArtifacts )
        {
            if ( artifact.getFile() != null )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "  Included: " + artifact.getId() );
                }

                exposedPluginArtifacts.add( artifact );

                try
                {
                    pluginRealm.addURL( artifact.getFile().toURI().toURL() );
                }
                catch ( MalformedURLException e )
                {
                    // Not going to happen
                }
            }
            else
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "  Excluded: " + artifact.getId() );
                }
            }
        }

        pluginDescriptor.setClassRealm( pluginRealm );
        pluginDescriptor.setArtifacts( exposedPluginArtifacts );

        try
        {
            for ( ComponentDescriptor<?> componentDescriptor : pluginDescriptor.getComponents() )
            {
                componentDescriptor.setRealm( pluginRealm );
                container.addComponentDescriptor( componentDescriptor );
            }

            container.discoverComponents( pluginRealm );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new PluginContainerException( plugin, pluginRealm, "Error in component graph of plugin "
                + plugin.getId() + ": " + e.getMessage(), e );
        }
        catch ( CycleDetectedInComponentGraphException e )
        {
            throw new PluginContainerException( plugin, pluginRealm, "Error in component graph of plugin "
                + plugin.getId() + ": " + e.getMessage(), e );
        }
    }

    /**
     * Gets all artifacts required for the class realm of the specified plugin. An artifact in the result list that has
     * no file set is meant to be excluded from the plugin realm in favor of the equivalent library from the current
     * core distro.
     */
    // FIXME: only exposed to allow workaround for MNG-4194
    protected List<Artifact> resolvePluginArtifacts( Plugin plugin, Artifact pluginArtifact,
                                                     RepositoryRequest repositoryRequest,
                                                     ArtifactFilter extensionArtifactFilter )
        throws PluginResolutionException
    {
        Set<Artifact> overrideArtifacts = new LinkedHashSet<Artifact>();
        for ( Dependency dependency : plugin.getDependencies() )
        {
            overrideArtifacts.add( repositorySystem.createDependencyArtifact( dependency ) );
        }

        ArtifactFilter collectionFilter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM );

        ArtifactFilter resolutionFilter = artifactFilterManager.getCoreArtifactFilter();

        if ( extensionArtifactFilter != null )
        {
            resolutionFilter = new AndArtifactFilter( Arrays.asList( resolutionFilter, extensionArtifactFilter ) );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( repositoryRequest );
        request.setArtifact( pluginArtifact );
        request.setArtifactDependencies( overrideArtifacts );
        request.setCollectionFilter( collectionFilter );
        request.setResolutionFilter( resolutionFilter );
        request.setResolveRoot( true );
        request.setResolveTransitively( true );

        ArtifactResolutionResult result = repositorySystem.resolve( request );
        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        List<Artifact> pluginArtifacts = new ArrayList<Artifact>( result.getArtifacts() );

        return pluginArtifacts;
    }

    public <T> T getConfiguredMojo( Class<T> mojoInterface, MavenSession session, MojoExecution mojoExecution )
        throws PluginConfigurationException, PluginContainerException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        ClassRealm pluginRealm = pluginDescriptor.getClassRealm();

        // We are forcing the use of the plugin realm for all lookups that might occur during
        // the lifecycle that is part of the lookup. Here we are specifically trying to keep
        // lookups that occur in contextualize calls in line with the right realm.
        ClassRealm oldLookupRealm = container.setLookupRealm( pluginRealm );
        container.setLookupRealm( pluginRealm );

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( pluginRealm );

        try
        {
            T mojo;

            try
            {
                mojo = container.lookup( mojoInterface, mojoDescriptor.getRoleHint() );
            }
            catch ( ComponentLookupException e )
            {
                Throwable cause = e.getCause();
                while ( cause != null && !( cause instanceof LinkageError )
                    && !( cause instanceof ClassNotFoundException ) )
                {
                    cause = cause.getCause();
                }

                if ( ( cause instanceof NoClassDefFoundError ) || ( cause instanceof ClassNotFoundException ) )
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream( 1024 );
                    PrintStream ps = new PrintStream( os );
                    ps.println( "Unable to load the mojo '" + mojoDescriptor.getGoal() + "' in the plugin '"
                        + pluginDescriptor.getId() + "'. A required class is missing: " + cause.getMessage() );
                    pluginRealm.display( ps );

                    throw new PluginContainerException( mojoDescriptor, pluginRealm, os.toString(), cause );
                }
                else if ( cause instanceof LinkageError )
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream( 1024 );
                    PrintStream ps = new PrintStream( os );
                    ps.println( "Unable to load the mojo '" + mojoDescriptor.getGoal() + "' in the plugin '"
                        + pluginDescriptor.getId() + "' due to an API incompatibility: " + e.getClass().getName()
                        + ": " + cause.getMessage() );
                    pluginRealm.display( ps );

                    throw new PluginContainerException( mojoDescriptor, pluginRealm, os.toString(), cause );
                }

                throw new PluginContainerException( mojoDescriptor, pluginRealm, "Unable to load the mojo '"
                    + mojoDescriptor.getGoal() + "' (or one of its required components) from the plugin '"
                    + pluginDescriptor.getId() + "'", e );
            }

            if ( mojo instanceof ContextEnabled )
            {
                MavenProject project = session.getCurrentProject();

                Map<String, Object> pluginContext = session.getPluginContext( pluginDescriptor, project );

                if ( pluginContext != null )
                {
                    pluginContext.put( "project", project );

                    pluginContext.put( "pluginDescriptor", pluginDescriptor );

                    ( (ContextEnabled) mojo ).setPluginContext( pluginContext );
                }
            }

            if ( mojo instanceof Mojo )
            {
                ( (Mojo) mojo ).setLog( new DefaultLog( logger ) );
            }

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

    private void populatePluginFields( Object mojo, MojoDescriptor mojoDescriptor, ClassRealm pluginRealm,
                                       PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        ComponentConfigurator configurator = null;

        String configuratorId = mojoDescriptor.getComponentConfigurator();

        if ( StringUtils.isEmpty( configuratorId ) )
        {
            configuratorId = "basic";
        }

        try
        {
            // TODO: could the configuration be passed to lookup and the configurator known to plexus via the descriptor
            // so that this method could entirely be handled by a plexus lookup?
            configurator = container.lookup( ComponentConfigurator.class, configuratorId );

            ConfigurationListener listener = new DebugConfigurationListener( logger );

            logger.debug( "Configuring mojo '" + mojoDescriptor.getId() + "' with " + configuratorId
                + " configurator -->" );

            configurator.configureComponent( mojo, configuration, expressionEvaluator, pluginRealm, listener );

            logger.debug( "-- end configuration --" );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(),
                                                    "Unable to parse configuration of mojo " + mojoDescriptor.getId()
                                                        + ": " + e.getMessage(), e );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(),
                                                    "Unable to retrieve component configurator " + configuratorId
                                                        + " for configuration of mojo " + mojoDescriptor.getId(), e );
        }
        catch ( NoClassDefFoundError e )
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream( 1024 );
            PrintStream ps = new PrintStream( os );
            ps.println( "A required class was missing during configuration of mojo " + mojoDescriptor.getId() + ": "
                + e.getMessage() );
            pluginRealm.display( ps );

            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(), os.toString(), e );
        }
        catch ( LinkageError e )
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream( 1024 );
            PrintStream ps = new PrintStream( os );
            ps.println( "An API incompatibility was encountered during configuration of mojo " + mojoDescriptor.getId()
                + ": " + e.getClass().getName() + ": " + e.getMessage() );
            pluginRealm.display( ps );

            throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(), os.toString(), e );
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
                    logger.debug( "Failed to release mojo configurator - ignoring." );
                }
            }
        }
    }

    public void releaseMojo( Object mojo, MojoExecution mojoExecution )
    {
        if ( mojo != null )
        {
            try
            {
                container.release( mojo );
            }
            catch ( ComponentLifecycleException e )
            {
                String goalExecId = mojoExecution.getGoal();

                if ( mojoExecution.getExecutionId() != null )
                {
                    goalExecId += " {execution: " + mojoExecution.getExecutionId() + "}";
                }

                logger.debug( "Error releasing mojo for " + goalExecId, e );
            }
        }
    }

}
