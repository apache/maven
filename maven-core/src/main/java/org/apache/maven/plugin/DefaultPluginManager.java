package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutionContext;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.version.PluginVersionManager;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
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
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultPluginManager
    extends AbstractLogEnabled
    implements PluginManager, Contextualizable
{
    private static final List RESERVED_GROUP_IDS;

    static
    {
        List rgids = new ArrayList();

        rgids.add( StateManagementUtils.GROUP_ID );

        RESERVED_GROUP_IDS = rgids;
    }

    protected MutablePlexusContainer container;

    protected PluginDescriptorBuilder pluginDescriptorBuilder;

    protected ArtifactFilterManager coreArtifactFilterManager;

    private Log mojoLogger;

    // component requirements
    protected PathTranslator pathTranslator;

    protected MavenPluginCollector pluginCollector;

    protected PluginVersionManager pluginVersionManager;

    protected ArtifactFactory artifactFactory;

    protected ArtifactResolver artifactResolver;

    protected ArtifactMetadataSource artifactMetadataSource;

    protected RuntimeInformation runtimeInformation;

    protected MavenProjectBuilder mavenProjectBuilder;

    protected PluginMappingManager pluginMappingManager;

    private BuildContextManager buildContextManager;

    // END component requirements

    public DefaultPluginManager()
    {
        pluginDescriptorBuilder = new PluginDescriptorBuilder();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
    {
        return pluginCollector.getPluginDescriptorForPrefix( prefix );
    }

    public Plugin getPluginDefinitionForPrefix( String prefix,
                                                MavenSession session,
                                                MavenProject project )
    {
        // TODO: since this is only used in the lifecycle executor, maybe it should be moved there? There is no other
        // use for the mapping manager in here
        return pluginMappingManager.getByPrefix( prefix, session.getSettings().getPluginGroups(),
                                                 project.getPluginArtifactRepositories(),
                                                 session.getLocalRepository() );
    }

    public PluginDescriptor verifyPlugin( Plugin plugin,
                                          MavenProject project,
                                          MavenSession session )
        throws ArtifactResolutionException, PluginVersionResolutionException,
        ArtifactNotFoundException, InvalidPluginException,
        PluginManagerException, PluginNotFoundException, PluginVersionNotFoundException
    {
        // TODO: this should be possibly outside
        // All version-resolution logic has been moved to DefaultPluginVersionManager.
        if ( plugin.getVersion() == null )
        {
            getLogger().debug( "Resolving version for plugin: " + plugin.getKey() );
            String version = pluginVersionManager.resolvePluginVersion( plugin.getGroupId(),
                                                                        plugin.getArtifactId(),
                                                                        project, session );
            plugin.setVersion( version );
        }

        return verifyVersionedPlugin( plugin, project, session );
    }

    private PluginDescriptor verifyVersionedPlugin( Plugin plugin,
                                                    MavenProject project,
                                                    MavenSession session )
        throws PluginVersionResolutionException, ArtifactNotFoundException,
        ArtifactResolutionException, InvalidPluginException,
        PluginManagerException, PluginNotFoundException
    {
        getLogger().debug( "In verifyVersionedPlugin for: " + plugin.getKey() );

        ArtifactRepository localRepository = session.getLocalRepository();

        // TODO: this might result in an artifact "RELEASE" being resolved continuously
        // FIXME: need to find out how a plugin gets marked as 'installed'
        // and no ChildContainer exists. The check for that below fixes
        // the 'Can't find plexus container for plugin: xxx' error.
        try
        {
            // if the groupId is internal, don't try to resolve it...
            if ( !RESERVED_GROUP_IDS.contains( plugin.getGroupId() ) )
            {
                VersionRange versionRange;
                try
                {
                    versionRange = VersionRange.createFromVersionSpec( plugin.getVersion() );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new PluginManagerException( plugin, e );
                }

                List remoteRepositories = new ArrayList();

                remoteRepositories.addAll( project.getPluginArtifactRepositories() );

                remoteRepositories.addAll( project.getRemoteArtifactRepositories() );

                checkRequiredMavenVersion( plugin, localRepository, remoteRepositories );

                Artifact pluginArtifact = artifactFactory.createPluginArtifact(
                                                                                plugin.getGroupId(),
                                                                                plugin.getArtifactId(),
                                                                                versionRange );

                pluginArtifact = project.replaceWithActiveArtifact( pluginArtifact );

                artifactResolver.resolve( pluginArtifact, project.getPluginArtifactRepositories(),
                                          localRepository );

                addPlugin( plugin, pluginArtifact, project, session );
            }
            else
            {
                getLogger().debug(
                                   "Skipping resolution for Maven built-in plugin: "
                                                   + plugin.getKey() );

                PluginDescriptor pd = pluginCollector.getPluginDescriptor( plugin );
                pd.setClassRealm( container.getContainerRealm() );
            }

            project.addPlugin( plugin );
        }
        catch ( ArtifactNotFoundException e )
        {
            String groupId = plugin.getGroupId();

            String artifactId = plugin.getArtifactId();

            String version = plugin.getVersion();

            if ( ( groupId == null ) || ( artifactId == null ) || ( version == null ) )
            {
                throw new PluginNotFoundException( plugin, e );
            }
            else if ( groupId.equals( e.getGroupId() ) && artifactId.equals( e.getArtifactId() )
                      && version.equals( e.getVersion() ) && "maven-plugin".equals( e.getType() ) )
            {
                throw new PluginNotFoundException( plugin, e );
            }
            else
            {
                throw e;
            }
        }

        PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptor( plugin );

        return pluginDescriptor;
    }

    /**
     * @todo would be better to store this in the plugin descriptor, but then it won't be available to the version
     * manager which executes before the plugin is instantiated
     */
    private void checkRequiredMavenVersion( Plugin plugin,
                                            ArtifactRepository localRepository,
                                            List remoteRepositories )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        try
        {
            Artifact artifact = artifactFactory.createProjectArtifact( plugin.getGroupId(),
                                                                       plugin.getArtifactId(),
                                                                       plugin.getVersion() );
            MavenProject project = mavenProjectBuilder.buildFromRepository( artifact,
                                                                            remoteRepositories,
                                                                            localRepository );
            // if we don't have the required Maven version, then ignore an update
            if ( ( project.getPrerequisites() != null )
                 && ( project.getPrerequisites().getMaven() != null ) )
            {
                DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion(
                                                                                     project.getPrerequisites()
                                                                                            .getMaven() );
                if ( runtimeInformation.getApplicationVersion().compareTo( requiredVersion ) < 0 )
                {
                    throw new PluginVersionResolutionException( plugin.getGroupId(),
                                                                plugin.getArtifactId(),
                                                                "Plugin requires Maven version "
                                                                                + requiredVersion );
                }
            }
        }
        catch ( ProjectBuildingException e )
        {
            throw new InvalidPluginException( "Unable to build project for plugin '"
                                              + plugin.getKey() + "': " + e.getMessage(), e );
        }
    }

    private String createPluginRealmId( String groupId, String artifactId )
    {
        return "/plugins/" + ArtifactUtils.versionlessKey( groupId, artifactId );
    }

    protected void addPlugin( Plugin plugin,
                              Artifact pluginArtifact,
                              MavenProject project,
                              MavenSession session )
        throws ArtifactNotFoundException, ArtifactResolutionException, PluginManagerException,
        InvalidPluginException
    {
        // ----------------------------------------------------------------------------
        // Get the dependencies for the Plugin
        // ----------------------------------------------------------------------------

        // the only Plugin instance which will have dependencies is the one specified in the project.
        // We need to look for a Plugin instance there, in case the instance we're using didn't come from
        // the project.
        Plugin projectPlugin = (Plugin) project.getBuild().getPluginsAsMap().get( plugin.getKey() );

        if ( projectPlugin == null )
        {
            projectPlugin = plugin;
        }

        Set artifacts = getPluginArtifacts( pluginArtifact, projectPlugin, project,
                                            session.getLocalRepository() );

        String key = projectPlugin.getKey();

        ClassRealm pluginRealm = null;

//        MavenProjectSession projectSession;
//
//        try
//        {
//            projectSession = session.getProjectSession( project );
//        }
//        catch ( PlexusContainerException e )
//        {
//            throw new PluginManagerException( plugin, "Failed to create project-specific session for project: " + project.getId()
//                                                + ".", project, e );
//        }
//

        String pluginRealmId = createPluginRealmId( projectPlugin.getGroupId(), projectPlugin.getArtifactId() );

        try
        {
//            pluginRealm = projectSession.getPluginRealm( projectPlugin );
            pluginRealm = container.getClassWorld().getRealm( pluginRealmId );
        }
        catch ( NoSuchRealmException e )
        {
            getLogger().debug( "Plugin realm is missing for: " + projectPlugin.getKey() + ". New realm will be created." );
        }

        if ( ( pluginRealm != null ) && ( pluginRealm != container.getContainerRealm() ) )
        {
            getLogger().debug(
                               "Realm already exists for: " + key
                                               + " (realm id: " + pluginRealm.getId() + "). Skipping addition..." );
            // we've already discovered this plugin, and configured it, so skip it this time.

            return;
        }

        // ----------------------------------------------------------------------------
        // Realm creation for a plugin
        // ----------------------------------------------------------------------------

        getLogger().debug( "Creating a ClassRealm instance for plugin: " + projectPlugin.getKey() + " for project: " + project.getId() );

        try
        {
//            pluginRealm = projectSession.createPluginRealm( projectPlugin );

            List jars = new ArrayList();

            jars.add( pluginArtifact.getFile() );
//            try
//            {
//                pluginRealm.addURL( pluginArtifact.getFile().toURI().toURL() );
//            }
//            catch ( MalformedURLException e )
//            {
//                throw new PluginContainerException( plugin, pluginRealm, "Error rendering plugin artifact: " + pluginArtifact.getId() + " as URL.", e );
//            }

            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

//                getLogger().debug( "Adding: " + artifact.getId() + " to plugin class-realm: " + key + " in project-session: " + project.getId() );

                getLogger().debug( "Adding: " + artifact.getId() + " to plugin class-realm: " + key );
                jars.add( artifact.getFile() );
//                try
//                {
//                    pluginRealm.addURL( artifact.getFile().toURI().toURL() );
//                }
//                catch ( MalformedURLException e )
//                {
//                    throw new PluginContainerException( plugin, pluginRealm, "Error rendering plugin artifact: " + artifact.getId() + " as URL.", e );
//                }
            }

            pluginRealm = container.createComponentRealm( pluginRealmId, jars );

            try
            {
                getLogger().debug( "Discovering components in realm: " + pluginRealm );

                container.discoverComponents( pluginRealm, false );
            }
            catch ( PlexusConfigurationException e )
            {
                throw new PluginContainerException( plugin, pluginRealm, "Error re-scanning project realm for components.", e );
            }
            catch ( ComponentRepositoryException e )
            {
                throw new PluginContainerException( plugin, pluginRealm, "Error re-scanning project realm for components.", e );
            }
        }
//        catch ( DuplicateRealmException e )
//        {
//            throw new PluginContainerException( plugin, pluginRealm, "Failed to create project-specific realm for plugin '" + projectPlugin
//                                                + " in project: " + project.getId(), e );
//        }
        catch ( PlexusContainerException e )
        {
          throw new PluginContainerException( plugin, pluginRealm,
                                                "Failed to create realm for plugin '"
                                                                + projectPlugin, e );
        }

        try
        {
            String parentRealmId = container.getContainerRealm().getId();

            // adding for MNG-3012 to try to work around problems with Xpp3Dom (from plexus-utils)
            // spawning a ClassCastException when a mojo calls plugin.getConfiguration() from maven-model...
            pluginRealm.importFrom( parentRealmId, Xpp3Dom.class.getName() );
            pluginRealm.importFrom( parentRealmId, "org.codehaus.plexus.util.xml.pull" );

            // Adding for MNG-2878, since maven-reporting-impl was removed from the
            // internal list of artifacts managed by maven, the classloader is different
            // between maven-reporting-impl and maven-reporting-api...so this resource
            // is not available from the AbstractMavenReport since it uses:
            // getClass().getResourceAsStream( "/default-report.xml" )
            // (maven-reporting-impl version 2.0; line 134; affects: checkstyle plugin, and probably others)
            pluginRealm.importFrom( parentRealmId, "/default-report.xml" );
        }
        catch ( NoSuchRealmException e )
        {
            throw new PluginContainerException( plugin, pluginRealm,
                                              "Failed to import Xpp3Dom from core realm for plugin: '"
                                                              + projectPlugin + ".", e );
        }

        // ----------------------------------------------------------------------------
        // The PluginCollector will now know about the plugin we are trying to load
        // ----------------------------------------------------------------------------

        getLogger().debug(
                           "Checking for plugin descriptor for: " + plugin.getKey()
                                           + " in collector: " + pluginCollector );

        PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptor( projectPlugin );

        if ( pluginDescriptor == null )
        {
            throw new IllegalStateException( "The PluginDescriptor for the plugin "
                                             + projectPlugin.getKey() + " was not found" );
        }

        pluginDescriptor.setPluginArtifact( pluginArtifact );

        // we're only setting the plugin's artifact itself as the artifact list, to allow it to be retrieved
        // later when the plugin is first invoked. Retrieving this artifact will in turn allow us to
        // transitively resolve its dependencies, and add them to the plugin container...
        //pluginDescriptor.setArtifacts( Collections.singletonList( pluginArtifact ) );

        //pluginDescriptor.setIntroducedDependencyArtifacts( artifacts );

        pluginDescriptor.setArtifacts( new ArrayList( artifacts ) );

        getLogger().debug( "Realm for plugin: " + plugin.getKey() + ":\n" + pluginRealm );

        pluginDescriptor.setClassRealm( pluginRealm );
    }

    private Set getPluginArtifacts( Artifact pluginArtifact,
                                    Plugin plugin,
                                    MavenProject project,
                                    ArtifactRepository localRepository )
        throws InvalidPluginException, ArtifactNotFoundException, ArtifactResolutionException
    {

        Set projectPluginDependencies;

        try
        {
            projectPluginDependencies = MavenMetadataSource.createArtifacts(
                                                                             artifactFactory,
                                                                             plugin.getDependencies(),
                                                                             null,
                                                                             coreArtifactFilterManager.getCoreArtifactFilter(),
                                                                             project );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new InvalidPluginException( "Plugin '" + plugin + "' is invalid: "
                                              + e.getMessage(), e );
        }

        ResolutionGroup resolutionGroup;

        try
        {
            resolutionGroup = artifactMetadataSource.retrieve(
                                                               pluginArtifact,
                                                               localRepository,
                                                               project.getPluginArtifactRepositories() );
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new ArtifactResolutionException(
                                                   "Unable to download metadata from repository for plugin '"
                                                                   + pluginArtifact.getId() + "': "
                                                                   + e.getMessage(),
                                                   pluginArtifact, e );
        }

        checkPlexusUtils( resolutionGroup, artifactFactory );

        Set dependencies = new LinkedHashSet();

        // resolve the plugin dependencies specified in <plugin><dependencies> first:
        dependencies.addAll( projectPluginDependencies );

        // followed by the plugin's default artifact set
        dependencies.addAll( resolutionGroup.getArtifacts() );

        List repositories = new ArrayList();

        repositories.addAll( resolutionGroup.getResolutionRepositories() );

        repositories.addAll( project.getRemoteArtifactRepositories() );

        ArtifactResolutionResult result = artifactResolver.resolveTransitively(
                                                                                dependencies,
                                                                                pluginArtifact,
                                                                                Collections.EMPTY_MAP,
                                                                                localRepository,
                                                                                repositories,
                                                                                artifactMetadataSource,
                                                                                coreArtifactFilterManager.getArtifactFilter() );

        Set resolved = new HashSet( result.getArtifacts() );

        for ( Iterator it = resolved.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( !artifact.equals( pluginArtifact ) )
            {
                artifact = project.replaceWithActiveArtifact( artifact );
            }
        }

        getLogger().debug(
                           "Using the following artifacts for classpath of: "
                                           + pluginArtifact.getId() + ":\n\n"
                                           + resolved.toString().replace( ',', '\n' ) );

        return resolved;
    }

    // ----------------------------------------------------------------------
    // Mojo execution
    // ----------------------------------------------------------------------

    public void executeMojo( MavenProject project,
                             MojoExecution mojoExecution,
                             MavenSession session )
        throws ArtifactResolutionException, MojoExecutionException, MojoFailureException,
        ArtifactNotFoundException, InvalidDependencyVersionException, PluginManagerException,
        PluginConfigurationException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        // NOTE: I'm putting these checks in here, since this is the central point of access for
        // anything that wants to execute a mojo.
        if ( mojoDescriptor.isProjectRequired() && !session.isUsingPOMsFromFilesystem() )
        {
            throw new MojoExecutionException(
                                              "Cannot execute mojo: "
                                                              + mojoDescriptor.getGoal()
                                                              + ". It requires a project with an existing pom.xml, but the build is not using one." );
        }

        if ( mojoDescriptor.isOnlineRequired() && session.getSettings().isOffline() )
        {
            // TODO: Should we error out, or simply warn and skip??
            throw new MojoExecutionException(
                                              "Mojo: "
                                                              + mojoDescriptor.getGoal()
                                                              + " requires online mode for execution. Maven is currently offline." );
        }

        if ( mojoDescriptor.isDependencyResolutionRequired() != null )
        {
            Collection projects;

            if ( mojoDescriptor.isAggregator() )
            {
                projects = session.getSortedProjects();
            }
            else
            {
                projects = Collections.singleton( project );
            }

            for ( Iterator i = projects.iterator(); i.hasNext(); )
            {
                MavenProject p = (MavenProject) i.next();

                resolveTransitiveDependencies( session, artifactResolver,
                                               mojoDescriptor.isDependencyResolutionRequired(),
                                               artifactFactory, p );
            }

            downloadDependencies( project, session, artifactResolver );
        }

        String goalName = mojoDescriptor.getFullGoalName();

        Mojo plugin;

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        Xpp3Dom dom = mojoExecution.getConfiguration();
        if ( dom != null )
        {
            // make a defensive copy, to keep things from getting polluted.
            dom = new Xpp3Dom( dom );
        }

        plugin = getConfiguredMojo( session, dom, project, false, mojoExecution );

        // Event monitoring.
        String event = MavenEvents.MOJO_EXECUTION;

        EventDispatcher dispatcher = session.getEventDispatcher();

        String goalExecId = goalName;

        if ( mojoExecution.getExecutionId() != null )
        {
            goalExecId += " {execution: " + mojoExecution.getExecutionId() + "}";
        }

        dispatcher.dispatchStart( event, goalExecId );

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

//        MavenProjectSession projectSession;
//        try
//        {
//            projectSession = session.getProjectSession( project );
//        }
//        catch ( PlexusContainerException e )
//        {
//            throw new PluginManagerException( mojoDescriptor, "Failed to create project-specific session for project: " + project.getId()
//                                                + ".", project, e );
//        }

        String pluginRealmId = createPluginRealmId( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId() );
        ClassRealm pluginRealm;
        try
        {
//            pluginRealm = projectSession.getPluginRealm( pluginDescriptor );
            pluginRealm = container.getClassWorld().getRealm( pluginRealmId );
        }
        catch ( NoSuchRealmException e )
        {
//            getLogger().debug( "Plugin realm: " + pluginDescriptor.getId() + " not found in project session for: " + project.getId() + ". Using project realm instead." );
//            pluginRealm = projectSession.getProjectRealm();

            getLogger().debug( "Plugin realm: " + pluginRealmId + " not found. Using container realm instead." );
            pluginRealm = container.getContainerRealm();
        }

        ClassRealm oldRealm = null;

        try
        {
            getLogger().debug( "Setting lookup realm and context classloader for plugin to: " + pluginRealm.getId() + " (instance is: " + pluginRealm + ")" );

            Thread.currentThread().setContextClassLoader( pluginRealm );

            oldRealm = container.setLookupRealm( pluginRealm );

            plugin.execute();

            // NEW: If the mojo that just executed is a report, store it in the LifecycleExecutionContext
            // for reference by future mojos.
            if ( plugin instanceof MavenReport )
            {
                LifecycleExecutionContext ctx = LifecycleExecutionContext.read( buildContextManager );
                if ( ctx == null )
                {
                    ctx = new LifecycleExecutionContext( project );
                }

                ctx.addReport( mojoDescriptor, (MavenReport) plugin );
                ctx.store( buildContextManager );
            }

            dispatcher.dispatchEnd( event, goalExecId );
        }
        catch ( MojoExecutionException e )
        {
            session.getEventDispatcher().dispatchError( event, goalExecId, e );

            throw e;
        }
        catch ( MojoFailureException e )
        {
            session.getEventDispatcher().dispatchError( event, goalExecId, e );

            throw e;
        }
        finally
        {
            pluginDescriptor.setClassRealm( null );
            if ( oldRealm != null )
            {
                container.setLookupRealm( oldRealm );
            }

            Thread.currentThread().setContextClassLoader( oldClassLoader );
        }
    }

    public MavenReport getReport( MavenProject project,
                                  MojoExecution mojoExecution,
                                  MavenSession session )
        throws ArtifactNotFoundException, PluginConfigurationException, PluginManagerException,
        ArtifactResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
        PluginDescriptor descriptor = mojoDescriptor.getPluginDescriptor();
        Xpp3Dom dom = project.getReportConfiguration( descriptor.getGroupId(),
                                                      descriptor.getArtifactId(),
                                                      mojoExecution.getExecutionId() );
        if ( mojoExecution.getConfiguration() != null )
        {
            dom = Xpp3Dom.mergeXpp3Dom( dom, mojoExecution.getConfiguration() );
        }

        return (MavenReport) getConfiguredMojo( session, dom, project, true, mojoExecution );
    }

    public PluginDescriptor verifyReportPlugin( ReportPlugin reportPlugin,
                                                MavenProject project,
                                                MavenSession session )
        throws PluginVersionResolutionException, ArtifactResolutionException,
        ArtifactNotFoundException, InvalidPluginException,
        PluginManagerException, PluginNotFoundException, PluginVersionNotFoundException
    {
        String version = reportPlugin.getVersion();

        if ( version == null )
        {
            version = pluginVersionManager.resolveReportPluginVersion(
                                                                       reportPlugin.getGroupId(),
                                                                       reportPlugin.getArtifactId(),
                                                                       project, session );

            reportPlugin.setVersion( version );
        }

        Plugin forLookup = new Plugin();

        forLookup.setGroupId( reportPlugin.getGroupId() );
        forLookup.setArtifactId( reportPlugin.getArtifactId() );
        forLookup.setVersion( version );

        return verifyVersionedPlugin( forLookup, project, session );
    }

    private Mojo getConfiguredMojo( MavenSession session,
                                    Xpp3Dom dom,
                                    MavenProject project,
                                    boolean report,
                                    MojoExecution mojoExecution )
        throws PluginConfigurationException, ArtifactNotFoundException, PluginManagerException,
        ArtifactResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        // if this is the first time this plugin has been used, the plugin's container will only
        // contain the plugin's artifact in isolation; we need to finish resolving the plugin's
        // dependencies, and add them to the container.
        //getPluginArtifacts( pluginDescriptor, container, project, session );

        Mojo plugin;

//        ClassRealm pluginRealm = null;
//
//        MavenProjectSession projectSession;
//        try
//        {
//            projectSession = session.getProjectSession( project );
//        }
//        catch ( PlexusContainerException e )
//        {
//            throw new PluginManagerException( mojoDescriptor, "Failed to create project-specific session for project: " + project.getId()
//                                                + ".", project, e );
//        }
//
//        try
//        {
//            pluginRealm = projectSession.getPluginRealm( pluginDescriptor );
//        }
//        catch ( NoSuchRealmException e )
//        {
//            getLogger().debug( "Plugin realm: " + pluginDescriptor.getId() + " not found in project session for: " + project.getId() + ". Using project realm instead." );
//            pluginRealm = projectSession.getProjectRealm();
//        }

        String pluginRealmId = createPluginRealmId( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId() );
        ClassRealm pluginRealm;
        try
        {
            pluginRealm = container.getClassWorld().getRealm( pluginRealmId );
        }
        catch ( NoSuchRealmException e )
        {
            getLogger().debug( "Plugin realm: " + pluginRealmId + " not found. Using container realm instead." );
            pluginRealm = container.getContainerRealm();
        }

        // We are forcing the use of the plugin realm for all lookups that might occur during
        // the lifecycle that is part of the lookup. Here we are specifically trying to keep
        // lookups that occur in contextualize calls in line with the right realm.

        ClassRealm oldRealm = container.setLookupRealm( pluginRealm );
        pluginDescriptor.setClassRealm( pluginRealm );

        getLogger().debug(
                           "Looking up mojo " + mojoDescriptor.getRoleHint() + " in realm "
                                           + pluginRealm.getId() + " - descRealmId="
                                           + mojoDescriptor.getRealmId() );

        try
        {
            plugin = (Mojo) container.lookup( Mojo.ROLE, mojoDescriptor.getRoleHint(), pluginRealm );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginContainerException( mojoDescriptor, pluginRealm, "Unable to find the mojo '"
                                              + mojoDescriptor.getRoleHint() + "' in the plugin '"
                                              + pluginDescriptor.getPluginLookupKey() + "'", e );
        }
        finally
        {
            container.setLookupRealm( oldRealm );
        }

        if ( plugin != null )
        {
            getLogger().debug(
                               "Looked up - " + plugin + " - "
                                               + plugin.getClass().getClassLoader() );
        }
        else
        {
            getLogger().warn( "No luck." );
        }

        if ( report && !( plugin instanceof MavenReport ) )
        {
            // TODO: the mojoDescriptor should actually capture this information so we don't get this far
            return null;
        }

        if ( plugin instanceof ContextEnabled )
        {
            Map pluginContext = session.getPluginContext( pluginDescriptor, project );

            pluginContext.put( "project", project );

            pluginContext.put( "pluginDescriptor", pluginDescriptor );

            ( (ContextEnabled) plugin ).setPluginContext( pluginContext );
        }

        plugin.setLog( mojoLogger );

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

        PlexusConfiguration mergedConfiguration = mergeMojoConfiguration( pomConfiguration,
                                                                          mojoDescriptor );

        // TODO: plexus changes to make this more like the component descriptor so this can be used instead
        //            PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration,
        //                                                                          mojoDescriptor.getConfiguration() );

        // NEW: Pass in the LifecycleExecutionContext so we have access to the current project,
        // forked project stack (future), and reports.
        LifecycleExecutionContext ctx = LifecycleExecutionContext.read( buildContextManager );
        if ( ctx == null )
        {
            ctx = new LifecycleExecutionContext( project );
        }

        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(
                                                                                          session,
                                                                                          mojoExecution,
                                                                                          pathTranslator,
                                                                                          ctx,
                                                                                          getLogger(),
                                                                                          session.getExecutionProperties() );

        PlexusConfiguration extractedMojoConfiguration = extractMojoConfiguration(
                                                                                   mergedConfiguration,
                                                                                   mojoDescriptor );

        checkRequiredParameters( mojoDescriptor, extractedMojoConfiguration, expressionEvaluator );

        populatePluginFields( plugin, mojoDescriptor, extractedMojoConfiguration, container,
                              expressionEvaluator );

        return plugin;
    }

    private PlexusConfiguration extractMojoConfiguration( PlexusConfiguration mergedConfiguration,
                                                          MojoDescriptor mojoDescriptor )
    {
        Map parameterMap = mojoDescriptor.getParameterMap();

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
                getLogger().debug(
                                   "*** WARNING: Configuration \'" + child.getName()
                                                   + "\' is not used in goal \'"
                                                   + mojoDescriptor.getFullGoalName()
                                                   + "; this may indicate a typo... ***" );
            }
        }

        return extractedConfiguration;
    }

    private void checkRequiredParameters( MojoDescriptor goal,
                                          PlexusConfiguration configuration,
                                          ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        // TODO: this should be built in to the configurator, as we presently double process the expressions

        List parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

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
                    throw new PluginConfigurationException( goal.getPluginDescriptor(),
                                                            e.getMessage(), e );
                }

                // only mark as invalid if there are no child nodes
                if ( ( fieldValue == null )
                     && ( ( value == null ) || ( value.getChildCount() == 0 ) ) )
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

    private void validatePomConfiguration( MojoDescriptor goal,
                                           PlexusConfiguration pomConfiguration )
        throws PluginConfigurationException
    {
        List parameters = goal.getParameters();

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

                    throw new PluginConfigurationException( goal.getPluginDescriptor(),
                                                            errorMessage.toString() );
                }

                String deprecated = parameter.getDeprecated();
                if ( StringUtils.isNotEmpty( deprecated ) )
                {
                    getLogger().warn( "DEPRECATED [" + parameter.getName() + "]: " + deprecated );
                }
            }
        }
    }

    private PlexusConfiguration mergeMojoConfiguration( XmlPlexusConfiguration fromPom,
                                                        MojoDescriptor mojoDescriptor )
    {
        XmlPlexusConfiguration result = new XmlPlexusConfiguration( fromPom.getName() );
        result.setValue( fromPom.getValue( null ) );

        if ( mojoDescriptor.getParameters() != null )
        {
            PlexusConfiguration fromMojo = mojoDescriptor.getMojoConfiguration();

            for ( Iterator it = mojoDescriptor.getParameters().iterator(); it.hasNext(); )
            {
                Parameter parameter = (Parameter) it.next();

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

                    if ( StringUtils.isNotEmpty( pomConfig.getValue( null ) )
                         || ( pomConfig.getChildCount() > 0 ) )
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
                    if ( ( implementation != null )
                         && ( toAdd.getAttribute( "implementation", null ) == null ) )
                    {

                        XmlPlexusConfiguration implementationConf = new XmlPlexusConfiguration(
                                                                                                paramName );

                        implementationConf.setAttribute( "implementation",
                                                         parameter.getImplementation() );

                        toAdd = buildTopDownMergedConfiguration( toAdd, implementationConf );
                    }

                    result.addChild( toAdd );
                }
            }
        }
        return result;
    }

    private XmlPlexusConfiguration buildTopDownMergedConfiguration( PlexusConfiguration dominant,
                                                                    PlexusConfiguration recessive )
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
            PlexusConfiguration childRec = recessive == null ? null
                            : recessive.getChild( childDom.getName(), false );

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

    private void populatePluginFields( Mojo plugin,
                                       MojoDescriptor mojoDescriptor,
                                       PlexusConfiguration configuration,
                                       PlexusContainer pluginContainer,
                                       ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        ComponentConfigurator configurator = null;

        ClassRealm pluginRealm = mojoDescriptor.getPluginDescriptor().getClassRealm();

        try
        {
            String configuratorId = mojoDescriptor.getComponentConfigurator();

            // TODO: could the configuration be passed to lookup and the configurator known to plexus via the descriptor
            // so that this meethod could entirely be handled by a plexus lookup?
            if ( StringUtils.isNotEmpty( configuratorId ) )
            {
                configurator = (ComponentConfigurator) pluginContainer.lookup( ComponentConfigurator.ROLE, configuratorId, pluginRealm );
            }
            else
            {
                configurator = (ComponentConfigurator) pluginContainer.lookup( ComponentConfigurator.ROLE, "basic", pluginRealm );
            }

            ConfigurationListener listener = new DebugConfigurationListener( getLogger() );

            getLogger().debug( "Configuring mojo '" + mojoDescriptor.getId() + "' with " + configuratorId + " configurator -->" );

            // This needs to be able to use methods
            configurator.configureComponent( plugin, configuration, expressionEvaluator, pluginRealm, listener );

            getLogger().debug( "-- end configuration --" );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new PluginConfigurationException(
                                                    mojoDescriptor.getPluginDescriptor(),
                                                    "Unable to parse the created DOM for plugin configuration",
                                                    e );
        }
        catch ( ComponentLookupException e )
        {
            throw new PluginConfigurationException(
                                                    mojoDescriptor.getPluginDescriptor(),
                                                    "Unable to retrieve component configurator for plugin configuration",
                                                    e );
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

    public static String createPluginParameterRequiredMessage( MojoDescriptor mojo,
                                                               Parameter parameter,
                                                               String expression )
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
    // LegacyLifecycle
    // ----------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (MutablePlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );

        mojoLogger = new DefaultLog( container.getLoggerManager().getLoggerForComponent( Mojo.ROLE ) );
    }

    // ----------------------------------------------------------------------
    // Artifact resolution
    // ----------------------------------------------------------------------

    private void resolveTransitiveDependencies( MavenSession context,
                                                ArtifactResolver artifactResolver,
                                                String scope,
                                                ArtifactFactory artifactFactory,
                                                MavenProject project )
        throws ArtifactResolutionException, ArtifactNotFoundException,
        InvalidDependencyVersionException
    {
        ArtifactFilter filter = new ScopeArtifactFilter( scope );

        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
        Artifact artifact = artifactFactory.createBuildArtifact( project.getGroupId(),
                                                                 project.getArtifactId(),
                                                                 project.getVersion(),
                                                                 project.getPackaging() );

        // TODO: we don't need to resolve over and over again, as long as we are sure that the parameters are the same
        // check this with yourkit as a hot spot.
        // Don't recreate if already created - for effeciency, and because clover plugin adds to it
        if ( project.getDependencyArtifacts() == null )
        {
            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
        }
        ArtifactResolutionResult result = artifactResolver.resolveTransitively(
                                                                                project.getDependencyArtifacts(),
                                                                                artifact,
                                                                                project.getManagedVersionMap(),
                                                                                context.getLocalRepository(),
                                                                                project.getRemoteArtifactRepositories(),
                                                                                artifactMetadataSource,
                                                                                filter );

        project.setArtifacts( result.getArtifacts() );
    }

    // ----------------------------------------------------------------------
    // Artifact downloading
    // ----------------------------------------------------------------------

    private void downloadDependencies( MavenProject project,
                                       MavenSession context,
                                       ArtifactResolver artifactResolver )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactRepository localRepository = context.getLocalRepository();
        List remoteArtifactRepositories = project.getRemoteArtifactRepositories();

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
        }
    }

    public Object getPluginComponent( Plugin plugin,
                                      String role,
                                      String roleHint )
        throws PluginManagerException, ComponentLookupException
    {
        ClassRealm pluginRealm = pluginCollector.getPluginDescriptor( plugin ).getClassRealm();

        if ( pluginRealm == null )
        {
            getLogger().warn( "getPluginComponent(" + plugin + ", " + role + "): descriptor is missing classRealm" );

            pluginRealm = container.getLookupRealm();
        }

        return container.lookup( role, roleHint, pluginRealm );
    }

    public Map getPluginComponents( Plugin plugin,
                                    String role )
        throws ComponentLookupException, PluginManagerException
    {
        getLogger().debug( "Looking for plugin realm: " + plugin + " using: " + pluginCollector );

        ClassRealm pluginRealm = pluginCollector.getPluginDescriptor( plugin ).getClassRealm();

        if ( pluginRealm == null )
        {
            getLogger().warn( "getPluginComponent(" + plugin + ", " + role + "): descriptor is missing classRealm" );

            pluginRealm = container.getLookupRealm();
        }

        return container.lookupMap( role, pluginRealm );
    }

    public static void checkPlexusUtils( ResolutionGroup resolutionGroup,
                                         ArtifactFactory artifactFactory )
    {
        // ----------------------------------------------------------------------------
        // If the plugin already declares a dependency on plexus-utils then we're all
        // set as the plugin author is aware of its use. If we don't have a dependency
        // on plexus-utils then we must protect users from stupid plugin authors who
        // did not declare a direct dependency on plexus-utils because the version
        // Maven uses is hidden from downstream use. We will also bump up any
        // anything below 1.1 to 1.1 as this mimics the behaviour in 2.0.5 where
        // plexus-utils 1.1 was being forced into use.
        // ----------------------------------------------------------------------------

        VersionRange vr = null;

        try
        {
            vr = VersionRange.createFromVersionSpec( "[1.1,)" );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // Won't happen
        }

        boolean plexusUtilsPresent = false;

        for ( Iterator i = resolutionGroup.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactId().equals( "plexus-utils" )
                 && vr.containsVersion( new DefaultArtifactVersion( a.getVersion() ) ) )
            {
                plexusUtilsPresent = true;

                break;
            }
        }

        if ( !plexusUtilsPresent )
        {
            // We will add plexus-utils as every plugin was getting this anyway from Maven itself. We will set the
            // version to the latest version we know that works as of the 2.0.6 release. We set the scope to runtime
            // as this is what's implicitly happening in 2.0.6.

            resolutionGroup.getArtifacts()
                           .add(
                                 artifactFactory.createArtifact( "org.codehaus.plexus",
                                                                 "plexus-utils", "1.1",
                                                                 Artifact.SCOPE_RUNTIME, "jar" ) );
        }
    }
}
