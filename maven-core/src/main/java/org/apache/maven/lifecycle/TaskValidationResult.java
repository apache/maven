package org.apache.maven.lifecycle;

import org.apache.maven.InvalidTaskException;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.loader.PluginLoaderException;

/** @author Jason van Zyl */
public class TaskValidationResult
{
    private String invalidTask;

    private String message;

    private Throwable cause;

    public TaskValidationResult()
    {
    }

    public TaskValidationResult( String invalidTask,
                                 String message,
                                 PluginLoaderException cause )
    {
        this.invalidTask = invalidTask;
        this.message = message;
        this.cause = cause;
    }

    public TaskValidationResult( String invalidTask,
                                 String message,
                                 LifecycleSpecificationException cause )
    {
        this.invalidTask = invalidTask;
        this.message = message;
        this.cause = cause;
    }

    public TaskValidationResult( String invalidTask,
                                 String message,
                                 LifecycleLoaderException cause )
    {
        this.invalidTask = invalidTask;
        this.message = message;
        this.cause = cause;
    }

    public TaskValidationResult( String task,
                                 String message,
                                 InvalidPluginException e )
    {
        invalidTask = task;
        this.message = message;
        cause = e;
    }

    public String getInvalidTask()
    {
        return invalidTask;
    }

    public String getMessage()
    {
        return message;
    }

    public Throwable getCause()
    {
        return cause;
    }

    public boolean isTaskValid()
    {
        return invalidTask == null;
    }

    public InvalidTaskException generateInvalidTaskException()
    {
        InvalidTaskException e = new InvalidTaskException( this, cause );

        return e;
    }
}
