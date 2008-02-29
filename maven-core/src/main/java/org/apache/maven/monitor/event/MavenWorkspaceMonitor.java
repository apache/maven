package org.apache.maven.monitor.event;

import org.apache.maven.workspace.MavenWorkspaceStore;

public interface MavenWorkspaceMonitor
    extends EventMonitor
{

    void setWorkspaceStore( MavenWorkspaceStore workspaceStore );

    void clearCache();

}
