package tests;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal validate
 * @phase validate
 */
public class TestMojo
    implements Mojo
{

    private Log log;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Verified plugin execution." );
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
