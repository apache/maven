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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a workspace reader that knows how to search the Maven reactor for artifacts, either as packaged
 * jar if it has been built, or only compile output directory if packaging hasn't happened yet.
 *
 * @author Jason van Zyl
 */
@Named( ReactorReader.HINT )
@SessionScoped
class ReactorReader
    implements MavenWorkspaceReader
{
    public static final String HINT = "reactor";

    private static final Collection<String> COMPILE_PHASE_TYPES =
        Arrays.asList( "jar", "ejb-client", "war", "rar", "ejb3", "par", "sar", "wsr", "har", "app-client" );

    private static final Logger LOGGER = LoggerFactory.getLogger( ReactorReader.class );

    private MavenSession session;

    private Map<String, MavenProject> projectsByGAV;

    private Map<String, List<MavenProject>> projectsByGA;

    private WorkspaceRepository repository;

    @Inject
    ReactorReader( MavenSession session )
    {
        this.session = session;
        this.projectsByGAV = new HashMap<>( session.getAllProjects().size() * 2 );
        session.getAllProjects().forEach( project ->
        {
            String projectId = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );
            this.projectsByGAV.put( projectId, project );
        } );

        projectsByGA = new HashMap<>( projectsByGAV.size() * 2 );
        for ( MavenProject project : projectsByGAV.values() )
        {
            String key = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            List<MavenProject> projects = projectsByGA.computeIfAbsent( key, k -> new ArrayList<>( 1 ) );

            projects.add( project );
        }

        repository = new WorkspaceRepository( "reactor", new HashSet<>( projectsByGAV.keySet() ) );
    }

    //
    // Public API
    //

    public WorkspaceRepository getRepository()
    {
        return repository;
    }

    public File findArtifact( Artifact artifact )
    {
        String projectKey = ArtifactUtils.key( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );

        MavenProject project = projectsByGAV.get( projectKey );

        if ( project != null )
        {
            File file = find( project, artifact );
            if ( file == null && project != project.getExecutionProject() )
            {
                file = find( project.getExecutionProject(), artifact );
            }
            return file;
        }

        return null;
    }

    public List<String> findVersions( Artifact artifact )
    {
        String key = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        List<MavenProject> projects = projectsByGA.get( key );
        if ( projects == null || projects.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<String> versions = new ArrayList<>();

        for ( MavenProject project : projects )
        {
            if ( find( project, artifact ) != null )
            {
                versions.add( project.getVersion() );
            }
        }

        return Collections.unmodifiableList( versions );
    }

    @Override
    public Model findModel( Artifact artifact )
    {
        String projectKey = ArtifactUtils.key( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        MavenProject project = projectsByGAV.get( projectKey );
        return project == null ? null : project.getModel();
    }

    //
    // Implementation
    //

    private File find( MavenProject project, Artifact artifact )
    {
        if ( "pom".equals( artifact.getExtension() ) )
        {
            return project.getFile();
        }

        Artifact projectArtifact = findMatchingArtifact( project, artifact );
        File packagedArtifactFile = determinePreviouslyPackagedArtifactFile( project, projectArtifact );

        if ( hasArtifactFileFromPackagePhase( projectArtifact ) )
        {
            return projectArtifact.getFile();
        }
        // Check whether an earlier Maven run might have produced an artifact that is still on disk.
        else if ( packagedArtifactFile != null && packagedArtifactFile.exists()
                && isPackagedArtifactUpToDate( project, packagedArtifactFile, artifact ) )
        {
            return packagedArtifactFile;
        }
        else if ( !hasBeenPackagedDuringThisSession( project ) )
        {
            // fallback to loose class files only if artifacts haven't been packaged yet
            // and only for plain old jars. Not war files, not ear files, not anything else.
            return determineBuildOutputDirectoryForArtifact( project, artifact );
        }

        // The fall-through indicates that the artifact cannot be found;
        // for instance if package produced nothing or classifier problems.
        return null;
    }

    private File determineBuildOutputDirectoryForArtifact( final MavenProject project, final Artifact artifact )
    {
        if ( isTestArtifact( artifact ) )
        {
            if ( project.hasLifecyclePhase( "test-compile" ) )
            {
                return new File( project.getBuild().getTestOutputDirectory() );
            }
        }
        else
        {
            String type = artifact.getProperty( "type", "" );
            File outputDirectory = new File( project.getBuild().getOutputDirectory() );

            // Check if the project is being built during this session, and if we can expect any output.
            // There is no need to check if the build has created any outputs, see MNG-2222.
            boolean projectCompiledDuringThisSession
                    = project.hasLifecyclePhase( "compile" ) && COMPILE_PHASE_TYPES.contains( type );

            // Check if the project is part of the session (not filtered by -pl, -rf, etc). If so, we check
            // if a possible earlier Maven invocation produced some output for that project which we can use.
            boolean projectHasOutputFromPreviousSession
                    = !session.getProjects().contains( project ) && outputDirectory.exists();

            if ( projectHasOutputFromPreviousSession || projectCompiledDuringThisSession )
            {
                return outputDirectory;
            }
        }

        // The fall-through indicates that the artifact cannot be found;
        // for instance if package produced nothing or classifier problems.
        return null;
    }

    private File determinePreviouslyPackagedArtifactFile( MavenProject project, Artifact artifact )
    {
        if ( artifact == null )
        {
            return null;
        }

        String fileName = String.format( "%s.%s", project.getBuild().getFinalName(), artifact.getExtension() );
        return new File( project.getBuild().getDirectory(), fileName );
    }

    private boolean hasArtifactFileFromPackagePhase( Artifact projectArtifact )
    {
        return projectArtifact != null && projectArtifact.getFile() != null && projectArtifact.getFile().exists();
    }

    private boolean isPackagedArtifactUpToDate( MavenProject project, File packagedArtifactFile, Artifact artifact )
    {
        Path outputDirectory = Paths.get( project.getBuild().getOutputDirectory() );
        if ( !outputDirectory.toFile().exists() )
        {
            return true;
        }

        try ( Stream<Path> outputFiles = Files.walk( outputDirectory ) )
        {
            // Not using File#lastModified() to avoid a Linux JDK8 milliseconds precision bug: JDK-8177809.
            long artifactLastModified = Files.getLastModifiedTime( packagedArtifactFile.toPath() ).toMillis();

            if ( session.getProjectBuildingRequest().getBuildStartTime() != null )
            {
                long buildStartTime = session.getProjectBuildingRequest().getBuildStartTime().getTime();
                if ( artifactLastModified > buildStartTime )
                {
                    return true;
                }
            }

            Iterator<Path> iterator = outputFiles.iterator();
            while ( iterator.hasNext() )
            {
                Path outputFile = iterator.next();

                if ( Files.isDirectory(  outputFile ) )
                {
                    continue;
                }

                long outputFileLastModified = Files.getLastModifiedTime( outputFile ).toMillis();
                if ( outputFileLastModified > artifactLastModified )
                {
                    File alternative = determineBuildOutputDirectoryForArtifact( project, artifact );
                    if ( alternative != null )
                    {
                        LOGGER.warn( "File '{}' is more recent than the packaged artifact for '{}'; using '{}' instead",
                                relativizeOutputFile( outputFile ), project.getArtifactId(),
                                relativizeOutputFile( alternative.toPath() ) );
                    }
                    else
                    {
                        LOGGER.warn( "File '{}' is more recent than the packaged artifact for '{}'; "
                                + "cannot use the build output directory for this type of artifact",
                                relativizeOutputFile( outputFile ), project.getArtifactId() );
                    }
                    return false;
                }
            }

            return true;
        }
        catch ( IOException e )
        {
            LOGGER.warn( "An I/O error occurred while checking if the packaged artifact is up-to-date "
                    + "against the build output directory. "
                    + "Continuing with the assumption that it is up-to-date.", e );
            return true;
        }
    }

    private boolean hasBeenPackagedDuringThisSession( MavenProject project )
    {
        return project.hasLifecyclePhase( "package" ) || project.hasLifecyclePhase( "install" )
            || project.hasLifecyclePhase( "deploy" );
    }

    private Path relativizeOutputFile( final Path outputFile )
    {
        Path projectBaseDirectory = Paths.get( session.getRequest().getMultiModuleProjectDirectory().toURI() );
        return projectBaseDirectory.relativize( outputFile );
    }

    /**
     * Tries to resolve the specified artifact from the artifacts of the given project.
     *
     * @param project The project to try to resolve the artifact from, must not be <code>null</code>.
     * @param requestedArtifact The artifact to resolve, must not be <code>null</code>.
     * @return The matching artifact from the project or <code>null</code> if not found. Note that this
     */
    private Artifact findMatchingArtifact( MavenProject project, Artifact requestedArtifact )
    {
        String requestedRepositoryConflictId = ArtifactIdUtils.toVersionlessId( requestedArtifact );

        Artifact mainArtifact = RepositoryUtils.toArtifact( project.getArtifact() );
        if ( requestedRepositoryConflictId.equals( ArtifactIdUtils.toVersionlessId( mainArtifact ) ) )
        {
            return mainArtifact;
        }

        for ( Artifact attachedArtifact : RepositoryUtils.toArtifacts( project.getAttachedArtifacts() ) )
        {
            if ( attachedArtifactComparison( requestedArtifact, attachedArtifact ) )
            {
                return attachedArtifact;
            }
        }

        return null;
    }

    private boolean attachedArtifactComparison( Artifact requested, Artifact attached )
    {
        //
        // We are taking as much as we can from the DefaultArtifact.equals(). The requested artifact has no file so
        // we want to remove that from the comparison.
        //
        return requested.getArtifactId().equals( attached.getArtifactId() )
            && requested.getGroupId().equals( attached.getGroupId() )
            && requested.getVersion().equals( attached.getVersion() )
            && requested.getExtension().equals( attached.getExtension() )
            && requested.getClassifier().equals( attached.getClassifier() );
    }

    /**
     * Determines whether the specified artifact refers to test classes.
     *
     * @param artifact The artifact to check, must not be {@code null}.
     * @return {@code true} if the artifact refers to test classes, {@code false} otherwise.
     */
    private static boolean isTestArtifact( Artifact artifact )
    {
        return ( "test-jar".equals( artifact.getProperty( "type", "" ) ) )
            || ( "jar".equals( artifact.getExtension() ) && "tests".equals( artifact.getClassifier() ) );
    }
}
