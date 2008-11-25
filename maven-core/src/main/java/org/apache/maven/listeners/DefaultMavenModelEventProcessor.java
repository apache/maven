package org.apache.maven.listeners;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = MavenModelEventProcessor.class)
public class DefaultMavenModelEventProcessor
    implements MavenModelEventProcessor
{
    @Requirement
    List<MavenModelEventListener> listeners;

    public void processModelContainers( MavenSession session )
    {
        for( MavenModelEventListener listener : listeners )
        {
            listener.processModelContainers( session );
        }
    }        
}
