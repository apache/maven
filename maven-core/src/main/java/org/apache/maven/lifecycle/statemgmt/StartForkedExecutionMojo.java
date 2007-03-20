package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.context.BuildContextManager;
import org.apache.maven.lifecycle.LifecycleExecutionContext;
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
    
    private int forkId = -1;
    
    private BuildContextManager buildContextManager;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Starting forked execution [fork id: " + forkId + "]" );
        
        LifecycleExecutionContext ctx = LifecycleExecutionContext.read( buildContextManager );
        ctx.addForkedProject( new MavenProject( project ) );
        ctx.store( buildContextManager );
    }

}
