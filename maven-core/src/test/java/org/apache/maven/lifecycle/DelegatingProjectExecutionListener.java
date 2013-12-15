package org.apache.maven.lifecycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

@Named
@Singleton
public class DelegatingProjectExecutionListener
    implements ProjectExecutionListener
{
    private final List<ProjectExecutionListener> listeners = new CopyOnWriteArrayList<ProjectExecutionListener>();

    public void beforeProjectExecution( MavenSession session, MavenProject project )
        throws LifecycleExecutionException
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.beforeProjectExecution( session, project );
        }
    }

    public void beforeProjectLifecycleExecution( MavenSession session, MavenProject project,
                                                 List<MojoExecution> executionPlan )
        throws LifecycleExecutionException
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.beforeProjectLifecycleExecution( session, project, executionPlan );
        }
    }

    public void afterProjectExecutionSuccess( MavenSession session, MavenProject project )
        throws LifecycleExecutionException
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.afterProjectExecutionSuccess( session, project );
        }
    }

    public void afterProjectExecutionFailure( MavenSession session, MavenProject project, Throwable cause )
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.afterProjectExecutionFailure( session, project, cause );
        }
    }

    public void addProjectExecutionListener( ProjectExecutionListener listener )
    {
        this.listeners.add( listener );
    }

    public void removeProjectExecutionListener( ProjectExecutionListener listener )
    {
        this.listeners.remove( listener );
    }
}
