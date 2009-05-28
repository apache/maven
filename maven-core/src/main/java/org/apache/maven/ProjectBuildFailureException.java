package org.apache.maven;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Exception which occurs when a normal (i.e. non-aggregator) mojo fails to
 * execute. In this case, the mojo failed while executing against a particular
 * project instance, so we can wrap the {@link MojoFailureException} with context
 * information including projectId and the {@link MojoBinding} that caused the
 * failure.
 *
 * @author jdcasey
 *
 */
public class ProjectBuildFailureException
    extends BuildFailureException
{

    private final String projectId;

    public ProjectBuildFailureException( String projectId, MojoFailureException cause )
    {
        super( "Build for project: " + projectId + " failed during execution of mojo.", cause );

        this.projectId = projectId;
    }

    public MojoFailureException getMojoFailureException()
    {
        return (MojoFailureException) getCause();
    }

    public String getProjectId()
    {
        return projectId;
    }
}
