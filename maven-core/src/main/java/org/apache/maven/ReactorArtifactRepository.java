package org.apache.maven;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.project.MavenProject;

/**
 * An implementation of a repository that knows how to search the Maven reactor for artifacts.
 * 
 * @author Jason van Zyl
 */

// maven-compat
//   target/classes
// maven-core
//   target/classes
// maven-embedder
//   target/classes
// maven-model
//   target/classes
// maven-model-builder
//   target/classes
// maven-plugin-api
//   target/classes
// maven-repository
//   target/classes
// maven-toolchain
//   target/classes

public class ReactorArtifactRepository
    extends LocalArtifactRepository
{
    private Map<String,MavenProject> reactorProjects;
    
    public ReactorArtifactRepository( Map<String,MavenProject> reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }
        
    @Override
    public Artifact find( Artifact artifact )
    {
        String projectKey = ArtifactUtils.key( artifact );
                
        MavenProject project = reactorProjects.get( projectKey );
                        
        if ( project != null )
        {
            //TODO: determine if we want to pass the artifact produced by the project if it
            // is present and under what conditions we will do such a thing.            
            
            File classesDirectory = new File( project.getBuild().getOutputDirectory() );
            
            if( classesDirectory.exists() )
            {
                artifact.setFile( classesDirectory );
             
                artifact.setFromAuthoritativeRepository( true );
                
                artifact.setResolved( true );                
            }            
        }
        
        return artifact;
    }

    @Override
    public boolean isAuthoritative()
    {
        return true;
    }

    @Override
    public boolean hasLocalMetadata()
    {
        return false;
    }
}
