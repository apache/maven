package org.apache.maven;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
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

    public ReactorArtifactRepository( Map<String, MavenProject> reactorProjects )
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
            if ( artifact.getType().equals( "pom" ) )
            {
                artifact.setFile( project.getFile() );

                artifact.setFromAuthoritativeRepository( true );

                artifact.setResolved( true );
            }
            else
            {
                //TODO Need to look for attached artifacts
                //TODO Need to look for plugins
                
                File artifactFile = project.getArtifact().getFile();

                if ( artifactFile != null && artifactFile.exists() )
                {
                    //TODO: This is really completely wrong and should probably be based on the phase that is currently being executed.
                    // If we are running before the packaging phase there is going to be no archive anyway, but if we are running prior to package
                    // we shouldn't even take the archive anyway.

                    artifact.setFile( artifactFile );

                    artifact.setFromAuthoritativeRepository( true );

                    artifact.setResolved( true );
                }
                /*
                
                TODO: This is being left out because Maven 2.x does not set this internally and it is only done by the compiler
                plugin and not done generally. This should be done generally but currently causes problems with MNG-3023
                
                else if ( new File( project.getBuild().getOutputDirectory() ).exists() )
                {
                    artifact.setFile( new File( project.getBuild().getOutputDirectory() ) );

                    artifact.setFromAuthoritativeRepository( true );

                    artifact.setResolved( true );
                }
                */
            }
        }

        return artifact;
    }

    @Override
    public String getId()
    {
        return "reactor";
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
