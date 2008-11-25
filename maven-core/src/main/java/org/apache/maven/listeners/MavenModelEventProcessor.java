package org.apache.maven.listeners;

import org.apache.maven.execution.MavenSession;

public interface MavenModelEventProcessor
{
    void processModelContainers( MavenSession session );
}
