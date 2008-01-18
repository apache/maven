package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal test
 * @requiresProject false
 *
 * @author jdcasey
 */
public class TestPlugin
    implements Mojo
{

    private Log log;
    
    /**
     * @component role-hint="nonexistant"
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        throw new MojoExecutionException( "THIS SHOULD NEVER BE CALLED." );
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
