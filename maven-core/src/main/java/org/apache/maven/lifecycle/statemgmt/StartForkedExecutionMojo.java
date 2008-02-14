package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Setup a new project instance for the forked executions to use.
 *
 * @author jdcasey
 *
 */
public class StartForkedExecutionMojo
    extends AbstractMojo
{

    private MavenProject project;

    private MavenSession session;

    private int forkId = -1;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Starting forked execution [fork id: " + forkId + "]" );

        if ( project != null )
        {
            session.addForkedProject( (MavenProject) project.clone() );
        }
    }

}
