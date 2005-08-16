package org.apache.maven.profiles.activation;

public class ProfileActivationException
    extends Exception
{

    public ProfileActivationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ProfileActivationException( String message )
    {
        super( message );
    }

}
