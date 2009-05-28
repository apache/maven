package org.apache.maven.exception;

// provide a
// - the exception
// - useful message
// - useful reference to a solution, or set of solutions
// - the configuration gleaned for examination
// - plugin repositories

public class ExceptionSummary
{
    private Exception exception;
    
    private String message;
    
    private String reference;

    public ExceptionSummary( Exception exception, String message, String reference )
    {
        this.exception = exception;
        this.message = message;
        this.reference = reference;
    }

    public Exception getException()
    {
        return exception;
    }

    public String getMessage()
    {
        return message;
    }

    public String getReference()
    {
        return reference;
    }        
}
