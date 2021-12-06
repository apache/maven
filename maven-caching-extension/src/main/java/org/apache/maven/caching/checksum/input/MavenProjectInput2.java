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
package org.apache.maven.caching.checksum.input;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.caching.CacheUtils;
import org.apache.maven.caching.MultiModuleSupport;
import org.apache.maven.caching.NormalizedModelProvider;
import org.apache.maven.caching.ProjectInputCalculator;
import org.apache.maven.caching.RemoteCacheRepository;
import org.apache.maven.caching.checksum.DependencyNotResolvedException;
import org.apache.maven.caching.checksum.DigestUtils;
import org.apache.maven.caching.checksum.KeyUtils;
import org.apache.maven.caching.hash.HashAlgorithm;
import org.apache.maven.caching.hash.HashChecksum;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.DtoUtils;
import org.apache.maven.caching.xml.build.DigestItem;
import org.apache.maven.caching.xml.build.ProjectsInputInfo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.replaceEachRepeatedly;
import static org.apache.maven.caching.CacheUtils.isPom;
import static org.apache.maven.caching.CacheUtils.isSnapshot;

/**
 * MavenProjectInput
 */
public class MavenProjectInput2
{


    /**
     * property name to pass glob value. The glob to be used to list directory files in plugins scanning
     */
    private static final String CACHE_INPUT_GLOB_NAME = "remote.cache.input.glob";
    /**
     * property name prefix to pass input files with project properties. smth like remote.cache.input.1 will be
     * accepted
     */
    private static final String CACHE_INPUT_NAME = "remote.cache.input";
    /**
     * Flag to control if we should check values from plugin configs as file system objects
     */
    private static final String CACHE_PROCESS_PLUGINS = "remote.cache.processPlugins";

    private static final Logger LOGGER = LoggerFactory.getLogger( MavenProjectInput2.class );

    private final MavenProject project;
    private final MavenSession session;
    private final RemoteCacheRepository remoteCache;
    private final RepositorySystem repoSystem;
    private final CacheConfig config;
    private final NormalizedModelProvider normalizedModelProvider;
    private final MultiModuleSupport multiModuleSupport;
    private final ProjectInputCalculator projectInputCalculator;
    private final Path baseDirPath;
    private final boolean processPlugins;

    @SuppressWarnings( "checkstyle:parameternumber" )
    public MavenProjectInput2( MavenProject project,
                               NormalizedModelProvider normalizedModelProvider,
                               MultiModuleSupport multiModuleSupport,
                               ProjectInputCalculator projectInputCalculator,
                               MavenSession session,
                               CacheConfig config,
                               RepositorySystem repoSystem,
                               RemoteCacheRepository remoteCache )
    {
        this.project = project;
        this.normalizedModelProvider = normalizedModelProvider;
        this.multiModuleSupport = multiModuleSupport;
        this.projectInputCalculator = projectInputCalculator;
        this.session = session;
        this.config = config;
        this.baseDirPath = project.getBasedir().toPath().toAbsolutePath();
        this.repoSystem = repoSystem;
        this.remoteCache = remoteCache;
        Properties properties = project.getProperties();
        this.processPlugins = Boolean.parseBoolean(
                properties.getProperty( CACHE_PROCESS_PLUGINS, config.isProcessPlugins() ) );
    }

    public ProjectsInputInfo calculateChecksum() throws IOException
    {
        final long t0 = System.currentTimeMillis();

        final String effectivePom = getEffectivePom( normalizedModelProvider.normalizedModel( project ) );
        final SortedSet<Path> inputFiles = isPom( project ) ? Collections.emptySortedSet() : getInputFiles();
        final SortedMap<String, String> dependenciesChecksum = getMutableDependencies();

        final long t1 = System.currentTimeMillis();

        // hash items: effective pom + input files + dependencies
        final int count = 1 + inputFiles.size() + dependenciesChecksum.size();
        final List<DigestItem> items = new ArrayList<>( count );
        final HashChecksum checksum = config.getHashFactory().createChecksum( count );

        Optional<ProjectsInputInfo> baselineHolder = Optional.empty();
        if ( config.isBaselineDiffEnabled() )
        {
            baselineHolder = remoteCache.findBaselineBuild( project ).map( b -> b.getDto().getProjectsInputInfo() );
        }

        DigestItem effectivePomChecksum = DigestUtils.pom( checksum, effectivePom );
        items.add( effectivePomChecksum );
        final boolean compareWithBaseline = config.isBaselineDiffEnabled() && baselineHolder.isPresent();
        if ( compareWithBaseline )
        {
            checkEffectivePomMatch( baselineHolder.get(), effectivePomChecksum );
        }

        boolean sourcesMatched = true;
        for ( Path file : inputFiles )
        {
            DigestItem fileDigest = DigestUtils.file( checksum, baseDirPath, file );
            items.add( fileDigest );
            if ( compareWithBaseline )
            {
                sourcesMatched &= checkItemMatchesBaseline( baselineHolder.get(), fileDigest );
            }
        }
        if ( compareWithBaseline )
        {
            LOGGER.info( "Source code: {}", sourcesMatched ? "MATCHED" : "OUT OF DATE" );
        }

        boolean dependenciesMatched = true;
        for ( Map.Entry<String, String> entry : dependenciesChecksum.entrySet() )
        {
            DigestItem dependencyDigest = DigestUtils.dependency( checksum, entry.getKey(), entry.getValue() );
            items.add( dependencyDigest );
            if ( compareWithBaseline )
            {
                dependenciesMatched &= checkItemMatchesBaseline( baselineHolder.get(), dependencyDigest );
            }
        }

        if ( compareWithBaseline )
        {
            LOGGER.info( "Dependencies: {}", dependenciesMatched ? "MATCHED" : "OUT OF DATE" );
        }

        final ProjectsInputInfo projectsInputInfoType = new ProjectsInputInfo();
        projectsInputInfoType.setChecksum( checksum.digest() );
        projectsInputInfoType.getItems().addAll( items );

        final long t2 = System.currentTimeMillis();

        for ( DigestItem item : projectsInputInfoType.getItems() )
        {
            LOGGER.debug( "Hash calculated, item: {}, hash: {}", item.getType(), item.getHash() );
        }
        LOGGER.info( "Project inputs calculated in {} ms. {} checksum [{}] calculated in {} ms.",
                t1 - t0, config.getHashFactory().getAlgorithm(), projectsInputInfoType.getChecksum(), t2 - t1 );
        return projectsInputInfoType;
    }

    private void checkEffectivePomMatch( ProjectsInputInfo baselineBuild, DigestItem effectivePomChecksum )
    {
        Optional<DigestItem> pomHolder = Optional.empty();
        for ( DigestItem it : baselineBuild.getItems() )
        {
            if ( it.getType().equals( "pom" ) )
            {
                pomHolder = Optional.of( it );
                break;
            }
        }

        if ( pomHolder.isPresent() )
        {
            DigestItem pomItem = pomHolder.get();
            final boolean matches = StringUtils.equals( pomItem.getHash(), effectivePomChecksum.getHash() );
            if ( !matches )
            {
                LOGGER.info( "Mismatch in effective poms. Current: {}, remote: {}",
                        effectivePomChecksum.getHash(), pomItem.getHash() );
            }
            LOGGER.info( "Effective pom: {}", matches ? "MATCHED" : "OUT OF DATE" );
        }
    }

    private boolean checkItemMatchesBaseline( ProjectsInputInfo baselineBuild, DigestItem fileDigest )
    {
        Optional<DigestItem> baselineFileDigest = Optional.empty();
        for ( DigestItem it : baselineBuild.getItems() )
        {
            if ( it.getType().equals( fileDigest.getType() )
                    && fileDigest.getValue().equals( it.getValue().trim() ) )
            {
                baselineFileDigest = Optional.of( it );
                break;
            }
        }

        boolean matched = false;
        if ( baselineFileDigest.isPresent() )
        {
            String hash = baselineFileDigest.get().getHash();
            matched = StringUtils.equals( hash, fileDigest.getHash() );
            if ( !matched )
            {
                LOGGER.info( "Mismatch in {}: {}. Local hash: {}, remote: {}",
                        fileDigest.getType(), fileDigest.getValue(), fileDigest.getHash(), hash );
            }
        }
        else
        {
            LOGGER.info( "Mismatch in {}: {}. Not found in remote cache",
                    fileDigest.getType(), fileDigest.getValue() );
        }
        return matched;
    }

    /**
     * @param prototype effective model fully resolved by maven build. Do not pass here just parsed Model.
     */
    private String getEffectivePom( Model prototype ) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( output );
            new MavenXpp3Writer().write( writer, prototype );

            //normalize env specifics
            final String[] searchList =
                    {baseDirPath.toString(), "\\", "windows", "linux"
                    };
            final String[] replacementList =
                    {"", "/", "os.classifier", "os.classifier"
                    };

            return replaceEachRepeatedly( output.toString(), searchList, replacementList );

        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private SortedSet<Path> getInputFiles()
    {
        long start = System.currentTimeMillis();

        WalksBuilder builder = new WalksBuilder( project, config.getDefaultSelector(), config.getExtraSelectors() );
        FilesCollector filesCollector = new FilesCollector( project.getBasedir().toPath(), builder.build() );
        ArrayList<Path> collectedFiles = new ArrayList<>( filesCollector.collect() );

        long walkKnownPathsFinished = System.currentTimeMillis() - start;

        LOGGER.info( "Scanning plugins configurations to find input files. Probing is {}", processPlugins
                ? "enabled, values will be checked for presence in file system"
                : "disabled, only tags with attribute " + CACHE_INPUT_NAME + "=\"true\" will be added" );

        if ( processPlugins )
        {
            PluginsScanner scanner = new PluginsScanner( project, config );
            List<InputFile> pluginsFileSet = scanner.getInputFiles();
            collectedFiles.addAll( pluginsFileSet.stream().map( it -> it.path ).collect( Collectors.toList() ) );
        }
        else
        {
            LOGGER.info( "Skipping check plugins scan (probing is disabled by config)" );
        }

        long pluginsFinished = System.currentTimeMillis() - start - walkKnownPathsFinished;

        TreeSet<Path> sorted = new TreeSet<>( new PathIgnoringCaseComparator() );
        for ( Path collectedFile : collectedFiles )
        {
            sorted.add( collectedFile.normalize().toAbsolutePath() );
        }

        LOGGER.info( "Found {} input files. Project dir processing: {}, plugins: {} millis",
                sorted.size(), walkKnownPathsFinished, pluginsFinished );
        LOGGER.debug( "Src input: {}", sorted );

        return sorted;
    }

    private SortedMap<String, String> getMutableDependencies() throws IOException
    {
        SortedMap<String, String> result = new TreeMap<>();

        for ( Dependency dependency : project.getDependencies() )
        {

            if ( CacheUtils.isPom( dependency ) )
            {
                // POM dependency will be resolved by maven system to actual dependencies
                // and will contribute to effective pom.
                // Effective result will be recorded by #getNormalizedPom
                // so pom dependencies must be skipped as meaningless by themselves
                continue;
            }

            // saved to index by the end of dependency build
            MavenProject dependencyProject = multiModuleSupport.tryToResolveProject(
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getVersion() )
                    .orElse( null );
            boolean isSnapshot = isSnapshot( dependency.getVersion() );
            if ( dependencyProject == null && !isSnapshot )
            {
                // external immutable dependency, should skip
                continue;
            }
            String projectHash;
            if ( dependencyProject != null ) //part of multi module
            {
                projectHash = projectInputCalculator.calculateInput( dependencyProject ).getChecksum();
            }
            else //this is a snapshot dependency
            {
                DigestItem resolved = resolveArtifact(
                        repoSystem.createDependencyArtifact( dependency ),
                        false );
                projectHash = resolved.getHash();
            }
            result.put(
                    KeyUtils.getVersionlessArtifactKey( repoSystem.createDependencyArtifact( dependency ) ),
                    projectHash );
        }
        return result;
    }

    @Nonnull
    private DigestItem resolveArtifact( final Artifact dependencyArtifact,
                                        boolean isOffline ) throws IOException
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact( dependencyArtifact )
                .setResolveRoot( true )
                .setResolveTransitively( false )
                .setLocalRepository( session.getLocalRepository() )
                .setRemoteRepositories( project.getRemoteArtifactRepositories() )
                .setOffline( session.isOffline() || isOffline )
                .setForceUpdate( session.getRequest().isUpdateSnapshots() )
                .setServers( session.getRequest().getServers() )
                .setMirrors( session.getRequest().getMirrors() )
                .setProxies( session.getRequest().getProxies() );

        final ArtifactResolutionResult result = repoSystem.resolve( request );

        if ( !result.isSuccess() )
        {
            throw new DependencyNotResolvedException( "Cannot resolve in-project dependency: " + dependencyArtifact );
        }

        if ( !result.getMissingArtifacts().isEmpty() )
        {
            throw new DependencyNotResolvedException(
                    "Cannot resolve artifact: " + dependencyArtifact + ", missing: " + result.getMissingArtifacts() );
        }

        if ( result.getArtifacts().size() != 1 )
        {
            throw new IllegalStateException(
                    "Unexpected number of artifacts returned. Requested: " + dependencyArtifact
                            + ", expected: 1, actual: " + result.getArtifacts() );
        }

        final Artifact resolved = result.getArtifacts().iterator().next();

        final HashAlgorithm algorithm = config.getHashFactory().createAlgorithm();
        final String hash = algorithm.hash( resolved.getFile().toPath() );
        return DtoUtils.createDigestedFile( resolved, hash );
    }

    /**
     * PathIgnoringCaseComparator
     */
    public static class PathIgnoringCaseComparator implements Comparator<Path>
    {

        @Override
        public int compare( Path f1, Path f2 )
        {
            String s1 = f1.toAbsolutePath().toString();
            String s2 = f2.toAbsolutePath().toString();
            if ( File.separator.equals( "\\" ) )
            {
                s1 = s1.replaceAll( "\\\\", "/" );
                s2 = s2.replaceAll( "\\\\", "/" );
            }
            return s1.compareToIgnoreCase( s2 );
        }
    }

}
