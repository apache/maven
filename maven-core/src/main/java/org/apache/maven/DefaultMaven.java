package org.apache.maven;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.LocalRepositoryNotAccessibleException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
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
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ChainedWorkspaceReader;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

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

    @Requirement( optional = true, hint = "simple" )
    private LocalRepositoryManagerFactory simpleLocalRepositoryManagerFactory;

    @Requirement
    private SettingsDecrypter settingsDecrypter;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private EventSpyDispatcher eventSpyDispatcher;

    @Requirement
    private SessionScope sessionScope;
        
    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        MavenExecutionResult result;

        try
        {
            result = doExecute( populator.populateDefaults( request ) );
        }
        catch ( OutOfMemoryError e )
        {
            result = addExceptionToResult( new DefaultMavenExecutionResult(), e );
        }
        catch ( MavenExecutionRequestPopulationException e )
        {
            result = addExceptionToResult( new DefaultMavenExecutionResult(), e );
        }
        catch ( RuntimeException e )
        {
            result =
                addExceptionToResult( new DefaultMavenExecutionResult(), new InternalErrorException( "Internal error: "
                    + e, e ) );
        }
        finally
        {
            legacySupport.setSession( null );
        }

        return result;
    }

    // 
    // 1) Setup initial properties.
    //
    // 2) Validate local repository directory is accessible.
    //
    // 3) Create RepositorySystemSession.
    //
    // 4) Create MavenSession.
    //
    // 5) Execute AbstractLifecycleParticipant.afterSessionStart(session)
    //
    // 6) Get reactor projects looking for general POM errors
    //
    // 7) Create ProjectDependencyGraph using trimming which takes into account --projects and reactor mode. This ensures
    //    that the projects passed into the ReactorReader are only those specified.
    //
    // 8) Create ReactorReader with the getProjectMap( projects ). NOTE that getProjectMap(projects) is the code that
    //    checks for duplicate projects definitions in the build. Ideally this type of duplicate checking should be part of
    //    getting the reactor projects in 6). The duplicate checking is conflated with getProjectMap(projects).
    //
    // 9) Execute AbstractLifecycleParticipant.afterProjectsRead(session)
    //
    // 10) Create ProjectDependencyGraph without trimming (as trimming was done in 7). A new topological sort is required after
    //     the execution of 9) as the AbstractLifecycleParticipants are free to mutate the MavenProject instances, which may change
    //     dependencies which can, in turn, affect the build order.
    // 
    // 11) Execute LifecycleStarter.start()
    //    
    private MavenExecutionResult doExecute( MavenExecutionRequest request )
    {
        if ( request.getStartTime() != null )
        {
            request.getSystemProperties().put( "${build.timestamp}",
                                               new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( request.getStartTime() ) );
        }

        request.setStartTime( new Date() );

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        try
        {
            validateLocalRepository( request );
        }
        catch ( LocalRepositoryNotAccessibleException e )
        {
            return addExceptionToResult( result, e );
        }

        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) newRepositorySession( request );

        MavenSession session = new MavenSession( container, repoSession, request, result );
        legacySupport.setSession( session );

        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( Collections.<MavenProject> emptyList() ) )
            {
                listener.afterSessionStart( session );
            }
        }
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }

        eventCatapult.fire( ExecutionEvent.Type.ProjectDiscoveryStarted, session, null );

        List<MavenProject> projects;
        try
        {
            projects = getProjectsForMavenReactor( session );
            //
            // Capture the full set of projects before any potential constraining is performed by --projects
            //
            session.setAllProjects( projects );
        }
        catch ( ProjectBuildingException e )
        {
            return addExceptionToResult( result, e );
        }

        validateProjects( projects );

        //
        // This creates the graph and trims the projects down based on the user request using something like:
        //
        // -pl project0,project2 eclipse:eclipse
        //
        ProjectDependencyGraph projectDependencyGraph = createProjectDependencyGraph( projects, request, result, true );

        if ( result.hasExceptions() )
        {
            return result;
        }

        session.setProjects( projectDependencyGraph.getSortedProjects() );

        try
        {
            session.setProjectMap( getProjectMap( session.getProjects() ) );
        }
        catch ( DuplicateProjectException e )
        {
            return addExceptionToResult( result, e );
        }
        
        WorkspaceReader reactorWorkspace;
        sessionScope.enter();
        sessionScope.seed( MavenSession.class, session );
        try
        {
            reactorWorkspace = container.lookup( WorkspaceReader.class, ReactorReader.HINT );
        }
        catch ( ComponentLookupException e )
        {
            return addExceptionToResult( result, e );
        }
        
        //
        // Desired order of precedence for local artifact repositories
        //
        // Reactor
        // Workspace
        // User Local Repository
        //        
        repoSession.setWorkspaceReader( ChainedWorkspaceReader.newInstance( reactorWorkspace,
                                                                            repoSession.getWorkspaceReader() ) );

        repoSession.setReadOnly();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( projects ) )
            {
                Thread.currentThread().setContextClassLoader( listener.getClass().getClassLoader() );

                listener.afterProjectsRead( session );
            }
        }
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }

        //
        // The projects need to be topologically after the participants have run their afterProjectsRead(session)
        // because the participant is free to change the dependencies of a project which can potentially change the
        // topological order of the projects, and therefore can potentially change the build order.
        //
        // Note that participants may affect the topological order of the projects but it is
        // not expected that a participant will add or remove projects from the session.
        //
        projectDependencyGraph = createProjectDependencyGraph( session.getProjects(), request, result, false );

        if ( result.hasExceptions() )
        {
            try 
            {
                afterSessionEnd( projects, session );
            } 
            catch ( MavenExecutionException e )
            {
                return addExceptionToResult( result, e );
            }

            return result;
        }
        
        session.setProjects( projectDependencyGraph.getSortedProjects() );

        session.setProjectDependencyGraph( projectDependencyGraph );

        result.setTopologicallySortedProjects( session.getProjects() );

        result.setProject( session.getTopLevelProject() );

        lifecycleStarter.execute( session );

        validateActivatedProfiles( session.getProjects(), request.getActiveProfiles() );

        if ( session.getResult().hasExceptions() )
        {
            return addExceptionToResult( result, session.getResult().getExceptions().get( 0 ) );
        }

        try 
        {
            afterSessionEnd( projects, session );
        } 
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }

        sessionScope.exit();

        return result;
    }

    private void afterSessionEnd( Collection<MavenProject> projects, MavenSession session ) 
        throws MavenExecutionException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( projects ) )
            {
                Thread.currentThread().setContextClassLoader( listener.getClass().getClassLoader() );

                listener.afterSessionEnd( session );
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }
    }

    public RepositorySystemSession newRepositorySession( MavenExecutionRequest request )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setCache( request.getRepositoryCache() );

        Map<Object, Object> configProps = new LinkedHashMap<Object, Object>();
        configProps.put( ConfigurationProperties.USER_AGENT, getUserAgent() );
        configProps.put( ConfigurationProperties.INTERACTIVE, request.isInteractiveMode() );
        configProps.putAll( request.getSystemProperties() );
        configProps.putAll( request.getUserProperties() );

        session.setOffline( request.isOffline() );
        session.setChecksumPolicy( request.getGlobalChecksumPolicy() );
        if ( request.isNoSnapshotUpdates() )
        {
            session.setUpdatePolicy( RepositoryPolicy.UPDATE_POLICY_NEVER );
        }
        else if ( request.isUpdateSnapshots() )
        {
            session.setUpdatePolicy( RepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }
        else
        {
            session.setUpdatePolicy( null );
        }

        int errorPolicy = 0;
        errorPolicy |= request.isCacheNotFound() ? ResolutionErrorPolicy.CACHE_NOT_FOUND : 0;
        errorPolicy |= request.isCacheTransferError() ? ResolutionErrorPolicy.CACHE_TRANSFER_ERROR : 0;
        session.setResolutionErrorPolicy( new SimpleResolutionErrorPolicy( errorPolicy, errorPolicy
            | ResolutionErrorPolicy.CACHE_NOT_FOUND ) );

        session.setArtifactTypeRegistry( RepositoryUtils.newArtifactTypeRegistry( artifactHandlerManager ) );

        LocalRepository localRepo = new LocalRepository( request.getLocalRepository().getBasedir() );

        if ( request.isUseLegacyLocalRepository() )
        {
            logger.warn( "Disabling enhanced local repository: using legacy is strongly discouraged to ensure build reproducibility." );
            try
            {
                session.setLocalRepositoryManager( simpleLocalRepositoryManagerFactory.newInstance( session, localRepo ) );
            }
            catch ( NoLocalRepositoryManagerException e )
            {

                logger.warn( "Failed to configure legacy local repository: back to default" );
                session.setLocalRepositoryManager( repoSystem.newLocalRepositoryManager( session, localRepo ) );
            }
        }
        else
        {
            session.setLocalRepositoryManager( repoSystem.newLocalRepositoryManager( session, localRepo ) );
        }

        if ( request.getWorkspaceReader() != null )
        {
            session.setWorkspaceReader( request.getWorkspaceReader() );
        }
        else
        {
            session.setWorkspaceReader( workspaceRepository );
        }

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies( request.getProxies() );
        decrypt.setServers( request.getServers() );
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt( decrypt );

        if ( logger.isDebugEnabled() )
        {
            for ( SettingsProblem problem : decrypted.getProblems() )
            {
                logger.debug( problem.getMessage(), problem.getException() );
            }
        }

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for ( Mirror mirror : request.getMirrors() )
        {
            mirrorSelector.add( mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                                mirror.getMirrorOfLayouts() );
        }
        session.setMirrorSelector( mirrorSelector );

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for ( Proxy proxy : decrypted.getProxies() )
        {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername( proxy.getUsername() ).addPassword( proxy.getPassword() );
            proxySelector.add( new org.eclipse.aether.repository.Proxy( proxy.getProtocol(), proxy.getHost(),
                                                                        proxy.getPort(), authBuilder.build() ),
                               proxy.getNonProxyHosts() );
        }
        session.setProxySelector( proxySelector );

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for ( Server server : decrypted.getServers() )
        {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername( server.getUsername() ).addPassword( server.getPassword() );
            authBuilder.addPrivateKey( server.getPrivateKey(), server.getPassphrase() );
            authSelector.add( server.getId(), authBuilder.build() );

            if ( server.getConfiguration() != null )
            {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for ( int i = dom.getChildCount() - 1; i >= 0; i-- )
                {
                    Xpp3Dom child = dom.getChild( i );
                    if ( "wagonProvider".equals( child.getName() ) )
                    {
                        dom.removeChild( i );
                    }
                }

                XmlPlexusConfiguration config = new XmlPlexusConfiguration( dom );
                configProps.put( "aether.connector.wagon.config." + server.getId(), config );
            }

            configProps.put( "aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions() );
            configProps.put( "aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions() );
        }
        session.setAuthenticationSelector( authSelector );

        session.setTransferListener( request.getTransferListener() );

        session.setRepositoryListener( eventSpyDispatcher.chainListener( new LoggingRepositoryListener( logger ) ) );

        session.setUserProperties( request.getUserProperties() );
        session.setSystemProperties( request.getSystemProperties() );
        session.setConfigProperties( configProps );

        return session;
    }

    private String getUserAgent()
    {
        return "Apache-Maven/" + getMavenVersion() + " (Java " + System.getProperty( "java.version" ) + "; "
            + System.getProperty( "os.name" ) + " " + System.getProperty( "os.version" ) + ")";
    }

    private String getMavenVersion()
    {
        Properties props = new Properties();

        InputStream is = getClass().getResourceAsStream( "/META-INF/maven/org.apache.maven/maven-core/pom.properties" );
        if ( is != null )
        {
            try
            {
                props.load( is );
            }
            catch ( IOException e )
            {
                logger.debug( "Failed to read Maven version", e );
            }
            IOUtil.close( is );
        }

        return props.getProperty( "version", "unknown-version" );
    }

    private void validateLocalRepository( MavenExecutionRequest request )
        throws LocalRepositoryNotAccessibleException
    {
        File localRepoDir = request.getLocalRepositoryPath();

        logger.debug( "Using local repository at " + localRepoDir );

        localRepoDir.mkdirs();

        if ( !localRepoDir.isDirectory() )
        {
            throw new LocalRepositoryNotAccessibleException( "Could not create local repository at " + localRepoDir );
        }
    }

    private Collection<AbstractMavenLifecycleParticipant> getLifecycleParticipants( Collection<MavenProject> projects )
    {
        Collection<AbstractMavenLifecycleParticipant> lifecycleListeners =
            new LinkedHashSet<AbstractMavenLifecycleParticipant>();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            try
            {
                lifecycleListeners.addAll( container.lookupList( AbstractMavenLifecycleParticipant.class ) );
            }
            catch ( ComponentLookupException e )
            {
                // this is just silly, lookupList should return an empty list!
                logger.warn( "Failed to lookup lifecycle participants: " + e.getMessage() );
            }

            Collection<ClassLoader> scannedRealms = new HashSet<ClassLoader>();

            for ( MavenProject project : projects )
            {
                ClassLoader projectRealm = project.getClassRealm();

                if ( projectRealm != null && scannedRealms.add( projectRealm ) )
                {
                    Thread.currentThread().setContextClassLoader( projectRealm );

                    try
                    {
                        lifecycleListeners.addAll( container.lookupList( AbstractMavenLifecycleParticipant.class ) );
                    }
                    catch ( ComponentLookupException e )
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

    private MavenExecutionResult addExceptionToResult( MavenExecutionResult result, Throwable e )
    {
        if ( !result.getExceptions().contains( e ) )
        {
            result.addException( e );
        }

        return result;
    }

    private List<MavenProject> getProjectsForMavenReactor( MavenSession session )
        throws ProjectBuildingException
    {
        MavenExecutionRequest request = session.getRequest();
        
        request.getProjectBuildingRequest().setRepositorySession( session.getRepositorySession() );

        List<MavenProject> projects = new ArrayList<MavenProject>();

        // We have no POM file.
        //
        if ( request.getPom() == null )
        {
            ModelSource modelSource = new UrlModelSource( DefaultMaven.class.getResource( "project/standalone.xml" ) );
            MavenProject project =
                projectBuilder.build( modelSource, request.getProjectBuildingRequest() ).getProject();
            project.setExecutionRoot( true );
            projects.add( project );
            request.setProjectPresent( false );
            return projects;
        }

        List<File> files = Arrays.asList( request.getPom().getAbsoluteFile() );
        collectProjects( projects, files, request );
        return projects;
    }

    private void collectProjects( List<MavenProject> projects, List<File> files, MavenExecutionRequest request )
        throws ProjectBuildingException
    {
        ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        List<ProjectBuildingResult> results =
            projectBuilder.build( files, request.isRecursive(), projectBuildingRequest );

        boolean problems = false;

        for ( ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );

            if ( !result.getProblems().isEmpty() && logger.isWarnEnabled() )
            {
                logger.warn( "" );
                logger.warn( "Some problems were encountered while building the effective model for "
                    + result.getProject().getId() );

                for ( ModelProblem problem : result.getProblems() )
                {
                    String location = ModelProblemUtils.formatLocation( problem, result.getProjectId() );
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

    private Map<String, MavenProject> getProjectMap( Collection<MavenProject> projects )
        throws DuplicateProjectException
    {
        Map<String, MavenProject> index = new LinkedHashMap<String, MavenProject>();
        Map<String, List<File>> collisions = new LinkedHashMap<String, List<File>>();

        for ( MavenProject project : projects )
        {
            String projectId = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );

            MavenProject collision = index.get( projectId );

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
            throw new DuplicateProjectException( "Two or more projects in the reactor"
                + " have the same identifier, please make sure that <groupId>:<artifactId>:<version>"
                + " is unique for each project: " + collisions, collisions );
        }

        return index;
    }

    private void validateProjects( List<MavenProject> projects )
    {
        Map<String, MavenProject> projectsMap = new HashMap<String, MavenProject>();

        for ( MavenProject project : projects )
        {
            String projectKey = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );

            projectsMap.put( projectKey, project );
        }

        for ( MavenProject project : projects )
        {
            // MNG-1911 / MNG-5572: Building plugins with extensions cannot be part of reactor 
            for ( Plugin plugin : project.getBuildPlugins() )
            {
                if ( plugin.isExtensions() )
                {
                    String pluginKey =
                        ArtifactUtils.key( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );

                    if ( projectsMap.containsKey( pluginKey ) )
                    {
                        logger.warn( project.getName() + " uses " + plugin.getKey()
                            + " as extensions, which is not possible within the same reactor build. This plugin was pulled from the local repository!" );
                    }
                }
            }
        }
    }

    private void validateActivatedProfiles( List<MavenProject> projects, List<String> activeProfileIds )
    {
        Collection<String> notActivatedProfileIds = new LinkedHashSet<String>( activeProfileIds );

        for ( MavenProject project : projects )
        {
            for ( List<String> profileIds : project.getInjectedProfileIds().values() )
            {
                notActivatedProfileIds.removeAll( profileIds );
            }
        }

        for ( String notActivatedProfileId : notActivatedProfileIds )
        {
            logger.warn( "The requested profile \"" + notActivatedProfileId
                + "\" could not be activated because it does not exist." );
        }
    }

    @Deprecated // 5 January 2014
    protected Logger getLogger()
    {
        return logger;
    }

    private ProjectDependencyGraph createProjectDependencyGraph( Collection<MavenProject> projects, MavenExecutionRequest request,
                                                                 MavenExecutionResult result, boolean trimming )
    {
        ProjectDependencyGraph projectDependencyGraph = null;

        try
        {
            projectDependencyGraph = new DefaultProjectDependencyGraph( projects );

            if ( trimming )
            {
                List<MavenProject> activeProjects = projectDependencyGraph.getSortedProjects();

                activeProjects = trimSelectedProjects( activeProjects, projectDependencyGraph, request );
                activeProjects = trimExcludedProjects( activeProjects,  request );
                activeProjects = trimResumedProjects( activeProjects, request );

                if ( activeProjects.size() != projectDependencyGraph.getSortedProjects().size() )
                {
                    projectDependencyGraph =
                        new FilteredProjectDependencyGraph( projectDependencyGraph, activeProjects );
                }
            }
        }
        catch ( CycleDetectedException e )
        {
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();

            ProjectCycleException error = new ProjectCycleException( message, e );

            addExceptionToResult( result, error );
        }
        catch ( org.apache.maven.project.DuplicateProjectException e )
        {
            addExceptionToResult( result, e );
        }
        catch ( MavenExecutionException e )
        {
            addExceptionToResult( result, e );
        }

        return projectDependencyGraph;
    }

    private List<MavenProject> trimSelectedProjects( List<MavenProject> projects, ProjectDependencyGraph graph,
                                                     MavenExecutionRequest request )
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

            Collection<MavenProject> selectedProjects = new LinkedHashSet<MavenProject>( projects.size() );

            for ( String selector : request.getSelectedProjects() )
            {
                MavenProject selectedProject = null;

                for ( MavenProject project : projects )
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
                for ( MavenProject selectedProject : new ArrayList<MavenProject>( selectedProjects ) )
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

            for ( MavenProject project : projects )
            {
                if ( selectedProjects.contains( project ) )
                {
                    result.add( project );
                }
            }
        }

        return result;
    }
    
    private List<MavenProject> trimExcludedProjects( List<MavenProject> projects, MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( !request.getExcludedProjects().isEmpty() )
        {
            File reactorDirectory = null;

            if ( request.getBaseDirectory() != null )
            {
                reactorDirectory = new File( request.getBaseDirectory() );
            }

            Collection<MavenProject> excludedProjects = new LinkedHashSet<MavenProject>( projects.size() );

            for ( String selector : request.getExcludedProjects() )
            {
                MavenProject excludedProject = null;

                for ( MavenProject project : projects )
                {
                    if ( isMatchingProject( project, selector, reactorDirectory ) )
                    {
                        excludedProject = project;
                        break;
                    }
                }

                if ( excludedProject != null )
                {
                    excludedProjects.add( excludedProject );
                }
                else
                {
                    throw new MavenExecutionException( "Could not find the selected project in the reactor: "
                        + selector, request.getPom() );
                }
            }

            result = new ArrayList<MavenProject>( projects.size() );
            for ( MavenProject project : projects )
            {
                if ( !excludedProjects.contains( project ) )
                {
                    result.add( project );
                }
            }
        }

        return result;
    }

    private List<MavenProject> trimResumedProjects( List<MavenProject> projects, MavenExecutionRequest request )
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

            String selector = request.getResumeFrom();

            result = new ArrayList<MavenProject>( projects.size() );

            boolean resumed = false;

            for ( MavenProject project : projects )
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

    private boolean isMatchingProject( MavenProject project, String selector, File reactorDirectory )
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
            File selectedProject = new File( new File( reactorDirectory, selector ).toURI().normalize() );

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
