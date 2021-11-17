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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.caching.xml.build.Scm;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.SessionData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.maven.artifact.Artifact.LATEST_VERSION;
import static org.apache.maven.artifact.Artifact.SNAPSHOT_VERSION;

/**
 * ProjectUtils
 */
public class CacheUtils
{

    public static boolean isPom( MavenProject project )
    {
        return project.getPackaging().equals( "pom" );
    }

    public static boolean isPom( Dependency dependency )
    {
        return dependency.getType().equals( "pom" );
    }

    public static boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT_VERSION ) || version.endsWith( LATEST_VERSION );
    }

    public static String normalizedName( Artifact artifact )
    {
        if ( artifact.getFile() == null )
        {
            return null;
        }

        StringBuilder filename = new StringBuilder( artifact.getArtifactId() );

        if ( artifact.hasClassifier() )
        {
            filename.append( "-" ).append( artifact.getClassifier() );
        }

        final ArtifactHandler artifactHandler = artifact.getArtifactHandler();
        if ( artifactHandler != null && StringUtils.isNotBlank( artifactHandler.getExtension() ) )
        {
            filename.append( "." ).append( artifactHandler.getExtension() );
        }
        return filename.toString();
    }

    public static String mojoExecutionKey( MojoExecution mojo )
    {
        return StringUtils.join( Arrays.asList(
                StringUtils.defaultIfEmpty( mojo.getExecutionId(), "emptyExecId" ),
                StringUtils.defaultIfEmpty( mojo.getGoal(), "emptyGoal" ),
                StringUtils.defaultIfEmpty( mojo.getLifecyclePhase(), "emptyLifecyclePhase" ),
                StringUtils.defaultIfEmpty( mojo.getArtifactId(), "emptyArtifactId" ),
                StringUtils.defaultIfEmpty( mojo.getGroupId(), "emptyGroupId" ),
                StringUtils.defaultIfEmpty( mojo.getVersion(), "emptyVersion" ) ), ":" );
    }

    public static Path getMultimoduleRoot( MavenSession session )
    {
        return session.getRequest().getMultiModuleProjectDirectory().toPath();
    }

    public static Scm readGitInfo( MavenSession session ) throws IOException
    {
        final Scm scmCandidate = new Scm();
        final Path gitDir = getMultimoduleRoot( session ).resolve( ".git" );
        if ( Files.isDirectory( gitDir ) )
        {
            final Path headFile = gitDir.resolve( "HEAD" );
            if ( Files.exists( headFile ) )
            {
                String headRef = readFirstLine( headFile, "<missing branch>" );
                if ( headRef.startsWith( "ref: " ) )
                {
                    String branch = trim( removeStart( headRef, "ref: " ) );
                    scmCandidate.setSourceBranch( branch );
                    final Path refPath = gitDir.resolve( branch );
                    if ( Files.exists( refPath ) )
                    {
                        String revision = readFirstLine( refPath, "<missing revision>" );
                        scmCandidate.setRevision( trim( revision ) );
                    }
                }
                else
                {
                    scmCandidate.setSourceBranch( headRef );
                    scmCandidate.setRevision( headRef );
                }
            }
        }
        return scmCandidate;
    }


    private static String readFirstLine( Path path, String defaultValue ) throws IOException
    {
        return Files.lines( path, StandardCharsets.UTF_8 ).findFirst().orElse( defaultValue );
    }


    public static <T> T getLast( List<T> list )
    {
        int size = list.size();
        if ( size > 0 )
        {
            return list.get( size - 1 );
        }
        throw new NoSuchElementException();
    }

    public static <T> T getOrCreate( MavenSession session, Object key, Supplier<T> supplier )
    {
        SessionData data = session.getRepositorySession().getData();
        while ( true )
        {
            T t = (T) data.get( key );
            if ( t == null )
            {
                t = supplier.get();
                if ( data.set( key, null, t ) )
                {
                    continue;
                }
            }
            return t;
        }
    }

    public static void zip( Path dir, Path zip ) throws IOException
    {
        try ( ZipOutputStream zipOutputStream = new ZipOutputStream( Files.newOutputStream( zip ) ) )
        {
            Files.walkFileTree( dir, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path path, BasicFileAttributes basicFileAttributes )
                        throws IOException
                {
                    final ZipEntry zipEntry = new ZipEntry( dir.relativize( path ).toString() );
                    zipOutputStream.putNextEntry( zipEntry );
                    Files.copy( path, zipOutputStream );
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
    }

    public static void unzip( Path zip, Path out ) throws IOException
    {
        try ( ZipInputStream zis = new ZipInputStream( Files.newInputStream( zip ) ) )
        {
            ZipEntry entry = zis.getNextEntry();
            while ( entry != null )
            {
                Path file = out.resolve( entry.getName() );
                if ( entry.isDirectory() )
                {
                    Files.createDirectory( file );
                }
                else
                {
                    Path parent = file.getParent();
                    Files.createDirectories( parent );
                    Files.copy( zis, file );
                }
                Files.setLastModifiedTime( file, FileTime.fromMillis( entry.getTime() ) );
                entry = zis.getNextEntry();
            }
        }
    }

}
