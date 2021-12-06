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

import org.apache.maven.caching.PluginScanConfig;
import org.apache.maven.caching.ScanConfigProperties;
import org.apache.maven.caching.Xpp3DomUtils;
import org.apache.maven.caching.checksum.WalkKey;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWithAny;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;

/**
 * MavenProjectInput
 */
public class PluginsScanner
{

    /**
     * property name prefix to pass input files with project properties. smth like remote.cache.input.1 will be
     * accepted
     */
    private static final String CACHE_INPUT_NAME = "remote.cache.input";
    /**
     * property name prefix to exclude files from input. smth like remote.cache.exclude.1 should be set in project
     * props
     */
    private static final String CACHE_EXCLUDE_NAME = "remote.cache.exclude";
    /**
     * Flag to control if we should check values from plugin configs as file system objects
     */
    private static final String CACHE_PROCESS_PLUGINS = "remote.cache.processPlugins";

    private static final Logger LOGGER = LoggerFactory.getLogger( PluginsScanner.class );

    private final MavenProject project;
    private final CacheConfig config;
    private final List<Path> filteredOutPaths;
    private final Path baseDirPath;
    private final boolean processPlugins;

    @SuppressWarnings( "checkstyle:parameternumber" )
    public PluginsScanner( MavenProject project,
                           CacheConfig config
    )
    {
        this.project = project;
        this.config = config;
        this.baseDirPath = project.getBasedir().toPath().toAbsolutePath();
        Properties properties = project.getProperties();
        this.processPlugins = Boolean.parseBoolean(
                properties.getProperty( CACHE_PROCESS_PLUGINS, config.isProcessPlugins() ) );

        org.apache.maven.model.Build build = project.getBuild();
        filteredOutPaths = new ArrayList<>( Arrays.asList( normalizedPath( build.getDirectory() ), // target by default
                normalizedPath( build.getOutputDirectory() ), normalizedPath( build.getTestOutputDirectory() ) ) );

        for ( String propertyName : properties.stringPropertyNames() )
        {
            if ( propertyName.startsWith( CACHE_EXCLUDE_NAME ) )
            {
                filteredOutPaths.add( Paths.get( properties.getProperty( propertyName ) ) );
            }
        }
    }

    public List<InputFile> getInputFiles()
    {
        long start = System.currentTimeMillis();
        HashSet<WalkKey> visitedDirs = new HashSet<>();
        ArrayList<InputFile> inputFiles = new ArrayList<>();

        LOGGER.debug( "Scanning plugins configurations to find input files. Probing is {}", processPlugins
                ? "enabled, values will be checked for presence in file system"
                : "disabled, only tags with attribute " + CACHE_INPUT_NAME + "=\"true\" will be added" );

        if ( processPlugins )
        {
            collectFromPlugins( inputFiles, visitedDirs );
        }
        else
        {
            LOGGER.info( "Skipping plugin parameters scan (probing is disabled by config)" );
        }

        long pluginsFinished = System.currentTimeMillis() - start;

        LOGGER.info( "Found {} input files from plugins. Plugins scan time: {} millis",
                inputFiles.size(), pluginsFinished );
        LOGGER.debug( "Src input: {}", inputFiles );

        return inputFiles;
    }

    /**
     * entry point for directory walk
     */
    private void startWalk( Path candidate,
                            String glob,
                            boolean recursive,
                            List<InputFile> collectedFiles,
                            Set<WalkKey> visitedDirs, String reference )
    {
        Path normalized = candidate.isAbsolute() ? candidate : baseDirPath.resolve( candidate );
        normalized = normalized.toAbsolutePath().normalize();
        WalkKey key = new WalkKey( normalized, glob, recursive );
        if ( visitedDirs.contains( key ) || !Files.exists( normalized ) )
        {
            return;
        }

        if ( Files.isDirectory( normalized ) )
        {
            if ( baseDirPath.startsWith( normalized ) )
            { // requested to walk parent, can do only non recursive
                key = new WalkKey( normalized, glob, false );
            }
            try
            {
                walkDir( key, collectedFiles, visitedDirs, reference );
                visitedDirs.add( key );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            if ( !isFilteredOutSubpath( normalized ) )
            {
                LOGGER.debug( "Adding: {}", normalized );
                collectedFiles.add( new InputFile( normalized, reference ) );
            }
        }
    }

    private Path normalizedPath( String directory )
    {
        return Paths.get( directory ).normalize();
    }

    private void collectFromPlugins( ArrayList<InputFile> files, HashSet<WalkKey> visitedDirs )
    {
        List<Plugin> plugins = project.getBuild().getPlugins();
        for ( Plugin plugin : plugins )
        {
            PluginScanConfig scanConfig = config.getPluginDirScanConfig( plugin );

            if ( scanConfig.isSkip() )
            {
                LOGGER.debug( "Skipping plugin config scan (skip by config): {}", plugin.getArtifactId() );
                continue;
            }

            Object configuration = plugin.getConfiguration();
            LOGGER.debug( "Processing plugin config: {}", plugin.getArtifactId() );
            if ( configuration != null )
            {
                addInputsFromPluginConfigs( Xpp3DomUtils.getChildren( configuration ), scanConfig, files, visitedDirs,
                        plugin.getArtifactId() );
            }

            for ( PluginExecution exec : plugin.getExecutions() )
            {
                final PluginScanConfig executionScanConfig = config.getExecutionDirScanConfig( plugin, exec );
                PluginScanConfig mergedConfig = scanConfig.mergeWith( executionScanConfig );

                if ( mergedConfig.isSkip() )
                {
                    LOGGER.debug( "Skipping plugin execution config scan (skip by config): {}, execId: {}",
                            plugin.getArtifactId(), exec.getId() );
                    continue;
                }

                Object execConfiguration = exec.getConfiguration();
                LOGGER.debug( "Processing plugin: {}, execution: {}", plugin.getArtifactId(), exec.getId() );

                if ( execConfiguration != null )
                {
                    addInputsFromPluginConfigs( Xpp3DomUtils.getChildren( execConfiguration ), mergedConfig, files,
                            visitedDirs, plugin.getArtifactId() + ":" + exec.getId() );
                }
            }
        }
    }

    private Path walkDir( final WalkKey key,
                          final List<InputFile> collectedFiles,
                          final Set<WalkKey> visitedDirs, String reference ) throws IOException
    {
        return Files.walkFileTree( key.getPath(), new SimpleFileVisitor<Path>()
        {

            @Override
            public FileVisitResult preVisitDirectory( Path path,
                                                      BasicFileAttributes basicFileAttributes ) throws IOException
            {
                WalkKey currentDirKey = new WalkKey( path.toAbsolutePath().normalize(), key.getGlob(),
                        key.isRecursive() );
                if ( isHidden( path ) )
                {
                    LOGGER.debug( "Skipping subtree (hidden): {}", path );
                    return FileVisitResult.SKIP_SUBTREE;
                }
                else if ( isFilteredOutSubpath( path ) )
                {
                    LOGGER.debug( "Skipping subtree (blacklisted): {}", path );
                    return FileVisitResult.SKIP_SUBTREE;
                }
                else if ( visitedDirs.contains( currentDirKey ) )
                {
                    LOGGER.debug( "Skipping subtree (visited): {}", path );
                    return FileVisitResult.SKIP_SUBTREE;
                }

                walkDirectoryFiles(
                        path,
                        collectedFiles,
                        key.getGlob(),
                        entry -> filteredOutPaths.stream()
                                .anyMatch( it -> it.getFileName().equals( entry.getFileName() ) ),
                        reference );

                if ( !key.isRecursive() )
                {
                    LOGGER.debug( "Skipping subtree (non recursive): {}", path );
                    return FileVisitResult.SKIP_SUBTREE;
                }

                LOGGER.debug( "Visiting subtree: {}", path );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    private void addInputsFromPluginConfigs( Object[] configurationChildren,
                                             PluginScanConfig scanConfig,
                                             ArrayList<InputFile> files, HashSet<WalkKey> visitedDirs,
                                             String reference )
    {
        if ( configurationChildren == null )
        {
            return;
        }

        for ( Object configChild : configurationChildren )
        {
            String tagName = Xpp3DomUtils.getName( configChild );
            String tagValue = Xpp3DomUtils.getValue( configChild );

            if ( !scanConfig.accept( tagName ) )
            {
                LOGGER.debug( "Skipping property (scan config)): {}, value: {}",
                        tagName, stripToEmpty( tagValue ) );
                continue;
            }

            LOGGER.debug( "Checking xml tag. Tag: {}, value: {}", tagName, stripToEmpty( tagValue ) );

            addInputsFromPluginConfigs( Xpp3DomUtils.getChildren( configChild ), scanConfig, files, visitedDirs,
                    reference + ":" + tagName );

            final ScanConfigProperties propertyConfig = scanConfig.getTagScanProperties( tagName );
            final String glob = defaultIfEmpty( propertyConfig.getGlob(), "*" );
            if ( "true".equals( Xpp3DomUtils.getAttribute( configChild, CACHE_INPUT_NAME ) ) )
            {
                LOGGER.info( "Found tag marked with {} attribute. Tag: {}, value: {}",
                        CACHE_INPUT_NAME, tagName, tagValue );
                startWalk( Paths.get( tagValue ), glob, propertyConfig.isRecursive(), files, visitedDirs, reference );
            }
            else
            {
                final Path candidate = getPathOrNull( tagValue );
                if ( candidate != null )
                {
                    startWalk( candidate, glob, propertyConfig.isRecursive(), files, visitedDirs, reference );
                    if ( "descriptorRef".equals( tagName ) )
                    { // hardcoded logic for assembly plugin which could reference files omitting .xml suffix
                        startWalk( Paths.get( tagValue + ".xml" ), glob, propertyConfig.isRecursive(), files,
                                visitedDirs, reference );
                    }
                }
            }
        }
    }

    private Path getPathOrNull( String text )
    {
        // small optimization to not probe not-paths
        boolean blacklisted = isBlank( text )
                || equalsAnyIgnoreCase( text, "true", "false", "utf-8", "null", "\\" ) // common values
                || contains( text, "*" ) // tag value is a glob or regex - unclear how to process
                || ( contains( text, ":" ) && !contains( text, ":\\" ) )// artifactId
                || startsWithAny( text, "com.", "org.", "io.", "java.", "javax." ) // java packages
                || startsWithAny( text, "${env." ) // env variables in maven notation
                || startsWithAny( text, "http:", "https:", "scm:", "ssh:", "git:", "svn:", "cp:",
                "classpath:" ); // urls identified by common protocols
        if ( !blacklisted )
        {
            try
            {
                return Paths.get( text );
            }
            catch ( Exception ignore )
            {
            }
        }
        LOGGER.debug( "{}: {}", text, blacklisted ? "skipped(blacklisted literal)" : "invalid path" );
        return null;
    }

    static void walkDirectoryFiles( Path dir, List<InputFile> collectedFiles, String glob,
                                    Predicate<Path> mustBeSkipped, String reference )
    {
        if ( !Files.isDirectory( dir ) )
        {
            return;
        }

        try
        {
            try ( DirectoryStream<Path> stream = Files.newDirectoryStream( dir, glob ) )
            {
                for ( Path entry : stream )
                {
                    if ( mustBeSkipped.test( entry ) )
                    {
                        continue;
                    }
                    File file = entry.toFile();
                    if ( file.isFile() && !isHidden( entry ) )
                    {
                        collectedFiles.add( new InputFile( entry, reference ) );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Cannot process directory: " + dir, e );
        }
    }

    private static boolean isHidden( Path entry ) throws IOException
    {
        return Files.isHidden( entry ) || entry.toFile().getName().startsWith( "." );
    }

    private boolean isFilteredOutSubpath( Path path )
    {
        Path normalized = path.normalize();
        for ( Path filteredOutDir : filteredOutPaths )
        {
            if ( normalized.startsWith( filteredOutDir ) )
            {
                return true;
            }
        }
        return false;
    }

}
