package org.apache.maven;

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

    public AggregatedBuildFailureException( String executionRootDirectory,
                                            MojoFailureException cause )
    {
        super( "Build in root directory: " + executionRootDirectory + " failed during execution of aggregator mojo.", cause );

        this.executionRootDirectory = executionRootDirectory;
    }

    public MojoFailureException getMojoFailureException()
    {
        return (MojoFailureException) getCause();
    }

    public String getExecutionRootDirectory()
    {
        return executionRootDirectory;
    }
}
