package org.apache.maven.caching;

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

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.caching.checksum.KeyUtils;
import org.apache.maven.caching.checksum.MavenProjectInput;
import org.apache.maven.caching.hash.HashAlgorithm;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.jaxb.ArtifactType;
import org.apache.maven.caching.jaxb.BuildDiffType;
import org.apache.maven.caching.jaxb.BuildInfoType;
import org.apache.maven.caching.jaxb.CacheReportType;
import org.apache.maven.caching.jaxb.CompletedExecutionType;
import org.apache.maven.caching.jaxb.DigestItemType;
import org.apache.maven.caching.jaxb.ProjectReportType;
import org.apache.maven.caching.jaxb.ProjectsInputInfoType;
import org.apache.maven.caching.jaxb.PropertyNameType;
import org.apache.maven.caching.jaxb.TrackedPropertyType;
import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.DtoUtils;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.ReflectionUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.maven.caching.CacheResult.empty;
import static org.apache.maven.caching.CacheResult.failure;
import static org.apache.maven.caching.CacheResult.partialSuccess;
import static org.apache.maven.caching.CacheResult.rebuilded;
import static org.apache.maven.caching.CacheResult.success;
import static org.apache.maven.caching.HttpRepositoryImpl.BUILDINFO_XML;
import static org.apache.maven.caching.checksum.KeyUtils.getVersionlessProjectKey;
import static org.apache.maven.caching.checksum.MavenProjectInput.CACHE_IMPLMENTATION_VERSION;

/**
 * CacheControllerImpl
 */
@Component( role = CacheController.class )
public class CacheControllerImpl implements CacheController
{

    public static final String FILE_SEPARATOR_SUBST = "_";
    private static final String GENERATEDSOURCES = "generatedsources";
    private static final String GENERATEDSOURCES_PREFIX = GENERATEDSOURCES + FILE_SEPARATOR_SUBST;
    @Requirement
    private Logger logger;

    @Requirement
    private MavenPluginManager mavenPluginManager;

    @Requirement
    private MavenProjectHelper projectHelper;

    @Requirement
    private LocalArtifactsRepository localCache;

    @Requirement
    private RemoteArtifactsRepository remoteCache;

    @Requirement
    private CacheConfig cacheConfig;

    @Requirement
    private RepositorySystem repoSystem;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Requirement
    private XmlService xmlService;

    private final ConcurrentMap<String, DigestItemType> artifactDigestByKey = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, CacheResult> cacheResults = new ConcurrentHashMap<>();

    private volatile BuildInfoType.Scm scm;

    @Override
    @Nonnull
    public CacheResult findCachedBuild( MavenSession session, MavenProject project, ProjectIndex projectIndex,
                                        List<MojoExecution> mojoExecutions )
    {

        final String highestRequestPhase = Iterables.getLast( mojoExecutions ).getLifecyclePhase();
        if ( !ProjectUtils.isLaterPhase( highestRequestPhase, "post-clean" ) )
        {
            return empty();
        }

        logInfo( project, "Attempting to restore project from build cache" );

        ProjectsInputInfoType inputInfo = calculateInput( project, session, projectIndex );

        final CacheContext context = new CacheContext( project, inputInfo, session );
        // remote build first
        CacheResult result = findCachedBuild( mojoExecutions, context );

        if ( !result.isSuccess() && result.getContext() != null )
        {

            logDebug( project, "Remote cache is incomplete or missing, trying local build" );

            CacheResult localBuild = findLocalBuild( mojoExecutions, context );

            if ( localBuild.isSuccess() || ( localBuild.isPartialSuccess() && !result.isPartialSuccess() ) )
            {
                result = localBuild;
            }
        }
        cacheResults.put( getVersionlessProjectKey( project ), result );

        return result;
    }

    private CacheResult findCachedBuild( List<MojoExecution> mojoExecutions, CacheContext context )
    {
        BuildInfo cachedBuild = null;
        try
        {
            cachedBuild = localCache.findBuild( context );
            return analyzeResult( context, mojoExecutions, cachedBuild );
        }
        catch ( Exception e )
        {
            logError( context.getProject(), "Cannot read cached build", e );
            return cachedBuild != null ? failure( cachedBuild, context ) : failure( context );
        }
    }

    private CacheResult findLocalBuild( List<MojoExecution> mojoExecutions, CacheContext context )
    {
        BuildInfo localBuild = null;
        try
        {
            localBuild = localCache.findLocalBuild( context );
            return analyzeResult( context, mojoExecutions, localBuild );
        }
        catch ( Exception e )
        {
            logError( context.getProject(), "Cannot read local build", e );
            return localBuild != null ? failure( localBuild, context ) : failure( context );
        }
    }

    private CacheResult analyzeResult( CacheContext context, List<MojoExecution> mojoExecutions, BuildInfo info )
    {

        try
        {
            if ( info != null )
            {

                final MavenProject project = context.getProject();
                final ProjectsInputInfoType inputInfo = context.getInputInfo();

                logInfo( project, "Found cached build, restoring from cache " + inputInfo.getChecksum() );

                if ( logger.isDebugEnabled() )
                {
                    logDebug( project, "Cached build details: " + info.toString() );
                }

                final String cacheImplementationVersion = info.getCacheImplementationVersion();
                if ( !CACHE_IMPLMENTATION_VERSION.equals( cacheImplementationVersion ) )
                {
                    logger.warn(
                            "Maven and cached build implementations mismatch, caching might not work correctly. "
                                    + "Implementation version: " + CACHE_IMPLMENTATION_VERSION + ", cached build: "
                                    + info.getCacheImplementationVersion() );
                }

                final List<MojoExecution> cachedSegment = info.getCachedSegment( mojoExecutions );

                if ( !info.isAllExecutionsPresent( cachedSegment, logger ) )
                {
                    logInfo( project, "Cached build doesn't contains all requested plugin executions, cannot restore" );
                    return failure( info, context );
                }

                if ( !isCachedSegmentPropertiesPresent( project, info, cachedSegment ) )
                {
                    logInfo( project, "Cached build violates cache rules, cannot restore" );
                    return failure( info, context );
                }

                final String highestRequestPhase = Iterables.getLast( mojoExecutions ).getLifecyclePhase();
                final String highestCompletedGoal = info.getHighestCompletedGoal();
                if ( ProjectUtils.isLaterPhase( highestRequestPhase, highestCompletedGoal ) && !canIgnoreMissingSegment(
                        info, mojoExecutions ) )
                {
                    logInfo( project,
                            "Project restored partially. Highest cached goal: " + highestCompletedGoal + ", requested: "
                                    + highestRequestPhase );
                    return partialSuccess( info, context );
                }

                return success( info, context );
            }
            else
            {
                logInfo( context.getProject(), "Project is not found in cache" );
                return empty( context );
            }
        }
        catch ( Exception e )
        {
            logger.error( "Failed to restore project", e );
            localCache.clearCache( context );
            return empty( context );
        }
    }

    private boolean canIgnoreMissingSegment( BuildInfo info, List<MojoExecution> mojoExecutions )
    {
        final List<MojoExecution> postCachedSegment = info.getPostCachedSegment( mojoExecutions );
        for ( MojoExecution mojoExecution : postCachedSegment )
        {
            if ( !cacheConfig.canIgnore( mojoExecution ) )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean restoreProjectArtifacts( CacheResult cacheResult )
    {

        final BuildInfo buildInfo = cacheResult.getBuildInfo();
        final CacheContext context = cacheResult.getContext();
        final MavenProject project = context.getProject();

        final ArtifactType artifact = buildInfo.getArtifact();
        artifact.setVersion( project.getVersion() );

        try
        {
            if ( isNotBlank( artifact.getFileName() ) )
            {
                // TODO if remote is forced, probably need to refresh or reconcile all files
                final Path artifactFile = localCache.getArtifactFile( context, cacheResult.getSource(), artifact );
                if ( !Files.exists( artifactFile ) )
                {
                    logInfo( project, "Missing file for cached build, cannot restore. File: " + artifactFile );
                    return false;
                }
                logDebug( project, "Setting project artifact " + artifact.getArtifactId() + " from: " + artifactFile );
                project.getArtifact().setFile( artifactFile.toFile() );
                project.getArtifact().setResolved( true );
                putChecksum( artifact, context.getInputInfo().getChecksum() );
            }

            for ( ArtifactType attachedArtifact : buildInfo.getAttachedArtifacts() )
            {
                attachedArtifact.setVersion( project.getVersion() );
                if ( isNotBlank( attachedArtifact.getFileName() ) )
                {
                    final Path attachedArtifactFile = localCache.getArtifactFile( context, cacheResult.getSource(),
                            attachedArtifact );
                    if ( !Files.exists( attachedArtifactFile ) )
                    {
                        logInfo( project,
                                "Missing file for cached build, cannot restore project. File: "
                                        + attachedArtifactFile );
                        project.getArtifact().setFile( null );
                        project.getArtifact().setResolved( false );
                        project.getAttachedArtifacts().clear();
                        return false;
                    }
                    logDebug( project,
                            "Attaching artifact " + artifact.getArtifactId() + " from: " + attachedArtifactFile );
                    if ( StringUtils.startsWith( attachedArtifact.getClassifier(), GENERATEDSOURCES_PREFIX ) )
                    {
                        // generated sources artifact
                        restoreGeneratedSources( attachedArtifact, attachedArtifactFile, project );
                    }
                    else
                    {
                        projectHelper.attachArtifact( project, attachedArtifact.getType(),
                                attachedArtifact.getClassifier(), attachedArtifactFile.toFile() );
                    }
                    putChecksum( attachedArtifact, context.getInputInfo().getChecksum() );
                }
            }
        }
        catch ( Exception e )
        {
            project.getArtifact().setFile( null );
            project.getArtifact().setResolved( false );
            project.getAttachedArtifacts().clear();
            logError( project, "Cannot restore cache, continuing with normal build.", e );
            return false;
        }

        return true;
    }

    private void putChecksum( ArtifactType artifact, String projectChecksum )
    {

        final DigestItemType projectArtifact = DtoUtils.createdDigestedByProjectChecksum( artifact, projectChecksum );
        final String dependencyKey = KeyUtils.getArtifactKey( artifact );
        artifactDigestByKey.put( dependencyKey, projectArtifact );

        if ( !"pom".equals( artifact.getType() ) )
        {
            final ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( artifact.getType() );
            ArtifactType copy = DtoUtils.copy( artifact );
            copy.setType( artifactHandler.getPackaging() );
            artifactDigestByKey.put( KeyUtils.getArtifactKey( copy ), projectArtifact );
            copy.setType( artifactHandler.getExtension() );
            artifactDigestByKey.put( KeyUtils.getArtifactKey( copy ), projectArtifact );
        }
    }

    private ProjectsInputInfoType calculateInput( MavenProject project, MavenSession session,
                                                  ProjectIndex projectIndex )
    {
        try
        {
            final MavenProjectInput inputs = new MavenProjectInput( project, session, cacheConfig, projectIndex,
                    artifactDigestByKey, repoSystem, artifactHandlerManager, logger, localCache, remoteCache );
            return inputs.calculateChecksum( cacheConfig.getHashFactory() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to calculate checksums for " + project.getArtifactId(), e );
        }
    }

    @Override
    public void save( CacheResult cacheResult, List<MojoExecution> mojoExecutions,
                      Map<String, MojoExecutionEvent> executionEvents )
    {

        CacheContext context = cacheResult.getContext();

        if ( context == null || context.getInputInfo() == null )
        {
            logger.info( "Cannot save project in cache, skipping" );
            return;
        }

        final MavenProject project = context.getProject();
        final MavenSession session = context.getSession();
        try
        {

            attachGeneratedSources( project );
            attachOutputs( project );

            final Artifact projectArtifact = project.getArtifact();
            final HashFactory hashFactory = cacheConfig.getHashFactory();
            final HashAlgorithm algorithm = hashFactory.createAlgorithm();
            final ArtifactType projectArtifactDto = artifactDto( project.getArtifact(), algorithm );

            final List<Artifact> attachedArtifacts =
                    project.getAttachedArtifacts() != null ? project.getAttachedArtifacts() : Collections.emptyList();
            List<ArtifactType> attachedArtifactDtos = artifactDtos( attachedArtifacts, algorithm );

            List<CompletedExecutionType> completedExecution = buildExecutionInfo( mojoExecutions, executionEvents );

            final BuildInfo buildInfo = new BuildInfo( session.getGoals(), projectArtifactDto, attachedArtifactDtos,
                    context.getInputInfo(), completedExecution, hashFactory.getAlgorithm() );
            populateGitInfo( buildInfo, session );
            buildInfo.getDto().setFinal( cacheConfig.isSaveFinal() );
            cacheResults.put( getVersionlessProjectKey( project ), rebuilded( cacheResult, buildInfo ) );

            // if package phase presence means new artifacts were packaged
            if ( project.hasLifecyclePhase( "package" ) )
            {
                localCache.beforeSave( context );
                localCache.saveBuildInfo( cacheResult, buildInfo );
                if ( projectArtifact.getFile() != null )
                {
                    localCache.saveArtifactFile( cacheResult, projectArtifact );
                    putChecksum( projectArtifactDto, context.getInputInfo().getChecksum() );
                }
                for ( Artifact attachedArtifact : attachedArtifacts )
                {
                    if ( attachedArtifact.getFile() != null && isOutputArtifact(
                            attachedArtifact.getFile().getName() ) )
                    {
                        localCache.saveArtifactFile( cacheResult, attachedArtifact );
                    }
                }
                for ( ArtifactType attachedArtifactDto : attachedArtifactDtos )
                {
                    putChecksum( attachedArtifactDto, context.getInputInfo().getChecksum() );
                }
            }
            else
            {
                localCache.saveBuildInfo( cacheResult, buildInfo );
            }

            if ( cacheConfig.isBaselineDiffEnabled() )
            {
                produceDiffReport( cacheResult, buildInfo );
            }

        }
        catch ( Exception e )
        {
            try
            {
                logger.error( "Failed to save project, cleaning cache. Project: " + project, e );
                localCache.clearCache( context );
            }
            catch ( Exception ex )
            {
                logger.error( "Failed to clean cache due to unexpected error:", e );
            }
        }
    }

    public void produceDiffReport( CacheResult cacheResult, BuildInfo buildInfo )
    {
        MavenProject project = cacheResult.getContext().getProject();
        Optional<BuildInfo> baselineHolder = remoteCache.findBaselineBuild( project );
        if ( baselineHolder.isPresent() )
        {
            BuildInfo baseline = baselineHolder.get();
            String outputDirectory = project.getBuild().getDirectory();
            Path reportOutputDir = Paths.get( outputDirectory, "incremental-maven" );
            logInfo( project, "Saving cache builds diff to: " + reportOutputDir );
            BuildDiffType diff = new CacheDiff( buildInfo.getDto(), baseline.getDto(), cacheConfig ).compare();
            try
            {
                Files.createDirectories( reportOutputDir );
                final ProjectsInputInfoType baselineInputs = baseline.getDto().getProjectsInputInfo();
                final String checksum = baselineInputs.getChecksum();
                Files.write( reportOutputDir.resolve( "buildinfo-baseline-" + checksum + ".xml" ),
                        xmlService.toBytes( baseline.getDto() ), TRUNCATE_EXISTING, CREATE );
                Files.write( reportOutputDir.resolve( "buildinfo-" + checksum + ".xml" ),
                        xmlService.toBytes( buildInfo.getDto() ), TRUNCATE_EXISTING, CREATE );
                Files.write( reportOutputDir.resolve( "buildsdiff-" + checksum + ".xml" ),
                        xmlService.toBytes( diff ), TRUNCATE_EXISTING, CREATE );
                final Optional<DigestItemType> pom = CacheDiff.findPom( buildInfo.getDto().getProjectsInputInfo() );
                if ( pom.isPresent() )
                {
                    Files.write( reportOutputDir.resolve( "effective-pom-" + checksum + ".xml" ),
                            pom.get().getValue().getBytes( StandardCharsets.UTF_8 ),
                            TRUNCATE_EXISTING, CREATE );
                }
                final Optional<DigestItemType> baselinePom = CacheDiff.findPom( baselineInputs );
                if ( baselinePom.isPresent() )
                {
                    Files.write( reportOutputDir.resolve(
                                    "effective-pom-baseline-" + baselineInputs.getChecksum() + ".xml" ),
                            baselinePom.get().getValue().getBytes( StandardCharsets.UTF_8 ),
                            TRUNCATE_EXISTING, CREATE );
                }
            }
            catch ( IOException e )
            {
                logError( project, "Cannot produce build diff for project", e );
            }
        }
        else
        {
            logInfo( project, "Cannot find project in baseline build, skipping diff" );
        }
    }

    private List<ArtifactType> artifactDtos( List<Artifact> attachedArtifacts, HashAlgorithm digest ) throws IOException
    {
        List<ArtifactType> result = new ArrayList<>();
        for ( Artifact attachedArtifact : attachedArtifacts )
        {
            if ( attachedArtifact.getFile() != null && isOutputArtifact( attachedArtifact.getFile().getName() ) )
            {
                result.add( artifactDto( attachedArtifact, digest ) );
            }
        }
        return result;
    }

    private ArtifactType artifactDto( Artifact projectArtifact, HashAlgorithm algorithm ) throws IOException
    {
        final ArtifactType dto = DtoUtils.createDto( projectArtifact );
        if ( projectArtifact.getFile() != null && projectArtifact.getFile().isFile() )
        {
            final Path file = projectArtifact.getFile().toPath();
            dto.setFileHash( algorithm.hash( file ) );
            dto.setFileSize( BigInteger.valueOf( Files.size( file ) ) );
        }
        return dto;
    }

    private List<CompletedExecutionType> buildExecutionInfo( List<MojoExecution> mojoExecutions,
                                                             Map<String, MojoExecutionEvent> executionEvents )
    {
        List<CompletedExecutionType> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            final String executionKey = ProjectUtils.mojoExecutionKey( mojoExecution );
            final MojoExecutionEvent executionEvent = executionEvents.get( executionKey );
            CompletedExecutionType executionInfo = new CompletedExecutionType();
            executionInfo.setExecutionKey( executionKey );
            executionInfo.setMojoClassName( mojoExecution.getMojoDescriptor().getImplementation() );
            if ( executionEvent != null )
            {
                recordMojoProperties( executionInfo, executionEvent );
            }
            list.add( executionInfo );
        }
        return list;
    }

    private void recordMojoProperties( CompletedExecutionType execution, MojoExecutionEvent executionEvent )
    {
        final MojoExecution mojoExecution = executionEvent.getExecution();

        final boolean logAll = cacheConfig.isLogAllProperties( mojoExecution );
        List<TrackedPropertyType> trackedProperties = cacheConfig.getTrackedProperties( mojoExecution );
        List<PropertyNameType> noLogProperties = cacheConfig.getNologProperties( mojoExecution );
        List<PropertyNameType> forceLogProperties = cacheConfig.getLoggedProperties( mojoExecution );
        final Mojo mojo = executionEvent.getMojo();

        final File baseDir = executionEvent.getProject().getBasedir();
        final String baseDirPath = FilenameUtils.normalizeNoEndSeparator( baseDir.getAbsolutePath() ) + File.separator;

        final List<Parameter> parameters = mojoExecution.getMojoDescriptor().getParameters();
        for ( Parameter parameter : parameters )
        {
            // editable parameters could be configured by user
            if ( !parameter.isEditable() )
            {
                continue;
            }

            final String propertyName = parameter.getName();
            final boolean tracked = isTracked( propertyName, trackedProperties );
            if ( !tracked && isExcluded( propertyName, logAll, noLogProperties, forceLogProperties ) )
            {
                continue;
            }

            try
            {
                final Object value = ReflectionUtils.getValueIncludingSuperclasses( propertyName, mojo );
                DtoUtils.addProperty( execution, propertyName, value, baseDirPath, tracked );
            }
            catch ( IllegalAccessException e )
            {
                logInfo( executionEvent.getProject(),
                        "Cannot get property " + propertyName + " value from " + mojo + ": " + e.getMessage() );
                if ( tracked )
                {
                    throw new IllegalArgumentException(
                            "Property configured in cache introspection config for " + mojo + " is not accessible: "
                                    + propertyName );
                }
            }
        }
    }

    private boolean isExcluded( String propertyName, boolean logAll, List<PropertyNameType> excludedProperties,
                                List<PropertyNameType> forceLogProperties )
    {

        if ( !forceLogProperties.isEmpty() )
        {
            for ( PropertyNameType logProperty : forceLogProperties )
            {
                if ( StringUtils.equals( propertyName, logProperty.getPropertyName() ) )
                {
                    return false;
                }
            }
            return true;
        }

        if ( !excludedProperties.isEmpty() )
        {
            for ( PropertyNameType excludedProperty : excludedProperties )
            {
                if ( StringUtils.equals( propertyName, excludedProperty.getPropertyName() ) )
                {
                    return true;
                }
            }
            return false;
        }

        return !logAll;
    }

    private boolean isTracked( String propertyName, List<TrackedPropertyType> trackedProperties )
    {
        for ( TrackedPropertyType trackedProperty : trackedProperties )
        {
            if ( StringUtils.equals( propertyName, trackedProperty.getPropertyName() ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean isCachedSegmentPropertiesPresent( MavenProject project, BuildInfo buildInfo,
                                                      List<MojoExecution> mojoExecutions )
    {
        for ( MojoExecution mojoExecution : mojoExecutions )
        {

            // completion of all mojos checked above, so we expect tp have execution info here
            final List<TrackedPropertyType> trackedProperties = cacheConfig.getTrackedProperties( mojoExecution );
            final CompletedExecutionType cachedExecution = buildInfo.findMojoExecutionInfo( mojoExecution );

            if ( cachedExecution == null )
            {
                logInfo( project,
                        "Execution is not cached. Plugin: " + mojoExecution.getExecutionId() + ", goal"
                                + mojoExecution.getGoal() );
                return false;
            }

            if ( !DtoUtils.containsAllProperties( cachedExecution, trackedProperties ) )
            {
                logInfo( project, "Build info doesn't match rules. Plugin: " + mojoExecution.getExecutionId() );
                return false;
            }
        }
        return true;
    }

    private void logDebug( MavenProject project, String message )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "[CACHE][" + project.getArtifactId() + "] " + message );
        }
    }

    private void logInfo( MavenProject project, String message )
    {
        logger.info( "[CACHE][" + project.getArtifactId() + "] " + message );
    }

    private void logError( MavenProject project, String message, Exception e )
    {
        logger.error( "[CACHE][" + project.getArtifactId() + "] " + message, e );
    }

    @Override
    public boolean isForcedExecution( MavenProject project, MojoExecution execution )
    {

        if ( cacheConfig.isForcedExecution( execution ) )
        {
            return true;
        }
        final Properties properties = project.getProperties();
        final String alwaysRunPlugins = properties.getProperty( "remote.cache.alwaysRunPlugins" );
        ArrayList<String> alwaysRunPluginsList = new ArrayList<>();
        if ( alwaysRunPlugins != null )
        {
            alwaysRunPluginsList = Lists.newArrayList( split( alwaysRunPlugins, "," ) );
        }
        for ( String pluginAndGoal : alwaysRunPluginsList )
        {
            final String[] tokens = pluginAndGoal.split( ":" );
            final String alwaysRunPlugin = tokens[0];
            String alwaysRunGoal = tokens.length == 1 ? "*" : tokens[1];
            if ( StringUtils.equals( execution.getPlugin().getArtifactId(), alwaysRunPlugin ) && ( "*".equals(
                    alwaysRunGoal ) || StringUtils.equals( execution.getGoal(), alwaysRunGoal ) ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveCacheReport( MavenSession session )
    {
        try
        {

            CacheReportType cacheReport = new CacheReportType();
            for ( CacheResult result : cacheResults.values() )
            {
                ProjectReportType projectReport = new ProjectReportType();
                CacheContext context = result.getContext();
                MavenProject project = context.getProject();
                projectReport.setGroupId( project.getGroupId() );
                projectReport.setArtifactId( project.getArtifactId() );
                projectReport.setChecksum( context.getInputInfo().getChecksum() );
                boolean checksumMatched = result.getStatus() != RestoreStatus.EMPTY;
                projectReport.setChecksumMatched( checksumMatched );
                projectReport.setLifecycleMatched( checksumMatched && result.isSuccess() );
                projectReport.setSource( String.valueOf( result.getSource() ) );
                if ( result.getSource() == CacheSource.REMOTE )
                {
                    projectReport.setUrl( remoteCache.getResourceUrl( context, BUILDINFO_XML ) );
                }
                else if ( result.getSource() == CacheSource.BUILD && cacheConfig.isSaveToRemote() )
                {
                    projectReport.setSharedToRemote( true );
                    projectReport.setUrl( remoteCache.getResourceUrl( context, BUILDINFO_XML ) );
                }
                cacheReport.getProject().add( projectReport );
            }

            String buildId = UUID.randomUUID().toString();
            localCache.saveCacheReport( buildId, session, cacheReport );
        }
        catch ( Exception e )
        {
            logger.error( "Cannot save incremental build aggregated report", e );
        }
    }

    private void populateGitInfo( BuildInfo buildInfo, MavenSession session ) throws IOException
    {
        if ( scm == null )
        {
            synchronized ( this )
            {
                if ( scm == null )
                {
                    try
                    {
                        scm = ProjectUtils.readGitInfo( session );
                    }
                    catch ( IOException e )
                    {
                        scm = new BuildInfoType.Scm();
                        logger.error( "Cannot populate git info", e );
                    }
                }
            }
        }
        buildInfo.getDto().setScm( scm );
    }

    private void zipAndAttachArtifact( MavenProject project, Path dir, String classifier ) throws IOException
    {

        try ( final InputStream inputStream = ZipUtils.zipFolder( dir ) )
        {
            File tempFile = File.createTempFile( "maven-incremental", project.getArtifactId() );
            tempFile.deleteOnExit();
            Files.copy( inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
            projectHelper.attachArtifact( project, "zip", classifier, tempFile );
        }
    }

    private String pathToClassifier( Path relative )
    {
        final int nameCount = relative.getNameCount();
        List<String> segments = new ArrayList<>( nameCount + 1 );
        for ( int i = 0; i < nameCount; i++ )
        {
            segments.add( relative.getName( i ).toFile().getName() );
        }
        // todo handle _ in file names
        return GENERATEDSOURCES_PREFIX + StringUtils.join( segments.iterator(), FILE_SEPARATOR_SUBST );
    }

    private Path classifierToPath( Path outputDir, String classifier )
    {
        classifier = StringUtils.removeStart( classifier, GENERATEDSOURCES_PREFIX );
        final String relPath = replace( classifier, FILE_SEPARATOR_SUBST, File.separator );
        return outputDir.resolve( relPath );
    }

    private void restoreGeneratedSources( ArtifactType artifact, Path artifactFilePath, MavenProject project )
            throws IOException
    {
        final Path targetDir = Paths.get( project.getBuild().getDirectory() );
        final Path outputDir = classifierToPath( targetDir, artifact.getClassifier() );
        try ( InputStream is = Files.newInputStream( artifactFilePath ) )
        {
            if ( Files.exists( outputDir ) )
            {
                FileUtils.cleanDirectory( outputDir.toFile() );
            }
            else
            {
                Files.createDirectories( outputDir );
            }
            ZipUtils.unzip( is, outputDir );
        }
    }

    //TODO: move to config
    public void attachGeneratedSources( MavenProject project ) throws IOException
    {
        final Path targetDir = Paths.get( project.getBuild().getDirectory() );

        final Path generatedSourcesDir = targetDir.resolve( "generated-sources" );
        attachDirIfNotEmpty( generatedSourcesDir, targetDir, project );

        final Path generatedTestSourcesDir = targetDir.resolve( "generated-test-sources" );
        attachDirIfNotEmpty( generatedTestSourcesDir, targetDir, project );

        Set<String> sourceRoots = new TreeSet<>();
        if ( project.getCompileSourceRoots() != null )
        {
            sourceRoots.addAll( project.getCompileSourceRoots() );
        }
        if ( project.getTestCompileSourceRoots() != null )
        {
            sourceRoots.addAll( project.getTestCompileSourceRoots() );
        }

        for ( String sourceRoot : sourceRoots )
        {
            final Path sourceRootPath = Paths.get( sourceRoot );
            if ( Files.isDirectory( sourceRootPath ) && sourceRootPath.startsWith(
                    targetDir ) && !( sourceRootPath.startsWith( generatedSourcesDir ) || sourceRootPath.startsWith(
                    generatedTestSourcesDir ) ) )
            { // dir within target
                attachDirIfNotEmpty( sourceRootPath, targetDir, project );
            }
        }
    }

    private void attachOutputs( MavenProject project ) throws IOException
    {
        final List<String> attachedDirs = cacheConfig.getAttachedOutputs();
        for ( String dir : attachedDirs )
        {
            final Path targetDir = Paths.get( project.getBuild().getDirectory() );
            final Path outputDir = targetDir.resolve( dir );
            attachDirIfNotEmpty( outputDir, targetDir, project );
        }
    }

    private void attachDirIfNotEmpty( Path candidateSubDir, Path parentDir, MavenProject project ) throws IOException
    {
        if ( Files.isDirectory( candidateSubDir ) && hasFiles( candidateSubDir ) )
        {
            final Path relativePath = parentDir.relativize( candidateSubDir );
            final String classifier = pathToClassifier( relativePath );
            zipAndAttachArtifact( project, candidateSubDir, classifier );
            logDebug( project, "Attached directory: " + candidateSubDir );
        }
    }

    private boolean hasFiles( Path candidateSubDir ) throws IOException
    {
        final MutableBoolean hasFiles = new MutableBoolean();
        Files.walkFileTree( candidateSubDir, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile( Path path, BasicFileAttributes basicFileAttributes )
            {
                hasFiles.setTrue();
                return FileVisitResult.TERMINATE;
            }
        } );
        return hasFiles.booleanValue();
    }

    private boolean isOutputArtifact( String name )
    {
        List<Pattern> excludePatterns = cacheConfig.getExcludePatterns();
        for ( Pattern pattern : excludePatterns )
        {
            if ( pattern.matcher( name ).matches() )
            {
                return false;
            }
        }
        return true;
    }
}
