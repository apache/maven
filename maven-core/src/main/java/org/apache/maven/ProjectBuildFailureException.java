package org.apache.maven;

import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
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
    private final MojoBinding binding;

    public ProjectBuildFailureException( String projectId,
                                         MojoBinding binding,
                                         MojoFailureException cause )
    {
        super( "Build for project: " + projectId + " failed during execution of mojo: "
               + MojoBindingUtils.toString( binding ), cause );

        this.projectId = projectId;
        this.binding = binding;
    }

    public MojoFailureException getMojoFailureException()
    {
        return (MojoFailureException) getCause();
    }

    public String getProjectId()
    {
        return projectId;
    }

    public MojoBinding getBinding()
    {
        return binding;
    }

}
