package org.apache.maven.execution.manager;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenExecutionRequestHandlerNotFoundException
    extends Exception
{
    public MavenExecutionRequestHandlerNotFoundException( String message )
    {
        super( message );
    }

    public MavenExecutionRequestHandlerNotFoundException( Throwable cause )
    {
        super( cause );
    }

    public MavenExecutionRequestHandlerNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
