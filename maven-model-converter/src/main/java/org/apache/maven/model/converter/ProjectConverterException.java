package org.apache.maven.model.converter;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class ProjectConverterException
    extends Exception
{
    public ProjectConverterException( String message )
    {
        super( message );
    }

    public ProjectConverterException( String message, Throwable throwable )
    {
        super( message, throwable );
    }
}
