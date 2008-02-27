package org.apache.maven.monitor.event;

import java.util.List;
import java.util.Map;

public class DeprecationEventDispatcher
    extends DefaultEventDispatcher
{

    private final Map deprecatedEventsByReplacement;

    public DeprecationEventDispatcher( Map deprecatedEventsByReplacement )
    {
        this.deprecatedEventsByReplacement = deprecatedEventsByReplacement;
    }

    public DeprecationEventDispatcher( Map deprecatedEventsByReplacement, List eventMonitors )
    {
        super( eventMonitors );
        this.deprecatedEventsByReplacement = deprecatedEventsByReplacement;
    }

    public void dispatchEnd( String event,
                             String target )
    {
        super.dispatchEnd( event, target );
        if ( deprecatedEventsByReplacement.containsKey( event ) )
        {
            super.dispatchEnd( (String) deprecatedEventsByReplacement.get( event ), target );
        }
    }

    public void dispatchError( String event,
                               String target,
                               Throwable cause )
    {
        super.dispatchError( event, target, cause );
        if ( deprecatedEventsByReplacement.containsKey( event ) )
        {
            super.dispatchError( (String) deprecatedEventsByReplacement.get( event ), target, cause );
        }
    }

    public void dispatchStart( String event,
                               String target )
    {
        super.dispatchStart( event, target );
        if ( deprecatedEventsByReplacement.containsKey( event ) )
        {
            super.dispatchStart( (String) deprecatedEventsByReplacement.get( event ), target );
        }
    }

}
