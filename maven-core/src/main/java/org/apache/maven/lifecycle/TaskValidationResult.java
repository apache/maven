package org.apache.maven.lifecycle;

import org.apache.maven.InvalidTaskException;
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
        InvalidTaskException e = null;
        if ( cause instanceof LifecycleLoaderException )
        {
            e = new InvalidTaskException( this, (LifecycleLoaderException)cause );
        }
        else if ( cause instanceof LifecycleSpecificationException )
        {
            e = new InvalidTaskException( this, (LifecycleSpecificationException)cause );
        }
        else if ( cause instanceof PluginLoaderException )
        {
            e = new InvalidTaskException( this, (PluginLoaderException)cause );
        }
        else
        {
            throw new IllegalStateException( "No matching constructor in InvalidTaskException for TaskValidationResult cause: " + cause + " ( invalid task: " + invalidTask + ")" );
        }

        return e;
    }
}
