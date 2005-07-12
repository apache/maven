package org.apache.maven.plugin.plugin;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.Iterator;
import java.util.List;

/**
 * @phase validate
 * @goal validatePom
 */
public class ValidatePluginPomMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        ArtifactRepository distributionRepository = project.getDistributionManagementArtifactRepository();

        if ( distributionRepository == null )
        {
            throw new MojoExecutionException(
                                              "You must provide a distributionManagement section with a repository element in your POM." );
        }

        String distributionRepositoryId = distributionRepository.getId();

        List remoteArtifactRepositories = project.getRemoteArtifactRepositories();

        ArtifactRepository remote = null;

        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            ArtifactRepository remoteRepository = (ArtifactRepository) it.next();

            if ( distributionRepositoryId.equals( remoteRepository.getId() ) )
            {
                remote = remoteRepository;
                break;
            }
        }

        if ( remote == null )
        {
            StringBuffer message = new StringBuffer();
            
            message.append( "You must provide a artifact repository definition in <repositories/> that matches " +
                    "the id of the repository specified in <distributionManagement/>: \'"
                                                  + distributionRepositoryId + "\'." );
            
            throw new MojoExecutionException( message.toString() );
        }
    }

}
