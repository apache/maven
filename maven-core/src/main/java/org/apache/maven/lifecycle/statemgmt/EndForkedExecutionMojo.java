package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Restore the lifecycle execution context's current-project, and set the project instance from the
 * forked execution to project.getExecutionProject() for the forking mojo to use.
 *
 * @author jdcasey
 *
 */
public class EndForkedExecutionMojo
    extends AbstractMojo
{

    private int forkId = -1;

    private MavenSession session;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Ending forked execution [fork id: " + forkId + "]" );

        MavenProject executionProject = session.removeForkedProject();

        MavenProject project = session.getCurrentProject();
        if ( ( project != null ) && ( executionProject != null ) )
        {
            project.setExecutionProject( executionProject );
        }
    }

}
