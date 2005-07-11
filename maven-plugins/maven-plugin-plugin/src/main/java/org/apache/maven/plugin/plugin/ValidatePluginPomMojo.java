package org.apache.maven.plugin.plugin;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

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
                throw new MojoExecutionException( "You must provide a distributionManagement section with a repository element in your POM." );
            }
    }

}
