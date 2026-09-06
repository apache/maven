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

import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionEventType;
import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.Listener;
import org.apache.maven.eventspy.EventSpy;

/**
 * Bridges between Maven3 events and Maven4 events.
 * Dispatches to both the deprecated generic {@link Listener#onEvent(org.apache.maven.api.Event)} handler
 * and the typed {@link ExecutionListener} callbacks.
 */
@Named
@Singleton
public class EventSpyImpl implements EventSpy {
    @Override
    public void init(Context context) throws Exception {}

    @Override
    public void onEvent(Object arg) throws Exception {
        if (arg instanceof org.apache.maven.execution.ExecutionEvent ee) {
            InternalMavenSession session =
                    InternalMavenSession.from(ee.getSession().getSession());
            ExecutionEventType eventType = convert(ee.getType());
            Collection<Listener> listeners = session.getListeners();
            if (!listeners.isEmpty()) {
                ExecutionEvent event = new DefaultEvent(session, ee, eventType);
                for (Listener listener : listeners) {
                    // Call deprecated generic handler for backward compatibility
                    listener.onEvent(event);
                    // Call typed handler for new-style listeners
                    if (listener instanceof ExecutionListener el) {
                        dispatchTyped(el, event, eventType);
                    }
                }
            }
        }
    }

    private void dispatchTyped(ExecutionListener listener, ExecutionEvent event, ExecutionEventType type) {
        switch (type) {
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
            default -> {}
        }
    }

    /**
     * Simple "conversion" from Maven3 event type enum to Maven4 enum.
     */
    protected ExecutionEventType convert(org.apache.maven.execution.ExecutionEvent.Type type) {
        return ExecutionEventType.values()[type.ordinal()];
    }

    @Override
    public void close() throws Exception {}
}
