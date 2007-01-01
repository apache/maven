package org.apache.maven.execution;

import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.ArrayList;

/** @author Jason van Zyl */
public class DefaultMavenExecutionResult
    implements MavenExecutionResult
{
    private List exceptions;

    private MavenProject mavenProject;

    private ReactorManager reactorManager;

    public DefaultMavenExecutionResult( List exceptions )
    {
        this.exceptions = exceptions;
    }

    public DefaultMavenExecutionResult( ReactorManager reactorManager )
    {
        this.reactorManager = reactorManager;
    }

    public DefaultMavenExecutionResult( List exceptions,
                                        ReactorManager reactorManager )
    {
        this.reactorManager = reactorManager;
        this.exceptions = exceptions;
    }

    public DefaultMavenExecutionResult( MavenProject project,
                                        List exceptions )
    {
        this.mavenProject = project;
        this.exceptions = exceptions;
    }

    public MavenProject getMavenProject()
    {
        if ( reactorManager != null )
        {
            return reactorManager.getTopLevelProject();
        }

        return mavenProject;
    }

    public ReactorManager getReactorManager()
    {
        return reactorManager;
    }

    public List getExceptions()
    {
        return exceptions;
    }

    public void addException( Throwable t )
    {
        if ( exceptions == null )
        {
            exceptions = new ArrayList();
        }

        exceptions.add( t );
    }

    public boolean hasExceptions()
    {
        return (exceptions != null && exceptions.size() > 0 );
    }
}
