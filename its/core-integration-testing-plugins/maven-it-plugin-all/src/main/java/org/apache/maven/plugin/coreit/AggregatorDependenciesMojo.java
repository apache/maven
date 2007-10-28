package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @requiresDependencyResolution test
 * @aggregator
 * @goal aggregator-dependencies
 * @phase validate
 */
public class AggregatorDependenciesMojo
    extends AbstractMojo
{
    

    public void execute()
        throws MojoExecutionException
    {
        //nothing to do, we are checking Maven's behavior here.
    }
}
