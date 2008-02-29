package org.apache.maven.monitor.event;

import org.apache.maven.workspace.MavenWorkspaceStore;

public abstract class AbstractWorkspaceMonitor
    extends AbstractEmbedderLifecycleMonitor
    implements MavenWorkspaceMonitor
{

    private MavenWorkspaceStore workspaceManager;

    public void setWorkspaceStore( MavenWorkspaceStore workspaceManager )
    {
        this.workspaceManager = workspaceManager;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.embedder.lifecycle.MavenWorkspaceMonitor#clearCache()
     */
    public void clearCache()
    {
        workspaceManager.clear();
    }

}