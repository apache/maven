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
import org.apache.maven.api.Listener;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges between Maven3 events and Maven4 events.
 * Each listener's {@link Listener#onEvent(org.apache.maven.api.Event)} method handles its own dispatch:
 * legacy listeners receive the event directly, while typed listeners (such as
 * {@link org.apache.maven.api.ExecutionListener}) route it to their specific callbacks.
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
                org.apache.maven.api.ExecutionEvent event = new DefaultEvent(session, ee, eventType);
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
