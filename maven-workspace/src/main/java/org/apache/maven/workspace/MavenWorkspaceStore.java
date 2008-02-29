package org.apache.maven.workspace;

import java.util.Map;

public interface MavenWorkspaceStore
{

    Map getWorkspaceCache( String cacheType );

    void initWorkspaceCache( String cacheType, Map cache );

    void clear();

}
