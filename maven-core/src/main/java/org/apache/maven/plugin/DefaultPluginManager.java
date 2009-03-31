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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.VersionNotFoundException;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
    protected PathTranslator pathTranslator;

    @Requirement
    protected MavenPluginCollector pluginCollector;

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    protected RuntimeInformation runtimeInformation;

    @Requirement
    protected MavenProjectBuilder mavenProjectBuilder;

    //!!jvz This is one of the last components to remove the legacy system.
    @Requirement
    protected RepositoryMetadataManager repositoryMetadataManager;    
    
    private Map<String,org.apache.maven.model.Plugin> pluginDefinitionsByPrefix = new HashMap<String,org.apache.maven.model.Plugin>();
    
    // This should be template method code for allowing subclasses to assist in contributing search/hint information
    public Plugin findPluginForPrefix( String prefix, MavenProject project, MavenSession session )
    {
        return getByPrefix( prefix, session.getPluginGroups(), project.getRemoteArtifactRepositories(), session.getLocalRepository() );
    }
    
    public PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {        
        PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptor( plugin );
            
        // There are cases where plugins are discovered but not actually populated. These are edge cases where you are working in the IDE on
        // Maven itself so this speaks to a problem we have with the system not starting entirely clean.
        if ( pluginDescriptor != null && pluginDescriptor.getClassRealm() != null )
        {
            return pluginDescriptor;
        }
                        
        try
        {         
            resolvePluginVersion( plugin, project, session );
                                     
            addPlugin( plugin, project, session );

            pluginDescriptor = pluginCollector.getPluginDescriptor( plugin );
                        
            project.addPlugin( plugin );
           
            return pluginDescriptor;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new PluginLoaderException( plugin, "Failed to load plugin. Reason: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
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
    
    protected void addPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws ArtifactNotFoundException, ArtifactResolutionException, PluginManagerException, InvalidPluginException, PluginVersionResolutionException
    {
        ArtifactRepository localRepository = session.getLocalRepository();

        MavenProject pluginProject = buildPluginProject( plugin, localRepository, project.getRemoteArtifactRepositories() );

        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin );

        checkRequiredMavenVersion( plugin, pluginProject, localRepository, project.getRemoteArtifactRepositories() );

        checkPluginDependencySpec( plugin, pluginProject );

        pluginArtifact = project.replaceWithActiveArtifact( pluginArtifact );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( pluginArtifact, localRepository, project.getRemoteArtifactRepositories() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        resolutionErrorHandler.throwErrors( request, result );

        ClassRealm pluginRealm = container.createChildRealm( pluginKey( plugin ) );

        Set<Artifact> pluginArtifacts = getPluginArtifacts( pluginArtifact, plugin, project, session.getLocalRepository() );

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
            logger.debug( "Discovering components in realm: " + pluginRealm );

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

        PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptor( plugin );
        pluginDescriptor.setPluginArtifact( pluginArtifact );
        pluginDescriptor.setArtifacts( new ArrayList<Artifact>( pluginArtifacts ) );
        pluginDescriptor.setClassRealm( pluginRealm );
    }

    // plugin artifact
    //   its dependencies while filtering out what's in the core
    //   layering on the project level plugin dependencies
    
    
    private Set<Artifact> getPluginArtifacts( Artifact pluginArtifact, Plugin plugin, MavenProject project, ArtifactRepository localRepository )
        throws InvalidPluginException, ArtifactNotFoundException, ArtifactResolutionException
    {
        AndArtifactFilter filter = new AndArtifactFilter();
        filter.add( coreArtifactFilterManager.getCoreArtifactFilter() );
        filter.add( new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM ) );
        
        Set<Artifact> projectPluginDependencies;

        // The case where we have a plugin that can host multiple versions of a particular tool. Say the 
        // Antlr plugin which has many versions and you may want the plugin to execute with version 2.7.1 of
        // Antlr versus 2.7.2. In this case the project itself would specify dependencies within the plugin
        // element.

        try
        {
            projectPluginDependencies = repositorySystem.createArtifacts( plugin.getDependencies(), null, filter, project );
        }
        catch ( VersionNotFoundException e )
        {
            InvalidDependencyVersionException ee = new InvalidDependencyVersionException( e.getProjectId(), e.getDependency(), e.getPomFile(), e.getCauseException() );
            throw new InvalidPluginException( "Plugin '" + plugin + "' is invalid: " + e.getMessage(), ee );
        }
        
        /* get plugin managed versions */
        Map<String,Artifact> pluginManagedDependencies = new HashMap<String,Artifact>();
        
        // This is really crappy that we have to do this. The repository system should deal with this. The retrieval of the transitive dependencies.
        
        List<Artifact> pluginArtifacts = new ArrayList<Artifact>();
        
        try
        {
            Artifact pluginPomArtifact = repositorySystem.createProjectArtifact( pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), pluginArtifact.getVersion() );
            
            // This does not populate the artifacts of the dependenct projects
            MavenProject pluginProject = mavenProjectBuilder.buildFromRepository( pluginPomArtifact, project.getRemoteArtifactRepositories(), localRepository );
            
            // This needs to be changed so that the resolver deals with this
            for ( Dependency d : pluginProject.getDependencies() )
            {
                pluginArtifacts.add( repositorySystem.createArtifact( d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType() ) );
            }
                        
            if ( pluginProject != null )
            {
                pluginManagedDependencies = pluginProject.getManagedVersionMap();
            }
        }
        catch ( ProjectBuildingException e )
        {
            throw new InvalidPluginException( "Error resolving plugin POM " + e.getMessage() );
        }

        Set<Artifact> dependencies = new LinkedHashSet<Artifact>();

        // resolve the plugin dependencies specified in <plugin><dependencies> first:
        dependencies.addAll( projectPluginDependencies );

        // followed by the plugin's default artifact set
        dependencies.addAll( pluginArtifacts );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( pluginArtifact )
            .setArtifactDependencies( dependencies )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( pluginManagedDependencies )
            .setFilter( filter )
            .setResolveRoot( false ); // We are setting this to false because the artifact itself has been resolved.

        ArtifactResolutionResult result = repositorySystem.resolve( request );
        resolutionErrorHandler.throwErrors( request, result );

        Set<Artifact> resolved = new LinkedHashSet<Artifact>();

        for ( Iterator<Artifact> it = result.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();

            if ( !artifact.equals( pluginArtifact ) )
            {
                artifact = project.replaceWithActiveArtifact( artifact );
            }

            resolved.add( artifact );
        }

        logger.debug( "Using the following artifacts for classpath of: " + pluginArtifact.getId() + ":\n\n" + resolved.toString().replace( ',', '\n' ) );

        return resolved;
    }

    // ----------------------------------------------------------------------
    // Mojo execution
    // ----------------------------------------------------------------------
    
    // We should assume that We've already loaded the plugin in question.
    
    public void executeMojo( MavenSession session, MojoExecution mojoExecution )
        throws MojoFailureException, PluginExecutionException, PluginConfigurationException
    {
        MavenProject project = session.getCurrentProject();
        
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( mojoDescriptor.isProjectRequired() && !session.isUsingPOMsFromFilesystem() )
        {
            throw new PluginExecutionException( mojoExecution, project, "Cannot execute mojo: " + mojoDescriptor.getGoal()
                + ". It requires a project with an existing pom.xml, but the build is not using one." );
        }

        if ( mojoDescriptor.isOnlineRequired() && session.isOffline() )
        {
            // TODO: Should we error out, or simply warn and skip??
            throw new PluginExecutionException( mojoExecution, project, "Mojo: " + mojoDescriptor.getGoal() + " requires online mode for execution. Maven is currently offline." );
        }

        if ( mojoDescriptor.getDeprecated() != null )
        {
            logger.warn( "Mojo: " + mojoDescriptor.getGoal() + " is deprecated.\n" + mojoDescriptor.getDeprecated() );
        }

        Model model = project.getModel();
        pathTranslator.alignToBaseDirectory( model, project.getBasedir() );
        project.setBuild( model.getBuild() );

        if ( mojoDescriptor.isDependencyResolutionRequired() != null )
        {
            Collection<MavenProject> projects;

            if ( mojoDescriptor.isAggregator() )
            {
                projects = session.getSortedProjects();
            }
            else
            {
                projects = Collections.singleton( project );
            }

            //!!jvz What is this? We resolveTransitiveDependencies() and then a line later downDependencies()? That can't be right. We should also already
            // know at this point that what we need to execute can't be found. This is the wrong time to find this out.
            
            try
            {
                for ( MavenProject p : projects )
                {
                    resolveTransitiveDependencies( session, repositorySystem, mojoDescriptor.isDependencyResolutionRequired(), p, mojoDescriptor.isAggregator() );
                }

                downloadDependencies( project, session, repositorySystem );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new PluginExecutionException( mojoExecution, project, e.getMessage() );                
            }
            catch ( InvalidDependencyVersionException e )
            {
                throw new PluginExecutionException( mojoExecution, project, e.getMessage() );                
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new PluginExecutionException( mojoExecution, project, e.getMessage() );                
            }
        }

        String goalName = mojoDescriptor.getFullGoalName();

        Mojo mojo = null;

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        // Here we are walking throught the POM to find plugin configuraiton that we
        // need to inject into the POM. Shane will soon take care of this.
        // Merge the plugin level configuration with the execution level configuration
        // where the latter is dominant.
                
        if ( project.getBuildPlugins() != null )
        {
            for ( Plugin buildPlugin : project.getBuildPlugins() )
            {
                // The POM builder should have merged any configuration at the upper level of the plugin
                // element with the execution level configuration where our goal is.

                // We have our plugin
                if ( buildPlugin.getArtifactId().equals( pluginDescriptor.getArtifactId() ) )
                {
                    Xpp3Dom dom = (Xpp3Dom) buildPlugin.getConfiguration();
                    
                    // Search through executions
                    for ( PluginExecution e : buildPlugin.getExecutions() )
                    {
                        // Search though goals to match what's been asked for.
                        for ( String g : e.getGoals() )
                        {
                            // This goal matches what's been asked for.                            
                            if ( g.equals( mojoDescriptor.getGoal() ) )
                            {
                                dom = Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) e.getConfiguration(), dom );
                            }
                        }
                    }
                    
                    mojoExecution.setConfiguration( dom );                    
                }
            }
        }
        
        Xpp3Dom dom = mojoExecution.getConfiguration();
        
        if ( dom != null )
        {
            try
            {
                List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
                interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties(
                session.getProjectBuilderConfiguration().getExecutionProperties(), PomInterpolatorTag.EXECUTION_PROPERTIES.name() ) );
                interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( session.getProjectBuilderConfiguration().getUserProperties(), PomInterpolatorTag.USER_PROPERTIES.name() ) );
                String interpolatedDom = interpolateXmlString( String.valueOf( dom ), interpolatorProperties );
                dom = Xpp3DomBuilder.build( new StringReader( interpolatedDom ) );
            }
            catch ( XmlPullParserException e )
            {
                throw new PluginExecutionException( mojoExecution, project, "Failed to calculate concrete state for configuration of: " + mojoDescriptor.getHumanReadableKey() );
            }
            catch ( IOException e )
            {
                throw new PluginExecutionException( mojoExecution, project, "Failed to calculate concrete state for configuration of: " + mojoDescriptor.getHumanReadableKey() );
            }
        }

        String goalExecId = goalName;
        if ( mojoExecution.getExecutionId() != null )
        {
            goalExecId += " {execution: " + mojoExecution.getExecutionId() + "}";
        }

        // by this time, the pluginDescriptor has had the correct realm setup from getConfiguredMojo(..)
        ClassRealm pluginRealm;
        ClassRealm oldLookupRealm = container.getLookupRealm();
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            mojo = getConfiguredMojo( session, dom, project, false, mojoExecution );

            pluginRealm = pluginDescriptor.getClassRealm();            
            
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

    private Mojo getConfiguredMojo( MavenSession session, Xpp3Dom dom, MavenProject project, boolean report, MojoExecution mojoExecution )
        throws PluginConfigurationException, PluginManagerException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        ClassRealm pluginRealm = pluginDescriptor.getClassRealm();
        
        // We are forcing the use of the plugin realm for all lookups that might occur during
        // the lifecycle that is part of the lookup. Here we are specifically trying to keep
        // lookups that occur in contextualize calls in line with the right realm.
        container.setLookupRealm( pluginRealm );

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( pluginRealm );
        try
        {
            logger.debug( "Looking up mojo " + mojoDescriptor.getRoleHint() + " in realm " + pluginRealm.getId() + " - descRealmId=" + mojoDescriptor.getRealm() );

            Mojo mojo;
            
            try
            {
                mojo = container.lookup( Mojo.class, mojoDescriptor.getRoleHint() );
            }
            catch ( ComponentLookupException e )
            {
                throw new PluginContainerException( mojoDescriptor, pluginRealm, "Unable to find the mojo '" + mojoDescriptor.getRoleHint() + "' in the plugin '"
                    + pluginDescriptor.getPluginLookupKey() + "'", e );
            }

            if ( mojo instanceof ContextEnabled )
            {
                Map<String,Object> pluginContext = session.getPluginContext( pluginDescriptor, project );

                if ( pluginContext != null )
                {
                    pluginContext.put( "project", project );

                    pluginContext.put( "pluginDescriptor", pluginDescriptor );

                    ( (ContextEnabled) mojo ).setPluginContext( pluginContext );
                }
            }

            mojo.setLog( new DefaultLog( logger ) );

            XmlPlexusConfiguration pomConfiguration;

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

            PlexusConfiguration mergedConfiguration = mergeMojoConfiguration( pomConfiguration, mojoDescriptor );

            // TODO: plexus changes to make this more like the component descriptor so this can be used instead
            //            PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration,
            //                                                                          mojoDescriptor.getConfiguration() );

            ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, mojoExecution, pathTranslator, logger, session.getExecutionProperties() );

            PlexusConfiguration extractedMojoConfiguration = extractMojoConfiguration( mergedConfiguration, mojoDescriptor );

            checkDeprecatedParameters( mojoDescriptor, pomConfiguration );

            checkRequiredParameters( mojoDescriptor, extractedMojoConfiguration, expressionEvaluator );

            populatePluginFields( mojo, mojoDescriptor, extractedMojoConfiguration, expressionEvaluator );

            return mojo;

        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldClassLoader );
        }
    }

    private void checkDeprecatedParameters( MojoDescriptor mojoDescriptor, PlexusConfiguration extractedMojoConfiguration )
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
                    try
                    {
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
                    }
                    catch ( PlexusConfigurationException e )
                    {
                        // forget it, this is just for deprecation checking, after all...
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

    private PlexusConfiguration extractMojoConfiguration( PlexusConfiguration mergedConfiguration, MojoDescriptor mojoDescriptor )
    {
        Map<String,Parameter> parameterMap = mojoDescriptor.getParameterMap();

        PlexusConfiguration[] mergedChildren = mergedConfiguration.getChildren();

        XmlPlexusConfiguration extractedConfiguration = new XmlPlexusConfiguration( "configuration" );

        for ( int i = 0; i < mergedChildren.length; i++ )
        {
            PlexusConfiguration child = mergedChildren[i];

            if ( parameterMap.containsKey( child.getName() ) )
            {
                extractedConfiguration.addChild( copyConfiguration( child ) );
            }
            else
            {
                // TODO: I defy anyone to find these messages in the '-X' output! Do we need a new log level?
                // ideally, this would be elevated above the true debug output, but below the default INFO level...
                // [BP] (2004-07-18): need to understand the context more but would prefer this could be either WARN or
                // removed - shouldn't need DEBUG to diagnose a problem most of the time.
                logger.debug( "*** WARNING: Configuration \'" + child.getName() + "\' is not used in goal \'" + mojoDescriptor.getFullGoalName() + "; this may indicate a typo... ***" );
            }
        }

        return extractedConfiguration;
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
            Parameter parameter = (Parameter) parameters.get( i );

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

    private PlexusConfiguration mergeMojoConfiguration( XmlPlexusConfiguration fromPom, MojoDescriptor mojoDescriptor )
    {
        XmlPlexusConfiguration result = new XmlPlexusConfiguration( fromPom.getName() );
        result.setValue( fromPom.getValue( null ) );

        if ( mojoDescriptor.getParameters() != null )
        {
            PlexusConfiguration fromMojo = mojoDescriptor.getMojoConfiguration();

            for ( Parameter parameter : mojoDescriptor.getParameters() )
            {
                String paramName = parameter.getName();
                String alias = parameter.getAlias();
                String implementation = parameter.getImplementation();

                PlexusConfiguration pomConfig = fromPom.getChild( paramName );
                PlexusConfiguration aliased = null;

                if ( alias != null )
                {
                    aliased = fromPom.getChild( alias );
                }

                PlexusConfiguration mojoConfig = fromMojo.getChild( paramName, false );

                // first we'll merge configurations from the aliased and real params.
                // TODO: Is this the right thing to do?
                if ( aliased != null )
                {
                    if ( pomConfig == null )
                    {
                        pomConfig = new XmlPlexusConfiguration( paramName );
                    }

                    pomConfig = buildTopDownMergedConfiguration( pomConfig, aliased );
                }

                PlexusConfiguration toAdd = null;

                if ( pomConfig != null )
                {
                    pomConfig = buildTopDownMergedConfiguration( pomConfig, mojoConfig );

                    if ( StringUtils.isNotEmpty( pomConfig.getValue( null ) ) || ( pomConfig.getChildCount() > 0 ) )
                    {
                        toAdd = pomConfig;
                    }
                }

                if ( ( toAdd == null ) && ( mojoConfig != null ) )
                {
                    toAdd = copyConfiguration( mojoConfig );
                }

                if ( toAdd != null )
                {
                    if ( ( implementation != null ) && ( toAdd.getAttribute( "implementation", null ) == null ) )
                    {

                        XmlPlexusConfiguration implementationConf = new XmlPlexusConfiguration( paramName );

                        implementationConf.setAttribute( "implementation", parameter.getImplementation() );

                        toAdd = buildTopDownMergedConfiguration( toAdd, implementationConf );
                    }

                    result.addChild( toAdd );
                }
            }
        }
        return result;
    }

    private XmlPlexusConfiguration buildTopDownMergedConfiguration( PlexusConfiguration dominant, PlexusConfiguration recessive )
    {
        XmlPlexusConfiguration result = new XmlPlexusConfiguration( dominant.getName() );

        String value = dominant.getValue( null );

        if ( StringUtils.isEmpty( value ) && ( recessive != null ) )
        {
            value = recessive.getValue( null );
        }

        if ( StringUtils.isNotEmpty( value ) )
        {
            result.setValue( value );
        }

        String[] attributeNames = dominant.getAttributeNames();

        for ( int i = 0; i < attributeNames.length; i++ )
        {
            String attributeValue = dominant.getAttribute( attributeNames[i], null );

            result.setAttribute( attributeNames[i], attributeValue );
        }

        if ( recessive != null )
        {
            attributeNames = recessive.getAttributeNames();

            for ( int i = 0; i < attributeNames.length; i++ )
            {
                String attributeValue = recessive.getAttribute( attributeNames[i], null );
                // TODO: recessive seems to be dominant here?
                result.setAttribute( attributeNames[i], attributeValue );
            }
        }

        PlexusConfiguration[] children = dominant.getChildren();

        for ( int i = 0; i < children.length; i++ )
        {
            PlexusConfiguration childDom = children[i];
            PlexusConfiguration childRec = recessive == null ? null : recessive.getChild( childDom.getName(), false );

            if ( childRec != null )
            {
                result.addChild( buildTopDownMergedConfiguration( childDom, childRec ) );
            }
            else
            { // FIXME: copy, or use reference?
                result.addChild( copyConfiguration( childDom ) );
            }
        }

        return result;
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

    private void populatePluginFields( Mojo plugin, MojoDescriptor mojoDescriptor, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        ComponentConfigurator configurator = null;

        // TODO: What is the point in using the plugin realm here instead of the core realm?
        ClassRealm realm = mojoDescriptor.getPluginDescriptor().getClassRealm();

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
            configurator.configureComponent( plugin, configuration, expressionEvaluator, realm, listener );

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

    public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter, String expression )
    {
        StringBuffer message = new StringBuffer();

        message.append( "The '" );
        message.append( parameter.getName() );
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
    // Artifact resolution
    // ----------------------------------------------------------------------

    protected void resolveTransitiveDependencies( MavenSession context, RepositorySystem repositorySystem, String scope, MavenProject project, boolean isAggregator )
        throws ArtifactResolutionException, ArtifactNotFoundException, InvalidDependencyVersionException
    {
        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
        Artifact artifact = repositorySystem.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging() );

        // TODO: we don't need to resolve over and over again, as long as we are sure that the parameters are the same
        // check this with yourkit as a hot spot.
        // Don't recreate if already created - for effeciency, and because clover plugin adds to it
        if ( project.getDependencyArtifacts() == null )
        {
            // NOTE: Don't worry about covering this case with the error-reporter bindings...it's already handled by the project error reporter.
            try
            {
                project.setDependencyArtifacts( repositorySystem.createArtifacts( project.getDependencies(), null, null, project ) );
            }
            catch ( VersionNotFoundException e )
            {
                throw new InvalidDependencyVersionException( e.getProjectId(), e.getDependency(), e.getPomFile(), e.getCauseException() );
            }
        }

        ArtifactFilter filter = new ScopeArtifactFilter( scope );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( false )
            .setArtifactDependencies( project.getDependencyArtifacts() )
            .setLocalRepository( context.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() )
            .setFilter( filter );

        ArtifactResolutionResult result = repositorySystem.resolve( request );
        
        resolutionErrorHandler.throwErrors( request, result );

        project.setArtifacts( result.getArtifacts() );
    }

    // ----------------------------------------------------------------------
    // Artifact downloading
    // ----------------------------------------------------------------------

    private void downloadDependencies( MavenProject project, MavenSession context, RepositorySystem repositorySystem )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactRepository localRepository = context.getLocalRepository();
        List<ArtifactRepository> remoteArtifactRepositories = project.getRemoteArtifactRepositories();

        for ( Iterator<Artifact> it = project.getArtifacts().iterator(); it.hasNext(); )
        {            
            Artifact artifact = (Artifact) it.next();
            
            repositorySystem.resolve( new ArtifactResolutionRequest( artifact, localRepository, remoteArtifactRepositories ) );
        }
    }

    private static String interpolateXmlString( String xml, List<InterpolatorProperty> interpolatorProperties )
        throws IOException
    {
        List<ModelProperty> modelProperties = ModelMarshaller.marshallXmlToModelProperties( new ByteArrayInputStream( xml.getBytes() ), ProjectUri.baseUri, PomTransformer.URIS );

        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put( "project.", "pom." );

        List<InterpolatorProperty> ips = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        ips.addAll( ModelTransformerContext.createInterpolatorProperties( modelProperties, ProjectUri.baseUri, aliases, PomInterpolatorTag.PROJECT_PROPERTIES.name(), false, false ) );

        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.properties ) && mp.getValue() != null )
            {
                String uri = mp.getUri();
                ips.add( new InterpolatorProperty( "${" + uri.substring( uri.lastIndexOf( "/" ) + 1, uri.length() ) + "}", mp.getValue() ) );
            }
        }

        ModelTransformerContext.interpolateModelProperties( modelProperties, ips );
        return ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
    }

    public void resolvePluginVersion( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {        
        String version = plugin.getVersion();
        
        if ( version != null && !Artifact.RELEASE_VERSION.equals( version ) )
        {
            return;
        }
        
        String groupId = plugin.getGroupId();
        
        String artifactId = plugin.getArtifactId();        
                
        if ( project.getBuildPlugins() != null )
        {
            for ( Plugin p : project.getBuildPlugins() )
            {
                if ( groupId.equals( p.getGroupId() ) && artifactId.equals( p.getArtifactId() ) )
                {
                    version = p.getVersion();
                }
            }
        }
        
        // final pass...retrieve the version for RELEASE and also set that resolved version as the <useVersion/>
        // in settings.xml.
        if ( StringUtils.isEmpty( version ) || Artifact.RELEASE_VERSION.equals( version ) )
        {
            // 1. resolve the version to be used
            version = resolveMetaVersion( groupId, artifactId, project, session.getLocalRepository(), Artifact.RELEASE_VERSION );
            logger.debug( "Version from RELEASE metadata: " + version );
        }

        // if we still haven't found a version, then fail early before we get into the update goop.
        if ( StringUtils.isEmpty( version ) )
        {
            throw new PluginVersionNotFoundException( groupId, artifactId );
        }

        plugin.setVersion( version );
    }

    private String resolveMetaVersion( String groupId, String artifactId, MavenProject project, ArtifactRepository localRepository, String metaVersionId )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        logger.info( "Attempting to resolve a version for plugin: " + groupId + ":" + artifactId + " using meta-version: " + metaVersionId );

        Artifact artifact = repositorySystem.createProjectArtifact( groupId, artifactId, metaVersionId );

        String version = null;

        String artifactVersion = artifact.getVersion();

        // make sure this artifact was transformed to a real version, and actually resolved to a file in the repo...
        if ( !metaVersionId.equals( artifactVersion ) && ( artifact.getFile() != null ) )
        {
            boolean pluginValid = false;

            while ( !pluginValid && ( artifactVersion != null ) )
            {
                pluginValid = true;
                
                MavenProject pluginProject;
                
                try
                {
                    artifact = repositorySystem.createProjectArtifact( groupId, artifactId, artifactVersion );

                    pluginProject = mavenProjectBuilder.buildFromRepository( artifact, project.getRemoteArtifactRepositories(), localRepository );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new InvalidPluginException( "Unable to build project information for plugin '" + ArtifactUtils.versionlessKey( groupId, artifactId ) + "': " + e.getMessage(), e );
                }
            }

            version = artifactVersion;
        }

        if ( version == null )
        {
            version = artifactVersion;
        }

        logger.info( "Using version: " + version + " of plugin: " + groupId + ":" + artifactId );

        return version;
    }

    // We need to strip out the methods in here for a validation method.
    public Artifact resolvePluginArtifact( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginManagerException, InvalidPluginException, PluginVersionResolutionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        logger.debug( "Resolving plugin artifact " + plugin.getKey() + " from " + project.getRemoteArtifactRepositories() );

        ArtifactRepository localRepository = session.getLocalRepository();

        // We need the POM for the actually plugin project so we can look at the prerequisite element.
        MavenProject pluginProject = buildPluginProject( plugin, localRepository, project.getRemoteArtifactRepositories() );

        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin );

        checkRequiredMavenVersion( plugin, pluginProject, localRepository, project.getRemoteArtifactRepositories() );

        checkPluginDependencySpec( plugin, pluginProject );

        pluginArtifact = project.replaceWithActiveArtifact( pluginArtifact );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( pluginArtifact, localRepository, project.getRemoteArtifactRepositories() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        resolutionErrorHandler.throwErrors( request, result );

        return pluginArtifact;
    }

    public MavenProject buildPluginProject( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws InvalidPluginException
    {
        Artifact artifact = repositorySystem.createProjectArtifact( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
        try
        {
            MavenProject p = mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );

            return p;
        }
        catch ( ProjectBuildingException e )
        {
            throw new InvalidPluginException( "Unable to build project for plugin '" + plugin.getKey() + "': " + e.getMessage(), e );
        }
    }

    /**
     * @param pluginProject
     * @todo would be better to store this in the plugin descriptor, but then it won't be available
     *       to the version manager which executes before the plugin is instantiated
     */
    public void checkRequiredMavenVersion( Plugin plugin, MavenProject pluginProject, ArtifactRepository localRepository, List remoteRepositories )
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

    public void checkPluginDependencySpec( Plugin plugin, MavenProject pluginProject )
        throws InvalidPluginException
    {
        ArtifactFilter filter = new ScopeArtifactFilter( "runtime" );
        try
        {
            repositorySystem.createArtifacts( pluginProject.getDependencies(), null, filter, pluginProject );
        }
        catch ( VersionNotFoundException e )
        {
            throw new InvalidPluginException( "Plugin: " + plugin.getKey() + " has a dependency with an invalid version." );
        }
    }
    
    // Plugin Mapping Manager
    
    public org.apache.maven.model.Plugin getByPrefix( String pluginPrefix, List groupIds, List pluginRepositories, ArtifactRepository localRepository )
    {
        // if not found, try from the remote repository
        if ( !pluginDefinitionsByPrefix.containsKey( pluginPrefix ) )
        {
            logger.info( "Searching repository for plugin with prefix: \'" + pluginPrefix + "\'." );

            loadPluginMappings( groupIds, pluginRepositories, localRepository );
        }

        org.apache.maven.model.Plugin result = (org.apache.maven.model.Plugin) pluginDefinitionsByPrefix.get( pluginPrefix );

        if ( result == null )
        {
            logger.debug( "Failed to resolve plugin from prefix: " + pluginPrefix, new Throwable() );
        }

        return result;
    }

    private void loadPluginMappings( List groupIds, List pluginRepositories, ArtifactRepository localRepository )
    {
        List pluginGroupIds = new ArrayList( groupIds );

        // TODO: use constant
        if ( !pluginGroupIds.contains( "org.apache.maven.plugins" ) )
        {
            pluginGroupIds.add( "org.apache.maven.plugins" );
        }
        if ( !pluginGroupIds.contains( "org.codehaus.mojo" ) )
        {
            pluginGroupIds.add( "org.codehaus.mojo" );
        }

        for ( Iterator it = pluginGroupIds.iterator(); it.hasNext(); )
        {
            String groupId = (String) it.next();
            logger.debug( "Loading plugin prefixes from group: " + groupId );
            try
            {
                loadPluginMappings( groupId, pluginRepositories, localRepository );
            }
            catch ( RepositoryMetadataResolutionException e )
            {
                logger.warn( "Cannot resolve plugin-mapping metadata for groupId: " + groupId + " - IGNORING." );

                logger.debug( "Error resolving plugin-mapping metadata for groupId: " + groupId + ".", e );
            }
        }
    }

    //!!jvz This should not be here, it's part of pre-processing.
    private void loadPluginMappings( String groupId, List pluginRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataResolutionException
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( groupId );

        logger.debug( "Checking repositories:\n" + pluginRepositories + "\n\nfor plugin prefix metadata: " + groupId );
        
        repositoryMetadataManager.resolve( metadata, pluginRepositories, localRepository );

        Metadata repoMetadata = metadata.getMetadata();
        
        if ( repoMetadata != null )
        {
            for ( Iterator pluginIterator = repoMetadata.getPlugins().iterator(); pluginIterator.hasNext(); )
            {
                org.apache.maven.artifact.repository.metadata.Plugin mapping = (org.apache.maven.artifact.repository.metadata.Plugin) pluginIterator.next();
                
                logger.debug( "Found plugin: " + mapping.getName() + " with prefix: " + mapping.getPrefix() );

                String prefix = mapping.getPrefix();

                //if the prefix has already been found, don't add it again.
                //this is to preserve the correct ordering of prefix searching (MNG-2926)
                if ( !pluginDefinitionsByPrefix.containsKey( prefix ) )
                {
                    String artifactId = mapping.getArtifactId();

                    org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

                    plugin.setGroupId( metadata.getGroupId() );

                    plugin.setArtifactId( artifactId );

                    pluginDefinitionsByPrefix.put( prefix, plugin );
                }
            }
        }
    }           
    
    public MojoDescriptor getMojoDescriptor( Plugin plugin, String goal, MavenSession session )
        throws PluginLoaderException
    {
        PluginDescriptor pluginDescriptor =  loadPlugin( plugin, session.getCurrentProject(), session );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
        
        return mojoDescriptor;
    }
}
