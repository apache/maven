package org.apache.maven.monitor.event;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
