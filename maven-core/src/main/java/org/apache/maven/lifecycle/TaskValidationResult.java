package org.apache.maven.lifecycle;

/** @author Jason van Zyl */
public class TaskValidationResult
{
    private String invalidTask;

    private Throwable cause;

    private String message;

    public TaskValidationResult()
    {
    }

    public TaskValidationResult( String invalidTask, String message )
    {
        this.invalidTask = invalidTask;
        this.message = message;
    }

    public TaskValidationResult( String invalidTask, String message, Throwable cause )
    {
        this.message = message;
        this.cause = cause;
        this.invalidTask = invalidTask;
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
}
