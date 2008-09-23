package tests;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal test
 * @execute phase="compile"
 */
public class TestMojo
    implements Mojo
{

    private Log log;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        System.out.println( "\n\n\nHI!\n\n" );
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
