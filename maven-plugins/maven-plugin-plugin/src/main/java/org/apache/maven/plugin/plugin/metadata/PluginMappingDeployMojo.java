package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal deploy-mapping
 * @phase deploy
 */
public class PluginMappingDeployMojo
    extends AbstractPluginMappingPublisherMojo
{

    public void execute()
        throws MojoExecutionException
    {
        publish( true );
    }

}
