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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.caching.xml.build.Scm;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.maven.artifact.Artifact.LATEST_VERSION;
import static org.apache.maven.artifact.Artifact.SNAPSHOT_VERSION;

/**
 * ProjectUtils
 */
public class ProjectUtils
{

    private static final List<String> PHASES = Lists.newArrayList(
            //clean
            "pre-clean", "clean", "post-clean",
            // default
            "validate", "initialize", "generate-sources", "process-sources", "generate-resources", "process-resources",
            "compile", "process-classes", "generate-test-sources", "process-test-sources", "generate-test-resources",
            "process-test-resources", "test-compile", "process-test-classes", "test", "prepare-package", "package",
            "pre-integration-test", "integration-test", "post-integration-test", "verify", "install", "deploy",
            //site
            "pre-site", "site", "post-site", "site-deploy" );

    /**
     * @param phase
     * @param other
     * @return true if the given phase is later than the other in maven lifecycle. Example: isLaterPhase("install",
     * "clean") returns true;
     */
    public static boolean isLaterPhase( String phase, String other )
    {
        checkArgument( PHASES.contains( phase ), "Unsupported phase: " + phase );
        checkArgument( PHASES.contains( other ), "Unsupported phase: " + other );

        return PHASES.indexOf( phase ) > PHASES.indexOf( other );
    }

    public static boolean isBuilding( Dependency dependency, ProjectIndex projectIndex )
    {
        final MavenProject key = new MavenProject();
        key.setGroupId( dependency.getGroupId() );
        key.setArtifactId( dependency.getArtifactId() );
        key.setVersion( dependency.getVersion() );
        return projectIndex.getProjects().containsKey( BuilderCommon.getKey( key ) );
    }

    public static boolean isPomPackaging( MavenProject project )
    {
        return project.getPackaging().equals( "pom" ) && !new File( getSrcDir( project ) ).exists();
    }

    public static boolean isPom( Artifact artifact )
    {
        return artifact.getType().equals( "pom" );
    }

    public static boolean isPom( Dependency dependency )
    {
        return dependency.getType().equals( "pom" );
    }

    public static boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT_VERSION ) || version.endsWith( LATEST_VERSION );
    }

    public static String getTargetDir( MavenProject project )
    {
        return FilenameUtils.concat( project.getBasedir().getAbsolutePath(), "target" );
    }

    public static String getSrcDir( MavenProject project )
    {
        return FilenameUtils.concat( project.getBasedir().getAbsolutePath(), "src" );
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
        return StringUtils.join( Lists.newArrayList( StringUtils.defaultIfEmpty( mojo.getExecutionId(), "emptyExecId" ),
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
        final List<String> lines = Files.readAllLines( path, StandardCharsets.UTF_8 );
        return Iterables.getFirst( lines, defaultValue );
    }


}
