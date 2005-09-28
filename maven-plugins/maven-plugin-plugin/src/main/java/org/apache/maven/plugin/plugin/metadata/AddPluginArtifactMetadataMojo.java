package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Inject any plugin-specific artifact metadata to the project's artifact, for subsequent installation
 * and deployment. The first use-case for this is to add the LATEST metadata (which is plugin-specific)
 * for shipping alongside the plugin's artifact.
 *
 * @phase package
 * @goal addPluginArtifactMetadata
 */
public class AddPluginArtifactMetadataMojo
    extends AbstractMojo
{

    /**
     * The project artifact, which should have the LATEST metadata added to it.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private String goalPrefix;

    public void execute()
        throws MojoExecutionException
    {
        Artifact projectArtifact = project.getArtifact();

        Versioning versioning = new Versioning();
        versioning.setLatest( projectArtifact.getVersion() );
        versioning.updateTimestamp();
        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( projectArtifact, versioning );
        projectArtifact.addMetadata( metadata );

        GroupRepositoryMetadata groupMetadata = new GroupRepositoryMetadata( project.getGroupId() );
        groupMetadata.addPluginMapping( getGoalPrefix(), project.getArtifactId(), project.getName() );

        projectArtifact.addMetadata( groupMetadata );
    }

    private String getGoalPrefix()
    {
        if ( goalPrefix == null )
        {
            goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        }

        return goalPrefix;
    }
}
