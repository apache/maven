package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;

/**
 *
 * @author jdcasey
 */
@Execute(goal = "my:something:else:that:is:wrong")
@org.apache.maven.plugins.annotations.Mojo(name = "test")
public class TestPlugin
        implements Mojo
{

    private Log log;

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
