package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal test
 * @aggregator
 *
 * @author jdcasey
 */
public class TestPlugin
    implements Mojo
{

    private Log log;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        throw new MojoFailureException( this, "This mojo will always fail.", "This mojo is programmed to fail at all times, to express certain error-reporting functions." );
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

}
