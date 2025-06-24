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

import org.apache.maven.api.Event;
import org.apache.maven.api.EventType;
import org.apache.maven.api.Listener;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;

/**
 * Bridges between Maven3 events and Maven4 events.
 */
@Named
@Singleton
public class EventSpyImpl implements EventSpy {
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
                Event event = new DefaultEvent(session, ee, eventType);
                for (Listener listener : listeners) {
                    listener.onEvent(event);
                }
            }
        }
    }

    /**
     * Simple "conversion" from Maven3 event type enum to Maven4 enum.
     */
    protected EventType convert(ExecutionEvent.Type type) {
        return EventType.values()[type.ordinal()];
    }

    @Override
    public void close() throws Exception {}
}
