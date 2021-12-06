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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MavenProjectInput
 */
public class FilesCollector
{

    private static final Logger LOGGER = LoggerFactory.getLogger( FilesCollector.class );

    private List<DirectorySpec> walks;
    private Path baseDir;

    @SuppressWarnings( "checkstyle:parameternumber" )
    public FilesCollector( Path baseDir, List<DirectorySpec> walks )
    {
        this.walks = walks;
        this.baseDir = baseDir;
    }

    public List<Path> collect()
    {
        long start = System.currentTimeMillis();
        List<Path> files = new LinkedList<>();

        walks.forEach( walk -> collect( walk, files ) );

        long walkKnownPathsFinished = System.currentTimeMillis() - start;

        LOGGER.info( "Found {} input files. Project dir processing: {} ms", files.size(), walkKnownPathsFinished );
        LOGGER.debug( "Src input: {}", files );

        return files;
    }

    /**
     * entry point for directory directorySpec
     */
    private void collect( DirectorySpec directorySpec,
                          List<Path> collectedFiles )
    {

        Path directory = directorySpec.getDirectory();


        if ( !Files.isDirectory( directory ) )
        {
            LOGGER.warn( "Provided for scanning directory is not accessible: {}", directory );
        }

        directory = directory.isAbsolute() ? directory : baseDir.resolve( directory ).toAbsolutePath();
        directory = directory.normalize();

        FileSystem fileSystem = directory.getFileSystem();

        List<PathMatcher> includes =
                directorySpec.getIncludes().stream().map( fileSystem::getPathMatcher ).collect( Collectors.toList() );
        List<PathMatcher> excludes =
                directorySpec.getExcludes().stream().map( fileSystem::getPathMatcher ).collect( Collectors.toList() );

        try
        {
            Files.walkFileTree( directory, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                {
                    boolean matched = includes.stream().anyMatch( include -> include.matches( file ) )
                            && excludes.stream().noneMatch( exclude -> exclude.matches( file ) );
                    if ( matched )
                    {
                        collectedFiles.add( file );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Error visiting directory {}", directory, e );
            throw new RuntimeException( e );
        }
    }
}
