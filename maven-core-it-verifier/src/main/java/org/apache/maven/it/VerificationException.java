package org.apache.maven.it;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class VerificationException
    extends Exception
{
    public VerificationException()
    {
    }

    public VerificationException( String message )
    {
        super( message );
    }

    public VerificationException( Throwable cause )
    {
        super( cause );
    }

    public VerificationException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
