package org.apache.maven.lifecycle;

public class NoSuchPhaseException
    extends LifecycleSpecificationException
{

    private final String phase;

    public NoSuchPhaseException( String phase, String message, Throwable cause )
    {
        super( message, cause );
        this.phase = phase;
    }

    public NoSuchPhaseException( String phase, String message )
    {
        super( message );
        this.phase = phase;
    }
    
    public String getPhase()
    {
        return phase;
    }

}
