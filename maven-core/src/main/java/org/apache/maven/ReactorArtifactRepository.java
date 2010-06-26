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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.LocalArtifactRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a repository that knows how to search the Maven reactor for artifacts.
 *
 * @author Jason van Zyl
 */
public class ReactorArtifactRepository
    extends LocalArtifactRepository
{

    private Map<String, MavenProject> projectsByGAV;

    private Map<String, List<MavenProject>> projectsByGA;

    private final int hashCode;

    @SuppressWarnings( { "ConstantConditions" } )
    public ReactorArtifactRepository( Map<String, MavenProject> reactorProjects )
    {
        projectsByGAV = reactorProjects;
        hashCode = ( reactorProjects != null ) ? reactorProjects.keySet().hashCode() : 0;

        projectsByGA = new HashMap<String, List<MavenProject>>( reactorProjects.size() * 2 );
        for ( MavenProject project : reactorProjects.values() )
        {
            String key = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            List<MavenProject> projects = projectsByGA.get( key );

            if ( projects == null )
            {
                projects = new ArrayList<MavenProject>( 1 );
                projectsByGA.put( key, projects );
            }

            projects.add( project );
        }
    }

    @Override
    public Artifact find( Artifact artifact )
    {
        String projectKey = ArtifactUtils.key( artifact );

        MavenProject project = projectsByGAV.get( projectKey );

        if ( project != null )
        {
            File file = find( project, artifact );
            if ( file != null )
            {
                resolve( artifact, file );
            }
        }

        return artifact;
    }

    private File find( MavenProject project, Artifact artifact )
    {
        if ( "pom".equals( artifact.getType() ) )
        {
            return project.getFile();
        }

        Artifact projectArtifact = findMatchingArtifact( project, artifact );

        if ( hasArtifactFileFromPackagePhase( projectArtifact ) )
        {
            return projectArtifact.getFile();
        }
        else if ( !project.hasCompletedPhase( "package" ) )
        {
            if ( isTestArtifact( artifact ) )
            {
                if ( project.hasCompletedPhase( "test-compile" ) )
                {
                    return new File( project.getBuild().getTestOutputDirectory() );
                }
            }
            else
            {
                if ( project.hasCompletedPhase( "compile" ) )
                {
                    return new File( project.getBuild().getOutputDirectory() );
                }
            }
        }

        // The fall-through indicates that the artifact cannot be found;
        // for instance if package produced nothing or classifier problems.
        return null;
    }

    private boolean hasArtifactFileFromPackagePhase( Artifact projectArtifact )
    {
        return projectArtifact != null && projectArtifact.getFile() != null && projectArtifact.getFile().exists();
    }

    private void resolve( Artifact artifact, File file )
    {
        artifact.setFile( file );

        artifact.setResolved( true );

        artifact.setRepository( this );
    }

    @Override
    public List<String> findVersions( Artifact artifact )
    {
        String key = ArtifactUtils.versionlessKey( artifact );

        List<MavenProject> projects = projectsByGA.get( key );
        if ( projects == null || projects.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<String> versions = new ArrayList<String>();

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
    public String getId()
    {
        return "reactor";
    }

    @Override
    public boolean hasLocalMetadata()
    {
        return false;
    }

    /**
     * Tries to resolve the specified artifact from the artifacts of the given project.
     *
     * @param project The project to try to resolve the artifact from, must not be <code>null</code>.
     * @param requestedArtifact The artifact to resolve, must not be <code>null</code>.
     * @return The matching artifact from the project or <code>null</code> if not found.
     */
    private Artifact findMatchingArtifact( MavenProject project, Artifact requestedArtifact )
    {
        String requestedDependencyConflictId = requestedArtifact.getDependencyConflictId();

        // check for match with project's main artifact by dependency conflict id
        Artifact mainArtifact = project.getArtifact();
        if ( requestedDependencyConflictId.equals( mainArtifact.getDependencyConflictId() ) )
        {
            return mainArtifact;
        }

        String requestedRepositoryConflictId = getRepositoryConflictId( requestedArtifact );

        // check for match with project's main artifact by repository conflict id
        if ( requestedRepositoryConflictId.equals( getRepositoryConflictId( mainArtifact ) ) )
        {
            return mainArtifact;
        }

        // check for match with one of the attached artifacts
        Collection<Artifact> attachedArtifacts = project.getAttachedArtifacts();
        if ( attachedArtifacts != null && !attachedArtifacts.isEmpty() )
        {
            // first try matching by dependency conflict id
            for ( Artifact attachedArtifact : attachedArtifacts )
            {
                if ( requestedDependencyConflictId.equals( attachedArtifact.getDependencyConflictId() ) )
                {
                    return attachedArtifact;
                }
            }

            // next try matching by repository conflict id
            for ( Artifact attachedArtifact : attachedArtifacts )
            {
                if ( requestedRepositoryConflictId.equals( getRepositoryConflictId( attachedArtifact ) ) )
                {
                    return attachedArtifact;
                }
            }
        }

        return null;
    }

    /**
     * Gets the repository conflict id of the specified artifact. Unlike the dependency conflict id, the repository
     * conflict id uses the artifact file extension instead of the artifact type. Hence, the repository conflict id more
     * closely reflects the identity of artifacts as perceived by a repository.
     *
     * @param artifact The artifact, must not be <code>null</code>.
     * @return The repository conflict id, never <code>null</code>.
     */
    private String getRepositoryConflictId( Artifact artifact )
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( artifact.getGroupId() );
        buffer.append( ':' ).append( artifact.getArtifactId() );
        if ( artifact.getArtifactHandler() != null )
        {
            buffer.append( ':' ).append( artifact.getArtifactHandler().getExtension() );
        }
        else
        {
            buffer.append( ':' ).append( artifact.getType() );
        }
        if ( artifact.hasClassifier() )
        {
            buffer.append( ':' ).append( artifact.getClassifier() );
        }
        return buffer.toString();
    }

    /**
     * Determines whether the specified artifact refers to test classes.
     *
     * @param artifact The artifact to check, must not be {@code null}.
     * @return {@code true} if the artifact refers to test classes, {@code false} otherwise.
     */
    private static boolean isTestArtifact( Artifact artifact )
    {
        if ( "test-jar".equals( artifact.getType() ) )
        {
            return true;
        }
        else if ( "jar".equals( artifact.getType() ) && "tests".equals( artifact.getClassifier() ) )
        {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        ReactorArtifactRepository other = (ReactorArtifactRepository) obj;

        return eq( projectsByGAV.keySet(), other.projectsByGAV.keySet() );
    }

    @Override
    public boolean isProjectAware()
    {
        return true;
    }

}
