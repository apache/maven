package org.apache.maven.lifecycle;

public abstract class LifecycleException
    extends Exception
{

    protected LifecycleException( String message, Throwable cause )
    {
        super( message, cause );
    }

    protected LifecycleException( String message )
    {
        super( message );
    }

}
