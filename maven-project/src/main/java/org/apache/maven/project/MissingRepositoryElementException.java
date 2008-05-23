package org.apache.maven.project;

import org.apache.maven.artifact.InvalidRepositoryException;

public class MissingRepositoryElementException
    extends InvalidRepositoryException
{

    public MissingRepositoryElementException( String message,
                                              String repositoryId )
    {
        super( message, repositoryId );
    }

    public MissingRepositoryElementException( String message )
    {
        super( message, "-unknown-" );
    }

}
