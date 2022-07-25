package org.apache.maven.internal.impl;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;
import java.util.Optional;

import org.apache.maven.api.Event;
import org.apache.maven.api.EventType;
import org.apache.maven.api.Listener;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;

@Named
@Singleton
public class EventSpyImpl implements EventSpy
{

    private DefaultSessionFactory sessionFactory;

    @Inject
    EventSpyImpl( DefaultSessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void init( Context context ) throws Exception
    {
    }

    @Override
    public void onEvent( Object arg ) throws Exception
    {
        if ( arg instanceof ExecutionEvent )
        {
            ExecutionEvent ee = (ExecutionEvent) arg;
            AbstractSession session =  ( AbstractSession ) sessionFactory.getSession( ee.getSession() );
            Collection<Listener> listeners = session.getListeners();
            if ( !listeners.isEmpty() )
            {
                Event event = new EventWrapper( session, ee );
                for ( Listener listener : listeners )
                {
                    listener.onEvent( event );
                }
            }
        }
    }

    @Override
    public void close() throws Exception
    {
    }

    static class MojoExecutionWrapper implements MojoExecution
    {
        private final org.apache.maven.plugin.MojoExecution delegate;

        MojoExecutionWrapper( org.apache.maven.plugin.MojoExecution delegate )
        {
            this.delegate = delegate;
        }

        @Override
        public Plugin getPlugin()
        {
            return delegate.getPlugin().getDelegate();
        }

        @Override
        public String getExecutionId()
        {
            return delegate.getExecutionId();
        }

        @Override
        public String getGoal()
        {
            return delegate.getGoal();
        }
    }

    static class EventWrapper implements Event
    {
        private final AbstractSession session;
        private final ExecutionEvent delegate;

        EventWrapper( AbstractSession session, ExecutionEvent delegate )
        {
            this.session = session;
            this.delegate = delegate;
        }

        @Override
        public EventType getType()
        {
            return EventType.valueOf( delegate.getType().name() );
        }

        @Override
        public Session getSession()
        {
            return session;
        }

        @Override
        public Optional<Project> getProject()
        {
            return Optional.ofNullable( delegate.getProject() ).map( session::getProject );
        }

        @Override
        public Optional<MojoExecution> getMojoExecution()
        {
            return Optional.ofNullable( delegate.getMojoExecution() ).map( MojoExecutionWrapper::new );
        }

        @Override
        public Optional<Exception> getException()
        {
            return Optional.empty();
        }
    }
}
