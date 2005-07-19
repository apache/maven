package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.LatestArtifactMetadata;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/** Inject any plugin-specific artifact metadata to the project's artifact, for subsequent installation
 *  and deployment. The first use-case for this is to add the LATEST metadata (which is plugin-specific)
 *  for shipping alongside the plugin's artifact.
 *  
 * @phase package
 * @goal addPluginArtifactMetadata
 */
public class AddPluginArtifactMetadataMojo
    extends AbstractMojo
{
    
    /** The project artifact, which should have the LATEST metadata added to it.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        Artifact projectArtifact = project.getArtifact();
        
        LatestArtifactMetadata metadata = new LatestArtifactMetadata( projectArtifact );
        
        metadata.setVersion( projectArtifact.getVersion() );
        
        projectArtifact.addMetadata( metadata );
    }

}
