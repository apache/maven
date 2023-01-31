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
package org.apache.maven.eventspy.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionListener;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositoryListener;

/**
 * Dispatches callbacks to all registered eventspies.
 * @since 3.0.2
 */
@Component(role = EventSpyDispatcher.class)
public class EventSpyDispatcher {

    @Requirement
    private Logger logger;

    @Requirement(role = EventSpy.class)
    private List<EventSpy> eventSpies;

    public void setEventSpies(List<EventSpy> eventSpies) {
        // make copy to get rid of needless overhead for dynamic lookups
        this.eventSpies = new ArrayList<>(eventSpies);
    }

    public List<EventSpy> getEventSpies() {
        return eventSpies;
    }

    public ExecutionListener chainListener(ExecutionListener listener) {
        if (eventSpies.isEmpty()) {
            return listener;
        }
        return new EventSpyExecutionListener(this, listener);
    }

    public RepositoryListener chainListener(RepositoryListener listener) {
        if (eventSpies.isEmpty()) {
            return listener;
        }
        return new EventSpyRepositoryListener(this, listener);
    }

    public void init(EventSpy.Context context) {
        if (eventSpies.isEmpty()) {
            return;
        }
        for (EventSpy eventSpy : eventSpies) {
            try {
                eventSpy.init(context);
            } catch (Exception | LinkageError e) {
                logError("initialize", e, eventSpy);
            }
        }
    }

    public void onEvent(Object event) {
        if (eventSpies.isEmpty()) {
            return;
        }
        for (EventSpy eventSpy : eventSpies) {
            try {
                eventSpy.onEvent(event);
            } catch (Exception | LinkageError e) {
                logError("notify", e, eventSpy);
            }
        }
    }

    public void close() {
        if (eventSpies.isEmpty()) {
            return;
        }
        for (EventSpy eventSpy : eventSpies) {
            try {
                eventSpy.close();
            } catch (Exception | LinkageError e) {
                logError("close", e, eventSpy);
            }
        }
    }

    private void logError(String action, Throwable e, EventSpy spy) {
        String msg = "Failed to " + action + " spy " + spy.getClass().getName() + ": " + e.getMessage();

        if (logger.isDebugEnabled()) {
            logger.warn(msg, e);
        } else {
            logger.warn(msg);
        }
    }
}
