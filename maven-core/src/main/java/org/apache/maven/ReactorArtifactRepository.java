package org.apache.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.LocalArtifactRepository;

/**
 * An implementation of a repository that knows how to search the Maven reactor for artifacts.
 * 
 * @author Jason van Zyl
 */

//TODO: need phase information here to determine whether to hand back the classes/ or archive.
public class ReactorArtifactRepository
    extends LocalArtifactRepository
{
    private Map<String, MavenProject> reactorProjects;

    private Map<String, List<String>> availableVersions;

    private MavenExecutionResult executionResult;

    private final int hashCode;

    public ReactorArtifactRepository( Map<String, MavenProject> reactorProjects, MavenSession session )
    {
        this.reactorProjects = reactorProjects;
        this.executionResult = ( session != null ) ? session.getResult() : null;
        hashCode = ( reactorProjects != null ) ? reactorProjects.keySet().hashCode() : 0;

        availableVersions = new HashMap<String, List<String>>( reactorProjects.size() * 2 );
        for ( MavenProject project : reactorProjects.values() )
        {
            String key = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            List<String> versions = availableVersions.get( key );

            if ( versions == null )
            {
                versions = new ArrayList<String>( 1 );
                availableVersions.put( key, versions );
            }

            versions.add( project.getVersion() );
        }
    }

    @Override
    public Artifact find( Artifact artifact )
    {
        String projectKey = ArtifactUtils.key( artifact );
        
        MavenProject project = reactorProjects.get( projectKey );

        if ( project != null )
        {
            if ( "pom".equals( artifact.getType() ) )
            {
                resolve( artifact, project.getFile() );
            }
            else
            {
                //TODO Need to look for plugins

                Artifact projectArtifact = findMatchingArtifact( project, artifact );

                if ( projectArtifact != null && projectArtifact.getFile() != null && projectArtifact.getFile().exists() )
                {
                    //TODO: This is really completely wrong and should probably be based on the phase that is currently being executed.
                    // If we are running before the packaging phase there is going to be no archive anyway, but if we are running prior to package
                    // we shouldn't even take the archive anyway.

                    resolve( artifact, projectArtifact.getFile() );
                }
                else if ( isProjectOutputValid( project ) )
                {
                    File classesDir;

                    if ( isTestArtifact( artifact ) )
                    {
                        classesDir = new File( project.getBuild().getTestOutputDirectory() );
                    }
                    else
                    {
                        classesDir = new File( project.getBuild().getOutputDirectory() );
                    }

                    if ( classesDir.isDirectory() )
                    {
                        resolve( artifact, classesDir );
                    }
                }
            }
        }

        return artifact;
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

        List<String> versions = availableVersions.get( key );

        return ( versions != null ) ? Collections.unmodifiableList( versions ) : Collections.<String> emptyList();
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
     * Determines whether the output directories of the specified project have valid contents and can be used for
     * artifact resolution.
     * 
     * @param project The project to check, must not be {@code null}.
     * @return {@code true} if the output directories are valid, {@code false} otherwise.
     */
    private boolean isProjectOutputValid( MavenProject project )
    {
        return executionResult != null && executionResult.getBuildSummary( project ) instanceof BuildSuccess;
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

        return eq( reactorProjects.keySet(), other.reactorProjects.keySet() );
    }

    @Override
    public boolean isProjectAware()
    {
        return true;
    }

}
