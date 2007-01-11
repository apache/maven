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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 */
public class DefaultEventDispatcher
    implements EventDispatcher
{

    private List eventMonitors = new ArrayList();

    public void addEventMonitor( EventMonitor monitor )
    {
        eventMonitors.add( monitor );
    }

    public void dispatchStart( String event, String target )
    {
        for ( Iterator it = eventMonitors.iterator(); it.hasNext(); )
        {
            EventMonitor monitor = (EventMonitor) it.next();
            monitor.startEvent( event, target, System.currentTimeMillis() );
        }
    }

    public void dispatchEnd( String event, String target )
    {
        for ( Iterator it = eventMonitors.iterator(); it.hasNext(); )
        {
            EventMonitor monitor = (EventMonitor) it.next();
            monitor.endEvent( event, target, System.currentTimeMillis() );
        }
    }

    public void dispatchError( String event, String target, Throwable cause )
    {
        for ( Iterator it = eventMonitors.iterator(); it.hasNext(); )
        {
            EventMonitor monitor = (EventMonitor) it.next();
            monitor.errorEvent( event, target, System.currentTimeMillis(), cause );
        }
    }

}