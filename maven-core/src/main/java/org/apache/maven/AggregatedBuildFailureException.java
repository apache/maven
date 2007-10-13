package org.apache.maven;

import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Exception which occurs when an @aggregator plugin fails to execute. This
 * exception is meant to wrap a {@link MojoFailureException}, and provide
 * additional details about the mojo that failed, via {@link MojoBinding} and
 * the root directory in which the build executes.
 *
 * @author jdcasey
 *
 */
public class AggregatedBuildFailureException
    extends BuildFailureException
{

    private final String executionRootDirectory;
    private final MojoBinding binding;

    public AggregatedBuildFailureException( String executionRootDirectory,
                                            MojoBinding binding,
                                            MojoFailureException cause )
    {
        super( "Build in root directory: " + executionRootDirectory
               + " failed during execution of aggregator mojo: "
               + MojoBindingUtils.toString( binding ), cause );

        this.executionRootDirectory = executionRootDirectory;
        this.binding = binding;
    }

    public MojoFailureException getMojoFailureException()
    {
        return (MojoFailureException) getCause();
    }

    public String getExecutionRootDirectory()
    {
        return executionRootDirectory;
    }

    public MojoBinding getBinding()
    {
        return binding;
    }

}
