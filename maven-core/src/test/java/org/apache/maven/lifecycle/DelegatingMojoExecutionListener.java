package org.apache.maven.lifecycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

@Named
@Singleton
public class DelegatingMojoExecutionListener
    implements MojoExecutionListener
{
    private final List<MojoExecutionListener> listeners = new CopyOnWriteArrayList<MojoExecutionListener>();

    public void beforeMojoExecution( MavenSession session, MavenProject project, MojoExecution execution, Mojo mojo )
        throws MojoExecutionException
    {
        for ( MojoExecutionListener listener : listeners )
        {
            listener.beforeMojoExecution( session, project, execution, mojo );
        }
    }

    public void afterMojoExecutionSuccess( MavenSession session, MavenProject project, MojoExecution execution,
                                           Mojo mojo )
        throws MojoExecutionException
    {
        for ( MojoExecutionListener listener : listeners )
        {
            listener.afterMojoExecutionSuccess( session, project, execution, mojo );
        }
    }

    public void afterExecutionFailure( MavenSession session, MavenProject project, MojoExecution execution, Mojo mojo,
                                       Throwable cause )
    {
        for ( MojoExecutionListener listener : listeners )
        {
            listener.afterExecutionFailure( session, project, execution, mojo, cause );
        }
    }

    public void addMojoExecutionListener( MojoExecutionListener listener )
    {
        this.listeners.add( listener );
    }

    public void removeMojoExecutionListener( MojoExecutionListener listener )
    {
        this.listeners.remove( listener );
    }

}
