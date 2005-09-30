package org.apache.maven.usability.plugin;

public class ExpressionDocumentationException
    extends Exception
{
    static final long serialVersionUID = 1;

    public ExpressionDocumentationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ExpressionDocumentationException( String message )
    {
        super( message );
    }

}
