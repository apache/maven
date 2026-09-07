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
package org.apache.maven.internal.impl;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;

import org.apache.maven.api.EventType;
import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.Listener;
import org.apache.maven.api.TypedListener;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges between Maven3 events and Maven4 events.
 */
@Named
@Singleton
public class EventSpyImpl implements EventSpy {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSpyImpl.class);

    @Override
    public void init(Context context) throws Exception {}

    @Override
    public void onEvent(Object arg) throws Exception {
        if (arg instanceof ExecutionEvent ee) {
            InternalMavenSession session =
                    InternalMavenSession.from(ee.getSession().getSession());
            EventType eventType = convert(ee.getType());
            Collection<Listener> listeners = session.getListeners();
            if (!listeners.isEmpty()) {
                org.apache.maven.api.ExecutionEvent event = null;
                for (Listener listener : listeners) {
                    if (listener instanceof TypedListener && !(listener instanceof ExecutionListener)) {
                        continue;
                    }
                    if (event == null) {
                        event = new DefaultEvent(session, ee, eventType);
                    }
                    try {
                        if (listener instanceof ExecutionListener executionListener) {
                            dispatch(executionListener, event);
                        } else {
                            listener.onEvent(event);
                        }
                    } catch (RuntimeException e) {
                        LOGGER.warn(
                                "Failed to notify execution listener {} about {}",
                                listener.getClass().getName(),
                                event.type(),
                                e);
                    }
                }
            }
        }
    }

    private static void dispatch(ExecutionListener listener, org.apache.maven.api.ExecutionEvent event) {
        switch (event.type()) {
            case PROJECT_DISCOVERY_STARTED -> listener.projectDiscoveryStarted(event);
            case SESSION_STARTED -> listener.sessionStarted(event);
            case SESSION_ENDED -> listener.sessionEnded(event);
            case PROJECT_SKIPPED -> listener.projectSkipped(event);
            case PROJECT_STARTED -> listener.projectStarted(event);
            case PROJECT_SUCCEEDED -> listener.projectSucceeded(event);
            case PROJECT_FAILED -> listener.projectFailed(event);
            case MOJO_SKIPPED -> listener.mojoSkipped(event);
            case MOJO_STARTED -> listener.mojoStarted(event);
            case MOJO_SUCCEEDED -> listener.mojoSucceeded(event);
            case MOJO_FAILED -> listener.mojoFailed(event);
            case FORK_STARTED -> listener.forkStarted(event);
            case FORK_SUCCEEDED -> listener.forkSucceeded(event);
            case FORK_FAILED -> listener.forkFailed(event);
            case FORKED_PROJECT_STARTED -> listener.forkedProjectStarted(event);
            case FORKED_PROJECT_SUCCEEDED -> listener.forkedProjectSucceeded(event);
            case FORKED_PROJECT_FAILED -> listener.forkedProjectFailed(event);
            default -> throw new IllegalArgumentException("Unsupported execution event: " + event.type());
        }
    }

    /**
     * Converts the Maven 3 execution event type to its Maven API counterpart.
     */
    protected EventType convert(ExecutionEvent.Type type) {
        return EventType.values()[type.ordinal()];
    }

    @Override
    public void close() throws Exception {}
}
