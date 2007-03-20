package org.apache.maven.lifecycle;

public class LifecycleException
    extends Exception
{

    public LifecycleException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public LifecycleException( String message )
    {
        super( message );
    }

}
