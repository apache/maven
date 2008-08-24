package org.apache.maven.monitor.event;



public class PerCallWorkspaceMonitor
    extends AbstractWorkspaceMonitor
{

    public void embedderMethodEnded( String method,
                                        long timestamp )
    {
        clearCache();
    }

    // Be double-sure that the cache is cleared when the embedder stops.
    public void embedderStopped( long timestamp )
    {
        clearCache();
    }



}
