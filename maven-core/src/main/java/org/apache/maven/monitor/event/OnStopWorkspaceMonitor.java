package org.apache.maven.monitor.event;

public class OnStopWorkspaceMonitor
    extends AbstractWorkspaceMonitor
{

    public void embedderStopped( long timestamp )
    {
        clearCache();
    }

}
