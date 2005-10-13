package org.apache.maven.artifact.manager;

import org.apache.maven.wagon.TransferFailedException;


public class WagonConfigurationException
    extends TransferFailedException
{
    
    static final long serialVersionUID = 1;

    private final String originalMessage;
    private final String repositoryId;

    public WagonConfigurationException( String repositoryId, String message, Throwable cause )
    {
        super( "While configuring wagon for \'" + repositoryId + "\': " + message, cause );
        
        this.repositoryId = repositoryId;
        this.originalMessage = message;
    }

    public WagonConfigurationException( String repositoryId, String message )
    {
        super( "While configuring wagon for \'" + repositoryId + "\': " + message );
        
        this.repositoryId = repositoryId;
        this.originalMessage = message;
    }
    
    public final String getRepositoryId()
    {
        return repositoryId;
    }
    
    public final String getOriginalMessage()
    {
        return originalMessage;
    }

}
