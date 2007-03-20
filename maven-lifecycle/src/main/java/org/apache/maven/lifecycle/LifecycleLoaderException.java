package org.apache.maven.lifecycle;

public class LifecycleLoaderException
    extends LifecycleException
{

    public LifecycleLoaderException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public LifecycleLoaderException( String message )
    {
        super( message );
    }

}
