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

import org.codehaus.plexus.logging.Logger;

/**
 * @author jdcasey
 */
public class DefaultEventMonitor
    extends AbstractSelectiveEventMonitor
{

    private static final String[] START_EVENTS = {MavenEvents.MOJO_EXECUTION};

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