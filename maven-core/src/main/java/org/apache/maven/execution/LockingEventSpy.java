package org.apache.maven.execution;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.SessionData;
import org.eclipse.sisu.Typed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EventSpy implementation that provides a per-project locking mechanism
 * to make sure a given project can not be built concurrently.
 * This case can happen when running parallel builds with forked lifecycles.
 */
@Singleton
@Named
@Typed( EventSpy.class )
public class LockingEventSpy extends AbstractEventSpy
{

    private static final Object LOCKS_KEY = new Object();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void onEvent( Object event ) throws Exception
    {
        if ( event instanceof ExecutionEvent )
        {
            ExecutionEvent executionEvent = ( ExecutionEvent ) event;
            switch ( executionEvent.getType() )
            {
            case ProjectStarted:
            case ForkedProjectStarted:
                Lock lock = getLock( executionEvent );
                if ( !lock.tryLock() )
                {
                    logger.debug( "Suspending concurrent execution of project '{}'", executionEvent.getProject() );
                    lock.lockInterruptibly();
                    logger.debug( "Resuming execution of project '{}'", executionEvent.getProject() );
                }
                break;
            case ProjectSucceeded:
            case ProjectFailed:
            case ForkedProjectSucceeded:
            case ForkedProjectFailed:
                getLock( executionEvent ).unlock();
                break;
            default:
                break;
            }
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private Lock getLock( ExecutionEvent event )
    {
        SessionData data = event.getSession().getRepositorySession().getData();
        Map<MavenProject, Lock> locks = ( Map ) data.get( LOCKS_KEY );
        // initialize the value if not already done (in case of a concurrent access) to the method
        if ( locks == null )
        {
            // the call to data.set(k, null, v) is effectively a call to data.putIfAbsent(k, v)
            data.set( LOCKS_KEY, null, new ConcurrentHashMap<>() );
            locks = ( Map ) data.get( LOCKS_KEY );
        }
        return locks.computeIfAbsent( event.getProject(), p -> new ReentrantLock() );
    }

}
