package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal install-mapping
 * @phase install
 */
public class PluginMappingInstallMojo
    extends AbstractPluginMappingPublisherMojo
{

    public void execute()
        throws MojoExecutionException
    {
        publish( false );
    }

}
