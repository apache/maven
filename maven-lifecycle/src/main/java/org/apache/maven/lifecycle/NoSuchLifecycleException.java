package org.apache.maven.lifecycle;

public class NoSuchLifecycleException
    extends LifecycleSpecificationException
{

    private final String packaging;

    public NoSuchLifecycleException( String packaging, String message, Throwable cause )
    {
        super( message, cause );
        this.packaging = packaging;
    }

    public NoSuchLifecycleException( String phase, String message )
    {
        super( message );
        this.packaging = phase;
    }
    
    public String getPackaging()
    {
        return packaging;
    }

}
