package org.apache.maven.eventspy.internal;

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

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;

/**
 * Forwards execution events to eventspies.
 * @since 3.0.2
 */
class EventSpyExecutionListener
    extends AbstractExecutionListener
{

    private final EventSpyDispatcher dispatcher;

    private final ExecutionListener delegate;

    EventSpyExecutionListener( EventSpyDispatcher dispatcher, ExecutionListener delegate )
    {
        this.dispatcher = dispatcher;
        this.delegate = delegate;
    }

    @Override
    public void projectDiscoveryStarted( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.projectDiscoveryStarted( event );
    }

    @Override
    public void sessionStarted( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.sessionStarted( event );
    }

    @Override
    public void sessionEnded( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.sessionEnded( event );
    }

    @Override
    public void projectSkipped( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.projectSkipped( event );
    }

    @Override
    public void projectStarted( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.projectStarted( event );
    }

    @Override
    public void projectSucceeded( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.projectSucceeded( event );
    }

    @Override
    public void projectFailed( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.projectFailed( event );
    }

    @Override
    public void forkStarted( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.forkStarted( event );
    }

    @Override
    public void forkSucceeded( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.forkSucceeded( event );
    }

    @Override
    public void forkFailed( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.forkFailed( event );
    }

    @Override
    public void mojoSkipped( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.mojoSkipped( event );
    }

    @Override
    public void mojoStarted( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.mojoStarted( event );
    }

    @Override
    public void mojoSucceeded( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.mojoSucceeded( event );
    }

    @Override
    public void mojoFailed( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.mojoFailed( event );
    }

    @Override
    public void forkedProjectStarted( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.forkedProjectStarted( event );
    }

    @Override
    public void forkedProjectSucceeded( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.forkedProjectSucceeded( event );
    }

    @Override
    public void forkedProjectFailed( ExecutionEvent event )
    {
        dispatcher.onEvent( event );
        delegate.forkedProjectFailed( event );
    }

}
