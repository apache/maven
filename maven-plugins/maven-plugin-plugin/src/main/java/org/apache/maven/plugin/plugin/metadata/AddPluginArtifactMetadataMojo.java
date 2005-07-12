package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.LatestArtifactMetadata;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

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
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact projectArtifact;

    public void execute()
        throws MojoExecutionException
    {
        LatestArtifactMetadata metadata = new LatestArtifactMetadata( projectArtifact );
        
        metadata.setVersion( projectArtifact.getVersion() );
        
        projectArtifact.addMetadata( metadata );
    }

}
