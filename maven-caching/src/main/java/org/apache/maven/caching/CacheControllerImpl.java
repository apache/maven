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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.maven.SessionScoped;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.caching.checksum.KeyUtils;
import org.apache.maven.caching.checksum.MavenProjectInput;
import org.apache.maven.caching.hash.HashAlgorithm;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.DtoUtils;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.caching.xml.build.Artifact;
import org.apache.maven.caching.xml.build.CompletedExecution;
import org.apache.maven.caching.xml.build.DigestItem;
import org.apache.maven.caching.xml.build.ProjectsInputInfo;
import org.apache.maven.caching.xml.build.Scm;
import org.apache.maven.caching.xml.config.PropertyName;
import org.apache.maven.caching.xml.config.TrackedProperty;
import org.apache.maven.caching.xml.diff.Diff;
import org.apache.maven.caching.xml.report.CacheReport;
import org.apache.maven.caching.xml.report.ProjectReport;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static org.apache.maven.caching.HttpCacheRepositoryImpl.BUILDINFO_XML;
import static org.apache.maven.caching.checksum.KeyUtils.getVersionlessProjectKey;
import static org.apache.maven.caching.checksum.MavenProjectInput.CACHE_IMPLEMENTATION_VERSION;

/**
 * CacheControllerImpl
 */
@SessionScoped
@Named
@SuppressWarnings( "unused" )
public class CacheControllerImpl implements CacheController
{

    public static final String FILE_SEPARATOR_SUBST = "_";
    private static final String GENERATEDSOURCES = "generatedsources";
    private static final String GENERATEDSOURCES_PREFIX = GENERATEDSOURCES + FILE_SEPARATOR_SUBST;

    private static final Logger LOGGER = LoggerFactory.getLogger( CacheControllerImpl.class );

    private final MavenProjectHelper projectHelper;
    private final RepositorySystem repoSystem;
    private final ArtifactHandlerManager artifactHandlerManager;
    private final XmlService xmlService;
    private final CacheConfig cacheConfig;
    private final LocalCacheRepository localCache;
    private final RemoteCacheRepository remoteCache;
    private final ConcurrentMap<String, DigestItem> artifactDigestByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CacheResult> cacheResults = new ConcurrentHashMap<>();
    private final LifecyclePhasesHelper lifecyclePhasesHelper;
    private final MavenSession mavenSession;
    private volatile Scm scm;

    @Inject
    public CacheControllerImpl(
            MavenProjectHelper projectHelper,
            RepositorySystem repoSystem,
            ArtifactHandlerManager artifactHandlerManager,
            XmlService xmlService,
            LocalCacheRepository localCache,
            RemoteCacheRepository remoteCache, 
            CacheConfig cacheConfig,
            LifecyclePhasesHelper lifecyclePhasesHelper,
            MavenSession mavenSession )
    {
        this.projectHelper = projectHelper;
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.cacheConfig = cacheConfig;
        this.repoSystem = repoSystem;
        this.artifactHandlerManager = artifactHandlerManager;
        this.xmlService = xmlService;
        this.lifecyclePhasesHelper = lifecyclePhasesHelper;
        this.mavenSession = mavenSession;
    }

    @Override
    @Nonnull
    public CacheResult findCachedBuild( MavenSession session, MavenProject project,
                                        List<MojoExecution> mojoExecutions )
    {
        final String highestRequestPhase = CacheUtils.getLast( mojoExecutions ).getLifecyclePhase();
        if ( !lifecyclePhasesHelper.isLaterPhaseThanClean( highestRequestPhase ) )
        {
            return empty();
        }

        LOGGER.info( "Attempting to restore project from build cache" );

        ProjectsInputInfo inputInfo = calculateInput( project, session );

        final CacheContext context = new CacheContext( project, inputInfo, session );
        // remote build first
        CacheResult result = findCachedBuild( mojoExecutions, context );

        if ( !result.isSuccess() && result.getContext() != null )
        {
            LOGGER.debug( "Remote cache is incomplete or missing, trying local build" );

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
        Build cachedBuild = null;
        try
        {
            cachedBuild = localCache.findBuild( context );
            return analyzeResult( context, mojoExecutions, cachedBuild );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Cannot read cached build", e );
            return cachedBuild != null ? failure( cachedBuild, context ) : failure( context );
        }
    }

    private CacheResult findLocalBuild( List<MojoExecution> mojoExecutions, CacheContext context )
    {
        Build localBuild = null;
        try
        {
            localBuild = localCache.findLocalBuild( context );
            return analyzeResult( context, mojoExecutions, localBuild );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Cannot read local build", e );
            return localBuild != null ? failure( localBuild, context ) : failure( context );
        }
    }

    private CacheResult analyzeResult( CacheContext context, List<MojoExecution> mojoExecutions, Build info )
    {
        try
        {
            if ( info != null )
            {
                final ProjectsInputInfo inputInfo = context.getInputInfo();

                LOGGER.info( "Found cached build, restoring from cache {}", inputInfo.getChecksum() );
                LOGGER.debug( "Cached build details: {}", info );

                final String cacheImplementationVersion = info.getCacheImplementationVersion();
                if ( !CACHE_IMPLEMENTATION_VERSION.equals( cacheImplementationVersion ) )
                {
                    LOGGER.warn(
                             "Maven and cached build implementations mismatch, caching might not work correctly. "
                                    + "Implementation version: " + CACHE_IMPLEMENTATION_VERSION + ", cached build: {}",
                             info.getCacheImplementationVersion() );
                }

                List<MojoExecution> cachedSegment = lifecyclePhasesHelper.getCachedSegment( mojoExecutions, info );
                List<MojoExecution> missingMojos = info.getMissingExecutions( cachedSegment );
                if ( !missingMojos.isEmpty() )
                {
                    LOGGER.warn( "Cached build doesn't contains all requested plugin executions "
                            + "(missing: {}), cannot restore", missingMojos );
                    return failure( info, context );
                }

                if ( !isCachedSegmentPropertiesPresent( context.getProject(), info, cachedSegment ) )
                {
                    LOGGER.info( "Cached build violates cache rules, cannot restore" );
                    return failure( info, context );
                }

                final String highestRequestPhase = CacheUtils.getLast( mojoExecutions ).getLifecyclePhase();
                if ( lifecyclePhasesHelper.isLaterPhaseThanBuild( highestRequestPhase, info )
                        && !canIgnoreMissingSegment( info, mojoExecutions ) )
                {
                    LOGGER.info( "Project restored partially. Highest cached goal: {}, requested: {}",
                            info.getHighestCompletedGoal(), highestRequestPhase );
                    return partialSuccess( info, context );
                }

                return success( info, context );
            }
            else
            {
                LOGGER.info( "Project is not found in cache" );
                return empty( context );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "Failed to restore project", e );
            localCache.clearCache( context );
            return empty( context );
        }
    }

    private boolean canIgnoreMissingSegment( Build info, List<MojoExecution> mojoExecutions )
    {
        final List<MojoExecution> postCachedSegment =
                lifecyclePhasesHelper.getPostCachedSegment( mojoExecutions, info );
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
        final Build build = cacheResult.getBuildInfo();
        final CacheContext context = cacheResult.getContext();
        final MavenProject project = context.getProject();

        final Artifact artifact = build.getArtifact();
        artifact.setVersion( project.getVersion() );

        try
        {
            File file = null;
            List<ArtifactToAttach> artifacts = new ArrayList<>();
            if ( isNotBlank( artifact.getFileName() ) )
            {
                // TODO if remote is forced, probably need to refresh or reconcile all files
                final Path artifactFile = localCache.getArtifactFile( context, cacheResult.getSource(), artifact );
                if ( !Files.exists( artifactFile ) )
                {
                    LOGGER.info(  "Missing file for cached build, cannot restore. File: {}", artifactFile );
                    return false;
                }
                LOGGER.debug( "Setting project artifact {} from {}", artifact.getArtifactId(), artifactFile );
                file = artifactFile.toFile();
                putChecksum( artifact, context.getInputInfo().getChecksum() );
            }

            for ( Artifact attachedArtifact : build.getAttachedArtifacts() )
            {
                attachedArtifact.setVersion( project.getVersion() );
                if ( isNotBlank( attachedArtifact.getFileName() ) )
                {
                    final Path attachedArtifactFile = localCache.getArtifactFile( context, cacheResult.getSource(),
                            attachedArtifact );
                    if ( !Files.exists( attachedArtifactFile ) )
                    {
                        throw new FileNotFoundException(
                                "Missing file for cached build, cannot restore. File: " + attachedArtifactFile );
                    }
                    LOGGER.debug( "Attaching artifact {} from {}",
                              artifact.getArtifactId(), attachedArtifactFile );
                    if ( StringUtils.startsWith( attachedArtifact.getClassifier(), GENERATEDSOURCES_PREFIX ) )
                    {
                        // generated sources artifact
                        restoreGeneratedSources( attachedArtifact, attachedArtifactFile, project );
                    }
                    else
                    {
                        artifacts.add( new ArtifactToAttach( attachedArtifact, attachedArtifactFile ) );
                    }
                    putChecksum( attachedArtifact, context.getInputInfo().getChecksum() );
                }
            }
            // Actually modify project at the end in case something went wrong during restoration,
            // in which case, the project is unmodified and we continue with normal build.
            if ( file != null )
            {
                project.getArtifact().setFile( file );
                project.getArtifact().setResolved( true );
            }
            artifacts.forEach( a -> projectHelper.attachArtifact( project, a.type, a.classifier, a.file ) );
            return true;
        }
        catch ( Exception e )
        {
            LOGGER.error( "Cannot restore cache, continuing with normal build.", e );
            return false;
        }
    }

    private void putChecksum( Artifact artifact, String projectChecksum )
    {
        final DigestItem projectArtifact = DtoUtils.createdDigestedByProjectChecksum( artifact, projectChecksum );
        final String dependencyKey = KeyUtils.getArtifactKey( artifact );
        artifactDigestByKey.put( dependencyKey, projectArtifact );

        final ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( artifact.getType() );
        String packaging = artifactHandler.getPackaging();
        if ( !Objects.equals( packaging, artifact.getType() ) )
        {
            artifactDigestByKey.put( KeyUtils.getArtifactKey( artifact, packaging ), projectArtifact );
        }
        String extension = artifactHandler.getExtension();
        if ( !Objects.equals( extension, artifact.getType() ) && !Objects.equals( extension, packaging ) )
        {
            artifactDigestByKey.put( KeyUtils.getArtifactKey( artifact, extension ), projectArtifact );
        }
    }

    private ProjectsInputInfo calculateInput( MavenProject project, MavenSession session )
    {
        try
        {
            final MavenProjectInput inputs = new MavenProjectInput( project, session, cacheConfig,
                    artifactDigestByKey, repoSystem, localCache, remoteCache );
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
            LOGGER.info( "Cannot save project in cache, skipping" );
            return;
        }

        final MavenProject project = context.getProject();
        final MavenSession session = context.getSession();
        try
        {
            attachGeneratedSources( project );
            attachOutputs( project );

            final org.apache.maven.artifact.Artifact projectArtifact = project.getArtifact();
            final HashFactory hashFactory = cacheConfig.getHashFactory();
            final HashAlgorithm algorithm = hashFactory.createAlgorithm();
            final Artifact projectArtifactDto = artifactDto( project.getArtifact(), algorithm );

            final List<org.apache.maven.artifact.Artifact> attachedArtifacts =
                    project.getAttachedArtifacts() != null ? project.getAttachedArtifacts() : Collections.emptyList();
            List<Artifact> attachedArtifactDtos = artifactDtos( attachedArtifacts, algorithm );

            List<CompletedExecution> completedExecution = buildExecutionInfo( mojoExecutions, executionEvents );

            final Build build = new Build( session.getGoals(), projectArtifactDto, attachedArtifactDtos,
                    context.getInputInfo(), completedExecution, hashFactory.getAlgorithm() );
            populateGitInfo( build, session );
            build.getDto().set_final( cacheConfig.isSaveFinal() );
            cacheResults.put( getVersionlessProjectKey( project ), rebuilded( cacheResult, build ) );

            // if package phase presence means new artifacts were packaged
            if ( project.hasLifecyclePhase( "package" ) )
            {
                localCache.beforeSave( context );
                localCache.saveBuildInfo( cacheResult, build );
                if ( projectArtifact.getFile() != null )
                {
                    localCache.saveArtifactFile( cacheResult, projectArtifact );
                    putChecksum( projectArtifactDto, context.getInputInfo().getChecksum() );
                }
                for ( org.apache.maven.artifact.Artifact attachedArtifact : attachedArtifacts )
                {
                    if ( attachedArtifact.getFile() != null && isOutputArtifact(
                            attachedArtifact.getFile().getName() ) )
                    {
                        localCache.saveArtifactFile( cacheResult, attachedArtifact );
                    }
                }
                for ( Artifact attachedArtifactDto : attachedArtifactDtos )
                {
                    putChecksum( attachedArtifactDto, context.getInputInfo().getChecksum() );
                }
            }
            else
            {
                localCache.saveBuildInfo( cacheResult, build );
            }

            if ( cacheConfig.isBaselineDiffEnabled() )
            {
                produceDiffReport( cacheResult, build );
            }

        }
        catch ( Exception e )
        {
            LOGGER.error( "Failed to save project, cleaning cache. Project: {}", project, e );
            try
            {
                localCache.clearCache( context );
            }
            catch ( Exception ex )
            {
                LOGGER.error( "Failed to clean cache due to unexpected error:", ex );
            }
        }
    }

    public void produceDiffReport( CacheResult cacheResult, Build build )
    {
        MavenProject project = cacheResult.getContext().getProject();
        Optional<Build> baselineHolder = remoteCache.findBaselineBuild( project );
        if ( baselineHolder.isPresent() )
        {
            Build baseline = baselineHolder.get();
            String outputDirectory = project.getBuild().getDirectory();
            Path reportOutputDir = Paths.get( outputDirectory, "incremental-maven" );
            LOGGER.info( "Saving cache builds diff to: {}", reportOutputDir );
            Diff diff = new CacheDiff( build.getDto(), baseline.getDto(), cacheConfig ).compare();
            try
            {
                Files.createDirectories( reportOutputDir );
                final ProjectsInputInfo baselineInputs = baseline.getDto().getProjectsInputInfo();
                final String checksum = baselineInputs.getChecksum();
                Files.write( reportOutputDir.resolve( "buildinfo-baseline-" + checksum + ".xml" ),
                        xmlService.toBytes( baseline.getDto() ), TRUNCATE_EXISTING, CREATE );
                Files.write( reportOutputDir.resolve( "buildinfo-" + checksum + ".xml" ),
                        xmlService.toBytes( build.getDto() ), TRUNCATE_EXISTING, CREATE );
                Files.write( reportOutputDir.resolve( "buildsdiff-" + checksum + ".xml" ),
                        xmlService.toBytes( diff ), TRUNCATE_EXISTING, CREATE );
                final Optional<DigestItem> pom = CacheDiff.findPom( build.getDto().getProjectsInputInfo() );
                if ( pom.isPresent() )
                {
                    Files.write( reportOutputDir.resolve( "effective-pom-" + checksum + ".xml" ),
                            pom.get().getValue().getBytes( StandardCharsets.UTF_8 ),
                            TRUNCATE_EXISTING, CREATE );
                }
                final Optional<DigestItem> baselinePom = CacheDiff.findPom( baselineInputs );
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
                LOGGER.error( "Cannot produce build diff for project", e );
            }
        }
        else
        {
            LOGGER.info( "Cannot find project in baseline build, skipping diff" );
        }
    }

    private List<Artifact> artifactDtos( List<org.apache.maven.artifact.Artifact> attachedArtifacts,
                                         HashAlgorithm digest ) throws IOException
    {
        List<Artifact> result = new ArrayList<>();
        for ( org.apache.maven.artifact.Artifact attachedArtifact : attachedArtifacts )
        {
            if ( attachedArtifact.getFile() != null && isOutputArtifact( attachedArtifact.getFile().getName() ) )
            {
                result.add( artifactDto( attachedArtifact, digest ) );
            }
        }
        return result;
    }

    private Artifact artifactDto( org.apache.maven.artifact.Artifact projectArtifact,
                                  HashAlgorithm algorithm ) throws IOException
    {
        final Artifact dto = DtoUtils.createDto( projectArtifact );
        if ( projectArtifact.getFile() != null && projectArtifact.getFile().isFile() )
        {
            final Path file = projectArtifact.getFile().toPath();
            dto.setFileHash( algorithm.hash( file ) );
            dto.setFileSize( Files.size( file ) );
        }
        return dto;
    }

    private List<CompletedExecution> buildExecutionInfo( List<MojoExecution> mojoExecutions,
                                                         Map<String, MojoExecutionEvent> executionEvents )
    {
        List<CompletedExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            final String executionKey = CacheUtils.mojoExecutionKey( mojoExecution );
            final MojoExecutionEvent executionEvent = executionEvents.get( executionKey );
            CompletedExecution executionInfo = new CompletedExecution();
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

    private void recordMojoProperties( CompletedExecution execution, MojoExecutionEvent executionEvent )
    {
        final MojoExecution mojoExecution = executionEvent.getExecution();

        final boolean logAll = cacheConfig.isLogAllProperties( mojoExecution );
        List<TrackedProperty> trackedProperties = cacheConfig.getTrackedProperties( mojoExecution );
        List<PropertyName> noLogProperties = cacheConfig.getNologProperties( mojoExecution );
        List<PropertyName> forceLogProperties = cacheConfig.getLoggedProperties( mojoExecution );
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
                LOGGER.info( "Cannot get property {} value from {}: {}", propertyName, mojo, e.getMessage() );
                if ( tracked )
                {
                    throw new IllegalArgumentException( "Property configured in cache introspection config "
                            + "for " + mojo + " is not accessible: " + propertyName );
                }
            }
        }
    }

    private boolean isExcluded( String propertyName, boolean logAll, List<PropertyName> excludedProperties,
                                List<PropertyName> forceLogProperties )
    {
        if ( !forceLogProperties.isEmpty() )
        {
            for ( PropertyName logProperty : forceLogProperties )
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
            for ( PropertyName excludedProperty : excludedProperties )
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

    private boolean isTracked( String propertyName, List<TrackedProperty> trackedProperties )
    {
        for ( TrackedProperty trackedProperty : trackedProperties )
        {
            if ( StringUtils.equals( propertyName, trackedProperty.getPropertyName() ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean isCachedSegmentPropertiesPresent( MavenProject project, Build build,
                                                      List<MojoExecution> mojoExecutions )
    {
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            // completion of all mojos checked above, so we expect tp have execution info here
            final List<TrackedProperty> trackedProperties = cacheConfig.getTrackedProperties( mojoExecution );
            final CompletedExecution cachedExecution = build.findMojoExecutionInfo( mojoExecution );

            if ( cachedExecution == null )
            {
                LOGGER.info( "Execution is not cached. Plugin: {}, goal {}",
                             mojoExecution.getExecutionId(), mojoExecution.getGoal() );
                return false;
            }

            if ( !DtoUtils.containsAllProperties( cachedExecution, trackedProperties ) )
            {
                LOGGER.info( "Build info doesn't match rules. Plugin: {}",
                             mojoExecution.getExecutionId() );
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isForcedExecution( MavenProject project, MojoExecution execution )
    {
        if ( cacheConfig.isForcedExecution( execution ) )
        {
            return true;
        }
        String alwaysRunPlugins = project.getProperties().getProperty( "remote.cache.alwaysRunPlugins" );
        if ( alwaysRunPlugins != null )
        {
            String[] alwaysRunPluginsList = split( alwaysRunPlugins, "," );
            for ( String pluginAndGoal : alwaysRunPluginsList )
            {
                String[] tokens = pluginAndGoal.split( ":" );
                String alwaysRunPlugin = tokens[0];
                String alwaysRunGoal = tokens.length == 1 ? "*" : tokens[1];
                if ( Objects.equals( execution.getPlugin().getArtifactId(), alwaysRunPlugin )
                        && ( "*".equals( alwaysRunGoal ) || Objects.equals( execution.getGoal(), alwaysRunGoal ) ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void saveCacheReport( MavenSession session )
    {
        try
        {
            CacheReport cacheReport = new CacheReport();
            for ( CacheResult result : cacheResults.values() )
            {
                ProjectReport projectReport = new ProjectReport();
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
                cacheReport.addProject( projectReport );
            }

            String buildId = UUID.randomUUID().toString();
            localCache.saveCacheReport( buildId, session, cacheReport );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Cannot save incremental build aggregated report", e );
        }
    }

    private void populateGitInfo( Build build, MavenSession session )
    {
        if ( scm == null )
        {
            synchronized ( this )
            {
                if ( scm == null )
                {
                    try
                    {
                        scm = CacheUtils.readGitInfo( session );
                    }
                    catch ( IOException e )
                    {
                        scm = new Scm();
                        LOGGER.error( "Cannot populate git info", e );
                    }
                }
            }
        }
        build.getDto().setScm( scm );
    }

    private void zipAndAttachArtifact( MavenProject project, Path dir, String classifier ) throws IOException
    {
        Path temp = Files.createTempFile( "maven-incremental", project.getArtifactId() );
        temp.toFile().deleteOnExit();
        CacheUtils.zip( dir, temp );
        projectHelper.attachArtifact( project, "zip", classifier, temp.toFile() );
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

    private void restoreGeneratedSources( Artifact artifact, Path artifactFilePath, MavenProject project )
            throws IOException
    {
        final Path targetDir = Paths.get( project.getBuild().getDirectory() );
        final Path outputDir = classifierToPath( targetDir, artifact.getClassifier() );
        if ( Files.exists( outputDir ) )
        {
            FileUtils.cleanDirectory( outputDir.toFile() );
        }
        else
        {
            Files.createDirectories( outputDir );
        }
        CacheUtils.unzip( artifactFilePath, outputDir );
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
            LOGGER.debug( "Attached directory: {}", candidateSubDir );
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

    private static class ArtifactToAttach
    {
        private final String type;
        private final String classifier;
        private final File file;

        ArtifactToAttach( String type, String classifier, File file )
        {
            this.type = type;
            this.classifier = classifier;
            this.file = file;
        }

        ArtifactToAttach( Artifact attachedArtifact, Path attachedArtifactFile )
        {
            this( attachedArtifact.getType(), attachedArtifact.getClassifier(), attachedArtifactFile.toFile() );
        }
    }

}
