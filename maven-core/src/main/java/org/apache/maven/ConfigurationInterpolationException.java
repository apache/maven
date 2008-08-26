package org.apache.maven;

public class ConfigurationInterpolationException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    public ConfigurationInterpolationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ConfigurationInterpolationException( String message )
    {
        super( message );
    }

}
