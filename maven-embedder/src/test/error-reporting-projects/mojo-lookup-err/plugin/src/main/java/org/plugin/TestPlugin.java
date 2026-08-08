package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author jdcasey
 */
@org.apache.maven.plugins.annotations.Mojo(name = "test", requiresProject = false)
public class TestPlugin
        implements Mojo
{

    private Log log;

    @Component
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
