package org.apache.maven;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.plugin.loader.PluginLoaderException;

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
                                 LifecycleLoaderException cause )
    {
        super( result.getMessage(), cause );
        task = result.getInvalidTask();
    }

    public InvalidTaskException( TaskValidationResult result,
                                 LifecycleSpecificationException cause )
    {
        super( result.getMessage(), cause );
        task = result.getInvalidTask();
    }

    public InvalidTaskException( TaskValidationResult result,
                                 PluginLoaderException cause )
    {
        super( result.getMessage(), cause );
        task = result.getInvalidTask();
    }

    public String getTask()
    {
        return task;
    }

}
