package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;

/**
 *
 * @author jdcasey
 */
@org.apache.maven.plugins.annotations.Mojo(name = "test")
public class TestPlugin
        implements Mojo
{

    private Log log;

    @Component
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
