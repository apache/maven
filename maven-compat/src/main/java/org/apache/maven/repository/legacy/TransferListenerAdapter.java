package org.apache.maven.repository.legacy;

import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

public class TransferListenerAdapter
    implements TransferListener
{
    private ArtifactTransferListener transferListener;

    public TransferListenerAdapter( ArtifactTransferListener transferListener )
    {
        this.transferListener = transferListener;
    }

    public void debug( String arg0 )
    {
    }

    public void transferCompleted( TransferEvent arg0 )
    {
    }

    public void transferError( TransferEvent arg0 )
    {
    }

    public void transferInitiated( TransferEvent arg0 )
    {
    }

    public void transferProgress( TransferEvent arg0, byte[] arg1, int arg2 )
    {
    }

    public void transferStarted( TransferEvent arg0 )
    {
    }    
}
