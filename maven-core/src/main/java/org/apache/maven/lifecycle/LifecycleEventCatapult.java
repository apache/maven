package org.apache.maven.lifecycle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;

/**
 * Assists in firing events from a generic method by abstracting from the actual callback method to be called on the
 * listener.
 * 
 * @author Benjamin Bentmann
 */
public interface LifecycleEventCatapult
{

    /**
     * Notifies the specified listener of the given event.
     * 
     * @param listener The listener to notify, must not be {@code null}.
     * @param event The event to fire, must not be {@code null}.
     */
    void fire( ExecutionListener listener, ExecutionEvent event );

    static final LifecycleEventCatapult SESSION_STARTED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.sessionStarted( event );
        }
    };

    static final LifecycleEventCatapult SESSION_ENDED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.sessionEnded( event );
        }
    };

    static final LifecycleEventCatapult PROJECT_SKIPPED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.projectSkipped( event );
        }
    };

    static final LifecycleEventCatapult PROJECT_STARTED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.projectStarted( event );
        }
    };

    static final LifecycleEventCatapult PROJECT_SUCCEEDED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.projectSucceeded( event );
        }
    };

    static final LifecycleEventCatapult PROJECT_FAILED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.projectFailed( event );
        }
    };

    static final LifecycleEventCatapult MOJO_SKIPPED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.mojoSkipped( event );
        }
    };

    static final LifecycleEventCatapult MOJO_STARTED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.mojoStarted( event );
        }
    };

    static final LifecycleEventCatapult MOJO_SUCCEEDED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.mojoSucceeded( event );
        }
    };

    static final LifecycleEventCatapult MOJO_FAILED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.mojoFailed( event );
        }
    };

    static final LifecycleEventCatapult FORK_STARTED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.forkStarted( event );
        }
    };

    static final LifecycleEventCatapult FORK_SUCCEEDED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.forkSucceeded( event );
        }
    };

    static final LifecycleEventCatapult FORK_FAILED = new LifecycleEventCatapult()
    {
        public void fire( ExecutionListener listener, ExecutionEvent event )
        {
            listener.forkFailed( event );
        }
    };

}
