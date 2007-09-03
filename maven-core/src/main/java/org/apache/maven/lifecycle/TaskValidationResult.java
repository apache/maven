package org.apache.maven.lifecycle;

/** @author Jason van Zyl */
public class TaskValidationResult
{
    private String invalidTask;

    private String message;

    public TaskValidationResult()
    {
    }

    public TaskValidationResult( String invalidTask, String message )
    {
        this.invalidTask = invalidTask;

        this.message = message;
    }

    public String getInvalidTask()
    {
        return invalidTask;
    }

    public String getMessage()
    {
        return message;
    }

    public boolean isTaskValid()
    {
        return invalidTask == null;
    }
}
