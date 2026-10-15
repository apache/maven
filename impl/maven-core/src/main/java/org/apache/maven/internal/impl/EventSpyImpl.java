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

import java.lang.ref.WeakReference;
import java.util.Collection;

import org.apache.maven.api.DependencyResolutionEvent;
import org.apache.maven.api.EventType;
import org.apache.maven.api.Listener;
import org.apache.maven.api.Project;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges between Maven3 events and Maven4 events, for the execution events and for the dependency resolution
 * of a project.
 * Each listener's {@link Listener#onEvent(org.apache.maven.api.Event)} method handles its own dispatch:
 * legacy listeners receive the event directly, while typed listeners (such as
 * {@link org.apache.maven.api.ExecutionListener}) route it to their specific callbacks.
 */
@Named
@Singleton
public class EventSpyImpl implements EventSpy {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSpyImpl.class);

    /**
     * The request of the dependency resolution in progress on the current thread. The result carries neither the
     * project nor the session, and the lifecycle dispatches the request and the result from the same thread.
     * A request is always dispatched before its result and replaces any earlier one, so a request whose resolution
     * ended with an unexpected failure, and therefore without a result, is never matched with a later result.
     * It is held weakly so that such a request is not retained by an idle thread.
     */
    private final ThreadLocal<WeakReference<DependencyResolutionRequest>> pendingResolution = new ThreadLocal<>();

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
                dispatch(listeners, new DefaultEvent(session, ee, eventType), eventType);
            }
        } else if (arg instanceof DependencyResolutionRequest request) {
            // drop any stale entry left by a resolution that ended without a result
            pendingResolution.remove();
            pendingResolution.set(new WeakReference<>(request));
            dispatchDependencyResolution(request, null);
        } else if (arg instanceof DependencyResolutionResult result) {
            WeakReference<DependencyResolutionRequest> pending = pendingResolution.get();
            pendingResolution.remove();
            DependencyResolutionRequest request = pending != null ? pending.get() : null;
            if (request != null) {
                dispatchDependencyResolution(request, result);
            }
        }
    }

    /**
     * Notifies the listeners that the dependencies of the project of the request are about to be resolved, or,
     * when a result is given, that they have been resolved or have failed to.
     *
     * @param request the request of the resolution
     * @param result the result of the resolution, or {@code null} when the resolution is about to start
     */
    private void dispatchDependencyResolution(DependencyResolutionRequest request, DependencyResolutionResult result) {
        InternalMavenSession session = InternalMavenSession.from(request.getRepositorySession());
        Collection<Listener> listeners = session.getListeners();
        Project project = session.getProject(request.getMavenProject());
        // a project without a base directory has no API counterpart to report
        if (project != null && !listeners.isEmpty()) {
            DependencyResolutionEvent event = result != null
                    ? new DefaultDependencyResolutionEvent(session, project, result)
                    : new DefaultDependencyResolutionEvent(session, project);
            dispatch(listeners, event, event.type());
        }
    }

    private void dispatch(Collection<Listener> listeners, org.apache.maven.api.Event event, Object eventType) {
        for (Listener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOGGER.warn(
                        "Failed to notify listener {} about {}",
                        listener.getClass().getName(),
                        eventType,
                        e);
            }
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
