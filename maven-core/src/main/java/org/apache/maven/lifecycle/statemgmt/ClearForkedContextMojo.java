package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Remove the execution-project used during the fork, now that the forking mojo is finished executing.
 *
 * @author jdcasey
 *
 */
public class ClearForkedContextMojo
    extends AbstractMojo
{

    private MavenProject project;

    private int forkId = -1;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Cleaning up forked execution context [fork id: " + forkId + "]" );

        if ( project != null )
        {
            project.clearExecutionProject();
        }
    }

}
