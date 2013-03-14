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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a workspace reader that knows how to search the Maven reactor for artifacts.
 * 
 * @author Jason van Zyl
 */
class ReactorReader
    implements WorkspaceReader
{
    private final static Collection<String> JAR_LIKE_TYPES = Arrays.asList( "jar", "test-jar", "ejb-client" );

    private final static Collection<String> COMPILE_PHASE_TYPES = Arrays.asList( "jar", "ejb-client" );

    private Map<String, MavenProject> projectsByGAV;

    private Map<String, List<MavenProject>> projectsByGA;

    private WorkspaceRepository repository;
    
    public ReactorReader( Map<String, MavenProject> reactorProjects )
    {
        projectsByGAV = reactorProjects;

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

        repository = new WorkspaceRepository( "reactor", new HashSet<String>( projectsByGAV.keySet() ) );
    }

    private File find( MavenProject project, Artifact artifact )
    {
        if ( "pom".equals( artifact.getExtension() ) )
        {
            return project.getFile();
        }

        org.apache.maven.artifact.Artifact projectArtifact = findMatchingArtifact( project, artifact );

        if ( hasArtifactFileFromPackagePhase( projectArtifact ) )
        {
            return projectArtifact.getFile();
        }
        else if ( !hasBeenPackaged( project ) ) 
        {
            // fallback to loose class files only if artifacts haven't been packaged yet
            // and only for plain old jars. Not war files, not ear files, not anything else.

            if ( isTestArtifact( artifact ) )
            {
                if ( project.hasLifecyclePhase( "test-compile" ) )
                {
                    return new File( project.getBuild().getTestOutputDirectory() );
                }
            }
            else
            {
                String type = artifact.getProperty( "type", "");
                if ( project.hasLifecyclePhase( "compile" ) && COMPILE_PHASE_TYPES.contains( type ) )
                {
                    return new File( project.getBuild().getOutputDirectory() );
                }
            }
        }

        // The fall-through indicates that the artifact cannot be found;
        // for instance if package produced nothing or classifier problems.
        return null;
    }

    private boolean hasArtifactFileFromPackagePhase( org.apache.maven.artifact.Artifact projectArtifact )
    {
        return projectArtifact != null && projectArtifact.getFile() != null && projectArtifact.getFile().exists();
    }

    private boolean hasBeenPackaged( MavenProject project )
    {
        return project.hasLifecyclePhase( "package" ) || project.hasLifecyclePhase( "install" )
            || project.hasLifecyclePhase( "deploy" );
    }

    /**
     * Tries to resolve the specified artifact from the artifacts of the given project.
     * 
     * @param project The project to try to resolve the artifact from, must not be <code>null</code>.
     * @param requestedArtifact The artifact to resolve, must not be <code>null</code>.
     * @return The matching artifact from the project or <code>null</code> if not found.
     * 
     * Note that this 
     */
    private org.apache.maven.artifact.Artifact findMatchingArtifact( MavenProject project, Artifact requestedArtifact )
    {
        String requestedRepositoryConflictId = getConflictId( requestedArtifact );

        org.apache.maven.artifact.Artifact mainArtifact = project.getArtifact();
        if ( requestedRepositoryConflictId.equals( getConflictId( mainArtifact ) ) )
        {
            return mainArtifact;
        }

        Collection<org.apache.maven.artifact.Artifact> attachedArtifacts = project.getAttachedArtifacts();
        if ( attachedArtifacts != null && !attachedArtifacts.isEmpty() )
        {
            for ( org.apache.maven.artifact.Artifact attachedArtifact : attachedArtifacts )
            {
                /*
                 * Don't use the conflict ids, use a customized comparison that takes various ideas into account.
                 */
                if ( attachedArtifactComparison ( requestedArtifact, attachedArtifact ) )
                {
                    return attachedArtifact;
                }
            }
        }

        return null;
    }
    
    /**
     * Try to satisfy both MNG-4065 and MNG-5214. Consider jar and test-jar equivalent.
     * @param requestedType
     * @param artifactType
     * @return
     */
    private boolean attachedArtifactComparison ( Artifact requestedArtifact, org.apache.maven.artifact.Artifact attachedArtifact )
    {
        if ( ! requestedArtifact.getGroupId().equals ( attachedArtifact.getGroupId() ) ) 
        { 
            return false;
        }
        if ( ! requestedArtifact.getArtifactId().equals ( attachedArtifact.getArtifactId() ) ) 
        { 
            return false;
        }
        String requestedExtension = requestedArtifact.getExtension();
        String attachedExtension = null;
        if ( attachedArtifact.getArtifactHandler() != null ) 
            {
                attachedExtension = attachedArtifact.getArtifactHandler().getExtension();
            }
        String requestedType = requestedArtifact.getProperty ( "type", "" );
        String attachedType = attachedArtifact.getType();
        boolean typeOk = false;
        
        if ( requestedExtension.equals ( attachedExtension ) )
        {
            // the ideal case.
            typeOk = true;
        }
        else if ( requestedType.equals( attachedType ) )
        {
            typeOk = true;
        }
        else if ( JAR_LIKE_TYPES.contains( requestedType ) && JAR_LIKE_TYPES.contains( attachedType ) )
        {
            typeOk = true;
        }
        
        if ( !typeOk )
        {
            return false;
        }
        return requestedArtifact.getClassifier().equals ( attachedArtifact.getClassifier() );
    }
    
    /**
     * Gets the repository conflict id of the specified artifact. Unlike the dependency conflict id, the repository
     * conflict id uses the artifact file extension instead of the artifact type. Hence, the repository conflict id more
     * closely reflects the identity of artifacts as perceived by a repository.
     * 
     * @param artifact The artifact, must not be <code>null</code>.
     * @return The repository conflict id, never <code>null</code>.
     */
    private String getConflictId( org.apache.maven.artifact.Artifact artifact )
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

    private String getConflictId( Artifact artifact )
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( artifact.getGroupId() );
        buffer.append( ':' ).append( artifact.getArtifactId() );
        buffer.append( ':' ).append( artifact.getExtension() );
        if ( artifact.getClassifier().length() > 0 )
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
        return ( "test-jar".equals( artifact.getProperty( "type", "" ) ) )
            || ( "jar".equals( artifact.getExtension() ) && "tests".equals( artifact.getClassifier() ) );
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

    public WorkspaceRepository getRepository()
    {
        return repository;
    }

}
