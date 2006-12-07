package org.apache.maven.execution;

import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @author Jason van Zyl
 */
public class DefaultMavenExecutionResult
    implements MavenExecutionResult
{
    private MavenProject mavenProject;

    private List exceptions;

    public DefaultMavenExecutionResult( MavenProject project,
                                        List exceptions )
    {
        this.mavenProject = project;
        this.exceptions = exceptions;
    }

    public MavenProject getMavenProject()
    {
        return mavenProject;
    }

    public List getExceptions()
    {
        return exceptions;
    }
}
