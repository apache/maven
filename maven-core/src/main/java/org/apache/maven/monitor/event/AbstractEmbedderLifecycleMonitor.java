package org.apache.maven.monitor.event;


public abstract class AbstractEmbedderLifecycleMonitor
    extends AbstractSelectiveEventMonitor implements MavenEmbedderLifecycleMonitor
{

    public static final String[] EMBEDDER_LIFECYCLE_EVENTS = {
        MavenEvents.EMBEDDER_LIFECYCLE,
        MavenEvents.EMBEDDER_METHOD
    };

    protected AbstractEmbedderLifecycleMonitor()
    {
        super( EMBEDDER_LIFECYCLE_EVENTS, EMBEDDER_LIFECYCLE_EVENTS, MavenEvents.NO_EVENTS );
    }

    public void embedderInitialized( long timestamp )
    {
    }

    public void embedderStopped( long timestamp )
    {
    }

    public void embedderMethodStarted( String method, long timestamp )
    {
    }

    public void embedderMethodEnded( String method, long timestamp )
    {
    }

    protected void doEndEvent( String eventName,
                               String target,
                               long timestamp )
    {
        if ( MavenEvents.EMBEDDER_LIFECYCLE.equals( eventName ) )
        {
            embedderStopped( timestamp );
        }
        else if ( MavenEvents.EMBEDDER_METHOD.equals( eventName ) )
        {
            embedderMethodEnded( target, timestamp );
        }
    }

    protected void doStartEvent( String eventName,
                                 String target,
                                 long timestamp )
    {
        if ( MavenEvents.EMBEDDER_LIFECYCLE.equals( eventName ) )
        {
            embedderInitialized( timestamp );
        }
        else if ( MavenEvents.EMBEDDER_METHOD.equals( eventName ) )
        {
            embedderMethodStarted( target, timestamp );
        }
    }

}
