package org.apache.maven.monitor.event;

import java.util.Arrays;
import java.util.List;

/**
 * @author jdcasey
 */
public abstract class AbstractSelectiveEventMonitor
    implements EventMonitor
{
    
    private List boundStartEvents;
    private List boundErrorEvents;
    private List boundEndEvents;

    protected AbstractSelectiveEventMonitor(String[] startEvents, String[] endEvents, String[] errorEvents)
    {
        this.boundStartEvents = Arrays.asList( startEvents );
        
        this.boundEndEvents = Arrays.asList( endEvents );
        
        this.boundErrorEvents = Arrays.asList( errorEvents );
    }

    public final void startEvent( String eventName, String target, long timestamp )
    {
        if( boundStartEvents.contains( eventName ) )
        {
            doStartEvent( eventName, target, timestamp );
        }
    }
    
    protected void doStartEvent( String eventName, String target, long timestamp )
    {
    }

    public final void endEvent( String eventName, String target, long timestamp )
    {
        if( boundEndEvents.contains( eventName ) )
        {
            doEndEvent( eventName, target, timestamp );
        }
    }

    protected void doEndEvent( String eventName, String target, long timestamp )
    {
    }

    public final void errorEvent( String eventName, String target, long timestamp, Throwable cause )
    {
        if( boundErrorEvents.contains( eventName ) )
        {
            doErrorEvent( eventName, target, timestamp, cause );
        }
    }

    protected void doErrorEvent( String eventName, String target, long timestamp, Throwable cause )
    {
    }

}
