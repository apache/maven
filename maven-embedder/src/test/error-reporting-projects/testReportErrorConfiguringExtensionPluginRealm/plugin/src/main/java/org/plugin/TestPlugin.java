package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal test
 *
 * @author jdcasey
 */
public class TestPlugin
    implements Mojo
{

    private Log log;

    /**
     * @component
     */
    private ComponentOne one;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
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
