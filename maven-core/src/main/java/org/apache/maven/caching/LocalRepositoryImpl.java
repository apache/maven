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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.caching.xml.domain.ArtifactType;
import org.apache.maven.caching.xml.domain.BuildInfoType;
import org.apache.maven.caching.xml.domain.Scm;
import org.apache.maven.caching.xml.report.CacheReportType;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.maven.caching.ProjectUtils.getMultimoduleRoot;
import static org.apache.maven.caching.checksum.MavenProjectInput.CACHE_IMPLMENTATION_VERSION;

/**
 * LocalRepositoryImpl
 */
@Component( role = LocalArtifactsRepository.class )
public class LocalRepositoryImpl implements LocalArtifactsRepository
{

    private static final String BUILDINFO_XML = "buildinfo.xml";
    private static final String LOOKUPINFO_XML = "lookupinfo.xml";
    private static final long ONE_HOUR_MILLIS = HOURS.toMillis( 1 );
    private static final long ONE_MINUTE_MILLIS = MINUTES.toMillis( 1 );
    private static final long ONE_DAY_MILLIS = DAYS.toMillis( 1 );
    private static final String EMPTY = "";
    private static final LastModifiedComparator LAST_MODIFIED_COMPARATOR = new LastModifiedComparator();
    private static final Function<Pair<BuildInfo, File>, Long> GET_LAST_MODIFIED =
            pair -> pair.getRight().lastModified();

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private RemoteArtifactsRepository remoteRepository;

    @Requirement
    private XmlService xmlService;

    @Requirement
    private CacheConfig cacheConfig;

    private final LoadingCache<Pair<MavenSession, Dependency>, Optional<BuildInfo>> bestBuildCache =
            CacheBuilder.newBuilder().build(
                    CacheLoader.from( new Function<Pair<MavenSession, Dependency>, Optional<BuildInfo>>()
                    {
                        @Override
                        public Optional<BuildInfo> apply( Pair<MavenSession, Dependency> input )
                        {
                            try
                            {
                                return findBestMatchingBuildImpl( input );
                            }
                            catch ( IOException e )
                            {
                                logger.error( "Cannot find dependency in cache", e );
                                return Optional.absent();
                            }
                        }
                    } ) );

    @Override
    public BuildInfo findLocalBuild( CacheContext context ) throws IOException
    {
        Path localBuildInfoPath = localBuildPath( context, BUILDINFO_XML, false );
        logDebug( context, "Checking local build info: " + localBuildInfoPath );
        if ( Files.exists( localBuildInfoPath ) )
        {
            logInfo( context, "Local build found by checksum " + context.getInputInfo().getChecksum() );
            try
            {
                final BuildInfoType dto = xmlService.fromFile( BuildInfoType.class, localBuildInfoPath.toFile() );
                return new BuildInfo( dto, CacheSource.LOCAL );
            }
            catch ( Exception e )
            {
                logger.error( "Local build info is not valid, deleting:  " + localBuildInfoPath, e );
                Files.delete( localBuildInfoPath );
            }
        }
        return null;
    }

    @Override
    public BuildInfo findBuild( CacheContext context ) throws IOException
    {

        Path buildInfoPath = remoteBuildPath( context, BUILDINFO_XML );
        logDebug( context, "Checking if build is already downloaded: " + buildInfoPath );

        if ( Files.exists( buildInfoPath ) )
        {
            logInfo( context, "Downloaded build found by checksum " + context.getInputInfo().getChecksum() );
            try
            {
                final BuildInfoType dto = xmlService.fromFile( BuildInfoType.class, buildInfoPath.toFile() );
                return new BuildInfo( dto, CacheSource.REMOTE );
            }
            catch ( Exception e )
            {
                logger.error( "Downloaded build info is not valid, deleting:  " + buildInfoPath, e );
                Files.delete( buildInfoPath );
            }
        }

        if ( !cacheConfig.isRemoteCacheEnabled() )
        {
            return null;
        }

        try
        {

            Path lookupInfoPath = remoteBuildPath( context, LOOKUPINFO_XML );
            if ( Files.exists( lookupInfoPath ) )
            {
                final BasicFileAttributes fileAttributes = Files.readAttributes( lookupInfoPath,
                        BasicFileAttributes.class );
                final long lastModified = fileAttributes.lastModifiedTime().toMillis();
                final long created = fileAttributes.creationTime().toMillis();
                final long now = System.currentTimeMillis();
                //  throttle remote cache calls, maven like
                if ( now < created + ONE_HOUR_MILLIS && now < lastModified + ONE_MINUTE_MILLIS )
                { // fresh file, allow lookup every minute
                    logInfo( context, "Skipping remote lookup, last unsuccessful lookup less than 1m ago." );
                    return null;
                }
                else if ( now < created + ONE_DAY_MILLIS && now < lastModified + ONE_HOUR_MILLIS )
                { // less than 1 day file, allow 1 per hour lookup
                    logInfo( context, "Skipping remote lookup, last unsuccessful lookup less than 1h ago." );
                    return null;
                }
                else if ( now > created + ONE_DAY_MILLIS && now < lastModified + ONE_DAY_MILLIS )
                {  // more than 1 day file, allow 1 per day lookup
                    logInfo( context, "Skipping remote lookup, last unsuccessful lookup less than 1d ago." );
                    return null;
                }
            }

            final BuildInfo buildInfo = remoteRepository.findBuild( context );
            if ( buildInfo != null )
            {
                logInfo( context, "Build info downloaded from remote repo, saving to:  " + buildInfoPath );
                Files.createDirectories( buildInfoPath.getParent() );
                Files.write( buildInfoPath, xmlService.toBytes( buildInfo.getDto() ), CREATE_NEW );
            }
            else
            {
                FileUtils.touch( lookupInfoPath.toFile() );
            }
            return buildInfo;
        }
        catch ( Exception e )
        {
            logger.error( "Remote build info is not valid, cached data is not compatible", e );
            return null;
        }
    }

    @Override
    public void clearCache( CacheContext context )
    {
        try
        {
            final Path buildCacheDir = buildCacheDir( context );
            Path artifactCacheDir = buildCacheDir.getParent();

            if ( !Files.exists( artifactCacheDir ) )
            {
                return;
            }

            List<Path> cacheDirs = new ArrayList<>();
            for ( Path dir : Files.newDirectoryStream( artifactCacheDir ) )
            {
                if ( Files.isDirectory( dir ) )
                {
                    cacheDirs.add( dir );
                }
            }
            if ( cacheDirs.size() > cacheConfig.getMaxLocalBuildsCached() )
            {
                Collections.sort( cacheDirs, LAST_MODIFIED_COMPARATOR );
                for ( Path dir : cacheDirs.subList( 0, cacheDirs.size() - cacheConfig.getMaxLocalBuildsCached() ) )
                {
                    FileUtils.deleteDirectory( dir.toFile() );
                }
            }
            final Path path = localBuildDir( context );
            if ( Files.exists( path ) )
            {
                FileUtils.deleteDirectory( path.toFile() );
            }
        }
        catch ( IOException e )
        {
            final String artifactId = context.getProject().getArtifactId();
            throw new RuntimeException(
                    "Failed to cleanup local cache of " + artifactId + " on build failure, it might be inconsistent",
                    e );
        }
    }

    @Override
    public Optional<BuildInfo> findBestMatchingBuild( MavenSession session, Dependency dependency )
    {
        return bestBuildCache.getUnchecked( Pair.of( session, dependency ) );
    }


    private Optional<BuildInfo> findBestMatchingBuildImpl( Pair<MavenSession, Dependency> dependencySession )
            throws IOException
    {
        final MavenSession session = dependencySession.getLeft();
        final Dependency dependency = dependencySession.getRight();

        final Path artifactCacheDir = artifactCacheDir( session, dependency.getGroupId(), dependency.getArtifactId() );

        final Multimap<Pair<String, String>, Pair<BuildInfo, File>> filesByVersion = ArrayListMultimap.create();

        Files.walkFileTree( artifactCacheDir, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile( Path o, BasicFileAttributes basicFileAttributes )
            {
                final File file = o.toFile();
                if ( file.getName().equals( BUILDINFO_XML ) )
                {
                    try
                    {
                        final BuildInfoType dto = xmlService.fromFile( BuildInfoType.class, file );
                        final Pair<BuildInfo, File> buildInfoAndFile = Pair.of( new BuildInfo( dto, CacheSource.LOCAL ),
                                file );
                        final String cachedVersion = dto.getArtifact().getVersion();
                        final String cachedBranch = getScmRef( dto.getScm() );
                        filesByVersion.put( Pair.of( cachedVersion, cachedBranch ), buildInfoAndFile );
                        if ( isNotBlank( cachedBranch ) )
                        {
                            filesByVersion.put( Pair.of( EMPTY, cachedBranch ), buildInfoAndFile );
                        }
                        if ( isNotBlank( cachedVersion ) )
                        {
                            filesByVersion.put( Pair.of( cachedVersion, EMPTY ), buildInfoAndFile );
                        }
                    }
                    catch ( Exception e )
                    {
                        // version is unusable nothing we can do here
                        logger.error( "Build info is not compatible to current maven implementation: " + file );
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        } );

        if ( filesByVersion.isEmpty() )
        {
            return Optional.absent();
        }

        final String currentRef = getScmRef( ProjectUtils.readGitInfo( session ) );
        // first lets try by branch and version
        Collection<Pair<BuildInfo, File>> bestMatched = new LinkedList<>();
        if ( isNotBlank( currentRef ) )
        {
            bestMatched = filesByVersion.get( Pair.of( dependency.getVersion(), currentRef ) );
        }
        if ( Iterables.isEmpty( bestMatched ) )
        {
            // then by version
            bestMatched = filesByVersion.get( Pair.of( dependency.getVersion(), EMPTY ) );
        }
        if ( Iterables.isEmpty( bestMatched ) && isNotBlank( currentRef ) )
        {
            // then by branch
            bestMatched = filesByVersion.get( Pair.of( EMPTY, currentRef ) );
        }
        if ( Iterables.isEmpty( bestMatched ) )
        {
            // ok lets take all
            bestMatched = filesByVersion.values();
        }

        List<Pair<BuildInfo, File>> orderedFiles = Ordering.natural().onResultOf(
                GET_LAST_MODIFIED ).reverse().sortedCopy( bestMatched );
        return Optional.of( orderedFiles.get( 0 ).getLeft() );
    }

    private String getScmRef( Scm scm )
    {
        if ( scm != null )
        {
            return scm.getSourceBranch() != null ? scm.getSourceBranch() : scm.getRevision();
        }
        else
        {
            return EMPTY;
        }
    }

    @Override
    public Path getArtifactFile( CacheContext context, CacheSource source, ArtifactType artifact ) throws IOException
    {
        if ( source == CacheSource.LOCAL )
        {
            return localBuildPath( context, artifact.getFileName(), false );
        }
        else
        {
            Path cachePath = remoteBuildPath( context, artifact.getFileName() );
            if ( !Files.exists( cachePath ) && cacheConfig.isRemoteCacheEnabled() )
            {
                final byte[] artifactContent = remoteRepository.getArtifactContent( context, artifact );
                if ( artifactContent != null )
                {
                    Files.write( cachePath, artifactContent, CREATE_NEW );
                }
            }
            return cachePath;
        }
    }

    @Override
    public void beforeSave( CacheContext environment )
    {
        clearCache( environment );
    }

    @Override
    public void saveBuildInfo( CacheResult cacheResult, BuildInfo buildInfo ) throws IOException
    {
        final Path path = localBuildPath( cacheResult.getContext(), BUILDINFO_XML, true );
        Files.write( path, xmlService.toBytes( buildInfo.getDto() ), TRUNCATE_EXISTING, CREATE );
        if ( cacheConfig.isRemoteCacheEnabled() && cacheConfig.isSaveToRemote() && !cacheResult.isFinal() )
        {
            remoteRepository.saveBuildInfo( cacheResult, buildInfo );
        }
    }

    @Override
    public void saveCacheReport( String buildId, MavenSession session, CacheReportType cacheReport ) throws IOException
    {
        Path path = Paths.get( getMultimoduleRoot( session ), "target", "maven-incremental" );
        Files.createDirectories( path );
        Files.write( path.resolve( "cache-report." + buildId + ".xml" ), xmlService.toBytes( cacheReport ),
                TRUNCATE_EXISTING, CREATE );
        if ( cacheConfig.isRemoteCacheEnabled() && cacheConfig.isSaveToRemote() )
        {
            logger.info( "[CACHE] Saving cache report on build completion" );
            remoteRepository.saveCacheReport( buildId, session, cacheReport );
        }
    }

    @Override
    public void saveArtifactFile( CacheResult cacheResult, Artifact artifact ) throws IOException
    {
        // safe artifacts to cache
        File artifactFile = artifact.getFile();
        Path cachePath = localBuildPath( cacheResult.getContext(), ProjectUtils.normalizedName( artifact ), true );
        Files.copy( artifactFile.toPath(), cachePath, StandardCopyOption.REPLACE_EXISTING );
        if ( cacheConfig.isRemoteCacheEnabled() && cacheConfig.isSaveToRemote() && !cacheResult.isFinal() )
        {
            remoteRepository.saveArtifactFile( cacheResult, artifact );
        }
    }

    private Path buildCacheDir( CacheContext context ) throws IOException
    {
        final MavenProject project = context.getProject();
        final Path artifactCacheDir = artifactCacheDir( context.getSession(), project.getGroupId(),
                project.getArtifactId() );
        return artifactCacheDir.resolve( context.getInputInfo().getChecksum() );
    }

    private Path artifactCacheDir( MavenSession session, String groupId, String artifactId ) throws IOException
    {
        final String localRepositoryRoot = session.getLocalRepository().getBasedir();
        final Path path = Paths.get( localRepositoryRoot, "..", "cache", CACHE_IMPLMENTATION_VERSION, groupId,
                artifactId ).normalize();
        if ( !Files.exists( path ) )
        {
            Files.createDirectories( path );
        }
        return path;
    }

    private Path remoteBuildPath( CacheContext context, String filename ) throws IOException
    {
        return buildCacheDir( context ).resolve( filename );
    }

    private Path localBuildPath( CacheContext context, String filename, boolean createDir ) throws IOException
    {
        final Path localBuildDir = localBuildDir( context );
        if ( createDir )
        {
            Files.createDirectories( localBuildDir );
        }
        return localBuildDir.resolve( filename );
    }

    private Path localBuildDir( CacheContext context ) throws IOException
    {
        return buildCacheDir( context ).resolve( "local" );
    }

    private void logDebug( CacheContext context, String message )
    {
        logger.debug( "[CACHE][" + context.getProject().getArtifactId() + "] " + message );
    }

    private void logInfo( CacheContext context, String message )
    {
        logger.info( "[CACHE][" + context.getProject().getArtifactId() + "] " + message );
    }

    private static class LastModifiedComparator implements Comparator<Path>
    {
        @Override
        public int compare( Path p1, Path p2 )
        {
            try
            {
                return Files.getLastModifiedTime( p1 ).compareTo( Files.getLastModifiedTime( p2 ) );
            }
            catch ( IOException e )
            {
                return 0;
            }
        }
    }
}
