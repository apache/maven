package org.apache.maven.monitor.event;

import org.codehaus.plexus.logging.Logger;

/**
 * @author jdcasey
 */
public class DefaultEventMonitor
    extends AbstractSelectiveEventMonitor
{
    
    private static final String[] START_EVENTS = {
        MavenEvents.MOJO_EXECUTION
    };
    
    private final Logger logger;

    public DefaultEventMonitor( Logger logger )
    {
        super( START_EVENTS, MavenEvents.NO_EVENTS, MavenEvents.NO_EVENTS );
        
        this.logger = logger;
    }

    protected void doStartEvent( String event, String target, long time )
    {
        logger.info( "[" + target + "]" );
    }

}