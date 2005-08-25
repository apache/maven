package org.apache.maven.project;

public class InvalidModelException
    extends ProjectBuildingException
{

    public InvalidModelException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public InvalidModelException( String message )
    {
        super( message );
    }

}
