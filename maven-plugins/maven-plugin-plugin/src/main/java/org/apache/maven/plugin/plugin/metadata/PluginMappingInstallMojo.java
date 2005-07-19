package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.repository.metadata.InvalidRepositoryMetadataException;
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
            try
            {
                getRepositoryMetadataManager().resolveLocally( metadata, getLocalRepository() );
            }
            catch ( InvalidRepositoryMetadataException e )
            {
                getRepositoryMetadataManager().purgeLocalCopy( metadata, getLocalRepository() );
            }
            
            boolean shouldUpdate = updatePluginMap( metadata );

            if ( shouldUpdate )
            {
                getRepositoryMetadataManager().install( metadata, getLocalRepository() );
            }
        }
        catch ( RepositoryMetadataManagementException e )
        {
            throw new MojoExecutionException( "Failed to install " + metadata, e );
        }
    }

}
