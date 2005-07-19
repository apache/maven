package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.mapping.metadata.PluginMappingMetadata;

/**
 * @goal installMapping
 * @phase install
 */
public class PluginMappingInstallMojo
    extends AbstractPluginMappingMojo
{

    public void execute()
        throws MojoExecutionException
    {
        RepositoryMetadata metadata = new PluginMappingMetadata( getProject().getGroupId() );
        
        try
        {
            getRepositoryMetadataManager().resolveLocally( metadata, getLocalRepository() );
            
            updatePluginMap( metadata );

            getRepositoryMetadataManager().install( metadata, getLocalRepository() );
        }
        catch ( RepositoryMetadataManagementException e )
        {
            throw new MojoExecutionException( "Failed to install " + metadata, e );
        }
    }

}
