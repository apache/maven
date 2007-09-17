package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.context.BuildContextManager;
import org.apache.maven.lifecycle.LifecycleExecutionContext;
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

    private BuildContextManager buildContextManager;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Ending forked execution [fork id: " + forkId + "]" );

        LifecycleExecutionContext ctx = LifecycleExecutionContext.read( buildContextManager );
        MavenProject executionProject = ctx.removeForkedProject();

        MavenProject project = ctx.getCurrentProject();
        if ( ( project != null ) && ( executionProject != null ) )
        {
            project.setExecutionProject( executionProject );
        }

        ctx.store( buildContextManager );
    }

}
