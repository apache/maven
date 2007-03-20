package org.apache.maven.lifecycle;

public class LifecycleSpecificationException
    extends LifecycleException
{

    public LifecycleSpecificationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public LifecycleSpecificationException( String message )
    {
        super( message );
    }

}
