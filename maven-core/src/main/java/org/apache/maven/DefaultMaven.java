package org.apache.maven;

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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleStarter;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;
import org.apache.maven.repository.LocalRepositoryNotAccessibleException;
import org.apache.maven.repository.mirror.RoutingMirrorSelector;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.aether.ConfigurationProperties;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencyManager;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.collection.DependencyTraverser;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.graph.manager.ClassicDependencyManager;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.ConflictMarker;
import org.sonatype.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;
import org.sonatype.aether.util.graph.transformer.NearestVersionConflictResolver;
import org.sonatype.aether.util.graph.traverser.FatArtifactTraverser;
import org.sonatype.aether.util.repository.ChainedWorkspaceReader;
import org.sonatype.aether.util.repository.DefaultAuthenticationSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jason van Zyl
 */
@Component( role = Maven.class )
public class DefaultMaven
    implements Maven
{

    @Requirement
    private Logger logger;

    @Requirement
    protected ProjectBuilder projectBuilder;

    @Requirement
    private LifecycleStarter lifecycleStarter;

    @Requirement
    protected PlexusContainer container;

    @Requirement
    MavenExecutionRequestPopulator populator;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Requirement( optional = true, hint = "ide" )
    private WorkspaceReader workspaceRepository;

    @Requirement
    private RepositorySystem repoSystem;

    @Requirement
    private SettingsDecrypter settingsDecrypter;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private EventSpyDispatcher eventSpyDispatcher;

    public MavenExecutionResult execute( final MavenExecutionRequest request )
    {
        MavenExecutionResult result;

        try
        {
            result = doExecute( populator.populateDefaults( request ) );
        }
        catch ( final OutOfMemoryError e )
        {
            result = processResult( new DefaultMavenExecutionResult(), e );
        }
        catch ( final MavenExecutionRequestPopulationException e )
        {
            result = processResult( new DefaultMavenExecutionResult(), e );
        }
        catch ( final RuntimeException e )
        {
            result =
                processResult( new DefaultMavenExecutionResult(),
                               new InternalErrorException( "Internal error: " + e, e ) );
        }
        finally
        {
            legacySupport.setSession( null );
        }

        return result;
    }

    @SuppressWarnings( { "ThrowableInstanceNeverThrown", "ThrowableResultOfMethodCallIgnored" } )
    private MavenExecutionResult doExecute( final MavenExecutionRequest request )
    {
        // TODO: Need a general way to inject standard properties
        if ( request.getStartTime() != null )
        {
            request.getSystemProperties()
                   .put( "${build.timestamp}", new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( request.getStartTime() ) );
        }

        request.setStartTime( new Date() );

        final MavenExecutionResult result = new DefaultMavenExecutionResult();

        try
        {
            validateLocalRepository( request );
        }
        catch ( final LocalRepositoryNotAccessibleException e )
        {
            return processResult( result, e );
        }

        final DelegatingLocalArtifactRepository delegatingLocalArtifactRepository =
            new DelegatingLocalArtifactRepository( request.getLocalRepository() );

        request.setLocalRepository( delegatingLocalArtifactRepository );

        final DefaultRepositorySystemSession repoSession =
            (DefaultRepositorySystemSession) newRepositorySession( request );

        final MavenSession session = new MavenSession( container, repoSession, request, result );
        legacySupport.setSession( session );

        try
        {
            for ( final AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( Collections.<MavenProject> emptyList() ) )
            {
                listener.afterSessionStart( session );
            }
        }
        catch ( final MavenExecutionException e )
        {
            return processResult( result, e );
        }

        eventCatapult.fire( ExecutionEvent.Type.ProjectDiscoveryStarted, session, null );

        request.getProjectBuildingRequest().setRepositorySession( session.getRepositorySession() );

        // TODO: optimize for the single project or no project

        List<MavenProject> projects;
        try
        {
            projects = getProjectsForMavenReactor( request );
        }
        catch ( final ProjectBuildingException e )
        {
            return processResult( result, e );
        }

        session.setProjects( projects );

        result.setTopologicallySortedProjects( session.getProjects() );

        result.setProject( session.getTopLevelProject() );

        try
        {
            Map<String, MavenProject> projectMap;
            projectMap = getProjectMap( session.getProjects() );

            // Desired order of precedence for local artifact repositories
            //
            // Reactor
            // Workspace
            // User Local Repository
            final ReactorReader reactorRepository = new ReactorReader( projectMap );

            repoSession.setWorkspaceReader( ChainedWorkspaceReader.newInstance( reactorRepository,
                                                                                repoSession.getWorkspaceReader() ) );
        }
        catch ( final org.apache.maven.DuplicateProjectException e )
        {
            return processResult( result, e );
        }

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            for ( final AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( projects ) )
            {
                Thread.currentThread().setContextClassLoader( listener.getClass().getClassLoader() );

                listener.afterProjectsRead( session );
            }
        }
        catch ( final MavenExecutionException e )
        {
            return processResult( result, e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }

        try
        {
            final ProjectSorter projectSorter = new ProjectSorter( session.getProjects() );

            final ProjectDependencyGraph projectDependencyGraph = createDependencyGraph( projectSorter, request );

            session.setProjects( projectDependencyGraph.getSortedProjects() );

            session.setProjectDependencyGraph( projectDependencyGraph );
        }
        catch ( final CycleDetectedException e )
        {
            final String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();

            final ProjectCycleException error = new ProjectCycleException( message, e );

            return processResult( result, error );
        }
        catch ( final DuplicateProjectException e )
        {
            return processResult( result, e );
        }
        catch ( final MavenExecutionException e )
        {
            return processResult( result, e );
        }

        result.setTopologicallySortedProjects( session.getProjects() );

        if ( result.hasExceptions() )
        {
            return result;
        }

        lifecycleStarter.execute( session );

        validateActivatedProfiles( session.getProjects(), request.getActiveProfiles() );

        if ( session.getResult().hasExceptions() )
        {
            return processResult( result, session.getResult().getExceptions().get( 0 ) );
        }

        return result;
    }

    public RepositorySystemSession newRepositorySession( final MavenExecutionRequest request )
    {
        final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        session.setCache( request.getRepositoryCache() );

        session.setIgnoreInvalidArtifactDescriptor( true ).setIgnoreMissingArtifactDescriptor( true );

        final Map<Object, Object> configProps = new LinkedHashMap<Object, Object>();
        configProps.put( ConfigurationProperties.USER_AGENT, getUserAgent() );
        configProps.put( ConfigurationProperties.INTERACTIVE, Boolean.valueOf( request.isInteractiveMode() ) );
        configProps.putAll( request.getSystemProperties() );
        configProps.putAll( request.getUserProperties() );

        session.setOffline( request.isOffline() );
        session.setChecksumPolicy( request.getGlobalChecksumPolicy() );
        session.setUpdatePolicy( request.isUpdateSnapshots() ? RepositoryPolicy.UPDATE_POLICY_ALWAYS : null );

        session.setNotFoundCachingEnabled( request.isCacheNotFound() );
        session.setTransferErrorCachingEnabled( request.isCacheTransferError() );

        session.setArtifactTypeRegistry( RepositoryUtils.newArtifactTypeRegistry( artifactHandlerManager ) );

        final LocalRepository localRepo = new LocalRepository( request.getLocalRepository().getBasedir() );
        session.setLocalRepositoryManager( repoSystem.newLocalRepositoryManager( localRepo ) );

        if ( request.getWorkspaceReader() != null )
        {
            session.setWorkspaceReader( request.getWorkspaceReader() );
        }
        else
        {
            session.setWorkspaceReader( workspaceRepository );
        }

        final DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies( request.getProxies() );
        decrypt.setServers( request.getServers() );
        final SettingsDecryptionResult decrypted = settingsDecrypter.decrypt( decrypt );

        if ( logger.isDebugEnabled() )
        {
            for ( final SettingsProblem problem : decrypted.getProblems() )
            {
                logger.debug( problem.getMessage(), problem.getException() );
            }
        }

        final RoutingMirrorSelector mirrorSelector =
            new RoutingMirrorSelector( request.getMirrorRoutingTable(), logger );
        
        for ( final Mirror mirror : request.getMirrors() )
        {
            mirrorSelector.add( mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                                mirror.getMirrorOfLayouts() );
        }
        
        session.setMirrorSelector( mirrorSelector );

        final DefaultProxySelector proxySelector = new DefaultProxySelector();
        for ( final Proxy proxy : decrypted.getProxies() )
        {
            final Authentication proxyAuth = new Authentication( proxy.getUsername(), proxy.getPassword() );
            proxySelector.add( new org.sonatype.aether.repository.Proxy( proxy.getProtocol(), proxy.getHost(),
                                                                         proxy.getPort(), proxyAuth ),
                               proxy.getNonProxyHosts() );
        }
        session.setProxySelector( proxySelector );

        final DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for ( final Server server : decrypted.getServers() )
        {
            final Authentication auth =
                new Authentication( server.getUsername(), server.getPassword(), server.getPrivateKey(),
                                    server.getPassphrase() );
            authSelector.add( server.getId(), auth );

            if ( server.getConfiguration() != null )
            {
                final Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for ( int i = dom.getChildCount() - 1; i >= 0; i-- )
                {
                    final Xpp3Dom child = dom.getChild( i );
                    if ( "wagonProvider".equals( child.getName() ) )
                    {
                        dom.removeChild( i );
                    }
                }

                final XmlPlexusConfiguration config = new XmlPlexusConfiguration( dom );
                configProps.put( "aether.connector.wagon.config." + server.getId(), config );
            }

            configProps.put( "aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions() );
            configProps.put( "aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions() );
        }
        session.setAuthenticationSelector( authSelector );

        final DependencyTraverser depTraverser = new FatArtifactTraverser();
        session.setDependencyTraverser( depTraverser );

        final DependencyManager depManager = new ClassicDependencyManager();
        session.setDependencyManager( depManager );

        final DependencySelector depFilter =
            new AndDependencySelector( new ScopeDependencySelector( "test", "provided" ),
                                       new OptionalDependencySelector(), new ExclusionDependencySelector() );
        session.setDependencySelector( depFilter );

        final DependencyGraphTransformer transformer =
            new ChainedDependencyGraphTransformer( new ConflictMarker(), new JavaEffectiveScopeCalculator(),
                                                   new NearestVersionConflictResolver(),
                                                   new JavaDependencyContextRefiner() );
        session.setDependencyGraphTransformer( transformer );

        session.setTransferListener( request.getTransferListener() );

        session.setRepositoryListener( eventSpyDispatcher.chainListener( new LoggingRepositoryListener( logger ) ) );

        session.setUserProps( request.getUserProperties() );
        session.setSystemProps( request.getSystemProperties() );
        session.setConfigProps( configProps );

        return session;
    }

    private String getUserAgent()
    {
        final StringBuilder buffer = new StringBuilder( 128 );

        buffer.append( "Apache-Maven/" ).append( getMavenVersion() );
        buffer.append( " (" );
        buffer.append( "Java " ).append( System.getProperty( "java.version" ) );
        buffer.append( "; " );
        buffer.append( System.getProperty( "os.name" ) ).append( " " ).append( System.getProperty( "os.version" ) );
        buffer.append( ")" );

        return buffer.toString();
    }

    private String getMavenVersion()
    {
        final Properties props = new Properties();

        final InputStream is =
            getClass().getResourceAsStream( "/META-INF/maven/org.apache.maven/maven-core/pom.properties" );
        if ( is != null )
        {
            try
            {
                props.load( is );
            }
            catch ( final IOException e )
            {
                logger.debug( "Failed to read Maven version", e );
            }
            IOUtil.close( is );
        }

        return props.getProperty( "version", "unknown-version" );
    }

    @SuppressWarnings( { "ResultOfMethodCallIgnored" } )
    private void validateLocalRepository( final MavenExecutionRequest request )
        throws LocalRepositoryNotAccessibleException
    {
        final File localRepoDir = request.getLocalRepositoryPath();

        logger.debug( "Using local repository at " + localRepoDir );

        localRepoDir.mkdirs();

        if ( !localRepoDir.isDirectory() )
        {
            throw new LocalRepositoryNotAccessibleException( "Could not create local repository at " + localRepoDir );
        }
    }

    private Collection<AbstractMavenLifecycleParticipant> getLifecycleParticipants( final Collection<MavenProject> projects )
    {
        final Collection<AbstractMavenLifecycleParticipant> lifecycleListeners =
            new LinkedHashSet<AbstractMavenLifecycleParticipant>();

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            try
            {
                lifecycleListeners.addAll( container.lookupList( AbstractMavenLifecycleParticipant.class ) );
            }
            catch ( final ComponentLookupException e )
            {
                // this is just silly, lookupList should return an empty list!
                logger.warn( "Failed to lookup lifecycle participants: " + e.getMessage() );
            }

            final Collection<ClassLoader> scannedRealms = new HashSet<ClassLoader>();

            for ( final MavenProject project : projects )
            {
                final ClassLoader projectRealm = project.getClassRealm();

                if ( projectRealm != null && scannedRealms.add( projectRealm ) )
                {
                    Thread.currentThread().setContextClassLoader( projectRealm );

                    try
                    {
                        lifecycleListeners.addAll( container.lookupList( AbstractMavenLifecycleParticipant.class ) );
                    }
                    catch ( final ComponentLookupException e )
                    {
                        // this is just silly, lookupList should return an empty list!
                        logger.warn( "Failed to lookup lifecycle participants: " + e.getMessage() );
                    }
                }
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }

        return lifecycleListeners;
    }

    private MavenExecutionResult processResult( final MavenExecutionResult result, final Throwable e )
    {
        if ( !result.getExceptions().contains( e ) )
        {
            result.addException( e );
        }

        return result;
    }

    private List<MavenProject> getProjectsForMavenReactor( final MavenExecutionRequest request )
        throws ProjectBuildingException
    {
        final List<MavenProject> projects = new ArrayList<MavenProject>();

        // We have no POM file.
        //
        if ( request.getPom() == null )
        {
            final ModelSource modelSource =
                new UrlModelSource( DefaultMaven.class.getResource( "project/standalone.xml" ) );
            final MavenProject project =
                projectBuilder.build( modelSource, request.getProjectBuildingRequest() ).getProject();
            project.setExecutionRoot( true );
            projects.add( project );
            request.setProjectPresent( false );
            return projects;
        }

        final List<File> files = Arrays.asList( request.getPom().getAbsoluteFile() );
        collectProjects( projects, files, request );
        return projects;
    }

    private Map<String, MavenProject> getProjectMap( final List<MavenProject> projects )
        throws org.apache.maven.DuplicateProjectException
    {
        final Map<String, MavenProject> index = new LinkedHashMap<String, MavenProject>();
        final Map<String, List<File>> collisions = new LinkedHashMap<String, List<File>>();

        for ( final MavenProject project : projects )
        {
            final String projectId =
                ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );

            final MavenProject collision = index.get( projectId );

            if ( collision == null )
            {
                index.put( projectId, project );
            }
            else
            {
                List<File> pomFiles = collisions.get( projectId );

                if ( pomFiles == null )
                {
                    pomFiles = new ArrayList<File>( Arrays.asList( collision.getFile(), project.getFile() ) );
                    collisions.put( projectId, pomFiles );
                }
                else
                {
                    pomFiles.add( project.getFile() );
                }
            }
        }

        if ( !collisions.isEmpty() )
        {
            throw new org.apache.maven.DuplicateProjectException( "Two or more projects in the reactor"
                            + " have the same identifier, please make sure that <groupId>:<artifactId>:<version>"
                            + " is unique for each project: " + collisions, collisions );
        }

        return index;
    }

    private void collectProjects( final List<MavenProject> projects, final List<File> files,
                                  final MavenExecutionRequest request )
        throws ProjectBuildingException
    {
        final ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        final List<ProjectBuildingResult> results =
            projectBuilder.build( files, request.isRecursive(), projectBuildingRequest );

        boolean problems = false;

        for ( final ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );

            if ( !result.getProblems().isEmpty() && logger.isWarnEnabled() )
            {
                logger.warn( "" );
                logger.warn( "Some problems were encountered while building the effective model for "
                                + result.getProject().getId() );

                for ( final ModelProblem problem : result.getProblems() )
                {
                    final String location = ModelProblemUtils.formatLocation( problem, result.getProjectId() );
                    logger.warn( problem.getMessage() + ( StringUtils.isNotEmpty( location ) ? " @ " + location : "" ) );
                }

                problems = true;
            }
        }

        if ( problems )
        {
            logger.warn( "" );
            logger.warn( "It is highly recommended to fix these problems"
                            + " because they threaten the stability of your build." );
            logger.warn( "" );
            logger.warn( "For this reason, future Maven versions might no"
                            + " longer support building such malformed projects." );
            logger.warn( "" );
        }
    }

    private void validateActivatedProfiles( final List<MavenProject> projects, final List<String> activeProfileIds )
    {
        final Collection<String> notActivatedProfileIds = new LinkedHashSet<String>( activeProfileIds );

        for ( final MavenProject project : projects )
        {
            for ( final List<String> profileIds : project.getInjectedProfileIds().values() )
            {
                notActivatedProfileIds.removeAll( profileIds );
            }
        }

        for ( final String notActivatedProfileId : notActivatedProfileIds )
        {
            logger.warn( "The requested profile \"" + notActivatedProfileId
                            + "\" could not be activated because it does not exist." );
        }
    }

    protected Logger getLogger()
    {
        return logger;
    }

    private ProjectDependencyGraph createDependencyGraph( final ProjectSorter sorter,
                                                          final MavenExecutionRequest request )
        throws MavenExecutionException
    {
        ProjectDependencyGraph graph = new DefaultProjectDependencyGraph( sorter );

        List<MavenProject> activeProjects = sorter.getSortedProjects();

        activeProjects = trimSelectedProjects( activeProjects, graph, request );
        activeProjects = trimResumedProjects( activeProjects, request );

        if ( activeProjects.size() != sorter.getSortedProjects().size() )
        {
            graph = new FilteredProjectDependencyGraph( graph, activeProjects );
        }

        return graph;
    }

    private List<MavenProject> trimSelectedProjects( final List<MavenProject> projects,
                                                     final ProjectDependencyGraph graph,
                                                     final MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( !request.getSelectedProjects().isEmpty() )
        {
            File reactorDirectory = null;
            if ( request.getBaseDirectory() != null )
            {
                reactorDirectory = new File( request.getBaseDirectory() );
            }

            final Collection<MavenProject> selectedProjects = new LinkedHashSet<MavenProject>( projects.size() );

            for ( final String selector : request.getSelectedProjects() )
            {
                MavenProject selectedProject = null;

                for ( final MavenProject project : projects )
                {
                    if ( isMatchingProject( project, selector, reactorDirectory ) )
                    {
                        selectedProject = project;
                        break;
                    }
                }

                if ( selectedProject != null )
                {
                    selectedProjects.add( selectedProject );
                }
                else
                {
                    throw new MavenExecutionException( "Could not find the selected project in the reactor: "
                                    + selector, request.getPom() );
                }
            }

            boolean makeUpstream = false;
            boolean makeDownstream = false;

            if ( MavenExecutionRequest.REACTOR_MAKE_UPSTREAM.equals( request.getMakeBehavior() ) )
            {
                makeUpstream = true;
            }
            else if ( MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM.equals( request.getMakeBehavior() ) )
            {
                makeDownstream = true;
            }
            else if ( MavenExecutionRequest.REACTOR_MAKE_BOTH.equals( request.getMakeBehavior() ) )
            {
                makeUpstream = true;
                makeDownstream = true;
            }
            else if ( StringUtils.isNotEmpty( request.getMakeBehavior() ) )
            {
                throw new MavenExecutionException( "Invalid reactor make behavior: " + request.getMakeBehavior(),
                                                   request.getPom() );
            }

            if ( makeUpstream || makeDownstream )
            {
                for ( final MavenProject selectedProject : new ArrayList<MavenProject>( selectedProjects ) )
                {
                    if ( makeUpstream )
                    {
                        selectedProjects.addAll( graph.getUpstreamProjects( selectedProject, true ) );
                    }
                    if ( makeDownstream )
                    {
                        selectedProjects.addAll( graph.getDownstreamProjects( selectedProject, true ) );
                    }
                }
            }

            result = new ArrayList<MavenProject>( selectedProjects.size() );

            for ( final MavenProject project : projects )
            {
                if ( selectedProjects.contains( project ) )
                {
                    result.add( project );
                }
            }
        }

        return result;
    }

    private List<MavenProject> trimResumedProjects( final List<MavenProject> projects,
                                                    final MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( StringUtils.isNotEmpty( request.getResumeFrom() ) )
        {
            File reactorDirectory = null;
            if ( request.getBaseDirectory() != null )
            {
                reactorDirectory = new File( request.getBaseDirectory() );
            }

            final String selector = request.getResumeFrom();

            result = new ArrayList<MavenProject>( projects.size() );

            boolean resumed = false;

            for ( final MavenProject project : projects )
            {
                if ( !resumed && isMatchingProject( project, selector, reactorDirectory ) )
                {
                    resumed = true;
                }

                if ( resumed )
                {
                    result.add( project );
                }
            }

            if ( !resumed )
            {
                throw new MavenExecutionException( "Could not find project to resume reactor build from: " + selector
                                + " vs " + projects, request.getPom() );
            }
        }

        return result;
    }

    private boolean isMatchingProject( final MavenProject project, final String selector, final File reactorDirectory )
    {
        // [groupId]:artifactId
        if ( selector.indexOf( ':' ) >= 0 )
        {
            String id = ':' + project.getArtifactId();

            if ( id.equals( selector ) )
            {
                return true;
            }

            id = project.getGroupId() + id;

            if ( id.equals( selector ) )
            {
                return true;
            }
        }

        // relative path, e.g. "sub", "../sub" or "."
        else if ( reactorDirectory != null )
        {
            final File selectedProject = new File( new File( reactorDirectory, selector ).toURI().normalize() );

            if ( selectedProject.isFile() )
            {
                return selectedProject.equals( project.getFile() );
            }
            else if ( selectedProject.isDirectory() )
            {
                return selectedProject.equals( project.getBasedir() );
            }
        }

        return false;
    }

}
