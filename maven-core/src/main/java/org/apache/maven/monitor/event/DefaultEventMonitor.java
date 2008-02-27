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

    private static final String[] START_EVENTS = {
        MavenEvents.PROJECT_EXECUTION,
        MavenEvents.PHASE_EXECUTION,
        MavenEvents.MOJO_EXECUTION
    };

    private static final String[] END_EVENTS = {
        MavenEvents.PHASE_EXECUTION
    };

    private final Logger logger;

    public DefaultEventMonitor( Logger logger )
    {
        super( START_EVENTS, END_EVENTS, MavenEvents.NO_EVENTS );

        this.logger = logger;
    }

    protected void doStartEvent( String event, String target, long time )
    {
        if ( MavenEvents.MOJO_EXECUTION.equals( event ) )
        {
            logger.info( "[" + target + "]" );
        }
        else if ( MavenEvents.PHASE_EXECUTION.equals( event ) )
        {
            logger.debug( line() );
            logger.debug( "Entering lifecycle phase: " + target );
            logger.debug( line() );
        }
        else if ( MavenEvents.PROJECT_EXECUTION.equals( event ) )
        {
            logger.info( line() );
            String[] targetParts = target.split( "\n" );
            logger.info( "Building " + targetParts[0] );
            if ( targetParts.length > 0 )
            {
                logger.info( "" );
                for ( int i = 1; i < targetParts.length; i++ )
                {
                    logger.info( targetParts[i] );
                }
            }
            logger.info( line() );
        }
    }

    protected void doEndEvent( String event,
                               String target,
                               long timestamp )
    {
        logger.debug( line() );
        logger.debug( "Completed lifecycle phase: " + target );
        logger.debug( line() );
    }

    private String line()
    {
        return "------------------------------------------------------------------------";
    }

}