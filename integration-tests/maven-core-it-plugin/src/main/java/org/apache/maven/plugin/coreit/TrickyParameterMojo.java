package org.apache.maven.plugin.coreit;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Check that tricky things having to do with parameter injection are handled correctly, 
 * by using the tricky params here!
 * 
 * @goal tricky-params
 */
public class TrickyParameterMojo
    extends AbstractMojo
{
    
    /**
     * @parameter expression="${project.distributionManagementArtifactRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository deploymentRepo;
    
    /**
     * @parameter expression="${requiredParam}"
     * @required
     */
    private String requiredParam;

    public void execute()
        throws MojoExecutionException
    {
        
        getLog().info( "Id of repository: " + deploymentRepo.getId() );
        getLog().info( "requiredParam: " + requiredParam );
        
    }

}
