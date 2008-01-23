package org.apache.maven;

import org.apache.maven.lifecycle.TaskValidationResult;

/**
 * Exception which occurs when a task or goal is specified on the command line
 * but cannot be resolved for execution. This validation is done up front, and
 * this exception wraps the {@link TaskValidationResult} generated as a result
 * of verifying the task.
 *
 * @author jdcasey
 *
 */
public class InvalidTaskException
    extends BuildFailureException
{

    private final String task;

    public InvalidTaskException( TaskValidationResult result,
                                 Throwable cause )
    {
        super( result.getMessage(), cause );
        task = result.getInvalidTask();
    }

    public String getTask()
    {
        return task;
    }

}
